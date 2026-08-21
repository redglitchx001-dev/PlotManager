package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PapiExpansion extends PlaceholderExpansion {
    private final PlotManager plugin;

    public PapiExpansion(PlotManager plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "plotmanager"; }
    @Override public @NotNull String getAuthor() { return "RedGlitchX"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        return plugin.placeholder(player, params);
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        Player p = player == null || !player.isOnline() ? null : player.getPlayer();
        return plugin.placeholder(p, params);
    }
}
