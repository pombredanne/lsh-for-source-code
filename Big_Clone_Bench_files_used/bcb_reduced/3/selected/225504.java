package gnu.java.security.sig.rsa;

import gnu.java.security.Configuration;
import gnu.java.security.hash.HashFactory;
import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.util.Util;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * An implementation of the EMSA-PSS encoding/decoding scheme.
 * <p>
 * EMSA-PSS coincides with EMSA4 in IEEE P1363a D5 except that EMSA-PSS acts on
 * octet strings and not on bit strings. In particular, the bit lengths of the
 * hash and the salt must be multiples of 8 in EMSA-PSS. Moreover, EMSA4 outputs
 * an integer of a desired bit length rather than an octet string.
 * <p>
 * EMSA-PSS is parameterized by the choice of hash function Hash and mask
 * generation function MGF. In this submission, MGF is based on a Hash
 * definition that coincides with the corresponding definitions in IEEE Std
 * 1363-2000, PKCS #1 v2.0, and the draft ANSI X9.44. In PKCS #1 v2.0 and the
 * draft ANSI X9.44, the recommended hash function is SHA-1, while IEEE Std
 * 1363-2000 recommends SHA-1 and RIPEMD-160.
 * <p>
 * References:
 * <ol>
 * <li><a
 * href="http://www.cosic.esat.kuleuven.ac.be/nessie/workshop/submissions/rsa-pss.zip">
 * RSA-PSS Signature Scheme with Appendix, part B.</a><br>
 * Primitive specification and supporting documentation.<br>
 * Jakob Jonsson and Burt Kaliski.</li>
 * </ol>
 */
public class EMSA_PSS implements Cloneable {

    private static final Logger log = Logger.getLogger(EMSA_PSS.class.getName());

    /** The underlying hash function to use with this instance. */
    private IMessageDigest hash;

    /** The output size of the hash function in octets. */
    private int hLen;

    /**
   * Trivial private constructor to enforce use through Factory method.
   * 
   * @param hash the message digest instance to use with this scheme instance.
   */
    private EMSA_PSS(IMessageDigest hash) {
        super();
        this.hash = hash;
        hLen = hash.hashSize();
    }

    /**
   * Returns an instance of this object given a designated name of a hash
   * function.
   * 
   * @param mdName the canonical name of a hash function.
   * @return an instance of this object configured for use with the designated
   *         options.
   */
    public static EMSA_PSS getInstance(String mdName) {
        IMessageDigest hash = HashFactory.getInstance(mdName);
        return new EMSA_PSS(hash);
    }

    public Object clone() {
        return getInstance(hash.name());
    }

    /**
   * The encoding operation EMSA-PSS-Encode computes the hash of a message
   * <code>M</code> using a hash function and maps the result to an encoded
   * message <code>EM</code> of a specified length using a mask generation
   * function.
   * 
   * @param mHash the byte sequence resulting from applying the message digest
   *          algorithm Hash to the message <i>M</i>.
   * @param emBits the maximal bit length of the integer OS2IP(EM), at least
   *          <code>8.hLen + 8.sLen + 9</code>.
   * @param salt the salt to use when encoding the output.
   * @return the encoded message <code>EM</code>, an octet string of length
   *         <code>emLen = CEILING(emBits / 8)</code>.
   * @exception IllegalArgumentException if an exception occurs.
   */
    public byte[] encode(byte[] mHash, int emBits, byte[] salt) {
        int sLen = salt.length;
        if (hLen != mHash.length) throw new IllegalArgumentException("wrong hash");
        if (emBits < (8 * hLen + 8 * sLen + 9)) throw new IllegalArgumentException("encoding error");
        int emLen = (emBits + 7) / 8;
        byte[] H;
        int i;
        synchronized (hash) {
            for (i = 0; i < 8; i++) hash.update((byte) 0x00);
            hash.update(mHash, 0, hLen);
            hash.update(salt, 0, sLen);
            H = hash.digest();
        }
        byte[] DB = new byte[emLen - sLen - hLen - 2 + 1 + sLen];
        DB[emLen - sLen - hLen - 2] = 0x01;
        System.arraycopy(salt, 0, DB, emLen - sLen - hLen - 1, sLen);
        byte[] dbMask = MGF(H, emLen - hLen - 1);
        if (Configuration.DEBUG) {
            log.fine("dbMask (encode): " + Util.toString(dbMask));
            log.fine("DB (encode): " + Util.toString(DB));
        }
        for (i = 0; i < DB.length; i++) DB[i] = (byte) (DB[i] ^ dbMask[i]);
        DB[0] &= (0xFF >>> (8 * emLen - emBits));
        byte[] result = new byte[emLen];
        System.arraycopy(DB, 0, result, 0, emLen - hLen - 1);
        System.arraycopy(H, 0, result, emLen - hLen - 1, hLen);
        result[emLen - 1] = (byte) 0xBC;
        return result;
    }

