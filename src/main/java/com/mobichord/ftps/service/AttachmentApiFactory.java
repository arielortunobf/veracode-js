package com.mobichord.ftps.service;

import com.mobichord.servicenow.api.http.AttachmentApi;
import com.mobichord.servicenow.api.http.ServiceNowClientConfiguration;

public interface AttachmentApiFactory {
    AttachmentApi create(ServiceNowClientConfiguration config);
}
