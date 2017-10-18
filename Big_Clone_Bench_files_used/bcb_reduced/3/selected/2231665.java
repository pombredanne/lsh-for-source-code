package org.infoeng.ictp.documents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.XMLUtils;
import org.infoeng.ictp.exceptions.ICTPException;
import org.infoeng.ictp.utils.ICTPConstants;
import org.infoeng.icws.util.Utils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class ICTPDocument {

    protected XPath xpath;

    public Document doc;

    private String SIGNATURE_BASE_URI = "";

    protected void init(String rootElementStr) throws Exception {
        this.init(rootElementStr, true);
        checkICTPDocumentValidity();
    }

    protected void init(String rootElementStr, boolean ifInclude) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        doc = dbf.newDocumentBuilder().getDOMImplementation().createDocument(ICTPConstants.ICT_NAMESPACE, rootElementStr, null);
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        doc.normalizeDocument();
        xpath = XPathFactory.newInstance().newXPath();
        if (ifInclude) {
            Element rootElement = doc.getDocumentElement();
            byte[] randBytes = new byte[20];
            new java.util.Random().nextBytes(randBytes);
            String rndStr = new java.math.BigInteger(randBytes).abs().toString(16);
            rootElement.setAttribute("Id", rndStr);
            rootElement.setIdAttribute("Id", true);
        }
        fillTradeFields();
        checkICTPDocumentValidity();
    }

    protected void load(InputStream isIS) throws Exception {
        xpath = XPathFactory.newInstance().newXPath();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        doc = dbf.newDocumentBuilder().parse(isIS);
        checkICTPDocumentValidity();
    }

    /**
     *  <p> fillTradeFields() is an internal method to fill the fields of the ICTPDocument
     *      for further processing. </p>
     *  <p> The fields filled by this method must be present for a valid ICTPDocument! </p>
     */
    protected void fillTradeFields() throws Exception {
        Element requestedInstrumentElem = (Element) doc.getDocumentElement().appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "requestedInstrument"));
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        requestedInstrumentElem.appendChild(doc.createTextNode("\n"));
        requestedInstrumentElem.appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "seriesID"));
        requestedInstrumentElem.appendChild(doc.createTextNode("\n"));
        requestedInstrumentElem.appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "quantity"));
        requestedInstrumentElem.appendChild(doc.createTextNode("\n"));
        Element offeredInstrumentElem = (Element) doc.getDocumentElement().appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "offeredInstrument"));
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        offeredInstrumentElem.appendChild(doc.createTextNode("\n"));
        offeredInstrumentElem.appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "seriesID"));
        offeredInstrumentElem.appendChild(doc.createTextNode("\n"));
        offeredInstrumentElem.appendChild(doc.createElementNS(ICTPConstants.ICT_NAMESPACE, "quantity"));
        offeredInstrumentElem.appendChild(doc.createTextNode("\n"));
    }

    public void setRequestedQuantity(int quantity) throws Exception {
        if (quantity < 1) throw new Exception("less than one quantity not allowed.");
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='requestedInstrument']/*[local-name()='quantity']", doc, XPathConstants.NODE);
        elem.setTextContent("" + quantity + "");
    }

    public void setRequestedSeriesID(String seriesId) throws Exception {
        if (seriesId == null) throw new Exception("requested seriesId is null");
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='requestedInstrument']/*[local-name()='seriesID']", doc, XPathConstants.NODE);
        elem.setTextContent("" + seriesId + "");
    }

    public void setOfferedQuantity(int quantity) throws Exception {
        if (quantity < 1) throw new Exception("less than one quantity not allowed.");
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='offeredInstrument']/*[local-name()='quantity']", doc, XPathConstants.NODE);
        elem.setTextContent("" + quantity + "");
    }

    public void setOfferedSeriesID(String seriesId) throws Exception {
        if (seriesId == null) throw new Exception("offeredSeriesId is null");
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='offeredInstrument']/*[local-name()='seriesID']", doc, XPathConstants.NODE);
        elem.setTextContent("" + seriesId + "");
    }

    public int getRequestedQuantity() throws Exception {
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='requestedInstrument']/*[local-name()='quantity']", doc, XPathConstants.NODE);
        if (elem == null) throw new ICTPException("Did not find '/*/requestedInstrument/quantity' element.");
        try {
            Integer quantInt = new Integer(elem.getTextContent());
            return quantInt.intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    public String getRequestedSeriesID() throws Exception {
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='requestedInstrument']/*[local-name()='seriesID']", doc, XPathConstants.NODE);
        if (elem == null) return null;
        if (elem.getTextContent() == null) return null;
        return elem.getTextContent().trim();
    }

    public int getOfferedQuantity() throws Exception {
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='offeredInstrument']/*[local-name()='quantity']", doc, XPathConstants.NODE);
        if (elem == null) throw new ICTPException("Did not find '/*/offeredInstrument/quantity' element.");
        try {
            Integer quantInt = new Integer(elem.getTextContent());
            return quantInt.intValue();
        } catch (Exception e) {
            return -1;
        }
    }

    public String getOfferedSeriesID() throws Exception {
        Element elem = (Element) xpath.evaluate("/*/*[local-name()='offeredInstrument']/*[local-name()='seriesID']", doc, XPathConstants.NODE);
        if (elem == null) return null;
        if (elem.getTextContent() == null) return null;
        return elem.getTextContent().trim();
    }

    public void sign(PrivateKey privKey) throws Exception {
        sign(privKey, false, true);
    }

    public void sign(PrivateKey privKey, boolean overwrite) throws Exception {
        sign(privKey, overwrite, true);
    }

    /**
     *  Sign the document using the private key, not overwriting an existing signature and including the KeyInfo.
     *  @param privKey PrivateKey for signing the document.
     *  @param overwrite boolean determining whether to overwrite an existing signature (if one is present).
     *  @param keyInclude boolean determining whether to include the public key value in the document.
     *  @throws Exception 
     */
    public void sign(PrivateKey privKey, boolean overwrite, boolean keyInclude) throws Exception {
        try {
            org.apache.xml.security.Init.init();
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
            Element sigElement = (Element) xpath.evaluate("/*/*[local-name()='Signature'][namespace-uri()='" + Constants.SignatureSpecNS + "']", doc, XPathConstants.NODE);
            if (sigElement != null) {
                if (overwrite) {
                    sigElement.getParentNode().removeChild(sigElement);
                } else {
                    throw new Exception("Cannot sign an already-signed document!");
                }
            } else {
                overwrite = false;
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
            if (keyInclude && keyType.equals("DSA")) {
                DSAPublicKey pubKey = generateDSAPublicKey(dsaPrivKey);
                sig.addKeyInfo(pubKey);
            } else if (keyInclude && keyType.equals("RSA")) {
                RSAPublicKey pubKey = generateRSAPublicKey(rsaPrivKey);
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
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public String getDigestValue() throws Exception {
        String docval = toString();
        byte[] digestBytes = MessageDigest.getInstance("SHA-1").digest(docval.getBytes());
        String digestStr = Utils.toHex(digestBytes);
        return digestStr;
    }

    public String getDigestValue(String digestSpec) throws Exception {
        String docval = toString();
        byte[] digestBytes = MessageDigest.getInstance(digestSpec).digest(docval.getBytes());
        String digestStr = Utils.toHex(digestBytes);
        return digestStr;
    }

    /** 
     *  Verify the signature using the public key provided.
     *  @param pubKey PublicKey used to verify the signature.
     *  @return boolean indicating whether signature verifies with supplied public key. 
     *  @throws Exception If there is not a signature present in the document.
     */
    public boolean verifySignature(PublicKey pubKey) throws Exception {
        org.apache.xml.security.Init.init();
        XMLSignature sig = getXMLSignature();
        if (sig == null) throw new Exception("No XMLSignature found.");
        return sig.checkSignatureValue(pubKey);
    }

    /**
     *  Verify the signature using the public key within the document.
     *  @return boolean indicating whether signature verifies with the public key within the document. 
     *  @throws Exception If there is not a signature present in the document.
     */
    public boolean verifySignature() throws Exception {
        org.apache.xml.security.Init.init();
        XMLSignature sig = getXMLSignature();
        if (sig == null) throw new Exception("No XMLSignature found.");
        if (sig.getKeyInfo() == null) throw new Exception(" No key info found. ");
        if (sig.getKeyInfo().getPublicKey() == null) throw new Exception(" No public key found. ");
        PublicKey pubKey = sig.getKeyInfo().getPublicKey();
        return sig.checkSignatureValue(pubKey);
    }

    /**
     *  Get the XMLSignature object for the document.
     *  @return XMLSignature Signature object from the document if present.
     *  @throws Exception If there is not a signature present in the document.
     */
    public XMLSignature getXMLSignature() throws Exception {
        org.apache.xml.security.Init.init();
        Element sigElement = (Element) xpath.evaluate("/*/*[local-name()='Signature'][namespace-uri()='" + Constants.SignatureSpecNS + "']", doc, XPathConstants.NODE);
        if (sigElement == null) return null;
        XMLSignature sig = new XMLSignature(sigElement, SIGNATURE_BASE_URI);
        return sig;
    }

    public static PublicKey generatePublicKey(PrivateKey privKey) {
        PublicKey pk = null;
        if (privKey instanceof RSAPrivateCrtKey) {
            pk = (PublicKey) ICTPDocument.generateRSAPublicKey((RSAPrivateCrtKey) privKey);
        } else if (privKey instanceof DSAPrivateKey) {
            pk = (PublicKey) ICTPDocument.generateDSAPublicKey((DSAPrivateKey) privKey);
        }
        return pk;
    }

    /**
     *  Generate a DSA public key based on the private key supplied.  
     *  @param dsaPrivKey DSA private key to be converted. 
     *  @return Public key corresponding to the input private key.
     */
    public static DSAPublicKey generateDSAPublicKey(DSAPrivateKey dsaPrivKey) {
        try {
            BigInteger bigX = dsaPrivKey.getX();
            BigInteger bigG = dsaPrivKey.getParams().getG();
            BigInteger bigP = dsaPrivKey.getParams().getP();
            BigInteger bigQ = dsaPrivKey.getParams().getQ();
            BigInteger bigY = bigG.modPow(bigX, bigP);
            DSAPublicKeySpec dsaPubKeySpec = new DSAPublicKeySpec(bigY, bigP, bigQ, bigG);
            KeyFactory kf = KeyFactory.getInstance("DSA");
            DSAPublicKey pubKey = (DSAPublicKey) kf.generatePublic(dsaPubKeySpec);
            return pubKey;
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    /**
     *  Generate a RSA public key based on the private key supplied.  
     *  @param privKey RSA private key to be converted. 
     *  @return Public key corresponding to the input private key.
     */
    public static RSAPublicKey generateRSAPublicKey(RSAPrivateKey privKey) {
        try {
            RSAPrivateCrtKey rsaPrivKey = (RSAPrivateCrtKey) privKey;
            RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(rsaPrivKey.getModulus(), rsaPrivKey.getPublicExponent());
            KeyFactory kf = KeyFactory.getInstance("RSA");
            RSAPublicKey rpk = (RSAPublicKey) kf.generatePublic(pubKeySpec);
            return rpk;
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    /**
     *  Get the id of the document (the 'Id' attribute of the root element).
     *  @return id string.
     */
    public String getId() {
        try {
            return doc.getDocumentElement().getAttribute("Id").trim();
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    /**  
     *  @return The xml string of the document. 
     */
    public String toString() {
        org.apache.xml.security.Init.init();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XMLUtils.outputDOM(doc, baos);
        return baos.toString();
    }

    /** 
     *  @param os The OutputStream to write the document to.
     */
    public void toString(OutputStream os) {
        org.apache.xml.security.Init.init();
        XMLUtils.outputDOM(doc, os);
    }

    /** 
     *  @return The document string with HTML entities added.
     */
    public String toHTML() {
        String retStr = toString();
        retStr = addEntities(retStr);
        return retStr;
    }

    /**
     *  @return The public key within the document.
     */
    public PublicKey getSignaturePublicKey() {
        try {
            XMLSignature sigObj = this.getXMLSignature();
            PublicKey pubKey = (PublicKey) sigObj.getKeyInfo().getPublicKey();
            return pubKey;
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    public Element removeSignatureElement() throws Exception {
        Element sigElement = (Element) xpath.evaluate("/*/*[local-name()='Signature'][namespace-uri()='" + Constants.SignatureSpecNS + "']", doc, XPathConstants.NODE);
        if (sigElement == null) return null;
        Node parentNode = sigElement.getParentNode();
        if (parentNode == null) return null;
        Element tmpElem = (Element) parentNode.removeChild(sigElement);
        return tmpElem;
    }

    public static String convertEntities(String oldValue) {
        String value = oldValue.replaceAll("&amp;", "&");
        value = value.replaceAll("&quot;", "\"");
        value = value.replaceAll("&apos;", "'");
        value = value.replaceAll("&lt;", "<");
        value = value.replaceAll("&gt;", ">");
        return value;
    }

    public void checkICTPDocumentValidity() throws Exception {
        getRequestedQuantity();
        getRequestedSeriesID();
        getOfferedQuantity();
        getOfferedSeriesID();
    }

    public static String addEntities(String oldValue) {
        String value = oldValue.replaceAll("&", "&amp;");
        value = value.replaceAll("\"", "&quot;");
        value = value.replaceAll("'", "&apos;");
        value = value.replaceAll("<", "&lt;");
        value = value.replaceAll(">", "&gt;");
        return value;
    }
}
