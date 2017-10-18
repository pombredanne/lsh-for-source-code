package edu.psu.its.lionshare.peerserver.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHAHash {

    private static MessageDigest md = null;

    private static final char[] digitSet = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '2', '3', '4', '5', '6', '7' };

    static {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public static String hash(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            int i;
            byte[] b = new byte[1024];
            while ((i = fis.read(b)) != -1) {
                md.update(b, 0, i);
            }
            fis.close();
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
        byte[] checksum = md.digest();
        md.reset();
        StringBuffer sb = new StringBuffer();
        for (int z = 0; z < 4; z++) {
            sb.append(digitSet[(checksum[z * 5] >>> 3) & 0x0000001F]);
            sb.append(digitSet[((checksum[z * 5] << 2) & 0x0000001C) ^ ((checksum[z * 5 + 1] >>> 6) & 0x00000003)]);
            sb.append(digitSet[(checksum[z * 5 + 1] >>> 1) & 0x0000001F]);
            sb.append(digitSet[((checksum[z * 5 + 1] << 4) & 0x00000010) ^ ((checksum[z * 5 + 2] >>> 4) & 0x0000000F)]);
            sb.append(digitSet[((checksum[z * 5 + 2] << 1) & 0x0000001E) ^ ((checksum[z * 5 + 3] >>> 7) & 0x00000001)]);
            sb.append(digitSet[(checksum[z * 5 + 3] >>> 2) & 0x0000001F]);
            sb.append(digitSet[((checksum[z * 5 + 3] << 3) & 0x00000018) ^ ((checksum[z * 5 + 4] >>> 5) & 0x00000007)]);
            sb.append(digitSet[checksum[z * 5 + 4] & 0x0000001F]);
        }
        String hash = "urn:sha1:" + sb.toString();
        return hash;
    }
}
