package com.strongauth.skcengine;

import com.strongauth.skcews.admin.Domain;
import com.strongauth.skcews.common.CommonWS;
import com.strongauth.skcews.exception.SkceWSException;
import com.strongauth.strongkeylite.web.Encryption;
import com.strongauth.strongkeylite.web.EncryptionService;
import com.strongauth.strongkeylite.web.StrongKeyLiteException_Exception;
import com.strongauth.skcews.util.Util;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * This class represents the implementation to the StrongKey Crypto Engine Java
 * library.  It allows applications to encrypt, decrypt and generate message
 * digests (hashes) of files, and store them on local or network-drives and
 * public or private-clouds.  The SKCE currently supports the Amazon S3
 * service and Eucalyptus Walrus.  However, it is our intent to support other
 * stacks/clouds such as OpenStack, Microsoft Azure, RackSpace, etc.
 *
 * The library communicates with the StrongAuth KeyAppliance on the back-
 * end to store the symmetric encryption key this library generates.  The
 * open-source version has configuration properties to talk to StrongAuth's
 * DEMO appliance for the escrow and recovery of the keys.  While StrongAuth
 * has made this capability available on the goodwill for development and
 * testing purposes, it is NOT meant for production use.

 * Therefore, it is STRONGLY RECOMMENDED THAT BEFORE YOU GO INTO PRODUCTION
 * WITH THIS CODE, YOU MODIFY YOUR SKCE CONFIGURATION PROPERTIES TO POINT TO
 * YOUR OWN STRONGAUTH KEYAPPLIANCE WITHIN YOUR NETWORK.  STRONGAUTH IS NOT
 * LIABLE FOR ANY KEYS/DATA SENT TO ITS DEMO APPLIANCE FOR ANY PURPOSE AT
 * ANY TIME.
 */
public class SKCEImpl implements SKCE {

    /**
     * Constants
     */
    private static int ITERATION = 1;

    protected static int BUFFER_SIZE = Integer.parseInt(Common.getConfigurationProperty("skcengine.cfg.property.buffersize"));

    protected static long CUTOFF_SIZE = Long.parseLong(Common.getConfigurationProperty("skcengine.cfg.property.zincsizecutoff"));

    private static final String AES = "AES";

    private static final String DESEDE = "DESede";

    protected static final String CIPHERTEXT_FILE_EXTENSION = ".ciphertext";

    protected static final String XMLENC_FILE_EXTENSION = ".xml";

    private static final String XML_PLUS_CIPHERTEXT_FILE_EXTENSION = ".zenc";

    protected static final String AES_TRANSFORM = Common.getConfigurationProperty("skcengine.cfg.property.aesenctransform");

    protected static final String DESEDE_TRANSFORM = Common.getConfigurationProperty("skcengine.cfg.property.desenctransform");

    private static final String ENCRYPTION_SERVICE_WSDL_SUFFIX = Common.getConfigurationProperty("skcengine.cfg.property.wsdlsuffix");

    private static final int DEFAULT_AES_KEYSIZE = Integer.parseInt(Common.getConfigurationProperty("skcengine.cfg.property.defaultaesenckeysize"));

    private static final int DEFAULT_DES_KEYSIZE = Integer.parseInt(Common.getConfigurationProperty("skcengine.cfg.property.defaultdesenckeysize"));

    protected static final int DEFAULT_IV_SIZE = Integer.parseInt(Common.getConfigurationProperty("skcengine.cfg.property.defaultivsize"));

    protected static final String DEFAULT_ALGORITHM = Common.getConfigurationProperty("skcengine.cfg.property.defaultenckeyalgorithm");

    private static final String DEFAULT_ALGORITHM_URL = Common.getConfigurationProperty("skcengine.cfg.property.defaultenckeyalgorithmurl");

    protected static final String DEFAULT_TRANSFORM = Common.getConfigurationProperty("skcengine.cfg.property.defaultenctransform");

    private static final String DEFAULT_DIGEST = Common.getConfigurationProperty("skcengine.cfg.property.defaultdigestalgorithm");

    private static final String DEFAULT_HOSTPORT = Common.getConfigurationProperty("skcengine.cfg.property.defaulthostport");

    private static final String DEFAULT_PRNG = Common.getConfigurationProperty("skcengine.cfg.property.defaultprngalgorithm");

    private static final String DEFAULT_DID = Common.getConfigurationProperty("skcengine.cfg.property.defaultdid");

    private static final String DEFAULT_ENC_USERNAME = Common.getConfigurationProperty("skcengine.cfg.property.defaultencusername");

    private static final String DEFAULT_ENC_PASSWORD = Common.getConfigurationProperty("skcengine.cfg.property.defaultencpassword");

    private static final String DEFAULT_DEC_USERNAME = Common.getConfigurationProperty("skcengine.cfg.property.defaultdecusername");

    private static final String DEFAULT_DEC_PASSWORD = Common.getConfigurationProperty("skcengine.cfg.property.defaultdecpassword");

    private static SecretKeyObject DEFAULT_KEY = null;

    ;

    protected static final String ENGINE_OUT_FOLDER = Common.getConfigurationProperty("skcengine.cfg.property.defaultoutfolder");

