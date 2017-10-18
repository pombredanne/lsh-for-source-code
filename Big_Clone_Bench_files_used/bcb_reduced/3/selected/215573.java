package org.infoeng.icws;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringBufferInputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.security.auth.x500.X500Principal;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.infoeng.icws.documents.CertificationRequest;
import org.infoeng.icws.documents.ICWSDocument;
import org.infoeng.icws.documents.InformationCurrencyUnit;
import org.infoeng.icws.documents.InformationCurrencySeries;
import org.infoeng.icws.documents.InformationIdentifier;
import org.infoeng.icws.documents.SeriesInfo;
import org.infoeng.icws.documents.SeriesListing;
import org.infoeng.icws.documents.PrimarySeriesInfo;
import org.infoeng.icws.documents.VerificationCertificate;
import org.infoeng.icws.exception.PermissionException;
import org.infoeng.icws.utils.Base64;
import org.infoeng.icws.utils.ICWSConstants;
import org.infoeng.icws.utils.ICWSUtils;
import org.infoeng.icws.utils.Utils;
import org.apache.axis.AxisFault;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.XMLUtils;
import org.apache.xpath.XPathAPI;
import org.xml.sax.InputSource;
import org.apache.commons.logging.Log;
import org.apache.axis.components.logger.LogFactory;

public class ICWSServerImpl {

    private String protocolString;

    private String hostName;

    private String hostPort;

    private String hostPath;

    private String seriesInfoPath;

    private Integer certNum;

    private Float withholding;

    private Integer numBytes;

    private String ldapURL;

    private String ldapUser;

    private String ldapPass;

    private Properties icwsProps;

    private KeyPairGenerator icwsKPG;

    private Signature icwsDSA;

    private String databaseDriver;

    private String databaseURL;

    private String databaseUser;

    private String databasePassword;

    private long certificateTimespan;

    private DSAPrivateKey icwsPrivateKey;

    private DSAPublicKey icwsPublicKey;

    private boolean icwsIncludeKeyInfo;

    public static final int PRIMARY_SERIES = 1;

    public static final int SECONDARY_SERIES = 2;

    public static final int CERTIFY_INFORMATION = 1;

    public static final int SECONDARY_ISSUANCE = 2;

    public static final int STORE_IC = 3;

    public static final int EXCHANGE_CERTIFICATE = 4;

    public static final int ACCOUNT_BALANCE = 5;

    public static final int RETRIEVE_IC = 6;

    public static final int SET_PARAMS = 7;

    public static final String ADMIN_USERNAME = "icwsAdmin";

    private static Log log = LogFactory.getLog("ICWSServerImpl.class");

