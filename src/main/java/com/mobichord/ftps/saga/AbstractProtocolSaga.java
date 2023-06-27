package com.mobichord.ftps.saga;

import com.mobichord.ftps.service.snclient.ServiceNowClient;
import com.mobichord.messaging.AbstractStatelessSaga;
import com.mobichord.messaging.Sender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractProtocolSaga extends AbstractStatelessSaga {

    @Autowired
    protected Sender sender;

    @Autowired
    protected ServiceNowClient serviceNowClient;

    public static final String UPLOAD_STATUS_TYPE = "com.mobichord.utils.fileservice.upload.status";
    public static final String DOWNLOAD_STATUS_TYPE = "com.mobichord.utils.fileservice.download.status";
    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

}
