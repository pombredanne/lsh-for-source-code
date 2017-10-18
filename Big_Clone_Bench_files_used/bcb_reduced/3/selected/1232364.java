package com.amazon.s3shell;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Simple interface class for using S3 in a basic fashion.  An instance of
 * this class is intended for a single thread to interact with a single
 * bucket in S3.
 * <p>
 * In general, methods of this class have three ways of failing.  They will
 * throw IllegalArgumentException on usage errors, IOException on network
 * errors or other errors that may be reasonably expected to succeed on
 * retry, and return false for conditions where retry will not succeed (e.g.
 * permission errors, buckets and items not existing, etc).
 * <p>
 * Instances of this class are <b>NOT</b> safe for concurrent use among multiple
 * threads.
 *
 * @author Grant Emery (c) 2006 Amazon.com
 **/
public class S3Store {

    private static S3Store instance;

    private static final Logger ourLogger = Logger.getLogger(S3Store.class.getName());

    /** S3 host to connect to */
    private final String m_host;

    /** AWS Access Key ID to connect as */
    private final String m_username;

    /** AWS Secret Access Key to connect with */
    private final String m_password;

    /** Bucket to perform operations with */
    private String m_bucket;

    /** Parser for S3 response XML messages */
    private final SAXParser m_parser;

    /** Signature algorithm used for S3 authentication: {@value} */
    private static final String SIGNATURE_ALGORITHM = "HmacSHA1";

    /** Number of millis to wait for S3 to respond to requests */
    private static final int READ_TIMEOUT = 30 * 1000;

    /**
     * Copy constructor.
     * 
     * @param copy The S3Store instance to copy [may not be null]
     **/
    public S3Store(final S3Store copy) {
        this(copy.m_host, copy.m_username, copy.m_password, copy.m_bucket);
    }

    /**
     * Standard constructor which takes basic connection parameters.  No
     * connection is made to the server until a method is called on the
     * constructed instance, so if any connection parameters are bad,
     * it won't be known until then.
     *
     * @param host The S3 storage host to connect to [may not be null]
     * @param username The Amazon Web Services Access Key ID to use to connect
     *      [may not be null]
     * @param password The Amazon Web Services Secret Access Key to use to connect
     *      [may not be null]
     **/
    public S3Store(final String host, final String username, final String password) {
        if (host == null) throw new IllegalArgumentException("host may not be null");
        if (username == null) throw new IllegalArgumentException("username may not be null");
        if (password == null) throw new IllegalArgumentException("password may not be null");
        m_host = host;
        m_username = username;
        m_password = password;
        try {
            SAXParserFactory parserfactory = SAXParserFactory.newInstance();
            parserfactory.setNamespaceAware(false);
            parserfactory.setValidating(false);
            m_parser = parserfactory.newSAXParser();
        } catch (Exception e) {
            throw new IllegalArgumentException("parser creation failed", e);
        }
    }

    /**
     * Standard constructor taking connection parameters and a default
     * bucket to use.  No connection is made until an instance method of
     * this class is called, so if these parameters are incorrect it
     * won't be known until then.
     *
     * @param host The S3 storage host to connect to [may not be null]
     * @param username The Amazon Web Services Access Key ID to use to connect
     *      [may not be null]
     * @param password The Amazon Web Services Secret Access Key to use to 
     *      connect [may not be null]
     * @param bucket The S3 bucket to use [may be null, although some operations will fail]
     **/
    public S3Store(final String host, final String username, final String password, final String bucket) {
        this(host, username, password);
        m_bucket = bucket;
    }

    /**
     * Sets the bucket to use for operations.
     * 
     * @param bucket The bucket to use [may be null, although some operations 
     *      will fail]
     **/
    public void setBucket(final String bucket) {
        m_bucket = bucket;
    }

    /**
     * Gets the bucket currently in use
     *
     * @return The bucket currently in use [may be null]
     **/
    public String getBucket() {
        return m_bucket;
    }

    /**
     * Gets the host currently used.
     */
    public String getHost() {
        return m_host;
    }

