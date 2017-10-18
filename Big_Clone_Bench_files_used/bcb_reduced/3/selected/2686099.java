package org.tm4j.topicmap.utils;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * The default IDGenerator implementation. Will generate a String based Unique ID
 * 
 */
public class ImprovedIDGeneratorImpl implements IDGenerator {

    /**
	 * Secure random generator. Kept static to avoid re-initialisation.
	 */
    private SecureRandom m_randomGenerator = null;

    /**
	 * Message digest, kept static to avoid re-initialisation.
	 */
    private MessageDigest m_digest = null;

    /**
	 * Individual count within the systems reported millisecond.
	 */
    private short m_count = 0;

    /**
	 * The address of this machine, attempting to keep the UUID unique over the network.
	 */
    private int m_address = 0;

    /**
	 * The time this class was loaded. Should be fairly unique when compared with address.
	 */
    private long m_startTime = System.currentTimeMillis();

    /**
     * Constructs a new IDGeneratorImpl object, with the
     * base part of the ID string set to the time of creation.
     */
    public ImprovedIDGeneratorImpl() {
        try {
            m_randomGenerator = SecureRandom.getInstance("SHA1PRNG");
            m_digest = MessageDigest.getInstance("MD5");
            m_address = toInt(InetAddress.getLocalHost().getAddress());
        } catch (UnknownHostException e) {
            m_address = 0;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Invalid algorithms coded when starting IDGenerator.");
        }
    }

    /**
	 * Static function for generating a securely random ID that can be used for identifying 
	 * items. The generated ID includes the IP address of the machine it is running on, some 
	 * time information, and a securely random integer.
	 */
    public synchronized String getID() {
        if (m_count < 0) {
            m_count = 0;
        }
        String uid = Integer.toHexString(m_address) + Long.toHexString(m_startTime) + Long.toHexString(System.currentTimeMillis()) + Integer.toHexString(m_count++) + Integer.toHexString(m_randomGenerator.nextInt());
        try {
            uid = hexEncode(m_digest.digest(uid.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("Invalid encoding specified for a byte array in id generator.");
        }
        return uid;
    }

    /**
	 * Convert a byte[] returned by MessageDigest into a nicer textual representation.
	 *
	 * This implementation follows the example of David Flanagan's book
	 * "Java In A Nutshell", and converts a byte array into a String
	 * of hex characters.
	 *
	 * An alternative is to use "Base64" encoding.
	*/
    private static String hexEncode(byte[] byteArray) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int i = 0; i < byteArray.length; i++) {
            byte b = byteArray[i];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    /**
	 * Converts a byte array into an integer value.
	 * @param bytes The byte array.
	 * @return An integer.
	 */
    private static int toInt(byte[] bytes) {
        int result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) - Byte.MIN_VALUE + (int) bytes[i];
        }
        return result;
    }
}
