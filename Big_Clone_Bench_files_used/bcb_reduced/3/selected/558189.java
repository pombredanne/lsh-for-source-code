package gnu.java.security.prng;

import gnu.java.security.Registry;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import java.util.Map;

/**
 * A simple pseudo-random number generator that relies on a hash algorithm, that
 * (a) starts its operation by hashing a <code>seed</code>, and then (b)
 * continuously re-hashing its output. If no hash algorithm name is specified in
 * the {@link Map} of attributes used to initialise the instance then the
 * SHA-160 algorithm is used as the underlying hash function. Also, if no
 * <code>seed</code> is given, an empty octet sequence is used.
 */
public class MDGenerator extends BasePRNG implements Cloneable {

    /** Property name of underlying hash algorithm for this generator. */
    public static final String MD_NAME = "gnu.crypto.prng.md.hash.name";

    /** Property name of seed material. */
    public static final String SEEED = "gnu.crypto.prng.md.seed";

    /** The underlying hash instance. */
    private IMessageDigest md;

    /** Trivial 0-arguments constructor. */
    public MDGenerator() {
        super(Registry.MD_PRNG);
    }

    public void setup(Map attributes) {
        String underlyingMD = (String) attributes.get(MD_NAME);
        if (underlyingMD == null) {
            if (md == null) {
                md = HashFactory.getInstance(Registry.SHA160_HASH);
            } else md.reset();
        } else md = HashFactory.getInstance(underlyingMD);
        byte[] seed = (byte[]) attributes.get(SEEED);
        if (seed == null) seed = new byte[0];
        md.update(seed, 0, seed.length);
    }

    public void fillBlock() throws LimitReachedException {
        IMessageDigest mdc = (IMessageDigest) md.clone();
        buffer = mdc.digest();
        md.update(buffer, 0, buffer.length);
    }

    public void addRandomByte(final byte b) {
        if (md == null) throw new IllegalStateException("not initialized");
        md.update(b);
    }

    public void addRandomBytes(final byte[] buf, final int off, final int len) {
        if (md == null) throw new IllegalStateException("not initialized");
        md.update(buf, off, len);
    }

    public Object clone() throws CloneNotSupportedException {
        MDGenerator result = (MDGenerator) super.clone();
        if (this.md != null) result.md = (IMessageDigest) this.md.clone();
        return result;
    }
}
