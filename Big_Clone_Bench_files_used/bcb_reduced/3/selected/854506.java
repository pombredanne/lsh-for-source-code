package org.ofbiz.base.crypto;

import java.security.MessageDigest;
import org.ofbiz.base.util.Debug;
import org.ofbiz.base.util.StringUtil;

/**
 * Utility class for doing SHA-1/MD5 One-Way Hash Encryption
 *
 */
public class HashCrypt {

    public static final String module = HashCrypt.class.getName();

    public static String getDigestHash(String str) {
        return getDigestHash(str, "SHA");
    }

    public static String getDigestHash(String str, String hashType) {
        if (str == null) return null;
        try {
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            byte strBytes[] = str.getBytes();
            messagedigest.update(strBytes);
            byte digestBytes[] = messagedigest.digest();
            int k = 0;
            char digestChars[] = new char[digestBytes.length * 2];
            for (int l = 0; l < digestBytes.length; l++) {
                int i1 = digestBytes[l];
                if (i1 < 0) i1 = 127 + i1 * -1;
                StringUtil.encodeInt(i1, k, digestChars);
                k += 2;
            }
            return new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            Debug.logError(e, "Error while computing hash of type " + hashType, module);
        }
        return str;
    }

    public static String getDigestHash(String str, String code, String hashType) {
        if (str == null) return null;
        try {
            byte codeBytes[] = null;
            if (code == null) codeBytes = str.getBytes(); else codeBytes = str.getBytes(code);
            MessageDigest messagedigest = MessageDigest.getInstance(hashType);
            messagedigest.update(codeBytes);
            byte digestBytes[] = messagedigest.digest();
            int i = 0;
            char digestChars[] = new char[digestBytes.length * 2];
            for (int j = 0; j < digestBytes.length; j++) {
                int k = digestBytes[j];
                if (k < 0) {
                    k = 127 + k * -1;
                }
                StringUtil.encodeInt(k, i, digestChars);
                i += 2;
            }
            return new String(digestChars, 0, digestChars.length);
        } catch (Exception e) {
            Debug.logError(e, "Error while computing hash of type " + hashType, module);
        }
        return str;
    }
}
