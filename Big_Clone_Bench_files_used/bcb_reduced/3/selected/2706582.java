package jimPreferences;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class EncryptionPoint {

    protected Cipher encCipher;

    protected Cipher decCipher;

    protected byte[] passwordHash;

    public EncryptionPoint(String password) {
        byte[] salt = new byte[8];
        Double theSum = new Double(computeSum(password));
        for (int i = 0; i < salt.length; i++) {
            salt[i] = theSum.byteValue();
            theSum = (theSum % 10) * 3;
        }
        int interator = getPrime(computeSum(password) % 100);
        interator = interator % 100;
        PBEKeySpec theSpec = new PBEKeySpec(password.toCharArray());
        PBEParameterSpec theParamSpec = new PBEParameterSpec(salt, interator);
        try {
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey pbeKey = keyFac.generateSecret(theSpec);
            encCipher = Cipher.getInstance(pbeKey.getAlgorithm());
            encCipher.init(Cipher.ENCRYPT_MODE, pbeKey, theParamSpec);
            decCipher = Cipher.getInstance(pbeKey.getAlgorithm());
            decCipher.init(Cipher.DECRYPT_MODE, pbeKey, theParamSpec);
            MessageDigest myDigest = MessageDigest.getInstance("MD5");
            passwordHash = myDigest.digest(password.getBytes());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    protected int computeSum(String password) {
        int i = 0;
        for (char c : password.toCharArray()) {
            i = i + c;
        }
        return i;
    }

    protected int getPrime(int primeIndex) {
        ArrayList<Integer> thePrimes = new ArrayList<Integer>();
        int i = 1;
        while (thePrimes.size() != primeIndex) {
            if (isPrime(i)) {
                thePrimes.add(i);
            }
            i++;
        }
        double toReturn = 1;
        for (Integer ii : thePrimes) {
            toReturn = toReturn * ii;
        }
        return (int) (toReturn - 1);
    }

    private boolean isPrime(int thePrime) {
        int i = 2;
        while (i < (thePrime / 2) + 1) {
            if (thePrime % i == 0) return false;
            i++;
        }
        return true;
    }

    public byte[] encrypt(byte[] data) {
        try {
            return encCipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] decrypt(byte[] data) {
        try {
            return decCipher.doFinal(data);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getPasswordHash() {
        return new String(passwordHash);
    }
}
