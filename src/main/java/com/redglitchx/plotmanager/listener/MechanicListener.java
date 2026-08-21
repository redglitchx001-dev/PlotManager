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
import com.redglitchx.plotmanager.data.ChestShop;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class MechanicListener implements Listener {
    private final PlotManager plugin;

    public MechanicListener(PlotManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        String header = PlainTextComponentSerializer.plainText().serialize(event.line(0)).replaceAll("§[0-9A-FK-ORa-fk-or]", "");
        Plot plot = plugin.store.index.at(event.getBlock().getLocation());
        if (header.equalsIgnoreCase("[Tip Jar]") || header.equalsIgnoreCase("[TipJar]")) {
            if (plot == null || !plot.canManage(player)) {
                event.setCancelled(true);
                return;
            }
            event.line(0, Text.component(plugin.lang.line(player, "signs.tip-jar-line1")));
            event.line(1, Text.component(plugin.lang.line(player, "signs.tip-jar-line2")));
            plugin.lang.msg(player, "economy.tip-jar-created");
            return;
        }
        String shopHeader = plugin.cfg().getString("chest_shops.sign_header", "[Shop]");
        if (header.equalsIgnoreCase(shopHeader) || header.equalsIgnoreCase("[Shop]")) {
            if (!plugin.cfg().getBoolean("chest_shops.enabled", true)) return;
            if (plot == null || !plot.canManage(player)) {
                event.setCancelled(true);
                return;
            }
            if (plot.shops.size() >= plugin.cfg().getInt("chest_shops.max_shops_per_plot", 20)) {
                event.setCancelled(true);
                plugin.lang.msg(player, "shops.limit");
                return;
            }
            int amount = parseInt(plain(event, 1), 1);
            Material item = Items.material(plain(event, 2), null);
            double price = parseDouble(plain(event, 3).replace("$", ""), -1);
            if (item == null || price < 0) {
                plugin.lang.msg(player, "shops.format");
                event.setCancelled(true);
                return;
            }
            ChestShop shop = new ChestShop();
            shop.world = event.getBlock().getWorld().getName();
            shop.x = event.getBlock().getX();
            shop.y = event.getBlock().getY();
            shop.z = event.getBlock().getZ();
            shop.item = item;
            shop.amount = Math.max(1, amount);
            shop.price = price;
            plot.shops.add(shop);
            String color = plugin.cfg().getString("chest_shops.sign_color", "&2");
            event.line(0, Text.component(color + shopHeader));
            event.line(1, Text.component(plugin.lang.line(player, "signs.shop-amount", "%amount%", String.valueOf(shop.amount))));
            event.line(2, Text.component(plugin.lang.line(player, "signs.shop-item", "%item%", item.name())));
            event.line(3, Text.component(plugin.lang.line(player, "signs.shop-price", "%price%", Text.money(price))));
            plugin.lang.msg(player, "chest_shops.shop_created_message",
                    "%amount%", String.valueOf(shop.amount),
                    "%item%", item.name(),
                    "%price%", Text.money(price));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) return;
        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign sign)) return;
        Player player = event.getPlayer();
        String l0 = PlainTextComponentSerializer.plainText().serialize(sign.getSide(Side.FRONT).line(0));
        Plot plot = plugin.store.index.at(block.getLocation());
        if (l0.toLowerCase().contains("tip jar")) {
            event.setCancelled(true);
            if (plot == null) return;
            plugin.session(player).priceEditPath = "tip:" + plot.id;
            plugin.lang.msg(player, "economy.tip-prompt", "%min%", String.valueOf(plugin.cfg().getDouble("economy.tip_jar_minimum")));
            return;
        }
        if (l0.toLowerCase().contains("shop") && plot != null) {
            event.setCancelled(true);
            ChestShop shop = null;
            for (ChestShop s : plot.shops) {
                if (s.x == block.getX() && s.y == block.getY() && s.z == block.getZ()
                        && s.world.equals(block.getWorld().getName())) {
                    shop = s;
                    break;
                }
            }
            if (shop == null) return;
            plugin.buyShop(player, plot, shop, block);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMailboxClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof Barrel barrel)) return;
        Plot plot = plugin.store.index.at(barrel.getLocation());
        if (plot == null || plot.mailboxWorld == null) return;
        if (plot.mailboxX != barrel.getX() || plot.mailboxY != barrel.getY() || plot.mailboxZ != barrel.getZ()) return;
        if (plot.canChests(player)) return;
        // visitors may only deposit
        if (event.getClickedInventory() == event.getView().getTopInventory()
                && (event.isShiftClick() || event.getAction().name().contains("PICKUP") || event.getAction().name().contains("MOVE")
                || event.getAction().name().contains("HOTBAR") || event.getAction().name().contains("DROP")
                || event.getAction().name().contains("COLLECT"))) {
            if (event.getCurrentItem() != null && !event.getCurrentItem().getType().isAir()
                    && event.getClickedInventory() == event.getView().getTopInventory()) {
                event.setCancelled(true);
                plugin.lang.msg(player, "mailbox.mailbox_steal_message");
            }
        }
        if (event.getClickedInventory() == event.getView().getBottomInventory() && event.isShiftClick()) {
            // allow depositing via shift-click
            plugin.notifyMailbox(plot, player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        String msg = PlainTextComponentSerializer.plainText().serialize(event.message());
        var session = plugin.session(player);
        if (session.priceEditPath != null) {
            event.setCancelled(true);
            String path = session.priceEditPath;
            session.priceEditPath = null;
            Bukkit.getScheduler().runTask(plugin, () -> plugin.handleChatInput(player, path, msg));
            return;
        }
        if (session.plotChat) {
            event.setCancelled(true);
            Plot plot = plugin.store.index.at(player.getLocation());
            if (plot == null) {
                Bukkit.getScheduler().runTask(plugin, () -> plugin.lang.msg(player, "general.stand-in-plot"));
                return;
            }
            String line = plugin.lang.line(player, "signs.plot-chat", "%player%", player.getName(), "%message%", msg);
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (plot.contains(p.getLocation()) || plot.isMember(p.getUniqueId())) {
                        Text.send(p, plugin.prefix() + line);
                    }
                }
            });
            return;
        }
        plugin.discord.chatFromMinecraft(player, msg);
    }

    @EventHandler
    public void onJump(PlayerToggleFlightEvent event) {
        // elevators handled via move + sneak; also try jump on iron
    }

    private String plain(SignChangeEvent event, int line) {
        return PlainTextComponentSerializer.plainText().serialize(event.line(line)).trim();
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.replaceAll("[^0-9-]", "")); } catch (Exception e) { return def; }
    }

    private double parseDouble(String s, double def) {
        try { return Double.parseDouble(s.replaceAll("[^0-9.\\-]", "")); } catch (Exception e) { return def; }
    }
}
