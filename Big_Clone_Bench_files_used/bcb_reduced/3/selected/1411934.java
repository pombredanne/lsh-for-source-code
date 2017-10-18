package org.cofax.util.digest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.cofax.cms.CofaxToolsUtil;
import com.Ostermiller.util.Base64;

public class SHADigestHandler implements IDigestHandler {

    public static final String RCS_ID = "$Header: /cvsroot/cofax/cofax/src/org/cofax/util/digest/SHADigestHandler.java,v 1.1 2005/04/27 16:42:17 badrchentouf Exp $";

    private static final String hexits = "0123456789abcdef";

    private static final String id = "SHA";

    public boolean isSupported(String digest) {
        if (digest.regionMatches(true, 0, "{SHA}", 0, 5)) {
            return true;
        } else if (digest.regionMatches(true, 0, "{SSHA}", 0, 6)) {
            return true;
        }
        return false;
    }

    /**
	 * Verifie que le mot de passe correspond bien au hashage passe
	 * en parametre.
	 * Renvoi true si c'est le cas, false sinon
	 */
    public boolean checkPassword(String password, String digest) {
        boolean passwordMatch = false;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
            if (digest.regionMatches(true, 0, "{SHA}", 0, 5)) {
                digest = digest.substring(5);
            } else if (digest.regionMatches(true, 0, "{SSHA}", 0, 6)) {
                digest = digest.substring(6);
            }
            byte[][] hs = split(Base64.decode(digest.getBytes()), 20);
            byte[] hash = hs[0];
            byte[] salt = hs[1];
            sha.reset();
            sha.update(password.getBytes());
            sha.update(salt);
            byte[] pwhash = sha.digest();
            if (MessageDigest.isEqual(hash, pwhash)) {
                passwordMatch = true;
            }
        } catch (NoSuchAlgorithmException nsae) {
            CofaxToolsUtil.log("Algorithme SHA-1 non supporte a la verification du password" + nsae + id);
        }
        return passwordMatch;
    }

    /**
	 * Genere le hashage (digest) SSHA partir du mot de passe et du salt.
	 */
    public static String getSSHADigest(String password, String salt) {
        String digest = null;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(password.getBytes());
            sha.update(salt.getBytes());
            byte[] pwhash = sha.digest();
            digest = "{SSHA}" + new String(Base64.encode(concatenate(pwhash, salt.getBytes())));
        } catch (NoSuchAlgorithmException nsae) {
            CofaxToolsUtil.log("Algorithme SHA-1 non supporte a la creation du hashage" + nsae + id);
        }
        return digest;
    }

    /**
	 * Genere le hashage (digest) SHA partir du mot de passe et du salt.
	 */
    public static String getSHADigest(String password) {
        String digest = null;
        MessageDigest sha = null;
        try {
            sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(password.getBytes());
            byte[] pwhash = sha.digest();
            digest = "{SHA}" + new String(Base64.encode(pwhash));
        } catch (NoSuchAlgorithmException nsae) {
            CofaxToolsUtil.log("Algorithme SHA-1 non supporte a la creation du hashage" + nsae + id);
        }
        return digest;
    }

    private static byte[] concatenate(byte[] l, byte[] r) {
        byte[] b = new byte[l.length + r.length];
        System.arraycopy(l, 0, b, 0, l.length);
        System.arraycopy(r, 0, b, l.length, r.length);
        return b;
    }

    private static byte[][] split(byte[] src, int n) {
        byte[] l, r;
        if (src.length <= n) {
            l = src;
            r = new byte[0];
        } else {
            l = new byte[n];
            r = new byte[src.length - n];
            System.arraycopy(src, 0, l, 0, n);
            System.arraycopy(src, n, r, 0, r.length);
        }
        byte[][] lr = { l, r };
        return lr;
    }

    private static String toHex(byte[] block) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < block.length; ++i) {
            buf.append(hexits.charAt((block[i] >>> 4) & 0xf));
            buf.append(hexits.charAt(block[i] & 0xf));
        }
        return buf + "";
    }

    private static byte[] fromHex(String s) {
        s = s.toLowerCase();
        byte[] b = new byte[(s.length() + 1) / 2];
        int j = 0;
        int h;
        int nybble = -1;
        for (int i = 0; i < s.length(); ++i) {
            h = hexits.indexOf(s.charAt(i));
            if (h >= 0) {
                if (nybble < 0) {
                    nybble = h;
                } else {
                    b[j++] = (byte) ((nybble << 4) + h);
                    nybble = -1;
                }
            }
        }
        if (nybble >= 0) {
            b[j++] = (byte) (nybble << 4);
        }
        if (j < b.length) {
            byte[] b2 = new byte[j];
            System.arraycopy(b, 0, b2, 0, j);
            b = b2;
        }
        return b;
    }
}
