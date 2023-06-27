package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.ftps.message.ProtocolUploadCommand;
import com.mobichord.ftps.message.ProtocolUploadRequest;
import com.mobichord.ftps.message.ProtocolUploadResponse;
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
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mobichord.messaging.SignalHandler.asList;

@Service
@Slf4j
public class ProtocolUploadSaga extends AbstractFtpsSaga {

    protected final Sender sender;

    protected final ServiceNowClient serviceNowClient;

    public ProtocolUploadSaga(Sender sender, ServiceNowClient serviceNowClient) {
        this.sender = sender;
        this.serviceNowClient = serviceNowClient;
    }

    @SagaEntry
    private class ProtocolUploadCommandHandler extends SignalHandler<ProtocolUploadCommand> {

        @Override
        public void handle(ProtocolUploadCommand command) {
            log.debug("started ProtocolUploadCommandHandler::handle: {}", command);
            ProtocolUploadRequest request = ProtocolUploadRequest.builder()
                    .activityId(command.getActivityId())
                    .protocol(command.getProtocol())
                    .protocolCfgId(command.getProtocolCfgId())
                    .attachmentIds(command.getAttachmentIds())
                    .path(command.getPath())
                    .encrypt(command.isEncrypt())
                    .overwrite(command.isOverwrite())
                    .encryptedFileExt(command.getEncryptedFileExt())
                    .cryptoCfgId(command.getCryptoCfgId())
                    .build();

            sender.send(request);
        }
    }

    private class ProtocolUploadResponseHandler extends SignalHandler<ProtocolUploadResponse> {

        @Override
        public void handle(ProtocolUploadResponse response) {
            log.debug("started ProtocolUploadResponseHandler::handle: {}", response);

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
        return asList(new ProtocolUploadCommandHandler(), new ProtocolUploadResponseHandler());
    }

}
