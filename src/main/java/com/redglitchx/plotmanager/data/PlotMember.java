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
