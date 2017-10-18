package org.commonlibrary.lcms.support.crypto;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.binary.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Provides utility functionality to use message digestion algorithms
 * @author diegomunguia
 * Date: Aug 5, 2008
 * Time: 3:08:13 PM
 */
public class MessageDigester {

    /**
     * Provides SHA-256 message digestion to encrypt (one-way) a message
     * @param message to encrypt
     * @return digested message
     * @throws NoSuchAlgorithmException when the current VM doesn't support SHA-256
     * @throws EncoderException when the Base64 encoding fails
     */
    public String digest(String message) throws NoSuchAlgorithmException, EncoderException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(message.getBytes());
        byte[] raw = messageDigest.digest();
        byte[] chars = new Base64().encode(raw);
        return new String(chars);
    }
}
