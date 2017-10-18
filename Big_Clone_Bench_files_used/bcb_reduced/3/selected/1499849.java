package gnu.javax.net.ssl.provider;

import java.security.InvalidKeyException;
import java.util.HashMap;
import java.util.Map;
import gnu.java.security.hash.IMessageDigest;
import gnu.javax.crypto.mac.HMac;

/**
 * The operation of this HMac is identical to normal HMacs, but this one
 * allows keys with short lengths (including zero).
 */
class TLSHMac extends HMac {

    private static final byte IPAD_BYTE = 0x36;

    private static final byte OPAD_BYTE = 0x5C;

    TLSHMac(IMessageDigest hash) {
        super(hash);
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
}
