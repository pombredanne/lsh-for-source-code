package jonelo.jacksum.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import jonelo.jacksum.adapt.com.bitzi.util.TigerTree;

/**
 * A wrapper class that can be used to compute TigerTree
 */
public class MDTree extends AbstractChecksum {

    private MessageDigest md = null;

    private boolean virgin = true;

    private byte[] digest = null;

    public MDTree(String arg) throws NoSuchAlgorithmException {
        length = 0;
        filename = null;
        separator = " ";
        encoding = BASE32;
        virgin = true;
        md = new TigerTree(arg);
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
