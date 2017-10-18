package com.myJava.file.driver.contenthash;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.myJava.configuration.FrameworkConfiguration;

public class ContentHashOutputStream extends OutputStream {

    private static final String HASH_ALGORITHM = FrameworkConfiguration.getInstance().getFileHashAlgorithm();

    private OutputStream out;

    private MessageDigest dg;

    public ContentHashOutputStream(OutputStream out) throws NoSuchAlgorithmException {
        super();
        this.out = out;
        this.dg = MessageDigest.getInstance(HASH_ALGORITHM);
    }

    public void close() throws IOException {
        out.write(dg.digest());
        this.flush();
        out.close();
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void write(byte[] b, int off, int len) throws IOException {
        update(b, off, len);
    }

    public void write(byte[] b) throws IOException {
        update(b, 0, b.length);
    }

    public void write(int b) throws IOException {
        update(new byte[] { (byte) b }, 0, 1);
    }

    private void update(byte[] buff, int off, int len) {
        dg.update(buff, off, len);
    }
}
