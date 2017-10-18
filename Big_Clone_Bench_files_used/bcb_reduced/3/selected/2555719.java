package com.milci.ebusinesssuite;

import java.io.*;
import java.security.*;
import java.util.*;

public class eBusinessSuiteSecurity {

    public eBusinessSuiteSecurity() {
    }

    static String control(String s1, int i1, String s2) {
        return newControl(s1, s2, i1, 0);
    }

    private static int[] a(byte abyte0[], int i1) {
        int ai[] = new int[16];
        int ai1[] = new int[5];
        u(ai1, null);
        int l1 = ai.length;
        int k1 = 0;
        int j2 = 0;
        int j1;
        for (j1 = 0; j1 < (i1 & -4); j1 += 4) {
            ai[k1] = abyte0[j2] | abyte0[j2 + 1] << 8 | abyte0[j2 + 2] << 16 | abyte0[j2 + 3] << 24;
            if (++k1 == l1) {
                u(ai1, ai);
                k1 = 0;
            }
            j2 += 4;
        }
        j1 = i1 - j1;
        if (j1 == 1) {
            ai[k1] = abyte0[j2] & 0xff | 0x8000;
        } else if (j1 == 2) {
            ai[k1] = abyte0[j2] | abyte0[j2 + 1] << 8 | 0x800000;
        } else if (j1 == 3) {
            ai[k1] = abyte0[j2] | abyte0[j2 + 1] << 8 | abyte0[j2 + 2] << 16 | 0x80000000;
        } else {
            ai[k1] = 128;
        }
        if (++k1 >= l1 - 2) {
            while (k1 < l1) {
                ai[k1++] = 0;
            }
            u(ai1, ai);
            k1 = 0;
        }
        while (k1 < l1 - 2) {
            ai[k1++] = 0;
        }
        int i2 = i1;
        ai[k1++] = i2 >> 29 & 7;
        ai[k1] = i2 << 3;
        u(ai1, ai);
        return ai1;
    }

    static String newCheck(String s1, String s2, boolean flag) {
        if (s1 == null || s2 == null || s2.length() >= "ZG_ENCRYPT_FAILED_".length() && s2.substring(0, "ZG_ENCRYPT_FAILED_".length()).equals("ZG_ENCRYPT_FAILED_")) {
            return null;
        }
        byte abyte0[];
        try {
            abyte0 = s1.getBytes("UTF8");
        } catch (UnsupportedEncodingException _ex) {
            return null;
        }
        int l2 = abyte0.length;
        int i3 = s2.length();
        int i1 = 1;
        byte byte0 = 2;
        int j1 = i3 - 2 - i1 * 2;
        if (j1 <= 0) {
            return null;
        }
        int k1 = (j1 / 16) * 8;
        if (k1 <= 0) {
            return null;
        }
        int l1 = (j1 % 16) / 2;
        int i2 = l1 + i1;
        int j2 = k1 - 1 - byte0;
        if (j2 <= 0) {
            return null;
        }
        if (!s2.substring(0, 2).equals("ZG")) {
            return null;
        }
        String s3 = s2.substring(2);
        byte abyte1[] = p(s3);
        byte abyte2[] = new byte[abyte1.length - i2];
        byte abyte3[] = new byte[i2];
        System.arraycopy(abyte1, 0, abyte2, 0, abyte1.length - i2);
        System.arraycopy(abyte1, abyte1.length - i2, abyte3, 0, i2);
        byte abyte4[] = new byte[i2 + l2];
        System.arraycopy(abyte3, 0, abyte4, 0, i2);
        System.arraycopy(abyte0, 0, abyte4, i2, l2);
        byte abyte5[] = v(null, abyte4, abyte2);
        if (abyte5 == null) {
            return null;
        }
        int j3 = abyte5.length;
        for (int k2 = byte0; k2 < abyte5.length; k2++) {
            if (abyte5[k2] != 0) {
                continue;
            }
            j3 = k2;
            break;
        }
        byte abyte6[] = new byte[j3 - byte0];
        System.arraycopy(abyte5, byte0, abyte6, 0, j3 - byte0);
        String s4;
        try {
            s4 = new String(abyte6, "UTF8");
        } catch (UnsupportedEncodingException _ex) {
            return null;
        }
        if (s4 != null && flag) {
            return w(s4, 0, flag);
        } else {
            return s4;
        }
    }

    private static void c(int ai[]) {
        ai[4] = j(ai[0], 5) + s(ai[1], ai[2], ai[3]) + ai[4] + ai[5] + 0x8f1bbcdc;
        ai[2] = j(ai[1], 30);
    }

    private static void d(byte abyte0[], int ai[]) {
        int ai1[] = new int[2];
        t(abyte0, ai1);
        F(ai1, ai);
        x(ai1, abyte0);
    }

    private static byte[] e(int ai[]) {
        byte abyte0[] = new byte[4];
        byte abyte1[] = null;
        if (ai != null) {
            abyte1 = new byte[ai.length];
            for (int i1 = 0; i1 < ai.length; i1++) {
                abyte0[3] = (byte) (ai[i1] & 0xff);
                abyte0[2] = (byte) ((ai[i1] & 0xff00) >> 8);
                abyte0[1] = (byte) ((ai[i1] & 0xff0000) >> 16);
                abyte0[0] = (byte) ((ai[i1] & 0xff000000) >> 24);
                abyte1[i1] = (byte) (abyte0[0] ^ abyte0[1] ^ abyte0[2] ^ abyte0[3]);
            }
        }
        return abyte1;
    }

