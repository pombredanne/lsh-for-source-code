package core.utils;

import java.security.MessageDigest;

public class StringUtils {

    public static final String atoz = "abcdefghijklmnopqrstuvwxyz";

    public static final String AtoZ = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String DIGITS = "0123456789";

    public static String generateRandomString(int len) {
        String chars = atoz + AtoZ + DIGITS;
        StringBuffer buf = new StringBuffer(len);
        for (int i = 0; i < len; i++) {
            buf.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return buf.toString();
    }

    public static String bytesArrayToHexString(byte hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (byte i : hash) {
            if (((int) i & 0xff) < 0x10) {
                buf.append("0");
            }
            buf.append(Long.toString((int) i & 0xff, 16));
        }
        return buf.toString();
    }

    public static String md5Hash(String src) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(src.getBytes());
            return bytesArrayToHexString(md.digest());
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean checkString(String s) {
        return s != null && !s.trim().equals("");
    }

    public static boolean checkStrings(String... ss) {
        for (String s : ss) {
            if (!checkString(s)) {
                return false;
            }
        }
        return true;
    }

    public static String trim(String src, char d) {
        int st = 0;
        int len = src.length() - 1;
        while (st < len && src.charAt(st) == d) {
            st++;
        }
        while (len > st && src.charAt(len) == d) {
            len--;
        }
        return src.substring(st, len + 1);
    }

    public static String trimRight(String src, char d) {
        int st = 0;
        int len = src.length() - 1;
        while (len > st && src.charAt(len) == d) {
            len--;
        }
        return src.substring(st, len + 1);
    }

    public static String[] getSuffixes(String src, int minLength) {
        int n = src.length() - minLength + 1;
        if (n <= 0) return new String[] {};
        String[] ret = new String[n];
        for (int i = 0; i < n; i++) {
            ret[i] = src.substring(i).toLowerCase();
        }
        return ret;
    }

    public static void main(String... args) {
        System.out.println(generateRandomString(5));
        System.out.println(md5Hash("qwe"));
        for (String s : getSuffixes("qwe.html", 3)) {
            System.out.println(s);
        }
    }
}
