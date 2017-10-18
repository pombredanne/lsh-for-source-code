package org.parosproxy.paros.extension.encoder;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {

    private static final String CODEPAGE = "8859_1";

    public String getURLEncode(String msg) {
        String result = "";
        try {
            result = URLEncoder.encode(msg, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getURLDecode(String msg) {
        String result = "";
        try {
            result = URLDecoder.decode(msg, "UTF8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public String getIllegalUTF8Encode(String msg, int bytes) {
        String result = IllegalUTF8.encode(msg, bytes);
        return result;
    }

    public byte[] getHashSHA1(byte[] buf) throws NoSuchAlgorithmException {
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        sha.update(buf);
        return sha.digest();
    }

    public byte[] getHashMD5(byte[] buf) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(buf);
        return md5.digest();
    }

    public String getHexString(byte[] buf) {
        StringBuffer sb = new StringBuffer(20);
        for (int i = 0; i < buf.length; i++) {
            int digit = ((int) buf[i]) & 0xFF;
            String hexDigit = Integer.toHexString(digit).toUpperCase();
            if (hexDigit.length() == 1) {
                sb.append('0');
            }
            sb.append(hexDigit);
        }
        return sb.toString();
    }

    public byte[] getBytes(String buf) {
        byte[] result = null;
        try {
            result = buf.getBytes(CODEPAGE);
        } catch (UnsupportedEncodingException e) {
        }
        return result;
    }

    /**
	 * The Base64 decoder perform Base64 decode even if the string is incorrect.
	 * This method is used to check if the code is correct.
	 */
    public boolean isValidBase64(String buf) {
        String result = Base64.encodeBytes(Base64.decode(buf));
        if (buf.equals(result)) {
            return true;
        } else {
            return false;
        }
    }

    public String getBase64Encode(String msg) {
        String result = "";
        result = Base64.encodeBytes(getBytes(msg));
        return result;
    }

    public String getBase64Decode(String msg) {
        String result = "";
        if (!isValidBase64(msg)) {
            return result;
        }
        try {
            result = new String(Base64.decode(msg), CODEPAGE);
        } catch (Exception e) {
        }
        return result;
    }
}
