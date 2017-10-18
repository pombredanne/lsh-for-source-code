package com.planetachewood.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Encoder;

/**
 * Contains static cryptographic utility methods.
 * 
 * @author <a href="mailto:mark.a.allen@gmail.com">Mark Allen</a>
 * @since Oct 13, 2006
 * @version $Revision: 7 $ $Date: 2007-01-12 22:20:55 -0500 (Fri, 12 Jan 2007) $
 */
public final class CryptoUtils {

    private static final String HASH_ALGORITHM = "SHA";

    private static final String INPUT_ENCODING = "UTF-8";

    /**
     * Prevents instantiation.
     */
    private CryptoUtils() {
    }

    /**
     * Performs a cryptographic hash on the input value.
     * 
     * @param value The value to hash.
     * @return The hash of the input value.
     */
    public static String hash(String value) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new CryptoException(e);
        }
        try {
            md.update(value.getBytes(INPUT_ENCODING));
        } catch (UnsupportedEncodingException e) {
            throw new CryptoException(e);
        }
        return new BASE64Encoder().encode(md.digest());
    }
}
