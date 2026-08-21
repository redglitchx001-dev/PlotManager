package com.redglitchx.plotmanager.data;

import com.redglitchx.plotmanager.util.Cuboid;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpatialIndex {
    private final Map<String, Set<UUID>> chunks = new ConcurrentHashMap<>();
    private final Map<UUID, Plot> plots;

    public SpatialIndex(Map<UUID, Plot> plots) {
        this.plots = plots;
    }

    public void rebuild() {
        chunks.clear();
        for (Plot plot : plots.values()) index(plot);
    }

    public void index(Plot plot) {
        Cuboid c = plot.cuboid();
        for (String key : c.chunkKeys()) {
            chunks.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(plot.id);
        }
    }

    public void remove(Plot plot) {
        Cuboid c = plot.cuboid();
        for (String key : c.chunkKeys()) {
            Set<UUID> set = chunks.get(key);
            if (set != null) {
                set.remove(plot.id);
                if (set.isEmpty()) chunks.remove(key);
            }
        }
    }

    public Plot at(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String key = loc.getWorld().getName() + ":" + (loc.getBlockX() >> 4) + ":" + (loc.getBlockZ() >> 4);
        Set<UUID> ids = chunks.get(key);
        if (ids == null) return null;
        for (UUID id : ids) {
            Plot plot = plots.get(id);
            if (plot != null && plot.contains(loc)) return plot;
        }
        return null;
    }

    public Plot atBlock(Block block) {
        return block == null ? null : at(block.getLocation());
    }

    public Plot atXZ(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        String key = loc.getWorld().getName() + ":" + (loc.getBlockX() >> 4) + ":" + (loc.getBlockZ() >> 4);
        Set<UUID> ids = chunks.get(key);
        if (ids == null) return null;
        for (UUID id : ids) {
            Plot plot = plots.get(id);
            if (plot != null && plot.containsXZ(loc)) return plot;
        }
        return null;
    }

    public boolean overlaps(Cuboid cuboid, UUID ignore) {
        for (String key : cuboid.chunkKeys()) {
            Set<UUID> ids = chunks.get(key);
            if (ids == null) continue;
            for (UUID id : ids) {
                if (ignore != null && ignore.equals(id)) continue;
                Plot plot = plots.get(id);
                if (plot != null && plot.cuboid().overlaps(cuboid)) return true;
            }
        }
        return false;
    }

    public Collection<Plot> all() {
        return plots.values();
    }
}
