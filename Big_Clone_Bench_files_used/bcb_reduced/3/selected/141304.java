package ias.springnote.rest;

import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 
 * @author ias
 * @deprecated
 */
public class AuthUtil {

    private static final SimpleDateFormat fmtDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private static final Random rng = new Random(System.currentTimeMillis());

    public static String generatePassword(String userKey, int applicationId, String applicationKey) {
        String nonce = generateNonce();
        String createDate = fmtDate.format(new Date());
        String keyDigest = null;
        MessageDigest sha1 = null;
        try {
            sha1 = MessageDigest.getInstance("SHA1");
            sha1.update(nonce.getBytes("UTF-8"));
            sha1.update(createDate.getBytes("UTF-8"));
            sha1.update(userKey.getBytes("UTF-8"));
            sha1.update(applicationKey.getBytes("UTF-8"));
            keyDigest = getHexaDecimal(sha1.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(applicationId);
        sb.append(',');
        sb.append(nonce);
        sb.append(',');
        sb.append(createDate);
        sb.append(',');
        sb.append(keyDigest);
        return sb.toString();
    }

    private static String generateNonce() {
        byte[] buf = new byte[8];
        rng.nextBytes(buf);
        return getHexaDecimal(buf);
    }

    private static String getHexaDecimal(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int v = (int) b[i];
            if (v < 0) v += 0x100;
            String s = Integer.toHexString(v);
            if (s.length() == 1) sb.append('0');
            sb.append(s);
        }
        return sb.toString();
    }
}
