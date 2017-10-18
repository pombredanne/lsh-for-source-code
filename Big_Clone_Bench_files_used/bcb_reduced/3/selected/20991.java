package medieveniti.util.generator;

import java.security.NoSuchAlgorithmException;
import medieveniti.util.SecurityUtilities;
import medieveniti.util.StringUtilities;

/**
 * Die Klasse erstellt zufällige, nahezu einmalige Codes.
 * @author Hans Kirchner
 */
public class UIDGenerator {

    /**
	 * Erstellt einen Code mit einer bestimmten Länge.
	 * @param length Länge des Codes
	 * @return Code
	 */
    public static String generate(int length) throws NoSuchAlgorithmException {
        StringBuilder string = new StringBuilder(length);
        byte[] buffer = new byte[32];
        while (string.length() < length) {
            SecurityUtilities.getSecureRandom().nextBytes(buffer);
            string.append(StringUtilities.toHex(SecurityUtilities.createMessageDigest().digest(buffer)));
        }
        string.setLength(length);
        return string.toString();
    }
}
