package com.sun.crypto.provider;

import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream.GetField;
import java.security.Security;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.InvalidParameterException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.AlgorithmParameters;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherSpi;
import javax.crypto.SecretKey;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.spec.*;
import sun.security.x509.AlgorithmId;
import sun.security.util.ObjectIdentifier;

/**
 * This class implements a protection mechanism for private keys. In JCE, we
 * use a stronger protection mechanism than in the JDK, because we can use
 * the <code>Cipher</code> class.
 * Private keys are protected using the JCE mechanism, and are recovered using
 * either the JDK or JCE mechanism, depending on how the key has been
 * protected. This allows us to parse Sun's keystore implementation that ships
 * with JDK 1.2.
 *
 * @author Jan Luehe
 *
 *
 * @see JceKeyStore
 */
final class KeyProtector {

    private static final String PBE_WITH_MD5_AND_DES3_CBC_OID = "1.3.6.1.4.1.42.2.19.1";

    private static final String KEY_PROTECTOR_OID = "1.3.6.1.4.1.42.2.17.1.1";

    private static final int SALT_LEN = 20;

    private static final int DIGEST_LEN = 20;

    private char[] password;

    private static final Provider PROV = Security.getProvider("SunJCE");

    KeyProtector(char[] password) {
        if (password == null) {
            throw new IllegalArgumentException("password can't be null");
        }
        this.password = password;
    }

