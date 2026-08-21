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
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * Optional FAWE / WorldEdit schematic save+paste via reflection.
 * Plugin compiles and runs without FAWE; rollback is skipped if missing.
 */
public class FaweHook {
    private final PlotManager plugin;
    private boolean available;

    public FaweHook(PlotManager plugin) {
        this.plugin = plugin;
        available = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null
                || Bukkit.getPluginManager().getPlugin("WorldEdit") != null;
        if (available) {
            try {
                Class.forName("com.sk89q.worldedit.WorldEdit");
            } catch (ClassNotFoundException e) {
                available = false;
            }
        }
    }

    public boolean available() {
        return available;
    }

    public String status() {
        return available ? "ONLINE" : "NOT INSTALLED";
    }

    public void saveSchematic(Plot plot) {
        if (!available || !plugin.cfg().getBoolean("reset_system.paste_original_schematic", true)) return;
        File dir = new File(plugin.getDataFolder(), "schematics");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, plot.id + ".schem");
        plot.schematicFile = file.getAbsolutePath();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                doSave(plot, file);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "FAWE schematic save failed for " + plot.id, t);
            }
        });
    }

    public void pasteSchematic(Plot plot, Runnable after) {
        if (!available || plot.schematicFile == null) {
            if (after != null) Bukkit.getScheduler().runTask(plugin, after);
            return;
        }
        File file = new File(plot.schematicFile);
        if (!file.exists()) {
            if (after != null) Bukkit.getScheduler().runTask(plugin, after);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                doPaste(plot, file);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "FAWE schematic paste failed for " + plot.id, t);
            }
            if (after != null) Bukkit.getScheduler().runTask(plugin, after);
        });
    }

    private void doSave(Plot plot, File file) throws Exception {
        World bw = plot.bukkitWorld();
        if (bw == null) return;
        Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        Object we = weClass.getMethod("getInstance").invoke(null);
        Class<?> bua = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object weWorld = bua.getMethod("adapt", World.class).invoke(null, bw);
        Class<?> bv = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        Method at = bv.getMethod("at", int.class, int.class, int.class);
        Object min = at.invoke(null, plot.minX, plot.minY, plot.minZ);
        Object max = at.invoke(null, plot.maxX, plot.maxY, plot.maxZ);
        Class<?> cuboid = Class.forName("com.sk89q.worldedit.regions.CuboidRegion");
        Constructor<?> cuboidCtor = cuboid.getConstructor(Class.forName("com.sk89q.worldedit.world.World"), bv, bv);
        Object region = cuboidCtor.newInstance(weWorld, min, max);
        Class<?> clipboardClass = Class.forName("com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard");
        Object clipboard = clipboardClass.getConstructor(Class.forName("com.sk89q.worldedit.regions.Region")).newInstance(region);
        Class<?> editSessionClass = Class.forName("com.sk89q.worldedit.EditSession");
        Object newEs = weClass.getMethod("newEditSession", Class.forName("com.sk89q.worldedit.world.World")).invoke(we, weWorld);
        try {
            Class<?> fec = Class.forName("com.sk89q.worldedit.function.operation.ForwardExtentCopy");
            Object copy = fec.getConstructor(Class.forName("com.sk89q.worldedit.extent.Extent"),
                    Class.forName("com.sk89q.worldedit.regions.Region"),
                    Class.forName("com.sk89q.worldedit.extent.Extent"),
                    bv).newInstance(newEs, region, clipboard, min);
            Class<?> ops = Class.forName("com.sk89q.worldedit.function.operation.Operations");
            ops.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation")).invoke(null, copy);
        } finally {
            try { editSessionClass.getMethod("close").invoke(newEs); } catch (Exception ignored) {}
        }
        Class<?> formats = Class.forName("com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat");
        Object format;
        try {
            format = Enum.valueOf((Class<Enum>) formats, "SPONGE_V3_SCHEMATIC");
        } catch (Exception e) {
            try {
                format = Enum.valueOf((Class<Enum>) formats, "SPONGE_SCHEMATIC");
            } catch (Exception e2) {
                format = formats.getEnumConstants()[0];
            }
        }
        Method getWriter = format.getClass().getMethod("getWriter", java.io.OutputStream.class);
        try (java.io.OutputStream os = new java.io.FileOutputStream(file)) {
            Object writer = getWriter.invoke(format, os);
            writer.getClass().getMethod("write", Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard")).invoke(writer, clipboard);
            writer.getClass().getMethod("close").invoke(writer);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void doPaste(Plot plot, File file) throws Exception {
        World bw = plot.bukkitWorld();
        if (bw == null) return;
        Class<?> weClass = Class.forName("com.sk89q.worldedit.WorldEdit");
        Object we = weClass.getMethod("getInstance").invoke(null);
        Class<?> bua = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
        Object weWorld = bua.getMethod("adapt", World.class).invoke(null, bw);
        Class<?> formats = Class.forName("com.sk89q.worldedit.extent.clipboard.io.BuiltInClipboardFormat");
        Object format;
        try {
            format = Enum.valueOf((Class<Enum>) formats, "SPONGE_V3_SCHEMATIC");
        } catch (Exception e) {
            try {
                format = Enum.valueOf((Class<Enum>) formats, "SPONGE_SCHEMATIC");
            } catch (Exception e2) {
                format = formats.getEnumConstants()[0];
            }
        }
        Object clipboard;
        try (java.io.InputStream is = new java.io.FileInputStream(file)) {
            Object reader = format.getClass().getMethod("getReader", java.io.InputStream.class).invoke(format, is);
            clipboard = reader.getClass().getMethod("read").invoke(reader);
            try { reader.getClass().getMethod("close").invoke(reader); } catch (Exception ignored) {}
        }
        Class<?> bv = Class.forName("com.sk89q.worldedit.math.BlockVector3");
        Object to = bv.getMethod("at", int.class, int.class, int.class).invoke(null, plot.minX, plot.minY, plot.minZ);
        Object es = weClass.getMethod("newEditSession", Class.forName("com.sk89q.worldedit.world.World")).invoke(we, weWorld);
        try {
            Class<?> opClass = Class.forName("com.sk89q.worldedit.session.ClipboardHolder");
            Object holder = opClass.getConstructor(Class.forName("com.sk89q.worldedit.extent.clipboard.Clipboard")).newInstance(clipboard);
            Object paste = holder.getClass().getMethod("createPaste", Class.forName("com.sk89q.worldedit.extent.Extent")).invoke(holder, es);
            paste.getClass().getMethod("to", bv).invoke(paste, to);
            try { paste.getClass().getMethod("ignoreAirBlocks", boolean.class).invoke(paste, false); } catch (Exception ignored) {}
            Object operation = paste.getClass().getMethod("build").invoke(paste);
            Class<?> ops = Class.forName("com.sk89q.worldedit.function.operation.Operations");
            ops.getMethod("complete", Class.forName("com.sk89q.worldedit.function.operation.Operation")).invoke(null, operation);
            try { es.getClass().getMethod("flushQueue").invoke(es); } catch (Exception ignored) {}
        } finally {
            try { es.getClass().getMethod("close").invoke(es); } catch (Exception ignored) {}
        }
        Location ignored = new Location(bw, plot.minX, plot.minY, plot.minZ);
        ignored.getBlock(); // keep world loaded
    }
}
