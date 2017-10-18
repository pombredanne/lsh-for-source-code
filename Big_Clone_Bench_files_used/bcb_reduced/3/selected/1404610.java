package ch.ethz.dcg.spamato.peerato.common.msg.data;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Represents a hash value and provides utility functions.
 * 
 * @author Michelle Ackermann
 */
public class Hash implements Serializable {

    private static final char[] hexChar = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private byte[] hashValue;

    public Hash() {
    }

    /**
	 * Creates a Hash for a given hash value.
	 * @param hashValue hash value to be represented by the hash object
	 */
    public Hash(byte[] hashValue) {
        if (hashValue == null) {
            throw new IllegalArgumentException("HashValue cannot be null");
        }
        this.hashValue = hashValue;
    }

    /**
	 * @return hash value as a byte array
	 */
    public byte[] getHashValue() {
        return hashValue;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer(hashValue.length * 2);
        for (int i = 0; i < hashValue.length; i++) {
            sb.append(hexChar[(hashValue[i] & 0xf0) >>> 4]);
            sb.append(hexChar[hashValue[i] & 0x0f]);
        }
        return sb.toString();
    }

    /**
	 * Two Hashs are equal if their hash values have the same length
	 * and contain the same byte values.
	 */
    public boolean equals(Object obj) {
        if (obj instanceof Hash) {
            return Arrays.equals(hashValue, ((Hash) obj).hashValue);
        } else {
            return false;
        }
    }

    public int hashCode() {
        int hashCode = 0;
        for (int i = 0; i < hashValue.length; i++) {
            hashCode = hashCode * 37 + hashValue[i];
        }
        return hashCode;
    }

    /**
	 * Checks if the specified data has the same hash value as this Hash.
	 * @param data data to check
	 * @param length length of the data
	 * @param algorithm hash algorithm the hash value was created with
	 * @return <code>true</code> if the hash value of the data is equal to the hash value of
	 * this Hash, <code>false</code> otherwise
	 */
    public boolean check(byte[] data, int length, String algorithm) {
        Hash dataHash;
        try {
            dataHash = Hash.createHash(data, length, algorithm);
        } catch (NoSuchAlgorithmException e) {
            dataHash = null;
        }
        return equals(dataHash);
    }

    /**
	 * Checks if the specified data has the same hash value as this Hash.
	 * @param data data to check the hash value of
	 * @param algorithm hash algorithm the hash value was created with
	 * @return <code>true</code> if the hash value of the data is equal to the hash value of
	 * this Hash, <code>false</code> otherwise
	 */
    public boolean check(byte[] data, String algorithm) {
        return check(data, data.length, algorithm);
    }

    /**
	 * Creates a Hash from the given data and the specified hash algorithm.
	 * @param data data to generate a hash value
	 * @param length length of the data
	 * @param algorithm hash algorithm to create the hash value with
	 * @return a Hash that represents the generated hash value
	 * @throws NoSuchAlgorithmException if the specified hash algorithm isn't a supported hash algorithm
	 */
    public static Hash createHash(byte[] data, int length, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(data, 0, length);
        return new Hash(md.digest());
    }

    /**
	 * Creates a Hash from the given data and the specified hash algorithm.
	 * @param data data to generate a hash value
	 * @param algorithm hash algorithm to create the hash value with
	 * @return a Hash that represents the generated hash value
	 * @throws NoSuchAlgorithmException if the hash algorithm taken from PeerSettings isn't a supported hash algorithm
	 */
    public static Hash createHash(byte[] data, String algorithm) throws NoSuchAlgorithmException {
        return createHash(data, data.length, algorithm);
    }
}
