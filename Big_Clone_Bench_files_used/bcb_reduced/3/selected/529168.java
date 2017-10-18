package com.jdbwc.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Security methods for this package.
 * 
 * @author Tim Gall
 * @version 2010-04-10
 */
public final class Security {

    private static final int SALT_LENGTH = 16;

    private Security() {
    }

    /**
	 * Return a secure String based on <b>input</b>.<br />
	 * The leading chars are the salt.
	 * This value can be reproduced on the other end 
	 * from the same input string and the output from this method
	 * as long as the salt length is known on both ends.<br />
	 * Computationally very hard to hack and not reversible (can't be decrypted)
	 *
	 * @param input String to derive secure hash from.
	 * @return String - secure hash based on input.
	 */
    public static String getSecureString(final String input) throws SQLException {
        String salt = rand(SALT_LENGTH * 2);
        salt = getHash("MD5", salt);
        salt = salt.substring(0, SALT_LENGTH);
        String output = salt + getHash("SHA-256", salt.concat(input));
        return output;
    }

    /**
	 * Generate and return a MessageDigest hash for the String <b>input</b>.
	 *
	 * @param input String to derive MessageDigest from.
	 * @param algorithm Algorithm type EG: MD5, SHA-256, SHA-512
	 * @return MessageDigest digest of input. Output as a String.
	 */
    public static String getHash(final String algorithm, final String input) throws SQLException {
        String output = "";
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            final byte mdBytes[] = input.getBytes();
            md.update(mdBytes);
            final byte mdsum[] = md.digest();
            final BigInteger bigInt = new BigInteger(1, mdsum);
            output = bigInt.toString(16);
            while (output.length() < (md.getDigestLength() * 2)) {
                output = "0" + output;
            }
            md.reset();
        } catch (final NoSuchAlgorithmException e) {
            throw new SQLException("NoSuchAlgorithmException: Error accessing a Java algorithm for: " + algorithm, "WCNAE", e);
        }
        return output;
    }

    /**
	 * Returns a Mixed-Character random value of given length.
	 * Suitable for passwords.
	 *
	 * @param aListSize int - must be greater than 0.
	 * @return randomly generated string aListSize in length.
	 */
    public static String rand(final int aListSize) {
        return rand(aListSize, false);
    }

    /**
	 * Returns an AlphaNumeric random value of given length.<br />
	 * Suitable for user-names.
	 * 
	 * @param aListSize int - must be greater than 0.
	 * @param alphNumeric If true, only AlphaNumeric characters are used, 
	 * otherwise mixed characters are used.<br />
	 * Mixed characters can break username's in certain situations but are ideal for strong passwords.
	 * @return randomly generated AlphaNumeric string aListSize in length.
	 */
    public static String rand(final int aListSize, boolean alphNumeric) {
        return rand(aListSize, alphNumeric, false);
    }

    /**
	 * Returns an AlphaNumeric random value of given length.<br />
	 * Suitable for user-names.
	 * 
	 * @param aListSize int - must be greater than 0.
	 * @param alphNumericOnly If true, only AlphaNumeric characters are used, 
	 * otherwise mixed characters are used.<br />
	 * Mixed characters can break username's in certain situations but are ideal for strong passwords.
	 * @param numericOnly If true only numbers will be used. This takes precedent over alphNumericOnly.
	 * @return randomly generated AlphaNumeric string aListSize in length.
	 */
    public static String rand(final int aListSize, boolean alphNumericOnly, boolean numericOnly) {
        Character[] charSet;
        if (numericOnly) {
            charSet = getNumeric();
        } else if (alphNumericOnly) {
            charSet = getAlphNumeric();
        } else {
            charSet = getFullSet();
        }
        final List<Character> list = Arrays.asList(charSet);
        Collections.shuffle(list);
        charSet = list.toArray(charSet);
        final int aUpperLimit = charSet.length;
        String randValue = "";
        final List<Integer> numbers = pickNumbers(aUpperLimit, aListSize);
        for (int number : numbers) {
            randValue += charSet[number];
        }
        return randValue;
    }

    private static Character[] getFullSet() {
        return new Character[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '!', '@', '#', '$', '%', '^', '&', '*', '(', ')', '_', '+', '=', '-', '~', '`', '/', '.' };
    }

    private static Character[] getAlphNumeric() {
        return new Character[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };
    }

    private static Character[] getNumeric() {
        return new Character[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' };
    }

    /**
	 * Get a List of random Integers in the range 0 (inclusive) to N (exclusive).
	 *
	 * @param aUpperLimit must be greater than 0.
	 * @param aListSize must be greater than 0.
	 * @exception IllegalArgumentException if parameter does not comply.
	 */
    private static List<Integer> pickNumbers(final int aUpperLimit, final int aListSize) {
        if (aUpperLimit <= 0) {
            throw new IllegalArgumentException("UpperLimit must be positive: " + aUpperLimit);
        }
        if (aListSize <= 0) {
            throw new IllegalArgumentException("Size of returned List must be greater than 0.");
        }
        final Random generator = new Random();
        final List<Integer> result = new ArrayList<Integer>();
        for (int idx = 0; idx < aListSize; ++idx) {
            result.add(new Integer(generator.nextInt(aUpperLimit)));
        }
        return result;
    }
}
