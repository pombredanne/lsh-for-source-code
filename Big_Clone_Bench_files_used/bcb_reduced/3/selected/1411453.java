package gnu.javax.net.ssl.provider;

import java.util.Arrays;
import java.util.Map;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.javax.crypto.mac.IMac;

/**
 * The MAC function in SSLv3. This mac is defined as:
 *
 * <pre>
 * hash(MAC_write_secret, pad_2 +
 *      hash(MAC_write_secret + pad_1 + data));</pre>
 *
 * <p><tt>hash</tt> is e.g. MD5 or SHA-1, <tt>pad_1</tt> is the value
 * 0x36 48 times for MD5 and 40 times for SHA-1, and <tt>pad_2</tt> is
 * the value 0x5c repeated similarly.
 */
class SSLHMac implements IMac, Cloneable {

    static final byte PAD1 = 0x36;

    static final byte PAD2 = 0x5c;

    protected IMessageDigest md;

    protected byte[] key;

    protected final byte[] pad1, pad2;

    SSLHMac(String mdName) {
        super();
        this.md = HashFactory.getInstance(mdName);
        if (mdName.equalsIgnoreCase("MD5")) {
            pad1 = new byte[48];
            pad2 = new byte[48];
        } else {
            pad1 = new byte[40];
            pad2 = new byte[40];
        }
        Arrays.fill(pad1, PAD1);
        Arrays.fill(pad2, PAD2);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException cnse) {
            throw new Error();
        }
    }

    public String name() {
        return "SSLHMac-" + md.name();
    }

    public int macSize() {
        return md.hashSize();
    }

    public void init(Map attributes) {
        key = (byte[]) attributes.get(MAC_KEY_MATERIAL);
        if (key == null) throw new NullPointerException();
        reset();
    }

    public void reset() {
        md.reset();
        md.update(key, 0, key.length);
        md.update(pad1, 0, pad1.length);
    }

    public byte[] digest() {
        byte[] h1 = md.digest();
        md.update(key, 0, key.length);
        md.update(pad2, 0, pad2.length);
        md.update(h1, 0, h1.length);
        byte[] result = md.digest();
        reset();
        return result;
    }

    public void update(byte b) {
        md.update(b);
    }

    public void update(byte[] buf, int off, int len) {
        md.update(buf, off, len);
    }

    public boolean selfTest() {
        return true;
    }
}
