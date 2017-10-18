package net.sf.wsdl2jibx.ext;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

    private static String hexa = "0123456789abcdef";

    public static String getFingerPrint(byte[] xml, String algo) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algo);
        byte[] hash = digest.digest(xml);
        return toHexa(hash);
    }

    private static String toHexa(byte[] digest) {
        StringBuffer buff = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            buff.append(hexa.charAt((digest[i] >> 4) & 0xF));
            buff.append(hexa.charAt(digest[i] & 0xF));
        }
        return buff.toString();
    }
}
