/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
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
