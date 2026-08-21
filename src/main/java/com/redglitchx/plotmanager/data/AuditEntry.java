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
