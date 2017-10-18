package com.google.gwt.i18n.server.keygen;

import com.google.gwt.i18n.server.KeyGenerator;
import com.google.gwt.i18n.server.Message;
import com.google.gwt.util.tools.shared.StringUtils;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Key generator using the MD5 hash of the text and meaning.
 */
public class MD5KeyGenerator implements KeyGenerator {

    public String generateKey(Message msg) {
        String text = msg.getDefaultMessage();
        String meaning = msg.getMeaning();
        if (text == null) {
            return null;
        }
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing MD5", e);
        }
        try {
            md5.update(text.getBytes("UTF-8"));
            if (meaning != null) {
                md5.update(meaning.getBytes("UTF-8"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 unsupported", e);
        }
        return StringUtils.toHexString(md5.digest());
    }
}
