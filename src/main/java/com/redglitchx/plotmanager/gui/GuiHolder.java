/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class GuiHolder implements InventoryHolder {
    public final Type type;
    public final UUID plotId;
    public final int page;
    public final String extra;
    public Inventory inventory;

    public GuiHolder(Type type, UUID plotId, int page, String extra) {
        this.type = type;
        this.plotId = plotId;
        this.page = page;
        this.extra = extra;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public enum Type {
        MAIN, BANK, MEMBERS, FLAGS, UPGRADES, VAULT, MARKET, BLACKMARKET,
        AUDIT, PRICES, BROWSE, HELP, COSMETICS, CONFIRM
    }
}
