package com.magnetstreet.ws.mdverify.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;

/**
 * SecurityUtilities
 *
 * @author Martin Dale Lyness <martin.lyness@gmail.com>
 * @version 0.1.0 Apr 28, 2009
 * @since Apr 28, 2009
 */
public class SecurityUtilities {

    public static String sha1(String in) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] data = new byte[40];
        try {
            md.update(in.getBytes("iso-8859-1"), 0, in.length());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        data = md.digest();
        return HexidecimalUtilities.convertFromByteArrayToHex(data);
    }
}
