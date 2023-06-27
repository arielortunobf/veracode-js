package com.mobichord.ftps.service;


import com.mobichord.ftps.data.FileProtocolRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.io.InputStream;

public interface ProtocolService {

    //put this in the interface
    void connect() throws IOException;

    String getType();

    //    public void uploadFile(InputStream inputStream, String fileNameWithExtension) throws IOException {
    void upload(FileProtocolRequest request) throws IOException;

    void checkDirectoryPath() throws IOException;

    FTPFile[] getListOfAvailableFiles(FileProtocolRequest request) throws FileServiceTransferException;

//    Object getListOfAvailableFiles(TenantService tenantService, DownloadRequest request) throws Exception;

//    void processSingleFile(TenantService tenantService, String table, String recordId, String remoteDirPath,
//                           Map<String, String> adjustedFileNames) throws IOException;

    void disconnect() throws FileServiceTransferException;

    boolean isFileExists(String fileName) throws IOException;
    boolean isConnected() throws FileServiceTransferException;

    void createDirectories() throws IOException;

    InputStream download(FileProtocolRequest rq) throws FileServiceTransferException;

    //void delete(FileProtocolRequest rq);

    void delete(FileProtocolRequest rq) throws FileServiceTransferException;

    void rename(FileProtocolRequest rq) throws FileServiceTransferException;
}


