package net.sourceforge.securevault.fp;

import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.Iterator;
import java.util.Vector;
import javax.crypto.CipherOutputStream;
import javax.crypto.CipherInputStream;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import net.sourceforge.securevault.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.EOFException;
import java.io.StreamCorruptedException;

public class DbManager {

    private static DbManager instance = null;

    private CategoryManager catMan = null;

    private SecretManager secretMan = null;

    private static final String CIPHER_TRANSFORMATION = "PBEWithMD5AndDES";

    private static final byte[] PBE_SALT = { (byte) 0xda, (byte) 0x10, (byte) 0x26, (byte) 0x8b, (byte) 0x4f, (byte) 0xf3, (byte) 0xe1, (byte) 0xa9 };

    private static final int PBE_COUNT = 20;

    private static final String DIGEST_ALGORITHM = "MD5";

    private Cipher cipher = null;

    private MessageDigest md = null;

    private DbManager() {
        try {
            cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            md = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (Exception e) {
            System.out.println("DbManager.DbManager(): " + e.getMessage());
            instance = null;
        }
        catMan = CategoryManager.getInstance();
        secretMan = SecretManager.getInstance();
    }

    protected static final DbManager getInstance() {
        if (instance == null) {
            instance = new DbManager();
        }
        return instance;
    }

    public void setCurrentDb(String dbFileName, String password) throws DbLocationException, InvalidPasswordException, DbFormatInvalidException {
        prepareCipher(Cipher.DECRYPT_MODE, password);
        ObjectInputStream inObjectStream = null;
        FileInputStream fin = null;
        DigestInputStream din = null;
        try {
            fin = new FileInputStream(dbFileName);
            inObjectStream = new ObjectInputStream(fin);
        } catch (FileNotFoundException e) {
            throw new DbLocationException();
        } catch (SecurityException e) {
            throw new DbLocationException();
        } catch (EOFException e) {
            throw new DbFormatInvalidException("DbManager.setCurrentDb: " + e.getMessage());
        } catch (StreamCorruptedException e) {
            throw new DbFormatInvalidException("DbManager.setCurrentDb: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Vector<FPCategory> catVector = new Vector<FPCategory>();
        try {
            byte hashedPwd[] = hashPassword(password);
            byte hashedPwdInFile[] = (byte[]) inObjectStream.readObject();
            if (!verifyPassword(hashedPwd, hashedPwdInFile)) throw new InvalidPasswordException();
            try {
                din = new DigestInputStream(new CipherInputStream(fin, cipher), md);
                md.reset();
                inObjectStream = new ObjectInputStream(din);
            } catch (Exception e) {
                throw new DbFormatInvalidException("DbManager.setCurrentDb: " + e.getMessage());
            }
            int numOfCat = inObjectStream.readInt();
            for (int i = 0; i < numOfCat; i++) catVector.add((FPCategory) inObjectStream.readObject());
            din.on(false);
            byte calculatedMD5[] = md.digest();
            byte fileMD5[] = (byte[]) inObjectStream.readObject();
            if (!MessageDigest.isEqual(calculatedMD5, fileMD5)) throw new DbFormatInvalidException("DbManager.setCurrentDb: File checksum test failed.");
            inObjectStream.close();
            din.close();
            fin.close();
        } catch (EOFException e) {
            throw new DbFormatInvalidException("DbManager.setCurrentDb: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new DbFormatInvalidException("DbManager.setCurrentDb: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("DbManager.setCurrentDb: " + e.getMessage());
        }
        unsetCurrentDb();
        Iterator<FPCategory> catIter = catVector.iterator();
        try {
            while (catIter.hasNext()) {
                FPCategory curCat = catIter.next();
                if (curCat.name().equals(CategoryManager.CONSTANT_CATEGORY)) {
                    catMan.replaceCategory(curCat);
                } else {
                    catMan.addCategory(curCat);
                }
                Iterator<Secret> secretIter = curCat.secrets(null);
                while (secretIter.hasNext()) secretMan.addSecret((FPSecret) secretIter.next());
            }
        } catch (Exception e) {
            System.out.println("DbManager.setCurrentDb: " + e.getMessage());
        }
    }

    public void unsetCurrentDb() {
        catMan.removeAllCategories();
        secretMan.removeAllSecrets();
    }

    public void saveCurrentDb(String dbFileName, String password) throws DbLocationException, InvalidPasswordException {
        prepareCipher(Cipher.ENCRYPT_MODE, password);
        ObjectOutputStream outObjectStream = null;
        FileOutputStream fout = null;
        DigestOutputStream dout = null;
        try {
            fout = new FileOutputStream(dbFileName);
            outObjectStream = new ObjectOutputStream(fout);
        } catch (FileNotFoundException e) {
            throw new DbLocationException();
        } catch (Exception e) {
            System.out.println("DbManager.setCurrentDb: " + e.getMessage());
        }
        Iterator<Category> catIter = catMan.searchCategories(null, CategoryManager.SearchMethod.SUBSTRING_MATCH);
        try {
            byte hashedPwd[] = hashPassword(password);
            outObjectStream.writeObject(hashedPwd);
            try {
                dout = new DigestOutputStream(new CipherOutputStream(fout, cipher), md);
                md.reset();
                outObjectStream = new ObjectOutputStream(dout);
            } catch (Exception e) {
                System.out.println("DbManager.setCurrentDb: " + e.getMessage());
            }
            outObjectStream.writeInt(catMan.getNumCategories());
            while (catIter.hasNext()) outObjectStream.writeObject(catIter.next());
            dout.on(false);
            byte fileMD5[] = md.digest();
            outObjectStream.writeObject(fileMD5);
            outObjectStream.close();
            dout.close();
            fout.close();
        } catch (Exception e) {
            System.out.println("DbManager.saveCurrentDb: " + e.getMessage());
        }
    }

    private void prepareCipher(int mode, String password) {
        PBEParameterSpec pbeParamSpec;
        SecretKeyFactory keyFac;
        try {
            pbeParamSpec = new PBEParameterSpec(PBE_SALT, PBE_COUNT);
            keyFac = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey pbeKey = keyFac.generateSecret(new PBEKeySpec(password.toCharArray()));
            cipher.init(mode, pbeKey, pbeParamSpec);
        } catch (Exception e) {
            System.out.println("DbManager.prepareCipher: " + e.getMessage());
        }
    }

    private byte[] hashPassword(String password) {
        byte hashedPwd[] = null;
        try {
            hashedPwd = md.digest(password.getBytes("ASCII"));
        } catch (Exception e) {
            System.out.println("DbManager.hashPassword: " + e.getMessage());
        }
        return hashedPwd;
    }

    private boolean verifyPassword(byte[] hashedPwd, byte[] hashedPwdInFile) {
        return (MessageDigest.isEqual(hashedPwd, hashedPwdInFile));
    }

    @Override
    public String toString() {
        return new String("DbManager: Encryption algorithm: " + cipher.getAlgorithm() + " Digest algorithm: " + md.getAlgorithm());
    }

    public boolean repOk() {
        if (catMan == null || secretMan == null || cipher == null || md == null) return false;
        return true;
    }
}
