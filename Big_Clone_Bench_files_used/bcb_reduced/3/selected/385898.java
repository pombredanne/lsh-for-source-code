package uk.org.brindy.jwebdoc.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import uk.org.brindy.jwebdoc.WebDocRuntimeException;

/**
 * @author brindy
 */
public final class MD5 {

    private static final int HEX = 16;

    private MD5() {
    }

    /**
     * Encrypt the given string as md5 hash.
     * @param text the text
     * @return the hash
     */
    public static String encrypt(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            throw new WebDocRuntimeException(ex);
        }
        md.update(text.getBytes());
        BigInteger hash = new BigInteger(1, md.digest());
        return hash.toString(HEX);
    }
}
