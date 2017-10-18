package com.beatsportable.tsukikage.core.tools;

import java.io.FileInputStream;
import java.security.MessageDigest;

/**
 * This is a collection of miscellaneous useful methods
 */
public abstract class Tools {

    public static final int BUFFER = 1024;

    public static final int BUFFER_LARGE = 1048576;

    /**
	 * Returns basename of a string
	 */
    public static String getBasename(String filename) {
        String ret = filename;
        int index = -1;
        if ((index = ret.lastIndexOf('/')) > -1 && ++index < ret.length()) {
            ret = ret.substring(index);
        }
        if ((index = ret.lastIndexOf('.')) > -1) {
            ret = ret.substring(0, index);
        }
        return ret;
    }

    /**
	 * Get byte checksum, used for getMD5Checksum
	 * Copied from http://www.rgagnon.com/javadetails/java-0416.html
	 */
    private static byte[] createChecksum(String filename) throws Exception {
        FileInputStream in = new FileInputStream(filename);
        byte[] buffer = new byte[BUFFER];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = in.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        in.close();
        return complete.digest();
    }

    /**
	 * Get MD5 string for a file
	 */
    public static String getMD5Checksum(String filename) {
        try {
            byte[] b = createChecksum(filename);
            String result = "";
            for (int i = 0; i < b.length; i++) {
                result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
