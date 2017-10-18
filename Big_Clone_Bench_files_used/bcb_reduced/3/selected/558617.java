package org.neodatis.odb.test.crypto;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoFileIO {

    private static int CRYPTO_BLOCKSIZE = 16;

    private static int BLOCKSIZE = 4 * 16;

    RandomAccessFile _raf = null;

    long _pos = 0;

    Cipher decipher;

    Cipher encipher;

    public CryptoFileIO(String fileName, String access) throws FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        _raf = new RandomAccessFile(fileName, access);
        String pwd = "password";
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        byte[] key = md5.digest(pwd.getBytes());
        SecretKeySpec skey = new SecretKeySpec(key, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(md5.digest(key));
        encipher = Cipher.getInstance("AES/CTR/NoPadding");
        encipher.init(Cipher.ENCRYPT_MODE, skey, ivSpec);
        decipher = Cipher.getInstance("AES/CTR/NoPadding");
        decipher.init(Cipher.DECRYPT_MODE, skey, ivSpec);
    }

    public void seek(long pos) throws IOException {
        _pos = pos;
        _raf.seek(pos);
    }

    public void close() throws IOException {
        _raf.close();
    }

    public void write(byte[] bytes) throws IOException, IllegalBlockSizeException, BadPaddingException {
        long pos = _pos;
        int balength = bytes.length;
        long pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
        byte[] blockbit = new byte[BLOCKSIZE];
        _raf.seek(pos1);
        if (_raf.read(blockbit) > 0) {
            blockbit = decipher.doFinal(blockbit);
        }
        _raf.seek(pos1);
        int len1 = BLOCKSIZE - (int) (pos - pos1);
        if (balength < len1) {
            len1 = balength;
        }
        System.arraycopy(bytes, 0, blockbit, (int) (pos - pos1), len1);
        blockbit = encipher.doFinal(blockbit);
        _raf.write(blockbit);
        pos += len1;
        balength -= len1;
        int blocks = balength / BLOCKSIZE;
        for (int i = 0; i < blocks; i++) {
            pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
            _raf.seek(pos1);
            if (_raf.read(blockbit) > 0) {
                blockbit = decipher.doFinal(blockbit);
            }
            _raf.seek(pos1);
            len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(bytes, bytes.length - balength, blockbit, 0, len1);
            blockbit = encipher.doFinal(blockbit);
            _raf.write(blockbit);
            pos += len1;
            balength -= len1;
        }
        if (balength > 0) {
            blockbit = new byte[BLOCKSIZE];
            pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
            _raf.seek(pos1);
            if (_raf.read(blockbit) > 0) {
                blockbit = decipher.doFinal(blockbit);
            }
            _raf.seek(pos1);
            len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(bytes, bytes.length - balength, blockbit, 0, len1);
            blockbit = encipher.doFinal(blockbit);
            _raf.write(blockbit);
            pos += len1;
            balength -= len1;
        }
        _pos += bytes.length;
    }

    public long read(byte[] bytes) throws IOException, IllegalBlockSizeException, BadPaddingException {
        int totalread = 0;
        long pos = _pos;
        int balength = bytes.length;
        long pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
        byte[] blockbit = new byte[BLOCKSIZE];
        _raf.seek(pos1);
        totalread += _raf.read(blockbit);
        blockbit = decipher.doFinal(blockbit);
        int len1 = BLOCKSIZE - (int) (pos - pos1);
        if (balength < len1) {
            len1 = balength;
        }
        System.arraycopy(blockbit, (int) (pos - pos1), bytes, 0, len1);
        pos += len1;
        balength -= len1;
        int blocks = balength / BLOCKSIZE;
        for (int i = 0; i < blocks; i++) {
            pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
            _raf.seek(pos1);
            totalread += _raf.read(blockbit);
            blockbit = decipher.doFinal(blockbit);
            len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(blockbit, 0, bytes, bytes.length - balength, len1);
            pos += len1;
            balength -= len1;
        }
        if (balength > 0) {
            blockbit = new byte[BLOCKSIZE];
            pos1 = (pos / BLOCKSIZE) * BLOCKSIZE;
            _raf.seek(pos1);
            totalread += _raf.read(blockbit);
            blockbit = decipher.doFinal(blockbit);
            len1 = BLOCKSIZE - (int) (pos - pos1);
            if (balength < len1) {
                len1 = balength;
            }
            System.arraycopy(blockbit, 0, bytes, bytes.length - balength, len1);
            pos += len1;
            balength -= len1;
        }
        _pos += Math.min(totalread, bytes.length);
        return Math.min(totalread, bytes.length);
    }
}
