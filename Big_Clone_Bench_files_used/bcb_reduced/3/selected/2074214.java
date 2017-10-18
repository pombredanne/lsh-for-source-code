package org.lhuillier.pwsafe.io.v3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.engines.TwofishEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.lhuillier.pwsafe.io.codec.BinConverter;
import com.google.inject.Inject;

class CryptoImpl implements Crypto {

    private final BinConverter decoder;

    @Inject
    CryptoImpl(BinConverter decoder) {
        this.decoder = decoder;
    }

    @Override
    public byte[] calculateHmac(byte[] plaintext, byte[] hmacKey) {
        HMac mac = new HMac(new SHA256Digest());
        mac.init(new KeyParameter(hmacKey));
        for (int i = 0; i + 16 < plaintext.length; ) {
            int len = decoder.readInt(plaintext, i);
            byte[] rawBytes = decoder.slice(plaintext, i + 5, len);
            mac.update(rawBytes, 0, rawBytes.length);
            i = nextBlockOffset(i, len);
        }
        byte[] vhmac = new byte[mac.getUnderlyingDigest().getDigestSize()];
        mac.doFinal(vhmac, 0);
        return vhmac;
    }

    /**
     * From a given offset and length of the field's data, determines what the
     * offset would be for the next record.
     * <p>
     * Each field is stored in blocks of 16 bytes, and there are an additional
     * 5 bytes for the type ID and length.
     */
    private int nextBlockOffset(int currentBlockOffset, int dataLength) {
        int fieldLength = 5 + dataLength;
        int padding = fieldLength % 16 == 0 ? 0 : (16 - (fieldLength % 16));
        return currentBlockOffset + fieldLength + padding;
    }

    @Override
    public byte[] stretchKey(byte[] pw, byte[] salt, int iterations) {
        byte[] stretchedKey = saltKey(pw, salt);
        MessageDigest messageDigest = sha256Digest();
        for (int i = 0; i <= iterations; i++) {
            messageDigest.update(stretchedKey);
            stretchedKey = messageDigest.digest();
        }
        messageDigest.update(stretchedKey);
        return stretchedKey;
    }

    private byte[] saltKey(byte[] pw, byte[] salt) {
        int keylen = pw.length;
        byte[] result = new byte[keylen + 32];
        System.arraycopy(pw, 0, result, 0, keylen);
        System.arraycopy(salt, 0, result, keylen, 32);
        return result;
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("SHA-256 is not available");
        }
    }

    @Override
    public byte[] digestKey(byte[] stretchedPw) {
        MessageDigest messageDigest = sha256Digest();
        messageDigest.update(stretchedPw);
        return messageDigest.digest();
    }

    @Override
    public byte[] decryptKey(byte[] data, byte[] stretchedKey) {
        TwofishEngine cipher = new TwofishEngine();
        KeyParameter kp = new KeyParameter(stretchedKey);
        cipher.init(false, kp);
        return processKey(data, cipher);
    }

    @Override
    public byte[] encryptKey(byte[] data, byte[] stretchedKey) {
        TwofishEngine cipher = new TwofishEngine();
        KeyParameter kp = new KeyParameter(stretchedKey);
        cipher.init(true, kp);
        return processKey(data, cipher);
    }

    private byte[] processKey(byte[] data, TwofishEngine cipher) {
        byte[] result = new byte[32];
        byte[] block = new byte[16];
        cipher.processBlock(decoder.slice(data, 0, 16), 0, block, 0);
        System.arraycopy(block, 0, result, 0, 16);
        cipher.processBlock(decoder.slice(data, 16, 16), 0, block, 0);
        System.arraycopy(block, 0, result, 16, 16);
        return result;
    }

    @Override
    public byte[] decryptRecords(byte[] data, byte[] recordKey, byte[] iv) {
        CBCBlockCipher cipher = initCipher(recordKey, iv, false);
        return processRecords(data, cipher);
    }

    private CBCBlockCipher initCipher(byte[] recordKey, byte[] iv, boolean encrypt) {
        TwofishEngine tfe = new TwofishEngine();
        CBCBlockCipher cipher = new CBCBlockCipher(tfe);
        KeyParameter kp = new KeyParameter(recordKey);
        ParametersWithIV piv = new ParametersWithIV(kp, iv);
        cipher.init(encrypt, piv);
        return cipher;
    }

    @Override
    public byte[] encryptRecords(byte[] data, byte[] recordKey, byte[] iv) {
        CBCBlockCipher cipher = initCipher(recordKey, iv, true);
        return processRecords(data, cipher);
    }

    private byte[] processRecords(byte[] data, CBCBlockCipher cipher) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i += 16) {
            byte[] inBlock = decoder.slice(data, i, 16);
            byte[] outBlock = new byte[16];
            cipher.processBlock(inBlock, 0, outBlock, 0);
            System.arraycopy(outBlock, 0, result, i, 16);
        }
        return result;
    }
}
