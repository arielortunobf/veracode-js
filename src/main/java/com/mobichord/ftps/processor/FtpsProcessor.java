package com.mobichord.ftps.processor;

import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.appchord.messages.Signal;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ftps.FtpsDownloadRequest;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
import com.mobichord.ftps.message.ftps.FtpsUploadRequest;
import com.mobichord.ftps.message.ftps.FtpsUploadResponse;
import com.mobichord.ftps.service.AttachmentApiFactory;
import com.mobichord.ftps.service.ClientFtpsService;
import com.mobichord.ftps.service.EncryptionService;
import com.mobichord.ftps.utility.FtpsUtils;
import com.mobichord.ftps.utility.PgpEncryptor;
import com.mobichord.messaging.Processor;
import com.mobichord.messaging.Sender;
import com.mobichord.messaging.SignalHandler;
import com.mobichord.okhttp.LoggingInterceptor;
import com.mobichord.servicenow.api.http.AttachmentApi;
import com.mobichord.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.mobichord.messaging.SignalHandler.asList;

@Service
@Slf4j
public class FtpsProcessor implements Processor {

    private final ClientFtpsService clientFtpsService;

    private final TenantService tenantService;

    private final Sender sender;

    private final AttachmentApiFactory apiFactory;

    private final EncryptionService encryptionService;

    public FtpsProcessor(ClientFtpsService clientFtpsService, TenantService tenantService, Sender sender,
                         AttachmentApiFactory apiFactory, EncryptionService encryptionService) {
        this.clientFtpsService = clientFtpsService;
        this.tenantService = tenantService;
        this.sender = sender;
        this.apiFactory = apiFactory;
        this.encryptionService = encryptionService;
    }

    public class FtpsUploadRequestHandler extends SignalHandler<FtpsUploadRequest> {

        @Override
        public void handle(FtpsUploadRequest request) {
            log.debug("started FtpsUploadRequestHandler::handle: {}", request);
            FtpsUploadResponse response = copyAttachments(request);
            response.setActivityId(request.getActivityId());
            sender.send(response);
        }
    }

    public class FtpsDownloadRequestHandler extends SignalHandler<FtpsDownloadRequest> {

        @Override
        public void handle(FtpsDownloadRequest request) {
            log.debug("started FtpsDownloadRequestHandler::handle: {}", request);
//            SftpConfiguration configuration = getFtpsConfiguration(request.getConfig());

            FtpsDownloadResponse response;
            try {
                response = clientFtpsService.downloadFileForClient(request.getRequest());
            } catch (FileServiceTransferException e) {
                response = new FtpsDownloadResponse();
                response.setOk(false);
                response.setMessage(e.getMessage());
            }

            sender.send(response);
        }
    }

    FtpsUploadResponse copyAttachments(FtpsUploadRequest request) {
        FtpsUploadResponse ftpsUploadResponse = new FtpsUploadResponse();

        SftpConfiguration cfg = tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class);
        if (cfg == null) {
            ftpsUploadResponse.setOk(false);
            ftpsUploadResponse.setMessage(String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
            return ftpsUploadResponse;
        }

        var srcClientConfig = tenantService.createSnClientConfig(request.getTenantCode());
        // To prevent response stream to be read by logger
        srcClientConfig.setLoggingOptions(LoggingInterceptor.LoggingOptions.builder().logRequestBody(false).build());
        AttachmentApi srcApi = apiFactory.create(srcClientConfig);

        for (String attachmentId : request.getAttachmentIds()) {
            try {
                if (request.isEncrypt()) {
                    handleFileAttachmentWithEncryption(cfg, request, ftpsUploadResponse, srcApi, attachmentId);
                } else {
                    handleFileAttachment(cfg, ftpsUploadResponse, srcApi, attachmentId, request.getPath(), request.isOverwrite());
                }


            } catch (Exception e) {
                log.error(e.getMessage());
                ftpsUploadResponse.setOk(false);
                ftpsUploadResponse.setMessage(e.getMessage());
            }
        }

        return ftpsUploadResponse;
    }

    void handleFileAttachment(SftpConfiguration cfg, FtpsUploadResponse ftpsUploadResponse, AttachmentApi srcApi, String attachmentId, String path, boolean isOverwrite) throws IOException, FileServiceTransferException {
        log.debug("Encryption is disabled for this request...");
        var attachment = srcApi.get(attachmentId);

        //use this response var for local testing
//           var response = getAttachmentById(attachmentId);
        try (var response = srcApi.downloadById(attachment.getId())) {
            log.debug("Attachment Download response filename from SN: {}", response.getFileName());
            clientFtpsService.uploadFileForClient(cfg, response.stream(), FtpsUtils.extractStringWithinQuotes(response.getFileName()), attachmentId, path, isOverwrite);
            ftpsUploadResponse.setOk(true);
            ftpsUploadResponse.setMessage("Uploaded " + attachmentId + " successfully.");
        }
    }

    void handleFileAttachmentWithEncryption(SftpConfiguration cfg, FtpsUploadRequest request, FtpsUploadResponse ftpsUploadResponse, AttachmentApi srcApi, String attachmentId) throws IOException, FileServiceTransferException, PGPException {
        log.debug("Encryption is enabled for this request...");
        CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
                CryptoConfiguration.class);
        var attachment = srcApi.get(attachmentId);

        //this if for local testing
//        cryptoConfiguration = FtpsUtils.getTestCryptoConfiguration();

        //use this response var for local testing
        //var response = getAttachmentById(attachmentId);
        try (var response = srcApi.downloadById(attachment.getId())) {

            String fileName = FtpsUtils.extractStringWithinQuotes(response.getFileName());

            String fileNoExtension = FilenameUtils.getBaseName(fileName);
            String extension = StringUtils.isBlank(request.getEncryptedFileExt()) ? "gpg" : request.getEncryptedFileExt();

            fileName = fileNoExtension + "." + extension;
            ByteArrayOutputStream baos = PgpEncryptor.encryptStream(response.stream(), fileName, true,
                    encryptionService.decrypt(cryptoConfiguration.getPublicKey(), cryptoConfiguration.getSalt()));

            InputStream encryptedStream = new ByteArrayInputStream(baos.toByteArray());

            clientFtpsService.uploadFileForClient(cfg, encryptedStream, fileName,
                    attachmentId, request.getPath(), request.isOverwrite());
            ftpsUploadResponse.setOk(true);
            ftpsUploadResponse.setMessage("Uploaded " + attachmentId + " successfully.");
        }
    }


    @Override
    public List<SignalHandler<? extends Signal>> getHandlers() {
        return asList(new FtpsUploadRequestHandler(), new FtpsDownloadRequestHandler());
    }
}
