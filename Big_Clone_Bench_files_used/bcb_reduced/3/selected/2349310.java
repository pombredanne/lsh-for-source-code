package jLastFM.assistants;

import java.security.*;

public class MD5Password {

    private String password;

    public MD5Password(String password) {
        this.password = password;
    }

    public String gen() {
        String plainText = this.password;
        MessageDigest mdAlgorithm;
        StringBuffer hexString = new StringBuffer();
        try {
            mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(plainText.getBytes());
            byte[] digest = mdAlgorithm.digest();
            for (int i = 0; i < digest.length; i++) {
                plainText = Integer.toHexString(0xFF & digest[i]);
                if (plainText.length() < 2) plainText = "0" + plainText;
                hexString.append(plainText);
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return hexString.toString();
    }
}
