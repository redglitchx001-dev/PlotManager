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
