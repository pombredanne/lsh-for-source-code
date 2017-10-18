package cn.ac.ntarl.umt.encrypt;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Hash extends Encryptor {

    public MD5Hash() {
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte[] encrypt(byte[] data) {
        return md.digest(data);
    }

    private MessageDigest md;
}
