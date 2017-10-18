package com.jguigen.secure;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.JPasswordField;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;

public final class PasswordService {

    private static PasswordService instance;

    private PasswordService() {
    }

    public synchronized String encrypt(String plaintext) throws UnsupportedEncodingException {
        try {
            encrypt(plaintext.toCharArray());
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedEncodingException(e.getMessage());
        }
        return "";
    }

    public synchronized String encrypt(char[] chartext) throws UnsupportedEncodingException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedEncodingException(e.getMessage());
        } catch (Exception ex) {
            System.out.println("General Exception encoding creating the Diget");
            ex.printStackTrace();
            throw new UnsupportedEncodingException(ex.getMessage());
        }
        try {
            byte[] bytetext = encodeUTF8(chartext);
            md.update(bytetext);
            for (int i = 0; i < bytetext.length; i++) {
                chartext[i] = 0;
                bytetext[i] = 0;
            }
        } catch (Exception e) {
            for (int i = 0; i < chartext.length; i++) {
                chartext[i] = 0;
            }
            throw new UnsupportedEncodingException(e.getMessage());
        }
        byte raw[] = md.digest();
        String hash = (new BASE64Encoder()).encode(raw);
        return hash;
    }

    public static synchronized PasswordService getInstance() {
        if (instance == null) {
            instance = new PasswordService();
        }
        return instance;
    }

    /**
	   * GHP Taken from koders.com
	   * The UTF-8 text transfer format is used throughout Freenet. Because the
	   * format is widely used this implementation has been optimized for
	   * performance. An advantage over methods like <code>String.getBytes(encoding)</code>
	   * is a UnsupportedEncodingException will never be thrown.
	   * <p>
	   * Unless the method is named <code>*WithCount</code> the characters are
	   * encoded without two bytes length prefixed. The <code>*WithCount</code>
	   * methods do the encoding like
	   * {@link java.io.DataOutuptStream#readUTF() DataOutputStream.readUTF}.
	   * </p>
	   * <p>
	   * See <a href="http://ietf.org/rfc/rfc2279.txt">RFC 2279</a> for details
	   * about the UTF-8 transform format.
	   * </p>
	   * 
	   * @author syoung
	   */
    public static byte[] encodeUTF8(char[] chars) {
        if (chars == null) {
            throw new IllegalArgumentException("Cannot convert null character array to UTF-8");
        }
        byte[] result = new byte[getEncodedLength(chars)];
        int pos = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c <= 0x007F) {
                result[pos++] = (byte) (c & 0xFF);
            } else if (c <= 0x07FF) {
                result[pos++] = (byte) (0xC0 | (c >> 6));
                result[pos++] = (byte) (0x80 | (c & 0x3F));
            } else {
                result[pos++] = (byte) (0xE0 | (c >> 12));
                result[pos++] = (byte) (0xC0 | (c >> 6));
                result[pos++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return result;
    }

    public static int getEncodedLength(char[] chars) {
        if (chars == null) {
            throw new IllegalArgumentException("Cannot calculate UTF-8 length of null array");
        }
        int result = 0;
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c >= 0x0001) && (c <= 0x007F)) {
                result++;
            } else if (c > 0x07FF) {
                result += 3;
            } else {
                result += 2;
            }
        }
        return result;
    }
}
