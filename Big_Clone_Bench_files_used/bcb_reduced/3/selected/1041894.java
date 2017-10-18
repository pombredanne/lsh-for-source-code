package com.chungco.rest.evdb;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EvdbUtils {

    private EvdbUtils() {
    }

    public static byte[] md5(byte[] buffer) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer);
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static final String hex(byte[] bytes) {
        final StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            int val = ((int) b) & 0xff;
            final String hex = Integer.toHexString(val);
            if (val < 16) sb.append("0");
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * From <a href="http://api.evdb.com/docs/auth/">Evdb documentation</a>.
     * The digest response is a hex representation of the MD5 digest of nonce
     * and the MD5-encoded password: lowercase( MD5(nonce + ':' +
     * lowercase(MD5(password))). Note that BOTH MD5 encodings need to return a
     * lowercase representation of the hex string. For example, in Perl the code
     * to generate an appropriate response would be: <code>
     *   use Digest::MD5 qw(md5_hex); 
     *   $response = lc( md5_hex( $nonce . ':' . lc(md5_hex($password) ) ) );
     * </code>
     * 
     * @param pNonceStr
     * @param pPasswordStr
     * @return
     * @throws UnsupportedEncodingException
     */
    public static String digest(final String pNonceStr, final String pPasswordStr) throws UnsupportedEncodingException {
        final byte[] pass = pPasswordStr.getBytes();
        final byte[] nonce = pNonceStr.getBytes();
        final byte[] colon = ":".getBytes();
        final byte[] md5pass = hex(md5(pass)).toLowerCase().getBytes();
        final byte[] out = new byte[nonce.length + colon.length + md5pass.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(colon, 0, out, nonce.length, colon.length);
        System.arraycopy(md5pass, 0, out, nonce.length + colon.length, md5pass.length);
        final String response = hex(md5(out)).toLowerCase();
        return response;
    }
}