    /**
     * This decrypt() method decrypts a file containing encrypted content and
     * returns the decrypted content in a new file.  The input file must be a
     * zipped file (ZINC) containing two files: an XML file containing the
     * meta-data about the encryption in the XMLEncryption standard, and the
     * ciphertext file containing the binary IV and encrypted data.  The XML
     * file has a .xml extension while the ciphertext file has a .ciphertext
     * extension.  These may be unzipped using unzip/Winzip for review if
     * necessary.
     *
     * If the ZINC file was created by a non-SKCE library, it must have the
     * necessary elements defined to retrieve the XML and ciphertext files,
     * and the XML file must have the necessary information to retrieve the
     * symmetric encryption key from a StrongAuth KeyAppliance.
     *
     * Since the XML file has all necessary parameters to decrypt the file,
     * the only item needed is the actual cryptographic key.  This key is
     * retrieved from a KeyAppliance using the username/password provided
     * as additional parameters to this call.  The default configuration
     * parameters point to the demo.strongauth.com KeyAppliance facing the
     * internet.  The DEMO machine is meant to be used purely for testing
     * and no real keys should be sent to it.
     *
     * In the event there is any problem, the method logs the error and throws
     * a StrongkeyException to notify the linking application.
     *
     * @param credentialsMap A Hashmap containing the global credentials for all
     * configured domains for this appliance. The credentials are set by the
     * skce admin.
     * @param input String containing the FILENAME of an XML document
     * conforming to the XMLEncryption standard.  If the SKCE was responsible
     * for encrypting the source document, the encrypted XML document will have
     * all the necessary values required for decryption (except the username
     * and password).
     * @return String containing the FILENAME of the output-file containing the
     * decrypted content.
     *
     * @throws StrongKeyException in the event there is an error of any kind.
     */
    @Override
    public String decrypt(HashMap credentialsMap, String input) throws SkceWSException {
        String base64key = null;
        String token = null;
        boolean USE_ZIPINPUTSTREAM = false;
        ByteArrayInputStream bais = null;
        BufferedInputStream ctis = null;
        BufferedInputStream xmlis = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        ZipInputStream zis = null;
        String algorithm = null;
        String transform = null;
        String hosturl = null;
        String olddigest = null;
        String digesttype = null;
        int xmlfilesize = 0;
        String ctxtfilename = null;
        ZipEntry entry = null;
        String entryname = null;
        byte[] xmldoc = null;
        SecretKey sk = null;
        SecretKeyObject sko = null;
        byte[] ivbytes = null;
        IvParameterSpec ivspec = null;
        Cipher cipher = null;
        try {
            File inpf = new File(input);
            if (!inpf.exists()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4001", input);
                throw new SkceWSException("No such file: " + input);
            } else if (!inpf.isFile()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4002", input);
                throw new SkceWSException("Not a file: " + input);
            } else if (!inpf.canRead()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4003", input);
                throw new SkceWSException("Not a readable file: " + input);
            } else if (inpf.length() <= 0) {
                Common.log(Level.SEVERE, "SKCE-ERR-4004", "HOLA" + input);
                throw new SkceWSException("No data in file: " + input);
            }
            fis = new FileInputStream(input);
            Common.log(Level.INFO, "SKCE-MSG-4002", input);
            if (inpf.length() >= CUTOFF_SIZE) {
                USE_ZIPINPUTSTREAM = true;
                Common.log(Level.INFO, "SKCE-MSG-4003", new long[] { inpf.length(), CUTOFF_SIZE });
            } else {
                Common.log(Level.INFO, "SKCE-MSG-4004", new long[] { inpf.length(), CUTOFF_SIZE });
            }
            if (USE_ZIPINPUTSTREAM) {
                zis = new ZipInputStream(new BufferedInputStream(fis, BUFFER_SIZE));
                while ((entry = zis.getNextEntry()) != null) {
                    entryname = entry.getName();
                    if (entryname.endsWith(XMLENC_FILE_EXTENSION)) {
                        Common.log(Level.INFO, "SKCE-MSG-4005", entryname);
                        int count;
                        ByteArrayOutputStream baos = new ByteArrayOutputStream(BUFFER_SIZE);
                        byte[] data = new byte[BUFFER_SIZE];
                        while ((count = zis.read(data, 0, BUFFER_SIZE)) != -1) {
                            baos.write(data, 0, count);
                        }
                        xmldoc = baos.toByteArray();
                        break;
                    }
                }
                bais = new ByteArrayInputStream(xmldoc);
                Common.log(Level.INFO, "SKCE-MSG-4006", new String[] { Integer.toString(xmldoc.length), input });
                zis.close();
                fis.close();
            } else {
                ZipFile zipfile = new ZipFile(input);
                Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zipfile.entries();
                while (entries.hasMoreElements()) {
                    entry = entries.nextElement();
                    if (entry.getName().endsWith(XMLENC_FILE_EXTENSION)) {
                        xmlfilesize = (int) entry.getSize();
                        xmlis = new BufferedInputStream(zipfile.getInputStream(entry), BUFFER_SIZE);
                    } else if (entry.getName().endsWith(CIPHERTEXT_FILE_EXTENSION)) {
                        ctxtfilename = entry.getName();
                        ctis = new BufferedInputStream(zipfile.getInputStream(entry), BUFFER_SIZE);
                    }
                }
                xmldoc = new byte[(int) xmlfilesize];
                int n = xmlis.read(xmldoc);
                bais = new ByteArrayInputStream(xmldoc);
                Common.log(Level.INFO, "SKCE-MSG-4006", new String[] { Integer.toString(n), input });
                fis.close();
            }
            Common.log(Level.FINE, "SKCE-MSG-5000", "DocumentBuilderFactory()");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.parse(bais);
            XPath xpath = XPathFactory.newInstance().newXPath();
            xpath.setNamespaceContext(new SKCENamespaceContext());
            InputSource is = new InputSource(bais);
            String did = getXPathElement(bais, is, xpath, "//skles:DomainId");
            if (did == null) {
                Common.log(Level.SEVERE, "SKCE-ERR-4011", "");
                throw new SkceWSException("SKCE-ERR-4011: Invalid input - could not find DomainID in XML file");
            }
            Common.log(Level.INFO, "SKCE-MSG-4008", did);
            Domain domain = null;
            if (credentialsMap != null) domain = (Domain) credentialsMap.get(did);
            if (domain != null) {
                String username = domain.getUsername();
                String password = domain.getPassword();
                algorithm = getXPathAttribute(bais, is, xpath, "//xenc:EncryptionMethod/@Algorithm");
                Common.log(Level.INFO, "SKCE-MSG-4007", algorithm);
                if (algorithm == null) {
                    algorithm = DEFAULT_ALGORITHM;
                    transform = DEFAULT_TRANSFORM;
                    ivbytes = new byte[DEFAULT_IV_SIZE];
                } else if (algorithm.startsWith("http://www.w3.org/2001/04/xmlenc#aes")) {
                    cipher = Cipher.getInstance(AES_TRANSFORM);
                    transform = AES_TRANSFORM;
                    ivbytes = new byte[16];
                } else if (algorithm.equalsIgnoreCase("http://www.w3.org/2001/04/xmlenc#tripledes-cbc")) {
                    cipher = Cipher.getInstance(DESEDE_TRANSFORM);
                    transform = DESEDE_TRANSFORM;
                    ivbytes = new byte[8];
                } else {
                    Common.log(Level.SEVERE, "SKCE-ERR-4005", algorithm);
                    throw new SkceWSException("SKCE-ERR-4005: Invalid input - algorithm is not AES or DESede:" + algorithm);
                }
                token = getXPathElement(bais, is, xpath, "//ds:KeyName");
                Common.log(Level.INFO, "SKCE-MSG-4009", token);
                hosturl = getXPathAttribute(bais, is, xpath, "//ds:RetrievalMethod/@URI");
                Common.log(Level.INFO, "SKCE-MSG-4010", hosturl);
                String uri = getXPathAttribute(bais, is, xpath, "//xenc:CipherReference/@URI");
                Common.log(Level.INFO, "SKCE-MSG-4011", uri);
                olddigest = getXPathElement(bais, is, xpath, "//ds:DigestValue");
                digesttype = getXPathAttribute(bais, is, xpath, "//ds:DigestMethod/@Algorithm");
                Common.log(Level.INFO, "SKCE-MSG-4012", new String[] { olddigest, digesttype });
                URL baseUrl = com.strongauth.strongkeylite.web.EncryptionService.class.getResource(".");
                URL url = new URL(baseUrl, hosturl);
                EncryptionService cryptosvc = new EncryptionService(url);
                Encryption port = cryptosvc.getEncryptionPort();
                Common.log(Level.INFO, "SKCE-MSG-4013", hosturl);
                base64key = port.decrypt(Long.parseLong(did), username, password, token);
                if (base64key != null) {
                    byte[] enckey = Base64.decode(base64key);
                    sk = new SecretKeySpec(enckey, algorithm);
                    sko = new SecretKeyObject(sk, algorithm, transform);
                    Common.putSymmetricKey(token, sko);
                    Common.log(Level.INFO, "SKCE-MSG-4014", token);
                }
                String path = Util.createUniqueDir(ENGINE_OUT_FOLDER, true);
                String target = path + CommonWS.fs + uri.substring(0, uri.indexOf(CIPHERTEXT_FILE_EXTENSION));
                File outf = new File(target);
                if (outf.exists()) {
                    Common.log(Level.SEVERE, "SKCE-ERR-4010", target);
                    throw new SkceWSException("SKCE-ERR-4010: Target output file already exists; aborting: " + target);
                }
                Common.log(Level.INFO, "SKCE-MSG-4015", target);
                File parent = outf.getParentFile();
                if (parent != null) {
                    if (parent.getFreeSpace() < (inpf.length())) {
                        Common.log(Level.SEVERE, "SKCE-ERR-4008", parent.getFreeSpace());
                        throw new SkceWSException("SKCE-ERR-4008: Insufficient estimated space to create output file: " + parent.getFreeSpace());
                    }
                }
                if (USE_ZIPINPUTSTREAM) {
                    zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(input), BUFFER_SIZE));
                    while ((entry = zis.getNextEntry()) != null) {
                        entryname = entry.getName();
                        if (entryname.endsWith(CIPHERTEXT_FILE_EXTENSION)) {
                            Common.log(Level.INFO, "SKCE-MSG-4016", entryname);
                            break;
                        }
                    }
                    zis.read(ivbytes, 0, ivbytes.length);
                    ivspec = new IvParameterSpec(ivbytes);
                    Common.log(Level.INFO, "SKCE-MSG-4017", ivbytes.length);
                } else {
                    ctis.read(ivbytes, 0, ivbytes.length);
                    ivspec = new IvParameterSpec(ivbytes);
                    Common.log(Level.INFO, "SKCE-MSG-4017", ivbytes.length);
                }
                cipher.init(Cipher.DECRYPT_MODE, sk, ivspec);
                MessageDigest digest = null;
                if (digesttype.equalsIgnoreCase("http://www.w3.org/2001/04/xmlenc#sha256")) {
                    digest = MessageDigest.getInstance("SHA-256");
                } else if (digesttype.equalsIgnoreCase("http://www.w3.org/2001/04/xmlenc#sha512")) {
                    digest = MessageDigest.getInstance("SHA-512");
                }
                Common.log(Level.INFO, "SKCE-MSG-4018", digesttype);
                int count = 0;
                int total = 0;
                byte[] data = new byte[BUFFER_SIZE];
                byte[] plaintext;
                String newdigest;
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outf), BUFFER_SIZE);
                if (USE_ZIPINPUTSTREAM) {
                    while ((count = zis.read(data)) != -1) {
                        plaintext = cipher.update(data, 0, count);
                        digest.update(plaintext, 0, plaintext.length);
                        bos.write(plaintext);
                        total += count;
                    }
                } else {
                    while ((count = ctis.read(data)) != -1) {
                        plaintext = cipher.update(data, 0, count);
                        digest.update(plaintext, 0, plaintext.length);
                        bos.write(plaintext);
                        total += count;
                    }
                }
                plaintext = cipher.doFinal();
                bos.write(plaintext);
                bos.flush();
                bos.close();
                Common.log(Level.INFO, "SKCE-MSG-4019", total);
                newdigest = new String(Base64.encode(digest.digest(plaintext)));
                if (!olddigest.equalsIgnoreCase(newdigest)) {
                    throw new SkceWSException("Decryption did not work correctly - digests do not match\n" + "Old digest: " + olddigest + '\n' + "New digest: " + newdigest);
                }
                Common.log(Level.INFO, "SKCE-MSG-4020", new String[] { Long.toString(outf.length()), outf.getName() });
                return outf.getAbsolutePath();
            } else {
                Common.log(Level.SEVERE, "SKCE-ERR-4012", did);
                throw new SkceWSException("Service NOT available for domain " + did);
            }
        } catch (SAXException ex) {
            throw new SkceWSException(ex);
        } catch (ParserConfigurationException ex) {
            throw new SkceWSException(ex);
        } catch (FileNotFoundException ex) {
            throw new SkceWSException(ex);
        } catch (BadPaddingException ex) {
            throw new SkceWSException(ex);
        } catch (IllegalBlockSizeException ex) {
            throw new SkceWSException(ex);
        } catch (InvalidAlgorithmParameterException ex) {
            throw new SkceWSException(ex);
        } catch (InvalidKeyException ex) {
            throw new SkceWSException(ex);
        } catch (IOException ex) {
            throw new SkceWSException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new SkceWSException(ex);
        } catch (NoSuchPaddingException ex) {
            throw new SkceWSException(ex);
        } catch (StrongKeyLiteException_Exception ex) {
            throw new SkceWSException(ex);
        } catch (GeneralSecurityException ex) {
            throw new SkceWSException(ex);
        } catch (Exception ex) {
            throw new SkceWSException(ex);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ex) {
                throw new SkceWSException(ex);
            }
        }
    }

    /**
     * This method encrypts a file (containing any kind of content and of any
     * size) using the parameters specified in the method call.  The
     * cryptographic symmetric-key, which is generated by the library is
     * escrowed and tokenized on a StrongAuth KeyAppliance.  After knowing
     * the key was successfully escrowed, it is used to encrypt the source-
     * file.  The encrypted ciphertext is written in binary form to a file
     * with a .ciphertext extension and all encryption parameters (except for
     * the actual key) are stored as an XML document in the XMLEncryption
     * format.  The two files (.ciphertext and .xml) are zipped into a ZINC
     * file and written to the configured output directory.
     * 
     * If the uniquekey flag is set to be true, the key is discarded after
     * the file is encrypted; but because it is escrowed, it can be recovered
     * with the proper authorization to decrypt the ciphertext when necessary.
     *
     * The generated XMLEncryption file conforms strictly to the schema
     * definition; but without access to the KeyAppliance that has the
     * escrowed symmetric encryption key, the ciphertext cannot be decrypted.
     *
     * @param did Long value containing the unique identifier of the encryption
     * domain on the SKLES.  It  may be null, in which case the properties file
     * of the SKCE library is consulted for the DID
     * @param username String value containing the username authorized to
     * request encryption services on the SKLES
     * @param password String password of the encryption-authorized username
     * @param input String containing the FILENAME whose content must be
     * encrypted
     * @param uniquekey boolean flag indicating if the library must generate a
     * new key to encrypt the file.  If the flag is set to 'false', the library
     * will re-use a "daily" key - a key generated at the start of a new day
     * and used throughout the day to encrypt files.
     * @param algorithm String specifying the encryption algorithm to use for
     * encryption: AES or DESede
     * @param keysize int value containing the key-size: 128, 192 or 256 for AES;
     * 112 or 168 for DESede
     * @return String containing the FILENAME of the ZINC output-file.  The
     * ZINC file is a zipped file containing the XMLEncryption-conformant XML
     * document file, and the encrypted ciphertext file.
     *
     * @throws SkceWSException in the event there is an error of any kind.
     */
    @Override
    public String encrypt(String did, String username, String password, String input, boolean uniquekey, String algorithm, int keysize) throws SkceWSException {
        String base64key = null;
        String token = null;
        FileInputStream fis = null;
        FileOutputStream fos = null;
        Document xencdoc = null;
        String algorithmurl = null;
        String transform = null;
        SecretKey sk = null;
        SecretKeyObject sko = null;
        byte[] ivbytes = null;
        IvParameterSpec ivspec = null;
        Cipher cipher = null;
        try {
            if (did == null) {
                did = DEFAULT_DID;
            }
            Common.log(Level.INFO, "SKCE-MSG-4008", did);
            if (username == null) {
                username = DEFAULT_ENC_USERNAME;
            }
            Common.log(Level.INFO, "SKCE-MSG-4001", username);
            if (password == null) {
                password = DEFAULT_ENC_PASSWORD;
            }
            File inpf = new File(input);
            if (!inpf.exists()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4001", input);
                throw new SkceWSException("No such file: " + input);
            } else if (!inpf.isFile()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4002", input);
                throw new SkceWSException("Not a file: " + input);
            } else if (!inpf.canRead()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4003", input);
                throw new SkceWSException("Not a readable file: " + input);
            } else if (inpf.length() <= 0) {
                Common.log(Level.SEVERE, "SKCE-ERR-4004", input);
                throw new SkceWSException("No data in file: " + input);
            }
            String infName = inpf.getName();
            fis = new FileInputStream(input);
            Common.log(Level.INFO, "SKCE-MSG-4002", input);
            SecureRandom securerandom = SecureRandom.getInstance(DEFAULT_PRNG);
            if (!uniquekey) {
                if (DEFAULT_KEY == null) {
                    algorithm = DEFAULT_ALGORITHM;
                    algorithmurl = DEFAULT_ALGORITHM_URL;
                    if (algorithm.equalsIgnoreCase("AES")) {
                        keysize = DEFAULT_AES_KEYSIZE;
                    } else if (algorithm.equalsIgnoreCase("DESEDE")) {
                        keysize = DEFAULT_DES_KEYSIZE;
                    }
                    KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
                    keygen.init(keysize, securerandom);
                    sk = keygen.generateKey();
                    base64key = new String(Base64.encode(sk.getEncoded()), "UTF-8");
                    sko = new SecretKeyObject(sk, algorithm, DEFAULT_TRANSFORM);
                    DEFAULT_KEY = sko;
                    Common.log(Level.FINE, "SKCE-MSG-4021", new String[] { algorithm, Integer.toString(keysize) });
                }
            } else {
                if (algorithm.equalsIgnoreCase("AES")) {
                    if (keysize != 128) {
                        if (keysize != 192) {
                            if (keysize != 256) {
                                keysize = DEFAULT_AES_KEYSIZE;
                                algorithmurl = DEFAULT_ALGORITHM_URL;
                                Common.log(Level.FINE, "SKCE-MSG-4022", keysize);
                            } else {
                                algorithmurl = "http://www.w3.org/2001/04/xmlenc#aes256-cbc";
                            }
                        } else {
                            algorithmurl = "http://www.w3.org/2001/04/xmlenc#aes192-cbc";
                        }
                    } else {
                        algorithmurl = "http://www.w3.org/2001/04/xmlenc#aes128-cbc";
                    }
                    ivbytes = new byte[16];
                    securerandom.nextBytes(ivbytes);
                    transform = AES_TRANSFORM;
                    cipher = Cipher.getInstance(transform);
                    ivspec = new IvParameterSpec(ivbytes);
                } else if (algorithm.equalsIgnoreCase(DESEDE)) {
                    if (keysize != 112 || keysize != 168) {
                        keysize = DEFAULT_DES_KEYSIZE;
                        Common.log(Level.FINE, "SKCE-MSG-4023", keysize);
                    }
                    algorithmurl = "http://www.w3.org/2001/04/xmlenc#tripledes-cbc";
                    transform = DESEDE_TRANSFORM;
                    ivbytes = new byte[8];
                    securerandom.nextBytes(ivbytes);
                    cipher = Cipher.getInstance(transform);
                    ivspec = new IvParameterSpec(ivbytes);
                }
                Common.log(Level.FINE, "SKCE-MSG-4024", keysize);
                Common.log(Level.FINE, "SKCE-MSG-4025", new String[] { new String(Base64.encode(ivbytes), "UTF-8"), Integer.toString(ivbytes.length) });
                KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
                keygen.init(keysize, securerandom);
                sk = keygen.generateKey();
                base64key = new String(Base64.encode(sk.getEncoded()), "UTF-8");
                sko = new SecretKeyObject(sk, algorithm, transform);
                Common.log(Level.FINE, "SKCE-MSG-4026", new String[] { algorithm, Integer.toString(keysize) });
            }
            Common.log(Level.FINE, "SKCE-MSG-4027", algorithmurl);
            String hosturl = DEFAULT_HOSTPORT + ENCRYPTION_SERVICE_WSDL_SUFFIX;
            URL baseUrl = com.strongauth.strongkeylite.web.EncryptionService.class.getResource(".");
            URL url = new URL(baseUrl, hosturl);
            Common.log(Level.INFO, "SKCE-MSG-4028", hosturl);
            EncryptionService cryptosvc = new EncryptionService(url);
            Encryption port = cryptosvc.getEncryptionPort();
            Common.log(Level.FINE, "SKCE-MSG-4013", hosturl);
            token = port.encrypt(Long.parseLong(did), username, password, base64key);
            if (token != null) {
                Common.putSymmetricKey(token, sko);
                Common.log(Level.FINE, "SKCE-MSG-4029", token);
            }
            String s = ManagementFactory.getRuntimeMXBean().getName();
            int atsign = s.indexOf('@');
            int pid = Integer.parseInt(s.substring(0, atsign));
            File of = new File(input + "-" + token + "-" + pid + "-" + ITERATION++ + XML_PLUS_CIPHERTEXT_FILE_EXTENSION);
            Common.log(Level.FINE, "SKCE-MSG-4030", of.getName());
            File parent = of.getParentFile();
            if (parent != null) {
                if (parent.getFreeSpace() < (inpf.length() * 2)) {
                    Common.log(Level.SEVERE, "SKCE-ERR-4008", parent.getFreeSpace());
                    throw new SkceWSException("Insufficient estimated space to create output file: " + parent.getFreeSpace());
                }
            }
            ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(of)));
            int count;
            int total = 0;
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(input), BUFFER_SIZE);
            Common.log(Level.INFO, "SKCE-MSG-4031", input);
            byte[] data = new byte[BUFFER_SIZE];
            MessageDigest digest = null;
            if (DEFAULT_DIGEST.equalsIgnoreCase("http://www.w3.org/2001/04/xmlenc#sha256")) {
                digest = MessageDigest.getInstance("SHA-256");
            } else if (DEFAULT_DIGEST.equalsIgnoreCase("http://www.w3.org/2001/04/xmlenc#sha512")) {
                digest = MessageDigest.getInstance("SHA-512");
            }
            cipher.init(Cipher.ENCRYPT_MODE, sk, ivspec);
            byte[] ciphertext;
            ZipEntry entry = new ZipEntry(infName + CIPHERTEXT_FILE_EXTENSION);
            zos.putNextEntry(entry);
            zos.write(ivbytes);
            while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                digest.update(data, 0, count);
                ciphertext = cipher.update(data, 0, count);
                zos.write(ciphertext);
                total += count;
            }
            Common.log(Level.INFO, "SKCE-MSG-4032", new String[] { Integer.toString(total), input });
            fis.close();
            bis.close();
            ciphertext = cipher.doFinal();
            zos.write(ciphertext);
            Common.log(Level.INFO, "SKCE-MSG-4033", input + CIPHERTEXT_FILE_EXTENSION);
            String hash = new String(Base64.encode(digest.digest()));
            Common.log(Level.INFO, "SKCE-MSG-4034", new String[] { hash, DEFAULT_DIGEST });
            Common.log(Level.FINE, "SKCE-MSG-5000", "new ByteArrayOutputStream()");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Common.log(Level.FINE, "SKCE-MSG-5000", "DocumentBuilderFactory()");
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            xencdoc = builder.newDocument();
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:EncryptedData)");
            Element root = (Element) xencdoc.createElement("xenc:EncryptedData");
            root.setAttribute("Id", "ID".concat(token.concat(Long.toString(Common.nowMs()))));
            root.setIdAttribute("Id", Boolean.TRUE);
            root.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            root.setAttribute("xsi:schemaLocation", "http://www.w3.org/2001/04/xmlenc# http://www.w3.org/TR/xmlenc-core/xenc-schema.xsd");
            root.setAttribute("xmlns:xenc", "http://www.w3.org/2001/04/xmlenc#");
            root.setAttribute("xmlns:ds", "http://www.w3.org/2000/09/xmldsig#");
            root.setAttribute("xmlns:skles", "http://strongkeylite.strongauth.com/SKLES201009");
            org.w3c.dom.Node encdatanode = (org.w3c.dom.Node) xencdoc.appendChild(root);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:EncryptionMethod)");
            Element encmethod = (Element) xencdoc.createElement("xenc:EncryptionMethod");
            encmethod.setAttribute("Algorithm", algorithmurl);
            org.w3c.dom.Node encmethodnode = (org.w3c.dom.Node) encdatanode.appendChild(encmethod);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(ds:KeyInfo)");
            Element keyinfo = (Element) xencdoc.createElement("ds:KeyInfo");
            org.w3c.dom.Node keyinfonode = (org.w3c.dom.Node) encdatanode.appendChild(keyinfo);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(skles:DomainId)");
            Element domid = (Element) xencdoc.createElement("skles:DomainId");
            org.w3c.dom.Node didnode = (org.w3c.dom.Node) keyinfonode.appendChild(domid);
            org.w3c.dom.Text didtext = xencdoc.createTextNode(did);
            didnode.appendChild(didtext);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(ds:KeyName)");
            Element keyname = (Element) xencdoc.createElement("ds:KeyName");
            org.w3c.dom.Node keynamenode = (org.w3c.dom.Node) keyinfonode.appendChild(keyname);
            org.w3c.dom.Text keynametext = xencdoc.createTextNode(token);
            keynamenode.appendChild(keynametext);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(ds:RetrievalMethod)");
            Element retmethod = (Element) xencdoc.createElement("ds:RetrievalMethod");
            retmethod.setAttribute("URI", hosturl);
            org.w3c.dom.Node retmethodnode = (org.w3c.dom.Node) keyinfonode.appendChild(retmethod);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:CipherData)");
            Element cipherdata = (Element) xencdoc.createElement("xenc:CipherData");
            org.w3c.dom.Node cipherdatanode = (org.w3c.dom.Node) encdatanode.appendChild(cipherdata);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:CipherReference)");
            Element cipherref = (Element) xencdoc.createElement("xenc:CipherReference");
            cipherref.setAttribute("URI", infName + CIPHERTEXT_FILE_EXTENSION);
            org.w3c.dom.Node cipherrefnode = (org.w3c.dom.Node) cipherdatanode.appendChild(cipherref);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:EncryptionProperties)");
            Element encproperties = (Element) xencdoc.createElement("xenc:EncryptionProperties");
            org.w3c.dom.Node encpropertiesnode = (org.w3c.dom.Node) encdatanode.appendChild(encproperties);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(xenc:EncryptionProperty)");
            Element encproperty1 = (Element) xencdoc.createElement("xenc:EncryptionProperty");
            encproperty1.setAttribute("Id", "MessageDigest");
            org.w3c.dom.Node encpropertynode1 = (org.w3c.dom.Node) encpropertiesnode.appendChild(encproperty1);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(ds:DigestMethod)");
            Element digestmethod = (Element) xencdoc.createElement("ds:DigestMethod");
            digestmethod.setAttribute("Algorithm", DEFAULT_DIGEST);
            org.w3c.dom.Node digestmethodnode = (org.w3c.dom.Node) encproperty1.appendChild(digestmethod);
            Common.log(Level.FINE, "SKCE-MSG-5000", "createElement(ds:DigestValue)");
            Element digestvalue = (Element) xencdoc.createElement("ds:DigestValue");
            org.w3c.dom.Node digestvaluenode = (org.w3c.dom.Node) encproperty1.appendChild(digestvalue);
            org.w3c.dom.Text digvaltext = xencdoc.createTextNode(hash);
            digestvaluenode.appendChild(digvaltext);
            Common.log(Level.FINE, "SKCE-MSG-5000", "StreamResult(baos)");
            DOMSource source = new DOMSource(xencdoc);
            StreamResult result = new StreamResult(baos);
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
            transformer.transform(source, result);
            entry = new ZipEntry(infName + XMLENC_FILE_EXTENSION);
            zos.putNextEntry(entry);
            zos.write(baos.toByteArray());
            zos.close();
            Common.log(Level.INFO, "SKCE-MSG-4035", new String[] { Long.toString(of.length()), of.getName() });
            return of.getAbsolutePath();
        } catch (TransformerConfigurationException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (TransformerException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (ParserConfigurationException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (FileNotFoundException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (BadPaddingException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (IllegalBlockSizeException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (InvalidAlgorithmParameterException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (InvalidKeyException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (IOException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (NoSuchAlgorithmException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (NoSuchPaddingException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (StrongKeyLiteException_Exception ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } catch (GeneralSecurityException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            throw new SkceWSException(ex);
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ex) {
                Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
                throw new SkceWSException(ex);
            }
        }
    }

    /**
     *
     * This method generates a message digest of a specified type based on
     * content provided in the input parameter.  This is just a convenience
     * method, since the encrypt/decrypt methods above, automatically check
     * the digests of the plaintext files before/after encryption/decryption
     * to verify their integrity.
     *
     * @param type - The type of digest to generate: SHA-1, SHA-256, SHA-384
     * or SHA-512
     * @param input - String input containing the FILENAME whose content must
     * be hashed
     * @return - String containing the hex-encoded message digest
     *
     * @throws StrongKeyException in the event there is an error of any kind.
     */
    @Override
    public String digest(String type, String input) throws SkceWSException {
        FileInputStream fis = null;
        try {
            if (type == null) {
                type = DEFAULT_DIGEST;
            }
            File f = new File(input);
            if (!f.exists()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4001", input);
                throw new SkceWSException("SKCE-ERR-4001: Invalid input - input file does not exist:" + input);
            } else if (!f.isFile()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4002", input);
                throw new SkceWSException("SKCE-ERR-4002: Invalid input - input file is not a file:" + input);
            } else if (!f.canRead()) {
                Common.log(Level.SEVERE, "SKCE-ERR-4003", input);
                throw new SkceWSException("SKCE-ERR-4003: Invalid input - input file is not readable:" + input);
            } else if (f.length() <= 0) {
                Common.log(Level.SEVERE, "SKCE-ERR-4004", input);
                throw new SkceWSException("SKCE-ERR-4004: Invalid input - input file is empty:" + input);
            }
            fis = new FileInputStream(input);
            Common.log(Level.FINE, "SKCE-MSG-4002", input);
            byte[] plaintext = new byte[Integer.parseInt(String.valueOf(f.length()))];
            int n = fis.read(plaintext);
            Common.log(Level.FINE, "SKCE-MSG-4032", new String[] { Integer.toString(n), input });
            fis.close();
            if (type.equalsIgnoreCase("SHA1") || type.equalsIgnoreCase("SHA-1") || type.equalsIgnoreCase("sha1")) {
                return getSHA1(plaintext, null);
            } else if (type.equalsIgnoreCase("SHA256") || type.equalsIgnoreCase("SHA-256") || type.equalsIgnoreCase("sha256")) {
                return getSHA256(plaintext, null);
            } else if (type.equalsIgnoreCase("SHA384") || type.equalsIgnoreCase("SHA-384") || type.equalsIgnoreCase("sha384")) {
                return getSHA384(plaintext, null);
            } else if (type.equalsIgnoreCase("SHA512") || type.equalsIgnoreCase("SHA-512") || type.equalsIgnoreCase("sha512")) {
                return getSHA512(plaintext, null);
            } else {
                return null;
            }
        } catch (FileNotFoundException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        } catch (IOException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        } catch (NoSuchAlgorithmException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        } catch (NoSuchProviderException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        } catch (GeneralSecurityException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        } finally {
            try {
                fis.close();
            } catch (IOException ex) {
                Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
            }
        }
        return null;
    }

    /**
     * Generates a SHA-1 message-digest and returns the hex-encoded hash
     *
     * @param input - byte array containing input that needs to be hashed
     * @param JCEProvider - optional Provider
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnsupportedEncodingException
     */
    String getSHA1(byte[] input, String JCEProvider) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
        MessageDigest hash = null;
        if (JCEProvider == null) {
            hash = MessageDigest.getInstance("SHA-1");
        } else {
            hash = MessageDigest.getInstance("SHA-1", JCEProvider);
        }
        byte[] digest = hash.digest(input);
        return new String(Base64.encode(digest));
    }

    /**
     * Generates a SHA-256 message-digest and returns the hex-encoded hash
     *
     * @param input - byte array containing input that needs to be hashed
     * @param JCEProvider - optional Provider
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnsupportedEncodingException
     */
    String getSHA256(byte[] input, String JCEProvider) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
        MessageDigest hash = null;
        if (JCEProvider == null) {
            hash = MessageDigest.getInstance("SHA-256");
        } else {
            hash = MessageDigest.getInstance("SHA-256", JCEProvider);
        }
        byte[] digest = hash.digest(input);
        return new String(Base64.encode(digest));
    }

    /**
     * Generates a SHA-384 message-digest and returns the hex-encoded hash
     *
     * @param input - byte array containing input that needs to be hashed
     * @param JCEProvider - optional Provider
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnsupportedEncodingException
     */
    String getSHA384(byte[] input, String JCEProvider) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
        MessageDigest hash = null;
        if (JCEProvider == null) {
            hash = MessageDigest.getInstance("SHA-384");
        } else {
            hash = MessageDigest.getInstance("SHA-384", JCEProvider);
        }
        byte[] digest = hash.digest(input);
        return new String(Base64.encode(digest));
    }

    /**
     * Generates a SHA-512 message-digest and returns the hex-encoded hash
     *
     * @param input - byte array containing input that needs to be hashed
     * @param JCEProvider - optional Provider
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnsupportedEncodingException
     */
    String getSHA512(byte[] input, String JCEProvider) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
        MessageDigest hash = null;
        if (JCEProvider == null) {
            hash = MessageDigest.getInstance("SHA-512");
        } else {
            hash = MessageDigest.getInstance("SHA-512", JCEProvider);
        }
        byte[] digest = hash.digest(input);
        return new String(Base64.encode(digest));
    }

    /**
     * Generates a MD5 message-digest and returns the Base64-encoded hash
     *
     * @param input - byte array containing input that needs to be hashed
     * @param JCEProvider - optional Provider
     * @return
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws UnsupportedEncodingException
     */
    String getMD5(byte[] input, String JCEProvider) throws NoSuchAlgorithmException, NoSuchProviderException, UnsupportedEncodingException {
        MessageDigest hash = null;
        if (JCEProvider == null) {
            hash = MessageDigest.getInstance("MD5");
        } else {
            hash = MessageDigest.getInstance("MD5", JCEProvider);
        }
        byte[] digest = hash.digest(input);
        return new String(Base64.encode(digest));
    }

    /**
     * Converts bytes to hex-encoded values
     *
     * @param bytes The bytes to be converted
     * @return a hex representation of bytes
     */
    public String bytesToHex(byte[] bytes) {
        String digits = "0123456789abcdef";
        int length = bytes.length;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i != length; i++) {
            int v = bytes[i] & 0xff;
            sb.append(digits.charAt(v >> 4));
            sb.append(digits.charAt(v & 0xf));
        }
        return sb.toString();
    }

    /**
     * Extracts and returns the specified element from the XML document
     *
     * @param bais the java.io.ByteArrayInputStream containing the XML document
     * @param is org.xml.sax.InputSource - needed for the XPath expression evalutation
     * @param xpath javax.xml.xpath.XPath the object performing the search
     * @param expression java.lang.String containing the search string
     *
     * @return java.lang.String element from the Symkey XML document
     */
    static String getXPathElement(ByteArrayInputStream bais, InputSource is, XPath xpath, String expression) {
        String s = null;
        try {
            bais.reset();
            expression = expression.trim().concat("/text()");
            s = xpath.evaluate(expression, is);
        } catch (XPathExpressionException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getMessage());
        }
        return s;
    }

    /**
     * Extracts and returns the specified attribute from the XML document
     *
     * @param bais the java.io.ByteArrayInputStream containing the XMLdocument
     * @param is org.xml.sax.InputSource - needed for the XPath expression evalutation
     * @param xpath javax.xml.xpath.XPath the object performing the search
     * @param expression java.lang.String containing the search string
     *
     * @return java.lang.String element from the Symkey XML document
     */
    static String getXPathAttribute(ByteArrayInputStream bais, InputSource is, XPath xpath, String expression) {
        String s = null;
        try {
            bais.reset();
            expression = expression.trim();
            s = xpath.evaluate(expression, is);
        } catch (XPathExpressionException ex) {
            Common.log(Level.SEVERE, "SKCE-ERR-1000", ex.getLocalizedMessage());
        }
        return s;
    }

    /**
 * This method authenticates a credential - username and password - against the 
 * configured LDAP directory.  Only LDAP-based authentication is currently
 * supported; both Active Directory and a standards-based, open-source LDAP
 * directories are supported.  For the later, this has been tested with
 * OpenDS 2.0 (https://docs.opends.org).
 *
 * @param username String containing the credential's username
 * @param password String containing the user's password
 * @return boolean value indicating either True (for authenticated) or False
 * (for unauthenticated or failure in processing)
 */
    @Override
    public boolean authenticateUser(String username, String password) throws SkceWSException {
        String ldapurl = Common.getConfigurationProperty("skcengine.cfg.property.ldapurl");
        String dnprefix = Common.getConfigurationProperty("skcengine.cfg.property.dnprefix");
        String dnsuffix = Common.getConfigurationProperty("skcengine.cfg.property.dnsuffix");
        Common.log(Level.FINE, "SKCE-MSG-1023", "setup principal");
        String principal = dnprefix + username + dnsuffix;
        Common.log(Level.FINE, "SKCE-MSG-1023", "new Hashtable()");
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, Common.getConfigurationProperty("skcengine.cfg.property.ldapctxfactory"));
        env.put(Context.PROVIDER_URL, ldapurl);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_AUTHENTICATION, Common.getConfigurationProperty("skcengine.cfg.property.ldapauthtype"));
        Common.log(Level.FINE, "SKCE-MSG-2004", principal);
        try {
            Common.log(Level.FINE, "SKCE-MSG-1023", "new InitialContext");
            Context ctx = new InitialContext(env);
            Common.log(Level.FINE, "SKCE-MSG-2005", principal);
            LdapContext lc = (LdapContext) ctx.lookup(principal);
            ctx.close();
            if (lc != null) return true; else return false;
        } catch (AuthenticationException ex) {
            Common.log(Level.WARNING, "SKCE-ERR-1000", "AuthenticationException: " + principal);
            throw new SkceWSException("AuthenticationException: " + principal);
        } catch (NamingException ex) {
            Common.log(Level.WARNING, "SKCE-ERR-1000", "NamingException: \n" + ex.getLocalizedMessage());
            throw new SkceWSException("NamingException: " + ex.getLocalizedMessage());
        }
    }

    /**
 * This method authenticates a credential - username and password - for a
 * specified operation against the configured LDAP directory.  Only LDAP-based
 * authentication is supported currently; however both Active Directory and a
 * standards-based, open-source LDAP directories are supported.  For the latter,
 * this has been tested with OpenDS 2.0 (https://docs.opends.org).
 *
 * @param username - String containing the credential's username
 * @param password - String containing the user's password
 * @param operation - String describing the operation being requested by the
 * user - either ENC or DEC for encryption and decryption respectively
 * @return boolean value indicating either True (for authenticated) or False
 * (for unauthenticated or failure in processing)
 */
    @Override
    public boolean authenticateUser(String username, String password, String operation) throws SkceWSException {
        String ldapurl = Common.getConfigurationProperty("skcengine.cfg.property.ldapurl");
        String dnprefix = Common.getConfigurationProperty("skcengine.cfg.property.dnprefix");
        String dnsuffix = Common.getConfigurationProperty("skcengine.cfg.property.dnsuffix");
        Common.log(Level.FINE, "SKCE-MSG-1023", "setup principal");
        String principal = dnprefix + username + dnsuffix;
        Common.log(Level.FINE, "SKCE-MSG-1023", "new Hashtable()");
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, Common.getConfigurationProperty("skcengine.cfg.property.ldapctxfactory"));
        env.put(Context.PROVIDER_URL, ldapurl);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.SECURITY_AUTHENTICATION, Common.getConfigurationProperty("skcengine.cfg.property.ldapauthtype"));
        Common.log(Level.FINE, "SKCE-MSG-2004", principal);
        try {
            Common.log(Level.FINE, "SKCE-MSG-1023", "new InitialContext");
            Context ctx = new InitialContext(env);
            String group = null;
            if (operation.equalsIgnoreCase("ENC")) {
                group = Common.getConfigurationProperty("skcengine.cfg.property.ldapencryptiongroup");
            } else if (operation.equalsIgnoreCase("DEC")) {
                group = Common.getConfigurationProperty("skcengine.cfg.property.ldapdecryptiongroup");
            } else if (operation.equalsIgnoreCase("CMV")) {
                group = Common.getConfigurationProperty("skcengine.cfg.property.ldapcloudmovegroup");
            } else if (operation.equalsIgnoreCase("ADM")) {
                group = Common.getConfigurationProperty("skcengine.cfg.property.ldapadmingroup");
            } else {
                Common.log(Level.WARNING, "SKCE-ERR-1010", "User-Operation=" + username + "-" + operation + "]");
                return false;
            }
            Common.log(Level.FINE, "SKCE-MSG-2005", group);
            LdapContext lc = (LdapContext) ctx.lookup(group);
            if (lc != null) {
                String ldaptype = Common.getConfigurationProperty("skcengine.cfg.property.ldaptype");
                Attributes attrs = lc.getAttributes("");
                if (ldaptype.equalsIgnoreCase("AD")) {
                    String[] attrIDs = { "member" };
                    attrs = lc.getAttributes("", attrIDs);
                } else if (ldaptype.equalsIgnoreCase("LDAP")) {
                    String[] attrIDs = { "uniqueMember" };
                    attrs = lc.getAttributes("", attrIDs);
                }
                for (NamingEnumeration ne = attrs.getAll(); ne.hasMore(); ) {
                    Attribute attr = (Attribute) ne.next();
                    String unqmem = null;
                    NamingEnumeration e = attr.getAll();
                    while (e.hasMore()) {
                        unqmem = (String) e.next();
                        Common.log(Level.FINE, "SKCE-MSG-2004", unqmem);
                        if (unqmem.equalsIgnoreCase((String) env.get(Context.SECURITY_PRINCIPAL))) {
                            Common.log(Level.INFO, "SKCE-MSG-2006", group + " (" + principal + ")");
                            ctx.close();
                            return true;
                        }
                    }
                    Common.log(Level.INFO, "SKCE-MSG-2007", group + " (" + principal + ")");
                    ctx.close();
                    return false;
                }
            }
        } catch (AuthenticationException ex) {
            Common.log(Level.WARNING, "SKCE-ERR-1000", "AuthenticationException: " + principal);
            throw new SkceWSException("AuthenticationException: " + principal);
        } catch (NamingException ex) {
            Common.log(Level.WARNING, "SKCE-ERR-1000", "NamingException: \n" + ex.getLocalizedMessage());
            throw new SkceWSException("NamingException: " + ex.getLocalizedMessage());
        }
        return false;
    }
}

/**
 *
 * An object to hold to hold the decrypted symmetric-key with the time it
 * was last used by the system.  An independent "cron" thread periodically
 * runs through the cache and removes SecretKeys that haven't been used
 * for a preconfigured duration.
 */
class SecretKeyObject {

    private SecretKey secretkey = null;

    private long lastused;

    private String algorithm;

    private String transform;

    SecretKeyObject(SecretKey secretkey, String algorithm, String transform) {
        this.secretkey = secretkey;
        this.algorithm = algorithm;
        this.transform = transform;
        lastused = System.currentTimeMillis();
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getTransform() {
        return transform;
    }

    public void setTransform(String transform) {
        this.transform = transform;
    }

    protected void setSecretKey(SecretKey secretkey) {
        this.secretkey = secretkey;
        lastused = System.currentTimeMillis();
    }

    protected SecretKey getSecretKey() {
        return secretkey;
    }

    protected void setLastUsed(long timeinms) {
        this.lastused = timeinms;
    }

    protected long getLastUsed() {
        return lastused;
    }
}
