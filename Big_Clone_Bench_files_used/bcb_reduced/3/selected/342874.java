package org.fetlar.spectatus.model;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.prefs.Preferences;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.NamespaceContext;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.fetlar.spectatus.controller.GroundControl;
import org.fetlar.spectatus.utilities.MiscUtils;
import org.imsglobal.xsd.imsmd_v1p2.LomType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class MetadataHelper {

    private static String updateStringFromPair(String[] pair, String mdString) {
        if (pair[0].equals("Title")) mdString = mdString.replace("{{TITLE}}", pair[1]);
        if (pair[0].equals("Description")) mdString = mdString.replace("{{DESCRIPTION}}", pair[1]);
        if (pair[0].equals("Identifier")) mdString = mdString.replace("{{IDENTIFIER}}", pair[1]);
        if (pair[0].equals("Author")) mdString = mdString.replace("{{AUTHOR}}", pair[1]);
        if (pair[0].equals("Location")) mdString = mdString.replace("{{LOCATION-URL}}", pair[1]);
        if (pair[0].equals("Software")) mdString = mdString.replace("{{SOFTWARE}}", pair[1]);
        if (pair[0].equals("Taxon")) {
            String xmlString = "<imsmd:classification>" + "<imsmd:purpose>" + "<imsmd:source>" + "<imsmd:langstring xml:lang=\"x-none\">imsmdv1.0</imsmd:langstring>" + "</imsmd:source>" + "<imsmd:value>" + "<imsmd:langstring xml:lang=\"x-none\">Discipline</imsmd:langstring>" + "</imsmd:value>" + "</imsmd:purpose>" + "<imsmd:taxonpath>" + "<imsmd:source>" + "<imsmd:langstring xml:lang=\"en\"><!-- TAXONNAMEHERE --></imsmd:langstring>" + "</imsmd:source>" + "<imsmd:taxon>" + "<imsmd:entry>" + "<imsmd:langstring xml:lang=\"en\">" + pair[1].trim() + "</imsmd:langstring>" + "</imsmd:entry>" + "</imsmd:taxon>" + "</imsmd:taxonpath>" + "</imsmd:classification>";
            mdString = mdString.replace("<!-- MATAXONHERE -->", xmlString);
            mdString = mdString.replace("<!-- TAXONNAMEHERE -->", Preferences.userRoot().node("Spectatus").get("TaxonName", "MathAssess Taxonomy"));
        }
        if (pair[0].equals("Keywords")) {
            String keywordsString = pair[1];
            keywordsString = keywordsString.trim();
            String[] keywords = keywordsString.split(",");
            String xmlString = "";
            for (String keyword : keywords) {
                keyword = keyword.trim();
                xmlString += "<imsmd:keyword><imsmd:langstring xml:lang=\"en\">" + keyword + "</imsmd:langstring></imsmd:keyword>\n";
            }
            mdString = mdString.replace("<!-- PNKEYWORDSHERE -->", xmlString);
        }
        return mdString;
    }

    /**
	 * Returns a Lom type object from a String[][]
	 * This uses the internal resource fetlar-md.xml, replaces the various
	 * placeholders with our data, and then uses JAXB to convert from the
	 * resulting string to an object
	 * @param pairs String array, 2xX
	 * @return a LOM
	 */
    public static LomType makeLomFromPairsAndSize(String[][] pairs, long size) {
        InputStream is = MetadataHelper.class.getResourceAsStream("/res/fetlar-md.xml");
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        String mdTemplate = sb.toString();
        for (String[] pair : pairs) {
            mdTemplate = updateStringFromPair(pair, mdTemplate);
        }
        if (Preferences.userRoot().node("Spectatus").get("TaxonNoFETLAR", "N").equals("Y")) {
            mdTemplate = mdTemplate.replaceAll("(?s)\\<!-- FETLAR-CLASS-START --\\>.*<!-- FETLARCLASSEND -->", "");
        }
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        String dateStr = df.format(date);
        mdTemplate = mdTemplate.replace("{{DATE}}", dateStr);
        mdTemplate = mdTemplate.replace("{{RANDOM-HASH}}", MiscUtils.fullUUID());
        mdTemplate = mdTemplate.replace("{{MIME-TYPE}}", "text/x-imsqti-test-xml");
        mdTemplate = mdTemplate.replace("{{TYPE}}", "ExaminationTest");
        mdTemplate = mdTemplate.replace("{{RESDESC}}", "A test");
        mdTemplate = mdTemplate.replace("{{FETLAR}}", "www.fetlar.ac.uk");
        if (size != -1) mdTemplate = mdTemplate.replace("{{SIZE}}", String.valueOf(size)); else mdTemplate = mdTemplate.replace("{{SIZE}}", "0");
        long epoch = System.currentTimeMillis() / 1000;
        mdTemplate = mdTemplate.replace("{{UNIQUE-ID}}", "FETLAR-2-" + String.valueOf(epoch));
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] data = String.valueOf(epoch).getBytes();
            m.update(data, 0, data.length);
            BigInteger i = new BigInteger(1, m.digest());
            String md5 = String.format("%1$032X", i);
            mdTemplate = mdTemplate.replace("{{RANDOM-HASH}}", md5);
        } catch (NoSuchAlgorithmException e) {
        }
        mdTemplate = mdTemplate.replaceAll(">\\s+$", ">");
        mdTemplate = mdTemplate.replaceAll(">\\s+<", "><");
        mdTemplate = mdTemplate.replaceAll("^\\s<", "<");
        try {
            return (GroundControl.fileHelper.getLomFromString(mdTemplate));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static LomType makeLomFromPairs(String[][] pairs) {
        return makeLomFromPairsAndSize(pairs, -1);
    }

    public static String[][] makePairsFromLom(LomType lom) {
        ArrayList<String[]> arrAsList = new ArrayList<String[]>();
        Document doc;
        try {
            doc = GroundControl.fileHelper.lomToDoc(lom);
            Element element = doc.getDocumentElement();
            XPathFactory xpfactory = XPathFactory.newInstance();
            XPath xpath = xpfactory.newXPath();
            NamespaceContext nc = new NamespaceContext() {

                @Override
                public String getNamespaceURI(String prefix) {
                    return ("http://www.imsglobal.org/xsd/imsmd_v1p2");
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return "lom";
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            };
            xpath.setNamespaceContext(nc);
            XPathExpression expr = xpath.compile("//lom:identifier");
            String[] s1 = { "Identifier", (String) expr.evaluate(element, XPathConstants.STRING) };
            arrAsList.add(s1);
            expr = xpath.compile("//lom:general/lom:title");
            String[] s4 = { "Title", (String) expr.evaluate(element, XPathConstants.STRING) };
            arrAsList.add(s4);
            expr = xpath.compile("//lom:general/lom:description/lom:langstring");
            String desc = (String) expr.evaluate(element, XPathConstants.STRING);
            String[] s2 = { "Description", desc };
            arrAsList.add(s2);
            expr = xpath.compile("//lom:general/lom:keyword");
            NodeList keywordsList = (NodeList) expr.evaluate(element, XPathConstants.NODESET);
            String kwString = "";
            for (int i = 0; i < keywordsList.getLength(); i++) {
                kwString += keywordsList.item(i).getTextContent();
                if (i != keywordsList.getLength() - 1) {
                    kwString += ", ";
                }
            }
            String[] s3 = { "Keywords", kwString };
            arrAsList.add(s3);
            expr = xpath.compile("//lom:centity/lom:vcard");
            String vcString = (String) expr.evaluate(element, XPathConstants.STRING);
            vcString = vcString.replace("BEGIN:VCARD FN:", "");
            vcString = vcString.replace(" END:VCARD", "");
            String[] s5 = { "Author", vcString };
            arrAsList.add(s5);
            expr = xpath.compile("//lom:technical/lom:location");
            String[] s6 = { "Location", (String) expr.evaluate(element, XPathConstants.STRING) };
            arrAsList.add(s6);
            expr = xpath.compile("//lom:otherplatformrequirements/lom:langstring");
            String[] s7 = { "Software", (String) expr.evaluate(element, XPathConstants.STRING) };
            arrAsList.add(s7);
            expr = xpath.compile("//lom:classification/lom:purpose/lom:source/lom:langstring[contains(.,'imsmd')]" + "/ancestor::lom:classification/lom:taxonpath/lom:taxon/lom:entry/lom:langstring");
            String taxStr = (String) expr.evaluate(element, XPathConstants.STRING);
            if (!taxStr.trim().equals("")) {
                String[] s8 = { "Taxon", taxStr };
                arrAsList.add(s8);
            }
            String[][] finalArray = new String[arrAsList.size()][2];
            int i = 0;
            for (String[] pair : arrAsList) {
                finalArray[i][0] = pair[0];
                finalArray[i][1] = pair[1];
                i++;
            }
            return finalArray;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String[][] makePairsFromLom(Node node) {
        Document doc = node.getOwnerDocument();
        LomType lom;
        try {
            lom = GroundControl.fileHelper.getLomFromDoc(doc);
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
        return makePairsFromLom(lom);
    }
}
