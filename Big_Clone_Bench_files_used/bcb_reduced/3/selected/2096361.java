package net.mreunion.web.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

public class MD5SecurityProvider implements ISecurityProvider {

    MessageDigest md;

    BASE64Encoder encoder;

    BASE64Decoder decoder;

    public MD5SecurityProvider() throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");
        encoder = new BASE64Encoder();
        decoder = new BASE64Decoder();
    }

    public boolean compare(String password, String hash) {
        if (password == null || hash == null) return false;
        try {
            byte[] hashedPassword = md.digest(password.getBytes("ASCII"));
            byte[] hashedDBPassword = decode(hash);
            if (hashedPassword.length != hashedDBPassword.length) {
                return false;
            } else {
                for (int i = 0; i < hashedPassword.length; i++) {
                    if (hashedPassword[i] != hashedDBPassword[i]) {
                        return false;
                    }
                }
                System.out.println("MATCHED!");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String encode(String password) throws UnsupportedEncodingException {
        String encodedString = "";
        if (password != null) {
            byte[] hash = md.digest(password.getBytes("ASCII"));
            encodedString = encoder.encode(hash);
        }
        return encodedString;
    }

    public byte[] decode(String hash) throws IOException {
        byte[] decodedHash = null;
        if (hash != null) {
            decodedHash = decoder.decodeBuffer(hash);
        }
        return decodedHash;
    }
}