    private static byte f(int i1) {
        byte byte0 = 0;
        int j1 = (char) i1 & 0x40;
        if (j1 >= 1) {
            byte0 = (byte) ((char) i1 - 55);
        } else {
            byte0 = (byte) ((char) i1 & 0xf);
        }
        return byte0;
    }

    private static byte[] g(byte abyte0[], byte abyte1[]) {
        if (abyte0.length < 32) {
            return null;
        }
        if (abyte1.length % 8 != 0) {
            return null;
        }
        int i1 = abyte1.length / 8;
        byte abyte3[] = new byte[8];
        byte abyte4[] = new byte[8];
        byte abyte5[] = new byte[8];
        byte abyte2[] = new byte[8];
        System.arraycopy(abyte0, 0, abyte3, 0, 8);
        System.arraycopy(abyte0, 8, abyte4, 0, 8);
        System.arraycopy(abyte0, 16, abyte5, 0, 8);
        System.arraycopy(abyte0, 24, abyte2, 0, 8);
        int ai[] = l(abyte3, false);
        int ai1[] = l(abyte4, true);
        int ai2[] = l(abyte5, false);
        byte abyte6[] = new byte[abyte1.length];
        int j1 = 0;
        for (int k1 = 0; j1 < i1; k1 += 8) {
            q(abyte1, k1, abyte6, k1, ai, ai1, ai2, abyte2, false);
            j1++;
        }
        byte byte0 = abyte6[abyte1.length - 1];
        if (byte0 < 1 || byte0 > 8) {
            return null;
        }
        for (int l1 = abyte1.length - byte0; l1 < abyte1.length; l1++) {
            if (abyte6[l1] != byte0) {
                return null;
            }
        }
        byte abyte7[] = new byte[abyte1.length - byte0];
        System.arraycopy(abyte6, 0, abyte7, 0, abyte1.length - byte0);
        return abyte7;
    }

    private static void h(int ai[]) {
        ai[4] = j(ai[0], 5) + o(ai[1], ai[2], ai[3]) + ai[4] + ai[5] + 0xca62c1d6;
        ai[2] = j(ai[1], 30);
    }

    private static byte[] i(byte abyte0[], byte abyte1[], byte abyte2[]) {
        return G(y(abyte0, abyte1), abyte2);
    }

    private static int j(int i1, int j1) {
        return i1 << j1 | i1 >>> 32 - j1;
    }

    private static byte[] k(byte abyte0[], byte abyte1[], int i1) {
        if (abyte0 == null || abyte1 == null) {
            return null;
        } else {
            int ai[] = a(abyte0, abyte0.length);
            byte abyte2[] = e(ai);
            byte abyte3[] = new byte[258];
            B(abyte3, abyte2, null, 5);
            byte abyte4[] = new byte[i1];
            B(abyte3, abyte1, abyte4, i1);
            return abyte4;
        }
    }

    private static int[] l(byte abyte0[], boolean flag) {
        byte abyte1[] = new byte[56];
        byte abyte2[] = new byte[56];
        int ai[] = new int[32];
        for (int j1 = 0; j1 < 56; j1++) {
            byte byte0 = V[j1];
            int l2 = byte0 & 7;
            abyte1[j1] = (byte) ((abyte0[byte0 >>> 3] & bi[l2]) == 0 ? 0 : 1);
        }
        for (int i1 = 0; i1 < 16; i1++) {
            int i3;
            if (flag) {
                i3 = i1 << 1;
            } else {
                i3 = 15 - i1 << 1;
            }
            int j3 = i3 + 1;
            ai[i3] = ai[j3] = 0;
            for (int k1 = 0; k1 < 28; k1++) {
                int j2 = k1 + bb[i1];
                if (j2 < 28) {
                    abyte2[k1] = abyte1[j2];
                } else {
                    abyte2[k1] = abyte1[j2 - 28];
                }
            }
            for (int l1 = 28; l1 < 56; l1++) {
                int k2 = l1 + bb[i1];
                if (k2 < 56) {
                    abyte2[l1] = abyte1[k2];
                } else {
                    abyte2[l1] = abyte1[k2 - 28];
                }
            }
            for (int i2 = 0; i2 < 24; i2++) {
                if (abyte2[S[i2]] != 0) {
                    ai[i3] |= W[i2];
                }
                if (abyte2[S[i2 + 24]] != 0) {
                    ai[j3] |= W[i2];
                }
            }
        }
        return C(ai);
    }

    static String oldControl(String s1, String s2, int i1) {
        if (s1 == null || s2 == null) {
            return null;
        }
        byte abyte0[] = A(s1.toCharArray());
        int j1 = s2.length();
        if (j1 > i1 - 1) {
            j1 = i1 - 1;
        }
        Random random = new Random();
        int k1 = i1 - j1 - 1;
        int ai[] = new int[k1];
        for (int l1 = 0; l1 < k1; l1++) {
            ai[l1] = random.nextInt();
        }
        byte abyte1[] = e(ai);
        byte abyte2[] = A((new String(s2 + '\0')).toCharArray());
        byte abyte3[] = new byte[k1 + abyte2.length];
        System.arraycopy(abyte2, 0, abyte3, 0, abyte2.length);
        System.arraycopy(abyte1, 0, abyte3, abyte2.length, k1);
        byte abyte4[] = k(abyte0, abyte3, i1);
        return z(abyte4);
    }

