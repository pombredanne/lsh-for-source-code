package net.sf.ahtutils.report;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sf.exlp.util.xml.JDomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReportUtilHash {

    static final Logger logger = LoggerFactory.getLogger(ReportUtilXls.class);

    private Document document;

    private File report;

    public ReportUtilHash(String jrxml) {
        org.jdom.Document jdomDoc = JDomUtil.load(jrxml);
        document = JDomUtil.toW3CDocument(jdomDoc);
        report = new File(jrxml);
    }

    public ReportUtilHash(InputStream report, String jrxml) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e1) {
            e1.printStackTrace();
        }
        try {
            document = builder.parse(report);
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.report = new File(jrxml);
    }

    public NodeList executeXPath(String query) {
        Object result = null;
        XPathFactory factory = XPathFactory.newInstance();
        XPath xpath = factory.newXPath();
        XPathExpression expr = null;
        try {
            expr = xpath.compile(query);
            result = expr.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e1) {
            e1.printStackTrace();
        }
        NodeList nodes = (NodeList) result;
        return nodes;
    }

    public String readAndRemoveHash() {
        String hashCode = "none";
        NodeList nodes = executeXPath("//property[@name='hash']");
        if (nodes.getLength() > 0) {
            Node hash = nodes.item(0);
            if (!hash.equals(null)) {
                Element hash2 = (Element) hash;
                hashCode = hash2.getAttribute("value");
            }
            hash.getParentNode().removeChild(hash);
        }
        return hashCode;
    }

    public String calculateHash() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Source xmlSource = new DOMSource(document);
        Result outputTarget = new StreamResult(outputStream);
        try {
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
        } catch (TransformerConfigurationException e1) {
            e1.printStackTrace();
        } catch (TransformerException e1) {
            e1.printStackTrace();
        } catch (TransformerFactoryConfigurationError e1) {
            e1.printStackTrace();
        }
        InputStream is = new ByteArrayInputStream(outputStream.toByteArray());
        MessageDigest md = null;
        DigestInputStream digestStream;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        }
        digestStream = new DigestInputStream(is, md);
        try {
            while (digestStream.read() != -1) ;
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        try {
            digestStream.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        byte[] calculatedHash = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < calculatedHash.length; i++) {
            hexString.append(Integer.toHexString(0xFF & calculatedHash[i]));
        }
        String newHash = hexString.toString();
        Element hashCode = document.createElement("property");
        hashCode.setAttribute("name", "hash");
        hashCode.setAttribute("value", newHash);
        NodeList nodes = executeXPath("//jasperReport");
        Node jasperReports = nodes.item(0);
        jasperReports.insertBefore(hashCode, jasperReports.getFirstChild());
        return newHash;
    }

    public void saveNewHash() {
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer = null;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(report);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        }
        try {
            transformer = tFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        }
        DOMSource source = new DOMSource(document);
        StreamResult result = new StreamResult(fos);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}
