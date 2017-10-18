package org.opencdspowered.opencds.indexwriter.writer;

import org.opencdspowered.opencds.indexwriter.main.MainFrame;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.math.BigInteger;
import java.security.MessageDigest;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.opencdspowered.opencds.indexwriter.util.Constants;

/**
 * The class that writes our XML.
 *
 * @author  Lars 'Levia' Wesselius
*/
public class XMLWriter {

    private MainFrame m_MainFrame;

    private File m_RootFolder = null;

    private File m_Destination = null;

    private Document m_Document = null;

    public XMLWriter(MainFrame frame) {
        m_MainFrame = frame;
    }

    public void setRootFolder(File file) {
        m_RootFolder = file;
    }

    public void setDestination(File file) {
        m_Destination = file;
    }

    public void write() {
        if (m_RootFolder == null) {
            m_MainFrame.log("You did not choose a root folder!", "red");
            return;
        }
        if (m_Destination == null) {
            m_MainFrame.log("You did not choose a destination!", "red");
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            m_Document = builder.getDOMImplementation().createDocument(null, null, null);
            Element root = m_Document.createElement("indexes");
            root.setAttribute("version", Constants.OPENCDS_VERSION);
            m_Document.appendChild(root);
            doDirectory(root, m_RootFolder);
            DOMSource domSource = new DOMSource(m_Document);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StreamResult sr = new StreamResult(m_Destination);
            transformer.transform(domSource, sr);
        } catch (Exception e) {
            m_MainFrame.log(e.getMessage(), "error");
            return;
        }
        m_Document = null;
    }

    public void doDirectory(Element elem, File dir) {
        File[] subFiles = dir.listFiles();
        Element dirElem = m_Document.createElement("folder");
        dirElem.setAttribute("name", dir.getName());
        elem.appendChild(dirElem);
        for (int i = 0; i != subFiles.length; ++i) {
            File file = subFiles[i];
            if (file.isDirectory()) {
                doDirectory(dirElem, file);
            } else {
                doFile(dirElem, file);
            }
        }
    }

    public void doFile(Element elem, File file) {
        Element fileElem = m_Document.createElement("file");
        fileElem.setAttribute("file", file.getName());
        fileElem.setAttribute("hash", getMD5FromFile(file));
        fileElem.setAttribute("size", String.valueOf(file.length()));
        elem.appendChild(fileElem);
    }

    public String getMD5FromFile(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            InputStream is = new FileInputStream(file);
            byte[] buffer = new byte[8192];
            int read = 0;
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            is.close();
            return output;
        } catch (Exception e) {
            m_MainFrame.log(e.getMessage(), "error");
        }
        return null;
    }
}
