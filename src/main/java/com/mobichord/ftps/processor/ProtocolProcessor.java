package com.mobichord.ftps.processor;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.appchord.messages.Signal;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ProtocolDownloadRequest;
import com.mobichord.ftps.message.ProtocolDownloadResponse;
import com.mobichord.ftps.message.ProtocolUploadRequest;
import com.mobichord.ftps.message.ProtocolUploadResponse;
import com.mobichord.ftps.service.AttachmentApiFactory;
import com.mobichord.ftps.service.FileProtocolService;
import com.mobichord.ftps.service.ProtocolConfiguration;
import com.mobichord.ftps.utility.FtpsUtils;
import com.mobichord.messaging.Processor;
import com.mobichord.messaging.Sender;
import com.mobichord.messaging.SignalHandler;
import com.mobichord.okhttp.LoggingInterceptor;
import com.mobichord.servicenow.api.http.AttachmentApi;
import com.mobichord.servicenow.api.http.AttachmentDownloadResponse;
import com.mobichord.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mobichord.messaging.SignalHandler.asList;

@Service
@Slf4j
public class ProtocolProcessor implements Processor {
    private final TenantService tenantService;
    private final FileProtocolService protocolService;
    private final Sender sender;
    private final AttachmentApiFactory apiFactory;

    public ProtocolProcessor(TenantService tenantService, Sender sender,
                             AttachmentApiFactory apiFactory, FileProtocolService protocolService) {
        this.tenantService = tenantService;
        this.sender = sender;
        this.apiFactory = apiFactory;
        this.protocolService = protocolService;
    }

    public class ProtocolUploadRequestHandler extends SignalHandler<ProtocolUploadRequest> {

        @Override
        public void handle(ProtocolUploadRequest request) {
            log.debug("started ProtocolUploadRequestHandler::handle: {}", request);
            ProtocolUploadResponse response = processUpload(request);
            response.setActivityId(request.getActivityId());
            sender.send(response);
        }
    }

    public class ProtocolDownloadRequestHandler extends SignalHandler<ProtocolDownloadRequest> {

        @Override
        public void handle(ProtocolDownloadRequest request) {
            log.debug("started ProtocolDownloadRequestHandler::handle: {}", request);
            ProtocolDownloadResponse response = processDownload(request);
            response.setActivityId(request.getActivityId());
            sender.send(response);
        }
    }

    ProtocolDownloadResponse processDownload(ProtocolDownloadRequest request) {
        ProtocolDownloadResponse response = new ProtocolDownloadResponse();
        try {

            String protocol = request.getProtocol();

            //check if configuration exists
            SftpConfiguration cfg = tenantService.getConfiguration(request.getProtocolCfgId(), SftpConfiguration.class);
            if (ObjectUtils.isEmpty(cfg)) {
                response.setOk(false);
                response.setMessage(String.format("%s configuration [%s] not found", protocol, request.getProtocolCfgId()));
                return response;
            }

            //create protocolconfiguration
            ProtocolConfiguration protocolConfiguration = getProtocolConfiguration(request.getProtocol(), request.getRequestPath(), cfg);

            //create instance of protocolservice
            //get available files from file server
            FTPFile[] files = protocolService.getAvailableFiles(protocolConfiguration, request);

            var srcClientConfig = tenantService.createSnClientConfig(request.getTenantCode());
            if (ObjectUtils.isEmpty(srcClientConfig)) {
                response.setOk(false);
                response.setMessage(String.format("Tenant [%s] does not have proper ServiceNowClientConfig.", request.getTenantCode()));
                return response;
            }

            // To prevent response stream to be read by logger
            srcClientConfig.setLoggingOptions(LoggingInterceptor.LoggingOptions.builder().logRequestBody(false).build());

            AttachmentApi attachmentApi = apiFactory.create(srcClientConfig);
            if (ObjectUtils.isEmpty(attachmentApi)) {
                response.setOk(false);
                response.setMessage("Cannot create AttachmentApi from the provided ServiceNowClientConfiguration.");
                return response;
            }

            downloadFileServerAttachmentsToSN(protocolConfiguration, response, attachmentApi, request);


        } catch (FileServiceTransferException e) {
            throw new RuntimeException(e);
        }

        return response;
    }

    ProtocolUploadResponse processUpload(ProtocolUploadRequest request) {
        ProtocolUploadResponse uploadResponse = new ProtocolUploadResponse();
        String protocol = request.getProtocol();

        //check if configuration exists
        SftpConfiguration cfg = tenantService.getConfiguration(request.getProtocolCfgId(), SftpConfiguration.class);
        if (ObjectUtils.isEmpty(cfg)) {
            uploadResponse.setOk(false);
            uploadResponse.setMessage(String.format("%s configuration [%s] not found", protocol, request.getProtocolCfgId()));
            return uploadResponse;
        }

        //create protocolconfiguration
        ProtocolConfiguration protocolConfiguration = getProtocolConfiguration(request.getProtocol(), request.getPath(), cfg);

        //create servicenowclientconfig
        var srcClientConfig = tenantService.createSnClientConfig(request.getTenantCode());
        if (ObjectUtils.isEmpty(srcClientConfig)) {
            uploadResponse.setOk(false);
            uploadResponse.setMessage(String.format("Tenant [%s] does not have proper ServiceNowClientConfig.", request.getTenantCode()));
            return uploadResponse;
        }
        // To prevent response stream to be read by logger
        srcClientConfig.setLoggingOptions(LoggingInterceptor.LoggingOptions.builder().logRequestBody(false).build());

        AttachmentApi srcApi = apiFactory.create(srcClientConfig);
        if (ObjectUtils.isEmpty(srcApi)) {
            uploadResponse.setOk(false);
            uploadResponse.setMessage("Cannot create AttachmentApi from the provided ServiceNowClientConfiguration.");
            return uploadResponse;
        }

        for (String attachmentId : request.getAttachmentIds()) {
            try {
                uploadSNAttachmentsToFileServer(protocolConfiguration, uploadResponse, srcApi, attachmentId, request);
            } catch (Exception e) {
                log.error(ExceptionUtils.getStackTrace(e));
                uploadResponse.setOk(false);
                uploadResponse.setMessage(e.getMessage());
            }
        }

        return uploadResponse;
    }


