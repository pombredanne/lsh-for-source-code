package net.sf.jawp.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Simple utility for CHAP protocol and password manipulation.
 * 
 * protocol explanation 1. a) server has encoded pass for user b) user knows his
 * pass 2. server sends random string to client - (and stores it) 3. client
 * encodes his pass, adds string form server... and encoded both again - sends
 * result to server 4. server does some as above (apart from that it alredy has
 * encoded client pass) server compares its result with the one sent by client
 * 
 * @author jarek
 * @version $Revision: 1.8 $
 * 
 */
public final class PasswordUtil {

    private static final Log LOG = Log.getLog(PasswordUtil.class);

    private static final String PASSWORD_ENCODING = "UTF-8";

    private static final SecureRandom RANDOM;

    static {
        try {
            RANDOM = SecureRandom.getInstance("SHA1PRNG");
        } catch (final Throwable t) {
            LOG.error(t, t);
            throw new RuntimeException(t);
        }
    }

    private PasswordUtil() {
    }

    private static MessageDigest getMessageDigest() {
        try {
            return MessageDigest.getInstance("SHA");
        } catch (final NoSuchAlgorithmException e) {
            LOG.error(e, e);
            throw new RuntimeException(e);
        }
    }

    /**
	 * this ecrypts password using one of statndard funkctions
	 */
    public static String encrypt(final String anOrig) {
        try {
            final byte[] todig = anOrig.getBytes(PASSWORD_ENCODING);
            final byte[] res = getMessageDigest().digest(todig);
            return new String(res, PASSWORD_ENCODING);
        } catch (final UnsupportedEncodingException uoe) {
            throw new UnsupportedOperationException("encoding:" + PASSWORD_ENCODING + " unknown");
        }
    }

    /**
	 * 
	 * 
	 */
    public static String encryptMix(final String anEncrypted, final String seed) {
        return encrypt(anEncrypted + seed);
    }

    public static boolean checkPass(final String aMixed, final String seed, final String encrypted) {
        return aMixed.equals(encryptMix(encrypted, seed));
    }

    public static String generateRandomSeed() {
        final byte[] secureBT = new byte[10];
        RANDOM.nextBytes(secureBT);
        return new String(secureBT);
    }
}