    private static char[] m(byte abyte0[]) {
        char ac1[];
        label0: {
            if (abyte0 == null) {
                return null;
            }
            char ac[] = new char[abyte0.length];
            int i1 = 0;
            boolean flag = false;
            boolean flag1 = false;
            ac1 = null;
            try {
                for (int j1 = 0; j1 < abyte0.length; j1++) {
                    byte byte0 = abyte0[j1];
                    if ((byte0 & 0x80) == 0) {
                        ac[i1++] = (char) byte0;
                        if ((char) byte0 == 0) {
                            break;
                        }
                        continue;
                    }
                    if ((byte0 & 0xe0) == 192) {
                        char c1 = (char) ((byte0 & 0x1f) << 6 & 0x7c0);
                        byte0 = abyte0[++j1];
                        if ((byte0 & 0x80) == 128) {
                            c1 |= byte0 & 0x3f;
                            ac[i1++] = c1;
                            continue;
                        }
                        break label0;
                    }
                    if ((byte0 & 0xf0) != 224) {
                        continue;
                    }
                    char c2 = (char) ((byte0 & 0xf) << 12 & 0xf000);
                    byte0 = abyte0[++j1];
                    if ((byte0 & 0x80) != 128) {
                        break label0;
                    }
                    c2 |= (byte0 & 0x3f) << 6 & 0xfc0;
                    byte0 = abyte0[++j1];
                    if ((byte0 & 0x80) != 128) {
                        break label0;
                    }
                    c2 |= byte0 & 0x3f;
                    ac[i1++] = c2;
                }
                ac1 = new char[i1];
                System.arraycopy(ac, 0, ac1, 0, i1);
            } catch (ArrayIndexOutOfBoundsException _ex) {
                ac1 = null;
            }
        }
        return ac1;
    }

    private static int n(int i1, int j1, int k1) {
        return i1 ^ j1 ^ k1;
    }

    private static int o(int i1, int j1, int k1) {
        return i1 ^ j1 ^ k1;
    }

    static String control(String s1, String s2, int i1) {
        return newControl(s1, s2, 0, i1);
    }

    private static byte[] p(String s1) {
        boolean flag = false;
        boolean flag1 = false;
        int i1 = 0;
        int j1 = 0;
        byte abyte0[] = null;
        if (s1 == null) {
            return null;
        }
        int k1 = s1.length() / 2;
        if (k1 > 0) {
            abyte0 = new byte[k1];
            for (; k1 > 0; k1--) {
                char c1 = s1.charAt(i1++);
                char c2 = s1.charAt(i1++);
                abyte0[j1++] = (byte) (f(c1) << 4 | f(c2));
            }
        }
        return abyte0;
    }

    private static void q(byte abyte0[], int i1, byte abyte1[], int j1, int ai[], int ai1[], int ai2[], byte abyte2[], boolean flag) {
        byte abyte3[] = new byte[8];
        System.arraycopy(abyte0, i1, abyte3, 0, 8);
        if (!flag) {
            d(abyte3, ai2);
            d(abyte3, ai1);
            d(abyte3, ai);
            D(abyte3, abyte2, abyte1, j1);
            System.arraycopy(abyte0, i1, abyte2, 0, 8);
            return;
        } else {
            D(abyte3, abyte2, abyte3, 0);
            d(abyte3, ai);
            d(abyte3, ai1);
            d(abyte3, ai2);
            System.arraycopy(abyte3, 0, abyte2, 0, 8);
            System.arraycopy(abyte3, 0, abyte1, j1, 8);
            return;
        }
    }

    private static int r(int i1, int j1, int k1) {
        return i1 & j1 | ~i1 & k1;
    }

    private static int s(int i1, int j1, int k1) {
        return i1 & j1 | i1 & k1 | j1 & k1;
    }

    static String hash(String s1) {
        if (s1 == null) {
            return null;
        }
        byte abyte0[];
        try {
            abyte0 = s1.getBytes("UTF8");
        } catch (UnsupportedEncodingException _ex) {
            return null;
        }
        byte abyte1[] = y(null, abyte0);
        return z(abyte1);
    }

