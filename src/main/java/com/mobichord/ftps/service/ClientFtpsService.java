package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
import com.mobichord.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTPSClient;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class ClientFtpsService {

    private final FtpsFileServiceFactory ftpsFileServiceFactory;

    final
    TenantService tenantService;

    public ClientFtpsService(FtpsFileServiceFactory ftpsFileServiceFactory, TenantService tenantService) {
        this.ftpsFileServiceFactory = ftpsFileServiceFactory;
        this.tenantService = tenantService;
    }

//    @Async
    public void uploadFileForClient(SftpConfiguration cfg, InputStream inputStream, String fileNameWithExtension, String aId, String path, boolean isOverwrite) throws IOException {
        log.debug("inside uploadFileForClient...");
        // Use the FtpsFileService instance to upload the file
        // Create a new FtpsFileService instance based on the client's FTPS information
        FtpsService ftpsFileService = ftpsFileServiceFactory.createFtpsFileService(cfg, path);
        FTPSClient ftpsClient = connect(ftpsFileService);

        try {
            log.info("uploading file to FTPS : {}", fileNameWithExtension);
            ftpsFileService.uploadFile(ftpsClient, inputStream, fileNameWithExtension, isOverwrite);
        } catch (IOException e) {
            throw new FileServiceTransferException(String.format("failed to upload attachment [%s]. %s", aId, e.getMessage()));
        }
    }

    public FtpsDownloadResponse downloadFileForClient(DownloadRequest request) throws FileServiceTransferException {
        log.debug("inside downloadFileForClient...");
        SftpConfiguration config = tenantService.getConfiguration(request.getFtpsCfgId(), SftpConfiguration.class);
        if (config == null) {
            FtpsDownloadResponse ftpsDownloadResponse = new FtpsDownloadResponse();
            ftpsDownloadResponse.setOk(false);
            ftpsDownloadResponse.setMessage(String.format("FTPS configuration [%s] not found", request.getFtpsCfgId()));
            return ftpsDownloadResponse;
        }

        FtpsService ftpsFileService = ftpsFileServiceFactory.createFtpsFileService(config, request.getPath());
        FTPSClient ftpsClient = connect(ftpsFileService);

        return ftpsFileService.downloadFile(ftpsClient, tenantService, request);
    }

    private FTPSClient connect(FtpsService ftpsFileService) throws FileServiceTransferException {
        FTPSClient ftpsClient;
        try {
            ftpsClient = ftpsFileService.connectToFtpsServer();
        } catch (IOException e) {
            throw new FileServiceTransferException("unable to connect to FTPS server", e);
        }

        return ftpsClient;
    }

}