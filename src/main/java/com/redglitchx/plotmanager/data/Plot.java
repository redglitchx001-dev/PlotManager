package com.redglitchx.plotmanager.data;

import com.redglitchx.plotmanager.util.Cuboid;
import com.redglitchx.plotmanager.util.Serial;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class Plot {
    public UUID id = UUID.randomUUID();
    public String name = "Unnamed Plot";
    public UUID owner;
    public String ownerName = "Unknown";
    public String world = "world";
    public int minX, minY, minZ, maxX, maxY, maxZ;
    public String description = "A survival plot.";
    public boolean frozen;
    public boolean hidden;
    public String premiumTier;
    public int level = 1;
    public long exp;
    public double bank;
    public int vaultPages = 1;
    public final List<String> vaultData = new CopyOnWriteArrayList<>();
    public final Map<UUID, PlotMember> members = new ConcurrentHashMap<>();
    public final List<UUID> banned = new CopyOnWriteArrayList<>();
    public final Map<PlotFlag, Boolean> flags = new ConcurrentHashMap<>();
    public final List<GeneratorInstance> generators = new CopyOnWriteArrayList<>();
    public final List<AuditEntry> audit = new CopyOnWriteArrayList<>();
    public final List<ChestShop> shops = new CopyOnWriteArrayList<>();
    public final List<CustomHologram> holograms = new CopyOnWriteArrayList<>();
    public final List<BlockRecord> history = new CopyOnWriteArrayList<>();
    public String musicDisc;
    public boolean flyUnlocked;
    public boolean cropBoost;
    public boolean musicUnlocked;
    public boolean sorterUnlocked;
    public boolean factoryUnlocked;
    public String borderCosmetic;
    public String particleCosmetic;
    public String mailboxWorld;
    public int mailboxX, mailboxY, mailboxZ;
    public boolean mailboxHolo;
    public String homeWorld;
    public double homeX, homeY, homeZ, homeYaw, homePitch;
    public String holoWorld;
    public double holoX, holoY, holoZ;
    public int hoppers;
    public int spawners;
    public long created = System.currentTimeMillis();
    public long lastOwnerLogin = System.currentTimeMillis();
    public long lastUpkeep = System.currentTimeMillis();
    public long lastWarning;
    public int visitorsOffline;
    public double tipsOffline;
    public double shopSalesOffline;
    public long generatorItemsOffline;
    public boolean blackmarketUsed;
    public double claimCostPaid;
    public String schematicFile;

    public Plot() {
        for (PlotFlag flag : PlotFlag.values()) {
            flags.put(flag, flag.defaultValue);
        }
    }

    public Cuboid cuboid() {
        return new Cuboid(world, minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void applyCuboid(Cuboid c) {
        this.world = c.world;
        this.minX = c.minX;
        this.minY = c.minY;
        this.minZ = c.minZ;
        this.maxX = c.maxX;
        this.maxY = c.maxY;
        this.maxZ = c.maxZ;
    }

    public boolean contains(Location loc) {
        return cuboid().contains(loc);
    }

    public boolean containsXZ(Location loc) {
        return cuboid().containsXZ(loc);
    }

    public World bukkitWorld() {
        return Bukkit.getWorld(world);
    }

    public Location center() {
        World w = bukkitWorld();
        return w == null ? null : cuboid().center(w);
    }

    public Location home() {
        World w = homeWorld == null ? bukkitWorld() : Bukkit.getWorld(homeWorld);
        if (w == null) return center();
        if (homeWorld == null) {
            Location c = center();
            if (c == null) return null;
            c.setY(Math.max(c.getY(), maxY + 1));
            return c;
        }
        return new Location(w, homeX, homeY, homeZ, (float) homeYaw, (float) homePitch);
    }

    public Location hologramLocation() {
        World w = holoWorld == null ? bukkitWorld() : Bukkit.getWorld(holoWorld);
        if (w == null) return null;
        if (holoWorld == null) {
            Location c = cuboid().center(w);
            c.setY(maxY + 3.0);
            return c;
        }
        return new Location(w, holoX, holoY, holoZ);
    }

    public Location mailboxLocation() {
        if (mailboxWorld == null) return null;
        World w = Bukkit.getWorld(mailboxWorld);
        if (w == null) return null;
        return new Location(w, mailboxX + 0.5, mailboxY + 0.5, mailboxZ + 0.5);
    }

    public void setHome(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        homeWorld = loc.getWorld().getName();
        homeX = loc.getX();
        homeY = loc.getY();
        homeZ = loc.getZ();
        homeYaw = loc.getYaw();
        homePitch = loc.getPitch();
    }

    public void setHologram(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        holoWorld = loc.getWorld().getName();
        holoX = loc.getX();
        holoY = loc.getY();
        holoZ = loc.getZ();
    }

    public void setMailbox(Location loc, boolean holo) {
        if (loc == null || loc.getWorld() == null) {
            mailboxWorld = null;
            return;
        }
        mailboxWorld = loc.getWorld().getName();
        mailboxX = loc.getBlockX();
        mailboxY = loc.getBlockY();
        mailboxZ = loc.getBlockZ();
        mailboxHolo = holo;
    }

    public PlotRole roleOf(UUID uuid) {
        if (uuid == null) return null;
        if (uuid.equals(owner)) return PlotRole.OWNER;
        PlotMember m = members.get(uuid);
        return m == null ? null : m.role;
    }

    public boolean isOwner(UUID uuid) {
        return uuid != null && uuid.equals(owner);
    }

    public boolean isMember(UUID uuid) {
        return isOwner(uuid) || members.containsKey(uuid);
    }

    public boolean isBanned(UUID uuid) {
        return uuid != null && banned.contains(uuid);
    }

    public boolean isTrusted(UUID uuid) {
        PlotRole r = roleOf(uuid);
        return r != null && r.atLeast(PlotRole.BUILDER);
    }

    public boolean canManage(Player player) {
        if (player == null) return false;
        if (player.hasPermission("plotmanager.admin") || player.hasPermission("plotmanager.bypass")) return true;
        PlotRole r = roleOf(player.getUniqueId());
        return r != null && r.atLeast(PlotRole.CO_OWNER);
    }

    public boolean canBuild(Player player) {
        if (player == null) return false;
        if (player.hasPermission("plotmanager.bypass") || player.hasPermission("plotmanager.admin")) return true;
        if (isOwner(player.getUniqueId())) return true;
        PlotMember m = members.get(player.getUniqueId());
        return m != null && m.canBuild;
    }

    public boolean canChests(Player player) {
        if (player == null) return false;
        if (player.hasPermission("plotmanager.bypass") || player.hasPermission("plotmanager.admin")) return true;
        if (isOwner(player.getUniqueId())) return true;
        PlotMember m = members.get(player.getUniqueId());
        return m != null && m.canChests;
    }

    public boolean canInteract(Player player) {
        if (player == null) return false;
        if (player.hasPermission("plotmanager.bypass") || player.hasPermission("plotmanager.admin")) return true;
        if (isOwner(player.getUniqueId())) return true;
        PlotMember m = members.get(player.getUniqueId());
        if (m == null) return flag(PlotFlag.ENTRY);
        return m.canInteract;
    }

    public boolean flag(PlotFlag flag) {
        return flags.getOrDefault(flag, flag.defaultValue);
    }

    public void setFlag(PlotFlag flag, boolean value) {
        flags.put(flag, value);
    }

    public int memberCount() {
        return members.size();
    }

    public void addMember(Player player, PlotRole role, boolean noChests) {
        PlotMember m = new PlotMember(player.getUniqueId(), player.getName(), role);
        if (noChests) m.canChests = false;
        members.put(player.getUniqueId(), m);
        audit(player.getName(), "MEMBER_ADD", role.display);
    }

    public void audit(String actor, String action, String details) {
        audit.add(0, new AuditEntry(actor, action, details));
        if (audit.size() > 200) {
            audit.subList(200, audit.size()).clear();
        }
    }

    public void history(Player player, String action, Material block, Location loc) {
        BlockRecord rec = new BlockRecord();
        rec.time = System.currentTimeMillis();
        rec.player = player == null ? "Unknown" : player.getName();
        rec.action = action;
        rec.block = block == null ? "AIR" : block.name();
        if (loc != null && loc.getWorld() != null) {
            rec.world = loc.getWorld().getName();
            rec.x = loc.getBlockX();
            rec.y = loc.getBlockY();
            rec.z = loc.getBlockZ();
        }
        history.add(0, rec);
        if (history.size() > 400) {
            history.subList(400, history.size()).clear();
        }
    }

    public BlockRecord findHistory(Location loc) {
        if (loc == null) return null;
        for (BlockRecord rec : history) {
            if (rec.x == loc.getBlockX() && rec.y == loc.getBlockY() && rec.z == loc.getBlockZ()
                    && rec.world != null && rec.world.equals(loc.getWorld().getName())) {
                return rec;
            }
        }
        return null;
    }

    public long expNeeded() {
        return (long) level * 100L;
    }

    public boolean addExp(long amount, int maxLevel) {
        if (level >= maxLevel) return false;
        exp += Math.max(0, amount);
        boolean leveled = false;
        while (level < maxLevel && exp >= expNeeded()) {
            exp -= expNeeded();
            level++;
            leveled = true;
        }
        return leveled;
    }

    public Inventory vaultPage(int page) {
        int size = page <= 1 ? 27 : 54;
        Inventory inv = Bukkit.createInventory(null, size, net.kyori.adventure.text.Component.text("Plot Vault"));
        int idx = page - 1;
        if (idx >= 0 && idx < vaultData.size()) {
            ItemStack[] items = Serial.inventoryFromBase64(vaultData.get(idx));
            for (int i = 0; i < items.length && i < inv.getSize(); i++) {
                inv.setItem(i, items[i]);
            }
        }
        return inv;
    }

    public void saveVaultPage(int page, Inventory inv) {
        while (vaultData.size() < page) vaultData.add("");
        vaultData.set(page - 1, Serial.inventoryToBase64(inv));
    }

    public boolean addToVault(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        ItemStack leftover = stack.clone();
        for (int page = 1; page <= Math.max(1, vaultPages); page++) {
            Inventory inv = vaultPage(page);
            Map<Integer, ItemStack> fail = inv.addItem(leftover);
            saveVaultPage(page, inv);
            if (fail.isEmpty()) return true;
            leftover = fail.values().iterator().next();
        }
        return false;
    }

    public ItemStack takeFromVault(Material material, int amount) {
        int remaining = amount;
        ItemStack result = new ItemStack(material, 0);
        for (int page = 1; page <= Math.max(1, vaultPages) && remaining > 0; page++) {
            Inventory inv = vaultPage(page);
            for (int i = 0; i < inv.getSize() && remaining > 0; i++) {
                ItemStack it = inv.getItem(i);
                if (it == null || it.getType() != material) continue;
                int take = Math.min(remaining, it.getAmount());
                it.setAmount(it.getAmount() - take);
                if (it.getAmount() <= 0) inv.setItem(i, null);
                remaining -= take;
                result.setAmount(result.getAmount() + take);
            }
            saveVaultPage(page, inv);
        }
        return result.getAmount() > 0 ? result : null;
    }

    public int countInVault(Material material) {
        int total = 0;
        for (int page = 1; page <= Math.max(1, vaultPages); page++) {
            Inventory inv = vaultPage(page);
            for (ItemStack it : inv.getContents()) {
                if (it != null && it.getType() == material) total += it.getAmount();
            }
        }
        return total;
    }

    public String sizeLabel() {
        return cuboid().sizeX() + "x" + cuboid().sizeZ();
    }

    public String flagsLabel() {
        List<String> on = new ArrayList<>();
        for (Map.Entry<PlotFlag, Boolean> e : flags.entrySet()) {
            if (Boolean.TRUE.equals(e.getValue())) on.add(e.getKey().name().toLowerCase());
        }
        return on.isEmpty() ? "none" : String.join(", ", on);
    }

    public Map<PlotFlag, Boolean> flagSnapshot() {
        return new EnumMap<>(flags);
    }
}
