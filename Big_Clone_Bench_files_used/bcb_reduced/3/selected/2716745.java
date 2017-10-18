package org.qtitools.mathqurate.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentItemType;
import org.qtitools.mathqurate.utilities.PrefsHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

/**
 * The Class MQAssessmentItem. 
 * Creates a model of the assessment item
 * for the Mathqurate model. Justs adds some basic meta-data
 * 
 * @author James Annesley and Paul Neve
 */
public class MQContentPackage extends MQHelper {

    /** The assessmentItem. */
    private AssessmentItemType assessmentItemType;

    /** metadata */
    private LinkedHashMap<String, String> extendedMetadata = new LinkedHashMap<String, String>();

    /**
	 * Instantiates a new mQ content package, given a QTI assessmentItem
	 * 
	 * @param assessmentItemType the assessment item
	 * 
	 */
    public MQContentPackage(AssessmentItemType assessmentItemType) {
        assessmentItemType.setToolName("Mathqurate");
        assessmentItemType.setToolVersion("1.0");
        this.contentXML = MQModel.mathqurateObjectFactory.getTypeAsXML(assessmentItemType);
        this.set("url", PrefsHelper.getMinibixUrl() + ":" + PrefsHelper.getMinibixPort());
        this.assessmentItemType = MQModel.mathqurateObjectFactory.createAssessmentType(contentXML);
        populateMdFields();
    }

    @Override
    public String getContentXML() {
        this.contentXML = MQModel.mathqurateObjectFactory.getTypeAsXML(assessmentItemType);
        return this.contentXML;
    }

    /**
	 * Instantiates a new mQ assessment item and populate its metadata with explicitly
	 * passed strings. (PN: not quite sure where this is used actually? Seems obsolete
	 * given the new metadata approach...)
	 * 
	 * @param assessmentItemType the assessment item type
	 * @param contentXML the content xml or html
	 * @param author the author
	 * @param datetime the date and time
	 * @param description the minibix item description
	 * @param taxon the MathAssess taxon
	 * @param ticket the minibix item ticket
	 * @param url the minibix url
	 * 
	 */
    public MQContentPackage(String contentXML, String author, String datetime, String description, String taxon, String ticket, String url) {
        this.contentXML = contentXML;
        this.set(MQMetadata.AUTHOR[0], author);
        this.set(MQMetadata.DESCRIPTION[0], description);
        this.set(MQMetadata.TAXON[0], taxon);
        this.set(MQMetadata.TICKET[0], ticket);
        this.set(MQMetadata.URL[0], url);
        this.assessmentItemType = MQModel.mathqurateObjectFactory.createAssessmentType(contentXML);
        populateMdFields();
    }

    /**
	 * Instantiates a new mQ content package from an existing one.
	 * 
	 * @param assessmentItem the Mathqurate assessment item
	 * 
	 */
    public MQContentPackage(MQContentPackage cp) {
        this.extendedMetadata = cp.getMetadataMap();
        this.contentXML = MQModel.mathqurateObjectFactory.getTypeAsXML(cp.getAssessmentItemType());
        this.assessmentItemType = MQModel.mathqurateObjectFactory.createAssessmentType(contentXML);
        populateMdFields();
    }

    /**
	 * Instantiates a new mQ assessment item.
	 */
    public MQContentPackage() {
    }

    /**
	 * Populates what metadata fields can be populated from the QTI attributes.
	 * Fill others with blanks if they're not already populated
	 */
    private void populateMdFields() {
        this.set(MQMetadata.IDENTIFIER[0], assessmentItemType.getIdentifier());
        this.set(MQMetadata.ADAPTIVE[0], String.valueOf(assessmentItemType.isAdaptive()));
        this.set(MQMetadata.TIMEDEPENDENT[0], String.valueOf(assessmentItemType.isTimeDependent()));
        this.set(MQMetadata.TITLE[0], assessmentItemType.getTitle());
        this.set(MQMetadata.AUTHORINGTOOL[0], assessmentItemType.getToolName());
        this.set(MQMetadata.LABEL[0], assessmentItemType.getLabel());
        this.set(MQMetadata.LANG[0], assessmentItemType.getLang());
        this.set(MQMetadata.TOOLVERSION[0], assessmentItemType.getToolVersion());
        for (String[] pair : MQMetadata.MQFIELDNAMES) {
            if (!extendedMetadata.containsKey(pair[0])) this.set(pair[0], "");
        }
    }

