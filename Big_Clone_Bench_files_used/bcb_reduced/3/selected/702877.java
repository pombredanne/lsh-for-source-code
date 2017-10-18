package org.isodl.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CertificateParser;
import net.sourceforge.scuba.smartcards.CardFileInputStream;
import net.sourceforge.scuba.smartcards.CardServiceException;
import net.sourceforge.scuba.util.Hex;

/**
 * A class for encapsulating the whole driving license contents: the files, and
 * the encryption keys. Can be intialized (a) almost empty, (b) from the
 * DrivingLicenseService object (i.e. read in from the card), (c) form a ZIP
 * file containg the data groups.
 * 
 * @author Wojciech Mostowski <woj@cs.ru.nl>
 * 
 */
public class DrivingLicense {

    private static final int BUFFER_SIZE = 243;

    private Map<Short, InputStream> rawStreams = new HashMap<Short, InputStream>();

    private Map<Short, InputStream> bufferedStreams = new HashMap<Short, InputStream>();

    private Map<Short, byte[]> filesBytes = new HashMap<Short, byte[]>();

    private Map<Short, Integer> fileLengths = new TreeMap<Short, Integer>();

    private Map<Short, Boolean> eapFlags = new TreeMap<Short, Boolean>();

    private int bytesRead = 0;

    private int totalLength = 0;

    private COMFile comFile = null;

    private SODFile sodFile = null;

    private boolean eapSupport = false;

    private boolean eapSuccess = false;

    private CVCertificate cvcaCertificate = null;

    private PrivateKey eapPrivateKey = null;

    private PrivateKey aaPrivateKey = null;

    private DocumentSigner signer = null;

    private List<Short> eapFids = new ArrayList<Short>();

    private byte[] keySeed = null;

    private boolean updateCOMSODfiles = true;

    private BufferedInputStream preReadFile(DrivingLicenseService service, short fid) throws CardServiceException {
        BufferedInputStream bufferedIn = null;
        if (rawStreams.containsKey(fid)) {
            int length = fileLengths.get(fid);
            bufferedIn = new BufferedInputStream(rawStreams.get(fid), length + 1);
            bufferedIn.mark(length + 1);
            rawStreams.put(fid, bufferedIn);
            return bufferedIn;
        } else {
            service.getFileSystem().selectFile(fid);
            CardFileInputStream cardIn = service.readFile();
            int length = cardIn.getFileLength();
            bufferedIn = new BufferedInputStream(cardIn, length + 1);
            totalLength += length;
            fileLengths.put(fid, length);
            bufferedIn.mark(length + 1);
            rawStreams.put(fid, bufferedIn);
            return bufferedIn;
        }
    }

    private void setupFile(DrivingLicenseService service, short fid) throws CardServiceException {
        service.getFileSystem().selectFile(fid);
        CardFileInputStream in = service.readFile();
        int fileLength = in.getFileLength();
        in.mark(fileLength + 1);
        rawStreams.put(fid, in);
        totalLength += fileLength;
        fileLengths.put(fid, fileLength);
    }

    /**
     * Constructor. Reads in the driving license data from the driving license
     * card service.
     * 
     * @param service
     *            the driving license card service
     * @throws IOException
     *             on problems
     * @throws CardServiceException
     *             on problems
     */
    public DrivingLicense(DrivingLicenseService service) throws IOException, CardServiceException {
        this(service, null);
    }

