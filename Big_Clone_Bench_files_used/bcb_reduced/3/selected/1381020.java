package org.softnetwork.io;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * @author $Author: smanciot $
 *
 * @version $Revision: 84 $
 */
public class MessageDigestInputStream extends InputStream {

    InputStream is;

    MessageDigest md;

    String algorithm;

    String provider;

    byte[] digest;

    byte[] buffer;

    boolean FULL = false;

    boolean EMPTY = true;

    int mark = -1;

    int pos = -1;

    /**
	 * End of file indicator.
	 */
    boolean EOF = false;

    /**
	 * Has this stream been closed?
	 */
    boolean isClosed = false;

    /**
	 * The number of bytes which have already been returned by this stream.
	 */
    int count = 0;

    public MessageDigestInputStream(InputStream is) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        this(is, null, null);
    }

    public MessageDigestInputStream(InputStream is, String algorithm) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        this(is, algorithm, null);
    }

    public MessageDigestInputStream(InputStream is, String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
        if (algorithm == null) algorithm = "MD5";
        this.is = is;
        this.algorithm = algorithm;
        this.provider = provider;
        if (provider == null) md = MessageDigest.getInstance(algorithm); else md = MessageDigest.getInstance(algorithm, provider);
    }

    public final int read() throws IOException {
        if (isClosed) throw new IOException();
        if (EOF) return -1;
        int c = -1;
        if (buffer != null) {
            if (FULL) {
                try {
                    return (int) buffer[pos++];
                } catch (ArrayIndexOutOfBoundsException e) {
                    c = is.read();
                    buffer = null;
                }
            } else {
                c = is.read();
                try {
                    buffer[pos++] = (byte) c;
                } catch (ArrayIndexOutOfBoundsException e) {
                    buffer = null;
                }
            }
            EOF = (c >= 0) ? false : true;
            if (!EOF) {
                md.update((byte) c);
                count++;
            }
            return c;
        } else {
            c = is.read();
            EOF = (c >= 0) ? false : true;
            if (!EOF) {
                md.update((byte) c);
                count++;
            }
            return c;
        }
    }

    public final int available() throws IOException {
        return is.available();
    }

    public final void close() throws IOException {
        if (isClosed) throw new IOException();
        digest();
        isClosed = true;
        is.close();
    }

    public synchronized void mark(int readlimit) {
        if (readlimit > 0) {
            mark = count;
            pos = 0;
            buffer = new byte[readlimit];
            FULL = false;
        }
    }

    public boolean markSupported() {
        return true;
    }

    public final int read(byte[] b, int off, int len) throws IOException {
        if (b == null) throw new NullPointerException();
        if (off < 0 || len < 0 || (off > b.length) || (off + len) > b.length) throw new IndexOutOfBoundsException();
        if (len == 0) return 0;
        int c = read();
        if (EOF) return -1;
        b[off] = (byte) c;
        len--;
        int ret = 1;
        while (len > 0 && !EOF) {
            try {
                c = read();
            } catch (IOException e) {
                break;
            }
            if (!EOF) b[off + ret] = (byte) c;
            ret = (EOF) ? ret : ret + 1;
            len--;
        }
        return ret;
    }

    public final int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public synchronized void reset() throws IOException {
        int len = (count - mark);
        if (buffer == null || len > buffer.length) throw new IOException();
        byte[] _temp = new byte[len];
        System.arraycopy(buffer, 0, _temp, 0, len);
        buffer = new byte[len];
        System.arraycopy(_temp, 0, buffer, 0, len);
        FULL = true;
        pos = 0;
    }

    public final long skip(long n) throws IOException {
        if (n <= 0) return 0;
        long ret = 0;
        while (n > 0 && !EOF) {
            read();
            ret = (EOF) ? ret : ret + 1;
            n--;
        }
        return ret;
    }

    public final byte[] digest() {
        if (digest != null) return digest;
        if (!EOF) try {
            while (read() != -1) {
            }
        } catch (IOException e) {
        }
        digest = md.digest();
        return digest;
    }

    public final boolean equals(Object obj) {
        if (obj instanceof MessageDigestInputStream) {
            return MessageDigest.isEqual(digest(), ((MessageDigestInputStream) obj).digest());
        } else if (obj instanceof InputStream) {
            MessageDigestInputStream mdi = null;
            try {
                mdi = new MessageDigestInputStream((InputStream) obj, algorithm, provider);
                return MessageDigest.isEqual(digest(), mdi.digest());
            } catch (NoSuchAlgorithmException e) {
            } catch (NoSuchProviderException e) {
            } catch (IOException e) {
            } finally {
            }
        }
        return false;
    }
}
