/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import com.redglitchx.plotmanager.PlotManager;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PlotStore {
    private final PlotManager plugin;
    private final File plotsFile;
    private final File extraFile;
    public final Map<UUID, Plot> plots = new ConcurrentHashMap<>();
    public final SpatialIndex index = new SpatialIndex(plots);
    public final List<BlackmarketListing> blackmarket = new ArrayList<>();
    public String leaderboardWorld;
    public double leaderboardX, leaderboardY, leaderboardZ;
    public double mayorTaxPercent;
    public UUID mayorPlot;

    public PlotStore(PlotManager plugin) {
        this.plugin = plugin;
        this.plotsFile = new File(plugin.getDataFolder(), "plots.yml");
        this.extraFile = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        plots.clear();
        blackmarket.clear();
        if (plotsFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(plotsFile);
            ConfigurationSection root = yaml.getConfigurationSection("plots");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    try {
                        Plot plot = readPlot(root.getConfigurationSection(key));
                        if (plot != null) plots.put(plot.id, plot);
                    } catch (Exception ex) {
                        plugin.getLogger().log(Level.WARNING, "Failed to load plot " + key, ex);
                    }
                }
            }
        }
        if (extraFile.exists()) {
            YamlConfiguration extra = YamlConfiguration.loadConfiguration(extraFile);
            leaderboardWorld = extra.getString("leaderboard.world");
            leaderboardX = extra.getDouble("leaderboard.x");
            leaderboardY = extra.getDouble("leaderboard.y");
            leaderboardZ = extra.getDouble("leaderboard.z");
            mayorTaxPercent = extra.getDouble("mayor.tax", 0);
            String mayor = extra.getString("mayor.plot");
            if (mayor != null) {
                try { mayorPlot = UUID.fromString(mayor); } catch (Exception ignored) {}
            }
            ConfigurationSection bm = extra.getConfigurationSection("blackmarket");
            if (bm != null) {
                for (String key : bm.getKeys(false)) {
                    ConfigurationSection s = bm.getConfigurationSection(key);
                    if (s == null) continue;
                    BlackmarketListing l = new BlackmarketListing();
                    try { l.id = UUID.fromString(key); } catch (Exception ignored) {}
                    try { l.seller = UUID.fromString(s.getString("seller", "")); } catch (Exception ignored) {}
                    l.sellerName = s.getString("sellerName", "Unknown");
                    try {
                        String p = s.getString("plot");
                        if (p != null) l.plotId = UUID.fromString(p);
                    } catch (Exception ignored) {}
                    l.itemBase64 = s.getString("item");
                    l.price = s.getDouble("price");
                    l.created = s.getLong("created");
                    blackmarket.add(l);
                }
            }
        }
        index.rebuild();
        plugin.getLogger().info("Loaded " + plots.size() + " plots.");
    }

    public synchronized void saveSync() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Plot plot : plots.values()) {
            writePlot(yaml.createSection("plots." + plot.id), plot);
        }
        YamlConfiguration extra = new YamlConfiguration();
        if (leaderboardWorld != null) {
            extra.set("leaderboard.world", leaderboardWorld);
            extra.set("leaderboard.x", leaderboardX);
            extra.set("leaderboard.y", leaderboardY);
            extra.set("leaderboard.z", leaderboardZ);
        }
        extra.set("mayor.tax", mayorTaxPercent);
        if (mayorPlot != null) extra.set("mayor.plot", mayorPlot.toString());
        int i = 0;
        for (BlackmarketListing l : blackmarket) {
            String path = "blackmarket." + l.id;
            extra.set(path + ".seller", l.seller == null ? null : l.seller.toString());
            extra.set(path + ".sellerName", l.sellerName);
            extra.set(path + ".plot", l.plotId == null ? null : l.plotId.toString());
            extra.set(path + ".item", l.itemBase64);
            extra.set(path + ".price", l.price);
            extra.set(path + ".created", l.created);
            i++;
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yaml.save(plotsFile);
            extra.save(extraFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save plot data", e);
        }
    }

    public void saveAsync() {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this::saveSync);
    }

    public void add(Plot plot) {
        plots.put(plot.id, plot);
        index.index(plot);
    }

    public void remove(Plot plot) {
        plots.remove(plot.id);
        index.remove(plot);
    }

    public Plot get(UUID id) {
        return id == null ? null : plots.get(id);
    }

    public List<Plot> ownedBy(UUID uuid) {
        List<Plot> list = new ArrayList<>();
        for (Plot p : plots.values()) if (p.isOwner(uuid)) list.add(p);
        return list;
    }

    public List<Plot> richest(int n) {
        List<Plot> list = new ArrayList<>(plots.values());
        list.sort((a, b) -> Double.compare(b.bank, a.bank));
        if (list.size() > n) return list.subList(0, n);
        return list;
    }

    private Plot readPlot(ConfigurationSection s) {
        if (s == null) return null;
        Plot p = new Plot();
        p.id = UUID.fromString(s.getName().contains("-") ? s.getName() : s.getString("id", s.getName()));
        try { p.id = UUID.fromString(s.getString("id", s.getName())); } catch (Exception ignored) {}
        p.name = s.getString("name", "Unnamed Plot");
        try { p.owner = UUID.fromString(s.getString("owner")); } catch (Exception ignored) {}
        p.ownerName = s.getString("ownerName", "Unknown");
        p.world = s.getString("world", "world");
        p.minX = s.getInt("minX"); p.minY = s.getInt("minY"); p.minZ = s.getInt("minZ");
        p.maxX = s.getInt("maxX"); p.maxY = s.getInt("maxY"); p.maxZ = s.getInt("maxZ");
        p.description = s.getString("description", "A survival plot.");
        p.frozen = s.getBoolean("frozen");
        p.hidden = s.getBoolean("hidden");
        p.premiumTier = s.getString("premiumTier");
        p.level = s.getInt("level", 1);
        p.exp = s.getLong("exp");
        p.bank = s.getDouble("bank");
        p.vaultPages = Math.max(1, s.getInt("vaultPages", 1));
        p.vaultData.clear();
        p.vaultData.addAll(s.getStringList("vault"));
        p.musicDisc = s.getString("musicDisc");
        p.flyUnlocked = s.getBoolean("flyUnlocked");
        p.cropBoost = s.getBoolean("cropBoost");
        p.musicUnlocked = s.getBoolean("musicUnlocked");
        p.sorterUnlocked = s.getBoolean("sorterUnlocked");
        p.factoryUnlocked = s.getBoolean("factoryUnlocked");
        p.borderCosmetic = s.getString("borderCosmetic");
        p.particleCosmetic = s.getString("particleCosmetic");
        p.mailboxWorld = s.getString("mailbox.world");
        p.mailboxX = s.getInt("mailbox.x");
        p.mailboxY = s.getInt("mailbox.y");
        p.mailboxZ = s.getInt("mailbox.z");
        p.mailboxHolo = s.getBoolean("mailbox.holo");
        p.homeWorld = s.getString("home.world");
        p.homeX = s.getDouble("home.x");
        p.homeY = s.getDouble("home.y");
        p.homeZ = s.getDouble("home.z");
        p.homeYaw = s.getDouble("home.yaw");
        p.homePitch = s.getDouble("home.pitch");
        p.holoWorld = s.getString("holo.world");
        p.holoX = s.getDouble("holo.x");
        p.holoY = s.getDouble("holo.y");
        p.holoZ = s.getDouble("holo.z");
        p.hoppers = s.getInt("hoppers");
        p.spawners = s.getInt("spawners");
        p.created = s.getLong("created", System.currentTimeMillis());
        p.lastOwnerLogin = s.getLong("lastOwnerLogin", System.currentTimeMillis());
        p.lastUpkeep = s.getLong("lastUpkeep", System.currentTimeMillis());
        p.visitorsOffline = s.getInt("offline.visitors");
        p.tipsOffline = s.getDouble("offline.tips");
        p.shopSalesOffline = s.getDouble("offline.shops");
        p.generatorItemsOffline = s.getLong("offline.generators");
        p.blackmarketUsed = s.getBoolean("blackmarketUsed");
        p.claimCostPaid = s.getDouble("claimCostPaid");
        p.schematicFile = s.getString("schematic");
        p.banned.clear();
        for (String b : s.getStringList("banned")) {
            try { p.banned.add(UUID.fromString(b)); } catch (Exception ignored) {}
        }
        ConfigurationSection mem = s.getConfigurationSection("members");
        if (mem != null) {
            for (String k : mem.getKeys(false)) {
                try {
                    UUID id = UUID.fromString(k);
                    PlotMember m = new PlotMember();
                    m.uuid = id;
                    m.name = mem.getString(k + ".name", "Unknown");
                    m.role = PlotRole.from(mem.getString(k + ".role", "VISITOR"));
                    m.canBuild = mem.getBoolean(k + ".canBuild");
                    m.canChests = mem.getBoolean(k + ".canChests");
                    m.canInteract = mem.getBoolean(k + ".canInteract", true);
                    m.addedAt = mem.getLong(k + ".addedAt", System.currentTimeMillis());
                    p.members.put(id, m);
                } catch (Exception ignored) {}
            }
        }
        ConfigurationSection fl = s.getConfigurationSection("flags");
        if (fl != null) {
            for (String k : fl.getKeys(false)) {
                PlotFlag flag = PlotFlag.from(k);
                if (flag != null) p.flags.put(flag, fl.getBoolean(k));
            }
        }
        List<Map<?, ?>> gens = s.getMapList("generators");
        for (Map<?, ?> g : gens) {
            GeneratorInstance gi = new GeneratorInstance();
            Object t = g.get("tier");
            if (t instanceof Number n) gi.tier = n.intValue();
            Object lt = g.get("lastTick");
            if (lt instanceof Number n) gi.lastTick = n.longValue();
            p.generators.add(gi);
        }
        ConfigurationSection shops = s.getConfigurationSection("shops");
        if (shops != null) {
            for (String k : shops.getKeys(false)) {
                ChestShop cs = new ChestShop();
                cs.world = shops.getString(k + ".world");
                cs.x = shops.getInt(k + ".x");
                cs.y = shops.getInt(k + ".y");
                cs.z = shops.getInt(k + ".z");
                try { cs.item = org.bukkit.Material.valueOf(shops.getString(k + ".item", "STONE")); } catch (Exception ignored) {}
                cs.amount = shops.getInt(k + ".amount", 1);
                cs.price = shops.getDouble(k + ".price");
                p.shops.add(cs);
            }
        }
        ConfigurationSection holos = s.getConfigurationSection("holograms");
        if (holos != null) {
            for (String k : holos.getKeys(false)) {
                CustomHologram h = new CustomHologram();
                try { h.id = UUID.fromString(k); } catch (Exception ignored) {}
                h.world = holos.getString(k + ".world");
                h.x = holos.getDouble(k + ".x");
                h.y = holos.getDouble(k + ".y");
                h.z = holos.getDouble(k + ".z");
                h.lines.addAll(holos.getStringList(k + ".lines"));
                p.holograms.add(h);
            }
        }
        return p;
    }

    private void writePlot(ConfigurationSection s, Plot p) {
        s.set("id", p.id.toString());
        s.set("name", p.name);
        s.set("owner", p.owner == null ? null : p.owner.toString());
        s.set("ownerName", p.ownerName);
        s.set("world", p.world);
        s.set("minX", p.minX); s.set("minY", p.minY); s.set("minZ", p.minZ);
        s.set("maxX", p.maxX); s.set("maxY", p.maxY); s.set("maxZ", p.maxZ);
        s.set("description", p.description);
        s.set("frozen", p.frozen);
        s.set("hidden", p.hidden);
        s.set("premiumTier", p.premiumTier);
        s.set("level", p.level);
        s.set("exp", p.exp);
        s.set("bank", p.bank);
        s.set("vaultPages", p.vaultPages);
        s.set("vault", new ArrayList<>(p.vaultData));
        s.set("musicDisc", p.musicDisc);
        s.set("flyUnlocked", p.flyUnlocked);
        s.set("cropBoost", p.cropBoost);
        s.set("musicUnlocked", p.musicUnlocked);
        s.set("sorterUnlocked", p.sorterUnlocked);
        s.set("factoryUnlocked", p.factoryUnlocked);
        s.set("borderCosmetic", p.borderCosmetic);
        s.set("particleCosmetic", p.particleCosmetic);
        s.set("mailbox.world", p.mailboxWorld);
        s.set("mailbox.x", p.mailboxX);
        s.set("mailbox.y", p.mailboxY);
        s.set("mailbox.z", p.mailboxZ);
        s.set("mailbox.holo", p.mailboxHolo);
        s.set("home.world", p.homeWorld);
        s.set("home.x", p.homeX);
        s.set("home.y", p.homeY);
        s.set("home.z", p.homeZ);
        s.set("home.yaw", p.homeYaw);
        s.set("home.pitch", p.homePitch);
        s.set("holo.world", p.holoWorld);
        s.set("holo.x", p.holoX);
        s.set("holo.y", p.holoY);
        s.set("holo.z", p.holoZ);
        s.set("hoppers", p.hoppers);
        s.set("spawners", p.spawners);
        s.set("created", p.created);
        s.set("lastOwnerLogin", p.lastOwnerLogin);
        s.set("lastUpkeep", p.lastUpkeep);
        s.set("offline.visitors", p.visitorsOffline);
        s.set("offline.tips", p.tipsOffline);
        s.set("offline.shops", p.shopSalesOffline);
        s.set("offline.generators", p.generatorItemsOffline);
        s.set("blackmarketUsed", p.blackmarketUsed);
        s.set("claimCostPaid", p.claimCostPaid);
        s.set("schematic", p.schematicFile);
        List<String> banned = new ArrayList<>();
        for (UUID u : p.banned) banned.add(u.toString());
        s.set("banned", banned);
        for (PlotMember m : p.members.values()) {
            String path = "members." + m.uuid;
            s.set(path + ".name", m.name);
            s.set(path + ".role", m.role.name());
            s.set(path + ".canBuild", m.canBuild);
            s.set(path + ".canChests", m.canChests);
            s.set(path + ".canInteract", m.canInteract);
            s.set(path + ".addedAt", m.addedAt);
        }
        for (var e : p.flags.entrySet()) {
            s.set("flags." + e.getKey().name(), e.getValue());
        }
        List<Map<String, Object>> gens = new ArrayList<>();
        for (GeneratorInstance g : p.generators) {
            gens.add(Map.of("tier", g.tier, "lastTick", g.lastTick));
        }
        s.set("generators", gens);
        int i = 0;
        for (ChestShop cs : p.shops) {
            String path = "shops." + i++;
            s.set(path + ".world", cs.world);
            s.set(path + ".x", cs.x);
            s.set(path + ".y", cs.y);
            s.set(path + ".z", cs.z);
            s.set(path + ".item", cs.item.name());
            s.set(path + ".amount", cs.amount);
            s.set(path + ".price", cs.price);
        }
        for (CustomHologram h : p.holograms) {
            String path = "holograms." + h.id;
            s.set(path + ".world", h.world);
            s.set(path + ".x", h.x);
            s.set(path + ".y", h.y);
            s.set(path + ".z", h.z);
            s.set(path + ".lines", h.lines);
        }
    }
}
