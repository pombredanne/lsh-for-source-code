package org.macchiato.db;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Helper class creating MD5 sum from an inputstream.
 *
 * @author fdietz
 */
public final class MD5SumHelper {

    public static byte[] createMD5(final InputStream data) throws NoSuchAlgorithmException, IOException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        final byte buf[] = new byte[8192];
        int bytes = 0;
        while ((bytes = data.read(buf)) != -1) {
            md5.update(buf, 0, bytes);
        }
        return md5.digest();
    }
}
