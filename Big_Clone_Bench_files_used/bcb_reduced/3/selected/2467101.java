package jpdstore;

import java.io.*;
import java.security.*;
import javax.crypto.*;
import java.util.*;
import java.util.zip.*;
import org.bouncycastle.crypto.generators.*;

public class ReferenceCryptoAdapter implements CryptoAdapter {

    private static final String cryptProvider = "BC";

    private static final int RAW_SIZE = 104 * 1024, PAYLOAD_SIZE = 100 * 1024;

    private static final int IV_LENGTH = 16;

    private Random rnd;

    public ReferenceCryptoAdapter() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        rnd = new SecureRandom();
    }

    public int getPayloadBlockSize() {
        return PAYLOAD_SIZE;
    }

    public int getRawBlockSize() {
        return RAW_SIZE;
    }

    public String getFilename(String password, int index) throws IOException {
        MessageDigest sha1;
        try {
            sha1 = MessageDigest.getInstance("SHA1", cryptProvider);
        } catch (GeneralSecurityException ex) {
            throw new IOException(ex.toString());
        }
        byte[] digest = sha1.digest((password + "-" + index).getBytes("UTF-8"));
        StringBuffer sb = new StringBuffer(digest.length * 2);
        for (int i = 0; i < digest.length; i++) {
            int b = digest[i] & 0xff;
            sb.append(b < 16 ? "0" : "").append(Integer.toHexString(b));
        }
        return sb.toString() + ".jpds";
    }

    public InputStream getInputStream(String password, File file, int blockCount) throws IOException {
        List streams = new ArrayList();
        for (int i = 0; i < blockCount; i++) {
            streams.add(getBlockInputStream(password, file, i));
        }
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    private InputStream getBlockInputStream(String password, File file, int index) throws IOException {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(file));
            if (index > 0) {
                in.skip(RAW_SIZE * index);
            }
            byte[] iv = new byte[IV_LENGTH], salt = new byte[16];
            in.readFully(salt);
            in.readFully(iv);
            InputStream cin = new BufferedInputStream(new CipherInputStream(in, getCipher(password, Cipher.DECRYPT_MODE, salt, iv)));
            while (cin.read() > 0) {
            }
            ;
            int type = cin.read();
            if (type == -1) {
                throw new IOException("No data");
            } else if (type == 42) {
                return new GZIPInputStream(cin);
            } else {
                throw new IOException("Incorrect type: " + type);
            }
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            throw new IOException(ex.toString());
        }
    }

    public void storeData(byte[] data, String password, File file, int blockCount) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        for (int i = 0; i < blockCount; i++) {
            raf.seek(i * RAW_SIZE);
            int start = i * PAYLOAD_SIZE;
            int len = data.length - start;
            if (len > PAYLOAD_SIZE && i < blockCount - 1) len = PAYLOAD_SIZE;
            if (len < 0) {
                start = 0;
                len = 0;
            }
            storeDataBlock(data, start, len, password, raf);
        }
        raf.setLength(RAW_SIZE * blockCount);
        raf.close();
        file.setLastModified(Controller.LAST_MODIFIED_DATE);
    }

    public void storeDataBlock(byte[] allData, int offs, int len, String password, RandomAccessFile raf) throws IOException {
        byte[] iv, data, salt = new byte[16];
        rnd.nextBytes(salt);
        try {
            Cipher cip = getCipher(password, Cipher.ENCRYPT_MODE, salt, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            OutputStream out = new GZIPOutputStream(baos);
            out.write(allData, offs, len);
            out.close();
            data = baos.toByteArray();
            baos.reset();
            out = new CipherOutputStream(baos, cip);
            if (data.length >= RAW_SIZE - IV_LENGTH - 17) throw new IOException("Data too large");
            out.write(makePadding(RAW_SIZE - data.length - IV_LENGTH - 17));
            out.write(42);
            out.write(data);
            out.close();
            data = baos.toByteArray();
            iv = cip.getIV();
        } catch (GeneralSecurityException ex) {
            ex.printStackTrace();
            throw new IOException(ex.toString());
        }
        raf.write(salt);
        raf.write(iv);
        raf.write(data);
        if (salt.length + iv.length + data.length != RAW_SIZE) {
            throw new RuntimeException("Assertion failed!");
        }
    }

    private Cipher getCipher(String password, int mode, byte[] salt, byte[] iv) throws IOException, GeneralSecurityException {
        PKCS5S2ParametersGenerator g = new PKCS5S2ParametersGenerator();
        g.init(PKCS5S2ParametersGenerator.PKCS5PasswordToBytes(password.toCharArray()), salt, 300);
        org.bouncycastle.crypto.CipherParameters cp = g.generateDerivedParameters(128);
        byte[] keyData = ((org.bouncycastle.crypto.params.KeyParameter) cp).getKey();
        Key key = new javax.crypto.spec.SecretKeySpec(keyData, "AES");
        Cipher cip = Cipher.getInstance("AES/CBC/NoPadding", "BC");
        if (iv == null) {
            cip.init(mode, key);
        } else {
            cip.init(mode, key, new javax.crypto.spec.IvParameterSpec(iv));
        }
        return cip;
    }

    private byte[] makePadding(int len) {
        byte[] padding = new byte[len];
        rnd.nextBytes(padding);
        for (int i = 0; i < len; i++) {
            while (padding[i] == 0) {
                padding[i] = 1;
            }
        }
        padding[len - 1] = 0;
        return padding;
    }

    public String getGarbagePassword() {
        byte[] buf = new byte[32];
        rnd.nextBytes(buf);
        try {
            return new String(buf, "ISO-8859-1");
        } catch (UnsupportedEncodingException ex) {
            return new String(buf);
        }
    }

    public boolean isFile(File f) {
        return f.getName().endsWith(".jpds") && f.getName().length() == 45;
    }
}
