package com.dokumentarchiv.plugins.archivelink;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.java.plugin.Plugin;
import de.inovox.AdvancedMimeMessage;
import com.dokumentarchiv.plugins.IArchive;
import com.dokumentarchiv.plugins.PluginHelper;
import com.dokumentarchiv.plugins.archivelink.parser.ParserFactory;
import com.dokumentarchiv.search.ListEntry;
import com.dokumentarchiv.search.Search;
import com.dokumentarchiv.search.SearchExpression;
import com.dokumentarchiv.search.SearchFunction;

/**
 * Plugin for Archive Link support
 * 
 * @author Carsten Burghardt
 * @version $Id: ArchiveLinkPlugin.java 616 2008-03-12 21:55:17Z carsten $
 */
public class ArchiveLinkPlugin extends Plugin implements IArchive {

    /**
     * serial id
     */
    private static final long serialVersionUID = 8770042574988240017L;

    private static Log log = LogFactory.getLog(ArchiveLinkPlugin.class);

    private static String PVERSION = "0045";

    private static String INDEXNAME = "index";

    private static String F_DOCID = "docid";

    private String alurl;

    private String archive;

    private HttpClient client;

    private Directory dir;

    private DocumentIndexer indexer;

    /**
     * Empty
     */
    public ArchiveLinkPlugin() {
        super();
    }

    protected void doStart() throws Exception {
        Configuration config = null;
        try {
            URL configUrl = getManager().getPathResolver().resolvePath(getDescriptor(), CONFIGNAME);
            config = new PropertiesConfiguration(configUrl);
        } catch (ConfigurationException e) {
            log.error("Can not read properties", e);
            getManager().disablePlugin(getDescriptor());
            return;
        }
        alurl = config.getString("al.url");
        if (alurl.endsWith("/")) {
            alurl = alurl.substring(0, alurl.length() - 1);
        }
        if (!alurl.endsWith("?")) {
            alurl += "?";
        }
        archive = config.getString("archive");
        client = new HttpClient();
        client.setConnectionTimeout(10000);
        String request = alurl + "serverInfo&pVersion=" + PVERSION;
        HttpMethod method = new GetMethod(request);
        int statusCode = client.executeMethod(method);
        method.releaseConnection();
        if (statusCode != HttpStatus.SC_OK) {
            log.fatal("serverInfo call failed:" + method.getStatusText());
            getManager().disablePlugin(getDescriptor());
            return;
        } else {
            log.info("Server ok");
        }
        boolean create = true;
        URL indexUrl = getManager().getPathResolver().resolvePath(getDescriptor(), ".");
        File d = new File(indexUrl.getPath() + File.separator + INDEXNAME);
        log.info("Using index " + d.getAbsolutePath());
        if (d.exists()) {
            create = false;
        }
        dir = FSDirectory.getDirectory(d, create);
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter writer = new IndexWriter(dir, analyzer, create);
        writer.close();
        indexer = new DocumentIndexer(dir);
    }

    protected void doStop() throws Exception {
        indexer.doIndexing();
    }

    public List findDocuments(Search search) {
        Vector result = new Vector();
        try {
            Searcher searcher = new IndexSearcher(dir);
            Query query = buildQuery(search);
            Hits hits = searcher.search(query);
            log.debug("Hits:" + hits.length());
            for (int i = 0; i < hits.length(); ++i) {
                Document doc = hits.doc(i);
                ListEntry entry = new ListEntry();
                entry.setId(doc.getField(F_DOCID).stringValue());
                entry.setFrom(doc.getField(ISearchField.FROM).stringValue());
                entry.setTo(doc.getField(ISearchField.TO).stringValue());
                entry.setSubject(doc.getField(ISearchField.SUBJECT).stringValue());
                Date date = DateFormat.getDateInstance().parse(doc.getField(ISearchField.DATE).stringValue());
                entry.setDate(date);
                result.add(entry);
            }
            searcher.close();
        } catch (Exception e) {
            log.error("Error searching", e);
        }
        return result;
    }

    private Query buildQuery(Search search) throws ParseException, java.text.ParseException {
        StringBuffer buffer = new StringBuffer();
        Iterator it = search.getChildren().iterator();
        String operator = search.getOperator().equals(Search.OPAND) ? " AND " : " OR ";
        while (it.hasNext()) {
            SearchExpression ex = (SearchExpression) it.next();
            String field = ex.getField().trim();
            if (!field.equals(ISearchField.CONTENT)) {
                buffer.append(field + ":\"");
            }
            buffer.append(ex.getValue());
            if (!field.equals(ISearchField.CONTENT)) {
                buffer.append("\"");
            }
            if (it.hasNext()) {
                buffer.append(operator);
            }
        }
        String queryString = buffer.toString();
        log.debug("Query=" + queryString);
        Query query = QueryParser.parse(queryString, ISearchField.CONTENT, new StandardAnalyzer());
        return query;
    }

