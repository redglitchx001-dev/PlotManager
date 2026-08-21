/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.command;

import com.redglitchx.plotmanager.PlotManager;
import com.redglitchx.plotmanager.data.CustomHologram;
import com.redglitchx.plotmanager.data.Plot;
import com.redglitchx.plotmanager.data.PlotFlag;
import com.redglitchx.plotmanager.data.PlotMember;
import com.redglitchx.plotmanager.data.PlotRole;
import com.redglitchx.plotmanager.util.Cuboid;
import com.redglitchx.plotmanager.util.Items;
import com.redglitchx.plotmanager.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Barrel;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class PlotCommand implements CommandExecutor, TabCompleter {
    private static final List<String> PLAYER_SUBS = List.of(
            "help", "menu", "wand", "claim", "unclaim", "info", "home", "sethome",
            "add", "remove", "trust", "untrust", "promote", "demote", "members",
            "flag", "deposit", "withdraw", "ban", "unban", "description", "list",
            "visit", "merge", "vault", "market", "blackmarket", "map", "music",
            "holo", "holomove", "setmailbox", "private", "browse", "maplink",
            "fly", "upgrades", "rename", "chat", "drone", "cosmetics", "tax", "lang", "hooks"
    );
    private static final List<String> ADMIN_SUBS = List.of(
            "reload", "delete", "freeze", "unfreeze", "purge", "inspect", "seize",
            "godwand", "rollbackwand", "adminspy", "audit", "settop", "editprices"
    );

    private final PlotManager plugin;

    public PlotCommand(PlotManager plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player p) {
                Plot plot = plugin.here(p);
                plugin.menus.openMain(p, plot);
            } else {
                sender.sendMessage("PlotManager — use /plot help");
            }
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("hooks") || sub.equals("diagnose") || sub.equals("status")) {
            for (String line : plugin.hooksReport()) sender.sendMessage(Text.component("&a" + line));
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("plotmanager.admin")) return deny(sender);
            plugin.reloadAll();
            plugin.lang.msg(sender, "general.config-reloaded");
            return true;
        }
        if (!(sender instanceof Player player)) {
            plugin.lang.msg(sender, "general.players-only");
            return true;
        }
        if (!player.hasPermission("plotmanager.use") && !player.hasPermission("plotmanager.admin")) return deny(player);
        return switch (sub) {
            case "help" -> { plugin.menus.openHelp(player); yield true; }
            case "menu" -> { plugin.menus.openMain(player, plugin.here(player)); yield true; }
            case "wand" -> wand(player);
            case "claim" -> claim(player);
            case "unclaim" -> unclaim(player, args);
            case "info" -> { plugin.sendInfo(player, plugin.here(player)); yield true; }
            case "home" -> home(player, args);
            case "sethome" -> sethome(player);
            case "add", "trust" -> add(player, args);
            case "remove", "untrust" -> remove(player, args);
            case "promote" -> promote(player, args, true);
            case "demote" -> promote(player, args, false);
            case "members" -> members(player);
            case "flag" -> flag(player, args);
            case "deposit" -> money(player, args, true);
            case "withdraw" -> money(player, args, false);
            case "ban" -> ban(player, args, true);
            case "unban" -> ban(player, args, false);
            case "description", "desc" -> desc(player, args);
            case "list" -> list(player);
            case "visit" -> visit(player, args);
            case "merge" -> merge(player);
            case "vault" -> vault(player);
            case "market" -> { plugin.menus.openMarket(player, 0); yield true; }
            case "blackmarket" -> { plugin.menus.openBlackmarket(player, 0); yield true; }
            case "map" -> map(player);
            case "music" -> music(player, args);
            case "holo" -> holo(player, args);
            case "holomove" -> holomove(player);
            case "setmailbox" -> mailbox(player);
            case "private" -> privacy(player);
            case "browse" -> { plugin.menus.openBrowse(player, 0); yield true; }
            case "maplink" -> maplink(player);
            case "fly" -> fly(player);
            case "upgrades" -> { Plot p = owned(player); if (p != null) plugin.menus.openUpgrades(player, p); yield true; }
            case "rename" -> rename(player, args);
            case "chat" -> chat(player);
            case "drone" -> { plugin.toggleDrone(player); yield true; }
            case "cosmetics" -> { Plot p = owned(player); if (p != null) plugin.menus.openCosmetics(player, p); yield true; }
            case "tax" -> tax(player, args);
            case "lang" -> language(player, args);
            case "delete" -> adminDelete(player, args);
            case "freeze" -> freeze(player, args, true);
            case "unfreeze" -> freeze(player, args, false);
            case "purge" -> purge(player, args);
            case "inspect" -> inspect(player);
            case "seize" -> seize(player);
            case "godwand" -> godwand(player);
            case "rollbackwand" -> rollbackwand(player);
            case "adminspy" -> spy(player);
            case "audit" -> audit(player);
            case "settop" -> settop(player);
            case "editprices" -> { if (!admin(player)) yield true; plugin.menus.openPrices(player); yield true; }
            default -> {
                plugin.lang.msg(player, "general.unknown-subcommand");
                yield true;
            }
        };
    }

    private boolean deny(CommandSender sender) {
        plugin.lang.msg(sender, "general.no-permission");
        return true;
    }

    private boolean admin(Player player) {
        if (player.hasPermission("plotmanager.admin")) return true;
        deny(player);
        return false;
    }

    private Plot owned(Player player) {
        Plot plot = plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.stand-in-plot");
            return null;
        }
        if (!plot.canManage(player)) {
            plugin.lang.msg(player, "general.cannot-manage");
            return null;
        }
        return plot;
    }

    private boolean wand(Player player) {
        if (!player.hasPermission("plotmanager.wand") && !player.hasPermission("plotmanager.admin")) return deny(player);
        Material mat = Items.material(plugin.cfg().getString("claiming.wand_item"), Material.GOLDEN_AXE);
        ItemStack item = Items.glow(Items.named(mat, plugin.cfg().getString("claiming.wand_name"), plugin.cfg().getStringList("claiming.wand_lore")));
        Items.byteTag(item, plugin.keys.wand);
        player.getInventory().addItem(item);
        plugin.lang.msg(player, "claiming.wand-given");
        return true;
    }

    private boolean claim(Player player) {
        if (!player.hasPermission("plotmanager.claim")) return deny(player);
        var sel = plugin.session(player).selection;
        if (!sel.complete()) {
            plugin.lang.msg(player, "claiming.no_selection_message");
            return true;
        }
        Cuboid cuboid = Cuboid.of(sel.pos1, sel.pos2);
        int min = plugin.cfg().getInt("claiming.min_claim_size_blocks", 10);
        int max = plugin.cfg().getInt("claiming.max_claim_size_blocks", 250);
        if (cuboid.sizeX() < min || cuboid.sizeZ() < min) {
            plugin.lang.msg(player, "claiming.too_small_message", "%min%", String.valueOf(min));
            return true;
        }
        if (cuboid.sizeX() > max || cuboid.sizeZ() > max) {
            plugin.lang.msg(player, "claiming.too_large_message", "%max%", String.valueOf(max));
            return true;
        }
        if (plugin.store.index.overlaps(cuboid, null)) {
            plugin.lang.msg(player, "claiming.overlap_message");
            return true;
        }
        int owned = plugin.store.ownedBy(player.getUniqueId()).size();
        int cap = plugin.luckPerms.maxPlots(player);
        if (owned >= cap) {
            plugin.lang.msg(player, "claiming.max_claims_message", "%owned%", String.valueOf(owned), "%max%", String.valueOf(cap));
            return true;
        }
        var tier = plugin.luckPerms.tierOf(player);
        double cost = tier != null ? tier.claimCost : plugin.cfg().getDouble("economy.claim_cost", 1000);
        if (!plugin.economy.charge(player, cost, plugin.lang.line(player, "claiming.claim_denied_message", "%cost%", Text.money(cost)))) {
            return true;
        }
        Plot plot = plugin.createPlot(player, cuboid, tier == null ? null : tier.id, cost);
        plugin.lang.msg(player, "claiming.claim_success_message", "%cost%", Text.money(cost));
        plugin.fx(player, "claim_success");
        plugin.discord.claim(player, plot, cost);
        return true;
    }

    private boolean unclaim(Player player, String[] args) {
        Plot plot = owned(player);
        if (plot == null) return true;
        boolean confirm = args.length > 1 && args[1].equalsIgnoreCase("confirm");
        plugin.unclaim(player, plot, confirm);
        return true;
    }

    private boolean home(Player player, String[] args) {
        Plot plot;
        if (args.length > 1) {
            List<Plot> owned = plugin.store.ownedBy(player.getUniqueId());
            try {
                int n = Integer.parseInt(args[1]);
                plot = n >= 1 && n <= owned.size() ? owned.get(n - 1) : null;
            } catch (Exception e) {
                plot = plugin.findByOwnerName(args[1]);
            }
        } else {
            List<Plot> owned = plugin.store.ownedBy(player.getUniqueId());
            plot = owned.isEmpty() ? plugin.here(player) : owned.get(0);
        }
        if (plot == null) {
            plugin.lang.msg(player, "general.plot-not-found");
            return true;
        }
        plugin.teleportHome(player, plot);
        return true;
    }

    private boolean sethome(Player player) {
        Plot plot = owned(player);
        if (plot == null) return true;
        plot.setHome(player.getLocation());
        plugin.lang.msg(player, "plot.home-set");
        return true;
    }

    private boolean add(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "members.usage-add");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.lang.msg(player, "general.player-not-found");
            return true;
        }
        int max = plugin.luckPerms.maxMembers(player);
        if (plot.memberCount() >= max) {
            plugin.lang.msg(player, "members.member_limit_message", "%current%", String.valueOf(plot.memberCount()), "%max%", String.valueOf(max));
            return true;
        }
        PlotRole role = PlotRole.BUILDER;
        boolean noChests = false;
        for (int i = 2; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("--no-chests")) noChests = true;
            else role = PlotRole.from(args[i]);
            if (role == PlotRole.OWNER) role = PlotRole.CO_OWNER;
        }
        plot.addMember(target, role, noChests);
        plugin.lang.msg(player, "members.member_add_message", "%player%", target.getName(), "%role%", role.display);
        plugin.lang.msg(target, "members.added-target", "%plot%", plot.name, "%role%", role.display);
        plugin.fx(player, "member_added");
        return true;
    }

    private boolean remove(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "members.usage-remove");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        Player target = Bukkit.getPlayerExact(args[1]);
        UUID id = target != null ? target.getUniqueId() : null;
        if (id == null) {
            for (PlotMember m : plot.members.values()) {
                if (m.name.equalsIgnoreCase(args[1])) { id = m.uuid; break; }
            }
        }
        if (id == null || !plot.members.containsKey(id)) {
            plugin.lang.msg(player, "members.not-a-member");
            return true;
        }
        plot.members.remove(id);
        plugin.lang.msg(player, "members.member_remove_message", "%player%", args[1]);
        plugin.fx(player, "member_removed");
        return true;
    }

    private boolean promote(Player player, String[] args, boolean up) {
        if (args.length < 2) {
            plugin.lang.msg(player, "members.usage-rank", "%sub%", up ? "promote" : "demote");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        PlotMember member = null;
        for (PlotMember m : plot.members.values()) {
            if (m.name.equalsIgnoreCase(args[1])) { member = m; break; }
        }
        if (member == null) {
            plugin.lang.msg(player, "members.member-not-found");
            return true;
        }
        member.role = up ? member.role.promote() : member.role.demote();
        member.applyRoleDefaults();
        plugin.lang.msg(player, up ? "members.promote_message" : "members.demote_message", "%player%", member.name, "%role%", member.role.display);
        return true;
    }

    private boolean members(Player player) {
        Plot plot = plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.stand-in-plot");
            return true;
        }
        plugin.menus.openMembers(player, plot);
        return true;
    }

    private boolean flag(Player player, String[] args) {
        if (args.length < 3) {
            plugin.lang.msg(player, "flags.usage");
            plugin.lang.msg(player, "flags.list", "%flags%", Arrays.stream(PlotFlag.values()).map(f -> f.name().toLowerCase()).collect(Collectors.joining(", ")));
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        PlotFlag flag = PlotFlag.from(args[1]);
        if (flag == null) {
            plugin.lang.msg(player, "flags.unknown");
            return true;
        }
        boolean val = args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("on") || args[2].equals("1");
        plot.setFlag(flag, val);
        plot.audit(player.getName(), "FLAG", flag.name() + "=" + val);
        plugin.lang.msg(player, "flags.set", "%flag%", flag.name().toLowerCase(), "%value%", String.valueOf(val));
        return true;
    }

    private boolean money(Player player, String[] args, boolean dep) {
        if (args.length < 2) {
            plugin.lang.msg(player, "bank.usage", "%sub%", dep ? "deposit" : "withdraw");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        double amount;
        try { amount = Double.parseDouble(args[1]); } catch (Exception e) {
            plugin.lang.msg(player, "general.invalid-amount");
            return true;
        }
        if (dep) plugin.deposit(player, plot, amount);
        else plugin.withdraw(player, plot, amount);
        return true;
    }

    private boolean ban(Player player, String[] args, boolean ban) {
        if (args.length < 2) {
            plugin.lang.msg(player, "general.usage-player", "%sub%", ban ? "ban" : "unban");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.lang.msg(player, "general.player-not-online");
            return true;
        }
        if (ban) {
            if (!plot.banned.contains(target.getUniqueId())) plot.banned.add(target.getUniqueId());
            plugin.lang.msg(player, "bouncer_shield.ban_message", "%player%", target.getName());
            if (plot.contains(target.getLocation())) {
                Location out = plot.cuboid().nearestOutside(target.getLocation(), 2);
                target.teleport(out);
            }
        } else {
            plot.banned.remove(target.getUniqueId());
            plugin.lang.msg(player, "bouncer_shield.unban_message", "%player%", target.getName());
        }
        return true;
    }

    private boolean desc(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "plot.description-usage");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        plot.description = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.holograms.spawnPlot(plot);
        plugin.lang.msg(player, "plot.description-updated");
        return true;
    }

    private boolean list(Player player) {
        List<Plot> owned = plugin.store.ownedBy(player.getUniqueId());
        if (owned.isEmpty()) {
            plugin.lang.msg(player, "plot.list-none");
            return true;
        }
        int i = 1;
        for (Plot p : owned) {
            plugin.lang.msg(player, "plot.list-entry", "%index%", String.valueOf(i++), "%name%", p.name, "%size%", p.sizeLabel(), "%level%", String.valueOf(p.level));
        }
        return true;
    }

    private boolean visit(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "visit.usage");
            return true;
        }
        Plot plot = plugin.findByOwnerName(args[1]);
        if (plot == null || plot.hidden) {
            plugin.lang.msg(player, "visit.none-public");
            return true;
        }
        plugin.teleportHome(player, plot);
        return true;
    }

    private boolean merge(Player player) {
        if (!plugin.cfg().getBoolean("claiming.allow_claim_merging", true)) {
            plugin.lang.msg(player, "merging.disabled");
            return true;
        }
        Plot here = plugin.here(player);
        if (here == null || !here.isOwner(player.getUniqueId())) {
            plugin.lang.msg(player, "merging.stand-in-own");
            return true;
        }
        Plot other = null;
        for (Plot p : plugin.store.ownedBy(player.getUniqueId())) {
            if (p.id.equals(here.id)) continue;
            if (p.cuboid().adjacent(here.cuboid())) { other = p; break; }
        }
        if (other == null) {
            plugin.lang.msg(player, "claiming.merge_fail_message");
            return true;
        }
        plugin.mergePlots(player, here, other);
        plugin.lang.msg(player, "claiming.merge_success_message");
        return true;
    }

    private boolean vault(Player player) {
        Plot plot = plugin.here(player);
        if (plot == null || !plot.canChests(player)) {
            plugin.lang.msg(player, "plot_protections.chest_denied_message");
            return true;
        }
        plugin.menus.openVault(player, plot, 1);
        return true;
    }

    private boolean map(Player player) {
        plugin.giveRadarMap(player);
        plugin.lang.msg(player, "map.given");
        return true;
    }

    private boolean music(Player player, String[] args) {
        Plot plot = owned(player);
        if (plot == null) return true;
        if (!plot.musicUnlocked) {
            plugin.lang.msg(player, "music.locked");
            return true;
        }
        if (args.length < 2) {
            plugin.lang.msg(player, "music.available", "%discs%", String.join(", ", plugin.cfg().getStringList("music.available_discs")));
            return true;
        }
        String disc = args[1].toUpperCase(Locale.ROOT);
        if (!disc.startsWith("MUSIC_DISC_")) disc = "MUSIC_DISC_" + disc;
        plot.musicDisc = disc;
        plugin.lang.msg(player, "music.music_set_message", "%disc%", disc);
        return true;
    }

    private boolean holo(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "holograms.usage");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        String act = args[1].toLowerCase(Locale.ROOT);
        switch (act) {
            case "create" -> plugin.createHologram(player, plot);
            case "addline" -> {
                if (args.length < 3) { plugin.lang.msg(player, "holograms.usage-addline"); return true; }
                plugin.addHoloLine(player, plot, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            case "removeline" -> {
                if (args.length < 3) { plugin.lang.msg(player, "holograms.usage-removeline"); return true; }
                try { plugin.removeHoloLine(player, plot, Integer.parseInt(args[2])); }
                catch (Exception e) { plugin.lang.msg(player, "general.invalid-number"); }
            }
            case "delete" -> plugin.deleteHologram(player, plot);
            default -> plugin.lang.msg(player, "holograms.unknown-action");
        }
        return true;
    }

    private boolean holomove(Player player) {
        Plot plot = owned(player);
        if (plot == null) return true;
        plot.setHologram(player.getLocation());
        plugin.holograms.spawnPlot(plot);
        plugin.lang.msg(player, "holograms.hologram_moved_message");
        return true;
    }

    private boolean mailbox(Player player) {
        Plot plot = owned(player);
        if (plot == null) return true;
        Block target = player.getTargetBlockExact(6);
        boolean holo = target == null || !(target.getState() instanceof Barrel);
        if (!holo) {
            plot.setMailbox(target.getLocation(), false);
            plugin.lang.msg(player, "mailbox.set_message");
        } else {
            plot.setMailbox(player.getLocation(), true);
            plugin.lang.msg(player, "mailbox.holographic-set");
        }
        return true;
    }

    private boolean privacy(Player player) {
        Plot plot = owned(player);
        if (plot == null) return true;
        plot.hidden = !plot.hidden;
        plot.setFlag(PlotFlag.PRIVATE, plot.hidden);
        plugin.lang.msg(player, plot.hidden ? "plot.hidden" : "plot.public");
        return true;
    }

    private boolean maplink(Player player) {
        if (!plugin.cfg().getBoolean("maplink.enabled", true)) {
            plugin.lang.msg(player, "map.link-disabled");
            return true;
        }
        String url = plugin.cfg().getString("maplink.url", "http://map.yourserver.com");
        String msg = plugin.cfg().getString("maplink.message", "%url%").replace("%url%", url);
        if (plugin.cfg().getBoolean("maplink.clickable", true)) {
            player.sendMessage(Text.clickableUrl(plugin.prefix() + msg, url, plugin.cfg().getString("maplink.hover_text", "Click")));
        } else {
            plugin.msg(player, msg);
        }
        return true;
    }

    private boolean fly(Player player) {
        Plot plot = plugin.here(player);
        if (!plugin.canFly(player, plot)) {
            plugin.lang.msg(player, "plot.fly-locked");
            return true;
        }
        boolean now = !player.getAllowFlight();
        player.setAllowFlight(now);
        if (!now) player.setFlying(false);
        plugin.lang.msg(player, now ? "plot.fly-enabled" : "plot.fly-disabled");
        return true;
    }

    private boolean rename(Player player, String[] args) {
        if (args.length < 2) {
            plugin.lang.msg(player, "plot.rename-usage");
            return true;
        }
        Plot plot = owned(player);
        if (plot == null) return true;
        plot.name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        plugin.holograms.spawnPlot(plot);
        plugin.blueMap.upsert(plot);
        plugin.lang.msg(player, "plot.renamed", "%name%", plot.name);
        return true;
    }

    private boolean chat(Player player) {
        var s = plugin.session(player);
        s.plotChat = !s.plotChat;
        plugin.lang.msg(player, s.plotChat ? "plot.chat-enabled" : "plot.chat-disabled");
        return true;
    }

    private boolean tax(Player player, String[] args) {
        if (!plugin.cfg().getBoolean("mayor.enabled", true)) {
            plugin.lang.msg(player, "mayor.disabled");
            return true;
        }
        Plot top = plugin.store.richest(1).stream().findFirst().orElse(null);
        if (top == null || !top.isOwner(player.getUniqueId())) {
            plugin.lang.msg(player, "mayor.only-mayor");
            return true;
        }
        if (args.length < 2) {
            plugin.lang.msg(player, "mayor.current-tax", "%percent%", String.valueOf(plugin.store.mayorTaxPercent));
            return true;
        }
        try {
            double pct = Math.max(0, Math.min(plugin.cfg().getDouble("mayor.max_tax_percent", 10), Double.parseDouble(args[1])));
            plugin.store.mayorTaxPercent = pct;
            plugin.store.mayorPlot = top.id;
            plugin.lang.msg(player, "mayor.tax_set_message", "%percent%", String.valueOf(pct));
        } catch (Exception e) {
            plugin.lang.msg(player, "mayor.invalid-percent");
        }
        return true;
    }

    private boolean adminDelete(Player player, String[] args) {
        if (!admin(player)) return true;
        Plot plot = args.length > 1 ? plugin.store.get(parseUuid(args[1])) : plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.plot-not-found");
            return true;
        }
        plugin.deletePlot(plot, true);
        plugin.lang.msg(player, "plot.deleted");
        return true;
    }

    private boolean freeze(Player player, String[] args, boolean freeze) {
        if (!admin(player)) return true;
        Plot plot = args.length > 1 ? plugin.store.get(parseUuid(args[1])) : plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.plot-not-found");
            return true;
        }
        plot.frozen = freeze;
        plugin.blueMap.upsert(plot);
        if (freeze) {
            plugin.fx(player, "quarantine");
            Location home = plot.cuboid().nearestOutside(plot.center(), 3);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (plot.contains(p.getLocation()) && !p.hasPermission("plotmanager.admin")) {
                    p.teleport(home);
                    plugin.lang.msg(p, "admin.quarantine_kick_message");
                }
            }
            plugin.lang.msg(player, "admin.quarantine_message");
        } else {
            plugin.lang.msg(player, "plot.unfrozen");
        }
        return true;
    }

    private boolean purge(Player player, String[] args) {
        if (!admin(player)) return true;
        int days = 7;
        if (args.length > 1) try { days = Integer.parseInt(args[1]); } catch (Exception ignored) {}
        int n = plugin.purgeInactive(days);
        plugin.lang.msg(player, "admin.purge_message", "%count%", String.valueOf(n));
        plugin.discord.purge(player.getName(), n);
        return true;
    }

    private boolean inspect(Player player) {
        if (!admin(player)) return true;
        Plot plot = plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.stand-in-plot");
            return true;
        }
        plugin.inspect(player, plot);
        return true;
    }

    private boolean seize(Player player) {
        if (!admin(player)) return true;
        Plot plot = plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.stand-in-plot");
            return true;
        }
        plugin.seize(player, plot);
        return true;
    }

    private boolean godwand(Player player) {
        if (!admin(player)) return true;
        Material mat = Items.material(plugin.cfg().getString("admin.godwand_item"), Material.STICK);
        ItemStack item = Items.glow(Items.named(mat, plugin.cfg().getString("admin.godwand_name"), plugin.cfg().getStringList("admin.godwand_lore")));
        Items.byteTag(item, plugin.keys.godWand);
        player.getInventory().addItem(item);
        plugin.lang.msg(player, "admin.godwand-given");
        return true;
    }

    private boolean rollbackwand(Player player) {
        if (!admin(player)) return true;
        Material mat = Items.material(plugin.cfg().getString("admin.rollbackwand_item"), Material.BLAZE_ROD);
        ItemStack item = Items.glow(Items.named(mat, plugin.cfg().getString("admin.rollbackwand_name"), plugin.cfg().getStringList("admin.rollbackwand_lore")));
        Items.byteTag(item, plugin.keys.rollbackWand);
        player.getInventory().addItem(item);
        plugin.lang.msg(player, "admin.rollbackwand-given");
        return true;
    }

    private boolean spy(Player player) {
        if (!admin(player)) return true;
        var s = plugin.session(player);
        s.adminSpy = !s.adminSpy;
        plugin.lang.msg(player, s.adminSpy ? "admin.spy_enabled_message" : "admin.spy_disabled_message");
        return true;
    }

    private boolean audit(Player player) {
        if (!admin(player)) return true;
        Plot plot = plugin.here(player);
        if (plot == null) {
            plugin.lang.msg(player, "general.stand-in-plot");
            return true;
        }
        plugin.menus.openAudit(player, plot);
        return true;
    }

    private boolean settop(Player player) {
        if (!admin(player)) return true;
        Location loc = player.getLocation();
        plugin.store.leaderboardWorld = loc.getWorld().getName();
        plugin.store.leaderboardX = loc.getX();
        plugin.store.leaderboardY = loc.getY();
        plugin.store.leaderboardZ = loc.getZ();
        plugin.holograms.spawnLeaderboard();
        plugin.lang.msg(player, "leaderboard.set_message");
        return true;
    }

    private boolean language(Player player, String[] args) {
        var codes = plugin.lang.available();
        String current = plugin.lang.codeFor(player);
        if (args.length < 2) {
            if (!current.equals(plugin.lang.defaultCode())) {
                plugin.lang.msg(player, "lang.current", "%code%", current, "%default%", plugin.lang.defaultCode());
            } else {
                plugin.lang.msg(player, "lang.current-server", "%code%", current);
            }
            plugin.lang.msg(player, "lang.usage", "%codes%", String.join(", ", codes));
            return true;
        }
        String code = args[1].toLowerCase(Locale.ROOT);
        if (code.equals("reset") || code.equals(plugin.lang.defaultCode())) {
            plugin.lang.setPlayerLanguage(player, null);
            plugin.lang.msg(player, "lang.reset", "%code%", plugin.lang.defaultCode());
            return true;
        }
        if (!plugin.lang.exists(code)) {
            plugin.lang.msg(player, "lang.usage", "%codes%", String.join(", ", codes));
            return true;
        }
        if (!plugin.cfg().getBoolean("language.per-player", true)) {
            plugin.lang.msg(player, "lang.no-permission");
            return true;
        }
        plugin.lang.setPlayerLanguage(player, code);
        plugin.lang.msg(player, "lang.changed", "%code%", code);
        return true;
    }

    private UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (Exception e) {
            for (Plot p : plugin.store.plots.values()) {
                if (p.id.toString().startsWith(s) || p.name.equalsIgnoreCase(s)) return p.id;
            }
            return null;
        }
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            List<String> all = new ArrayList<>(PLAYER_SUBS);
            if (sender.hasPermission("plotmanager.admin")) all.addAll(ADMIN_SUBS);
            StringUtil.copyPartialMatches(args[0], all, out);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "add", "remove", "trust", "untrust", "promote", "demote", "ban", "unban", "visit" -> {
                    return null; // players
                }
                case "flag" -> StringUtil.copyPartialMatches(args[1],
                        Arrays.stream(PlotFlag.values()).map(f -> f.name().toLowerCase()).toList(), out);
                case "holo" -> StringUtil.copyPartialMatches(args[1], List.of("create", "addline", "removeline", "delete"), out);
                case "music" -> StringUtil.copyPartialMatches(args[1], plugin.cfg().getStringList("music.available_discs"), out);
                case "lang" -> StringUtil.copyPartialMatches(args[1], plugin.lang.available(), out);
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("flag")) {
            StringUtil.copyPartialMatches(args[2], List.of("true", "false"), out);
        }
        return out;
    }
}
