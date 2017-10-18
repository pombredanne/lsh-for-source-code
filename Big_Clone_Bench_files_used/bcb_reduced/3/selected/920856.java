package medieveniti.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import medieveniti.util.generator.UIDGenerator;
import medieveniti.util.validator.StringValidator;

/**
 * Klasse, die für alles zuständig ist, was mit Sicherheit zu tun hat.
 * Sie validiert Eingaben, generiert Zufallzahlen, und verschlüsselt Passwörter.
 * @author Hans Kirchner
 */
public class SecurityUtilities {

    private static Random random;

    private static SecureRandom secureRandom;

    private static HashMap<String, StringValidator> validators;

    static {
        try {
            random = new Random();
            secureRandom = SecureRandom.getInstance("SHA1PRNG");
            secureRandom.setSeed(System.currentTimeMillis());
            validators = new HashMap<String, StringValidator>();
            validators.put("mail", new StringValidator(Pattern.compile("[^\\s<>]+@[^\\s<>]+\\.[^\\s<>]{2,6}")));
            validators.put("username", new StringValidator(Pattern.compile("[A-Za-z_.][A-Za-z0-9_.,-]{1,28}[A-Za-z0-9.]")));
            validators.put("password", new StringValidator(Pattern.compile(".{3,50}")));
        } catch (Exception e) {
        }
    }

    /**
	 * Gibt ein Random-Objekt zurück.
	 * @return Random-Objekt
	 * TODO möglicherweise entstehen Probleme, wenn mehrer Threads auf ein und dasselbe Random-Objekt zugreifen.
	 */
    public static Random getRandom() {
        return random;
    }

    /**
	 * Gibt ein SecureRandom-Objekt zurück. Dieses generiert vermutlich bessere Zufallszahlen als das normale
	 * Zufalls-Objekt.
	 * @return SecureRandom-Objekt
	 */
    public static SecureRandom getSecureRandom() {
        return secureRandom;
    }

    /**
	 * Gibt einen MessageDigest zurück, mit dem zum Beispiel Passwörter verschlüsselt werden können.
	 * @return einen MessageDigest
	 * @throws NoSuchAlgorithmException
	 */
    public static MessageDigest createMessageDigest() throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA");
    }

    /**
	 * Generiert eine Session-ID.
	 * @deprecated Wird nicht mehr benötigt, da das interne Session-System verwendet wird.
	 */
    public static String generateSessionId() throws NoSuchAlgorithmException {
        return UIDGenerator.generate(30);
    }

    /**
	 * Generiert ein Passwort.
	 */
    public static String generatePassword() throws NoSuchAlgorithmException {
        return UIDGenerator.generate(15);
    }

    /**
	 * Generiert einen Aktivierungscode.
	 */
    public static String generateActivationCode() throws NoSuchAlgorithmException {
        return UIDGenerator.generate(25);
    }

    /**
	 * Generiert einen Passwort-Vergessen-Code.
	 */
    public static String generateForgotPasswordCode() throws NoSuchAlgorithmException {
        return UIDGenerator.generate(25);
    }

    /**
	 * Validiert einen Benutzernamen.
	 */
    public static boolean validateUsername(String string) {
        return validators.get("username").validate(string);
    }

    /**
	 * Validiert eine Mail-Adresse.
	 */
    public static boolean validateMailAddress(String string) {
        return validators.get("mail").validate(string);
    }

    /**
	 * Validiert ein Passwort.
	 */
    public static boolean validatePassword(String string) {
        return validators.get("password").validate(string);
    }

    /**
	 * Verschlüsselt ein Passwort mit einem Hash-Algorithmus (zur Zeit SHA-1).
	 */
    public static String encryptPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return StringUtilities.toHex(createMessageDigest().digest(password.getBytes("utf-8")));
    }

    /**
	 * Macht ein Passwort unkenntlich, indem ein bestimmtes Zeichen wiederholt wird.
	 * Dafür wird *, ● oder • verwendet.
	 */
    public static String obscurePassword(String password) {
        return StringUtilities.repeatChar('•', password.length());
    }
}
