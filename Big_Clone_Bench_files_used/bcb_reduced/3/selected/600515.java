package sevs.crypto;

import java.io.*;
import javax.crypto.*;
import java.security.*;
import java.security.GeneralSecurityException;

/**
 * Cryptographic utility methods.
 */
public class Util {

    /**
     * Serializes a given object, encrypt it with a given key,
     * then sends it to the given output stream, so that the
     * other end can decrypt it by using the same shared key.
     * 
     * <p>
     * The caller cannot assume any particular encoding scheme.
     * The only contract made by this interface is that
     * it must be possible to get the same object by decrypting
     * it with the same key.
     * 
     * <p>
     * The encryption scheme we adopt is as follows:
     * <pre>
     * Ks{length},Ks{message,padding}
     * </pre>
     * where the message is a serialized form of the object,
     * the length is the number of bytes of the message.
     * 
     * The size of the message plus padding will always be
     * a multiple of 8 (as DES works on 8 byte blocks.)
     * 
     * @param   key
     *      A shared secret key which should be used to encrypt
     *      the message.
     * @param   obj
     *      An object which will be sent to the output stream
     *      in an encrpyted form.
     * @param   out
     *      A stream that receives encrypted image of the object.
     */
    public static void encrypt(SecretKey key, Serializable obj, OutputStream out) throws IOException {
        byte[] dataBuf = serialize(obj);
        int msgLen = dataBuf.length;
        byte[] len = intToByteArray(msgLen);
        int totalLength = ((msgLen + 7) / 8) * 8;
        byte[] encryptBuf = new byte[totalLength];
        System.arraycopy(dataBuf, 0, encryptBuf, 0, msgLen);
        Cipher cipher = null;
        try {
            cipher = Cipher.getInstance("DES", "CryptixCrypto");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            addRandomPadding(encryptBuf, msgLen);
            encryptBuf = cipher.doFinal(encryptBuf);
            len = cipher.doFinal(len);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException("Cryptix library is not installed");
        }
        out.write(len);
        out.write(encryptBuf);
        out.flush();
    }

    /**
     * Serializes an object into a byte array and returns it.
     */
    private static byte[] serialize(Serializable obj) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(byteStream));
        os.writeObject(obj);
        os.close();
        return byteStream.toByteArray();
    }

    /**
     * Deserializes an object from the given byte array.
     */
    private static Serializable deserialize(byte[] bytes) throws IOException {
        try {
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return (Serializable) ois.readObject();
        } catch (ClassNotFoundException e) {
            throw new NoClassDefFoundError(e.getMessage());
        }
    }

    /**
     * Reads n bytes from the stream and returns them as an byte array.
     */
    private static byte[] read(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int i = 0;
        while (i < n) {
            int k = in.read(buf, i, n - i);
            if (k == -1) throw new IOException("Unexpected EOF");
            i += k;
        }
        return buf;
    }

    /**
     * Decrypts a ciphertext which was encrypted by the encrypt method.
     * 
     * @param
     *      A shared secret key to be used to decipher the data
     * @param
     *      An input stream from which encrypted data must be read.
     */
    public static Serializable decrypt(SecretKey key, InputStream in) throws IOException {
        try {
            Cipher cipher = Cipher.getInstance("DES", "CryptixCrypto");
            cipher.init(Cipher.DECRYPT_MODE, key);
            int msgLen = byteArrayToInt(cipher.doFinal(read(in, 8)));
            int totalLength = ((msgLen + 7) / 8) * 8;
            byte[] msg = read(in, totalLength);
            msg = cipher.doFinal(msg);
            return deserialize(msg);
        } catch (GeneralSecurityException e) {
            e.printStackTrace();
            throw new IOException("Cryptix library is not installed");
        }
    }

    /**
     * Converts an integer into a byte array of 8 bytes.
     */
    private static byte[] intToByteArray(int n) {
        String in = String.valueOf(n);
        byte[] temp = new byte[8];
        int counter = 7;
        for (int i = in.length(); i > 0; i--) {
            String s = in.substring(i - 1, i);
            temp[counter] = (byte) (Integer.parseInt(s));
            counter--;
        }
        return temp;
    }

    /**
	 * Converts an byte array to the integer.
	 * This function does the reverse operation of the intToByteArray method.
	 */
    private static int byteArrayToInt(byte[] in) {
        StringBuffer out = new StringBuffer();
        for (int i = 0; i < in.length; i++) out.append((char) ('0' + in[i]));
        return Integer.parseInt(out.toString());
    }

    /**
	 * Used to add random paddings.
	 */
    private static SecureRandom sr;

    /** Gets the secure random object that the caller can use. */
    public static SecureRandom getSecureRandom() {
        return sr;
    }

    static {
        try {
            sr = SecureRandom.getInstance("SHA1PRNG");
            sr.nextBoolean();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Fills-in random bytes from buf[start] till the end of the buffer.
	 */
    private static void addRandomPadding(byte[] buf, int start) {
        if (buf.length == start) return;
        byte[] rndPad = new byte[buf.length - start];
        synchronized (sr) {
            sr.nextBytes(rndPad);
        }
        System.arraycopy(rndPad, 0, buf, start, rndPad.length);
    }

    /**
     * Computes the hexadecimal dump of the given byte array.
     * Useful for trace messages.
     */
    public static String hexEncode(byte[] b) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            String t = Integer.toHexString(b[i]);
            switch(t.length()) {
                case 1:
                    buf.append('0');
                    buf.append(t);
                    break;
                case 2:
                    buf.append(t);
                    break;
                case 8:
                    buf.append(t.substring(6));
                    break;
                default:
                    throw new InternalError();
            }
            if ((i % 4) == 0 && i != 0) buf.append(' ');
        }
        return buf.toString();
    }

    /**
     * Decodes the hexadecimal dump into a byte array.
     */
    public static byte[] hexDecode(String str) {
        final int len = str.length() / 2;
        byte[] r = new byte[len];
        for (int i = 0; i < len; i++) r[i] = (byte) Integer.parseInt(str.substring(i * 2, (i + 1) * 2), 16);
        return r;
    }

    /** hashes given byte array. */
    public static byte[] hash(byte[] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5 not supported");
        }
        return md.digest(data);
    }

    /** overloaded version of the hash method that handles String. */
    public static String hash(String data) {
        return Base64.encode(hash(data.getBytes()));
    }
}
