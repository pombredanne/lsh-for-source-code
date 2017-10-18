package net.conselldemallorca.ad.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Aplica la funció de hash SHA1
 * 
 * @author Limit Tecnologies <limit@limit.es>
 */
public class SHA1 {

    public static String getHash(String message) throws NoSuchAlgorithmException {
        String hash = "";
        byte[] buffer = message.getBytes();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(buffer);
        byte[] digest = md.digest();
        for (byte aux : digest) {
            int b = aux & 0xff;
            if (Integer.toHexString(b).length() == 1) hash += "0";
            hash += Integer.toHexString(b);
        }
        return hash;
    }
}
