package it.unibo.mortemale.cracker.john.hasher;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Sha256Hasher implements IHasher {

    private final String alg_str = "SHA-256";

    public static Sha256Hasher get_Sha256_Hasher() {
        return new Sha256Hasher();
    }

    private Sha256Hasher() {
    }

    @Override
    public String compute_hash(String plaintext) {
        MessageDigest d;
        try {
            d = MessageDigest.getInstance(get_algorithm_name());
            d.update(plaintext.getBytes());
            byte[] hash = d.digest();
            StringBuffer sb = new StringBuffer();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String get_algorithm_name() {
        return alg_str;
    }
}
