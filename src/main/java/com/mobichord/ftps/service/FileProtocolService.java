package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.mobichord.ftps.data.FileProtocolRequest;
import com.mobichord.ftps.exception.FileServiceTransferException;
import com.mobichord.ftps.message.ProtocolDownloadRequest;
import com.mobichord.ftps.message.ProtocolUploadRequest;
import com.mobichord.ftps.utility.FtpsUtils;
import com.mobichord.ftps.utility.PgpDecryptor;
import com.mobichord.ftps.utility.PgpEncryptor;
import com.mobichord.servicenow.api.http.AttachmentDownloadResponse;
import com.mobichord.tenant.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.net.ftp.FTPFile;
import org.bouncycastle.openpgp.PGPException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
@Slf4j
public class FileProtocolService {

    private final ProtocolServiceFactory protocolServiceFactory;

    private final EncryptionService encryptionService;

    final
    TenantService tenantService;

    public FileProtocolService(ProtocolServiceFactory protocolServiceFactory, EncryptionService encryptionService, TenantService tenantService) {
        this.protocolServiceFactory = protocolServiceFactory;
        this.encryptionService = encryptionService;
        this.tenantService = tenantService;
    }

    public void upload(ProtocolConfiguration configuration, ProtocolUploadRequest request, AttachmentDownloadResponse attachment) throws FileServiceTransferException {

        try {
            //this will create an instance of the required protocol eg. FTPS SFTP FTP etc
            ProtocolService service = protocolServiceFactory.getProtocol(configuration, encryptionService);

            //assign variables
            InputStream stream = attachment.stream();
            String fileName = FtpsUtils.extractStringWithinQuotes(attachment.getFileName());

            //check if encryption is needed
            if (request.isEncrypt()) {
                String fileNoExtension = FilenameUtils.getBaseName(fileName);
                String extension = StringUtils.isBlank(request.getEncryptedFileExt()) ? "gpg" : request.getEncryptedFileExt();
                fileName = fileNoExtension + "." + extension;

                //need to get CryptoConfig
                CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
                        CryptoConfiguration.class);

                if (ObjectUtils.isEmpty(cryptoConfiguration)) {
                    throw new FileServiceTransferException(String.format("CryptoConfiguration for [%s] not found.",request.getCryptoCfgId()));
                }

                ByteArrayOutputStream baos = PgpEncryptor.encryptStream(stream, fileName, true,
                        encryptionService.decrypt(cryptoConfiguration.getPublicKey(), cryptoConfiguration.getSalt()));

                stream = new ByteArrayInputStream(baos.toByteArray());
            }

            //create reqyest parameter  to be sent to protocolservice
            FileProtocolRequest rq = FileProtocolRequest.builder()
                    .stream(stream)
                    .fileName(fileName)
                    .isOverWrite(request.isOverwrite())
                    .build();

            service.upload(rq);
        } catch (IOException | PGPException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }

    }

    public FTPFile[] getAvailableFiles(ProtocolConfiguration configuration, ProtocolDownloadRequest request) throws FileServiceTransferException {
        try {
            //this will create an instance of the required protocol eg. FTPS SFTP FTP etc
            ProtocolService service = protocolServiceFactory.getProtocol(configuration, encryptionService);

            FileProtocolRequest rq = FileProtocolRequest.builder()
                    .fileMask(request.getFileMask())
                    .build();

            //get files from file server
            FTPFile[] files = service.getListOfAvailableFiles(rq);

            if (ObjectUtils.isEmpty(files)) {
                throw new FileServiceTransferException("No file/s found in the current directory path.");
            }

            return files;

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }
    }

    public InputStream download(ProtocolConfiguration configuration, ProtocolDownloadRequest request) throws IOException {
        InputStream is;
        InputStream decryptInputStream = null;

        try {
            //this will create an instance of the required protocol eg. FTPS SFTP FTP etc
            ProtocolService service = protocolServiceFactory.getProtocol(configuration, encryptionService);
            String remoteFilePath = FilenameUtils.concat(configuration.getPath(), request.getFileName());
            String adjustedFileName = request.getAdjustedFileName();

            FileProtocolRequest rq = FileProtocolRequest.builder()
                    .fileName(remoteFilePath)
                    .adjustedFileName(adjustedFileName)
                    .build();

            //get files from file server
            is = service.download(rq);

            if (ObjectUtils.isEmpty(is)) {
                throw new FileServiceTransferException("No file/s found in the current directory path.");
            }

            //decrypt if needed
            if(request.isDecrypt()){
                try {
                    CryptoConfiguration cryptoConfiguration = tenantService.getConfiguration(request.getCryptoCfgId(),
                            CryptoConfiguration.class);

                    if (ObjectUtils.isEmpty(cryptoConfiguration)) {
                        throw new FileServiceTransferException(String.format("CryptoConfiguration for [%s] not found.",request.getCryptoCfgId()));
                    }

                    PgpDecryptor pgpDecryptor = new PgpDecryptor(
                            encryptionService.decrypt(cryptoConfiguration.getPrivateKey(), cryptoConfiguration.getSalt()),
                            encryptionService.decrypt(cryptoConfiguration.getPassword(), cryptoConfiguration.getSalt()));

                    //used for local testing
//                    PgpDecryptor pgpDecryptor = new PgpDecryptor(
//                            encryptionService.decrypt(cryptoConfiguration.getPrivateKey(), cryptoConfiguration.getSalt()),
//                            "ftp.123456.*");

                    byte[] decryptedBytes = pgpDecryptor.decrypt(is.readAllBytes());
                    decryptInputStream = new ByteArrayInputStream(decryptedBytes);
                    return decryptInputStream;
                } catch (PGPException e) {
                    log.error("unable to decrypt file: {}", remoteFilePath);
                    throw new FileServiceTransferException("unable to decrypt file: " + remoteFilePath, e);
                }
            }

            return is;

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }
    }

    public void delete(ProtocolConfiguration configuration, ProtocolDownloadRequest request) throws IOException {
        try {
            //this will create an instance of the required protocol eg. FTPS SFTP FTP etc
            ProtocolService service = protocolServiceFactory.getProtocol(configuration, encryptionService);
            String remoteFilePath = FilenameUtils.concat(configuration.getPath(), request.getAdjustedFileName());
            String adjustedFileName = request.getAdjustedFileName();

            FileProtocolRequest rq = FileProtocolRequest.builder()
                    .fileName(remoteFilePath)
                    .build();

           service.delete(rq);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }
    }

    public void rename(ProtocolConfiguration configuration, ProtocolDownloadRequest request) throws FileServiceTransferException {
        try {
            //this will create an instance of the required protocol eg. FTPS SFTP FTP etc
            ProtocolService service = protocolServiceFactory.getProtocol(configuration, encryptionService);
            String toRename = FilenameUtils.concat(configuration.getPath(), request.getAdjustedFileName());
            String newFileName = String.format("mc_%s_%s", FilenameUtils.getName(toRename), String.valueOf(System.currentTimeMillis() / 1000l));
            String newPath = FilenameUtils.concat(configuration.getPath(), newFileName);

            FileProtocolRequest rq = FileProtocolRequest.builder()
                    .oldFileName(toRename)
                    .newFileName(newPath)
                    .build();

            service.rename(rq);

        } catch (IOException e) {
            log.error(ExceptionUtils.getStackTrace(e));
            throw new FileServiceTransferException(ExceptionUtils.getStackTrace(e));
        }
    }
}