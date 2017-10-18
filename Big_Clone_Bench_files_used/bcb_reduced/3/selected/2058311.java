package wsl.licence;

import java.security.*;
import java.util.Vector;
import java.util.Random;
import java.util.zip.CRC32;

/**
 * Title:        Mobile Data Now
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      WAP Solutions Ltd
 * @author Paul Lupton
 * @version 1.0
 */
public class KeyManager {

    private static final String USER_INFO = "Paul Lupton\n3 Arcadia Cres\nGlenfield\nAuckland\npaulus@consultant.com";

    private static final String DIGEST_ALGORITHM = "SHA";

    private static final String SIGNATURE_ALGORITHM = "DSA";

    private MessageDigest _digest = null;

    private Signature _sig = null;

    private KeyPair _keyPair = null;

    private static final String APP_VERSION = "1.2.0.54";

    private byte _seed[];

    private static final int APOS = 0;

    private static final int aPOS = 26;

    private static final int numPOS = 52;

    private static final int symPOS = 62;

    private static final char _sourceChars[] = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '-' };

    private static final char _scrambledChars[] = { 'D', 'i', '3', 'L', 'c', 'h', 'F', 'v', 'r', 'Z', 'k', 'C', 'X', 'K', 'N', 'U', 'f', 'm', 'l', 'y', 'x', 'Y', '5', '4', 'A', 'O', 'Q', 'q', '9', 'w', 'a', '8', 'z', 'n', 'M', 'p', '7', 'R', '6', 'J', 'e', 'u', 'o', 'T', 'H', '1', 'G', 'S', 'I', 'V', '+', 'B', '0', 'j', 'P', '2', '-', 'd', 'g', 'b', 'E', 's', 't', 'W' };

    private static KeyManager _inst;

    private KeyManager() throws Exception {
    }

    public static KeyManager instance() throws Exception {
        if (_inst == null) _inst = new KeyManager();
        return _inst;
    }

    private void init() throws Exception {
        _keyPair = createKeyPair();
    }

    private KeyPair createKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        _seed = APP_VERSION.getBytes();
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        random.setSeed(_seed);
        keyGen.initialize(1024, random);
        return keyGen.generateKeyPair();
    }

    public static void main(String[] args) {
        try {
            KeyManager km = KeyManager.instance();
            String key = "keyvalue";
            byte b2[];
            String encoded = km.encrypt(USER_INFO.getBytes(), key.getBytes());
            b2 = km.decrypt(encoded, key);
            System.out.println(new String(b2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] digestMessage(byte[] msg) throws Exception {
        if (_digest == null) _digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        return _digest.digest(msg);
    }

    private byte[] signMessage(byte[] msg) throws Exception {
        PrivateKey key = _keyPair.getPrivate();
        if (_sig == null) _sig = Signature.getInstance("SHA1withDSA");
        _sig.initSign(key);
        _sig.update(msg);
        return _sig.sign();
    }

    private boolean verifyMessage(byte[] msg, byte[] sig) throws Exception {
        PublicKey key = _keyPair.getPublic();
        if (_sig == null) _sig = Signature.getInstance("SHA1withDSA");
        _sig.initVerify(key);
        _sig.update(msg);
        return _sig.verify(sig);
    }

    private void dumpBytes(String msg, byte[] data) {
        System.out.println(msg + " :- Dumping " + data.length + " bytes:");
        StringBuffer buf;
        int rows = (int) Math.ceil(data.length / 32) + 1;
        for (int i = 0; i < rows; i++) {
            buf = new StringBuffer();
            for (int j = 0; j < 32 && (32 * i + j < data.length); j++) buf.append(Byte.toString(data[32 * i + j]) + (j != 31 ? ", " : ""));
            System.out.println(buf.toString());
        }
    }

    private void randomize() {
        StringBuffer buf = new StringBuffer();
        Vector vec = new Vector();
        for (int i = 0; i < _sourceChars.length; i++) vec.add(new Character(_sourceChars[i]));
        Random rand = new Random(System.currentTimeMillis());
        System.out.print("{");
        while (vec.size() > 1) {
            int idx = rand.nextInt(vec.size());
            char c = ((Character) vec.remove(idx)).charValue();
            System.out.print("'" + c + "'" + ",");
            buf.append(c);
        }
        char c = ((Character) vec.remove(0)).charValue();
        System.out.println("'" + c + "'}");
        buf.append(c);
    }

    private String encode(byte[] data) {
        byte[] src = data;
        StringBuffer buf = new StringBuffer();
        int triplets = (int) (data.length / 3);
        int pad = (data.length % 3 == 0) ? 0 : 3 - (data.length % 3);
        if (pad > 0) {
            triplets++;
            src = new byte[data.length + pad];
            System.arraycopy(data, 0, src, 0, data.length);
            for (int i = 0; i < pad; i++) src[data.length + i] = (byte) '$';
        }
        int b[] = new int[3];
        byte[] outBytes = new byte[4];
        for (int i = 0; i < triplets; i++) {
            int idx = i * 3;
            for (int j = 0; j < 3; j++) {
                b[j] = (int) src[idx + j];
            }
            outBytes[0] = (byte) (((b[0] & 0xfc) >>> 2) & 0x3f);
            outBytes[1] = (byte) ((((b[0] & 0x03) << 4) | ((b[1] & 0xf0) >>> 4)) & 0x3f);
            outBytes[2] = (byte) ((((b[1] & 0x0f) << 2) | ((b[2] & 0xc0) >>> 6)) & 0x3f);
            outBytes[3] = (byte) (b[2] & 0x3f);
            for (int k = 0; k < 4; k++) buf.append(_sourceChars[(int) outBytes[k]]);
        }
        buf.append(_sourceChars[pad]);
        return buf.toString();
    }

    private byte[] decode(String data) {
        char c = data.charAt(data.length() - 1);
        int pad = c - 'A';
        int inData[] = new int[4];
        int len = data.length() - 1;
        int rows = (int) len / 4;
        int outSize = ((int) len / 4) * 3 - pad;
        byte outBytes[] = new byte[outSize];
        int val;
        boolean done = false;
        for (int i = 0; i < rows && !done; i++) {
            for (int j = 0; j < 4; j++) {
                c = data.charAt(i * 4 + j);
                if (c >= 'A' && c <= 'Z') val = c - 'A' + APOS; else if (c >= 'a' && c <= 'z') val = c - 'a' + aPOS; else if (c >= '0' && c <= '9') val = c - '0' + numPOS; else if (c == _sourceChars[62]) val = 62; else if (c == _sourceChars[63]) val = 63; else throw new RuntimeException("Unknown character in encoded string.");
                inData[j] = val;
            }
            byte b[] = new byte[3];
            b[0] = (byte) ((inData[0] << 2) | ((inData[1] & 0x30) >>> 4));
            b[1] = (byte) (((inData[1] & 0x0f) << 4) | ((inData[2] & 0x3c) >>> 2));
            b[2] = (byte) (((inData[2] & 0x03) << 6) | (inData[3] & 0x3f));
            for (int k = 0; k < 3; k++) {
                int idx = i * 3 + k;
                if (idx >= outSize) {
                    done = true;
                    break;
                }
                outBytes[idx] = b[k];
            }
        }
        return outBytes;
    }

    public String encrypt(byte data[]) {
        return encrypt(data, APP_VERSION.getBytes());
    }

    public String encrypt(byte data[], byte key[]) {
        byte newData[] = shuffle(data);
        return encode(scramble(newData, key));
    }

    public byte[] decrypt(String data) throws Exception {
        return decrypt(data, APP_VERSION);
    }

    public byte[] decrypt(String data, String key) throws Exception {
        byte newData[] = scramble(decode(data), key.getBytes());
        return unshuffle(newData);
    }

    public byte[] decrypt(String data, byte[] key) throws Exception {
        byte newData[] = scramble(decode(data), key);
        return unshuffle(newData);
    }

    private byte[] scramble(byte data[], byte key[]) {
        int maxLen = data.length;
        int keyIdx = 0;
        int scrambledIdx = 0;
        byte result[] = new byte[maxLen];
        for (int i = 0; i < maxLen; i++) {
            result[i] = (byte) (data[i] ^ key[keyIdx] ^ ((byte) _scrambledChars[scrambledIdx]));
            if (++keyIdx >= key.length) keyIdx = 0;
            if (scrambledIdx >= _scrambledChars.length) scrambledIdx = 0;
        }
        return result;
    }

    private byte[] shuffle(byte data[]) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) result[i] = (byte) ((data[i] << 3) | ((((int) data[i]) & 0xe0) >>> 5));
        return result;
    }

    private byte[] unshuffle(byte data[]) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) result[i] = (byte) (((data[i] & 0x07) << 5) | ((((int) data[i]) & 0xff) >>> 3));
        return result;
    }
}
