package com.zhyi.checksumz.common;

import com.zhyi.checksumz.util.ChecksumzToolkit;
import java.security.MessageDigest;

/**
 * Wraps JDK's {@code java.security.MessageDigest}.
 */
public class MessageDigestChecksum implements Checksumz {

    private MessageDigest md;

    public MessageDigestChecksum(MessageDigest md) {
        this.md = md;
    }

    @Override
    public void update(byte[] data, int offset, int length) {
        md.update(data, offset, length);
    }

    @Override
    public void reset() {
        md.reset();
    }

    @Override
    public String getHexResult(boolean upperCase) {
        byte[] result = md.digest();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < result.length / 4; i++) {
            int tmp = result[4 * i] << 24;
            tmp |= (result[4 * i + 1] << 16) & 0x00FF0000;
            tmp |= (result[4 * i + 2] << 8) & 0x0000FF00;
            tmp |= result[4 * i + 3] & 0x000000FF;
            sb.append(ChecksumzToolkit.prependZero(Integer.toHexString(tmp), 8));
            sb.append(" ");
        }
        String hexResult = sb.substring(0, sb.length() - 1);
        return upperCase ? hexResult.toUpperCase() : hexResult;
    }
}
