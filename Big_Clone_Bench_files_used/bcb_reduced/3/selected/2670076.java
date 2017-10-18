package net.chipped.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Tools {

    public static final String VERSION = "$Id: Tools.java,v 1.1 2006/02/08 05:52:47 gnovos Exp $";

    private static final String _line = System.getProperty("line.separator", "\n");

    public static final String HASH_ALGORITHM = "MD5";

    private Tools() {
    }

    public static String getLineSeparator() {
        return _line;
    }

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * Turns array of bytes into string representing each byte as
	 * unsigned hex number.
	 *
	 * @param hash Array of bytes to convert to hex-string
	 * @return Generated hex string
	 */
    public static String asHex(byte hash[]) {
        char buf[] = new char[hash.length * 2];
        for (int i = 0, x = 0; i < hash.length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

    public static final String MD5hash(byte[] clearText) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance(HASH_ALGORITHM);
        md5.reset();
        md5.update(clearText);
        return asHex(md5.digest());
    }

    public static final String MD5File(File file) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance(HASH_ALGORITHM);
        md5.reset();
        try {
            byte[] buffer = new byte[1024];
            InputStream in = new BufferedInputStream(new FileInputStream(file));
            int c = 0;
            while ((c = in.read(buffer)) != -1) {
                md5.update(buffer, 0, c);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return asHex(md5.digest());
    }
}
