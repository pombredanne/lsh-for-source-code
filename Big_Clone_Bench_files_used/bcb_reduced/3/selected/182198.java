package jonelo.jacksum.algorithm;

import jonelo.jacksum.adapt.gnu.crypto.hash.HashFactory;
import jonelo.jacksum.adapt.gnu.crypto.hash.IMessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A class that can be used to compute the eDonkey of a data stream.
 */
public class Edonkey extends AbstractChecksum {

    private static final String AUX_ALGORITHM = "md4";

    private IMessageDigest md4 = null;

    private IMessageDigest md4final = null;

    private boolean virgin = true;

    private static final int BLOCKSIZE = 9728000;

    private byte[] edonkeyHash = new byte[16];

    private byte[] digest = null;

    /** Creates a new Edonkey object */
    public Edonkey() throws NoSuchAlgorithmException {
        super();
        separator = " ";
        encoding = HEX;
        md4 = HashFactory.getInstance(AUX_ALGORITHM);
        if (md4 == null) throw new NoSuchAlgorithmException(AUX_ALGORITHM + " is an unknown algorithm.");
        md4final = HashFactory.getInstance(AUX_ALGORITHM);
        virgin = true;
    }

    public void reset() {
        md4.reset();
        md4final.reset();
        length = 0;
        virgin = true;
    }

    public void update(byte b) {
        md4.update(b);
        length++;
        if ((length % BLOCKSIZE) == 0) {
            System.arraycopy(md4.digest(), 0, edonkeyHash, 0, 16);
            md4final.update(edonkeyHash, 0, 16);
            md4.reset();
        }
    }

    public void update(int b) {
        update((byte) (b & 0xFF));
    }

    public void update(byte[] buffer, int offset, int len) {
        int zuSchreiben = len - offset;
        int passed = (int) (length % BLOCKSIZE);
        int platz = BLOCKSIZE - passed;
        if (platz > zuSchreiben) {
            md4.update(buffer, offset, len);
            length += len;
        } else if (platz == zuSchreiben) {
            md4.update(buffer, offset, len);
            length += len;
            System.arraycopy(md4.digest(), 0, edonkeyHash, 0, 16);
            md4final.update(edonkeyHash, 0, 16);
            md4.reset();
        } else if (platz < zuSchreiben) {
            md4.update(buffer, offset, platz);
            length += platz;
            System.arraycopy(md4.digest(), 0, edonkeyHash, 0, 16);
            md4final.update(edonkeyHash, 0, 16);
            md4.reset();
            md4.update(buffer, offset + platz, zuSchreiben - platz);
            length += zuSchreiben - platz;
        }
    }

    public String toString() {
        return getFormattedValue() + separator + (isTimestampWanted() ? getTimestampFormatted() + separator : "") + getFilename();
    }

    public byte[] getByteArray() {
        if (virgin) {
            if (length < BLOCKSIZE) {
                System.arraycopy(md4.digest(), 0, edonkeyHash, 0, 16);
            } else {
                IMessageDigest md4temp = (IMessageDigest) md4final.clone();
                md4temp.update(md4.digest(), 0, 16);
                System.arraycopy(md4temp.digest(), 0, edonkeyHash, 0, 16);
            }
            virgin = false;
            digest = edonkeyHash;
        }
        byte[] save = new byte[digest.length];
        System.arraycopy(digest, 0, save, 0, digest.length);
        return save;
    }
}
