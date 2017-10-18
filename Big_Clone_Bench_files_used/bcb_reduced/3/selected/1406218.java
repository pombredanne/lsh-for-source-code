package br.unb.unbiquitous.ubiquitos.authentication;

import java.io.UnsupportedEncodingException;
import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 
 * Class responsible to generate the hash of a given message. Its methods
 * will be used in first and second steps of the authentication protocol.
 * */
public class HashGenerator {

    /** 
	 * Converts an array of bytes to a String. The value of the output String
	 * will be the same that the input message, in hexadecimal notation.
	 * 
	 * @param mensagem - array of bytes that will be converted to String
	 * */
    private static String convertToHex(byte[] mensagem) {
        StringBuffer strbuf = new StringBuffer();
        for (int i = 0; i < mensagem.length; i++) {
            int meiobyte = (mensagem[i] >>> 4) & 0x0F;
            int byte_completo = 0;
            do {
                if ((0 <= meiobyte) && (meiobyte <= 9)) strbuf.append((char) ('0' + meiobyte)); else strbuf.append((char) ('a' + (meiobyte - 10)));
                meiobyte = mensagem[i] & 0x0F;
            } while (byte_completo++ < 1);
        }
        return strbuf.toString();
    }

    /** 
	 * Receives a message and generates id Hash. The method used to genarate
	 * the hash is SHA-256.
	 * 
	 * @param text - the message witch hash will be generated
	 * */
    public static String generateHashSE(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException, DigestException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-256");
        byte[] hashSHA256 = new byte[32];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        md.digest(hashSHA256, 0, text.length());
        return convertToHex(hashSHA256);
    }

    public static String generateHash(String message) throws NoSuchAlgorithmException, UnsupportedEncodingException, DigestException {
        MessageDigest digest;
        digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        digest.update(message.getBytes("iso-8859-1"), 0, message.length());
        byte[] output = new byte[20];
        digest.digest(output, 0, output.length);
        return convertToHex(output);
    }
}
