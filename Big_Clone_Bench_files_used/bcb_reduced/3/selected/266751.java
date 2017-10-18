package cdc.standard.mac.hmac;

import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import java.math.BigInteger;
import cdc.standard.pbe.PBEBMPKey;

/**
 * This class extends the javax.crypto.MACSpi class for providing
 * the functionality of the HMAC(Keyed-Hashing for
 * Message Authentication) algorithm, as specified in
 * <a href="http://info.internet.isi.edu:80/in-notes/rfc/files/rfc2104.txt">
 * RFC 2104</a>.
 *
 *<p>
 * Any application dealing with MAC computation, uses the getInstance
 * method of the MAC class for creating a MAC object.
 *
 *<p>
 * cdcProvider supports HMAC computation based on the SHA-1 and
 * Md5 hash algorithms.
 */
public class HMac extends MacSpi {

    /**
     * The algorithm used for the HMac. Usually SHA1 or MD5
     */
    private String algorithm_;

    private static byte ipad_ = 0x36;

    private static byte opad_ = 0x5c;

    private static int B_ = 64;

    protected int L_;

    private byte[] ipad_key = new byte[64];

    private byte[] opad_key = new byte[64];

    private byte[] macKey = new byte[64];

    protected MessageDigest md;

    /**
     * Creates a new HMac for the specified hash algorithm.
     *
     * This constructor is called by every subclass for specifying
     * the particular hash algorithm to be used for HMac computation.
     * @param hashAlgorithm the hash algorithm to use.
     */
    public HMac(String hashAlgorithm) throws NoSuchAlgorithmException {
        algorithm_ = hashAlgorithm;
        md = MessageDigest.getInstance(algorithm_);
        L_ = md.getDigestLength();
    }

    private byte[] augment(byte[] in) {
        int n = in.length;
        int v = 64;
        int tmp;
        int amount;
        int iter;
        byte[] out;
        tmp = n / v;
        if (n % v != 0) tmp++;
        amount = v * tmp;
        out = new byte[amount];
        iter = amount / n;
        for (int i = 0; i < iter; i++) System.arraycopy(in, 0, out, i * n, n);
        if (amount % n != 0) System.arraycopy(in, 0, out, iter * n, amount % n);
        return out;
    }

    /**
     * Returns the calculated MAC value. After the MAC finally has been
     * calculated, the MAC object is reset for further MAC computations.
     * @return the calculated MAC value.
     */
    public byte[] engineDoFinal() {
        byte[] hash1;
        byte[] hmac;
        hash1 = md.digest();
        md.reset();
        md.update(opad_key);
        md.update(hash1);
        hmac = md.digest();
        return hmac;
    }

    public int engineGetMacLength() {
        return L_;
    }

    /**
     * Initializes this Mac Object with the given secret key and algorithm
     * parameterSpec specification. The parameters are ignored.
     *
     * @param key the secret key with which this MAC object is initialized.
     * @param params the parameters are ignored, if the key is not a
     * HmacKey.
     */
    public void engineInit(Key key, AlgorithmParameterSpec params) throws InvalidKeyException, InvalidAlgorithmParameterException {
        PBEBMPKey hmacKey;
        byte[] keyBytes;
        PBEParameterSpec spec;
        if (key instanceof PBEBMPKey) {
            hmacKey = (PBEBMPKey) key;
            if (params instanceof PBEParameterSpec) {
                spec = (PBEParameterSpec) params;
                keyBytes = generateKeyBytesPKCS12(hmacKey, spec.getSalt(), spec.getIterationCount());
            } else throw new InvalidAlgorithmParameterException("Unsupported AlgorithmParameterSpec");
        } else if (key instanceof SecretKey) {
            keyBytes = key.getEncoded();
        } else throw new InvalidKeyException();
        int n = B_ - keyBytes.length;
        if (n > 0) {
            System.arraycopy(keyBytes, 0, macKey, 0, keyBytes.length);
            for (int i = 0; i < n; i++) macKey[keyBytes.length + i] = 0;
        } else {
            if (n < 0) {
                md.update(keyBytes);
                macKey = md.digest();
            }
        }
        System.arraycopy(macKey, 0, ipad_key, 0, B_);
        System.arraycopy(macKey, 0, opad_key, 0, B_);
        for (int j = 0; j < B_; j++) {
            ipad_key[j] = (byte) (ipad_key[j] ^ ipad_);
            opad_key[j] = (byte) (opad_key[j] ^ opad_);
        }
        md.update(ipad_key);
    }

    /**
     * Resets this MAC object so that it may be used for further
     * MAC comptations.
     */
    public void engineReset() {
        md.reset();
    }

    /**
     * Processes the given number of bytes, supplied in a byte array
     * starting at the given position.
     */
    public void engineUpdate(byte[] input, int offset, int len) {
        md.update(input, offset, len);
    }

    /**
     * Processes the given byte
     * @param input the byte to be processed.
     */
    public void engineUpdate(byte input) {
        md.update(input);
    }

    /**
    * This function takes the passphrase from the PBEKey and the salt
    * according to the key generation scheme described in PKCS12.
    *
    * <p>
    * @param pbeKey the PBEKey.
    * @param salt the salt.
    * @param iterationCount the iteration count.
    * @param keyLength the length of the generated key.
    * @param id the ID byte. If the byte string is to be used as key material for encryption/decryption ID is 1, in case it is to be used as an iv vector ID is 2 and in case of integrity protection ID is 3.
    * @return the bytes representing a key for the underlying cipher.
    */
    private byte[] generateKeyBytesPKCS12(PBEBMPKey hmacKey, byte[] salt, int iterationCount) {
        byte[] passwd = hmacKey.getEncoded();
        byte[] mD = new byte[64];
        for (int i = 0; i < mD.length; i++) mD[i] = 3;
        byte[] mP = augment(passwd);
        byte[] mS = augment(salt);
        byte[] mI = new byte[mP.length + mS.length];
        System.arraycopy(mS, 0, mI, 0, mS.length);
        System.arraycopy(mP, 0, mI, mS.length, mP.length);
        byte[] mA;
        md.update(mD);
        md.update(mI);
        mA = md.digest();
        for (int i = 1; i < iterationCount; i++) {
            md.update(mA);
            mA = md.digest();
        }
        return mA;
    }

    /**
     * Returns the length of the calculated MAC value in bytes.
     */
    public int getMacLength() {
        return L_;
    }
}
