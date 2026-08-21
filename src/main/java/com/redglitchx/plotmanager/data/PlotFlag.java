package com.redglitchx.plotmanager.data;

import java.util.Locale;

public enum PlotFlag {
    PVP(false),
    MOBS(true),
    FIRE(false),
    EXPLOSIONS(false),
    ENTRY(true),
    PVP_FRIENDLY(false),
    AUTOWIPE_SNOW(false),
    AUTOWIPE_GRASS(false),
    AUTOWIPE_WEEDS(false),
    REDSTONE(true),
    ANIMALS(true),
    VILLAGER_TRADES(false),
    MUSIC(true),
    PRIVATE(false);

    public final boolean defaultValue;

    PlotFlag(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public static PlotFlag from(String raw) {
        if (raw == null) return null;
        try {
            return PlotFlag.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
