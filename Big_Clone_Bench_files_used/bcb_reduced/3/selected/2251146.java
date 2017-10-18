package org.stars.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Use the algorithm "SHA-1" to generate a digest string.
 * 
 * i.e.
 * 
 * 50f87345956411acb25c66bdf0c098456179ea27
 */
public class Digester {

    public Digester(String input) {
        digester = buildByteArray(input);
    }

    public static Digester build(String input) {
        return new Digester(input);
    }

    private static final String SHA_1 = "SHA-1";

    /**
     * Setter dell'attributo Password
     * @param password Password
     */
    public void setSecret(String input) {
        digester = buildByteArray(input);
    }

    /**
     * Restituisce il Digest sotto forma di string. Mostra la sua utilita' quando
     * si deve salvare il digest di una password in un database.
     * @return digest sottoforma di stringa.
     */
    @Override
    public String toString() {
        return convertToHexString(digester);
    }

    public byte[] toByteArray() {
        return digester;
    }

    /**
     * Converte un array di byte in una stringa.
     * @param digest digest sottoforma di array di byte.
     * @return digest sottoforma di stringa.
     */
    private String convertToHexString(byte digester[]) {
        String hash = "";
        String app;
        for (int i = 0; i < digester.length; i++) {
            if (digester[i] < 0) app = Integer.toHexString(digester[i] + 256); else app = Integer.toHexString(digester[i]);
            if (app.length() == 1) app = "0" + app;
            hash = hash + app;
        }
        return hash;
    }

    /**
     * Converte una stringa che rappresenta un array di byte in un array di byte. Serve
     * per recuperare da una stringa salvata normalmente in un db il digest di una
     * password.
     * @param sDigest digest sottoforma di stringa
     * @return digest sottoforma di array di byte.
     */
    private byte[] toByte(String digester) {
        byte ret[] = new byte[digester.length() / 2];
        int conta = 0;
        String app;
        for (int i = 0; i < digester.length(); ) {
            app = digester.substring(i, i + 2);
            int car = Integer.parseInt(app, 16);
            if (car >= 128) car -= 256;
            ret[conta] = (byte) car;
            i += 2;
            conta++;
        }
        return ret;
    }

    /**
     * Data una password costruisce il digest.
     * @param pwd password
     * @return digest
     */
    protected static byte[] buildByteArray(String secret) {
        byte buf[] = secret.getBytes();
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance(SHA_1);
        } catch (NoSuchAlgorithmException e) {
        }
        if (algorithm == null) {
            buf = null;
        } else {
            algorithm.reset();
            algorithm.update(buf);
            buf = algorithm.digest();
        }
        return buf;
    }

    /**
     * Verifica se una stringa che rappresenta un digest corrisponde al digest della password
     * memorizzata nella classe.
     * @param sDigest2 Digest sottoforma di stringa con il quale effettuare il confronto.
     * @return Se i due digest sono uguali viene restituito <i>true</i>
     */
    public boolean equals(String sDigest2) {
        byte digest2[] = toByte(sDigest2);
        return equals(digest2);
    }

    public boolean equals(Digester digest) {
        byte digest2[] = digest.toByteArray();
        return equals(digest2);
    }

    /**
     * Confronta il digest con un altro digest sottoforma di array di byte.
     * @param Digest2 array di byte che rappresenta il secondo digest.
     * @return Se i due digest sono uguali viene restituito <i>true</i>
     */
    public boolean equals(byte digester2[]) {
        return equals(digester, digester2);
    }

    public static boolean equals(byte digest1[], byte digest2[]) {
        if (digest1.length != digest2.length) return false;
        for (int i = 0; i < digest1.length; i++) if (digest1[i] != digest2[i]) return false;
        return true;
    }

    /**
     * digest.
     */
    private byte digester[];
}
