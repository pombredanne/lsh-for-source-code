package net.sf.jmyspaceiml;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.io.IOException;

public class LoginResponse {

    static final int HASH_SIZE = 0x14;

    /** < Size of SHA-1 hashPassword for login, 20 base 10 */
    static final int NONCE_SIZE = 0x20;

    /** < Half of decoded 'nc' field, 32 base 10 */
    static final char[] MSIM_LOGIN_IP_LIST = { 0x00, 0x00, 0x00, 0x00, 0x05, 0x7f, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x0a, 0x00, 0x00, 0x40, 0xc0, 0xa8, 0x58, 0x01, 0xc0, 0xa8, 0x3c, 0x01 };

    public byte[] compute(byte[] nc, String email, String password) throws Exception {
        byte[] nonce0 = new byte[NONCE_SIZE];
        byte[] nonce1 = new byte[NONCE_SIZE];
        for (int i = 0; i < NONCE_SIZE; i++) {
            nonce0[i] = nc[i];
            nonce1[i] = nc[i + NONCE_SIZE];
        }
        byte[] hashedPassword = hashPassword(password.toLowerCase());
        byte[] key = createKey(hashedPassword, nonce1);
        return encryptMessage(key, nonce0, email);
    }

    byte[] createKey(byte[] hashedPassword, byte[] nonce) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(hashedPassword, 0, HASH_SIZE);
        md.update(nonce, 0, NONCE_SIZE);
        byte[] hash = md.digest();
        byte[] retval = new byte[16];
        for (int i = 0; i < 16; i++) {
            retval[i] = hash[i];
        }
        return retval;
    }

    byte[] hashPassword(String s) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(s.getBytes("UTF-16LE"));
        return md.digest();
    }

    byte[] encryptMessage(byte[] key, byte[] nonce, String email) throws RC4EncryptionExample.EncryptionException {
        String data = new String(nonce) + email + new String(MSIM_LOGIN_IP_LIST);
        RC4EncryptionExample rc4 = new RC4EncryptionExample(key);
        return rc4.encrypt(data.getBytes());
    }

    public byte[] decode64(String encryptedBytes64) throws IOException {
        return new BASE64Decoder().decodeBuffer(encryptedBytes64);
    }

    public String encode64(byte[] data) {
        return new BASE64Encoder().encode(data);
    }
}
