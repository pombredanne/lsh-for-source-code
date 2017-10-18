package net.sf.jradius.util;

import java.security.MessageDigest;
import java.util.Random;

/**
 * Radius Utilities
 * 
 * @author David Bird
 */
public final class RadiusUtils {

    /**
     * This method encodes the plaintext user password according to RFC 2865
     * @param userPass java.lang.String the password to encrypt
     * @param requestAuthenticator byte[] the requestAuthenicator to use in the encryption
     * @return byte[] the byte array containing the encrypted password
     */
    public static byte[] encodePapPassword(final MessageDigest md5, final byte[] userPass, final byte[] requestAuthenticator, final String sharedSecret) {
        byte[] userPassBytes = null;
        if (userPass.length > 128) {
            userPassBytes = new byte[128];
            System.arraycopy(userPass, 0, userPassBytes, 0, 128);
        } else {
            userPassBytes = userPass;
        }
        byte[] encryptedPass = null;
        if (userPassBytes.length < 128) {
            if (userPassBytes.length % 16 == 0) {
                encryptedPass = new byte[userPassBytes.length];
            } else {
                encryptedPass = new byte[((userPassBytes.length / 16) * 16) + 16];
            }
        } else {
            encryptedPass = new byte[128];
        }
        System.arraycopy(userPassBytes, 0, encryptedPass, 0, userPassBytes.length);
        for (int i = userPassBytes.length; i < encryptedPass.length; i++) {
            encryptedPass[i] = 0;
        }
        md5.reset();
        md5.update(sharedSecret.getBytes());
        md5.update(requestAuthenticator);
        byte bn[] = md5.digest();
        for (int i = 0; i < 16; i++) {
            encryptedPass[i] = (byte) (bn[i] ^ encryptedPass[i]);
        }
        if (encryptedPass.length > 16) {
            for (int i = 16; i < encryptedPass.length; i += 16) {
                md5.reset();
                md5.update(sharedSecret.getBytes());
                md5.update(encryptedPass, i - 16, 16);
                bn = md5.digest();
                for (int j = 0; j < 16; j++) {
                    encryptedPass[i + j] = (byte) (bn[j] ^ encryptedPass[i + j]);
                }
            }
        }
        return encryptedPass;
    }

    /**
     * This method builds a Request Authenticator for use in outgoing RADIUS
     * Access-Request packets as specified in RFC 2865.
     * @return byte[]
     */
    public static byte[] makeRFC2865RequestAuthenticator(final MessageDigest md5, final String sharedSecret) {
        byte[] requestAuthenticator = new byte[16];
        Random r = new Random();
        for (int i = 0; i < 16; i++) {
            requestAuthenticator[i] = (byte) r.nextInt();
        }
        md5.reset();
        md5.update(sharedSecret.getBytes());
        md5.update(requestAuthenticator);
        return md5.digest();
    }

    /**
     * This method builds a Response Authenticator for use in validating
     * responses from the RADIUS Authentication process as specified in RFC 2865.
     * The byte array returned should match exactly the response authenticator
     * recieved in the response packet.
     * @param code byte
     * @param identifier byte
     * @param length short
     * @param requestAuthenticator byte[]
     * @param responseAttributeBytes byte[]
     * @return byte[]
     */
    public static byte[] makeRFC2865ResponseAuthenticator(final MessageDigest md5, final String sharedSecret, byte code, byte identifier, short length, byte[] requestAuthenticator, byte[] responseAttributeBytes) {
        md5.reset();
        md5.update((byte) code);
        md5.update((byte) identifier);
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuthenticator, 0, requestAuthenticator.length);
        md5.update(responseAttributeBytes, 0, responseAttributeBytes.length);
        md5.update(sharedSecret.getBytes());
        return md5.digest();
    }

    /**
     * This method builds a Request Authenticator for use in RADIUS Accounting
     * packets as specified in RFC 2866.
     * @param code byte
     * @param identifier byte
     * @param length short
     * @param requestAttributes byte[]
     * @return byte[]
     */
    public static byte[] makeRFC2866RequestAuthenticator(final MessageDigest md5, final String sharedSecret, byte code, byte identifier, int length, byte[] requestAttributes) {
        byte[] requestAuthenticator = new byte[16];
        for (int i = 0; i < 16; i++) {
            requestAuthenticator[i] = 0;
        }
        md5.reset();
        md5.update((byte) code);
        md5.update((byte) identifier);
        md5.update((byte) (length >> 8));
        md5.update((byte) (length & 0xff));
        md5.update(requestAuthenticator, 0, requestAuthenticator.length);
        md5.update(requestAttributes, 0, requestAttributes.length);
        md5.update(sharedSecret.getBytes());
        return md5.digest();
    }

    /**
     * Converts a hex array to a human readable string
     * @param in bytes to be hexed
     * @return Returns a hex string
     */
    public static String byteArrayToHexString(byte in[]) {
        byte ch = 0x00;
        int i = 0;
        if (in == null || in.length <= 0) return null;
        String pseudo[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
        StringBuffer out = new StringBuffer(in.length * 2);
        while (i < in.length) {
            ch = (byte) (in[i] & 0xF0);
            ch = (byte) (ch >>> 4);
            ch = (byte) (ch & 0x0F);
            out.append(pseudo[(int) ch]);
            ch = (byte) (in[i] & 0x0F);
            out.append(pseudo[(int) ch]);
            i++;
        }
        String rslt = new String(out);
        return rslt;
    }
}