    /**
	 * Gets the assessment item type.
	 * 
	 * @return the assessment item type
	 */
    public AssessmentItemType getAssessmentItemType() {
        return assessmentItemType;
    }

    /**
	 * Sets the assessment item type.
	 * 
	 * @param assessmentItemType the new assessment item type
	 */
    public void setAssessmentItemType(AssessmentItemType assessmentItemType) {
        this.assessmentItemType = assessmentItemType;
    }

    /**
	 * Sets a metadata value 
	 * @param key metadata to set
	 * @param value value to set
	 */
    public void set(String key, String value) {
        key = key.toUpperCase();
        extendedMetadata.put(key, value);
        if (key.equals(MQMetadata.IDENTIFIER[0])) assessmentItemType.setIdentifier(value);
        if (key.equals(MQMetadata.ADAPTIVE[0])) {
            if (value.toLowerCase().equals("true")) assessmentItemType.setAdaptive(true); else assessmentItemType.setAdaptive(false);
        }
        if (key.equals(MQMetadata.TIMEDEPENDENT[0])) {
            if (value.toLowerCase().equals("true")) assessmentItemType.setTimeDependent(true); else assessmentItemType.setTimeDependent(false);
        }
        if (key.equals(MQMetadata.TITLE[0])) {
            assessmentItemType.setTitle(value);
        }
        if (key.equals(MQMetadata.AUTHORINGTOOL[0])) assessmentItemType.setToolName(value);
        if (key.equals(MQMetadata.LANG[0])) assessmentItemType.setLang(value);
        if (key.equals(MQMetadata.TOOLVERSION[0])) assessmentItemType.setToolVersion(value);
    }

    /**
	 * Gets a generic metadata value. DATETIME will always return current date.
	 * @param key
	 * @return
	 */
    public String get(String key) {
        key = key.toUpperCase();
        if (key.equals(MQMetadata.DATETIME[0])) {
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date();
            String dateStr = df.format(date);
            return dateStr;
        } else return extendedMetadata.get(key);
    }

