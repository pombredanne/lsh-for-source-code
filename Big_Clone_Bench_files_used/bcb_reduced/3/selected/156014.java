package ca.uwaterloo.crysp.otr.crypt.jca;

import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;
import java.security.*;

/**
 * The SHA-256 hash algorithm, as implemented by the Java Cryptography
 * Architecture.
 * 
 * @author Can Tang <c24tang@gmail.com>
 */
public class JCASHA256 extends ca.uwaterloo.crysp.otr.crypt.SHA256 {

    MessageDigest sha;

    public JCASHA256() {
        super();
        try {
            sha = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public byte[] hash() throws OTRCryptException {
        return sha.digest();
    }

    public void update(byte[] data) throws OTRCryptException {
        sha.update(data);
    }

    public void update(byte[] data, int offset, int length) throws OTRCryptException {
        sha.update(data, offset, length);
    }

    public byte[] hash(byte[] data) throws OTRCryptException {
        sha.update(data);
        return sha.digest();
    }

    public byte[] hash(byte[] data, int offset, int length) throws OTRCryptException {
        sha.update(data, offset, length);
        return sha.digest();
    }

    public boolean verify(byte[] digest, byte[] data) throws OTRCryptException {
        sha.update(data);
        byte[] trueDigest = sha.digest();
        return MessageDigest.isEqual(digest, trueDigest);
    }

    public boolean verify(byte[] digest, byte[] data, int offset, int length) throws OTRCryptException {
        sha.update(data, offset, length);
        byte[] trueDigest = sha.digest();
        return MessageDigest.isEqual(digest, trueDigest);
    }

    public String toString() {
        return sha.toString();
    }
}
