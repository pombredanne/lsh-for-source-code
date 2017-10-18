package au.gov.naa.digipres.xena.kernel.metadatawrapper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;
import au.gov.naa.digipres.xena.javatools.FileName;
import au.gov.naa.digipres.xena.kernel.MultiInputSource;
import au.gov.naa.digipres.xena.kernel.XenaException;
import au.gov.naa.digipres.xena.kernel.XenaInputSource;
import au.gov.naa.digipres.xena.kernel.metadata.AbstractMetaData;
import au.gov.naa.digipres.xena.kernel.metadata.MetaDataManager;
import au.gov.naa.digipres.xena.kernel.metadata.XenaMetaData;
import au.gov.naa.digipres.xena.kernel.normalise.AbstractNormaliser;
import au.gov.naa.digipres.xena.util.TagContentFinder;
import au.gov.naa.digipres.xena.util.UrlEncoder;

public class DefaultWrapper extends AbstractMetaDataWrapper {

    private ContentHandler checksumHandler;

    private ByteArrayOutputStream checksumBAOS;

    private MessageDigest digest;

    private OutputStreamWriter checksumOSW;

    private boolean startedChecksumming = false;

    private SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private static final String DEFAULTWRAPPER = "Default Package Wrapper";

    @Override
    public String getName() {
        return DEFAULTWRAPPER;
    }

    @Override
    public String toString() {
        return "Xena Default XML Wrapper";
    }

    @Override
    public String getOpeningTag() {
        return TagNames.XENA;
    }

    @Override
    public String getSourceId(XenaInputSource input) throws XenaException {
        return TagContentFinder.getTagContents(input, XenaMetaData.INPUT_SOURCE_URI_TAG);
    }

    @Override
    public String getSourceName(XenaInputSource input) throws XenaException {
        return TagContentFinder.getTagContents(input, XenaMetaData.INPUT_SOURCE_URI_TAG);
    }

