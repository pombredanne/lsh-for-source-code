package org.magicbox.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

/**
 * Classe di utilit� di Criptazione
 * 
 * @author Massimiliano Dess� (desmax74@yahoo.it)
 * @since jdk 1.6.0
 * @version 3.0
 */
public class Cripto {

    /**
     * Genera Hash da una Stringa
     * 
     * @param String
     * @return String
     */
    public static String generaHash(String testo) {
        String hash = null;
        try {
            String control = testo + String.valueOf(System.currentTimeMillis());
            hash = new String(cripta(control.toCharArray()));
        } catch (Exception ex) {
            Logger log = Logger.getLogger("Cripto");
            log.severe("Eccezione in genera Hash:" + ex.getMessage());
        }
        return hash;
    }

    /**
     * Converte un array di byte in un array di caratteri contenente una rappresentazione esadecimale stampabile dei bytes. Ciascun byte dell'array sorgente contribuisce con una coppia di byte
     * dell'array restituito.
     * 
     * @param src l'array sorgente
     * @return array di caratteri contenente una versione stampabile dell'array sorgente
     */
    private static char[] hexDump(byte src[]) {
        char buf[] = new char[src.length * 2];
        for (int b = 0; b < src.length; b++) {
            String byt = Integer.toHexString((int) src[b] & 0xFF);
            if (byt.length() < 2) {
                buf[b * 2 + 0] = '0';
                buf[b * 2 + 1] = byt.charAt(0);
            } else {
                buf[b * 2 + 0] = byt.charAt(0);
                buf[b * 2 + 1] = byt.charAt(1);
            }
        }
        return buf;
    }

    /**
     * Azzera il contenuto dell'array specificato. Usato tipicamente per cancellare la memorizzazione provvisoria della password di accesso in modo che non venga lasciata da qualche parte nella
     * memoria.
     * 
     * @param pwd array della password da azzerare
     */
    private static void smudge(byte pwd[]) {
        if (null != pwd) {
            for (int b = 0; b < pwd.length; b++) {
                pwd[b] = 0;
            }
        }
    }

    /**
     * Effettua SHA hashing sulla password fornita e restituisce un array di char che contiene contenere la password cifrata come stringa stampabile. L'hash � calcolato sugli 8 bit di livello basso
     * di ogni character.
     * 
     * @param pwd La password da cifrare
     * @return array di 32 char contenente MD5 hashing della password
     * @exception Exception Description of the Exception
     */
    private static char[] cripta(char pwd[]) throws Exception {
        if (null == md) {
            md = MessageDigest.getInstance(ALGORITHM_SHA);
        }
        md.reset();
        byte pwdb[] = new byte[pwd.length];
        for (int b = 0; b < pwd.length; b++) {
            pwdb[b] = (byte) pwd[b];
        }
        char crypt[] = hexDump(md.digest(pwdb));
        smudge(pwdb);
        return crypt;
    }

    /**
     * Effettua SHA su una Stringa
     * 
     * @param String
     * @return String
     */
    public static String stringToSHA(String buffer) {
        try {
            MessageDigest shaDigest = MessageDigest.getInstance(ALGORITHM_SHA);
            byte[] mybytes = buffer.getBytes();
            byte[] byteResult = shaDigest.digest(mybytes);
            String result = bytesToHex(byteResult);
            return result;
        } catch (NoSuchAlgorithmException md5ex) {
        }
        return null;
    }

    /**
     * Effettua MD5 su una Stringa
     * 
     * @param String
     * @return String
     */
    public static String stringToMD5(String buffer) {
        try {
            MessageDigest digest1 = MessageDigest.getInstance(ALGORITHM_MD5);
            byte[] mybytes = buffer.getBytes();
            byte[] byteResult = digest1.digest(mybytes);
            String result = bytesToHex(byteResult);
            return result;
        } catch (NoSuchAlgorithmException md5ex) {
        }
        return null;
    }

    private static String bytesToHex(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; ++i) {
            sb.append((Integer.toHexString((b[i] & 0xFF) | 0x100)).substring(1, 3));
        }
        return sb.toString();
    }

    private static final String ALGORITHM_SHA = "SHA-1";

    private static final String ALGORITHM_MD5 = "MD5";

    private static MessageDigest md = null;
}
