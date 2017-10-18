package com.indigen.victor.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.jaxen.BaseXPath;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Configuration;
import org.w3c.tidy.EntityTable;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Static methods for manipulating XML data
 * 
 * @author mig
 *
 */
public class XmlUtils {

    /**
	 * This method should never be called as XmlUtils only contains static methods
	 * @throws InstantiationException
	 */
    private XmlUtils() throws InstantiationException {
        throw new InstantiationException(getClass().getName() + " should never be instantiated");
    }

    /**
	 * Regular expression pattern to recognize the starting <code>body</code> tag within
	 * a HTML string 
	 */
    static final Pattern startBodyPattern = Pattern.compile(".*?<body.*?>");

    /**
	 * Regular expression pattern to recognize the ending <code>body</code> tag within
	 * a HTML string 
	 */
    static final Pattern endBodyPattern = Pattern.compile(".*</body>");

    /**
	 * Regular expression pattern to recognize a PHP processor instruction
	 */
    static final Pattern phpPattern = Pattern.compile("&lt;\\?(=|php)(.*?)\\?&gt;", Pattern.DOTALL);

    /**
	 * A <code>NumberFormat</code> object to generate integer string representations
	 * with minimum 4 digits and no grouping
	 */
    static final NumberFormat intFormat = NumberFormat.getIntegerInstance();

    /**
	 * A <code>DocumentBuilder</code> object to create empty XML documents 
	 */
    private static DocumentBuilder documentBuilder = null;

    /**
	 * A <code>DOMImplementation</code> object to create empty XML documents 
	 */
    private static DOMImplementation domImplementation = null;

