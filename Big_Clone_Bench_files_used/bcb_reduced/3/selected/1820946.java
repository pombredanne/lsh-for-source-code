package gov.lanl.xmlsig;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.apache.xml.security.algorithms.SignatureAlgorithm;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.keys.content.RetrievalMethod;
import org.apache.xml.security.signature.Reference;
import org.apache.xml.security.signature.SignedInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.XPath2FilterContainer;
import org.apache.xml.security.transforms.params.XPathContainer;
import org.apache.xml.security.utils.Base64;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.SignerOutputStream;
import org.apache.xml.security.utils.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

/**
 * CreateXMLSig.java<br>
 * <br>
 * Uses the Apache XML Security API to sign xml docs and inline xml using 
 * Java KeyStores.
 * 
 * @author Lyudmila Balakireva
 * @version 1.0 
 */
public class CreateXMLSig {

    static {
        org.apache.xml.security.Init.init();
    }

    String DIDL_NS = "urn:mpeg:mpeg21:2002:02-DIDL-NS";

    DocumentBuilder db;

    PrivateKey privk;

    PublicKey pubkey;

    XMLSignature signature;

    Document doc;

    String certificateURL;

    static Logger log = Logger.getLogger(CreateXMLSig.class.getName());

    /**
     * Constructor CreateXMLSig
     * @param keystoreType - the type of keystore; i.e. "JKS"
     * @param keystoreFile - absolute path to keystore containing certificate
     * @param keystorePass - the password used to unlock the keystore
     * @param privateKeyAlias - the private key alias name
     * @param privateKeyPass - the password for recovering the key
     * @param certificateURL - URL location of the exported certificate
     * @throws Exception
     */
    public CreateXMLSig(String keystoreType, String keystoreFile, String keystorePass, String privateKeyAlias, String privateKeyPass, String certificateURL) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        db = dbf.newDocumentBuilder();
        this.certificateURL = certificateURL;
        KeyStore keyStore = KeyStore.getInstance(keystoreType);
        FileInputStream fileInputStream = new FileInputStream(keystoreFile);
        keyStore.load(fileInputStream, keystorePass.toCharArray());
        privk = (PrivateKey) keyStore.getKey(privateKeyAlias, privateKeyPass.toCharArray());
        Constants.setSignatureSpecNSprefix("dsig");
    }

    /**
     * Provided a didl xml string, generates document, returns XMLSignature XML fragment
     * Used for DIDL Level Signatures
     * @param didl - Inline string version of didl document
     * @return signature for document
     * @throws Exception
     */
    public String signDidl(String didl) throws Exception {
        InputSource IS = new InputSource(new StringReader(didl));
        org.w3c.dom.Document doc = db.parse(IS);
        String sig = this.signDidl(doc);
        log.debug("from xmlsig:" + sig);
        return sig;
    }

    /**
     * Provided a DOM Document, returns XMLSignature XML fragment
     * @param doc - DOM Document Object
     * @return signature for document
     * @throws Exception
     */
    public String signDidl(Document doc) throws Exception {
        XMLSignature xmlSig = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, "http://www.w3.org/2001/10/xml-exc-c14n#");
        Element didl = (Element) doc.getElementsByTagNameNS(DIDL_NS, "DIDL").item(0);
        Element item = (Element) doc.getElementsByTagNameNS(DIDL_NS, "Item").item(0);
        Element didlinfo = doc.createElementNS(DIDL_NS, "didl:DIDLInfo");
        didlinfo.appendChild(xmlSig.getElement());
        didl.insertBefore(didlinfo, item);
        Transforms transforms = new Transforms(doc);
        String[][] filters = { { XPath2FilterContainer.INTERSECT, "here()/ancestor::*[7]" }, { XPath2FilterContainer.SUBTRACT, "here()/ancestor::dsig:Signature[1]" } };
        transforms.addTransform(Transforms.TRANSFORM_XPATH2FILTER, XPath2FilterContainer.newInstances(doc, filters));
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        xmlSig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
        xmlSig.getKeyInfo().add(new RetrievalMethod(doc, certificateURL, null, "http://www.w3.org/2000/09/xmldsig#X509Data"));
        xmlSig.sign(privk);
        Element outsig = xmlSig.getElement();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLUtils.outputDOMc14nWithComments(outsig, os);
        return os.toString("UTF-8");
    }

    /**
     * Datastream level signature, returns XMLSignature XML fragment
     * @param ref - URI of the resource to be signed.
     * @param digest - Pre-calculated digest of resource, used as reference in didl.
     * @return signature for the provided refurl
     * @throws Exception
     */
    public String signRef(String ref, String digest) throws Exception {
        doc = db.newDocument();
        signature = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, "http://www.w3.org/2001/10/xml-exc-c14n#");
        signature.addDocument(ref, null, Constants.ALGO_ID_DIGEST_SHA1);
        signature.getKeyInfo().add(new RetrievalMethod(doc, certificateURL, null, "http://www.w3.org/2000/09/xmldsig#X509Data"));
        doc.appendChild(signature.getElement());
        if (digest == null) {
            signature.sign(privk);
        } else {
            this.sign(privk, digest);
        }
        Element outsig = signature.getElement();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLUtils.outputDOMc14nWithComments(outsig, os);
        return os.toString("UTF-8");
    }

    /**
     * Provide a resolvable reference without a digest, 
     * returns XMLSignature XML fragment
     * @param ref - URI of the resource to be signed.
     * @return signature for reference
     */
    public String signRef(String ref) throws Exception {
        return this.signRef(ref, null);
    }

    /**
     * Download and unzip reference before calculating digest.
     * @param ref - Reference to gzip file
     * @param digest - Pre-calculated digest of unzipped stream
     * @return signature for gzip reference
     */
    public String signGzipRef(String ref, String digest) throws Exception {
        doc = db.newDocument();
        signature = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, "http://www.w3.org/2001/10/xml-exc-c14n#");
        Transforms transforms = new Transforms(doc);
        transforms.addTransform("http://www.iana.org/assignments/http-parameters#gzip");
        signature.addDocument(ref, transforms, Constants.ALGO_ID_DIGEST_SHA1);
        signature.getKeyInfo().add(new RetrievalMethod(doc, certificateURL, null, "http://www.w3.org/2000/09/xmldsig#X509Data"));
        doc.appendChild(signature.getElement());
        if (digest == null) {
            signature.sign(privk);
        } else {
            this.sign(privk, digest);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLUtils.outputDOM(doc, os);
        return os.toString("UTF-8");
    }

    /**
     * Download and unzip reference before calculating digest.
     * @param ref - Reference to gzip file
     * @return signature for gzip reference
     */
    public String signGzipRef(String ref) throws Exception {
        return this.signGzipRef(ref, null);
    }

    /**
     * Provided inline xml, generates digest, and returns signature
     * @param refurl - xml_id of component, i.e. "#uuid_XXXXXX"
     * @param fragment - xml fragement
     * @return signature for xml fragment
     * @throws Exception
     */
    public String signXMLinline(String refurl, String fragment) throws Exception {
        byte[] canbytes = this.canonicalizeXML(fragment);
        String digest = this.calculateDigest(canbytes, "SHA1");
        return this.signXML(refurl, digest);
    }

    /**
     * Provided xml URI and digest, returns signature
     * @param refurl - URI of the resource to be signed.
     * @param digest - Pre-calculated digest of ref ur
     * @return signature for the provided refurl
     * @throws Exception
     */
    private String signXML(String refurl, String digest) throws Exception {
        doc = db.newDocument();
        signature = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, "http://www.w3.org/2001/10/xml-exc-c14n#");
        Transforms transforms = new Transforms(doc);
        XPathContainer xpath = new XPathContainer(doc);
        String xp1 = "ancestor-or-self::didl:Resource[not(@encoding)][not(@ref)]/*";
        xpath.setXPathNamespaceContext("didl", DIDL_NS);
        xpath.setXPath(xp1);
        transforms.addTransform(Transforms.TRANSFORM_XPATH, xpath.getElementPlusReturns());
        transforms.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
        signature.addDocument(refurl, transforms, Constants.ALGO_ID_DIGEST_SHA1);
        signature.getKeyInfo().add(new RetrievalMethod(doc, certificateURL, null, "http://www.w3.org/2000/09/xmldsig#X509Data"));
        doc.appendChild(signature.getElement());
        this.sign(privk, digest);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLUtils.outputDOM(doc, os);
        return os.toString("UTF-8");
    }

    /**
     * Provided Text URI and digest, returns signature
     * @param refurl - URI of the resource to be signed.
     * @param digest - Pre-calculated digest of ref ur
     * @return signature for the provided refurl
     * @throws Exception
     */
    public String signText(String refurl, String digest) throws Exception {
        doc = db.newDocument();
        signature = new XMLSignature(doc, "", XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, "http://www.w3.org/2001/10/xml-exc-c14n#");
        Transforms transforms = new Transforms(doc);
        XPathContainer xpath = new XPathContainer(doc);
        String xp1 = "ancestor-or-self::node()=//didl:Resource[not(@encoding)][not(@ref)]/text()";
        xpath.setXPathNamespaceContext("didl", DIDL_NS);
        xpath.setXPath(xp1);
        transforms.addTransform(Transforms.TRANSFORM_XPATH, xpath.getElementPlusReturns());
        signature.addDocument(refurl, transforms, Constants.ALGO_ID_DIGEST_SHA1);
        signature.getKeyInfo().add(new RetrievalMethod(doc, certificateURL, null, "http://www.w3.org/2000/09/xmldsig#X509Data"));
        doc.appendChild(signature.getElement());
        this.sign(privk, digest);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        XMLUtils.outputDOM(doc, os);
        return os.toString("UTF-8");
    }

    /**
     * Utility method to calculate digest
     * 
     * @param streamBytes
     * @param algoritm
     *            like "SHA1"
     * @return base64 digest
     * @throws NoSuchAlgorithmException
     */
    public String calculateDigest(byte[] streamBytes, String algoritm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algoritm);
        byte[] digest = md.digest(streamBytes);
        String base64codedValue = Base64.encode(digest);
        return base64codedValue;
    }

    /**
     * Method canonicalizeXML
     * 
     * @param fragment
     * @return canonicalized XML
     */
    public byte[] canonicalizeXML(String fragment) throws Exception {
        InputSource IS = new InputSource(new StringReader(fragment));
        org.w3c.dom.Document doc = db.parse(IS);
        Canonicalizer c14n = Canonicalizer.getInstance("http://www.w3.org/2001/10/xml-exc-c14n#");
        byte[] outputBytes = c14n.canonicalizeSubtree(doc);
        return outputBytes;
    }

    /**
     * Method setDigestValueElement
     * 
     * @param base64codedValue
     */
    public void setDigestValueElement(String base64codedValue) throws XMLSecurityException {
        Reference ref = signature.getSignedInfo().item(0);
        Element digestValueElement = XMLUtils.selectDsNode(ref.getElement().getFirstChild(), Constants._TAG_DIGESTVALUE, 0);
        Node n = digestValueElement.getFirstChild();
        while (n != null) {
            digestValueElement.removeChild(n);
            n = n.getNextSibling();
        }
        Text t = doc.createTextNode(base64codedValue);
        digestValueElement.appendChild(t);
    }

    public void setSignatureValueElement(byte[] bytes) {
        Element signatureValueElem = XMLUtils.selectDsNode(signature.getElement().getFirstChild(), Constants._TAG_SIGNATUREVALUE, 0);
        while (signatureValueElem.hasChildNodes()) {
            signatureValueElem.removeChild(signatureValueElem.getFirstChild());
        }
        String base64codedValue = Base64.encode(bytes);
        if (base64codedValue.length() > 76) {
            base64codedValue = "\n" + base64codedValue + "\n";
        }
        Text t = doc.createTextNode(base64codedValue);
        signatureValueElem.appendChild(t);
    }

    /**
     * Datastream level signature, returns XMLSignature XML fragment
     * @param signingKey - Private Key generated and set by constructor
     * @param digest - Pre-calculated digest of resource
     */
    public void sign(Key signingKey, String digest) throws XMLSignatureException, Exception {
        try {
            SignedInfo si = signature.getSignedInfo();
            Element signatureMethodElement = si.getSignatureMethodElement();
            SignatureAlgorithm sa = new SignatureAlgorithm(signatureMethodElement, signature.getBaseURI());
            sa.initSign(signingKey);
            this.setDigestValueElement(digest);
            OutputStream so = new BufferedOutputStream(new SignerOutputStream(sa));
            try {
                so.close();
            } catch (IOException e) {
            }
            si.signInOctectStream(so);
            byte jcebytes[] = sa.sign();
            setSignatureValueElement(jcebytes);
        } catch (CanonicalizationException ex) {
            throw new XMLSignatureException("empty", ex);
        } catch (InvalidCanonicalizerException ex) {
            throw new XMLSignatureException("empty", ex);
        } catch (XMLSecurityException ex) {
            throw new XMLSignatureException("empty", ex);
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: java gov.lanl.xmlsig.CreateXMLSig <sig.properties>");
            System.exit(-1);
        }
        XMLSigProperties props = new XMLSigProperties(args[0]);
        String keystoreType = props.getKeyStoreType();
        String keystoreFile = props.getKeyStoreFile();
        String keystorePass = props.getKeyStorePass();
        String privateKeyAlias = props.getPrivateKeyAlias();
        String privateKeyPass = props.getPrivateKeyPass();
        String certificateAlias = props.getCertificateAlias();
        String certificateURL = props.getCertificateURL();
        CreateXMLSig c = new CreateXMLSig(keystoreType, keystoreFile, keystorePass, privateKeyAlias, privateKeyPass, certificateURL);
        String sig = c.signRef("http://gws.lanl.gov", "o+c5rvs4+vieRz1y0dee0ycqk9k=");
        log.info(sig);
    }
}
