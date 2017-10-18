package jp.web.sync.android.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author m-shichi@SYNC Co., Ltd.
 *
 */
public class CustomStringUtils {

    private static final String MATCH_MAIL = "([a-zA-Z0-9][a-zA-Z0-9_.+\\-]*)@(([a-zA-Z0-9][a-zA-Z0-9_\\-]+\\.)+[a-zA-Z]{2,6})";

    private CustomStringUtils() {
    }

    /**
	 *
	 * @param o
	 * @return
	 */
    public static String nullValue(Object o) {
        if (null == o) {
            return "";
        }
        return o.toString();
    }

    /**
	 *
	 * @param str
	 * @return
	 */
    public static String digestMd5(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("文字列がNull、または空です。");
        }
        MessageDigest md5;
        byte[] enclyptedHash;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(str.getBytes());
            enclyptedHash = md5.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return "";
        }
        return bytesToHexString(enclyptedHash);
    }

    /**
	 *
	 * @param fromByte
	 * @return
	 */
    private static String bytesToHexString(byte[] fromByte) {
        StringBuilder hexStrBuilder = new StringBuilder();
        for (int i = 0; i < fromByte.length; i++) {
            if ((fromByte[i] & 0xff) < 0x10) {
                hexStrBuilder.append("0");
            }
            hexStrBuilder.append(Integer.toHexString(0xff & fromByte[i]));
        }
        return hexStrBuilder.toString();
    }

    /**
	 *
	 * @param str
	 * @return
	 */
    public static boolean checkMailAddr(String str) {
        Pattern pattern = Pattern.compile(MATCH_MAIL);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }
}
