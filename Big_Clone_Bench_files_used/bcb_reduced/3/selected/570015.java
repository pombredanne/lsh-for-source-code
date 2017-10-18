package jsmex.cardservice;

import java.lang.Exception;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import jsmex.JSmexTools;
import jsmex.function.SmartCardFile;
import opencard.core.terminal.CardTerminalException;
import opencard.core.terminal.CommandAPDU;
import opencard.core.terminal.ResponseAPDU;
import opencard.core.util.HexString;

/**
 *
 * @author Tobias Senger (jsmex@t-senger.de)
 */
public class MRTDCardService extends JSmexCardService {

    private static CommandAPDU command = new CommandAPDU(150);

    private static ResponseAPDU response;

    private byte[] AID = { (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x47, (byte) 0x10, (byte) 0x01 };

    private boolean useProtectedAPDUs = false;

    private byte[] ssc = new byte[] { (byte) 0x88, (byte) 0x70, (byte) 0x22, (byte) 0x12, (byte) 0x0C, (byte) 0x06, (byte) 0xC2, (byte) 0x2A };

    private byte[] kmac = null;

    private byte[] kenc = null;

    private byte[] ksmac = null;

    private byte[] ksenc = null;

    private boolean isBacProtected = false;

    /**
     * Creates a new instance of MRTDCardService
     */
    public MRTDCardService() {
        super();
    }

    public boolean selectMrtdApp() {
        ResponseAPDU resp = null;
        byte[] cmd = { (byte) 0x00, (byte) 0xA4, (byte) 0x04, (byte) 0x0C, (byte) 0x07, (byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x47, (byte) 0x10, (byte) 0x01 };
        command.setLength(0);
        command.append(cmd);
        try {
            resp = sendCommandAPDU(command);
        } catch (CardTerminalException ex) {
            ex.printStackTrace();
        }
        if (resp.sw() == 0x9000) return true; else return false;
    }

    private boolean getAccessCondition() {
        ResponseAPDU resp = null;
        byte[] cmd = { (byte) 0x00, (byte) 0xB0, (byte) 0x9E, (byte) 0x00, (byte) 0x00 };
        command.setLength(0);
        command.append(cmd);
        try {
            resp = sendCommandAPDU(command);
        } catch (CardTerminalException ex) {
            ex.printStackTrace();
        }
        if (resp.sw() == 0x6982) isBacProtected = true; else if (resp.sw() == 0x9000) isBacProtected = false; else System.out.println("MRTD Application is not ICAO LDS compliant!\nReturncode was " + HexString.hexify(resp.sw()));
        lt.info("getAccesCondition response: " + HexString.hexify(resp.sw1()) + " " + HexString.hexify(resp.sw2()) + "\nisBacProtected: " + isBacProtected, this);
        return isBacProtected;
    }

    public boolean isBacProtected() {
        if (!isBacProtected) getAccessCondition();
        return isBacProtected;
    }

    /**
     * Checks the checksum of the given Response APDU
     * @param rapdu The RepsonseAPDU
     * @return Returns true if checksum is correct, otherwise this method returns a false
     */
    private boolean verifyRAPDU(byte[] rapdu) {
        int pointer = 0;
        byte[] do87 = extractDO((byte) 0x87, rapdu, pointer);
        if (do87 != null) pointer = pointer + do87.length;
        byte[] do99 = extractDO((byte) 0x99, rapdu, pointer);
        if (do87 != null) pointer = pointer + do99.length;
        byte[] do8E = extractDO((byte) 0x8E, rapdu, pointer);
        ssc = incByteArray(ssc);
        byte[] k = null;
        if (do87 != null) {
            k = jsmex.JSmexTools.mergeByteArray(ssc, do87);
            k = jsmex.JSmexTools.mergeByteArray(k, do99);
        } else k = jsmex.JSmexTools.mergeByteArray(ssc, do99);
        byte[] cc2 = computeMAC(ksmac, k);
        if (compareByteArray(cc2, extractDOdata(do8E))) return true; else return false;
    }

    private boolean compareByteArray(byte[] a, byte[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }

    private byte[] extractDOdata(byte[] dataObject) {
        byte[] data = null;
        if (dataObject[0] == (byte) 0x87) {
            int len = asn1DataLength(dataObject, 0);
            int startIndex = 0;
            if (JSmexTools.toUnsignedInt(dataObject[1]) <= 0x7F) startIndex = 3; else if (JSmexTools.toUnsignedInt(dataObject[1]) == 0x81) startIndex = 4; else if (JSmexTools.toUnsignedInt(dataObject[1]) == 0x82) startIndex = 5;
            data = new byte[len - 1];
            System.arraycopy(dataObject, startIndex, data, 0, data.length);
        } else {
            data = new byte[JSmexTools.toUnsignedInt(dataObject[1])];
            System.arraycopy(dataObject, 2, data, 0, data.length);
        }
        return data;
    }

    private int asn1DataLength(byte[] asn1Data, int startByte) {
        if (JSmexTools.toUnsignedInt(asn1Data[(startByte + 1)]) <= 0x7f) return JSmexTools.toUnsignedInt(asn1Data[(startByte + 1)]);
        if (JSmexTools.toUnsignedInt(asn1Data[(startByte + 1)]) == 0x81) return JSmexTools.toUnsignedInt(asn1Data[(startByte + 2)]);
        if (JSmexTools.toUnsignedInt(asn1Data[(startByte + 1)]) == 0x82) return (JSmexTools.toUnsignedInt(asn1Data[(startByte + 2)]) * 256 + JSmexTools.toUnsignedInt(asn1Data[(startByte + 3)]));
        return 0;
    }

    private byte[] extractDO(byte doID, byte[] rapdu, int startByte) {
        for (int i = startByte; i < rapdu.length; i++) {
            if (rapdu[i] == doID) {
                int len = asn1DataLength(rapdu.clone(), i);
                int addlen = 2;
                if (rapdu[i + 1] == (byte) 0x81) addlen = 3; else if (rapdu[i + 1] == (byte) 0x82) addlen = 4;
                byte[] dataObject = new byte[(len + addlen)];
                System.arraycopy(rapdu, i, dataObject, 0, dataObject.length);
                return dataObject;
            }
        }
        return null;
    }

    /**
     * Sends a getChallenge CommandAPDU to the MRTD
     *
     * @return byte[] Array with data from Response. Contains RND.ICC <p>
     */
    private byte[] getChallenge() {
        byte[] cmd = { (byte) 0x00, (byte) 0x84, (byte) 0x00, (byte) 0x00, (byte) 0x08 };
        response = null;
        command.setLength(0);
        command.append(cmd);
        try {
            response = sendCommandAPDU(command);
        } catch (CardTerminalException cte) {
            lt.warning("method getChallenge() throws CardTerminalException", this);
        }
        lt.info("called method getChallenge() returns: RND.ICC " + HexString.hexify(response.data()) + " SW: " + HexString.hexify(response.sw1()) + HexString.hexify(response.sw2()), this);
        return response.data();
    }

    public boolean doMutualAuthentication(String mrzInfo) throws Exception {
        calculateBACKeys(mrzInfo);
        if (kenc == null || kmac == null) throw new Exception("Didn't found K_MAC or K_ENC!");
        byte[] rndicc = getChallenge();
        byte[] rndifd = new byte[8];
        byte[] kifd = new byte[16];
        Random rand = new Random();
        rand.nextBytes(rndifd);
        rand.nextBytes(kifd);
        byte[] s = new byte[32];
        System.arraycopy(rndifd, 0, s, 0, rndifd.length);
        System.arraycopy(rndicc, 0, s, 8, rndicc.length);
        System.arraycopy(kifd, 0, s, 16, kifd.length);
        byte[] eifd = encryptTDES(kenc, s);
        byte[] mifd = computeMAC(kmac, eifd);
        byte[] cmd_data = jsmex.JSmexTools.mergeByteArray(eifd, mifd);
        byte[] resp_data = sendMutualAuthenticate(cmd_data);
        byte[] eicc = new byte[32];
        byte[] micc = new byte[8];
        byte[] r = null;
        System.arraycopy(resp_data, 0, eicc, 0, eicc.length);
        System.arraycopy(resp_data, 32, micc, 0, micc.length);
        if (compareByteArray(computeMAC(kmac, eicc), micc)) {
            r = decryptTDES(kenc, eicc);
            byte[] received_rndifd = new byte[8];
            System.arraycopy(r, 8, received_rndifd, 0, received_rndifd.length);
            if (compareByteArray(rndifd, received_rndifd)) lt.info("Received RND.IFD is correct!", this); else {
                lt.warning("Received RND.IFD is NOT correct! -> BAC failed", this);
                throw new Exception("BAC failed!");
            }
        } else {
            lt.warning("MAC over EICC is NOT correct! -> BAC failed", this);
            throw new Exception("BAC failed!");
        }
        byte[] kicc = new byte[16];
        System.arraycopy(r, 16, kicc, 0, kicc.length);
        calculateSessionKeys(kifd, kicc);
        System.arraycopy(rndicc, 4, ssc, 0, 4);
        System.arraycopy(rndifd, 4, ssc, 4, 4);
        return true;
    }

    private void calculateSessionKeys(byte[] kifd, byte[] kicc) {
        byte[] kseed = xorArray(kicc, kifd);
        lt.info("K_SEED: " + HexString.hexify(kseed), this);
        ksenc = computeKey(kseed, new byte[] { 0, 0, 0, 1 });
        lt.info("KS_ENC: " + HexString.hexify(ksenc), this);
        ksmac = computeKey(kseed, new byte[] { 0, 0, 0, 2 });
        lt.info("KS_MAC: " + HexString.hexify(ksmac), this);
    }

    private byte[] sendMutualAuthenticate(byte[] data) throws Exception {
        byte[] ma_cmd = { (byte) 0x00, (byte) 0x82, (byte) 0x00, (byte) 0x00, (byte) (data.length) };
        CommandAPDU command2 = new CommandAPDU(46);
        command2.append(ma_cmd);
        command2.append(data);
        command2.append((byte) 0x28);
        try {
            response = sendCommandAPDU(command2);
            if (response.sw1() != (byte) 0x90) {
                lt.info("called method mutualAuthenticate(data :" + HexString.hexify(data) + "). returns SW: " + HexString.hexify(response.sw1()) + " " + HexString.hexify(response.sw2()), this);
                throw new Exception("mutualAuthentication failed!");
            }
        } catch (CardTerminalException cte) {
            lt.warning("method sendMutualAuthenticate(data:" + HexString.hexify(data) + ") throws CardTerminalException.", this);
            return null;
        }
        lt.info("called method sendMutualAuthenticate(data:" + HexString.hexify(data) + ")returns resp_data: " + HexString.hexify(response.data()) + ", SW: " + HexString.hexify(response.sw()), this);
        return response.data();
    }

    private byte[] buildPA(byte[] header) {
        return null;
    }

    private CommandAPDU buildPA(byte[] header, byte le) {
        header[0] = (byte) (header[0] | (byte) 0x0C);
        byte[] paddedheader = padByteArray(header);
        byte[] do97 = buildDO97(le);
        byte[] m = new byte[paddedheader.length + do97.length];
        System.arraycopy(paddedheader, 0, m, 0, paddedheader.length);
        System.arraycopy(do97, 0, m, paddedheader.length, do97.length);
        lt.info("M: " + HexString.hexify(m), this);
        byte[] cc = buildCC(m);
        byte[] do8E = buildDO8E(cc);
        command.setLength(0);
        command.append(header);
        command.append((byte) (do97.length + do8E.length));
        command.append(do97);
        command.append(do8E);
        command.append((byte) 0x00);
        return command;
    }

    private CommandAPDU buildPA(byte[] header, byte lc, byte[] data) {
        header[0] = (byte) (header[0] | (byte) 0x0C);
        byte[] paddedheader = padByteArray(header);
        byte[] do87 = buildDO87(data);
        byte[] m = new byte[paddedheader.length + do87.length];
        System.arraycopy(paddedheader, 0, m, 0, paddedheader.length);
        System.arraycopy(do87, 0, m, paddedheader.length, do87.length);
        lt.info("M: " + HexString.hexify(m), this);
        byte[] cc = buildCC(m);
        byte[] do8E = buildDO8E(cc);
        command.setLength(0);
        command.append(header);
        command.append((byte) (do87.length + do8E.length));
        command.append(do87);
        command.append(do8E);
        command.append((byte) 0x00);
        return command;
    }

    private CommandAPDU buildPA(byte[] header, byte lc, byte[] data, byte le) {
        return null;
    }

    private byte[] xorArray(byte[] a, byte[] b) {
        byte[] c = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            c[i] = (byte) (a[i] ^ b[i]);
        }
        return c;
    }

    private byte[] computeMAC(byte[] key, byte[] plaintext) {
        Cipher des;
        byte[] ka = new byte[8];
        byte[] kb = new byte[8];
        System.arraycopy(key, 0, ka, 0, 8);
        System.arraycopy(key, 8, kb, 0, 8);
        SecretKeySpec skeya = new SecretKeySpec(ka, "DES");
        SecretKeySpec skeyb = new SecretKeySpec(kb, "DES");
        byte[] current = new byte[8];
        byte[] mac = new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 };
        plaintext = padByteArray(plaintext);
        for (int i = 0; i < plaintext.length; i += 8) {
            System.arraycopy(plaintext, i, current, 0, 8);
            mac = xorArray(current, mac);
            try {
                des = Cipher.getInstance("DES/ECB/NoPadding");
                des.init(Cipher.ENCRYPT_MODE, skeya);
                mac = des.update(mac);
            } catch (NoSuchAlgorithmException ex) {
                lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchAlgorithmException", this);
            } catch (NoSuchPaddingException ex) {
                lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchPaddingException", this);
            } catch (InvalidKeyException ex) {
                lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws InvalidKeyException", this);
            }
        }
        try {
            des = Cipher.getInstance("DES/ECB/NoPadding");
            des.init(Cipher.DECRYPT_MODE, skeyb);
            mac = des.update(mac);
            des.init(Cipher.ENCRYPT_MODE, skeya);
            mac = des.doFinal(mac);
        } catch (NoSuchAlgorithmException ex) {
            lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchAlgorithmException", this);
        } catch (NoSuchPaddingException ex) {
            lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchPaddingException", this);
        } catch (InvalidKeyException ex) {
            lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws InvalidKeyException", this);
        } catch (BadPaddingException ex) {
            lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws BadPaddingException", this);
        } catch (IllegalBlockSizeException ex) {
            lt.warning("method computeMAC(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws IllegalBlockSizeException", this);
        }
        return mac;
    }

    private static byte[] incByteArray(byte[] array) {
        for (int i = array.length - 1; i >= 1; i--) {
            if (array[i] == (byte) 0xFF) {
                array[i] = (byte) 0x00;
            } else {
                byte a = array[i];
                a++;
                array[i] = a;
                return array;
            }
        }
        return array;
    }

    private byte[] buildCC(byte[] m) {
        ssc = incByteArray(ssc);
        byte[] n = new byte[ssc.length + m.length];
        System.arraycopy(ssc, 0, n, 0, ssc.length);
        System.arraycopy(m, 0, n, ssc.length, m.length);
        byte[] cc = computeMAC(ksmac, n);
        return cc;
    }

    private byte[] buildDO87(byte[] data) {
        data = padByteArray(data);
        byte[] encrypted_data = encryptTDES(ksenc, data);
        byte[] do87 = new byte[encrypted_data.length + 3];
        byte[] header = new byte[] { (byte) 0x87, (byte) (encrypted_data.length + 1), (byte) 0x01 };
        System.arraycopy(header, 0, do87, 0, header.length);
        System.arraycopy(encrypted_data, 0, do87, 3, encrypted_data.length);
        return do87;
    }

    private byte[] buildDO8E(byte[] cc) {
        byte[] do8E = new byte[cc.length + 2];
        byte[] header = new byte[] { (byte) 0x8E, (byte) (cc.length) };
        System.arraycopy(header, 0, do8E, 0, header.length);
        System.arraycopy(cc, 0, do8E, 2, cc.length);
        return do8E;
    }

    private byte[] buildDO97(byte le) {
        return new byte[] { (byte) 0x97, (byte) 0x01, le };
    }

    private byte[] encryptTDES(byte[] key, byte[] plaintext) {
        Cipher des;
        byte[] ciphertext = null;
        IvParameterSpec iv = new IvParameterSpec(new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 });
        SecretKeySpec skey = new SecretKeySpec(key, "DESede");
        try {
            des = Cipher.getInstance("DESede/CBC/NoPadding");
            des.init(Cipher.ENCRYPT_MODE, skey, iv);
            ciphertext = des.doFinal(plaintext);
        } catch (NoSuchAlgorithmException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchAlgorithmException", this);
        } catch (NoSuchPaddingException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws NoSuchPaddingException", this);
        } catch (InvalidKeyException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws InvalidKeyException", this);
        } catch (InvalidAlgorithmParameterException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws InvalidAlgorithmParameterException", this);
        } catch (BadPaddingException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws BadPaddingException", this);
        } catch (IllegalBlockSizeException ex) {
            lt.warning("method encryptTDES(key: " + HexString.hexify(key) + ", plaintext: " + HexString.hexify(plaintext) + ") throws IllegalBlockSizeException", this);
        }
        return ciphertext;
    }

