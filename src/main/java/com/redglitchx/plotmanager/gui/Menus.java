package com.redglitchx.plotmanager.gui;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.BlackmarketListing;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import com.redglitchx.plotmanager.data.PlotMember;
import com.redglitchx.plotmanager.data.PlotRole;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Serial;
import com.redglitchx.plotmanager.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Menus {
    private final PlotManager plugin;
    public final NamespacedKey actionKey;

    public Menus(PlotManager plugin) {
        this.plugin = plugin;
        this.actionKey = new NamespacedKey(plugin, "gui-action");
    }

    public void openMain(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.MAIN, plot == null ? null : plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.main_menu_title", "&2&lPlot Menu")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(13, btn(mat("gui.info_item", Material.BOOK), "&aPlot Info", List.of("&7Click to view plot stats"), "info"));
        inv.setItem(20, btn(mat("gui.bank_item", Material.GOLD_INGOT), "&6Plot Bank", List.of("&7Deposit or withdraw"), "bank"));
        inv.setItem(21, btn(mat("gui.members_item", Material.PLAYER_HEAD), "&aMembers", List.of("&7Manage trusted players"), "members"));
        inv.setItem(22, btn(mat("gui.flags_item", Material.OAK_SIGN), "&aFlags", List.of("&7Toggle plot settings"), "flags"));
        inv.setItem(23, btn(mat("gui.upgrades_item", Material.DIAMOND), "&bUpgrades", List.of("&7Buy fly, vaults, generators"), "upgrades"));
        inv.setItem(24, btn(mat("gui.vault_item", Material.ENDER_CHEST), "&dPlot Vault", List.of("&7Open virtual storage"), "vault"));
        inv.setItem(29, btn(Material.EMERALD, "&aGlobal Market", List.of("&7Browse all chest shops"), "market"));
        inv.setItem(30, btn(Material.NETHERITE_SWORD, "&8Blackmarket", List.of("&7Shh... tax-free trades"), "blackmarket"));
        inv.setItem(31, btn(mat("gui.home_item", Material.COMPASS), "&aHome", List.of("&7Teleport to plot spawn"), "home"));
        inv.setItem(32, btn(Material.MAP, "&aBrowse Plots", List.of("&7Visit public plots"), "browse"));
        inv.setItem(33, btn(Material.JUKEBOX, "&dMusic & Cosmetics", List.of("&7Trails, borders, discs"), "cosmetics"));
        inv.setItem(38, btn(Material.NAME_TAG, "&aHolograms", List.of("&7/plot holo create"), "holoinfo"));
        inv.setItem(39, btn(Material.BARREL, "&6Mailbox", List.of("&7Look at a barrel: /plot setmailbox"), "mailboxinfo"));
        inv.setItem(40, btn(Material.PHANTOM_MEMBRANE, "&bDrone Mode", List.of("&7Aerial camera, survival only"), "drone"));
        inv.setItem(41, btn(mat("gui.unclaim_item", Material.TNT), "&cUnclaim", List.of("&cShift-click to unclaim"), "unclaim"));
        inv.setItem(49, btn(mat("gui.close_button_item", Material.BARRIER), plugin.cfg().getString("gui.close_button_name", "&cClose"), List.of(), "close"));
        player.openInventory(inv);
        plugin.fx(player, "gui_click");
    }

    public void openBank(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BANK, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 27, Text.component(plugin.cfg().getString("gui.bank_menu_title", "&2&lPlot Bank")));
        holder.inventory = inv;
        fillBorder(inv, 27);
        inv.setItem(11, btn(Material.GOLD_NUGGET, "&aDeposit 100", List.of("&7Click to deposit"), "dep:100"));
        inv.setItem(12, btn(Material.GOLD_INGOT, "&aDeposit 1000", List.of("&7Click to deposit"), "dep:1000"));
        inv.setItem(13, btn(Material.GOLD_BLOCK, "&aDeposit All", List.of("&7Balance: &f$" + Text.money(plugin.economy.balance(player))), "dep:all"));
        inv.setItem(14, btn(Material.IRON_INGOT, "&cWithdraw 1000", List.of("&7Plot bank: &f$" + Text.money(plot.bank)), "wd:1000"));
        inv.setItem(15, btn(Material.HOPPER, "&cWithdraw All", List.of("&7Plot bank: &f$" + Text.money(plot.bank)), "wd:all"));
        inv.setItem(22, btn(Material.ARROW, "&7Back", List.of(), "back"));
        player.openInventory(inv);
    }

    public void openMembers(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.MEMBERS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.members_menu_title", "&2&lPlot Members")));
        holder.inventory = inv;
        fillBorder(inv);
        int slot = 10;
        ItemStack ownerHead = btn(Material.PLAYER_HEAD, "&6Owner: &f" + plot.ownerName, List.of("&7Full control"), "noop");
        if (ownerHead.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(plot.owner));
            ownerHead.setItemMeta(meta);
        }
        inv.setItem(slot++, ownerHead);
        for (PlotMember m : plot.members.values()) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
            if (slot >= 44) break;
            inv.setItem(slot++, btn(Material.PLAYER_HEAD, "&a" + m.name,
                    List.of("&7Role: &f" + m.role.display,
                            "&7Build: " + yn(m.canBuild),
                            "&7Chests: " + yn(m.canChests),
                            "&eClick to promote  &cShift-click to kick"),
                    "member:" + m.uuid));
        }
        inv.setItem(49, btn(Material.ARROW, "&7Back", List.of(), "back"));
        player.openInventory(inv);
    }

    public void openFlags(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.FLAGS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 27, Text.component(plugin.cfg().getString("gui.flags_menu_title", "&2&lPlot Flags")));
        holder.inventory = inv;
        fillBorder(inv, 27);
        int slot = 10;
        for (PlotFlag flag : PlotFlag.values()) {
            if (slot == 17) slot = 19;
            if (slot > 16 && slot < 19) slot = 19;
            if (slot >= 22) break;
            boolean on = plot.flag(flag);
            inv.setItem(slot++, btn(on ? Material.LIME_DYE : Material.GRAY_DYE,
                    (on ? "&a" : "&7") + flag.name(),
                    List.of("&7Currently: " + (on ? "&aON" : "&cOFF"), "&eClick to toggle"),
                    "flag:" + flag.name()));
        }
        inv.setItem(22, btn(Material.ARROW, "&7Back", List.of(), "back"));
        player.openInventory(inv);
    }

    public void openUpgrades(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.UPGRADES, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.upgrades_menu_title", "&2&lPlot Upgrades")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(10, upgrade(Material.FEATHER, "Fly", "upgrades.fly", plot.flyUnlocked, "buy:fly"));
        inv.setItem(11, upgrade(Material.WHEAT, "Crop Boost", "upgrades.crop_boost", plot.cropBoost, "buy:crop"));
        inv.setItem(12, upgrade(Material.JUKEBOX, "Music", "upgrades.music", plot.musicUnlocked, "buy:music"));
        inv.setItem(13, upgrade(Material.HOPPER, "Smart Sorter", "upgrades.smart_sorter", plot.sorterUnlocked, "buy:sorter"));
        inv.setItem(14, upgrade(Material.BLAST_FURNACE, "Factory", "upgrades.factory", plot.factoryUnlocked, "buy:factory"));
        inv.setItem(15, upgrade(Material.CHEST, "Vault Page " + (plot.vaultPages + 1), "upgrades.vault_page",
                plot.vaultPages >= plugin.cfg().getInt("upgrades.vault_page.max_pages", 5), "buy:vault"));
        inv.setItem(19, upgrade(Material.IRON_BLOCK, "Iron Generator", "upgrades.generator_tier_1", false, "buy:gen1"));
        inv.setItem(20, upgrade(Material.GOLD_BLOCK, "Gold Generator", "upgrades.generator_tier_2", false, "buy:gen2"));
        inv.setItem(21, upgrade(Material.DIAMOND_BLOCK, "Diamond Generator", "upgrades.generator_tier_3", false, "buy:gen3"));
        inv.setItem(49, btn(Material.ARROW, "&7Back", List.of(), "back"));
        player.openInventory(inv);
    }

    public void openVault(Player player, Plot plot, int page) {
        page = Math.max(1, Math.min(page, Math.max(1, plot.vaultPages)));
        int size = page <= 1 && plot.vaultPages == 1 ? 27 : 54;
        GuiHolder holder = new GuiHolder(GuiHolder.Type.VAULT, plot.id, page, null);
        String title = plugin.cfg().getString("gui.vault_menu_title", "&2&lPlot Vault (Page %page%)").replace("%page%", String.valueOf(page));
        Inventory inv = Bukkit.createInventory(holder, size, Text.component(title));
        holder.inventory = inv;
        Inventory stored = plot.vaultPage(page);
        for (int i = 0; i < stored.getSize() && i < inv.getSize(); i++) {
            inv.setItem(i, stored.getItem(i));
        }
        player.openInventory(inv);
    }

    public void openMarket(Player player, int page) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.MARKET, null, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.market_menu_title", "&a&lGlobal Market")));
        holder.inventory = inv;
        fillBorder(inv);
        List<MarketEntry> entries = new ArrayList<>();
        for (Plot plot : plugin.store.plots.values()) {
            for (var shop : plot.shops) {
                entries.add(new MarketEntry(plot, shop.item, shop.amount, shop.price, shop.x, shop.y, shop.z));
            }
        }
        entries.sort((a, b) -> Double.compare(a.price, b.price));
        int start = page * 28;
        int slot = 10;
        for (int i = start; i < entries.size() && slot < 44; i++) {
            if (slot % 9 == 8) slot += 2;
            MarketEntry e = entries.get(i);
            inv.setItem(slot++, btn(e.item, "&a" + e.item.name(),
                    List.of("&7Amount: &f" + e.amount,
                            "&7Price: &a$" + Text.money(e.price),
                            "&7Plot: &f" + e.plot.name,
                            "&7Owner: &f" + e.plot.ownerName,
                            "&eClick to GPS navigate"),
                    "goto:" + e.plot.id));
        }
        inv.setItem(45, btn(Material.ARROW, "&7Previous", List.of(), "page:" + Math.max(0, page - 1)));
        inv.setItem(49, btn(Material.BARRIER, "&cClose", List.of(), "close"));
        inv.setItem(53, btn(Material.ARROW, "&7Next", List.of(), "page:" + (page + 1)));
        player.openInventory(inv);
    }

    public void openBlackmarket(Player player, int page) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BLACKMARKET, null, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.blackmarket_title", "&8&lThe Underworld")));
        holder.inventory = inv;
        fillBorder(inv);
        plugin.msg(player, plugin.cfg().getString("blackmarket.player_warning", "&8Shh..."));
        int slot = 10;
        int i = 0;
        for (BlackmarketListing l : plugin.store.blackmarket) {
            if (i++ < page * 28) continue;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            ItemStack item = Serial.itemFromBase64(l.itemBase64);
            if (item == null) continue;
            ItemStack icon = item.clone();
            List<String> lore = new ArrayList<>();
            lore.add("&8Anonymous listing");
            lore.add("&7Price: &a$" + Text.money(l.price));
            lore.add("&eClick to buy  &cShift-click to cancel if yours");
            inv.setItem(slot++, Items.tagged(Items.named(icon.getType(), "&8" + icon.getType().name(), lore), actionKey, "bm:" + l.id));
        }
        inv.setItem(49, btn(Material.CHEST, "&aSell held item", List.of("&7Type price in chat after clicking"), "bm-sell"));
        player.openInventory(inv);
    }

    public void openBrowse(Player player, int page) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BROWSE, null, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.browse_menu_title", "&2&lBrowse Public Plots")));
        holder.inventory = inv;
        fillBorder(inv);
        List<Plot> publicPlots = new ArrayList<>();
        for (Plot p : plugin.store.plots.values()) if (!p.hidden && !p.frozen) publicPlots.add(p);
        publicPlots.sort((a, b) -> Integer.compare(b.level, a.level));
        int slot = 10;
        int i = 0;
        for (Plot p : publicPlots) {
            if (i++ < page * 28) continue;
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            inv.setItem(slot++, btn(Material.GRASS_BLOCK, "&a" + p.name,
                    List.of("&7Owner: &f" + p.ownerName,
                            "&7Level: &f" + p.level,
                            "&7Size: &f" + p.sizeLabel(),
                            "&eLeft-click: visit",
                            "&aRight-click: GPS trail"),
                    "visit:" + p.id));
        }
        inv.setItem(45, btn(Material.ARROW, "&7Previous", List.of(), "page:" + Math.max(0, page - 1)));
        inv.setItem(49, btn(Material.BARRIER, "&cClose", List.of(), "close"));
        inv.setItem(53, btn(Material.ARROW, "&7Next", List.of(), "page:" + (page + 1)));
        player.openInventory(inv);
    }

    public void openHelp(Player player) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.HELP, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.help_menu_title", "&2&lPlot Help")));
        holder.inventory = inv;
        fillBorder(inv);
        String[][] cmds = {
                {"/plot claim", "Claim wand selection"},
                {"/plot menu", "Open this GUI"},
                {"/plot home", "Teleport home"},
                {"/plot add <player>", "Trust a player"},
                {"/plot vault", "Open plot vault"},
                {"/plot market", "Global shops"},
                {"/plot browse", "Visit public plots"},
                {"/plot drone", "Aerial camera mode"},
                {"/plot fly", "Toggle plot fly"},
                {"/plot holo create", "Buy a hologram"},
                {"/plot setmailbox", "Set a delivery barrel"},
                {"/plot blackmarket", "Tax-free market"}
        };
        int slot = 10;
        for (String[] c : cmds) {
            if (slot % 9 == 8) slot += 2;
            inv.setItem(slot++, btn(Material.PAPER, "&a" + c[0], List.of("&7" + c[1]), "noop"));
        }
        inv.setItem(49, btn(Material.BARRIER, "&cClose", List.of(), "close"));
        player.openInventory(inv);
    }

    public void openAudit(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.AUDIT, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.audit_menu_title", "&c&lPlot Audit Log")));
        holder.inventory = inv;
        fillBorder(inv);
        int slot = 10;
        for (var e : plot.audit) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            inv.setItem(slot++, btn(Material.PAPER, "&a" + e.action,
                    List.of("&7By: &f" + e.actor,
                            "&7" + e.details,
                            "&8" + Text.formatDate(e.time, plugin.cfg().getString("plugin.date_format"), plugin.cfg().getString("plugin.timezone"))),
                    "noop"));
        }
        inv.setItem(49, btn(Material.ARROW, "&7Close", List.of(), "close"));
        player.openInventory(inv);
    }

    public void openPrices(Player player) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.PRICES, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.price_editor_title", "&6&lPrice Editor")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(10, priceBtn("Claim Cost", "economy.claim_cost", Material.GRASS_BLOCK));
        inv.setItem(11, priceBtn("Premium Claim", "economy.premium_claim_cost", Material.GOLD_BLOCK));
        inv.setItem(12, priceBtn("Fly", "upgrades.fly.cost", Material.FEATHER));
        inv.setItem(13, priceBtn("Crop Boost", "upgrades.crop_boost.cost", Material.WHEAT));
        inv.setItem(14, priceBtn("Vault Page", "upgrades.vault_page.cost_per_page", Material.CHEST));
        inv.setItem(15, priceBtn("Iron Gen", "upgrades.generator_tier_1.cost", Material.IRON_INGOT));
        inv.setItem(16, priceBtn("Gold Gen", "upgrades.generator_tier_2.cost", Material.GOLD_INGOT));
        inv.setItem(19, priceBtn("Diamond Gen", "upgrades.generator_tier_3.cost", Material.DIAMOND));
        inv.setItem(20, priceBtn("Music", "upgrades.music.cost", Material.JUKEBOX));
        inv.setItem(21, priceBtn("Hologram", "holograms.cost_per_hologram", Material.NAME_TAG));
        inv.setItem(22, priceBtn("Tip Minimum", "economy.tip_jar_minimum", Material.GOLD_NUGGET));
        inv.setItem(49, btn(Material.BARRIER, "&cClose", List.of(), "close"));
        player.openInventory(inv);
    }

    public void openCosmetics(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.COSMETICS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.cfg().getString("gui.cosmetics_menu_title", "&d&lPlot Cosmetics")));
        holder.inventory = inv;
        fillBorder(inv);
        ConfigurationSection borders = plugin.cfg().getConfigurationSection("cosmetics.borders");
        int slot = 10;
        if (borders != null) {
            for (String k : borders.getKeys(false)) {
                if (slot % 9 == 8) slot += 2;
                if (slot >= 28) break;
                Material mat = Items.material(borders.getString(k + ".material"), Material.STONE);
                String name = borders.getString(k + ".name", k);
                double price = borders.getDouble(k + ".price");
                boolean owned = k.equals(plot.borderCosmetic);
                inv.setItem(slot++, btn(mat, name, List.of("&7Price: &a$" + Text.money(price), owned ? "&aSELECTED" : "&eClick to buy/select"), "border:" + k));
            }
        }
        ConfigurationSection parts = plugin.cfg().getConfigurationSection("cosmetics.particles");
        slot = 28;
        if (parts != null) {
            for (String k : parts.getKeys(false)) {
                if (slot % 9 == 8) slot += 2;
                if (slot >= 44) break;
                String name = parts.getString(k + ".name", k);
                double price = parts.getDouble(k + ".price");
                boolean owned = k.equals(plot.particleCosmetic);
                inv.setItem(slot++, btn(Material.BLAZE_POWDER, name, List.of("&7Price: &a$" + Text.money(price), owned ? "&aSELECTED" : "&eClick to buy/select"), "part:" + k));
            }
        }
        inv.setItem(49, btn(Material.ARROW, "&7Back", List.of(), "back"));
        player.openInventory(inv);
    }

    private ItemStack priceBtn(String name, String path, Material mat) {
        double v = plugin.cfg().getDouble(path);
        return btn(mat, "&6" + name, List.of("&7Current: &a$" + Text.money(v), "&eClick then type a new price in chat"), "price:" + path);
    }

    private ItemStack upgrade(Material mat, String name, String path, boolean owned, String action) {
        double cost = plugin.cfg().getDouble(path + ".cost", plugin.cfg().getDouble(path + ".cost_per_page", 0));
        String desc = plugin.cfg().getString(path + ".description", "");
        List<String> lore = new ArrayList<>();
        if (!desc.isEmpty()) lore.add("&7" + desc);
        lore.add(owned ? "&aUNLOCKED" : "&7Cost: &a$" + Text.money(cost));
        ItemStack item = btn(mat, (owned ? "&a" : "&e") + name, lore, action);
        return owned ? Items.glow(item) : item;
    }

    private void fillBorder(Inventory inv) {
        fillBorder(inv, inv.getSize());
    }

    private void fillBorder(Inventory inv, int size) {
        Material border = Items.material(plugin.cfg().getString("gui.border_glass_color"), Material.BLACK_STAINED_GLASS_PANE);
        Material fill = Items.material(plugin.cfg().getString("gui.glass_pane_color"), Material.GREEN_STAINED_GLASS_PANE);
        ItemStack b = Items.pane(border, " ");
        ItemStack f = Items.pane(fill, " ");
        int rows = size / 9;
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            if (row == 0 || row == rows - 1 || col == 0 || col == 8) inv.setItem(i, b);
            else if (inv.getItem(i) == null) inv.setItem(i, f);
        }
    }

    private ItemStack btn(Material mat, String name, List<String> lore, String action) {
        return Items.tagged(Items.named(mat, name, lore), actionKey, action);
    }

    private Material mat(String path, Material fb) {
        return Items.material(plugin.cfg().getString(path), fb);
    }

    private String yn(boolean v) {
        return v ? "&aYes" : "&cNo";
    }

    private record MarketEntry(Plot plot, Material item, int amount, double price, int x, int y, int z) {}
}