    public InputStream getDocumentByID(String id) {
        String request = alurl + "get&pVersion=" + PVERSION + "&contRep=" + archive + "&docId=" + id;
        HttpMethod method = new GetMethod(request);
        try {
            int statusCode = client.executeMethod(method);
            if (statusCode != HttpStatus.SC_OK) {
                log.error("Failed to get the document " + id + ":" + method.getStatusText());
                return null;
            }
            byte[] bytes = method.getResponseBody();
            return new ByteArrayInputStream(bytes);
        } catch (Exception e) {
            log.error("Failed to get the document " + id, e);
            return null;
        }
    }

    public HashMap getSupportedFunctions() {
        HashMap map = new HashMap();
        Vector functions = new Vector();
        functions.add(SearchFunction.getStringForFunction(SearchFunction.CONTAINS));
        map.put(ISearchField.CONTENT, functions);
        map.put(ISearchField.FROM, functions);
        map.put(ISearchField.SUBJECT, functions);
        map.put(ISearchField.TO, functions);
        functions = new Vector();
        functions.add(SearchFunction.getStringForFunction(SearchFunction.EQUALS));
        map.put(ISearchField.DATE, functions);
        return map;
    }

    public boolean archiveEMail(AdvancedMimeMessage msg) {
        String docid = null;
        byte[] bytes = null;
        try {
            bytes = msg.getBytes();
            docid = generateDocid(bytes);
        } catch (Exception e) {
            log.error("Failed to read content", e);
            return false;
        }
        String content = new String(bytes);
        String request = alurl + "create&pVersion=" + PVERSION + "&contRep=" + archive + "&docId=" + docid + "&compId=data&Content-Length=" + bytes.length;
        log.debug(request);
        PutMethod method = new PutMethod(request);
        method.setRequestBody(content);
        int statusCode;
        try {
            statusCode = client.executeMethod(method);
        } catch (Exception e) {
            log.error("Archiving request failed", e);
            method.releaseConnection();
            return false;
        }
        if (statusCode != 201) {
            log.error("Archiving request failed; status=" + statusCode + "; message=" + method.getStatusText());
            method.releaseConnection();
            return false;
        }
        log.debug("Document archived with docid " + docid);
        try {
            Document doc = new Document();
            doc.add(Field.UnIndexed(F_DOCID, docid));
            indexMessage(doc, msg);
            indexer.addDocument(doc);
        } catch (Exception e) {
            log.error("Failed to index document", e);
        }
        return true;
    }

    /**
     * Index the message
     * @param doc
     * @param msg
     * @throws IOException
     * @throws MessagingException
     */
    private void indexMessage(Document doc, MimeMessage msg) throws IOException, MessagingException {
        doc.add(Field.Text(ISearchField.SUBJECT, msg.getSubject()));
        doc.add(Field.Text(ISearchField.FROM, PluginHelper.addressToString(msg.getFrom())));
        doc.add(Field.Text(ISearchField.TO, PluginHelper.addressToString(msg.getRecipients(Message.RecipientType.TO))));
        Date sent = msg.getSentDate();
        String sentStr = DateFormat.getDateInstance().format(sent);
        doc.add(Field.Text(ISearchField.DATE, sentStr));
        indexPart(doc, msg);
    }

    /**
     * Iterate over parts and add them to the index if we can handle the type
     * @param doc
     * @param p
     * @throws MessagingException 
     * @throws IOException 
     */
    private void indexPart(Document doc, Part p) throws MessagingException, IOException {
        log.debug("contentType=" + p.getContentType());
        if (p.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) p.getContent();
            int count = mp.getCount();
            for (int i = 0; i < count; i++) {
                indexPart(doc, mp.getBodyPart(i));
            }
        } else if (p.isMimeType("message/rfc822")) {
            indexPart(doc, (Part) p.getContent());
        } else if (!isPartIndexable(doc, p)) {
            log.info("Can not handle part");
        }
    }

    /**
     * Return true if we can handle the content type
     * @param doc
     * @param p
     * @return true if the part was indexed
     * @throws MessagingException 
     * @throws IOException 
     */
    private boolean isPartIndexable(Document doc, Part p) throws MessagingException, IOException {
        if (p.isMimeType("text/plain")) {
            doc.add(Field.UnStored(ISearchField.CONTENT, (String) p.getContent()));
            return true;
        }
        return ParserFactory.parse(doc, p);
    }

    /**
     * Generate a new docid
     * @param bytes
     * @return unique docid
     * @throws NoSuchAlgorithmException
     */
    private String generateDocid(byte[] bytes) throws NoSuchAlgorithmException {
        String docid = null;
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha.digest(bytes);
        docid = PluginHelper.hexEncode(digest) + System.currentTimeMillis();
        return docid;
    }
}
