package com.mobichord.ftps.service;

import com.mobichord.security.chipher.AesChipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class EncryptionServiceImpl implements EncryptionService {

    private final Environment environment;

    @Autowired
    public EncryptionServiceImpl(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String encrypt(String toEncrypt) {
        return null;
    }

    @Override
    public String decrypt(String encryptedString, String salt) {
        String secret = environment.getProperty("MC_SECRET");
        return AesChipher.decrypt(encryptedString, salt + secret);
    }
}
