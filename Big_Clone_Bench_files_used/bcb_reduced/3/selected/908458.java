package sdloader.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * メッセージダイジェスト用Util
 * 
 * @author shot
 * @author c9katayama
 */
public class MessageDigestUtil {

    protected static String ALGORHTYM = "MD5";

    /**
	 * 引数の文字列を元にダイジェストした文字列を返します。
	 * 
	 * @param planeText
	 * @return
	 */
    public static String digest(String planeText) {
        MessageDigest digest = createMessageDigest();
        byte[] b = planeText.getBytes();
        String hex = toHexString(digest.digest(b));
        return hex;
    }

    /**
	 * byte[]を16進文字列に変換します
	 * 
	 * @param buf
	 * @return
	 */
    public static String toHexString(byte[] buf) {
        String digestText = "";
        for (int i = 0; i < buf.length; i++) {
            int n = buf[i] & 0xff;
            if (n < 16) {
                digestText += "0";
            }
            digestText += Integer.toHexString(n).toUpperCase();
        }
        return digestText;
    }

    /**
	 * MessageDisgestを作成します。
	 * 
	 * @return
	 */
    public static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance(ALGORHTYM);
        } catch (NoSuchAlgorithmException e) {
            throw new ExceptionInInitializerError();
        }
    }

    public static void setAlgorithm(String algorithm) {
        ALGORHTYM = algorithm;
    }

    public static void resetAlgorithm() {
        ALGORHTYM = "MD5";
    }
}
