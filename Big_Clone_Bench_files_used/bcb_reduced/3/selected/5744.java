package websiteschema.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.*;

public class EncryptUtil {

    public EncryptUtil() {
    }

    public String Encrypt(String strSrc, String encName) {
        MessageDigest md = null;
        String strDes = null;
        byte[] bt = strSrc.getBytes();
        try {
            if (encName == null || encName.equals("")) {
                encName = "MD5";
            }
            md = MessageDigest.getInstance(encName);
            md.update(bt);
            strDes = bytes2Hex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Invalid algorithm.");
            return null;
        }
        return strDes;
    }

    @SuppressWarnings("unchecked")
    public String base64(String str) {
        return new sun.misc.BASE64Encoder().encode(str.getBytes());
    }

    public String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }

    public static void main(String[] args) throws UnsupportedEncodingException {
        EncryptUtil te = new EncryptUtil();
        String strSrc = "Fengyingrui1!";
        System.out.println("Source String:" + strSrc);
        System.out.println("Encrypted String:");
        System.out.println("Use Def:" + te.Encrypt(strSrc, null));
        System.out.println("Use MD5:" + te.Encrypt(strSrc, "MD5"));
        System.out.println("Use SHA:" + te.Encrypt(strSrc, "SHA-1"));
        System.out.println("Use SHA-256:" + te.Encrypt(strSrc, "SHA-256"));
        System.out.println("Use BASE64:" + te.base64(""));
        String username = "yingrui.f@gmail.com";
        System.out.println(URLEncoder.encode(username, "UTF-8"));
        System.out.println("username:" + te.base64(URLEncoder.encode(username, "UTF-8")));
        String nonce = "TEZ47T";
        String serverTime = "1333185355";
        String password = "websiteschema";
        System.out.println("password:" + te.Encrypt(te.Encrypt(te.Encrypt(password, "SHA-1"), "SHA-1") + serverTime + nonce, "SHA-1"));
    }
}
