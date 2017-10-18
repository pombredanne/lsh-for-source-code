package org.localstorm.mcc.ejb.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class MD5Util {

    static {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public static byte[] getMD5(String str) {
        return md5.digest(str.getBytes());
    }

    private static MessageDigest md5;

    public static String md5ToString(byte[] md5) {
        int len = md5.length;
        StringBuffer res = new StringBuffer(len);
        for (int i = 0; i < len; ++i) {
            if (md5[i] < 0) {
                res.append(Integer.toHexString(md5[i] + 256));
            } else {
                String rs = Integer.toHexString(md5[i]);
                if (rs.length() == 1) {
                    res.append('0');
                }
                res.append(rs);
            }
        }
        return res.toString();
    }
}
