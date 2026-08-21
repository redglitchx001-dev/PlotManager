package com.redglitchx.plotmanager;

import com.redglitchx.plotmanager.command.PlotCommand;
import com.redglitchx.plotmanager.data.BlackmarketListing;
import com.redglitchx.plotmanager.data.ChestShop;
import com.redglitchx.plotmanager.data.CustomHologram;
import com.redglitchx.plotmanager.data.GeneratorInstance;
import com.redglitchx.plotmanager.data.PlayerSession;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotStore;
import com.redglitchx.plotmanager.gui.Menus;
import com.redglitchx.plotmanager.hologram.HologramEngine;
import com.redglitchx.plotmanager.integration.BlueMapHook;
import com.redglitchx.plotmanager.integration.DiscordBot;
import com.redglitchx.plotmanager.integration.FaweHook;
import com.redglitchx.plotmanager.integration.LuckPermsHook;
import com.redglitchx.plotmanager.integration.PapiHook;
import com.redglitchx.plotmanager.integration.VoiceHook;
import com.redglitchx.plotmanager.listener.GuiListener;
import com.redglitchx.plotmanager.listener.MechanicListener;
import com.redglitchx.plotmanager.listener.MoveListener;
import com.redglitchx.plotmanager.listener.PlayerListener;
import com.redglitchx.plotmanager.listener.ProtectionListener;
import com.redglitchx.plotmanager.service.EconomyService;
import com.redglitchx.plotmanager.task.PluginTasks;
import com.redglitchx.plotmanager.util.Cuboid;
import com.redglitchx.plotmanager.util.FX;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Keys;
import com.redglitchx.plotmanager.util.Serial;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Container;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class PlotManager extends JavaPlugin {
    public PlotStore store;
    public EconomyService economy;
    public HologramEngine holograms;
    public Menus menus;
    public Keys keys;
    public LuckPermsHook luckPerms;
    public DiscordBot discord;
    public FaweHook fawe;
    public BlueMapHook blueMap;
    public PapiHook papi;
    public VoiceHook voice;
    public final Map<UUID, PlayerSession> sessions = new ConcurrentHashMap<>();
    public final Map<UUID, Long> combat = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        keys = new Keys(this);
        store = new PlotStore(this);
        economy = new EconomyService(this);
        if (!economy.setup()) {
            getLogger().severe("Vault economy not found! PlotManager requires Vault + an economy plugin.");
        }
        luckPerms = new LuckPermsHook(this);
        discord = new DiscordBot(this);
        fawe = new FaweHook(this);
        blueMap = new BlueMapHook(this);
        papi = new PapiHook(this);
        voice = new VoiceHook(this);
        holograms = new HologramEngine(this);
        menus = new Menus(this);

        store.load();
        discord.start();
        blueMap.start();

        PlotCommand cmd = new PlotCommand(this);
        var plot = getCommand("plot");
        if (plot != null) {
            plot.setExecutor(cmd);
            plot.setTabCompleter(cmd);
        }

        Bukkit.getPluginManager().registerEvents(new ProtectionListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MoveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MechanicListener(this), this);

        holograms.start();
        PluginTasks.start(this);
        printBanner();
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerSession s = sessions.get(player.getUniqueId());
            if (s != null && s.drone) disableDrone(player, false);
        }
        holograms.shutdown();
        discord.shutdown();
        store.saveSync();
    }

    public FileConfiguration cfg() {
        return getConfig();
    }

    public String prefix() {
        return cfg().getString("plugin.prefix", "&2&l[PlotManager] &a");
    }

    public void msg(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        Text.sendPrefixed(sender, prefix(), message);
    }

    public void fx(Player player, String key) {
        FX.play(player, cfg().getString("sounds." + key, "UI_BUTTON_CLICK"));
    }

    public boolean bypass(Player player) {
        if (player == null) return false;
        if (player.hasPermission("plotmanager.bypass") || player.hasPermission("plotmanager.admin")) return true;
        PlayerSession s = sessions.get(player.getUniqueId());
        return s != null && s.adminSpy;
    }

    public boolean protectedWorld(World world) {
        if (world == null) return false;
        String name = cfg().getString("world_settings.protected_world", "world");
        return world.getName().equalsIgnoreCase(name) || name.equalsIgnoreCase("*") || name.equalsIgnoreCase("all");
    }

    public PlayerSession session(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), PlayerSession::new);
    }

    public Plot here(Player player) {
        return store.index.at(player.getLocation());
    }

    public String placeholders(Player player, String text, Plot plot) {
        if (text == null) return "";
        String out = text;
        if (plot != null) {
            out = out.replace("%plot_name%", plot.name)
                    .replace("%plotmanager_current_plot_owner%", plot.ownerName)
                    .replace("%plotmanager_current_plot_level%", String.valueOf(plot.level))
                    .replace("%plotmanager_current_plot_exp%", String.valueOf(plot.exp))
                    .replace("%plotmanager_current_plot_bank%", Text.money(plot.bank))
                    .replace("%plotmanager_current_plot_members%", String.valueOf(plot.memberCount()))
                    .replace("%plotmanager_current_plot_members_max%", String.valueOf(cfg().getInt("members.max_members_default", 5)))
                    .replace("%plotmanager_current_plot_created%", Text.formatDate(plot.created, cfg().getString("placeholders.date_format"), cfg().getString("plugin.timezone")))
                    .replace("%plotmanager_current_plot_description%", plot.description == null ? "" : plot.description)
                    .replace("%plotmanager_current_plot_generators%", String.valueOf(plot.generators.size()))
                    .replace("%owner%", plot.ownerName)
                    .replace("%level%", String.valueOf(plot.level))
                    .replace("%members%", String.valueOf(plot.memberCount()))
                    .replace("%max_members%", String.valueOf(cfg().getInt("members.max_members_default", 5)))
                    .replace("%size_x%", String.valueOf(plot.cuboid().sizeX()))
                    .replace("%size_z%", String.valueOf(plot.cuboid().sizeZ()))
                    .replace("%claim_cost%", Text.money(cfg().getDouble("economy.claim_cost")));
        }
        out = papi.apply(player, out);
        if (player != null) out = out.replace("%player%", player.getName());
        return out;
    }

    public String placeholder(Player player, String params) {
        return PapiHook.builtin(this, player, params);
    }

    public Plot createPlot(Player player, Cuboid cuboid, String tier, double cost) {
        Plot plot = new Plot();
        plot.owner = player.getUniqueId();
        plot.ownerName = player.getName();
        plot.name = player.getName() + "'s Plot";
        plot.applyCuboid(cuboid);
        plot.premiumTier = tier;
        plot.claimCostPaid = cost;
        plot.setHome(player.getLocation());
        plot.setHologram(cuboid.centerTop(player.getWorld()).add(0, cfg().getDouble("auto_hologram.height_above_center", 3) - 1, 0));
        store.add(plot);
        fawe.saveSchematic(plot);
        holograms.spawnPlot(plot);
        blueMap.upsert(plot);
        plot.audit(player.getName(), "CLAIM", cuboid.sizeX() + "x" + cuboid.sizeZ() + " for $" + Text.money(cost));
        store.saveAsync();
        return plot;
    }

    public void unclaim(Player player, Plot plot, boolean confirm) {
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("plotmanager.admin")) {
            msg(player, "&cOnly the owner can unclaim.");
            return;
        }
        if (!confirm) {
            session(player).confirmUnclaim = plot.id;
            msg(player, cfg().getString("claiming.unclaim_confirm_message"));
            return;
        }
        double refund = plot.claimCostPaid * (cfg().getDouble("economy.unclaim_refund_percent", 50) / 100.0);
        economy.deposit(player, refund);
        deletePlot(plot, true);
        msg(player, cfg().getString("claiming.unclaim_success_message").replace("%amount%", Text.money(refund)));
        fx(player, "unclaim_success");
    }

    public void deletePlot(Plot plot, boolean restore) {
        holograms.despawnPlot(plot);
        blueMap.remove(plot);
        Runnable finish = () -> {
            store.remove(plot);
            store.saveAsync();
        };
        if (restore && cfg().getBoolean("reset_system.paste_original_schematic", true)) {
            fawe.pasteSchematic(plot, finish);
        } else {
            finish.run();
        }
    }

    public void mergePlots(Player player, Plot a, Plot b) {
        Cuboid merged = a.cuboid().union(b.cuboid());
        store.index.remove(a);
        a.applyCuboid(merged);
        a.bank += b.bank;
        a.exp += b.exp;
        a.level = Math.max(a.level, b.level);
        a.vaultPages = Math.max(a.vaultPages, b.vaultPages);
        a.vaultData.addAll(b.vaultData);
        a.generators.addAll(b.generators);
        a.members.putAll(b.members);
        a.shops.addAll(b.shops);
        a.flyUnlocked |= b.flyUnlocked;
        a.cropBoost |= b.cropBoost;
        a.musicUnlocked |= b.musicUnlocked;
        a.factoryUnlocked |= b.factoryUnlocked;
        a.sorterUnlocked |= b.sorterUnlocked;
        holograms.despawnPlot(b);
        blueMap.remove(b);
        store.remove(b);
        store.index.index(a);
        holograms.spawnPlot(a);
        blueMap.upsert(a);
        a.audit(player.getName(), "MERGE", "Merged adjacent plot");
        fawe.saveSchematic(a);
    }

    public Plot findByOwnerName(String name) {
        for (Plot p : store.plots.values()) {
            if (p.ownerName != null && p.ownerName.equalsIgnoreCase(name) && !p.hidden) return p;
        }
        Player t = Bukkit.getPlayerExact(name);
        if (t != null) {
            List<Plot> owned = store.ownedBy(t.getUniqueId());
            return owned.isEmpty() ? null : owned.get(0);
        }
        return null;
    }

    public void sendInfo(Player player, Plot plot) {
        if (plot == null) {
            msg(player, "&7You are in the wilderness.");
            return;
        }
        Map<String, String> ph = Map.of(
                "name", plot.name,
                "owner", plot.ownerName,
                "level", String.valueOf(plot.level),
                "exp", String.valueOf(plot.exp),
                "bank", Text.money(plot.bank),
                "members", String.valueOf(plot.memberCount()),
                "max_members", String.valueOf(cfg().getInt("members.max_members_default", 5)),
                "size_x", String.valueOf(plot.cuboid().sizeX()),
                "size_z", String.valueOf(plot.cuboid().sizeZ()),
                "date", Text.formatDate(plot.created, cfg().getString("plugin.date_format"), cfg().getString("plugin.timezone"))
        );
        String[] keys = {"header", "name", "owner", "level", "bank", "members", "size", "created", "description", "status", "flags", "generators", "vault_tier", "footer"};
        for (String k : keys) {
            String line = cfg().getString("plot_info." + k, "");
            line = line.replace("%description%", plot.description == null ? "" : plot.description)
                    .replace("%status%", cfg().getString("placeholders.occupied_text", "OCCUPIED"))
                    .replace("%flags%", plot.flagsLabel())
                    .replace("%generators%", String.valueOf(plot.generators.size()))
                    .replace("%tier%", String.valueOf(plot.vaultPages));
            msg(player, Text.apply(line, ph));
        }
    }

    public void teleportHome(Player player, Plot plot) {
        Location home = plot.home();
        if (home == null) {
            msg(player, "&cHome location is invalid.");
            return;
        }
        player.teleport(home);
        fx(player, "teleport_home");
    }

    public void deposit(Player player, Plot plot, double amount) {
        if (amount <= 0) return;
        if (!plot.canManage(player) && !plot.isOwner(player.getUniqueId())) {
            msg(player, "&cOnly owners and co-owners can deposit.");
            return;
        }
        amount = Math.min(amount, economy.balance(player));
        if (!economy.withdraw(player, amount)) {
            msg(player, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))));
            return;
        }
        plot.bank += amount;
        plot.audit(player.getName(), "DEPOSIT", "$" + Text.money(amount));
        msg(player, cfg().getString("economy.deposit_message").replace("%amount%", Text.money(amount)).replace("%balance%", Text.money(plot.bank)));
    }

    public void withdraw(Player player, Plot plot, double amount) {
        if (amount <= 0) return;
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("plotmanager.admin")) {
            msg(player, "&cOnly the owner can withdraw.");
            return;
        }
        if (plot.bank < amount) {
            msg(player, cfg().getString("economy.not_enough_bank_message"));
            return;
        }
        plot.bank -= amount;
        economy.deposit(player, amount);
        plot.audit(player.getName(), "WITHDRAW", "$" + Text.money(amount));
        msg(player, cfg().getString("economy.withdraw_message").replace("%amount%", Text.money(amount)).replace("%balance%", Text.money(plot.bank)));
    }

    public void buyUpgrade(Player player, Plot plot, String id) {
        if (!plot.canManage(player)) {
            msg(player, "&cYou cannot buy upgrades for this plot.");
            return;
        }
        switch (id) {
            case "fly" -> purchase(player, plot, cfg().getDouble("upgrades.fly.cost"), () -> {
                plot.flyUnlocked = true;
                msg(player, cfg().getString("upgrades.fly.purchase_message").replace("%cost%", Text.money(cfg().getDouble("upgrades.fly.cost"))));
            }, plot.flyUnlocked);
            case "crop" -> purchase(player, plot, cfg().getDouble("upgrades.crop_boost.cost"), () -> {
                plot.cropBoost = true;
                msg(player, cfg().getString("upgrades.crop_boost.purchase_message")
                        .replace("%cost%", Text.money(cfg().getDouble("upgrades.crop_boost.cost")))
                        .replace("%multiplier%", String.valueOf(cfg().getDouble("upgrades.crop_boost.multiplier"))));
            }, plot.cropBoost);
            case "music" -> purchase(player, plot, cfg().getDouble("upgrades.music.cost"), () -> {
                plot.musicUnlocked = true;
                msg(player, cfg().getString("upgrades.music.purchase_message").replace("%cost%", Text.money(cfg().getDouble("upgrades.music.cost"))));
            }, plot.musicUnlocked);
            case "sorter" -> purchase(player, plot, cfg().getDouble("upgrades.smart_sorter.cost"), () -> {
                plot.sorterUnlocked = true;
                msg(player, cfg().getString("upgrades.smart_sorter.purchase_message").replace("%cost%", Text.money(cfg().getDouble("upgrades.smart_sorter.cost"))));
            }, plot.sorterUnlocked);
            case "factory" -> purchase(player, plot, cfg().getDouble("upgrades.factory.cost"), () -> {
                plot.factoryUnlocked = true;
                msg(player, cfg().getString("upgrades.factory.purchase_message").replace("%cost%", Text.money(cfg().getDouble("upgrades.factory.cost"))));
            }, plot.factoryUnlocked);
            case "vault" -> {
                int max = cfg().getInt("upgrades.vault_page.max_pages", 5);
                if (plot.vaultPages >= max) {
                    msg(player, "&cMax vault pages reached.");
                    return;
                }
                double cost = cfg().getDouble("upgrades.vault_page.cost_per_page");
                purchase(player, plot, cost, () -> {
                    plot.vaultPages++;
                    msg(player, cfg().getString("upgrades.vault_page.purchase_message")
                            .replace("%page%", String.valueOf(plot.vaultPages)).replace("%cost%", Text.money(cost)));
                }, false);
            }
            case "gen1" -> buyGen(player, plot, 1);
            case "gen2" -> buyGen(player, plot, 2);
            case "gen3" -> buyGen(player, plot, 3);
        }
    }

    private void buyGen(Player player, Plot plot, int tier) {
        int need = switch (tier) {
            case 2 -> cfg().getInt("leveling.generator_tier2_unlocked_at_level", 10);
            case 3 -> cfg().getInt("leveling.generator_tier3_unlocked_at_level", 25);
            default -> 1;
        };
        if (plot.level < need) {
            msg(player, "&cThis generator unlocks at plot level " + need + ".");
            return;
        }
        String path = "upgrades.generator_tier_" + tier;
        double cost = cfg().getDouble(path + ".cost");
        purchase(player, plot, cost, () -> {
            GeneratorInstance g = new GeneratorInstance();
            g.tier = tier;
            g.lastTick = System.currentTimeMillis();
            plot.generators.add(g);
            msg(player, cfg().getString(path + ".purchase_message").replace("%cost%", Text.money(cost)));
        }, false);
    }

    private void purchase(Player player, Plot plot, double cost, Runnable success, boolean owned) {
        if (owned) {
            msg(player, "&aAlready unlocked.");
            return;
        }
        if (plot.bank >= cost) {
            plot.bank -= cost;
        } else if (!economy.charge(player, cost, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))))) {
            fx(player, "upgrade_denied");
            return;
        }
        success.run();
        plot.audit(player.getName(), "UPGRADE", "Purchased for $" + Text.money(cost));
        fx(player, "upgrade_purchase");
    }

    public void buyCosmetic(Player player, Plot plot, String type, String id) {
        String path = "cosmetics." + type + "." + id;
        double price = cfg().getDouble(path + ".price");
        boolean selected = type.equals("borders") ? id.equals(plot.borderCosmetic) : id.equals(plot.particleCosmetic);
        if (!selected) {
            if (plot.bank < price && !economy.has(player, price)) {
                msg(player, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))));
                return;
            }
            if (plot.bank >= price) plot.bank -= price;
            else economy.withdraw(player, price);
        }
        if (type.equals("borders")) plot.borderCosmetic = id;
        else plot.particleCosmetic = id;
        msg(player, "&aSelected " + cfg().getString(path + ".name", id));
    }

    public boolean canFly(Player player, Plot plot) {
        if (player == null) return false;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (bypass(player)) return true;
        if (plot == null) return false;
        if (!plot.isMember(player.getUniqueId()) && !plot.isOwner(player.getUniqueId())) return false;
        return plot.flyUnlocked || plot.level >= cfg().getInt("leveling.fly_unlocked_at_level", 15);
    }

    public void updateFly(Player player, Plot plot) {
        if (session(player).drone) return;
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return;
        boolean allow = canFly(player, plot);
        if (!allow && player.getAllowFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        } else if (allow && plot != null && plot.flyUnlocked) {
            player.setAllowFlight(true);
        }
    }

    public void toggleDrone(Player player) {
        if (!cfg().getBoolean("drone.enabled", true)) {
            msg(player, "&cDrone mode is disabled.");
            return;
        }
        PlayerSession s = session(player);
        if (s.drone) {
            disableDrone(player, true);
            return;
        }
        Plot plot = here(player);
        if (plot == null || !plot.isOwner(player.getUniqueId()) && !plot.canManage(player)) {
            msg(player, "&cYou can only drone your own plot.");
            return;
        }
        s.drone = true;
        s.droneReturn = player.getLocation().clone();
        s.droneGameMode = player.getGameMode();
        s.droneWasAllowFlight = player.getAllowFlight();
        s.droneWasFlying = player.isFlying();
        if (player.getGameMode() != GameMode.SURVIVAL) player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
        player.setFlying(true);
        if (cfg().getBoolean("drone.invisibility", true)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
        }
        msg(player, cfg().getString("drone.enabled_message"));
    }

    public void disableDrone(Player player, boolean message) {
        PlayerSession s = session(player);
        s.drone = false;
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        if (s.droneGameMode != null) player.setGameMode(s.droneGameMode);
        player.setAllowFlight(s.droneWasAllowFlight);
        player.setFlying(s.droneWasFlying);
        if (s.droneReturn != null) player.teleport(s.droneReturn);
        s.droneReturn = null;
        if (message) msg(player, cfg().getString("drone.disabled_message"));
    }

    public void tickDrone(Player player, Plot plot) {
        PlayerSession s = session(player);
        if (!s.drone) return;
        Plot home = store.index.at(s.droneReturn == null ? player.getLocation() : s.droneReturn);
        if (home == null) home = plot;
        double max = cfg().getDouble("drone.max_outside_blocks", 10);
        if (home == null || player.getLocation().getWorld() == null) return;
        Cuboid c = home.cuboid();
        Location loc = player.getLocation();
        boolean outside = loc.getBlockX() < c.minX - max || loc.getBlockX() > c.maxX + max
                || loc.getBlockZ() < c.minZ - max || loc.getBlockZ() > c.maxZ + max;
        if (outside) {
            msg(player, cfg().getString("drone.lost_signal_message"));
            Location center = home.center();
            if (center != null) player.teleport(center.add(0, 8, 0));
            disableDrone(player, true);
        }
        if (player.getGameMode() != GameMode.SURVIVAL) player.setGameMode(GameMode.SURVIVAL);
        player.setAllowFlight(true);
    }

    public void startGps(Player player, Plot plot) {
        if (!cfg().getBoolean("gps.enabled", true)) {
            teleportHome(player, plot);
            return;
        }
        PlayerSession s = session(player);
        s.gpsPlot = plot.id;
        s.gpsTarget = plot.home();
        s.gpsStarted = System.currentTimeMillis();
        msg(player, cfg().getString("gps.start_message"));
    }

    public void tickGps(Player player) {
        PlayerSession s = session(player);
        if (s.gpsTarget == null) return;
        if (System.currentTimeMillis() - s.gpsStarted > cfg().getInt("gps.max_duration_seconds", 180) * 1000L) {
            s.gpsTarget = null;
            return;
        }
        Location to = s.gpsTarget.clone();
        if (to.getWorld() == null || player.getWorld() == null || !to.getWorld().equals(player.getWorld())) return;
        if (player.getLocation().distanceSquared(to) <= Math.pow(cfg().getDouble("gps.arrive_distance", 4), 2)) {
            Plot plot = store.get(s.gpsPlot);
            msg(player, cfg().getString("gps.arrive_message").replace("%plot_name%", plot == null ? "plot" : plot.name));
            s.gpsTarget = null;
            return;
        }
        Vector dir = to.toVector().subtract(player.getLocation().toVector());
        if (dir.lengthSquared() < 0.01) return;
        dir.normalize();
        Location p = player.getLocation().add(dir.multiply(2)).add(0, 0.2, 0);
        FX.spawn(p, cfg().getString("gps.particle", "END_ROD"), 6);
    }

    public void tryElevator(Player player, boolean sneak) {
        if (!cfg().getBoolean("elevators.enabled", true)) return;
        Plot plot = here(player);
        if (plot == null) return;
        Block at = player.getLocation().subtract(0, 1, 0).getBlock();
        Material mat = Items.material(cfg().getString("elevators.block"), Material.IRON_BLOCK);
        if (at.getType() != mat) return;
        int max = cfg().getInt("elevators.max_distance", 64);
        if (sneak) {
            for (int y = at.getY() - 1; y >= Math.max(plot.minY, at.getY() - max); y--) {
                Block b = at.getWorld().getBlockAt(at.getX(), y, at.getZ());
                if (b.getType() == mat) {
                    player.teleport(b.getLocation().add(0.5, 1, 0.5));
                    FX.play(player, cfg().getString("elevators.sound", "ENTITY_ENDERMAN_TELEPORT"));
                    return;
                }
            }
        }
    }

    public void handleJumpElevator(Player player) {
        if (!cfg().getBoolean("elevators.enabled", true)) return;
        if (player.getVelocity().getY() <= 0) return;
        Plot plot = here(player);
        if (plot == null) return;
        Block at = player.getLocation().subtract(0, 1, 0).getBlock();
        Material mat = Items.material(cfg().getString("elevators.block"), Material.IRON_BLOCK);
        if (at.getType() != mat) return;
        int max = cfg().getInt("elevators.max_distance", 64);
        for (int y = at.getY() + 1; y <= Math.min(plot.maxY, at.getY() + max); y++) {
            Block b = at.getWorld().getBlockAt(at.getX(), y, at.getZ());
            if (b.getType() == mat) {
                player.teleport(b.getLocation().add(0.5, 1, 0.5));
                FX.play(player, cfg().getString("elevators.sound", "ENTITY_ENDERMAN_TELEPORT"));
                return;
            }
        }
    }

    public void tagCombat(Player a, Player b) {
        long until = System.currentTimeMillis() + cfg().getInt("plot_protections.combat_tag_seconds", 5) * 1000L;
        session(a).combatUntil = until;
        session(b).combatUntil = until;
        combat.put(a.getUniqueId(), until);
        combat.put(b.getUniqueId(), until);
    }

    public void addPlotExp(Plot plot, long amount) {
        if (!cfg().getBoolean("leveling.enabled", true) || amount <= 0) return;
        int max = cfg().getInt("leveling.max_level", 100);
        boolean up = plot.addExp(amount, max);
        if (up) {
            Player owner = plot.owner == null ? null : Bukkit.getPlayer(plot.owner);
            if (owner != null) {
                msg(owner, cfg().getString("leveling.level_up_message").replace("%level%", String.valueOf(plot.level)));
                fx(owner, "level_up");
            }
            holograms.spawnPlot(plot);
            discord.maybeRole(plot);
            maybeMayor(plot);
        }
    }

    public void maybeMayor(Plot plot) {
        if (!cfg().getBoolean("mayor.enabled", true)) return;
        var top = store.richest(1);
        if (top.isEmpty()) return;
        Plot mayor = top.get(0);
        if (store.mayorPlot == null || !store.mayorPlot.equals(mayor.id)) {
            store.mayorPlot = mayor.id;
            Text.broadcast(prefix() + cfg().getString("mayor.announce_message", "%player% is Mayor").replace("%player%", mayor.ownerName));
        }
    }

    public void runOfflineGenerators(Plot plot) {
        long now = System.currentTimeMillis();
        for (GeneratorInstance g : plot.generators) {
            String path = "upgrades.generator_tier_" + g.tier;
            long interval = cfg().getLong(path + ".interval_minutes", 10) * 60_000L;
            if (interval <= 0) continue;
            long cycles = Math.max(0, (now - g.lastTick) / interval);
            if (cycles <= 0) continue;
            Material item = Items.material(cfg().getString(path + ".item"), Material.IRON_INGOT);
            int amount = cfg().getInt(path + ".amount", 1);
            long produced = 0;
            for (int i = 0; i < cycles; i++) {
                if (plot.addToVault(new ItemStack(item, amount))) produced += amount;
                else break;
            }
            plot.generatorItemsOffline += produced;
            g.lastTick = now;
        }
    }

    public void tickGenerators() {
        long now = System.currentTimeMillis();
        for (Plot plot : store.plots.values()) {
            for (GeneratorInstance g : plot.generators) {
                String path = "upgrades.generator_tier_" + g.tier;
                long interval = cfg().getLong(path + ".interval_minutes", 10) * 60_000L;
                if (now - g.lastTick < interval) continue;
                Material item = Items.material(cfg().getString(path + ".item"), Material.IRON_INGOT);
                int amount = cfg().getInt(path + ".amount", 1);
                plot.addToVault(new ItemStack(item, amount));
                g.lastTick = now;
            }
        }
    }

    public void buyShop(Player player, Plot plot, ChestShop shop, Block sign) {
        if (player.getUniqueId().equals(plot.owner)) {
            msg(player, "&7That's your shop.");
            return;
        }
        double tax = cfg().getDouble("chest_shops.tax_percent", 5) + store.mayorTaxPercent;
        double total = shop.price;
        if (!economy.has(player, total)) {
            msg(player, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))));
            return;
        }
        ItemStack stack = takeShopStock(plot, shop, sign);
        if (stack == null) {
            msg(player, cfg().getString("chest_shops.out_of_stock_message"));
            return;
        }
        economy.withdraw(player, total);
        double taxAmt = total * (tax / 100.0);
        double net = total - taxAmt;
        plot.bank += net;
        if (taxAmt > 0 && store.mayorPlot != null) {
            Plot mayor = store.get(store.mayorPlot);
            if (mayor != null && !mayor.id.equals(plot.id)) mayor.bank += taxAmt * (store.mayorTaxPercent / Math.max(1.0, tax));
        }
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(stack);
        overflow.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
        msg(player, cfg().getString("chest_shops.purchase_message_buyer")
                .replace("%amount%", String.valueOf(shop.amount))
                .replace("%item%", shop.item.name())
                .replace("%price%", Text.money(shop.price)));
        Player owner = Bukkit.getPlayer(plot.owner);
        if (owner != null) {
            msg(owner, cfg().getString("chest_shops.purchase_message_seller")
                    .replace("%buyer%", player.getName())
                    .replace("%amount%", String.valueOf(shop.amount))
                    .replace("%item%", shop.item.name())
                    .replace("%price%", Text.money(shop.price)));
        } else {
            plot.shopSalesOffline += net;
        }
        addPlotExp(plot, cfg().getInt("leveling.exp_per_shop_sale", 10));
        plot.audit(player.getName(), "SHOP", shop.item.name() + " x" + shop.amount);
    }

    private ItemStack takeShopStock(Plot plot, ChestShop shop, Block sign) {
        Block chest = findAdjacentChest(sign);
        if (chest != null && chest.getState() instanceof Container container) {
            ItemStack taken = remove(container.getInventory(), shop.item, shop.amount);
            if (taken != null) {
                container.update();
                return taken;
            }
            if (plot.factoryUnlocked && cfg().getBoolean("factories.auto_restock", true)) {
                ItemStack fromVault = plot.takeFromVault(shop.item, shop.amount);
                if (fromVault != null) return fromVault;
            }
            return null;
        }
        if (plot.factoryUnlocked) return plot.takeFromVault(shop.item, shop.amount);
        return null;
    }

    private Block findAdjacentChest(Block sign) {
        for (var face : org.bukkit.block.BlockFace.values()) {
            Block rel = sign.getRelative(face);
            if (rel.getState() instanceof Chest) return rel;
        }
        return sign.getRelative(org.bukkit.block.BlockFace.DOWN).getState() instanceof Chest ? sign.getRelative(org.bukkit.block.BlockFace.DOWN) : null;
    }

    private ItemStack remove(org.bukkit.inventory.Inventory inv, Material mat, int amount) {
        int left = amount;
        for (int i = 0; i < inv.getSize() && left > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != mat) continue;
            int take = Math.min(left, it.getAmount());
            it.setAmount(it.getAmount() - take);
            if (it.getAmount() <= 0) inv.setItem(i, null);
            left -= take;
        }
        return left == 0 ? new ItemStack(mat, amount) : null;
    }

    public void notifyMailbox(Plot plot, Player player) {
        if (!cfg().getBoolean("mailbox.notification_on_delivery", true)) return;
        Player owner = Bukkit.getPlayer(plot.owner);
        if (owner != null) {
            msg(owner, cfg().getString("mailbox.delivery_message").replace("%player%", player.getName()));
            fx(owner, "mailbox_delivery");
        }
    }

    public void handleHoloMailbox(Player player, Item drop) {
        if (drop == null) return;
        Plot plot = store.index.at(drop.getLocation());
        if (plot == null || !plot.mailboxHolo || plot.mailboxWorld == null) return;
        Location zone = plot.mailboxLocation();
        if (zone == null || zone.distanceSquared(drop.getLocation()) > Math.pow(cfg().getDouble("mailbox.holo_zone_radius", 2.5), 2)) return;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!drop.isValid()) return;
            ItemStack stack = drop.getItemStack();
            plot.addToVault(stack);
            drop.remove();
            FX.spawn(zone, "END_ROD", 20);
            notifyMailbox(plot, player);
            fx(player, "mailbox_delivery");
        }, cfg().getLong("mailbox.holo_suck_ticks", 25));
    }

    public void handleChatInput(Player player, String path, String msg) {
        if (path.startsWith("tip:")) {
            try {
                UUID id = UUID.fromString(path.substring(4));
                Plot plot = store.get(id);
                double amt = Double.parseDouble(msg.replace("$", "").trim());
                double min = cfg().getDouble("economy.tip_jar_minimum", 10);
                if (amt < min) {
                    msg(player, "&cMinimum tip is $" + Text.money(min));
                    return;
                }
                if (!economy.charge(player, amt, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))))) return;
                plot.bank += amt;
                addPlotExp(plot, (long) (amt * cfg().getDouble("leveling.exp_per_tip_dollar", 1)));
                Player owner = Bukkit.getPlayer(plot.owner);
                if (owner != null) msg(owner, cfg().getString("economy.tip_jar_message").replace("%player%", player.getName()).replace("%amount%", Text.money(amt)));
                else plot.tipsOffline += amt;
                msg(player, "&aTipped &2$" + Text.money(amt));
            } catch (Exception e) {
                msg(player, "&cInvalid amount.");
            }
            return;
        }
        if (path.equals("blackmarket-sell")) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (hand == null || hand.getType().isAir()) {
                msg(player, "&cHold the item you want to sell.");
                return;
            }
            try {
                double price = Double.parseDouble(msg.replace("$", "").trim());
                if (store.blackmarket.stream().filter(l -> player.getUniqueId().equals(l.seller)).count()
                        >= cfg().getInt("blackmarket.max_listings_per_player", 10)) {
                    msg(player, "&cYou have too many listings.");
                    return;
                }
                BlackmarketListing l = new BlackmarketListing();
                l.seller = player.getUniqueId();
                l.sellerName = player.getName();
                Plot plot = here(player);
                l.plotId = plot == null ? null : plot.id;
                l.itemBase64 = Serial.itemToBase64(hand.clone());
                l.price = price;
                store.blackmarket.add(l);
                player.getInventory().setItemInMainHand(null);
                if (plot != null) plot.blackmarketUsed = true;
                msg(player, "&8Item listed anonymously for &a$" + Text.money(price));
            } catch (Exception e) {
                msg(player, "&cInvalid price.");
            }
            return;
        }
        try {
            double v = Double.parseDouble(msg.replace("$", "").trim());
            cfg().set(path, v);
            saveConfig();
            msg(player, "&aUpdated &2" + path + " &ato &f$" + Text.money(v));
            fx(player, "claim_success");
        } catch (Exception e) {
            msg(player, "&cInvalid number.");
        }
    }

    public void maybeSnitch(Plot plot) {
        int chance = cfg().getInt("blackmarket.snitch_chance_percent", 5);
        if (ThreadLocalRandom.current().nextInt(100) < chance) {
            discord.snitch(plot);
        }
    }

    public void createHologram(Player player, Plot plot) {
        int max = cfg().getInt("holograms.max_holograms_per_plot", 2);
        if (plot.holograms.size() >= max) {
            msg(player, cfg().getString("holograms.max_holograms_message")
                    .replace("%current%", String.valueOf(plot.holograms.size())).replace("%max%", String.valueOf(max)));
            return;
        }
        double cost = cfg().getDouble("holograms.cost_per_hologram", 500);
        if (plot.bank >= cost) plot.bank -= cost;
        else if (!economy.charge(player, cost, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))))) return;
        CustomHologram h = new CustomHologram();
        h.world = player.getWorld().getName();
        h.x = player.getLocation().getX();
        h.y = player.getLocation().getY();
        h.z = player.getLocation().getZ();
        h.lines.addAll(cfg().getStringList("holograms.default_lines_on_create"));
        plot.holograms.add(h);
        holograms.spawnCustom(plot, h);
        msg(player, cfg().getString("holograms.hologram_created_message").replace("%cost%", Text.money(cost)));
    }

    public void addHoloLine(Player player, Plot plot, String text) {
        if (cfg().getBoolean("holograms.profanity_filter", true) && isProfane(text)) {
            msg(player, cfg().getString("holograms.profanity_blocked_message"));
            return;
        }
        CustomHologram h = holograms.nearest(plot, player.getLocation(), 6);
        if (h == null) {
            msg(player, "&cStand closer to a hologram.");
            return;
        }
        if (h.lines.size() >= cfg().getInt("holograms.max_lines_per_hologram", 10)) {
            msg(player, "&cMax lines reached.");
            return;
        }
        double cost = cfg().getDouble("holograms.cost_per_extra_line", 50);
        if (plot.bank >= cost) plot.bank -= cost;
        else if (!economy.charge(player, cost, cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(economy.balance(player))))) return;
        h.lines.add(text);
        holograms.spawnCustom(plot, h);
        msg(player, cfg().getString("holograms.line_added_message").replace("%text%", text));
    }

    public void removeHoloLine(Player player, Plot plot, int number) {
        CustomHologram h = holograms.nearest(plot, player.getLocation(), 6);
        if (h == null || number < 1 || number > h.lines.size()) {
            msg(player, "&cInvalid line.");
            return;
        }
        h.lines.remove(number - 1);
        holograms.spawnCustom(plot, h);
        msg(player, cfg().getString("holograms.line_removed_message").replace("%number%", String.valueOf(number)));
    }

    public void deleteHologram(Player player, Plot plot) {
        CustomHologram h = holograms.nearest(plot, player.getLocation(), 6);
        if (h == null) {
            msg(player, "&cStand closer to a hologram.");
            return;
        }
        holograms.despawn("custom:" + plot.id + ":" + h.id);
        plot.holograms.remove(h);
        msg(player, cfg().getString("holograms.hologram_deleted_message"));
    }

    public boolean isProfane(String text) {
        String lower = text.toLowerCase();
        for (String w : cfg().getStringList("holograms.banned_words")) {
            if (w != null && !w.isEmpty() && lower.contains(w.toLowerCase())) return true;
        }
        return false;
    }

    public int purgeInactive(int days) {
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        List<Plot> doomed = new ArrayList<>();
        for (Plot p : store.plots.values()) {
            if (p.lastOwnerLogin < cutoff) doomed.add(p);
        }
        for (Plot p : doomed) {
            discord.reset(p, days);
            deletePlot(p, true);
        }
        return doomed.size();
    }

    public void inspect(Player player, Plot plot) {
        Map<Material, Integer> counts = new HashMap<>();
        List<String> scan = cfg().getStringList("admin.inspect_items_to_scan");
        World world = plot.bukkitWorld();
        if (world == null) return;
        msg(player, "&aScanning plot...");
        for (int cx = plot.minX >> 4; cx <= plot.maxX >> 4; cx++) {
            for (int cz = plot.minZ >> 4; cz <= plot.maxZ >> 4; cz++) {
                if (!world.isChunkLoaded(cx, cz)) continue;
                for (org.bukkit.block.BlockState state : world.getChunkAt(cx, cz).getTileEntities()) {
                    if (!plot.contains(state.getLocation())) continue;
                    if (scan.contains(state.getType().name())) counts.merge(state.getType(), 1, Integer::sum);
                    if (state instanceof Container c) {
                        for (ItemStack it : c.getInventory().getContents()) {
                            if (it == null) continue;
                            if (scan.contains(it.getType().name())) {
                                counts.merge(it.getType(), it.getAmount(), Integer::sum);
                            }
                        }
                    }
                }
            }
        }
        double wealth = 0;
        for (var e : counts.entrySet()) {
            msg(player, cfg().getString("admin.inspect_format")
                    .replace("%count%", String.valueOf(e.getValue()))
                    .replace("%item%", e.getKey().name()));
            wealth += e.getValue() * 10;
        }
        wealth += plot.bank;
        msg(player, cfg().getString("admin.inspect_total_format").replace("%total%", Text.money(wealth)));
    }

    public void seize(Player admin, Plot plot) {
        double bank = plot.bank;
        economy.deposit(admin, bank);
        plot.bank = 0;
        plot.owner = admin.getUniqueId();
        plot.ownerName = admin.getName();
        plot.members.clear();
        holograms.spawnPlot(plot);
        blueMap.upsert(plot);
        msg(admin, cfg().getString("admin.seize_message").replace("%bank%", Text.money(bank)));
        discord.seize(admin.getName(), plot);
    }

    public void giveRadarMap(Player player) {
        Plot plot = here(player);
        World world = player.getWorld();
        MapView view = Bukkit.createMap(world);
        view.getRenderers().forEach(view::removeRenderer);
        view.addRenderer(new MapRenderer() {
            @Override
            public void render(MapView map, MapCanvas canvas, Player p) {
                Plot pl = here(p);
                java.awt.Color bg = new java.awt.Color(20, 20, 20);
                java.awt.Color green = new java.awt.Color(40, 200, 40);
                java.awt.Color red = new java.awt.Color(200, 40, 40);
                for (int x = 0; x < 128; x++) for (int z = 0; z < 128; z++) canvas.setPixelColor(x, z, bg);
                if (pl == null) return;
                Cuboid c = pl.cuboid();
                int w = Math.max(1, c.sizeX());
                int d = Math.max(1, c.sizeZ());
                for (int x = 0; x < 128; x++) {
                    int bx = c.minX + x * w / 128;
                    for (int z = 0; z < 128; z++) {
                        int bz = c.minZ + z * d / 128;
                        boolean border = bx <= c.minX + 1 || bx >= c.maxX - 1 || bz <= c.minZ + 1 || bz >= c.maxZ - 1;
                        if (border) canvas.setPixelColor(x, z, green);
                    }
                }
                for (Player other : Bukkit.getOnlinePlayers()) {
                    if (!pl.contains(other.getLocation())) continue;
                    int px = (int) ((other.getLocation().getBlockX() - c.minX) / (double) w * 128);
                    int pz = (int) ((other.getLocation().getBlockZ() - c.minZ) / (double) d * 128);
                    if (px >= 0 && px < 128 && pz >= 0 && pz < 128) {
                        canvas.setPixelColor(px, pz, pl.isMember(other.getUniqueId()) ? green : red);
                    }
                }
            }
        });
        ItemStack map = new ItemStack(Material.FILLED_MAP);
        MapMeta meta = (MapMeta) map.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.component("&2&lPlot Radar"));
            meta.setMapView(view);
            map.setItemMeta(meta);
        }
        Items.byteTag(map, keys.map);
        player.getInventory().addItem(map);
    }

    public void reloadAll() {
        reloadConfig();
        luckPerms.reload();
        holograms.shutdown();
        holograms.start();
    }

    private void printBanner() {
        if (!cfg().getBoolean("plugin.startup_banner.enabled", true)) {
            getLogger().info("PlotManager v" + getPluginMeta().getVersion() + " enabled.");
            return;
        }
        Map<String, String> ph = Map.of(
                "plugin_version", getPluginMeta().getVersion(),
                "server_version", Bukkit.getVersion(),
                "total_plots", String.valueOf(store.plots.size()),
                "db_status", "YAML OK",
                "vault_status", economy.status(),
                "discord_status", discord.status(),
                "bluemap_status", blueMap.status(),
                "fawe_status", fawe.status()
        );
        for (String line : cfg().getStringList("plugin.startup_banner.lines")) {
            Bukkit.getConsoleSender().sendMessage(Text.component(Text.apply(line, ph)));
        }
    }
}
