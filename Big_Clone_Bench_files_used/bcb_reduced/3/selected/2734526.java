package huf.io;

import java.io.InputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This input stream provides methods to read data while maintaining
 * MD5 sum for read bytes.
 */
public class MD5InputStream extends InputStream {

    /** Underlying input stream. */
    private final InputStream in;

    /** MD5-counting helper object. */
    private final MessageDigest md5;

    /**
	 * Create new MD5 input stream.
	 *
	 * @param inputStream underlying input stream
	 */
    public MD5InputStream(InputStream inputStream) {
        this.in = inputStream;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Unsupported MessageDigest algorithm: MD5");
        }
    }

    /** Temporary variable used in read() method. */
    private int b = 0;

    @Override
    public int read() throws IOException {
        b = in.read();
        if (b >= 0) {
            md5.update((byte) (b & 0xff));
        }
        return b;
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