    /**
   * The decoding operation EMSA-PSS-Decode recovers the message hash from an
   * encoded message <code>EM</code> and compares it to the hash of
   * <code>M</code>.
   * 
   * @param mHash the byte sequence resulting from applying the message digest
   *          algorithm Hash to the message <i>M</i>.
   * @param EM the <i>encoded message</i>, an octet string of length
   *          <code>emLen = CEILING(emBits/8).
   * @param emBits the maximal bit length of the integer OS2IP(EM), at least
   * <code>8.hLen + 8.sLen + 9</code>.
   * @param sLen the length, in octets, of the expected salt.
   * @return <code>true</code> if the result of the verification was
   * <i>consistent</i> with the expected reseult; and <code>false</code> if the
   * result was <i>inconsistent</i>.
   * @exception IllegalArgumentException if an exception occurs.
   */
    public boolean decode(byte[] mHash, byte[] EM, int emBits, int sLen) {
        if (Configuration.DEBUG) {
            log.fine("mHash: " + Util.toString(mHash));
            log.fine("EM: " + Util.toString(EM));
            log.fine("emBits: " + String.valueOf(emBits));
            log.fine("sLen: " + String.valueOf(sLen));
        }
        if (sLen < 0) throw new IllegalArgumentException("sLen");
        if (hLen != mHash.length) {
            if (Configuration.DEBUG) log.fine("hLen != mHash.length; hLen: " + String.valueOf(hLen));
            throw new IllegalArgumentException("wrong hash");
        }
        if (emBits < (8 * hLen + 8 * sLen + 9)) {
            if (Configuration.DEBUG) log.fine("emBits < (8hLen + 8sLen + 9); sLen: " + String.valueOf(sLen));
            throw new IllegalArgumentException("decoding error");
        }
        int emLen = (emBits + 7) / 8;
        if ((EM[EM.length - 1] & 0xFF) != 0xBC) {
            if (Configuration.DEBUG) log.fine("EM does not end with 0xBC");
            return false;
        }
        if ((EM[0] & (0xFF << (8 - (8 * emLen - emBits)))) != 0) {
            if (Configuration.DEBUG) log.fine("Leftmost 8emLen - emBits bits of EM are not 0s");
            return false;
        }
        byte[] DB = new byte[emLen - hLen - 1];
        byte[] H = new byte[hLen];
        System.arraycopy(EM, 0, DB, 0, emLen - hLen - 1);
        System.arraycopy(EM, emLen - hLen - 1, H, 0, hLen);
        byte[] dbMask = MGF(H, emLen - hLen - 1);
        int i;
        for (i = 0; i < DB.length; i++) DB[i] = (byte) (DB[i] ^ dbMask[i]);
        DB[0] &= (0xFF >>> (8 * emLen - emBits));
        if (Configuration.DEBUG) {
            log.fine("dbMask (decode): " + Util.toString(dbMask));
            log.fine("DB (decode): " + Util.toString(DB));
        }
        for (i = 0; i < (emLen - hLen - sLen - 2); i++) {
            if (DB[i] != 0) {
                if (Configuration.DEBUG) log.fine("DB[" + String.valueOf(i) + "] != 0x00");
                return false;
            }
        }
        if (DB[i] != 0x01) {
            if (Configuration.DEBUG) log.fine("DB's byte at position (emLen -hLen -sLen -2); i.e. " + String.valueOf(i) + " is not 0x01");
            return false;
        }
        byte[] salt = new byte[sLen];
        System.arraycopy(DB, DB.length - sLen, salt, 0, sLen);
        byte[] H0;
        synchronized (hash) {
            for (i = 0; i < 8; i++) hash.update((byte) 0x00);
            hash.update(mHash, 0, hLen);
            hash.update(salt, 0, sLen);
            H0 = hash.digest();
        }
        return Arrays.equals(H, H0);
    }

    /**
   * A mask generation function takes an octet string of variable length and a
   * desired output length as input, and outputs an octet string of the desired
   * length. There may be restrictions on the length of the input and output
   * octet strings, but such bounds are generally very large. Mask generation
   * functions are deterministic; the octet string output is completely
   * determined by the input octet string. The output of a mask generation
   * function should be pseudorandom, that is, it should be infeasible to
   * predict, given one part of the output but not the input, another part of
   * the output. The provable security of RSA-PSS relies on the random nature of
   * the output of the mask generation function, which in turn relies on the
   * random nature of the underlying hash function.
   * 
   * @param Z a seed.
   * @param l the desired output length in octets.
   * @return the mask.
   * @exception IllegalArgumentException if the desired output length is too
   *              long.
   */
    private byte[] MGF(byte[] Z, int l) {
        if (l < 1 || (l & 0xFFFFFFFFL) > ((hLen & 0xFFFFFFFFL) << 32L)) throw new IllegalArgumentException("mask too long");
        byte[] result = new byte[l];
        int limit = ((l + hLen - 1) / hLen) - 1;
        IMessageDigest hashZ = null;
        hashZ = (IMessageDigest) hash.clone();
        hashZ.digest();
        hashZ.update(Z, 0, Z.length);
        IMessageDigest hashZC = null;
        byte[] t;
        int sofar = 0;
        int length;
        for (int i = 0; i < limit; i++) {
            hashZC = (IMessageDigest) hashZ.clone();
            hashZC.update((byte) (i >>> 24));
            hashZC.update((byte) (i >>> 16));
            hashZC.update((byte) (i >>> 8));
            hashZC.update((byte) i);
            t = hashZC.digest();
            length = l - sofar;
            length = (length > hLen ? hLen : length);
            System.arraycopy(t, 0, result, sofar, length);
            sofar += length;
        }
        return result;
    }
}
