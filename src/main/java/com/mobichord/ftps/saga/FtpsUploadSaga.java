package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.ftps.message.ftps.FtpsUploadCommand;
import com.mobichord.ftps.message.ftps.FtpsUploadRequest;
import com.mobichord.ftps.message.ftps.FtpsUploadResponse;
import com.mobichord.ftps.service.data.FeedbackErrorBody;
import com.mobichord.ftps.service.data.FeedbackSuccessBody;
import com.mobichord.ftps.service.data.RequestData;
import com.mobichord.ftps.service.snclient.ServiceNowClient;
import com.mobichord.messaging.SagaEntry;
import com.mobichord.messaging.Sender;
import com.mobichord.messaging.SignalHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mobichord.messaging.SignalHandler.asList;

@Service
@Slf4j
public class FtpsUploadSaga extends AbstractFtpsSaga {

    protected final Sender sender;

    protected final ServiceNowClient serviceNowClient;

    public FtpsUploadSaga(Sender sender, ServiceNowClient serviceNowClient) {
        this.sender = sender;
        this.serviceNowClient = serviceNowClient;
    }

    @SagaEntry
    private class FtpsUploadCommandHandler extends SignalHandler<FtpsUploadCommand> {

        @Override
        public void handle(FtpsUploadCommand command) {
            log.debug("started FtpsUploadCommandHandler::handle: {}", command);
            FtpsUploadRequest request = new FtpsUploadRequest();
            request.setActivityId(command.getActivityId());
//            request.setConfig(command.getConfig());
            request.setFtpsCfgId(command.getFtpsCfgId());
            request.setAttachmentIds(command.getAttachmentIds());
            request.setPath(command.getPath());
            request.setEncrypt(command.isEncrypt());
            request.setOverwrite(command.isOverwrite());
            request.setEncryptedFileExt(command.getEncryptedFileExt());
            request.setCryptoCfgId(command.getCryptoCfgId());

            sender.send(request);
        }
    }

    private class FtpsUploadResponseHandler extends SignalHandler<FtpsUploadResponse> {

        @Override
        public void handle(FtpsUploadResponse response) {
            log.debug("started FtpsUploadResponseHandler::handle: {}", response);

            if (response.isOk()) {
                RequestData requestData = new RequestData();
                requestData.setActivityId(response.getActivityId());
                requestData.setType(UPLOAD_STATUS_TYPE);
                FeedbackSuccessBody body = new FeedbackSuccessBody();
                body.setStatus(SUCCESS);
                requestData.setBody(body);
                serviceNowClient.post(requestData);
            } else {
                RequestData requestData = new RequestData();
                requestData.setActivityId(response.getActivityId());
                requestData.setType(UPLOAD_STATUS_TYPE);
                FeedbackErrorBody body = new FeedbackErrorBody();
                body.setStatus(FAILURE);
                body.setError(response.getMessage());
                requestData.setBody(body);
                serviceNowClient.post(requestData);
            }
        }
    }

    @Override
    public List<SignalHandler<? extends Signal>> getHandlers() {
        return asList(new FtpsUploadCommandHandler(), new FtpsUploadResponseHandler());
    }

}
