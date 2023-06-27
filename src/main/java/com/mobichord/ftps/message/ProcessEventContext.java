package com.mobichord.ftps.message;

import java.util.HashMap;
import java.util.Map;

public interface ProcessEventContext {

    String getActivityId();

    String getTenantKey();

    default Map<String, String> ctx() {
        return new HashMap<String, String>() {
            {
                this.put("activityId", ProcessEventContext.this.getActivityId());
                this.put("tenantCode", ProcessEventContext.this.getTenantKey());
            }
        };
    }

    static ProcessEventContext simple(String activityId, String tenantKey) {
        return new SimpleProcessEventContext(activityId, tenantKey);
    }
}
