package net.krecan.ec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class HashBreaker {

    private final byte[] hash;

    private final byte[] startInput;

    private final byte[] endInput;

    private final MessageDigest digest;

    public HashBreaker(byte[] hash, byte[] startInput, byte[] endInput) {
        this(hash, startInput, endInput, createMD5Digest());
    }

    public HashBreaker(byte[] hash, byte[] startInput, byte[] endInput, MessageDigest digest) {
        super();
        this.hash = hash;
        this.startInput = startInput;
        this.endInput = endInput;
        this.digest = digest;
    }

    public byte[] findCollisionForHash() {
        byte[] input = startInput.clone();
        do {
            if (Arrays.equals(hash, digest.digest(input))) {
                return input;
            }
            input = generateNextInput(input);
        } while (!Arrays.equals(input, endInput));
        return null;
    }

    /**
	 * Generates next input. If possible changes current input but creates new array if needed (overflow].
	 * @param input
	 * @return
	 */
    static byte[] generateNextInput(byte[] input) {
        return addOne(input, 0);
    }

    private static byte[] addOne(byte[] input, int i) {
        if (input.length <= i) {
            input = Arrays.copyOf(input, input.length + 1);
        }
        if (input[i] < 127) {
            input[i]++;
            return input;
        } else {
            input[i] = 0;
            return addOne(input, i + 1);
        }
    }

    private static MessageDigest createMD5Digest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
