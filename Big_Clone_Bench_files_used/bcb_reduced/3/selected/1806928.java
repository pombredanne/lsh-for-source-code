package ar.com.AmberSoft.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class PKGenerator {

    private static long lastTime = 0;

    private String pk;

    public PKGenerator() {
        long currentTime = System.currentTimeMillis();
        while (currentTime == lastTime) currentTime = System.currentTimeMillis();
        String clave = new Long(currentTime).toString();
        lastTime = currentTime;
        pk = encriptar(clave);
    }

    public long getLastTime() {
        return lastTime;
    }

    public int getIntLastTime() {
        return new Long(lastTime).intValue();
    }

    public String getPk() {
        return pk;
    }

    /**
	 * Encripta el texto pasado como parametro en MD5
	 * con Base64
	 * @param texto
	 * @return
	 */
    public static String encriptar(String texto) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        byte[] textBytes = md.digest(texto.getBytes());
        int size = textBytes.length;
        StringBuffer h = new StringBuffer(size);
        for (int i = 0; i < size; i++) {
            int u = textBytes[i] & 255;
            if (u < 16) {
                h.append("0" + Integer.toHexString(u));
            } else {
                h.append(Integer.toHexString(u));
            }
        }
        return h.toString();
    }
}
