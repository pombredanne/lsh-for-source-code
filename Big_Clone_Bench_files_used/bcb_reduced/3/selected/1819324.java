package gp.utils.arrays;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Gwenhael Pasquiers
 */
public class DigestArray extends ReadOnlyArray {

    private boolean doDigest;

    private Array array;

    private byte[] digest;

    private MessageDigest msgDigest;

    public DigestArray(Array array, String algorithm) {
        this.array = array;
        try {
            this.msgDigest = (MessageDigest) MessageDigest.getInstance(algorithm).clone();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        this.length = msgDigest.getDigestLength();
        this.doDigest = true;
    }

    @Override
    protected byte doGet(int i) {
        if (this.doDigest) {
            computeDigest();
        }
        return digest[i];
    }

    private synchronized void computeDigest() {
        if (this.doDigest == false) return;
        this.msgDigest.reset();
        for (int i = 0; i < this.array.length; i++) this.msgDigest.update(this.array.get(i));
        this.digest = this.msgDigest.digest();
        this.length = this.digest.length;
        this.doDigest = false;
    }

    @Override
    protected void doGetBytes(int sourceOffset, byte[] target, int targetOffset, int copyLength) {
        if (this.doDigest) {
            computeDigest();
        }
        System.arraycopy(digest, sourceOffset, target, targetOffset, copyLength);
    }
}
