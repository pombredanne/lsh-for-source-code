package com.wuala.loader2.crypto;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import com.wuala.loader2.copied.Util;

public class SymmetricCBCCrypto {

    private static final SymmetricCBCCrypto instance = new SymmetricCBCCrypto();

    public static final String KEY_GENERATION_ALGO = "AES";

    public static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    public static final String ALGORITHM_NO_PADDING = "AES/CBC/NoPadding";

    private static final int KEY_LEN_BITS = 128;

    public static final int KEY_LEN = KEY_LEN_BITS / 8;

    private Cipher cipher;

    private Cipher noPadding;

    private MessageDigest sha;

    private AlgorithmParameters nullIv;

    private SymmetricCBCCrypto() {
        try {
            cipher = Cipher.getInstance(ALGORITHM);
            noPadding = Cipher.getInstance(ALGORITHM_NO_PADDING);
            sha = MessageDigest.getInstance("SHA-256");
            nullIv = AlgorithmParameters.getInstance("AES");
            nullIv.init(new IvParameterSpec(new byte[KEY_LEN]));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (NoSuchPaddingException e) {
            throw new RuntimeException(e);
        } catch (InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static SymmetricCBCCrypto getInstance() {
        return instance;
    }

    ByteBuffer encrypt(ByteBuffer source, Key key, int seed) {
        return runCipher(cipher, Cipher.ENCRYPT_MODE, source, key, seed);
    }

    ByteBuffer decrypt(ByteBuffer source, Key key, int seed) {
        return runCipher(cipher, Cipher.DECRYPT_MODE, source, key, seed);
    }

    void encrypt(ByteBuffer source, ByteBuffer target, Key key, int seed) {
        runCipher(cipher, Cipher.ENCRYPT_MODE, source, target, key, seed);
    }

    void decrypt(ByteBuffer source, ByteBuffer target, Key key, int seed) {
        runCipher(cipher, Cipher.DECRYPT_MODE, source, target, key, seed);
    }

    ByteBuffer encryptUnpadded(ByteBuffer source, Key key, int seed) {
        return runCipher(noPadding, Cipher.ENCRYPT_MODE, source, key, seed);
    }

    ByteBuffer decryptUnpadded(ByteBuffer source, Key key, int seed) {
        return runCipher(noPadding, Cipher.DECRYPT_MODE, source, key, seed);
    }

    void encryptUnpadded(ByteBuffer source, ByteBuffer target, Key key, int seed) {
        runCipher(noPadding, Cipher.ENCRYPT_MODE, source, target, key, seed);
    }

    void decryptUnpadded(ByteBuffer source, ByteBuffer target, Key key, int seed) {
        runCipher(noPadding, Cipher.DECRYPT_MODE, source, target, key, seed);
    }

    private synchronized void runCipher(Cipher cipher, int mode, ByteBuffer source, ByteBuffer target, Key key, int seed) {
        try {
            cipher.init(mode, key, getIV(key, seed));
            cipher.doFinal(source, target);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (ShortBufferException e) {
            throw new BufferOverflowException();
        } catch (InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized AlgorithmParameters getIV(Key key, int seed) throws NoSuchAlgorithmException, InvalidParameterSpecException {
        if (seed == 0) {
            return nullIv;
        } else {
            AlgorithmParameters params = AlgorithmParameters.getInstance("AES");
            byte[] ivData = new byte[KEY_LEN];
            Util.toBytes(seed, ivData, 0, 4);
            Util.toBytes(seed, ivData, 4, 4);
            Util.toBytes(seed, ivData, 8, 4);
            Util.toBytes(seed, ivData, 12, 4);
            sha.update(key.getEncoded());
            sha.update(ivData);
            params.init(new IvParameterSpec(sha.digest(), 2, 16));
            return params;
        }
    }

    private synchronized ByteBuffer runCipher(Cipher cipher, int mode, ByteBuffer source, Key key, int seed) {
        try {
            cipher.init(mode, key, getIV(key, seed));
            int outSize = cipher.getOutputSize(source.remaining());
            ByteBuffer target = ByteBuffer.allocate(outSize);
            cipher.doFinal(source, target);
            target.flip();
            return target;
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        } catch (IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        } catch (BadPaddingException e) {
            throw new RuntimeException(e);
        } catch (ShortBufferException e) {
            throw new RuntimeException(source.toString(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (InvalidParameterSpecException e) {
            throw new RuntimeException(e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new RuntimeException(e);
        }
    }

    public long predictEncryptedSize(long actualSize) {
        actualSize += 16;
        actualSize -= actualSize % 16;
        return actualSize;
    }

    public static boolean needsPadding(int size) {
        return size % 16 != 0;
    }
}
