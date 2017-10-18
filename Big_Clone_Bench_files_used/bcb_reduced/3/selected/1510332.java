package net.sf.jnclib.tp.ssh2.crai.hash;

import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>The implementation of the <i>HMAC</i> (Keyed-Hash Message Authentication
 * Code).</p>
 *
 * <p><i>HMAC</i> can be used in combination with any iterated cryptographic
 * hash function. <i>HMAC</i> also uses a <i>secret key</i> for calculation and
 * verification of the message authentication values. The main goals behind this
 * construction are</p>
 *
 * <ul>
 *    <li>To use, without modifications, available hash functions. In
 *    particular, hash functions that perform well in software, and for which
 *    code is freely and widely available.</li>
 *
 *    <li>To preserve the original performance of the hash function without
 *    incurring a significant degradation.</li>
 *
 *    <li>To use and handle keys in a simple way.</li>
 *
 *    <li>To have a well understood cryptographic analysis of the strength of
 *    the authentication mechanism based on reasonable assumptions on the
 *    underlying hash function.</li>
 *
 *    <li>To allow for easy replaceability of the underlying hash function in
 *    case that faster or more secure hash functions are found or required.</li>
 * </ul>
 *
 * <p>References:</p>
 *
 * <ol>
 *    <li><a href="http://www.ietf.org/rfc/rfc-2104.txt">RFC 2104</a>HMAC:
 *    Keyed-Hashing for Message Authentication.<br>
 *    H. Krawczyk, M. Bellare, and R. Canetti.</li>
 * </ol>
 *
 * @version $Revision: 1.10 $
 */
public class HMac extends BaseMac implements Cloneable {

    public static final String USE_WITH_PKCS5_V2 = "gnu.crypto.hmac.pkcs5";

    private static final byte IPAD_BYTE = 0x36;

    private static final byte OPAD_BYTE = 0x5C;

    /** caches the result of the correctness test, once executed. */
    private static Boolean valid;

    protected int macSize;

    protected int blockSize;

    protected IMessageDigest ipadHash;

    protected IMessageDigest opadHash;

    protected byte[] ipad;

    /**
    * <p>Trivial constructor for use by concrete subclasses.</p>
    *
    * @param underlyingHash the underlying hash algorithm instance.
    */
    public HMac(IMessageDigest underlyingHash) {
        super("HMAC-" + underlyingHash.name(), underlyingHash);
        this.blockSize = underlyingHash.blockSize();
        this.macSize = underlyingHash.hashSize();
        ipadHash = opadHash = null;
    }

    public void init(byte[] key) throws InvalidKeyException, IllegalStateException {
        HashMap attr = new HashMap();
        attr.put(MAC_KEY_MATERIAL, key);
        attr.put(USE_WITH_PKCS5_V2, Boolean.TRUE);
        init(attr);
    }

    public void init(Map attributes) throws InvalidKeyException, IllegalStateException {
        Integer ts = (Integer) attributes.get(TRUNCATED_SIZE);
        truncatedSize = (ts == null ? macSize : ts.intValue());
        if (truncatedSize < (macSize / 2)) {
            throw new IllegalArgumentException("Truncated size too small");
        } else if (truncatedSize < 10) {
            throw new IllegalArgumentException("Truncated size less than 80 bits");
        }
        byte[] K = (byte[]) attributes.get(MAC_KEY_MATERIAL);
        if (K == null) {
            if (ipadHash == null) {
                throw new InvalidKeyException("Null key");
            }
            underlyingHash = (IMessageDigest) ipadHash.clone();
            return;
        }
        Boolean pkcs5 = (Boolean) attributes.get(USE_WITH_PKCS5_V2);
        if (pkcs5 == null) {
            pkcs5 = Boolean.FALSE;
        }
        if (K.length < macSize && !pkcs5.booleanValue()) {
            throw new InvalidKeyException("Key too short");
        }
        if (K.length > blockSize) {
            underlyingHash.update(K, 0, K.length);
            K = underlyingHash.digest();
        }
        if (K.length < blockSize) {
            int limit = (K.length > blockSize) ? blockSize : K.length;
            byte[] newK = new byte[blockSize];
            System.arraycopy(K, 0, newK, 0, limit);
            K = newK;
        }
        underlyingHash.reset();
        opadHash = (IMessageDigest) underlyingHash.clone();
        if (ipad == null) {
            ipad = new byte[blockSize];
        }
        for (int i = 0; i < blockSize; i++) {
            ipad[i] = (byte) (K[i] ^ IPAD_BYTE);
        }
        for (int i = 0; i < blockSize; i++) {
            opadHash.update((byte) (K[i] ^ OPAD_BYTE));
        }
        underlyingHash.update(ipad, 0, blockSize);
        ipadHash = (IMessageDigest) underlyingHash.clone();
        K = null;
    }

    public void reset() {
        super.reset();
        if (ipad != null) {
            underlyingHash.update(ipad, 0, blockSize);
            ipadHash = (IMessageDigest) underlyingHash.clone();
        }
    }

    public byte[] digest() {
        if (ipadHash == null) {
            throw new IllegalStateException("HMAC not initialised");
        }
        byte[] out = underlyingHash.digest();
        underlyingHash = (IMessageDigest) opadHash.clone();
        underlyingHash.update(out, 0, macSize);
        out = underlyingHash.digest();
        if (truncatedSize == macSize) return out;
        byte[] result = new byte[truncatedSize];
        System.arraycopy(out, 0, result, 0, truncatedSize);
        return result;
    }
}
