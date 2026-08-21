/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

public enum PlotRole {
    OWNER(4, "Owner"),
    CO_OWNER(3, "Co-Owner"),
    BUILDER(2, "Builder"),
    VISITOR(1, "Visitor");

    public final int rank;
    public final String display;

    PlotRole(int rank, String display) {
        this.rank = rank;
        this.display = display;
    }

    public boolean atLeast(PlotRole other) {
        return this.rank >= other.rank;
    }

    public PlotRole promote() {
        return switch (this) {
            case VISITOR -> BUILDER;
            case BUILDER -> CO_OWNER;
            default -> this;
        };
    }

    public PlotRole demote() {
        return switch (this) {
            case CO_OWNER -> BUILDER;
            case BUILDER -> VISITOR;
            default -> this;
        };
    }

    public static PlotRole from(String raw) {
        if (raw == null) return VISITOR;
        String n = raw.trim().toUpperCase().replace("-", "_").replace(" ", "_");
        try {
            return PlotRole.valueOf(n);
        } catch (IllegalArgumentException e) {
            if (n.contains("CO")) return CO_OWNER;
            if (n.contains("BUILD")) return BUILDER;
            if (n.contains("OWN")) return OWNER;
            return VISITOR;
        }
    }
}
