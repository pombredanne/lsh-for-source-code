package gov.lanl.util;

import java.io.InputStream;
import java.security.MessageDigest;

public class DigestUtil {

    /**
     * Creates a base32-encoded digest from a byte array
     * @param bytes
     *         byte array to generate digest for
     * @param digestType
     *         type of digest to used (e.g. sha1)
     * @return
     *         base32-encoded digest of the specified type
     * @throws Exception
     */
    public static String createBase32Digest(byte[] bytes, String digestType) throws Exception {
        return new String(Base32.encode(createDigest(bytes, digestType)));
    }

    /**
     * Creates a base32-encoded digest from an InputStream
     * @param is
     *         InputStream to generate digest for
     * @param digestType
     *         type of digest to used (e.g. sha1)
     * @return
     *         base32-encoded digest of the specified type
     * @throws Exception
     */
    public static String createBase32Digest(InputStream is, String digestType) throws Exception {
        return new String(Base32.encode(createDigest(is, digestType)));
    }

    /**
     * Creates a digest from a byte[]
     * @param bytes
     *         byte array to generate digest for
     * @param digestType
     *         type of digest to used (e.g. sha1)
     * @return
     *         unencoded digest of the specified type
     * @throws Exception
     */
    public static byte[] createDigest(byte[] bytes, String digestType) throws Exception {
        MessageDigest complete = MessageDigest.getInstance(digestType);
        return complete.digest(bytes);
    }

    /**
     * Creates a digest from an InputStream
     * @param is
     *         InputStream to generate digest for
     * @param digestType
     *         type of digest to used (e.g. sha1)
     * @return
     *         unencoded digest of the specified type
     * @throws Exception
     */
    public static byte[] createDigest(InputStream is, String digestType) throws Exception {
        byte[] buffer = new byte[1024 * 10];
        MessageDigest complete = MessageDigest.getInstance(digestType);
        int n;
        do {
            n = is.read(buffer);
            if (n > 0) {
                complete.update(buffer, 0, n);
            }
        } while (n != -1);
        is.close();
        return complete.digest();
    }

    /**
     * Creates a base32-encoded sha1 digest from an InputStream
     * @param is
     *         InputStream to generate digest�for
     * @return
     *         a base32-encode sha1 digest value
     * @throws Exception
     */
    public static String getSHA1Digest(InputStream is) throws Exception {
        return createBase32Digest(is, "SHA1");
    }

    /**
     * Creates a base32-encoded sha1 digest from a byte array
     * @param bytes
     *         byte array to generate digest for
     * @return
     *         a base32-encode sha1 digest value
     * @throws Exception
     */
    public static String getSHA1Digest(byte[] bytes) throws Exception {
        return createBase32Digest(bytes, "SHA1");
    }

    /**
     * Formats a digest and digest type as a URN
     * 
     * @param digest
     *            formatted digest value
     * @param digestType
     *            digest type
     * @return formatted digest URN
     */
    public static String toURN(String digest, String digestType) {
        return "urn:" + digestType.toLowerCase() + ":" + digest;
    }
}
