package com.dokumentarchiv.plugins.exo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.mail.service.Attachment;
import org.exoplatform.mail.service.Folder;
import org.exoplatform.mail.service.Message;
import org.exoplatform.mail.service.MimeMessageParser;
import org.exoplatform.mail.service.SpamFilter;
import org.exoplatform.mail.service.Utils;
import org.exoplatform.services.jcr.rmi.api.client.ClientRepositoryFactory;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.java.plugin.Plugin;
import com.dokumentarchiv.plugins.IArchive;
import com.dokumentarchiv.search.Search;
import de.inovox.AdvancedMimeMessage;

public class ExoPlugin extends Plugin implements IArchive {

    private static final String MAIL_SERVICE = "MailApplication";

    private ClientRepositoryFactory repositoryFactory = null;

    private Repository repository = null;

    private Session jcrSession = null;

    private String exoServer;

    private String exoUser;

    private String exoPassword;

    private String exoWorkspace;

    private String exoDocumentRoot;

    private String exoMainDomain;

    private boolean base64encode;

    /** Configuration */
    private Configuration config;

    /**
	 * 
	 */
    private static final long serialVersionUID = -4856051356936566858L;

    private static Log log = LogFactory.getLog(ExoPlugin.class);

    /**
     * Constructor
     * @param manager
     * @param descr
     */
    public ExoPlugin() {
        super();
    }

    /**
     * Constructor for JUnit
     * @param init
     * @throws Exception
     */
    public ExoPlugin(boolean init) throws Exception {
        if (init) {
            doStart();
        }
        exoServer = "//dms.thunderbox.local:9999/repository";
        exoUser = "root";
        exoPassword = "exo";
        exoWorkspace = "collaboration";
        exoDocumentRoot = "MailArchives";
        repositoryFactory = new ClientRepositoryFactory();
        repository = repositoryFactory.getRepository(exoServer);
        exoMainDomain = "cburghardt.com";
        rmiConnect();
    }

    @Override
    protected void doStart() throws Exception {
        try {
            URL configUrl = getManager().getPathResolver().resolvePath(getDescriptor(), CONFIGNAME);
            this.config = new PropertiesConfiguration(configUrl);
        } catch (ConfigurationException e) {
            log.error("Can not read properties", e);
            getManager().disablePlugin(getDescriptor());
            return;
        }
        base64encode = config.getBoolean("base64.encoding.enabled", true);
        rmiInitiliase();
        log.info("Connected to:" + exoServer);
    }

    @Override
    protected void doStop() throws Exception {
        rmiDisconnect();
        log.info(getDescriptor() + " Stop.");
    }

    @Override
    public boolean archiveEMail(AdvancedMimeMessage msg) {
        try {
            String sender = msg.getHeader("From", null);
            String recipient = msg.getHeader("To", null);
            String subject = msg.getHeader("Subject", null);
            log.info("Subject of this mail is " + subject);
            String fileName = getUniqueFilename(msg);
            if (sender == null) return false;
            if (recipient == null) return false;
            InternetAddress ia = null;
            String[] folderIds = { "Inbox", "Sent", "Internal" };
            if ((sender.contains(exoMainDomain)) && recipient.contains(exoMainDomain)) {
                ia = new InternetAddress(recipient, false);
                log.info("Message " + subject + " stored as Internal mail for " + ia.getAddress());
                saveMessage(jcrSession, ia.getAddress(), "Internal", msg, folderIds, null, null);
            } else if (sender.contains(exoMainDomain)) {
                ia = new InternetAddress(sender, false);
                log.info("Message " + subject + " stored as Sent mail for " + ia.getAddress());
                saveMessage(jcrSession, ia.getAddress(), "Sent", msg, folderIds, null, null);
            } else {
                ia = new InternetAddress(recipient, false);
                log.info("Message " + subject + " stored as Inbox  for " + ia.getAddress());
                saveMessage(jcrSession, ia.getAddress(), "Inbox", msg, folderIds, null, null);
            }
            log.info("Filename= " + fileName);
        } catch (Exception e) {
            log.error("Failed to archive message");
            return false;
        }
        log.info(getDescriptor() + " Archive Email.");
        return true;
    }

