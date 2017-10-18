package org.apache.bookkeeper.client;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

class MacDigestManager extends DigestManager {

    public static String DIGEST_ALGORITHM = "SHA-1";

    public static String KEY_ALGORITHM = "HmacSHA1";

    Mac mac;

    public MacDigestManager(long ledgerId, byte[] passwd) throws GeneralSecurityException {
        super(ledgerId);
        byte[] macKey = genDigest("mac", passwd);
        SecretKeySpec keySpec = new SecretKeySpec(macKey, KEY_ALGORITHM);
        mac = Mac.getInstance(KEY_ALGORITHM);
        mac.init(keySpec);
    }

    static byte[] genDigest(String pad, byte[] passwd) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        digest.update(pad.getBytes());
        digest.update(passwd);
        return digest.digest();
    }

    @Override
    int getMacCodeLength() {
        return 20;
    }

    @Override
    byte[] getValueAndReset() {
        return mac.doFinal();
    }

    @Override
    void update(byte[] data, int offset, int length) {
        mac.update(data, offset, length);
    }
}
