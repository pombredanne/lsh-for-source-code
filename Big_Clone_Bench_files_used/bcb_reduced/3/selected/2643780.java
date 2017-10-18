package hu.ihash.hashing.methods;

import org.apache.log4j.Logger;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Various common hashing utils.
 *
 * @author Gergely Kiss
 */
public abstract class HashUtils {

    private static final Logger log = Logger.getLogger(HashUtils.class);

    public static byte[] md5(InputStream is) throws IOException {
        return hash(is, "MD5");
    }

    public static byte[] sha256(InputStream is) throws IOException {
        return hash(is, "SHA-256");
    }

    public static byte[] md5(byte[] bytes) {
        try {
            return md5(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    public static byte[] sha256(byte[] bytes) {
        try {
            return sha256(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Digests the given input stream's contents with <code>method</code>, then closes the stream.
     *
     * @param is
     * @param method
     * @return
     * @throws IOException
     */
    private static final byte[] hash(InputStream is, String method) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance(method);
            int num;
            byte[] buffer = new byte[1024];
            while ((num = is.read(buffer)) > 0) {
                md.update(buffer, 0, num);
            }
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            log.fatal("Hashing failed", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
            }
        }
        return null;
    }
}
