package com.mobichord.ftps.service;

import com.appchord.data.tenant.configurations.SftpConfiguration;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class FtpsFileServiceFactory {

    final
    EncryptionService encryptionService;

    public FtpsFileServiceFactory(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public FtpsService createFtpsFileService(SftpConfiguration config, String path) {
        FtpsService ftpsFileService = new FtpsFileService(encryptionService);

        configureFtpsFileService(config, ftpsFileService, path);

        return ftpsFileService;
    }

    protected void configureFtpsFileService(SftpConfiguration config, FtpsService ftpsFileService, String requestPath) {
        // Configure the FtpsFileService instance based on the client information
        ftpsFileService.setFtpsHost(config.getHost());
        ftpsFileService.setFtpsPort(config.getPort());
        ftpsFileService.setFtpsUsername(config.getUsername());
        ftpsFileService.setFtpsPassword(encryptionService.decrypt(config.getPassword(), config.getSalt()));

        Path confPath = Paths.get(config.getPath());
        Path combinedPathObj = confPath.resolve(requestPath);
        String combinedPathString = combinedPathObj != null ? combinedPathObj.toString() : config.getPath();
        //this is taken from the default configuration: base path
        String path = ObjectUtils.isEmpty(requestPath) ? config.getPath() : combinedPathString;
        ftpsFileService.setPath(path);
        ftpsFileService.setBasePath(config.getPath());
        ftpsFileService.setRequestPath(requestPath);
    }
}

