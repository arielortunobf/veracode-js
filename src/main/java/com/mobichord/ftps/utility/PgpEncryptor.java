package com.mobichord.ftps.utility;

import com.appchord.data.tenant.configurations.CryptoConfiguration;
import com.mobichord.servicenow.api.http.AttachmentDownloadResponse;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.*;
import org.bouncycastle.openpgp.operator.jcajce.*;

import java.io.*;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;


public class PgpEncryptor extends PgpBase {

    /**
     * Encrypt inputStream and return as ByteArrayOutputStream
     * @param inputStream The input stream from {@link AttachmentDownloadResponse}
     * @param fileName The file name of the encrypted file
     * @param armor Enable armor
     * @param publicKeyString The public key from {@link CryptoConfiguration}
     * @return ByteArrayOutputStream of the encrypted stream that can be later be converted to input stream for upload.
     * @throws IOException
     * @throws PGPException
     */
    public static ByteArrayOutputStream encryptStream(InputStream inputStream, String fileName, boolean armor, String publicKeyString)
            throws IOException, PGPException {

        byte[] inputBytes = inputStream.readAllBytes();

        ByteArrayOutputStream encOut = new ByteArrayOutputStream();

        OutputStream out = encOut;
        if (armor) {
            out = new ArmoredOutputStream(out);
        }

        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(
                PGPCompressedDataGenerator.ZIP);
        OutputStream cos = comData.open(bOut);
        PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();

        OutputStream pOut = lData.open(cos,
                PGPLiteralData.BINARY, fileName,
                inputBytes.length,
                new Date()
        );
        pOut.write(inputBytes);

        lData.close();
        comData.close();

       PGPPublicKey key = getPublicKey(publicKeyString);

        PGPEncryptedDataGenerator cPk = new PGPEncryptedDataGenerator(
                new JcePGPDataEncryptorBuilder(SymmetricKeyAlgorithmTags.CAST5).setSecureRandom(
                        new SecureRandom()).setProvider(BouncyCastleProvider.PROVIDER_NAME).setWithIntegrityPacket(true));
        cPk.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(key)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME).setSecureRandom(new SecureRandom()));

        byte[] bytes = bOut.toByteArray();

        OutputStream cOut = cPk.open(out, bytes.length);
        cOut.write(bytes);
        cOut.close();
        out.close();

        return encOut;
    }

}
