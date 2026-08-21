/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

/** Central registry of every persistent-data key PlotManager writes. */
public class Keys {
    public final NamespacedKey wand;
    public final NamespacedKey godWand;
    public final NamespacedKey rollbackWand;
    public final NamespacedKey map;
    public final NamespacedKey smartHopper;
    /** Voice-channel tag written on players inside a plot (read by voice-chat add-ons). */
    public final NamespacedKey voiceChannel;

    public Keys(Plugin plugin) {
        wand = new NamespacedKey(plugin, "wand");
        godWand = new NamespacedKey(plugin, "godwand");
        rollbackWand = new NamespacedKey(plugin, "rollbackwand");
        map = new NamespacedKey(plugin, "plotmap");
        smartHopper = new NamespacedKey(plugin, "smarthopper");
        voiceChannel = new NamespacedKey(plugin, "voicechannel");
    }
}
