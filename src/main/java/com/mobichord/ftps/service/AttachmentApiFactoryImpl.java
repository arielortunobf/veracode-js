package com.mobichord.ftps.service;

import com.mobichord.servicenow.api.http.AttachmentApi;
import com.mobichord.servicenow.api.http.AttachmentApiImpl;
import com.mobichord.servicenow.api.http.ServiceNowClientConfiguration;
import com.mobichord.servicenow.api.http.ServiceNowHttpClient;
import org.springframework.stereotype.Component;

@Component
public class AttachmentApiFactoryImpl implements AttachmentApiFactory {

    @Override
    public AttachmentApi create(ServiceNowClientConfiguration config) {
        ServiceNowHttpClient client = new ServiceNowHttpClient(config);
        return new AttachmentApiImpl(client);
    }
}
