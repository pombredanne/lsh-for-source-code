package org.opensrs;

import java.security.MessageDigest;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * This class shouldn't be called CBC, since its just 
 * a wrapper around the various encryption packages
 * and has precious little to do with Cipher Block Chaining
 * but i'm following the model of the OpenSRS .pm files..
 * im only bothering with blowfish for now though..
 * @author Noah Couture
 * @author Robert Dale
 */
public class CBC {

    private Cipher cipher;

    private SecretKeySpec key;

    public static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

    public static String toHexString(byte[] block) {
        StringBuffer buf = new StringBuffer();
        int len = block.length;
        for (int i = 0; i < len; i++) {
            CBC.byte2hex(block[i], buf);
            if (i < len - 1) {
                buf.append(":");
            }
        }
        return buf.toString();
    }

    public CBC(String private_key) {
        byte[] pvtkey = new byte[56];
        if (private_key.length() == 111) private_key += "0";
        System.out.println("key length = " + private_key.length());
        for (int i = 0; i < 56; i++) pvtkey[i] = (byte) Short.parseShort(private_key.substring(i * 2, i * 2 + 2), 16);
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        byte[] realkey = new byte[56];
        byte[] hash = md.digest(pvtkey);
        System.arraycopy(hash, 0, realkey, 0, 16);
        md.update(realkey, 0, 16);
        hash = md.digest();
        System.arraycopy(hash, 0, realkey, 16, 16);
        md.update(realkey, 0, 32);
        hash = md.digest();
        System.arraycopy(hash, 0, realkey, 32, 16);
        md.update(realkey, 0, 48);
        hash = md.digest();
        System.arraycopy(hash, 0, realkey, 48, 8);
        try {
            key = new SecretKeySpec(realkey, "Blowfish");
            cipher = Cipher.getInstance("Blowfish/CBC/NoPadding", Config.PROVIDER);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public byte[] encrypt(byte[] input) {
        try {
            cipher.init(Cipher.ENCRYPT_MODE, key, SecureRandom.getInstance("SHA1PRNG"));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        int il = input.length, np = 8 - (il % 8);
        byte[] buf = new byte[input.length + np];
        for (int i = il; i < (il + np); i++) buf[il] = (byte) np;
        String riv = "RandomIV";
        System.arraycopy(input, 0, buf, 0, input.length);
        try {
            buf = cipher.doFinal(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int l = riv.length();
        byte[] buf2 = new byte[l + 8 + buf.length];
        System.arraycopy(riv.getBytes(), 0, buf2, 0, l);
        System.arraycopy(cipher.getIV(), 0, buf2, l, 8);
        System.arraycopy(buf, 0, buf2, l + 8, buf.length);
        return buf2;
    }

    public byte[] decrypt(byte[] input) {
        System.out.println("decrypt received " + input.length + " bytes.");
        System.out.println("encrypted:\n" + toHexString(input) + "\n");
        byte[] buf;
        byte[] iv = new byte[8];
        int l = 16;
        if ((new String(input)).startsWith("RandomIV")) {
            System.arraycopy(input, 8, iv, 0, 8);
            buf = new byte[input.length - 16];
            System.arraycopy(input, 16, buf, 0, input.length - 16);
        } else {
            buf = new byte[input.length];
            System.arraycopy(input, 0, buf, 0, input.length);
        }
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        try {
            cipher.init(Cipher.DECRYPT_MODE, key, ivspec);
            buf = cipher.doFinal(buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
        int pads = buf[buf.length - 1];
        System.out.println("got real pads: " + pads);
        System.out.println("using padding length: " + pads);
        System.out.println("buf is " + buf.length + " bytes.");
        System.out.println("buf2 should therefore be " + (buf.length - pads) + " bytes long.");
        byte[] buf2 = new byte[buf.length - pads];
        System.out.println("buf2: " + buf2.length);
        System.arraycopy(buf, 0, buf2, 0, Math.min(buf2.length, buf.length));
        System.out.println("decrypted:\n" + new String(buf2));
        return buf2;
    }
}
