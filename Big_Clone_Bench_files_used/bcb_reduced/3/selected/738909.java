package de.annotatio.client;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class PostParser {

    private DocumentBuilderFactory factory;

    private DocumentBuilder builder;

    private Document document;

    private String annohash;

    private Document responseDoc;

    private String Location = null;

    private String baseurl = null;

    private String annoNS = "http://www.w3.org/2000/10/annotation-ns#";

    private String rdfNS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    private String dubNS = "http://purl.org/dc/elements/1.0/";

    private String threadNS = "http://www.w3.org/2001/03/thread#";

    private String alNS = "http://www.annotatio.de/al";

    private Transformer xformer = null;

    /**
	 * Constructor if called from the HTML Aufbereitungslayer
	 * @param document Document containing the annotation object
	 * @param annoMan Annotation-Manager Object
	 * @param the parsed Document for which we are creating an annotation
	 */
    public PostParser(Document document, AnnotationManager annoMan, Document htmldoc) {
        _PostParser(document, annoMan, htmldoc, "http://127.0.0.1:8080/annotate");
    }

    /**
	 * Constructor if called directly from Webserver
	 * @param contentmsg Content we got by HTML Post
	 * @param annoMan The annotation manager object
	 * @throws Exception
	 */
    public PostParser(String contentmsg, AnnotationManager annoMan, String baseurl) throws Exception {
        xformer = annoMan.getTransformer();
        builder = annoMan.getBuilder();
        StringReader strr = new StringReader(contentmsg);
        try {
            document = builder.parse(new InputSource(strr));
        } catch (SAXException sxe) {
            Exception e = (sxe.getException() != null) ? sxe.getException() : sxe;
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        DOMParser parser = new DOMParser();
        NodeList nl = document.getElementsByTagNameNS(annoNS, "annotates");
        if (nl.getLength() < 1) {
            System.out.println("Could not get URL context of the posted annotation.");
            throw new Exception();
        }
        String url = ((Element) nl.item(0)).getAttributeNS(rdfNS, "resource");
        parser.parse(url);
        Document htmldoc = parser.getDocument();
        if (htmldoc == null) {
            System.out.println("Could not parse the Document to annotate. The URL I got was:\n" + url);
            throw new Exception();
        }
        _PostParser(document, annoMan, htmldoc, baseurl);
    }

    private void _PostParser(Document document, AnnotationManager annoMan, Document htmldoc, String baseurl) {
        xformer = annoMan.getTransformer();
        builder = annoMan.getBuilder();
        String annohash = "";
        if (document == null) return;
        NodeList ndlist = document.getElementsByTagNameNS(annoNS, "body");
        if (ndlist.getLength() != 1) {
            System.out.println("Sorry Annotation Body was found " + ndlist.getLength() + " times");
            return;
        }
        Element bodynode = (Element) ndlist.item(0);
        Node htmlNode = bodynode.getElementsByTagName("html").item(0);
        if (htmlNode == null) htmlNode = bodynode.getElementsByTagName("HTML").item(0);
        Document newdoc = builder.newDocument();
        Element rootelem = newdoc.createElementNS(rdfNS, "r:RDF");
        rootelem.setAttribute("xmlns:r", rdfNS);
        rootelem.setAttribute("xmlns:a", annoNS);
        rootelem.setAttribute("xmlns:d", dubNS);
        rootelem.setAttribute("xmlns:t", threadNS);
        newdoc.appendChild(rootelem);
        Element tmpelem;
        NodeList tmpndlist;
        Element annoElem = newdoc.createElementNS(annoNS, "a:Annotation");
        rootelem.appendChild(annoElem);
        tmpelem = (Element) document.getElementsByTagNameNS(annoNS, "context").item(0);
        String context = tmpelem.getChildNodes().item(0).getNodeValue();
        annoElem.setAttributeNS(annoNS, "a:context", context);
        NodeList elemcontl = tmpelem.getElementsByTagNameNS(alNS, "context-element");
        Node ncontext_element = null;
        if (elemcontl.getLength() > 0) {
            Node old_context_element = elemcontl.item(0);
            ncontext_element = newdoc.importNode(old_context_element, true);
        }
        tmpndlist = document.getElementsByTagNameNS(dubNS, "title");
        annoElem.setAttributeNS(dubNS, "d:title", tmpndlist.getLength() > 0 ? tmpndlist.item(0).getChildNodes().item(0).getNodeValue() : "Default");
        tmpelem = (Element) document.getElementsByTagNameNS(dubNS, "creator").item(0);
        annoElem.setAttributeNS(dubNS, "d:creator", tmpelem.getChildNodes().item(0).getNodeValue());
        tmpelem = (Element) document.getElementsByTagNameNS(annoNS, "created").item(0);
        annoElem.setAttributeNS(annoNS, "a:created", tmpelem.getChildNodes().item(0).getNodeValue());
        tmpelem = (Element) document.getElementsByTagNameNS(dubNS, "date").item(0);
        annoElem.setAttributeNS(dubNS, "d:date", tmpelem.getChildNodes().item(0).getNodeValue());
        tmpndlist = document.getElementsByTagNameNS(dubNS, "language");
        String language = (tmpndlist.getLength() > 0 ? tmpndlist.item(0).getChildNodes().item(0).getNodeValue() : "en");
        annoElem.setAttributeNS(dubNS, "d:language", language);
        Node typen = newdoc.importNode(document.getElementsByTagNameNS(rdfNS, "type").item(0), true);
        annoElem.appendChild(typen);
        Element contextn = newdoc.createElementNS(annoNS, "a:context");
        contextn.setAttributeNS(rdfNS, "r:resource", context);
        annoElem.appendChild(contextn);
        Node annotatesn = newdoc.importNode(document.getElementsByTagNameNS(annoNS, "annotates").item(0), true);
        annoElem.appendChild(annotatesn);
        Element newbodynode = newdoc.createElementNS(annoNS, "a:body");
        annoElem.appendChild(newbodynode);
        if (ncontext_element != null) {
            contextn.appendChild(ncontext_element);
        } else {
            System.out.println("No context element found, we create one...");
            try {
                XPointer xptr = new XPointer(htmldoc);
                NodeRange xprange = xptr.getRange(context, htmldoc);
                Element context_elem = newdoc.createElementNS(alNS, "al:context-element");
                context_elem.setAttributeNS(alNS, "al:text", xprange.getContentString());
                context_elem.appendChild(newdoc.createTextNode(annoMan.generateContextString(xprange)));
                contextn.appendChild(context_elem);
            } catch (XPointerRangeException e2) {
                e2.printStackTrace();
            }
        }
        WordFreq wf = new WordFreq(annoMan.extractTextFromNode(htmldoc));
        Element docident = newdoc.createElementNS(alNS, "al:document-identifier");
        annotatesn.appendChild(docident);
        docident.setAttributeNS(alNS, "al:orig-url", ((Element) annotatesn).getAttributeNS(rdfNS, "resource"));
        docident.setAttributeNS(alNS, "al:version", "1");
        Iterator it = null;
        it = wf.getSortedWordlist();
        Map.Entry ent;
        String word;
        int count;
        int i = 0;
        while (it.hasNext()) {
            ent = (Map.Entry) it.next();
            word = ((String) ent.getKey());
            count = ((Counter) ent.getValue()).count;
            if ((word.length() > 4) && (i < 10)) {
                Element wordelem = newdoc.createElementNS(alNS, "al:word");
                wordelem.setAttributeNS(alNS, "al:freq", Integer.toString(count));
                wordelem.appendChild(newdoc.createTextNode(word));
                docident.appendChild(wordelem);
                i++;
            }
        }
        try {
            StringWriter strw = new StringWriter();
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");
            xformer.transform(new DOMSource(newdoc), new StreamResult(strw));
            messagedigest.update(strw.toString().getBytes());
            byte[] md5bytes = messagedigest.digest();
            annohash = "";
            for (int b = 0; b < md5bytes.length; b++) {
                String s = Integer.toHexString(md5bytes[b] & 0xFF);
                annohash = annohash + ((s.length() == 1) ? "0" + s : s);
            }
            this.annohash = annohash;
            annoElem.setAttribute("xmlns:al", alNS);
            annoElem.setAttributeNS(alNS, "al:id", getAnnohash());
            Location = (baseurl + "/annotation/" + getAnnohash());
            annoElem.setAttributeNS(rdfNS, "r:about", Location);
            newbodynode.setAttributeNS(rdfNS, "r:resource", baseurl + "/annotation/body/" + getAnnohash());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        annoMan.store(newdoc.getDocumentElement());
        annoMan.createAnnoResource(newdoc.getDocumentElement(), getAnnohash());
        if (htmlNode != null) annoMan.createAnnoBody(htmlNode, getAnnohash());
        Location = (this.baseurl + "/annotation/" + getAnnohash());
        annoElem.setAttributeNS(rdfNS, "r:about", Location);
        this.responseDoc = newdoc;
    }

    public Node findChildNode(String tag, Node nnode) {
        if (nnode.getNodeName().equals(tag)) {
            return nnode;
        } else {
            for (int i = 0; i < nnode.getChildNodes().getLength(); i++) {
                Node tempnode = findChildNode(tag, nnode.getChildNodes().item(i));
                if (tempnode != null) {
                    return (tempnode);
                }
            }
            return null;
        }
    }

    public String getResponse() {
        StringWriter resp = new StringWriter();
        try {
            xformer.transform(new DOMSource(responseDoc), new StreamResult(resp));
        } catch (TransformerException e) {
            e.printStackTrace();
            return "";
        }
        return resp.toString();
    }

    public String getLocation() {
        return Location;
    }

    /**
	 * @return Returns the annohash.
	 */
    public String getAnnohash() {
        return annohash;
    }
}
