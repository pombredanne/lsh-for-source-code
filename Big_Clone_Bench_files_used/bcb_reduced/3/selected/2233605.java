package com.phasotron.security.authentication.encoding;

import org.postgresql.util.UnixCrypt;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.codec.Base64;
import java.security.MessageDigest;
import java.util.Random;
import jcifs.smb.NtlmPasswordAuthentication;

public class LdapPasswordEncoder implements PasswordEncoder {

    private static final int SHA_LENGTH = 20;

    private static final int MD5_LENGTH = 16;

    private static final String SSHA = "SSHA";

    private static final String SHA = "SHA";

    private static final String MD5 = "MD5";

    private static final String SMD5 = "SMD5";

    private static final String CRYPT = "CRYPT";

    private static final String MD5CRYPT = "MD5CRYPT";

    private static final String SSHA_PREFIX = "{SSHA}";

    private static final String SHA_PREFIX = "{SHA}";

    private static final String MD5_PREFIX = "{MD5}";

    private static final String SMD5_PREFIX = "{SMD5}";

    private static final String CRYPT_PREFIX = "{CRYPT}";

    private static final String MD5_CRYPT_MAGIC = "$1$";

    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";

    private boolean forceLowerCasePrefix;

    private String encodeAlgorithm = "plaintext";

    public LdapPasswordEncoder() {
    }

    public String encodePassword(String rawPass, Object salt) {
        if (encodeAlgorithm.equalsIgnoreCase(MD5)) {
            return encodeMD5Password(rawPass);
        }
        if (encodeAlgorithm.equalsIgnoreCase(SMD5)) {
            return encodeSMD5Password(rawPass);
        }
        if (encodeAlgorithm.equalsIgnoreCase(SHA)) {
            return encodeSHAPassword(rawPass);
        }
        if (encodeAlgorithm.equalsIgnoreCase(SSHA)) {
            return encodeSSHAPassword(rawPass);
        }
        if (encodeAlgorithm.equalsIgnoreCase(CRYPT)) {
            return encodeCryptPassword(rawPass);
        }
        if (encodeAlgorithm.equalsIgnoreCase(MD5CRYPT)) {
            return encodeMD5CryptPassword(rawPass);
        }
        return rawPass;
    }

    public String encodePassword(String rawPass) {
        return encodePassword(rawPass, null);
    }

    private String encodeMD5Password(String rawPass) {
        String prefix = forceLowerCasePrefix ? MD5_PREFIX.toLowerCase() : MD5_PREFIX;
        return prefix + encodePassword(rawPass, null, MD5);
    }

    private String encodeSMD5Password(String rawPass) {
        String prefix = forceLowerCasePrefix ? SMD5_PREFIX.toLowerCase() : SMD5_PREFIX;
        return prefix + encodePassword(rawPass, generateSalt(), MD5);
    }

    private String encodeSHAPassword(String rawPass) {
        String prefix = forceLowerCasePrefix ? SHA_PREFIX.toLowerCase() : SHA_PREFIX;
        return prefix + encodePassword(rawPass, null, SHA);
    }

    private String encodeSSHAPassword(String rawPass) {
        String prefix = forceLowerCasePrefix ? SSHA_PREFIX.toLowerCase() : SSHA_PREFIX;
        return prefix + encodePassword(rawPass, generateSalt(), SHA);
    }

    private String encodeCryptPassword(String rawPass) {
        String prefix = forceLowerCasePrefix ? CRYPT_PREFIX.toLowerCase() : CRYPT_PREFIX;
        return prefix + UnixCrypt.crypt(rawPass);
    }

    private String encodeMD5CryptPassword(String rawPass) {
        String prefix = forceLowerCasePrefix ? CRYPT_PREFIX.toLowerCase() : CRYPT_PREFIX;
        return prefix + encodeMD5CryptPassword(rawPass, generateSalt());
    }

    private String encodePassword(String rawPass, byte[] salt, String encodeAlgorithm) {
        MessageDigest messageDigest = getMessageDigest(encodeAlgorithm);
        messageDigest.update(rawPass.getBytes());
        String encPass;
        if (salt != null) {
            messageDigest.update(salt);
            byte[] hash = combineHashAndSalt(messageDigest.digest(), salt);
            encPass = new String(Base64.encode(hash));
        } else {
            encPass = new String(Base64.encode(messageDigest.digest()));
        }
        return encPass;
    }

