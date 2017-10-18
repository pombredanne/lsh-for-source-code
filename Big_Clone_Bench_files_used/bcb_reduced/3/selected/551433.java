package com.kapil.framework.crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang.exception.ExceptionUtils;
import com.kapil.framework.lang.StringUtils;
import com.kapil.framework.logger.ILogger;
import com.kapil.framework.logger.LogFactory;

/**
 * <p>
 * Computes a text digest using the <code>Secure Hash Algorithm (SHA-1)</code>. <code>SHA-1</code> produces a 160-bit
 * one-way digest of the text. The digest is said to be one-way since the original text cannot be recovered from it.
 * </p>
 * <p>
 * <code>SHA-1</code> is said to be crypto-analytically secure since it is not only impossible to retrieve the original
 * text from its digest but also because there is a very small probability that two totally dissimilar text messages
 * will produce the same digest value.
 * </p>
 */
public final class SecureHashDigester implements IDigester {

    private static final ILogger LOGGER = LogFactory.getInstance().getLogger(SecureHashDigester.class);

    /**
     * <p>
     * Computes an <code>SHA-1</code> digest of a text message.
     * </p>
     * 
     * <p>
     * The returned digest has a prefix of <code>{SHA}</code> indicating that it is a Secure Hash digest.
     * </p>
     *
     * @param text A {@link java.lang.String} containing the text to be digested.
     * @return A printable {@link java.lang.String} digest.
     */
    public String digest(String text) {
        return addPrefix(getRawDigest(text));
    }

    /**
     * Adds prefix <code>{SHA}</code> to a digest if it is not blank.
     *
     * @param digest An array of bytes containing the digest value.
     * @return {@link java.lang.String} containing digest in Base-64 encoded form, with the algorith name prefixed.
     */
    private String addPrefix(byte[] digest) {
        return digest == null ? null : "{SHA}" + Base64Util.encode(digest);
    }

    /**
     * Computes SHA-1 digest for a text message.
     *
     * @param text {@link java.lang.String} text.
     * @return An array of bytes containing the digest.
     */
    public byte[] getRawDigest(String text) {
        byte[] digest = null;
        if (StringUtils.isNotBlank(text)) {
            try {
                MessageDigest messageDigester = MessageDigest.getInstance("SHA");
                digest = messageDigester.digest(text.getBytes());
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
        return digest;
    }
}
