package mas.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Stellt die Berechnung von Hashwerten bereit
 * @author hypersquirrel
 */
public class Digest {

    /**
	 * Berechnet einen Hashwert
	 * @param b Daten deren Hash berechnet werden soll
	 * @param alg Der zu verwendende Algorithmus
	 * @return der Hashwert Base64 codiert
	 * @throws NoSuchAlgorithmException falls der Algorithmus nicht gefunden werden konnte
	 */
    public static String digest(byte b[], String alg) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(alg);
        md.update(b);
        return Base64.encodeBytes(md.digest());
    }

    /**
	 * Berechnet einen Hashwert nach SHA
	 * @param b Daten deren Hash berechnet werden soll
	 * @return der Hashwert Base64 codiert
	 * @throws NoSuchAlgorithmException falls SHA nicht gefunden werden konnte
	 */
    public static String digest(byte b[]) throws NoSuchAlgorithmException {
        return digest(b, "SHA");
    }
}
