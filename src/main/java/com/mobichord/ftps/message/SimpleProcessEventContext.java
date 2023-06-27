package com.mobichord.ftps.message;

final class SimpleProcessEventContext implements ProcessEventContext {
    private final String activityId;
    private final String tenantKey;

    SimpleProcessEventContext(String activityId, String tenantKey) {
        this.activityId = activityId;
        this.tenantKey = tenantKey;
    }

    public String getActivityId() {
        return this.activityId;
    }

    public String getTenantKey() {
        return this.tenantKey;
    }
}

