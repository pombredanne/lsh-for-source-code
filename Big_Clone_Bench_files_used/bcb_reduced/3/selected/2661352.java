package dht.identifier.identifier;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Random;

/**
 * todo write javadoc
 */
public final class Identifiers {

    private Identifiers() {
    }

    /**
	 * Creates a new random identifier.
	 *
	 * @param length the identifier length in bytes
	 * @return a new random identifier.
	 */
    public static Identifier randomIdentifier(int length) {
        return ByteArrayIdentifier.randomIdentifier(length);
    }

    /**
	 * Creates a new random identifier.
	 *
	 * @param length the identifier length in bytes
	 * @param random the random generator
	 * @return a new random identifier.
	 */
    public static Identifier randomIdentifier(int length, Random random) {
        return ByteArrayIdentifier.randomIdentifier(length, random);
    }

    /**
	 * Creates a new identifier based on the string value. Uses MD5 hash-function.
	 *
	 * @param str the string which is the base for the identifier
	 * @param len is the length of the identifier
	 * @return a new identifier.
	 */
    public static Identifier createIdentifier(String str, int len) {
        byte[] identifier;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] input = str.getBytes("UTF-8");
            identifier = md.digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        byte[] wrapper = new byte[len];
        if (wrapper.length >= identifier.length) {
            System.arraycopy(identifier, 0, wrapper, 0, identifier.length);
        } else {
            for (int i = 0; i < wrapper.length; i++) {
                wrapper[i] = identifier[i];
            }
        }
        return new ByteArrayIdentifier(wrapper);
    }

    /**
	 * Creates a new identifier based on the byte array.
	 *
	 * @param array the byte array which is the base for the identifier
	 * @return a new identifier.
	 */
    public static Identifier getIdentifierFromByteArray(byte[] array) {
        return new ByteArrayIdentifier(array);
    }

    /**
	 * Converts the string representation of the identifier to a new identifier.
	 *
	 * @param identifier the string representation of identifer.
	 * @return a new identifier.
	 * @throws java.text.ParseException
	 */
    public static Identifier convertFromString(String identifier) throws ParseException {
        if (identifier.length() == 0) {
            throw new ParseException(identifier, 0);
        }
        byte[] data = new byte[identifier.length() / 2];
        for (int i = 0; i < data.length; i++) {
            try {
                data[i] = (byte) Integer.parseInt(identifier.substring(2 * i, 2 * i + 2), 16);
            } catch (NumberFormatException e) {
                throw new ParseException(identifier, i);
            }
        }
        return new ByteArrayIdentifier(data);
    }
}
