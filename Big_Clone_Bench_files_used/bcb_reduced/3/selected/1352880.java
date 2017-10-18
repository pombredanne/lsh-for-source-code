package edu.vt.middleware.crypt.util;

import java.io.File;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.crypto.SecretKey;
import edu.vt.middleware.crypt.CryptException;
import edu.vt.middleware.crypt.digest.MD2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for {@link CryptReader} and {@link CryptWriter} classes.
 *
 * @author  Middleware Services
 * @version  $Revision: 84 $
 */
public class CryptReaderWriterTest {

    /** Path to directory containing public/private keys. */
    private static final String KEY_DIR_PATH = "src/test/resources/edu/vt/middleware/crypt/keys/";

    /** Path to directory containing CRL data. */
    private static final String CRL_DIR_PATH = "src/test/resources/edu/vt/middleware/crypt/x509/";

    /** Logger instance. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
   * @return  Public key test data.
   *
   * @throws  Exception  On test data generation failure.
   */
    @DataProvider(name = "pubkeydata")
    public Object[][] createPubKeyTestData() throws Exception {
        final KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance("RSA");
        final KeyPair rsaKeys = rsaKeyGen.generateKeyPair();
        final KeyPairGenerator dsaKeyGen = KeyPairGenerator.getInstance("DSA");
        final KeyPair dsaKeys = dsaKeyGen.generateKeyPair();
        return new Object[][] { { rsaKeys.getPublic() }, { dsaKeys.getPublic() } };
    }

    /**
   * @return  Private key test data.
   *
   * @throws  Exception  On test data generation failure.
   */
    @DataProvider(name = "privkeydata")
    public Object[][] createPrivKeyTestData() throws Exception {
        final KeyPairGenerator rsaKeyGen = KeyPairGenerator.getInstance("RSA");
        final KeyPair rsaKeys = rsaKeyGen.generateKeyPair();
        final KeyPairGenerator dsaKeyGen = KeyPairGenerator.getInstance("DSA");
        final KeyPair dsaKeys = dsaKeyGen.generateKeyPair();
        return new Object[][] { { rsaKeys.getPrivate(), "S33Kr1t!" }, { dsaKeys.getPrivate(), "S33Kr1t!" }, { rsaKeys.getPrivate(), null }, { dsaKeys.getPrivate(), null } };
    }

    /**
   * @return  Private key test data.
   *
   * @throws  Exception  On test data generation failure.
   */
    @DataProvider(name = "readprivkeydata")
    public Object[][] createReadPrivKeyTestData() throws Exception {
        return new Object[][] { { "dsa-openssl-priv-nopass.der", null }, { "dsa-openssl-priv-nopass.pem", null }, { "dsa-openssl-priv-des3.pem", "vtcrypt" }, { "dsa-pkcs8-priv-nopass.pem", null }, { "dsa-pkcs8-priv-nopass.der", null }, { "dsa-pkcs8-priv-v2-des3.der", "vtcrypt" }, { "dsa-pkcs8-priv-v2-des3.pem", "vtcrypt" }, { "rsa-openssl-priv-nopass.der", null }, { "rsa-openssl-priv-nopass.pem", null }, { "rsa-openssl-priv-des.pem", "vtcrypt" }, { "rsa-openssl-priv-des-noheader.pem", "vtcrypt" }, { "rsa-openssl-priv-des3.pem", "vtcrypt" }, { "rsa-pkcs8-priv-nopass.der", null }, { "rsa-pkcs8-priv-nopass.pem", null }, { "rsa-pkcs8-priv-nopass-noheader.pem", null }, { "rsa-pkcs8-priv-v1-md5-des.der", "vtcrypt" }, { "rsa-pkcs8-priv-v1-md5-des.pem", "vtcrypt" }, { "rsa-pkcs8-priv-v1-md5-rc2-64.der", "vtcrypt" }, { "rsa-pkcs8-priv-v2-aes256.der", "vtcrypt" }, { "rsa-pkcs8-priv-v2-aes256.pem", "vtcrypt" }, { "rsa-pkcs8-priv-v2-aes256-noheader.pem", "vtcrypt" } };
    }

    /**
   * @return  Public key test data.
   *
   * @throws  Exception  On test data generation failure.
   */
    @DataProvider(name = "readpubkeydata")
    public Object[][] createReadDerPubKeyTestData() throws Exception {
        return new Object[][] { { "rsa-pub.der" }, { "rsa-pub.pem" }, { "dsa-pub.der" }, { "dsa-pub.pem" } };
    }

    /**
   * @return  CRL test data.
   *
   * @throws  Exception  On test data generation failure.
   */
    @DataProvider(name = "crldata")
    public Object[][] createCRLTestData() throws Exception {
        return new Object[][] { { "vtuca-crl.der" }, { "vtuca-crl.pem" } };
    }

    /**
   * @param  file  Key file to read.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "readpubkeydata")
    public void testReadPublicKey(final String file) throws Exception {
        final File keyFile = new File(KEY_DIR_PATH + file);
        logger.info("Testing read of DER-encoded public key {}", keyFile);
        AssertJUnit.assertNotNull(CryptReader.readPublicKey(keyFile));
    }

    /** @throws  Exception  On test failure. */
    @Test(groups = { "functest", "util" })
    public void testReadDerCertificate() throws Exception {
        final File certFile = new File(KEY_DIR_PATH + "rsa-pub-cert.der");
        logger.info("Testing read of DER-encoded X.509 certificate {}", certFile);
        AssertJUnit.assertNotNull(CryptReader.readCertificate(certFile));
    }

