package net.sourceforge.epoint.io;

import java.io.OutputStream;
import java.io.IOException;
import java.security.MessageDigest;

/**
 * <code>OutputStream</code> for calculating SHA1 checksums
 *
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class SHA1OutputStream extends ChecksumOutputStream {

    private MessageDigest md;

    public SHA1OutputStream() throws java.security.NoSuchAlgorithmException {
        md = MessageDigest.getInstance("SHA1");
    }

    public void write(int b) throws IOException {
        md.update((byte) b);
    }

    public void reset() {
        md.reset();
    }

    public byte[] getChecksum() {
        return md.digest();
    }
}
