package org.crypthing.things.signer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import org.crypthing.things.config.Bundle;
import org.crypthing.things.fips.PINCallback;

/**
 * A facade to user's key store cryptographic services
 * @author yorickflannagan
 * @version 1.0
 *
 */
public abstract class SignthingStore implements java.io.Serializable {

    private static final long serialVersionUID = 3367233347269961903L;

    private static ConcurrentHashMap<String, SignthingStore> registry = new ConcurrentHashMap<String, SignthingStore>();

    /**
   * Gets an instance of SignthingStore implementation.
   * @param implementation - the registration name of the implementation.
   * @return - a new instance of SignthingStore implementation, if it was registered; otherwise, null.
   */
    public static SignthingStore getInstance(String implementation) {
        return registry.get(implementation);
    }

    /**
   * Register a new SignthingStore implementation.
   * @param implementation - the implementation name.
   * @param instance - an instance of the SignthingStore implementation.
   */
    public static void register(String implementation, Object instance) {
        registry.put(implementation, (SignthingStore) instance);
    }

    protected KeyStore keyStore;

    private String keyAlias;

    private String keyStoreName;

    private String keyStoreFile;

    /**
   * Ensures an empty constructor
   */
    protected SignthingStore() {
        keyStore = null;
        keyAlias = null;
        keyStoreName = null;
        keyStoreFile = null;
    }

    /**
   * Instantiates a new SignthingStore for the specified key store.
   * @param keyStore - the key store.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   */
    protected SignthingStore(String keyStore) {
        this();
        keyStoreName = keyStore;
    }

    /**
   * Loads the specified key store.
   * @param keyStore - the key store.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   */
    protected void setKeyStore(String keyStore) {
        setKeyStore(keyStore, null, null);
    }

    /**
   * Loads the specified key store from an input stream using specified password. 
   * @param keyStore - the key store.
   * @param stream - the store input stream.
   * @param pwd - the password to access the key store.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   */
    protected void setKeyStore(String keyStore, InputStream stream, char[] pwd) {
        try {
            this.keyStore = KeyStore.getInstance(keyStore);
            this.keyStore.load(stream, pwd);
        } catch (GeneralSecurityException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_LOADING_ERROR").replace("[KEYSTORE]", keyStore), e);
        } catch (IOException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_LOADING_ERROR").replace("[KEYSTORE]", keyStore), e);
        }
    }

