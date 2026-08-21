/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class PapiHook {
    private final PlotManager plugin;
    private final boolean available;
    private boolean registered;
    private Method setPlaceholders;

    public PapiHook(PlotManager plugin) {
        this.plugin = plugin;
        boolean found = false;
        try {
            found = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        } catch (Throwable ignored) {
        }
        this.available = found;
        if (!found) return;
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholders = api.getMethod("setPlaceholders", OfflinePlayer.class, String.class);
            // Loaded reflectively: PapiExpansion extends a PlaceholderAPI class, so it
            // must never be touched on servers without PlaceholderAPI installed.
            Class<?> expansion = Class.forName("com.redglitchx.plotmanager.integration.PapiExpansion");
            Object inst = expansion.getConstructor(PlotManager.class).newInstance(plugin);
            Object ok = expansion.getMethod("register").invoke(inst);
            registered = !(ok instanceof Boolean b) || b;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "PlaceholderAPI hook failed (placeholders stay internal)", t);
        }
    }

    public boolean available() { return available; }

    public String status() {
        if (!available) return "NOT INSTALLED";
        return registered ? "ONLINE" : "INSTALLED (expansion not registered)";
    }

    public String apply(Player player, String text) {
        return apply((OfflinePlayer) player, text);
    }

    public String apply(OfflinePlayer player, String text) {
        if (text == null) return "";
        if (setPlaceholders != null && player != null) {
            try {
                Object out = setPlaceholders.invoke(null, player, text);
                if (out instanceof String s) return s;
            } catch (Throwable ignored) {
            }
        }
        return text;
    }

    public static String builtin(PlotManager plugin, Player player, String params) {
        Plot at = player == null ? null : plugin.store.index.at(player.getLocation());
        List<Plot> owned = player == null ? List.of() : plugin.store.ownedBy(player.getUniqueId());
        String none = plugin.cfg().getString("placeholders.none_text", "None");
        String wild = plugin.cfg().getString("placeholders.wilderness_text", "Wilderness");
        return switch (params) {
            case "plots_owned" -> String.valueOf(owned.size());
            case "plots_max" -> String.valueOf(player == null ? 0 : plugin.luckPerms.maxPlots(player));
            case "current_plot_name" -> at == null ? wild : at.name;
            case "current_plot_id" -> at == null ? none : at.id.toString().substring(0, 8);
            case "current_plot_owner" -> at == null ? none : at.ownerName;
            case "current_plot_level" -> at == null ? "0" : String.valueOf(at.level);
            case "current_plot_exp" -> at == null ? "0" : String.valueOf(at.exp);
            case "current_plot_bank" -> at == null ? "0" : Text.money(at.bank);
            case "current_plot_vault_tier" -> at == null ? "0" : String.valueOf(at.vaultPages);
            case "current_plot_members" -> at == null ? "0" : String.valueOf(at.memberCount());
            case "current_plot_members_max" -> String.valueOf(plugin.cfg().getInt("members.max_members_default", 5));
            case "current_plot_members_format" -> {
                if (at == null) yield "0/0";
                yield at.memberCount() + "/" + plugin.cfg().getInt("members.max_members_default", 5);
            }
            case "current_plot_role" -> {
                if (at == null || player == null) yield none;
                var r = at.roleOf(player.getUniqueId());
                yield r == null ? "Visitor" : r.display;
            }
            case "current_plot_status" -> at == null
                    ? plugin.cfg().getString("placeholders.free_text", "FREE")
                    : plugin.cfg().getString("placeholders.occupied_text", "OCCUPIED");
            case "current_plot_created" -> at == null ? none : Text.formatDate(at.created, plugin.cfg().getString("placeholders.date_format"), plugin.cfg().getString("plugin.timezone"));
            case "current_plot_description" -> at == null ? none : at.description;
            case "current_plot_size" -> at == null ? none : at.sizeLabel();
            case "current_plot_generators" -> at == null ? "0" : String.valueOf(at.generators.size());
            case "current_plot_upkeep" -> String.valueOf(plugin.cfg().getDouble("upkeep.cost_per_cycle"));
            case "current_plot_flags" -> at == null ? none : at.flagsLabel();
            case "in_wilderness" -> at == null ? plugin.cfg().getString("placeholders.yes_text", "Yes") : plugin.cfg().getString("placeholders.no_text", "No");
            case "total_server_plots" -> String.valueOf(plugin.store.plots.size());
            case "total_server_players" -> String.valueOf(plugin.store.plots.values().stream().map(p -> p.owner).distinct().count());
            case "player_total_bank" -> Text.money(owned.stream().mapToDouble(p -> p.bank).sum());
            case "player_rank" -> {
                var richest = plugin.store.richest(9999);
                if (player == null) yield "0";
                int rank = 1;
                UUID id = player.getUniqueId();
                for (Plot p : richest) {
                    if (id.equals(p.owner)) yield String.valueOf(rank);
                    rank++;
                }
                yield "0";
            }
            default -> top(plugin, params);
        };
    }

    private static String top(PlotManager plugin, String params) {
        if (params == null || !params.startsWith("top_")) return null;
        String rest = params.substring(4);
        int idx = rest.indexOf('_');
        if (idx < 0) return null;
        int n;
        try { n = Integer.parseInt(rest.substring(0, idx)); } catch (Exception e) { return null; }
        String field = rest.substring(idx + 1);
        var list = plugin.store.richest(10);
        if (n < 1 || n > list.size()) return plugin.cfg().getString("placeholders.none_text", "None");
        Plot p = list.get(n - 1);
        return switch (field) {
            case "owner" -> p.ownerName;
            case "bank" -> Text.money(p.bank);
            case "level" -> String.valueOf(p.level);
            case "name" -> p.name;
            default -> null;
        };
    }
}
