package de.zefania.api.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public final class Utilities {

    private Utilities() {
    }

    public static class XmlFilter implements FilenameFilter {

        @Override
        public final boolean accept(final File f, final String s) {
            return new File(f, s).isFile() && s.toLowerCase().endsWith(".xml");
        }
    }

    public static String getDigest(final File file, final String algorithm) throws NoSuchAlgorithmException {
        StringBuffer result = new StringBuffer(32);
        Formatter f = new Formatter(result);
        MessageDigest messageDigest = null;
        try {
            byte[] md = new byte[8192];
            FileInputStream in = new FileInputStream(file);
            messageDigest = MessageDigest.getInstance(algorithm);
            for (int n = 0; (n = in.read(md)) > 0; ) {
                messageDigest.update(md, 0, n);
            }
            for (byte b : messageDigest.digest()) {
                f.format("%02x", b);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