    /**
   * Loads specified key store from a file. The access password (PIN) is get by a PINCallback instance. 
   * @param keyStore - the key store.
   * @param file - the file where the private key must be got.
   * @throws KeyStoreAccessException - if an error occurs.
   */
    protected void setKeyStore(String keyStore, String file) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            PINCallback pin = new PINCallback();
            PasswordCallback callback = new PasswordCallback(Bundle.getInstance().getResourceString(this, "KT_INPUTPIN_MSG"), false);
            try {
                pin.handle(new Callback[] { callback });
                this.setKeyStore(keyStore, stream, callback.getPassword());
            } catch (UnsupportedCallbackException e) {
                throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_LOADING_ERROR").replace("[KEYSTORE]", keyStore), e);
            }
        } catch (FileNotFoundException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_LOADING_ERROR").replace("[KEYSTORE]", keyStore), e);
        } finally {
            try {
                stream.close();
            } catch (IOException ignored) {
            }
        }
    }

    /**
   * Gets the current loaded key store.
   * @return - the key store.
   */
    protected KeyStore getKeyStore() {
        if (keyStore == null) {
            if (getKeyStoreFile() == null) setKeyStore(keyStoreName, null, null); else setKeyStore(keyStoreName, getKeyStoreFile());
        }
        return keyStore;
    }

    /**
   * Sets the key store file, if needed.
   * @param file - the file itself.
   */
    public void setKeyStoreFile(String file) {
        keyStoreFile = file;
    }

    /**
   * Gets the key store file.
   * @return - the thing itself.
   */
    public String getKeyStoreFile() {
        return keyStoreFile;
    }

    /**
   * Sets the selected certificate alias. This property must be set before all calls except enumAliases().
   * @param alias - the certificate alias
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotFoundException - if the specified alias is not foun in the key store.
   */
    public void setKeyAlias(String alias) {
        try {
            if (getKeyStore().isKeyEntry(alias)) keyAlias = alias; else throw new KeyAliasNotFoundException(Bundle.getInstance().getResourceString(this, "KT_ALIAS_NOT_FOUND_ERROR").replace("[ALIAS]", alias));
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_ACCESS_ERROR"), e);
        }
    }

    /**
   * Gets the selected certificate alias.
   * @return - the certificate alias.
   */
    public String getKeyAlias() {
        return keyAlias;
    }

    /**
   * Gets an iterator to key aliases within current key store.
   * @return - The key aliases iterator.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   */
    public Iterator<String> enumAliases() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            Enumeration<String> aliases = getKeyStore().aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (getKeyStore().isKeyEntry(alias)) list.add(alias);
            }
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_ACCESS_ERROR"), e);
        }
        return list.iterator();
    }

    /**
   * Gets the certificate associated to selected key.
   * @return - The user's certificate.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public Certificate getCertificate() {
        return getCertificate(assertAlias());
    }

    /**
   * Gets the certificate associated to specified key.
   * @param alias - the key alias
   * @return - The user's certificate.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public Certificate getCertificate(String alias) {
        Certificate cert = null;
        try {
            cert = getKeyStore().getCertificate(alias);
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_ACCESS_ERROR"), e);
        }
        return cert;
    }

    /**
   * Gets the certificate chain associated to selected key.
   * @return - The user's certificate chain.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public Certificate[] getCertificateChain() {
        return getCertificateChain(assertAlias());
    }

    /**
   * Gets the certificate chain associated to specified key.
   * @param alias - the key alias.
   * @return - The user's certificate chain.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public Certificate[] getCertificateChain(String alias) {
        Certificate[] list = null;
        try {
            list = getKeyStore().getCertificateChain(alias);
        } catch (KeyStoreException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_ACCESS_ERROR"), e);
        }
        return list;
    }

    /**
   * Signs the specified document with private key corresponding to current selected key alias.
   * @param document - the document to sign.
   * @param signedAttributes - other signed attributes.
   * @param algorithm - the signing algorithm.
   * @return - the signed document hash.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public byte[] sign(byte[] document, byte[] signedAttributes, String algorithm) {
        return sign(document, signedAttributes, algorithm, (char[]) null);
    }

    /**
   * Signs the specified document with private key corresponding to current selected key alias.
   * @param document - the document to sign.
   * @param signedAttributes - other signed attributes.
   * @param algorithm - the signing algorithm.
   * @param pwd - the password to access private key.
   * @return - the signed document hash.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   * @throws KeyAliasNotSelectedException - if the service is required before a key alias selection.
   */
    public byte[] sign(byte[] document, byte[] signedAttributes, String algorithm, char[] pwd) {
        PrivateKey key;
        Signature sign;
        byte[] signedDocument = null;
        try {
            key = (PrivateKey) getKeyStore().getKey(assertAlias(), pwd);
            sign = Signature.getInstance(algorithm, getKeyStore().getProvider());
            sign.initSign(key);
            sign.update(document);
            sign.update(signedAttributes);
            signedDocument = sign.sign();
        } catch (InvalidKeyException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_KEY_ACCESS_ERROR"), e);
        } catch (SignatureException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_SIGNATURE_ERROR"), e);
        } catch (GeneralSecurityException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_ACCESS_ERROR"), e);
        }
        return signedDocument;
    }

    public byte[] sign(byte[] document, byte[] signedAttributes, String algorithm, String file) {
        char[] pwd = null;
        PINCallback pin = new PINCallback();
        PasswordCallback callback = new PasswordCallback(Bundle.getInstance().getResourceString(this, "KT_INPUTPIN_MSG"), false);
        try {
            pin.handle(new Callback[] { callback });
            pwd = callback.getPassword();
        } catch (UnsupportedCallbackException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_UNSUPPORTED_CALLBACK_ERROR"), e);
        }
        return sign(document, signedAttributes, algorithm, pwd);
    }

    /**
   * Verifies a document signature.
   * @param cert - the certificate associated to signer private key.
   * @param document - the signed document. 
   * @param signedAttributes - other signed attributes.
   * @param signedDocument - the signed document hash.
   * @param algorithm - the signing algorithm.
   * @return - true if signature matches; otherwise, false.
   * @throws KeyStoreAccessException - if an error occurs while trying to access the key store.
   */
    public boolean verify(Certificate cert, byte[] document, byte[] signedAttributes, byte[] signedDocument, String algorithm) {
        Signature sign;
        boolean verified = false;
        try {
            sign = Signature.getInstance(algorithm);
            sign.initVerify(cert);
            sign.update(document);
            sign.update(signedAttributes);
            verified = sign.verify(signedDocument);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_UNSUPPORTED_ALGORITHM_ERROR").replace("[ALGORITHM]", algorithm), e);
        } catch (InvalidKeyException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_KEY_ACCESS_ERROR"), e);
        } catch (SignatureException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_SIGNATURE_ERROR"), e);
        }
        return verified;
    }

    /**
   * Calculates the document digest.
   * @param document - the document to hash.
   * @param algorithm - the message digest algorithm/
   * @return - the document digest.
   * @throws KeyStoreAccessException - if the specified algorithm is not supported by the provider.
   */
    public byte[] calculatesHash(byte[] document, String algorithm) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
            digest.update(document);
        } catch (NoSuchAlgorithmException e) {
            throw new KeyStoreAccessException(Bundle.getInstance().getResourceString(this, "KT_UNSUPPORTED_ALGORITHM_ERROR").replace("[ALGORITHM]", algorithm), e);
        }
        return digest.digest();
    }

    private String assertAlias() {
        String alias = getKeyAlias();
        if ((alias == null) || (alias.contentEquals(""))) throw new KeyAliasNotSelectedException(Bundle.getInstance().getResourceString(this, "KT_UNSELECTED_ALIAS_ERROR"));
        return alias;
    }
}
