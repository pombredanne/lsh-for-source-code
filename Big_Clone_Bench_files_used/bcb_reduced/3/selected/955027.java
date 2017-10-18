package com.jeronimo.eko.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.jeronimo.eko.core.EkoRuntimeException;

/**
 * Some cryto functions
 * @author J�r�me Bonnet
 *
 */
public class CryptoUtils {

    public static byte[] computeSHA1FromString(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            byte[] input = digest.digest(s.getBytes("UTF8"));
            return input;
        } catch (NoSuchAlgorithmException e) {
            throw new EkoRuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new EkoRuntimeException(e);
        }
    }
}
