package com.luis.db.android.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.Random;

/**
 *
 * @author luis
 */
public class EntityUtils {

    private Random random;

    private String alphabet;

    private String digits;

    public EntityUtils() {
        this.alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        this.digits = "0123456789";
        this.random = new Random();
    }

    public String generateLetters(int length) {
        String ref = "";
        for (int count = 0; count < length; count++) {
            int pos = random.nextInt(alphabet.length());
            ref += alphabet.charAt(pos);
        }
        return ref;
    }

    public String generateNumbers(int length) {
        String ref = "";
        for (int count = 0; count < length; count++) {
            int pos = random.nextInt(digits.length());
            ref += digits.charAt(pos);
        }
        return ref;
    }

    public String generateLettersAndNumbers(int length) {
        int letters = random.nextInt(length);
        int numbers = length - letters;
        String ref = "";
        for (int count = 0; count < letters; count++) {
            int pos = random.nextInt(alphabet.length());
            ref += alphabet.charAt(pos);
        }
        for (int count = 0; count < numbers; count++) {
            int pos = random.nextInt(digits.length());
            ref += digits.charAt(pos);
        }
        char[] auxref = ref.toCharArray();
        for (int count = 0; count < letters + numbers; count++) {
            int pos = random.nextInt(ref.length());
            int index = random.nextInt(ref.length());
            char aux = auxref[pos];
            auxref[pos] = auxref[index];
            auxref[index] = aux;
        }
        ref = new String(auxref);
        return ref;
    }

    public String encrypt(String password) throws Exception {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(password.getBytes());
        BigInteger hash = new BigInteger(1, md5.digest());
        String hashword = hash.toString(16);
        return hashword;
    }
}
