package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.ftps.message.ProtocolDownloadCommand;
import com.mobichord.ftps.message.ProtocolDownloadRequest;
import com.mobichord.ftps.message.ProtocolDownloadResponse;
import com.mobichord.ftps.service.data.FeedbackErrorBody;
import com.mobichord.ftps.service.data.FeedbackSuccessBody;
import com.mobichord.ftps.service.data.RequestData;
import com.mobichord.messaging.SagaEntry;
import com.mobichord.messaging.SignalHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mobichord.messaging.SignalHandler.asList;

@Service
@Slf4j
public class ProtocolDownloadSaga extends AbstractProtocolSaga {

    @SagaEntry
    private class ProtocolDownloadCommandHandler extends SignalHandler<ProtocolDownloadCommand> {

        @Override
        public void handle(ProtocolDownloadCommand command) {
            log.debug("started ProtocolDownloadCommandHandler::handle: {}",command);

            ProtocolDownloadRequest request = ProtocolDownloadRequest.builder()
                    .activityId(command.getActivityId())
                    .protocolCfgId(command.getProtocolCfgId())
                    .targetTable(command.getTargetTable())
                    .targetSysId(command.getTargetSysId())
                    .decrypt(command.isDecrypt())
                    .cryptoCfgId(command.getCryptoCfgId())
                    .encryptionContext(command.getEncryptionContext())
                    .requestPath(command.getPath())
                    .protocol(command.getProtocol())
                    .fileMask(command.getFileMask())
                    .afterAction(command.getAfterAction())
                    .build();

            sender.send(request);
        }
    }

    private class ProtocolDownloadResponseHandler extends SignalHandler<ProtocolDownloadResponse> {

        @Override
        public void handle(ProtocolDownloadResponse response) {
            log.debug("started ProtocolDownloadResponseHandler::handle: {}",response);

            if (response.isOk()) {
                RequestData requestData = new RequestData();
                requestData.setActivityId(response.getActivityId());
                requestData.setType(DOWNLOAD_STATUS_TYPE);
                FeedbackSuccessBody body = new FeedbackSuccessBody();
                body.setStatus(SUCCESS);
                requestData.setBody(body);
                serviceNowClient.post(requestData);
            } else {
                RequestData requestData = new RequestData();
                requestData.setActivityId(response.getActivityId());
                requestData.setType(DOWNLOAD_STATUS_TYPE);
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
        return asList(new ProtocolDownloadCommandHandler(), new ProtocolDownloadResponseHandler());
    }

}
