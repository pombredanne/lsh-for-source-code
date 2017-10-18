package cz.cvut.fel.mvod.crypto;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Knihovní třída poskytující kryptografické funkce potřebné pro program.
 * @author jakub
 */
public final class CryptoUtils {

    /**
	 * Řetězec používaný pro solení při hashování hesla.
	 */
    private static final String salt = "yV@!4\"=zfk9w&xDkkv[%1d(agt'|v0Oycg_ACDUt";

    /**
	 * Implementace SHA-2 hashovacího algoritmu.
	 * @param data řetězce k vytvoření hashe
	 * @return SHA-2 otisk (32 bajtů)
	 */
    private static byte[] sha2(String... data) {
        byte[] digest = new byte[32];
        StringBuilder buffer = new StringBuilder();
        for (String s : data) {
            buffer.append(s);
        }
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            assert false;
        }
        sha256.update(buffer.toString().getBytes());
        try {
            sha256.digest(digest, 0, digest.length);
        } catch (DigestException ex) {
            assert false;
        }
        return digest;
    }

    /**
	 * Spočítá hash hesla. SHA-2(password|userName|salt),
	 * kde | je oprátor zřetězení, a salt je tajný token použitý pro vyšší ochranu.
	 * @param password heslo
	 * @param userName uživatelské jméno
	 * @return 32 bajtový hash hesla
	 */
    public static byte[] passwordDigest(String password, String userName) {
        return sha2(password, userName, salt);
    }

    private CryptoUtils() {
    }
}
