package org.webcastellum;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public final class CryptoUtils {

    private CryptoUtils() {
    }

    private static final String CIPHER_DATA = "AES";

    private static final String CIPHER_KEY = "AES";

    private static final String DIGEST = "SHA-1";

    private static final int KEY_SIZE = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final Random RANDOM = new Random();

    private static int digestLength = -1;

    private static final char[] ALPHABET = new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };

    private static String internalToHexString(final byte b) {
        final int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        String ret = (value < 16 ? "0" : "");
        ret += Integer.toHexString(value).toUpperCase();
        return ret;
    }

    private static String[] hexStringLookup = new String[256];

    static {
        for (byte b = Byte.MIN_VALUE; b <= Byte.MAX_VALUE; b++) {
            hexStringLookup[b + 128] = internalToHexString(b);
            if (b == Byte.MAX_VALUE) break;
        }
    }

    public static String toHexString(final byte b) {
        return hexStringLookup[b + 128];
    }

    public static byte toByteValue(String hex) {
        final int length = hex.length();
        if (length < 1 || length > 2) throw new IllegalArgumentException("hex must be at max a two-digit hex value like B1");
        return (byte) Integer.parseInt(hex, 16);
    }

    public static int toIntValue(String hex) {
        final int length = hex.length();
        if (length < 1 || length > 8) throw new IllegalArgumentException("hex must be at max a eight-digit hex value like ABCDEF12");
        return Integer.parseInt(hex, 16);
    }

    static final WordDictionary UNWANTED_RANDOM_CONTENT = new WordDictionary("etc passwd exe bin format select insert delete union update drop shit fuck sex cmd 0d0a x00 dba admin");

    public static String generateRandomToken(final boolean secure) {
        return generateRandomToken(secure, (secure ? SECURE_RANDOM : RANDOM).nextInt(3) + 7);
    }

    public static String generateRandomToken(final boolean secure, final int length) {
        StringBuilder result = null;
        for (int x = 0; x < 100; x++) {
            result = new StringBuilder(length);
            final int max = ALPHABET.length - 1;
            for (int i = 0; i < length; i++) {
                result.append(ALPHABET[generateRandomNumber(secure, 0, max)]);
            }
            if (!WordMatchingUtils.matchesWord(UNWANTED_RANDOM_CONTENT, result.toString(), WebCastellumFilter.TRIE_MATCHING_THRSHOLD)) break;
        }
        return result.toString();
    }

    public static byte[] generateRandomBytes(final boolean secure) {
        return generateRandomBytes(secure, (secure ? SECURE_RANDOM : RANDOM).nextInt(8) + 10);
    }

    public static byte[] generateRandomBytes(final boolean secure, final int length) {
        final byte[] result = new byte[length];
        (secure ? SECURE_RANDOM : RANDOM).nextBytes(result);
        return result;
    }

    public static int generateRandomNumber(final boolean secure) {
        return (secure ? SECURE_RANDOM : RANDOM).nextInt();
    }

    public static int generateRandomNumber(final boolean secure, final int low, final int high) {
        if (low >= high) throw new IllegalArgumentException("Low value must be lower than high value (low=" + low + " and high=" + high + ")");
        final int difference = high - low;
        return low + (secure ? SECURE_RANDOM : RANDOM).nextInt(difference);
    }

    public static int getHashLength() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance(DIGEST).getDigestLength();
    }

    public static byte[] hash(final byte[] saltBefore, final String content, final byte[] saltAfter, final int repeatedHashingCount) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (content == null) return null;
        final MessageDigest digest = MessageDigest.getInstance(DIGEST);
        if (digestLength == -1) digestLength = digest.getDigestLength();
        for (int i = 0; i < repeatedHashingCount; i++) {
            if (i > 0) digest.update(digest.digest());
            digest.update(saltBefore);
            digest.update(content.getBytes(WebCastellumFilter.DEFAULT_CHARACTER_ENCODING));
            digest.update(saltAfter);
        }
        return digest.digest();
    }

    public static Cipher getCipher() throws NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance(CIPHER_DATA);
    }

    public static CryptoKeyAndSalt generateRandomCryptoKeyAndSalt(final boolean extraHashingProtection) throws NoSuchAlgorithmException {
        final KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_KEY);
        keyGenerator.init(KEY_SIZE);
        final CryptoKeyAndSalt key;
        final SecretKey secret = keyGenerator.generateKey();
        if (extraHashingProtection) {
            final byte[] saltBefore = generateRandomBytes(true);
            final byte[] saltAfter = generateRandomBytes(true);
            final int repeatedHashingCount = generateRandomNumber(true, 2, 5);
            key = new CryptoKeyAndSalt(saltBefore, secret, saltAfter, repeatedHashingCount);
        } else {
            key = new CryptoKeyAndSalt(secret);
        }
        return key;
    }

    public static String encryptURLSafe(final String content, final CryptoKeyAndSalt key, Cipher cipher) throws InvalidKeyException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, UnsupportedEncodingException {
        if (cipher == null) cipher = getCipher();
        cipher.init(Cipher.ENCRYPT_MODE, key.getKey(), SECURE_RANDOM);
        final byte[] payloadAndHash;
        if (key.isExtraHashingProtection()) {
            final byte[] payload = cipher.update(content.getBytes(WebCastellumFilter.DEFAULT_CHARACTER_ENCODING));
            final byte[] hash = cipher.doFinal(hash(key.getSaltBefore(), content, key.getSaltAfter(), key.getRepeatedHashingCount()));
            payloadAndHash = new byte[payload.length + hash.length];
            System.arraycopy(payload, 0, payloadAndHash, 0, payload.length);
            System.arraycopy(hash, 0, payloadAndHash, payload.length, hash.length);
        } else {
            payloadAndHash = cipher.doFinal(content.getBytes(WebCastellumFilter.DEFAULT_CHARACTER_ENCODING));
        }
        return Base64Utils.encode(payloadAndHash);
    }

    public static String decryptURLSafe(String content, final CryptoKeyAndSalt key) throws IllegalBlockSizeException, InvalidKeyException, NoSuchAlgorithmException, BadPaddingException, NoSuchPaddingException, UnsupportedEncodingException {
        if (content == null) throw new NullPointerException("content must not be null");
        if (key == null) throw new NullPointerException("key must not be null");
        final Cipher cipher = Cipher.getInstance(CIPHER_DATA);
        cipher.init(Cipher.DECRYPT_MODE, key.getKey(), SECURE_RANDOM);
        if (content.indexOf('%') >= 0) content = ServerUtils.urlDecode(content);
        final byte[] decryptedBytes = cipher.doFinal(Base64Utils.decode(content));
        final int stop;
        final byte[] hash;
        if (key.isExtraHashingProtection()) {
            final int signatureHexLength = digestLength > 0 ? digestLength : 16;
            hash = new byte[signatureHexLength];
            stop = decryptedBytes.length - signatureHexLength;
            System.arraycopy(decryptedBytes, stop, hash, 0, signatureHexLength);
        } else {
            stop = decryptedBytes.length;
            hash = null;
        }
        final String url = new String(decryptedBytes, 0, stop, WebCastellumFilter.DEFAULT_CHARACTER_ENCODING);
        if (key.isExtraHashingProtection()) {
            final byte[] expectedHash = hash(key.getSaltBefore(), url, key.getSaltAfter(), key.getRepeatedHashingCount());
            if (!Arrays.equals(hash, expectedHash)) throw new IllegalArgumentException("Hash of decrypted value does not match");
        }
        return url;
    }

    public static String bytesToHex(final byte[] bytes) {
        if (bytes == null) return null;
        if (bytes.length == 0) return "";
        final StringBuilder result = new StringBuilder(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            result.append(toHexString(bytes[i]));
        }
        return result.toString();
    }

    public static byte[] hexToBytes(final String hex) {
        byte[] bts = new byte[hex.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    public static byte[] compress(final byte[] input) {
        final Deflater compressor = new Deflater();
        compressor.setLevel(Deflater.BEST_COMPRESSION);
        compressor.setInput(input);
        compressor.finish();
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream(input.length);
            byte[] buf = new byte[1024];
            while (!compressor.finished()) {
                int count = compressor.deflate(buf);
                bos.write(buf, 0, count);
            }
        } finally {
            if (bos != null) try {
                bos.close();
            } catch (IOException ignored) {
            }
        }
        return bos.toByteArray();
    }

    public static byte[] decompress(final byte[] input) throws DataFormatException {
        final Inflater decompressor = new Inflater();
        decompressor.setInput(input);
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream(input.length);
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int count = decompressor.inflate(buf);
                bos.write(buf, 0, count);
            }
        } finally {
            if (bos != null) try {
                bos.close();
            } catch (IOException ignored) {
            }
        }
        return bos.toByteArray();
    }
}