    /**
	 * Returns the metadata as an MQMetadata.
	 * The MQMetadata is still used in the view class for populating the
	 * metadata table, so this is a convenience method for that purpose.
	 * @return
	 */
    public MQMetadata getMetadata() {
        MQMetadata metadata = new MQMetadata();
        ArrayList<AttribValue> metadataArray = new ArrayList<AttribValue>();
        metadata.setTaxon(this.get(MQMetadata.TAXON[0]));
        Iterator it = extendedMetadata.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String keyString = (String) pairs.getKey();
            String value = (String) pairs.getValue();
            for (String[] mdfield : MQMetadata.MQFIELDNAMES) {
                if (keyString.toUpperCase().equals(mdfield[0])) {
                    keyString = mdfield[1];
                }
            }
            String[] oneEntryArray = { keyString, value };
            if (!keyString.startsWith("@")) metadataArray.add(new AttribValue(keyString, value));
        }
        metadata.setMetadataArray(metadataArray);
        return metadata;
    }

    /**
	 * Sets the metadata hashmap from an MQMetadata object. As above,
	 * a convenience method used by the metadata UI view.
	 * @param metadata
	 */
    public void setMetadata(MQMetadata metadata) {
        this.set(MQMetadata.TAXON[0], metadata.getTaxon());
        ArrayList<AttribValue> metadataArray = metadata.getMetadataArray();
        for (AttribValue pair : metadataArray) {
            String keyString = pair.getAttrib();
            String value = pair.getValue();
            for (String[] mdfield : MQMetadata.MQFIELDNAMES) {
                if (keyString.equals(mdfield[1])) {
                    keyString = mdfield[0];
                }
            }
            if (!keyString.startsWith("@")) this.set(keyString, value);
        }
    }

    /**
	 * Returns the metadata as a linked hashmap
	 * @return
	 */
    public LinkedHashMap<String, String> getMetadataMap() {
        return extendedMetadata;
    }

    /**
	 * Set the metadata map from a linked hashmap
	 * @param extendedMetadata
	 */
    public void setMetadataMap(LinkedHashMap<String, String> extendedMetadata) {
        this.extendedMetadata = extendedMetadata;
        populateMdFields();
    }

    /**
	 * Gets metadata as a 2 x Y array
	 * @return
	 */
    public String[][] getMDAsArray() {
        ArrayList<String[]> al = new ArrayList();
        Iterator it = extendedMetadata.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String keyString = (String) pairs.getKey();
            String value = (String) pairs.getValue();
            String[] oneEntryArray = { keyString, value };
            al.add(oneEntryArray);
        }
        String[][] array = new String[2][al.size()];
        int i = 0;
        for (String[] oneEntryArray : al) {
            array[0][i] = oneEntryArray[0];
            array[1][i] = oneEntryArray[1];
            i++;
        }
        return array;
    }

    /**
	 * Set the metadata map from an existing CP's metadata
	 * @param cp
	 */
    public void setMetadataMap(MQContentPackage cp) {
        this.extendedMetadata = cp.getMetadataMap();
    }

    /**
	 * Returns the CP's metadata as an IMS CP manifest.
	 * @return
	 */
    public String metadataToXml() {
        return metadataToXml(this);
    }

    /**
	 * Returns an IMS manifest for a ContentPackage as a string
	 * Convenience static method - probably now won't ever be used in a static
	 * context, but there you go.
	 * @param md
	 * @return
	 */
    public static String metadataToXml(MQContentPackage cp) {
        InputStream is = MQContentPackage.class.getClassLoader().getResourceAsStream("org/qtitools/mathqurate/resources/md-template.xml");
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        String template = sb.toString();
        LinkedHashMap<String, String> mdmap = cp.getMetadataMap();
        template = template.replace("{{IDENTIFIER}}", mdmap.get(MQMetadata.IDENTIFIER[0]));
        template = template.replace("{{TITLE}}", mdmap.get(MQMetadata.TITLE[0]));
        template = template.replace("{{DESCRIPTION}}", mdmap.get(MQMetadata.DESCRIPTION[0]));
        template = template.replace("{{AUTHOR}}", mdmap.get(MQMetadata.AUTHOR[0]));
        template = template.replace("{{DESCRIPTION}}", mdmap.get(MQMetadata.DESCRIPTION[0]));
        template = template.replace("{{LOCATION-URL}}", mdmap.get(MQMetadata.LOCATION[0]));
        template = template.replace("{{SOFTWARE}}", mdmap.get(MQMetadata.SOFTWARE[0]));
        template = template.replace("{{CC-URL}}", mdmap.get(MQMetadata.LICENCEURL[0]));
        template = template.replace("{{FETLAR}}", mdmap.get(MQMetadata.REPODOMAIN[0]));
        String keywordsString = mdmap.get(MQMetadata.KEYWORDS[0]);
        if (keywordsString != null) {
            String[] keywords = keywordsString.split(",");
            String xmlString = "";
            for (String keyword : keywords) {
                keyword = keyword.trim();
                xmlString += "<imsmd:keyword><imsmd:langstring xml:lang=\"en\">" + keyword + "</imsmd:langstring></imsmd:keyword>\n";
            }
            template = template.replace("<!-- KEYWORDSHERE -->", xmlString);
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = df.format(date);
        template = template.replace("{{DATE}}", dateStr);
        long epoch = System.currentTimeMillis() / 1000;
        template = template.replace("{{UNIQUE-ID}}", "FETLAR-2-" + String.valueOf(epoch));
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = String.valueOf(epoch).getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            String md5 = String.format("%1$032X", i);
            template = template.replace("{{RANDOM-HASH}}", md5);
        } catch (NoSuchAlgorithmException e) {
        }
        String xmlString = "<imsmd:classification>" + "<imsmd:purpose>" + "<imsmd:source>" + "<imsmd:langstring xml:lang=\"x-none\">imsmdv1.0</imsmd:langstring>" + "</imsmd:source>" + "<imsmd:value>" + "<imsmd:langstring xml:lang=\"x-none\">Discipline</imsmd:langstring>" + "</imsmd:value>" + "</imsmd:purpose>" + "<imsmd:taxonpath>" + "<imsmd:source>" + "<imsmd:langstring xml:lang=\"en\"><!-- TAXONNAMEHERE --></imsmd:langstring>" + "</imsmd:source>" + "<imsmd:taxon>" + "<imsmd:entry>" + "<imsmd:langstring xml:lang=\"en\">" + mdmap.get(MQMetadata.TAXON[0]) + "</imsmd:langstring>" + "</imsmd:entry>" + "</imsmd:taxon>" + "</imsmd:taxonpath>" + "</imsmd:classification>";
        template = template.replace("<!-- MATAXONHERE -->", xmlString);
        template = template.replace("<!-- TAXONNAMEHERE -->", "MathAssess Taxonomy");
        template = template.replace("{{RES-ID}}", "id-" + String.valueOf(UUID.randomUUID()));
        template = template.replace("{{FILENAME}}", mdmap.get(MQMetadata.FILENAME[0]));
        template = template.replaceAll("\\{\\{.+\\}", "");
        int startpoint = template.indexOf("<!-- MDSTART -->");
        int endpoint = template.indexOf("<!-- MDEND -->");
        String metaelement = template.substring(startpoint, endpoint);
        metaelement = metaelement.replace("<!-- MDSTART -->", "");
        String qtiMD = "<imsqti:qtiMetadata>" + "<imsqti:timeDependent>{{TIMEDEPENDENT}}</imsqti:timeDependent>" + "<imsqti:solutionAvailable>{{SOLUTIONAVAILABLE}}</imsqti:solutionAvailable>" + "<imsqti:toolName>Mathqurate</imsqti:toolName>" + "<imsqti:toolVersion>{{MQVERSION}}</imsqti:toolVersion>" + "</imsqti:qtiMetadata>";
        metaelement = metaelement.replace("</metadata>", qtiMD + "</metadata>");
        template = template.replace("<!-- METADATA -->", metaelement);
        template = template.replace("{{TIMEDEPENDENT}}", mdmap.get(MQMetadata.TIMEDEPENDENT[0]));
        template = template.replace("{{SOLUTIONAVAILABLE}}", mdmap.get(MQMetadata.SOLUTIONAVAILABLE[0]));
        template = template.replace("{{MQVERSION}}", mdmap.get(MQMetadata.TOOLVERSION[0]));
        template = template.replaceAll("\\<!--.+--\\>", "");
        return format(template);
    }

    /**
	 * Returns a hashmap of metadata, given an IMS manifest XML file.
	 * @param filename a string containing the filename of the XML file.
	 * @return
	 */
    public static LinkedHashMap<String, String> metadataFromFile(String filename) {
        return metadataFromFile(new File(filename));
    }

    /**
	 * Returns a hashmap of metadata, given an IMS manifest XML file.
	 * @param xmlFile the XML file
	 * @return
	 */
    public static LinkedHashMap<String, String> metadataFromFile(File xmlFile) {
        try {
            String filename = xmlFile.getAbsolutePath();
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder docBuilder = null;
            try {
                docBuilder = docFactory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                return null;
            }
            Document doc = docBuilder.parse(filename);
            Element element = doc.getDocumentElement();
            XPathFactory xpfactory = XPathFactory.newInstance();
            XPath xpath = xpfactory.newXPath();
            NamespaceContext ncImsMd = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    if (prefix.equals("cp")) {
                        return ("http://www.imsglobal.org/xsd/imscp_v1p1");
                    } else return ("http://www.imsglobal.org/xsd/imsmd_v1p2");
                }

                public String getPrefix(String namespaceURI) {
                    if (namespaceURI.equals("http://www.imsglobal.org/xsd/imscp_v1p1")) {
                        return "cp";
                    } else return "lom";
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            };
            NamespaceContext ncImsQti = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    return ("http://www.imsglobal.org/xsd/imsqti_v2p1");
                }

                public String getPrefix(String namespaceURI) {
                    return "imsqti";
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            };
            NamespaceContext ncImsCp = new NamespaceContext() {

                public String getNamespaceURI(String prefix) {
                    return ("http://www.imsglobal.org/xsd/imscp_v1p1");
                }

                public String getPrefix(String namespaceURI) {
                    return "cp";
                }

                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            };
            xpath.setNamespaceContext(ncImsMd);
            XPathExpression expr = xpath.compile("//lom:general/lom:identifier");
            String identifier = (String) expr.evaluate(element, XPathConstants.STRING);
            expr = xpath.compile("//lom:general/lom:title");
            String title = (String) expr.evaluate(element, XPathConstants.STRING);
            title = title.trim();
            expr = xpath.compile("//lom:general/lom:description/lom:langstring");
            String description = (String) expr.evaluate(element, XPathConstants.STRING);
            description = description.trim();
            expr = xpath.compile("//cp:manifest/cp:metadata/lom:lom/lom:general/lom:keyword");
            NodeList keywordsList = (NodeList) expr.evaluate(element, XPathConstants.NODESET);
            String keywords = "";
            for (int i = 0; i < keywordsList.getLength(); i++) {
                keywords += keywordsList.item(i).getTextContent().replaceAll(",", "").trim();
                if (i != keywordsList.getLength() - 1) {
                    keywords += ", ";
                }
            }
            expr = xpath.compile("//lom:centity/lom:vcard");
            String author = (String) expr.evaluate(element, XPathConstants.STRING);
            author = author.replaceAll("(?i)BEGIN:VCARD FN:", "");
            author = author.replaceAll("(?i)END:VCARD", "");
            author = author.trim();
            expr = xpath.compile("//lom:technical/lom:location");
            String location = (String) expr.evaluate(element, XPathConstants.STRING);
            location = location.trim();
            expr = xpath.compile("//lom:otherplatformrequirements/lom:langstring");
            String software = (String) expr.evaluate(element, XPathConstants.STRING);
            software = software.trim();
            expr = xpath.compile("//lom:classification/lom:purpose/lom:source/lom:langstring[contains(.,'imsmd')]" + "/ancestor::lom:classification/lom:taxonpath/lom:taxon/lom:entry/lom:langstring");
            String taxon = (String) expr.evaluate(element, XPathConstants.STRING);
            taxon = taxon.trim();
            if (taxon.equals("")) {
                expr = xpath.compile("//lom:taxon/lom:entry/lom:langstring");
                taxon = (String) expr.evaluate(element, XPathConstants.STRING);
                taxon = taxon.trim();
            }
            expr = xpath.compile("//lom:rights/lom:description/lom:langstring");
            String licence = (String) expr.evaluate(element, XPathConstants.STRING);
            licence = licence.trim();
            xpath.setNamespaceContext(ncImsQti);
            expr = xpath.compile("//imsqti:timeDependent");
            String timedep = (String) expr.evaluate(element, XPathConstants.STRING);
            expr = xpath.compile("//imsqti:solutionAvailable");
            String solutionAvailable = (String) expr.evaluate(element, XPathConstants.STRING);
            expr = xpath.compile("//imsqti:toolVersion");
            String toolVersion = (String) expr.evaluate(element, XPathConstants.STRING);
            xpath.setNamespaceContext(ncImsCp);
            expr = xpath.compile("//imsqti:file/@href");
            String fileHref = (String) expr.evaluate(element, XPathConstants.STRING);
            LinkedHashMap<String, String> mdresults = new LinkedHashMap<String, String>();
            mdresults.put(MQMetadata.IDENTIFIER[0], identifier);
            mdresults.put(MQMetadata.TITLE[0], title);
            mdresults.put(MQMetadata.DESCRIPTION[0], description);
            mdresults.put(MQMetadata.KEYWORDS[0], keywords);
            mdresults.put(MQMetadata.AUTHOR[0], author);
            mdresults.put(MQMetadata.LOCATION[0], location);
            mdresults.put(MQMetadata.SOFTWARE[0], software);
            mdresults.put(MQMetadata.TAXON[0], taxon);
            mdresults.put(MQMetadata.TIMEDEPENDENT[0], timedep);
            mdresults.put(MQMetadata.SOLUTIONAVAILABLE[0], solutionAvailable);
            mdresults.put(MQMetadata.TOOLVERSION[0], toolVersion);
            mdresults.put(MQMetadata.FILENAME[0], fileHref);
            mdresults.put(MQMetadata.LICENCEURL[0], licence);
            return mdresults;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String format(String unformattedXml) {
        try {
            final Document document = parseXmlFile(unformattedXml);
            OutputFormat format = new OutputFormat(document);
            format.setLineWidth(65);
            format.setIndenting(true);
            format.setIndent(4);
            Writer out = new StringWriter();
            XMLSerializer serializer = new XMLSerializer(out, format);
            serializer.serialize(document);
            return out.toString();
        } catch (IOException e) {
        }
        return null;
    }

    public static Document parseXmlFile(String in) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(in));
            return db.parse(is);
        } catch (ParserConfigurationException e) {
        } catch (SAXException e) {
        } catch (IOException e) {
        }
        return null;
    }
}
