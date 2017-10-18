package gnu.javax.crypto.prng;

import gnu.java.security.prng.BasePRNG;
import gnu.java.security.prng.LimitReachedException;
import gnu.javax.crypto.mac.HMac;
import gnu.javax.crypto.mac.IMac;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * An implementation of the <i>key derivation function</i> KDF2 from PKCS #5:
 * Password-Based Cryptography (<b>PBE</b>). This KDF is essentially a way to
 * transform a password and a salt into a stream of random bytes, which may then
 * be used to initialize a cipher or a MAC.
 * <p>
 * This version uses a MAC as its pseudo-random function, and the password is
 * used as the key.
 * <p>
 * References:
 * <ol>
 * <li>B. Kaliski, <a href="http://www.ietf.org/rfc/rfc2898.txt">RFC 2898:
 * Password-Based Cryptography Specification, Version 2.0</a></li>
 * </ol>
 */
public class PBKDF2 extends BasePRNG implements Cloneable {

    /**
   * The bytes fed into the MAC. This is initially the concatenation of the salt
   * and the block number.
   */
    private byte[] in;

    /** The iteration count. */
    private int iterationCount;

    /** The salt. */
    private byte[] salt;

    /** The MAC (the pseudo-random function we use). */
    private IMac mac;

    /** The number of hLen-sized blocks generated. */
    private long count;

    /**
   * Creates a new PBKDF2 object. The argument is the MAC that will serve as the
   * pseudo-random function. The MAC does not need to be initialized.
   * 
   * @param mac The pseudo-random function.
   */
    public PBKDF2(IMac mac) {
        super("PBKDF2-" + mac.name());
        this.mac = mac;
        iterationCount = -1;
    }

    public void setup(Map attributes) {
        Map macAttrib = new HashMap();
        macAttrib.put(HMac.USE_WITH_PKCS5_V2, Boolean.TRUE);
        byte[] s = (byte[]) attributes.get(IPBE.SALT);
        if (s == null) {
            if (salt == null) throw new IllegalArgumentException("no salt specified");
        } else salt = s;
        byte[] macKeyMaterial;
        char[] password = (char[]) attributes.get(IPBE.PASSWORD);
        if (password != null) {
            String encoding = (String) attributes.get(IPBE.PASSWORD_ENCODING);
            if (encoding == null || encoding.trim().length() == 0) encoding = IPBE.DEFAULT_PASSWORD_ENCODING; else encoding = encoding.trim();
            try {
                macKeyMaterial = new String(password).getBytes(encoding);
            } catch (UnsupportedEncodingException uee) {
                throw new IllegalArgumentException("Unknown or unsupported encoding: " + encoding, uee);
            }
        } else macKeyMaterial = (byte[]) attributes.get(IMac.MAC_KEY_MATERIAL);
        if (macKeyMaterial != null) macAttrib.put(IMac.MAC_KEY_MATERIAL, macKeyMaterial); else if (!initialised) throw new IllegalArgumentException("Neither password nor key-material were specified");
        try {
            mac.init(macAttrib);
        } catch (Exception x) {
            throw new IllegalArgumentException(x.getMessage());
        }
        Integer ic = (Integer) attributes.get(IPBE.ITERATION_COUNT);
        if (ic != null) iterationCount = ic.intValue();
        if (iterationCount <= 0) throw new IllegalArgumentException("bad iteration count");
        count = 0L;
        buffer = new byte[mac.macSize()];
        try {
            fillBlock();
        } catch (LimitReachedException x) {
            throw new Error(x.getMessage());
        }
    }

    public void fillBlock() throws LimitReachedException {
        if (++count > ((1L << 32) - 1)) throw new LimitReachedException();
        Arrays.fill(buffer, (byte) 0x00);
        int limit = salt.length;
        in = new byte[limit + 4];
        System.arraycopy(salt, 0, in, 0, salt.length);
        in[limit++] = (byte) (count >>> 24);
        in[limit++] = (byte) (count >>> 16);
        in[limit++] = (byte) (count >>> 8);
        in[limit] = (byte) count;
        for (int i = 0; i < iterationCount; i++) {
            mac.reset();
            mac.update(in, 0, in.length);
            in = mac.digest();
            for (int j = 0; j < buffer.length; j++) buffer[j] ^= in[j];
        }
    }
}
