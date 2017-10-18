package rath.jmsn.util;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * If you use 'Remember password' feature, 
 * your password will be stored in Global.prop file as 
 * clear-text format.
 * <p>
 * Okay, this class provides crypted password in 
 * your jmsn configuration file by DES crypto library.
 * <p>
 * I use JCE library that default included in jdk 1.4.
 * If you use jdk 1.2/1.3, you can download JCE manually.
 * See the following URL, 
 * <p>
 * <a href="http://javashoplm.sun.com/ECom/docs/Welcome.jsp?StoreId=22&PartDetailId=JCE-1_2_2-G-JS&TransactionId=Try">http://javashoplm.sun.com/ECom/docs/Welcome.jsp?StoreId=22&PartDetailId=JCE-1_2_2-G-JS&TransactionId=Try</a>
 * <p>
 * Good luck.
 * 
 * @author Jang-Ho Hwang, rath@xrath.com
 * @version 1.0.000, 2004/06/08
 */
public class LocalPassword {

    private static LocalPassword THIS = null;

    private Cipher cipher = null;

    private LocalPassword() {
        try {
            cipher = Cipher.getInstance("DES");
        } catch (NoSuchPaddingException e) {
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public String encode(String userid, String password) {
        String ret = null;
        try {
            cipher.init(Cipher.ENCRYPT_MODE, makeKey(userid));
            byte[] out = cipher.doFinal(password.getBytes());
            ret = toHexString(out);
        } catch (InvalidKeyException e) {
            System.err.println("LocalPassword encode failed: " + e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("LocalPassword encode failed: " + e);
        } catch (BadPaddingException e) {
            System.err.println("LocalPassword encode failed: " + e);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("LocalPassword cannot make a key: " + e);
        }
        return ret;
    }

    public String decode(String userid, String encoded) {
        String ret = null;
        try {
            cipher.init(Cipher.DECRYPT_MODE, makeKey(userid));
            byte[] out = cipher.doFinal(fromHexString(encoded));
            ret = new String(out);
        } catch (InvalidKeyException e) {
            System.err.println("LocalPassword decode failed: " + e);
        } catch (IllegalBlockSizeException e) {
            System.err.println("LocalPassword decode failed: " + e);
        } catch (BadPaddingException e) {
            System.err.println("LocalPassword decode failed: " + e);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("LocalPassword cannot make a key: " + e);
        }
        return ret;
    }

    /**
	 * Make a SecretKey instance from User
	 */
    protected SecretKey makeKey(String userid) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("md5");
        byte[] k = md5.digest(userid.getBytes());
        return new SecretKeySpec(k, 0, 8, "DES");
    }

    /** 
	 *
	 */
    protected String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            int v = b[i] < 0 ? (int) b[i] + 0x100 : (int) b[i];
            String hex = Integer.toHexString(v);
            if (hex.length() == 1) sb.append('0');
            sb.append(hex);
        }
        return sb.toString();
    }

    protected byte[] fromHexString(String hex) {
        byte[] ret = new byte[hex.length() / 2];
        for (int i = 0, len = hex.length(); i < len; i += 2) {
            int v = Integer.parseInt(hex.substring(i, i + 2), 16);
            ret[i / 2] = (byte) v;
        }
        return ret;
    }

    /**
	 * Get LocalPassword instance as singleton.
	 */
    public static LocalPassword getInstance() {
        if (THIS != null) return THIS;
        try {
            Class.forName("javax.crypto.Cipher");
            THIS = new LocalPassword();
        } catch (ClassNotFoundException e) {
            THIS = new NoJCE();
        }
        return THIS;
    }

    /**
	 * When user have not JCE lib, this LocalPassword provider 
	 * would be launched!
	 */
    static class NoJCE extends LocalPassword {

        private NoJCE() {
        }

        public String encode(String userid, String password) {
            return password;
        }

        public String decode(String userid, String encoded) {
            return encoded;
        }
    }
}
