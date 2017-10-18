package br.biofoco.p2p.peer;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import com.google.common.base.Preconditions;

public final class IDFactory {

    private static final Random random = new Random(System.nanoTime());

    private static MessageDigest md;

    static {
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private IDFactory() {
    }

    public static ID newID(byte[] input) {
        Preconditions.checkNotNull(input, "Seed data cannot be null!");
        if (input.length == 0) throw new IllegalArgumentException("Seed data cannot be empty!");
        byte[] digested = digest(input);
        return new ID(digested);
    }

    public static ID fromString(String input) {
        return new ID(new BigInteger(input, 16).abs());
    }

    private static byte[] digest(byte[] input) {
        md.reset();
        md.update(input);
        return md.digest();
    }

    public static ID newRandomID() {
        BigInteger seed = new BigInteger("" + random.nextLong());
        return new ID(seed.abs());
    }
}
