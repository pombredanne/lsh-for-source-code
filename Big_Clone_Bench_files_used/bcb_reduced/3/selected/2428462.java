package pipe4j.pipe.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import pipe4j.pipe.SimpleStreamPipe;

/**
 * Digests {@link InputStream} with the configured algorithm and writes
 * resulting hash compuation to {@link OutputStream}. Default algorithm set to
 * MD5.
 * 
 * @author bbennett
 */
public class DigestPipe extends SimpleStreamPipe {

    private static final String HEXES = "0123456789abcdef";

    private static final String defaultAlgorithm = "MD5";

    private String algorithm;

    public DigestPipe() {
        this(defaultAlgorithm);
    }

    public DigestPipe(String algorithm) {
        super();
        this.algorithm = algorithm;
    }

    @Override
    protected void run(InputStream inputStream, OutputStream outputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] buffer = new byte[8192];
        int numRead;
        while (!cancelled() && (numRead = inputStream.read(buffer)) != -1) {
            md.update(buffer, 0, numRead);
        }
        if (cancelled()) {
            return;
        }
        String checksum = getHex(md.digest());
        OutputStreamWriter writer = new OutputStreamWriter(outputStream);
        writer.write(checksum);
        writer.flush();
    }

    private String getHex(byte[] raw) {
        StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
