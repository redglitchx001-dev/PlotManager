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
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.main.title")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(13, btn(mat("gui.info_item", Material.BOOK), plugin.lang.line(player, "gui.main.info-name"), plugin.lang.list(player, "gui.main.info-lore"), "info"));
        inv.setItem(20, btn(mat("gui.bank_item", Material.GOLD_INGOT), plugin.lang.line(player, "gui.main.bank-name"), plugin.lang.list(player, "gui.main.bank-lore"), "bank"));
        inv.setItem(21, btn(mat("gui.members_item", Material.PLAYER_HEAD), plugin.lang.line(player, "gui.main.members-name"), plugin.lang.list(player, "gui.main.members-lore"), "members"));
        inv.setItem(22, btn(mat("gui.flags_item", Material.OAK_SIGN), plugin.lang.line(player, "gui.main.flags-name"), plugin.lang.list(player, "gui.main.flags-lore"), "flags"));
        inv.setItem(23, btn(mat("gui.upgrades_item", Material.DIAMOND), plugin.lang.line(player, "gui.main.upgrades-name"), plugin.lang.list(player, "gui.main.upgrades-lore"), "upgrades"));
        inv.setItem(24, btn(mat("gui.vault_item", Material.ENDER_CHEST), plugin.lang.line(player, "gui.main.vault-name"), plugin.lang.list(player, "gui.main.vault-lore"), "vault"));
        inv.setItem(29, btn(Material.EMERALD, plugin.lang.line(player, "gui.main.market-name"), plugin.lang.list(player, "gui.main.market-lore"), "market"));
        inv.setItem(30, btn(Material.NETHERITE_SWORD, plugin.lang.line(player, "gui.main.blackmarket-name"), plugin.lang.list(player, "gui.main.blackmarket-lore"), "blackmarket"));
        inv.setItem(31, btn(mat("gui.home_item", Material.COMPASS), plugin.lang.line(player, "gui.main.home-name"), plugin.lang.list(player, "gui.main.home-lore"), "home"));
        inv.setItem(32, btn(Material.MAP, plugin.lang.line(player, "gui.main.browse-name"), plugin.lang.list(player, "gui.main.browse-lore"), "browse"));
        inv.setItem(33, btn(Material.JUKEBOX, plugin.lang.line(player, "gui.main.cosmetics-name"), plugin.lang.list(player, "gui.main.cosmetics-lore"), "cosmetics"));
        inv.setItem(38, btn(Material.NAME_TAG, plugin.lang.line(player, "gui.main.holo-name"), plugin.lang.list(player, "gui.main.holo-lore"), "holoinfo"));
        inv.setItem(39, btn(Material.BARREL, plugin.lang.line(player, "gui.main.mailbox-name"), plugin.lang.list(player, "gui.main.mailbox-lore"), "mailboxinfo"));
        inv.setItem(40, btn(Material.PHANTOM_MEMBRANE, plugin.lang.line(player, "gui.main.drone-name"), plugin.lang.list(player, "gui.main.drone-lore"), "drone"));
        inv.setItem(41, btn(mat("gui.unclaim_item", Material.TNT), plugin.lang.line(player, "gui.main.unclaim-name"), plugin.lang.list(player, "gui.main.unclaim-lore"), "unclaim"));
        inv.setItem(43, btn(mat("gui.language_item", Material.WRITABLE_BOOK), plugin.lang.line(player, "gui.main.lang-name"), plugin.lang.list(player, "gui.main.lang-lore"), "language"));
        inv.setItem(49, btn(mat("gui.close_button_item", Material.BARRIER), plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        player.openInventory(inv);
        plugin.fx(player, "gui_click");
    }

    public void openBank(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BANK, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 27, Text.component(plugin.lang.line(player, "gui.bank.title")));
        holder.inventory = inv;
        fillBorder(inv, 27);
        inv.setItem(11, btn(Material.GOLD_NUGGET, plugin.lang.line(player, "gui.bank.deposit-100"), plugin.lang.list(player, "gui.bank.click-deposit"), "dep:100"));
        inv.setItem(12, btn(Material.GOLD_INGOT, plugin.lang.line(player, "gui.bank.deposit-1000"), plugin.lang.list(player, "gui.bank.click-deposit"), "dep:1000"));
        inv.setItem(13, btn(Material.GOLD_BLOCK, plugin.lang.line(player, "gui.bank.deposit-all"), List.of(plugin.lang.line(player, "gui.bank.balance-lore", "%balance%", Text.money(plugin.economy.balance(player)))), "dep:all"));
        inv.setItem(14, btn(Material.IRON_INGOT, plugin.lang.line(player, "gui.bank.withdraw-1000"), List.of(plugin.lang.line(player, "gui.bank.bank-lore", "%balance%", Text.money(plot.bank))), "wd:1000"));
        inv.setItem(15, btn(Material.HOPPER, plugin.lang.line(player, "gui.bank.withdraw-all"), List.of(plugin.lang.line(player, "gui.bank.bank-lore", "%balance%", Text.money(plot.bank))), "wd:all"));
        inv.setItem(22, btn(Material.ARROW, plugin.lang.line(player, "gui.common.back"), List.of(), "back"));
        player.openInventory(inv);
    }

    public void openMembers(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.MEMBERS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.members.title")));
        holder.inventory = inv;
        fillBorder(inv);
        int slot = 10;
        ItemStack ownerHead = btn(Material.PLAYER_HEAD, plugin.lang.line(player, "gui.members.owner-name", "%player%", plot.ownerName), plugin.lang.list(player, "gui.members.owner-lore"), "noop");
        if (ownerHead.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(plot.owner));
            ownerHead.setItemMeta(meta);
        }
        inv.setItem(slot++, ownerHead);
        for (PlotMember m : plot.members.values()) {
            if (slot == 17 || slot == 26 || slot == 35) slot += 2;
            if (slot >= 44) break;
            inv.setItem(slot++, btn(Material.PLAYER_HEAD, "&a" + m.name,
                    List.of(plugin.lang.line(player, "gui.members.role-lore", "%role%", m.role.display),
                            plugin.lang.line(player, "gui.members.build-lore", "%value%", yn(player, m.canBuild)),
                            plugin.lang.line(player, "gui.members.chests-lore", "%value%", yn(player, m.canChests)),
                            plugin.lang.line(player, "gui.members.click-lore")),
                    "member:" + m.uuid));
        }
        inv.setItem(49, btn(Material.ARROW, plugin.lang.line(player, "gui.common.back"), List.of(), "back"));
        player.openInventory(inv);
    }

    public void openFlags(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.FLAGS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 27, Text.component(plugin.lang.line(player, "gui.flags.title")));
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
                    List.of(plugin.lang.line(player, "gui.flags.currently", "%value%", on ? plugin.lang.line(player, "gui.flags.on-state") : plugin.lang.line(player, "gui.flags.off-state")),
                            plugin.lang.line(player, "gui.flags.click-toggle")),
                    "flag:" + flag.name()));
        }
        inv.setItem(22, btn(Material.ARROW, plugin.lang.line(player, "gui.common.back"), List.of(), "back"));
        player.openInventory(inv);
    }

    public void openUpgrades(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.UPGRADES, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.upgrades.title")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(10, upgrade(player, Material.FEATHER, plugin.lang.line(player, "upgrades.names.fly"), "upgrades.fly", plot.flyUnlocked, "buy:fly"));
        inv.setItem(11, upgrade(player, Material.WHEAT, plugin.lang.line(player, "upgrades.names.crop_boost"), "upgrades.crop_boost", plot.cropBoost, "buy:crop"));
        inv.setItem(12, upgrade(player, Material.JUKEBOX, plugin.lang.line(player, "upgrades.names.music"), "upgrades.music", plot.musicUnlocked, "buy:music"));
        inv.setItem(13, upgrade(player, Material.HOPPER, plugin.lang.line(player, "upgrades.names.smart_sorter"), "upgrades.smart_sorter", plot.sorterUnlocked, "buy:sorter"));
        inv.setItem(14, upgrade(player, Material.BLAST_FURNACE, plugin.lang.line(player, "upgrades.names.factory"), "upgrades.factory", plot.factoryUnlocked, "buy:factory"));
        inv.setItem(15, upgrade(player, Material.CHEST, plugin.lang.line(player, "upgrades.names.vault_page", "%page%", String.valueOf(plot.vaultPages + 1)), "upgrades.vault_page",
                plot.vaultPages >= plugin.cfg().getInt("upgrades.vault_page.max_pages", 5), "buy:vault"));
        inv.setItem(19, upgrade(player, Material.IRON_BLOCK, plugin.lang.line(player, "upgrades.names.generator_tier_1"), "upgrades.generator_tier_1", false, "buy:gen1"));
        inv.setItem(20, upgrade(player, Material.GOLD_BLOCK, plugin.lang.line(player, "upgrades.names.generator_tier_2"), "upgrades.generator_tier_2", false, "buy:gen2"));
        inv.setItem(21, upgrade(player, Material.DIAMOND_BLOCK, plugin.lang.line(player, "upgrades.names.generator_tier_3"), "upgrades.generator_tier_3", false, "buy:gen3"));
        inv.setItem(49, btn(Material.ARROW, plugin.lang.line(player, "gui.common.back"), List.of(), "back"));
        player.openInventory(inv);
    }

    public void openVault(Player player, Plot plot, int page) {
        page = Math.max(1, Math.min(page, Math.max(1, plot.vaultPages)));
        int size = page <= 1 && plot.vaultPages == 1 ? 27 : 54;
        GuiHolder holder = new GuiHolder(GuiHolder.Type.VAULT, plot.id, page, null);
        String title = plugin.lang.line(player, "gui.vault.title", "%page%", String.valueOf(page));
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
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.market.title")));
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
            inv.setItem(slot++, btn(e.item, plugin.lang.line(player, "gui.market.entry-name", "%item%", e.item.name()),
                    List.of(plugin.lang.line(player, "gui.market.amount-lore", "%amount%", String.valueOf(e.amount)),
                            plugin.lang.line(player, "gui.market.price-lore", "%price%", Text.money(e.price)),
                            plugin.lang.line(player, "gui.market.plot-lore", "%plot%", e.plot.name),
                            plugin.lang.line(player, "gui.market.owner-lore", "%player%", e.plot.ownerName),
                            plugin.lang.line(player, "gui.market.gps-lore")),
                    "goto:" + e.plot.id));
        }
        inv.setItem(45, btn(Material.ARROW, plugin.lang.line(player, "gui.common.previous"), List.of(), "page:" + Math.max(0, page - 1)));
        inv.setItem(49, btn(Material.BARRIER, plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        inv.setItem(53, btn(Material.ARROW, plugin.lang.line(player, "gui.common.next"), List.of(), "page:" + (page + 1)));
        player.openInventory(inv);
    }

    public void openBlackmarket(Player player, int page) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BLACKMARKET, null, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.blackmarket.title")));
        holder.inventory = inv;
        fillBorder(inv);
        plugin.lang.msg(player, "blackmarket.player-warning");
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
            lore.add(plugin.lang.line(player, "gui.blackmarket.anonymous"));
            lore.add(plugin.lang.line(player, "gui.blackmarket.price-lore", "%price%", Text.money(l.price)));
            lore.add(plugin.lang.line(player, "gui.blackmarket.buy-lore"));
            inv.setItem(slot++, Items.tagged(Items.named(icon.getType(), "&8" + icon.getType().name(), lore), actionKey, "bm:" + l.id));
        }
        inv.setItem(49, btn(Material.CHEST, plugin.lang.line(player, "gui.blackmarket.sell-name"), plugin.lang.list(player, "gui.blackmarket.sell-lore"), "bm-sell"));
        player.openInventory(inv);
    }

    public void openBrowse(Player player, int page) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.BROWSE, null, page, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.browse.title")));
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
            inv.setItem(slot++, btn(Material.GRASS_BLOCK, plugin.lang.line(player, "gui.browse.entry-name", "%plot%", p.name),
                    List.of(plugin.lang.line(player, "gui.browse.owner-lore", "%player%", p.ownerName),
                            plugin.lang.line(player, "gui.browse.level-lore", "%level%", String.valueOf(p.level)),
                            plugin.lang.line(player, "gui.browse.size-lore", "%size%", p.sizeLabel()),
                            plugin.lang.line(player, "gui.browse.visit-lore"),
                            plugin.lang.line(player, "gui.browse.gps-lore")),
                    "visit:" + p.id));
        }
        inv.setItem(45, btn(Material.ARROW, plugin.lang.line(player, "gui.common.previous"), List.of(), "page:" + Math.max(0, page - 1)));
        inv.setItem(49, btn(Material.BARRIER, plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        inv.setItem(53, btn(Material.ARROW, plugin.lang.line(player, "gui.common.next"), List.of(), "page:" + (page + 1)));
        player.openInventory(inv);
    }

    public void openHelp(Player player) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.HELP, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.help.title")));
        holder.inventory = inv;
        fillBorder(inv);
        String[][] cmds = {
                {"/plot claim", plugin.lang.line(player, "gui.help.claim")},
                {"/plot menu", plugin.lang.line(player, "gui.help.menu")},
                {"/plot home", plugin.lang.line(player, "gui.help.home")},
                {"/plot add <player>", plugin.lang.line(player, "gui.help.add")},
                {"/plot vault", plugin.lang.line(player, "gui.help.vault")},
                {"/plot market", plugin.lang.line(player, "gui.help.market")},
                {"/plot browse", plugin.lang.line(player, "gui.help.browse")},
                {"/plot drone", plugin.lang.line(player, "gui.help.drone")},
                {"/plot fly", plugin.lang.line(player, "gui.help.fly")},
                {"/plot holo create", plugin.lang.line(player, "gui.help.holo")},
                {"/plot setmailbox", plugin.lang.line(player, "gui.help.setmailbox")},
                {"/plot blackmarket", plugin.lang.line(player, "gui.help.blackmarket")},
                {"/plot lang <code>", plugin.lang.line(player, "gui.help.language")}
        };
        int slot = 10;
        for (String[] c : cmds) {
            if (slot % 9 == 8) slot += 2;
            inv.setItem(slot++, btn(Material.PAPER, "&a" + c[0], List.of("&7" + c[1]), "noop"));
        }
        inv.setItem(49, btn(Material.BARRIER, plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        player.openInventory(inv);
    }

    public void openAudit(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.AUDIT, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.audit.title")));
        holder.inventory = inv;
        fillBorder(inv);
        int slot = 10;
        for (var e : plot.audit) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            inv.setItem(slot++, btn(Material.PAPER, "&a" + e.action,
                    List.of(plugin.lang.line(player, "gui.audit.by-lore", "%player%", e.actor),
                            "&7" + e.details,
                            "&8" + Text.formatDate(e.time, plugin.cfg().getString("plugin.date_format"), plugin.cfg().getString("plugin.timezone"))),
                    "noop"));
        }
        inv.setItem(49, btn(Material.ARROW, plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        player.openInventory(inv);
    }

    public void openPrices(Player player) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.PRICES, null, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.prices.title")));
        holder.inventory = inv;
        fillBorder(inv);
        inv.setItem(10, priceBtn(player, plugin.lang.line(player, "gui.prices.claim"), "economy.claim_cost", Material.GRASS_BLOCK));
        inv.setItem(11, priceBtn(player, plugin.lang.line(player, "gui.prices.premium"), "economy.premium_claim_cost", Material.GOLD_BLOCK));
        inv.setItem(12, priceBtn(player, plugin.lang.line(player, "upgrades.names.fly"), "upgrades.fly.cost", Material.FEATHER));
        inv.setItem(13, priceBtn(player, plugin.lang.line(player, "upgrades.names.crop_boost"), "upgrades.crop_boost.cost", Material.WHEAT));
        inv.setItem(14, priceBtn(player, plugin.lang.line(player, "upgrades.names.vault_page", "%page%", "N"), "upgrades.vault_page.cost_per_page", Material.CHEST));
        inv.setItem(15, priceBtn(player, plugin.lang.line(player, "upgrades.names.generator_tier_1"), "upgrades.generator_tier_1.cost", Material.IRON_INGOT));
        inv.setItem(16, priceBtn(player, plugin.lang.line(player, "upgrades.names.generator_tier_2"), "upgrades.generator_tier_2.cost", Material.GOLD_INGOT));
        inv.setItem(19, priceBtn(player, plugin.lang.line(player, "upgrades.names.generator_tier_3"), "upgrades.generator_tier_3.cost", Material.DIAMOND));
        inv.setItem(20, priceBtn(player, plugin.lang.line(player, "upgrades.names.music"), "upgrades.music.cost", Material.JUKEBOX));
        inv.setItem(21, priceBtn(player, plugin.lang.line(player, "gui.prices.hologram"), "holograms.cost_per_hologram", Material.NAME_TAG));
        inv.setItem(22, priceBtn(player, plugin.lang.line(player, "gui.prices.tip-minimum"), "economy.tip_jar_minimum", Material.GOLD_NUGGET));
        inv.setItem(49, btn(Material.BARRIER, plugin.lang.line(player, "gui.common.close"), List.of(), "close"));
        player.openInventory(inv);
    }

    public void openCosmetics(Player player, Plot plot) {
        GuiHolder holder = new GuiHolder(GuiHolder.Type.COSMETICS, plot.id, 0, null);
        Inventory inv = Bukkit.createInventory(holder, 54, Text.component(plugin.lang.line(player, "gui.cosmetics.title")));
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
                inv.setItem(slot++, btn(mat, name, List.of(plugin.lang.line(player, "gui.cosmetics.price-lore", "%price%", Text.money(price)), owned ? plugin.lang.line(player, "gui.common.selected") : plugin.lang.line(player, "gui.common.click-select")), "border:" + k));
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
                inv.setItem(slot++, btn(Material.BLAZE_POWDER, name, List.of(plugin.lang.line(player, "gui.cosmetics.price-lore", "%price%", Text.money(price)), owned ? plugin.lang.line(player, "gui.common.selected") : plugin.lang.line(player, "gui.common.click-select")), "part:" + k));
            }
        }
        inv.setItem(49, btn(Material.ARROW, plugin.lang.line(player, "gui.common.back"), List.of(), "back"));
        player.openInventory(inv);
    }

    private ItemStack priceBtn(Player viewer, String name, String path, Material mat) {
        double v = plugin.cfg().getDouble(path);
        return btn(mat, "&6" + name, List.of(plugin.lang.line(viewer, "gui.prices.current-lore", "%price%", Text.money(v)), plugin.lang.line(viewer, "gui.prices.click-lore")), "price:" + path);
    }

    private ItemStack upgrade(Player viewer, Material mat, String name, String path, boolean owned, String action) {
        double cost = plugin.cfg().getDouble(path + ".cost", plugin.cfg().getDouble(path + ".cost_per_page", 0));
        String desc = plugin.cfg().getString(path + ".description", "");
        List<String> lore = new ArrayList<>();
        if (!desc.isEmpty()) lore.add("&7" + desc);
        lore.add(owned ? plugin.lang.line(viewer, "gui.upgrades.unlocked") : plugin.lang.line(viewer, "gui.upgrades.cost-lore", "%cost%", Text.money(cost)));
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

    private String yn(Player viewer, boolean v) {
        return v ? plugin.lang.line(viewer, "gui.common.label-yes") : plugin.lang.line(viewer, "gui.common.label-no");
    }

    private record MarketEntry(Plot plot, Material item, int amount, double price, int x, int y, int z) {}
}
