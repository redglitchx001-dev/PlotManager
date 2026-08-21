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
import org.bukkit.entity.Player;

/**
 * Discord integration façade.
 * <p>
 * This class contains every message template and rule, but not a single Discord
 * class: the actual gateway ({@code DiscordGateway}) is resolved reflectively and
 * only when the feature is switched on. If the bundled library is missing, the
 * token is blank or Discord is unreachable, every method here degrades to a
 * silent no-op instead of breaking plugin startup.
 */
public class DiscordBot {

    private static final String GATEWAY = "com.redglitchx.plotmanager.integration.discord.DiscordGateway";

    private final PlotManager plugin;
    private volatile DiscordSink sink;
    private volatile String failure;

    public DiscordBot(PlotManager plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.cfg().getBoolean("discord.enabled", false)) return;
        String token = plugin.cfg().getString("discord.bot_token", "");
        if (token == null || token.isBlank() || token.contains("YOUR_BOT_TOKEN")) {
            plugin.getLogger().info("Discord bot disabled (no token configured).");
            return;
        }
        final String trimmed = token.trim();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            DiscordSink created;
            try {
                created = (DiscordSink) Class.forName(GATEWAY)
                        .getConstructor(PlotManager.class)
                        .newInstance(plugin);
            } catch (Throwable t) {
                failure = "library unavailable";
                plugin.getLogger().warning("Discord bot could not start (bundled library unavailable): " + t);
                return;
            }
            try {
                created.connect(trimmed, statusText());
                sink = created;
                plugin.getLogger().info("Discord bot connected.");
            } catch (Throwable t) {
                failure = String.valueOf(t.getMessage());
                try {
                    created.shutdown();
                } catch (Throwable ignored) {
                }
                plugin.getLogger().warning("Discord bot failed to log in: " + t.getMessage()
                        + " (this is optional - PlotManager keeps running)");
            }
        });
    }

    public void shutdown() {
        DiscordSink current = sink;
        sink = null;
        if (current != null) {
            try {
                current.shutdown();
            } catch (Throwable ignored) {
            }
        }
    }

    public boolean ready() {
        DiscordSink current = sink;
        return current != null && current.ready();
    }

    public String status() {
        if (ready()) return "ONLINE";
        if (!plugin.cfg().getBoolean("discord.enabled", false)) return "DISABLED";
        return failure == null ? "CONNECTING" : "OFFLINE";
    }

    public void log(String message) {
        send(plugin.cfg().getString("discord.log_channel_id"), message);
    }

    public void chatFromMinecraft(Player player, String message) {
        if (!plugin.cfg().getBoolean("discord.chat_sync.enabled", true)) return;
        String fmt = plugin.cfg().getString("discord.chat_sync.minecraft_to_discord_format", "[MC] **%player%**: %message%");
        send(plugin.cfg().getString("discord.chat_sync.chat_channel_id"),
                fmt.replace("%player%", player.getName()).replace("%message%", message));
    }

    public void joinLeave(String template, String player) {
        if (template == null) return;
        send(plugin.cfg().getString("discord.join_leave_channel_id"), template.replace("%player%", player));
    }

    public void claim(Player player, Plot plot, double cost) {
        String msg = plugin.cfg().getString("discord.claim_log_message", "%player% claimed a plot");
        log(msg.replace("%player%", player.getName())
                .replace("%x%", String.valueOf(plot.minX))
                .replace("%z%", String.valueOf(plot.minZ))
                .replace("%cost%", Text.money(cost)));
    }

    public void reset(Plot plot, int days) {
        String msg = plugin.cfg().getString("discord.reset_log_message", "Plot reset");
        log(msg.replace("%plot_id%", shortId(plot))
                .replace("%owner%", plot.ownerName)
                .replace("%days%", String.valueOf(days)));
    }

    public void purge(String admin, int count) {
        String msg = plugin.cfg().getString("discord.purge_log_message", "Purged plots");
        log(msg.replace("%admin%", admin).replace("%count%", String.valueOf(count)));
    }

    public void seize(String admin, Plot plot) {
        String msg = plugin.cfg().getString("discord.seize_log_message", "Plot seized");
        log(msg.replace("%admin%", admin)
                .replace("%plot_id%", shortId(plot))
                .replace("%owner%", plot.ownerName));
    }

    public void snitch(Plot plot) {
        String msg = plugin.cfg().getString("blackmarket.snitch_discord_message", "Anonymous tip");
        log(msg.replace("%plot_id%", shortId(plot)).replace("%owner%", plot.ownerName));
    }

    public void maybeRole(Plot plot) {
        if (!ready() || !plugin.cfg().getBoolean("discord.role_sync.enabled", true)) return;
        int need = plugin.cfg().getInt("discord.role_sync.required_plot_level", 10);
        if (plot.level < need) return;
        String roleId = plugin.cfg().getString("discord.role_sync.plot_lord_role_id", "");
        if (roleId == null || roleId.contains("YOUR_")) return;
        // Role IDs require linked Discord accounts; we announce instead of guessing snowflakes.
        log(":crown: **" + plot.ownerName + "** reached Plot Lord (Level " + plot.level + ")");
    }

    public void refreshStatus() {
        DiscordSink current = sink;
        if (current == null || !current.ready()) return;
        try {
            current.activity(statusText());
        } catch (Throwable ignored) {
        }
    }

    private String statusText() {
        return plugin.cfg().getString("discord.bot_status", "Watching plots")
                .replace("%total_plots%", String.valueOf(plugin.store == null ? 0 : plugin.store.plots.size()));
    }

    private static String shortId(Plot plot) {
        String id = plot.id.toString();
        return id.length() > 8 ? id.substring(0, 8) : id;
    }

    private void send(String channelId, String message) {
        DiscordSink current = sink;
        if (current == null || !current.ready()) return;
        if (channelId == null || channelId.isBlank() || channelId.contains("YOUR_")) return;
        if (message == null || message.isBlank()) return;
        if (!plugin.isEnabled()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                current.send(channelId, message);
            } catch (Throwable ignored) {
            }
        });
    }
}
