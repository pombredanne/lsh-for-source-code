package uk.co.marcoratto.checksum;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import uk.co.marcoratto.file.ReaderInputStream;
import uk.co.marcoratto.util.Utility;

public class GenericChecksum {

    protected static final int BUFFER_SIZE = 1 * 1024 * 1024;

    protected MessageDigest md = null;

    private static GenericChecksum instance = null;

    private GenericChecksum(String algorithm) throws ChecksumException {
        try {
            this.md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new ChecksumException(e);
        }
    }

    public static final GenericChecksum getInstance(String algorithm) throws ChecksumException {
        if (instance == null) {
            instance = new GenericChecksum(algorithm);
        }
        return instance;
    }

    public byte[] encode(String s) {
        if (s == null) {
            return null;
        }
        return encode(s.getBytes());
    }

    public byte[] encode(byte[] buffer) {
        return this.md.digest(buffer);
    }

    public byte[] encodeBinary(Reader reader) {
        if (reader == null) {
            return null;
        }
        byte[] fileDigest = null;
        try {
            ReaderInputStream ris = new ReaderInputStream(reader);
            byte[] buf = new byte[BUFFER_SIZE];
            this.md.reset();
            DigestInputStream dis = new DigestInputStream(ris, this.md);
            dis.on(true);
            while (dis.read(buf, 0, BUFFER_SIZE) != -1) ;
            dis.close();
            ris.close();
            ris = null;
            fileDigest = this.md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fileDigest;
    }

    public byte[] encodeBinary(File file) {
        if (file == null) {
            return null;
        }
        byte[] fileDigest = null;
        try {
            if (!file.canRead()) {
                return null;
            }
            FileInputStream fis = null;
            byte[] buf = new byte[BUFFER_SIZE];
            this.md.reset();
            fis = new FileInputStream(file);
            DigestInputStream dis = new DigestInputStream(fis, this.md);
            dis.on(true);
            while (dis.read(buf, 0, BUFFER_SIZE) != -1) ;
            dis.close();
            fis.close();
            fis = null;
            fileDigest = this.md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fileDigest;
    }

    public byte[] encodeText(File file) {
        if (file == null) {
            return null;
        }
        byte[] fileDigest = null;
        try {
            if (!file.canRead()) {
                return null;
            }
            FileInputStream fis = null;
            fis = new FileInputStream(file);
            String riga = "";
            BufferedReader br = new BufferedReader(new InputStreamReader(fis));
            this.md.reset();
            while ((riga = br.readLine()) != null) {
                riga += Utility.LINE_SEP;
                this.md.update(riga.getBytes());
            }
            fis.close();
            fis = null;
            fileDigest = this.md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fileDigest;
    }

    public byte[] encodeText(Reader reader) {
        if (reader == null) {
            return null;
        }
        byte[] fileDigest = null;
        try {
            ReaderInputStream ris = new ReaderInputStream(reader);
            BufferedReader br = new BufferedReader(new InputStreamReader(ris));
            this.md.reset();
            String riga = "";
            while ((riga = br.readLine()) != null) {
                riga += Utility.LINE_SEP;
                this.md.update(riga.getBytes());
            }
            ris.close();
            ris = null;
            fileDigest = this.md.digest();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return fileDigest;
    }

    public static final String getHexString(byte[] b) {
        StringBuffer result = new StringBuffer("");
        for (int i = 0; i < b.length; i++) {
            result.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return result.toString();
    }
}
