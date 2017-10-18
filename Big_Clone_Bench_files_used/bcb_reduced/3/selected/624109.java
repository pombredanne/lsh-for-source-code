package org.openconcerto.utils.sync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public class HashWriter {

    public static int blockSize = 1024;

    private File in;

    public HashWriter(File inputFile) {
        this.in = inputFile;
    }

    public void saveHash(File outputFile) {
        try {
            if (!outputFile.exists()) {
                new File(outputFile.getParent()).mkdirs();
            }
            DataOutputStream bOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            bOut.writeInt((int) this.in.length());
            System.out.println("FileSize:" + this.in.length());
            MessageDigest hashSum = MessageDigest.getInstance("SHA-256");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BufferedInputStream fb = new BufferedInputStream(new FileInputStream(in));
            RollingChecksum32 r32 = new RollingChecksum32();
            byte[] buffer = new byte[blockSize];
            int readSize = fb.read(buffer);
            while (readSize > 0) {
                r32.check(buffer, 0, readSize);
                md5.reset();
                md5.update(buffer, 0, readSize);
                hashSum.update(buffer, 0, readSize);
                readSize = fb.read(buffer);
                final byte[] engineDigest = md5.digest();
                bOut.writeInt(r32.getValue());
                bOut.write(engineDigest);
            }
            byte[] fileHash = new byte[hashSum.getDigestLength()];
            fileHash = hashSum.digest();
            bOut.write(fileHash);
            bOut.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] getHash(File f) throws Exception {
        MessageDigest hashSum = MessageDigest.getInstance("SHA-256");
        final BufferedInputStream fb = new BufferedInputStream(new FileInputStream(f));
        byte[] buffer = new byte[blockSize];
        int readSize = fb.read(buffer);
        while (readSize > 0) {
            hashSum.update(buffer, 0, readSize);
            readSize = fb.read(buffer);
        }
        fb.close();
        byte[] fileHash = new byte[hashSum.getDigestLength()];
        fileHash = hashSum.digest();
        return fileHash;
    }

    public static boolean compareHash(byte[] h1, byte[] h2) {
        final int length = h1.length;
        if (length != h2.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (h1[i] != h2[i]) {
                return false;
            }
        }
        return true;
    }
}
