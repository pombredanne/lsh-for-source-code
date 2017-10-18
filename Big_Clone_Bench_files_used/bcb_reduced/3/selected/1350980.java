package org.dcm4chee.xds.repository.mbean;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import junit.framework.TestCase;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4chee.xds.repository.XDSDocumentWriter;
import org.dcm4chee.xds.repository.XDSDocumentWriterFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class Store2DcmTest extends TestCase {

    private static final XDSDocumentWriterFactory fac = XDSDocumentWriterFactory.getInstance();

    private static File locateFile(String name) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        return new File(cl.getResource(name).toString().substring(5));
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(Store2DcmTest.class);
    }

    public Store2DcmTest(String arg0) {
        super(arg0);
    }

    public final void testEncapsulateSimple() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        File docFile = locateFile("test.txt");
        XDSDocumentWriter docWriter = fac.getDocumentWriter(docFile);
        File f = new File("target/test-out/EncapsulateSimple.dcm");
        f.getParentFile().mkdirs();
        DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        Store2Dcm store = new Store2Dcm(null, docWriter, null, (File) null);
        byte[] digest = store.encapsulate(dos, true);
        DicomInputStream dis = new DicomInputStream(f);
        DicomObject obj = dis.readDicomObject();
        checkBasicAttributes(obj, digest);
    }

    public final void testEncapsulateXml() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        File docFile = locateFile("test.txt");
        File xmlFile = locateFile("patinfo.xml");
        XDSDocumentWriter docWriter = fac.getDocumentWriter(docFile);
        File f = new File("target/test-out/EncapsulatePatInfo.dcm");
        f.getParentFile().mkdirs();
        DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        Store2Dcm store = new Store2Dcm(xmlFile, docWriter, null, (File) null);
        byte[] digest = store.encapsulate(dos, true);
        DicomInputStream dis = new DicomInputStream(f);
        DicomObject obj = dis.readDicomObject();
        checkBasicAttributes(obj, digest);
        assertEquals("Patient ID incorrect!", "1234", obj.getString(Tag.PatientID));
        assertEquals("Patient Name incorrect!", "Homer J.^Simpson", obj.getString(Tag.PatientName));
        assertEquals("Patient Birthdate incorrect!", "19841231", obj.getString(Tag.PatientBirthDate));
        assertEquals("Patient Sex incorrect!", "M", obj.getString(Tag.PatientSex));
    }

    public final void testEncapsulateXsl() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        checkEncapsulateXsl("test.txt", "test1_subm.xml", "test1_subm.xsl");
    }

    public final void testEncapsulateXslLeafRegistry() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        checkEncapsulateXsl("test.txt", "test1_LeafRegistryObjectList.xml", "test1_subm.xsl");
    }

    public final void testEncapsulateXslExtrinsic() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        checkEncapsulateXsl("test.txt", "test1_ExtrinsicObject.xml", "test1_subm.xsl");
    }

    public final void testEncapsulateXslFull() throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        DicomObject obj = checkEncapsulateXsl("test.txt", "test1_subm.xml", "test_full_subm.xsl");
        assertEquals("StudyDate incorrect!", "20051224", obj.getString(Tag.StudyDate));
        assertEquals("SeriesDate incorrect!", "20051224", obj.getString(Tag.SeriesDate));
        assertEquals("ContentDate incorrect!", "20051224", obj.getString(Tag.ContentDate));
        DicomElement elem = obj.get(Tag.ConceptNameCodeSequence);
        assertNotNull("Missing ConceptNameCodeSequence !", elem);
        assertTrue("ConceptNameCodeSequence is empty", !elem.isEmpty());
        DicomObject item = elem.getDicomObject();
        assertEquals("CodeValue incorrect!", "34098-4", item.getString(Tag.CodeValue));
        assertEquals("CodeMeaning incorrect!", "Conference Evaluation Note", item.getString(Tag.CodeMeaning));
        assertEquals("CodingSchemeDesignator incorrect!", "LOINC", item.getString(Tag.CodingSchemeDesignator));
    }

    public final void testEncapsulateXslSOAP() throws ParserConfigurationException, SAXException, IOException, SOAPException, TransformerException, NoSuchAlgorithmException {
        File docFile = locateFile("test.txt");
        XDSDocumentWriter docWriter = fac.getDocumentWriter(docFile);
        File submFile = locateFile("test1_subm.xml");
        File xslFile = locateFile("test1_subm.xsl");
        Document d = this.readXMLFile(submFile);
        Node submSetNode = d.getElementsByTagNameNS("urn:oasis:names:tc:ebxml-regrep:registry:xsd:2.1", "SubmitObjectsRequest").item(0);
        MessageFactory messageFactory = MessageFactory.newInstance();
        SOAPMessage msg = messageFactory.createMessage();
        SOAPEnvelope envelope = msg.getSOAPPart().getEnvelope();
        SOAPBody soapBody = envelope.getBody();
        SOAPElement bodyElement = soapBody.addBodyElement(envelope.createName("SubmitObjectsRequest", "rs", "urn:oasis:names:tc:ebxml-regrep:registry:xsd:2.1"));
        bodyElement.appendChild(bodyElement.getOwnerDocument().importNode(submSetNode, true));
        SOAPBody body = msg.getSOAPBody();
        Document doc = body.getOwnerDocument();
        DOMSource domSrc = new DOMSource(doc);
        File f = new File("target/test-out/EncapsulateSOAP.dcm");
        f.getParentFile().mkdirs();
        DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        Store2Dcm store = new Store2Dcm(null, docWriter, domSrc, xslFile);
        byte[] digest = store.encapsulate(dos, true);
        DicomInputStream dis = new DicomInputStream(f);
        DicomObject obj = dis.readDicomObject();
        checkBasicAttributes(obj, digest);
        assertEquals("PatientID incorrect!", "pid1", obj.getString(Tag.PatientID));
    }

    private final DicomObject checkEncapsulateXsl(String docFileName, String submFilename, String xslFilename) throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        File docFile = locateFile(docFileName);
        File submFile = locateFile(submFilename);
        File xslFile = locateFile(xslFilename);
        XDSDocumentWriter docWriter = fac.getDocumentWriter(docFile);
        FileInputStream submStream = new FileInputStream(submFile);
        File f = new File("target/test-out/EncapsulateXsl.dcm");
        f.getParentFile().mkdirs();
        DicomOutputStream dos = new DicomOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
        Store2Dcm store = new Store2Dcm(null, docWriter, new StreamSource(submStream), xslFile);
        byte[] digest = store.encapsulate(dos, true);
        DicomInputStream dis = new DicomInputStream(f);
        DicomObject obj = dis.readDicomObject();
        checkBasicAttributes(obj, digest);
        assertEquals("PatientID incorrect!", "pid1", obj.getString(Tag.PatientID));
        assertEquals("Issuer of PatientID incorrect!", "domain", obj.getString(Tag.IssuerOfPatientID));
        assertEquals("Patients Name incorrect!", "Doe^John", obj.getString(Tag.PatientName));
        assertEquals("Patients Birthdate incorrect!", "19560527", obj.getString(Tag.PatientBirthDate));
        assertEquals("Patients Sex incorrect!", "M", obj.getString(Tag.PatientSex));
        DicomObject item = obj.get(Tag.OtherPatientIDsSequence).getDicomObject();
        assertEquals("OtherPatientIDs Sequence: PatientID incorrect!", "NIST-1", item.getString(Tag.PatientID));
        assertEquals("OtherPatientIDs Sequence: Issuer incorrect!", "&1.3.6.1.4.1.21367.2005.3.7&ISO", item.getString(Tag.IssuerOfPatientID));
        assertEquals("SOP Instance UID incorrect!", "1.2.40.0.13.1.172.25.13.160.2765838.20071002142226890.18", obj.getString(Tag.SOPInstanceUID));
        assertEquals("MimeType incorrect!", "text/xml", obj.getString(Tag.MIMETypeOfEncapsulatedDocument));
        return obj;
    }

    private void checkBasicAttributes(DicomObject obj, byte[] digest) throws NoSuchAlgorithmException {
        DicomElement elem = obj.get(Tag.SOPInstanceUID);
        assertNotNull("Missing SOP Instance UID !", elem);
        assertTrue("SOP Instance UID is empty", !elem.isEmpty());
        assertNotNull("Missing Series Instance UID !", elem = obj.get(Tag.SeriesInstanceUID));
        assertTrue("Series Instance UID is empty", !elem.isEmpty());
        assertNotNull("Missing Study Instance UID !", elem = obj.get(Tag.StudyInstanceUID));
        assertTrue("Study Instance UID is empty", !elem.isEmpty());
        assertNotNull("Missing SOP Class UID !", elem = obj.get(Tag.SOPClassUID));
        assertTrue("SOP Class UID is empty", !elem.isEmpty());
        assertNotNull("Missing SpecificCharacterSet !", elem = obj.get(Tag.SpecificCharacterSet));
        assertTrue("SpecificCharacterSet is empty", !elem.isEmpty());
        assertNotNull("Missing ConceptNameCodeSequence !", elem = obj.get(Tag.ConceptNameCodeSequence));
        assertNotNull("Missing InstanceCreationDate !", elem = obj.get(Tag.InstanceCreationDate));
        assertTrue("InstanceCreationDate is empty", !elem.isEmpty());
        assertNotNull("Missing InstanceCreationTime !", elem = obj.get(Tag.InstanceCreationTime));
        assertTrue("InstanceCreationTime is empty", !elem.isEmpty());
        byte[] ba = obj.getBytes(Tag.EncapsulatedDocument);
        assertEquals("EncapsulatedDocument incorrect!", "1234567890", new String(ba));
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(ba);
        assertTrue("SHA1 Digest not equal!", Arrays.equals(md.digest(), digest));
    }

    private Document readXMLFile(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        dbFactory.setNamespaceAware(true);
        DocumentBuilder builder = dbFactory.newDocumentBuilder();
        return builder.parse(xmlFile);
    }
}
