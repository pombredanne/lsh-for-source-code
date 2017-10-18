package com.modp.cida;

import java.security.MessageDigest;

/**
 * Provides a simple "entropy pool" where input data is constantly stired to
 * provide "random" data.
 * 
 * <p>
 * For more information, see Chapter 4, pages 150-151, in <i>Cryptography for
 * Internet and Database Applications</i>
 * 
 * @author Nick Galbreath
 * @version 1.0
 *  
 */
public class EntropyPool {

    protected static final String HASH_NAME = "SHA";

    protected static final int HASH_SIZE = 20;

    protected static final int POOL_SIZE = 512;

    protected byte[] pool = new byte[POOL_SIZE];

    protected int index = 0;

    protected int newbytes = 0;

    protected int carry = 0;

    protected MessageDigest md;

    /**
	 * Constructor.
	 * 
	 * @throws RuntimeException
	 *             if unable to create a MessageDigest instance
	 */
    public EntropyPool() {
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (Exception e) {
            throw new RuntimeException("Should never happen" + e);
        }
    }

    /**
	 * Add <code>byte[]</code> data to the pool.
	 * 
	 * @param input
	 *            The input byte array
	 */
    public void add(byte[] input) {
        for (int i = input.length - 1; i >= 0; --i) {
            int a = pool[(index + i) % POOL_SIZE] & 0xff;
            int b = input[i] & 0xff;
            int sum = a + b + carry;
            carry = sum >>> 8;
            pool[(index + i) % POOL_SIZE] = (byte) (sum & 0xff);
            index++;
            newbytes++;
        }
    }

    /**
	 * Add <code>int</code> to the pool.
	 * 
	 * @param input
	 *            The input data.
	 */
    public void add(int input) {
        add(ByteArray.intToByteArray(input));
    }

    /**
	 * Add <code>long</code> to the pool.
	 * 
	 * @param input
	 *            The input data.
	 */
    public void add(long input) {
        add(ByteArray.longToByteArray(input));
    }

    /**
	 * Get a seed value for use in the secure random.
	 * 
	 * @return a byte array the same size as the underlying message digest
	 *  
	 */
    public byte[] getSeed() {
        byte[] seed = md.digest(pool);
        add(seed);
        newbytes = 0;
        index = index % POOL_SIZE;
        return seed;
    }

    /**
	 * Retrieve number of bytes added to pool since last seeding.
	 */
    public int newPooledBytes() {
        return newbytes;
    }
}
