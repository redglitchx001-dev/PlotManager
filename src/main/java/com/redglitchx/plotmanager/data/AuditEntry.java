/*
 * PlotManager — The Ultimate Plot Management System
 * Copyright (c) 2026 RedGlitchX. All Rights Reserved.
 *
 * This file is proprietary and confidential. Unauthorised copying,
 * redistribution, modification or use of this file, via any medium,
 * is strictly prohibited. See the LICENSE file for the full terms.
 */
package com.redglitchx.plotmanager.data;

public class AuditEntry {
    public long time;
    public String actor;
    public String action;
    public String details;

    public AuditEntry() {}

    public AuditEntry(String actor, String action, String details) {
        this.time = System.currentTimeMillis();
        this.actor = actor;
        this.action = action;
        this.details = details;
    }
}
