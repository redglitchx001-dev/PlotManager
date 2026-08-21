/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import org.bukkit.Material;

public class ChestShop {
    public String world;
    public int x, y, z;
    public Material item = Material.STONE;
    public int amount = 1;
    public double price = 0;
    public boolean buy = true;

    public String key() {
        return world + ":" + x + ":" + y + ":" + z;
    }
}
