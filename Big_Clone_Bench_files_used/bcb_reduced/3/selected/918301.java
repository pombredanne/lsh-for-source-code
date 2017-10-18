package jws.util;

import java.security.ProtectionDomain;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * A collection of various uncategorized helper routines
 */
public final class SysUtils {

    private SysUtils() {
    }

    /**
     * Checks if the specified value is not null. Helpful when checking method's arguments' values
     * @param name Name of the value being checked, which is passed to exception if check fails
     * @param value Value to be checked
     * @exception NullPointerException If <code>value</code> is <code>null</code>
     */
    public static void checkNotNull(String name, Object value) {
        if (value == null) {
            throw new NullPointerException(name + " can not be null");
        }
    }

    /**
     * Checks if the specified numeric value is within certain bounds. Helpful when checking method's arguments' values
     * @param name Name of the value being checked, which is passed to exception if check fails
     * @param value Value to be checked
     * @param low Minimum allowed value for <code>value</code>
     * @param high Maximum allowed value for <code>value</code>
     * @exception ArrayIndexOutOfBoundsException If <code>(value < low) || (value > high)</code>
     * produces <code>true</code>
     */
    public static void checkValueWithin(String name, Number value, Number low, Number high) {
        checkNotNull(name, value);
        if (value.doubleValue() < low.doubleValue() || value.doubleValue() > high.doubleValue()) {
            throw new ArrayIndexOutOfBoundsException(name + " must be within " + low + " and " + high);
        }
    }

    /**
     * Returns path to the class' codebase.<br/>
     * The codebase of a class is determined by its {@link java.security.ProtectionDomain}
     * and may be <code>null</code> if the class was dynamically defined.
     * @param cls The class who's codebase is to be returned
     * @return URI of the <code>cls</code>'s codebase
     */
    public static String getCodebase(Class cls) {
        try {
            ProtectionDomain pDomain = cls.getProtectionDomain();
            File file = new File(pDomain.getCodeSource().getLocation().toURI());
            return file.getParent();
        } catch (Throwable th) {
            return null;
        }
    }

    /**
     * Creates a unique identifier for a specified object
     * @return A new UID for <code>obj</code>
     */
    public static synchronized String createUid() {
        String uid = (new java.rmi.server.UID()).toString();
        try {
            return encrypt(uid, "MD5");
        } catch (Throwable th) {
            return uid;
        }
    }

    /**
     * Encrypts the specified data with the given encryption algorithm
     * @param data Data to encrypt
     * @param algorithm Algorithm to be used for encryption
     * @return <code>data</code>, encrypted with <code>algorithm</code>
     * @throws NoSuchAlgorithmException If the specified algorithm is not supported
     */
    public static String encrypt(String data, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance(algorithm);
        byte result[] = md5.digest(UTF8.encode(data));
        StringBuffer sb = new StringBuffer();
        for (byte b : result) {
            String s = Integer.toHexString(b);
            int length = s.length();
            if (length >= 2) {
                sb.append(s.substring(length - 2, length));
            } else {
                sb.append("0");
                sb.append(s);
            }
        }
        return sb.toString();
    }

    /**
     * Converts a character to hex-encoded string
     * @param c Character to convert
     * @return Hex-encoded string representation of <code>c</code>
     */
    public static String charToHex(char c) {
        byte hi = (byte) (c >>> 8);
        byte lo = (byte) (c & 0xff);
        return byteToHex(hi) + byteToHex(lo);
    }

    private static String byteToHex(byte b) {
        char hexDigit[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        return new String(array);
    }

    public static void pump(InputStream ins, OutputStream outs) throws IOException {
        byte[] buff = new byte[4096];
        int count;
        do {
            count = ins.read(buff, 0, 4096);
            if (count > 0) {
                outs.write(buff, 0, count);
            }
        } while (count > 0);
    }
}
