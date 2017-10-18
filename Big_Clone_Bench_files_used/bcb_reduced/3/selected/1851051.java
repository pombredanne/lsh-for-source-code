package net.wimpi.pim.util;

import java.net.InetAddress;

/**
 * Provides a UUID generator, that generates alphanumeric
 * identifiers with a random component (padding), a temporal
 * component (system time) and a spatial component
 * (IPv4 address bytes).
 * <p/>
 * For constant size, the combination of the components is hashed
 * with MD5.
 * <p/>
 * This implementation uses a highly optimized and strong
 * random number generator implementation (MersenneTwister).
 * Buffers are pooled and recycled, generation methods can
 * be used concurrently.
 *
 * @author Dieter Wimberger (wimpi)
 * @version @version@ (@date@)
 */
public final class UUIDGenerator {

    private static final RandomGenerator c_RndGen;

    private static final RandomSeedGenerator c_RndSeed;

    private static int c_ReseedCounter = 0;

    private static byte[] c_SpatialBytes;

    private static final int RANDOM_RESEED = 10000;

    private static final int UID_LENGTH = 156;

    private static final byte[] c_Buffer = new byte[UID_LENGTH];

    static {
        c_RndGen = new RandomGenerator();
        c_RndSeed = new RandomSeedGenerator();
        seedRandom();
        c_RndGen.nextBlock();
        try {
            c_SpatialBytes = InetAddress.getLocalHost().getAddress();
        } catch (Exception ex) {
            c_SpatialBytes = c_RndGen.nextBytes(4, 0, new byte[4]);
        }
    }

    private UUIDGenerator() {
    }

    /**
   * Returns a UUID (unique identifier) as <tt>String</tt>.
   * The returned string is a hex representation of a raw
   * UUID.
   *
   * @return a UUID as <tt>String</tt>.
   */
    public static final String getUID() {
        return new String(EncodingUtility.encodeHex(getRawUID()));
    }

    /**
   * Returns a UUID (unique identifier) as <tt>String</tt>.
   * The identifier represents the MD5 hashed combination
   * of a completely random padding, a temporal (system time)
   * and a spatial (IP address) component.
   *
   * @return a raw UUID as <tt>byte[]</tt>.
   */
    public static final synchronized byte[] getRawUID() {
        try {
            if (c_ReseedCounter == RANDOM_RESEED) {
                seedRandom();
                c_ReseedCounter = 0;
            } else {
                c_ReseedCounter++;
            }
            c_RndGen.nextBytes(UID_LENGTH, 0, c_Buffer);
            longToBytes(System.currentTimeMillis(), c_Buffer, 21);
            System.arraycopy(c_SpatialBytes, 0, c_Buffer, 52, c_SpatialBytes.length);
            return MD5.digest(c_Buffer);
        } catch (Exception ex) {
            ex.printStackTrace(System.err);
        }
        return null;
    }

    /**
   * Places a long value as bytes into a given buffer at
   * the given offset.
   *
   * @param v   the value to be converted.
   * @param buf the buffer to place the bytes.
   * @param off the offset where to start placing bytes from.
   */
    private static final void longToBytes(long v, byte[] buf, int off) {
        buf[off] = (byte) (0xff & (v >> 56));
        buf[off + 1] = (byte) (0xff & (v >> 48));
        buf[off + 2] = (byte) (0xff & (v >> 40));
        buf[off + 3] = (byte) (0xff & (v >> 32));
        buf[off + 4] = (byte) (0xff & (v >> 24));
        buf[off + 5] = (byte) (0xff & (v >> 16));
        buf[off + 6] = (byte) (0xff & (v >> 8));
        buf[off + 7] = (byte) (0xff & v);
    }

    public static final void seedRandom() {
        c_RndGen.setSeed(c_RndSeed.nextSeed());
    }

    public static final void main(String[] args) {
        Runnable r = new Runnable() {

            public void run() {
                String[] uids = new String[1000];
                for (int k = 0; k < 100; k++) {
                    int i = 0;
                    long start = System.currentTimeMillis();
                    while (i < 1000) {
                        uids[i] = getUID();
                        i++;
                    }
                    long stop = System.currentTimeMillis();
                    System.out.println(Thread.currentThread().getName() + ":: Time =" + (stop - start) + "[ms]");
                }
            }
        };
        for (int n = 0; n < 5; n++) {
            new Thread(r, "Thread" + n).start();
        }
        for (int k = 0; k < 10; k++) {
            System.out.println(getUID());
        }
    }
}
