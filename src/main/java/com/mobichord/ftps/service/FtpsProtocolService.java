package com.mobichord.ftps.service;

import com.mobichord.ftps.data.FileProtocolRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.utility.FtpsUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.util.TrustManagerUtils;
import org.springframework.util.ObjectUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

//@Service
@Slf4j
public class FtpsProtocolService implements ProtocolService {

    private final ProtocolConfiguration configuration;
    private final FTPSClient ftpsClient;
    private final EncryptionService encryptionService;

    public FtpsProtocolService(ProtocolConfiguration configuration, EncryptionService encryptionService) {
        this.configuration = configuration;
        this.encryptionService = encryptionService;
        this.ftpsClient = new FTPSClient("TLS", false);
        this.ftpsClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        this.ftpsClient.setEnabledProtocols(new String[]{"TLSv1.2"});
    }

    //put this in the interface
    @Override
    public void connect() throws IOException {
        //temp credentials
        ftpsClient.connect("creditoselporvenir.com", 21);

//        ftpsClient.connect(configuration.getHost(), configuration.getPort());
        int reply = ftpsClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpsClient.disconnect();
            throw new FileServiceTransferException("FTP server refused connection.");
        }

        //decrypt password
        //used for local testing
//        if (!ftpsClient.login("bf@creditoselporvenir.com", "ftp.123456.*")) {
//            ftpsClient.logout();
//            throw new FileServiceTransferException("FTP server login failed.");
//        }

        if (!ftpsClient.login(configuration.getUsername(), encryptionService.decrypt(configuration.getPassword(), configuration.getPasswordSalt()))) {
            ftpsClient.logout();
            throw new FileServiceTransferException("FTP server login failed.");
        }

