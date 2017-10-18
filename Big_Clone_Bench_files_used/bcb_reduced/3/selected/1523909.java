package org.infoeng.icws.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.openssl.PEMReader;
import org.infoeng.icws.documents.ICWSDocument;
import org.infoeng.icws.documents.InformationCurrencySeries;
import org.infoeng.icws.documents.IssuanceRequest;
import org.infoeng.icws.util.Base64;
import org.infoeng.icws.util.ICWSConstants;
import org.infoeng.icws.util.IssuanceUtils;
import org.infoeng.icws.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.apache.axis.components.logger.LogFactory;
import org.apache.commons.logging.Log;

public class URLIssuanceClient {

    private static Log log = LogFactory.getLog("URLIssuanceClient.class");

    /** @param digestAlgorithms the algorithm name used to generate the digest 
     *                          and the rdf element for the digest value.
     **/
    private static String[] digestValueStrings = { "SHA-1", "in:SHA1DigestValue" };

    private static Properties issuanceProps;

    private static String usageStr = "\n" + "Usage: org.infoeng.icws.clients.URLIssuanceClient PARAMETER-FILE URL \n" + "\n" + "   PARAMETER-FILE is the XML parameters file specifying the issuance parameters.\n" + "   URL is the unform resource locator to be accessed and digested to generate \n" + "   information currency.\n" + "\n" + "   For issuance, the PARAMETER-FILE must be a XML Java parameter file \n" + "  (see http://java.sun.com/dtd/properties.dtd), including the properties:\n" + "\n" + "   \"digest_algorithm\": the digest algorithm used to create the digest value of the resource.\n" + "\n" + "   \"certification_user\": the username provided to the issuer. \n" + "\n" + "   \"certification_endpoint\": the SOAP endpoint address of the issuer.\n" + "\n" + "   \"certification_keyfile\": the file containing the key for authenticating the issuance request - this \n" + "                            must be a PEM-encoded private key for this version of the program.\n" + "\n" + "   \"ics_output_directory\": the directory for output of the received information currency.\n" + "\n" + "   \"trustStore\": if the connection is over a SOAP endpoint, this is the Java \n" + "                 keystore file with the endpoint's SSL certificate.\n" + "\n" + "   \"trustStorePassword\": if the connection is over a SOAP endpoint, \n" + "                         this is the password to access the Java keystore specified in trustStore.\n\n";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.print(usageStr);
            System.exit(1);
        }
        issuanceProps = new Properties();
        Utils.loadPropertiesFromXML(issuanceProps, new FileInputStream(args[0]));
        URL issuanceURL = new URL(args[1]);
        PrivateKey privKey = null;
        if (System.getProperty("icws.privatekey.generate") != null) {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.genKeyPair();
            privKey = kp.getPrivate();
        } else {
            privKey = (PrivateKey) getPrivateKey(issuanceProps);
        }
        InformationCurrencySeries retICS = generateIC(issuanceURL, privKey, issuanceProps);
        try {
            if (System.getProperty("icws.output.dir") != null) {
                issuanceProps.setProperty("ics_output_directory", System.getProperty("icws.output.dir"));
            }
            File outputFile = File.createTempFile("ics-", ".xml", new File(issuanceProps.getProperty("ics_output_directory")));
            if (outputFile == null) {
                log.error("Error writing information currency file.  Received information currency series is:");
                log.error(retICS.toString());
                System.exit(1);
            }
            FileWriter fw = new FileWriter(outputFile);
            fw.write(retICS.toString());
            fw.flush();
            fw.close();
            System.out.println(" Wrote information currency series to " + outputFile.toString());
            System.exit(0);
        } catch (java.lang.Exception e) {
            log.debug(" Error writing information currency file.  Received information currency series is:");
            log.debug(retICS.toString());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static InformationCurrencySeries generateIC(URL certURL, PrivateKey privKey, Properties issuanceProps) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat(ICWSConstants.DATE_FORMAT_SPECIFICATION);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        int loglevel = -1;
        try {
            int tmpInt = new Integer(issuanceProps.getProperty("client_loglevel")).intValue();
            if (tmpInt > 0 || tmpInt < 6) loglevel = tmpInt;
        } catch (java.lang.Exception e) {
        }
        File tmpFile = File.createTempFile("issuance", ".url");
        log.debug(" writing url " + certURL.toString() + " to file " + tmpFile.toURL().toString());
        FileOutputStream fos = new FileOutputStream(tmpFile);
        URLConnection certConnection = certURL.openConnection();
        InputStream certIS = certConnection.getInputStream();
        while (true) {
            int certInt = certIS.read();
            if (certInt == -1) break;
            fos.write(certInt);
        }
        FileInputStream fileFIS = new FileInputStream(tmpFile);
        MessageDigest md = MessageDigest.getInstance(digestValueStrings[0]);
        DigestInputStream dis = new DigestInputStream(fileFIS, md);
        fos.close();
        fos = new FileOutputStream("/dev/null");
        Utils.bufferedCopy(dis, fos);
        String digestValue = Base64.encode(dis.getMessageDigest().digest());
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Document doc = dbf.newDocumentBuilder().newDocument();
        doc.appendChild(doc.createElementNS(ICWSConstants.RDF_NAMESPACE, "rdf:RDF"));
        doc.getDocumentElement().setAttributeNS(ICWSConstants.XMLNS_NAMESPACE, "xmlns:in", ICWSConstants.IN_NAMESPACE);
        doc.getDocumentElement().setAttributeNS(ICWSConstants.XMLNS_NAMESPACE, "xmlns:dc", ICWSConstants.DC_NAMESPACE);
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        Element descElem = (Element) doc.getDocumentElement().appendChild(doc.createElementNS(ICWSConstants.RDF_NAMESPACE, "rdf:Description"));
        descElem.appendChild(doc.createTextNode("\n"));
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        descElem.setAttributeNS(ICWSConstants.RDF_NAMESPACE, "rdf:about", certURL.toString());
        Element idElem = (Element) descElem.appendChild(doc.createElementNS(ICWSConstants.DC_NAMESPACE, "dc:identifier"));
        descElem.appendChild(doc.createTextNode("\n"));
        idElem.setTextContent(certURL.toString());
        Element dvElem = (Element) descElem.appendChild(doc.createElementNS(ICWSConstants.IN_NAMESPACE, digestValueStrings[1]));
        dvElem.setTextContent(digestValue);
        descElem.appendChild(doc.createTextNode("\n"));
        Element dateElem = (Element) descElem.appendChild(doc.createElementNS(ICWSConstants.DC_NAMESPACE, "dc:date"));
        String nowDate = sdf.format(new Date()).trim();
        dateElem.setTextContent(nowDate);
        descElem.appendChild(doc.createTextNode("\n"));
        tmpFile.delete();
        IssuanceRequest certReq = new IssuanceRequest();
        signUnderlierDocument(doc, privKey);
        certReq.setUnderlyingInformation(doc.getDocumentElement());
        try {
            certReq.setCertificateNumber(Integer.parseInt(issuanceProps.getProperty("seriesCertificateNumber")));
        } catch (Exception e) {
        }
        try {
            certReq.setSeriesTimespan(Long.parseLong(issuanceProps.getProperty("seriesTimespan")));
        } catch (Exception e) {
        }
        certReq.setNotAfter(new Date(System.currentTimeMillis() + 50000));
        certReq.setNotBefore(new Date());
        certReq.sign(privKey);
        boolean verified = certReq.verifySignature();
        if (verified) {
            System.out.println("certReq verified.");
        } else {
            System.out.println("certReq did not verify.");
        }
        if (System.getProperty("icws.ssl.trustStore") != null) {
            issuanceProps.setProperty("trustStore", System.getProperty("icws.ssl.trustStore"));
        }
        if (System.getProperty("icws.ssl.endpoint") != null) {
            issuanceProps.setProperty("certification_endpoint", System.getProperty("icws.ssl.endpoint"));
        }
        if (loglevel > 3) {
            File crFile = File.createTempFile("issuance-request-", ".xml", new File(issuanceProps.getProperty("ics_output_directory")));
            certReq.toString(new FileOutputStream(crFile));
            log.debug("Wrote issuance request to " + crFile.getCanonicalPath() + ".");
        }
        String certResponse = IssuanceUtils.processIssuanceRequest(issuanceProps, certReq.toString());
        if (certResponse == null) {
            log.debug("No information currency series was received.");
        } else if (certResponse != null) {
            InformationCurrencySeries ics = new InformationCurrencySeries(new ByteArrayInputStream(Utils.convertEntities(certResponse).getBytes()));
            return ics;
        }
        return null;
    }

    private static void signUnderlierDocument(Document doc, PrivateKey privKey) throws Exception {
        org.apache.xml.security.Init.init();
        String SIGNATURE_BASE_URI = "";
        DSAPrivateKey dsaPrivKey = null;
        RSAPrivateKey rsaPrivKey = null;
        String keyType = null;
        if (privKey instanceof DSAPrivateKey) {
            dsaPrivKey = (DSAPrivateKey) privKey;
            keyType = "DSA";
        } else if (privKey instanceof RSAPrivateKey) {
            rsaPrivKey = (RSAPrivateKey) privKey;
            keyType = "RSA";
        } else {
            throw new Exception("we don't handle this key: " + privKey.toString());
        }
        XPath xpath = XPathFactory.newInstance().newXPath();
        Element sigElement = (Element) xpath.evaluate("/*/*[local-name()='Signature'][namespace-uri()='" + Constants.SignatureSpecNS + "']", doc, XPathConstants.NODE);
        if (sigElement != null) {
            sigElement.getParentNode().removeChild(sigElement);
        }
        XMLSignature sig = null;
        if (keyType.equals("DSA")) {
            sig = new XMLSignature(doc, SIGNATURE_BASE_URI, XMLSignature.ALGO_ID_SIGNATURE_DSA);
        } else if (keyType.equals("RSA")) {
            sig = new XMLSignature(doc, SIGNATURE_BASE_URI, XMLSignature.ALGO_ID_SIGNATURE_RSA);
        }
        Transforms transforms = new Transforms(doc);
        transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
        transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
        sig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
        if (keyType.equals("DSA")) {
            DSAPublicKey pubKey = ICWSDocument.generateDSAPublicKey(dsaPrivKey);
            sig.addKeyInfo(pubKey);
        } else if (keyType.equals("RSA")) {
            RSAPublicKey pubKey = ICWSDocument.generateRSAPublicKey(rsaPrivKey);
            if (pubKey != null) {
                sig.addKeyInfo(pubKey);
            }
        }
        byte[] idBytes = new byte[20];
        new java.util.Random().nextBytes(idBytes);
        String idStr = new java.math.BigInteger(idBytes).abs().toString(16);
        sig.setId(idStr);
        doc.getDocumentElement().appendChild(sig.getElement());
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        if (keyType.equals("DSA")) {
            sig.sign(dsaPrivKey);
        } else if (keyType.equals("RSA")) {
            sig.sign(rsaPrivKey);
        }
    }

    private static PrivateKey getPrivateKey(Properties icwsProps) throws Exception {
        try {
            String certKeyFile = icwsProps.getProperty("certification_keyfile");
            if (certKeyFile == null) throw new Exception("certification_keyfile must be defined!");
            System.out.println("Reading key from " + certKeyFile + ".");
            java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            PEMReader pr = new PEMReader(new BufferedReader(new FileReader(new File(certKeyFile))), new Utils());
            KeyPair kp = (KeyPair) pr.readObject();
            return kp.getPrivate();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }
}
