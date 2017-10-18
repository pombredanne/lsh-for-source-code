package Util;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author dalia
 */
public class MD5 {

    /**
     * Metodo que genera el hash MD5
     * @param data Cadena de la cual queremos calcular el hash MD5
     * @param key Clave que emplearemos en el calculo del hash.
     * @return Un array de bits con el valor resultante del hash
     */
    public static byte[] getHashMD5(String data, String key) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] textBytes = data.getBytes();
            md5.update(textBytes);
            byte[] result = md5.digest();
            return result;
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Convierte un byte[] a un String con su valor equivalente en hexadecimal
     * De gran utilidad para convertir el hash calculado con getHashMD5
     * a hexadecimal
     * @param datos
     * @return
     */
    public static String toHexadecimal(byte[] datos) {
        String resultado = "";
        ByteArrayInputStream input = new ByteArrayInputStream(datos);
        String cadAux;
        int leido = input.read();
        while (leido != -1) {
            cadAux = Integer.toHexString(leido);
            if (cadAux.length() < 2) {
                resultado += "0";
            }
            resultado += cadAux;
            leido = input.read();
        }
        return resultado;
    }
}