        ftpsClient.enterLocalPassiveMode();
        ftpsClient.setFileType(FTP.LOCAL_FILE_TYPE);

    }

    @Override
    public String getType() {
        return "FTPS";
    }

    @Override
    public void upload(FileProtocolRequest request) throws FileServiceTransferException {
        InputStream is = null;
        InputStream inputStream = null;
        String path = configuration.getPath();
        String basePath = configuration.getBasePath();
        String requestPath = configuration.getRequestPath();

        try {

            connect();
            inputStream = request.getStream();
            String fileName = request.getFileName();

            is = new BufferedInputStream(inputStream, 1024);
            String newRemoteFilePath = FtpsUtils.getFileNameWithPath(fileName, path + "/");

            log.info("Path: {}", path);
//            log.info("Filename with extension: {}", fileNameWithExtension);
            log.info("New RemoteFileName: {}", newRemoteFilePath);
//            checkDirectoryPath();
            //create directory if not existing
            createDirectories();
//            log.info("Storing file in FTPS server in this location: {}", newRemoteFilePath);
////            log.info("overwriting files: {}", isOverWrite);
//
            boolean isOverWrite = request.isOverWrite();

            if (!isOverWrite) {
                //get file name
                boolean isFileExisting = isFileExists(fileName);
                log.info("File exists: {}", isFileExisting);
                if (isFileExisting) {
                    log.info("File already exists...");
                    throw new FileServiceTransferException("File already exists in the server.");
                } else {
                    ftpsClient.storeFile(newRemoteFilePath, is);
                }
            } else {
                ftpsClient.storeFile(newRemoteFilePath, is);
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        } finally {
            closeQuietly(inputStream, is);
            disconnect();
        }
    }

    @Override
    public void checkDirectoryPath() throws FileServiceTransferException {
        // Use the ftpsClient instance variable from your class, instead of passing it as a parameter.
        // Make sure to call this method after the ftpsClient instance has been initialized.
        String path = configuration.getPath();
        String basePath = configuration.getBasePath();
        String requestPath = configuration.getRequestPath();

        //check if dir is existing first
        log.info("Changing directory to: {}", path);

        try {
            if (!ObjectUtils.isEmpty(requestPath)) {
                boolean isDirExisting = ftpsClient.changeWorkingDirectory(path);
                if (isDirExisting) {
                    log.info("Successfully changed working directory to: {}", path);
                } else {
                    log.info("Failed to change working directory to: {}, will use base directory: {}", path, basePath);

                    //change to base dir
                    ftpsClient.changeWorkingDirectory(basePath);
                    log.info("Successfully changed working directory to base directory: {}", basePath);

                    //create path now if requestpath is not null
                    if (!ObjectUtils.isEmpty(requestPath)) {
                        boolean created = ftpsClient.makeDirectory(path);
                        if (created) {
                            log.info("Directory created successfully: {}", path);
                            ftpsClient.changeWorkingDirectory(path);
                        } else {
                            log.info("Failed to create directory: {}", path);
                        }
                    }
                }
            } else {
                log.info("Changing directory to default path: {}", basePath);
                ftpsClient.changeWorkingDirectory(basePath);
            }
        } catch (Exception e) {
            log.error("Error occurred during FTPS client check directory path:" + ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException("Error occurred during FTPS client check directory path:");
        }


    }

    @Override
    public FTPFile[] getListOfAvailableFiles(FileProtocolRequest request) throws FileServiceTransferException {
        String path = configuration.getPath();
        String basePath = configuration.getBasePath();
        String requestPath = configuration.getRequestPath();

        try {

            connect();

//            String remoteDirectory = request.getPath();
            String fileMask = request.getFileMask();
            if (StringUtils.isBlank(fileMask)) {
                fileMask = "*";
            }
            log.info("Filemask : {}", fileMask);

            String finalFileMask = fileMask;
            FTPFile[] files = ftpsClient.listFiles(path, new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile file) {
                    Pattern compiledPattern = Pattern.compile(FtpsUtils.wildcardToRegex(finalFileMask));
                    if (compiledPattern.matcher(file.getName()).matches()) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            return files;
        } catch (Exception e) {
            log.error("Error occurred during FTPS client download: " + ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException("Error occurred during FTPS client download.");
        }
    }

//    @Override
//    public Object getListOfAvailableFiles(TenantService tenantService, DownloadRequest request) throws Exception {
//        return null;
//    }
//
//    @Override
//    public void processSingleFile(TenantService tenantService, String table, String recordId, String remoteDirPath, Map<String, String> adjustedFileNames) throws IOException {
//
//    }

    @Override
    public void disconnect() throws FileServiceTransferException {
        if (ftpsClient != null) {
            try {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } catch (IOException e) {
                log.error("Error occurred during FTPS client disconnection: " + ExceptionUtils.getStackTrace(e));
                throw new FileServiceTransferException("Error occurred during FTPS client disconnection: ");
            }
        }
    }

    private void closeQuietly(Closeable... closeables) throws FileServiceTransferException {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                log.error("Error occurred during stream closing: " + ExceptionUtils.getStackTrace(e));
                throw new FileServiceTransferException("Error occurred during stream closing: ");
            }
        }
    }

    @Override
    public boolean isFileExists(String fileName) throws FileServiceTransferException {
        FTPFile[] files = new FTPFile[0];
        try {
            files = ftpsClient.listFiles();
            for (FTPFile file : files) {
                if (file.isFile() && file.getName().equals(fileName)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }
    }

    @Override
    public boolean isConnected() throws FileServiceTransferException {
        return false;
    }

    @Override
    public void createDirectories() throws FileServiceTransferException {
        boolean dirExists = true;
        try {
            String[] directories = StringUtils.split(configuration.getPath(), File.separator);
            if (directories != null && directories.length > 0) {
                for (String dir : directories) {
                    if (StringUtils.isNotBlank(dir)) {
                        if (dirExists) {
                            dirExists = ftpsClient.changeWorkingDirectory(dir);
                        }
                        if (!dirExists) {
                            if (!ftpsClient.makeDirectory(dir)) {
                                log.info("Unable to create directory: {}", dir);
                            }
                            if (!ftpsClient.changeWorkingDirectory(dir)) {
                                log.info("Unable to change directory to: {}", dir);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }


    }

    @Override
    public InputStream download(FileProtocolRequest rq) throws FileServiceTransferException {
        OutputStream output = null;
        InputStream inputStreamResponse = null;
        Path tempFile = null;

        try {
            log.info("Downloading file from FTPS server: {}", rq.getFileName());
            connect();


            tempFile = Files.createTempFile("ftps-", "-tmp");
            log.info("Creating temp file : {}", tempFile.getFileName());

            output = Files.newOutputStream(tempFile);

            if (!ftpsClient.retrieveFile(configuration.getPath() + "/" + rq.getAdjustedFileName(), output)) {
                log.error("Failed to retrieve file: " + rq.getFileName());
                throw new FileServiceTransferException("Failed to retrieve file: " + rq.getFileName());
            }

            if (!ObjectUtils.isEmpty(output)) {
                output.close();
            }

            inputStreamResponse = Files.newInputStream(tempFile);

            return inputStreamResponse;

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        } finally {
            closeQuietly(output);

            disconnect();

            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException e) {
                    log.error("Error when deleting temp file : " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void delete(FileProtocolRequest request) throws FileServiceTransferException {
        try {

            connect();
            log.info("After action : Deleting [{}].", request.getFileName());
            if (!ftpsClient.deleteFile(request.getFileName())) {
                throw new FileServiceTransferException(String.format("failed to delete file [%s].", request.getFileName()));
            }
        } catch (IOException e) {
            throw new FileServiceTransferException(String.format("failed to delete file [%s]. %s", request.getFileName(), e));
        } finally {
            disconnect();
        }
    }

    @Override
    public void rename(FileProtocolRequest request) throws FileServiceTransferException {
        try {
            connect();

            log.info("After action : Renaming from [{}] to [{}].", request.getOldFileName(), request.getOldFileName());
            if (!ftpsClient.rename(request.getOldFileName(), request.getNewFileName())) {
                throw new FileServiceTransferException(String.format("failed to rename file [%s].", request.getOldFileName()));
            }
        } catch (IOException e) {
            throw new FileServiceTransferException(String.format("failed to rename file [%s]. %s", request.getOldFileName(), e));
        } finally {
            disconnect();
        }
    }
}
