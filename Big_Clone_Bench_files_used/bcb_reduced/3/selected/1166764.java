package bruckbox.helper;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Generiert Hashes f&uuml;r Strings. Wahlfreie Verwendung der
 * Hashfunktion.
 *
 * @author
 *     Daniel Henry
 * @version
 *     1.0
 */
public final class HashGenerator {

    private static final String WARN_NO_SUCH_ALGORITHM = "The crypting algorithm '%s' is not supported by this operating system";

    private static final String DEFAULT_ENCRYPTION = "SHA-1";

    /**
   * Hashfunktion f&uuml;r Strings. Minimalkonstruktor.
   *
   * @see #calc(String, String)
   * @param value
   *     Passwort im Klartext.
   * @return
   *     Verschl&uuml;sselter Passwortstring.
   */
    public static final String calc(String value) {
        ArgumentHelper.checkForContent(value);
        return calc(DEFAULT_ENCRYPTION, value);
    }

    /**
   * Hashfunktion f&uuml;r Strings. Maximalkonstruktor mit 
   * Angabe des Hashalgorithmus.
   *
   * @param algorithm
   *     Hashalgorithmus (default: MD5, SHA-1) je nach System
   *     mehrere verf�gbar).
   * @param value
   *     Klartext des zu hashenden Strings.
   * @return
   *     Hash f�r den &uuml;bermittelten Wert.
   */
    public static final String calc(String algorithm, String value) {
        ArgumentHelper.checkForContent(algorithm);
        ArgumentHelper.checkForContent(value);
        final StringBuffer sb = new StringBuffer("");
        try {
            final MessageDigest md = MessageDigest.getInstance(algorithm);
            final byte[] digest = md.digest(value.getBytes());
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(WARN_NO_SUCH_ALGORITHM);
        }
        return sb.toString();
    }

    /**
   * Generiert einen eindeutigen Aktivierungsschl&uuml;&szlig;el.
   * <p/>
   * Der generiert Schl&uuml;&szlig;el bassiert auf der aktuellen
   * Uhrzeit und ist dadurch immer eindeutig.
   *
   * @return
   *     Den generierten Aktivierungsschl&uuml;&szlig;el.
   */
    public static final String generateKey() {
        final long now = System.currentTimeMillis();
        return calc(DEFAULT_ENCRYPTION, Long.toString(now));
    }

    /**
   * Generiert ein 6-stelliges Passwort
   * Kann z.B. verwendet werden um den User ein neues Passwort zuzusenden
   * 
   * @return
   *  Das generierte 6-stellige Passwort
   */
    public static final String generatePassword() {
        java.util.Random rGen = new java.util.Random();
        char[] passArray = new char[6];
        for (int i = 0; i < 6; ++i) {
            passArray[i] = (char) (rGen.nextInt(26) + 97);
        }
        return new String(passArray);
    }

    private static byte[] createChecksum(String filename) {
        try {
            final BufferedInputStream fis = new BufferedInputStream(new FileInputStream(filename));
            byte[] buffer = new byte[1024];
            final MessageDigest complete = MessageDigest.getInstance("SHA1");
            int numRead;
            do {
                numRead = fis.read(buffer);
                if (numRead > 0) {
                    complete.update(buffer, 0, numRead);
                }
            } while (numRead != -1);
            fis.close();
            return complete.digest();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
	 * Erzeugt eine MD5-Checksumme der �bergebenen Datei
	 * @param file
	 * @return
	 */
    public static String getSHA1Checksum(File file) {
        byte[] raw = createChecksum(file.getAbsolutePath());
        final byte[] HEX_CHAR_TABLE = { (byte) '0', (byte) '1', (byte) '2', (byte) '3', (byte) '4', (byte) '5', (byte) '6', (byte) '7', (byte) '8', (byte) '9', (byte) 'a', (byte) 'b', (byte) 'c', (byte) 'd', (byte) 'e', (byte) 'f' };
        byte[] hex = new byte[2 * raw.length];
        int index = 0;
        for (byte b : raw) {
            int v = b & 0xFF;
            hex[index++] = HEX_CHAR_TABLE[v >>> 4];
            hex[index++] = HEX_CHAR_TABLE[v & 0xF];
        }
        try {
            return new String(hex, "ASCII");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /**
   * Private Constructor with AssertionError to prevent creating
   * multiple instances of this Object
   */
    private HashGenerator() {
        throw new AssertionError();
    }
}
