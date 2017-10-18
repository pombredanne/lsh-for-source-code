package gnu.javax.crypto.sasl.srp;

import gnu.javax.crypto.mac.IMac;
import gnu.javax.crypto.mac.MacFactory;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import javax.security.sasl.SaslException;

/**
 * A Factory class that returns IALG (Integrity Algorithm) instances that
 * operate as described in the draft-burdis-cat-sasl-srp-04 and later.
 */
public final class IALG implements Cloneable {

    private IMac hmac;

    /** Private constructor to enforce instantiation through Factory method. */
    private IALG(final IMac hmac) {
        super();
        this.hmac = hmac;
    }

    /**
   * Returns an instance of a SASL-SRP IALG implementation.
   * 
   * @param algorithm the name of the HMAC algorithm.
   * @return an instance of this object.
   */
    static synchronized IALG getInstance(final String algorithm) throws SaslException {
        final IMac hmac;
        hmac = MacFactory.getInstance(algorithm);
        if (hmac == null) throw new SaslException("getInstance()", new NoSuchAlgorithmException(algorithm));
        return new IALG(hmac);
    }

    public Object clone() throws CloneNotSupportedException {
        return new IALG((IMac) hmac.clone());
    }

    public void init(final KDF kdf) throws SaslException {
        try {
            final byte[] sk = kdf.derive(hmac.macSize());
            final HashMap map = new HashMap();
            map.put(IMac.MAC_KEY_MATERIAL, sk);
            hmac.init(map);
        } catch (InvalidKeyException x) {
            throw new SaslException("getInstance()", x);
        }
    }

    public void update(final byte[] data) {
        hmac.update(data, 0, data.length);
    }

    public void update(final byte[] data, final int offset, final int length) {
        hmac.update(data, offset, length);
    }

    public byte[] doFinal() {
        return hmac.digest();
    }

    /**
   * Returns the length (in bytes) of this SASL SRP Integrity Algorithm.
   * 
   * @return the length, in bytes, of this integrity protection algorithm.
   */
    public int length() {
        return hmac.macSize();
    }
}
