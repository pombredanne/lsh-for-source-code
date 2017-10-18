package gov.lanl.ingest.oaitape;

import gov.lanl.ingest.IngestConstants;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import javax.swing.text.EditorKit;
import javax.swing.text.ElementIterator;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import sun.misc.BASE64Encoder;

/**
 * @author rchute
 *  
 */
public class OAITapeUtils implements IngestConstants {

    /**
     * this is default utility method to do dereferencing from url
     * 
     * @param url - URL to resolve
     * @return Byte Array of resolved reference 
     */
    public byte[] resolveRef(String url) throws MalformedURLException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URL addressurl = new URL(url);
        InputStream in = addressurl.openStream();
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = in.read(buffer, 0, bufferSize)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
        byte[] bout = out.toByteArray();
        in.close();
        out.close();
        return bout;
    }

    /**
     * 
     * @param url
     * @param agent
     * @return Byte Array of Resolved Reference
     * @throws MalformedURLException
     * @throws IOException
     */
    public byte[] getBinary(String url, String agent) throws MalformedURLException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URL u = new URL(url);
        URLConnection uc = u.openConnection();
        uc.setRequestProperty(TAG_USER_AGENT, agent);
        InputStream raw = uc.getInputStream();
        InputStream in = new BufferedInputStream(raw);
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = in.read(buffer, 0, bufferSize)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
        in.close();
        return out.toByteArray();
    }

    /**
     * this is default utility method to calculate digest
     * 
     * @param streamBytes
     * @param algoritm
     *            like "SHA1"
     * @return base64 digest
     * @throws NoSuchAlgorithmException
     */
    public String calculateDigest(byte[] streamBytes, String algoritm) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algoritm);
        byte[] digest = md.digest(streamBytes);
        BASE64Encoder encoder = new BASE64Encoder();
        String coded = new String(encoder.encodeBuffer(digest));
        return coded.trim();
    }

    /**
     * this is default utility method to transform byte array of ziped stream to
     * unzipped
     * 
     * @param gzipinput
     * @return Byte Array of Unzipped Stream 
     * @throws IOException
     */
    public byte[] unzipStream(byte[] gzipinput) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(gzipinput));
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            baos.write(buf, 0, len);
        }
        return baos.toByteArray();
    }

    /**
     * For html or text download in String form
     * @param url
     * @param agent
     * @return String version of provided URL
     * @throws MalformedURLException
     * @throws IOException
     */
    public String getPage(String url, String agent) throws MalformedURLException, IOException {
        URL u = new URL(url);
        URLConnection uc = u.openConnection();
        uc.setRequestProperty(TAG_USER_AGENT, agent);
        String cT = uc.getContentType();
        String encoding = null;
        if (cT != null) {
            int i = cT.indexOf(";");
            if (i > 0) {
                encoding = cT.substring(i + 1).trim();
                int j = encoding.indexOf("=");
                if (j > 0) {
                    encoding = encoding.substring(j + 1).trim();
                }
            }
        }
        if (encoding == null) {
            encoding = uc.getContentEncoding();
        }
        if (encoding == null) {
            encoding = "UTF-8";
        }
        String inputLine;
        StringBuffer b = new StringBuffer();
        BufferedReader d = new BufferedReader(new InputStreamReader(uc.getInputStream(), encoding));
        while ((inputLine = d.readLine()) != null) {
            b.append(inputLine);
        }
        d.close();
        return b.toString();
    }

    /**
     * Parsing html to get link on full text
     * 
     * @param html
     *            page to parse
     * @return link to pdf
     */
    public String getPdfLink(String html) {
        String link = null;
        EditorKit kit = new HTMLEditorKit();
        javax.swing.text.Document doc = kit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        try {
            Reader rd = new StringReader(html);
            kit.read(rd, doc, 0);
            ElementIterator it = new ElementIterator(doc);
            javax.swing.text.Element elem;
            while ((elem = it.next()) != null) {
                SimpleAttributeSet s = (SimpleAttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
                if (s != null) {
                    String alink = (String) s.getAttribute(HTML.Attribute.HREF);
                    if (alink.startsWith("/pdf")) {
                        link = alink;
                    } else if (alink.endsWith("pdf")) {
                        link = alink;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return link;
    }

    /**
     * Parsing html to get link on full text
     * 
     * @param html
     *            page to parse
     * @param formats
     *            list of file format extensions
     * @return link to pdf
     */
    public String getLink(String html, String[] formats) {
        String link = null;
        EditorKit kit = new HTMLEditorKit();
        javax.swing.text.Document doc = kit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        try {
            Reader rd = new StringReader(html);
            kit.read(rd, doc, 0);
            ElementIterator it = new ElementIterator(doc);
            javax.swing.text.Element elem;
            while ((elem = it.next()) != null) {
                SimpleAttributeSet s = (SimpleAttributeSet) elem.getAttributes().getAttribute(HTML.Tag.A);
                if (s != null) {
                    String alink = (String) s.getAttribute(HTML.Attribute.HREF);
                    for (int i = 0; i < formats.length; i++) {
                        if (alink.startsWith("/" + formats[i])) {
                            link = alink;
                        } else if (alink.endsWith(formats[i])) {
                            link = alink;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return link;
    }

    /**
     * Downloading pdf with custom User-Agent
     * @param url
     * @return Byte Array of Provided URL
     * @throws MalformedURLException
     * @throws IOException
     */
    public byte[] getBinary(String url) throws MalformedURLException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        URL u = new URL(url);
        URLConnection uc = u.openConnection();
        uc.setRequestProperty(TAG_USER_AGENT, DEFAULT_USER_AGENT);
        InputStream raw = uc.getInputStream();
        InputStream in = new BufferedInputStream(raw);
        int bufferSize = 4096;
        byte[] buffer = new byte[bufferSize];
        int bytesRead;
        while ((bytesRead = in.read(buffer, 0, bufferSize)) != -1) {
            out.write(buffer, 0, bytesRead);
            out.flush();
        }
        in.close();
        return out.toByteArray();
    }
}
