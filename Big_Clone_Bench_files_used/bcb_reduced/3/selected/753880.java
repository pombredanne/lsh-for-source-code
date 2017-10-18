package com.tieland.xunda.common.util.encode;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: qiukx
 * Date: 2008-7-16
 * Company: Freshpower
 * Description:
 */
public class MD5Encoder implements Encoder {

    public String encode(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plain.getBytes());
            byte b[] = md.digest();
            int i;
            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) i += 256;
                if (i < 16) buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String args[]) {
        MD5Encoder m = new MD5Encoder();
        String a = m.encode("asda");
        System.out.println(a);
    }

    public String decode(String crypto) {
        return null;
    }
}