    @Override
    public List findDocuments(Search search) {
        return null;
    }

    @Override
    public InputStream getDocumentByID(String id) {
        return null;
    }

    @Override
    public HashMap getSupportedFunctions() {
        return null;
    }

    /**
	 * 
	 * @throws ClassCastException
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws RepositoryException 
	 * @throws NoSuchWorkspaceException 
	 * @throws LoginException 
	 */
    private void rmiInitiliase() throws ClassCastException, MalformedURLException, RemoteException, NotBoundException, LoginException, NoSuchWorkspaceException, RepositoryException {
        exoServer = config.getString("baseurl");
        exoUser = config.getString("username");
        exoPassword = config.getString("password");
        exoWorkspace = config.getString("workspace");
        exoDocumentRoot = config.getString("documentroot");
        repositoryFactory = new ClientRepositoryFactory();
        repository = repositoryFactory.getRepository(exoServer);
        exoMainDomain = config.getString("homedomain");
        rmiConnect();
    }

    /**
	 * 
	 * @throws LoginExceptiondate
	 * @throws NoSuchWorkspaceException
	 * @throws RepositoryException
	 */
    public void rmiConnect() throws LoginException, NoSuchWorkspaceException, RepositoryException {
        Credentials credentials = new SimpleCredentials(exoUser, exoPassword.toCharArray());
        jcrSession = repository.login(credentials, exoWorkspace);
    }

    /**
	 * 
	 */
    public void rmiDisconnect() {
        jcrSession.logout();
    }

    /**
     * Generate a unique filename
     * @param msg
     * @return
     * @throws IOException
     * @throws MessagingException
     * @throws NoSuchAlgorithmException
     */
    private String getUniqueFilename(MimeMessage msg) throws IOException, MessagingException, NoSuchAlgorithmException {
        byte[] bytes = msg.getContent().toString().getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha.digest(bytes);
        return System.currentTimeMillis() + hexEncode(digest);
    }

