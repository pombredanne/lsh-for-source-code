package de.iritgo.aktera.crypto;

import de.iritgo.aktera.core.exception.NestedException;
import de.iritgo.aktera.crypto.Encryptor;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 *
 */
public abstract class AbstractEncryptor implements Encryptor {

    public static String ROLE = "de.iritgo.aktera.crypto.Encryptor";

    public abstract byte[] decrypt(byte[] inputData) throws NestedException;

    public abstract byte[] encrypt(byte[] inputData) throws NestedException;

    /**
	 * "Hash" the given data
	 *
	 * @param   inputData[] Data to be hashed
	 * @return
	 */
    public byte[] hash(byte[] inputData) throws NestedException {
        assert inputData.length != 0;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            return sha.digest(inputData);
        } catch (NoSuchAlgorithmException ex) {
            throw new NestedException("Error loading SHA Algorithm.", ex);
        }
    }

    public abstract void setKey(String newKey);
}
