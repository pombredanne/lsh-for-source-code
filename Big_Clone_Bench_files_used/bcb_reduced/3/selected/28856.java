package com.sanecode;

import java.applet.Applet;
import java.awt.Label;
import java.io.IOException;
import java.security.AccessController;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.BASE64Encoder;

/**
 *
 * @author kinjal
 */
public class SignerAPI extends Applet {

    private static final int VERSION = 0x0006;

    private static final String COPYRIGHT = "Copyright (c) 2009, Sanecode Consultants, Ahmedabad";

    private Certificate certificate;

    private String alias;

    String data = null;

    String publicKey = null;

    String encrypt = null;

    String signature = null;

    KeyStore ks = null;

    /**
     * getVersion gets the API version
     * 
     * @return the api version
     */
    public int getVersion() {
        return VERSION;
    }

    /**
     * gets the copyright string
     *
     * @return the copyright string
     */
    public String getCopyright() {
        return COPYRIGHT;
    }

    public void init() {
        add(new Label(getCopyright()));
    }

    public int checkEnvironment() {
        String java_version = System.getProperty("java.version");
        if (!java_version.substring(0, 3).equals("1.6")) {
            return (1);
        }
        return (0);
    }

    private String messageDigestType = "MD5";

    public void setMessageDigestType(String type) {
        messageDigestType = type;
    }

    public void selectCertificate_Old(String certificatePath) {
        AccessController.doPrivileged(new PrivilegedAction() {

            public Object run() {
                try {
                    KeyStore ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
                    ks.load(null, null);
                    Enumeration<String> aliases = ks.aliases();
                    while (aliases.hasMoreElements()) {
                    }
                } catch (IOException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (CertificateException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KeyStoreException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchProviderException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
            }
        });
    }

    public String getMessageDigest() {
        String digest = null;
        try {
            MessageDigest md = MessageDigest.getInstance(messageDigestType);
            byte[] badigest = md.digest(data.getBytes());
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < badigest.length; i++) {
                String hex = Integer.toHexString(0xff & badigest[i]);
                if (hex.length() == 1) {
                    buf.append("0");
                }
                buf.append(hex);
            }
            digest = buf.toString();
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
        return (digest);
    }

    public boolean openKeyStore(String keystore, final String password) {
        Boolean rv = AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

            public Boolean run() {
                if (ks != null) {
                    ks = null;
                }
                try {
                    ks = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
                    ks.load(null, password.toCharArray());
                } catch (IOException ex) {
                    ks = null;
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException ex) {
                    ks = null;
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (CertificateException ex) {
                    ks = null;
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KeyStoreException ex) {
                    ks = null;
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchProviderException ex) {
                    ks = null;
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                }
                return new Boolean(ks != null);
            }
        });
        return rv.booleanValue();
    }

    public int getCertificateCount() {
        int cc = 0;
        if (ks != null) {
            try {
                cc = ks.size();
            } catch (KeyStoreException ex) {
                Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return (cc);
    }

    public String getCertificateAlias(int certificate) {
        String rv = "unknown_alias";
        if (ks != null) {
            try {
                Enumeration<String> aliases = ks.aliases();
                while (aliases.hasMoreElements() && certificate >= 0) {
                    rv = aliases.nextElement();
                    certificate--;
                }
            } catch (KeyStoreException ex) {
                Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return rv;
    }

    public void selectCertificate(String alias) {
        try {
            certificate = ks.getCertificate(alias);
            this.alias = alias;
        } catch (KeyStoreException ex) {
            Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getCertificateInfo(int info) {
        String certInfo = info + " is not a valid index";
        X509Certificate cert = (X509Certificate) certificate;
        switch(info) {
            case 0:
                certInfo = cert.getSubjectDN().getName();
                break;
            case 1:
                certInfo = cert.getIssuerDN().getName();
                break;
            case 2:
                certInfo = cert.getSerialNumber().toString();
                break;
            case 3:
                try {
                    certInfo = cert.getSubjectUniqueID().toString();
                } catch (Exception ex) {
                    certInfo = "null";
                }
                break;
            case 4:
                try {
                    certInfo = cert.getIssuerUniqueID().toString();
                } catch (Exception ex) {
                    certInfo = "null";
                }
                break;
            case 5:
                certInfo = cert.getNotBefore().toString();
                break;
            case 6:
                certInfo = cert.getNotAfter().toString();
                break;
        }
        return (certInfo);
    }

    public String getPublicKey() {
        String rv = "cant get public key";
        X509Certificate cert = (X509Certificate) certificate;
        rv = cert.getPublicKey().toString();
        return (rv);
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSignature() {
        String sign = AccessController.doPrivileged(new PrivilegedAction<String>() {

            public String run() {
                String sign = "";
                try {
                    PrivateKey pk = (PrivateKey) ks.getKey(alias, "".toCharArray());
                    Signature pksign = Signature.getInstance("SHA1withRSA");
                    pksign.initSign(pk);
                    pksign.update(data.getBytes());
                    byte[] b = pksign.sign();
                    BASE64Encoder encoder = new BASE64Encoder();
                    sign = encoder.encode(b);
                } catch (SignatureException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvalidKeyException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (KeyStoreException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchAlgorithmException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                } catch (UnrecoverableKeyException ex) {
                    Logger.getLogger(SignerAPI.class.getName()).log(Level.SEVERE, null, ex);
                }
                return sign;
            }
        });
        return sign;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getEncrypt() {
        return "data [" + data + "] encrypted by [" + publicKey + "]";
    }

    public void setEncrypt(String encrypt) {
        this.encrypt = encrypt;
    }

    public String getDecrypt() {
        return encrypt.substring(5, encrypt.indexOf("]"));
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isVerifySignature() {
        return true;
    }

    public String getData() {
        return data;
    }
}
