package com.sts.webmeet.server.acegi.encoding;

import org.acegisecurity.providers.encoding.PasswordEncoder;
import org.springframework.dao.DataAccessException;
import org.apache.commons.codec.binary.Hex;
import java.security.MessageDigest;

public class WHPasswordEncoder implements PasswordEncoder {

    private static final String CHAR_ENCODING = "UTF-8";

    private static final String HASH_ALGORITHM = "SHA";

    public String encodePassword(String rawPass, Object salt) throws DataAccessException {
        if (true) throw new RuntimeException(getClass().getName() + ".encodePassword not implemented yet");
        return null;
    }

    public boolean isPasswordValid(String encPass, String rawPass, Object salt) throws DataAccessException {
        boolean bMatch = false;
        try {
            String strSalt = (String) salt;
            byte[] baSalt = Hex.decodeHex(strSalt.toCharArray());
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(rawPass.getBytes(CHAR_ENCODING));
            md.update(baSalt);
            byte[] baCalculatedHash = md.digest();
            byte[] baStoredHash = Hex.decodeHex(encPass.toCharArray());
            bMatch = MessageDigest.isEqual(baCalculatedHash, baStoredHash);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bMatch;
    }
}
