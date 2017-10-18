package sun.security.provider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.util.*;
import sun.security.pkcs.PKCS8Key;
import sun.security.pkcs.EncryptedPrivateKeyInfo;
import sun.security.x509.AlgorithmId;
import sun.security.util.ObjectIdentifier;
import sun.security.util.DerValue;

/**
 * This is an implementation of a Sun proprietary, exportable algorithm
 * intended for use when protecting (or recovering the cleartext version of)
 * sensitive keys.
 * This algorithm is not intended as a general purpose cipher.
 *
 * This is how the algorithm works for key protection:
 *
 * p - user password
 * s - random salt
 * X - xor key
 * P - to-be-protected key
 * Y - protected key
 * R - what gets stored in the keystore
 *
 * Step 1:
 * Take the user's password, append a random salt (of fixed size) to it,
 * and hash it: d1 = digest(p, s)
 * Store d1 in X.
 *
 * Step 2:
 * Take the user's password, append the digest result from the previous step,
 * and hash it: dn = digest(p, dn-1).
 * Store dn in X (append it to the previously stored digests).
 * Repeat this step until the length of X matches the length of the private key
 * P.
 *
 * Step 3:
 * XOR X and P, and store the result in Y: Y = X XOR P.
 *
 * Step 4:
 * Store s, Y, and digest(p, P) in the result buffer R:
 * R = s + Y + digest(p, P), where "+" denotes concatenation.
 * (NOTE: digest(p, P) is stored in the result buffer, so that when the key is
 * recovered, we can check if the recovered key indeed matches the original
 * key.) R is stored in the keystore.
 *
 * The protected key is recovered as follows:
 *
 * Step1 and Step2 are the same as above, except that the salt is not randomly
 * generated, but taken from the result R of step 4 (the first length(s)
 * bytes).
 *
 * Step 3 (XOR operation) yields the plaintext key.
 *
 * Then concatenate the password with the recovered key, and compare with the
 * last length(digest(p, P)) bytes of R. If they match, the recovered key is
 * indeed the same key as the original key.
 *
 * @author Jan Luehe
 *
 * @version 1.23, 10/10/06
 *
 * @see java.security.KeyStore
 * @see JavaKeyStore
 * @see KeyTool
 *
 * @since JDK1.2
 */
final class KeyProtector {

    private static final int SALT_LEN = 20;

    private static final String DIGEST_ALG = "SHA";

    private static final int DIGEST_LEN = 20;

    private static final String KEY_PROTECTOR_OID = "1.3.6.1.4.1.42.2.17.1.1";

    private byte[] passwdBytes;

    private MessageDigest md;

    /**
     * Creates an instance of this class, and initializes it with the given
     * password.
     *
     * <p>The password is expected to be in printable ASCII.
     * Normal rules for good password selection apply: at least
     * seven characters, mixed case, with punctuation encouraged.
     * Phrases or words which are easily guessed, for example by
     * being found in dictionaries, are bad.
     */
    public KeyProtector(char[] password) throws NoSuchAlgorithmException {
        int i, j;
        if (password == null) {
            throw new IllegalArgumentException("password can't be null");
        }
        md = MessageDigest.getInstance(DIGEST_ALG);
        passwdBytes = new byte[password.length * 2];
        for (i = 0, j = 0; i < password.length; i++) {
            passwdBytes[j++] = (byte) (password[i] >> 8);
            passwdBytes[j++] = (byte) password[i];
        }
    }

    /**
     * Ensures that the password bytes of this key protector are
     * set to zero when there are no more references to it.
     */
    protected void finalize() {
        if (passwdBytes != null) {
            Arrays.fill(passwdBytes, (byte) 0x00);
            passwdBytes = null;
        }
    }

