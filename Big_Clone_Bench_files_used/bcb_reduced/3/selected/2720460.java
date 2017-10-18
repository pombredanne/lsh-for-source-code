package com.phloc.commons.messagedigest;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.WillClose;
import javax.annotation.concurrent.NotThreadSafe;
import com.phloc.commons.annotations.Nonempty;
import com.phloc.commons.annotations.ReturnsMutableObject;
import com.phloc.commons.collections.ArrayHelper;
import com.phloc.commons.io.streams.StreamUtils;
import com.phloc.commons.string.ToStringGenerator;

/**
 * Base class for creating a cryptographic hash value. Don't mix it up with the
 * {@link com.phloc.commons.hash.HashCodeGenerator} which is used to generate
 * hash values for Java objects.
 * 
 * @author philip
 */
@NotThreadSafe
public final class NonBlockingMessageDigestGenerator extends AbstractMessageDigestGenerator {

    private final MessageDigest m_aMessageDigest;

    private byte[] m_aDigest;

    /**
   * Create a default hash generator with the default algorithm.
   */
    public NonBlockingMessageDigestGenerator() {
        this(DEFAULT_ALGORITHM);
    }

    /**
   * Create a hash generator with a set of possible algorithms to use.
   * 
   * @param aAlgorithms
   *        The parameters to test. May not be <code>null</code>.
   * @throws NullPointerException
   *         If the array of algorithms is <code>null</code> or if one element
   *         of the array is <code>null</code>.
   * @throws IllegalArgumentException
   *         If no algorithm was passed or if no applicable algorithm was used.
   */
    public NonBlockingMessageDigestGenerator(@Nonnull @Nonempty final EMessageDigestAlgorithm... aAlgorithms) {
        if (aAlgorithms == null) throw new NullPointerException("algorithms");
        MessageDigest aMessageDigest = null;
        for (final EMessageDigestAlgorithm eMD : aAlgorithms) try {
            aMessageDigest = MessageDigest.getInstance(eMD.getAlgorithm());
            break;
        } catch (final NoSuchAlgorithmException ex) {
        }
        if (aMessageDigest == null) {
            throw new IllegalArgumentException("None of the algorithms in " + Arrays.toString(aAlgorithms) + " was applicable!");
        }
        m_aMessageDigest = aMessageDigest;
    }

    @Nonnull
    public String getAlgorithmName() {
        return m_aMessageDigest.getAlgorithm();
    }

    @Nonnegative
    public int getDigestLength() {
        return m_aMessageDigest.getDigestLength();
    }

    @Nonnull
    public IMessageDigestGenerator update(final byte aValue) {
        if (m_aDigest != null) throw new IllegalStateException("The hash has already been finished. Call reset manually!");
        m_aMessageDigest.update(aValue);
        return this;
    }

    @Nonnull
    public IMessageDigestGenerator update(@Nonnull final byte[] aValue, @Nonnegative final int nOffset, @Nonnegative final int nLength) {
        if (aValue == null) throw new NullPointerException("byteArray");
        if (m_aDigest != null) throw new IllegalStateException("The hash has already been finished. Call reset manually!");
        m_aMessageDigest.update(aValue, nOffset, nLength);
        return this;
    }

    public void reset() {
        m_aMessageDigest.reset();
        m_aDigest = null;
    }

    @Nonnull
    @ReturnsMutableObject(reason = "design")
    private byte[] _getDigest() {
        if (m_aDigest == null) m_aDigest = m_aMessageDigest.digest();
        return m_aDigest;
    }

    @Nonnull
    public byte[] getDigest() {
        return ArrayHelper.getCopy(_getDigest());
    }

    @Nonnull
    public byte[] getDigest(@Nonnegative final int nLength) {
        return ArrayHelper.getCopy(_getDigest(), 0, nLength);
    }

    @Override
    public String toString() {
        return new ToStringGenerator(this).append("messageDigest", m_aMessageDigest).appendIfNotNull("digest", m_aDigest).toString();
    }

    /**
   * Create a hash value from the complete input stream.
   * 
   * @param aIS
   *        The input stream to create the hash value from. May not be
   *        <code>null</code>.
   * @param aAlgorithms
   *        The list of algorithms to choose the first one from. May neither be
   *        <code>null</code> nor empty.
   * @return The non-<code>null</code> message digest byte array
   */
    @Nonnull
    public static byte[] getDigestFromInputStream(@Nonnull @WillClose final InputStream aIS, @Nonnull @Nonempty final EMessageDigestAlgorithm... aAlgorithms) {
        if (aIS == null) throw new NullPointerException("inputStream");
        final NonBlockingMessageDigestGenerator aMDGen = new NonBlockingMessageDigestGenerator(aAlgorithms);
        final byte[] aBuf = new byte[2048];
        try {
            int nBytesRead;
            while ((nBytesRead = aIS.read(aBuf)) > -1) aMDGen.update(aBuf, 0, nBytesRead);
            return aMDGen.getDigest();
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed to read from InputStream for hashing!", ex);
        } finally {
            StreamUtils.close(aIS);
        }
    }
}
