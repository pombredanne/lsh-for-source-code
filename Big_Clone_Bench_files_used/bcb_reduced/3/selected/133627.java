package webmoney.cryptography;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.Random;

public final class Signer {

    private static final int INT_SIZE = 32;

    private static final int SHORT_SIZE = 16;

    private static final int BYTE_SIZE = 8;

    private static final int HASH_SIZE = 128;

    private static final byte[] HEADER = new byte[] { 0x38, 0 };

    private static final byte[] TAIL = new byte[] { 1, 0 };

    private final MessageDigest messageDigest;

    private final int[] exponent;

    private final int[] modulus;

    private final int keyLength;

    private final Random random = new Random();

    public Signer(byte[] exponent, byte[] modulus) {
        int exponentLength = Algebra.significance(exponent);
        int modulusLength = Algebra.significance(modulus);
        this.exponent = new int[(exponentLength - 1) / (INT_SIZE / BYTE_SIZE) + 1];
        this.modulus = new int[(modulusLength - 1) / (INT_SIZE / BYTE_SIZE) + 1];
        Buffer.blockCopy(exponent, 0, this.exponent, 0, exponentLength);
        Buffer.blockCopy(modulus, 0, this.modulus, 0, modulusLength);
        this.keyLength = modulusLength * BYTE_SIZE;
        messageDigest = new MD4();
    }

    public final String sign(String message) throws UnsupportedEncodingException {
        return sign(message, true);
    }

    public final String sign(String message, boolean randomEnable) throws UnsupportedEncodingException {
        if (message.indexOf('\r') >= 0) throw new IllegalArgumentException("Message cannot contain of the following character: '\r'.");
        byte[] bytes = message.getBytes("windows-1251");
        int[] signature = sign(bytes, randomEnable);
        StringBuilder stringBuilder = new StringBuilder(modulus.length * 2 + 1);
        for (int pos = 0; pos < this.keyLength / SHORT_SIZE; pos++) {
            if (signature.length > pos / (INT_SIZE / SHORT_SIZE)) {
                int shift = (0 == pos % (INT_SIZE / SHORT_SIZE)) ? 0 : SHORT_SIZE;
                stringBuilder.append(String.format("%04x", (short) (signature[pos / (INT_SIZE / SHORT_SIZE)] >>> shift)));
            } else {
                stringBuilder.append(String.format("%04x", 0));
            }
        }
        return stringBuilder.toString();
    }

    public final int[] sign(byte[] message, boolean randomEnable) {
        messageDigest.reset();
        byte[] hash = messageDigest.digest(message);
        byte[] rndBuffer = new byte[keyLength / BYTE_SIZE - HEADER.length - HASH_SIZE / BYTE_SIZE - TAIL.length];
        if (randomEnable) random.nextBytes(rndBuffer);
        int[] blob = new int[(this.keyLength / BYTE_SIZE + INT_SIZE / BYTE_SIZE - 1) / (INT_SIZE / BYTE_SIZE)];
        Buffer.blockCopy(HEADER, 0, blob, 0, HEADER.length);
        Buffer.blockCopy(hash, 0, blob, HEADER.length, HASH_SIZE / BYTE_SIZE);
        Buffer.blockCopy(rndBuffer, 0, blob, HEADER.length + HASH_SIZE / BYTE_SIZE, rndBuffer.length);
        Buffer.blockCopy(TAIL, 0, blob, HEADER.length + HASH_SIZE / BYTE_SIZE + rndBuffer.length, TAIL.length);
        return Montgomery.montgomeryExponentiation(blob, exponent, modulus);
    }
}
