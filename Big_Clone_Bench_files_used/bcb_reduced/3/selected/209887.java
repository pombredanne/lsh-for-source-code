package net.sf.jradius.util;

import gnu.crypto.hash.HashFactory;
import gnu.crypto.hash.IMessageDigest;
import gnu.crypto.mac.IMac;
import gnu.crypto.mac.MacFactory;
import java.util.HashMap;

/**
 * MD5 Utils including HMAC-MD5
 * @author David Bird
 */
public class MD5 {

    public static byte[] md5(byte[] text) {
        IMessageDigest md = HashFactory.getInstance("MD5");
        md.update(text, 0, text.length);
        return md.digest();
    }

    public static byte[] md5(byte[] text1, byte[] text2) {
        IMessageDigest md = HashFactory.getInstance("MD5");
        md.update(text1, 0, text1.length);
        md.update(text2, 0, text2.length);
        return md.digest();
    }

    public static byte[] hmac_md5(byte[] text, byte[] key) {
        int minKeyLen = 64;
        byte[] digest = new byte[16];
        if (key.length < minKeyLen) {
            byte[] t = new byte[minKeyLen];
            System.arraycopy(key, 0, t, 0, key.length);
            key = t;
        }
        IMac mac = MacFactory.getInstance("HMAC-MD5");
        HashMap attributes = new HashMap();
        attributes.put(IMac.MAC_KEY_MATERIAL, key);
        attributes.put(IMac.TRUNCATED_SIZE, new Integer(16));
        try {
            mac.init(attributes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mac.update(text, 0, text.length);
        System.arraycopy(mac.digest(), 0, digest, 0, 16);
        return digest;
    }
}