    static String newControl(String s1, String s2, int i1, int j1) {
        if (s1 == null || s2 == null) {
            return "ZG_ENCRYPT_FAILED_BADINPUT";
        }
        int k1;
        if (i1 > 0) {
            k1 = i1;
        } else if (j1 == 32) {
            k1 = 100;
        } else if (j1 < 32 && j1 == s2.length()) {
            k1 = 100;
        } else {
            byte abyte1[];
            try {
                abyte1 = s2.getBytes("UTF8");
            } catch (UnsupportedEncodingException _ex) {
                return "ZG_ENCRYPT_FAILED_MISC";
            }
            k1 = ((abyte1.length + 10) / 8) * 16 + 4 + 14;
        }
        int l1 = 1;
        byte byte0 = 2;
        int i2 = k1 - 2 - l1 * 2;
        if (i2 <= 0) {
            return "ZG_ENCRYPT_FAILED_SMALLBUF";
        }
        int j2 = (i2 / 16) * 8;
        if (j2 <= 0) {
            return "ZG_ENCRYPT_FAILED_SMALLBUF";
        }
        int k2 = (i2 % 16) / 2;
        int l2 = k2 + l1;
        int i3 = j2 - 1 - byte0;
        if (i3 <= 0) {
            return "ZG_ENCRYPT_FAILED_SMALLBUF";
        }
        byte abyte0[];
        byte abyte2[];
        try {
            abyte0 = s1.getBytes("UTF8");
            abyte2 = s2.getBytes("UTF8");
        } catch (UnsupportedEncodingException _ex) {
            return "ZG_ENCRYPT_FAILED_MISC";
        }
        int j3 = abyte0.length;
        int k3 = abyte2.length;
        if (k3 > i3) {
            return "ZG_ENCRYPT_FAILED_CHARSET_CLIP";
        }
        Random random = new Random();
        int ai[] = new int[l2];
        for (int l3 = 0; l3 < l2; l3++) {
            ai[l3] = random.nextInt();
        }
        byte abyte3[] = e(ai);
        ai = null;
        byte byte1 = byte0;
        ai = new int[byte1];
        for (int i4 = 0; i4 < byte1; i4++) {
            ai[i4] = random.nextInt();
        }
        byte abyte4[] = e(ai);
        ai = null;
        int j4 = i3 - k3;
        ai = new int[j4];
        for (int k4 = 0; k4 < j4; k4++) {
            if (k4 == 0) {
                ai[k4] = 0;
            } else {
                ai[k4] = random.nextInt();
            }
        }
        byte abyte5[] = e(ai);
        ai = null;
        byte abyte6[] = new byte[byte1 + j4 + k3];
        System.arraycopy(abyte4, 0, abyte6, 0, byte1);
        System.arraycopy(abyte2, 0, abyte6, byte1, k3);
        System.arraycopy(abyte5, 0, abyte6, byte1 + k3, j4);
        byte abyte7[] = new byte[l2 + j3];
        System.arraycopy(abyte3, 0, abyte7, 0, l2);
        System.arraycopy(abyte0, 0, abyte7, l2, j3);
        byte abyte8[] = i(null, abyte7, abyte6);
        if (abyte8 == null) {
            return "ZG_ENCRYPT_FAILED_MISC";
        } else {
            byte abyte9[] = new byte[abyte8.length + l2];
            System.arraycopy(abyte8, 0, abyte9, 0, abyte8.length);
            System.arraycopy(abyte3, 0, abyte9, abyte8.length, l2);
            String s3 = z(abyte9);
            return "ZG" + s3;
        }
    }

    private static void t(byte abyte0[], int ai[]) {
        int i1 = 0;
        ai[0] = (abyte0[i1] & 0xff) << 24;
        i1++;
        ai[0] |= (abyte0[i1] & 0xff) << 16;
        i1++;
        ai[0] |= (abyte0[i1] & 0xff) << 8;
        i1++;
        ai[0] |= abyte0[i1] & 0xff;
        i1++;
        ai[1] = (abyte0[i1] & 0xff) << 24;
        i1++;
        ai[1] |= (abyte0[i1] & 0xff) << 16;
        i1++;
        ai[1] |= (abyte0[i1] & 0xff) << 8;
        i1++;
        ai[1] |= abyte0[i1] & 0xff;
    }

    private static int u(int ai[], int ai1[]) {
        boolean flag = false;
        boolean flag1 = false;
        int ai2[] = new int[80];
        int l1 = 0;
        int ai3[] = new int[5];
        int ai4[] = new int[6];
        int ai5[] = new int[5];
        ai3[0] = 0x67452301;
        ai3[1] = 0xefcdab89;
        ai3[2] = 0x98badcfe;
        ai3[3] = 0x10325476;
        ai3[4] = 0xc3d2e1f0;
        if (ai1 != null) {
            if (ai != null) {
                System.arraycopy(ai, 0, ai4, 0, 5);
            }
            byte byte0 = 80;
            int i1;
            for (i1 = 0; i1 < 16; i1++) {
                ai2[i1] = ai1[i1];
            }
            for (; i1 < byte0; i1++) {
                ai2[i1] = ai2[i1 - 3] ^ ai2[i1 - 8] ^ ai2[i1 - 14] ^ ai2[i1 - 16];
            }
            for (int j1 = 0; j1 < 80; j1++) {
                if (j1 != 0) {
                    System.arraycopy(ai4, 0, ai5, 0, 5);
                    ai4[0] = ai5[4];
                    ai4[1] = ai5[0];
                    ai4[2] = ai5[1];
                    ai4[3] = ai5[2];
                    ai4[4] = ai5[3];
                }
                ai4[5] = ai2[j1];
                if (j1 < 20) {
                    H(ai4);
                } else if (j1 < 40) {
                    I(ai4);
                } else if (j1 < 60) {
                    c(ai4);
                } else if (j1 < 80) {
                    h(ai4);
                }
            }
            ai3[0] = ai4[4];
            ai3[1] = ai4[0];
            ai3[2] = ai4[1];
            ai3[3] = ai4[2];
            ai3[4] = ai4[3];
            for (int k1 = 0; k1 < byte0; k1++) {
                ai2[k1] = 0;
            }
        }
        if (ai != null) {
            ai[0] = ai3[0];
            ai[1] = ai3[1];
            ai[2] = ai3[2];
            ai[3] = ai3[3];
            ai[4] = ai3[4];
        }
        l1 = ai3[0] ^ ai3[1] ^ ai3[2] ^ ai3[3] ^ ai3[4];
        return l1;
    }

    private static byte[] v(byte abyte0[], byte abyte1[], byte abyte2[]) {
        return g(y(abyte0, abyte1), abyte2);
    }

    private static String w(String s1, int i1, boolean flag) {
        if (s1 == null) {
            return "";
        }
        int j1 = s1.length();
        int k1 = s1.indexOf('\0');
        if (k1 > -1) {
            j1 = k1;
        }
        int l1;
        if (j1 > i1 && i1 > 0) {
            l1 = i1;
        } else {
            l1 = j1;
        }
        if (flag) {
            return new String(s1.substring(0, l1).toUpperCase(Locale.US));
        } else {
            return new String(s1.substring(0, l1));
        }
    }

