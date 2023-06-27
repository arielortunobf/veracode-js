package com.mobichord.ftps.service;

import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
import com.mobichord.tenant.TenantService;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public interface FtpsService {

    void setPath(String path);

    void setFtpsHost(String ftpsHost);

    void setFtpsPort(int ftpsPort);

    void setFtpsUsername(String ftpsUsername);

    void setFtpsPassword(String ftpsPassword);

    void setBasePath(String basePath);
    void setRequestPath(String requestPath);

    FTPSClient connectToFtpsServer() throws IOException;

    void uploadFile(String localFilePath, String remoteFilePath) throws IOException;

    void uploadFile(FTPSClient ftpsClient, InputStream inputStream, String fileNameWithExtension, boolean isOverWrite) throws IOException;

    void downloadFile(String remoteFilePath, String localFilePath) throws IOException;

    void downloadFile(TenantService tenantService, DownloadRequest request) throws IOException;

    FtpsDownloadResponse downloadFile(FTPSClient ftpsClient, TenantService tenantService, DownloadRequest request) throws FileServiceTransferException;

    void processSingleFile(TenantService tenantService, FTPFile file, FTPSClient ftpsClient, String table, String recordId, String remoteDirPath,
                           Map<String, String> adjustedFileNames) throws IOException;
}
