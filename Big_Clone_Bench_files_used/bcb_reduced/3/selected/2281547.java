package gov.nist.atlas.util;

import gov.nist.atlas.io.ATLASIOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created May 16, 2003 3:02:32 PM.
 * @author <a href='mailto:ubik@users.sf.net'>Chris Laprun</a>
 * @version $Revision: 1.1 $
 * @since 2.0 Beta 6
 */
public class DigestUtil {

    public static byte[] digest(byte[] bytes) {
        return MESSAGE_DIGEST.digest(bytes);
    }

    public static byte[] digest(String resourceURI) throws ATLASIOException {
        URL url = null;
        try {
            url = new URI(resourceURI).toURL();
        } catch (Exception e) {
            throw new ATLASIOException("DigestUtil couldn't digest resource at: " + resourceURI + ". The following error was encountered: " + e.getLocalizedMessage(), e);
        }
        return digest(url);
    }

    public static byte[] digest(URL resource) throws ATLASIOException {
        try {
            byte[] buffer = new byte[8192];
            InputStream is = new BufferedInputStream(resource.openStream(), 8192);
            int lenght = 0;
            while ((lenght = is.read(buffer)) != -1) MESSAGE_DIGEST.update(buffer, 0, lenght);
            is.close();
            return MESSAGE_DIGEST.digest();
        } catch (Exception e) {
            throw new ATLASIOException("DigestUtil couldn't digest resource at: " + resource.toExternalForm() + ". The following error was encountered: " + e.getLocalizedMessage(), e);
        }
    }

    /**
   * Creates a String representing each byte of the specified byte array as an unsigned hexadecimal number.
   *
   * @param bytes the array of bytes to convert to an hexadecimal String
   * @return the hexadecimal representation of the specified byte array.
   */
    public static String asHexString(byte[] bytes) {
        if (bytes == null) return "";
        int l = bytes.length;
        char[] out = new char[l << 1];
        int b;
        for (int i = 0, j = 0; i < l; i++) {
            b = bytes[i] & 0xFF;
            out[j++] = HEX_CHARS[b >>> 4];
            out[j++] = HEX_CHARS[b & 0x0F];
        }
        return new String(out);
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        try {
            MESSAGE_DIGEST = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Couldn't get MessageDigest instance.", e);
        }
    }
}
