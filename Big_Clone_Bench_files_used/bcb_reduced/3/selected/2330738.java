package com.intel.bluetooth.obex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author vlads
 *
 */
class MD5DigestWrapper {

    private MessageDigest md5impl;

    MD5DigestWrapper() {
        try {
            md5impl = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    void update(byte[] input) {
        md5impl.update(input);
    }

    byte[] digest() {
        return md5impl.digest();
    }
}
