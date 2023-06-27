package com.mobichord.ftps.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;

/*this will be instantiated by the processor depending on which type of protocol it needs*/
@Service
@Slf4j
public class ProtocolServiceFactory {

    public ProtocolService getProtocol(ProtocolConfiguration configuration, EncryptionService encryptionService) throws IOException {
        return switch (configuration.getType().toUpperCase()) {
            case "FTPS" -> new FtpsProtocolService(configuration, encryptionService);
            case "FTP" -> throw new IllegalArgumentException("Invalid protocol type: " + configuration.getType());
            case "SFTP" -> throw new IllegalArgumentException("Invalid protocol type: " + configuration.getType());
            default -> throw new IllegalArgumentException("Invalid protocol type: " + configuration.getType());
        };
    }
}

