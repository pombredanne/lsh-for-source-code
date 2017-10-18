package org.beepcore.beep.profile.sasl.otp.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.beepcore.beep.profile.sasl.InvalidParameterException;

/**
 *
 * @author Eric Dixon
 * @author Huston Franklin
 * @author Jay Kint
 * @author Scott Pead
 * @version $Revision: 1.5 $, $Date: 2003/11/18 14:03:10 $
 *
 */
public abstract class AlgorithmImpl implements Algorithm {

    private String internalAlgorithmName;

    /**
     * Method AlgorithmImpl
     *
     * @param internal The data used by the JVM internally to represent
     *                 a certain MessageDigest hash algorithm.   This is
     *                 defined in JVM documentation and in constants in
     *                 SASLOTPProfile.
     *
     */
    public AlgorithmImpl(String internal) {
        internalAlgorithmName = internal;
    }

    /**
     * Method getName
     */
    public abstract String getName();

    /**
     * Method generateHash generate a hash value using the appropriate
     * hash function.
     *
     * @param s The data to be hashed
     * @return byte[] the hash value in binary form.
     *
     * @throws SASLException if an error is encountered during the 
     * generation of hte hash.
     *
     */
    public byte[] generateHash(String s) throws InvalidParameterException {
        return generateHash(s.toLowerCase().getBytes());
    }

    /**
     * Method generateHash generate a hash value using the appropriate
     * hash function.
     *
     * @param data The data to be hashed
     * @return byte[] the hash value in binary form.
     *
     * @throws SASLException if an error is encountered during the 
     * generation of hte hash.
     *
     */
    public byte[] generateHash(byte data[]) throws InvalidParameterException {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(internalAlgorithmName);
        } catch (NoSuchAlgorithmException x) {
            throw new RuntimeException(internalAlgorithmName + " hash algorithm not found");
        }
        return digest.digest(data);
    }

    /**
     * Method foldHash is provided for implementations, as the value
     * of the message digest hash must be folding into 64 bits before 
     * it can be used by the SASLOTPProfile and its supporting classes.
     *
     * @param hash The hash value to be folded
     * @return byte[] is the folded hash.
     *
     * @throws InvalidParameterException of the has provided is
     * somehow improper or invalid.
     *
     */
    protected abstract byte[] foldHash(byte hash[]) throws InvalidParameterException;
}
