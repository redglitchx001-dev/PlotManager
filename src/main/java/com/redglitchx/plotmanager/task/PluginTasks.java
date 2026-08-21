package com.redglitchx.plotmanager.task;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import com.redglitchx.plotmanager.util.FX;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.Hopper;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PluginTasks {
    private PluginTasks() {}

    public static void start(PlotManager plugin) {
        long save = Math.max(1, plugin.cfg().getInt("plugin.auto_save_interval_minutes", 5)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, plugin.store::saveSync, save, save);

        Bukkit.getScheduler().runTaskTimer(plugin, plugin::tickGenerators, 20L * 30, 20L * 30);

        long inactivity = Math.max(1, plugin.cfg().getInt("reset_system.check_interval_minutes", 60)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> checkInactivity(plugin), inactivity, inactivity);

        int border = Math.max(5, plugin.cfg().getInt("border_particles.interval_ticks", 15));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> borders(plugin), border, border);

        long snitch = Math.max(1, plugin.cfg().getInt("blackmarket.snitch_check_interval_minutes", 60)) * 60L * 20L;
        Bukkit.getScheduler().runTaskTimer(plugin, () -> snitch(plugin), snitch, snitch);

        Bukkit.getScheduler().runTaskTimer(plugin, () -> upkeep(plugin), 20L * 60, 20L * 60 * 30);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> smartClear(plugin), plugin.cfg().getLong("smart_clear.interval_ticks", 100), plugin.cfg().getLong("smart_clear.interval_ticks", 100));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> factories(plugin), plugin.cfg().getLong("factories.smelt_interval_ticks", 200), plugin.cfg().getLong("factories.smelt_interval_ticks", 200));
        Bukkit.getScheduler().runTaskTimer(plugin, () -> sorters(plugin), 40L, 40L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> visitors(plugin), 20L * 60, 20L * 60);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> plugin.discord.refreshStatus(), 20L * 60, 20L * 60);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> plugin.maybeMayor(null), 20L * 120, 20L * 120);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> cropBoost(plugin), 40L, 40L);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> jumpElevators(plugin), 4L, 4L);
    }

    private static void checkInactivity(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("reset_system.enabled", true)) return;
        int days = plugin.cfg().getInt("reset_system.inactivity_days", 7);
        int warn = plugin.cfg().getInt("reset_system.warning_days_before", 2);
        long now = System.currentTimeMillis();
        long cutoff = now - days * 86_400_000L;
        long warnAt = now - (days - warn) * 86_400_000L;
        for (Plot plot : List.copyOf(plugin.store.plots.values())) {
            if (plot.lastOwnerLogin < cutoff) {
                plugin.discord.reset(plot, days);
                plugin.deletePlot(plot, true);
            } else if (plot.lastOwnerLogin < warnAt) {
                Player owner = plot.owner == null ? null : Bukkit.getPlayer(plot.owner);
                if (owner != null && now - plot.lastWarning > 3_600_000L) {
                    long remaining = Math.max(1, (plot.lastOwnerLogin + days * 86_400_000L - now) / 86_400_000L);
                    plugin.lang.msg(owner, "reset_system.warning_message", "%remaining%", String.valueOf(remaining));
                    plot.lastWarning = now;
                }
            }
        }
    }

    private static void borders(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("border_particles.enabled", true) && plugin.store.plots.values().stream().noneMatch(p -> p.frozen || p.borderCosmetic != null)) {
            // still draw frozen
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            Plot plot = plugin.store.index.atXZ(player.getLocation());
            if (plot == null) continue;
            String particle = plugin.cfg().getString("border_particles.particle", "VILLAGER_HAPPY");
            if (plot.frozen) particle = plugin.cfg().getString("border_particles.frozen_particle", "REDSTONE");
            else if (plot.borderCosmetic != null) {
                // cosmetic is a block border; still use particles for members
            }
            int density = plugin.cfg().getInt("border_particles.density", 8);
            var c = plot.cuboid();
            Location loc = player.getLocation();
            double y = loc.getY() + 0.2;
            for (int i = 0; i < density; i++) {
                double t = (player.getTicksLived() + i * 8) % 360;
                double ang = Math.toRadians(t);
                // nearest edge particles
                Location a = new Location(loc.getWorld(), c.minX + 0.5, y, loc.getZ() + Math.sin(ang));
                Location b = new Location(loc.getWorld(), c.maxX + 0.5, y, loc.getZ() + Math.cos(ang));
                Location d = new Location(loc.getWorld(), loc.getX() + Math.sin(ang), y, c.minZ + 0.5);
                Location e = new Location(loc.getWorld(), loc.getX() + Math.cos(ang), y, c.maxZ + 0.5);
                if (loc.distanceSquared(a) < 64) FX.spawn(a, particle, 1);
                if (loc.distanceSquared(b) < 64) FX.spawn(b, particle, 1);
                if (loc.distanceSquared(d) < 64) FX.spawn(d, particle, 1);
                if (loc.distanceSquared(e) < 64) FX.spawn(e, particle, 1);
            }
        }
        // physical cosmetic border blocks are not placed automatically (would grief builds);
        // particle walls represent the purchased style.
    }

    private static void snitch(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("blackmarket.enabled", true)) return;
        for (Plot plot : plugin.store.plots.values()) {
            if (plot.blackmarketUsed) plugin.maybeSnitch(plot);
        }
    }

    private static void upkeep(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("upkeep.enabled", true)) return;
        double cost = plugin.cfg().getDouble("upkeep.cost_per_cycle", 0);
        if (cost <= 0) return;
        long hours = plugin.cfg().getLong("upkeep.cycle_hours", 168);
        long cycle = hours * 3_600_000L;
        long now = System.currentTimeMillis();
        for (Plot plot : plugin.store.plots.values()) {
            if (now - plot.lastUpkeep < cycle) continue;
            Player owner = plot.owner == null ? null : Bukkit.getPlayer(plot.owner);
            if (plot.bank >= cost) {
                plot.bank -= cost;
                plot.lastUpkeep = now;
                if (owner != null) plugin.lang.msg(owner, "upkeep.upkeep_paid_message", "%cost%", Text.money(cost));
            } else {
                if (owner != null) plugin.lang.msg(owner, "upkeep.upkeep_failed_message");
                plot.lastUpkeep = now;
            }
        }
    }

    private static void smartClear(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("smart_clear.enabled", true)) return;
        List<String> snow = plugin.cfg().getStringList("smart_clear.snow_blocks");
        List<String> grass = plugin.cfg().getStringList("smart_clear.grass_blocks");
        List<String> weeds = plugin.cfg().getStringList("smart_clear.weed_blocks");
        for (Player player : Bukkit.getOnlinePlayers()) {
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null) continue;
            boolean doSnow = plot.flag(PlotFlag.AUTOWIPE_SNOW);
            boolean doGrass = plot.flag(PlotFlag.AUTOWIPE_GRASS);
            boolean doWeeds = plot.flag(PlotFlag.AUTOWIPE_WEEDS);
            if (!doSnow && !doGrass && !doWeeds) continue;
            Location loc = player.getLocation();
            World w = loc.getWorld();
            if (w == null) continue;
            int r = 8;
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    for (int y = -2; y <= 3; y++) {
                        Block b = w.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                        if (!plot.contains(b.getLocation())) continue;
                        String n = b.getType().name();
                        if (doSnow && snow.contains(n)) b.setType(n.equals("SNOW_BLOCK") ? Material.GRASS_BLOCK : Material.AIR, false);
                        else if (doGrass && grass.contains(n)) b.setType(Material.AIR, false);
                        else if (doWeeds && weeds.contains(n)) b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }

    private static void factories(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("factories.enabled", true)) return;
        var map = plugin.cfg().getConfigurationSection("factories.smelt_map");
        if (map == null || !plugin.cfg().getBoolean("factories.virtual_smelter", true)) return;
        for (Plot plot : plugin.store.plots.values()) {
            if (!plot.factoryUnlocked) continue;
            for (String from : map.getKeys(false)) {
                Material src = Items.material(from, null);
                Material dst = Items.material(map.getString(from), null);
                if (src == null || dst == null) continue;
                ItemStack taken = plot.takeFromVault(src, 1);
                if (taken != null) plot.addToVault(new ItemStack(dst, 1));
            }
        }
    }

    private static void sorters(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("sorters.enabled", true)) return;
        int radius = plugin.cfg().getInt("sorters.scan_radius", 6);
        String hopperName = plugin.cfg().getString("sorters.hopper_name", "&a&lSmart Hopper");
        for (Player player : Bukkit.getOnlinePlayers()) {
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null || !plot.sorterUnlocked) continue;
            World w = player.getWorld();
            Location loc = player.getLocation();
            Map<Material, Container> targets = new HashMap<>();
            for (var entity : w.getNearbyEntities(loc, radius, radius, radius)) {
                if (entity instanceof ItemFrame frame && frame.getItem() != null && !frame.getItem().getType().isAir()) {
                    Block attached = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
                    if (attached.getState() instanceof Container c && plot.contains(attached.getLocation())) {
                        targets.put(frame.getItem().getType(), c);
                    }
                }
            }
            if (targets.isEmpty()) continue;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -2; y <= 2; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block b = w.getBlockAt(loc.getBlockX() + x, loc.getBlockY() + y, loc.getBlockZ() + z);
                        if (!(b.getState() instanceof Hopper hopper)) continue;
                        if (!plot.contains(b.getLocation())) continue;
                        var meta = hopper.getInventory();
                        // treat hoppers as smart hoppers inside unlocked plots
                        for (int i = 0; i < meta.getSize(); i++) {
                            ItemStack it = meta.getItem(i);
                            if (it == null) continue;
                            Container dest = targets.get(it.getType());
                            if (dest == null) continue;
                            HashMap<Integer, ItemStack> left = dest.getInventory().addItem(it);
                            if (left.isEmpty()) meta.setItem(i, null);
                            else meta.setItem(i, left.values().iterator().next());
                        }
                    }
                }
            }
        }
    }

    private static void visitors(PlotManager plugin) {
        int xp = plugin.cfg().getInt("leveling.exp_per_visitor_minute", 5);
        if (xp <= 0) return;
        for (Player player : Bukkit.getOnlinePlayers()) {
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null || plot.isOwner(player.getUniqueId())) continue;
            plugin.addPlotExp(plot, xp);
        }
    }

    private static void cropBoost(PlotManager plugin) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null || !plot.cropBoost) continue;
            Location loc = player.getLocation();
            World w = loc.getWorld();
            if (w == null) continue;
            Block b = w.getBlockAt(loc.getBlockX() + ThreadLocalRandom(), loc.getBlockY(), loc.getBlockZ() + ThreadLocalRandom());
            if (!plot.contains(b.getLocation())) continue;
            if (b.getBlockData() instanceof org.bukkit.block.data.Ageable age && age.getAge() < age.getMaximumAge()) {
                age.setAge(age.getAge() + 1);
                b.setBlockData(age, false);
            }
        }
    }

    private static int ThreadLocalRandom() {
        return java.util.concurrent.ThreadLocalRandom.current().nextInt(-6, 7);
    }

    private static void jumpElevators(PlotManager plugin) {
        if (!plugin.cfg().getBoolean("elevators.enabled", true)) return;
        Material mat = Items.material(plugin.cfg().getString("elevators.block"), Material.IRON_BLOCK);
        int max = plugin.cfg().getInt("elevators.max_distance", 64);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnGround() || player.getVelocity().getY() <= 0.08) continue;
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null) continue;
            Block at = player.getLocation().subtract(0, 1, 0).getBlock();
            if (at.getType() != mat && player.getLocation().getBlock().getRelative(0, -1, 0).getType() != mat) {
                at = player.getLocation().getBlock().getRelative(0, -1, 0);
                if (at.getType() != mat) continue;
            }
            for (int y = at.getY() + 1; y <= Math.min(plot.maxY, at.getY() + max); y++) {
                Block b = at.getWorld().getBlockAt(at.getX(), y, at.getZ());
                if (b.getType() == mat) {
                    player.teleport(b.getLocation().add(0.5, 1, 0.5));
                    FX.play(player, plugin.cfg().getString("elevators.sound", "ENTITY_ENDERMAN_TELEPORT"));
                    break;
                }
            }
        }
    }
}
