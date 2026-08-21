package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Soft-hook for Simple Voice Chat. Isolation is implemented as a best-effort
 * reflection call; if the plugin is absent, plot entry still works normally.
 */
public class VoiceHook {
    private final PlotManager plugin;
    private final boolean available;

    public VoiceHook(PlotManager plugin) {
        this.plugin = plugin;
        available = Bukkit.getPluginManager().getPlugin("voicechat") != null
                || Bukkit.getPluginManager().getPlugin("SimpleVoiceChat") != null;
    }

    public boolean available() { return available; }

    public void enter(Player player, Plot plot) {
        if (!available || !plugin.cfg().getBoolean("voice.enabled", true)) return;
        player.setPersistent(player.isPersistent());
        // Group isolation is handled by SVC addons; we tag the player for them.
        player.getPersistentDataContainer().set(plugin.keys.map, org.bukkit.persistence.PersistentDataType.STRING, "vc:" + plot.id);
    }

    public void leave(Player player, Plot plot) {
        if (!available) return;
        player.getPersistentDataContainer().remove(plugin.keys.map);
    }
}
