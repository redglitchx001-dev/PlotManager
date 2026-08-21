/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.hologram;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.CustomHologram;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HologramEngine {
    private final PlotManager plugin;
    private final NamespacedKey key;
    private final Map<String, List<ArmorStand>> spawned = new ConcurrentHashMap<>();
    private long tick;

    public HologramEngine(PlotManager plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "hologram");
    }

    public void start() {
        for (Plot plot : plugin.store.plots.values()) {
            spawnPlot(plot);
            for (CustomHologram h : plot.holograms) spawnCustom(plot, h);
        }
        spawnLeaderboard();
        int interval = Math.max(10, plugin.cfg().getInt("auto_hologram.update_ticks", 40));
        plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void shutdown() {
        for (List<ArmorStand> list : spawned.values()) {
            for (ArmorStand stand : list) {
                if (stand != null && stand.isValid()) stand.remove();
            }
        }
        spawned.clear();
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof ArmorStand stand && stand.getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                    stand.remove();
                }
            }
        }
    }

    public void spawnPlot(Plot plot) {
        despawn("plot:" + plot.id);
        if (!plugin.cfg().getBoolean("auto_hologram.enabled", true)) return;
        Location loc = plot.hologramLocation();
        if (loc == null) return;
        double height = plugin.cfg().getDouble("auto_hologram.height_above_center", 3.0);
        if (plot.holoWorld == null) loc.setY(plot.maxY + height);
        List<String> lines = linesFor(plot);
        spawnLines("plot:" + plot.id, loc, lines);
    }

    public void spawnCustom(Plot plot, CustomHologram holo) {
        String id = "custom:" + plot.id + ":" + holo.id;
        despawn(id);
        World w = Bukkit.getWorld(holo.world);
        if (w == null) return;
        spawnLines(id, new Location(w, holo.x, holo.y, holo.z), holo.lines);
    }

    public void spawnLeaderboard() {
        despawn("leaderboard");
        if (!plugin.cfg().getBoolean("leaderboard.enabled", true)) return;
        if (plugin.store.leaderboardWorld == null) return;
        World w = Bukkit.getWorld(plugin.store.leaderboardWorld);
        if (w == null) return;
        Location loc = new Location(w, plugin.store.leaderboardX, plugin.store.leaderboardY, plugin.store.leaderboardZ);
        spawnLines("leaderboard", loc, leaderboardLines());
    }

    public void despawnPlot(Plot plot) {
        despawn("plot:" + plot.id);
        for (CustomHologram h : plot.holograms) despawn("custom:" + plot.id + ":" + h.id);
    }

    public void despawn(String id) {
        List<ArmorStand> list = spawned.remove(id);
        if (list == null) return;
        for (ArmorStand stand : list) {
            if (stand != null && stand.isValid()) stand.remove();
        }
    }

    private void tick() {
        tick++;
        boolean anim = plugin.cfg().getBoolean("auto_hologram.animation.enabled", true);
        double speed = plugin.cfg().getDouble("auto_hologram.animation.speed", 0.02);
        double bob = anim ? Math.sin(tick * speed) * 0.12 : 0;
        for (Plot plot : plugin.store.plots.values()) {
            List<ArmorStand> list = spawned.get("plot:" + plot.id);
            if (list == null || list.isEmpty()) {
                spawnPlot(plot);
                continue;
            }
            List<String> lines = linesFor(plot);
            update(list, lines, bob);
            for (CustomHologram h : plot.holograms) {
                List<ArmorStand> cl = spawned.get("custom:" + plot.id + ":" + h.id);
                if (cl == null) spawnCustom(plot, h);
                else update(cl, h.lines, bob * 0.5);
            }
        }
        if (tick % 10 == 0) spawnLeaderboard();
    }

    private void update(List<ArmorStand> stands, List<String> lines, double bob) {
        int n = Math.min(stands.size(), lines.size());
        for (int i = 0; i < n; i++) {
            ArmorStand stand = stands.get(i);
            if (stand == null || !stand.isValid()) continue;
            stand.customName(Text.component(plugin.placeholders(null, lines.get(i), null)));
            if (bob != 0) {
                Location l = stand.getLocation();
                // keep tiny bob without accumulating: we teleport relative to current? skip if marker
            }
        }
    }

    private List<String> linesFor(Plot plot) {
        String path = "auto_hologram.lines_occupied";
        if ("vip".equalsIgnoreCase(plot.premiumTier)) path = "auto_hologram.lines_premium_vip";
        if ("mvp".equalsIgnoreCase(plot.premiumTier) || "god".equalsIgnoreCase(plot.premiumTier)) {
            path = "auto_hologram.lines_premium_mvp";
        }
        List<String> raw = plugin.cfg().getStringList(path);
        List<String> out = new ArrayList<>();
        Player owner = plot.owner == null ? null : Bukkit.getPlayer(plot.owner);
        for (String line : raw) {
            out.add(plugin.placeholders(owner, line, plot));
        }
        return out;
    }

    private List<String> leaderboardLines() {
        List<String> lines = new ArrayList<>();
        lines.add(plugin.cfg().getString("leaderboard.title", "&2&l━━ Top 10 Richest Plots ━━"));
        int max = plugin.cfg().getInt("leaderboard.max_entries", 10);
        var richest = plugin.store.richest(max);
        String format = plugin.cfg().getString("leaderboard.entry_format", "&a#%rank% &2%owner% &7- &a$%bank% &8(Lvl %level%)");
        String empty = plugin.cfg().getString("leaderboard.empty_entry", "&7#%rank% — Empty");
        for (int i = 0; i < max; i++) {
            if (i < richest.size()) {
                Plot p = richest.get(i);
                lines.add(format
                        .replace("%rank%", String.valueOf(i + 1))
                        .replace("%owner%", p.ownerName)
                        .replace("%bank%", Text.money(p.bank))
                        .replace("%level%", String.valueOf(p.level))
                        .replace("%plot_name%", p.name));
            } else {
                lines.add(empty.replace("%rank%", String.valueOf(i + 1)));
            }
        }
        return lines;
    }

    private void spawnLines(String id, Location base, List<String> lines) {
        if (base == null || base.getWorld() == null) return;
        double spacing = plugin.cfg().getDouble("auto_hologram.line_spacing", 0.28);
        List<ArmorStand> list = new ArrayList<>();
        double startY = base.getY() + (lines.size() - 1) * spacing * 0.5;
        for (int i = 0; i < lines.size(); i++) {
            Location loc = base.clone();
            loc.setY(startY - i * spacing);
            loc.setPitch(0);
            loc.setYaw(0);
            ArmorStand stand = spawnStand(loc, id);
            stand.customName(Text.component(plugin.placeholders(null, lines.get(i), null)));
            list.add(stand);
        }
        spawned.put(id, list);
    }

    private ArmorStand spawnStand(Location loc, String id) {
        World world = loc.getWorld();
        ArmorStand stand = world.spawn(loc, ArmorStand.class, as -> {
            as.setInvisible(plugin.cfg().getBoolean("auto_hologram.invisible_armorstand", true));
            as.setMarker(true);
            as.setGravity(false);
            as.setSmall(true);
            as.setBasePlate(false);
            as.setArms(false);
            as.setCollidable(false);
            as.setInvulnerable(true);
            as.setPersistent(false);
            as.setSilent(true);
            as.setCanPickupItems(false);
            as.setCustomNameVisible(true);
            if (plugin.cfg().getBoolean("auto_hologram.glow_effect", false)) as.setGlowing(true);
            as.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
            as.setRemoveWhenFarAway(false);
        });
        return stand;
    }

    public CustomHologram nearest(Plot plot, Location loc, double max) {
        CustomHologram best = null;
        double bestD = max * max;
        for (CustomHologram h : plot.holograms) {
            if (h.world == null || loc.getWorld() == null || !h.world.equals(loc.getWorld().getName())) continue;
            double d = loc.distanceSquared(new Location(loc.getWorld(), h.x, h.y, h.z));
            if (d < bestD) {
                bestD = d;
                best = h;
            }
        }
        return best;
    }
}
