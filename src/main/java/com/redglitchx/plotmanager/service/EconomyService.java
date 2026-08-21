package com.redglitchx.plotmanager.service;

import com.redglitchx.plotmanager.PlotManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyService {
    private final PlotManager plugin;
    private Economy economy;

    public EconomyService(PlotManager plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        economy = rsp.getProvider();
        return economy != null;
    }

    public boolean ready() {
        return economy != null;
    }

    public String status() {
        return economy == null ? "OFFLINE" : "ONLINE (" + economy.getName() + ")";
    }

    public double balance(OfflinePlayer player) {
        if (economy == null || player == null) return 0;
        return economy.getBalance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        if (economy == null) return false;
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (amount <= 0) return true;
        if (economy == null) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public boolean charge(Player player, double amount, String failMessage) {
        if (amount <= 0) return true;
        if (!has(player, amount)) {
            plugin.msg(player, failMessage);
            plugin.fx(player, "upgrade_denied");
            return false;
        }
        return withdraw(player, amount);
    }
}
