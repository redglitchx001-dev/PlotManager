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
import com.redglitchx.plotmanager.data.PlayerSession;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Text;
import net.kyori.adventure.inventory.Book;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.List;

public class PlayerListener implements Listener {
    private final PlotManager plugin;

    public PlayerListener(PlotManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.session(player);
        for (Plot plot : plugin.store.ownedBy(player.getUniqueId())) {
            plot.lastOwnerLogin = System.currentTimeMillis();
            plot.ownerName = player.getName();
            plugin.runOfflineGenerators(plot);
            if (plugin.cfg().getBoolean("offline_report.enabled", true)
                    && (plot.visitorsOffline > 0 || plot.tipsOffline > 0 || plot.shopSalesOffline > 0 || plot.generatorItemsOffline > 0)) {
                giveReport(player, plot);
                plot.visitorsOffline = 0;
                plot.tipsOffline = 0;
                plot.shopSalesOffline = 0;
                plot.generatorItemsOffline = 0;
            }
            int warnDays = plugin.cfg().getInt("reset_system.warning_days_before", 2);
            // owner just logged in, so inactivity is reset
        }
        plugin.discord.joinLeave(plugin.cfg().getString("discord.join_message", "%player% joined"), player.getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        PlayerSession session = plugin.session(player);
        if (session.combatUntil > System.currentTimeMillis()) {
            player.setHealth(0);
        }
        if (session.drone) plugin.disableDrone(player, false);
        for (Plot plot : plugin.store.ownedBy(player.getUniqueId())) {
            plot.lastOwnerLogin = System.currentTimeMillis();
        }
        plugin.discord.joinLeave(plugin.cfg().getString("discord.leave_message", "%player% left"), player.getName());
        plugin.sessions.remove(player.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        plugin.session(event.getPlayer()).combatUntil = 0;
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Plot plot = plugin.store.index.at(event.getPlayer().getLocation());
            plugin.updateFly(event.getPlayer(), plot);
        }, 5L);
    }

    @EventHandler
    public void onWand(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack item = event.getItem();
        if (item == null) return;
        Player player = event.getPlayer();
        if (Items.hasTag(item, plugin.keys.wand)) {
            if (event.getClickedBlock() == null) return;
            event.setCancelled(true);
            if (!player.hasPermission("plotmanager.wand") && !player.hasPermission("plotmanager.admin")) return;
            var sel = plugin.session(player).selection;
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                sel.pos1 = event.getClickedBlock().getLocation();
                plugin.lang.msg(player, "claiming.pos1_message",
                        "%x%", String.valueOf(sel.pos1.getBlockX()),
                        "%y%", String.valueOf(sel.pos1.getBlockY()),
                        "%z%", String.valueOf(sel.pos1.getBlockZ()));
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                sel.pos2 = event.getClickedBlock().getLocation();
                plugin.lang.msg(player, "claiming.pos2_message",
                        "%x%", String.valueOf(sel.pos2.getBlockX()),
                        "%y%", String.valueOf(sel.pos2.getBlockY()),
                        "%z%", String.valueOf(sel.pos2.getBlockZ()));
            }
            return;
        }
        if (Items.hasTag(item, plugin.keys.godWand)) {
            event.setCancelled(true);
            if (!player.hasPermission("plotmanager.admin")) return;
            if (event.getClickedBlock() == null) return;
            Plot plot = plugin.store.index.at(event.getClickedBlock().getLocation());
            if (plot == null) return;
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                plot.level = plugin.cfg().getInt("leveling.max_level", 100);
                plot.bank = 999_999_999;
                plot.flyUnlocked = true;
                plot.cropBoost = true;
                plot.musicUnlocked = true;
                plot.factoryUnlocked = true;
                plot.sorterUnlocked = true;
                plot.vaultPages = plugin.cfg().getInt("upgrades.vault_page.max_pages", 5);
                plugin.lang.msg(player, "admin.godwand_max_message");
                plugin.holograms.spawnPlot(plot);
            } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                LocationStrike(player, plot);
            }
            return;
        }
        if (Items.hasTag(item, plugin.keys.rollbackWand)) {
            event.setCancelled(true);
            if (!player.hasPermission("plotmanager.admin")) return;
            if (event.getClickedBlock() == null) return;
            Plot plot = plugin.store.index.at(event.getClickedBlock().getLocation());
            if (plot == null) return;
            var rec = plot.findHistory(event.getClickedBlock().getLocation());
            if (rec == null) {
                plugin.lang.msg(player, "admin.no-history");
                return;
            }
            plugin.lang.msg(player, "admin.rollbackwand_format",
                    "%block%", rec.block,
                    "%action%", rec.action,
                    "%player%", rec.player,
                    "%time%", Text.formatDate(rec.time, plugin.cfg().getString("plugin.date_format"), plugin.cfg().getString("plugin.timezone")));
        }
    }

    private void LocationStrike(Player player, Plot plot) {
        var loc = plot.center();
        if (loc != null && loc.getWorld() != null) loc.getWorld().strikeLightningEffect(loc);
        plugin.deletePlot(plot, true);
        plugin.lang.msg(player, "admin.godwand_smite_message");
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        plugin.handleHoloMailbox(event.getPlayer(), event.getItemDrop());
    }

    private void giveReport(Player player, Plot plot) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta == null) return;
        meta.setTitle(Text.color(plugin.cfg().getString("offline_report.book_title", "Earnings Report")).replace('§', '&'));
        meta.setAuthor(plugin.cfg().getString("offline_report.book_author", "PlotManager"));
        String page = plugin.cfg().getString("offline_report.header", "") + "\n"
                + plugin.cfg().getString("offline_report.visitors_line", "").replace("%count%", String.valueOf(plot.visitorsOffline)) + "\n"
                + plugin.cfg().getString("offline_report.tips_line", "").replace("%amount%", Text.money(plot.tipsOffline)) + "\n"
                + plugin.cfg().getString("offline_report.shop_sales_line", "").replace("%amount%", Text.money(plot.shopSalesOffline)) + "\n"
                + plugin.cfg().getString("offline_report.generator_line", "").replace("%items%", String.valueOf(plot.generatorItemsOffline)) + "\n"
                + plugin.cfg().getString("offline_report.total_line", "").replace("%total%", Text.money(plot.tipsOffline + plot.shopSalesOffline));
        meta.addPages(Text.component(page));
        book.setItemMeta(meta);
        player.getInventory().addItem(book);
        player.openBook(Book.book(
                Text.component(plugin.cfg().getString("offline_report.book_title", "Earnings")),
                Text.component(plugin.cfg().getString("offline_report.book_author", "PlotManager")),
                List.of(Text.component(page))
        ));
    }
}
