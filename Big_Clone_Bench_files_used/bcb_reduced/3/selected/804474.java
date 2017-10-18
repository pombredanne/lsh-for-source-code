package edu.mit.lcs.haystack.rdf;

import edu.mit.lcs.haystack.Constants;
import org.apache.xml.serialize.XMLSerializer;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;

/**
 * Represents an RDF statement.
 * @author Dennis Quan
 */
public final class Statement implements Serializable {

    protected Resource subject;

    protected Resource predicate;

    protected RDFNode object;

    /**
	 * Constructs a Statement object with the given subject, predicate, and object.
	 */
    public Statement(Resource subject, Resource predicate, RDFNode object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    /**
	 * Returns the subject of this statement.
	 */
    public final Resource getSubject() {
        return subject;
    }

    /**
	 * Returns the predicate of this statement.
	 */
    public final Resource getPredicate() {
        return predicate;
    }

    static String byteChars = "0123456789abcdef";

    /**
	 * Returns a resource identifying this statement based on its MD5 hash.
	 */
    public Resource getMD5HashResource() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.write(subject.getContent().length());
            dos.write(predicate.getContent().length());
            dos.write(object.getContent().length());
            dos.writeBoolean(object instanceof Literal);
            dos.writeChars(subject.getContent());
            dos.writeChars(predicate.getContent());
            dos.writeChars(object.getContent());
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(baos.toByteArray());
            StringBuffer sb = new StringBuffer("urn:statement:md5:");
            for (int i = 0; i < bytes.length; i++) {
                int loNibble = bytes[i] & 0xf;
                int hiNibble = (bytes[i] >> 4) & 0xf;
                sb.append(byteChars.charAt(hiNibble));
                sb.append(byteChars.charAt(loNibble));
            }
            return new Resource(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Returns the object of this statement.
	 */
    public final RDFNode getObject() {
        return object;
    }

    public boolean equals(Object o) {
        if ((o == null) || !(o instanceof Statement)) {
            return false;
        }
        Statement s = (Statement) o;
        return s.subject.equals(subject) && s.object.equals(object) && s.predicate.equals(predicate);
    }

    public int hashCode() {
        return subject.hashCode() + predicate.hashCode() + object.hashCode();
    }

    /**
	 * Generates an N3 representation of this statement.
	 */
    public final String generateN3() {
        return subject.toString() + " " + predicate.toString() + " " + object.toString() + " .";
    }

    /**
	 * Generates an Adenine representation of this statement.
	 */
    public final String generateAdenine() {
        return subject.toString() + " " + predicate.toString() + " " + object.toString() + " ";
    }

    public String toString() {
        return generateN3();
    }

    /**
	 * Generates an RDF Description tag representing this statement.
	 */
    public String generateRDF() {
        StringBuffer sb = new StringBuffer("<rdf:Description xmlns:rdf=\"");
        sb.append(Constants.s_rdf_namespace);
        sb.append("\" rdf:about=\"");
        sb.append(subject.getURI());
        sb.append("\">\n\t<x:");
        String namespace = Utilities.guessNamespace(predicate.getURI());
        String name = Utilities.guessName(predicate.getURI());
        sb.append(name);
        sb.append(" xmlns:x=\"");
        sb.append(namespace);
        sb.append("\"");
        if (object instanceof Resource) {
            sb.append(" rdf:resource=\"");
            sb.append(object.getContent());
            sb.append("\" />");
        } else {
            sb.append(" rdf:parseType=\"Literal\">");
            sb.append(Utilities.xmlEncode(object.getContent()));
            sb.append("</x:");
            sb.append(name);
            sb.append(">");
        }
        sb.append("\n</rdf:Description>");
        return sb.toString();
    }

    public void generateRDF(XMLSerializer xmls) throws SAXException {
        AttributesImpl descAttribs = new AttributesImpl();
        descAttribs.addAttribute(Constants.s_rdf_namespace, "about", "rdf:about", "string", subject.getURI());
        xmls.startElement(Constants.s_rdf_namespace, "Description", "", descAttribs);
        String namespace = Utilities.guessNamespace(predicate.getURI());
        String name = Utilities.guessName(predicate.getURI());
        xmls.startPrefixMapping("x", namespace);
        AttributesImpl predAttribs = new AttributesImpl();
        if (object instanceof Resource) {
            predAttribs.addAttribute(Constants.s_rdf_namespace, "resource", "rdf:resource", "string", object.getContent());
        } else {
            predAttribs.addAttribute(Constants.s_rdf_namespace, "parseType", "rdf:parseType", "string", "Literal");
        }
        xmls.startElement(namespace, name, "", predAttribs);
        if (object instanceof Literal) {
            xmls.characters(object.getContent().toCharArray(), 0, object.getContent().length());
        }
        xmls.endElement(namespace, name, "");
        xmls.endElement(Constants.s_rdf_namespace, "Description", "");
    }
}
