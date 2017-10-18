package ca.uwaterloo.crysp.otr.crypt.jca;

import ca.uwaterloo.crysp.otr.crypt.OTRCryptException;
import java.security.*;

/**
 * The SHA-1 hash algorithm, as implemented by the Java Cryptography
 * Architecture.
 * 
 * @author Can Tang <c24tang@gmail.com>
 */
public class JCASHA1 extends ca.uwaterloo.crysp.otr.crypt.SHA1 {

    MessageDigest sha;

    public JCASHA1() {
        super();
        try {
            sha = MessageDigest.getInstance("SHA-1");
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

    public static byte[] fromHex(byte[] msg) {
        byte[] ret = new byte[msg.length / 2];
        for (int i = 0; i < msg.length; i++) {
            if (msg[i] <= 57) msg[i] -= 48; else msg[i] -= 87;
            if (i % 2 == 0) ret[i / 2] += (msg[i] << 4); else ret[i / 2] += msg[i];
        }
        return ret;
    }
}
