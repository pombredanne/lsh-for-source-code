package com.metamech.jabber;

import java.security.MessageDigest;
import java.security.SecureRandom;
import org.apache.log4j.Logger;
import com.metamech.io.HexString;
import com.metamech.log.Log;
import com.metamech.vorpal.handler.RosterHandler;

public class Authenticator {

    private static Logger logger = Logger.getLogger(Authenticator.class);

    MessageDigest sha;

    static SecureRandom random;

    static {
        try {
            logger.info("create SecureRandom using SHA1PRNG");
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (Exception ex) {
            logger.error("Could not create SecureRandom ", ex);
            System.exit(-1);
        }
    }

    public Authenticator() {
        try {
            sha = MessageDigest.getInstance("SHA");
        } catch (Exception ex) {
            logger.error("Could not create SHA MessageDigest ", ex);
            System.exit(-1);
        }
    }

    public String getZeroKHash(int sequence, byte[] token, byte[] password) {
        byte[] runningHash = sha.digest(password);
        sha.update(HexString.toString(runningHash).getBytes());
        runningHash = sha.digest(token);
        for (int i = 0; i < sequence; i++) {
            runningHash = sha.digest(HexString.toString(runningHash).getBytes());
        }
        return HexString.toString(runningHash);
    }

    public String getDigest(String streamID, String password) {
        sha.update(streamID.getBytes());
        return HexString.toString(sha.digest(password.getBytes()));
    }

    public boolean isDigestAuthenticated(String streamID, String password, String digest) {
        return digest.equals(getDigest(streamID, password));
    }

    public boolean isHashAuthenticated(String userHash, String testHash) {
        testHash = HexString.toString(sha.digest(testHash.getBytes()));
        return testHash.equals(userHash);
    }

    public static String randomToken() {
        return Integer.toHexString(random.nextInt());
    }
}
