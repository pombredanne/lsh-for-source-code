package net.jxta.impl.util;

import java.util.*;
import java.io.File;
import java.math.BigInteger;
import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;
import java.util.logging.Level;
import net.jxta.logging.Logging;

/**
 *  A message digest wrapper to provide hashing using java.security.MesssageDigest
 */
public class JxtaHash {

    private static final Logger LOG = Logger.getLogger(JxtaHash.class.getName());

    public static final String SHA = "SHA";

    public static final String SHA1 = "SHA1";

    public static final String MD2 = "MD2";

    public static final String MD5 = "MD5";

    public static final String DSA = "DSA";

    public static final String RSA = "RSA";

    public static final String SHA1withDSA = "SHA1WITHDSA";

    private MessageDigest dig = null;

    /**
     * Default JxtaHash constructor, with the default algorithm SHA1
     *
     */
    public JxtaHash() {
        try {
            dig = MessageDigest.getInstance(SHA1);
        } catch (NoSuchAlgorithmException ex) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(ex.toString());
            }
        }
    }

    /**
     * Default JxtaHash constructor, with the default algorithm SHA1
     *
     * @param  expression  message to hash
     */
    public JxtaHash(String expression) {
        this(SHA1, expression);
    }

    /**
     * Constructor for the JxtaHash object
     *
     * @deprecated This implementation may produce inconsistent results
     * based upon varience of the locale. (The locale of getBytes() is
     * not defined).
     *
     * @param  algorithm   algorithm - the name of the algorithm requested
     * @param  expression  expression to digest
     */
    @Deprecated
    public JxtaHash(String algorithm, String expression) {
        this(algorithm, expression.getBytes());
    }

    /**
     * Constructor for the JxtaHash object
     *
     * @param  algorithm   algorithm - the name of the algorithm requested
     * @param  expression  expression to digest
     */
    public JxtaHash(String algorithm, byte[] expression) {
        try {
            dig = MessageDigest.getInstance(algorithm);
            if (expression != null) {
                dig.update(expression);
            }
        } catch (NoSuchAlgorithmException ex) {
            if (Logging.SHOW_FINE && LOG.isLoggable(Level.FINE)) {
                LOG.fine(ex.toString());
            }
        }
    }

    /**
     * Constructor for the JxtaHash object
     *
     * @param  expression  expression to digest
     */
    public void update(String expression) {
        if (expression != null) {
            dig.update(expression.getBytes());
        }
    }

    /**
     *  Gets the digest as digestInteger
     *
     * @return    The digestInteger value
     */
    public BigInteger getDigestInteger() {
        return new BigInteger(dig.digest());
    }

    /**
     *  Gets the digest as digestInteger
     *
     * @param  expression  expression to digest
     * @return             The digestInteger value
     */
    public BigInteger getDigestInteger(byte[] expression) {
        dig.reset();
        dig.update(expression);
        return new BigInteger(dig.digest());
    }

    /**
     *  Gets the digest as digestInteger
     *
     * @param  expression  expression to digest
     * @return             The digestInteger value
     */
    public BigInteger getDigestInteger(String expression) {
        return getDigestInteger(expression.getBytes());
    }

    /**
     *   Returns a int whose value is (getDigestInteger mod m).
     *
     * @param  m  the modulus.
     * @return    (getDigestInteger mod m).
     */
    public int mod(long m) {
        BigInteger bi = getDigestInteger();
        BigInteger mod = new BigInteger(longToBytes(m));
        BigInteger result = bi.mod(mod);
        return result.intValue();
    }

    /**
     *  convert a long into the byte array
     *
     * @param  value  long value to convert
     * @return        byte array
     */
    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int eachByte = 0; eachByte < 8; eachByte++) {
            bytes[eachByte] = (byte) (value >> ((7 - eachByte) * 8L));
        }
        return bytes;
    }
}
