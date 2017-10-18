package net.sourceforge.epoint.pgp;

import java.security.*;
import java.security.spec.*;

/**
 * Salted <code>S2K</code> as defined in rfc2440
 * 
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public abstract class SaltedS2K implements S2K {

    protected byte[] salt;

    protected int keyLength;

    protected String algorithm;

    /**
     * Turn a sequence of bytes into a cryptographic key
     * @param keyData datablock containing the key material
     * @return cryptographic key
     */
    protected abstract Key makeKey(byte[] keyData) throws InvalidKeySpecException, InvalidKeyException, NoSuchAlgorithmException;

    /**
     * Generate a key using a passphrase and salt
     * @param s passphrase
     * @return cryptographic key
     */
    public final Key getKey(String s) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException, CloneNotSupportedException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        MessageDigest md2;
        byte[] km = new byte[keyLength], pw = s.getBytes();
        int i, l = md.getDigestLength(), r = keyLength - l;
        for (i = 0; i < r; i += l) {
            md2 = (MessageDigest) md.clone();
            md.update((byte) 0);
            md2.update(salt);
            md2.update(pw);
            System.arraycopy(md2.digest(), 0, km, i, l);
        }
        md.update(salt);
        md.update(pw);
        System.arraycopy(md.digest(), 0, km, i, keyLength - i);
        return makeKey(km);
    }

    public final String getDigestAlgorithm() {
        return new String(algorithm);
    }
}
