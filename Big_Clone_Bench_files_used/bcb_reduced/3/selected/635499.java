package net.sf.securejdms.keymanagement.core.extensions.basecsp;

import java.security.MessageDigest;
import net.sf.securejdms.keymanagement.core.extensionpoints.IHashFunction;
import net.sf.securejdms.keymanagement.core.extensionpoints.IHashFunctionContent;
import org.apache.log4j.Logger;

/**
 * Cryptographic Service Provider extension: SHA1 hash function.
 * 
 * @author Boris Brodski
 */
public class HashFunctionSHA1 implements IHashFunction {

    private static final Logger log = Logger.getLogger(HashFunctionSHA1.class);

    private static MessageDigest messageDigest;

    static {
        try {
            messageDigest = MessageDigest.getInstance("SHA1");
        } catch (Throwable t) {
            log.warn("Cann't get SHA1 message digest algorithm", t);
        }
    }

    public HashFunctionSHA1() {
        if (messageDigest == null) {
            String message = "SHA1 message digest algorithm couldn't be initialized. See log for  more details.";
            log.error(message);
            throw new RuntimeException(message);
        }
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public IHashFunctionContent beginProcessing(byte[] data) {
        return null;
    }

    /**
	 * {@inheritDoc}
	 */
    @Override
    public byte[] process(byte[] entireData) {
        return messageDigest.digest(entireData);
    }
}
