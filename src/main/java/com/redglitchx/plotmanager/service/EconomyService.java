/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.service;

import com.redglitchx.plotmanager.PlotManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Vault economy bridge.
 * <p>
 * Every call into Vault goes through reflection on purpose: this class holds
 * <b>no</b> compile-time reference to {@code net.milkbowl.vault.*}, so a server
 * without Vault (or with a Vault fork such as VaultUnlocked) can never produce a
 * {@code NoClassDefFoundError} while PlotManager is enabling. When no economy is
 * present the plugin still loads — only money features go idle.
 */
public class EconomyService {

    private final PlotManager plugin;

    private Object economy;          // net.milkbowl.vault.economy.Economy
    private String providerName = "";
    private Method mGetName;
    private Method mGetBalance;
    private Method mHas;
    private Method mWithdraw;
    private Method mDeposit;
    private Method mTransactionSuccess;

    public EconomyService(PlotManager plugin) {
        this.plugin = plugin;
    }

    /**
     * Attempts to bind to a Vault economy provider.
     *
     * @return true when an economy is available after this call
     */
    public boolean setup() {
        if (economy != null) return true;
        try {
            Class<?> economyClass = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(economyClass);
            if (rsp == null) return false;
            Object provider = rsp.getProvider();
            if (provider == null) return false;

            mGetName = economyClass.getMethod("getName");
            mGetBalance = economyClass.getMethod("getBalance", OfflinePlayer.class);
            mHas = economyClass.getMethod("has", OfflinePlayer.class, double.class);
            mWithdraw = economyClass.getMethod("withdrawPlayer", OfflinePlayer.class, double.class);
            mDeposit = economyClass.getMethod("depositPlayer", OfflinePlayer.class, double.class);
            mTransactionSuccess = Class.forName("net.milkbowl.vault.economy.EconomyResponse")
                    .getMethod("transactionSuccess");

            economy = provider;
            try {
                Object name = mGetName.invoke(economy);
                providerName = name == null ? "Unknown" : String.valueOf(name);
            } catch (Throwable ignored) {
                providerName = provider.getClass().getSimpleName();
            }
            return true;
        } catch (ClassNotFoundException notInstalled) {
            return false;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Could not hook the Vault economy", t);
            return false;
        }
    }

    /** True when a Vault economy provider is bound. */
    public boolean ready() {
        return economy != null;
    }

    public String providerName() {
        return providerName;
    }

    public String status() {
        return economy == null ? "OFFLINE" : "ONLINE (" + providerName + ")";
    }

    public double balance(OfflinePlayer player) {
        if (economy == null || player == null) return 0;
        try {
            Object out = mGetBalance.invoke(economy, player);
            return out instanceof Number n ? n.doubleValue() : 0;
        } catch (Throwable t) {
            return 0;
        }
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        if (economy == null || player == null) return false;
        try {
            Object out = mHas.invoke(economy, player, amount);
            return out instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        return transact(mWithdraw, player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        return transact(mDeposit, player, amount);
    }

    private boolean transact(Method method, OfflinePlayer player, double amount) {
        if (economy == null || player == null || method == null) return false;
        try {
            Object response = method.invoke(economy, player, amount);
            if (response == null) return false;
            Object ok = mTransactionSuccess.invoke(response);
            return ok instanceof Boolean b && b;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Economy transaction failed", t);
            return false;
        }
    }

    /**
     * Charges a player, messaging them when they cannot afford it.
     * With no economy installed, purchases are refused rather than given away.
     */
    public boolean charge(Player player, double amount, String failMessage) {
        if (amount <= 0) return true;
        if (economy == null) {
            String message;
            try {
                message = plugin.lang.line(player, "economy.no_economy_message");
            } catch (Throwable t) {
                message = plugin.cfg().getString("economy.no_economy_message",
                        "&cMoney features are disabled: no Vault economy is installed on this server.");
            }
            plugin.msg(player, message);
            return false;
        }
        if (!has(player, amount)) {
            plugin.msg(player, failMessage);
            plugin.fx(player, "upgrade_denied");
            return false;
        }
        return withdraw(player, amount);
    }
}
