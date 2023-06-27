package com.mobichord.ftps.rest;

import com.appchord.data.tenant.TenantConfig;
import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.data.UploadRequest;
import com.mobichord.ftps.message.ProtocolDownloadCommand;
import com.mobichord.ftps.message.ProtocolUploadCommand;
import com.mobichord.messaging.Sender;
import com.mobichord.tenant.TenantService;
import com.mobichord.tracking.TrackingService;
import com.mobichord.web.rest.controller.BaseController;
import com.mobichord.web.rest.controller.DeferredResultStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
public class FileProtocolController extends BaseController {
    private final TrackingService trackingService;

    private final TenantService tenantService;
    private final ObjectMapper mapper;

    public FileProtocolController(Sender sender, DeferredResultStore deferredResultStore, TrackingService trackingService,
                                  TenantService tenantService, ObjectMapper mapper) {
        super(sender, trackingService, tenantService, deferredResultStore);
        this.trackingService = trackingService;
        this.tenantService = tenantService;
        this.mapper = mapper;
    }

    @PostMapping(value = "upload",  consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> uploadFile(@RequestBody @Valid UploadRequest request) {
        String responseMessage;
        try {

            logger.debug("Receive request: \n{}", toJson(request));
            responseMessage = String.format("%s upload has been successfully initiated.", request.getProtocol());

            //todo : need to check if tenantconfig and cfg, cryptocfg needs to be done here since it's already being checked in the other services
            //check for tenant config if valid else throw an error
            TenantConfig tenantConfig = tenantService.getCurrentTenantConfig();
            if (tenantConfig == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Tenant [%s] not found");
            }

//            SftpConfiguration cfg = tenantService.getConfiguration(request.getProtocolCfgId(), SftpConfiguration.class);
//            if (cfg == null) {
//                return errorResponse(HttpStatus.NOT_FOUND, String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
//            }

//            if (request.isEncrypt()) {
//                CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
//                        CryptoConfiguration.class);
//                if (cryptoConfiguration == null) {
//                    return errorResponse(HttpStatus.NOT_FOUND, String.format("Crypto configuration [%s] not found", request.getCryptoCfgId()));
//                }
//            }

            String activityId = trackingService.getCurrentActivityId();

            //create cmd based from prototype field
            ProtocolUploadCommand uploadCommand = ProtocolUploadCommand.builder()
                    .activityId(activityId)
                    .attachmentIds(request.getAttachments())
                    .path(request.getPath())
                    .protocolCfgId(request.getProtocolCfgId())
                    .cryptoCfgId(request.getCryptoCfgId())
                    .protocol(request.getProtocol())
                    .cryptoCfgId(request.getCryptoCfgId())
                    .encrypt(request.isEncrypt())
                    .sourceTenantCode(tenantConfig.getTenant().getTenantCode())
                    .build();

            if(!ObjectUtils.isEmpty(request.getOptions())){
                uploadCommand.setOverwrite(request.getOptions().isOverwrite());
                uploadCommand.setEncryptedFileExt(request.getOptions().getEncryptedFileExt());
            }

            logger.info("Sending message to queue: {}", toJson(uploadCommand));
            sender.send(uploadCommand);
            return successResponse(HttpStatus.OK, activityId, responseMessage);
        } catch (Exception e) {
            responseMessage = String.format("%s upload failed: ", request.getProtocol());
            return ResponseEntity.badRequest().body(responseMessage + e.getMessage());
        }
    }


    @PostMapping(value = "download",  consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> downloadFile(@RequestBody @Valid DownloadRequest request) {
        String responseMessage;
        try {

            logger.info("Received download request: \n{}", toJson(request));
            responseMessage = String.format("%s download has been successfully initiated.", request.getProtocol());

            //check for tenant config if valid else throw an error
            TenantConfig tenantConfig = tenantService.getCurrentTenantConfig();
            if (tenantConfig == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Tenant [%s] not found");
            }

//            SftpConfiguration cfg = tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class);
//            if (cfg == null) {
//                return errorResponse(HttpStatus.NOT_FOUND, String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
//            }

            String activityId = trackingService.getCurrentActivityId();

            //create cmd then send to queue
            ProtocolDownloadCommand downloadCommand = ProtocolDownloadCommand.builder()
                    .activityId(activityId)
                    .protocolCfgId(request.getFtpsCfgId())
                    .protocol(request.getProtocol())
                    .fileMask(request.getFileMask())
                    .path(request.getPath())
                    .targetTable(request.getTargetTable())
                    .targetSysId(request.getTargetSysId())
                    .decrypt(request.isDecrypt())
                    .cryptoCfgId(request.getCryptoCfgId())
                    .build();

            if(!ObjectUtils.isEmpty(request.getOptions())){
                downloadCommand.setAfterAction(request.getOptions().getAfterAction());
            }

            logger.info("Sending message to queue: {}", toJson(downloadCommand));
            sender.send(downloadCommand);

            return successResponse(HttpStatus.OK, activityId, responseMessage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to download the file. " + e.getMessage());
        }
    }

    private String toJson(Object request) {
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            logger.warn("Failed to create json: ", e);
        }
        return "";
    }

}
