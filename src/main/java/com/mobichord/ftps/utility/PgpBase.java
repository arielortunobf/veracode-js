package com.mobichord.ftps.utility;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.jcajce.JcaPGPPublicKeyRingCollection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Iterator;
import java.util.Objects;

public abstract class PgpBase {

    static {
        // Add Bouncy castle to JVM
        if (Objects.isNull(Security.getProvider(BouncyCastleProvider.PROVIDER_NAME))) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    public static PGPPublicKey getPublicKey(String publicKey) throws IOException, PGPException {
        InputStream in=new ByteArrayInputStream(publicKey.getBytes());
        in = org.bouncycastle.openpgp.PGPUtil.getDecoderStream(in);

        JcaPGPPublicKeyRingCollection pgpPub = new JcaPGPPublicKeyRingCollection(in);
        in.close();

        PGPPublicKey pgpKey = null;
        Iterator<PGPPublicKeyRing> rIt = pgpPub.getKeyRings();
        while (pgpKey == null && rIt.hasNext())
        {
            PGPPublicKeyRing kRing = rIt.next();
            Iterator<PGPPublicKey> kIt = kRing.getPublicKeys();
            while (pgpKey == null && kIt.hasNext())
            {
                PGPPublicKey k = kIt.next();

                if (k.isEncryptionKey())
                {
                    pgpKey = k;
                }
            }
        }

        return pgpKey;
    }
}
