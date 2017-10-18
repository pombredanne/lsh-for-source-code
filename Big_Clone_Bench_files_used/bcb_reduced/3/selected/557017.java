package org.infoeng.icws.documents;

import org.infoeng.icws.documents.ICWSDocument;
import org.infoeng.icws.documents.PrimarySeriesInfo;
import org.infoeng.icws.documents.SeriesInfo;
import org.infoeng.icws.documents.VerificationCertificate;
import org.infoeng.icws.utils.ICWSConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.DSAPublicKey;
import java.util.Arrays;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.infoeng.icws.utils.Base64;
import org.w3c.dom.Element;
import org.apache.xml.security.utils.XMLUtils;
import org.apache.xpath.XPathAPI;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceFactory;

public class InformationCurrencyUnit extends ICWSDocument {

    public InformationCurrencyUnit(Element icuElem) throws Exception {
        try {
            super.init("icu");
            if (icuElem == null) throw new Exception("InformationCurrencyUnit(Element) must not be null.");
            Element sidElem = (Element) XPathAPI.selectSingleNode(icuElem, "*[local-name()='sid']");
            Element sigElem = (Element) XPathAPI.selectSingleNode(icuElem, "*[local-name()='sig']");
            Element ciElem = (Element) XPathAPI.selectSingleNode(icuElem, "*[local-name()='ci']");
            if (sidElem.getTextContent() == null || "".equals(sidElem.getTextContent().trim())) throw new Exception("sid element must not be null");
            if (sigElem.getTextContent() == null || "".equals(sigElem.getTextContent().trim())) throw new Exception("sig element must not be null");
            if (ciElem.getTextContent() == null || "".equals(ciElem.getTextContent().trim())) throw new Exception("ci element must not be null");
            doc.getDocumentElement().appendChild(doc.createElement("sid")).setTextContent(sidElem.getTextContent());
            doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            doc.getDocumentElement().appendChild(doc.createElement("ci")).setTextContent(ciElem.getTextContent());
            doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            doc.getDocumentElement().appendChild(doc.createElement("sig")).setTextContent(sigElem.getTextContent());
            doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            checkValidity();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public InformationCurrencyUnit(String seriesId) throws Exception {
        try {
            super.init("icu");
            Element sidElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("sid"));
            doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            sidElem.setTextContent(seriesId);
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public InformationCurrencyUnit(String seriesId, byte[] certBytes, byte[] sigBytes) throws Exception {
        super.init("icu");
        Element sidElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("sid"));
        sidElem.setTextContent(seriesId);
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        Element ciElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("ci"));
        ciElem.setTextContent(Base64.encode(certBytes));
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
        Element sigElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("sig"));
        sigElem.setTextContent(Base64.encode(sigBytes));
        doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
    }

