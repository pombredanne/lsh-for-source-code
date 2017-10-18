package bunny.x.xmlrpc;

import bunny.x.BunnyX;
import bunny.x.nio.http.HTTPRequestLine;
import bunny.x.nio.http.HTTPHeaders;
import bunny.x.xmlrpc.contenthandlers.ServiceContentHandler;
import bunny.x.xmlrpc.loopinterface.BunnyXClient;
import bunny.x.xmlrpc.servicesconfiguration.Service;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.apache.xmlrpc.Base64;

/**
 * Class providing methods to perform session related tasks.
 *
 * @author Christopher Ottley.
 */
public class BunnyxStaticSessionHandler {

    /** Random number generator. */
    private SecureRandom prng;

    /** Encoder. */
    private Base64 encoder;

    /**
   * Initialize the number generator and encoder.
   */
    public BunnyxStaticSessionHandler() {
        try {
            prng = SecureRandom.getInstance("SHA1PRNG");
            encoder = new Base64();
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Could not initialize algorithm...");
        }
    }

    /**
   * Register the request as a session request.
   * @param method HTTP method.
   * @param urlToRetrieve URL to register session for,
   * @param headers Headers used to register session.
   * @param body HTTP body.
   * @return Table containing session id to use in later requests.
   */
    public final Hashtable register(final String method, final String urlToRetrieve, final String headers, final String body) {
        Hashtable result = new Hashtable();
        String sessionId = generateId();
        Hashtable blockresult;
        try {
            blockresult = asBLOCKXML(method, urlToRetrieve, headers, body);
            if (insertId(sessionId, urlToRetrieve, (String) blockresult.get("replyline"), (String) blockresult.get("headers"), (String) blockresult.get("body"))) {
                result.put("sessionId", sessionId);
            } else {
                result.put("sessionId", "-1");
            }
        } catch (Exception e) {
            e.printStackTrace();
            result.put("sessionId", "-1");
        }
        return result;
    }

    /**
  * Get the current XML page stored in the registered session database.
  * @param sessionId The session id to use.
  * @return The current XML page for the session.
  */
    public final Hashtable getXMLPage(final String sessionId) {
        Hashtable result = new Hashtable();
        try {
            Statement st = null;
            st = BunnyX.dbconn().createStatement();
            ResultSet rs = st.executeQuery("select xmlpage from sessionpage where sessionId='" + sessionId + "'");
            rs.next();
            String xmlpage = rs.getString("xmlpage");
            st.close();
            result.put("xmlpage", xmlpage);
        } catch (Exception e) {
            System.err.println("Could not get XML Page from database...");
        }
        return result;
    }

    /**
   * Close the session specified.
   * @param sessionId The session id.
   * @return Table containing true if successful, false otherwise.
   */
    public final Hashtable close(final String sessionId) {
        Hashtable result = new Hashtable();
        boolean bresult = false;
        try {
            Statement st = null;
            st = BunnyX.dbconn().createStatement();
            st.executeQuery("delete from sessionpage where sessionId='" + sessionId + "'");
            st.close();
            bresult = true;
        } catch (Exception e) {
            System.err.println("Could not delete database session...");
        }
        result.put("boolean", bresult + "");
        return result;
    }

    /**
   * Get the available service methods for the current session's xml page.
   * @param sessionId The session id.
   * @return Table containing methods, signature, parameter defaults
   *         and parameter options.
   */
    public final Hashtable getMethods(final String sessionId) {
        Hashtable result = new Hashtable();
        try {
            Statement st = null;
            st = BunnyX.dbconn().createStatement();
            ResultSet rs = st.executeQuery("select xmlpage from sessionpage where sessionId='" + sessionId + "'");
            rs.next();
            String xmlpage = rs.getString("xmlpage");
            st.close();
            ServiceContentHandler ch = new ServiceContentHandler();
            XMLReader parser = XMLReaderFactory.createXMLReader("hotsax.html.sax.SaxParser");
            parser.setContentHandler(ch);
            xmlpage = removeDOCTYPE(removeData(xmlpage));
            parser.parse(new InputSource(new StringReader(xmlpage)));
            Hashtable services = ch.services();
            Vector resultMethods = new Vector();
            Vector resultMethodsSignature = new Vector();
            Vector resultMethodsSignatureDefaults = new Vector();
            Vector resultMethodsSignatureOptions = new Vector();
            for (Enumeration e = services.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                resultMethods.add(key);
                resultMethodsSignature.add(((Service) services.get(key)).getParameterList().toArray());
                resultMethodsSignatureDefaults.add(((Service) services.get(key)).getParameterValueList().toArray());
                resultMethodsSignatureOptions.add(((Service) services.get(key)).getParameterValueOptionsList().toArray());
            }
            result.put("methods", resultMethods.toArray());
            result.put("methodsignature", resultMethodsSignature.toArray());
            result.put("methodsignaturedefaults", resultMethodsSignatureDefaults.toArray());
            result.put("methodsignatureoptions", resultMethodsSignatureOptions.toArray());
        } catch (Exception e) {
            System.err.println("Could not get method list...");
        }
        return result;
    }

