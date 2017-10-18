package org.tapestrycomponents.tassel.utilities;

import java.security.MessageDigest;

public class MDFiver {

    public static String toMD5Sum(String arg0) {
        String ret;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(arg0.getBytes());
            ret = toHexString(md.digest());
        } catch (Exception e) {
            ret = arg0;
        }
        return ret;
    }

    private static String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer(b.length * 2);
        for (int i = 0; i < b.length; i++) {
            sb.append(hexChar[(b[i] & 0xf0) >>> 4]);
            sb.append(hexChar[b[i] & 0x0f]);
        }
        return sb.toString();
    }

    private static char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
}
