/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.integration;

/**
 * JDA-free contract between the plugin and the bundled Discord gateway.
 * <p>
 * Nothing in this interface mentions a Discord class, so {@link DiscordBot} can
 * be loaded, used and garbage collected on servers where the shaded Discord
 * library is unavailable or disabled — it simply never resolves an
 * implementation.
 */
public interface DiscordSink {

    /** Opens the gateway connection. Returns true once the bot is logged in. */
    boolean connect(String token, String activity) throws Throwable;

    /** Closes the connection; safe to call when never connected. */
    void shutdown();

    /** True when the bot is connected and able to send messages. */
    boolean ready();

    /** Sends a plain message to a channel id; no-ops when not ready. */
    void send(String channelId, String message);

    /** Updates the "watching ..." presence text. */
    void activity(String text);
}
