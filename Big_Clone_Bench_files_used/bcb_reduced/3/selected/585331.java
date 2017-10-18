package com.digitalinksecurities.erinyes;

import java.io.*;
import javax.naming.Context;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author krdean
 *
 */
public class MqJmsJceBaseBean extends MqJmsBaseBean {

    private static final long serialVersionUID = 1L;

    private static String MESSAGE_DIGEST_ALGORITHM = "MD5";

    private static String CIPHER_ALGORITHM = "ARCFOUR";

    private static String RSA_ALGORITHM = "RSA/ECB/PKCS1Padding";

    private String SIGNATURE_ALGORITHM = "MD5WITHRSA";

    private int KEY_SIZE = 128;

    private java.security.PublicKey pubKy;

    private javax.crypto.SecretKey sKey;

    /**
* MqJmsJceBaseBean constructor comment.
*/
    public MqJmsJceBaseBean() {
        super();
        Properties props = new Properties();
        String user_dir = System.getProperties().getProperty("user.dir") + "/MqJmsJceBeanProperties.xml";
        try {
            props.loadFromXML(new FileInputStream(user_dir));
            MESSAGE_DIGEST_ALGORITHM = props.getProperty("MESSAGE_DIGEST_ALGORITHM");
            CIPHER_ALGORITHM = props.getProperty("CIPHER_ALGORITHM");
            RSA_ALGORITHM = props.getProperty("RSA_ALGORITHM");
            SIGNATURE_ALGORITHM = props.getProperty("SIGNATURE_ALGORITHM");
            KEY_SIZE = new Integer(props.getProperty("KEY_SIZE"));
        } catch (InvalidPropertiesFormatException e) {
            super.fireOffWarningInformation(e.getMessage());
        } catch (IOException e) {
            super.fireOffWarningInformation(e.getMessage());
        }
    }

