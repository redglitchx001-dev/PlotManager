/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

import java.util.UUID;

public class PlotMember {
    public UUID uuid;
    public String name;
    public PlotRole role = PlotRole.VISITOR;
    public boolean canBuild = false;
    public boolean canChests = false;
    public boolean canInteract = true;
    public long addedAt = System.currentTimeMillis();

    public PlotMember() {}

    public PlotMember(UUID uuid, String name, PlotRole role) {
        this.uuid = uuid;
        this.name = name;
        this.role = role;
        applyRoleDefaults();
    }

    public void applyRoleDefaults() {
        switch (role) {
            case OWNER, CO_OWNER -> {
                canBuild = true;
                canChests = true;
                canInteract = true;
            }
            case BUILDER -> {
                canBuild = true;
                canChests = false;
                canInteract = true;
            }
            case VISITOR -> {
                canBuild = false;
                canChests = false;
                canInteract = true;
            }
        }
    }
}
