package org.dcm4chee.xds.repository.mbean;

import java.io.File;
import java.io.IOException;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamSource;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.UID;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.ContentHandlerAdapter;
import org.dcm4che2.io.DicomOutputStream;
import org.dcm4che2.util.UIDUtils;
import org.dcm4chee.xds.repository.XDSDocumentWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

public class Store2Dcm {

    public static final String MIME_PDF = "application/pdf";

    public static final String MIME_OCTET = "application/octet-stream";

    public static final String KEY_DEFAULT_CUID = "default";

    public static final String ENCAPSULATED_DOCUMENT_STORAGE_CUID = "1.2.40.0.13.1.5.1.4.1.1.104.1";

    private String transferSyntax = UID.ExplicitVRLittleEndian;

    private static Map mime2CuidMap = new LinkedHashMap();

    private String charset = "ISO_IR 100";

    private File xmlFile;

    private XDSDocumentWriter docWriter;

    private Source metadata;

    private Source xslSource;

    private int rspStatus;

    private boolean stored = false;

    private boolean committed = false;

    private DicomObject attrs;

    private static Logger log = LoggerFactory.getLogger(Store2Dcm.class);

    public Store2Dcm(File xmlFile, XDSDocumentWriter docWriter, Source metadata, File xslFile) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        this.xmlFile = xmlFile;
        this.docWriter = docWriter;
        this.metadata = metadata;
        if (xslFile != null) xslSource = new StreamSource(xslFile);
        init();
    }

    public Store2Dcm(File xmlFile, XDSDocumentWriter docWriter, Source metadata, Source xslSource) throws ParserConfigurationException, SAXException, IOException, TransformerException {
        this.xmlFile = xmlFile;
        this.docWriter = docWriter;
        this.metadata = metadata;
        this.xslSource = xslSource;
        init();
    }

    private void init() throws ParserConfigurationException, SAXException, IOException, TransformerException {
        if (!mime2CuidMap.containsKey(KEY_DEFAULT_CUID)) {
            mime2CuidMap.put(KEY_DEFAULT_CUID, ENCAPSULATED_DOCUMENT_STORAGE_CUID);
        }
        attrs = new BasicDicomObject();
        ContentHandlerAdapter ch = new ContentHandlerAdapter(attrs);
        if (xmlFile != null) {
            SAXParserFactory f = SAXParserFactory.newInstance();
            SAXParser p = f.newSAXParser();
            p.parse(xmlFile, ch);
        }
        if (metadata != null && xslSource != null) {
            SAXTransformerFactory tf = (SAXTransformerFactory) TransformerFactory.newInstance();
            Templates templates = tf.newTemplates(xslSource);
            TransformerHandler th = tf.newTransformerHandler(templates);
            Transformer t = th.getTransformer();
            t.transform(metadata, new SAXResult(ch));
        }
        addMissingAttributes(attrs);
        attrs.initFileMetaInformation(transferSyntax);
    }

    public static Map getMime2CuidMap() {
        return mime2CuidMap;
    }

    public String getTransferSyntax() {
        return transferSyntax;
    }

    public String getSOPClassUID() {
        return attrs.getString(Tag.SOPClassUID);
    }

    public String getSOPInstanceUID() {
        return attrs.getString(Tag.SOPInstanceUID);
    }

    public int size() {
        return docWriter.size();
    }

    public int getRspStatus() {
        return rspStatus;
    }

    public void setRspStatus(int rspStatus) {
        this.rspStatus = rspStatus;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }

    public boolean isCommitted() {
        return committed;
    }

    public void setCommitted(boolean committed) {
        this.committed = committed;
    }

    public byte[] encapsulate(DicomOutputStream dos, boolean writeFMI) throws IOException, ParserConfigurationException, SAXException, TransformerException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        DigestOutputStream dgstos = new DigestOutputStream(dos, md);
        dgstos.on(false);
        byte[] hash = null;
        try {
            int docLen = docWriter.size();
            if (writeFMI) dos.writeFileMetaInformation(attrs);
            dos.writeDataset(attrs.subSet(Tag.SpecificCharacterSet, Tag.EncapsulatedDocument), transferSyntax);
            log.info("Encapsulated Document length:" + docLen);
            dos.writeHeader(Tag.EncapsulatedDocument, VR.OB, (docLen + 1) & ~1);
            dgstos.on(true);
            docWriter.writeTo(dgstos);
            dgstos.on(false);
            if ((docLen & 1) != 0) {
                dos.write(0);
            }
            dgstos.on(false);
            dos.writeDataset(attrs.subSet(Tag.EncapsulatedDocument, -1), transferSyntax);
        } finally {
            dgstos.close();
            docWriter.close();
        }
        hash = md.digest();
        return hash;
    }

    private void addMissingAttributes(DicomObject attrs) {
        ensureUID(attrs, Tag.StudyInstanceUID);
        ensureUID(attrs, Tag.SeriesInstanceUID);
        ensureUID(attrs, Tag.SOPInstanceUID);
        String mime = attrs.getString(Tag.MIMETypeOfEncapsulatedDocument);
        if (mime == null) {
            mime = MIME_OCTET;
        }
        if (attrs.get(Tag.SOPClassUID) == null) attrs.putString(Tag.SOPClassUID, VR.UI, getCuidForMime(mime));
        if (attrs.get(Tag.SpecificCharacterSet) == null) attrs.putString(Tag.SpecificCharacterSet, VR.CS, charset);
        if (attrs.get(Tag.Modality) == null) attrs.putString(Tag.Modality, VR.CS, "XDS");
        if (attrs.get(Tag.ConceptNameCodeSequence) == null) {
            attrs.putSequence(Tag.ConceptNameCodeSequence);
        }
        if (attrs.getDate(Tag.InstanceCreationDate) == null) {
            Date now = new Date();
            attrs.putDate(Tag.InstanceCreationDate, VR.DA, now);
            attrs.putDate(Tag.InstanceCreationTime, VR.TM, now);
        }
    }

    private String getCuidForMime(String mime) {
        String cuid = (String) mime2CuidMap.get(mime);
        if (cuid == null) cuid = (String) mime2CuidMap.get(KEY_DEFAULT_CUID);
        return isDigit(cuid.charAt(0)) ? cuid : UID.forName(cuid);
    }

    private void ensureUID(DicomObject attrs, int tag) {
        if (!attrs.containsValue(tag)) {
            attrs.putString(tag, VR.UI, UIDUtils.createUID());
        }
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
}