    static {
        intFormat.setMinimumIntegerDigits(4);
        intFormat.setGroupingUsed(false);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            documentBuilder = factory.newDocumentBuilder();
            domImplementation = documentBuilder.getDOMImplementation();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Escapes a string to xml-encode &amp;, &lt; and &gt;
     * @param str the input string
     * @return the escaped string
     */
    public static String xmlEscape(String str) {
        String s = str.replaceAll("&", "&amp;");
        s = s.replaceAll("<", "&lt;");
        s = s.replaceAll(">", "&gt;");
        s = s.replaceAll("'", "&apos;");
        s = s.replaceAll("\"", "&quot;");
        return s;
    }

    /**
     * In addition to escaping XML special character through method 
     * {@link #xmlEscape(String)},
     * this method also replaces non-ascii characters with the HTML entity
     * representation. 
     * @param str the input string
     * @return the escaped string
     */
    public static String xhtmlEscape(String str) {
        String s = xmlEscape(str);
        StringBuffer sb = new StringBuffer();
        EntityTable et = EntityTable.getDefaultEntityTable();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch > 0x7F) {
                String entity = et.entityName((short) ch);
                sb.append("&");
                if (entity == null) sb.append("#" + (int) ch); else sb.append(entity);
                sb.append(";");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    /**
     * Transform XML entities into single chracter representation: &amp;, 
     * &lt;, &gt;, &quot;
     * @param str the input string to be unescaped
     * @return the unescaped string
     */
    public static String xmlUnescape(String str) {
        String s = str.replaceAll("&amp;", "&");
        s = s.replaceAll("&lt;", "<");
        s = s.replaceAll("&gt;", ">");
        s = s.replaceAll("&quot;", "\"");
        return s;
    }

    /**
     * Serialize a DOM document into a string
     * @param doc the document to be serialized
     * @return the serialized document as a string
     */
    public static String serialize(Document doc) {
        return serialize(doc.getDocumentElement());
    }

    /**
	 * Serialize a DOM node and the nodes below into a string
	 * @param node the DOM node to be serialized 
	 * @return the serialized document as a string
	 */
    public static String serialize(Node node) {
        return serialize(node, false);
    }

    /**
     * Serialize a DOM node and the nodes below into a string. Optionally, the tag names
     * may be forced to lower case. 
     * @param node the DOM node to be serialized
     * @param lowerCase indicates whether tag names must be forced to lower case
     * @return the serialized document
     */
    public static String serialize(Node node, boolean lowerCase) {
        return serialize(node, lowerCase, true);
    }

    /**
     * Serialize a DOM node and the nodes below into a string. Optionally, the tag names
     * may be forced to lower case and special characters converted to HTML entities.
     * @param node the DOM node to be serialized
     * @param lowerCase indicates whether tag names must be forced to lower case
     * @param useEntities indicates whether special (non-ASCII) characters must be converted
     * to HTML entities
     * @return the serialized document
     */
    public static String serialize(Node node, boolean lowerCase, boolean useEntities) {
        StringBuffer sb = new StringBuffer();
        serialize(node, sb, lowerCase, useEntities);
        return sb.toString();
    }

    /**
	 * Same as {@link #serialize(Node, boolean, boolean)} but
	 * using a StringBuffer for serialization.
	 * @param node
	 * @param sb
	 * @param lowerCase
	 * @param useEntities
	 */
    private static void serialize(Node node, StringBuffer sb, boolean lowerCase, boolean useEntities) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) node;
            sb.append("<");
            if (lowerCase) sb.append(elem.getTagName().toLowerCase()); else sb.append(elem.getTagName());
            NamedNodeMap attrs = elem.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                String attrName = attr.getNodeName().toLowerCase();
                if (attrName.equals("xmlns")) {
                } else {
                    sb.append(' ');
                    StringBuffer sb1 = new StringBuffer();
                    String attrName0 = attr.getNodeName().toLowerCase();
                    for (int j = 0; j < attrName0.length(); j++) {
                        char ch = attrName0.charAt(j);
                        if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == ':' || (sb1.length() > 0 && ((ch >= '0' && ch <= '9') || ch == '-' || ch == '.'))) sb1.append(ch);
                    }
                    if (sb1.length() == 0) sb1.append('_');
                    sb.append(sb1.toString());
                    sb.append("=\"");
                    sb.append(xmlEscape(attr.getNodeValue()));
                    sb.append("\"");
                }
            }
            node = elem.getFirstChild();
            if (node == null) {
                sb.append("/>");
            } else {
                sb.append(">");
                while (node != null) {
                    serialize(node, sb, lowerCase, useEntities);
                    node = node.getNextSibling();
                }
                if (lowerCase) sb.append("</" + elem.getNodeName().toLowerCase() + ">"); else sb.append("</" + elem.getNodeName() + ">");
            }
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            String text = node.getNodeValue();
            if (text == null) text = "";
            if (useEntities) sb.append(xhtmlEscape(text)); else sb.append(xmlEscape(text));
        } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            String text = node.getNodeValue();
            if (text == null) text = "";
            sb.append("<![CDATA[");
            sb.append(text);
            sb.append("]]>");
        }
    }

    /**
	 * Generates a string digest out of a DOM node (and descendant nodes). This allows comparing
	 * different DOM portions for equivalence. Element attributes are reordered as well as
	 * same level elements unless parent element has attribute <code>array</code> set to
	 * <code>true</code>.
	 * @param node the DOM node to be digested
	 * @return a string representing the DOM node. The resulting string is not constrained to a
	 * specific size and may be big.
	 */
    public static String getDigest(Node node) {
        List descrs = new Vector();
        getDigest(node, "", descrs, false, 0);
        Collections.sort(descrs);
        StringBuffer digest = new StringBuffer();
        for (Iterator it = descrs.iterator(); it.hasNext(); ) digest.append((String) it.next());
        return digest.toString();
    }

    /**
	 * Recursive method used by {@link #getDigest(Node)}.
	 * @param node
	 * @param descr
	 * @param descrs
	 * @param maintainsOrder
	 * @param index
	 */
    private static void getDigest(Node node, String descr, List descrs, boolean maintainsOrder, int index) {
        switch(node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element elem = (Element) node;
                descr = descr + "/" + elem.getTagName();
                if (maintainsOrder) {
                    descr += intFormat.format(index);
                    maintainsOrder = false;
                }
                if (elem.hasAttributes()) {
                    NamedNodeMap attrs = elem.getAttributes();
                    List names = new Vector();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        String name = attrs.item(i).getNodeName();
                        names.add(name);
                        if (name.equals("array") && elem.getAttribute(name).equals("true")) maintainsOrder = true;
                    }
                    Collections.sort(names);
                    for (Iterator it = names.iterator(); it.hasNext(); ) {
                        String name = (String) it.next();
                        descr += " " + name + ":\"" + elem.getAttribute(name) + "\"";
                    }
                }
                if (node.hasChildNodes()) {
                    index = 0;
                    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) getDigest(child, descr, descrs, maintainsOrder, index++);
                } else descrs.add(descr);
                break;
            case Node.TEXT_NODE:
                descrs.add(descr + "/" + node.getNodeValue());
                break;
        }
    }

    /**
	 * Aggregates all text and CDATA nodes within a document and returns the resulting string
	 * @param document the document the text should be extracted from.
	 * @return the result of all texts aggregation
	 */
    public static String getString(Document document) {
        return getString(document.getDocumentElement());
    }

    /**
	 * Aggregates all text and CDATA nodes below and including the given node and returns 
	 * the resulting string
	 * @param node the DOM node the text should be extracted from.
	 * @return the result of all texts aggregation
	 */
    public static String getString(Node node) {
        StringBuffer sb = new StringBuffer();
        getString(node, sb);
        return sb.toString();
    }

    /**
	 * Recursive method for {@link #getString(Document)} and {@link #getString(Node)}
	 * @param node
	 * @param sb
	 */
    private static void getString(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getNodeValue());
        } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            sb.append(node.getNodeValue());
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            node = ((Element) node).getFirstChild();
            while (node != null) {
                getString(node, sb);
                node = node.getNextSibling();
            }
        }
    }

    /**
	 * Generate an XPath string representation to locate <code>node</code> relatively to
	 * <code>context</code>. If <code>context</code> is not null, <code>node</code> must 
	 * be below <code>context</code> otherwise an absolutge XPath is generated.
	 * @param node the node to be located
	 * @param context the base for locating <code>node</code>. This parameter can be null
	 * @return an XPath representation of the given node. For instance: 
	 * <code>./table[3]/tr[2]/td[5]/text()[1]</code>
	 */
    public static String getXPath(Node node, Node context) {
        return getXPath(node, context, null);
    }

    /**
	 * Generate an XPath string representation to locate <code>node</code> relatively to
	 * <code>context</code>. If <code>context</code> is not null, <code>node</code> must 
	 * be below <code>context</code> otherwise an absolutge XPath is generated.
	 * @param node the node to be located
	 * @param context the base for locating <code>node</code>. This parameter can be null
	 * @param prefix if non-null, all element names are prefixed by <code>prefix</code>
	 * @return an XPath representation of the given node. For instance: 
	 * <code>./html:table[3]/html:tr[2]/html:td[5]/text()[1]</code>
	 */
    public static String getXPath(Node node, Node context, String prefix) {
        StringBuffer sb = new StringBuffer();
        while (node.getOwnerDocument().getDocumentElement() != node && !node.equals(context)) {
            int index = 1;
            Node node0 = node.getParentNode().getFirstChild();
            while (node0 != node) {
                if (node0.getNodeName().equalsIgnoreCase(node.getNodeName())) {
                    index++;
                }
                node0 = node0.getNextSibling();
            }
            if (sb.length() > 0) {
                sb.insert(0, "/");
            }
            sb.insert(0, node.getNodeName() + "[" + index + "]");
            if (prefix != null) {
                sb.insert(0, prefix + ":");
            }
            node = node.getParentNode();
        }
        if (sb.length() > 0) sb.insert(0, "/");
        if (node.getOwnerDocument().getDocumentElement() == node) {
            sb.insert(0, "/" + node.getNodeName() + "[1]");
            if (prefix != null) {
                sb.insert(0, prefix + ":");
            }
        } else sb.insert(0, ".");
        return sb.toString();
    }

    /**
     * Returns a <code>BaseXPath</code> object out of a string xpath. The <code>xhtml</code>
     * namespace is automatically added.
     * @param xpath the XPath representation
     * @return a <code>BaseXPath</code> object
     * @throws JaxenException
     */
    public static BaseXPath getDOMXPath(String xpath) throws JaxenException {
        DOMXPath domXPath = new DOMXPath(xpath);
        domXPath.addNamespace("xhtml", "http://www.w3.org/1999/xhtml");
        return domXPath;
    }

    /**
	 * Returns a list of nodes matching the given XPath in the given DOM.
	 * @param dom the root node of the DOM where to look for nodes
	 * @param xpath the XPath string representation of the nodes to get
	 * @return the list, eventually empty, of matching node, or null if the
	 * XPath expression is incorrect
	 */
    public static List getNodesFromXPath(Node dom, String xpath) {
        try {
            BaseXPath domXPath = getDOMXPath(xpath);
            return domXPath.selectNodes(dom);
        } catch (JaxenException e) {
            return new Vector();
        }
    }

    /**
	 * Returns a list a String objects representing the concatenation of text portions
	 * for each matching node.
	 * @param dom the root node of the DOM where to look for nodes
	 * @param xpath the XPath string representation of the nodes to get
	 * @return the list, eventually empty, of String objects representing the content of
	 * the matching nodes
	 */
    public static List getStringsFromXPath(Node dom, String xpath) {
        try {
            BaseXPath domXPath = getDOMXPath(xpath);
            List strings = new Vector();
            List nodes = domXPath.selectNodes(dom);
            Iterator i = nodes.iterator();
            while (i.hasNext()) {
                Node node = (Node) i.next();
                String str = getString(node);
                strings.add(str);
            }
            return strings;
        } catch (JaxenException e) {
            return null;
        }
    }

    /**
	 * Return the first node matching the given XPath in the given DOM. 
	 * @param dom the root node of the DOM where to look for the node
	 * @param xpath the XPath string representation of the node to get
	 * @return the matching DOM node or null if no node matching the XPath
	 */
    public static Node getNodeFromXPath(Node dom, String xpath) {
        try {
            BaseXPath domXPath = getDOMXPath(xpath);
            return (Node) domXPath.selectSingleNode(dom);
        } catch (JaxenException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Returns the string content of the node matching the given XPath in the
	 * given DOM.
	 * @param dom the root node of the DOM where to look for the node
	 * @param xpath the XPath string representation of the node to get
	 * @return the string content of the matching node. If no matching node is found,
	 * an empty string is returned. If the XPath expression is incorrect, null is
	 * returned
	 */
    public static String getStringFromXPath(Node dom, String xpath) {
        try {
            BaseXPath domXPath = getDOMXPath(xpath);
            return domXPath.stringValueOf(dom);
        } catch (JaxenException e) {
            return null;
        }
    }

    /**
	 * Returns the value of the given attribute of the element matching the given XPath
	 * in the given DOM.
	 * @param dom the root node of the DOM where to look for the node
	 * @param xpath the XPath string representation of the node to get
	 * @param attrName the name of the attribute of the matching node
	 * @return the attribute value, or null if
	 * the node is not found or not an element, or an empty string if the element
	 * does not have the given attribute
	 */
    public static String getAttributeFromXPath(Node dom, String xpath, String attrName) {
        Node node = getNodeFromXPath(dom, xpath);
        if (node == null || !(node instanceof Element)) return null;
        String attrValue = ((Element) node).getAttribute(attrName);
        return attrValue;
    }

    /**
	 * Returns the deepest common ancestor for the two given nodes. The search is bound
	 * to nodes below (including) the given base node 
	 * @param link1 the first node
	 * @param link2 the second node
	 * @param baseNode the upper possible node
	 * @return the common ancestor node, or null if the two nodes do not have an ancestor 
	 * below the base node
	 */
    public static Node getCommonAncestor(Node link1, Node link2, Node baseNode) {
        List ancestors1 = new Vector();
        Node node = link1;
        while (node != null) {
            ancestors1.add(node);
            if (node == baseNode) break;
            node = node.getParentNode();
        }
        Collections.reverse(ancestors1);
        node = link2;
        while (node != null) {
            Iterator i = ancestors1.iterator();
            while (i.hasNext()) {
                Node ancestor = (Node) i.next();
                if (node == ancestor) return node;
            }
            if (node == baseNode) return null;
            node = node.getParentNode();
        }
        return null;
    }

    /**
	 * Serialize the nodes below the given node into a string using Tidy library. The top element
	 * is a <code>&lt;body&gt;</code> tag.
	 * @param node the top node to be serialized
	 * @return the string representation of the DOM portion
	 * @see <a href="http://sourceforge.net/projects/jtidy">JTidy</a>
	 */
    public static String tidySerializeContent(Node node) {
        Document doc = Tidy.createEmptyDocument();
        Node body = doc.createElement("body");
        doc.getDocumentElement().appendChild(body);
        node = node.getFirstChild();
        while (node != null) {
            Node node0 = cloneNode(node, doc);
            body.appendChild(node0);
            node = node.getNextSibling();
        }
        String htmlCode = tidySerialize(doc);
        if (htmlCode == null) return null;
        return getBodyFromXHtml(htmlCode);
    }

    /**
	 * Serialize the document into a string using Tidy library.
	 * @param doc document to be serialized
	 * @return the string representation of the DOM portion
	 * @see <a href="http://sourceforge.net/projects/jtidy">JTidy</a>
	 */
    public static String tidySerialize(Document doc) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int tidyEnc = Configuration.ASCII;
        Tidy tidy = new Tidy();
        tidy.setQuoteNbsp(true);
        tidy.setCharEncoding(tidyEnc);
        tidy.setXHTML(true);
        tidy.setXmlOut(true);
        tidy.setTidyMark(false);
        tidy.setIndentContent(true);
        tidy.setWrapPhp(true);
        tidy.setWraplen(0xFFFFFF);
        tidy.setXmlPi(true);
        tidy.pprint(doc, baos);
        String code = null;
        try {
            code = new String(baos.toByteArray(), "us-ascii");
        } catch (UnsupportedEncodingException e) {
        }
        return code;
    }

    /**
	 * Extract the body portion of the given HTML string code. If escaped PHP processor
	 * instructions are found, they are unescaped. 
	 * @param code a portion of HTML code
	 * @return the body portion of the HTML code
	 */
    public static String getBodyFromXHtml(String code) {
        int start = 0;
        Matcher m = startBodyPattern.matcher(code);
        if (m.find()) {
            start = m.end();
        }
        int end = code.length();
        m = endBodyPattern.matcher(code);
        if (m.find()) {
            end = m.start();
        }
        code = code.substring(start, end);
        StringBuffer sb = new StringBuffer();
        int lastIndex = 0;
        m = phpPattern.matcher(code);
        while (m.find()) {
            sb.append(code.substring(lastIndex, m.start()));
            sb.append("<?");
            sb.append(m.group(1));
            sb.append(xmlUnescape(m.group(2)));
            sb.append("?>");
            lastIndex = m.end();
        }
        sb.append(code.substring(lastIndex));
        code = sb.toString();
        return code;
    }

    /**
	 * Serializes the given DOM into an indented string
	 * @param node the DOM to be serialized
	 * @return a nice-to-look-at string representation
	 */
    public static String prettySerialize(Node node) {
        StringBuffer sb = new StringBuffer();
        prettySerializeNode(node, sb, "", false);
        return sb.toString();
    }

    /**
	 * Serializes the nodes below the given DOM into an indented string
	 * @param node the DOM to be serialized
	 * @return a nice-to-look-at string representation
	 */
    public static String prettySerializeContent(Node node) {
        StringBuffer sb = new StringBuffer();
        node = node.getFirstChild();
        while (node != null) {
            prettySerializeNode(node, sb, "", false);
            node = node.getNextSibling();
        }
        return sb.toString();
    }

    /**
     * Serialize a DOM branch as a string in a human-readable indented way,
     * optionnally including XPath as comment before each element
     * @param node the root branch
     * @param sb the buffer to write output to
     * @param indent indentation
     * @param xpath if <code>true</code>, the XPath of each element is
     * written as a comment before the element
     */
    public static void prettySerializeNode(org.w3c.dom.Node node, StringBuffer sb, String indent, boolean xpath) {
        if (node.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
            String text = node.getNodeValue();
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == 0xA0) sb.append("&nbsp;"); else sb.append(c);
            }
        } else if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            if (xpath == true) {
                sb.append("\n<!-- ");
                sb.append(generateXPath(node, node.getOwnerDocument().getDocumentElement()));
                sb.append(" -->");
            }
            sb.append("\n" + indent);
            sb.append("<");
            sb.append(node.getNodeName());
            NamedNodeMap children = node.getAttributes();
            for (int i = 0; i < children.getLength(); i++) {
                org.w3c.dom.Node attr = children.item(i);
                sb.append(" " + attr.getNodeName() + "=\"" + attr.getNodeValue() + "\"");
            }
            if (node.getFirstChild() == null && notSingleTags.get(node.getNodeName().toLowerCase()) == null) {
                sb.append("/>");
            } else {
                sb.append(">");
                boolean lastChildText = false;
                for (org.w3c.dom.Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
                    prettySerializeNode(child, sb, indent + "  ", xpath);
                    if (child.getNodeType() == Node.TEXT_NODE) {
                        lastChildText = true;
                    } else {
                        lastChildText = false;
                    }
                }
                if (!lastChildText) {
                    sb.append("\n" + indent);
                }
                sb.append("</" + node.getNodeName() + ">");
            }
        }
    }

    /**
     * List of HTML tags that must not autoclose (i.e &lt;tag/&gt;)
     */
    static Hashtable notSingleTags = new Hashtable();

    static {
        notSingleTags.put("script", "");
        notSingleTags.put("a", "");
        notSingleTags.put("textarea", "");
        notSingleTags.put("iframe", "");
    }

    /**
     * Prefixes all the axioms in the given xpath with the <code>xhtml:</code> prefix  
     * @param xpath the original xpath
     * @return the same xpath with axioms prefixed
     */
    public static String makeXHtmlXPath(String xpath) {
        StringBuffer sb = new StringBuffer();
        String[] axioms = xpath.split("/");
        for (int j = 0; j < axioms.length; j++) {
            String axiom = axioms[j];
            if (!axiom.equals("")) {
                if (j > 0 || xpath.charAt(0) == '/') sb.append('/');
                sb.append("xhtml:");
                sb.append(axiom.toLowerCase());
            }
        }
        return sb.toString();
    }

    /**
	 * Returns a string representation of the given node (and nodes below). The returned string
	 * is not longer XML.
	 * @param node the node to get representation from
	 * @return the string representation
	 */
    public static String getTagSignature(Node node) {
        StringBuffer sb = new StringBuffer();
        getTagSignature(node, sb);
        return sb.toString();
    }

    /**
	 * Processing function for {@link #getTagSignature(Node)}
	 * @param node
	 * @param sb
	 */
    static void getTagSignature(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            sb.append(node.getNodeName() + "/");
            node = node.getFirstChild();
            while (node != null) {
                getTagSignature(node, sb);
                node = node.getNextSibling();
            }
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append("#/");
        }
        sb.append("../");
    }

    /**
	 * Returns a deep copy of the given DOM node. The copied nodes belongs to the same document
	 * as the original nodes
	 * @param node the node to be copied
	 * @return a new DOM
	 */
    public static Node cloneNode(Node node) {
        return cloneNode(node, null);
    }

    /**
	 * Returns a deep copy of the given DOM node. The new nodes are created within the
	 * given document, if not null, but not appended to it.
	 * @param node the node to copy from
	 * @param doc the document where to create new nodes
	 * @return a new DOM
	 */
    public static Node cloneNode(Node node, Document doc) {
        if (doc == null) doc = node.getOwnerDocument();
        String nameSpaceURI = doc.getNamespaceURI();
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Node node0;
            if (nameSpaceURI != null) node0 = doc.createElementNS(doc.getNamespaceURI(), node.getNodeName()); else node0 = doc.createElement(node.getNodeName());
            NamedNodeMap nnm = ((Element) node).getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                Node attrNode = nnm.item(i);
                ((Element) node0).setAttribute(attrNode.getNodeName(), attrNode.getNodeValue());
            }
            Node node1 = node.getFirstChild();
            while (node1 != null) {
                Node node2 = cloneNode(node1, doc);
                if (node2 != null) node0.appendChild(node2);
                node1 = node1.getNextSibling();
            }
            return node0;
        } else if (node.getNodeType() == Node.TEXT_NODE) {
            return doc.createTextNode(node.getNodeValue());
        } else {
            return null;
        }
    }

    /**
	 * Walk through the given DOM to find an element with an attribute mathching the given
	 * parameters 
	 * @param node the node to start the search from
	 * @param attrName the name of the searched attribute
	 * @param attrValue the value for the attribute
	 * @return a matching element or null if not found
	 */
    public static Element getElementByAttribute(Node node, String attrName, String attrValue) {
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) node;
            if (elem.getAttribute(attrName).equals(attrValue)) return elem;
            node = node.getFirstChild();
            while (node != null) {
                elem = getElementByAttribute(node, attrName, attrValue);
                if (elem != null) return elem;
                node = node.getNextSibling();
            }
        }
        return null;
    }

    /**
     * Configuration properties for Tidy
     */
    static Properties tidyProperties = new Properties();

    static {
        tidyProperties.setProperty("ascii-chars", "true");
        tidyProperties.setProperty("new-inline-tags", "noscript");
        tidyProperties.setProperty("char-encoding", "ascii");
        tidyProperties.setProperty("drop-empty-paras", "true");
        tidyProperties.setProperty("drop-font-tags", "false");
        tidyProperties.setProperty("uppercase-tags", "true");
        tidyProperties.setProperty("output-xhtml", "true");
        tidyProperties.setProperty("enclose-text", "true");
    }

    /**
     * Parses an HTML stream using Tidy parser
     * @param is the stream for the HTML data
     * @return the parsed document
	 * @see <a href="http://sourceforge.net/projects/jtidy">JTidy</a>
     */
    public static Document tidyParse(InputStream is) {
        return tidyParse(is, null);
    }

    /**
     * Parses an HTML stream using Tidy parser
     * @param is the stream for the HTML data
	 * @param encoding the encoding to be used for parsing the data. If null,
	 * default to <code>utf-8</code>
     * @return the parsed document
	 * @see <a href="http://sourceforge.net/projects/jtidy">JTidy</a>
	 */
    public static Document tidyParse(InputStream is, String encoding) {
        Tidy tidy = new Tidy();
        Configuration config = tidy.getConfiguration();
        config.addProps(tidyProperties);
        if (encoding.equalsIgnoreCase("utf-8")) tidy.setCharEncoding(Configuration.UTF8); else if (encoding.equalsIgnoreCase("iso-8859-1")) tidy.setCharEncoding(Configuration.LATIN1); else if (encoding.equalsIgnoreCase("us-ascii")) tidy.setCharEncoding(Configuration.ASCII);
        StringWriter sw = new StringWriter();
        tidy.setErrout(new PrintWriter(sw));
        Document doc = tidy.parseDOM(is, null);
        return doc;
    }

    /** 
	 * Parses an HTML stream using Neko parser
     * @param is the stream for the HTML data
     * @return the parsed document
	 * @see <a href="http://people.apache.org/~andyc/neko/doc/html/index.html">Neko</a>
	 */
    public static Document nekoParse(InputStream is) {
        org.apache.xerces.xni.parser.XMLParserConfiguration config = new org.cyberneko.html.HTMLConfiguration();
        config.setFeature("http://cyberneko.org/html/features/augmentations", true);
        config.setProperty("http://cyberneko.org/html/properties/names/elems", "lower");
        org.apache.xerces.parsers.DOMParser parser = new org.apache.xerces.parsers.DOMParser(config);
        try {
            parser.parse(new InputSource(is));
        } catch (Exception e) {
            return null;
        }
        Document doc = parser.getDocument();
        if (doc == null) return null;
        Element elem = (Element) doc.getDocumentElement();
        if (elem == null) return null;
        elem.setAttribute("xmlns", "http://www.w3.org/1999/xhtml");
        return doc;
    }

    /**
	 * Parse an XML string using Xerces parser
	 * @param str the XML string to be parsed
	 * @return the parsed document or null if the string could not be parsed
	 */
    public static Document parseString(String str) {
        return parseStream(new ByteArrayInputStream(str.getBytes()));
    }

    /**
	 * Parse an XML stream using Xerces parser
	 * @param inputStream the XML stream to be parsed
	 * @return the parsed document or null if the string could not be parsed
	 */
    public static Document parseStream(InputStream inputStream) {
        org.apache.xerces.parsers.DOMParser parser = new org.apache.xerces.parsers.DOMParser();
        InputSource is = new InputSource(inputStream);
        try {
            parser.setEntityResolver(new ChromeEntityResolver(parser.getEntityResolver()));
            parser.parse(is);
        } catch (SAXException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return parser.getDocument();
    }

    /**
	 * Create an empty XML document
	 * @return a new document
	 */
    public static Document createDocument() {
        return documentBuilder.newDocument();
    }

    /**
	 * Create a new XML document having the given document element
	 * @param rootTagName the tag name for the root element
	 * @return a new document
	 */
    public static Document createDocument(String rootTagName) {
        return domImplementation.createDocument(null, rootTagName, null);
    }

    /**
     * Return the XPath that describes the given node.
     * The xpath expression starts with <code>BODY</code>. Eventual leaf text node
     * is indicated as <code>text()</code>. All path elements display their
     * element index. For instance <code>generateXPath</code> could return
     * <code>BODY/TABLE[2]/TR[3]/TD[1]/text()[2]</code>
     *
     * @param node the node we want the xpath expression for.
     * @param rootNode the rootNode where to stop generating the xpath
     * @return String
     */
    public static String generateXPath(Node node, Node rootNode) {
        String str = "";
        if (node == rootNode) {
            str = ".";
        } else if (node.getParentNode() == rootNode) {
            str = node.getNodeName();
        } else {
            str = generateXPath(node.getParentNode(), rootNode) + "/";
            if (node.getNodeType() == Node.TEXT_NODE) {
                str += "text()";
            } else {
                str += node.getNodeName();
            }
            int index = 1;
            Node node0 = node.getParentNode().getFirstChild();
            while (node0 != node) {
                String nodeName = node0.getNodeName();
                if (nodeName != null && nodeName.equals(node.getNodeName())) {
                    index++;
                }
                node0 = node0.getNextSibling();
            }
            str += "[" + index + "]";
        }
        return str;
    }

    /**
     * Returns a human readable representation of the given node type
     * @param type the value from <code>Node.getNodeType()</code>
     * @return the type representation
     */
    public static String getNodeTypeName(short type) {
        switch(type) {
            case Node.ATTRIBUTE_NODE:
                return "attribute";
            case Node.CDATA_SECTION_NODE:
                return "cdata_section";
            case Node.COMMENT_NODE:
                return "comment";
            case Node.DOCUMENT_FRAGMENT_NODE:
                return "document_fragment";
            case Node.DOCUMENT_NODE:
                return "document";
            case Node.DOCUMENT_TYPE_NODE:
                return "document_type";
            case Node.ELEMENT_NODE:
                return "element";
            case Node.ENTITY_NODE:
                return "entity";
            case Node.ENTITY_REFERENCE_NODE:
                return "entity_reference";
            case Node.NOTATION_NODE:
                return "notation";
            case Node.PROCESSING_INSTRUCTION_NODE:
                return "processing_instruction";
            case Node.TEXT_NODE:
                return "text";
            default:
                return "unkonwn_" + type;
        }
    }

    /**
     * Used when MD5 signature could not be calculated
     */
    public static final String badSignature = "FFFFFFFFFFFFFFFF";

    /**
     * Get a MD5 signature from the given document
     * @param doc the document to sign
     * @return the MD5 signature
     */
    public static String getMD5Signature(Document doc) {
        if (doc == null) return badSignature;
        return getMD5Signature(doc.getDocumentElement());
    }

    /**
     * Get a MD5 signature from the given DOM
     * @param rootNode the root of the dom
     * @return the DOM MD5 signature
     */
    public static String getMD5Signature(Node rootNode) {
        if (rootNode == null) return badSignature;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return badSignature;
        }
        getSignature(rootNode, md);
        return getStringSignature(md);
    }

    /**
     * Return a MD5 signature form the given text
     * @param text the text to be signed
     * @return the MD5 signature
     */
    public static String getMD5Signature(String text) {
        if (text == null) return badSignature;
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return badSignature;
        }
        md.update(text.getBytes());
        return getStringSignature(md);
    }

    /**
     * Convert a MD5 digest into an haxadecimal string representation
     * @param md the digest containing the signature
     * @return something like <code>A023C...F32</code>
     */
    public static String getStringSignature(MessageDigest md) {
        StringBuffer sb = new StringBuffer();
        byte[] sign = md.digest();
        for (int i = 0; i < sign.length; i++) {
            byte b = sign[i];
            int in = (int) b;
            if (in < 0) in = 127 - b;
            String hex = Integer.toHexString(in).toUpperCase();
            if (hex.length() == 1) hex = "0" + hex;
            sb.append(hex);
        }
        return sb.toString();
    }

    /**
     * Recursive method for processing {@link #getMD5Signature(Node)} 
     * @param node
     * @param md
     */
    private static void getSignature(Node node, MessageDigest md) {
        if (node == null || node.getNodeName() == null) return;
        md.update(node.getNodeName().getBytes());
        if (node.getNodeType() == Node.TEXT_NODE) md.update(node.getNodeValue().getBytes()); else if (node.getNodeType() == Node.ELEMENT_NODE) {
            Element elem = (Element) node;
            List attrs = new Vector();
            NamedNodeMap nnm = elem.getAttributes();
            for (int i = 0; i < nnm.getLength(); i++) {
                attrs.add(nnm.item(i).getNodeName());
            }
            Collections.sort(attrs);
            Iterator j = attrs.iterator();
            while (j.hasNext()) {
                String attrName = (String) j.next();
                md.update(attrName.getBytes());
                String attrValue = elem.getAttribute(attrName);
                if (attrValue != null) md.update(attrValue.getBytes());
            }
            node = node.getFirstChild();
            while (node != null) {
                getSignature(node, md);
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Copy the given input stream to the given output stream
     * @param input the input stream
     * @param output the output stream
     * @throws IOException
     */
    public static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
        }
    }

    /**
     * Generate a Javascript-like representation of the given Java object. Recognized types
     * are Map, List and String.
     * @param obj the object to get a representation from
     * @return an indented javascript-like string
     */
    public static String dumpObject(Object obj) {
        StringBuffer sb = new StringBuffer();
        dumpObject(obj, sb, 0);
        return sb.toString();
    }

    /**
     * Processing function for {@link #dumpObject(Object)}
     * @param obj
     * @param sb
     * @param depth
     */
    private static void dumpObject(Object obj, StringBuffer sb, int depth) {
        if (obj instanceof List) {
            sb.append("[\n");
            Iterator j = ((List) obj).iterator();
            while (j.hasNext()) {
                Object obj1 = j.next();
                for (int i = 0; i <= depth; i++) sb.append(' ');
                dumpObject(obj1, sb, depth + 1);
                if (j.hasNext()) sb.append(",\n");
            }
            sb.append('\n');
            for (int i = 0; i < depth; i++) sb.append(' ');
            sb.append("]");
        } else if (obj instanceof Map) {
            sb.append("{\n");
            Iterator j = ((Map) obj).keySet().iterator();
            while (j.hasNext()) {
                Object key = j.next();
                for (int i = 0; i <= depth; i++) sb.append(' ');
                dumpObject(key, sb, depth + 1);
                sb.append(": ");
                dumpObject(((Map) obj).get(key), sb, depth + 1);
                if (j.hasNext()) sb.append(",\n");
            }
            sb.append('\n');
            for (int i = 0; i < depth; i++) sb.append(' ');
            sb.append("}");
        } else if (obj instanceof String) {
            sb.append("\"");
            sb.append(obj.toString());
            sb.append("\"");
        } else {
            sb.append(obj.toString());
        }
    }

    /**
     * Returns the number of nodes below (and including) the given node
     * @param node the node to start count from
     * @return the total number of nodes
     */
    public static int getNodeCount(Node node) {
        int count = 1;
        node = node.getFirstChild();
        while (node != null) {
            count += getNodeCount(node);
            node = node.getNextSibling();
        }
        return count;
    }

    /**
     * Return the index of the given node in the list of parent's children
     * @param child Node
     * @return int the index of the given node
     */
    public static int getNodeChildAbsoluteIndex(Node child) {
        Node parent = child.getParentNode();
        int i = 0;
        Node node = parent.getFirstChild();
        while (node != null) {
            if (node == child) {
                return i;
            }
            i++;
            node = node.getNextSibling();
        }
        return -1;
    }

    /**
     * Returns the first child element of the given element
     * @param elem the parent element
     * @return given element's first element child
     */
    public static final Element getFirstElementChild(Element elem) {
        Node node = elem.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * Returns the next element with the same parent as the given element
     * @param elem brother element
     * @return next element or null
     */
    public static final Element getNextElementSibling(Element elem) {
        Node node = elem.getNextSibling();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                return (Element) node;
            }
            node = node.getNextSibling();
        }
        return null;
    }

    /**
     * Returns the first child element of the given element, having the
     * given tag name
     * @param elem parent element
     * @param elemName the element tag name
     * @return the element found or null
     */
    public static final Element getFirstElementChild(Element elem, String elemName) {
        Element child = getFirstElementChild(elem);
        while (child != null) {
            if (child.getTagName().equals(elemName)) {
                return child;
            }
            child = getNextElementSibling(child);
        }
        return null;
    }

    /**
     * Returns the entire text contained into Text and CData nodes below
     * a given node
     * @param node the branch where to get text from
     * @return String the extracted text
     */
    public static final String getTextValue(Node node) {
        StringBuffer sb = new StringBuffer();
        getTextValue(node, sb);
        return sb.toString();
    }

    /**
     * Fill recursively a StringBuffer with text contained into Text and
     * CData nodes below a given node
     * @param node the branch where to get text from
     * @param sb the StringBuffer to fill content with
     */
    static void getTextValue(Node node, StringBuffer sb) {
        if (node.getNodeType() == Node.TEXT_NODE) {
            sb.append(node.getNodeValue());
        } else if (node.getNodeType() == Node.CDATA_SECTION_NODE) {
            sb.append(node.getNodeValue());
        } else if (node.getNodeType() == Node.ELEMENT_NODE) {
            node = ((Element) node).getFirstChild();
            while (node != null) {
                getTextValue(node, sb);
                node = node.getNextSibling();
            }
        }
    }

    /**
     * Return the <code>BODY</code> element of a document
     * @param doc Document
     * @return Node
     */
    public static final Node getBody(Document doc) {
        return getBody(doc.getDocumentElement());
    }

    /**
     * Return the <code>BODY</code> element below a given node
     * @param node Node
     * @return Node
     */
    public static final Node getBody(Node node) {
        if (!(node instanceof Element)) {
            return null;
        }
        Element elem = (Element) node;
        NodeList bodies = elem.getElementsByTagName("body");
        if (bodies.getLength() == 0) {
            return null;
        }
        return bodies.item(0);
    }

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_DISCONNECTED = 0x01;

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_PRECEDING = 0x02;

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_FOLLOWING = 0x04;

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_CONTAINS = 0x08;

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_CONTAINED_BY = 0x10;

    /**
     * @see <a href="http://www.w3.org/TR/2003/WD-DOM-Level-3-Core-20030226/idl-definitions.html">W3C definition</a>
     */
    public static final short DOCUMENT_POSITION_BUG = 0x20;

    /**
     * Compares position of 2 nodes within a DOM.
     * @param node0 first node
     * @param node1 second node
     * @return combination of <code>DOCUMENT_POSITION_xxx</code> with each <code>xxx<code> one of<ul>
     * <li>PRECEDING</li>
     * <li>FOLLOWING</li>
     * <li>CONTAINS</li>
     * <li>CONTAINED_BY</li>
     * <li>DISCONNECTED</li></ul>
     */
    public static short compareDocumentPosition(Node node0, Node node1) {
        if (node1 == node0) {
            return DOCUMENT_POSITION_PRECEDING | DOCUMENT_POSITION_FOLLOWING | DOCUMENT_POSITION_CONTAINS | DOCUMENT_POSITION_CONTAINED_BY;
        }
        Vector nodes0 = new Vector();
        while (node0 != null) {
            nodes0.add(0, node0);
            node0 = node0.getParentNode();
        }
        Vector nodes1 = new Vector();
        while (node1 != null) {
            nodes1.add(0, node1);
            node1 = node1.getParentNode();
        }
        node0 = (Node) nodes0.remove(0);
        node1 = (Node) nodes1.remove(0);
        if (node0 != node1) {
            return DOCUMENT_POSITION_DISCONNECTED;
        }
        while (true) {
            if (node0 != node1) {
                Node node = node0.getParentNode().getFirstChild();
                while (node != null) {
                    if (node == node0) {
                        return DOCUMENT_POSITION_FOLLOWING;
                    }
                    if (node == node1) {
                        return DOCUMENT_POSITION_PRECEDING;
                    }
                    node = node.getNextSibling();
                }
                return DOCUMENT_POSITION_BUG;
            }
            if (nodes0.size() == 0) {
                return DOCUMENT_POSITION_CONTAINED_BY;
            }
            if (nodes1.size() == 0) {
                return DOCUMENT_POSITION_CONTAINS;
            }
            node0 = (Node) nodes0.remove(0);
            node1 = (Node) nodes1.remove(0);
        }
    }
}