    public ICWSServerImpl() throws AxisFault {
        icwsProps = new java.util.Properties();
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            InputStream icwsPropsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream("icws-properties.xml");
            icwsProps.loadFromXML(icwsPropsIS);
            if (ICWSUtils.getServiceEndpoint(icwsProps) == null) throw new AxisFault("ServiceEndpoint not defined.");
            if (ICWSUtils.getSeriesID(icwsProps, "") == null) throw new AxisFault("SeriesID path not defined!");
            protocolString = icwsProps.getProperty("protocol");
            hostName = icwsProps.getProperty("hostname");
            hostPort = icwsProps.getProperty("port");
            hostPath = icwsProps.getProperty("servicePath");
            ldapURL = icwsProps.getProperty("ldapURL");
            ldapUser = icwsProps.getProperty("ldapUser");
            ldapPass = icwsProps.getProperty("ldapPass");
            Integer keybits = new Integer(icwsProps.getProperty("keyBits"));
            withholding = new Float(icwsProps.getProperty("withholding"));
            if ((withholding.floatValue() < 0) || (withholding.floatValue() > 1)) {
                withholding = new Float(".025");
            }
            certNum = new Integer(icwsProps.getProperty("certificateNumber"));
            if ((certNum.intValue() < 0) || (certNum.intValue() > 1000)) {
                certNum = new Integer("40");
            }
            seriesInfoPath = icwsProps.getProperty("seriesInfoPath");
            numBytes = new Integer(icwsProps.getProperty("numberBytes"));
            icwsKPG = KeyPairGenerator.getInstance("DSA");
            icwsKPG.initialize(keybits.intValue());
            icwsDSA = Signature.getInstance(ICWSConstants.SIGNATURE_ALGORITHM);
            databaseDriver = icwsProps.getProperty("databaseDriver");
            databaseURL = icwsProps.getProperty("databaseURL");
            databaseUser = icwsProps.getProperty("databaseUser");
            databasePassword = icwsProps.getProperty("databasePassword");
            certificateTimespan = Long.parseLong(icwsProps.getProperty("certificateTimespan"));
            if (icwsProps.getProperty("documentSignatureKeyInclude") != null && icwsProps.getProperty("documentSignatureKeyInclude").trim().equals("false")) {
                icwsIncludeKeyInfo = false;
            } else {
                icwsIncludeKeyInfo = true;
            }
            if (!(icwsProps.getProperty("keystoreFilename") == null || icwsProps.getProperty("keystorePassword") == null || icwsProps.getProperty("keystoreKeyAlias") == null)) {
                KeyStore icwsKS = null;
                try {
                    icwsKS = KeyStore.getInstance(KeyStore.getDefaultType());
                } catch (java.lang.Exception e) {
                }
                try {
                    InputStream keystoreIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(icwsProps.getProperty("keystoreFilename"));
                    char[] passwdArray = icwsProps.getProperty("keystorePassword").toCharArray();
                    icwsKS.load(keystoreIS, passwdArray);
                    String keystoreAlias = icwsProps.getProperty("keystoreKeyAlias");
                    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) icwsKS.getEntry(keystoreAlias, new KeyStore.PasswordProtection(passwdArray));
                    icwsPrivateKey = (DSAPrivateKey) pkEntry.getPrivateKey();
                } catch (java.lang.Exception e) {
                    icwsPrivateKey = null;
                }
            }
        } catch (java.io.IOException e) {
            throw new AxisFault("You must supply the properties for the server! - cwd is " + System.getProperty("user.dir"), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AxisFault("NSAException:", e);
        } catch (Exception e) {
            throw new AxisFault("The current working directory is " + System.getProperty("user.dir"), e);
        }
        if (databaseDriver == null) {
            throw new AxisFault("databaseDriver must not be null!");
        }
        try {
            Class.forName(databaseDriver).newInstance();
        } catch (java.lang.Exception e) {
            throw new AxisFault("Error setting database driver!", e);
        }
    }

    public ICWSServerImpl(String configurationFilePath) throws AxisFault {
        icwsProps = new java.util.Properties();
        try {
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            icwsProps.loadFromXML(new java.io.FileInputStream(configurationFilePath));
            if (ICWSUtils.getServiceEndpoint(icwsProps) == null) throw new AxisFault("ServiceEndpoint not defined.");
            if (ICWSUtils.getSeriesID(icwsProps, "") == null) throw new AxisFault("SeriesID path not defined!");
            protocolString = icwsProps.getProperty("protocol");
            hostName = icwsProps.getProperty("hostname");
            hostPort = icwsProps.getProperty("port");
            hostPath = icwsProps.getProperty("servicePath");
            ldapURL = icwsProps.getProperty("ldapURL");
            ldapUser = icwsProps.getProperty("ldapUser");
            ldapPass = icwsProps.getProperty("ldapPass");
            Integer keybits = new Integer(icwsProps.getProperty("keyBits"));
            withholding = new Float(icwsProps.getProperty("withholding"));
            if ((withholding.floatValue() < 0) || (withholding.floatValue() > 1)) {
                withholding = new Float(".025");
            }
            certNum = new Integer(icwsProps.getProperty("certificateNumber"));
            if ((certNum.intValue() < 0) || (certNum.intValue() > 1000)) {
                certNum = new Integer("40");
            }
            seriesInfoPath = icwsProps.getProperty("seriesInfoPath");
            numBytes = new Integer(icwsProps.getProperty("numberBytes"));
            icwsKPG = KeyPairGenerator.getInstance("DSA");
            icwsKPG.initialize(keybits.intValue());
            icwsDSA = Signature.getInstance(ICWSConstants.SIGNATURE_ALGORITHM);
            databaseDriver = icwsProps.getProperty("databaseDriver");
            databaseURL = icwsProps.getProperty("databaseURL");
            databaseUser = icwsProps.getProperty("databaseUser");
            databasePassword = icwsProps.getProperty("databasePassword");
            certificateTimespan = Long.parseLong(icwsProps.getProperty("certificateTimespan"));
            if (icwsProps.getProperty("documentSignatureKeyInclude") != null && icwsProps.getProperty("documentSignatureKeyInclude").trim().equals("false")) {
                icwsIncludeKeyInfo = false;
            } else {
                icwsIncludeKeyInfo = true;
            }
            if (!(icwsProps.getProperty("keystoreFilename") == null || icwsProps.getProperty("keystorePassword") == null || icwsProps.getProperty("keystoreKeyAlias") == null)) {
                KeyStore icwsKS = null;
                try {
                    icwsKS = KeyStore.getInstance(KeyStore.getDefaultType());
                } catch (java.lang.Exception e) {
                }
                try {
                    InputStream keystoreIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(icwsProps.getProperty("keystoreFilename"));
                    char[] passwdArray = icwsProps.getProperty("keystorePassword").toCharArray();
                    icwsKS.load(keystoreIS, passwdArray);
                    String keystoreAlias = icwsProps.getProperty("keystoreKeyAlias");
                    KeyStore.PrivateKeyEntry pkEntry = (KeyStore.PrivateKeyEntry) icwsKS.getEntry(keystoreAlias, new KeyStore.PasswordProtection(passwdArray));
                    icwsPrivateKey = (DSAPrivateKey) pkEntry.getPrivateKey();
                } catch (java.lang.Exception e) {
                    icwsPrivateKey = null;
                }
            }
        } catch (java.io.IOException e) {
            throw new AxisFault("You must supply the properties for the server! - cwd is " + System.getProperty("user.dir"), e);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AxisFault("NSAException:", e);
        } catch (Exception e) {
            throw new AxisFault("The current working directory is " + System.getProperty("user.dir"), e);
        }
        if (databaseDriver == null) {
            throw new AxisFault("databaseDriver must not be null!");
        }
        try {
            Class.forName(databaseDriver).newInstance();
        } catch (java.lang.Exception e) {
            throw new AxisFault("Error setting database driver!", e);
        }
    }

    public InformationCurrencySeries certifyInformation(final String certRequest) throws AxisFault {
        if (log.isDebugEnabled()) {
            log.debug("certifyInformation: received certificationRequest\n" + certRequest + "\n");
        }
        Connection conn = null;
        try {
            String userString = null;
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            SecureRandom sr = new SecureRandom();
            conn = getConnection();
            PreparedStatement keyIns = conn.prepareStatement("insert into keyPair values (?,?)");
            PreparedStatement srsIns = conn.prepareStatement(" insert into certificateSeries ( seriesInt, seriesUniq, type, " + " issuer, createdTime, expiresTime, numBytes, certNum, keyPair, title) values " + " ( ?,?,?,'1',now(),now() + '1 year'::interval,?,?,?,?)");
            PreparedStatement pSrsIns = conn.prepareStatement("insert into primarySeries (primarySeriesInt, seriesInt) " + " values (?, ?)");
            PreparedStatement pInfoIdIns = conn.prepareStatement("insert into informationIdentifiers " + " (infoId, seriesInt, digestValue, underlierLocator) " + " values (?, ?, ?, ?)");
            PreparedStatement certIns = conn.prepareStatement("insert into certificates (certificateID, " + "certInfo, certDigest, certSignature, seriesInt, " + " owner, reserve, valid) values (?, ?,?,?,?,?,?,'1')");
            PreparedStatement iiSel = conn.prepareStatement(" select seriesInt from informationidentifiers where " + " underlierLocator=? and digestValue=? ");
            PreparedStatement numIISel = conn.prepareStatement(" select count(*) from informationidentifiers where " + " seriesInt=? ");
            CertificationRequest cr = new CertificationRequest(certRequest);
            if (cr == null) throw new AxisFault("CertificationRequest is null");
            InformationIdentifier[] crIdent = cr.getIdentifiers();
            if ((crIdent == null) || (crIdent.length == 0) || (cr.getTitle() == null)) {
                releaseConnection(conn);
                throw new AxisFault("Certification request must have a title and identifier element(s) present.");
            }
            this.authorize(cr, this.CERTIFY_INFORMATION);
            String allowMultiple = icwsProps.getProperty("allowMultipleIssuance");
            if (allowMultiple != null && ("1".equals(allowMultiple) || "true".equals(allowMultiple))) {
            } else {
                HashSet totalResultSet = null;
                InformationIdentifier[] iiArray = cr.getIdentifiers();
                for (int i = 0; i < iiArray.length; i++) {
                    HashSet tmpSet = new HashSet();
                    String dvString = iiArray[i].getDigestValue();
                    String ulString = iiArray[i].getUnderlierLocator();
                    if (ulString == null) {
                        iiSel.setNull(1, Types.VARCHAR);
                    } else {
                        iiSel.setString(1, ulString);
                    }
                    if (dvString == null) {
                        iiSel.setNull(2, Types.VARCHAR);
                    } else {
                        iiSel.setString(2, dvString);
                    }
                    ResultSet iiRS = iiSel.executeQuery();
                    if (iiRS != null) {
                        while (iiRS.next()) {
                            int resultInt = iiRS.getInt(1);
                            if (resultInt != 0) {
                                tmpSet.add(new Integer(resultInt));
                            }
                        }
                    }
                    if (totalResultSet == null) {
                        totalResultSet = tmpSet;
                    } else {
                        totalResultSet.retainAll(tmpSet);
                    }
                }
                Iterator totalResultIt = totalResultSet.iterator();
                while (totalResultIt.hasNext()) {
                    Integer resultInt = (Integer) totalResultIt.next();
                    numIISel.setInt(1, resultInt.intValue());
                    ResultSet iiRS = numIISel.executeQuery();
                    if (iiRS != null) {
                        if (iiRS.next()) {
                            int numRows = iiRS.getInt(1);
                            if (numRows == iiArray.length) {
                                throw new AxisFault(" invalid set of information identifiers - already present in system ");
                            }
                        }
                    }
                }
            }
            int userID = -1;
            try {
                userID = this.getUserID(cr);
            } catch (java.lang.Exception e) {
            }
            String titleStr = cr.getTitle();
            KeyPair kp = icwsKPG.genKeyPair();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(kp);
            int randVal = -1;
            while (true) {
                randVal = sr.nextInt(Integer.MAX_VALUE);
                keyIns.setInt(1, randVal);
                keyIns.setBytes(2, baos.toByteArray());
                int resCode = keyIns.executeUpdate();
                if (resCode == 1) break;
            }
            icwsDSA.initSign(kp.getPrivate());
            Integer srsInt = null;
            String seriesID = null;
            Integer pSrsInt = null;
            while (true) {
                srsInt = new Integer(sr.nextInt(Integer.MAX_VALUE));
                byte[] objectBytes = new byte[128];
                sr.nextBytes(objectBytes);
                byte[] digestBytes = sha.digest(objectBytes);
                String digestStr = Utils.byteArrayToHexString(digestBytes).replaceAll(" ", "").toLowerCase();
                seriesID = ICWSUtils.getSeriesID(icwsProps, digestStr);
                srsIns.setInt(1, srsInt.intValue());
                srsIns.setString(2, seriesID);
                srsIns.setInt(3, this.PRIMARY_SERIES);
                srsIns.setInt(4, numBytes.intValue());
                srsIns.setInt(5, certNum.intValue());
                srsIns.setInt(6, randVal);
                srsIns.setString(7, titleStr);
                int updated = srsIns.executeUpdate();
                if (updated == 1) {
                    break;
                }
            }
            while (true) {
                pSrsInt = new Integer(sr.nextInt(Integer.MAX_VALUE));
                pSrsIns.setInt(1, pSrsInt.intValue());
                pSrsIns.setInt(2, srsInt.intValue());
                int updated = pSrsIns.executeUpdate();
                if (updated == 1) {
                    break;
                }
            }
            for (int r = 0; r < crIdent.length; r++) {
                while (true) {
                    pInfoIdIns.setInt(1, sr.nextInt(Integer.MAX_VALUE));
                    pInfoIdIns.setInt(2, srsInt.intValue());
                    if (crIdent[r].getDigestValue() != null) {
                        pInfoIdIns.setString(3, crIdent[r].getDigestValue());
                    } else if ("".equals(crIdent[r].getDigestValue())) {
                        pInfoIdIns.setNull(3, Types.VARCHAR);
                    } else {
                        pInfoIdIns.setNull(3, Types.VARCHAR);
                    }
                    if (crIdent[r].getUnderlierLocator() != null) {
                        pInfoIdIns.setString(4, crIdent[r].getUnderlierLocator());
                    } else if ("".equals(crIdent[r].getUnderlierLocator())) {
                        pInfoIdIns.setNull(4, Types.VARCHAR);
                    } else {
                        pInfoIdIns.setNull(4, Types.VARCHAR);
                    }
                    int updated = pInfoIdIns.executeUpdate();
                    if (updated == 1) break;
                }
            }
            InformationCurrencySeries ics = new InformationCurrencySeries(seriesID);
            Float whNum = new Float(withholding.floatValue() * certNum.floatValue());
            Integer whNumber = new Integer(whNum.intValue());
            if (whNumber.intValue() == certNum.intValue()) {
                whNumber = new Integer(certNum.intValue() - 1);
            }
            Integer distNumber = new Integer(certNum.intValue() - whNumber.intValue());
            try {
                this.authorize(cr, this.SET_PARAMS);
                if ((cr.getCertificateNumber() == -1) || (cr.getWithholdingFraction() == -1)) {
                    releaseConnection(conn);
                    throw new PermissionException("didn't have values " + "cr.getCertificateNumber() is " + cr.getCertificateNumber() + " cr.getWithholdingFraction() " + cr.getWithholdingFraction());
                }
                withholding = Float.valueOf(cr.getWithholdingFraction());
                if ((withholding.floatValue() < 0) || (withholding.floatValue() > 1)) {
                    withholding = new Float(".025");
                }
                certNum = Integer.valueOf(cr.getCertificateNumber());
                if ((certNum.intValue() < 0) || (certNum.intValue() > 1000)) {
                    certNum = new Integer("40");
                }
                whNum = new Float(withholding.floatValue() * certNum.floatValue());
                whNumber = new Integer(whNum.intValue());
                if (whNumber.intValue() == certNum.intValue()) {
                    whNumber = new Integer(certNum.intValue() - 1);
                }
                distNumber = new Integer(certNum.intValue() - whNumber.intValue());
            } catch (PermissionException e) {
            }
            for (int i = 0; i < distNumber.intValue(); i++) {
                byte[] byteArray = new byte[numBytes.intValue()];
                sr.nextBytes(byteArray);
                String certStr = Base64.encode(byteArray);
                byte[] certDigestBytes = sha.digest(byteArray);
                String certDigestStr = Base64.encode(certDigestBytes);
                icwsDSA.update(byteArray);
                byte[] certSigBytes = icwsDSA.sign();
                String certSigStr = Base64.encode(certSigBytes);
                while (true) {
                    certIns.setInt(1, sr.nextInt(Integer.MAX_VALUE));
                    certIns.setBytes(2, byteArray);
                    certIns.setBytes(3, certDigestBytes);
                    certIns.setBytes(4, certSigBytes);
                    certIns.setInt(5, srsInt.intValue());
                    certIns.setInt(6, userID);
                    certIns.setBoolean(7, false);
                    int certUpdate = certIns.executeUpdate();
                    if (certUpdate == 1) break;
                }
                ics.addICU(new InformationCurrencyUnit(seriesID, byteArray, certSigBytes));
            }
            for (int j = 0; j < whNumber.intValue(); j++) {
                byte[] byteArray = new byte[numBytes.intValue()];
                sr.nextBytes(byteArray);
                String certStr = Base64.encode(byteArray);
                byte[] certDigestBytes = sha.digest(byteArray);
                String certDigestStr = Base64.encode(certDigestBytes);
                icwsDSA.update(byteArray);
                byte[] certSigBytes = icwsDSA.sign();
                String certSigStr = Base64.encode(certSigBytes);
                while (true) {
                    certIns.setInt(1, sr.nextInt(Integer.MAX_VALUE));
                    certIns.setBytes(2, byteArray);
                    certIns.setBytes(3, certDigestBytes);
                    certIns.setBytes(4, certSigBytes);
                    certIns.setInt(5, srsInt.intValue());
                    certIns.setInt(6, 0);
                    certIns.setBoolean(7, true);
                    int certUpdate = certIns.executeUpdate();
                    if (certUpdate == 1) break;
                }
            }
            releaseConnection(conn);
            try {
                if (icwsIncludeKeyInfo) {
                    if (icwsPrivateKey != null) ics.sign((PrivateKey) icwsPrivateKey, false, icwsIncludeKeyInfo);
                } else {
                    if (icwsPrivateKey != null) ics.sign((PrivateKey) icwsPrivateKey);
                }
            } catch (java.lang.Exception e) {
            }
            return ics;
        } catch (PermissionException e) {
            releaseConnection(conn);
            throw new AxisFault("certifyInformation: PermissionException ", e);
        } catch (java.security.SignatureException e) {
            releaseConnection(conn);
            throw new AxisFault("certifyInformation: SignatureException ", e);
        } catch (java.sql.SQLException e) {
            releaseConnection(conn);
            throw new AxisFault("certifyInformation: SQL Exception ", e);
        } catch (Exception e) {
            releaseConnection(conn);
            throw new AxisFault("certifyInformation: Exception ", e);
        }
    }

    public InformationCurrencyUnit exchangeCertificate(final String inputCertificate) throws AxisFault {
        Connection conn = null;
        try {
            ObjectInputStream ois = null;
            ByteArrayInputStream bais = null;
            KeyPair kp = null;
            SecureRandom sr = new SecureRandom();
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            conn = getConnection();
            Signature dsa = Signature.getInstance(ICWSConstants.SIGNATURE_ALGORITHM);
            int userID = -1;
            Integer certificateSeriesInt = null;
            String seriesUniq = null;
            InformationCurrencyUnit icuOld = new InformationCurrencyUnit(new ByteArrayInputStream(inputCertificate.getBytes()));
            PreparedStatement seriesSel = conn.prepareStatement(" select seriesUniq, type, certificateSeries.seriesInt, " + " numBytes, createdTime, expiresTime from " + " certificates, certificateSeries where " + " certificates.certInfo=? and certificateSeries.issuer='1' and " + " certificates.seriesInt=certificateSeries.seriesInt ");
            PreparedStatement keyPairSel = conn.prepareStatement(" select keyPairBytes from certificateSeries, keyPair where " + " keyPair.keyPairID=certificateSeries.keypair and " + " certificateSeries.seriesInt=?");
            PreparedStatement invalidUpdate = conn.prepareStatement(" update certificates set valid='f' where certInfo=? ");
            PreparedStatement newCertIns = conn.prepareStatement(" insert into certificates (certificateID, certInfo, certDigest, " + " certSignature, seriesInt, owner, reserve, valid) values " + " (?, ?, ?, ?, ?, ?, 'f', 't')");
            PreparedStatement certSel = conn.prepareStatement(" select certsignature from certificates where certdigest=? and valid='t'");
            seriesSel.setBytes(1, icuOld.getCertBytes());
            ResultSet rs = seriesSel.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    seriesUniq = rs.getString("seriesUniq");
                    certificateSeriesInt = new Integer(rs.getInt(3));
                    Timestamp expiresTime = rs.getTimestamp("expiresTime");
                    Date nowTime = new Date();
                    Timestamp nowTS = new Timestamp(nowTime.getTime());
                    if (expiresTime.compareTo(nowTS) < 0) {
                        releaseConnection(conn);
                        return icuOld;
                    }
                    boolean certificateAbsent = true;
                    certSel.setBytes(1, icuOld.getDigestBytes());
                    ResultSet certSelRS = certSel.executeQuery();
                    if (certSelRS != null) {
                        if (certSelRS.next()) {
                            certificateAbsent = false;
                        }
                    }
                    if (certificateAbsent) {
                        releaseConnection(conn);
                        return icuOld;
                    }
                    keyPairSel.setInt(1, certificateSeriesInt.intValue());
                    ResultSet rskp = keyPairSel.executeQuery();
                    if ((rskp != null) && rskp.next()) {
                        byte[] kpBytes = rskp.getBytes("keyPairBytes");
                        bais = new ByteArrayInputStream(kpBytes);
                        ois = new ObjectInputStream(bais);
                        kp = (KeyPair) ois.readObject();
                    } else {
                        releaseConnection(conn);
                        return icuOld;
                    }
                    try {
                        userID = this.getUserID(icuOld);
                    } catch (java.lang.Exception e) {
                    }
                    byte[] certBytes = new byte[numBytes.intValue()];
                    sr.nextBytes(certBytes);
                    String newCertStr = Base64.encode(certBytes);
                    byte[] certDigest = sha.digest(certBytes);
                    String certDigestStr = Base64.encode(certDigest);
                    dsa.initSign(kp.getPrivate());
                    dsa.update(certBytes);
                    byte[] certSig = dsa.sign();
                    String certSigStr = Base64.encode(certSig);
                    invalidUpdate.setBytes(1, icuOld.getCertBytes());
                    int cInvalid = invalidUpdate.executeUpdate();
                    while (true) {
                        newCertIns.setInt(1, sr.nextInt(Integer.MAX_VALUE));
                        newCertIns.setBytes(2, certBytes);
                        newCertIns.setBytes(3, certDigest);
                        newCertIns.setBytes(4, certSig);
                        newCertIns.setInt(5, certificateSeriesInt.intValue());
                        newCertIns.setInt(6, userID);
                        int certUpdate = newCertIns.executeUpdate();
                        if (certUpdate == 1) break;
                    }
                    InformationCurrencyUnit icu = new InformationCurrencyUnit(seriesUniq, certBytes, certSig);
                    try {
                        if (icwsIncludeKeyInfo) {
                            if (icwsPrivateKey != null) icu.sign((PrivateKey) icwsPrivateKey, false, icwsIncludeKeyInfo);
                        } else {
                            if (icwsPrivateKey != null) icu.sign((PrivateKey) icwsPrivateKey);
                        }
                    } catch (java.lang.Exception e) {
                    }
                    releaseConnection(conn);
                    return icu;
                }
            }
            releaseConnection(conn);
            return icuOld;
        } catch (java.security.NoSuchAlgorithmException e) {
            releaseConnection(conn);
            throw new AxisFault("exchangeCertificate: NSA Exception ", e);
        } catch (java.sql.SQLException e) {
            releaseConnection(conn);
            throw new AxisFault("exchangeCertificate: SQL Exception ", e);
        } catch (Exception e) {
            releaseConnection(conn);
            throw new AxisFault("exchangeCertificate: Exception ", e);
        }
    }

    public java.lang.String verifyCertificate(final String verifyCertificate) throws AxisFault {
        Connection conn = null;
        try {
            VerificationCertificate inVC = new VerificationCertificate(new ByteArrayInputStream(verifyCertificate.getBytes()));
            VerificationCertificate returnVC = new VerificationCertificate();
            returnVC.setSeriesID(inVC.getSeriesID());
            returnVC.setDigestValue(inVC.getDigestValue());
            conn = getConnection();
            PreparedStatement srsSel = conn.prepareStatement("select seriesInt, expiresTime, keyPairBytes from " + " certificateSeries, keypair where seriesUniq=? and " + " certificateSeries.keypair=keypair.keypairid");
            PreparedStatement certSel = conn.prepareStatement(" select certSignature from certificates where certDigest=? " + " and seriesInt=? and valid='t' ");
            Integer certificateSeriesInt = null;
            java.sql.Timestamp expiresTS = null;
            KeyPair kp = null;
            srsSel.setString(1, inVC.getSeriesID());
            ResultSet rsa = srsSel.executeQuery();
            if ((rsa != null)) {
                if (rsa.next()) {
                    certificateSeriesInt = new Integer(rsa.getInt("seriesInt"));
                    expiresTS = rsa.getTimestamp("expiresTime");
                    byte[] kpBytes = rsa.getBytes("keyPairBytes");
                    if (kpBytes != null) {
                        try {
                            kp = (KeyPair) new ObjectInputStream(new ByteArrayInputStream(kpBytes)).readObject();
                        } catch (java.lang.Exception e) {
                        }
                    }
                }
            }
            if (certificateSeriesInt == null || expiresTS == null) {
                releaseConnection(conn);
                returnVC.setRandomValue(randomVerificationString(null, null, false));
                if (kp != null) {
                    try {
                        returnVC.sign(kp.getPrivate());
                    } catch (Exception e) {
                    }
                }
                return returnVC.toString();
            }
            long currentTimeVal = System.currentTimeMillis();
            if (expiresTS.before(new Timestamp(currentTimeVal))) {
                releaseConnection(conn);
                returnVC.setRandomValue(randomVerificationString(null, null, false));
                if (kp != null) {
                    try {
                        returnVC.sign(kp.getPrivate());
                    } catch (Exception e) {
                    }
                }
                return returnVC.toString();
            }
            certSel.setBytes(1, inVC.getDigestBytes());
            certSel.setInt(2, certificateSeriesInt.intValue());
            ResultSet rs = certSel.executeQuery();
            if (rs != null) {
                if (rs.next()) {
                    byte[] sigBytes = rs.getBytes(1);
                    releaseConnection(conn);
                    returnVC.setRandomValue(randomVerificationString(sigBytes, inVC.getRandomBytes(), true));
                    if (kp != null) {
                        try {
                            returnVC.sign(kp.getPrivate());
                        } catch (Exception e) {
                        }
                    }
                    return returnVC.toString();
                } else {
                    releaseConnection(conn);
                    returnVC.setRandomValue(randomVerificationString(null, null, false));
                    if (kp != null) {
                        try {
                            returnVC.sign(kp.getPrivate());
                        } catch (Exception e) {
                        }
                    }
                    return returnVC.toString();
                }
            }
            releaseConnection(conn);
            returnVC.setRandomValue(randomVerificationString(null, null, false));
            if (kp != null) {
                try {
                    returnVC.sign(kp.getPrivate());
                } catch (Exception e) {
                }
            }
            return returnVC.toString();
        } catch (java.sql.SQLException e) {
            releaseConnection(conn);
            throw new AxisFault("verifyCertificate: SQL Exception ", e);
        } catch (Exception e) {
            releaseConnection(conn);
            throw new AxisFault("verifyCertificate: Exception ", e);
        }
    }

    private String randomVerificationString(byte[] sigBytes, byte[] rndBytes, boolean validString) throws Exception {
        try {
            if (validString) {
                if (sigBytes == null || rndBytes == null) throw new Exception(" sigBytes or rndBytes is null. ");
                byte[] returnBytes = new byte[sigBytes.length + rndBytes.length];
                System.arraycopy(sigBytes, 0, returnBytes, 0, sigBytes.length);
                System.arraycopy(rndBytes, 0, returnBytes, sigBytes.length, rndBytes.length);
                return Base64.encode(MessageDigest.getInstance("SHA-1").digest(returnBytes));
            }
        } catch (java.lang.Exception e) {
        }
        byte[] retArray = new byte[20];
        new java.security.SecureRandom().nextBytes(retArray);
        return Base64.encode(retArray);
    }

    public SeriesInfo getSeriesInfo(final String seriesID) throws AxisFault {
        Connection c = null;
        Connection cTwo = null;
        Connection cThree = null;
        Connection cFour = null;
        Connection cFive = null;
        Connection cSix = null;
        try {
            c = getConnection();
            Statement st = c.createStatement();
            if (seriesID == null) {
                return null;
            }
            PreparedStatement seriesSel = c.prepareStatement(" select seriesInt, seriesUniq, createdTime, " + " expiresTime, numbytes, certnum, type, issuer, keypair " + " from certificateSeries where seriesUniq like ?");
            seriesSel.setString(1, "%" + seriesID + "");
            ResultSet rs = seriesSel.executeQuery();
            if (rs != null && rs.next()) {
                cTwo = getConnection();
                int keypairid = rs.getInt("keypair");
                KeyPair kp = null;
                PreparedStatement keyFindSel = cTwo.prepareStatement("select keyPairBytes from keyPair where keyPair.keyPairID=?");
                keyFindSel.setInt(1, keypairid);
                ResultSet rsTwo = keyFindSel.executeQuery();
                if (rsTwo != null) {
                    if (rsTwo.next()) {
                        byte[] kpBytes = rsTwo.getBytes("keyPairBytes");
                        ByteArrayInputStream bais = new ByteArrayInputStream(kpBytes);
                        ObjectInputStream ois = new ObjectInputStream(bais);
                        kp = (KeyPair) ois.readObject();
                    }
                }
                if (rs.getInt("type") == this.PRIMARY_SERIES) {
                    cThree = getConnection();
                    PrimarySeriesInfo psi = new PrimarySeriesInfo();
                    psi.setServiceEndpoint(ICWSUtils.getServiceEndpoint(icwsProps));
                    psi.setSeriesID(rs.getString("seriesUniq"));
                    psi.setCreatedTime(rs.getTimestamp("createdTime"));
                    psi.setExpiresTime(rs.getTimestamp("expiresTime"));
                    psi.setCertificateNumber(rs.getInt("certNum"));
                    psi.setCertificateBytes(rs.getInt("numBytes"));
                    if (kp != null) {
                        psi.setSeriesPublicKey(kp.getPublic());
                    }
                    int seriesInteger = rs.getInt("seriesInt");
                    PreparedStatement priSrsSel = cThree.prepareStatement("select title, digestValue, underlierLocator from " + " certificateSeries, primarySeries, informationIdentifiers where " + " certificateSeries.seriesInt=informationIdentifiers.seriesInt " + " and certificateSeries.seriesInt=? and " + " primarySeries.seriesInt=certificateSeries.seriesInt");
                    priSrsSel.setInt(1, seriesInteger);
                    ResultSet rsThree = priSrsSel.executeQuery();
                    if ((rsThree != null)) {
                        while (rsThree.next()) {
                            psi.setTitle(rsThree.getString("title"));
                            psi.addInformationIdentifier(new InformationIdentifier(rsThree.getString("underlierLocator"), rsThree.getString("digestValue")));
                        }
                    }
                    releaseConnection(c);
                    releaseConnection(cTwo);
                    releaseConnection(cThree);
                    try {
                        if (icwsIncludeKeyInfo) {
                            if (icwsPrivateKey != null) psi.sign(icwsPrivateKey, false, icwsIncludeKeyInfo);
                        } else {
                            if (icwsPrivateKey != null) psi.sign(icwsPrivateKey);
                        }
                    } catch (java.lang.Exception e) {
                    }
                    return psi;
                }
                releaseConnection(c);
                releaseConnection(cTwo);
                releaseConnection(cThree);
                releaseConnection(cFour);
                releaseConnection(cFive);
                releaseConnection(cSix);
                return null;
            }
            releaseConnection(c);
        } catch (java.io.IOException e) {
            releaseConnection(c);
            releaseConnection(cTwo);
            releaseConnection(cThree);
            releaseConnection(cFour);
            releaseConnection(cFive);
            releaseConnection(cSix);
            throw new AxisFault("getSeriesInfo: PermissionException ", e);
        } catch (java.lang.ClassNotFoundException e) {
            releaseConnection(c);
            releaseConnection(cTwo);
            releaseConnection(cThree);
            releaseConnection(cFour);
            releaseConnection(cFive);
            releaseConnection(cSix);
            throw new AxisFault("getSeriesInfo: ClassNotFoundException ", e);
        } catch (java.sql.SQLException e) {
            releaseConnection(c);
            releaseConnection(cTwo);
            releaseConnection(cThree);
            releaseConnection(cFour);
            releaseConnection(cFive);
            releaseConnection(cSix);
            throw new AxisFault("getSeriesInfo: SQL Exception ", e);
        } catch (Exception e) {
            releaseConnection(c);
            releaseConnection(cTwo);
            releaseConnection(cThree);
            releaseConnection(cFour);
            releaseConnection(cFive);
            releaseConnection(cSix);
            throw new AxisFault("getSeriesInfo: Exception ", e);
        }
        return null;
    }

    private int getUserID(ICWSDocument iDoc) throws PermissionException {
        try {
            DSAPublicKey pubKey = (DSAPublicKey) iDoc.getXMLSignature().getKeyInfo().getPublicKey();
            if (!iDoc.verifySignature(pubKey)) return -1;
            int userId = getUserID(pubKey);
            return userId;
        } catch (java.lang.Exception e) {
            return -1;
        }
    }

    private int getUserID(DSAPublicKey pubKey) throws PermissionException {
        Connection conn = null;
        try {
            conn = getConnection();
            int authKeyId = -1;
            PreparedStatement keySelect = conn.prepareStatement("select keyId from authorities where keyType=? and keyString=?");
            boolean keyAbsent = true;
            String pubKeyStr = null;
            if (pubKey == null) {
                releaseConnection(conn);
                throw new PermissionException("PublicKey is null.  Authentication not possible.");
            }
            pubKeyStr = pubKey.getY().toString(16);
            keySelect.setString(1, "user");
            keySelect.setString(2, pubKeyStr);
            ResultSet authRS = keySelect.executeQuery();
            if (authRS != null) {
                if (authRS.next()) {
                    keyAbsent = false;
                    authKeyId = authRS.getInt(1);
                    releaseConnection(conn);
                    return authKeyId;
                }
            }
            if (keyAbsent || authKeyId == -1) {
                releaseConnection(conn);
                throw new PermissionException("getUserID - authCert key not found in db!");
            }
            releaseConnection(conn);
            return authKeyId;
        } catch (java.lang.Exception e) {
            releaseConnection(conn);
            throw new PermissionException("getUserID - Exception: ", e);
        }
    }

    public void authorize(final CertificationRequest crObj, final int operation) throws PermissionException {
        PreparedStatement keyMessage = null;
        Connection conn = null;
        try {
            int authKeyId = -1;
            boolean keyAbsent = true;
            conn = getConnection();
            PreparedStatement authKeyFind = conn.prepareStatement(" select keyId from authorities where keyString=? and keyType=? ");
            PreparedStatement nonceFind = conn.prepareStatement(" select * from certificateNonce where nonce=? and expiresTime=?");
            PreparedStatement nonceIns = conn.prepareStatement(" insert into certificateNonce values (?, ?, ?) ");
            BigInteger pubKeyId = crObj.getSignaturePublicKeyId();
            String pubKeyStr = null;
            if (pubKeyId == null) {
                releaseConnection(conn);
                throw new PermissionException("PublicKey is null.  Authentication not possible.");
            }
            try {
                if (!crObj.verifySignature()) {
                    releaseConnection(conn);
                    throw new PermissionException("CertificationRequest public key did not verify!");
                }
                long timeVal = System.currentTimeMillis();
                if (!crObj.valid(timeVal)) {
                    releaseConnection(conn);
                    throw new PermissionException("CertificationRequest (notBefore: " + crObj.getNotBefore().getTime() + "notAfter: " + crObj.getNotAfter().getTime() + ") not valid at this time  !");
                }
                pubKeyStr = pubKeyId.toString(16);
            } catch (java.lang.Exception e) {
                releaseConnection(conn);
                throw new PermissionException(e);
            }
            authKeyFind.setString(1, pubKeyStr);
            if ((operation == this.CERTIFY_INFORMATION) || (operation == this.SECONDARY_ISSUANCE)) {
                authKeyFind.setString(2, "issuance");
            } else if ((operation == this.STORE_IC) || (operation == this.ACCOUNT_BALANCE) || (operation == this.EXCHANGE_CERTIFICATE) || (operation == this.RETRIEVE_IC)) {
                authKeyFind.setString(2, "user");
            } else if ((operation == this.SET_PARAMS)) {
                authKeyFind.setString(2, "parameters");
            }
            ResultSet authRS = authKeyFind.executeQuery();
            if (authRS != null) {
                if (authRS.next()) {
                    keyAbsent = false;
                    authKeyId = authRS.getInt(1);
                }
            }
            if (keyAbsent) {
                releaseConnection(conn);
                throw new PermissionException("authorize - authCert key not found in db!");
            }
            Timestamp nonceTS = new Timestamp(crObj.getNotAfter().getTime());
            String commentNonce = crObj.getRequestId();
            if (commentNonce == null || "".equals(commentNonce.trim())) {
                releaseConnection(conn);
                throw new PermissionException("authorize - nonce must be set already used!");
            }
            nonceFind.setString(1, commentNonce);
            nonceFind.setTimestamp(2, nonceTS);
            ResultSet nonceFindRS = nonceFind.executeQuery();
            if (nonceFindRS != null) {
                if (nonceFindRS.next()) {
                    releaseConnection(conn);
                    throw new PermissionException("authorize - Replay prevention - nonce already used!");
                }
            }
            nonceIns.setString(1, commentNonce);
            nonceIns.setInt(2, authKeyId);
            nonceIns.setTimestamp(3, nonceTS);
            int nonceInsertValue = nonceIns.executeUpdate();
            releaseConnection(conn);
        } catch (java.io.IOException e) {
            releaseConnection(conn);
            throw new PermissionException("authorize - IOException: ", e);
        } catch (java.sql.SQLException e) {
            releaseConnection(conn);
            throw new PermissionException("authorize - SQLException ", e);
        } catch (java.lang.Exception e) {
            releaseConnection(conn);
            throw new PermissionException("authorize - Exception: ", e);
        }
    }

    public String findUnderlierLocator(final String underlierRequest) throws AxisFault {
        Connection conn = null;
        try {
            conn = getConnection();
            PreparedStatement findSeriesPS = conn.prepareStatement(" select distinct seriesUniq from certificateSeries, informationIdentifiers " + " where ( underlierLocator ilike ? or certificateSeries.title ilike ? ) " + " and certificateSeries.seriesInt=informationIdentifiers.seriesInt ");
            findSeriesPS.setString(1, "%" + underlierRequest + "%");
            findSeriesPS.setString(2, "%" + underlierRequest + "%");
            String resultSet = "<SeriesListing>";
            ResultSet findSeriesRS = findSeriesPS.executeQuery();
            if (findSeriesRS != null) {
                while (findSeriesRS.next()) {
                    String seriesID = findSeriesRS.getString(1);
                    resultSet += "<seriesID>" + seriesID + "</seriesID>";
                }
            }
            resultSet += "</SeriesListing>";
            releaseConnection(conn);
            return resultSet;
        } catch (java.lang.Exception e) {
            releaseConnection(conn);
            throw new AxisFault("findUnderlierLocator");
        }
    }

    public String getSeriesListing(String seriesListingDoc) throws AxisFault {
        Connection conn = null;
        PreparedStatement preparedStatement = null;
        try {
            conn = getConnection();
            PreparedStatement ulPS = conn.prepareStatement("select distinct seriesuniq from certificateseries, informationidentifiers " + " where underlierLocator=? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
            PreparedStatement dvPS = conn.prepareStatement("select distinct seriesuniq from certificateseries, informationidentifiers " + " where digestValue=? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
            PreparedStatement uldvPS = conn.prepareStatement(" select distinct seriesuniq from certificateseries, informationidentifiers " + " where digestValue=? and underlierLocator=? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
            SeriesListing sl = new SeriesListing(seriesListingDoc);
            SeriesListing seriesListingReturn = new SeriesListing();
            if (sl.getSearchUnderlierLocator() != null && sl.getSearchDigestValue() != null) {
                preparedStatement = conn.prepareStatement(" select distinct seriesuniq from certificateseries, informationidentifiers " + " where digestValue=? and underlierLocator ilike ? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
                preparedStatement.setString(1, sl.getSearchDigestValue());
                preparedStatement.setString(2, "%" + sl.getSearchUnderlierLocator() + "%");
            } else if (sl.getSearchUnderlierLocator() != null && sl.getSearchDigestValue() == null) {
                preparedStatement = conn.prepareStatement("select distinct seriesuniq from certificateseries, informationidentifiers " + " where underlierLocator ilike ? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
                preparedStatement.setString(1, "%" + sl.getSearchUnderlierLocator() + "%");
            } else if (sl.getSearchUnderlierLocator() == null && sl.getSearchDigestValue() != null) {
                preparedStatement = conn.prepareStatement("select distinct seriesuniq from certificateseries, informationidentifiers " + " where digestValue=? and " + " certificateseries.seriesint=informationidentifiers.seriesint;");
                preparedStatement.setString(1, sl.getSearchDigestValue());
            } else {
                releaseConnection(conn);
                throw new AxisFault("No search terms.");
            }
            ResultSet rs = preparedStatement.executeQuery();
            if (rs != null) {
                while (rs.next()) {
                    String sidStr = new String(rs.getString(1)).trim();
                    seriesListingReturn.addSeriesListingResult(sidStr);
                }
            }
            releaseConnection(conn);
            try {
                if (icwsIncludeKeyInfo) {
                    if (icwsPrivateKey != null) seriesListingReturn.sign((PrivateKey) icwsPrivateKey, false, icwsIncludeKeyInfo);
                } else {
                    if (icwsPrivateKey != null) seriesListingReturn.sign((PrivateKey) icwsPrivateKey);
                }
            } catch (java.lang.Exception e) {
            }
            return seriesListingReturn.toString();
        } catch (Exception e) {
            releaseConnection(conn);
            throw new AxisFault("series listing exception: ", e);
        }
    }

    private Connection getConnection() throws SQLException {
        if (databaseURL == null || databaseUser == null || databasePassword == null) {
            throw new SQLException(" null values not permitted! " + " databaseURL: =-=" + databaseURL + "=-= databaseUser: =-=" + databaseUser + "=-= databasePassword: =-=" + databasePassword);
        }
        return DriverManager.getConnection(databaseURL, databaseUser, databasePassword);
    }

    private void releaseConnection(Connection c) {
        if (c == null) return;
        try {
            c.close();
        } catch (java.lang.Exception e) {
        }
    }
}
