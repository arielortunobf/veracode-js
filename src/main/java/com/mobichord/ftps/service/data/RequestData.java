package com.mobichord.ftps.service.data;

import lombok.Data;

@Data
public class RequestData {

    private String type;
    private String activityId;
    private FeedbackBody body;
}


