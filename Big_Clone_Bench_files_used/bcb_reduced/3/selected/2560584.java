package net.sf.buildbox.installer;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Checksum {

    public static String toString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public static byte[] computeChecksum(File filename, String alg) throws IOException, NoSuchAlgorithmException {
        final InputStream fis = new FileInputStream(filename);
        try {
            final byte[] buffer = new byte[1024];
            final MessageDigest complete = MessageDigest.getInstance(alg);
            int numRead = fis.read(buffer);
            while (numRead != -1) {
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
                numRead = fis.read(buffer);
            }
            return complete.digest();
        } finally {
            fis.close();
        }
    }

    public static String sha1(File file) throws IOException, NoSuchAlgorithmException {
        return toString(computeChecksum(file, "SHA1"));
    }

    public static String md5(File file) throws IOException, NoSuchAlgorithmException {
        return toString(computeChecksum(file, "MD5"));
    }

    public static void main(String[] args) throws Exception {
        final String fn = "/home/pk/.m2/repository/net/sf/buildbox/strictlogging/strictlogging-api/1.0.1-beta-1/strictlogging-api-1.0.1-beta-1.jar";
        System.out.println(sha1(new File(fn)));
    }
}
