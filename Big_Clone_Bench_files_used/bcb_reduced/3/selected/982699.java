package binky.dan.utils.encryption;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5HashUtil extends HashUtil {

    MD5HashUtil() {
    }

    @Override
    public String hashString(String input) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] bytesOfMessage = input.getBytes("UTF-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        return bytesToHex(md.digest(bytesOfMessage));
    }
}
