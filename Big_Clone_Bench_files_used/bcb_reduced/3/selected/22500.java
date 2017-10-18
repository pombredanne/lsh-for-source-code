package cc.carnero.ctwee;

import java.net.*;
import java.math.*;
import java.security.*;
import android.util.Log;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * set of basic useful functions
 */
public class ctCommon {

    private static char[] base64map1 = new char[64];

    static {
        int i = 0;
        for (char c = 'A'; c <= 'Z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = 'a'; c <= 'z'; c++) {
            base64map1[i++] = c;
        }
        for (char c = '0'; c <= '9'; c++) {
            base64map1[i++] = c;
        }
        base64map1[i++] = '+';
        base64map1[i++] = '/';
    }

    private static byte[] base64map2 = new byte[128];

    static {
        for (int i = 0; i < base64map2.length; i++) {
            base64map2[i] = -1;
        }
        for (int i = 0; i < 64; i++) {
            base64map2[base64map1[i]] = (byte) i;
        }
    }

    /**
	 * Returns md5 hash of given string
	 *
	 * @param	text	text to be hashed
	 * @return	 hash
	 */
    public static String md5(String text) {
        String hashed = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(text.getBytes(), 0, text.length());
            hashed = new BigInteger(1, digest.digest()).toString(16);
        } catch (Exception e) {
            Log.e(ctGlobal.tag, "ctCommon.md5: " + e.toString());
        }
        return hashed;
    }

    /**
	 * Joins text items in array into one string using given delimiter
	 *
	 * @param	delim	delimiter
	 * @param	array	array of texts to join
	 * @return 	joined string
	 */
    public static String implode(String delim, Object[] array) {
        String out = "";
        try {
            for (int i = 0; i < array.length; i++) {
                if (i != 0) {
                    out += delim;
                }
                out += array[i].toString();
            }
        } catch (Exception e) {
            Log.e(ctGlobal.tag, "ctCommon.implode: " + e.toString());
        }
        return out;
    }

    /**
	 * Encrypt text with salt using HMAC-SHA1 algorithm
	 *
	 * @param	text	string to encrypt
	 * @param	salt	string used to encryption
	 * @return	 encrypted bytes
	 */
    public static byte[] hashHmac(String text, String salt) {
        byte[] macBytes = {};
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(salt.getBytes(), "HmacSHA1");
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKeySpec);
            macBytes = mac.doFinal(text.getBytes());
        } catch (Exception e) {
            Log.e(ctGlobal.tag, "ctCommon.hashHmac: " + e.toString());
        }
        return macBytes;
    }

    /**
	* Encodes a byte array into Base64 format
	* No blanks or line breaks are inserted
	 *
	* @param	in	an array containing the data bytes to be encoded
	* @param	iLen	number of bytes to process in 'in'
	* @return	 character array with the Base64 encoded data
	*/
    public static String base64Encode(byte[] in) {
        int iLen = in.length;
        int oDataLen = (iLen * 4 + 2) / 3;
        int oLen = ((iLen + 2) / 3) * 4;
        char[] out = new char[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++] & 0xff;
            int i1 = ip < iLen ? in[ip++] & 0xff : 0;
            int i2 = ip < iLen ? in[ip++] & 0xff : 0;
            int o0 = i0 >>> 2;
            int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
            int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
            int o3 = i2 & 0x3F;
            out[op++] = base64map1[o0];
            out[op++] = base64map1[o1];
            out[op] = op < oDataLen ? base64map1[o2] : '=';
            op++;
            out[op] = op < oDataLen ? base64map1[o3] : '=';
            op++;
        }
        return new String(out);
    }

    /**
	* Decodes a byte array from Base64 format
	* No blanks or line breaks are allowed within the Base64 encoded data
	 *
	* @param	text	string containing the Base64 encoded data
	* @return    an	array containing the decoded data bytes
	* @throws	IllegalArgumentException if the input is not valid Base64 encoded data
	*/
    public static byte[] base64Decode(String text) {
        char[] in = text.toCharArray();
        int iLen = in.length;
        if (iLen % 4 != 0) {
            throw new IllegalArgumentException("Length of Base64 encoded input string is not a multiple of 4.");
        }
        while (iLen > 0 && in[iLen - 1] == '=') {
            iLen--;
        }
        int oLen = (iLen * 3) / 4;
        byte[] out = new byte[oLen];
        int ip = 0;
        int op = 0;
        while (ip < iLen) {
            int i0 = in[ip++];
            int i1 = in[ip++];
            int i2 = ip < iLen ? in[ip++] : 'A';
            int i3 = ip < iLen ? in[ip++] : 'A';
            if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int b0 = base64map2[i0];
            int b1 = base64map2[i1];
            int b2 = base64map2[i2];
            int b3 = base64map2[i3];
            if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
                throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
            }
            int o0 = (b0 << 2) | (b1 >>> 4);
            int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
            int o2 = ((b2 & 3) << 6) | b3;
            out[op++] = (byte) o0;
            if (op < oLen) {
                out[op++] = (byte) o1;
            }
            if (op < oLen) {
                out[op++] = (byte) o2;
            }
        }
        return out;
    }

    public static String urlencode_rfc3986(String text) {
        String encoded = URLEncoder.encode(text).replace("+", "%20").replaceAll("%7E", "~");
        return encoded;
    }
}
