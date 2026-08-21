/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.util;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public final class Serial {
    private Serial() {}

    public static String inventoryToBase64(Inventory inventory) {
        if (inventory == null) return "";
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeInt(inventory.getSize());
            for (int i = 0; i < inventory.getSize(); i++) {
                oos.writeObject(inventory.getItem(i));
            }
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack[] inventoryFromBase64(String data) {
        if (data == null || data.isEmpty()) return new ItemStack[0];
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            int size = ois.readInt();
            ItemStack[] items = new ItemStack[size];
            for (int i = 0; i < size; i++) {
                items[i] = (ItemStack) ois.readObject();
            }
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    public static String itemToBase64(ItemStack item) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(bos)) {
            oos.writeObject(item);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static ItemStack itemFromBase64(String data) {
        if (data == null || data.isEmpty()) return null;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bis)) {
            return (ItemStack) ois.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isEmpty(ItemStack[] items) {
        if (items == null) return true;
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) return false;
        }
        return true;
    }
}
