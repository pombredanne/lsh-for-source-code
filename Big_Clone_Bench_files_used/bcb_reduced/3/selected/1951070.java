package org.fao.fenix.communication.cryptography;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class SHA1Manager {

    /**
	 * Generates a SHA digest from a given file.
	 * 
	 * @param file File to generate digest.
	 * @return A SHA string.
	 */
    public static String generate(File source) {
        byte[] SHA = new byte[20];
        String SHADigest = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            FileInputStream inputStream = new FileInputStream(source);
            byte[] data = new byte[inputStream.available()];
            inputStream.read(data);
            digest.update(data);
            SHA = digest.digest();
            for (int i = 0; i < SHA.length; i++) SHADigest += SHA[i];
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NO SUCH ALGORITHM EXCEPTION: " + e.getMessage());
        } catch (FileNotFoundException e) {
            System.out.println("FILE NOT FOUND EXCEPTION: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO EXCEPTION: " + e.getMessage());
        }
        return SHADigest.trim();
    }

    /**
	 * Generates a SHA digest for a string.
	 * 
	 * @param source The string to digest.
	 * @return The SHA string.
	 */
    public static String generate(String source) {
        byte[] SHA = new byte[20];
        String SHADigest = "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(source.getBytes());
            SHA = digest.digest();
            for (int i = 0; i < SHA.length; i++) SHADigest += (char) SHA[i];
        } catch (NoSuchAlgorithmException e) {
            System.out.println("NO SUCH ALGORITHM EXCEPTION: " + e.getMessage());
        }
        return SHADigest;
    }
}
