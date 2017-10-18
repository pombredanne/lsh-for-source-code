package com.gwtaf.security.server;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.gwtaf.core.shared.util.UnexpectedErrorException;
import com.gwtaf.security.shared.IHashAlgorithm;

public class SHA256 implements IHashAlgorithm {

    private MessageDigest md;

    public static SHA256 get() throws NoSuchAlgorithmException {
        return new SHA256();
    }

    public SHA256() throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance("SHA-256");
    }

    public String calcHash(String value) {
        md.reset();
        try {
            md.update(value.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new UnexpectedErrorException(e);
        }
        byte[] digest = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            String hex = Integer.toHexString(0xFF & digest[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