    /**
* This method will perform a message digest check.
* @return <code>String</code> A literal 'true' indicate success.
* @param msgDigest <code>java.lang.String</code>
* @param msg$ <code>java.lang.String</code>
*/
    String checkMsgDigest(byte[] msgDigest, byte[] msg) {
        try {
            MessageDigest digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM, getProviderByAlgorithm(MESSAGE_DIGEST_ALGORITHM));
            byte[] result = digest.digest(msg);
            boolean ok = MessageDigest.isEqual(result, msgDigest);
            if (ok) {
                if (super.isVerbose()) {
                    super.fireOffWarningInformation("MqJmsJceBaseBean:checkMsgDigest() - Message Digest Verification Passed");
                }
                return "true";
            } else {
                return "MqJmsJceBaseBean:checkMsgDigest() - Message Digest Verification Failed";
            }
        } catch (GeneralSecurityException e) {
            String errMsg$ = "MqJmsJceBaseBean:checkMsgDigest() - " + e.getMessage();
            super.fireOffWarningInformation(errMsg$);
            return errMsg$;
        }
    }

    /**
* This method verifies the signature of a message by retrieving the public key,
* via the certificate from a specified user stored in a ldap server.
* If an error occurs anywhere within this method the error message is returned
* to the caller. Only a returned String of 'true' indicates a signature being
* verified successfully.
* @return <code>java.lang.String</code>
* @param msg <code>byte[]</code>
* @param sign$ <code>java.lang.String</code>
* @param usrdn$ <code>java.lang.String</code>
*/
    String checkSignature(byte[] msg, byte[] signature, String usrdn$) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:checkSignature() called.");
        }
        java.security.PublicKey pk = null;
        String verify$ = "MqJmsJceBaseBean:checkSignature() - Verification Failed";
        if (msg == null) {
            String errMsg$ = "MqJmsJceBaseBean:checkSignature() msg parameter is null.";
            if (super.isVerbose()) {
                super.fireOffWarningInformation(errMsg$);
            }
            return errMsg$;
        }
        byte[] result = msgDigest(msg);
        pk = this.getPubkyFromLdapUser(usrdn$);
        if (pk != null) {
            try {
                Signature sign = Signature.getInstance(SIGNATURE_ALGORITHM, getProviderByAlgorithm(SIGNATURE_ALGORITHM));
                if (super.isVerbose()) {
                    super.fireOffWarningInformation("MqJmsJceBaseBean:checkSignature() signature " + new String(signature));
                }
                sign.initVerify(pk);
                sign.update(result);
                boolean signFlag = sign.verify(signature);
                if (super.isVerbose()) {
                    super.fireOffWarningInformation("MqJmsJceBaseBean:checkSignature() verifing signature " + signFlag);
                }
                if (signFlag) {
                    verify$ = "true";
                }
            } catch (GeneralSecurityException e) {
                String errMsg$ = "MqJmsJceBaseBean:checkSignature() - " + e.getMessage();
                super.fireOffWarningInformation(errMsg$);
                return errMsg$;
            }
        } else {
            String errMsg$ = "MqJmsJceBaseBean:checkSignature() retrieval of the public key failed for user " + usrdn$;
            if (super.isVerbose()) {
                super.fireOffWarningInformation(errMsg$);
            }
            return errMsg$;
        }
        return verify$;
    }

    /**
* This method will encrypt a message only if the pubKy variable contains a
* java.security.PublicKey object. If you attempt to call this method with
* a invalid pubKy, the returned message will be null.
* The public key will be used to encrypt the symmetric 'sKey' object to be sent
* along with the jms message.
* This method only does supports stream cipher algorithms like RC4.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @return byte[] the encrypted message
* @param msg byte[] the message to be encrypted.
*/
    final byte[] decryptMsgRC4(byte[] msg, SecretKey sk) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:decryptMsgRC4() called.");
        }
        byte[] decryptedData = null;
        if (sk != null) {
            try {
                Cipher cipherDecrypt = Cipher.getInstance(CIPHER_ALGORITHM);
                cipherDecrypt.init(Cipher.DECRYPT_MODE, sk);
                decryptedData = new byte[cipherDecrypt.getOutputSize(msg.length)];
                int outputLenUpdate = cipherDecrypt.update(msg, 0, msg.length, decryptedData, 0);
                cipherDecrypt.doFinal(decryptedData, outputLenUpdate);
            } catch (GeneralSecurityException e) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:decryptMsgRC4() - " + e.getMessage());
                return null;
            }
        }
        return decryptedData;
    }

    /**
* This method will encrypt a message only if the pubKy variable contains a
* java.security.PublicKey object. If you attempt to call this method with
* a invalid pubKy, the returned message will be null.
* The public key will be used to encrypt the symmetric 'sKey' object to be sent
* along with the jms message.
* This method only supports stream cipher algorithms like RC4.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @return byte[] the encrypted message
* @param msg byte[] the message to be encrypted.
*/
    final byte[] encryptMsgRC4(byte[] msg) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:encryptMsgRC4() called.");
        }
        byte[] encryptedData = null;
        if (pubKy != null) {
            try {
                if (sKey == null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
                    long l = cal.get(java.util.Calendar.MILLISECOND);
                    random.setSeed(l);
                    KeyGenerator keyGen = KeyGenerator.getInstance("RC4");
                    keyGen.init(KEY_SIZE, random);
                    sKey = keyGen.generateKey();
                }
                Cipher cipherEncrypt = Cipher.getInstance(CIPHER_ALGORITHM);
                cipherEncrypt.init(Cipher.ENCRYPT_MODE, sKey);
                encryptedData = new byte[cipherEncrypt.getOutputSize(msg.length)];
                int outputLenUpdate = cipherEncrypt.update(msg, 0, msg.length, encryptedData, 0);
                cipherEncrypt.doFinal(encryptedData, outputLenUpdate);
            } catch (GeneralSecurityException e) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:encryptMsg() - " + e.getMessage());
                return null;
            }
        }
        return encryptedData;
    }

    /**
* This method will encrypt a message only if the pubKy variable contains a
* java.security.PublicKey object. If you attempt to call this method with
* a invalid pubKy, the returned message will be null.
* The public key will be used to encrypt the symmetric 'sKey' object to be sent
* along with the jms message.
* This method only supports stream cipher algorithms like RC4.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @return byte[] the encrypted message
* @param msg byte[] the message to be encrypted.
*/
    final byte[] encryptMsgRSA(byte[] key) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:encryptMsgRSA(<byte[]>) called.");
        }
        byte[] encryptedData = null;
        if (pubKy != null) {
            try {
                Cipher rsaCipher = Cipher.getInstance(RSA_ALGORITHM);
                rsaCipher.init(Cipher.ENCRYPT_MODE, pubKy);
                encryptedData = rsaCipher.doFinal(key);
            } catch (GeneralSecurityException e) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:encryptMsgRSA(<byte[]>) - " + e.getMessage());
                return null;
            }
        }
        return encryptedData;
    }

    /**
* @return <code>java.lang.String</code> The algorithm used for message digests.
*/
    public java.lang.String getMsgdigestAlgorithm() {
        return MESSAGE_DIGEST_ALGORITHM;
    }

    /**
* Insert the method's description here.
* Creation date: (6/11/2001 9:13:31 AM)
* @return java.security.PublicKey
*/
    public java.security.PublicKey getPubKy() {
        return pubKy;
    }

    /**
* This method looks for a ldap user specified by the usrDn$ parameter and
* extracts the public key.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @return <code>java.security.PublicKey</code>
* @param certDn$ <code>java.lang.String</code> 
*/
    PublicKey getPubkyFromLdapUser(String usrDn$) {
        java.security.cert.X509Certificate cert = null;
        try {
            javax.naming.directory.Attributes attrs = super.getDirCtx().getAttributes(usrDn$);
            byte[] b12 = null;
            try {
                b12 = (byte[]) attrs.get("userCertificate;binary").get();
            } catch (NullPointerException e) {
                b12 = (byte[]) attrs.get("userCertificate").get();
            }
            java.io.ByteArrayInputStream baisCert = new java.io.ByteArrayInputStream(b12);
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            cert = (java.security.cert.X509Certificate) cf.generateCertificate(baisCert);
            if (super.isVerbose()) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:getPubkyFromLdapUser() -  retrieving certificate for " + cert.getSubjectDN().getName());
            }
        } catch (javax.naming.NamingException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:getPubkyFromLdapUser() - " + e.getMessage());
            return null;
        } catch (java.security.cert.CertificateException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:getPubkyFromLdapUser() - " + e.getMessage());
            return null;
        }
        return cert.getPublicKey();
    }

    /**
* This method will decrypt a message only if the pvtKy variable contains a
* java.security.PrivateKey object. If you attempt to call this method with
* a invalid pvtKy, the returned message will be null.
* The private key will be used to decrypt the digital enveloper that contains
* the symmetric key.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @return byte[] the digital envelope
* @param msg byte[] the message to be encrypted.
*/
    final javax.crypto.SecretKey getPvtKyFromDigEnv(byte[] digenv, PrivateKey pvtKy) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:getPvtKyFromDigEnv() called.");
        }
        byte[] decryptedDigEnv = null;
        javax.crypto.SecretKey key = null;
        if (pvtKy != null) {
            try {
                Cipher rsaCipher = Cipher.getInstance(RSA_ALGORITHM);
                rsaCipher.init(Cipher.DECRYPT_MODE, pvtKy);
                decryptedDigEnv = rsaCipher.doFinal(digenv);
                key = new SecretKeySpec(decryptedDigEnv, "ARCFOUR");
            } catch (GeneralSecurityException e) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:getPvtKyFromDigEnv() - " + e.getMessage());
                return null;
            }
        } else {
            super.fireOffWarningInformation("MqJmsJceBaseBean:getPvtKyFromDigEnv() - No private (pvtKy) in which to decrypt the digital envelope.");
        }
        return key;
    }

    /**
 * This method will set the pubic key from a java keystore.
 * 
 * @param kstore$ path to the java keystore
 * @param alias$ alias of the certificate entry stored in the keystore
 * @param pwordKyStore$ password of the java keystore
 */
    public void setPubCertFromKeystore(String kstore$, String alias$, String pwordKyStore$) {
        if (isVerbose()) {
            fireOffWarningInformation("MqJmsJceBaseBean:setPubCertFromKeystore called");
        }
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(kstore$), pwordKyStore$.toCharArray());
            if (ks.isCertificateEntry(alias$)) {
                pubKy = ks.getCertificate(alias$).getPublicKey();
            }
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            fireOffFatalException(e);
        }
    }

    /**
* This methods validates the PKCS12 file with the passphrase, extracts the
* private key and sets the pvtKy varable.
* PKCSException & IOException raised in this method will be propagated up to the
* MqJmsJceBaseBean (mqjibb) component.
* Listeners registered with the mqjibb component should look for the 'warn_jms_ex'
* property name in its propertyChange method so it can handle it.
* @param p12inStream java.io.InputStream
* @param pp$ String
*/
    public PrivateKey setPvtKy(java.io.InputStream p12inStream, String pp$) {
        KeyStore.PrivateKeyEntry pkentry = null;
        java.security.PrivateKey pvtKy = null;
        KeyStore ks = null;
        try {
            ks = KeyStore.getInstance("PKCS12");
            ks.load(p12inStream, pp$.toCharArray());
            Enumeration e = ks.aliases();
            for (; e.hasMoreElements(); ) {
                String alias = (String) e.nextElement();
                if (ks.isKeyEntry(alias)) {
                    try {
                        pkentry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(pp$.toCharArray()));
                        pvtKy = pkentry.getPrivateKey();
                    } catch (UnrecoverableEntryException e1) {
                        ;
                    }
                }
            }
        } catch (NoSuchAlgorithmException e2) {
            e2.printStackTrace();
        } catch (CertificateException e2) {
            e2.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        if (isVerbose()) {
            fireOffWarningInformation("MqJmsJceClientBean:setPvtKy() - algorithm " + pvtKy.getAlgorithm());
        }
        return pvtKy;
    }

    /**
 * This method will extract a private key contained in a pkcs12 object
 * from a java keystore.
 * If the alias$ parameter is null, this method will extract the private key stored
 * in the first pkcs12 object that matches the pwordPkcs12 parameter.
 * @param kstore$ path to the java keystore
 * @param alias$ alias of the pkcs12 entry stored in the keystore
 * @param pwordKyStore$ password of the java keystore
 * @param pwordPkcs12$ password of the pkcs12 entry
 * @return java.security.PrivateKey
 */
    java.security.PrivateKey getPvtKyFromKeystore(String kstore$, String alias$, String pwordKyStore$, String pwordPkcs12$) {
        KeyStore ks;
        try {
            ks = KeyStore.getInstance("JKS");
            ks.load(new FileInputStream(kstore$), pwordKyStore$.toCharArray());
            KeyStore.PrivateKeyEntry pkentry = null;
            if (alias$ == null) {
                Enumeration e = ks.aliases();
                for (; e.hasMoreElements(); ) {
                    String alias = (String) e.nextElement();
                    if (ks.isKeyEntry(alias)) {
                        try {
                            pkentry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(pwordPkcs12$.toCharArray()));
                            return pkentry.getPrivateKey();
                        } catch (UnrecoverableEntryException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            } else {
                if (ks.isKeyEntry(alias$)) {
                    try {
                        pkentry = (KeyStore.PrivateKeyEntry) ks.getEntry(alias$, new KeyStore.PasswordProtection(pwordPkcs12$.toCharArray()));
                    } catch (UnrecoverableEntryException e) {
                        e.printStackTrace();
                    }
                    return pkentry.getPrivateKey();
                }
            }
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            fireOffFatalException(e);
        }
        if (isVerbose()) {
            fireOffWarningInformation("MqJmsJceBaseBean:getPvtKyFromKeystore called");
        }
        return null;
    }

    /**
* @return javax.crypto.SecretKey
*/
    javax.crypto.SecretKey getSKey() {
        return sKey;
    }

    /**
* This method will return a message digest.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @param msg byte[] The message to be digested.
* @return byte[] The message digest.
*/
    byte[] msgDigest(byte[] msg) {
        int blockSize = 4096;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM, getProviderByAlgorithm(MESSAGE_DIGEST_ALGORITHM));
            ByteArrayInputStream bis = new ByteArrayInputStream(msg);
            byte[] inputBlock = new byte[blockSize];
            int bytesRead = 0;
            while ((bytesRead = bis.read(inputBlock)) != -1) {
                digest.update(inputBlock, 0, bytesRead);
            }
        } catch (GeneralSecurityException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:msgDigest - " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:msgDigest - " + e.getMessage());
            return null;
        }
        return digest.digest();
    }

    /**
* This method will return a message digest.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex' and
* the return value will be null.
* @param msg$ <code>java.lang.String</code> The message to be digested.
* @return byte[] The message digest.
*/
    byte[] msgDigest(String msg$) {
        int blockSize = 4096;
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM, getProviderByAlgorithm(MESSAGE_DIGEST_ALGORITHM));
            ByteArrayInputStream bis = new ByteArrayInputStream(msg$.getBytes());
            byte[] inputBlock = new byte[blockSize];
            int bytesRead = 0;
            while ((bytesRead = bis.read(inputBlock)) != -1) {
                digest.update(inputBlock, 0, bytesRead);
            }
        } catch (GeneralSecurityException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:msgDigest - " + e.getMessage());
            return null;
        } catch (java.io.IOException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:msgDigest - " + e.getMessage());
            return null;
        }
        return digest.digest();
    }

    public void setDirCtx(String icf, String pu, String sa, String sp, String sc, String ss) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:setDirCtx() called.");
        }
        java.util.Hashtable<String, String> contextEnv = new java.util.Hashtable<String, String>();
        if (pu.toLowerCase().startsWith("file:")) {
            icf = "com.sun.jndi.fscontext.RefFSContextFactory";
        } else {
            if (sa == null || sa.equals("")) {
                contextEnv.put(Context.SECURITY_AUTHENTICATION, "simple");
            } else {
                contextEnv.put(Context.SECURITY_AUTHENTICATION, sa);
            }
        }
        if (icf == null || icf.equals("")) {
            contextEnv.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        } else {
            contextEnv.put(Context.INITIAL_CONTEXT_FACTORY, icf);
        }
        contextEnv.put(Context.PROVIDER_URL, pu);
        if (!(sp == null || sp.equals(""))) {
            contextEnv.put(Context.SECURITY_PRINCIPAL, sp);
        }
        if (!(sc == null || sc.equals(""))) {
            contextEnv.put(Context.SECURITY_CREDENTIALS, sc);
        }
        contextEnv.put(Context.REFERRAL, "throw");
        if (ss != null && ss.equals("ssl")) {
            contextEnv.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        contextEnv.put("java.naming.ldap.attributes.binary", "userPKCS12");
        super.setDirCtx(contextEnv);
    }

    /**
* This method will enable encryption of JMS base messages to occur.
* The certificate location (certLoc$) can either be an absolute path to a certificate
* file or dn of a ldap user object that contains the certificate. If the dn of
* a ldap user is provided, the ldap server that the user resides on must be
* accessable and the proper security credentials must be established by calling
* the setDirCtx method.
* The receiving application must have assess to the private key associated with
* the public key contained in the certificate to decrypt the message.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex'.
* @param certLoc$ <code>java.lang.String</code>
*/
    public void setEncryptEnable(String certLoc$) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:setEncryptEnable() called.");
        }
        java.io.FileInputStream fisCert = null;
        java.io.ByteArrayInputStream baisCert = null;
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:setEncryptEnable() - Getting certificate from " + certLoc$);
        }
        if (certLoc$.startsWith("cn=")) {
            try {
                javax.naming.directory.Attributes attrs = super.getDirCtx().getAttributes(certLoc$);
                if (super.isVerbose()) {
                    super.fireOffWarningInformation("MqJmsJceBaseBean:setEncryptEnable() - Extracting certificate from ldap user " + certLoc$);
                }
                byte[] b12 = (byte[]) attrs.get("userCertificate;binary").get();
                baisCert = new java.io.ByteArrayInputStream(b12);
                setPubKy(baisCert);
            } catch (Exception e) {
                super.fireOffFatalException(e);
            }
        } else {
            if (super.isVerbose()) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:setEncryptEnable() - Extracting certificate from file " + certLoc$);
            }
            try {
                fisCert = new java.io.FileInputStream(certLoc$);
                setPubKy(fisCert);
            } catch (java.io.FileNotFoundException e) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:setEncryptEnable() - " + e.getMessage());
            }
        }
    }

    /**
* @param newSignAlgorithm <code>java.lang.String</code> Sets the algorithm used for message digesting.
*/
    public void setMsgdigestAlgorithm(java.lang.String newMsgdigestAlgorithm) {
        MESSAGE_DIGEST_ALGORITHM = newMsgdigestAlgorithm;
    }

    /**
* This method take certificate InputStream object, extract the public key and
* set it to the pubKy variable.
* Any exceptions raised in this method will be propagated out to
* any registered property change listeners marked as a warning 'warn_jms_ex'.
* @param is <code>java.io.InputStream</code>
*/
    void setPubKy(InputStream is) {
        if (super.isVerbose()) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:setPubKy() called");
        }
        try {
            if (super.isVerbose()) {
                super.fireOffWarningInformation("MqJmsJceBaseBean:setPubKy() - Extracting the public key from the certificate");
            }
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf.generateCertificate(is);
            pubKy = cert.getPublicKey();
        } catch (java.security.cert.CertificateException e) {
            super.fireOffWarningInformation("MqJmsJceBaseBean:setPubKy() - " + e.getMessage());
        }
    }

    String getProviderByAlgorithm(String algorithm$) {
        String[] algorithmsID = { "Cipher", "Signature", "MessageDigest" };
        Provider[] pArr = Security.getProviders();
        Provider p = null;
        java.security.Provider.Service ps = null;
        for (int i = 0; i < pArr.length; i++) {
            p = pArr[i];
            for (int j = 0; j < algorithmsID.length; j++) {
                ps = p.getService(algorithmsID[j], algorithm$);
                if (ps != null) {
                    return ps.getProvider().getName();
                }
            }
        }
        return null;
    }

    /**
	 * Used for testing and purposes
	 */
    public static void main(java.lang.String[] argv) {
        String sc$ = "cn=Administrator,cn=Users,dc=dev,dc=digitalinksecurities,dc=com";
        String sp$ = "@cc3ssIBM";
        String usrdn$ = "cn=ken dean,cn=Users,dc=dev,dc=digitalinksecurities,dc=com";
        String purl_ldap$ = "ldap://192.168.2.8:389";
        MqJmsJceBaseBean mqibb = new com.digitalinksecurities.erinyes.MqJmsJceBaseBean();
        mqibb.setVerbose(true);
        mqibb.setDirCtx(null, purl_ldap$, null, sc$, sp$, null);
        Object o = mqibb.getPubkyFromLdapUser(usrdn$);
        System.out.println(o.toString());
    }
}