    /**
     * Creates the current bucket.
     *
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean createBucket() throws IOException {
        final HttpURLConnection bucketConn = getBucketURLConnection("PUT");
        bucketConn.connect();
        return checkResponse("createBucket", bucketConn);
    }

    /**
     * Deletes the current bucket.
     *
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean deleteBucket() throws IOException {
        final HttpURLConnection bucketConn = getBucketURLConnection("DELETE");
        bucketConn.connect();
        return checkResponse("deleteBucket", bucketConn);
    }

    /**
     * Lists the buckets owned by the current user.
     *
     * @return A List of Strings of item ids in this bucket or null if there
     * was an error.
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public List<String> listBuckets() throws IOException {
        final HttpURLConnection rootConn = getRootURLConnection("GET");
        rootConn.connect();
        if (!checkResponse("listBuckets()", rootConn)) {
            return null;
        }
        final ObjectListParser olp = new ObjectListParser("name");
        final InputStream responseData = rootConn.getInputStream();
        try {
            m_parser.parse(responseData, olp);
        } catch (SAXException e) {
            throw new IllegalArgumentException("SAX parser failed", e);
        } finally {
            responseData.close();
        }
        return olp.getList();
    }

    /**
     * Stores item data into S3.  No metadata headers are added.
     *
     * @param id The ID to store the item to [may not be null]
     * @param data The binary data to store [may not be null]
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean storeItem(final String id, final byte[] data) throws IOException {
        return storeItem(id, data, (Map<String, List<String>>) null);
    }

    private void addAclHeader(final Map<String, List<String>> headers, final String acl) {
        headers.put("x-amz-acl", Collections.singletonList(acl));
    }

    /**
     * Stores item data into S3.  No metadata headers are added.
     *
     * @param id The ID to store the item to [may not be null]
     * @param data The binary data to store [may not be null]
     * @param acl convenience param to specify an acl.  equivalent to including a header of "x-amz-acl" with this value.
     * Must be one of public-read, public-write, authenticated-read, or private (the default).    See:
     * http://docs.amazonwebservices.com/AmazonS3/latest/index.html?S3_ACLs.html for more info.
     * @param headers other headers to send.  may be null or empty.  useful for setting content-type, acls, or other user
     * meta-data.  see http://docs.amazonwebservices.com/AmazonS3/latest/index.html?UsingMetadata.html for more info.
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean storeItem(final String id, final byte[] data, final String acl) throws IOException {
        final Map<String, List<String>> headers = new HashMap<String, List<String>>(1);
        addAclHeader(headers, acl);
        return storeItem(id, data, headers);
    }

    /**
     * Stores item data into S3.  No metadata headers are added.
     *
     * @param id The ID to store the item to [may not be null]
     * @param data The binary data to store [may not be null]
     * @param acl convenience param to specify an acl.  equivalent to including a header of "x-amz-acl" with this value.
     * Must be one of public-read, public-write, authenticated-read, or private (the default).  See:
     * http://docs.amazonwebservices.com/AmazonS3/latest/index.html?S3_ACLs.html for more info.
     * @param headers other headers to send.  may be null or empty.  useful for setting content-type, acls, or other user
     * meta-data.  see http://docs.amazonwebservices.com/AmazonS3/latest/index.html?UsingMetadata.html for more info.
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean storeItem(final String id, final byte[] data, final String acl, final Map<String, List<String>> _headers) throws IOException {
        final Map<String, List<String>> headers = new HashMap<String, List<String>>();
        if (_headers != null) {
            headers.putAll(_headers);
        }
        addAclHeader(headers, acl);
        return storeItem(id, data, headers);
    }

    /**
     * Stores item data into S3.  No metadata headers are added.
     *
     * @param id The ID to store the item to [may not be null]
     * @param data The binary data to store [may not be null]
     * @param headers other headers to send.  may be null or empty.  useful for setting content-type, acls, or other user
     * meta-data.  see http://docs.amazonwebservices.com/AmazonS3/latest/index.html?UsingMetadata.html for more info.
     * @return True if the operation succeeded, false if it failed.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean storeItem(final String id, final byte[] data, final Map<String, List<String>> headers) throws IOException {
        if (id == null) throw new IllegalArgumentException("id may not be null");
        if (data == null) throw new IllegalArgumentException("data may not be null");
        final HttpURLConnection itemConn = getItemURLConnection("PUT", id, data, headers);
        itemConn.setDoOutput(true);
        itemConn.connect();
        OutputStream dataout = itemConn.getOutputStream();
        dataout.write(data);
        dataout.close();
        return checkResponse("storeItem", itemConn);
    }

    public boolean copyItem(final String fromKey, final String toKey, final String acl) throws IOException {
        if (fromKey == null || toKey == null) {
            throw new IllegalArgumentException("neither fromKey or toKey can be null");
        }
        final Map<String, List<String>> headers = new HashMap<String, List<String>>(3);
        if (acl != null) {
            addAclHeader(headers, acl);
        }
        final String fullDst = String.format("%s/%s", this.m_bucket, fromKey);
        headers.put("x-amz-copy-source", Collections.singletonList(fullDst));
        final HttpURLConnection itemConn = getItemURLConnection("PUT", toKey, null, headers);
        itemConn.connect();
        return checkResponse("copyItem", itemConn);
    }

    public Map<String, List<String>> getMeta(final String key) throws IOException {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        final HttpURLConnection itemConn = getItemURLConnection("HEAD", key, null, null);
        if (!checkResponse("getMeta", itemConn)) return null;
        return itemConn.getHeaderFields();
    }

    /**
     * Gets an item from the current bucket.
     * 
     * @param id The item to get [may not be null]
     * @return The item data, or null if there was an error (e.g. the item
     * doesn't exist)
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public S3Object getItemWithHeaders(final String id) throws IOException {
        if (id == null) throw new IllegalArgumentException("id may not be null");
        final HttpURLConnection itemConn = getItemURLConnection("GET", id, null, null);
        itemConn.connect();
        if (!checkResponse("getItem", itemConn)) return null;
        final int responseBytes = itemConn.getContentLength();
        final byte[] retval = new byte[responseBytes];
        final DataInputStream datainput = new DataInputStream(itemConn.getInputStream());
        try {
            datainput.readFully(retval);
        } finally {
            datainput.close();
        }
        return new S3Object(retval, itemConn.getHeaderFields());
    }

    public byte[] getItem(final String id) throws IOException {
        final S3Object ret = getItemWithHeaders(id);
        return (ret == null ? null : ret.getData());
    }

    /**
     * Deletes an item from the current bucket.
     *
     * @param id The item to delete [may not be null]
     * @return True on success, false on failure.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public boolean deleteItem(final String id) throws IOException {
        if (id == null) throw new IllegalArgumentException("id may not be null");
        final HttpURLConnection itemConn = getItemURLConnection("DELETE", id, null, null);
        itemConn.connect();
        return checkResponse("deleteItem", itemConn);
    }

    /**
     * Lists the contents of the current bucket from the beginning.  The number
     * of items returned may be limited by the server.
     *
     * @return A list of ids or null if there was an error.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public List<String> listItems() throws IOException {
        return listItems(null, null, 0);
    }

    /**
     * Lists those contents of the current bucket with IDs starting with
     * the given prefix.
     *
     * @param prefix The prefix to limit searches to.  If null, no restriction
     * is applied.
     * @return A list of ids or null if there was an error.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public List<String> listItems(final String prefix) throws IOException {
        return listItems(prefix, null, 0);
    }

    /**
     * Lists those contents of the current bucket with IDs starting with
     * the given prefix that occur strictly lexicographically after the
     * the given marker.
     *
     * @param prefix The prefix to limit searches to.  If null, no restriction
     * is applied.
     * @param marker The marker indicating where to start returning results.
     * If null, no restriction is applied.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public List<String> listItems(final String prefix, final String marker) throws IOException {
        return listItems(prefix, marker, 0);
    }

    /**
     * Lists those contents of the current bucket with IDs starting
     * with the given prefix that occur strictly lexicographically after
     * the given marker, limiting the results to the given maximum number.
     * 
     * @param prefix The prefix to limit searches to.  If null, no restriction
     * is applied.
     * @param marker The marker indicating where to start returning results.
     * If null, no restriction is applied.
     * @param max The maximum number of results to return.  If 0, no additional
     * restriction beyond the server default is applied.
     * @throws IllegalArgumentException If there is no bucket set
     * @throws IOException From underlying network problems or if S3 returned
     * an internal server error.
     **/
    public List<String> listItems(final String prefix, final String marker, final int max) throws IOException {
        if (max < 0) throw new IllegalArgumentException("max must be >= 0");
        final HttpURLConnection bucketConn = getBucketURLConnection("GET", prefix, marker, max);
        bucketConn.connect();
        if (!checkResponse("listItems(" + prefix + "," + marker + "," + max + ")", bucketConn)) {
            return null;
        }
        final ObjectListParser olp = new ObjectListParser("key");
        InputStream responseData = bucketConn.getInputStream();
        try {
            m_parser.parse(responseData, olp);
        } catch (SAXException e) {
            throw new IllegalArgumentException("SAX parser failed", e);
        } finally {
            responseData.close();
        }
        return olp.getList();
    }

