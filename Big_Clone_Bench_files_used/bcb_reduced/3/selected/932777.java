package org.exist.versioning.svn.internal.wc.admin;

import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.exist.versioning.svn.internal.wc.SVNFileUtil;

/**
 * @version 1.3
 * @author  TMate Software Ltd.
 */
public class SVNChecksumOutputStream extends OutputStream {

    public static final String MD5_ALGORITHM = "MD5";

    private OutputStream myTarget;

    private MessageDigest myDigest;

    private byte[] myDigestResult;

    private boolean myCloseTarget;

    public SVNChecksumOutputStream(OutputStream target, String algorithm, boolean closeTarget) {
        myTarget = target;
        myCloseTarget = closeTarget;
        algorithm = algorithm == null ? MD5_ALGORITHM : algorithm;
        try {
            myDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
        }
    }

    public void write(int b) throws IOException {
        myDigest.update((byte) (b & 0xFF));
        myTarget.write(b);
    }

    public void write(byte[] b) throws IOException {
        if (b != null) {
            myDigest.update(b);
        }
        myTarget.write(b);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (b != null) {
            myDigest.update(b, off, len);
        }
        myTarget.write(b, off, len);
    }

    public void close() throws IOException {
        if (myDigestResult == null) {
            myDigestResult = myDigest.digest();
        }
        if (myCloseTarget) {
            myTarget.close();
        }
    }

    public String getDigest() {
        if (myDigestResult == null) {
            myDigestResult = myDigest.digest();
        }
        return SVNFileUtil.toHexDigest(myDigestResult);
    }
}
