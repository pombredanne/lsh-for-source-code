package gnu.javax.crypto.sasl.crammd5;

import gnu.java.security.Registry;
import gnu.java.security.util.Util;
import gnu.javax.crypto.mac.HMacFactory;
import gnu.javax.crypto.mac.IMac;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.util.HashMap;
import javax.security.sasl.SaslException;

/**
 * A package-private CRAM-MD5-specific utility class.
 */
class CramMD5Util {

    private CramMD5Util() {
        super();
    }

    static byte[] createMsgID() throws SaslException {
        final String encoded;
        try {
            encoded = Util.toBase64(Thread.currentThread().getName().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException x) {
            throw new SaslException("createMsgID()", x);
        }
        String hostname = "localhost";
        try {
            hostname = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ignored) {
        }
        final byte[] result;
        try {
            result = new StringBuffer("<").append(encoded.substring(0, encoded.length())).append(".").append(String.valueOf(System.currentTimeMillis())).append("@").append(hostname).append(">").toString().getBytes("UTF-8");
        } catch (UnsupportedEncodingException x) {
            throw new SaslException("createMsgID()", x);
        }
        return result;
    }

    static byte[] createHMac(final char[] passwd, final byte[] data) throws InvalidKeyException, SaslException {
        final IMac mac = HMacFactory.getInstance(Registry.HMAC_NAME_PREFIX + Registry.MD5_HASH);
        final HashMap map = new HashMap();
        final byte[] km;
        try {
            km = new String(passwd).getBytes("UTF-8");
        } catch (UnsupportedEncodingException x) {
            throw new SaslException("createHMac()", x);
        }
        map.put(IMac.MAC_KEY_MATERIAL, km);
        mac.init(map);
        mac.update(data, 0, data.length);
        return mac.digest();
    }
}
