package net.minecraft.src;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5String {

    private String field_27370_a;

    public MD5String(String par1Str) {
        field_27370_a = par1Str;
    }

    /**
     * Gets the MD5 string
     */
    public String getMD5String(String par1Str) {
        try {
            String s = (new StringBuilder()).append(field_27370_a).append(par1Str).toString();
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            messagedigest.update(s.getBytes(), 0, s.length());
            return (new BigInteger(1, messagedigest.digest())).toString(16);
        } catch (NoSuchAlgorithmException nosuchalgorithmexception) {
            throw new RuntimeException(nosuchalgorithmexception);
        }
    }
}