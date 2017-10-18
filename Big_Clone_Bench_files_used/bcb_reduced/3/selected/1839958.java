package cornell.herbivore.system;

import cornell.herbivore.util.*;
import java.io.*;
import java.net.*;
import java.util.Random;
import java.security.*;
import xjava.security.*;
import cryptix.provider.rsa.*;

public class HerbivoreChallenge {

    public static final int Y_LEN = 128;

    private String name;

    private HerbivoreRSAKeyPair keyPair;

    private byte[] y;

    private HerbivoreChallenge(String aname, HerbivoreRSAKeyPair pair, byte[] y) {
        name = aname;
        keyPair = pair;
        this.y = y;
    }

    public static HerbivoreChallenge compute(String aname, int m) {
        MessageDigest md = null, md2 = null;
        Random rng;
        long val;
        int count = 0;
        try {
            byte[] publicKeyBytes = null;
            byte[] privateKeyBytes = null;
            byte[] yKeyBytes = new byte[8];
            PublicKey publicKey = new RawRSAPublicKey(new FileInputStream(aname + ".pubkey"));
            publicKeyBytes = publicKey.getEncoded();
            PrivateKey privateKey = new RawRSAPrivateKey(new FileInputStream(aname + ".privkey"));
            privateKeyBytes = privateKey.getEncoded();
            (new FileInputStream(aname + ".puzzlekey")).read(yKeyBytes);
            Log.info("Read public/private/puzzle keys from disk...");
            return new HerbivoreChallenge(aname, new HerbivoreRSAKeyPair(publicKeyBytes, privateKeyBytes), yKeyBytes);
        } catch (Exception e) {
            System.out.println("Can't read public/private/puzzle keys from file.");
        }
        Log.info("Generating public/private/puzzle keys...");
        rng = new Random();
        long startTime = System.currentTimeMillis();
        if (m > Y_LEN) return null;
        try {
            md = MessageDigest.getInstance("SHA");
            md2 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA support!");
        }
        HerbivoreRSAKeyPair pair = HerbivoreRSA.generateKeyPair();
        md.update(pair.getPublicKey());
        byte[] digest = md.digest();
        byte[] digest2 = null;
        boolean match = false;
        byte raw[] = new byte[8];
        while (!match) {
            count++;
            val = rng.nextLong();
            for (int i = 0; i < 8; i++) {
                raw[i] = (byte) (val & 0xff);
                val >>= 8;
            }
            md2.reset();
            md2.update(raw);
            digest2 = md2.digest();
            match = HerbivoreUtil.equalsBits(digest, digest2, m);
        }
        long ms = System.currentTimeMillis() - startTime;
        System.out.println("It took " + count + " tries " + ms + " ms for a " + m + " bit match");
        try {
            FileOutputStream fos = null;
            System.out.println("Saving keys to disk...");
            fos = new FileOutputStream(aname + ".pubkey");
            fos.write(pair.getPublicKey());
            fos.close();
            fos = new FileOutputStream(aname + ".privkey");
            fos.write(pair.getPrivateKey());
            fos.close();
            fos = new FileOutputStream(aname + ".puzzlekey");
            fos.write(raw);
            fos.close();
        } catch (Exception e) {
            System.out.println("Can't save keys to file.");
        }
        return new HerbivoreChallenge(aname, pair, raw);
    }

    public HerbivoreRSAKeyPair getKeyPair() {
        return keyPair;
    }

    public byte[] getY() {
        return y;
    }

    public static byte[] computeLocation(byte[] pubkey, byte[] puzzlekey) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA support!");
            System.exit(1);
        }
        md.update(pubkey);
        md.update(puzzlekey);
        byte[] digest = md.digest();
        byte[] subset = new byte[128 / 8];
        for (int i = 0; i < 128 / 8; ++i) subset[i] = digest[i];
        return subset;
    }

    public byte[] location() {
        return computeLocation(keyPair.getPublicKey(), y);
    }

    public static boolean check(byte[] location, byte[] publickey, byte[] puzzlekey, int m) {
        MessageDigest md1 = null, md2 = null, md3 = null;
        try {
            md1 = MessageDigest.getInstance("SHA");
            md2 = MessageDigest.getInstance("SHA");
            md3 = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA support!");
        }
        md1.update(publickey);
        md2.update(puzzlekey);
        if (!HerbivoreUtil.equalsBits(md1.digest(), md2.digest(), m)) return false;
        md3.update(publickey);
        md3.update(puzzlekey);
        byte[] digest = md3.digest();
        if (location.length < 128 / 8) return false;
        for (int i = 0; i < location.length; ++i) if (location[i] != digest[i]) return false;
        return true;
    }
}
