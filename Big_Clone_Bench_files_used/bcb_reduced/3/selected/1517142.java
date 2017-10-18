package uk.ac.ebi.mg.xchg.upload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDigest {

    private byte[] digest;

    public FileDigest(File f, long offset, long len) throws IOException {
        computeDigest(f, offset, len);
    }

    public FileDigest(File f) throws IOException {
        computeDigest(f, 0, f.length());
    }

    public byte[] getDigest() {
        return digest;
    }

    private void computeDigest(File f, long offset, long len) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(Constants.hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
        }
        FileInputStream fis = new FileInputStream(f);
        fis.skip(offset);
        byte[] buffer = new byte[2000];
        long count = 0;
        int l;
        while ((l = fis.read(buffer, 0, (int) (buffer.length < len - count ? buffer.length : len - count))) > 0 && count < len) {
            md.update(buffer, 0, l);
            count += l;
        }
        digest = md.digest();
    }
}
