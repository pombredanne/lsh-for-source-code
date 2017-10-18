package net.sourceforge.sdm.util;

import net.sourceforge.sdm.resources.*;
import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

public class DESwPbE implements SecureFileAccess {

    static ResourceBundle res = ResourceFactory.getBundle();

    private String algorithm = res.getString("DES");

    private byte[] salt = new byte[8];

    private int iterations = 20;

    private SecretKey key;

    private Cipher cipher;

    private Provider sunJce;

    private String filePasswd;

    private String keyPasswd;

    private String user;

    private String keyFile = "SDM.keystore";

    public DESwPbE() {
    }

    public void setUser(String userName) {
        user = userName;
    }

    public void setKeyStore(String fileName) {
        keyFile = fileName;
    }

    public void setFilePassPhrase(String Password) {
        filePasswd = Password;
    }

    public void setKeyPassPhrase(String Password) {
        keyPasswd = Password;
    }

    public boolean ChangeKeyPassPhrase(String oldPassword, String newPassword, String fileName) {
        FileInputStream fsKeysIn;
        FileOutputStream fsKeysOut;
        try {
            if ((keyFile == null) || (keyFile.length() == 0)) {
                fsKeysIn = null;
            } else {
                fsKeysIn = new FileInputStream(keyFile);
            }
            KeyStore ks = KeyStore.getInstance("JCEKS");
            ks.load(fsKeysIn, oldPassword.toCharArray());
            if (fsKeysIn != null) {
                fsKeysIn.close();
            }
            if ((keyFile == null) || (keyFile.length() == 0)) {
                fsKeysOut = new FileOutputStream("SDM.keystore");
            } else {
                fsKeysOut = new FileOutputStream(keyFile);
            }
            ks.store(fsKeysOut, newPassword.toCharArray());
            fsKeysOut.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public boolean WriteFile(java.io.Serializable inObj, String fileName) throws Exception {
        FileOutputStream out;
        try {
            SecretKey skey = null;
            AlgorithmParameterSpec aps;
            out = new FileOutputStream(fileName);
            cipher = Cipher.getInstance(algorithm);
            KeySpec kspec = new PBEKeySpec(filePasswd.toCharArray());
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            skey = skf.generateSecret(kspec);
            MessageDigest md = MessageDigest.getInstance(res.getString("MD5"));
            md.update(filePasswd.getBytes());
            byte[] digest = md.digest();
            System.arraycopy(digest, 0, salt, 0, 8);
            aps = new PBEParameterSpec(salt, iterations);
            out.write(salt);
            ObjectOutputStream s = new ObjectOutputStream(out);
            cipher.init(Cipher.ENCRYPT_MODE, skey, aps);
            SealedObject so = new SealedObject(inObj, cipher);
            s.writeObject(so);
            s.flush();
            out.close();
        } catch (Exception e) {
            Log.out("fileName=" + fileName);
            Log.out("algorithm=" + algorithm);
            Log.out(e);
            throw e;
        }
        return true;
    }

    public java.io.Serializable ReadFile(String fileName) throws Exception {
        FileInputStream in;
        AlgorithmParameterSpec aps;
        java.io.Serializable retObj;
        try {
            in = new FileInputStream(fileName);
            in.read(salt);
            KeySpec ks = new PBEKeySpec(filePasswd.toCharArray());
            SecretKeyFactory skf = SecretKeyFactory.getInstance(algorithm);
            key = skf.generateSecret(ks);
            aps = new PBEParameterSpec(salt, iterations);
            cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, key, aps);
            ObjectInputStream s = new ObjectInputStream(in);
            SealedObject so = (SealedObject) s.readObject();
            retObj = (java.io.Serializable) so.getObject(cipher);
            in.close();
        } catch (Exception e) {
            Log.out("fileName=" + fileName);
            Log.out("algorithm=" + algorithm);
            Log.out(e);
            throw e;
        }
        return retObj;
    }
}