    /**
     * Constructor. Reads in the driving license data from the driving license
     * card service.
     * 
     * @param service
     *            the driving license card service
     * @param documentNumber
     *            the document number to use for EAP, if not provided (null) the
     *            one from DG1 will be used.
     * @throws IOException
     *             on problems
     * @throws CardServiceException
     *             on problems
     */
    public DrivingLicense(DrivingLicenseService service, String documentNumber) throws IOException, CardServiceException {
        BufferedInputStream bufferedIn = null;
        bufferedIn = preReadFile(service, DrivingLicenseService.EF_COM);
        comFile = new COMFile(bufferedIn);
        bufferedIn.reset();
        String caRef = null;
        SecurityObjectIndicator[] indicators = comFile.getSOIArray();
        for (SecurityObjectIndicator indicator : indicators) {
            if (indicator instanceof SecurityObjectIndicatorDG14) {
                eapSupport = true;
                SecurityObjectIndicatorDG14 i = (SecurityObjectIndicatorDG14) indicator;
                caRef = new String(i.getCertificateSubjectId(), 1, i.getCertificateSubjectId()[0]);
                List<Integer> dgs = i.getDataGroups();
                for (Integer dg : dgs) {
                    eapFids.add(DrivingLicenseFile.lookupFIDByTag(DrivingLicenseFile.lookupTagByDataGroupNumber(dg)));
                    eapFlags.put(DrivingLicenseFile.lookupFIDByTag(DrivingLicenseFile.lookupTagByDataGroupNumber(dg)), true);
                }
            }
        }
        DG14File dg14file = null;
        for (int tag : comFile.getTagList()) {
            short fid = DrivingLicenseFile.lookupFIDByTag(tag);
            if (fid == DrivingLicenseService.EF_DG14) {
                bufferedIn = preReadFile(service, DrivingLicenseService.EF_DG14);
                dg14file = new DG14File(bufferedIn);
                bufferedIn.reset();
            } else {
                if (!eapFids.contains(fid)) {
                    setupFile(service, fid);
                }
            }
        }
        bufferedIn = preReadFile(service, DrivingLicenseService.EF_SOD);
        sodFile = new SODFile(bufferedIn);
        bufferedIn.reset();
        if (eapSupport) {
            List<CVCertificate> termCerts = null;
            PrivateKey termKey = null;
            TerminalCVCertificateDirectory d = TerminalCVCertificateDirectory.getInstance();
            if (caRef != null) {
                try {
                    List<CVCertificate> t = d.getCertificates(caRef);
                    if (t != null) {
                        termCerts = t;
                        termKey = d.getPrivateKey(caRef);
                    }
                } catch (NoSuchElementException nsee) {
                    nsee.printStackTrace();
                }
            }
            if (termCerts == null || termCerts.size() == 0) {
                return;
            }
            if (documentNumber == null) {
                bufferedIn = preReadFile(service, DrivingLicenseService.EF_DG1);
                documentNumber = new DG1File(bufferedIn).getDriverInfo().number;
                bufferedIn.reset();
            }
            Set<Integer> keyIds = dg14file.getIds();
            for (int i : keyIds) {
                try {
                    service.doEAP(i, dg14file.getKey(i), termCerts, termKey, documentNumber);
                    eapSuccess = true;
                    break;
                } catch (CardServiceException cse) {
                    cse.printStackTrace();
                }
            }
            if (eapSuccess) {
                for (Short fid : eapFids) {
                    setupFile(service, fid);
                }
            }
        }
    }

    /**
     * Constructor. Reads in the driving license data from a ZIP file.
     * 
     * @param file
     *            the ZIP file
     * @throws IOException
     *             on problems
     */
    public DrivingLicense(File file) throws IOException {
        this(file, true, null);
    }

