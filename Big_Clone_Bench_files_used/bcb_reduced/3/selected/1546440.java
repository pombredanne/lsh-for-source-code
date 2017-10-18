package issrg.pba.rbac;

import java.security.MessageDigest;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.*;

/**
 *
 * @author Linying Su
 */
public class XMLSignatureVerifier extends SimpleSignatureVerifier {

    private static Logger logger = Logger.getLogger(XMLSignatureVerifier.class.getName());

    /** Creates a new instance of XMLSignatureVerifier */
    public XMLSignatureVerifier() {
        super();
    }

    public XMLSignatureVerifier(issrg.security.Verifier verifier) {
        super(verifier);
    }

    /**
     * @param data is the to be digested data.
     * @param digestVal is the digest value.
     * @param digestMethod is the digest method.
     */
    public boolean referenceValidation(byte[] data, byte[] digestVal, String digestMethod) {
        byte[] digested = null;
        try {
            digested = this.digest(data, digestMethod);
        } catch (DigestException de) {
            logger.debug("error: " + de);
            return false;
        }
        if (digested.length != digestVal.length) return false;
        for (int i = 0; i < digested.length; i++) {
            if (digested[i] == digestVal[i]) continue; else return false;
        }
        return true;
    }

    private byte[] digest(byte[] data, String method) throws DigestException {
        try {
            MessageDigest md = MessageDigest.getInstance(method);
            md.update(data);
            byte[] toDigest = md.digest();
            return toDigest;
        } catch (NoSuchAlgorithmException ne) {
            throw new DigestException("couldn't make digest due to the wrong digest method");
        }
    }
}
