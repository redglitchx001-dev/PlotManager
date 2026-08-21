package com.redglitchx.plotmanager.integration;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import org.bukkit.Bukkit;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.logging.Level;

public class BlueMapHook {
    private final PlotManager plugin;
    private boolean available;

    public BlueMapHook(PlotManager plugin) {
        this.plugin = plugin;
        available = Bukkit.getPluginManager().getPlugin("BlueMap") != null;
        if (available) {
            try {
                Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
            } catch (ClassNotFoundException e) {
                available = false;
            }
        }
    }

    public boolean available() { return available; }

    public String status() { return available ? "ONLINE" : "NOT INSTALLED"; }

    public void start() {
        if (!available || !plugin.cfg().getBoolean("bluemap.enabled", true)) return;
        try {
            Class<?> api = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
            Method onEnable = api.getMethod("onEnable", Consumer.class);
            onEnable.invoke(null, (Consumer<Object>) this::refreshAll);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "BlueMap hook failed", t);
        }
    }

    public void refreshAll(Object api) {
        for (Plot plot : plugin.store.plots.values()) upsert(plot);
    }

    public void upsert(Plot plot) {
        if (!available || !plugin.cfg().getBoolean("bluemap.enabled", true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                doUpsert(plot);
            } catch (Throwable t) {
                if (plugin.cfg().getBoolean("plugin.debug_mode")) {
                    plugin.getLogger().log(Level.FINE, "BlueMap marker update failed", t);
                }
            }
        });
    }

    public void remove(Plot plot) {
        if (!available) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                doRemove(plot);
            } catch (Throwable ignored) {
            }
        });
    }

    private void doUpsert(Plot plot) throws Exception {
        Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
        Object opt = apiClass.getMethod("getInstance").invoke(null);
        if (!(Boolean) opt.getClass().getMethod("isPresent").invoke(opt)) return;
        Object api = opt.getClass().getMethod("get").invoke(opt);
        Object maps = api.getClass().getMethod("getMaps").invoke(api);
        Iterable<?> iterable = (Iterable<?>) maps;
        String id = "plotmanager-" + plot.id;
        String label = plugin.cfg().getString("bluemap.label_format", "Plot: %plot_name%")
                .replace("%plot_name%", plot.name)
                .replace("%owner%", plot.ownerName)
                .replace("%level%", String.valueOf(plot.level))
                .replace("%members%", String.valueOf(plot.memberCount()))
                .replace("%max_members%", String.valueOf(plugin.cfg().getInt("members.max_members_default", 5)))
                .replace("%bank%", String.valueOf((long) plot.bank));
        String hex = plot.frozen ? plugin.cfg().getString("bluemap.border_color_frozen", "#FF0000")
                : (plot.premiumTier != null ? plugin.cfg().getString("bluemap.border_color_premium", "#FFD700")
                : plugin.cfg().getString("bluemap.border_color", "#00FF00"));
        Color color = Color.decode(hex.startsWith("#") ? hex : "#" + hex);
        float opacity = (float) plugin.cfg().getDouble("bluemap.border_opacity", 0.7);
        for (Object map : iterable) {
            Object worldOpt = map.getClass().getMethod("getWorld").invoke(map);
            Object world = worldOpt;
            try {
                if (worldOpt.getClass().getMethod("isPresent").invoke(worldOpt) instanceof Boolean b && b) {
                    world = worldOpt.getClass().getMethod("get").invoke(worldOpt);
                }
            } catch (Exception ignored) {}
            String worldId;
            try {
                worldId = String.valueOf(world.getClass().getMethod("getId").invoke(world));
            } catch (Exception e) {
                try { worldId = String.valueOf(world.getClass().getMethod("getName").invoke(world)); }
                catch (Exception e2) { continue; }
            }
            if (!worldId.contains(plot.world) && !plot.world.contains(worldId)) continue;
            Object markerApi = map.getClass().getMethod("getMarkerAPI").invoke(map);
            Object set = markerApi.getClass().getMethod("getOrCreateMarkerSet", String.class).invoke(markerApi, "plotmanager");
            try { set.getClass().getMethod("setLabel", String.class).invoke(set, "PlotManager"); } catch (Exception ignored) {}
            Class<?> vector = Class.forName("de.bluecolored.bluemap.api.math.Vector3d");
            Object v1 = vector.getConstructor(double.class, double.class, double.class).newInstance((double) plot.minX, (double) plot.minY, (double) plot.minZ);
            Object v2 = vector.getConstructor(double.class, double.class, double.class).newInstance((double) plot.maxX + 1, (double) plot.maxY + 1, (double) plot.maxZ + 1);
            Object shape;
            try {
                Class<?> box = Class.forName("de.bluecolored.bluemap.api.markers.ExtrudeMarker");
                Method builder = box.getMethod("builder");
                Object b = builder.invoke(null);
                try { b.getClass().getMethod("position", vector).invoke(b, v1); } catch (Exception ignored) {}
                try { b.getClass().getMethod("label", String.class).invoke(b, label); } catch (Exception ignored) {}
                try {
                    Class<?> lineColor = Class.forName("de.bluecolored.bluemap.api.math.Color");
                    Object c = lineColor.getConstructor(int.class, int.class, int.class, float.class)
                            .newInstance(color.getRed(), color.getGreen(), color.getBlue(), opacity);
                    try { b.getClass().getMethod("lineColor", lineColor).invoke(b, c); } catch (Exception ignored) {}
                    try { b.getClass().getMethod("fillColor", lineColor).invoke(b, c); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
                shape = b.getClass().getMethod("build").invoke(b);
            } catch (Exception e) {
                Class<?> poi = Class.forName("de.bluecolored.bluemap.api.markers.POIMarker");
                Object b = poi.getMethod("builder").invoke(null);
                b.getClass().getMethod("label", String.class).invoke(b, label);
                try { b.getClass().getMethod("position", vector).invoke(b, v1); } catch (Exception ignored) {}
                shape = b.getClass().getMethod("build").invoke(b);
            }
            set.getClass().getMethod("put", String.class, Class.forName("de.bluecolored.bluemap.api.markers.Marker"))
                    .invoke(set, id, shape);
            try { markerApi.getClass().getMethod("save").invoke(markerApi); } catch (Exception ignored) {}
        }
    }

    private void doRemove(Plot plot) throws Exception {
        Class<?> apiClass = Class.forName("de.bluecolored.bluemap.api.BlueMapAPI");
        Object opt = apiClass.getMethod("getInstance").invoke(null);
        if (!(Boolean) opt.getClass().getMethod("isPresent").invoke(opt)) return;
        Object api = opt.getClass().getMethod("get").invoke(opt);
        Iterable<?> maps = (Iterable<?>) api.getClass().getMethod("getMaps").invoke(api);
        String id = "plotmanager-" + plot.id;
        for (Object map : maps) {
            Object markerApi = map.getClass().getMethod("getMarkerAPI").invoke(map);
            Object setOpt;
            try {
                setOpt = markerApi.getClass().getMethod("getMarkerSet", String.class).invoke(markerApi, "plotmanager");
                Object set = setOpt;
                if (setOpt.getClass().getName().contains("Optional")) {
                    if (!(Boolean) setOpt.getClass().getMethod("isPresent").invoke(setOpt)) continue;
                    set = setOpt.getClass().getMethod("get").invoke(setOpt);
                }
                set.getClass().getMethod("remove", String.class).invoke(set, id);
            } catch (Exception ignored) {}
        }
    }
}