    /**
     * Given an HttpURLConnection, this method determines whether the request
     * succeeded or not.  A request is a success if it returns a success
     * response code (generally "200 OK" or "204 No Content", depending on
     * the specific operation).  In most cases, this method returns true
     * on success, false on non-recoverable failure, or throws an IOException
     * in cases where a retry might be reasonably expected to succeed.  When
     * this method returns false, it will also print an error message to
     * the logger.
     **/
    private boolean checkResponse(final String operation, final HttpURLConnection conn) throws IOException {
        final int responseCode = conn.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_INTERNAL_ERROR) {
            throw new IOException(operation + ": internal server error");
        }
        if (responseCode == HttpURLConnection.HTTP_UNAVAILABLE) {
            throw new IOException(operation + ": service unavailable");
        }
        if (responseCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT) {
            throw new IOException(operation + ": gateway timeout");
        }
        if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
            return false;
        }
        if (responseCode / 100 != 2) {
            ourLogger.log(Level.SEVERE, String.format("%s: response code %d", operation, responseCode));
            printError(conn);
            return false;
        }
        return true;
    }

    /**
     * If a connection to S3 returned an error response code, this method
     * will parse the error response XML and send the user-visible message
     * to the logger as SEVERE.
     **/
    private void printError(final HttpURLConnection conn) throws IOException {
        final InputStream errorData = conn.getErrorStream();
        if (errorData == null) return;
        final ObjectListParser olp = new ObjectListParser("message");
        try {
            m_parser.parse(errorData, olp);
        } catch (SAXException e) {
            throw new IllegalArgumentException("SAX parser failed", e);
        } finally {
            errorData.close();
        }
        for (String msg : olp.getList()) {
            ourLogger.log(Level.SEVERE, msg);
        }
    }

    /**
     * Creates a new HttpURLConnection that refers to the S3 root level for
     * the "list buckets" operation.
     **/
    private HttpURLConnection getRootURLConnection(final String method) throws IOException {
        final String url = "http://" + m_host + "/";
        final URL rootURL = new URL(url);
        final HttpURLConnection rootConn = (HttpURLConnection) rootURL.openConnection();
        rootConn.setRequestMethod(method);
        rootConn.setReadTimeout(READ_TIMEOUT);
        addAuthorization(rootConn, method, null);
        return rootConn;
    }

    /**
     * Creates a new HttpURLConnection that refers to the current bucket for
     * operations such as bucket creation, bucket deletion, and listing
     * bucket contents.
     **/
    private HttpURLConnection getBucketURLConnection(final String method) throws IOException {
        return getBucketURLConnection(method, null, null, 0);
    }

    /**
     * Creates a new HttpURLConnection that refers to the current bucket for
     * specialized listing of bucket contents.
     **/
    private HttpURLConnection getBucketURLConnection(final String method, final String prefix, final String marker, final int max) throws IOException {
        if (m_bucket == null) {
            throw new IllegalArgumentException("bucket is not set");
        }
        String url = "http://" + m_host + "/" + m_bucket;
        final StringBuilder query = new StringBuilder("");
        if (prefix != null) {
            query.append("&prefix=").append(URLEncoder.encode(prefix, "UTF-8"));
        }
        if (marker != null) {
            query.append("&marker=").append(URLEncoder.encode(marker, "UTF-8"));
        }
        if (max != 0) {
            query.append("&max-keys=").append(max);
        }
        if (query.length() > 0) {
            query.setCharAt(0, '?');
            url += query;
        }
        final URL bucketURL = new URL(url);
        final HttpURLConnection bucketConn = (HttpURLConnection) bucketURL.openConnection();
        bucketConn.setRequestMethod(method);
        bucketConn.setReadTimeout(READ_TIMEOUT);
        addAuthorization(bucketConn, method, null);
        return bucketConn;
    }

    /**
     * Gets an HttpURLConnection referring to a specific item for storing
     * and retrieving of data.
     **/
    private HttpURLConnection getItemURLConnection(final String method, final String id, final byte[] data, final Map<String, List<String>> headers) throws IOException {
        if (m_bucket == null) {
            throw new IllegalArgumentException("bucket is not set");
        }
        final URL itemURL = new URL("http://" + m_host + "/" + m_bucket + "/" + id);
        final HttpURLConnection urlConn = (HttpURLConnection) itemURL.openConnection();
        urlConn.setRequestMethod(method);
        urlConn.setReadTimeout(READ_TIMEOUT);
        if (headers != null) {
            for (final Map.Entry<String, List<String>> me : headers.entrySet()) {
                for (final String v : me.getValue()) {
                    urlConn.setRequestProperty(me.getKey(), v);
                }
            }
        }
        addAuthorization(urlConn, method, data);
        return urlConn;
    }

    public String getContentType(final HttpURLConnection conn) {
        for (final Map.Entry<String, List<String>> me : conn.getRequestProperties().entrySet()) {
            if ("Content-Type".equalsIgnoreCase(me.getKey())) {
                return me.getValue().iterator().next();
            }
        }
        return "";
    }

    /**
     * Given an HttpURLConnection, this method adds the appropriate
     * authentication data to it to connect to S3.  If connection data
     * is provided, an MD5 digest is included for additional security, 
     * but the data itself is not written to the connection.
     **/
    private void addAuthorization(final HttpURLConnection conn, final String method, final byte[] data) throws IOException {
        String contentType = getContentType(conn);
        String contentMD5 = "";
        if (data != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.update(data);
                contentMD5 = Base64.encodeBytes(md.digest());
            } catch (Exception e) {
                throw new IllegalArgumentException("unable to compute content-md5", e);
            }
            conn.addRequestProperty("Content-MD5", contentMD5);
        }
        final String DateFormat = "EEE, dd MMM yyyy HH:mm:ss ";
        final SimpleDateFormat format = new SimpleDateFormat(DateFormat, Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        final String date = format.format(new Date()) + "GMT";
        conn.addRequestProperty("Date", date);
        final StringBuilder buf = new StringBuilder();
        buf.append(method).append("\n");
        buf.append(contentMD5).append("\n");
        buf.append(contentType).append("\n");
        buf.append(date).append("\n");
        final String headers = getHeaders(conn);
        if (headers.length() > 0) {
            buf.append(headers);
        }
        buf.append(conn.getURL().getPath());
        String auth;
        try {
            final SecretKeySpec signingKey = new SecretKeySpec(m_password.getBytes(), SIGNATURE_ALGORITHM);
            final Mac mac = Mac.getInstance(SIGNATURE_ALGORITHM);
            mac.init(signingKey);
            auth = Base64.encodeBytes(mac.doFinal(buf.toString().getBytes()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to calculate digest", e);
        }
        conn.setRequestProperty("Authorization", "AWS " + m_username + ":" + auth);
    }

    private String getHeaders(final HttpURLConnection conn) {
        final Map<String, List<String>> props = conn.getRequestProperties();
        final List<String> keys = new ArrayList<String>(props.keySet());
        Collections.sort(keys);
        final StringBuilder buf = new StringBuilder();
        for (final String k : keys) {
            if (k.toLowerCase().startsWith("x-amz-")) {
                final List<String> vals = props.get(k);
                if (vals.size() > 0) {
                    buf.append(k.toLowerCase().trim());
                    buf.append(":");
                    for (final String v : vals) {
                        buf.append(v.trim());
                        buf.append(",");
                    }
                    buf.deleteCharAt(buf.length() - 1);
                    buf.append("\n");
                }
            }
        }
        return buf.toString();
    }

    /**
     * This is our simple SAX parser for handling response XML from
     * S3.  In every case where we parse an XML response, we're looking
     * for the contents of a single tag which may occur multiple times,
     * such as "key" tags when listing bucket contents or "message" tags
     * when parsing error responses.  This class assumes that the target
     * tags don't contain any subtags.
     **/
    private static class ObjectListParser extends DefaultHandler {

        private final String m_lookfor;

        private final List<String> m_ids = new ArrayList<String>();

        private boolean m_storeChars = false;

        private StringBuilder m_keyName;

        /**
         * Constructs an object list parser that gather the contents of
         * tags with the given name.
         *
         * @param lookfor The element name to gather character data from
         **/
        public ObjectListParser(final String lookfor) {
            m_lookfor = lookfor;
        }

        /**
         * Once parsing is complete, this retrieves the list of contents
         * of all the matching tags encountered.
         *
         * @return The list of contents of all matching tags [not null]
         **/
        public List<String> getList() {
            return m_ids;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            if (qName.equalsIgnoreCase(m_lookfor)) {
                m_keyName = new StringBuilder();
                m_storeChars = true;
            }
        }

        @Override
        public void endElement(String url, String localName, String qName) {
            if (qName.equalsIgnoreCase(m_lookfor)) {
                m_ids.add(m_keyName.toString());
                m_storeChars = false;
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) {
            if (m_storeChars) {
                m_keyName.append(ch, start, length);
            }
        }
    }

    public static S3Store getInstance() {
        if (instance == null) {
            String host = System.getProperty("org.ualr.cpsc.amazon.s3.host");
            String access_key = System.getProperty("org.ualr.cpsc.amazon.s3.access");
            String secret_key = System.getProperty("org.ualr.cpsc.amazon.s3.secret");
            String default_bucket = System.getProperty("org.ualr.cpsc.amazon.s3.bucket.default");
            instance = new S3Store(host, access_key, secret_key, default_bucket);
        }
        return instance;
    }
}