    private byte[] decryptTDES(byte[] key, byte[] ciphertext) {
        Cipher des;
        byte[] plaintext = null;
        IvParameterSpec iv = new IvParameterSpec(new byte[] { (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 0 });
        SecretKeySpec skey = new SecretKeySpec(key, "DESede");
        try {
            des = Cipher.getInstance("DESede/CBC/NoPadding");
            des.init(Cipher.DECRYPT_MODE, skey, iv);
            plaintext = des.doFinal(ciphertext);
        } catch (NoSuchAlgorithmException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws NoSuchAlgorithmException", this);
        } catch (NoSuchPaddingException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws NoSuchPaddingException", this);
        } catch (InvalidKeyException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws InvalidKeyException", this);
        } catch (InvalidAlgorithmParameterException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws InvalidAlgorithmParameterException", this);
        } catch (BadPaddingException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws BadPaddingException", this);
        } catch (IllegalBlockSizeException ex) {
            lt.warning("method decryptTDES(key: " + HexString.hexify(key) + ", ciphertext: " + HexString.hexify(ciphertext) + ") throws IllegalBlockSizeException", this);
        }
        return plaintext;
    }

    /**
     * This method fills the byte-array data with one 0x80 value and several 0x00 values until
     * the the array length is a multiple from 8 bytes
     * @param data The byte-array to fill.
     * @return The filled byte-array.
     */
    private byte[] padByteArray(byte[] data) {
        int i = 0;
        byte[] tempdata = new byte[data.length + 8];
        for (i = 0; i < data.length; i++) {
            tempdata[i] = data[i];
        }
        tempdata[i] = (byte) 0x80;
        for (i = i + 1; ((i) % 8) != 0; i++) {
            tempdata[i] = (byte) 0;
        }
        byte[] filledArray = new byte[i];
        System.arraycopy(tempdata, 0, filledArray, 0, i);
        lt.info("called method padByteArray(data: " + HexString.hexify(data) + "). returns: " + HexString.hexify(filledArray), this);
        return filledArray;
    }

    private void calculateBACKeys(String mrz) {
        byte[] mrzinfo = (byte[]) mrz.getBytes();
        byte[] kseed = calculateKSeed(mrzinfo);
        kenc = computeKey(kseed, new byte[] { 0, 0, 0, 1 });
        lt.info("method calculateBACKeys(" + mrz + ") returns K_ENC= " + HexString.hexify(kenc), this);
        kmac = computeKey(kseed, new byte[] { 0, 0, 0, 2 });
        lt.info("method calculateBACKeys(" + mrz + ") returns K_MAC: " + HexString.hexify(kmac), this);
    }

    private byte[] calculateKSeed(byte[] MRZ_information) {
        byte[] hash = calculateSHA1(MRZ_information);
        byte[] kseed = new byte[16];
        for (int i = 0; i < 16; i++) kseed[i] = hash[i];
        return kseed;
    }

    private byte[] calculateSHA1(byte[] input) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            lt.warning("method calculateKSeed(" + HexString.hexify(input) + ") throws SuchAlgorithmException", this);
        }
        md.update(input);
        return md.digest();
    }

    private byte[] computeKey(byte[] kseed, byte[] c) {
        byte[] d = new byte[20];
        System.arraycopy(kseed, 0, d, 0, kseed.length);
        System.arraycopy(c, 0, d, 16, c.length);
        byte[] hd = calculateSHA1(d);
        byte[] ka = new byte[8];
        byte[] kb = new byte[8];
        System.arraycopy(hd, 0, ka, 0, ka.length);
        System.arraycopy(hd, 8, kb, 0, kb.length);
        adjustParity(ka, 0);
        adjustParity(kb, 0);
        byte[] key = new byte[24];
        System.arraycopy(ka, 0, key, 0, 8);
        System.arraycopy(kb, 0, key, 8, 8);
        System.arraycopy(ka, 0, key, 16, 8);
        return key;
    }

    /**
     * Constants that help in determining whether or not a byte array is parity
     * adjusted.
     */
    private static final byte[] PARITY = { 8, 1, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 2, 8, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 3, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 0, 8, 0, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 4, 8, 8, 0, 8, 0, 0, 8, 8, 0, 0, 8, 0, 8, 8, 0, 8, 5, 0, 8, 0, 8, 8, 0, 0, 8, 8, 0, 8, 0, 6, 8 };

    /**
     * <p>Adjust the parity for a raw key array. This essentially means that each
     * byte in the array will have an odd number of '1' bits (the last bit in
     * each byte is unused.</p>
     *
     * @param kb The key array, to be parity-adjusted.
     * @param offset The starting index into the key bytes.
     */
    private void adjustParity(byte[] key, int offset) {
        for (int i = offset; i < 8; i++) {
            key[i] ^= (PARITY[key[i] & 0xff] == 8) ? 1 : 0;
        }
    }

    private byte[] decodeResponseData(byte[] rData) throws Exception {
        byte[] responseData = null;
        byte[] decryptedData = null;
        int pointer = 0;
        if (verifyRAPDU(rData)) {
            byte[] do87 = extractDO((byte) 0x87, rData, pointer);
            if (do87 != null) {
                byte[] encryptedData = extractDOdata(do87);
                decryptedData = decryptTDES(ksenc, encryptedData);
                decryptedData = removePadding(decryptedData);
                pointer = pointer + do87.length;
            }
            byte[] do99 = extractDO((byte) 0x99, rData, pointer);
            byte[] sw = extractDOdata(do99);
            if (do87 != null) responseData = jsmex.JSmexTools.mergeByteArray(decryptedData, sw); else responseData = sw.clone();
        } else {
            System.out.println("Error in MRTDCardService:decodeResponseData");
            System.out.println("SSC: " + HexString.hexify(ssc));
            System.exit(0);
            throw new Exception("Checksum incorrect!");
        }
        return responseData;
    }

    /**
     * Not used.
     * @param scfile
     * @return null
     */
    public SmartCardFile[] getFileList(SmartCardFile scfile) {
        return null;
    }

    /**
     * not used.
     * @param fid
     * @return null
     */
    public ResponseAPDU selectEF(byte[] fid) throws Exception {
        ResponseAPDU resp = null;
        if (isBacProtected) {
            byte[] header = { (byte) 0x00, (byte) 0xA4, (byte) 0x02, (byte) 0x0C };
            byte lc = (byte) 0x02;
            command = buildPA(header, lc, fid);
        } else {
            byte[] cmd = { (byte) 0x00, (byte) 0xA4, (byte) 0x02, (byte) 0x0C, (byte) 0x02 };
            command.setLength(0);
            command.append(cmd);
            command.append(fid);
        }
        try {
            resp = sendCommandAPDU(command);
        } catch (CardTerminalException ex) {
            ex.printStackTrace();
        }
        if (isBacProtected) {
            byte[] data = decodeResponseData(resp.data());
            return new ResponseAPDU(data);
        } else {
            return resp;
        }
    }

    /**
     * not used.
     * @param fid
     * @return null
     */
    public ResponseAPDU selectDF(byte[] fid) {
        return null;
    }

    /**
     * not used
     * @param oh
     * @param ol
     * @param length
     * @return null
     */
    public ResponseAPDU readBinary(byte oh, byte ol, byte le) throws Exception {
        ResponseAPDU resp = null;
        if (isBacProtected) {
            byte[] header = { (byte) 0x00, (byte) 0xB0, oh, ol };
            command = buildPA(header, le);
        } else {
            byte[] cmd = { (byte) 0x00, (byte) 0xB0, oh, ol, le };
            command.setLength(0);
            command.append(cmd);
        }
        try {
            resp = sendCommandAPDU(command);
            if ((resp.sw() != 0x9000)) lt.info("readBinary: SW was:" + HexString.hexify(resp.sw1()) + " " + HexString.hexify(resp.sw2()), this);
        } catch (CardTerminalException ex) {
            ex.printStackTrace();
        }
        if (isBacProtected) {
            byte[] data = null;
            data = decodeResponseData(resp.data());
            return new ResponseAPDU(data);
        } else {
            return resp;
        }
    }

    private byte[] removePadding(byte[] b) {
        byte[] rd = null;
        int i = b.length - 1;
        do {
            i--;
        } while (b[i] == (byte) 0x00);
        if (b[i] == (byte) 0x80) {
            rd = new byte[i];
            System.arraycopy(b, 0, rd, 0, rd.length);
            return rd;
        }
        return b;
    }

    /**
     * not used
     * @param recno
     * @param length
     * @return null
     */
    public ResponseAPDU readRecord(byte recno, int length) {
        return null;
    }

    /**
     * not used
     * @return false
     */
    public boolean selectMF() {
        return false;
    }

    /**
     * not used
     * @param chvindication
     * @param chv
     * @return null
     */
    public ResponseAPDU verifyCHV(byte chvindication, String chv) {
        return null;
    }

    /**
     * not used
     * @param chvindicator
     * @return false
     */
    public boolean chvIsVerified(int chvindicator) {
        return false;
    }

    /**
     * not used
     * @param fidString
     * @return null
     */
    public SmartCardFile getSmartCardFile(String fidString) {
        return null;
    }

    /**
     * not used
     * @param oh
     * @param ol
     * @param data
     * @return null
     */
    public ResponseAPDU updateBinary(byte oh, byte ol, byte[] data) {
        return null;
    }

    /**
     * not used
     * @param recno
     * @param data
     * @return null
     */
    public ResponseAPDU updateRecord(byte recno, byte[] data) {
        return null;
    }
}
