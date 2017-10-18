package foo.bar.forum.security.encryption;

import org.jsecurity.codec.Base64;
import org.jsecurity.codec.Hex;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * An abstract class that encrypt (hash) the password based on an the encryption
 * algorithm to be determined by it's subclass. (through returning a different
 * implementation of JDK's {@link java.security.MessageDigest}.
 *
 * @see {@link Md2Encryptor}
 * @see {@link Md5Encryptor}
 * @see {@link Sha1Encryptor}
 * @see {@link Sha256Encryptor}
 * @see {@link Sha384Encryptor}
 * @see {@link Sha512Encryptor}
 *
 * @author tmjee
 * @version $Date$ $Id$
 */
public abstract class HashedEncryptor implements Encryptor {

    private String salt = "";

    private int iteration = 0;

    private boolean storedCredentialsHexEncoded;

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public int getIteration() {
        return iteration;
    }

    public void setIteration(int iteration) {
        this.iteration = iteration;
    }

    public boolean isStoredCredentialsHexEncoded() {
        return storedCredentialsHexEncoded;
    }

    public void setStoredCredentialsHexEncoded(boolean storedCredentialsHexEncoded) {
        this.storedCredentialsHexEncoded = storedCredentialsHexEncoded;
    }

    public String encrypt(String password) {
        try {
            MessageDigest digest = getMessageDigest();
            digest.reset();
            if ((salt != null) && (salt.trim().length() > 0)) {
                digest.update(salt.getBytes("UTF-8"));
            }
            byte[] encryptedPassword = digest.digest(password.getBytes("UTF-8"));
            for (int a = 0; a < iteration; a++) {
                digest.reset();
                encryptedPassword = digest.digest(encryptedPassword);
            }
            if (storedCredentialsHexEncoded) {
                return new String(Hex.encode(encryptedPassword));
            } else {
                return new String(Base64.encode(encryptedPassword), "UTF-8");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract MessageDigest getMessageDigest() throws NoSuchAlgorithmException;
}
