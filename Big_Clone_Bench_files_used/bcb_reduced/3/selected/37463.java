package com.youda.core.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EncryUtils {

    static Logger logger = LoggerFactory.getLogger(EncryUtils.class);

    public static String MD5(String source) {
        logger.info(source);
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(source.getBytes());
            byte[] bytes = digest.digest();
            result = EncodeUtils.hexEncode(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        logger.info(result);
        return result;
    }

    public static String SHA256(String source) {
        logger.info(source);
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(source.getBytes());
            byte[] bytes = digest.digest();
            result = EncodeUtils.hexEncode(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        logger.info(result);
        return result;
    }

    public static String SHA(String source) {
        logger.info(source);
        String result = null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA");
            digest.update(source.getBytes());
            byte[] bytes = digest.digest();
            result = EncodeUtils.hexEncode(bytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        logger.info(result);
        return result;
    }
}
