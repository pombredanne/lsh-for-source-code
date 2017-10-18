package org.jpxx.commons.util;

import java.security.MessageDigest;

/**
 * <p>
 * The algorithm takes as input a message of arbitrary length and produces as
 * output a 128-bit "fingerprint" or "message digest" of the input. It is
 * conjectured that it is computationally infeasible to produce two messages
 * having the same message digest, or to produce any message having a given
 * prespecified target message digest. The MD5 algorithm is intended for digital
 * signature applications, where a large file must be "compressed" in a secure
 * manner before being encrypted with a private (secret) key under a public-key
 * cryptosystem such as RSA.
 * </p>
 * 
 * <p>
 * The MD5 algorithm is designed to be quite fast on 32-bit machines. In
 * addition, the MD5 algorithm does not require any large substitution tables;
 * the algorithm can be coded quite compactly.
 * </p>
 * 
 * <p>
 * The MD5 algorithm is an extension of the MD4 message-digest algorithm 1,2].
 * MD5 is slightly slower than MD4, but is more "conservative" in design. MD5
 * was designed because it was felt that MD4 was perhaps being adopted for use
 * more quickly than justified by the existing critical review; because MD4 was
 * designed to be exceptionally fast, it is "at the edge" in terms of risking
 * successful cryptanalytic attack. MD5 backs off a bit, giving up a little in
 * speed for a much greater likelihood of ultimate security. It incorporates
 * some suggestions made by various reviewers, and contains additional
 * optimizations. The MD5 algorithm is being placed in the public domain for
 * review and possible adoption as a standard.
 * </p>
 * 
 * @author Jun Li lijun@jpxx.org (http://www.jpxx.org)
 * @version 1.0.0 $ org.jpxx.mail.util.security.MD5.java $, $Date: 2009-4-29 $
 * 
 */
public class MD5 {

    /**
     * Encode String with MD5
     * 
     * @param s
     *            String.
     * @return Encoded String
     */
    public static final String encode(String s) {
        char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            byte[] strTemp = s.getBytes();
            MessageDigest mdTemp = MessageDigest.getInstance("MD5");
            mdTemp.update(strTemp);
            byte[] md = mdTemp.digest();
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (int i = 0; i < j; i++) {
                byte byte0 = md[i];
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (Exception e) {
            return null;
        }
    }
}
