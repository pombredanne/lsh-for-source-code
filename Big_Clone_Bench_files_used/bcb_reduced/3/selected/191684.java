package com.acuityph.commons.crypto;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * Md5 is a utility class that provides static utility methods for computing MD5
 * hashes.
 *
 * @author Alistair A. Israel
 * @since 0.4.1
 */
public final class Md5 {

    /**
     * The MD5 message digest name.
     */
    public static final String MD5 = "MD5";

    /**
     * The UTF-8 character set name.
     */
    public static final String UTF_8 = "UTF-8";

    /**
     * Use an 8KB buffer.
     */
    private static final int BUF_SIZE = 8 * 1024;

    /**
     * Utility classes should not have a public or default constructor.
     */
    private Md5() {
    }

    /**
     * @param bytes
     *        the bytes to digest
     * @return the MD5 hash of the given bytes
     * @throws GeneralSecurityException
     *         on exception
     */
    public static byte[] hash(final byte[] bytes) throws GeneralSecurityException {
        final MessageDigest digest = MessageDigest.getInstance(MD5);
        return digest.digest(bytes);
    }

    /**
     * @param s
     *        a String
     * @return the MD5 hash of the string's UTF-8 representation
     * @throws GeneralSecurityException
     *         on exception
     */
    public static byte[] hash(final String s) throws GeneralSecurityException {
        try {
            return hash(s.getBytes(UTF_8));
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
     * Compute the MD5 hash of a given {@link InputStream}. Will attempt to read
     * bytes until the end of the stream has been reached.
     *
     * @param is
     *        the InputStream to read bytes from
     * @return the MD5 digest
     * @throws GeneralSecurityException
     *         on exception
     * @throws IOException
     *         on exception
     */
    public static byte[] hash(final InputStream is) throws GeneralSecurityException, IOException {
        final MessageDigest digest = MessageDigest.getInstance(MD5);
        final byte[] buf = new byte[BUF_SIZE];
        int read = is.read(buf);
        while (read > 0) {
            digest.update(buf, 0, read);
            read = is.read(buf);
        }
        return digest.digest();
    }

    /**
     * Read a file and compute the MD5 hash of its contents.
     *
     * @param file
     *        the file to compute the MD5 hash for
     * @return the MD5 digest
     * @throws IOException
     *         on exception
     * @throws GeneralSecurityException
     *         on exception
     */
    public static byte[] hash(final File file) throws IOException, GeneralSecurityException {
        final FileInputStream is = new FileInputStream(file);
        try {
            return hash(is);
        } finally {
            is.close();
        }
    }
}
