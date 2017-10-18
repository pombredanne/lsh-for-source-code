package gnu.java.security.provider;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandomSpi;
import java.util.Random;

public class SHA1PRNG extends SecureRandomSpi implements Serializable {

    MessageDigest digest;

    byte seed[];

    byte data[];

    int seedpos;

    int datapos;

    private boolean seeded = false;

    /**
   * The size of seed.
   */
    private static final int SEED_SIZE = 20;

    /**
   * The size of data.
   */
    private static final int DATA_SIZE = 40;

    /**
   * Create a new SHA-1 pseudo-random number generator.
   */
    public SHA1PRNG() {
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            throw new InternalError("no SHA implementation found");
        }
        seed = new byte[SEED_SIZE];
        seedpos = 0;
        data = new byte[DATA_SIZE];
        datapos = SEED_SIZE;
    }

    public void engineSetSeed(byte[] seed) {
        for (int i = 0; i < seed.length; i++) this.seed[seedpos++ % SEED_SIZE] ^= seed[i];
        seedpos %= SEED_SIZE;
    }

    public void engineNextBytes(byte[] bytes) {
        ensureIsSeeded();
        int loc = 0;
        while (loc < bytes.length) {
            int copy = Math.min(bytes.length - loc, SEED_SIZE - datapos);
            if (copy > 0) {
                System.arraycopy(data, datapos, bytes, loc, copy);
                datapos += copy;
                loc += copy;
            } else {
                System.arraycopy(seed, 0, data, SEED_SIZE, SEED_SIZE);
                byte[] digestdata = digest.digest(data);
                System.arraycopy(digestdata, 0, data, 0, SEED_SIZE);
                datapos = 0;
            }
        }
    }

    public byte[] engineGenerateSeed(int numBytes) {
        byte tmp[] = new byte[numBytes];
        engineNextBytes(tmp);
        return tmp;
    }

    private void ensureIsSeeded() {
        if (!seeded) {
            new Random(0L).nextBytes(seed);
            byte[] digestdata = digest.digest(data);
            System.arraycopy(digestdata, 0, data, 0, SEED_SIZE);
            seeded = true;
        }
    }
}
