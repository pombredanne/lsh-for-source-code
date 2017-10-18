package huf.io;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This output stream provides methods to write data while maintaining
 * MD5 sum for written bytes.
 */
public class MD5OutputStream extends OutputStream {

    /** Underlying output stream. */
    private final OutputStream out;

    /** MD5-counting helper object. */
    private final MessageDigest md5;

    /**
	 * Create new MD5 output stream.
	 *
	 * @param outputStream underlying output stream
	 */
    public MD5OutputStream(OutputStream outputStream) {
        this.out = outputStream;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported MessageDigest algorithm: MD5");
        }
    }

    @Override
    public void write(int b) throws IOException {
        out.write(b);
        md5.update((byte) (b & 0xff));
    }

    /**
	 * Get MD5 sum (MD5 COMPUTATION IS RESET AFTER THIS OPERATION).
	 *
	 * @return computed MD5 sum
	 */
    public byte[] getMD5() {
        return md5.digest();
    }
}
