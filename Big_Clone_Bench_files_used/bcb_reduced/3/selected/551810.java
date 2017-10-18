package org.gomba.utils.token;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * Random id generator. This code has been *stolen* and adapted from Tomcat 5
 * <code>org.apache.catalina.session.ManagerBase</code>. Thank you guys! :)
 * 
 * @author Craig R. McClanahan
 * @author Flavio Tordini
 * @version $Id: IdGenerator.java,v 1.1 2004/11/26 17:52:58 flaviotordini Exp $
 */
public class IdGenerator {

    /**
     * The session id length created by this class.
     */
    protected int idLength = 16;

    /**
     * A random number generator to use when generating session identifiers.
     */
    protected Random random = null;

    /**
     * A String initialization parameter used to increase the entropy of the
     * initialization of our random number generator.
     */
    protected String entropy = null;

    /**
     * The Java class name of the random number generator class to be used when
     * generating session identifiers.
     */
    protected String randomClass = "java.security.SecureRandom";

    /**
     * The default message digest algorithm to use if we cannot use the
     * requested one.
     */
    protected static final String DEFAULT_ALGORITHM = "MD5";

    /**
     * The message digest algorithm to be used when generating session
     * identifiers. This must be an algorithm supported by the
     * <code>java.security.MessageDigest</code> class on your platform.
     */
    protected String algorithm = DEFAULT_ALGORITHM;

    /**
     * Return the MessageDigest implementation to be used when creating session
     * identifiers.
     */
    protected MessageDigest digest = null;

    /**
     * Return the MessageDigest object to be used for calculating session
     * identifiers. If none has been created yet, initialize one the first time
     * this method is called.
     */
    public MessageDigest getDigest() {
        if (this.digest == null) {
            synchronized (this) {
                if (this.digest == null) {
                    try {
                        this.digest = MessageDigest.getInstance(this.algorithm);
                    } catch (NoSuchAlgorithmException e) {
                        try {
                            this.digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
                        } catch (NoSuchAlgorithmException f) {
                            this.digest = null;
                        }
                    }
                }
            }
        }
        return (this.digest);
    }

    /**
     * Return the entropy increaser value, or compute a semi-useful value if
     * this String has not yet been set.
     */
    public String getEntropy() {
        if (this.entropy == null) setEntropy(this.toString());
        return (this.entropy);
    }

    /**
     * Set the entropy increaser value.
     * 
     * @param entropy
     *                   The new entropy increaser value
     */
    public void setEntropy(String entropy) {
        this.entropy = entropy;
    }

    /**
     * Return the random number generator instance we should use for generating
     * session identifiers. If there is no such generator currently defined,
     * construct and seed a new one.
     */
    public Random getRandom() {
        if (this.random == null) {
            synchronized (this) {
                if (this.random == null) {
                    long seed = System.currentTimeMillis();
                    char entropy[] = getEntropy().toCharArray();
                    for (int i = 0; i < entropy.length; i++) {
                        long update = ((byte) entropy[i]) << ((i % 8) * 8);
                        seed ^= update;
                    }
                    try {
                        Class clazz = Class.forName(this.randomClass);
                        this.random = (Random) clazz.newInstance();
                        this.random.setSeed(seed);
                    } catch (Exception e) {
                        this.random = new java.util.Random();
                        this.random.setSeed(seed);
                    }
                }
            }
        }
        return (this.random);
    }

    protected void getRandomBytes(byte bytes[]) {
        getRandom().nextBytes(bytes);
    }

    /**
     * Generate and return a new identifier.
     */
    public String generateId() {
        byte randomBytes[] = new byte[16];
        StringBuffer buffer = new StringBuffer();
        int resultLenBytes = 0;
        while (resultLenBytes < this.idLength) {
            getRandomBytes(randomBytes);
            randomBytes = getDigest().digest(randomBytes);
            for (int j = 0; j < randomBytes.length && resultLenBytes < this.idLength; j++) {
                byte b1 = (byte) ((randomBytes[j] & 0xf0) >> 4);
                byte b2 = (byte) (randomBytes[j] & 0x0f);
                if (b1 < 10) buffer.append((char) ('0' + b1)); else buffer.append((char) ('A' + (b1 - 10)));
                if (b2 < 10) buffer.append((char) ('0' + b2)); else buffer.append((char) ('A' + (b2 - 10)));
                resultLenBytes++;
            }
        }
        return buffer.toString();
    }
}
