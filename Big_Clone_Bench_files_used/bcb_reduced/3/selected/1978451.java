package fi.hiit.framework.crypto;

import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import javax.crypto.SecretKey;

public class SHA1Digest extends DigestAlgo {

    public static final String SHA1 = "SHA1";

    public static final String PROVIDER = "BC";

    public static final int HASH_SIZE = 20;

    public SHA1Digest() {
        CryptoPrivder.init();
    }

    /**
	 * Absolite method in non-keyed digest
	 */
    public boolean init(Key key) {
        return true;
    }

    public byte[] digest(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA1, PROVIDER);
            md.update(data);
            return md.digest();
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean verify(byte[] hash, byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance(SHA1, PROVIDER);
            md.update(data);
            return Arrays.equals(hash, md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }
}
