package utils.passwd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class PasswdUtils {

    public static String encodePassword(String plainTextPassword) throws UnsupportedEncodingException, NoSuchAlgorithmException {
        StringBuffer sb = new StringBuffer();
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
        messageDigest.update(plainTextPassword.getBytes("UTF-8"));
        byte[] digestBytes = messageDigest.digest();
        String hex = null;
        for (int i = 0; i < digestBytes.length; i++) {
            hex = Integer.toHexString(0xFF & digestBytes[i]);
            if (hex.length() < 2) sb.append("0");
            sb.append(hex);
        }
        return new String(sb);
    }

    private PasswdUtils() {
    }
}
