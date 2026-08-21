/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.util;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Cuboid {
    public final String world;
    public final int minX, minY, minZ, maxX, maxY, maxZ;

    public Cuboid(String world, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.world = world;
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public static Cuboid of(Location a, Location b) {
        return new Cuboid(a.getWorld().getName(), a.getBlockX(), a.getBlockY(), a.getBlockZ(),
                b.getBlockX(), b.getBlockY(), b.getBlockZ());
    }

    public boolean contains(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean containsBlock(int x, int y, int z, String worldName) {
        if (!Objects.equals(world, worldName)) return false;
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean containsXZ(Location loc) {
        if (loc == null || loc.getWorld() == null) return false;
        if (!loc.getWorld().getName().equals(world)) return false;
        int x = loc.getBlockX();
        int z = loc.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean overlaps(Cuboid other) {
        if (other == null || !Objects.equals(world, other.world)) return false;
        return minX <= other.maxX && maxX >= other.minX
                && minY <= other.maxY && maxY >= other.minY
                && minZ <= other.maxZ && maxZ >= other.minZ;
    }

    public boolean adjacent(Cuboid other) {
        if (other == null || !Objects.equals(world, other.world)) return false;
        boolean xTouch = maxX + 1 == other.minX || other.maxX + 1 == minX;
        boolean zTouch = maxZ + 1 == other.minZ || other.maxZ + 1 == minZ;
        boolean xOverlap = minX <= other.maxX && maxX >= other.minX;
        boolean zOverlap = minZ <= other.maxZ && maxZ >= other.minZ;
        boolean yOverlap = minY <= other.maxY && maxY >= other.minY;
        return yOverlap && ((xTouch && zOverlap) || (zTouch && xOverlap) || overlaps(other));
    }

    public Cuboid union(Cuboid other) {
        return new Cuboid(world,
                Math.min(minX, other.minX), Math.min(minY, other.minY), Math.min(minZ, other.minZ),
                Math.max(maxX, other.maxX), Math.max(maxY, other.maxY), Math.max(maxZ, other.maxZ));
    }

    public int sizeX() { return maxX - minX + 1; }
    public int sizeY() { return maxY - minY + 1; }
    public int sizeZ() { return maxZ - minZ + 1; }
    public long volume() { return (long) sizeX() * sizeY() * sizeZ(); }
    public int footprint() { return sizeX() * sizeZ(); }

    public Location center(World w) {
        return new Location(w, (minX + maxX) / 2.0 + 0.5, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0 + 0.5);
    }

    public Location centerTop(World w) {
        return new Location(w, (minX + maxX) / 2.0 + 0.5, maxY + 1.0, (minZ + maxZ) / 2.0 + 0.5);
    }

    public List<String> chunkKeys() {
        List<String> keys = new ArrayList<>();
        int cx1 = minX >> 4;
        int cz1 = minZ >> 4;
        int cx2 = maxX >> 4;
        int cz2 = maxZ >> 4;
        for (int cx = cx1; cx <= cx2; cx++) {
            for (int cz = cz1; cz <= cz2; cz++) {
                keys.add(world + ":" + cx + ":" + cz);
            }
        }
        return keys;
    }

    public Vector outward(Location from) {
        double cx = (minX + maxX) / 2.0 + 0.5;
        double cz = (minZ + maxZ) / 2.0 + 0.5;
        Vector v = new Vector(from.getX() - cx, 0.35, from.getZ() - cz);
        if (v.lengthSquared() < 0.01) v = new Vector(0.5, 0.4, 0.5);
        return v.normalize();
    }

    public Location nearestOutside(Location from, double extra) {
        double x = from.getX();
        double z = from.getZ();
        double dxMin = Math.abs(x - minX);
        double dxMax = Math.abs(x - (maxX + 1));
        double dzMin = Math.abs(z - minZ);
        double dzMax = Math.abs(z - (maxZ + 1));
        double best = Math.min(Math.min(dxMin, dxMax), Math.min(dzMin, dzMax));
        Location loc = from.clone();
        if (best == dxMin) loc.setX(minX - extra);
        else if (best == dxMax) loc.setX(maxX + 1 + extra);
        else if (best == dzMin) loc.setZ(minZ - extra);
        else loc.setZ(maxZ + 1 + extra);
        return loc;
    }

    public boolean isBorder(Block block, int thickness) {
        if (block == null) return false;
        int x = block.getX();
        int z = block.getZ();
        if (!block.getWorld().getName().equals(world)) return false;
        boolean onX = x <= minX + thickness - 1 || x >= maxX - thickness + 1;
        boolean onZ = z <= minZ + thickness - 1 || z >= maxZ - thickness + 1;
        return (onX || onZ) && x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }
}
