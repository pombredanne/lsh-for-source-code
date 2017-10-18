package com.tieland.xunda.common.util.encode;

import java.security.MessageDigest;

/**
 * User: qiukx
 * Date: 2008-7-16
 * Company: Freshpower
 * Description:
 */
public class SHAEncoder implements Encoder {

    public String encode(String plain) {
        try {
            byte[] strTemp = plain.getBytes();
            MessageDigest SHA = MessageDigest.getInstance("SHA");
            SHA.update(strTemp);
            byte[] sha = SHA.digest();
            return new String(sha);
        } catch (Exception e) {
            return null;
        }
    }

    public String decode(String crypto) {
        return null;
    }
}
