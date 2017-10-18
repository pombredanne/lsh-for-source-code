package com.yubuild.coreman.common.util;

import java.security.MessageDigest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.codec.binary.Base64;

public class StringUtil {

    private static final Log log;

    protected static final String hexChars[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F" };

    static {
        log = LogFactory.getLog(com.yubuild.coreman.common.util.StringUtil.class);
    }

    public static String encodePassword(String password, String algorithm) {
        if (password == null) return null;
        byte unencodedPassword[] = password.getBytes();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (Exception e) {
            log.error("Exception: " + e);
            return password;
        }
        md.reset();
        md.update(unencodedPassword);
        byte encodedPassword[] = md.digest();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < encodedPassword.length; i++) {
            if ((encodedPassword[i] & 0xff) < 16) buf.append("0");
            buf.append(Long.toString(encodedPassword[i] & 0xff, 16));
        }
        return buf.toString();
    }

    public static String encodeString(String str) {
        return new String(Base64.encodeBase64(str.getBytes()));
    }

    public static String decodeString(String str) {
        return new String(Base64.decodeBase64(str.getBytes()));
    }

    public static String hexify(byte data[]) {
        if (data == null) return "null";
        StringBuffer out = new StringBuffer(1256);
        for (int i = 0; i < data.length; i++) {
            out.append(hexChars[data[i] >> 4 & 0xf]);
            out.append(hexChars[data[i] & 0xf]);
        }
        return out.toString();
    }

    public static byte[] parseHexString(String byteString) {
        byte result[] = new byte[byteString.length() / 2];
        for (int i = 0; i < byteString.length(); i += 2) {
            String toParse = byteString.substring(i, i + 2);
            result[i / 2] = (byte) Integer.parseInt(toParse, 16);
        }
        return result;
    }

    public static String replaceString(String s, String s1, String s2) {
        if ("".equals(s1) || "".equals(s) || s == null || s1 == null || s2 == null) return s;
        StringBuffer stringbuffer = null;
        int j = 0;
        int i;
        while ((i = s.indexOf(s1, j)) != -1) {
            String s4 = s.substring(j, i);
            j = i + s1.length();
            if (stringbuffer == null) stringbuffer = new StringBuffer(s2.length());
            stringbuffer.append(s4);
            stringbuffer.append(s2);
        }
        return stringbuffer != null ? stringbuffer.append(s.substring(j)).toString() : s;
    }

    public static String removeSerCharsAndSpace(String string1) {
        String string2 = "";
        if (string1 == null) return null;
        char[] charArray = string1.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            switch((int) charArray[i]) {
                case 352:
                    string2 += "S";
                    break;
                case 353:
                    string2 += "s";
                    break;
                case 272:
                    string2 += "Dj";
                    break;
                case 273:
                    string2 += "dj";
                    break;
                case 269:
                case 263:
                    string2 += "c";
                    break;
                case 268:
                case 262:
                    string2 += "C";
                    break;
                case 381:
                    string2 += "Z";
                    break;
                case 382:
                    string2 += "z";
                    break;
                case ' ':
                    string2 += "_";
                    break;
                default:
                    string2 += charArray[i];
            }
        }
        return string2;
    }
}
