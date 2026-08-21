package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.util.Text;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.EnumSet;
import java.util.logging.Level;

public class DiscordBot extends ListenerAdapter {
    private final PlotManager plugin;
    private JDA jda;
    private boolean ready;

    public DiscordBot(PlotManager plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!plugin.cfg().getBoolean("discord.enabled", false)) return;
        String token = plugin.cfg().getString("discord.bot_token", "");
        if (token == null || token.isBlank() || token.contains("YOUR_BOT_TOKEN")) {
            plugin.getLogger().info("Discord bot disabled (no token).");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                jda = JDABuilder.createLight(token, EnumSet.of(
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT,
                                GatewayIntent.GUILD_MEMBERS
                        ))
                        .addEventListeners(this)
                        .setStatus(OnlineStatus.ONLINE)
                        .setActivity(Activity.watching(statusText()))
                        .build();
                jda.awaitReady();
                ready = true;
                plugin.getLogger().info("Discord bot connected.");
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Failed to start Discord bot: " + t.getMessage());
                ready = false;
            }
        });
    }

    public void shutdown() {
        ready = false;
        if (jda != null) {
            try { jda.shutdownNow(); } catch (Exception ignored) {}
            jda = null;
        }
    }

    public String status() {
        return ready ? "ONLINE" : (plugin.cfg().getBoolean("discord.enabled") ? "OFFLINE" : "DISABLED");
    }

    public boolean ready() { return ready; }

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
        send(plugin.cfg().getString("discord.join_leave_channel_id"),
                template.replace("%player%", player));
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
        log(msg.replace("%plot_id%", plot.id.toString().substring(0, 8))
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
                .replace("%plot_id%", plot.id.toString().substring(0, 8))
                .replace("%owner%", plot.ownerName));
    }

    public void snitch(Plot plot) {
        String msg = plugin.cfg().getString("blackmarket.snitch_discord_message", "Anonymous tip");
        log(msg.replace("%plot_id%", plot.id.toString().substring(0, 8))
                .replace("%owner%", plot.ownerName));
    }

    public void maybeRole(Plot plot) {
        if (!ready || !plugin.cfg().getBoolean("discord.role_sync.enabled", true)) return;
        int need = plugin.cfg().getInt("discord.role_sync.required_plot_level", 10);
        if (plot.level < need) return;
        String roleId = plugin.cfg().getString("discord.role_sync.plot_lord_role_id", "");
        if (roleId == null || roleId.contains("YOUR_")) return;
        // Role IDs require linked Discord accounts; we log instead of guessing snowflakes.
        log(":crown: **" + plot.ownerName + "** reached Plot Lord (Level " + plot.level + ")");
    }

    public void refreshStatus() {
        if (!ready || jda == null) return;
        try {
            jda.getPresence().setActivity(Activity.watching(statusText()));
        } catch (Exception ignored) {}
    }

    private String statusText() {
        return plugin.cfg().getString("discord.bot_status", "Watching plots")
                .replace("%total_plots%", String.valueOf(plugin.store.plots.size()));
    }

    private void send(String channelId, String message) {
        if (!ready || jda == null || channelId == null || channelId.isBlank() || channelId.contains("YOUR_")) return;
        if (message == null || message.isBlank()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                TextChannel ch = jda.getTextChannelById(channelId.trim());
                if (ch != null) ch.sendMessage(message).queue();
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!plugin.cfg().getBoolean("discord.chat_sync.enabled", true)) return;
        if (event.getAuthor().isBot()) return;
        String chatId = plugin.cfg().getString("discord.chat_sync.chat_channel_id", "");
        if (chatId == null || !chatId.equals(event.getChannel().getId())) return;
        String fmt = plugin.cfg().getString("discord.chat_sync.discord_to_minecraft_format", "&9[Discord] &f%user%&7: &f%message%");
        String line = fmt.replace("%user%", event.getAuthor().getName())
                .replace("%message%", event.getMessage().getContentDisplay());
        Bukkit.getScheduler().runTask(plugin, () -> Text.broadcast(plugin.prefix() + line));
    }
}
