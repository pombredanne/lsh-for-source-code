package com.jcorporate.expresso.core.security;

import com.jcorporate.expresso.kernel.exception.ChainedException;
import org.apache.log4j.Logger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Base class for hashing.  Takes a string or byte array and returns a
 * byte array that's the hash of the encryption.
 *
 * @author      Michael Rimov
 * @version     $Revision: 3 $      $Date: 2006-03-01 06:17:08 -0500 (Wed, 01 Mar 2006) $
 */
public final class StringHash {

    private static final String thisClass = "com.jcorporate.expresso.core.security.strongencryption.StringHash";

    private MessageDigest sha;

    static Logger logCat = Logger.getLogger(StringHash.class);

    private CryptoManager cryptoManager;

    /**
     *
     *
     * @throws  ChainedException
     */
    public StringHash() throws ChainedException {
        try {
            sha = MessageDigest.getInstance("SHA");
            logCat.debug("StringHash initialized and SHA-1 algorithm loaded");
        } catch (NoSuchAlgorithmException ex) {
            logCat.error("Unable to load SHA-1 Hash Algorithm", ex);
            throw new ChainedException(thisClass + ".StringHash()" + ":Error loading SHA-1 Algorithm." + "  You may not have installed the" + " Cryptography Extensions Properly:", ex);
        }
    }

    /**
     * Produces a strong cryptographic hash.
     *
     * @param   inputData  The data to produce the Hash for
     * @throws  IllegalArgumentException
     * @throws  ChainedException
     * @return A byte array representing the hashed input data.
     */
    public byte[] produceHash(byte[] inputData) throws ChainedException {
        final String myName = thisClass + ".produceHash(byte)";
        if (inputData.length == 0) {
            throw new IllegalArgumentException(myName + " inputData's length must not be zero");
        }
        if (logCat.isDebugEnabled()) {
            logCat.debug("Producing a hash for the data: " + new String(inputData));
        }
        return sha.digest(inputData);
    }

    public void setCryptoManager(CryptoManager cryptoManager) {
        this.cryptoManager = cryptoManager;
    }

    public CryptoManager getCryptoManager() {
        return cryptoManager;
    }
}
