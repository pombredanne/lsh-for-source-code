package net.sourceforge.jdirdiff.util;

import java.io.File;
import java.io.FileInputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 *  Calculates MD5 from a file.
 *
 * @author    Al Vega
 */
public class MD5 {

    static final int BUFLEN = 8192;

    /**
     *  Compares two files' MD5 checksum.
     *
     * @param  aa  first file.
     * @param  bb  second file.
     * @return  true if they are equal.
     * @exception  Exception
     */
    public boolean compareMD5(File aa, File bb) throws Exception {
        return equalsByteArray(getMD5(aa), getMD5(bb));
    }

    /**
     *  The main program for the MD5 class
     *
     * @param  args  The command line arguments
     */
    public static void main(String[] args) {
        try {
            MD5 md5 = new MD5();
            System.out.println(md5.compareMD5(new File(args[0]), new File(args[1])));
        } catch (Exception e) {
            System.err.println(e);
        }
    }

    private byte[] getMD5(File file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        DigestInputStream in = new DigestInputStream(new FileInputStream(file), md);
        byte[] buffer = new byte[BUFLEN];
        while (in.read(buffer) != -1) ;
        byte[] raw = md.digest();
        return raw;
    }

    private boolean equalsByteArray(byte[] aa, byte[] bb) {
        if (aa.length != bb.length) return false;
        for (int i = 0; i < aa.length; i++) if (aa[i] != bb[i]) return false;
        return true;
    }
}
