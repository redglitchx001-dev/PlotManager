package com.redglitchx.plotmanager.listener;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.BlackmarketListing;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import com.redglitchx.plotmanager.gui.GuiHolder;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Serial;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Iterator;
import java.util.UUID;

public class GuiListener implements Listener {
    private final PlotManager plugin;

    public GuiListener(PlotManager plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        if (holder.type == GuiHolder.Type.VAULT) {
            Plot plot = plugin.store.get(holder.plotId);
            if (plot == null || !plot.canChests(player)) {
                event.setCancelled(true);
                plugin.msg(player, plugin.cfg().getString("plot_protections.chest_denied_message"));
            }
            return;
        }
        event.setCancelled(true);
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;
        String action = Items.tag(clicked, plugin.menus.actionKey);
        if (action == null || action.equals("noop")) return;
        plugin.fx(player, "gui_click");
        Plot plot = holder.plotId == null ? plugin.store.index.at(player.getLocation()) : plugin.store.get(holder.plotId);

        switch (action) {
            case "close" -> player.closeInventory();
            case "back" -> plugin.menus.openMain(player, plot);
            case "info" -> { player.closeInventory(); plugin.sendInfo(player, plot); }
            case "bank" -> { if (plot != null) plugin.menus.openBank(player, plot); }
            case "members" -> { if (plot != null) plugin.menus.openMembers(player, plot); }
            case "flags" -> { if (plot != null) plugin.menus.openFlags(player, plot); }
            case "upgrades" -> { if (plot != null) plugin.menus.openUpgrades(player, plot); }
            case "vault" -> { if (plot != null) plugin.menus.openVault(player, plot, 1); }
            case "market" -> plugin.menus.openMarket(player, 0);
            case "blackmarket" -> plugin.menus.openBlackmarket(player, 0);
            case "browse" -> plugin.menus.openBrowse(player, 0);
            case "cosmetics" -> { if (plot != null) plugin.menus.openCosmetics(player, plot); }
            case "home" -> { player.closeInventory(); if (plot != null) plugin.teleportHome(player, plot); }
            case "drone" -> { player.closeInventory(); plugin.toggleDrone(player); }
            case "unclaim" -> {
                if (plot == null) return;
                if (event.isShiftClick()) {
                    player.closeInventory();
                    plugin.unclaim(player, plot, true);
                } else {
                    plugin.msg(player, plugin.cfg().getString("claiming.unclaim_confirm_message"));
                }
            }
            case "holoinfo" -> plugin.msg(player, "&aUse &2/plot holo create &ato buy a hologram.");
            case "mailboxinfo" -> plugin.msg(player, "&aLook at a barrel and type &2/plot setmailbox&a.");
            case "bm-sell" -> {
                player.closeInventory();
                plugin.session(player).priceEditPath = "blackmarket-sell";
                plugin.msg(player, "&aHold the item and type the price in chat.");
            }
            default -> handleDynamic(player, plot, holder, action, event);
        }
    }

