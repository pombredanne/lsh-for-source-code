package com.divosa.eformulieren.web.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
* This class defines common routines for generating
* authentication signatures for AWS requests.
* @author arjen
*/
public class SHA1Signature {

    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";

    /**
* Computes RFC 2104-compliant HMAC signature.
* * @param data
* The data to be signed.
* @param key
* The signing key.
* @return
* The Base64-encoded RFC 2104-compliant HMAC signature.
* @throws
* java.security.SignatureException when signature generation fails
*/
    public static String calculateSHA1(String data, String key) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        data += key;
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(data.getBytes("iso-8859-1"), 0, data.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
