package es.caib.signatura.provider.impl.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class SHA1Util {

    public SHA1Util() {
        super();
    }

    public static byte[] digest(byte data[]) throws NoSuchAlgorithmException, NoSuchProviderException {
        MessageDigest digester;
        digester = MessageDigest.getInstance("SHA-1", "BC");
        digester.update(data);
        return digester.digest();
    }
}
