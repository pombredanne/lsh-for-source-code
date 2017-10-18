package com.leemba.monitor.server.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import org.apache.log4j.Logger;
import org.apache.mina.util.Base64;
import org.terracotta.modules.annotations.InstrumentedClass;

/**
 *
 * @author mrjohnson
 */
@InstrumentedClass
public class PasswordCrypt {

    private static final transient Logger log = Logger.getLogger(PasswordCrypt.class);

    private final String salt;

    public PasswordCrypt() {
        Random rand = new Random();
        long s = rand.nextLong();
        salt = s + "";
    }

    public String hash(String password) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            log.info("No sha-256 available");
            try {
                digest = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                log.fatal("sha-1 is not available", e);
                throw new RuntimeException("Couldn't get a hash algorithm from Java");
            }
        }
        try {
            digest.reset();
            digest.update((salt + password).getBytes("UTF-8"));
            byte hash[] = digest.digest();
            return new String(Base64.encodeBase64(hash, false));
        } catch (Throwable t) {
            throw new RuntimeException("Couldn't hash password");
        }
    }
}
