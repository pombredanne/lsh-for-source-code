package org.digitall.lib.ssl;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public abstract class MD5 {

    public static String getMD5(String _pwd) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(_pwd.getBytes());
            return toHexadecimal(new String(md.digest()).getBytes());
        } catch (NoSuchAlgorithmException x) {
            x.printStackTrace();
            return "";
        }
    }

    private static String toHexadecimal(byte[] datos) {
        String resultado = "";
        ByteArrayInputStream input = new ByteArrayInputStream(datos);
        String cadAux;
        int leido = input.read();
        while (leido != -1) {
            cadAux = Integer.toHexString(leido);
            if (cadAux.length() < 2) resultado += "0";
            resultado += cadAux;
            leido = input.read();
        }
        return resultado;
    }
}