    @Override
    public void startDocument() throws SAXException {
        XMLReader normaliser = (XMLReader) getProperty("http://xena/normaliser");
        if (normaliser == null) {
            throw new SAXException("http://xena/normaliser is not set for Package Wrapper");
        }
        XenaInputSource xis = (XenaInputSource) getProperty("http://xena/input");
        AbstractMetaData xenaMetaData = ((MetaDataManager) getProperty("http://xena/metaManager")).getXenaMetaData();
        File outfile = ((File) getProperty("http://xena/file"));
        xenaMetaData.setProperty("http://xena/normaliser", getProperty("http://xena/normaliser"));
        xenaMetaData.setProperty("http://xena/input", getProperty("http://xena/input"));
        String fileName;
        char[] id;
        boolean isBinary = normaliser.getClass().getName().equals("au.gov.naa.digipres.xena.plugin.basic.BinaryToXenaBinaryNormaliser");
        super.startDocument();
        ContentHandler th = getContentHandler();
        AttributesImpl att = new AttributesImpl();
        th.startElement(TagNames.XENA_URI, TagNames.XENA, TagNames.XENA, att);
        th.startElement(TagNames.XENA_URI, TagNames.META, TagNames.XENA_META, att);
        xenaMetaData.setContentHandler(this);
        try {
            xenaMetaData.parse(xis);
        } catch (IOException e) {
            throw new SAXException(e);
        }
        th.endElement(TagNames.XENA_URI, TagNames.META, TagNames.XENA_META);
        th.startElement(TagNames.WRAPPER_URI, TagNames.SIGNED_AIP, TagNames.WRAPPER_SIGNED_AIP, att);
        th.startElement(TagNames.WRAPPER_URI, TagNames.AIP, TagNames.WRAPPER_AIP, att);
        th.startElement(TagNames.PACKAGE_URI, TagNames.PACKAGE, TagNames.PACKAGE_PACKAGE, att);
        th.startElement(TagNames.PACKAGE_URI, TagNames.META, TagNames.PACKAGE_META, att);
        th.startElement(TagNames.NAA_URI, TagNames.WRAPPER, TagNames.NAA_WRAPPER, att);
        th.characters(TagNames.NAA_PACKAGE.toCharArray(), 0, TagNames.NAA_PACKAGE.toCharArray().length);
        th.endElement(TagNames.NAA_URI, TagNames.WRAPPER, TagNames.NAA_WRAPPER);
        th.startElement(TagNames.DCTERMS_URI, TagNames.CREATED, TagNames.DCCREATED, att);
        char[] sDate = isoDateFormat.format(new java.util.Date(System.currentTimeMillis())).toCharArray();
        th.characters(sDate, 0, sDate.length);
        th.endElement(TagNames.DCTERMS_URI, TagNames.CREATED, TagNames.DCCREATED);
        if (xis.getFile() != null || outfile != null) {
            if (outfile != null) {
                th.startElement(TagNames.DC_URI, TagNames.IDENTIFIER, TagNames.DCIDENTIFIER, att);
                fileName = xis.getOutputFileName().substring(0, xis.getOutputFileName().lastIndexOf('.'));
                id = fileName.toCharArray();
                th.characters(id, 0, id.length);
                th.endElement(TagNames.DC_URI, TagNames.IDENTIFIER, TagNames.DCIDENTIFIER);
            }
            th.startElement(TagNames.NAA_URI, TagNames.DATASOURCES, TagNames.NAA_DATASOURCES, att);
            {
                List<XenaInputSource> xenaInputSourceList = new ArrayList<XenaInputSource>();
                if (xis instanceof MultiInputSource) {
                    Iterator it = ((MultiInputSource) xis).getSystemIds().iterator();
                    while (it.hasNext()) {
                        String url = (String) it.next();
                        xenaInputSourceList.add(new XenaInputSource(url, null));
                    }
                } else {
                    xenaInputSourceList.add(xis);
                }
                Iterator it = xenaInputSourceList.iterator();
                while (it.hasNext()) {
                    XenaInputSource source = (XenaInputSource) it.next();
                    th.startElement(TagNames.NAA_URI, TagNames.DATASOURCE, TagNames.NAA_DATASOURCE, att);
                    XenaInputSource relsource = null;
                    try {
                        java.net.URI uri = new java.net.URI(source.getSystemId());
                        if (uri.getScheme().equals("file")) {
                            File file = new File(uri);
                            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss");
                            char[] lastModStr = sdf.format(new Date(file.lastModified())).toCharArray();
                            th.startElement(TagNames.NAA_URI, TagNames.LASTMODIFIED, TagNames.NAA_LASTMODIFIED, att);
                            th.characters(lastModStr, 0, lastModStr.length);
                            th.endElement(TagNames.NAA_URI, TagNames.LASTMODIFIED, TagNames.NAA_LASTMODIFIED);
                            String relativePath = null;
                            File baseDir;
                            if (getMetaDataWrapperManager().getPluginManager().getMetaDataWrapperManager().getBasePathName() != null) {
                                try {
                                    baseDir = new File(getMetaDataWrapperManager().getPluginManager().getMetaDataWrapperManager().getBasePathName());
                                    if (baseDir != null) {
                                        relativePath = FileName.relativeTo(baseDir, file);
                                    }
                                } catch (IOException iox) {
                                    relativePath = null;
                                }
                            }
                            if (relativePath == null) {
                                relativePath = file.getName();
                            }
                            String encodedPath = null;
                            try {
                                encodedPath = UrlEncoder.encode(relativePath);
                            } catch (UnsupportedEncodingException x) {
                                throw new SAXException(x);
                            }
                            relsource = new XenaInputSource(new java.net.URI("file:/" + encodedPath).toASCIIString(), null);
                        } else {
                            relsource = source;
                        }
                    } catch (java.net.URISyntaxException x) {
                        x.printStackTrace();
                    }
                    th.startElement(TagNames.DC_URI, TagNames.SOURCE, TagNames.DCSOURCE, att);
                    char[] src = relsource.getSystemId().toCharArray();
                    th.characters(src, 0, src.length);
                    th.endElement(TagNames.DC_URI, TagNames.SOURCE, TagNames.DCSOURCE);
                    if (isBinary) {
                        char[] typename = "binary data".toCharArray();
                        th.startElement(TagNames.NAA_URI, TagNames.TYPE, TagNames.NAA_TYPE, att);
                        th.characters(typename, 0, typename.length);
                        th.endElement(TagNames.NAA_URI, TagNames.TYPE, TagNames.NAA_TYPE);
                    }
                    th.endElement(TagNames.NAA_URI, TagNames.DATASOURCE, TagNames.NAA_DATASOURCE);
                }
            }
            th.endElement(TagNames.NAA_URI, TagNames.DATASOURCES, TagNames.NAA_DATASOURCES);
        }
        th.endElement(TagNames.PACKAGE_URI, TagNames.META, TagNames.PACKAGE_META);
        startElement(TagNames.PACKAGE_URI, TagNames.CONTENT, TagNames.PACKAGE_CONTENT, att);
    }

