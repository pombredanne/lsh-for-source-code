package org.apache.nutch.tools;

import java.io.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.apache.xerces.util.XMLChar;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.nutch.util.LogUtil;
import org.apache.nutch.util.NutchConfiguration;

/** Utility that converts DMOZ RDF into a flat file of URLs to be injected. */
public class DmozParser {

    public static final Log LOG = LogFactory.getLog(DmozParser.class);

    long pages = 0;

    /**
   * This filter fixes characters that might offend our parser.
   * This lets us be tolerant of errors that might appear in the input XML.
   */
    private static class XMLCharFilter extends FilterReader {

        private boolean lastBad = false;

        public XMLCharFilter(Reader reader) {
            super(reader);
        }

        public int read() throws IOException {
            int c = in.read();
            int value = c;
            if (c != -1 && !(XMLChar.isValid(c))) value = 'X'; else if (lastBad && c == '<') {
                in.mark(1);
                if (in.read() != '/') value = 'X';
                in.reset();
            }
            lastBad = (c == 65533);
            return value;
        }

        public int read(char[] cbuf, int off, int len) throws IOException {
            int n = in.read(cbuf, off, len);
            if (n != -1) {
                for (int i = 0; i < n; i++) {
                    char c = cbuf[off + i];
                    char value = c;
                    if (!(XMLChar.isValid(c))) value = 'X'; else if (lastBad && c == '<') {
                        if (i != n - 1 && cbuf[off + i + 1] != '/') value = 'X';
                    }
                    lastBad = (c == 65533);
                    cbuf[off + i] = value;
                }
            }
            return n;
        }
    }

    /**
   * The RDFProcessor receives tag messages during a parse
   * of RDF XML data.  We build whatever structures we need
   * from these messages.
   */
    private class RDFProcessor extends DefaultHandler {

        String curURL = null, curSection = null;

        boolean titlePending = false, descPending = false, insideAdultSection = false;

        Pattern topicPattern = null;

        StringBuffer title = new StringBuffer(), desc = new StringBuffer();

        XMLReader reader;

        int subsetDenom;

        int hashSkew;

        boolean includeAdult;

        Locator location;

        /**
     * Pass in an XMLReader, plus a flag as to whether we 
     * should include adult material.
     */
        public RDFProcessor(XMLReader reader, int subsetDenom, boolean includeAdult, int skew, Pattern topicPattern) throws IOException {
            this.reader = reader;
            this.subsetDenom = subsetDenom;
            this.includeAdult = includeAdult;
            this.topicPattern = topicPattern;
            this.hashSkew = skew != 0 ? skew : new Random().nextInt();
        }

        /**
     * Start of an XML elt
     */
        public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
            if ("Topic".equals(qName)) {
                curSection = atts.getValue("r:id");
            } else if ("ExternalPage".equals(qName)) {
                if ((!includeAdult) && curSection.startsWith("Top/Adult")) {
                    return;
                }
                if (topicPattern != null && !topicPattern.matcher(curSection).matches()) {
                    return;
                }
                String url = atts.getValue("about");
                int hashValue = MD5Hash.digest(url).hashCode();
                hashValue = Math.abs(hashValue ^ hashSkew);
                if ((hashValue % subsetDenom) != 0) {
                    return;
                }
                curURL = url;
            } else if (curURL != null && "d:Title".equals(qName)) {
                titlePending = true;
            } else if (curURL != null && "d:Description".equals(qName)) {
                descPending = true;
            }
        }

        /**
     * The contents of an XML elt
     */
        public void characters(char ch[], int start, int length) {
            if (titlePending) {
                title.append(ch, start, length);
            } else if (descPending) {
                desc.append(ch, start, length);
            }
        }

