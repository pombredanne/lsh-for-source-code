package jonelo.jacksum.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A wrapper class that can be used to compute MD5, SHA-1, SHA-256, SHA-384 and SHA-512
 * (provided by your JVM vendor).
 */
public class MD extends AbstractChecksum {

    private MessageDigest md = null;

    private boolean virgin = true;

    private byte[] digest = null;

    public MD(String arg) throws NoSuchAlgorithmException {
        length = 0;
        filename = null;
        separator = " ";
        encoding = HEX;
        virgin = true;
        md = MessageDigest.getInstance(arg);
    }

    public void reset() {
        md.reset();
        length = 0;
        virgin = true;
    }

    public void update(byte[] buffer, int offset, int len) {
        md.update(buffer, offset, len);
        length += len;
    }

    public void update(byte b) {
        md.update(b);
        length++;
    }

    public void update(int b) {
        update((byte) (b & 0xFF));
    }

    public String toString() {
        return getFormattedValue() + separator + (isTimestampWanted() ? getTimestampFormatted() + separator : "") + getFilename();
    }

    public byte[] getByteArray() {
        if (virgin) {
            digest = md.digest();
            virgin = false;
        }
        byte[] save = new byte[digest.length];
        System.arraycopy(digest, 0, save, 0, digest.length);
        return save;
    }
}
