package net.sourceforge.sdm.util;

import java.io.*;
import java.security.*;

public class SDMKeyStore {

    private KeyStore keyStore;

    private byte[] keyFile;

    private byte[] keyPassword;

    /**
   * Default constructor. Merely initializes private member variables.
   */
    public SDMKeyStore() {
        keyStore = null;
        keyFile = null;
        keyPassword = null;
    }

    /**
   * Accessors for keyStore member variable
   * @param ks > this is the new keystore value to set
   */
    public void setKeyStore(KeyStore ks) {
        keyStore = ks;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    /**
   * Opens the specified keystore file and loads it into memory.
   * 
   * @param keyFileName     > KeyStore file
   * @param keyFilePassword > KeyStore password
   * @throws Exception  
   */
    public void OpenKeyStore(byte[] keyFileName, byte[] keyFilePassword) throws Exception {
        FileInputStream fsKeysIn;
        keyPassword = keyFilePassword;
        try {
            if ((keyFileName == null) || (keyFileName.length == 0)) {
                fsKeysIn = null;
            } else {
                fsKeysIn = new FileInputStream(new String(keyFileName));
            }
            keyStore = KeyStore.getInstance("JCEKS");
            keyStore.load(fsKeysIn, new String(keyPassword).toCharArray());
            if (fsKeysIn != null) {
                fsKeysIn.close();
            }
        } catch (Exception e) {
            throw e;
        }
        keyFile = keyFileName;
    }

    /**
   * Writes the currently loaded KeyStore back to disk. This overwrites the
   * existing file from which it was loaded.
   * 
   * @throws Exception > nested exception or if there is 
   *                     no open KeyStore.
   */
    public void CloseKeyStore() throws Exception {
        FileOutputStream fsKeysOut;
        if (keyStore == null) {
            throw new Exception("No open keystore");
        }
        try {
            if ((keyFile == null) || (keyFile.length == 0)) {
                fsKeysOut = new FileOutputStream("SDM.keystore");
            } else {
                fsKeysOut = new FileOutputStream(new String(keyFile));
            }
            keyStore.store(fsKeysOut, new String(keyPassword).toCharArray());
            fsKeysOut.close();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
   * Method to change the password on the keystore itself. Writes the current
   * open KeyStore to disk with a new password. You must first call
   * OpenKeyStore. The currently loaded KeyStore is first flushed to disk 
   * (CloseKeyStore is called).
   * 
   * @param newPassword > new password
   * @throws Exception  > nested exception or KeyStoreNotOpen exception.
   */
    public void ChangeKeyStorePassPhrase(byte[] newPassword) throws Exception {
        keyPassword = newPassword;
        CloseKeyStore();
    }

    /**
   * Changes the password for a specific key (entry) in the keystore. The 
   * currently open KeyStore will be written to disk first. You must have 
   * already called the OpenKeyStore method. It also rewrites the affected data
   * file to insure that the KeyStore key and the encrypted data are in sync.
   * 
   * @param oldPassword > current password for the key
   * @param newPassword > new password for the key
   * @param algorithm   > what encryption method to use for new key. This will 
   *                      be one of the following: 
   *                      DES-EDE, AES, BLOWFISH, RC4, RSA
   * @param aliasName   > key name to change
   * @throws Exception  > Nested exception or an SDM an exception if the 
   *                      encrypted data file can't be located, if the 
   *                      KeyStore is not open or if an invalid algorithm is 
   *                      specified. 
   */
    public void ChangeKeyEntryPassword(byte[] oldPassword, byte[] newPassword, byte[] algorithm, byte[] aliasName) throws Exception {
    }

    /**
   * Method used to create a copy of the currently loaded KeyStore. You must
   * first have called OpenKeyStore. A new KeyStore file is created, but 
   * if the file to be created already exists then a KeyExists exception 
   * is raised. You can not use this method to overwrite an existing KeyStore.
   * You can specify a different password than the one used for the current
   * KeyStore.
   * 
   * @param newKeyFile  > file name for the new KeyStore.
   * @param newPassword > password for new KeyStore file.
   * @throws Exception  > a nested exception or a KeyExists exception.
   */
    public void CloneKeyStore(byte[] newKeyFile, byte[] newPassword) throws Exception {
        FileOutputStream fsKeysOut;
        File fOut;
        try {
            if (newKeyFile.length == 0) {
                throw new Exception("null parameter: newKeyFile");
            } else {
                fOut = new File(newKeyFile.toString());
                if (fOut.exists()) {
                    throw new Exception("key file alreqady exists");
                } else {
                    fsKeysOut = new FileOutputStream(fOut);
                }
            }
            keyStore.store(fsKeysOut, newPassword.toString().toCharArray());
            fsKeysOut.close();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
   * Exports (copies) a single key from the currently loaded KeyStore to a new
   * (or existing) KeyStore file. You must first have called OpenKeystore.
   * If the file does not exist it is created, but if it already exists this 
   * exported key is added. If the key to be added already exists, then an 
   * exception is raised. This can not be used to alter an existing key in an 
   * existing KeyStore by overwriting, it can only be used to add a new one.
   * 
   * @param alias       > Key to be exported
   * @param newKeyStore > KeyStore file to be created or updated
   * @param newPassword > password for KeyStore file to update/create
   * @param keyPassword > password for the key to be exported
   * @throws Exception  > either a nested exception or a KeyExists exception
   */
    public void ExportKey(byte[] alias, byte[] newKeyStore, byte[] newPassword, byte[] keyPassword) throws Exception {
        FileOutputStream fsKeysOut;
        FileInputStream fsKeysIn = null;
        File fOut;
        Key key = null;
        KeyStore newKey;
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(keyPassword);
        CloseKeyStore();
        try {
            fOut = new File(newKeyStore.toString());
            newKey = KeyStore.getInstance("JCEKS");
            if (fOut.exists()) {
                fsKeysIn = new FileInputStream(fOut);
                newKey.load(fsKeysIn, new String(newPassword).toCharArray());
                fsKeysIn.close();
            }
            key = keyStore.getKey(new String(alias), new String(hash).toCharArray());
            if (newKey.containsAlias(new String(alias))) {
                throw new Exception("key already exits in export target");
            }
            fsKeysOut = new FileOutputStream(new String(newKeyStore));
            newKey.setKeyEntry(new String(alias), key, new String(hash).toCharArray(), null);
            keyStore.store(fsKeysOut, newPassword.toString().toCharArray());
            fsKeysOut.close();
        } catch (Exception e) {
            throw e;
        }
    }

    /**
   * Looks up a given entry in the KeyStore and returns the encryption 
   * algorithm used to store that data file. You must have first called
   * OpenKeyStore (and have a loaded KeyStore). This will let you know
   * which decryption method to instance and use to load a data file.
   * 
   * @param alias > the key to look up
   * @param keyPassword > the passowrd for the key (the 'file' passphrase)
   * @return > The name of a currently implemented algorithm. It will be one 
   *           of the following string values: 
   *           DES-EDE, AES, BLOWFISH, RC4, RSA
   * @throws Exception 
   */
    String GetEncryptionMethod(byte[] alias, byte[] keyPassword) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(keyPassword);
        Key key = null;
        key = keyStore.getKey(new String(alias), new String(hash).toCharArray());
        return key.getAlgorithm();
    }
}