    private static void x(int ai[], byte abyte0[]) {
        int i1 = 0;
        abyte0[i1] = (byte) (ai[0] >> 24 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[0] >> 16 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[0] >> 8 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[0] & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[1] >> 24 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[1] >> 16 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[1] >> 8 & 0xff);
        i1++;
        abyte0[i1] = (byte) (ai[1] & 0xff);
    }

    static String oldCheck(String s1, String s2, boolean flag) {
        byte abyte0[] = A(s1.toCharArray());
        byte abyte1[] = p(s2);
        byte abyte2[] = k(abyte0, abyte1, abyte1.length);
        char ac[] = m(abyte2);
        if (ac != null) {
            String s3 = new String(ac);
            return w(s3, 0, flag);
        } else {
            return null;
        }
    }

    private static byte[] y(byte abyte0[], byte abyte1[]) {
        byte abyte2[] = new byte[32];
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException _ex) {
            return null;
        }
        messagedigest.reset();
        byte byte0 = 20;
        if (abyte0 != null && abyte0.length > 0) {
            messagedigest.update(abyte0);
        }
        if (abyte1 != null && abyte1.length > 0) {
            messagedigest.update(abyte1);
        }
        messagedigest.update((byte) 1);
        byte abyte3[] = messagedigest.digest();
        System.arraycopy(abyte3, 0, abyte2, 0, byte0);
        messagedigest.reset();
        if (abyte0 != null && abyte0.length > 0) {
            messagedigest.update(abyte0);
        }
        if (abyte1 != null && abyte1.length > 0) {
            messagedigest.update(abyte1);
        }
        messagedigest.update((byte) 2);
        abyte3 = messagedigest.digest();
        System.arraycopy(abyte3, 0, abyte2, byte0, 32 - byte0);
        return abyte2;
    }

    private static String z(byte abyte0[]) {
        if (abyte0 == null) {
            return null;
        }
        String s1 = "0123456789ABCDEF";
        StringBuffer stringbuffer = new StringBuffer(abyte0.length * 2);
        int i1 = 0;
        for (int j1 = abyte0.length; j1 > 0; ) {
            byte byte0 = abyte0[i1];
            stringbuffer.append(s1.charAt(byte0 >> 4 & 0xf));
            stringbuffer.append(s1.charAt(byte0 & 0xf));
            j1--;
            i1++;
        }
        return stringbuffer.toString();
    }

    private static byte[] A(char ac[]) {
        byte abyte0[] = new byte[ac.length * 3];
        int i1 = 0;
        boolean flag = false;
        for (int j1 = 0; j1 < ac.length; j1++) {
            char c1 = ac[j1];
            if (c1 >= 0 && c1 <= '\177') {
                abyte0[i1++] = (byte) (c1 & 0x7f);
            } else if (c1 >= '\200' && c1 <= '߿') {
                abyte0[i1++] = (byte) ((c1 & 0x7c0) >> 6 | 0xc0);
                abyte0[i1++] = (byte) (c1 & 0x3f | 0x80);
            } else if (c1 >= 'ࠀ' && c1 <= '￿') {
                abyte0[i1++] = (byte) ((c1 & 0xf000) >> 12 | 0xe0);
                abyte0[i1++] = (byte) ((c1 & 0xfc0) >> 6 | 0x80);
                abyte0[i1++] = (byte) (c1 & 0x3f | 0x80);
            }
        }
        byte abyte1[] = new byte[i1];
        System.arraycopy(abyte0, 0, abyte1, 0, i1);
        return abyte1;
    }

    private static void B(byte abyte0[], byte abyte1[], byte abyte2[], int i1) {
        if (abyte0 == null) {
            if (i1 > 0 && abyte1 != null && abyte2 != null) {
                for (int j1 = 0; j1 < i1; j1++) {
                    abyte2[j1] = abyte1[j1];
                }
                return;
            }
        } else {
            if (abyte2 == null) {
                for (int k1 = 0; k1 < 256; k1++) {
                    abyte0[k1] = (byte) k1;
                }
                if (abyte1 != null && i1 > 0) {
                    int j2 = 0;
                    int l2 = 0;
                    for (int l1 = 0; l1 < 256; l1++) {
                        int j3 = (new Byte(abyte0[l1])).intValue() & 0xff;
                        j2 = j2 + j3 + ((new Byte(abyte1[l2])).intValue() & 0xff) & 0xff;
                        abyte0[l1] = abyte0[j2];
                        abyte0[j2] = (new Integer(j3 & 0xff)).byteValue();
                        l2 = (l2 + 1) % i1;
                    }
                }
                abyte0[256] = 0;
                abyte0[257] = 0;
                return;
            }
            int i2 = (new Byte(abyte0[256])).intValue() & 0xff;
            int k2 = (new Byte(abyte0[257])).intValue() & 0xff;
            for (int i3 = 0; i3 < i1; i3++) {
                i2 = i2 + 1 & 0xff;
                int k3 = (new Byte(abyte0[i2])).intValue() & 0xff;
                k2 = k2 + k3 & 0xff;
                abyte0[i2] = abyte0[k2];
                abyte0[k2] = (new Integer(k3 & 0xff)).byteValue();
                k3 += (new Byte(abyte0[i2])).intValue() & 0xff;
                abyte2[i3] = (byte) (abyte1[i3] ^ abyte0[k3 & 0xff]);
                new String("");
            }
            abyte0[256] = (byte) (i2 & 0xff);
            abyte0[257] = (byte) (k2 & 0xff);
        }
    }

    static boolean check(String s1, String s2, boolean flag) {
        String cid;
        if (s2 != null && s2.length() > 0) {
            if (s2.substring(0, 2).equals("ZG")) {
                cid = newCheck(s1, s2, flag);
            } else {
                cid = oldCheck(s1, s2, flag);
            }
        } else {
            return false;
        }
        if (cid == null) {
            return false;
        } else {
            return true;
        }
    }

    private static int[] C(int ai[]) {
        int ai1[] = new int[32];
        int ai2[] = ai;
        int i1 = 0;
        boolean flag = false;
        int k1 = 0;
        int l1 = 0;
        while (i1 < 16) {
            int j1 = k1++;
            ai1[l1] = (ai2[j1] & 0xfc0000) << 6;
            ai1[l1] |= (ai2[j1] & 0xfc0) << 10;
            ai1[l1] |= (ai[k1] & 0xfc0000) >> 10;
            ai1[l1] |= (ai[k1] & 0xfc0) >> 6;
            l1++;
            ai1[l1] = (ai2[j1] & 0x3f000) << 12;
            ai1[l1] |= (ai2[j1] & 0x3f) << 16;
            ai1[l1] |= (ai[k1] & 0x3f000) >> 4;
            ai1[l1] |= ai[k1] & 0x3f;
            l1++;
            i1++;
            k1++;
        }
        return ai1;
    }

    private static void D(byte abyte0[], byte abyte1[], byte abyte2[], int i1) {
        for (int j1 = 0; j1 < 8; j1++) {
            abyte2[j1 + i1] = (byte) (abyte0[j1] ^ abyte1[j1]);
        }
    }

    private static void E(String s1) {
    }

    private static void F(int ai[], int ai1[]) {
        int j2 = 0;
        int l1 = ai[0];
        int k1 = ai[1];
        int j1 = (l1 >>> 4 ^ k1) & 0xf0f0f0f;
        k1 ^= j1;
        l1 ^= j1 << 4;
        j1 = (l1 >>> 16 ^ k1) & 0xffff;
        k1 ^= j1;
        l1 ^= j1 << 16;
        j1 = (k1 >>> 2 ^ l1) & 0x33333333;
        l1 ^= j1;
        k1 ^= j1 << 2;
        j1 = (k1 >>> 8 ^ l1) & 0xff00ff;
        l1 ^= j1;
        k1 ^= j1 << 8;
        k1 = (k1 << 1 | k1 >>> 31 & 1) & -1;
        j1 = (l1 ^ k1) & 0xaaaaaaaa;
        l1 ^= j1;
        k1 ^= j1;
        l1 = (l1 << 1 | l1 >>> 31 & 1) & -1;
        for (int i2 = 0; i2 < 8; i2++) {
            j1 = k1 << 28 | k1 >>> 4;
            long l2 = 0L;
            l2 = ai1[j2];
            j1 ^= ai1[j2];
            j2++;
            int i1 = bj[j1 & 0x3f];
            i1 |= L[j1 >>> 8 & 0x3f];
            i1 |= P[j1 >>> 16 & 0x3f];
            i1 |= T[j1 >>> 24 & 0x3f];
            j1 = k1 ^ ai1[j2];
            j2++;
            i1 |= bg[j1 & 0x3f];
            i1 |= bm[j1 >>> 8 & 0x3f];
            i1 |= N[j1 >>> 16 & 0x3f];
            i1 |= R[j1 >>> 24 & 0x3f];
            l1 ^= i1;
            j1 = l1 << 28 | l1 >>> 4;
            j1 ^= ai1[j2];
            j2++;
            i1 = bj[j1 & 0x3f];
            i1 |= L[j1 >>> 8 & 0x3f];
            i1 |= P[j1 >>> 16 & 0x3f];
            i1 |= T[j1 >>> 24 & 0x3f];
            j1 = l1 ^ ai1[j2];
            j2++;
            i1 |= bg[j1 & 0x3f];
            i1 |= bm[j1 >>> 8 & 0x3f];
            i1 |= N[j1 >>> 16 & 0x3f];
            i1 |= R[j1 >>> 24 & 0x3f];
            k1 ^= i1;
        }
        k1 = k1 << 31 | k1 >>> 1;
        j1 = (l1 ^ k1) & 0xaaaaaaaa;
        l1 ^= j1;
        k1 ^= j1;
        l1 = l1 << 31 | l1 >>> 1;
        j1 = (l1 >>> 8 ^ k1) & 0xff00ff;
        k1 ^= j1;
        l1 ^= j1 << 8;
        j1 = (l1 >>> 2 ^ k1) & 0x33333333;
        k1 ^= j1;
        l1 ^= j1 << 2;
        j1 = (k1 >>> 16 ^ l1) & 0xffff;
        l1 ^= j1;
        k1 ^= j1 << 16;
        j1 = (k1 >>> 4 ^ l1) & 0xf0f0f0f;
        l1 ^= j1;
        k1 ^= j1 << 4;
        ai[0] = k1;
        ai[1] = l1;
    }

    private static byte[] G(byte abyte0[], byte abyte1[]) {
        if (abyte0.length < 32) {
            return null;
        }
        byte abyte3[] = new byte[8];
        byte abyte4[] = new byte[8];
        byte abyte5[] = new byte[8];
        byte abyte2[] = new byte[8];
        System.arraycopy(abyte0, 0, abyte3, 0, 8);
        System.arraycopy(abyte0, 8, abyte4, 0, 8);
        System.arraycopy(abyte0, 16, abyte5, 0, 8);
        System.arraycopy(abyte0, 24, abyte2, 0, 8);
        int ai[] = l(abyte3, true);
        int ai1[] = l(abyte4, false);
        int ai2[] = l(abyte5, true);
        int i1 = abyte1.length % 8;
        byte byte0 = (byte) (8 - i1);
        byte abyte6[] = new byte[abyte1.length + byte0];
        int j1 = abyte6.length / 8 - 1;
        int k1 = 8 * j1;
        byte abyte7[] = new byte[8];
        System.arraycopy(abyte1, k1, abyte7, 0, i1);
        for (int l1 = i1; l1 < 8; l1++) {
            abyte7[l1] = byte0;
        }
        int i2 = 0;
        for (int j2 = 0; i2 < j1; j2 += 8) {
            q(abyte1, j2, abyte6, j2, ai, ai1, ai2, abyte2, true);
            i2++;
        }
        q(abyte7, 0, abyte6, k1, ai, ai1, ai2, abyte2, true);
        return abyte6;
    }

    private static void H(int ai[]) {
        ai[4] = j(ai[0], 5) + r(ai[1], ai[2], ai[3]) + ai[4] + ai[5] + 0x5a827999;
        ai[2] = j(ai[1], 30);
    }

    private static void I(int ai[]) {
        ai[4] = j(ai[0], 5) + n(ai[1], ai[2], ai[3]) + ai[4] + ai[5] + 0x6ed9eba1;
        ai[2] = j(ai[1], 30);
    }

    private static final int J = 2;

    private static final int K = 4;

    private static final int L[] = { 256, 0x2080100, 0x2080000, 0x42000100, 0x80000, 256, 0x40000000, 0x2080000, 0x40080100, 0x80000, 0x2000100, 0x40080100, 0x42000100, 0x42080000, 0x80100, 0x40000000, 0x2000000, 0x40080000, 0x40080000, 0, 0x40000100, 0x42080100, 0x42080100, 0x2000100, 0x42080000, 0x40000100, 0, 0x42000000, 0x2080100, 0x2000000, 0x42000000, 0x80100, 0x80000, 0x42000100, 256, 0x2000000, 0x40000000, 0x2080000, 0x42000100, 0x40080100, 0x2000100, 0x40000000, 0x42080000, 0x2080100, 0x40080100, 256, 0x2000000, 0x42080000, 0x42080100, 0x80100, 0x42000000, 0x42080100, 0x2080000, 0, 0x40080000, 0x42000000, 0x80100, 0x2000100, 0x40000100, 0x80000, 0, 0x40080000, 0x2080100, 0x40000100 };

    private static final int M = 2;

    private static final int N[] = { 0x802001, 8321, 8321, 128, 0x802080, 0x800081, 0x800001, 8193, 0, 0x802000, 0x802000, 0x802081, 129, 0, 0x800080, 0x800001, 1, 8192, 0x800000, 0x802001, 128, 0x800000, 8193, 8320, 0x800081, 1, 8320, 0x800080, 8192, 0x802080, 0x802081, 129, 0x800080, 0x800001, 0x802000, 0x802081, 129, 0, 0, 0x802000, 8320, 0x800080, 0x800081, 1, 0x802001, 8321, 8321, 128, 0x802081, 129, 1, 8192, 0x800001, 8193, 0x802080, 0x800081, 8193, 8320, 0x800000, 0x802001, 128, 0x800000, 8192, 0x802080 };

    private static final int O = 1;

    private static final int P[] = { 520, 0x8020200, 0, 0x8020008, 0x8000200, 0, 0x20208, 0x8000200, 0x20008, 0x8000008, 0x8000008, 0x20000, 0x8020208, 0x20008, 0x8020000, 520, 0x8000000, 8, 0x8020200, 512, 0x20200, 0x8020000, 0x8020008, 0x20208, 0x8000208, 0x20200, 0x20000, 0x8000208, 8, 0x8020208, 512, 0x8000000, 0x8020200, 0x8000000, 0x20008, 520, 0x20000, 0x8020200, 0x8000200, 0, 512, 0x20008, 0x8020208, 0x8000200, 0x8000008, 512, 0, 0x8020008, 0x8000208, 0x20000, 0x8000000, 0x8020208, 8, 0x20208, 0x20200, 0x8000008, 0x8020000, 0x8000208, 520, 0x8020000, 0x20208, 8, 0x8020008, 0x20200 };

    private static final int Q = 0x8f1bbcdc;

    private static final int R[] = { 0x80108020, 0x80008000, 32768, 0x108020, 0x100000, 32, 0x80100020, 0x80008020, 0x80000020, 0x80108020, 0x80108000, 0x80000000, 0x80008000, 0x100000, 32, 0x80100020, 0x108000, 0x100020, 0x80008020, 0, 0x80000000, 32768, 0x108020, 0x80100000, 0x100020, 0x80000020, 0, 0x108000, 32800, 0x80108000, 0x80100000, 32800, 0, 0x108020, 0x80100020, 0x100000, 0x80008020, 0x80100000, 0x80108000, 32768, 0x80100000, 0x80008000, 32, 0x80108020, 0x108020, 32, 32768, 0x80000000, 32800, 0x80108000, 0x100000, 0x80000020, 0x100020, 0x80008020, 0x80000020, 0x100020, 0x108000, 0, 0x80008000, 32800, 0x80000000, 0x80100020, 0x80108020, 0x108000 };

    private static byte S[] = { 13, 16, 10, 23, 0, 4, 2, 27, 14, 5, 20, 9, 22, 18, 11, 3, 25, 7, 15, 6, 26, 19, 12, 1, 40, 51, 30, 36, 46, 54, 29, 39, 50, 44, 32, 47, 43, 48, 38, 55, 33, 52, 45, 41, 49, 35, 28, 31 };

    private static final int T[] = { 0x1010400, 0, 0x10000, 0x1010404, 0x1010004, 0x10404, 4, 0x10000, 1024, 0x1010400, 0x1010404, 1024, 0x1000404, 0x1010004, 0x1000000, 4, 1028, 0x1000400, 0x1000400, 0x10400, 0x10400, 0x1010000, 0x1010000, 0x1000404, 0x10004, 0x1000004, 0x1000004, 0x10004, 0, 1028, 0x10404, 0x1000000, 0x10000, 0x1010404, 4, 0x1010000, 0x1010400, 0x1000000, 0x1000000, 1024, 0x1010004, 0x10000, 0x10400, 0x1000004, 1024, 4, 0x1000404, 0x10404, 0x1010404, 0x10004, 0x1010000, 0x1000404, 0x1000004, 1028, 0x10404, 0x1010400, 1028, 0x1000400, 0x1000400, 0, 0x10004, 0x10400, 0, 0x1010004 };

    private static final int U = 8;

    private static byte V[] = { 56, 48, 40, 32, 24, 16, 8, 0, 57, 49, 41, 33, 25, 17, 9, 1, 58, 50, 42, 34, 26, 18, 10, 2, 59, 51, 43, 35, 62, 54, 46, 38, 30, 22, 14, 6, 61, 53, 45, 37, 29, 21, 13, 5, 60, 52, 44, 36, 28, 20, 12, 4, 27, 19, 11, 3 };

    private static int W[] = { 0x800000, 0x400000, 0x200000, 0x100000, 0x80000, 0x40000, 0x20000, 0x10000, 32768, 16384, 8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1 };

    private static final int X = 0x6ed9eba1;

    private static final int Y = 8;

    private static final int Z = 100;

    private static final int ba = 32;

    private static int bb[] = { 1, 2, 4, 6, 8, 10, 12, 14, 15, 17, 19, 21, 23, 25, 27, 28 };

    private static final int bc = 30;

    private static final String bd = "ZG";

    private static final boolean be = false;

    private static final int bf = 0x5a827999;

    private static final int bg[] = { 0x10001040, 4096, 0x40000, 0x10041040, 0x10000000, 0x10001040, 64, 0x10000000, 0x40040, 0x10040000, 0x10041040, 0x41000, 0x10041000, 0x41040, 4096, 64, 0x10040000, 0x10000040, 0x10001000, 4160, 0x41000, 0x40040, 0x10040040, 0x10041000, 4160, 0, 0, 0x10040040, 0x10000040, 0x10001000, 0x41040, 0x40000, 0x41040, 0x40000, 0x10041000, 4096, 64, 0x10040040, 4096, 0x41040, 0x10001000, 64, 0x10000040, 0x10040000, 0x10040040, 0x10000000, 0x40000, 0x10001040, 0, 0x10041040, 0x40040, 0x10000040, 0x10040000, 0x10001000, 0x10001040, 0, 0x10041040, 0x41000, 0x41000, 4160, 4160, 0x40040, 0x10000000, 0x10041000 };

    private static final int bh = 0xca62c1d6;

    private static byte bi[] = { -128, 64, 32, 16, 8, 4, 2, 1 };

    private static final int bj[] = { 0x200000, 0x4200002, 0x4000802, 0, 2048, 0x4000802, 0x200802, 0x4200800, 0x4200802, 0x200000, 0, 0x4000002, 2, 0x4000000, 0x4200002, 2050, 0x4000800, 0x200802, 0x200002, 0x4000800, 0x4000002, 0x4200000, 0x4200800, 0x200002, 0x4200000, 2048, 2050, 0x4200802, 0x200800, 2, 0x4000000, 0x200800, 0x4000000, 0x200800, 0x200000, 0x4000802, 0x4000802, 0x4200002, 0x4200002, 2, 0x200002, 0x4000000, 0x4000800, 0x200000, 0x4200800, 2050, 0x200802, 0x4200800, 2050, 0x4000002, 0x4200802, 0x4200000, 0x200800, 0, 2, 0x4200802, 0, 0x200802, 0x4200000, 2048, 0x4000002, 0x4000800, 2048, 0x200002 };

    private static final String bk = "ZG_ENCRYPT_FAILED_";

    private static final int bl = 15;

    private static final int bm[] = { 0x20000010, 0x20400000, 16384, 0x20404010, 0x20400000, 16, 0x20404010, 0x400000, 0x20004000, 0x404010, 0x400000, 0x20000010, 0x400010, 0x20004000, 0x20000000, 16400, 0, 0x400010, 0x20004010, 16384, 0x404000, 0x20004010, 16, 0x20400010, 0x20400010, 0, 0x404010, 0x20404000, 16400, 0x404000, 0x20404000, 0x20000000, 0x20004000, 16, 0x20400010, 0x404000, 0x20404010, 0x400000, 16400, 0x20000010, 0x400000, 0x20004000, 0x20000000, 16400, 0x20000010, 0x20404010, 0x404000, 0x20400000, 0x404010, 0x20404000, 0, 0x20400010, 16, 16384, 0x20400000, 0x404010, 16384, 0x400010, 0x20004010, 0, 0x20404000, 0x20000000, 0x400010, 0x20004010 };
}
