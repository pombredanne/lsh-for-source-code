package eu.more.keydistributionservice.internal.hash;

import java.security.*;
import org.soda.dpws.DPWSException;
import eu.more.keydistributionservice.commons.Base64;
import eu.more.keydistributionservice.internal.serialization.Serialization;

/**
 * 
 * @author Emilio Salazar
 *
 **/
public class Hash {

    public enum availableHashes {

        SHA1, SHA256, SHA512, MD5
    }

    /**
	 * Get a digest of the object given with the algorithm provided
	 * @param object
	 * @param hashType
	 * @return
	 * @throws DPWSException 
	 */
    public static byte[] hash(Object object, availableHashes hashType) throws DPWSException {
        byte[] result = null;
        byte[] serializedObject = Serialization.serializeToBytes(object);
        try {
            MessageDigest md = MessageDigest.getInstance(hashType.toString());
            md.reset();
            result = md.digest(serializedObject);
        } catch (NoSuchAlgorithmException e) {
            throw new DPWSException("NoSuchAlgorithmException");
        }
        return result;
    }

    /**
	 * Gets a SHA1-256 bit digest of the object
	 * @param object
	 * @return
	 * @throws DPWSException 
	 */
    public static String hash(Object object) throws DPWSException {
        byte[] result = hash(object, availableHashes.SHA256);
        return Base64.encodeBytes(result);
    }

    /**
	 * Gets a SHA1-256 bit digest of the object
	 * @param object
	 * @return
	 * @throws DPWSException 
	 */
    public static byte[] hashToBytes(Object object) throws DPWSException {
        return hash(object, availableHashes.SHA256);
    }

    /**
	 * Produces a digest of the object with the selected algorithm.
	 * @param object
	 * @param hashType
	 * @return
	 * @throws DPWSException 
	 */
    public static String hashToString(Object object, availableHashes hashType) throws DPWSException {
        byte[] result = hash(object, hashType);
        return Base64.encodeBytes(result);
    }
}