    /**
     * Protects the given cleartext private key, using the password provided at
     * construction time.
     */
    byte[] protect(PrivateKey key) throws Exception {
        byte[] salt = new byte[8];
        SunJCE.RANDOM.nextBytes(salt);
        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, 20);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(this.password);
        SecretKey sKey = new PBEKey(pbeKeySpec, "PBEWithMD5AndTripleDES");
        pbeKeySpec.clearPassword();
        PBEWithMD5AndTripleDESCipher cipher;
        cipher = new PBEWithMD5AndTripleDESCipher();
        cipher.engineInit(Cipher.ENCRYPT_MODE, sKey, pbeSpec, null);
        byte[] plain = (byte[]) key.getEncoded();
        byte[] encrKey = cipher.engineDoFinal(plain, 0, plain.length);
        AlgorithmParameters pbeParams = AlgorithmParameters.getInstance("PBE", PROV);
        pbeParams.init(pbeSpec);
        AlgorithmId encrAlg = new AlgorithmId(new ObjectIdentifier(PBE_WITH_MD5_AND_DES3_CBC_OID), pbeParams);
        return new EncryptedPrivateKeyInfo(encrAlg, encrKey).getEncoded();
    }

    Key recover(EncryptedPrivateKeyInfo encrInfo) throws UnrecoverableKeyException, NoSuchAlgorithmException {
        byte[] plain;
        try {
            String encrAlg = encrInfo.getAlgorithm().getOID().toString();
            if (!encrAlg.equals(PBE_WITH_MD5_AND_DES3_CBC_OID) && !encrAlg.equals(KEY_PROTECTOR_OID)) {
                throw new UnrecoverableKeyException("Unsupported encryption " + "algorithm");
            }
            if (encrAlg.equals(KEY_PROTECTOR_OID)) {
                plain = recover(encrInfo.getEncryptedData());
            } else {
                byte[] encodedParams = encrInfo.getAlgorithm().getEncodedParams();
                AlgorithmParameters pbeParams = AlgorithmParameters.getInstance("PBE");
                pbeParams.init(encodedParams);
                PBEParameterSpec pbeSpec = (PBEParameterSpec) pbeParams.getParameterSpec(PBEParameterSpec.class);
                PBEKeySpec pbeKeySpec = new PBEKeySpec(this.password);
                SecretKey sKey = new PBEKey(pbeKeySpec, "PBEWithMD5AndTripleDES");
                pbeKeySpec.clearPassword();
                PBEWithMD5AndTripleDESCipher cipher;
                cipher = new PBEWithMD5AndTripleDESCipher();
                cipher.engineInit(Cipher.DECRYPT_MODE, sKey, pbeSpec, null);
                plain = cipher.engineDoFinal(encrInfo.getEncryptedData(), 0, encrInfo.getEncryptedData().length);
            }
            String oidName = new AlgorithmId(new PrivateKeyInfo(plain).getAlgorithm().getOID()).getName();
            KeyFactory kFac = KeyFactory.getInstance(oidName);
            return kFac.generatePrivate(new PKCS8EncodedKeySpec(plain));
        } catch (NoSuchAlgorithmException ex) {
            throw ex;
        } catch (IOException ioe) {
            throw new UnrecoverableKeyException(ioe.getMessage());
        } catch (GeneralSecurityException gse) {
            throw new UnrecoverableKeyException(gse.getMessage());
        }
    }

    private byte[] recover(byte[] protectedKey) throws UnrecoverableKeyException, NoSuchAlgorithmException {
        int i, j;
        byte[] digest;
        int numRounds;
        int xorOffset;
        int encrKeyLen;
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] salt = new byte[SALT_LEN];
        System.arraycopy(protectedKey, 0, salt, 0, SALT_LEN);
        encrKeyLen = protectedKey.length - SALT_LEN - DIGEST_LEN;
        numRounds = encrKeyLen / DIGEST_LEN;
        if ((encrKeyLen % DIGEST_LEN) != 0) numRounds++;
        byte[] encrKey = new byte[encrKeyLen];
        System.arraycopy(protectedKey, SALT_LEN, encrKey, 0, encrKeyLen);
        byte[] xorKey = new byte[encrKey.length];
        byte[] passwdBytes = new byte[password.length * 2];
        for (i = 0, j = 0; i < password.length; i++) {
            passwdBytes[j++] = (byte) (password[i] >> 8);
            passwdBytes[j++] = (byte) password[i];
        }
        for (i = 0, xorOffset = 0, digest = salt; i < numRounds; i++, xorOffset += DIGEST_LEN) {
            md.update(passwdBytes);
            md.update(digest);
            digest = md.digest();
            md.reset();
            if (i < numRounds - 1) {
                System.arraycopy(digest, 0, xorKey, xorOffset, digest.length);
            } else {
                System.arraycopy(digest, 0, xorKey, xorOffset, xorKey.length - xorOffset);
            }
        }
        byte[] plainKey = new byte[encrKey.length];
        for (i = 0; i < plainKey.length; i++) {
            plainKey[i] = (byte) (encrKey[i] ^ xorKey[i]);
        }
        md.update(passwdBytes);
        java.util.Arrays.fill(passwdBytes, (byte) 0x00);
        passwdBytes = null;
        md.update(plainKey);
        digest = md.digest();
        md.reset();
        for (i = 0; i < digest.length; i++) {
            if (digest[i] != protectedKey[SALT_LEN + encrKeyLen + i]) {
                throw new UnrecoverableKeyException("Cannot recover key");
            }
        }
        return plainKey;
    }

    /**
     * Seals the given cleartext key, using the password provided at
     * construction time
     */
    SealedObject seal(Key key) throws Exception {
        byte[] salt = new byte[8];
        SunJCE.RANDOM.nextBytes(salt);
        PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, 20);
        PBEKeySpec pbeKeySpec = new PBEKeySpec(this.password);
        SecretKey sKey = new PBEKey(pbeKeySpec, "PBEWithMD5AndTripleDES");
        pbeKeySpec.clearPassword();
        Cipher cipher;
        PBEWithMD5AndTripleDESCipher cipherSpi;
        cipherSpi = new PBEWithMD5AndTripleDESCipher();
        cipher = new CipherForKeyProtector(cipherSpi, PROV, "PBEWithMD5AndTripleDES");
        cipher.init(Cipher.ENCRYPT_MODE, sKey, pbeSpec);
        return new SealedObjectForKeyProtector(key, cipher);
    }

    /**
     * Unseals the sealed key.
     */
    Key unseal(SealedObject so) throws NoSuchAlgorithmException, UnrecoverableKeyException {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(this.password);
            SecretKey skey = new PBEKey(pbeKeySpec, "PBEWithMD5AndTripleDES");
            pbeKeySpec.clearPassword();
            SealedObjectForKeyProtector soForKeyProtector = null;
            if (!(so instanceof SealedObjectForKeyProtector)) {
                soForKeyProtector = new SealedObjectForKeyProtector(so);
            } else {
                soForKeyProtector = (SealedObjectForKeyProtector) so;
            }
            AlgorithmParameters params = soForKeyProtector.getParameters();
            if (params == null) {
                throw new UnrecoverableKeyException("Cannot get " + "algorithm parameters");
            }
            PBEWithMD5AndTripleDESCipher cipherSpi;
            cipherSpi = new PBEWithMD5AndTripleDESCipher();
            Cipher cipher = new CipherForKeyProtector(cipherSpi, PROV, "PBEWithMD5AndTripleDES");
            cipher.init(Cipher.DECRYPT_MODE, skey, params);
            return (Key) soForKeyProtector.getObject(cipher);
        } catch (NoSuchAlgorithmException ex) {
            throw ex;
        } catch (IOException ioe) {
            throw new UnrecoverableKeyException(ioe.getMessage());
        } catch (ClassNotFoundException cnfe) {
            throw new UnrecoverableKeyException(cnfe.getMessage());
        } catch (GeneralSecurityException gse) {
            throw new UnrecoverableKeyException(gse.getMessage());
        }
    }
}

final class CipherForKeyProtector extends javax.crypto.Cipher {

    /**
     * Creates a Cipher object.
     *
     * @param cipherSpi the delegate
     * @param provider the provider
     * @param transformation the transformation
     */
    protected CipherForKeyProtector(CipherSpi cipherSpi, Provider provider, String transformation) {
        super(cipherSpi, provider, transformation);
    }
}

final class SealedObjectForKeyProtector extends javax.crypto.SealedObject {

    static final long serialVersionUID = -3650226485480866989L;

    SealedObjectForKeyProtector(Serializable object, Cipher c) throws IOException, IllegalBlockSizeException {
        super(object, c);
    }

    SealedObjectForKeyProtector(SealedObject so) {
        super(so);
    }

    AlgorithmParameters getParameters() {
        AlgorithmParameters params = null;
        if (super.encodedParams != null) {
            try {
                params = AlgorithmParameters.getInstance("PBE", "SunJCE");
                params.init(super.encodedParams);
            } catch (NoSuchProviderException nspe) {
            } catch (NoSuchAlgorithmException nsae) {
            } catch (IOException ioe) {
            }
        }
        return params;
    }
}
