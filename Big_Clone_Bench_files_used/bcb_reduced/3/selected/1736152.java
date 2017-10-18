package net.sf.jvifm.util;

import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public class Digest {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public static String digest(String filename, String algorithm) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try {
            DigestInputStream dis = new DigestInputStream(fis, md);
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) ;
        } finally {
            fis.close();
        }
        String result = byteArrayToHexString(md.digest());
        return result;
    }
}
