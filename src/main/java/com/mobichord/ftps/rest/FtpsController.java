package com.mobichord.ftps.rest;

import com.appchord.data.tenant.TenantConfig;
import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobichord.ftps.data.AttachmentDownloadResponse;
import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.data.UploadRequest;
import com.mobichord.ftps.message.ftps.FtpsDownloadCommand;
import com.mobichord.ftps.message.ftps.FtpsUploadCommand;
import com.mobichord.messaging.Sender;
import com.mobichord.tenant.TenantService;
import com.mobichord.tracking.TrackingService;
import com.mobichord.web.rest.controller.BaseController;
import com.mobichord.web.rest.controller.DeferredResultStore;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.internal.http.RealResponseBody;
import okio.Okio;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.io.File;
import java.io.FileNotFoundException;

@Slf4j
@Deprecated
//@RestController
public class FtpsController extends BaseController {
    private final TrackingService trackingService;

    private final TenantService tenantService;
    private final ObjectMapper mapper;

    public FtpsController(Sender sender, DeferredResultStore deferredResultStore,TrackingService trackingService,
                          TenantService tenantService, ObjectMapper mapper) {
        super(sender, trackingService, tenantService, deferredResultStore);
        this.trackingService = trackingService;
        this.tenantService = tenantService;
        this.mapper = mapper;
    }

    @GetMapping()
    public ResponseEntity<?> checkIfUps(){
        log.debug("Received check endpoint request for integration-ftps...");
        return ResponseEntity.ok().body("yes");
    }

    @GetMapping(value = "/test")
    public ResponseEntity<?> checkIfUp(){
        log.debug("Received check endpoint request for (/test) integration-ftps...");
        return ResponseEntity.ok().body("yes");
    }

    @PostMapping(value = "/upload",  consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> uploadFile(@RequestBody @Valid UploadRequest request) {
        try {

            logger.debug("Receive request: \n{}", toJson(request));

            //check for tenant config if valid else throw an error
            TenantConfig tenantConfig = tenantService.getCurrentTenantConfig();
            if (tenantConfig == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Tenant [%s] not found");
            }

            SftpConfiguration cfg = tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class);
            if (cfg == null) {
                return errorResponse(HttpStatus.NOT_FOUND, String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
            }

            if (request.isEncrypt()) {
                CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
                        CryptoConfiguration.class);
                if (cryptoConfiguration == null) {
                    return errorResponse(HttpStatus.NOT_FOUND, String.format("Crypto configuration [%s] not found", request.getCryptoCfgId()));
                }
            }

            String activityId = trackingService.getCurrentActivityId();

            //create cmd then send in queue
            FtpsUploadCommand command = new FtpsUploadCommand();
            command.setFtpsCfgId(request.getFtpsCfgId());
            command.setActivityId(activityId);
            command.setAttachmentIds(request.getAttachments());
            command.setPath(request.getPath());
            command.setCryptoCfgId(request.getCryptoCfgId());
            command.setEncrypt(request.isEncrypt());
            command.setSourceTenantCode(tenantConfig.getTenant().getTenantCode());
            if (request.getOptions() != null) {
                command.setOverwrite(request.getOptions().isOverwrite());
                command.setEncryptedFileExt(request.getOptions().getEncryptedFileExt());
            }

            logger.info("Sending message to queue: {}", toJson(command));
            sender.send(command);

            return successResponse(HttpStatus.OK, activityId, "FTPS upload has been successfully initiated.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("File upload failed: " + e.getMessage());
        }
    }


    @PostMapping(value = "/download",  consumes = {MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<?> downloadFile(@RequestBody @Valid DownloadRequest request) {
        try {

            logger.info("Received download request: \n{}", toJson(request));

            //check for tenant config if valid else throw an error
            TenantConfig tenantConfig = tenantService.getCurrentTenantConfig();
            if (tenantConfig == null) {
                return errorResponse(HttpStatus.NOT_FOUND, "Tenant [%s] not found");
            }

            SftpConfiguration cfg = tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class);
            if (cfg == null) {
                return errorResponse(HttpStatus.NOT_FOUND, String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
            }

            String activityId = trackingService.getCurrentActivityId();

            //create cmd then send to queue
            FtpsDownloadCommand command = new FtpsDownloadCommand();
            command.setRequest(request);
            command.setFtpsCfgId(request.getFtpsCfgId());
            command.setActivityId(activityId);
            command.setTenantCode(tenantConfig.getTenant().getTenantCode());

            logger.info("Sending message to queue: {}", toJson(command));
            sender.send(command);

            return successResponse(HttpStatus.OK, activityId, "FTPS download has been successfully initiated.");
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

    static AttachmentDownloadResponse getAttachmentById(String anyString){

        File file = new File("src/main/resources/Paymentfile-.csv");

        // Create a dummy body
        okhttp3.RequestBody body = okhttp3.RequestBody.create(okhttp3.MediaType.parse("application/octet-stream"), file);
        Response response;
        AttachmentDownloadResponse attachmentDownloadResponse = null;

        try {
            // create okhttp response object
            response = new Response.Builder()
                    .request(new okhttp3.Request.Builder().url("http://localhost").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .header("Content-Disposition", "attachment; filename=" + file.getName())
                    .body(new RealResponseBody("application/octet-stream", file.length(), Okio.buffer(Okio.source(file))))
                    .build();

            attachmentDownloadResponse = new AttachmentDownloadResponse(response);

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return attachmentDownloadResponse;
    }

}
