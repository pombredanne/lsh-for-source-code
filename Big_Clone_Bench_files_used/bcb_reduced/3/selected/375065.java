package com.vayoodoot.research;

import java.security.MessageDigest;
import java.io.InputStream;
import java.io.FileInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: Sachin Shetty
 * Date: Aug 12, 2007
 * Time: 9:26:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class ComputeMD5 {

    public static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);
        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;
        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);
        fis.close();
        return complete.digest();
    }

    public static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";
        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void main(String args[]) throws Exception {
        System.out.println(getMD5Checksum("C:\\SHARE1\\test_small.zip"));
        System.out.println(getMD5Checksum("C:\\SHARE1\\test.zip"));
    }
}
