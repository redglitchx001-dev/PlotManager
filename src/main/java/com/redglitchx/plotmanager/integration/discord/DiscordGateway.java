/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.integration.discord;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.integration.DiscordSink;
import com.redglitchx.plotmanager.util.Text;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;

import java.util.EnumSet;

/**
 * The only class in PlotManager that touches the bundled Discord library.
 * <p>
 * It is instantiated reflectively by {@code DiscordBot} and <b>only</b> when the
 * Discord bot is enabled and a token is configured, so a server that never uses
 * Discord never loads a single Discord class.
 */
public final class DiscordGateway extends ListenerAdapter implements DiscordSink {

    private final PlotManager plugin;
    private volatile JDA jda;
    private volatile boolean ready;

    public DiscordGateway(PlotManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean connect(String token, String activity) throws Throwable {
        jda = JDABuilder.createLight(token, EnumSet.of(
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS))
                .addEventListeners(this)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching(activity == null ? "plots" : activity))
                .build();
        jda.awaitReady();
        ready = true;
        return true;
    }

    @Override
    public void shutdown() {
        ready = false;
        JDA current = jda;
        jda = null;
        if (current != null) {
            try {
                current.shutdownNow();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean ready() {
        return ready && jda != null;
    }

    @Override
    public void send(String channelId, String message) {
        if (!ready()) return;
        try {
            MessageChannel channel = jda.getChannelById(MessageChannel.class, channelId.trim());
            if (channel != null) channel.sendMessage(message).queue();
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void activity(String text) {
        if (!ready()) return;
        try {
            jda.getPresence().setActivity(Activity.watching(text));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            if (!plugin.cfg().getBoolean("discord.chat_sync.enabled", true)) return;
            if (event.getAuthor().isBot()) return;
            String chatId = plugin.cfg().getString("discord.chat_sync.chat_channel_id", "");
            if (chatId == null || !chatId.trim().equals(event.getChannel().getId())) return;
            String fmt = plugin.cfg().getString("discord.chat_sync.discord_to_minecraft_format",
                    "&9[Discord] &f%user%&7: &f%message%");
            String line = fmt.replace("%user%", event.getAuthor().getName())
                    .replace("%message%", event.getMessage().getContentDisplay());
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> Text.broadcast(plugin.prefix() + line));
        } catch (Throwable ignored) {
        }
    }
}
