package brainlink.core.network.protocol;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NullCipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the control class for the use of cyptographic algorithms
 * in brainlink. With the current version of NTE, DES is optionally
 * used to encrypt network traffic.
 * 
 * TODO: currently exhibiting strange behaviour where packets encrypted
 * here are readable by other brainlink clients and by NTE but packets
 * sent by NTE are not readable here?
 * 
 * @author iain
 */
public class ProtocolCryptographyManager {

    /**
     * The default DES initialization vector for NTE.
     */
    private static final byte[] NTE_INITVECTOR = { 0, 0, 0, 0, 0, 0, 0, 0 };

    private static final String NTE_DES_VARIANT = "DES/CBC/PKCS5Padding";

    private static final String NTE_CIPHER_SHORT_NAME = "DES";

    private String cipherName;

    private String cipherShortName;

    private IvParameterSpec cipherParams;

    private KeySpec cipherKey;

    private static final Log log = LogFactory.getLog(ProtocolCryptographyManager.class);

    /**
     * Creates a cryptography manager with encryption turned on or
     * off for all subsequent operations using this manager.
     * @param sessionPassword the password that is to be used as the
     * key for this session. If this is null, then encryption is
     * disabled for this session.
     * @throws NoSuchAlgorithmException if there is a problem accessing
     * the cryptography library.
     * @throws NoSuchPaddingException if there is a problem accessing
     * the cryptography library.
     */
    public ProtocolCryptographyManager(String sessionPassword) {
        if (sessionPassword != null) {
            cipherName = NTE_DES_VARIANT;
            cipherShortName = NTE_CIPHER_SHORT_NAME;
            try {
                cipherKey = generateDESKeySpec(sessionPassword);
                cipherParams = new IvParameterSpec(NTE_INITVECTOR);
            } catch (Exception e) {
                log.error("error in generating DES key from session password - falling back to plaintext mode");
                cipherName = null;
            }
        } else {
            cipherName = null;
            cipherKey = null;
        }
    }

