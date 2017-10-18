package eu.more.cryptographicservicecore.hashes;

import java.security.*;
import eu.more.cryptographicservicecore.commons.Base64;
import eu.more.cryptographicservicecore.commons.SecurityException;
import eu.more.cryptographicservicecore.serializators.SerializationCore;

/**
 * @author Emilio Salazar
 **/
public class HashCore {

    public enum availableHashes {

        SHA1, SHA256, SHA512, MD5
    }

    /**
	 * Get a digest of the object given with the algorithm provided
	 * 
	 * @param object
	 * @param hashType
	 * @return
	 * @throws SecurityException
	 */
    public static byte[] hash(Object object, availableHashes hashType) throws SecurityException {
        byte[] result = null;
        byte[] serializedObject = SerializationCore.serializeToBytes(object);
        try {
            MessageDigest md = MessageDigest.getInstance(hashType.toString(), "BC");
            md.reset();
            result = md.digest(serializedObject);
        } catch (NoSuchAlgorithmException e) {
            throw new SecurityException(e);
        } catch (NoSuchProviderException e) {
            throw new SecurityException(e);
        }
        return result;
    }

    /**
	 * Gets a SHA1-256 bit digest of the object
	 * 
	 * @param object
	 * @return
	 * @throws SecurityException
	 */
    public static String hash(Object object) throws SecurityException {
        byte[] result = hash(object, availableHashes.SHA256);
        return Base64.encodeBytes(result);
    }

    /**
	 * Gets a SHA1-256 bit digest of the object
	 * 
	 * @param object
	 * @return
	 * @throws SecurityException
	 */
    public static byte[] hashToBytes(Object object) throws SecurityException {
        return hash(object, availableHashes.SHA256);
    }

    /**
	 * Produces a digest of the object with the selected algorithm.
	 * 
	 * @param object
	 * @param hashType
	 * @return
	 * @throws SecurityException
	 */
    public static String hashToString(Object object, availableHashes hashType) throws SecurityException {
        byte[] result = hash(object, hashType);
        return Base64.encodeBytes(result);
    }
}