    private static ProtocolConfiguration getProtocolConfiguration(String protocol, String requestPath, SftpConfiguration cfg) {
        String basePath = cfg.getPath();
        Path confPath = Paths.get(basePath);
        Path combinedPathObj = confPath.resolve(requestPath);
        String combinedPathString = combinedPathObj.toString();

        return ProtocolConfiguration.builder()
                .type(protocol)
                .port(cfg.getPort())
                .host(cfg.getHost())
                .basePath(basePath)
                .requestPath(requestPath)
                .path(combinedPathString)
                .username(cfg.getUsername())
                .password(cfg.getPassword())
                .passwordSalt(cfg.getSalt())
                .build();
    }

    /*create connection to SN, get attachments then upload to File server */
    void uploadSNAttachmentsToFileServer(ProtocolConfiguration protocolConfiguration, ProtocolUploadResponse protocolUploadResponse, AttachmentApi srcApi, String attachmentId, ProtocolUploadRequest request) throws IOException, FileServiceTransferException {
        var attachment = srcApi.get(attachmentId);

        //use this response var for local testing
//           var response = getAttachmentById(attachmentId);

        //loop each attachment from the request
        try (AttachmentDownloadResponse attachmentRs = srcApi.downloadById(attachment.getId())) {
            log.debug("Attachment Download response filename from SN: {}", attachmentRs.getFileName());
            //upload to file server
            protocolService.upload(protocolConfiguration, request, attachmentRs);
            protocolUploadResponse.setOk(true);
            protocolUploadResponse.setMessage("Uploaded " + attachmentId + " successfully.");
        }
    }

    /*create connection to file server, then create connection to SN to upload files*/
    void downloadFileServerAttachmentsToSN(ProtocolConfiguration protocolConfiguration, ProtocolDownloadResponse response, AttachmentApi attachmentApi, ProtocolDownloadRequest request) {

        try {
            //get available files from file server
            FTPFile[] files = protocolService.getAvailableFiles(protocolConfiguration, request);

            List<String> filePaths = Arrays.stream(files)
                    .map(file -> FilenameUtils.concat(protocolConfiguration.getPath(), file.getName()))
                    .collect(Collectors.toList());

            Map<String, String> adjustedFileNames = FtpsUtils.adjustAttachmentFileNames(filePaths, FileSystems.getDefault().getSeparator());

            if (files.length > 0) {
                for (FTPFile file : files) {
                    if (file.isFile()) {
                        try {
                            String remoteFilePath = FilenameUtils.concat(protocolConfiguration.getPath(), file.getName());
                            String adjustedFileName = adjustedFileNames.get(remoteFilePath);

                            log.info("Getting files from FTPS: {}", file.getName());
                            request.setFileName(file.getName());
                            request.setAdjustedFileName(adjustedFileName);

                            //retrieve files from file server then upload to SN
                            InputStream inputStream = protocolService.download(protocolConfiguration, request);
                            if (ObjectUtils.isEmpty(inputStream)) {
                                log.error("Empty inputstream for file : {}", file.getName());
                                inputStream.close();
                                continue;
                            }

                            //upload stream now to SN
                            log.info("Uploading file to ServiceNow: {}", protocolConfiguration.getPath() + "/" + file.getName());
                            attachmentApi.create(request.getTargetTable(), request.getTargetSysId(), adjustedFileName, inputStream, 0, request.getEncryptionContext());
//                          processSingleFile(tenantService, file, ftpsClient, request, adjustedFileNames);

                            if (!StringUtils.isBlank(request.getAfterAction()) || !StringUtils.equalsIgnoreCase("none", request.getAfterAction())) {
                                if (StringUtils.equalsIgnoreCase("delete", request.getAfterAction())) {
                                    protocolService.delete(protocolConfiguration, request);
                                } else if (StringUtils.equalsIgnoreCase("rename", request.getAfterAction())) {
                                    protocolService.rename(protocolConfiguration, request);
                                }
                            }
                        } catch (Exception e) {
                            response.setOk(false);
                            response.setMessage(String.format("Failed to process file: %s , due to : %s", file.getName(), e.getMessage()));
                            break;
                        }
                    }
                }
            } else {
                log.info("No files found.");
            }

            if (response.isOk()) {
                response.setMessage("Successfully download.");
            }

        } catch (FileServiceTransferException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<SignalHandler<? extends Signal>> getHandlers() {
        return asList(new ProtocolUploadRequestHandler(), new ProtocolDownloadRequestHandler());
    }
}
