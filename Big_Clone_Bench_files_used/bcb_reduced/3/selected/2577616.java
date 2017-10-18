package de.fau.cs.dosis.util.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import de.fau.cs.dosis.util.ByteToString;

/**
 * Creates and validates password hashes.
 * Security questions are implemented according to RSA PKCS5 standard.
 * 
 * @author Stefan Schiessl - gneiterl
 * 
 */
public class PasswordCryptographer {

    /**
	 * To slow down the computation it is recommended to iterate the hash
	 * operation n times. A minimum of 1000 operations is recommended in RSA
	 * PKCS5 standard.
	 */
    private static int HASHING_ITERATIONS = 1000;

    /**
	 * Defines the algorithm that is used to create the hash
	 */
    private static String HASH_ALGORITHM = "SHA-1";

    /**
	 * TODO consider refactoring this constant to a project wide constant in
	 * separate class
	 */
    private static String ENCODING = "UTF-8";

    /**
	 * Separator between meta information and hashes.
	 */
    private static String SPLIT_SEPARATOR = "::";

    /**
	 * Validates the user with the entered password.
	 * @param securePasswordHash
	 * @param password
	 * @return true, if the password is correct
	 * @throws PasswordCryptographerException occurs if anything severe goes wrong
	 */
    public boolean validatePassword(String securePasswordHash, String password) throws PasswordCryptographerException {
        try {
            IdentificationSerializer id = new IdentificationSerializer(securePasswordHash);
            byte[] expectedHash = calculateHashGen(id.algorithm, id.encoding, id.iterations, password, ByteToString.convertToByteArray(id.salt));
            return ByteToString.convertToHex(expectedHash).equals(id.hash);
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordCryptographerException(e);
        } catch (UnsupportedEncodingException e) {
            throw new PasswordCryptographerException(e);
        }
    }

    /**
	 * Creates a secure password hash according to the RSA PKCS5 standard.
	 * Meta information (algorithm, salt ...) are added with SPLIT_SEPARATOR.
	 * TODO consider storing the salt in an extra field in the database.
	 * @param password
	 * @return
	 * @throws PasswordCryptographerException occurs if anything severe goes wrong
	 */
    public String createSecurePasswordHash(String password) throws PasswordCryptographerException {
        try {
            return getPasswordIdentifier(password);
        } catch (NoSuchAlgorithmException e) {
            throw new PasswordCryptographerException(e);
        } catch (UnsupportedEncodingException e) {
            throw new PasswordCryptographerException(e);
        }
    }

    private String getPasswordIdentifier(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        IdentificationSerializer id = generatePasswordHashSalted(password);
        id.setIdentification(HASH_ALGORITHM, ENCODING, HASHING_ITERATIONS);
        return id.toString();
    }

    /**
	 * Password Hash Creation according to RSA PKCS5 standard.
	 * 
	 * @param password
	 * @return  secure password hash
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    private IdentificationSerializer generatePasswordHashSalted(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] saltArray = generateSalt();
        byte[] digestArray = calculateHash(HASHING_ITERATIONS, password, saltArray);
        String digest = ByteToString.convertToHex(digestArray);
        String salt = ByteToString.convertToHex(saltArray);
        return new IdentificationSerializer(salt, digest);
    }

    /**
	 * Generate a random 64 bit salt
	 * @return
	 */
    public byte[] generateSalt() {
        Random random = new Random();
        byte[] saltArray = new byte[8];
        random.nextBytes(saltArray);
        return saltArray;
    }

    /**
	 * Calculates the hash with the given password and salt. The hashing is
	 * recalculated hashingIterations times. Default algorithm 
	 * and encoding is used.
	 * @see calculateHashGen
	 * 
	 * @param hashingIterations
	 *            int The number of iterations of the algorithm
	 * @param password
	 *            String
	 * @param salt
	 *            byte[]
	 * @return byte[] The digested password
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
    private byte[] calculateHash(int hashingIterations, String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return calculateHashGen(HASH_ALGORITHM, ENCODING, hashingIterations, password, salt);
    }

    private byte[] calculateHashGen(String algorithm, String encoding, int hashingIterations, String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        digest.reset();
        digest.update(salt);
        byte[] input = digest.digest(password.getBytes(encoding));
        for (int i = 0; i < hashingIterations; i++) {
            digest.reset();
            input = digest.digest(input);
        }
        return input;
    }

    private class IdentificationSerializer {

        private String algorithm;

        private String encoding;

        private String salt;

        private String hash;

        private int iterations;

        public IdentificationSerializer(String salt, String hash) {
            this.salt = salt;
            this.hash = hash;
        }

        public void setIdentification(String algorithm, String encoding, int iterations) {
            this.algorithm = algorithm;
            this.encoding = encoding;
            this.iterations = iterations;
        }

        public IdentificationSerializer(String hashIdentifier) throws PasswordCryptographerException {
            String[] split = hashIdentifier.split(SPLIT_SEPARATOR);
            if (split.length != 5) {
                throw new PasswordCryptographerException("Password hash incorrect, " + "could not instantiate IdentificationSerializer");
            }
            algorithm = split[0];
            encoding = split[1];
            try {
                iterations = Integer.valueOf(split[2]);
            } catch (NumberFormatException e) {
                throw new PasswordCryptographerException("Password hash incorrect, " + "could not instantiate IdentificationSerializer (" + e.getLocalizedMessage() + ")");
            }
            salt = split[3];
            hash = split[4];
        }

        @Override
        public String toString() {
            return algorithm + SPLIT_SEPARATOR + encoding + SPLIT_SEPARATOR + iterations + SPLIT_SEPARATOR + salt + SPLIT_SEPARATOR + hash;
        }
    }
}