        /**
     * Termination of XML elt
     */
        public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
            if (curURL != null) {
                if ("ExternalPage".equals(qName)) {
                    System.out.println(curURL);
                    pages++;
                    if (title.length() > 0) {
                        title.delete(0, title.length());
                    }
                    if (desc.length() > 0) {
                        desc.delete(0, desc.length());
                    }
                    curURL = null;
                } else if ("d:Title".equals(qName)) {
                    titlePending = false;
                } else if ("d:Description".equals(qName)) {
                    descPending = false;
                }
            }
        }

        /**
     * When parsing begins
     */
        public void startDocument() {
            if (LOG.isInfoEnabled()) {
                LOG.info("Begin parse");
            }
        }

        /**
     * When parsing ends
     */
        public void endDocument() {
            if (LOG.isInfoEnabled()) {
                LOG.info("Completed parse.  Found " + pages + " pages.");
            }
        }

        /**
     * From time to time the Parser will set the "current location"
     * by calling this function.  It's useful for emitting locations
     * for error messages.
     */
        public void setDocumentLocator(Locator locator) {
            location = locator;
        }

        /**
     * Emit the exception message
     */
        public void error(SAXParseException spe) {
            if (LOG.isFatalEnabled()) {
                LOG.fatal("Error: " + spe.toString() + ": " + spe.getMessage());
                spe.printStackTrace(LogUtil.getFatalStream(LOG));
            }
        }

        /**
     * Emit the exception message, with line numbers
     */
        public void fatalError(SAXParseException spe) {
            if (LOG.isFatalEnabled()) {
                LOG.fatal("Fatal err: " + spe.toString() + ": " + spe.getMessage());
                LOG.fatal("Last known line is " + location.getLineNumber() + ", column " + location.getColumnNumber());
                spe.printStackTrace(LogUtil.getFatalStream(LOG));
            }
        }

        /**
     * Emit exception warning message
     */
        public void warning(SAXParseException spe) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Warning: " + spe.toString() + ": " + spe.getMessage());
                spe.printStackTrace(LogUtil.getWarnStream(LOG));
            }
        }
    }

    /**
   * Iterate through all the items in this structured DMOZ file.
   * Add each URL to the web db.
   */
    public void parseDmozFile(File dmozFile, int subsetDenom, boolean includeAdult, int skew, Pattern topicPattern) throws IOException, SAXException, ParserConfigurationException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser parser = parserFactory.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        RDFProcessor rp = new RDFProcessor(reader, subsetDenom, includeAdult, skew, topicPattern);
        reader.setContentHandler(rp);
        reader.setErrorHandler(rp);
        if (LOG.isInfoEnabled()) {
            LOG.info("skew = " + rp.hashSkew);
        }
        XMLCharFilter in = new XMLCharFilter(new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(dmozFile)), "UTF-8")));
        try {
            InputSource is = new InputSource(in);
            reader.parse(is);
        } catch (Exception e) {
            if (LOG.isFatalEnabled()) {
                LOG.fatal(e.toString());
                e.printStackTrace(LogUtil.getFatalStream(LOG));
            }
            System.exit(0);
        } finally {
            in.close();
        }
    }

    private static void addTopicsFromFile(String topicFile, Vector topics) throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(topicFile), "UTF-8"));
            String line = null;
            while ((line = in.readLine()) != null) {
                topics.addElement(new String(line));
            }
        } catch (Exception e) {
            if (LOG.isFatalEnabled()) {
                LOG.fatal(e.toString());
                e.printStackTrace(LogUtil.getFatalStream(LOG));
            }
            System.exit(0);
        } finally {
            in.close();
        }
    }

    /**
   * Command-line access.  User may add URLs via a flat text file
   * or the structured DMOZ file.  By default, we ignore Adult
   * material (as categorized by DMOZ).
   */
    public static void main(String argv[]) throws Exception {
        if (argv.length < 1) {
            System.err.println("Usage: DmozParser <dmoz_file> [-subset <subsetDenominator>] [-includeAdultMaterial] [-skew skew] [-topicFile <topic list file>] [-topic <topic> [-topic <topic> [...]]]");
            return;
        }
        int subsetDenom = 1;
        int skew = 0;
        String dmozFile = argv[0];
        boolean includeAdult = false;
        Pattern topicPattern = null;
        Vector topics = new Vector();
        Configuration conf = NutchConfiguration.create();
        FileSystem fs = FileSystem.get(conf);
        try {
            for (int i = 1; i < argv.length; i++) {
                if ("-includeAdultMaterial".equals(argv[i])) {
                    includeAdult = true;
                } else if ("-subset".equals(argv[i])) {
                    subsetDenom = Integer.parseInt(argv[i + 1]);
                    i++;
                } else if ("-topic".equals(argv[i])) {
                    topics.addElement(argv[i + 1]);
                    i++;
                } else if ("-topicFile".equals(argv[i])) {
                    addTopicsFromFile(argv[i + 1], topics);
                    i++;
                } else if ("-skew".equals(argv[i])) {
                    skew = Integer.parseInt(argv[i + 1]);
                    i++;
                }
            }
            DmozParser parser = new DmozParser();
            if (!topics.isEmpty()) {
                String regExp = new String("^(");
                int j = 0;
                for (; j < topics.size() - 1; ++j) {
                    regExp = regExp.concat((String) topics.get(j));
                    regExp = regExp.concat("|");
                }
                regExp = regExp.concat((String) topics.get(j));
                regExp = regExp.concat(").*");
                if (LOG.isInfoEnabled()) {
                    LOG.info("Topic selection pattern = " + regExp);
                }
                topicPattern = Pattern.compile(regExp);
            }
            parser.parseDmozFile(new File(dmozFile), subsetDenom, includeAdult, skew, topicPattern);
        } finally {
            fs.close();
        }
    }
}
