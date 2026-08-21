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
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

/**
 * Soft hook for Simple Voice Chat.
 * <p>
 * Isolation is exposed as a persistent-data tag that voice add-ons can read; the
 * plugin never touches a voice-chat class, so plot entry works exactly the same
 * on servers without the mod installed.
 */
public class VoiceHook {

    private final PlotManager plugin;
    private final boolean available;

    public VoiceHook(PlotManager plugin) {
        this.plugin = plugin;
        boolean found = false;
        try {
            found = Bukkit.getPluginManager().getPlugin("voicechat") != null
                    || Bukkit.getPluginManager().getPlugin("SimpleVoiceChat") != null;
        } catch (Throwable ignored) {
        }
        this.available = found;
    }

    public boolean available() {
        return available;
    }

    public String status() {
        return available ? "ONLINE" : "NOT INSTALLED";
    }

    public void enter(Player player, Plot plot) {
        if (!available || player == null || plot == null) return;
        if (!plugin.cfg().getBoolean("voice.enabled", true)) return;
        try {
            player.getPersistentDataContainer()
                    .set(plugin.keys.voiceChannel, PersistentDataType.STRING, "plot:" + plot.id);
        } catch (Throwable ignored) {
        }
    }

    public void leave(Player player, Plot plot) {
        if (player == null) return;
        try {
            player.getPersistentDataContainer().remove(plugin.keys.voiceChannel);
        } catch (Throwable ignored) {
        }
    }
}