    @Override
    public void endDocument() throws org.xml.sax.SAXException {
        ContentHandler th = getContentHandler();
        endElement(TagNames.PACKAGE_URI, TagNames.CONTENT, TagNames.PACKAGE_CONTENT);
        endElement(TagNames.PACKAGE_URI, TagNames.PACKAGE, TagNames.PACKAGE_PACKAGE);
        MetaDataManager metaDataManager = (MetaDataManager) getProperty("http://xena/metaManager");
        AbstractMetaData defaultMetaData = metaDataManager.getDefaultMetaData();
        defaultMetaData.setContentHandler(this);
        AbstractNormaliser normaliser = (AbstractNormaliser) getProperty("http://xena/normaliser");
        if (normaliser != null) {
            defaultMetaData.setProperty("http://xena/exported_digest", normaliser.getProperty("http://xena/exported_digest"));
            defaultMetaData.setProperty("http://xena/exported_digest_comment", normaliser.getProperty("http://xena/exported_digest_comment"));
        } else {
            defaultMetaData.setProperty("http://xena/exported_digest", "");
        }
        defaultMetaData.setProperty("http://xena/digest", getProperty("http://xena/digest"));
        XenaInputSource xis = (XenaInputSource) getProperty("http://xena/input");
        defaultMetaData.setProperty("http://xena/input", xis);
        try {
            defaultMetaData.parse(xis);
            metaDataManager.parseMetaDataObjects(this, xis);
        } catch (IOException e) {
            throw new SAXException(e);
        } catch (XenaException e) {
            throw new SAXException(e);
        }
        th.endElement(TagNames.WRAPPER_URI, TagNames.AIP, TagNames.WRAPPER_AIP);
        th.endElement(TagNames.WRAPPER_URI, TagNames.SIGNED_AIP, TagNames.WRAPPER_SIGNED_AIP);
        th.endElement(TagNames.XENA_URI, TagNames.XENA, TagNames.XENA);
        super.endDocument();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
        if (startedChecksumming) {
            checksumHandler.characters(ch, start, length);
            try {
                checksumOSW.flush();
                checksumBAOS.flush();
                digest.update(checksumBAOS.toByteArray());
                checksumBAOS.reset();
            } catch (IOException iex) {
                throw new SAXException("Problem updating checksum", iex);
            }
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
        if ((uri.equals(TagNames.PACKAGE_URI) && (localName.equals(TagNames.CONTENT)) && (qName.equals(TagNames.PACKAGE_CONTENT)))) {
            startedChecksumming = false;
            setProperty("http://xena/digest", convertToHex(digest.digest()));
            try {
                if (checksumBAOS != null) {
                    checksumBAOS.close();
                }
                if (checksumOSW != null) {
                    checksumOSW.close();
                }
            } catch (IOException e) {
                throw new SAXException("Could not close checksum streams", e);
            }
        }
        if (startedChecksumming) {
            checksumHandler.endElement(uri, localName, qName);
            try {
                checksumOSW.flush();
                checksumBAOS.flush();
                digest.update(checksumBAOS.toByteArray());
                checksumBAOS.reset();
            } catch (IOException iex) {
                throw new SAXException("Problem updating checksum", iex);
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);
        if (startedChecksumming) {
            checksumHandler.startElement(uri, localName, qName, atts);
            try {
                checksumOSW.flush();
                checksumBAOS.flush();
                digest.update(checksumBAOS.toByteArray());
                checksumBAOS.reset();
            } catch (IOException iex) {
                throw new SAXException("Problem updating checksum", iex);
            }
        }
        if ((uri.equals(TagNames.PACKAGE_URI) && (localName.equals(TagNames.CONTENT)) && (qName.equals(TagNames.PACKAGE_CONTENT)))) {
            startedChecksumming = true;
            try {
                checksumBAOS = new ByteArrayOutputStream();
                checksumHandler = createChecksumHandler(checksumBAOS);
                digest = MessageDigest.getInstance(TagNames.DEFAULT_CHECKSUM_ALGORITHM);
            } catch (Exception e) {
                throw new SAXException("Could not create checksum handler", e);
            }
        }
    }

    private ContentHandler createChecksumHandler(ByteArrayOutputStream baos) throws IOException, TransformerException {
        TransformerHandler transformerHandler = null;
        SAXTransformerFactory transformFactory = (SAXTransformerFactory) TransformerFactory.newInstance();
        transformerHandler = transformFactory.newTransformerHandler();
        checksumOSW = new OutputStreamWriter(baos, "UTF-8");
        StreamResult streamResult = new StreamResult(checksumOSW);
        transformerHandler.setResult(streamResult);
        transformerHandler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        return transformerHandler;
    }

    private static String convertToHex(byte[] byteArray) {
        String s;
        String hexString = "";
        for (byte element : byteArray) {
            s = Integer.toHexString(element & 0xFF);
            if (s.length() == 1) {
                s = "0" + s;
            }
            hexString = hexString + s;
        }
        return hexString;
    }
}