    /**
   * Remove the data segment from a Block XML string.
   * @param source The source Block XML string.
   * @return The Block XML string with no data section.
   */
    private String removeData(final String source) {
        Pattern dataSegment = Pattern.compile("<(\\p{Space})*data(\\p{Space})*>.*?" + "<(\\p{Space})*/(\\p{Space})*data(\\p{Space})*>", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
        Matcher dataSegmetMatcher = dataSegment.matcher(source);
        return dataSegmetMatcher.replaceAll("");
    }

    /**
   * The SAX Processor chokes on HTML processing if there is a DOCTYPE
   * with a URL that doesn't resolve or match the definition of the
   * document being processed.
   * This removes the doctype tag from the html page.
   * @param source The source string to remove the doctype from.
   * @return The string with no doctype, internal or external.
   */
    private String removeDOCTYPE(final String source) {
        String result;
        Pattern idocTypeTag = Pattern.compile("<(\\p{Space})*!DOCTYPE.*?]>", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
        Pattern edocTypeTag = Pattern.compile("<(\\p{Space})*!DOCTYPE.*?>", Pattern.CASE_INSENSITIVE + Pattern.DOTALL);
        Matcher docTypeMatcher = idocTypeTag.matcher(source.toString());
        result = docTypeMatcher.replaceAll("");
        docTypeMatcher = edocTypeTag.matcher(result.toString());
        result = docTypeMatcher.replaceAll("");
        return result;
    }

    /**
   * Add session information to database.
   * @param sessionId The session id to add.
   * @param nurlToRetrieve The current URL the session is at.
   * @param nreplyline The server reply line after retrieving the URL.
   * @param nheaders The headers sent back by the server.
   * @param nblockxml The page body in XML Block format.
   * @return true if insertion was successful.
   */
    private synchronized boolean insertId(final String sessionId, final String nurlToRetrieve, final String nreplyline, final String nheaders, final String nblockxml) {
        boolean inserted = true;
        try {
            String urlToRetrieve;
            String replyline;
            String headers;
            String blockxml;
            Statement st = null;
            st = BunnyX.dbconn().createStatement();
            if (nurlToRetrieve == null) {
                urlToRetrieve = "";
            } else {
                urlToRetrieve = nurlToRetrieve;
            }
            if (nreplyline == null) {
                replyline = "";
            } else {
                replyline = nreplyline;
            }
            if (nheaders == null) {
                headers = "";
            } else {
                headers = nheaders;
            }
            if (nblockxml == null) {
                blockxml = "";
            } else {
                blockxml = nblockxml;
            }
            String sql = "INSERT INTO sessionpage(sessionId,url,replyline," + "headers,xmlpage) VALUES('" + sessionId + "', '" + urlToRetrieve.replaceAll("'", "''") + "', '" + replyline.replaceAll("'", "''") + "', '" + headers.replaceAll("'", "''") + "', '" + blockxml.replaceAll("'", "''") + "')";
            st.executeUpdate(sql);
            st.close();
        } catch (Exception e) {
            inserted = false;
            System.err.println("Could not add session to database...");
        }
        return inserted;
    }

    /**
   * Request the URL using the method and parameters and return
   * the result as block XML.
   * @param method The HTTP method.
   * @param urlToRetrieve The URL to retrieve.
   * @param headers The headers to send with the request.
   * @param body HTTP Body if post method.
   * @return Table containing replyline, headers and body from the server.
   * @throws MalformedURLException If the urlToRetrieve is malformed.
   */
    private Hashtable asBLOCKXML(final String method, final String urlToRetrieve, final String headers, final String body) throws MalformedURLException {
        Hashtable result = new Hashtable();
        URL dest = new URL(urlToRetrieve);
        HTTPRequestLine reqLine = new HTTPRequestLine(method, dest.toString(), "HTTP/1.0");
        HTTPHeaders reqHeaders = new HTTPHeaders();
        reqHeaders.parse(headers);
        reqHeaders.put("Host", dest.getHost());
        reqHeaders.put("X-ynnuB-FormatAs", "XMLBLOCK");
        reqHeaders.put("Accept-Encoding", "identity");
        reqHeaders.put("X-ynnuB-URLPrepend", "");
        BunnyXClient client = new BunnyXClient();
        try {
            client.sendRequest(reqLine, reqHeaders, body);
            result.put("replyline", client.replyLine());
            result.put("headers", client.replyHeaders());
            result.put("body", client.replyBody());
        } catch (Exception e) {
            System.err.println("Error making XML Block request for url...");
        }
        return result;
    }

    /**
   * Dump the contents of the current database.
   */
    private void dumpsession() {
        final int sessionIndex = 1;
        final int pageIndex = 3;
        try {
            Statement stmt = BunnyX.dbconn().createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM sessionpage");
            while (rs.next()) {
                String s = rs.getString(sessionIndex);
                String t = rs.getString(pageIndex);
                System.out.println(s + " " + t);
            }
        } catch (Exception e) {
            System.err.println("Cannot query database...");
        }
    }

    /**
   * Generate a unique identifier for use as a registered session id.
   * @return A unique session id.
   *         -1 if something went wrong.
   */
    private String generateId() {
        String strResult = "-1";
        try {
            String randomNum = new Integer(prng.nextInt()).toString();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] result = md5.digest(randomNum.getBytes());
            strResult = new String(encoder.encode(result));
            strResult = strResult.trim();
        } catch (NoSuchAlgorithmException ex) {
            System.err.println("Cannot load MD5 algorithm...");
        } catch (Exception e) {
            System.err.println("Cannot encode result id...");
        }
        return strResult;
    }
}
