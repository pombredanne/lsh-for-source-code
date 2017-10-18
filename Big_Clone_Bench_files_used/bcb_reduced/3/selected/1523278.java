package radius.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * 
 * @author zzzhc
 */
public class RadiusUtil {

    public static final int DEFAULT_CHALLENGE_SIZE = 16;

    private static final SecureRandom srand;

    private static final ThreadLocal local;

    static {
        srand = new SecureRandom();
        local = new ThreadLocal();
    }

    private RadiusUtil() {
    }

    public static final MessageDigest getMd5() {
        MessageDigest md5 = (MessageDigest) local.get();
        if (md5 == null) {
            try {
                md5 = MessageDigest.getInstance("MD5");
                local.set(md5);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not access MD5 algorithm, fatal error", e);
            }
        }
        md5.reset();
        return md5;
    }

    /**
	 * 
	 * @return a random byte to use as a chap Identifier
	 */
    public static final byte getNextChapIdentifier() {
        synchronized (srand) {
            return (byte) srand.nextInt(256);
        }
    }

    /**
	 * Get a fixed number of bytes to use as a chap challenge, generally you
	 * don't want this to be less than the number of bytes outbut by the hash
	 * algoritm to be used to encrypt the password, in this case 16 since Radius
	 * rfc 2865 section 2.2 specifies MD5 if size is <1 we will use the default
	 * of 16
	 * 
	 * @param size
	 *            number of bytes the challenge will be
	 * @return  bytes of random data to use as a chapchallenge
	 */
    public static final byte[] getNextChapChallenge(final int size) {
        byte[] challenge = new byte[size];
        synchronized (srand) {
            srand.nextBytes(challenge);
        }
        return challenge;
    }

    /**
	 * This method performs the CHAP encryption according to RFC 2865 section
	 * 2.2 for use with Radius Servers using MD5 as the one way hashing
	 * algorithm
	 * 
	 * @param chapIdentifier
	 *            a byte to help correlate unique challenges/responses
	 * @param plaintextPassword
	 *            exactly what it says.
	 * @param chapChallenge
	 *            the bytes to encode the plaintext password with
	 * @return the encrypted password as a byte array (16 bytes to be exact as a
	 *         result of the MD5 process)
	 */
    public static final byte[] chapEncrypt(final byte chapIdentifier, final byte[] plaintextPassword, byte[] chapChallenge) {
        byte[] chapPassword = plaintextPassword;
        MessageDigest md5 = getMd5();
        md5.update(chapIdentifier);
        md5.update(plaintextPassword);
        chapPassword = md5.digest(chapChallenge);
        return chapPassword;
    }

    public static final byte[] decryptPassword(byte[] requestAuthenticator, byte[] encrypt, byte[] secret) {
        assert encrypt != null && encrypt.length % 16 == 0;
        byte[] cc = doCompute(requestAuthenticator, encrypt, secret, false);
        int len = cc.length;
        for (; len > 0; len--) {
            if (cc[len - 1] != 0) break;
        }
        byte[] bb = new byte[len];
        System.arraycopy(cc, 0, bb, 0, len);
        return bb;
    }

    private static final byte[] doCompute(byte[] ra, byte[] p_e, byte[] s, boolean encrypt) {
        assert ra != null && ra.length == 16;
        assert p_e != null;
        MessageDigest md5 = getMd5();
        byte[] b, c;
        int len = p_e.length;
        int n = len / 16;
        int left = len % 16;
        int count = (len + 16 - 1) / 16;
        byte[] cc = new byte[count * 16];
        for (int i = 0; i < n; i++) {
            md5.reset();
            md5.update(s);
            if (i == 0) {
                md5.update(ra);
            } else {
                if (encrypt) {
                    md5.update(cc, (i - 1) * 16, 16);
                } else {
                    md5.update(p_e, (i - 1) * 16, 16);
                }
            }
            c = md5.digest();
            for (int j = 0; j < 16; j++) {
                cc[i * 16 + j] = (byte) (p_e[i * 16 + j] ^ c[j]);
            }
        }
        if (n < count) {
            byte[] b16 = new byte[16];
            System.arraycopy(p_e, n * 16, b16, 0, left);
            md5.reset();
            md5.update(s);
            if (n == 0) {
                md5.update(ra);
            } else {
                if (encrypt) {
                    md5.update(cc, (n - 1) * 16, 16);
                } else {
                    md5.update(p_e, (n - 1) * 16, 16);
                }
            }
            c = md5.digest();
            for (int j = 0; j < 16; j++) {
                cc[n * 16 + j] = (byte) (b16[j] ^ c[j]);
            }
        }
        return cc;
    }

    /**
	 * Call the shared secret S and the pseudo-random 128-bit Request<br>
	 * Authenticator RA. Break the password into 16-octet chunks p1, p2,<br>
	 * etc. with the last one padded at the end with nulls to a 16-octet<br>
	 * boundary. Call the ciphertext blocks c(1), c(2), etc. We'll need<br>
	 * intermediate values b1, b2, etc.<br>
	 * b1 = MD5(S + RA) c(1) = p1 xor b1<br>
	 * b2 = MD5(S + c(1)) c(2) = p2 xor b2<br> . .<br> . .<br> . .<br>
	 * bi = MD5(S + c(i-1)) c(i) = pi xor bi<br>
	 * The String will contain c(1)+c(2)+...+c(i) where + denotes<br>
	 * concatenation.<br>
	 * 
	 * @param password
	 * @param secret
	 * @return
	 */
    public static final byte[] encryptPassword(byte[] requestAuthenticator, byte[] password, byte[] secret) {
        assert requestAuthenticator != null && requestAuthenticator.length == 16;
        assert password != null;
        return doCompute(requestAuthenticator, password, secret, true);
    }

    public static final byte[] computeChap(byte id, byte[] password, byte[] challenge) {
        MessageDigest md5 = getMd5();
        md5.update(id);
        md5.update(password);
        md5.update(challenge);
        return md5.digest();
    }

    public static void main(String[] args) {
        byte[] requestAuthenticator = new byte[16];
        for (int i = 0; i < 16; i++) {
            requestAuthenticator[i] = (byte) i;
        }
        String p = "1334556756jgfjhkytuyukyuktuykuykuykgjkgkjfk";
        String s = "1234567890";
        byte[] cc = null;
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            cc = encryptPassword(requestAuthenticator, p.getBytes(), s.getBytes());
        }
        long end = System.currentTimeMillis();
        System.out.println("time=" + (end - start) + "ms,avg=" + (end - start) * 1.0 / 1000 + "/ms\t\n" + cc.length + "\t" + ConvertUtil.toHex(cc));
        byte[] pb = decryptPassword(requestAuthenticator, cc, s.getBytes());
        System.out.println(new String(pb));
    }
}