    private void handleDynamic(Player player, Plot plot, GuiHolder holder, String action, InventoryClickEvent event) {
        if (action.startsWith("dep:") && plot != null) {
            double amount = "all".equals(action.substring(4)) ? plugin.economy.balance(player) : parse(action.substring(4));
            plugin.deposit(player, plot, amount);
            plugin.menus.openBank(player, plot);
        } else if (action.startsWith("wd:") && plot != null) {
            double amount = "all".equals(action.substring(3)) ? plot.bank : parse(action.substring(3));
            plugin.withdraw(player, plot, amount);
            plugin.menus.openBank(player, plot);
        } else if (action.startsWith("flag:") && plot != null && plot.canManage(player)) {
            PlotFlag flag = PlotFlag.from(action.substring(5));
            if (flag != null) {
                plot.setFlag(flag, !plot.flag(flag));
                plot.audit(player.getName(), "FLAG", flag.name() + "=" + plot.flag(flag));
                plugin.menus.openFlags(player, plot);
            }
        } else if (action.startsWith("buy:") && plot != null) {
            plugin.buyUpgrade(player, plot, action.substring(4));
            plugin.menus.openUpgrades(player, plot);
        } else if (action.startsWith("member:") && plot != null && plot.canManage(player)) {
            try {
                UUID id = UUID.fromString(action.substring(7));
                if (event.isShiftClick()) {
                    plot.members.remove(id);
                    plugin.msg(player, plugin.cfg().getString("members.member_remove_message").replace("%player%", Bukkit.getOfflinePlayer(id).getName()));
                } else {
                    var m = plot.members.get(id);
                    if (m != null) {
                        m.role = m.role.promote();
                        m.applyRoleDefaults();
                        plugin.msg(player, plugin.cfg().getString("members.promote_message").replace("%player%", m.name).replace("%role%", m.role.display));
                    }
                }
                plugin.menus.openMembers(player, plot);
            } catch (Exception ignored) {}
        } else if (action.startsWith("goto:")) {
            try {
                Plot p = plugin.store.get(UUID.fromString(action.substring(5)));
                player.closeInventory();
                if (p != null) plugin.startGps(player, p);
            } catch (Exception ignored) {}
        } else if (action.startsWith("visit:")) {
            try {
                Plot p = plugin.store.get(UUID.fromString(action.substring(6)));
                player.closeInventory();
                if (p == null) return;
                if (event.isRightClick()) plugin.startGps(player, p);
                else plugin.teleportHome(player, p);
            } catch (Exception ignored) {}
        } else if (action.startsWith("page:")) {
            int page = (int) parse(action.substring(5));
            if (holder.type == GuiHolder.Type.MARKET) plugin.menus.openMarket(player, page);
            else if (holder.type == GuiHolder.Type.BROWSE) plugin.menus.openBrowse(player, page);
            else if (holder.type == GuiHolder.Type.BLACKMARKET) plugin.menus.openBlackmarket(player, page);
        } else if (action.startsWith("bm:")) {
            handleBlackmarket(player, action.substring(3), event.isShiftClick());
        } else if (action.startsWith("price:")) {
            player.closeInventory();
            plugin.session(player).priceEditPath = action.substring(6);
            plugin.msg(player, "&aType the new price in chat.");
        } else if (action.startsWith("border:") && plot != null) {
            plugin.buyCosmetic(player, plot, "borders", action.substring(7));
            plugin.menus.openCosmetics(player, plot);
        } else if (action.startsWith("part:") && plot != null) {
            plugin.buyCosmetic(player, plot, "particles", action.substring(5));
            plugin.menus.openCosmetics(player, plot);
        }
    }

    private void handleBlackmarket(Player player, String id, boolean shift) {
        UUID uid;
        try { uid = UUID.fromString(id); } catch (Exception e) { return; }
        Iterator<BlackmarketListing> it = plugin.store.blackmarket.iterator();
        while (it.hasNext()) {
            BlackmarketListing l = it.next();
            if (!l.id.equals(uid)) continue;
            if (shift && l.seller.equals(player.getUniqueId())) {
                ItemStack item = Serial.itemFromBase64(l.itemBase64);
                if (item != null) player.getInventory().addItem(item);
                it.remove();
                plugin.msg(player, "&aListing cancelled.");
                plugin.menus.openBlackmarket(player, 0);
                return;
            }
            if (!plugin.economy.has(player, l.price)) {
                plugin.msg(player, plugin.cfg().getString("economy.not_enough_money_message").replace("%balance%", Text.money(plugin.economy.balance(player))));
                return;
            }
            plugin.economy.withdraw(player, l.price);
            if (l.seller != null) plugin.economy.deposit(Bukkit.getOfflinePlayer(l.seller), l.price);
            ItemStack item = Serial.itemFromBase64(l.itemBase64);
            if (item != null) player.getInventory().addItem(item);
            it.remove();
            Plot plot = plugin.store.get(l.plotId);
            if (plot != null) {
                plot.blackmarketUsed = true;
                plugin.maybeSnitch(plot);
            }
            plugin.msg(player, "&aPurchased anonymously for &2$" + Text.money(l.price));
            player.closeInventory();
            return;
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder holder && holder.type != GuiHolder.Type.VAULT) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) return;
        if (holder.type != GuiHolder.Type.VAULT) return;
        if (!(event.getPlayer() instanceof Player player)) return;
        Plot plot = plugin.store.get(holder.plotId);
        if (plot == null) return;
        plot.saveVaultPage(holder.page, event.getInventory());
    }

    private double parse(String s) {
        try { return Double.parseDouble(s); } catch (Exception e) { return 0; }
    }
}
