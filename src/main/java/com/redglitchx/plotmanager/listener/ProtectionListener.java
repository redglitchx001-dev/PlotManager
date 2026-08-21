/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.listener;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Iterator;
import java.util.List;

public class ProtectionListener implements Listener {
    private final PlotManager plugin;

    public ProtectionListener(PlotManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Plot plot = plugin.store.index.at(block.getLocation());
        if (plot != null) {
            if (plot.frozen && !plugin.bypass(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "admin.quarantine_message");
                return;
            }
            if (!plot.canBuild(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "plot_protections.break_denied_message");
                return;
            }
            plot.history(player, "BREAK", block.getType(), block.getLocation());
            if (block.getType() == Material.HOPPER) plot.hoppers = Math.max(0, plot.hoppers - 1);
            if (block.getType() == Material.SPAWNER) plot.spawners = Math.max(0, plot.spawners - 1);
            return;
        }
        if (plugin.protectedWorld(block.getWorld()) && !plugin.cfg().getBoolean("world_settings.wilderness_building", false) && !plugin.bypass(player)) {
            event.setCancelled(true);
            plugin.lang.msg(player, "plot_protections.wilderness_break_message");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Plot plot = plugin.store.index.at(block.getLocation());
        if (plot != null) {
            if (plot.frozen && !plugin.bypass(player)) {
                event.setCancelled(true);
                return;
            }
            if (!plot.canBuild(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "plot_protections.place_denied_message");
                return;
            }
            int hopperMax = plugin.luckPerms.hopperLimit(player);
            int spawnerMax = plugin.luckPerms.spawnerLimit(player);
            if (block.getType() == Material.HOPPER) {
                if (plot.hoppers >= hopperMax) {
                    event.setCancelled(true);
                    plugin.lang.msg(player, "plot_protections.hopper_limit_message", "%current%", String.valueOf(plot.hoppers), "%max%", String.valueOf(hopperMax));
                    return;
                }
                plot.hoppers++;
            }
            if (block.getType() == Material.SPAWNER) {
                if (plot.spawners >= spawnerMax) {
                    event.setCancelled(true);
                    plugin.lang.msg(player, "plot_protections.spawner_limit_message", "%current%", String.valueOf(plot.spawners), "%max%", String.valueOf(spawnerMax));
                    return;
                }
                plot.spawners++;
            }
            plot.history(player, "PLACE", block.getType(), block.getLocation());
            int xp = plugin.cfg().getInt("leveling.exp_per_block_placed", 0);
            if (xp > 0) plugin.addPlotExp(plot, xp);
            return;
        }
        if (plugin.protectedWorld(block.getWorld()) && !plugin.cfg().getBoolean("world_settings.wilderness_building", false) && !plugin.bypass(player)) {
            event.setCancelled(true);
            plugin.lang.msg(player, "plot_protections.wilderness_break_message");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (event.getAction() == Action.PHYSICAL) {
            if (isCrop(block.getType()) && (plugin.store.index.at(block.getLocation()) != null
                    || !plugin.cfg().getBoolean("world_settings.wilderness_crop_trample", false))) {
                Plot plot = plugin.store.index.at(block.getLocation());
                if (plot == null || !plot.canBuild(player)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        Plot plot = plugin.store.index.at(block.getLocation());
        boolean container = isContainer(block.getType());
        if (plot != null) {
            if (plot.frozen && !plugin.bypass(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "admin.quarantine_message");
                return;
            }
            if (container && !plot.canChests(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "plot_protections.chest_denied_message");
                return;
            }
            if (!container && event.getAction().isRightClick() && !plot.canInteract(player) && !plot.canBuild(player)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "plot_protections.interact_denied_message");
            }
            return;
        }
        if (!plugin.cfg().getBoolean("world_settings.wilderness_interact", true)
                && plugin.protectedWorld(block.getWorld()) && !plugin.bypass(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (plugin.cfg().getBoolean("world_settings.disable_explosions_everywhere", true)) {
            event.blockList().clear();
            return;
        }
        event.blockList().removeIf(b -> {
            Plot p = plugin.store.index.at(b.getLocation());
            return p != null && !p.flag(PlotFlag.EXPLOSIONS);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (plugin.cfg().getBoolean("world_settings.disable_explosions_everywhere", true)) {
            event.blockList().clear();
            return;
        }
        event.blockList().removeIf(b -> plugin.store.index.at(b.getLocation()) != null);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPrime(ExplosionPrimeEvent event) {
        if (plugin.cfg().getBoolean("world_settings.disable_explosions_everywhere", true)) {
            event.setRadius(0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!plugin.cfg().getBoolean("plot_protections.prevent_piston_push_in", true)
                && !plugin.cfg().getBoolean("plot_protections.prevent_piston_push_out", true)) return;
        Plot origin = plugin.store.index.at(event.getBlock().getLocation());
        if (crosses(origin, event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Plot origin = plugin.store.index.at(event.getBlock().getLocation());
        if (crosses(origin, event.getBlocks(), event.getDirection())) event.setCancelled(true);
    }

    private boolean crosses(Plot origin, List<Block> blocks, BlockFace dir) {
        for (Block block : blocks) {
            Plot here = plugin.store.index.at(block.getLocation());
            Plot dest = plugin.store.index.at(block.getRelative(dir).getLocation());
            if (origin == null && (here != null || dest != null)) return true;
            if (origin != null && dest != null && !origin.id.equals(dest.id)) return true;
            if (origin != null && dest == null && plugin.cfg().getBoolean("plot_protections.prevent_piston_push_out", true)) return true;
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent event) {
        if (!plugin.cfg().getBoolean("plot_protections.prevent_liquid_flow_in", true)) return;
        Plot from = plugin.store.index.at(event.getBlock().getLocation());
        Plot to = plugin.store.index.at(event.getToBlock().getLocation());
        if (to != null && (from == null || !from.id.equals(to.id))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (plugin.cfg().getBoolean("world_settings.disable_fire_everywhere", true)
                || plugin.cfg().getBoolean("plot_protections.prevent_fire_spread", true)) {
            if (event.getSource().getType().name().contains("FIRE") || event.getNewState().getType().name().contains("FIRE")) {
                event.setCancelled(true);
            }
        }
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && !plot.flag(PlotFlag.FIRE) && event.getNewState().getType().name().contains("FIRE")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (plugin.cfg().getBoolean("world_settings.disable_fire_everywhere", true)) event.setCancelled(true);
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && !plot.flag(PlotFlag.FIRE)) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onLeaves(LeavesDecayEvent event) {
        if (!plugin.cfg().getBoolean("plot_protections.prevent_leaf_decay", false)) return;
        if (plugin.store.index.at(event.getBlock().getLocation()) != null) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onChange(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Enderman && plugin.cfg().getBoolean("world_settings.disable_enderman_pickup", true)) {
            event.setCancelled(true);
            return;
        }
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && plugin.cfg().getBoolean("plot_protections.prevent_enderman_grief", true)
                && event.getEntity() instanceof Enderman) {
            event.setCancelled(true);
        }
        if (plugin.cfg().getBoolean("world_settings.disable_wither_everywhere", true)
                && event.getEntity().getType().name().contains("WITHER")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        Plot plot = plugin.store.index.at(event.getLocation());
        if (plot != null && event.getEntity() instanceof Monster && !plot.flag(PlotFlag.MOBS)) {
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM
                    && event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.SPAWNER_EGG) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player damager = damager(event.getDamager());
        if (event.getEntity() instanceof Player victim) {
            Plot plot = plugin.store.index.at(victim.getLocation());
            if (plot != null && !plot.flag(PlotFlag.PVP) && damager != null && !plugin.bypass(damager)) {
                event.setCancelled(true);
                return;
            }
            if (plot != null && plot.flag(PlotFlag.PVP) && damager != null) {
                plugin.tagCombat(damager, victim);
            }
        }
        if (event.getEntity() instanceof Animals || event.getEntity() instanceof Villager) {
            Plot plot = plugin.store.index.at(event.getEntity().getLocation());
            if (plot != null && plugin.cfg().getBoolean("plot_protections.entity_protection", true)
                    && (damager == null || !plot.canBuild(damager))) {
                event.setCancelled(true);
            }
        }
        if (event.getEntity() instanceof ItemFrame || event.getEntity() instanceof ArmorStand || event.getEntity() instanceof Painting) {
            Plot plot = plugin.store.index.at(event.getEntity().getLocation());
            if (plot != null && (damager == null || !plot.canBuild(damager))) event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity e = event.getRightClicked();
        Player player = event.getPlayer();
        Plot plot = plugin.store.index.at(e.getLocation());
        if (plot == null) return;
        if (e instanceof Villager && !plot.canBuild(player) && !plot.flag(PlotFlag.VILLAGER_TRADES)) {
            event.setCancelled(true);
            return;
        }
        if ((e instanceof Animals || e instanceof ItemFrame || e instanceof ArmorStand) && !plot.canBuild(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        Plot plot = plugin.store.index.at(event.getEntity().getLocation());
        if (plot != null && !plot.canBuild(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent event) {
        Plot plot = plugin.store.index.at(event.getRightClicked().getLocation());
        if (plot != null && !plot.canBuild(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHanging(HangingBreakByEntityEvent event) {
        Plot plot = plugin.store.index.at(event.getEntity().getLocation());
        Player p = damager(event.getRemover());
        if (plot != null && (p == null || !plot.canBuild(p))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && !plot.canBuild(event.getPlayer())) event.setCancelled(true);
        else if (plot == null && plugin.protectedWorld(event.getBlock().getWorld())
                && !plugin.cfg().getBoolean("world_settings.wilderness_building") && !plugin.bypass(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && !plot.canBuild(event.getPlayer())) event.setCancelled(true);
        else if (plot == null && plugin.protectedWorld(event.getBlock().getWorld())
                && !plugin.cfg().getBoolean("world_settings.wilderness_building") && !plugin.bypass(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getTo() == null) return;
        Plot from = plugin.store.index.at(event.getFrom());
        Plot to = plugin.store.index.at(event.getTo());
        if (to != null && (from == null || !from.id.equals(to.id))) {
            if (to.isBanned(event.getPlayer().getUniqueId()) && !plugin.bypass(event.getPlayer())) {
                event.setCancelled(true);
                plugin.lang.msg(event.getPlayer(), "bouncer_shield.cooldown_message");
                return;
            }
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
                    && !plugin.cfg().getBoolean("plot_protections.enderpearl_into_plot", false)
                    && !to.isMember(event.getPlayer().getUniqueId()) && !plugin.bypass(event.getPlayer())) {
                event.setCancelled(true);
            }
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT
                    && !plugin.cfg().getBoolean("plot_protections.chorus_fruit_into_plot", false)
                    && !to.isMember(event.getPlayer().getUniqueId()) && !plugin.bypass(event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Plot from = plugin.store.index.at(event.getBlock().getLocation());
        BlockFace face = null;
        if (event.getBlock().getBlockData() instanceof org.bukkit.block.data.Directional d) face = d.getFacing();
        if (face != null) {
            Plot to = plugin.store.index.at(event.getBlock().getRelative(face).getLocation());
            if (to != null && (from == null || !from.id.equals(to.id))) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent event) {
        InventoryHolder src = event.getSource().getHolder();
        InventoryHolder dest = event.getDestination().getHolder();
        if (dest instanceof HopperMinecart cart) {
            Plot plot = plugin.store.index.at(cart.getLocation());
            if (plot != null) event.setCancelled(true);
        }
        if (src instanceof org.bukkit.block.BlockState bs) {
            Plot from = plugin.store.index.at(bs.getLocation());
            if (dest instanceof org.bukkit.block.BlockState bd) {
                Plot to = plugin.store.index.at(bd.getLocation());
                if (from != null && to != null && !from.id.equals(to.id)) event.setCancelled(true);
                if (from != null && to == null) event.setCancelled(true);
                if (from == null && to != null) event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (!(event.getVehicle() instanceof HopperMinecart cart)) return;
        Plot from = plugin.store.index.at(event.getFrom());
        Plot to = plugin.store.index.at(event.getTo());
        if (to != null && (from == null || !from.id.equals(to.id))) {
            cart.remove();
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Plot plot = plugin.store.index.at(event.getVehicle().getLocation());
        Player p = damager(event.getAttacker());
        if (plot != null && (p == null || !plot.canBuild(p))) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (plot != null && plot.cropBoost) {
            double mult = plugin.cfg().getDouble("upgrades.crop_boost.multiplier", 2.0);
            if (Math.random() < (1.0 - 1.0 / Math.max(1.0, mult))) {
                // extra growth chance already applied by repeating; skip cancel
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (!plugin.cfg().getBoolean("plot_protections.prevent_villager_conversion", true)) return;
        if (plugin.store.index.at(event.getEntity().getLocation()) != null
                && event.getTransformReason() == EntityTransformEvent.TransformReason.INFECTION) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLectern(PlayerTakeLecternBookEvent event) {
        Plot plot = plugin.store.index.at(event.getLectern().getLocation());
        if (plot != null && !plot.canChests(event.getPlayer())) event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityTrample(EntityInteractEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (isCrop(event.getBlock().getType())) event.setCancelled(true);
    }

    private boolean isContainer(Material m) {
        String n = m.name();
        return m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.BARREL
                || m == Material.HOPPER || m == Material.DROPPER || m == Material.DISPENSER
                || n.contains("SHULKER") || n.contains("FURNACE") || n.equals("SMOKER")
                || n.equals("BLAST_FURNACE") || n.contains("CHEST") || n.equals("BREWING_STAND")
                || n.equals("LECTERN") || n.equals("JUKEBOX");
    }

    private boolean isCrop(Material m) {
        String n = m.name();
        return n.contains("CROP") || n.equals("FARMLAND") || n.contains("WHEAT") || n.equals("CARROTS")
                || n.equals("POTATOES") || n.equals("BEETROOTS");
    }

    private Player damager(Entity entity) {
        if (entity instanceof Player p) return p;
        if (entity instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        if (entity instanceof LivingEntity le && le.getKiller() != null) return le.getKiller();
        return null;
    }
}
