package de.mex.premis.objectcreator;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Diese Klasse generiert eine Check Summe f�r eine Datei nach einem vorgegebenen Algorithmus,
 * da die Klasse MessageDigest von Java.Security eingesetzt wird, sind nur Algorithmen m�glich, die diese Klasse unterst�tzt
 * @author Matthias Pickel
 *
 */
public class CheckSum {

    public static final String AlgorithmMD5 = "MD5";

    public static final String AlgorithmSHA = "SHA";

    /**
	 * Liefert eine Checksumme f�r die angegebene Datei mit dem angegebenen Algorithmus
	 * @param input
	 * @param algorithm
	 * @return
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 */
    public static String createCheckSum(String input, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        byte[] md5Bytes = null;
        FileInputStream fis = new FileInputStream(input);
        byte[] fileBytes = new byte[1000];
        while (fis.read(fileBytes) > 0) {
            md.update(fileBytes);
        }
        md5Bytes = md.digest();
        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16) hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        return hexValue.toString();
    }
}
