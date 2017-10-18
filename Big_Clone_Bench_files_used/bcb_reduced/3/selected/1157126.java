package mecca.util;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

public final class PasswordService {

    private static String testpwd;

    static {
        try {
            testpwd = encrypt("super");
        } catch (Exception e) {
        }
    }

    public static String encrypt(String txt) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA");
        md.update(txt.getBytes("UTF-8"));
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public static boolean compare(String userpwd, String storedpwd) throws Exception {
        return storedpwd.equals(encrypt(userpwd));
    }

    public static void main(String[] args) throws Exception {
        String txt = args[0];
        if (PasswordService.compare(txt, testpwd)) {
            System.out.println("Access Granted..");
        } else {
            System.out.println("ACCESS DENIED!!");
        }
    }
}
