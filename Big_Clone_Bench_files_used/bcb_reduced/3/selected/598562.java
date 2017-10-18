package com.nayesoftware.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author ezhgnu
 */
public class crypto {

    private MessageDigest algoritmoActual;

    /**
     *
     * @param tipoAlgoritmo
     */
    public void setAlgoritmo(String tipoAlgoritmo) {
        try {
            algoritmoActual = MessageDigest.getInstance(tipoAlgoritmo);
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("NoSuchAlgorithmException:" + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     *
     * @param b
     * @return
     */
    public String getMsj(byte[] b) {
        algoritmoActual.reset();
        algoritmoActual.update(b);
        byte[] hash = algoritmoActual.digest();
        String d = "";
        for (int i = 0; i < hash.length; i++) {
            int v = hash[i] & 0xFF;
            if (v < 16) d += 0;
            d += Integer.toString(v, 16).toUpperCase() + "";
        }
        return d;
    }
}
