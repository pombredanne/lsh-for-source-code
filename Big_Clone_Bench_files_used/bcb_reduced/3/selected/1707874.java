package csiebug.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Scanner;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import org.apache.commons.codec.DecoderException;
import sun.misc.BASE64Encoder;

public class ShaEncoder {

    public static void main(String[] args) throws InvalidKeyException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, DecoderException {
        Scanner in = new Scanner(System.in);
        System.out.print("請輸入密碼:");
        String password = in.nextLine();
        System.out.print("加密密碼:" + getSHA256String(password));
        in.close();
    }

    private static String convertToHex(byte[] data) {
        AssertUtility.notNull(data);
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private static String getHashString(String text, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        AssertUtility.notNull(text);
        AssertUtility.notNullAndNotSpace(algorithm);
        MessageDigest md;
        md = MessageDigest.getInstance(algorithm);
        md.update(text.getBytes("UTF-8"), 0, text.length());
        byte[] hash = md.digest();
        return convertToHex(hash);
    }

    private static String getBase64(String text, String algorithm) throws NoSuchAlgorithmException {
        AssertUtility.notNull(text);
        AssertUtility.notNullAndNotSpace(algorithm);
        String base64;
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(text.getBytes());
        base64 = new BASE64Encoder().encode(md.digest());
        return base64;
    }

    /**
	 * fortify不鼓勵使用SHA-1演算法來加密
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    @Deprecated
    public static String getSHA1String(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        AssertUtility.notNull(text);
        return getHashString(text, "SHA-1");
    }

    public static String getSHA256String(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        AssertUtility.notNull(text);
        return getHashString(text, "SHA-256");
    }

    /**
	 * fortify不鼓勵使用SHA-1演算法來加密
	 * @param text
	 * @return
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    @Deprecated
    public static String getSHA1Base64(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        AssertUtility.notNull(text);
        return getBase64(text, "SHA-1");
    }

    public static String getSHA256Base64(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        AssertUtility.notNull(text);
        return getBase64(text, "SHA-256");
    }
}
