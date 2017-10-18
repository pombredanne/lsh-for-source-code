package net.cryff.security;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

public class Algorithm {

    public static String toMD5(String pw) {
        StringBuffer hexString = new StringBuffer();
        try {
            MessageDigest algorithm = MessageDigest.getInstance("MD5");
            byte[] defaultBytes = pw.getBytes();
            algorithm.reset();
            algorithm.update(defaultBytes);
            byte messageDigest[] = algorithm.digest();
            for (int i = 0; i < messageDigest.length; i++) {
                String hex = Integer.toHexString(0xFF & messageDigest[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hexString + "";
    }

    public static String checkSum(String file) throws IOException {
        FileInputStream fis = null;
        CheckedInputStream cis = null;
        CRC32 crc = null;
        fis = new FileInputStream(file);
        crc = new CRC32();
        cis = new CheckedInputStream(fis, crc);
        byte[] buffer = new byte[100];
        while (cis.read(buffer) >= 0) {
        }
        return "" + cis.getChecksum().getValue();
    }
}
