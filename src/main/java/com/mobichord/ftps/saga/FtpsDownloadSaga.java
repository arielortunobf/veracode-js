package com.mobichord.ftps.saga;

import com.appchord.messages.Signal;
import com.mobichord.ftps.message.ftps.FtpsDownloadCommand;
import com.mobichord.ftps.message.ftps.FtpsDownloadRequest;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
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
public class FtpsDownloadSaga extends AbstractFtpsSaga{

    @SagaEntry
    private class FtpsDownloadCommandHandler extends SignalHandler<FtpsDownloadCommand> {

        @Override
        public void handle(FtpsDownloadCommand command) {
            log.debug("started FtpsDownloadCommandHandler::handle: {}",command);
            FtpsDownloadRequest request = new FtpsDownloadRequest();
            request.setActivityId(command.getActivityId());
//            request.setConfig(command.getConfig());
            request.setFtpsCfgId(command.getFtpsCfgId());
            request.setRequest(command.getRequest());
            request.setTenantCode(command.getTenantCode());



            sender.send(request);
        }
    }

    private class FtpsDownloadResponseHandler extends SignalHandler<FtpsDownloadResponse> {

        @Override
        public void handle(FtpsDownloadResponse response) {
            log.debug("started FtpsDownloadResponseHandler::handle: {}",response);
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
        return asList(new FtpsDownloadCommandHandler(), new FtpsDownloadResponseHandler());
    }

}
