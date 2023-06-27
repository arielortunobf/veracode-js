package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.mobichord.ftps.data.DownloadRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ftps.FtpsDownloadResponse;
import com.mobichord.ftps.utility.FtpsUtils;
import com.mobichord.ftps.utility.PgpDecryptor;
import com.mobichord.servicenow.api.http.AttachmentApi;
import com.mobichord.servicenow.api.http.AttachmentApiImpl;
import com.mobichord.servicenow.api.http.ServiceNowClientConfiguration;
import com.mobichord.servicenow.api.http.ServiceNowHttpClient;
import com.mobichord.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.*;
import org.apache.commons.net.util.TrustManagerUtils;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.*;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FtpsFileService implements FtpsService {

    private final EncryptionService encryptionService;

    private String ftpsHost;
    private int ftpsPort;
    private String ftpsUsername;
    private String ftpsPassword;
    private String path;

    private String basePath;
    private String requestPath;

    public FtpsFileService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setFtpsHost(String ftpsHost) {
        this.ftpsHost = ftpsHost;
    }

    public void setFtpsPort(int ftpsPort) {
        this.ftpsPort = ftpsPort;
    }

    public void setFtpsUsername(String ftpsUsername) {
        this.ftpsUsername = ftpsUsername;
    }

    public void setFtpsPassword(String ftpsPassword) {
        this.ftpsPassword = ftpsPassword;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    protected FileInputStream createFileInputStream(String filePath) throws FileNotFoundException, FileNotFoundException {
        return new FileInputStream(filePath);
    }

    protected FileOutputStream createFileOutputStream(String filePath) throws FileNotFoundException {
        return new FileOutputStream(filePath);
    }

    protected FTPSClient createFtpsClient() throws IOException {
        FTPSClient ftpsClient = new FTPSClient("TLS", false);
        ftpsClient.setTrustManager(TrustManagerUtils.getAcceptAllTrustManager());
        ftpsClient.setEnabledProtocols(new String[]{"TLSv1.2"});

        ftpsClient.connect(ftpsHost, ftpsPort);
//        ftpsClient.login(ftpsUsername, "MomXyYcmCB@7#88a");
        int reply = ftpsClient.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpsClient.disconnect();
            throw new FileServiceTransferException("FTP server refused connection.");
        }

        if (!ftpsClient.login(ftpsUsername, ftpsPassword)) {
            ftpsClient.logout();
            throw new FileServiceTransferException("FTP server login failed.");
        }

        ftpsClient.enterLocalPassiveMode();
        ftpsClient.setFileType(FTP.LOCAL_FILE_TYPE);
        return ftpsClient;
    }

    public void uploadFile(String localFilePath, String remoteFilePath) throws IOException {
        FTPSClient ftpsClient = null;
        FileInputStream fis = null;
        try {
            ftpsClient = createFtpsClient();
            fis = createFileInputStream(localFilePath); // Use the factory method here
            log.debug("storing file in the FTPS server...");
            ftpsClient.storeFile(remoteFilePath, fis);
        } finally {
            closeQuietly(fis);
            disconnectFtpsClient(ftpsClient);
        }
    }

    public FTPSClient connectToFtpsServer() throws IOException {
        return createFtpsClient();
    }




    public void uploadFile(FTPSClient ftpsClient, InputStream inputStream, String fileNameWithExtension, boolean isOverWrite) throws IOException {
        InputStream is = null;
        try {
            is = new BufferedInputStream(inputStream, 1024);
            String newRemoteFilePath = FtpsUtils.getFileNameWithPath(fileNameWithExtension, path + "/");

            log.debug("Path: {}", path);
            log.debug("Filename with extension: {}", fileNameWithExtension);
            log.debug("New RemoteFileName: {}", newRemoteFilePath);
//            checkDirectoryPath(ftpsClient);
            createDirectories(ftpsClient, path);

            log.info("Storing file in FTPS server in this location: {}", newRemoteFilePath);
            log.info("overwriting files: {}", isOverWrite);

            if (!isOverWrite) {
                boolean isFileExisting = isFileExists(ftpsClient, fileNameWithExtension);
                log.info("File exists: {}", isFileExisting);
                if (isFileExisting) {
                    log.info("File already exists...");
                    //throw error
                    throw new FileServiceTransferException("File already exists in the server.");
                } else {
                    ftpsClient.storeFile(newRemoteFilePath, is);
                }
            } else {
                ftpsClient.storeFile(newRemoteFilePath, is);
            }

        } finally {
            closeQuietly(inputStream, is);
            disconnectFtpsClient(ftpsClient);
        }
    }

    public void checkDirectoryPath(FTPSClient ftpsClient) throws IOException {
        //check if dir is existing first
        log.info("change directory to: {}", path);

        if (!ObjectUtils.isEmpty(requestPath)) {
            boolean isDirExisting = ftpsClient.changeWorkingDirectory(path);
            if (isDirExisting) {
                log.info("Successfully changed working directory to: {}", path);
            } else {
                log.info("Failed to change working directory to: {}, will use basedirectory: {}", path, basePath);
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
    }

    private void createDirectories(FTPSClient ftpsClient, String path) throws IOException {
        boolean dirExists = true;
        String directories[] = StringUtils.split(path, File.separator);
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
    }

    public void downloadFile(String remoteFilePath, String localFilePath) throws IOException {
        FTPSClient ftpsClient = null;
        FileOutputStream fos = null;
        try {
            ftpsClient = createFtpsClient();
            fos = createFileOutputStream(localFilePath); // Use the factory method here
            ftpsClient.retrieveFile(remoteFilePath, fos);
        } finally {
            closeQuietly(fos);
            disconnectFtpsClient(ftpsClient);
        }
    }

    public void downloadFile(TenantService tenantService, DownloadRequest request) throws IOException {
        FTPSClient ftpsClient = null;

        try {
            ftpsClient = createFtpsClient();

            String remoteDirectory = request.getPath();
            String fileMask = request.getFileMask();
            FTPFile[] files = null;
            files = ftpsClient.listFiles(remoteDirectory);

            List<String> filePaths = Arrays.stream(files)
                    .map(file -> FilenameUtils.concat(remoteDirectory, file.getName()))
                    .collect(Collectors.toList());

            List<String> filteredList = FtpsUtils.filterList(filePaths, fileMask);

            Map<String, String> adjustedFileNames = FtpsUtils.adjustAttachmentFileNames(filteredList, FileSystems.getDefault().getSeparator());

            for (FTPFile file : files) {
                if (file.isFile()) {
                    try {
                        processSingleFile(tenantService, file, ftpsClient, request.getTargetTable(), request.getTargetSysId(), remoteDirectory, adjustedFileNames);
                    } catch (Exception e) {
                        log.error("Failed to process file: {} , due to : {}", file.getName(), e);
                    }
                }
            }
        } finally {
            disconnectFtpsClient(ftpsClient);
        }
    }


    public FtpsDownloadResponse downloadFile(FTPSClient ftpsClient, TenantService tenantService, DownloadRequest request) throws FileServiceTransferException {
        log.info("inside downloadFile...");
        //        test config
//        cryptoConfiguration = FtpsUtils.getTestCryptoConfiguration();

        FtpsDownloadResponse response = new FtpsDownloadResponse();
        response.setOk(true);
        try {
            String remoteDirectory = request.getPath();

            String fileMask = request.getFileMask();

            if(StringUtils.isBlank(fileMask)){
                fileMask = "*";
            }

            log.info("Filemask : {}", fileMask);
//            FTPFile[] files = null;
//            files = ftpsClient.listFiles(path);

            String finalFileMask = fileMask;
            FTPFile[] files = ftpsClient.listFiles(path, new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile file) {
                    Pattern compiledPattern = Pattern.compile(FtpsUtils.wildcardRegex(finalFileMask));
                    if (compiledPattern.matcher(file.getName()).matches()) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            List<String> filePaths = Arrays.stream(files)
                    .map(file -> FilenameUtils.concat(remoteDirectory, file.getName()))
                    .collect(Collectors.toList());

            Map<String, String> adjustedFileNames = FtpsUtils.adjustAttachmentFileNames(filePaths, FileSystems.getDefault().getSeparator());

            if (files != null && files.length > 0) {
                for (FTPFile file : files) {
                    if (file.isFile()) {
                        try {
                            log.info("Getting files from FTPS: {}", file.getName());
                            processSingleFile(tenantService, file, ftpsClient, request, adjustedFileNames);
                        } catch (Exception e) {
                            response.setOk(false);
                            response.setMessage(String.format("Failed to process file: %s , due to : %s", file.getName(), e.getMessage()));
                            break;
                        }
                    }
                }
            } else {
                log.info("No files that matches {} in {}", fileMask, path);
            }
            if (response.isOk()) {
                response.setMessage("Successfully download.");
            }

        } catch (IOException e) {
            response.setOk(false);
            response.setMessage(String.format("Unable to list contents of : %s", request.getPath(), e.getMessage()));
        } finally {
            disconnectFtpsClient(ftpsClient);
        }

        return response;
    }

    public void processSingleFile(TenantService tenantService, FTPFile file, FTPSClient ftpsClient, String table, String recordId, String remoteDirPath,
                                  Map<String, String> adjustedFileNames) throws IOException {

        String remoteFilePath = FilenameUtils.concat(remoteDirPath, file.getName());
        String adjustedFileName = adjustedFileNames.get(remoteFilePath);
        Path tempFile = null;

        try {
            String tenantCode = tenantService.getCurrentTenantConfig().getTenant().getTenantCode();
            ServiceNowClientConfiguration cfg = tenantService.createSnClientConfig(tenantCode);
            ServiceNowHttpClient httpClient = new ServiceNowHttpClient(cfg);
            AttachmentApi attachmentApi = new AttachmentApiImpl(httpClient);

            log.info("Downloading file from ftps: {}", remoteFilePath);

            tempFile = Files.createTempFile("ftps-", "-tmp");

            try (OutputStream output = Files.newOutputStream(tempFile)) {
                if (!ftpsClient.retrieveFile(remoteFilePath, output)) {
                    log.error("Failed to retrieve file: " + file.getName());
                    return;
                }
            } catch (IOException e) {
                log.error("I/O error occurred during file download: " + e.getMessage());
            }

            try (InputStream inputStream = Files.newInputStream(tempFile)) {
                try {
                    log.info("Uploading file to ServiceNow: {}", remoteFilePath);
                    attachmentApi.create(adjustedFileName, table, recordId, inputStream);
                } catch (Exception e) {
                    log.error("Failed to create Attachment: " + e.getMessage());
                    return;
                }
            } catch (IOException e) {
                log.error("I/O error occurred during attachment creation: " + e.getMessage());
            }

        } catch (IOException e) {
            log.error("I/O error occurred: " + e.getMessage());
        } finally {
            // clean up the temporary file
            if (tempFile != null) {
                Files.delete(tempFile);
            }

        }
    }

    public void processSingleFile(TenantService tenantService, FTPFile file, FTPSClient ftpsClient, DownloadRequest request,
                                  Map<String, String> adjustedFileNames) throws FileServiceTransferException {

        String remoteFilePath = FilenameUtils.concat(request.getPath(), file.getName());
        String adjustedFileName = adjustedFileNames.get(remoteFilePath);
        Path tempFile = null;

        try {
            String tenantCode = tenantService.getCurrentTenantConfig().getTenant().getTenantCode();
            ServiceNowClientConfiguration cfg = tenantService.createSnClientConfig(tenantCode);
            ServiceNowHttpClient httpClient = new ServiceNowHttpClient(cfg);
            AttachmentApi attachmentApi = new AttachmentApiImpl(httpClient);

            log.info("Downloading file from ftps: {}", remoteFilePath);
            log.info("Downloading file from ftps: {}", path + "/" + adjustedFileName);

            tempFile = Files.createTempFile("ftps-", "-tmp");

            try (OutputStream output = Files.newOutputStream(tempFile)) {
                log.info("Creating temp file : {}", file.getName());
                if (!ftpsClient.retrieveFile(path + "/" + adjustedFileName, output)) {
                    log.error("Failed to retrieve file: " + file.getName());
                    throw new FileServiceTransferException("Failed to retrieve file: " + file.getName());
                }
            } catch (IOException e) {
                log.error("I/O error occurred during file download: " + e.getMessage());
                throw new FileServiceTransferException("I/O error occurred during file download: " + e.getMessage(), e);
            }

            try (InputStream inputStream = Files.newInputStream(tempFile)) {

                InputStream decryptInputStream = null;
                if (request.isDecrypt()) {
                    try {
                        CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
                                CryptoConfiguration.class);

                        PgpDecryptor pgpDecryptor = new PgpDecryptor(
                                encryptionService.decrypt(cryptoConfiguration.getPrivateKey(), cryptoConfiguration.getSalt()),
                                encryptionService.decrypt(cryptoConfiguration.getPassword(), cryptoConfiguration.getSalt()));
                        byte[] decryptedBytes = pgpDecryptor.decrypt(inputStream.readAllBytes());
                        decryptInputStream = new ByteArrayInputStream(decryptedBytes);
                    } catch (PGPException e) {
                        log.error("unable to decrypt file: {}", remoteFilePath);
                        throw new FileServiceTransferException("unable to decrypt file: " + remoteFilePath, e);
                    }

                    // test
//                    File dfile = new File("/Users/valtejano/Downloads/ftps/Paymentfile-20230518_test3.csv");
//                    FileUtils.copyInputStreamToFile(decryptInputStream, dfile);

                    try {
                        log.info("Uploading decrypted file to ServiceNow: {}", remoteFilePath);
                        attachmentApi.create(request.getTargetTable(), request.getTargetSysId(), adjustedFileName, decryptInputStream, 0, request.getEncryptionContext());
                    } catch (Exception e) {
                        log.error("Failed to create Attachment: " + e.getMessage());
                        throw new FileServiceTransferException("Failed to create Attachment: " + e.getMessage(), e);
                    }
                }
                else {

                    try {
                        log.info("Uploading file to ServiceNow: {}", remoteFilePath);
                        attachmentApi.create(request.getTargetTable(), request.getTargetSysId(), adjustedFileName, inputStream, 0, request.getEncryptionContext());
                    } catch (Exception e) {
                        log.error("Failed to create Attachment: " + e.getMessage());
                        throw new FileServiceTransferException("Failed to create Attachment: " + e.getMessage(), e);
                    }
                }

                if (null != request.getOptions() && !StringUtils.equalsIgnoreCase("none", request.getOptions().getAfterAction())) {
                    if (StringUtils.equalsIgnoreCase("delete", request.getOptions().getAfterAction())) {
                        String toDelete = FilenameUtils.concat(path, adjustedFileName);
                        log.info("After action : Deleting " + toDelete);
                        deleteRemoteFile(ftpsClient, toDelete);
                    } else if (StringUtils.equalsIgnoreCase("rename", request.getOptions().getAfterAction())) {
                        String toRename = FilenameUtils.concat(path, adjustedFileName);
                        String newFileName = String.format("mc_%s_%s", FilenameUtils.getName(toRename), String.valueOf(System.currentTimeMillis() / 1000l));
                        String newPath = FilenameUtils.concat(path, newFileName);
                        log.info("After action : Rename " + toRename + " to " + newPath);
                        renameRemoteFile(ftpsClient, toRename, newPath);
                    }
                }
            } catch (IOException e) {
                log.error("I/O error occurred during attachment creation: " + e.getMessage());
                throw new FileServiceTransferException("I/O error occurred during attachment creation: " + e.getMessage(), e);
            }
        } catch (IOException e) {
            log.error("Error when creating temp file : " + e.getMessage());
            throw new FileServiceTransferException("Error when creating temp file: " + e.getMessage(), e);
        } finally {
            // clean up the temporary file
            if (tempFile != null) {
                try {
                    Files.delete(tempFile);
                } catch (IOException e) {
                    log.error("Error when deleting temp file : " + e.getMessage());
                }
            }
        }
    }

    private void deleteRemoteFile(FTPSClient ftpsClient, String filename) throws FileServiceTransferException {
        try {
            if (!ftpsClient.deleteFile(filename)) {
                throw new FileServiceTransferException(String.format("failed to delete file [%s].", filename));
            }
        } catch (IOException e) {
            throw new FileServiceTransferException(String.format("failed to delete file [%s]. %s", filename, e));
        }
    }

    private void renameRemoteFile(FTPSClient ftpsClient, String oldFileName, String newFileName) throws FileServiceTransferException {
        try {
            if (!ftpsClient.rename(oldFileName, newFileName)) {
                throw new FileServiceTransferException(String.format("failed to rename file [%s].", oldFileName));
            }
        } catch (IOException e) {
            throw new FileServiceTransferException(String.format("failed to rename file [%s]. %s", oldFileName, e));
        }
    }

    private void disconnectFtpsClient(FTPSClient ftpsClient) {
        if (ftpsClient != null) {
            try {
                ftpsClient.logout();
                ftpsClient.disconnect();
            } catch (IOException e) {
                log.error("Error occurred during FTPS client disconnection: " + e.getMessage());
            }
        }
    }

    private void closeQuietly(Closeable... closeables) {
        for (Closeable closeable : closeables) {
            try {
                if (closeable != null) {
                    closeable.close();
                }
            } catch (IOException e) {
                log.error("Error occurred during stream closing: " + e.getMessage());
            }
        }
    }

    private boolean isFileExists(FTPSClient ftpsClient, String fileName) throws IOException {
        FTPFile[] files = ftpsClient.listFiles();
        for (FTPFile file : files) {
            if (file.isFile() && file.getName().equals(fileName)) {
                return true;
            }
        }
        return false;
    }
}
