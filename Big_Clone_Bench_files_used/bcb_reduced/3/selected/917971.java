package com.googlecode.oskis.library.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author thomas
 */
public class CryptedPasswd {

    protected String unencryptedPasswd;

    private String encryptedPasswd;

    protected String salt;

    protected String cryptmethod;

    /**
     * Einfachster Aufruf, nur Passwort, Rest default
     * @param passwd
     */
    public CryptedPasswd(String passwd) {
        this.unencryptedPasswd = passwd;
        this.salt = "AA";
        this.cryptmethod = "MD5";
    }

    /**
     * Passwort + salt, Rest default
     * @param passwd
     * @param salt
     */
    public CryptedPasswd(String passwd, String salt) {
        this.unencryptedPasswd = passwd;
        this.salt = salt;
        this.cryptmethod = "MD5";
    }

    /**
     * Keine Defaults
     * @param passwd
     * @param salt
     * @param cryptmethod
     */
    public CryptedPasswd(String passwd, String salt, String cryptmethod) {
        this.unencryptedPasswd = passwd;
        this.salt = salt;
        this.cryptmethod = cryptmethod;
    }

    private void doCrypt() {
        byte[] passwdbytes = unencryptedPasswd.getBytes();
        byte[] saltbytes = salt.getBytes();
        encryptedPasswd = null;
        try {
            MessageDigest algorithm = MessageDigest.getInstance(cryptmethod);
            algorithm.reset();
            algorithm.update(passwdbytes);
            algorithm.update(saltbytes);
            byte[] digest = algorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                String hex = Integer.toHexString(0xFF & digest[i]);
                if (hex.length() == 1) {
                    hexString.append("0");
                }
                hexString.append(hex);
            }
            encryptedPasswd = hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    public String getEncryptedPasswd() {
        doCrypt();
        return (encryptedPasswd);
    }
}
