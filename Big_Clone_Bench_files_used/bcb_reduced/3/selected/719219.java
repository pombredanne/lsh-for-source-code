package org.homemotion.dao;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.inject.Singleton;
import org.apache.log4j.Logger;

@Singleton
public final class MD5Encrypter implements Encrypter {

    private static Logger LOG = Logger.getLogger(MD5Encrypter.class);

    private MessageDigest mdEnc;

    public MD5Encrypter() {
        try {
            mdEnc = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            LOG.fatal("Failed to load MD5 algorithm!", e);
        }
    }

    public String encrypt(String toEnc) {
        this.mdEnc.update(toEnc.getBytes(), 0, toEnc.length());
        return new BigInteger(1, this.mdEnc.digest()).toString(16);
    }
}
