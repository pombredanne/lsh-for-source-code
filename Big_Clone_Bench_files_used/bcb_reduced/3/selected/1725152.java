package collabreate.server;

import java.security.*;

/**
 * HmacMD5
 * This class is responsible for computing an HmacMD5 value
 * for use in the challenge and response authentication portion
 * of a collabreate server connection, see RFC 2104
 * @author Tim Vidas
 * @author Chris Eagle
 * @version 0.1.0, August 2008
 */
public class HmacMD5 {

    /**
    * hmac calculates an HmacMD5 value
    * @param msg a byte array to hash
    * @param key a byte array to use as the hmac key
    * @return the hmacMD5
    */
    protected static byte[] hmac(byte[] msg, byte[] key) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception ex) {
        }
        if (key.length > 64) {
            md5.reset();
            key = md5.digest(key);
        }
        byte ipad[] = new byte[64];
        System.arraycopy(key, 0, ipad, 0, key.length);
        byte opad[] = ipad.clone();
        for (int i = 0; i < ipad.length; i++) {
            ipad[i] ^= (byte) 0x36;
            opad[i] ^= (byte) 0x5c;
        }
        md5.reset();
        md5.update(ipad);
        byte digest[] = md5.digest(msg);
        md5.reset();
        md5.update(opad);
        return md5.digest(digest);
    }
}
