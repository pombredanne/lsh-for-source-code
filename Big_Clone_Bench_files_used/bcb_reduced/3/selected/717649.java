package gnu.javax.net.ssl.provider;

import java.util.Map;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.prng.IRandom;
import gnu.java.security.prng.LimitReachedException;

class SSLRandom implements IRandom {

    static final String SECRET = "jessie.sslprng.secret";

    static final String SEED = "jessie.sslprng.seed";

    private final IMessageDigest md5, sha;

    private byte[] secret;

    private byte[] buffer;

    private byte pad;

    private byte[] seed;

    private int idx;

    SSLRandom() {
        md5 = HashFactory.getInstance("MD5");
        sha = HashFactory.getInstance("SHA-1");
    }

    public void init(Map attrib) {
        secret = (byte[]) attrib.get(SECRET);
        seed = (byte[]) attrib.get(SEED);
        if (secret == null || seed == null) throw new NullPointerException();
        pad = (byte) 'A';
        try {
            buffer = nextBlock();
        } catch (LimitReachedException cantHappen) {
        }
    }

    public String name() {
        return "SSLRandom";
    }

    public Object clone() {
        throw new UnsupportedOperationException();
    }

    public byte nextByte() throws LimitReachedException {
        if (buffer == null) throw new IllegalStateException();
        if (idx >= buffer.length) buffer = nextBlock();
        return buffer[idx++];
    }

    public void nextBytes(byte[] buf, int off, int len) throws LimitReachedException {
        if (buffer == null) throw new IllegalStateException();
        if (buf == null) throw new NullPointerException();
        if (off < 0 || len < 0 || off + len > buf.length) throw new IndexOutOfBoundsException();
        int count = 0;
        while (count < len) {
            if (idx >= buffer.length) buffer = nextBlock();
            int l = Math.min(buffer.length - idx, len - count);
            System.arraycopy(buffer, idx, buf, off + count, l);
            count += l;
            idx += l;
        }
    }

    public boolean selfTest() {
        return true;
    }

    public void addRandomByte(byte b) {
    }

    public void addRandomBytes(byte[] buffer) {
        addRandomBytes(buffer, 0, buffer.length);
    }

    public void addRandomBytes(byte[] b, int i, int j) {
    }

    private byte[] nextBlock() throws LimitReachedException {
        int count = pad - 'A' + 1;
        if (count > 26) throw new LimitReachedException();
        for (int i = 0; i < count; i++) sha.update(pad);
        sha.update(secret, 0, secret.length);
        sha.update(seed, 0, seed.length);
        byte[] b = sha.digest();
        md5.update(secret, 0, secret.length);
        md5.update(b, 0, b.length);
        idx = 0;
        pad++;
        return md5.digest();
    }
}
