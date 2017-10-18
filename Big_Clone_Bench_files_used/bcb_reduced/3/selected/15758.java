package com.rhythm.commons.security;

import com.rhythm.base.Nulls;
import com.rhythm.base.Strings;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Michael J. Lee
 */
public class Encrypter {

    private static final String MD5 = "MD5";

    private static final String SHA1 = "SHA-1";

    private static final String ISO_CHARSET = "iso-8859-1";

    /**
     * MD5 is a cryptographic message digest algorithm.  This method produces a 
     * 128-bit encryption of the given String.
     * 
     * @param str
     * @return a 128-bit encryption of the given str
     * @throws NullPointerException if str is null.
     */
    public static synchronized String toMD5(String str) {
        Nulls.failIfNull(str, "Cannot create an MD5 encryption form a NULL string");
        String hashword = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            md5.update(str.getBytes());
            BigInteger hash = new BigInteger(1, md5.digest());
            hashword = hash.toString(16);
            return Strings.padLeft(hashword, 32, "0");
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return hashword;
    }

    /**
     * SHA is a cryptographic message digest algorithm similar to MD5. This 
     * method produces a 160-bit encryption of the given String.
     * 
     * @param str
     * @return a 160-bit encryption of the given str
     * @throws NullPointerException if str is null.
     */
    public static synchronized String toSHA1(String str) {
        Nulls.failIfNull(str, "Cannot create an SHA1 encryption form a NULL string");
        try {
            MessageDigest md;
            md = MessageDigest.getInstance(SHA1);
            byte[] sha1hash = new byte[40];
            md.update(str.getBytes(ISO_CHARSET), 0, str.length());
            sha1hash = md.digest();
            return convertToHex(sha1hash);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private static synchronized String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) {
                    buf.append((char) ('0' + halfbyte));
                } else {
                    buf.append((char) ('a' + (halfbyte - 10)));
                }
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
