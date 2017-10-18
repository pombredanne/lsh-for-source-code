package ru.nnov.kino.web.model.torrent;

import org.apache.log4j.Logger;
import java.security.MessageDigest;

/**
 * Used to create hash
 *
 * @author chernser
 */
public class SHAWrapper {

    private static final Logger logger = Logger.getLogger(SHAWrapper.class);

    private static SHAWrapper instance;

    static {
        try {
            instance = new SHAWrapper();
        } catch (Exception ex) {
            logger.fatal("SHA-1 not found", ex);
        }
    }

    public static SHAWrapper getInstance() {
        return instance;
    }

    private final MessageDigest messageDigest;

    private SHAWrapper() throws Exception {
        messageDigest = MessageDigest.getInstance("SHA-1");
    }

    /**
     * encodes string to sha-1 string
     *
     * @param str to encode
     * @return sha-1 string
     */
    public byte[] encode(String str) {
        byte[] result;
        synchronized (messageDigest) {
            result = messageDigest.digest(str.getBytes());
            messageDigest.notifyAll();
        }
        return result;
    }
}