    public InformationCurrencyUnit(InputStream icuIS) throws Exception {
        try {
            super.load(icuIS);
            checkValidity();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public String getCertInfo() throws Exception {
        try {
            Element ciElem = (Element) XPathAPI.selectSingleNode(doc, "/icu/ci");
            return ciElem.getTextContent();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public byte[] getCertBytes() throws Exception {
        try {
            Element ciElem = (Element) XPathAPI.selectSingleNode(doc, "/icu/ci");
            return org.infoeng.icws.utils.Base64.decode(ciElem.getTextContent());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public String getSignatureValue() {
        try {
            Element sigElem = (Element) XPathAPI.selectSingleNode(doc, "/icu/sig");
            return sigElem.getTextContent();
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    public byte[] getSignatureBytes() throws Exception {
        try {
            Element sigElem = (Element) XPathAPI.selectSingleNode(doc, "/icu/sig");
            return org.infoeng.icws.utils.Base64.decode(sigElem.getTextContent());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public boolean equals(InformationCurrencyUnit icu) {
        try {
            boolean result = Arrays.equals(this.getCertBytes(), icu.getCertBytes()) && Arrays.equals(this.getSignatureBytes(), icu.getSignatureBytes()) && this.getSeriesID().equals(icu.getSeriesID());
            return result;
        } catch (java.lang.Exception e) {
            return false;
        }
    }

    public String getDigestValue() throws Exception {
        try {
            return Base64.encode(getDigestBytes());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public byte[] getDigestBytes() throws Exception {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA");
            return sha.digest(getCertBytes());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public String getSeriesID() {
        try {
            Element sidElem = (Element) XPathAPI.selectSingleNode(doc, "/icu/sid");
            return sidElem.getTextContent().trim();
        } catch (java.lang.Exception e) {
            return null;
        }
    }

    public int getNumberBytes() {
        try {
            return getCertBytes().length;
        } catch (java.lang.Exception e) {
            return -1;
        }
    }

    public Element getElement() {
        return doc.getDocumentElement();
    }

    public void checkValidity() throws Exception {
        try {
            String seriesId = getSeriesID();
            if (seriesId == null || "".equals(seriesId)) throw new Exception("seriesId must be defined");
            if (getCertBytes().length < 16) throw new Exception("certBytes must have length greater than 16");
            if (getSignatureBytes().length < 8) throw new Exception("signature byte array must be greater than 32");
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public VerificationCertificate getVerificationCertificate() throws Exception {
        return new VerificationCertificate(this);
    }

    public String toString() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        org.apache.xml.security.Init.init();
        XMLUtils.outputDOMc14nWithComments(doc, baos);
        return new String(baos.toByteArray());
    }

    public boolean verifySignatureBytes(PublicKey pk) throws Exception {
        try {
            Signature sig = Signature.getInstance(ICWSConstants.SIGNATURE_ALGORITHM);
            sig.initVerify(pk);
            sig.update(getCertBytes());
            return sig.verify(getSignatureBytes());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public VerificationCertificate[] verify(boolean input) throws Exception {
        VerificationCertificate[] retArray = new VerificationCertificate[2];
        QName portName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_PORT_NAME);
        QName serviceName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_SERVICE_NAME);
        Properties clientProps = new Properties();
        try {
            String clientFileStr = System.getenv(ICWSConstants.CLIENT_PARAMETERS_FILE);
            if (clientFileStr != null) {
                FileInputStream fis = new FileInputStream(new File(clientFileStr));
                clientProps.loadFromXML(fis);
            } else {
                InputStream clientPropsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(ICWSConstants.DEFAULT_FILE_NAME);
                clientProps.loadFromXML(clientPropsIS);
            }
        } catch (java.lang.Exception e) {
        }
        if (clientProps.getProperty("trustStore") != null && clientProps.getProperty("trustStorePassword") != null) {
            System.setProperty("javax.net.ssl.trustStore", clientProps.getProperty("trustStore"));
            System.setProperty("javax.net.ssl.trustStorePassword", clientProps.getProperty("trustStorePassword"));
        }
        try {
            String seriesId = getSeriesID();
            URL seriesURL = new URL(seriesId);
            URLConnection seriesConn = seriesURL.openConnection();
            InputStream seriesIS = seriesConn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int seriesByte = seriesIS.read();
                if (seriesByte == -1) break;
                baos.write(seriesByte);
            }
            PrimarySeriesInfo psi = new PrimarySeriesInfo(baos.toString());
            String serviceEndpointStr = psi.getServiceEndpoint();
            Object[] inputParams = new Object[1];
            VerificationCertificate origVeriCert = getVerificationCertificate();
            retArray[0] = origVeriCert;
            inputParams[0] = origVeriCert.toString();
            Service service = ServiceFactory.newInstance().createService(new URL(serviceEndpointStr + "?wsdl"), serviceName);
            Call call = service.createCall(portName, "verifyCertificate");
            Object obj = call.invoke(inputParams);
            try {
                String veriCertStr = (String) obj;
                VerificationCertificate veriCert = new VerificationCertificate(veriCertStr);
                retArray[1] = veriCert;
                byte[] expectedBytes = origVeriCert.expectedReturnValue();
                byte[] rndRetBytes = veriCert.getRandomBytes();
                boolean hasStrings = Arrays.equals(expectedBytes, rndRetBytes);
                PublicKey vcPubKey = veriCert.getSignaturePublicKey();
                boolean hasKey = verifySignatureBytes(vcPubKey);
                boolean isValid = (hasKey && hasStrings);
                if (isValid) {
                    return retArray;
                } else {
                    return null;
                }
            } catch (java.lang.Exception e) {
                throw new Exception("Did not find VerificationCertificate in return string " + obj + ".");
            }
        } catch (java.lang.Exception e) {
            throw new Exception("InformationCurrencyUnit.exchange(): ", e);
        }
    }

    public boolean verify() throws Exception {
        QName portName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_PORT_NAME);
        QName serviceName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_SERVICE_NAME);
        Properties clientProps = new Properties();
        try {
            String clientFileStr = System.getenv(ICWSConstants.CLIENT_PARAMETERS_FILE);
            if (clientFileStr != null) {
                FileInputStream fis = new FileInputStream(new File(clientFileStr));
                clientProps.loadFromXML(fis);
            } else {
                InputStream clientPropsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(ICWSConstants.DEFAULT_FILE_NAME);
                clientProps.loadFromXML(clientPropsIS);
            }
        } catch (java.lang.Exception e) {
        }
        if (clientProps.getProperty("trustStore") != null && clientProps.getProperty("trustStorePassword") != null) {
            System.setProperty("javax.net.ssl.trustStore", clientProps.getProperty("trustStore"));
            System.setProperty("javax.net.ssl.trustStorePassword", clientProps.getProperty("trustStorePassword"));
        }
        try {
            String seriesId = getSeriesID();
            URL seriesURL = new URL(seriesId);
            URLConnection seriesConn = seriesURL.openConnection();
            InputStream seriesIS = seriesConn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int seriesByte = seriesIS.read();
                if (seriesByte == -1) break;
                baos.write(seriesByte);
            }
            PrimarySeriesInfo psi = new PrimarySeriesInfo(baos.toString());
            String serviceEndpointStr = psi.getServiceEndpoint();
            Object[] inputParams = new Object[1];
            VerificationCertificate origVeriCert = getVerificationCertificate();
            inputParams[0] = origVeriCert.toString();
            Service service = ServiceFactory.newInstance().createService(new URL(serviceEndpointStr + "?wsdl"), serviceName);
            Call call = service.createCall(portName, "verifyCertificate");
            Object obj = call.invoke(inputParams);
            try {
                String veriCertStr = (String) obj;
                VerificationCertificate veriCert = new VerificationCertificate(veriCertStr);
                byte[] expectedBytes = origVeriCert.expectedReturnValue();
                byte[] rndRetBytes = veriCert.getRandomBytes();
                boolean hasStrings = Arrays.equals(expectedBytes, rndRetBytes);
                PublicKey vcPubKey = veriCert.getSignaturePublicKey();
                boolean hasKey = verifySignatureBytes(vcPubKey);
                boolean isValid = (hasKey && hasStrings);
                return isValid;
            } catch (java.lang.Exception e) {
                throw new Exception("Did not find VerificationCertificate in return string " + obj + ".");
            }
        } catch (java.lang.Exception e) {
            throw new Exception("InformationCurrencyUnit.exchange(): ", e);
        }
    }

    public PrimarySeriesInfo getSeriesInfo() throws Exception {
        Properties clientProps = new Properties();
        try {
            String clientFileStr = System.getenv(ICWSConstants.CLIENT_PARAMETERS_FILE);
            if (clientFileStr != null) {
                FileInputStream fis = new FileInputStream(new File(clientFileStr));
                clientProps.loadFromXML(fis);
            } else {
                InputStream clientPropsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(ICWSConstants.DEFAULT_FILE_NAME);
                clientProps.loadFromXML(clientPropsIS);
            }
        } catch (java.lang.Exception e) {
        }
        if (clientProps.getProperty("trustStore") != null && clientProps.getProperty("trustStorePassword") != null) {
            System.setProperty("javax.net.ssl.trustStore", clientProps.getProperty("trustStore"));
            System.setProperty("javax.net.ssl.trustStorePassword", clientProps.getProperty("trustStorePassword"));
        }
        String seriesId = getSeriesID();
        URL seriesURL = new URL(seriesId);
        URLConnection seriesConn = seriesURL.openConnection();
        InputStream seriesIS = seriesConn.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while (true) {
            int seriesByte = seriesIS.read();
            if (seriesByte == -1) break;
            baos.write(seriesByte);
        }
        PrimarySeriesInfo psi = new PrimarySeriesInfo(baos.toString());
        return psi;
    }

    public InformationCurrencyUnit exchange() throws Exception {
        QName portName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_PORT_NAME);
        QName serviceName = new QName(ICWSConstants.ICWS_URN, ICWSConstants.ICWS_SERVICE_NAME);
        Properties clientProps = new Properties();
        try {
            String clientFileStr = System.getenv(ICWSConstants.CLIENT_PARAMETERS_FILE);
            if (clientFileStr != null) {
                FileInputStream fis = new FileInputStream(new File(clientFileStr));
                clientProps.loadFromXML(fis);
            } else {
                InputStream clientPropsIS = Thread.currentThread().getContextClassLoader().getResourceAsStream(ICWSConstants.DEFAULT_FILE_NAME);
                clientProps.loadFromXML(clientPropsIS);
            }
        } catch (java.lang.Exception e) {
        }
        if (clientProps.getProperty("trustStore") != null && clientProps.getProperty("trustStorePassword") != null) {
            System.setProperty("javax.net.ssl.trustStore", clientProps.getProperty("trustStore"));
            System.setProperty("javax.net.ssl.trustStorePassword", clientProps.getProperty("trustStorePassword"));
        }
        try {
            String seriesId = getSeriesID();
            URL seriesURL = new URL(seriesId);
            URLConnection seriesConn = seriesURL.openConnection();
            InputStream seriesIS = seriesConn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int seriesByte = seriesIS.read();
                if (seriesByte == -1) break;
                baos.write(seriesByte);
            }
            PrimarySeriesInfo psi = new PrimarySeriesInfo(baos.toString());
            String serviceEndpointStr = psi.getServiceEndpoint();
            Object[] inputParams = new Object[1];
            inputParams[0] = this.toString();
            Service service = ServiceFactory.newInstance().createService(new URL(serviceEndpointStr + "?wsdl"), serviceName);
            Call call = service.createCall(portName, "exchangeCertificate");
            Object obj = call.invoke(inputParams);
            try {
                String icuStr = (String) obj;
                InformationCurrencyUnit newICU = new InformationCurrencyUnit(new ByteArrayInputStream(icuStr.getBytes()));
                if (this.equals(newICU)) return null;
                return newICU;
            } catch (java.lang.Exception e) {
                throw new Exception("Exception parsing returned object: " + obj + "");
            }
        } catch (java.lang.Exception e) {
            throw new Exception("InformationCurrencyUnit.exchange(): ", e);
        }
    }
}
