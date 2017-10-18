package sun.security.provider;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandomSpi;
import java.security.NoSuchAlgorithmException;

/**
 * <p>This class provides a crytpographically strong pseudo-random number
 * generator based on the SHA-1 hash algorithm.
 *
 * <p>Note that if a seed is not provided, we attempt to provide sufficient
 * seed bytes to completely randomize the internal state of the generator
 * (20 bytes).  However, our seed generation algorithm has not been thoroughly
 * studied or widely deployed.
 *
 * <p>Also note that when a random object is deserialized,
 * <a href="#engineNextBytes(byte[])">engineNextBytes</a> invoked on the
 * restored random object will yield the exact same (random) bytes as the
 * original object.  If this behaviour is not desired, the restored random
 * object should be seeded, using
 * <a href="#engineSetSeed(byte[])">engineSetSeed</a>.
 *
 * @version 1.10, 02/02/00
 * @author Benjamin Renaud
 * @author Josh Bloch
 * @author Gadi Guy
 */
public final class SecureRandom extends SecureRandomSpi implements java.io.Serializable {

    /**
     * This static object will be seeded by SeedGenerator, and used
     * to seed future instances of SecureRandom
     */
    private static SecureRandom seeder;

    private static final int DIGEST_SIZE = 20;

    private transient MessageDigest digest;

    private byte[] state;

    private byte[] remainder;

    private int remCount;

    /**
     * This empty constructor automatically seeds the generator.  We attempt
     * to provide sufficient seed bytes to completely randomize the internal
     * state of the generator (20 bytes).  Note, however, that our seed
     * generation algorithm has not been thoroughly studied or widely deployed.
     *
     * <p>The first time this constructor is called in a given Virtual Machine,
     * it may take several seconds of CPU time to seed the generator, depending
     * on the underlying hardware.  Successive calls run quickly because they
     * rely on the same (internal) pseudo-random number generator for their
     * seed bits.
     */
    public SecureRandom() {
        init(null);
    }

    /**
     * This constructor is used to instatiate the private seeder object
     * with a given seed from the SeedGenerator.
     *
     * @param seed the seed.
     */
    private SecureRandom(byte seed[]) {
        init(seed);
    }

    /**
     * This call, used by the constructors, instantiates the SHA digest
     * and sets the seed, if given.
     */
    private void init(byte[] seed) {
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("internal error: SHA-1 not available.");
        }
        if (seed != null) {
            engineSetSeed(seed);
        }
    }

    /**
     * Returns the given number of seed bytes, computed using the seed
     * generation algorithm that this class uses to seed itself.  This
     * call may be used to seed other random number generators.  While
     * we attempt to return a "truly random" sequence of bytes, we do not
     * know exactly how random the bytes returned by this call are.  (See
     * the empty constructor <a href = "#SecureRandom">SecureRandom</a>
     * for a brief description of the underlying algorithm.)
     * The prudent user will err on the side of caution and get extra
     * seed bytes, although it should be noted that seed generation is
     * somewhat costly.
     *
     * @param numBytes the number of seed bytes to generate.
     *
     * @return the seed bytes.
     */
    public byte[] engineGenerateSeed(int numBytes) {
        byte[] b = new byte[numBytes];
        SeedGenerator.generateSeed(b);
        return b;
    }

    /**
     * Reseeds this random object. The given seed supplements, rather than
     * replaces, the existing seed. Thus, repeated calls are guaranteed
     * never to reduce randomness.
     *
     * @param seed the seed.
     */
    public synchronized void engineSetSeed(byte[] seed) {
        if (state != null) {
            digest.update(state);
            for (int i = 0; i < state.length; i++) state[i] = 0;
        }
        state = digest.digest(seed);
    }

    private static void updateState(byte[] state, byte[] output) {
        int last = 1;
        int v = 0;
        byte t = 0;
        boolean zf = false;
        for (int i = 0; i < state.length; i++) {
            v = (int) state[i] + (int) output[i] + last;
            t = (byte) v;
            zf = zf | (state[i] != t);
            state[i] = t;
            last = v >> 8;
        }
        if (!zf) state[0]++;
    }

    /**
     * Generates a user-specified number of random bytes.
     *
     * @param bytes the array to be filled in with random bytes.
     */
    public synchronized void engineNextBytes(byte[] result) {
        int index = 0;
        int todo;
        byte[] output = remainder;
        if (state == null) {
            if (seeder == null) {
                seeder = new SecureRandom(SeedGenerator.getSystemEntropy());
                seeder.engineSetSeed(engineGenerateSeed(DIGEST_SIZE));
            }
            byte[] seed = new byte[DIGEST_SIZE];
            seeder.engineNextBytes(seed);
            state = digest.digest(seed);
        }
        int r = remCount;
        if (r > 0) {
            todo = (result.length - index) < (DIGEST_SIZE - r) ? (result.length - index) : (DIGEST_SIZE - r);
            for (int i = 0; i < todo; i++) {
                result[i] = output[r];
                output[r++] = 0;
            }
            remCount += todo;
            index += todo;
        }
        while (index < result.length) {
            digest.update(state);
            output = digest.digest();
            updateState(state, output);
            todo = (result.length - index) > DIGEST_SIZE ? DIGEST_SIZE : result.length - index;
            for (int i = 0; i < todo; i++) {
                result[index++] = output[i];
                output[i] = 0;
            }
            remCount += todo;
        }
        remainder = output;
        remCount %= DIGEST_SIZE;
    }

    private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("internal error: SHA-1 not available.");
        }
    }
}
