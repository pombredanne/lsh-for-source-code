package au.gov.naa.digipres.xena.plugin.naa;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Wrapper for an AIP. This wrapper is only used once per AIP, and is placed at the outermost level
 * of the XML file. Its purpose is to contain the signature tag - a checksum produced from the content
 * contained within this wrapper.
 *
 * @author Justin Waddell
 */
public class NaaOuterWrapNormaliser extends XMLFilterImpl {

    public static final String DEFAULT_CHECKSUM_ALGORITHM = "SHA-512";

    private ContentHandler checksumHandler;

    private ByteArrayOutputStream checksumBAOS;

    private MessageDigest digest;

    private OutputStreamWriter checksumOSW;

    private String description = "This checksum is created from the entire contents of the " + NaaTagNames.WRAPPER_AIP + " tag, not including the tag itself";

    @Override
    public String toString() {
        return "NAA Package Wrap Outer";
    }

    /**
	 * Opens the signed-aip and aip tags, and intialises the checksum producing system.
	 */
    @Override
    public void startDocument() throws org.xml.sax.SAXException {
        super.startDocument();
        ContentHandler th = getContentHandler();
        AttributesImpl att = new AttributesImpl();
        th.startElement(NaaTagNames.WRAPPER_URI, NaaTagNames.SIGNED_AIP, NaaTagNames.WRAPPER_SIGNED_AIP, att);
        th.startElement(NaaTagNames.WRAPPER_URI, NaaTagNames.AIP, NaaTagNames.WRAPPER_AIP, att);
        try {
            checksumBAOS = new ByteArrayOutputStream();
            checksumHandler = createChecksumHandler(checksumBAOS);
            digest = MessageDigest.getInstance(DEFAULT_CHECKSUM_ALGORITHM);
        } catch (Exception e) {
            throw new SAXException("Could not create checksum handler", e);
        }
    }

    /**
	 * Calculates the checksum and writes it to the signature tag.
	 */
    @Override
    public void endDocument() throws org.xml.sax.SAXException {
        ContentHandler th = getContentHandler();
        th.endElement(NaaTagNames.WRAPPER_URI, NaaTagNames.AIP, NaaTagNames.WRAPPER_AIP);
        if (digest != null) {
            checksumHandler.endDocument();
            AttributesImpl atts = new AttributesImpl();
            th.startElement(NaaTagNames.WRAPPER_URI, NaaTagNames.META, NaaTagNames.WRAPPER_META, atts);
            atts.addAttribute(NaaTagNames.WRAPPER_URI, "description", NaaTagNames.WRAPPER_PREFIX + ":description", "CDATA", description);
            atts.addAttribute(NaaTagNames.WRAPPER_URI, "algorithm", NaaTagNames.WRAPPER_PREFIX + ":algorithm", "CDATA", DEFAULT_CHECKSUM_ALGORITHM);
            th.startElement(NaaTagNames.WRAPPER_URI, NaaTagNames.SIGNATURE, NaaTagNames.WRAPPER_SIGNATURE, atts);
            char[] signatureVal = convertToHex(digest.digest()).toCharArray();
            th.characters(signatureVal, 0, signatureVal.length);
            th.endElement(NaaTagNames.WRAPPER_URI, NaaTagNames.SIGNATURE, NaaTagNames.WRAPPER_SIGNATURE);
            th.endElement(NaaTagNames.WRAPPER_URI, NaaTagNames.META, NaaTagNames.WRAPPER_META);
        }
        th.endElement(NaaTagNames.WRAPPER_URI, NaaTagNames.SIGNED_AIP, NaaTagNames.WRAPPER_SIGNED_AIP);
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
        super.endDocument();
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        super.characters(ch, start, length);
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

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        super.endElement(uri, localName, qName);
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

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        super.startElement(uri, localName, qName, atts);
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
