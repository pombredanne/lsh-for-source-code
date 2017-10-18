package org.openmim.msn;

import java.security.*;

/** Not used by MSN plugin */
public class test_md5 {

    public static void main(String[] a) {
        try {
            byte[] msg1 = "MessageDigest.getInstance(\"GOOGOO\");".getBytes("ASCII");
            byte[] aab = "aab".getBytes("ASCII");
            byte[] cdd = "cdd".getBytes("ASCII");
            byte[] aa = "aa".getBytes("ASCII");
            byte[] bc = "bc".getBytes("ASCII");
            byte[] dd = "dd".getBytes("ASCII");
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(msg1);
            md.update(aab);
            md.update(cdd);
            println("one: ", md.digest());
            md.update(msg1);
            md.update(aa);
            md.update(dd);
            md.update(bc);
            println("two: ", md.digest());
        } catch (Throwable tr) {
            tr.printStackTrace();
        }
    }

    private static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

    static void appendByteAsHexString(StringBuffer sb, byte b) {
        sb.append(HEX_DIGITS[(b >> 4) & (byte) 0xf]).append(HEX_DIGITS[b & (byte) 0xf]);
    }

    static void println(String prefix, byte[] ba) {
        StringBuffer sb = new StringBuffer(ba.length << 1);
        for (int i = 0; i < ba.length; i++) {
            appendByteAsHexString(sb, ba[i]);
        }
        System.err.print(prefix);
        System.err.println(sb.toString());
    }
}
