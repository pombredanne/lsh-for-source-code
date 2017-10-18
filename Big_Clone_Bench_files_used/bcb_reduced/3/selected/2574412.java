package lv.odylab.evemanage.security;

import lv.odylab.appengine.repackaged.Base64;
import lv.odylab.evemanage.application.exception.EveManageSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCalculatorImpl implements HashCalculator {

    @Override
    public String hashApiKey(Long apiKeyUserID, String apiKeyString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            byte[] apiKeyUserIDBytes = String.valueOf(apiKeyUserID).getBytes();
            byte[] salt = Base64.encodeBytesToBytes(apiKeyUserIDBytes);
            digest.update(salt);
            return Base64.encodeBytes(digest.digest(apiKeyString.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new EveManageSecurityException(e);
        }
    }
}