    /**
     * Constructor. Reads in the driving license data from a ZIP file.
     * 
     * @param file
     *            the ZIP file
     * @param allowInconsistent whether the ZIP file can be incomplete (the missing data will be ignored)
     * @param docSigningPrivateKey private key to sign the data on the driving license (SOD), can be null
     * @throws IOException
     *             on problems
     */
    public DrivingLicense(File file, boolean allowInconsistent, DocumentSigner signer) throws IOException {
        this.signer = signer;
        ZipFile zipIn = new ZipFile(file);
        Enumeration<? extends ZipEntry> entries = zipIn.entries();
        List<ZipEntry> entryList = new ArrayList<ZipEntry>();
        boolean generateaa = false;
        boolean generateca = false;
        String signatureAlgorithm = "SHA256withRSA";
        X509Certificate docCertificate = null;
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String fileName = entry.getName();
            if (fileName.equals("keyseed.bin")) {
                int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
                if (size != 16) {
                    throw new IOException("Wrong key seed length in " + file);
                }
                keySeed = new byte[16];
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(keySeed);
            } else if (fileName.equals("generateaa.key")) {
                generateaa = true;
            } else if (fileName.equals("generateca.key")) {
                generateca = true;
            } else if (fileName.equals("aaprivatekey.der")) {
                int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
                byte[] keyData = new byte[size];
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(keyData);
                try {
                    KeyFactory kf = KeyFactory.getInstance("RSA");
                    aaPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyData));
                } catch (Exception ex) {
                    throw new IOException("Invalid RSA private key: " + ex.getMessage());
                }
            } else if (fileName.equals("caprivatekey.der")) {
                int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
                byte[] keyData = new byte[size];
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(keyData);
                try {
                    KeyFactory kf = KeyFactory.getInstance("ECDH");
                    eapPrivateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(keyData));
                } catch (Exception ex) {
                    throw new IOException("Invalid ECDH private key: " + ex.getMessage());
                }
            } else if (fileName.equals("cacert.cvcert")) {
                int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
                byte[] data = new byte[size];
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(data);
                try {
                    cvcaCertificate = (CVCertificate) CertificateParser.parseCertificate(data);
                } catch (Exception ex) {
                    throw new IOException("Invalid CVCA certificate: " + ex.getMessage());
                }
            } else if (fileName.equals("doccert.der")) {
                int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
                byte[] data = new byte[size];
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(data);
                try {
                    docCertificate = (X509Certificate) CertificateFactory.getInstance("X509").generateCertificate(new ByteArrayInputStream(data));
                    signer.setCertificate(docCertificate);
                } catch (Exception ex) {
                    throw new IOException("Invalid doc certificate: " + ex.getMessage());
                }
            } else if (fileName.equals("001D.bin") || fileName.equals("001E.bin")) {
                updateCOMSODfiles = false;
                entryList.add(entry);
            } else {
                entryList.add(entry);
            }
        }
        if (docCertificate == null && updateCOMSODfiles) {
            docCertificate = generateDummyCertificate(signatureAlgorithm);
        }
        for (ZipEntry entry : entryList) {
            String fileName = entry.getName();
            int size = (int) (entry.getSize() & 0x00000000FFFFFFFFL);
            try {
                boolean eapProtection = fileName.indexOf("eap") != -1;
                int delimIndex = eapProtection ? fileName.indexOf("eap") : fileName.indexOf('.');
                if (delimIndex != 4) {
                    System.out.println("DEBUG: skipping file " + fileName + "(delimIndex == " + delimIndex + ")");
                    continue;
                }
                short fid = Hex.hexStringToShort(fileName.substring(0, delimIndex));
                if (cvcaCertificate == null) {
                    eapProtection = false;
                }
                byte[] bytes = new byte[size];
                int fileLength = bytes.length;
                fileLengths.put(fid, fileLength);
                DataInputStream dataIn = new DataInputStream(zipIn.getInputStream(entry));
                dataIn.readFully(bytes);
                rawStreams.put(fid, new ByteArrayInputStream(bytes));
                eapFlags.put(fid, eapProtection);
                totalLength += fileLength;
                if (fid == DrivingLicenseService.EF_COM) {
                    comFile = new COMFile(new ByteArrayInputStream(bytes));
                } else if (fid == DrivingLicenseService.EF_SOD) {
                    sodFile = new SODFile(new ByteArrayInputStream(bytes));
                }
            } catch (IOException ioe) {
            } catch (NumberFormatException nfe) {
            }
        }
        try {
            if (updateCOMSODfiles && docCertificate == null) {
                throw new IOException("No SOD and no doc certificate provided.");
            }
            if (generateaa && aaPrivateKey != null) {
                throw new IOException("Both AA private key and request to generate one present.");
            }
            if (generateca && eapPrivateKey != null) {
                throw new IOException("Both CA private key and request to generate one present.");
            }
            if ((generateaa || generateca) && !updateCOMSODfiles) {
                throw new IOException("COM or SOD file present and request to generate private AA or CA keys.");
            }
            if (getFileList().contains(DrivingLicenseService.EF_DG13) && aaPrivateKey == null) {
                throw new IOException("DG13 present, but no AA private key.");
            }
            if (getFileList().contains(DrivingLicenseService.EF_DG14) && eapPrivateKey == null) {
                throw new IOException("DG14 present, but no CA private key.");
            }
        } catch (IOException ioe) {
            if (allowInconsistent) {
                updateCOMSODfiles = true;
                ioe.printStackTrace();
            } else {
                throw ioe;
            }
        }
        try {
            if (generateaa) {
                Provider provider = Security.getProvider("BC");
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", provider);
                generator.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4));
                setAAKeys(generator.generateKeyPair());
            }
            if (generateca) {
                String preferredProvider = "BC";
                Provider provider = Security.getProvider(preferredProvider);
                KeyPairGenerator generator = KeyPairGenerator.getInstance("ECDH", provider);
                generator.initialize(new ECGenParameterSpec(DrivingLicensePersoService.EC_CURVE_NAME));
                setEAPKeys(generator.generateKeyPair());
            }
            if (comFile == null) {
                comFile = new COMFile(1, 0, new ArrayList<Integer>(), null);
            }
            if (sodFile == null) {
                String digestAlgorithm = "SHA256";
                Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
                MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
                hashes.put(1, digest.digest(new byte[0]));
                hashes.put(2, digest.digest(new byte[0]));
                sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, this.signer, docCertificate);
            }
            updateCOMSODFile(docCertificate);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new IOException("Key generation failed: " + ex.getMessage());
        }
    }

    private X509Certificate generateDummyCertificate(String signatureAlgorithm) {
        try {
            Date today = Calendar.getInstance().getTime();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = keyPair.getPublic();
            PrivateKey privateKey = keyPair.getPrivate();
            Date dateOfIssuing = today;
            Date dateOfExpiry = today;
            X509V3CertificateGenerator certGenerator = new X509V3CertificateGenerator();
            certGenerator.setSerialNumber(new BigInteger("1"));
            certGenerator.setIssuerDN(new X509Name("C=NL, O=ISODL, OU=CSCA, CN=isodl.sourceforge.net/emailAddress=woj@cs.ru.nl"));
            certGenerator.setSubjectDN(new X509Name("C=NL, O=ISODL, OU=DSCA, CN=isodl.sourceforge.net/emailAddress=woj@cs.ru.nl"));
            certGenerator.setNotBefore(dateOfIssuing);
            certGenerator.setNotAfter(dateOfExpiry);
            certGenerator.setPublicKey(publicKey);
            certGenerator.setSignatureAlgorithm(signatureAlgorithm);
            X509Certificate cert = (X509Certificate) certGenerator.generate(privateKey, "BC");
            if (signer == null) {
                signer = new SimpleDocumentSigner(privateKey, cert);
            }
            return cert;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Constructs an alomost empty driving license with just the COM and SOD
     * files.
     * 
     * @throws GeneralSecurityException
     *             when ciphers or similar are not available.
     */
    public DrivingLicense() throws GeneralSecurityException {
        List<Integer> tagList = new ArrayList<Integer>();
        comFile = new COMFile(1, 0, tagList, null);
        byte[] comBytes = comFile.getEncoded();
        int fileLength = comBytes.length;
        totalLength += fileLength;
        fileLengths.put(DrivingLicenseService.EF_COM, fileLength);
        rawStreams.put(DrivingLicenseService.EF_COM, new ByteArrayInputStream(comBytes));
        String signatureAlgorithm = "SHA256withRSA";
        X509Certificate certificate = generateDummyCertificate(signatureAlgorithm);
        String digestAlgorithm = "SHA256";
        Map<Integer, byte[]> hashes = new HashMap<Integer, byte[]>();
        MessageDigest digest = MessageDigest.getInstance(digestAlgorithm);
        hashes.put(1, digest.digest(new byte[0]));
        hashes.put(2, digest.digest(new byte[0]));
        sodFile = new SODFile(digestAlgorithm, signatureAlgorithm, hashes, signer, certificate);
        byte[] sodBytes = sodFile.getEncoded();
        fileLength = sodBytes.length;
        totalLength += fileLength;
        fileLengths.put(DrivingLicenseService.EF_SOD, fileLength);
        rawStreams.put(DrivingLicenseService.EF_SOD, new ByteArrayInputStream(sodBytes));
    }

    /**
     * Gets an inputstream that is ready for reading.
     * 
     * @param fid
     * @return the input stream for reading
     */
    public synchronized InputStream getInputStream(final short fid) {
        try {
            InputStream in = null;
            byte[] file = filesBytes.get(fid);
            if (file != null) {
                in = new ByteArrayInputStream(file);
                in.mark(file.length + 1);
            } else {
                in = bufferedStreams.get(fid);
                if (in != null && in.markSupported()) {
                    in.reset();
                }
            }
            if (in == null) {
                startCopyingRawInputStream(fid);
                in = bufferedStreams.get(fid);
            }
            return in;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("ERROR: " + ioe.toString());
        }
    }

    /**
     * Starts a thread to read the raw inputstream.
     * 
     * @param fid
     * @throws IOException
     */
    public synchronized void startCopyingRawInputStream(final short fid) throws IOException {
        final DrivingLicense dl = this;
        final InputStream unBufferedIn = rawStreams.get(fid);
        if (unBufferedIn == null) {
            throw new IOException("No raw inputstream to copy " + Integer.toHexString(fid));
        }
        final int fileLength = fileLengths.get(fid);
        unBufferedIn.reset();
        final PipedInputStream pipedIn = new PipedInputStream(fileLength + 1);
        final PipedOutputStream out = new PipedOutputStream(pipedIn);
        final ByteArrayOutputStream copyOut = new ByteArrayOutputStream();
        InputStream in = new BufferedInputStream(pipedIn, fileLength + 1);
        in.mark(fileLength + 1);
        bufferedStreams.put(fid, in);
        (new Thread(new Runnable() {

            public void run() {
                byte[] buf = new byte[BUFFER_SIZE];
                try {
                    while (true) {
                        int bytesRead = unBufferedIn.read(buf);
                        if (bytesRead < 0) {
                            break;
                        }
                        out.write(buf, 0, bytesRead);
                        copyOut.write(buf, 0, bytesRead);
                        dl.bytesRead += bytesRead;
                    }
                    out.flush();
                    out.close();
                    copyOut.flush();
                    byte[] copyOutBytes = copyOut.toByteArray();
                    filesBytes.put(fid, copyOutBytes);
                    copyOut.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        })).start();
    }

    /**
     * Puts/replaces the given file in this driving license. Triggers all
     * necessary changes (COM/SOD file update, resigning, etc.)
     * 
     * @param fid
     *            the FID of the file
     * @param bytes
     *            the file contents
     */
    public void putFile(short fid, byte[] bytes) {
        putFile(fid, bytes, false);
    }

    /**
     * Puts/replaces the given file in this driving license. Triggers all
     * necessary changes (COM/SOD file update, resigning, etc.)
     * 
     * @param fid
     *            the FID of the file
     * @param bytes
     *            the file contents
     * @param eapProtection
     *            whether the file should be EAP protected
     */
    public void putFile(short fid, byte[] bytes, boolean eapProtection) {
        if (bytes == null) {
            return;
        }
        updateCOMSODfiles = true;
        filesBytes.put(fid, bytes);
        eapFlags.put(fid, eapProtection);
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        int fileLength = bytes.length;
        in.mark(fileLength + 1);
        bufferedStreams.put(fid, in);
        fileLengths.put(fid, fileLength);
        totalLength += fileLength;
        if (fid != DrivingLicenseService.EF_COM && fid != DrivingLicenseService.EF_SOD) {
            updateCOMSODFile(null);
        }
    }

    /**
     * Removes the given file in this driving license. Triggers all necessary
     * changes (COM/SOD file update, resigning, etc.)
     * 
     * @param fid
     *            the FID of the file to be removed
     */
    public void removeFile(short fid) {
        filesBytes.remove(fid);
        eapFlags.remove(fid);
        int fileLength = fileLengths.get(fid);
        bufferedStreams.remove(fid);
        fileLengths.remove(fid);
        totalLength -= fileLength;
        if (fid != DrivingLicenseService.EF_COM && fid != DrivingLicenseService.EF_SOD) {
            updateCOMSODFile(null);
        }
    }

    private void updateCOMSODFile(X509Certificate newCertificate) {
        if (!updateCOMSODfiles || sodFile == null || comFile == null) {
            return;
        }
        try {
            String digestAlg = sodFile.getDigestAlgorithm();
            X509Certificate cert = newCertificate != null ? newCertificate : sodFile.getDocSigningCertificate();
            String signatureAlg = cert.getSigAlgName();
            byte[] signature = sodFile.getEncryptedDigest();
            Map<Integer, byte[]> dgHashes = new TreeMap<Integer, byte[]>();
            List<Short> dgFids = getFileList();
            if (dgFids.size() < 4) {
                return;
            }
            comFile.getTagList().clear();
            Collections.sort(dgFids);
            MessageDigest digest = MessageDigest.getInstance(digestAlg);
            for (Short fid : dgFids) {
                if (fid != DrivingLicenseService.EF_COM && fid != DrivingLicenseService.EF_SOD) {
                    byte[] data = getFileBytes(fid);
                    byte tag = data[0];
                    dgHashes.put(DrivingLicenseFile.lookupDataGroupNumberByTag(tag), digest.digest(data));
                    comFile.insertTag(new Integer(tag));
                }
            }
            if (signer != null) {
                signer.setCertificate(cert);
                sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, signer, cert);
            } else {
                sodFile = new SODFile(digestAlg, signatureAlg, dgHashes, signature, cert);
            }
            updateSOIS();
            putFile(DrivingLicenseService.EF_SOD, sodFile.getEncoded());
            putFile(DrivingLicenseService.EF_COM, comFile.getEncoded());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the contents of the given file.
     * 
     * @param fid
     *            the file's FID
     * @return the file contents
     */
    public byte[] getFileBytes(short fid) {
        byte[] result = filesBytes.get(fid);
        if (result != null) {
            return result;
        }
        InputStream in = getInputStream(fid);
        if (in == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[256];
        while (true) {
            try {
                int bytesRead = in.read(buf);
                if (bytesRead < 0) {
                    break;
                }
                out.write(buf, 0, bytesRead);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
        return out.toByteArray();
    }

    /**
     * Sets the current document signer. Triggers all
     * necessary changes (COM/SOD file update, resigning, etc.)
     * 
     * @param signer
     *            the document signer
     */
    public void setSigner(DocumentSigner signer) {
        updateCOMSODfiles = true;
        this.signer = signer;
        updateCOMSODFile(null);
    }

    /**
     * Sets the current document signing certificate. Alters SOD file. Triggers
     * all necessary changes (COM/SOD file update, resigning, etc.)
     * 
     * @param newCertificate
     *            the new document signing certificate
     */
    public void setDocSigningCertificate(X509Certificate newCertificate) {
        updateCOMSODfiles = true;
        updateCOMSODFile(newCertificate);
    }

    private void updateSOIS() {
        if (!updateCOMSODfiles || comFile == null) {
            return;
        }
        SecurityObjectIndicatorDG13 soi13 = null;
        SecurityObjectIndicatorDG14 soi14 = null;
        if (getFileList().contains(DrivingLicenseService.EF_DG13) && aaPrivateKey != null) {
            soi13 = new SecurityObjectIndicatorDG13(new ArrayList<Integer>());
        }
        if (getFileList().contains(DrivingLicenseService.EF_DG14) && eapPrivateKey != null && cvcaCertificate != null) {
            List<Integer> dgs = new ArrayList<Integer>();
            for (short fid : getFileList()) {
                if (eapFlags.get(fid)) {
                    dgs.add(DrivingLicenseFile.lookupDataGroupNumberByFID(fid));
                }
            }
            Collections.sort(dgs);
            soi14 = new SecurityObjectIndicatorDG14(cvcaCertificate, dgs);
        }
        int length = (soi13 != null ? 1 : 0) + (soi14 != null ? 1 : 0);
        SecurityObjectIndicator[] sois = new SecurityObjectIndicator[length];
        int index = 0;
        if (soi13 != null) {
            sois[index++] = soi13;
        }
        if (soi14 != null) {
            sois[index] = soi14;
        }
        comFile.setSOIArray(sois);
    }

    /**
     * Sets the current EAP CVCA certificate. Alters COM file. Triggers the
     * update of Security Object Indicators in the COM file.
     * 
     * @param cert
     *            the new EAP CVCA certificate
     */
    public void setCVCertificate(CVCertificate cert) {
        this.cvcaCertificate = cert;
        updateCOMSODfiles = true;
        updateSOIS();
    }

    /**
     * @return the stored EAP CVCA certificate
     */
    public CVCertificate getCVCertificate() {
        return cvcaCertificate;
    }

    /**
     * 
     * @return the current document signer
     */
    public DocumentSigner getSigner() {
        return signer;
    }

    /**
     * Sets the EAP key pair (alters DG14). Triggers all necessary changes
     * (COM/SOD file update, resigning, etc.)
     * 
     * @param keyPair
     *            the EAP key pair
     */
    public void setEAPKeys(KeyPair keyPair) {
        this.eapPrivateKey = keyPair.getPrivate();
        Map<Integer, PublicKey> key = new TreeMap<Integer, PublicKey>();
        key.put(-1, keyPair.getPublic());
        DG14File dg14file = new DG14File(key);
        putFile(DrivingLicenseService.EF_DG14, dg14file.getEncoded());
    }

    /**
     * Sets the AA key pair (alters DG13). Triggers all necessary changes
     * (COM/SOD file update, resigning, etc.)
     * 
     * @param keyPair
     *            the AA key pair
     */
    public void setAAKeys(KeyPair keyPair) {
        this.aaPrivateKey = keyPair.getPrivate();
        DG13File dg13file = new DG13File(keyPair.getPublic());
        putFile(DrivingLicenseService.EF_DG13, dg13file.getEncoded());
    }

    /**
     * 
     * @return the current AA private key
     */
    public PrivateKey getAAPrivateKey() {
        return aaPrivateKey;
    }

    /**
     * Sets the current AA private key.
     * 
     * @param key
     *            the AA private key.
     */
    public void setAAPrivateKey(PrivateKey key) {
        aaPrivateKey = key;
        updateSOIS();
    }

    /**
     * Sets the AA public key (alters DG13). Triggers all necessary changes
     * (COM/SOD file update, resigning, etc.)
     * 
     * @param key
     *            the AA public key
     */
    public void setAAPublicKey(PublicKey key) {
        DG13File dg13file = new DG13File(key);
        putFile(DrivingLicenseService.EF_DG13, dg13file.getEncoded());
    }

    /**
     * 
     * @return the current EAP private key
     */
    public PrivateKey getEAPPrivateKey() {
        return eapPrivateKey;
    }

    /**
     * 
     * @return whether the driving license has EAP support
     */
    public boolean hasEAP() {
        return eapSupport;
    }

    /**
     * 
     * @return whether EAP was successfully performed.
     */
    public boolean wasEAPPerformed() {
        return eapSuccess;
    }

    /**
     * 
     * @return the list of FIDs that are EAP protected on this driving license.
     */
    public List<Short> getEAPFiles() {
        return eapFids;
    }

    /**
     * 
     * @return total length of all the files.
     */
    public int getTotalLength() {
        return totalLength;
    }

    /**
     * 
     * @return total number of files read in so far from the driving license
     *         (card).
     */
    public int getBytesRead() {
        return bytesRead;
    }

    /**
     * 
     * @return the list of all FIDS contained in this driving license.
     */
    public List<Short> getFileList() {
        List<Short> result = new ArrayList<Short>();
        result.addAll(fileLengths.keySet());
        return result;
    }

    /**
     * 
     * @return the stored key seed, null if missing
     */
    public byte[] getKeySeed() {
        return keySeed;
    }

    /**
     * Sets the key seed. 
     */
    public void setKeySeed(byte[] keySeed) {
        this.keySeed = keySeed;
    }

    /**
     * Uploads this driving license to the card using the provided
     * personalisation service.
     * 
     * @param persoService
     *            the driving license personalisation service
     * @throws CardServiceException
     *             on errors
     */
    public void upload(DrivingLicensePersoService persoService) throws CardServiceException {
        upload(persoService, null);
    }

    /**
     * Uploads this driving license to the card using the provided
     * personalisation service.
     * 
     * @param persoService
     *            the driving license personalisation service
     * @param keySeed
     *            the key seed string (SAI) to use
     * @throws CardServiceException
     *             on errors
     */
    public void upload(DrivingLicensePersoService persoService, byte[] keySeed) throws CardServiceException {
        if (keySeed == null && this.keySeed != null) {
            keySeed = this.keySeed;
            if (keySeed.length != 16) {
                throw new CardServiceException("Wrong key seed length.");
            }
        }
        List<Short> fileList = getFileList();
        String sicId = null;
        boolean enableAASupport = aaPrivateKey != null;
        if (fileList.contains(DrivingLicenseService.EF_DG13) != enableAASupport) {
            throw new CardServiceException("DG13 present, but no AA private key found, or vice versa.");
        }
        boolean enableCASupport = eapPrivateKey != null;
        if (fileList.contains(DrivingLicenseService.EF_DG14) != enableCASupport) {
            throw new CardServiceException("DG14 present, but no CA private key found, or vice versa.");
        }
        boolean enableTASupport = enableCASupport && cvcaCertificate != null;
        List<Integer> eapDGS = new ArrayList<Integer>();
        for (SecurityObjectIndicator soi : comFile.getSOIArray()) {
            if (soi instanceof SecurityObjectIndicatorDG13) {
                if (!enableAASupport) {
                    throw new CardServiceException("AA support declared in COM, but no required AA data present.");
                }
            } else if (soi instanceof SecurityObjectIndicatorDG14) {
                if (!enableTASupport) {
                    throw new CardServiceException("EAP support declared in COM, but no required CA/TA data present.");
                }
                eapDGS.addAll(((SecurityObjectIndicatorDG14) soi).getDataGroups());
            }
        }
        for (short fid : fileList) {
            byte[] fileBytes = getFileBytes(fid);
            boolean eapProtection = eapDGS.contains(DrivingLicenseFile.lookupDataGroupNumberByFID(fid));
            persoService.createFile(fid, (short) fileBytes.length, eapProtection);
            persoService.selectFile(fid);
            ByteArrayInputStream in = new ByteArrayInputStream(fileBytes);
            persoService.writeFile(fid, in);
            if (enableTASupport && fid == DrivingLicenseService.EF_DG1) {
                try {
                    DG1File dg1 = new DG1File(new ByteArrayInputStream(fileBytes));
                    sicId = dg1.getDriverInfo().number;
                } catch (IOException ioe) {
                }
            }
        }
        if (enableAASupport) {
            persoService.putPrivateKey(aaPrivateKey);
        }
        if (enableCASupport) {
            persoService.putPrivateEAPKey((ECPrivateKey) eapPrivateKey);
        }
        if (enableTASupport) {
            persoService.putCVCertificate(cvcaCertificate);
            persoService.setSicId(sicId);
        }
        if (keySeed != null) {
            persoService.setBAP(keySeed);
        }
        persoService.lockApplet();
    }
}