    public byte[] protect(Key key) throws KeyStoreException {
        int i;
        int numRounds;
        byte[] digest;
        int xorOffset;
        int encrKeyOffset = 0;
        if (key == null) {
            throw new IllegalArgumentException("plaintext key can't be null");
        }
        byte[] plainKey = key.getEncoded();
        numRounds = plainKey.length / DIGEST_LEN;
        if ((plainKey.length % DIGEST_LEN) != 0) numRounds++;
        byte[] salt = new byte[SALT_LEN];
        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        byte[] xorKey = new byte[plainKey.length];
        for (i = 0, xorOffset = 0, digest = salt; i < numRounds; i++, xorOffset += DIGEST_LEN) {
            md.update(passwdBytes);
            md.update(digest);
            digest = md.digest();
            md.reset();
            if (i < numRounds - 1) {
                System.arraycopy(digest, 0, xorKey, xorOffset, digest.length);
            } else {
                System.arraycopy(digest, 0, xorKey, xorOffset, xorKey.length - xorOffset);
            }
        }
        byte[] tmpKey = new byte[plainKey.length];
        for (i = 0; i < tmpKey.length; i++) {
            tmpKey[i] = (byte) (plainKey[i] ^ xorKey[i]);
        }
        byte[] encrKey = new byte[salt.length + tmpKey.length + DIGEST_LEN];
        System.arraycopy(salt, 0, encrKey, encrKeyOffset, salt.length);
        encrKeyOffset += salt.length;
        System.arraycopy(tmpKey, 0, encrKey, encrKeyOffset, tmpKey.length);
        encrKeyOffset += tmpKey.length;
        md.update(passwdBytes);
        Arrays.fill(passwdBytes, (byte) 0x00);
        passwdBytes = null;
        md.update(plainKey);
        digest = md.digest();
        md.reset();
        System.arraycopy(digest, 0, encrKey, encrKeyOffset, digest.length);
        AlgorithmId encrAlg;
        try {
            encrAlg = new AlgorithmId(new ObjectIdentifier(KEY_PROTECTOR_OID));
            return new EncryptedPrivateKeyInfo(encrAlg, encrKey).getEncoded();
        } catch (IOException ioe) {
            throw new KeyStoreException(ioe.getMessage());
        }
    }

    public Key recover(EncryptedPrivateKeyInfo encrInfo) throws UnrecoverableKeyException {
        int i;
        byte[] digest;
        int numRounds;
        int xorOffset;
        int encrKeyLen;
        AlgorithmId encrAlg = encrInfo.getAlgorithm();
        if (!(encrAlg.getOID().toString().equals(KEY_PROTECTOR_OID))) {
            throw new UnrecoverableKeyException("Unsupported key protection " + "algorithm");
        }
        byte[] protectedKey = encrInfo.getEncryptedData();
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(protectedKey, 0, salt, 0, SALT_LEN);
        encrKeyLen = protectedKey.length - SALT_LEN - DIGEST_LEN;
        numRounds = encrKeyLen / DIGEST_LEN;
        if ((encrKeyLen % DIGEST_LEN) != 0) numRounds++;
        byte[] encrKey = new byte[encrKeyLen];
        System.arraycopy(protectedKey, SALT_LEN, encrKey, 0, encrKeyLen);
        byte[] xorKey = new byte[encrKey.length];
        for (i = 0, xorOffset = 0, digest = salt; i < numRounds; i++, xorOffset += DIGEST_LEN) {
            md.update(passwdBytes);
            md.update(digest);
            digest = md.digest();
            md.reset();
            if (i < numRounds - 1) {
                System.arraycopy(digest, 0, xorKey, xorOffset, digest.length);
            } else {
                System.arraycopy(digest, 0, xorKey, xorOffset, xorKey.length - xorOffset);
            }
        }
        byte[] plainKey = new byte[encrKey.length];
        for (i = 0; i < plainKey.length; i++) {
            plainKey[i] = (byte) (encrKey[i] ^ xorKey[i]);
        }
        md.update(passwdBytes);
        Arrays.fill(passwdBytes, (byte) 0x00);
        passwdBytes = null;
        md.update(plainKey);
        digest = md.digest();
        md.reset();
        for (i = 0; i < digest.length; i++) {
            if (digest[i] != protectedKey[SALT_LEN + encrKeyLen + i]) {
                throw new UnrecoverableKeyException("Cannot recover key");
            }
        }
        try {
            return PKCS8Key.parseKey(new DerValue(plainKey));
        } catch (IOException ioe) {
            throw new UnrecoverableKeyException(ioe.getMessage());
        }
    }
}
