package de.mnit.basis.crypt.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import de.mnit.basis.crypt.JavaBase64;
import de.mnit.basis.fehler.Fehler;

/**
 * @author Michael Nitsche
 * Erstellt: 2007-08-05
 */
public class JavaDigest {

    private final JAVA_DIGEST vg;

    private final MessageDigest md;

    public JavaDigest(JAVA_DIGEST vg) {
        Fehler.objekt.wenn_Null(vg);
        this.vg = vg;
        try {
            this.md = MessageDigest.getInstance(this.vg.javaName);
        } catch (NoSuchAlgorithmException e) {
            throw Fehler.weitergeben(e, "Ungültiger Algorithmus: " + vg.name() + " (" + vg.javaName + ")");
        }
    }

    public static JavaDigest neu(JAVA_DIGEST vg) {
        return new JavaDigest(vg);
    }

    public byte[] berechnen(byte[] text) {
        Fehler.objekt.wenn_Null(text);
        byte[] hash = md.digest(text);
        md.reset();
        return hash;
    }

    public void plus(byte[] ba) {
        md.update(ba);
    }

    public void plus(byte b) {
        md.update(b);
    }

    public void plus(byte[] ba, int offset, int len) {
        md.update(ba, offset, len);
    }

    public byte[] berechnen() {
        byte[] hash = md.digest();
        md.reset();
        return hash;
    }

    public String toString() {
        return JavaBase64.verschluesseln(berechnen());
    }
}
