package org.infoeng.icws.documents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.infoeng.icws.documents.InformationCurrencyUnit;
import org.infoeng.icws.utils.Base64;
import org.infoeng.icws.utils.ICWSConstants;
import org.apache.xpath.XPathAPI;
import javax.xml.namespace.QName;
import javax.xml.rpc.Call;
import javax.xml.rpc.Service;
import javax.xml.rpc.ServiceFactory;

public class VerificationCertificate extends ICWSDocument {

    private InformationCurrencyUnit icu;

    /**
     * <p> 
     *     The verification certificate constructor.  The format of a verification certificate is 
     *     
     *     &lt;VerificationCertificate&gt;
     *       &lt;sid&gt;&lt;/sid&gt;
     *       &lt;digestValue&gt;&lt;/digestValue&gt;
     *       &lt;randomValue&gt;&lt;/randomValue&gt;
     *     &lt;/VerificationCertificate&gt;.
     * </p>
     * @param vcXML The verification certificate document to be parsed to 
     *              create the verification certificate.  
     *
     **/
    public VerificationCertificate(InformationCurrencyUnit thisICU) throws Exception {
        try {
            super.init("VerificationCertificate");
            setSeriesID(thisICU.getSeriesID());
            setDigestValue(thisICU.getDigestValue());
            byte[] rndBytes = new byte[20];
            new java.security.SecureRandom().nextBytes(rndBytes);
            setRandomValue(rndBytes);
            setInformationCurrencyUnit(thisICU);
            checkValidity();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public VerificationCertificate(String vcStr) throws Exception {
        super.load(new ByteArrayInputStream(vcStr.getBytes()));
        checkValidity();
    }

    public VerificationCertificate(InputStream vcIS) throws Exception {
        super.load(vcIS);
        checkValidity();
    }

    public VerificationCertificate() throws Exception {
        super.init("VerificationCertificate");
    }

    private void checkValidity() throws Exception {
        try {
            if (getSeriesID() == null || getDigestValue() == null || getRandomValue() == null) throw new Exception(" seriesID is " + getSeriesID() + " and digest value is " + getDigestValue() + " and random string is " + getRandomValue() + ".");
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    private void setInformationCurrencyUnit(InformationCurrencyUnit thisICU) {
        icu = thisICU;
    }

    private InformationCurrencyUnit getInformationCurrencyUnit() throws Exception {
        return icu;
    }

    public String getSeriesID() throws Exception {
        try {
            Element srsElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/seriesID");
            if (srsElem == null) return null;
            return srsElem.getTextContent();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public void setSeriesID(String seriesId) throws Exception {
        try {
            Element srsElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/seriesID");
            if (srsElem == null) {
                srsElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("seriesID"));
                doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            }
            srsElem.setTextContent(seriesId.trim());
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public void setRandomValue(String rndStr) throws Exception {
        try {
            Element rndElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/randomValue");
            if (rndElem == null) {
                rndElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("randomValue"));
                doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            }
            rndElem.setTextContent(rndStr);
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public void setRandomValue(byte[] randomBytes) throws Exception {
        setRandomValue(Base64.encode(randomBytes));
    }

    public byte[] getRandomBytes() throws Exception {
        String randStr = getRandomValue();
        if (randStr == null) return null;
        return Base64.decode(randStr);
    }

    public String getRandomValue() throws Exception {
        try {
            Element rndElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/randomValue");
            if (rndElem == null) return null;
            return rndElem.getTextContent().trim();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public void setDigestValue(String dvStr) throws Exception {
        try {
            Element digestElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/digestValue");
            if (digestElem == null) {
                digestElem = (Element) doc.getDocumentElement().appendChild(doc.createElement("digestValue"));
                doc.getDocumentElement().appendChild(doc.createTextNode("\n"));
            }
            digestElem.setTextContent(dvStr);
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public void setDigestValue(byte[] dvBytes) throws Exception {
        try {
            String dvString = Base64.encode(dvBytes);
            setDigestValue(dvString);
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public String getDigestValue() throws Exception {
        try {
            Element rndElem = (Element) XPathAPI.selectSingleNode(doc, "/VerificationCertificate/digestValue");
            if (rndElem == null) return null;
            return rndElem.getTextContent().trim();
        } catch (java.lang.Exception e) {
            throw new Exception(e);
        }
    }

    public byte[] getDigestBytes() throws Exception {
        String digestStr = getDigestValue();
        return Base64.decode(digestStr);
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
            inputParams[0] = this.toString();
            Service service = ServiceFactory.newInstance().createService(new URL(serviceEndpointStr + "?wsdl"), serviceName);
            Call call = service.createCall(portName, "verifyCertificate");
            Object obj = call.invoke(inputParams);
            try {
                String veriCertStr = (String) obj;
                VerificationCertificate veriCert = new VerificationCertificate(veriCertStr);
                byte[] expectedBytes = expectedReturnValue();
                byte[] rndRetBytes = veriCert.getRandomBytes();
                return Arrays.equals(expectedBytes, rndRetBytes);
            } catch (java.lang.Exception e) {
                throw new Exception("Did not find VerificationCertificate in return string " + obj + ".");
            }
        } catch (java.lang.Exception e) {
            throw new Exception("VerificationCertificate.verify(): ", e);
        }
    }

    public byte[] expectedReturnValue() {
        try {
            byte[] sigBytes = icu.getSignatureBytes();
            byte[] rndBytes = getRandomBytes();
            byte[] returnBytes = new byte[sigBytes.length + rndBytes.length];
            System.arraycopy(sigBytes, 0, returnBytes, 0, sigBytes.length);
            System.arraycopy(rndBytes, 0, returnBytes, sigBytes.length, rndBytes.length);
            return MessageDigest.getInstance("SHA-1").digest(returnBytes);
        } catch (java.lang.Exception e) {
            return null;
        }
    }
}
