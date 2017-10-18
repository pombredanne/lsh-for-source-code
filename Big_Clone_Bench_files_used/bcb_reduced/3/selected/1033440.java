package com.sun.jsr082.obex;

import com.sun.midp.crypto.MessageDigest;
import com.sun.midp.crypto.GeneralSecurityException;
import com.sun.midp.crypto.DigestException;
import java.io.IOException;

final class SSLWrapper {

    private MessageDigest md5;

    SSLWrapper() throws IOException {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (GeneralSecurityException e) {
            throw new IOException(e.getMessage());
        }
    }

    void update(byte[] input, int offset, int length) {
        md5.update(input, offset, length);
    }

    void doFinal(byte[] srcData, int srcOff, int srcLen, byte[] dstData, int dstOff) {
        if (srcLen != 0) {
            md5.update(srcData, srcOff, srcLen);
        }
        try {
            md5.digest(dstData, dstOff, dstData.length - dstOff);
        } catch (DigestException e) {
            throw new RuntimeException("output buffer too short");
        }
    }
}