    private KeySpec generateDESKeySpec(String sessionPassword) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] desKey = getDESKeyBytes(sessionPassword);
        return new DESKeySpec(desKey);
    }

    /**
     * Generates an NTE-compatible DES key from the session password.
     */
    private Key generateDESKey(KeySpec desKeySpec) throws NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException {
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        return keyFactory.generateSecret(desKeySpec);
    }

    /**
     * Generates the 8-byte DES key for the given session password in
     * a way that is compatible with the key generation used in
     * NTE. In NTE, the key is generated from a session password
     * by generating an MD5 56-bit hash from the session password,
     * and then adding an extra pad byte for every 7 bits of the hash.
     * The pad bit is the most significant bit of each byte in the
     * MD5 hash.
     * @param sessionPassword the session password that is to
     * be used to generate the DES key.
     */
    private byte[] getDESKeyBytes(String sessionPassword) throws NoSuchAlgorithmException {
        int i, j, k;
        byte[] passBytes = getPasswordBytes(sessionPassword);
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        byte[] md5bytes = md5Digest.digest(passBytes);
        int[] convertedMD5Bytes = new int[7];
        for (i = 0; i < 7; i++) {
            convertedMD5Bytes[i] = (int) md5bytes[i] & 0xff;
        }
        String md5BytesString = "md5 bytes: ";
        for (i = 0; i < md5bytes.length; i++) {
            md5BytesString += Integer.toHexString(md5bytes[i]) + " ";
        }
        log.info(md5BytesString);
        byte[] desBytes = new byte[8];
        desBytes[0] = (byte) convertedMD5Bytes[0];
        desBytes[1] = (byte) (convertedMD5Bytes[0] << 7 | convertedMD5Bytes[1] >>> 1);
        desBytes[2] = (byte) (convertedMD5Bytes[1] << 6 | convertedMD5Bytes[2] >>> 2);
        desBytes[3] = (byte) (convertedMD5Bytes[2] << 5 | convertedMD5Bytes[3] >>> 3);
        desBytes[4] = (byte) (convertedMD5Bytes[3] << 4 | convertedMD5Bytes[4] >>> 4);
        desBytes[5] = (byte) (convertedMD5Bytes[4] << 3 | convertedMD5Bytes[5] >>> 5);
        desBytes[6] = (byte) (convertedMD5Bytes[5] << 2 | convertedMD5Bytes[6] >>> 6);
        desBytes[7] = (byte) (convertedMD5Bytes[6] << 1);
        String desKeyString = "des key (before pad): ";
        for (i = 0; i < 8; i++) {
            desKeyString += Integer.toHexString(desBytes[i]) + " ";
        }
        log.info(desKeyString);
        for (i = 0; i < 8; ++i) {
            k = desBytes[i] & 0xfe;
            j = k;
            j ^= j >>> 4;
            j ^= j >>> 2;
            j ^= j >>> 1;
            j = (j & 1) ^ 1;
            desBytes[i] = (byte) (k | j);
        }
        desKeyString = "des key: ";
        for (i = 0; i < 8; i++) {
            desKeyString += Integer.toHexString(desBytes[i]) + " ";
        }
        log.info(desKeyString);
        return desBytes;
    }

    /**
     * Converts the session password into ASCII bytes, as opposed
     * to java's usual unicode 2-byte characters. This is to
     * retain compatibility with NTE.
     */
    private byte[] getPasswordBytes(String sessionPassword) {
        byte[] sessionPassBytes = new byte[sessionPassword.length()];
        for (int i = 0; i < sessionPassword.length(); i++) {
            sessionPassBytes[i] = (byte) sessionPassword.charAt(i);
        }
        return sessionPassBytes;
    }

    /**
     * Generates the required DES algorithm parameters to be
     * compatible with NTE's DES encryption scheme.
     */
    private AlgorithmParameters generateDESAlgorithmParams() throws NoSuchAlgorithmException, InvalidParameterSpecException {
        AlgorithmParameters params = AlgorithmParameters.getInstance(cipherShortName);
        params.init(cipherParams);
        return params;
    }

    /**
     * Returns a new instance of the decryption cipher for
     * decrypting packets.  A new instance is required each
     * time as the Cipher stores internal state when encoding
     * a bit stream.
     * @return
     */
    private Cipher getDecryptCipher() {
        try {
            Cipher decryptCipher;
            if (cipherName == null) {
                decryptCipher = new NullCipher();
            } else {
                decryptCipher = Cipher.getInstance(cipherName);
                Key desKey = generateDESKey(cipherKey);
                AlgorithmParameters desParams = generateDESAlgorithmParams();
                decryptCipher.init(Cipher.DECRYPT_MODE, desKey, desParams);
            }
            return decryptCipher;
        } catch (Exception e) {
            log.fatal("unable to create decryption cipher:\n " + e);
            System.exit(1);
            return null;
        }
    }

    /**
     * Returns a new instance of the encryption cipher for
     * encrypting packets. A new instance is required each
     * time as the Cipher stores internal state when encoding
     * a bit stream.
     */
    private Cipher getEncryptCipher() {
        try {
            Cipher encryptCipher;
            if (cipherName == null) {
                encryptCipher = new NullCipher();
            } else {
                encryptCipher = Cipher.getInstance(cipherName);
                Key desKey = generateDESKey(cipherKey);
                AlgorithmParameters desParams = generateDESAlgorithmParams();
                encryptCipher.init(Cipher.ENCRYPT_MODE, desKey, desParams);
            }
            return encryptCipher;
        } catch (Exception e) {
            log.fatal("unable to create encryption cipher:\n" + e);
            System.exit(1);
            return null;
        }
    }

    /**
     * Returns a data input stream which can be used to read the encoded data
     * from the packet. This will also handle any cryptography concerns, so
     * the data that is retrieved is the plaintext of the message.
     */
    public DataInputStream getPacketInputStream(DatagramPacket receivedPacket) {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(receivedPacket.getData(), receivedPacket.getOffset(), receivedPacket.getLength());
        Cipher decryptCipher = getDecryptCipher();
        CipherInputStream cipherStream = new CipherInputStream(byteStream, decryptCipher);
        return new DataInputStream(cipherStream);
    }

    /**
     * Returns a packet data output stream for writing the contents of a
     * packet. The stream will automatically handle any cryptography concerns,
     * and the packet which can be transmitted can be retrieved from the stream
     * using the getPacket method.
     */
    public PacketDataOutputStream getPacketOutputStream() {
        return PacketDataOutputStream.createPacketDataOutputStream(this);
    }

    /**
     * Creates a CipherOutputStream layered on top of the provided output
     * stream, which will encrypt bytes with the encryption settings of
     * this protocol cryptography manager.
     * @param outputStream the output stream that the cipher stream will
     * write into.
     */
    CipherOutputStream getCipherOutputStream(OutputStream outputStream) {
        Cipher encryptCipher = getEncryptCipher();
        return new CipherOutputStream(outputStream, encryptCipher);
    }
}
