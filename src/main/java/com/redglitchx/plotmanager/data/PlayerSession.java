/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSession {
    public final UUID uuid;
    public final Selection selection = new Selection();
    public UUID lastPlot;
    public boolean adminSpy;
    public boolean plotChat;
    public boolean drone;
    public Location droneReturn;
    public GameMode droneGameMode;
    public boolean droneWasAllowFlight;
    public boolean droneWasFlying;
    public Location gpsTarget;
    public UUID gpsPlot;
    public long gpsStarted;
    public String priceEditPath;
    public UUID confirmUnclaim;
    public long combatUntil;
    public UUID combatWith;
    public long lastBouncer;
    public ItemStack[] droneInv;
    public String lang;
    public Map<String, Object> extra = new ConcurrentHashMap<>();

    public PlayerSession(UUID uuid) {
        this.uuid = uuid;
    }
}
