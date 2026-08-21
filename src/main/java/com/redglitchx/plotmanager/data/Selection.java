package com.redglitchx.plotmanager.data;

import org.bukkit.Location;

public class Selection {
    public Location pos1;
    public Location pos2;

    public boolean complete() {
        return pos1 != null && pos2 != null && pos1.getWorld() != null && pos2.getWorld() != null
                && pos1.getWorld().equals(pos2.getWorld());
    }
}
