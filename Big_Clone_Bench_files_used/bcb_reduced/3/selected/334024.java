package com.wle.server.persistence;

import java.security.*;
import sun.misc.BASE64Encoder;
import com.wle.server.persistence.exception.*;

;

public class PasswordGenerator {

    /**
	 * Generates a password hash given a password and username (used for salt)
	 * @param username cheapo salt value
	 * @param password the password in question
	 * @return a base 64 hash using SHA-256 (256 bit long code)
	 * @throws PersistenceException
	 */
    public static String generate(String username, String password) throws PersistenceException {
        String output = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            md.update(username.getBytes());
            md.update(password.getBytes());
            byte[] rawhash = md.digest();
            output = byteToBase64(rawhash);
        } catch (Exception e) {
            throw new PersistenceException("error, could not generate password");
        }
        return output;
    }

    /**
	    * From a byte[] returns a base 64 representation
	    * @param data byte[]
	    * @return String
	    * @throws IOException
	    */
    private static String byteToBase64(byte[] data) {
        BASE64Encoder endecoder = new BASE64Encoder();
        return endecoder.encode(data);
    }
}
