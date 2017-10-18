package com.pawlowsky.zhu.mediafiler.commands;

import java.io.IOException;
import gnu.crypto.hash.HashFactory;
import gnu.crypto.hash.IMessageDigest;
import org.apache.commons.codec.binary.Base64;

/**
*/
public class ChecksumGenerator {

    /**
     * 
     */
    public ChecksumGenerator() {
    }

    /**
     * Create a checksum for a data stream.
     * @param srcData Stream of data to read.
     * @return Base64 representation of checksum of the contents fo the stream.
     */
    public String checksum(java.io.InputStream srcData) throws IOException {
        IMessageDigest messageDigest = HashFactory.getInstance("sha-256");
        int bytesRead = 1;
        byte[] buffer = new byte[4096];
        while (bytesRead >= 0) {
            bytesRead = srcData.read(buffer);
            messageDigest.update(buffer, 0, bytesRead);
        }
        byte[] digest = messageDigest.digest();
        byte[] encodedDigest = Base64.encodeBase64(digest);
        String result = new String(encodedDigest);
        int len = result.length();
        if (len != 44) {
            throw new IOException("checksum length=" + len);
        }
        return result;
    }
}
