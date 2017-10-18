package com.sun.midp.crypto;

/**
 * Implements a pseudo random number generator.
 */
final class PRand extends SecureRandom {

    /** Local handle to message digest. */
    private static MessageDigest md = null;

    /**
     * For an arbitrary choice of the default seed, we use bits from the 
     * binary expansion of pi.
     * <p>
     * This seed is just an example implementation and NOT
     * considered for used in SECURE (unpredicable) protocols, for this class
     * to be considered a secure source of random data the seed MUST
     * be derived from unpredicatable data in a production
     * device at the native level.
     * (see IETF RFC 1750, Randomness Recommendations for Security,
     *  http://www.ietf.org/rfc/rfc1750.txt)
     */
    private static byte[] seed = { (byte) 0xC9, (byte) 0x0F, (byte) 0xDA, (byte) 0xA2, (byte) 0x21, (byte) 0x68, (byte) 0xC2, (byte) 0x34, (byte) 0xC4, (byte) 0xC6, (byte) 0x62, (byte) 0x8B, (byte) 0x80, (byte) 0xDC, (byte) 0x1C, (byte) 0xD1 };

    /** buffer of random bytes */
    private static byte[] randomBytes;

    /** number of random bytes currently available */
    private static int bytesAvailable = 0;

    /** Constructor for random data. */
    public PRand() {
        if (md != null) return;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new RuntimeException("MD5 missing");
        }
        randomBytes = new byte[seed.length];
        updateSeed();
    }

    /**
     * This does a reasonable job of producing unpredictable
     * random data by using a one way hash as a mixing function and
     * the current time in milliseconds as a source of entropy.
     * @param b buffer of input data
     * @param off offset into the provided buffer
     * @param len length of the data to be processed
     */
    public void nextBytes(byte[] b, int off, int len) {
        synchronized (md) {
            int i = 0;
            while (true) {
                if (bytesAvailable == 0) {
                    md.update(seed, 0, seed.length);
                    try {
                        md.digest(randomBytes, 0, randomBytes.length);
                    } catch (DigestException de) {
                    }
                    updateSeed();
                    bytesAvailable = randomBytes.length;
                }
                while (bytesAvailable > 0) {
                    if (i == len) return;
                    b[off + i] = randomBytes[--bytesAvailable];
                    i++;
                }
            }
        }
    }

    /**
     * Set the random number seed.
     * @param b initial data to use as the seed 
     * @param off offset into the provided buffer
     * @param len length of the data to be used
     */
    public void setSeed(byte[] b, int off, int len) {
        int j = 0;
        if ((len <= 0) || (b.length < (off + len))) return;
        for (int i = 0; i < seed.length; i++, j++) {
            if (j == len) j = 0;
            seed[i] = b[off + j];
        }
    }

    /**
     * This does a reasonable job of producing unpredictable
     * random data by using a one way hash as a mixing function and
     * the current time in milliseconds as a source of entropy for the seed.
     * This method assumes the original seed data is unpredicatble.
     */
    private void updateSeed() {
        long t = System.currentTimeMillis();
        byte[] tmp = new byte[8];
        for (int i = 0; i < 8; i++) {
            tmp[i] = (byte) (t & 0xff);
            t = (t >>> 8);
        }
        md.update(seed, 0, seed.length);
        md.update(tmp, 0, tmp.length);
        try {
            md.digest(seed, 0, seed.length);
        } catch (DigestException de) {
        }
    }
}
