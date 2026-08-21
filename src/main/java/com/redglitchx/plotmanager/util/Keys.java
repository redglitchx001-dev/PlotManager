package com.redglitchx.plotmanager.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

public class Keys {
    public final NamespacedKey wand;
    public final NamespacedKey godWand;
    public final NamespacedKey rollbackWand;
    public final NamespacedKey map;
    public final NamespacedKey smartHopper;

    public Keys(Plugin plugin) {
        wand = new NamespacedKey(plugin, "wand");
        godWand = new NamespacedKey(plugin, "godwand");
        rollbackWand = new NamespacedKey(plugin, "rollbackwand");
        map = new NamespacedKey(plugin, "plotmap");
        smartHopper = new NamespacedKey(plugin, "smarthopper");
    }
}
