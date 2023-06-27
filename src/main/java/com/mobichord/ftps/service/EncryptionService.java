package com.mobichord.ftps.service;

public interface EncryptionService {

    String encrypt(String toEncrypt);
    String decrypt(String encryptedString, String salt);
}