    private String getUniqueID(String account) throws Exception {
        byte[] bytes = account.getBytes();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] digest = sha.digest(bytes);
        return System.currentTimeMillis() + hexEncode(digest);
    }

    /**
     * hex encode the string to make a nice string representation
     * @param aInput
     * @return hex encoded string
     */
    private String hexEncode(byte[] aInput) {
        StringBuffer result = new StringBuffer();
        char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        for (int idx = 0; idx < aInput.length; ++idx) {
            byte b = aInput[idx];
            result.append(digits[(b & 0xf0) >> 4]);
            result.append(digits[b & 0x0f]);
        }
        return result.toString();
    }

    public boolean saveMessage(Session sProvider, String username, String accId, javax.mail.Message msg, String folderIds[], List<String> tagList, SpamFilter spamFilter) throws Exception {
        long t1, t2, t3, t4;
        String from;
        String msgId = MimeMessageParser.getMessageId(msg);
        log.warn("MessageId = " + msgId);
        Calendar gc = MimeMessageParser.getReceivedDate(msg);
        Node msgHomeNode = getDateStoreNode(sProvider, username, accId, gc.getTime());
        if (msgHomeNode == null) return false;
        try {
            Node msgNode = msgHomeNode.getNode(msgId);
            log.warn("Check duplicate ......................................");
            for (int i = 0; i < folderIds.length; i++) {
                String folderId = folderIds[i];
                byte checkDuplicate = checkDuplicateStatus(sProvider, username, msgHomeNode, accId, msgNode, folderId);
                if (checkDuplicate == Utils.MAIL_DUPLICATE_IN_OTHER_FOLDER) {
                    return true;
                }
                if (checkDuplicate == Utils.MAIL_DUPLICATE_IN_SAME_FOLDER) {
                    return false;
                }
            }
        } catch (Exception e) {
        }
        log.warn("Saving message to JCR ...");
        t1 = System.currentTimeMillis();
        Node node = null;
        try {
            node = msgHomeNode.addNode(msgId, Utils.NT_UNSTRUCTURED);
        } catch (Exception e) {
            msgId = "Message" + getUniqueID(accId);
            log.warn("The MessageId is NOT GOOD, generated another one = " + msgId);
            node = msgHomeNode.addNode(msgId, Utils.NT_UNSTRUCTURED);
        }
        try {
            msgHomeNode.save();
            node.setProperty(Utils.EXO_ID, msgId);
            node.setProperty(Utils.EXO_IN_REPLY_TO_HEADER, MimeMessageParser.getInReplyToHeader(msg));
            node.setProperty(Utils.EXO_ACCOUNT, accId);
            from = Utils.decodeText(InternetAddress.toString(msg.getFrom()));
            node.setProperty(Utils.EXO_FROM, from);
            node.setProperty(Utils.EXO_TO, getAddresses(msg, javax.mail.Message.RecipientType.TO));
            node.setProperty(Utils.EXO_CC, getAddresses(msg, javax.mail.Message.RecipientType.CC));
            node.setProperty(Utils.EXO_BCC, getAddresses(msg, javax.mail.Message.RecipientType.BCC));
            node.setProperty(Utils.EXO_REPLYTO, Utils.decodeText(InternetAddress.toString(msg.getReplyTo())));
            String subject = msg.getSubject();
            if (subject != null) subject = Utils.decodeText(msg.getSubject()); else subject = "";
            node.setProperty(Utils.EXO_SUBJECT, subject);
            node.setProperty(Utils.EXO_RECEIVEDDATE, gc);
            Calendar sc = GregorianCalendar.getInstance();
            if (msg.getSentDate() != null) sc.setTime(msg.getSentDate()); else sc = gc;
            node.setProperty(Utils.EXO_SENDDATE, sc);
            node.setProperty(Utils.EXO_SIZE, Math.abs(msg.getSize()));
            node.setProperty(Utils.EXO_ISUNREAD, true);
            node.setProperty(Utils.EXO_STAR, false);
            long priority = MimeMessageParser.getPriority(msg);
            node.setProperty(Utils.EXO_PRIORITY, priority);
            if (msg.getHeader("Disposition-Notification-To") != null) node.setProperty(Utils.IS_RETURN_RECEIPT, true); else node.setProperty(Utils.IS_RETURN_RECEIPT, false);
            if (spamFilter != null && spamFilter.checkSpam(msg)) {
                folderIds = new String[] { Utils.createFolderId(accId, Utils.FD_SPAM, false) };
            }
            node.setProperty(Utils.MSG_FOLDERS, folderIds);
            if (tagList != null && tagList.size() > 0) node.setProperty(Utils.EXO_TAGS, tagList.toArray(new String[] {}));
            ArrayList<String> values = new ArrayList<String>();
            Enumeration enu = msg.getAllHeaders();
            while (enu.hasMoreElements()) {
                Header header = (Header) enu.nextElement();
                values.add(header.getName() + "=" + header.getValue());
            }
            node.setProperty(Utils.MSG_HEADERS, values.toArray(new String[] {}));
            log.warn("Saved body and attachment of message .... size : " + Math.abs(msg.getSize()) + " B");
            t2 = System.currentTimeMillis();
            MimeMessage cmsg = (MimeMessage) msg;
            Object obj = new Object();
            try {
                obj = msg.getContent();
            } catch (MessagingException mex) {
                cmsg = new MimeMessage((MimeMessage) msg);
                try {
                    obj = cmsg.getContent();
                } catch (MessagingException mex1) {
                    System.out.println("##### Error when fetch message body");
                }
            }
            String contentType = "text/plain";
            if (cmsg.isMimeType("text/html") || cmsg.isMimeType("multipart/alternative") || cmsg.isMimeType("multipart/related")) contentType = "text/html";
            String body = "";
            if (obj instanceof Multipart) {
                body = setMultiPart((Multipart) obj, node, body);
            } else {
                body = setPart(cmsg, node, body);
            }
            node.setProperty(Utils.EXO_CONTENT_TYPE, contentType);
            node.setProperty(Utils.EXO_BODY, Utils.decodeText(body));
            t3 = System.currentTimeMillis();
            log.warn("Saved body (and attachments) of message finished : " + (t3 - t2) + " ms");
            node.save();
            createHTMLMessage(node, contentType);
            t4 = System.currentTimeMillis();
            log.warn("Saved total message to JCR finished : " + (t4 - t1) + " ms");
            log.warn("Adding message to thread ...");
            t1 = System.currentTimeMillis();
            t2 = System.currentTimeMillis();
            log.warn("Added message to thread finished : " + (t2 - t1) + " ms");
            log.warn("Updating number message to folder ...");
            t1 = System.currentTimeMillis();
            for (int i = 0; i < folderIds.length; i++) {
                increaseFolderItem(sProvider, username, accId, folderIds[i]);
            }
            t2 = System.currentTimeMillis();
            log.warn("Updated number message to folder finished : " + (t2 - t1) + " ms");
            return true;
        } catch (Exception e) {
            try {
                msgHomeNode.refresh(true);
            } catch (Exception ex) {
                e.printStackTrace();
                log.warn(" [WARNING] Can't refresh.");
            }
            log.warn(" [WARNING] Cancel saving message to JCR.");
            return false;
        }
    }

    private void createHTMLMessage(Node messageNode, String contentType) throws Exception {
        StringBuffer buffer = new StringBuffer();
        try {
            buffer.append("Id=" + messageNode.getProperty(Utils.EXO_ID).getString() + "\n");
        } catch (Exception e) {
        }
        buffer.append("Path=" + messageNode.getPath() + "\n");
        try {
            buffer.append("InReplyToHeader=" + messageNode.getProperty(Utils.EXO_IN_REPLY_TO_HEADER).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("Message Type=" + messageNode.getProperty(Utils.EXO_ACCOUNT).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("From=" + messageNode.getProperty(Utils.EXO_FROM).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("MessageTo=" + messageNode.getProperty(Utils.EXO_TO).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("Subject=" + messageNode.getProperty(Utils.EXO_SUBJECT).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("MessageCc=" + messageNode.getProperty(Utils.EXO_CC).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("MessageBcc=" + messageNode.getProperty(Utils.EXO_BCC).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("ReplyTo=" + messageNode.getProperty(Utils.EXO_REPLYTO).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("ContentType=" + messageNode.getProperty(Utils.EXO_CONTENT_TYPE).getString() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("MessageBody :\n");
            buffer.append(messageNode.getProperty(Utils.EXO_BODY).getString() + "\n\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("Size=" + FileUtils.byteCountToDisplaySize(messageNode.getProperty(Utils.EXO_SIZE).getLong()) + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("HasAttachment=" + (messageNode.getProperty(Utils.EXO_HASATTACH).getBoolean() ? "Yes" : "No") + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("HasStar=" + (messageNode.getProperty(Utils.EXO_STAR).getBoolean() ? "Yes" : "No") + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("Priority=" + messageNode.getProperty(Utils.EXO_PRIORITY).getLong() + "\n");
        } catch (Exception e) {
        }
        try {
            buffer.append("Unread=" + (messageNode.getProperty(Utils.EXO_ISUNREAD).getBoolean() ? "Yes" : "No") + "\n");
        } catch (Exception e) {
        }
        GregorianCalendar cal = new GregorianCalendar();
        try {
            cal.setTimeInMillis(messageNode.getProperty(Utils.EXO_RECEIVEDDATE).getLong());
            buffer.append("ReceivedDate=" + cal.getTime().toString() + "\n");
        } catch (Exception e) {
        }
        try {
            cal.setTimeInMillis(messageNode.getProperty(Utils.EXO_SENDDATE).getLong());
            buffer.append("SendDate=" + cal.getTime().toString() + "\n");
        } catch (Exception e) {
        }
        Node data = null;
        String messageBody = null;
        data = messageNode.addNode("Message.html", "nt:file").addNode("jcr:content", "nt:resource");
        messageBody = Utils.text2html(buffer.toString());
        data.setProperty("jcr:mimeType", Utils.MIMETYPE_TEXTHTML);
        data.setProperty("jcr:encoding", "UTF-8");
        data.setProperty("jcr:data", new ByteArrayInputStream(messageBody.getBytes()));
        data.setProperty("jcr:lastModified", Calendar.getInstance());
        messageNode.save();
    }

    private String getAddresses(javax.mail.Message msg, javax.mail.Message.RecipientType type) throws Exception {
        String recipients = "";
        String t = "To";
        if (type.equals(javax.mail.Message.RecipientType.CC)) t = "Cc"; else if (type.equals(javax.mail.Message.RecipientType.BCC)) t = "Bcc";
        try {
            recipients = InternetAddress.toString(msg.getRecipients(type));
        } catch (Exception e) {
            String[] ccs = msg.getHeader(t);
            for (int i = 0; i < ccs.length; i++) recipients += ccs[i] + ",";
        }
        return Utils.decodeText(recipients);
    }

    private void increaseFolderItem(Session sProvider, String username, String accId, String folderId) throws Exception {
        try {
            Node node = getFolderNodeById(sProvider, username, accId, folderId);
            if (node != null) {
                node.setProperty(Utils.EXO_UNREADMESSAGES, node.getProperty(Utils.EXO_UNREADMESSAGES).getLong() + 1);
                node.setProperty(Utils.EXO_TOTALMESSAGE, node.getProperty(Utils.EXO_TOTALMESSAGE).getLong() + 1);
                node.save();
            }
        } catch (PathNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String setMultiPart(Multipart multipart, Node node, String body) {
        try {
            boolean readText = true;
            if (multipart.getContentType().toLowerCase().indexOf("multipart/alternative") > -1) {
                Part bodyPart;
                for (int i = 0; i < multipart.getCount(); i++) {
                    bodyPart = multipart.getBodyPart(i);
                    if (bodyPart.isMimeType("text/html") || bodyPart.isMimeType("multipart/related")) {
                        body = setPart(bodyPart, node, body);
                        readText = false;
                    }
                }
            }
            if (readText) {
                for (int i = 0, n = multipart.getCount(); i < n; i++) {
                    body = setPart(multipart.getBodyPart(i), node, body);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }

    private String setPart(Part part, Node node, String body) {
        try {
            String disposition = part.getDisposition();
            String ct = part.getContentType();
            if (disposition == null) {
                if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    body = appendMessageBody(part, node, body);
                } else if (part.isMimeType("multipart/alternative")) {
                    Part bodyPart;
                    boolean readText = true;
                    MimeMultipart mimeMultiPart = (MimeMultipart) part.getContent();
                    for (int i = 0; i < mimeMultiPart.getCount(); i++) {
                        bodyPart = mimeMultiPart.getBodyPart(i);
                        if (bodyPart.isMimeType("text/html")) {
                            body = setPart(bodyPart, node, body);
                            readText = false;
                        }
                    }
                    if (readText) {
                        for (int i = 0; i < mimeMultiPart.getCount(); i++) {
                            body = setPart(mimeMultiPart.getBodyPart(i), node, body);
                        }
                    }
                } else if (part.isMimeType("multipart/*")) {
                    MimeMultipart mimeMultiPart = (MimeMultipart) part.getContent();
                    for (int i = 0; i < mimeMultiPart.getCount(); i++) {
                        body = setPart(mimeMultiPart.getBodyPart(i), node, body);
                    }
                } else if (part.isMimeType("message/rfc822")) {
                    body = getNestedMessageBody(part, node, body);
                }
            } else if (disposition.equalsIgnoreCase(Part.INLINE)) {
                if (part.isMimeType("text/plain") || part.isMimeType("text/html")) {
                    body = appendMessageBody(part, node, body);
                } else if (part.isMimeType("message/rfc822")) {
                    body = getNestedMessageBody(part, node, body);
                }
            } else if (disposition.equalsIgnoreCase(Part.ATTACHMENT)) {
                Node attHome = null;
                try {
                    attHome = node.getNode(Utils.KEY_ATTACHMENT);
                } catch (PathNotFoundException e) {
                    attHome = node.addNode(Utils.KEY_ATTACHMENT, Utils.NT_UNSTRUCTURED);
                }
                String fileName = null;
                try {
                    fileName = Utils.decodeText(part.getFileName());
                } catch (Exception e) {
                    fileName = "corrupted";
                }
                Node nodeFile = attHome.addNode(fileName, Utils.NT_FILE);
                Node nodeContent = nodeFile.addNode(Utils.JCR_CONTENT, Utils.NT_RESOURCE);
                MimeTypeResolver mimeTypeSolver = new MimeTypeResolver();
                String mimeType = mimeTypeSolver.getMimeType(fileName);
                nodeContent.setProperty(Utils.JCR_MIMETYPE, mimeType);
                try {
                    nodeContent.setProperty(Utils.JCR_DATA, part.getInputStream());
                } catch (Exception e) {
                    nodeContent.setProperty(Utils.JCR_DATA, new ByteArrayInputStream("".getBytes()));
                    node.setProperty(Utils.ATT_IS_LOADED_PROPERLY, false);
                }
                nodeContent.setProperty(Utils.JCR_LASTMODIFIED, Calendar.getInstance().getTimeInMillis());
                node.setProperty(Utils.EXO_HASATTACH, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return body;
    }

    private String getNestedMessageBody(Part part, Node node, String body) throws Exception {
        try {
            body = setPart((Part) part.getContent(), node, body);
        } catch (ClassCastException e) {
            Object obj = part.getContent();
            if (obj instanceof String) {
                body += (String) obj;
            } else if (obj instanceof InputStream) {
                StringBuffer sb = new StringBuffer();
                InputStream is = (InputStream) obj;
                int c;
                while ((c = is.read()) != -1) sb.append(c);
                body += sb.toString();
            } else if (obj instanceof Multipart) {
                body = setMultiPart((Multipart) obj, node, body);
            } else {
                log.warn("This is a unknown type.");
            }
        }
        return body;
    }

    private String appendMessageBody(Part part, Node node, String body) throws Exception {
        StringBuffer messageBody = new StringBuffer();
        InputStream is = part.getInputStream();
        String ct = part.getContentType();
        String charset = "UTF-8";
        if (ct != null) {
            String cs = new ContentType(ct).getParameter("charset");
            boolean convertCharset = true;
            for (int i = 0; i < Utils.NOT_SUPPORTED_CHARSETS.length; i++) {
                if (cs.equalsIgnoreCase(Utils.NOT_SUPPORTED_CHARSETS[i])) {
                    convertCharset = false;
                }
            }
            if (cs != null && convertCharset) {
                charset = cs;
            }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, charset));
        String inputLine;
        String breakLine = "";
        if (part.isMimeType("text/plain")) breakLine = "\n";
        while ((inputLine = reader.readLine()) != null) {
            messageBody.append(inputLine + breakLine);
        }
        if (part.isMimeType("text/plain")) {
            if (body != null && !body.equals("")) {
                body = body + "\n" + Utils.encodeHTML(messageBody.toString());
            } else {
                body = Utils.encodeHTML(messageBody.toString());
            }
        } else if (part.isMimeType("text/html")) {
            if (body != null && !body.equals("")) {
                body = body + "<br>" + messageBody.toString();
            } else {
                body = messageBody.toString();
            }
        }
        return body;
    }

    public Folder getFolder(Session sProvider, String username, String accountId, String folderId) throws Exception {
        Folder folder = null;
        Node node = getFolderNodeById(sProvider, username, accountId, folderId);
        if (node != null) {
            folder = new Folder();
            folder.setId(node.getProperty(Utils.EXO_ID).getString());
            folder.setPath(node.getPath());
            folder.setURLName(node.getProperty(Utils.EXO_LABEL).getString());
            folder.setName(node.getProperty(Utils.EXO_NAME).getString());
            folder.setType(node.getProperty(Utils.EXO_FOLDERTYPE).getLong());
            folder.setPersonalFolder(node.getProperty(Utils.EXO_PERSONAL).getBoolean());
            folder.setNumberOfUnreadMessage(node.getProperty(Utils.EXO_UNREADMESSAGES).getLong());
            folder.setTotalMessage(node.getProperty(Utils.EXO_TOTALMESSAGE).getLong());
            try {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(node.getProperty(Utils.EXO_LAST_CHECKED_TIME).getLong());
                folder.setLastCheckedDate(cal.getTime());
            } catch (Exception e) {
                folder.setLastCheckedDate(null);
            }
            try {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(node.getProperty(Utils.EXO_LAST_START_CHECKING_TIME).getLong());
                folder.setLastStartCheckingTime(cal.getTime());
            } catch (Exception e) {
                folder.setLastStartCheckingTime(null);
            }
            try {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTimeInMillis(node.getProperty(Utils.EXO_CHECK_FROM_DATE).getLong());
                folder.setCheckFromDate(cal.getTime());
            } catch (Exception e) {
            }
        }
        return folder;
    }

    public String getFolderParentId(Session sProvider, String username, String accountId, String folderId) throws Exception {
        Node parentNode = getFolderNodeById(sProvider, username, accountId, folderId).getParent();
        try {
            if (parentNode != null) return parentNode.getProperty(Utils.EXO_ID).getString(); else return null;
        } catch (PathNotFoundException e) {
            return null;
        }
    }

    private Node getFolderNodeById(Session sProvider, String username, String accountId, String folderId) throws Exception {
        Node accountNode = getMailHomeNode(sProvider, username).getNode(accountId);
        Session sess = accountNode.getSession();
        QueryManager qm = sess.getWorkspace().getQueryManager();
        StringBuffer queryString = new StringBuffer("/jcr:root" + accountNode.getPath() + "//element(*,exo:folder)[@exo:id='").append(folderId).append("']");
        Query query = qm.createQuery(queryString.toString(), Query.XPATH);
        QueryResult result = query.execute();
        NodeIterator it = result.getNodes();
        Node node = null;
        if (it.hasNext()) node = it.nextNode();
        return node;
    }

    public Folder getFolder(Node node) throws Exception {
        Folder folder = new Folder();
        folder.setId(node.getProperty(Utils.EXO_ID).getString());
        folder.setURLName(node.getProperty(Utils.EXO_LABEL).getString());
        folder.setPath(node.getPath());
        folder.setName(node.getProperty(Utils.EXO_NAME).getString());
        folder.setPersonalFolder(node.getProperty(Utils.EXO_PERSONAL).getBoolean());
        folder.setType(node.getProperty(Utils.EXO_FOLDERTYPE).getLong());
        folder.setNumberOfUnreadMessage(node.getProperty(Utils.EXO_UNREADMESSAGES).getLong());
        folder.setTotalMessage(node.getProperty(Utils.EXO_TOTALMESSAGE).getLong());
        try {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(node.getProperty(Utils.EXO_LAST_CHECKED_TIME).getLong());
            folder.setLastCheckedDate(cal.getTime());
        } catch (Exception e) {
            folder.setLastCheckedDate(null);
        }
        try {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(node.getProperty(Utils.EXO_LAST_START_CHECKING_TIME).getLong());
            folder.setLastStartCheckingTime(cal.getTime());
        } catch (Exception e) {
            folder.setLastStartCheckingTime(null);
        }
        try {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(node.getProperty(Utils.EXO_CHECK_FROM_DATE).getLong());
            folder.setCheckFromDate(cal.getTime());
        } catch (Exception e) {
        }
        return folder;
    }

    public Node getDateStoreNode(Session sProvider, String username, String accountId, Date date) throws Exception {
        Node msgHome = getMessageHome(sProvider, username, accountId);
        java.util.Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        Node yearNode;
        Node monthNode;
        String year = "Y" + String.valueOf(calendar.get(java.util.Calendar.YEAR));
        String month = "M" + String.valueOf(calendar.get(java.util.Calendar.MONTH) + 1);
        String day = "D" + String.valueOf(calendar.get(java.util.Calendar.DATE));
        try {
            yearNode = msgHome.getNode(year);
        } catch (Exception e) {
            yearNode = msgHome.addNode(year, Utils.NT_UNSTRUCTURED);
            msgHome.save();
        }
        try {
            monthNode = yearNode.getNode(month);
        } catch (Exception e) {
            monthNode = yearNode.addNode(month, Utils.NT_UNSTRUCTURED);
            yearNode.save();
        }
        try {
            return monthNode.getNode(day);
        } catch (Exception e) {
            Node dayNode = monthNode.addNode(day, Utils.NT_UNSTRUCTURED);
            monthNode.save();
            return dayNode;
        }
    }

    public Node getMessageHome(Session sProvider, String username, String accountId) throws Exception {
        Node accountHome = getMailHomeNode(sProvider, username);
        Node typeHome = null;
        try {
            typeHome = accountHome.getNode(accountId);
        } catch (PathNotFoundException e) {
            typeHome = accountHome.addNode(accountId, Utils.NT_UNSTRUCTURED);
            accountHome.save();
        }
        return typeHome;
    }

    private Node getMailHomeNode(Session sProvider, String username) throws Exception {
        Node root = null;
        Node repoNode = jcrSession.getRootNode();
        try {
            root = repoNode.getNode(exoDocumentRoot);
        } catch (PathNotFoundException e) {
            root = repoNode.addNode(exoDocumentRoot);
            repoNode.save();
        }
        Node userNoode = null;
        try {
            userNoode = root.getNode(username);
        } catch (PathNotFoundException e) {
            userNoode = root.addNode(username, Utils.NT_UNSTRUCTURED);
            if (root.isNew()) root.getSession().save(); else root.save();
        }
        return userNoode;
    }

    /**
    	   * 
    	   * @param sProvider
    	   * @param username
    	   * @param msgHomeNode
    	   * @param accId
    	   * @param msg
    	   * @param msgId
    	   * @param folderId
    	   * @return
    	   */
    private byte checkDuplicateStatus(Session sProvider, String username, Node msgHomeNode, String accId, Node msgNode, String folderId) {
        byte ret = Utils.NO_MAIL_DUPLICATE;
        try {
            Value[] propFolders = msgNode.getProperty(Utils.MSG_FOLDERS).getValues();
            for (int i = 0; i < propFolders.length; i++) {
                if (propFolders[i].getString().indexOf(folderId) > -1) {
                    log.warn("DUPLICATE MAIL ... removed");
                    return Utils.MAIL_DUPLICATE_IN_SAME_FOLDER;
                }
            }
            String[] folders = new String[propFolders.length + 1];
            folders[0] = folderId;
            for (int i = 0; i < propFolders.length; i++) {
                folders[i + 1] = propFolders[i].getString();
            }
            msgNode.setProperty(Utils.EXO_ISUNREAD, true);
            msgNode.setProperty(Utils.EXO_STAR, false);
            msgNode.setProperty(Utils.MSG_FOLDERS, folders);
            msgHomeNode.save();
            increaseFolderItem(sProvider, username, accId, folderId);
            log.warn("DUPLICATE MAIL IN ANOTHER FOLDER ... ");
            ret = Utils.MAIL_DUPLICATE_IN_OTHER_FOLDER;
        } catch (Exception e) {
        }
        return ret;
    }
}
