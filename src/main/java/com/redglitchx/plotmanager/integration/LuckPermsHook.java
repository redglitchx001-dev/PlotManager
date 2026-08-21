package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.PremiumTier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Resolves premium tiers from LuckPerms group names listed in config.
 * LuckPerms (and Vault) expose groups as permission nodes {@code group.<name>}.
 */
public class LuckPermsHook {
    private final PlotManager plugin;
    public final List<PremiumTier> tiers = new ArrayList<>();

    public LuckPermsHook(PlotManager plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        tiers.clear();
        ConfigurationSection sec = plugin.cfg().getConfigurationSection("premium.tiers");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            PremiumTier t = new PremiumTier();
            t.id = id;
            t.displayName = sec.getString(id + ".display_name", id);
            t.luckpermsGroups = sec.getStringList(id + ".luckperms_groups");
            t.claimCost = sec.getDouble(id + ".claim_cost");
            t.maxPlots = sec.getInt(id + ".max_plots", 3);
            t.maxMembers = sec.getInt(id + ".max_members", 5);
            t.hopperLimit = sec.getInt(id + ".hopper_limit", 50);
            t.spawnerLimit = sec.getInt(id + ".spawner_limit", 10);
            tiers.add(t);
        }
        tiers.sort(Comparator.comparingInt((PremiumTier t) -> t.maxPlots).reversed());
    }

    public PremiumTier tierOf(Player player) {
        if (player == null || !plugin.cfg().getBoolean("premium.enabled", true)) return null;
        for (PremiumTier t : tiers) {
            if (inGroups(player, t.luckpermsGroups) || player.hasPermission("plotmanager.premium." + t.id)) {
                return t;
            }
        }
        return null;
    }

    public boolean inGroups(Player player, List<String> groups) {
        if (player == null || groups == null) return false;
        for (String g : groups) {
            if (g == null || g.isEmpty()) continue;
            if (player.hasPermission("group." + g.toLowerCase(Locale.ROOT))) return true;
            if (player.hasPermission("group." + g)) return true;
        }
        return false;
    }

    public int maxPlots(Player player) {
        PremiumTier t = tierOf(player);
        if (t != null) return t.maxPlots;
        if (player != null && (inGroups(player, List.of("vip")) || player.hasPermission("plotmanager.premium.claim"))) {
            return plugin.cfg().getInt("claiming.max_plots_per_player_vip", 10);
        }
        return plugin.cfg().getInt("claiming.max_plots_per_player_default", 3);
    }

    public int maxMembers(Player player) {
        PremiumTier t = tierOf(player);
        if (t != null) return t.maxMembers;
        return plugin.cfg().getInt("members.max_members_default", 5);
    }

    public int hopperLimit(Player player) {
        PremiumTier t = tierOf(player);
        if (t != null) return t.hopperLimit;
        return plugin.cfg().getInt("plot_protections.hopper_limit", 50);
    }

    public int spawnerLimit(Player player) {
        PremiumTier t = tierOf(player);
        if (t != null) return t.spawnerLimit;
        return plugin.cfg().getInt("plot_protections.spawner_limit", 10);
    }
}