    /**
   * @param  file  Private key file to read.
   * @param  password  Private key encryption password; may be null to indicate
   * key is not encrypted.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "readprivkeydata")
    public void testReadPrivateKey(final String file, final String password) throws Exception {
        final File keyFile = new File(KEY_DIR_PATH + file);
        logger.info("Testing read of private key {}", keyFile);
        final PrivateKey key;
        if (password != null) {
            key = CryptReader.readPrivateKey(keyFile, password.toCharArray());
        } else {
            key = CryptReader.readPrivateKey(keyFile);
        }
        AssertJUnit.assertNotNull(key);
    }

    /** @throws  Exception  On test failure. */
    @Test(groups = { "functest", "util" })
    public void testReadPemCertificate() throws Exception {
        final File certFile = new File(KEY_DIR_PATH + "rsa-pub-cert.pem");
        logger.info("Testing read of PEM-encoded X.509 certificate {}", certFile);
        AssertJUnit.assertNotNull(CryptReader.readCertificate(certFile));
    }

    /**
   * @param  key  Key to write and read.
   * @param  password  Key encryption password.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "privkeydata")
    public void testReadWriteDerPrivateKey(final PrivateKey key, final String password) throws Exception {
        logger.info("Testing {} private key.", key.getAlgorithm());
        final File keyFile = new File(getKeyPath(key, "DER", null));
        keyFile.getParentFile().mkdir();
        CryptWriter.writeEncodedKey(key, keyFile);
        AssertJUnit.assertEquals(key, CryptReader.readPrivateKey(keyFile));
    }

    /**
   * @param  key  Key to write and read.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "pubkeydata")
    public void testReadWriteDerPublicKey(final PublicKey key) throws Exception {
        logger.info("Testing {} public key.", key.getAlgorithm());
        final File keyFile = new File(getKeyPath(key, "DER", null));
        keyFile.getParentFile().mkdir();
        CryptWriter.writeEncodedKey(key, keyFile);
        AssertJUnit.assertEquals(key, CryptReader.readPublicKey(keyFile));
    }

    /**
   * @param  key  Key to write and read.
   * @param  password  Key encryption password.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "privkeydata")
    public void testReadWritePemPrivateKey(final PrivateKey key, final String password) throws Exception {
        logger.info("Testing {} private key.", key.getAlgorithm());
        final char[] pwchars;
        if (password != null) {
            pwchars = password.toCharArray();
        } else {
            pwchars = null;
        }
        final File keyFile = new File(getKeyPath(key, "PEM", pwchars));
        keyFile.getParentFile().mkdir();
        CryptWriter.writePemKey(key, pwchars, new SecureRandom(), keyFile);
        final PrivateKey keyFromFile;
        if (pwchars != null) {
            keyFromFile = CryptReader.readPrivateKey(keyFile, pwchars);
        } else {
            keyFromFile = CryptReader.readPrivateKey(keyFile);
        }
        AssertJUnit.assertEquals(key, keyFromFile);
    }

    /**
   * @param  key  Key to write and read.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "pubkeydata")
    public void testReadWritePemPublicKey(final PublicKey key) throws Exception {
        logger.info("Testing {} public key.", key.getAlgorithm());
        final File keyFile = new File(getKeyPath(key, "PEM", null));
        keyFile.getParentFile().mkdir();
        CryptWriter.writePemKey(key, keyFile);
        AssertJUnit.assertEquals(key, CryptReader.readPublicKey(keyFile));
    }

    /**
   * @param  file  CRL file to read.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "util" }, dataProvider = "crldata")
    public void testReadCRL(final String file) throws Exception {
        final File crlFile = new File(CRL_DIR_PATH + file);
        logger.info("Testing read of CRL {}", crlFile);
        AssertJUnit.assertNotNull(CryptReader.readCRL(crlFile));
    }

    /**
   * Generates a fingerprint for the given key.
   *
   * @param  key  Key to fingerprint.
   *
   * @return  Hashed representation of encoded key bytes.
   *
   * @throws  CryptException  On hash calculation errors.
   */
    private String fingerPrint(final Key key) throws CryptException {
        final MD2 hash = new MD2();
        return hash.digest(key.getEncoded(), new HexConverter());
    }

    /**
   * Gets a unique path to a private key.
   *
   * @param  key  Private key.
   * @param  type  PEM or DER.
   * @param  password  Password on private key; may be null.
   *
   * @return  Path to key.
   */
    private String getKeyPath(final Key key, final String type, final char[] password) {
        final StringBuffer sb = new StringBuffer();
        if (key instanceof PrivateKey) {
            sb.append("target/test-output/privkey_");
        } else if (key instanceof SecretKey) {
            sb.append("target/test-output/secretkey_");
        } else if (key instanceof PublicKey) {
            sb.append("target/test-output/pubkey_");
        } else {
            throw new IllegalArgumentException("Unrecognized key type.");
        }
        sb.append(key.getAlgorithm());
        sb.append("_");
        try {
            sb.append(fingerPrint(key));
        } catch (CryptException e) {
            sb.append(key.hashCode());
        }
        if (!(key instanceof PublicKey)) {
            if (password != null) {
                sb.append("_withpass");
            } else {
                sb.append("_nopass");
            }
        }
        sb.append('.');
        sb.append(type.toLowerCase());
        return sb.toString();
    }
}