    private String encodeMD5CryptPassword(String password, byte[] salt) {
        return MD5Crypt.crypt(password, new String(salt));
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) {
        if (encPass.toUpperCase().startsWith(MD5_PREFIX)) return isMD5PasswordValid(encPass, rawPass);
        if (encPass.toUpperCase().startsWith(SMD5_PREFIX)) return isSMD5PasswordValid(encPass, rawPass);
        if (encPass.toUpperCase().startsWith(SHA_PREFIX)) return isSHAPasswordValid(encPass, rawPass);
        if (encPass.toUpperCase().startsWith(SSHA_PREFIX)) return isSSHAPasswordValid(encPass, rawPass);
        if (encPass.toUpperCase().startsWith(CRYPT_PREFIX)) return isCryptPasswordValid(encPass, rawPass);
        return isPlaintextPasswordValid(encPass, rawPass);
    }

    public boolean isPasswordValid(String encPass, String rawPass) {
        return isPasswordValid(encPass, rawPass, null);
    }

    public String encodeSmbPass(String password) {
        byte[] encPassbytes = NtlmPasswordAuthentication.nTOWFv1(password);
        String encPass = "";
        for (int i = 0; i < encPassbytes.length; i++) {
            int c = encPassbytes[i] & 0x000000FF;
            String str = Integer.toHexString(c).toUpperCase();
            if (c < 16) str = "0" + str;
            encPass = encPass + str;
        }
        return encPass;
    }

    private boolean isPlaintextPasswordValid(String encPass, String rawPass) {
        String pass1 = encPass + "";
        return pass1.equals(rawPass);
    }

    private boolean isMD5PasswordValid(String encPass, String rawPass) {
        String encPassWithoutPrefix = encPass.substring(MD5_PREFIX.length());
        return encodePassword(rawPass, null, MD5).equals(encPassWithoutPrefix);
    }

    private boolean isSMD5PasswordValid(String encPass, String rawPass) {
        String encPassWithoutPrefix = encPass.substring(SMD5_PREFIX.length());
        byte[] salt = extractSalt(encPassWithoutPrefix, MD5_LENGTH);
        return encodePassword(rawPass, salt, MD5).equals(encPassWithoutPrefix);
    }

    private boolean isSHAPasswordValid(String encPass, String rawPass) {
        String encPassWithoutPrefix = encPass.substring(SHA_PREFIX.length());
        return encodePassword(rawPass, null, SHA).equals(encPassWithoutPrefix);
    }

    private boolean isSSHAPasswordValid(String encPass, String rawPass) {
        String encPassWithoutPrefix = encPass.substring(SSHA_PREFIX.length());
        byte[] salt = extractSalt(encPassWithoutPrefix, SHA_LENGTH);
        return encodePassword(rawPass, salt, SHA).equals(encPassWithoutPrefix);
    }

    private boolean isCryptPasswordValid(String encPass, String rawPass) {
        String encPassWithoutPrefix = encPass.substring(CRYPT_PREFIX.length());
        if (encPassWithoutPrefix.startsWith(MD5_CRYPT_MAGIC)) {
            String saltStr = encPassWithoutPrefix.substring(MD5_CRYPT_MAGIC.length());
            if (saltStr.indexOf('$') != -1) saltStr = saltStr.substring(0, saltStr.indexOf('$'));
            if (saltStr.length() > 8) saltStr = saltStr.substring(0, 8);
            return encodeMD5CryptPassword(rawPass, saltStr.getBytes()).equals(encPassWithoutPrefix);
        } else return UnixCrypt.matches(encPassWithoutPrefix, rawPass);
    }

    public void setForceLowerCasePrefix(boolean forceLowerCasePrefix) {
        this.forceLowerCasePrefix = forceLowerCasePrefix;
    }

    private byte[] combineHashAndSalt(byte[] hash, byte[] salt) {
        if (salt == null) {
            return hash;
        }
        byte[] hashAndSalt = new byte[hash.length + salt.length];
        System.arraycopy(hash, 0, hashAndSalt, 0, hash.length);
        System.arraycopy(salt, 0, hashAndSalt, hash.length, salt.length);
        return hashAndSalt;
    }

    private MessageDigest getMessageDigest(String algorithm) {
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("No such algorithm [" + algorithm + "]");
        }
        return messageDigest;
    }

    private byte[] extractSalt(String encPass, int hashLength) {
        byte[] hashAndSalt = Base64.decode(encPass.getBytes());
        int saltLength = hashAndSalt.length - hashLength;
        byte[] salt = new byte[saltLength];
        System.arraycopy(hashAndSalt, hashLength, salt, 0, saltLength);
        return salt;
    }

    private byte[] generateSalt() {
        StringBuffer salt = new StringBuffer();
        Random randgen = new Random();
        while (salt.length() < 4) {
            int index = (int) (randgen.nextFloat() * SALT_CHARS.length());
            salt.append(SALT_CHARS.substring(index, index + 1));
        }
        return salt.toString().getBytes();
    }

    public void setEncodeAlgorithm(String encodeAlgorithm) {
        this.encodeAlgorithm = encodeAlgorithm;
    }
}
