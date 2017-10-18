package xml;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sqlServer.CryptString;

public class XmlWebSet {

    private File xmlWebSetFile;

    private static final File xsdSchema = new File("src/xml/WebSetup.xsd");

    private Document doc;

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private String userPsw;

    public XmlWebSet(String userName, String psw) throws Exception {
        xmlWebSetFile = new File("settings/" + userName + "_WebSetup.xml");
        userPsw = getMdPsw(psw);
        String schemaLang = "http://www.w3.org/2001/XMLSchema";
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(true);
        factory.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage", "http://www.w3.org/2001/XMLSchema");
        builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new SimpleErrorHandler());
        if (!xmlWebSetFile.exists()) {
            try {
                xmlWebSetFile.createNewFile();
                doc = builder.newDocument();
                Element rootElement = doc.createElement("tns:winUsers");
                rootElement.setAttribute("xmlns:tns", "http://www.example.org/WebSetup");
                rootElement.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
                rootElement.setAttribute("xsi:schemaLocation", "http://www.example.org/WebSetup " + xsdSchema.getAbsolutePath());
                doc.appendChild(rootElement);
                Element em = doc.createElement("tns:user");
                em.setAttribute("userName", userName);
                rootElement.appendChild(em);
                Element emPsw = doc.createElement("tns:userPsw");
                emPsw.appendChild(doc.createTextNode(userPsw));
                em.appendChild(emPsw);
                writeXmlToFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        SchemaFactory factory = SchemaFactory.newInstance(schemaLang);
        Schema schema = factory.newSchema(new StreamSource(xsdSchema));
        Validator validator = schema.newValidator();
        String xmlEncFile = readFileAsString(xmlWebSetFile);
        Writer outWriterEnFile = new StringWriter();
        outWriterEnFile.write(xmlEncFile);
        System.out.println("xmlEncFile: " + xmlEncFile);
        DESKeySpec key = new DESKeySpec(userPsw.getBytes());
        System.out.println("userPsw: " + userPsw);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        CryptString crypt = new CryptString(keyFactory.generateSecret(key));
        String decryptedString = crypt.decryptBase64(xmlEncFile);
        validator.validate(new StreamSource((InputStream) (new ByteArrayInputStream(decryptedString.getBytes()))));
        System.out.println("decryptedString: " + decryptedString);
        doc = builder.parse((InputStream) (new ByteArrayInputStream(decryptedString.getBytes())));
        Node rootNode = doc.getFirstChild();
        System.out.println("Root node: " + rootNode.getNodeName());
    }

    public static String getMdPsw(String passwd) throws Exception {
        MessageDigest md;
        md = MessageDigest.getInstance("MD5");
        byte[] md5hash = new byte[32];
        md.update(passwd.getBytes("iso-8859-1"), 0, passwd.length());
        md5hash = md.digest();
        return convertToHex(md5hash);
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private String readFileAsString(File filePath) throws java.io.IOException {
        byte[] buffer = new byte[(int) filePath.length()];
        BufferedInputStream f = new BufferedInputStream(new FileInputStream(filePath));
        f.read(buffer);
        return new String(buffer);
    }

    public void updateXmlSetingsParameters(HashMap<String, String> useWebSettings) {
        Set<?> entries = useWebSettings.entrySet();
        Iterator<?> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<?, ?> me = (Map.Entry<?, ?>) iterator.next();
            setXmlParameter((String) me.getKey(), (String) me.getValue(), useWebSettings.get("webAddress"));
        }
        try {
            writeXmlToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateXmlSetingsParameters(HashMap<String, String> useWebSettings, String pagName) {
        Set<?> entries = useWebSettings.entrySet();
        Iterator<?> iterator = entries.iterator();
        String curentPageName = pagName;
        while (iterator.hasNext()) {
            Map.Entry<?, ?> me = (Map.Entry<?, ?>) iterator.next();
            setXmlParameter((String) me.getKey(), (String) me.getValue(), curentPageName);
            if (((String) me.getKey()).equals("webAddress")) {
                curentPageName = (String) me.getValue();
            }
        }
        try {
            writeXmlToFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashMap<Integer, String> getInstalledWebs() {
        HashMap<Integer, String> installedWebs = new HashMap<Integer, String>();
        NodeList webNodes = doc.getElementsByTagName("tns:web");
        for (int i = 0; i < webNodes.getLength(); i++) {
            Element section = (Element) webNodes.item(i);
            Node title = section.getFirstChild();
            while (title != null && title.getNodeType() != Node.ELEMENT_NODE) title = title.getNextSibling();
            if (title.getNodeName().toString().equals("tns:webAddress")) installedWebs.put(i, title.getFirstChild().getNodeValue().toString());
        }
        return installedWebs;
    }

    public boolean webExistAlready(String updatedPage) {
        boolean isThere = false;
        NodeList webNodes = doc.getElementsByTagName("tns:web");
        if (webNodes.getLength() == 0) {
            isThere = false;
        } else {
            for (int i = 0; i < webNodes.getLength(); i++) {
                Element section = (Element) webNodes.item(i);
                Node title = section.getFirstChild();
                while (title != null && title.getNodeType() != Node.ELEMENT_NODE) title = title.getNextSibling();
                System.out.println(title.getNodeName());
                if (title.getNodeName().toString().equals("tns:webAddress")) if (title.getFirstChild().getNodeValue().toString().equals(updatedPage)) isThere = true;
            }
        }
        return isThere;
    }

    public String getWebPageParameter(String updatedPage, String param) {
        String paramValue = "";
        NodeList webNodes = doc.getElementsByTagName("tns:web");
        if (webNodes.getLength() == 0) {
            paramValue = "";
        } else {
            for (int i = 0; i < webNodes.getLength(); i++) {
                Element section = (Element) webNodes.item(i);
                Node title = section.getFirstChild();
                while (title != null && title.getNodeType() != Node.ELEMENT_NODE) title = title.getNextSibling();
                if (title.getNodeName().toString().equals("tns:webAddress")) if (title.getFirstChild().getNodeValue().toString().equals(updatedPage)) if (section.getElementsByTagName("tns:" + param).item(0).getChildNodes().getLength() > 0) paramValue = section.getElementsByTagName("tns:" + param).item(0).getFirstChild().getNodeValue();
            }
        }
        return paramValue;
    }

    private void setXmlParameter(String xmlTag, String param, String updatedPage) {
        try {
            NodeList webNodes = doc.getElementsByTagName("tns:web");
            if (webNodes.getLength() == 0) {
                createWebProperties(updatedPage);
            }
            webNodes = doc.getElementsByTagName("tns:web");
            Element webToChange = null;
            for (int i = 0; i < webNodes.getLength(); i++) {
                Element section = (Element) webNodes.item(i);
                Node title = section.getFirstChild();
                while (title != null && title.getNodeType() != Node.ELEMENT_NODE) title = title.getNextSibling();
                if (title.getNodeName().toString().equals("tns:webAddress")) if (title.getFirstChild().getNodeValue().toString().equals(updatedPage)) webToChange = section;
            }
            if (webToChange == null) {
                createWebProperties(updatedPage);
                for (int i = 0; i < webNodes.getLength(); i++) {
                    Element section = (Element) webNodes.item(i);
                    Node title = section.getFirstChild();
                    while (title != null && title.getNodeType() != Node.ELEMENT_NODE) title = title.getNextSibling();
                    System.out.println(title.getNodeName());
                    if (title.getNodeName().toString().equals("tns:webAddress")) if (title.getFirstChild().getNodeValue().toString().equals(updatedPage)) webToChange = section;
                    System.out.println(title.getFirstChild().getNodeValue());
                }
            }
            if (webToChange.getElementsByTagName("tns:" + xmlTag).item(0).getChildNodes().getLength() > 0) webToChange.getElementsByTagName("tns:" + xmlTag).item(0).removeChild(webToChange.getElementsByTagName("tns:" + xmlTag).item(0).getFirstChild());
            webToChange.getElementsByTagName("tns:" + xmlTag).item(0).appendChild(doc.createTextNode(param));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeXmlToFile() throws Exception {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        Writer outWriter = new StringWriter();
        StreamResult result = new StreamResult(outWriter);
        transformer.transform(source, result);
        DESKeySpec key = new DESKeySpec(userPsw.getBytes());
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
        CryptString crypt = new CryptString(keyFactory.generateSecret(key));
        String encryptedString = crypt.encryptBase64(outWriter.toString());
        System.out.println(outWriter.toString());
        FileWriter xmlWebSetFileStream = new FileWriter(xmlWebSetFile);
        BufferedWriter out = new BufferedWriter(xmlWebSetFileStream);
        out.write(encryptedString);
        out.close();
    }

    private void createWebProperties(String forWeb) {
        NodeList userNodes = doc.getElementsByTagName("tns:user");
        for (int i = 0; i < userNodes.getLength(); i++) {
            Element section = (Element) userNodes.item(i);
            Element webEl = doc.createElement("tns:web");
            section.appendChild(webEl);
            Element webAddrEl = doc.createElement("tns:webAddress");
            webAddrEl.appendChild(doc.createTextNode(forWeb));
            webEl.appendChild(webAddrEl);
            Element webFtp = doc.createElement("tns:webFtp");
            Element ftpName = doc.createElement("tns:ftpName");
            Element defHomeDir = doc.createElement("tns:defHomeDir");
            Element ftpUserName = doc.createElement("tns:ftpUserName");
            Element ftpPsw = doc.createElement("tns:ftpPsw");
            webFtp.appendChild(ftpName);
            webFtp.appendChild(defHomeDir);
            webFtp.appendChild(ftpUserName);
            webFtp.appendChild(ftpPsw);
            webEl.appendChild(webFtp);
            Element webDb = doc.createElement("tns:webDb");
            Element dbLink = doc.createElement("tns:dbLink");
            Element dbName = doc.createElement("tns:dbName");
            Element dbUser = doc.createElement("tns:dbUser");
            Element dbPsw = doc.createElement("tns:dbPsw");
            webDb.appendChild(dbLink);
            webDb.appendChild(dbName);
            webDb.appendChild(dbUser);
            webDb.appendChild(dbPsw);
            webEl.appendChild(webDb);
        }
    }
}
