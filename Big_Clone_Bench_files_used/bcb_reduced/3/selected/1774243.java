package org.imivic.core;

import java.io.*;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.*;
import javax.naming.*;
import javax.servlet.http.*;
import javax.servlet.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import org.imivic.sql.Queries;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import org.imivic.util.PageValues;

/**
 * Class is designed to controll flows in web application based on imivic. Class is reponsible for set and run tasks related with
 * action, generate XML from data returned by tasks and generate output based and XML and XSL related with action.
 * @author  zbigniewk
 */
public class ControlBean implements Serializable {

    public static String MESSAGES = "messages";

    public static int FORMTOKENSCOUNT = 100;

    public static String DEFAULTMESSAGESFILE = "messages_default.xml";

    private String actionsFileName = "actions.xml";

    private String messagesFileNames = "messages_default.xml";

    private Actions actions = null;

    private Queries queries = null;

    private Map<Locale, Messages> messages = new HashMap<Locale, Messages>();

    private long actionsModifyTime = 0;

    private long messagesModifyTime = 0;

    private final Map<String, String> pages = new LinkedHashMap<String, String>();

    private int nCachedPages = 0;

    private int nCachedTransformers = 0;

    private String basePath = "";

    private final Map<String, Transformer> transformers = new LinkedHashMap<String, Transformer>();

    private Map<String, String> links = new HashMap<String, String>();

    private Map<String, String> reverseLinks = new HashMap<String, String>();

    private Map<String, String> formTokens = new HashMap<String, String>();

    static int il = 0;

    private DataSource dataSource = null;

    boolean inited = false;

    /**
     *  Creates a new instance of ControllBean 
     * @param ctx
     * @throws NamingException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
     */
    public ControlBean(ServletContext ctx) throws NamingException, IOException, SAXException, ParserConfigurationException {
        String temp = null;
        temp = ctx.getInitParameter("actionsFileName");
        if (temp == null) {
            setActionsFileName(ctx.getRealPath("/WEB-INF") + "/actions.xml");
        } else {
            setActionsFileName(temp);
        }
        temp = ctx.getInitParameter("messagesFileNames");
        if (temp != null) {
            setMessagesFileName(temp);
        }
        if (ctx.getInitParameter("numberCachedPages") != null) {
            nCachedPages = Integer.parseInt(ctx.getInitParameter("numberCachedPages"));
        }
        if (ctx.getInitParameter("numberCachedTransformers") != null) {
            nCachedTransformers = Integer.parseInt(ctx.getInitParameter("numberCachedTransformers"));
        }
        basePath = ctx.getInitParameter("basePath");
        if (basePath == null) {
            basePath = ctx.getRealPath("/");
        }
        init();
    }

    /**
     * Method is invoked in <code>GetPage</code> servlet and is reponsible for init imivic.
     * @throws NamingException
     * @throws IOException
     * @throws SAXException
     * @throws ParserConfigurationException 
     */
    synchronized void init() throws NamingException, IOException, SAXException, ParserConfigurationException {
        if (!inited) {
            String[] mf = messagesFileNames.split(";");
            Locale loc;
            for (String s : mf) {
                if (DEFAULTMESSAGESFILE.equals(s)) {
                    loc = Locale.getDefault();
                } else {
                    String l = s.substring(MESSAGES.length() + 1, MESSAGES.length() + 3);
                    loc = new Locale(l.toLowerCase(), l.toUpperCase());
                }
                File messagesFile = new File(basePath + "WEB-INF/" + s);
                if (messagesModifyTime == 0 || new File(s).lastModified() != messagesModifyTime) {
                    messagesModifyTime = messagesFile.lastModified();
                    messages.put(loc, new Messages(messagesFile));
                }
            }
        }
        File actionsFile = new File(actionsFileName);
        if (new File(actionsFileName).lastModified() != actionsModifyTime) {
            actionsModifyTime = actionsFile.lastModified();
            actions = new Actions(actionsFile);
        }
        inited = true;
    }

    XML getXMLInstance(String action) throws ClassNotFoundException, InstantiationException, IllegalAccessException, ActionNotFoundException {
        return actions.getXMLInstance(action);
    }

    String getXslFile(String action) throws ActionNotFoundException {
        return actions.getXslFile(action);
    }

    String getErrorXslFile(String action) throws ActionNotFoundException {
        return actions.getErrorXslFile(action);
    }

    Result perform(String action, JoinRequest jRequest, HttpServletResponse response) throws ClassNotFoundException, NamingException, IOException, SAXException, ParserConfigurationException, ProcessDataException, ParamNotFoundException, ActionNotFoundException, SQLException, Exception {
        Result result = null;
        init();
        Task anyAction = null;
        int k = actions.getNumberOfClasses(action);
        Map access = null;
        PageValues p = ((PageValues) jRequest.getRequest().getSession().getAttribute("access"));
        if (p != null) {
            access = p.getValues();
        }
        Locale l = (Locale) jRequest.getRequest().getSession().getAttribute("User.locale");
        if (l == null) {
            l = Locale.getDefault();
        }
        if (messages.get(l) == null) {
            throw new ParamNotFoundException("Messages for locale " + l + " not found. Message file for that locale should be named messages_" + l.getCountry().toLowerCase() + ".xml");
        }
        for (int i = 0; i < k; i++) {
            anyAction = actions.getActionInstance(action, i, access);
            if (result == null) {
                result = anyAction.perform(new JoinRequest(jRequest, action, i), response, messages.get(l));
            } else {
                result.merge(anyAction.perform(new JoinRequest(jRequest, action, i), response, messages.get(l)));
            }
            if (result != null && result.getState().equals("error")) {
                break;
            }
        }
        l = (Locale) jRequest.getRequest().getSession().getAttribute("User.locale");
        if (l == null) {
            l = Locale.getDefault();
        }
        if (result != null) {
            result.setLocaleMessage(messages.get(l));
            result.setProtocol(actions.getProtocol(action));
        }
        return result;
    }

    String getOutputPage(String action, String XML, String xslFileName, InputStream pageS, HttpServletRequest request) throws NoSuchAlgorithmException, UnsupportedEncodingException, TransformerException {
        String sPage = null;
        Transformer transformer = null;
        String dig = null;
        CharArrayWriter page = new CharArrayWriter();
        if (this.nCachedPages > 0) {
            java.security.MessageDigest mess = java.security.MessageDigest.getInstance("SHA1");
            mess.update(XML.getBytes());
            mess.update(Long.toString(new File(basePath + xslFileName).lastModified()).getBytes());
            dig = new String(mess.digest());
            synchronized (pages) {
                if (pages.containsKey(dig)) {
                    sPage = pages.get(dig);
                }
            }
        }
        if (sPage == null && xslFileName.length() > 4) {
            try {
                long modifyTime = new File(basePath + xslFileName).lastModified();
                String path = basePath.replaceAll("\\\\", "/") + xslFileName;
                path = "file:///" + path;
                boolean add2cache = false;
                if (this.nCachedTransformers > 0) {
                    String cacheKey = action + xslFileName + modifyTime;
                    if (this.transformers.containsKey(cacheKey)) {
                        transformer = this.transformers.get(cacheKey);
                        synchronized (transformer) {
                            transformer.transform(new StreamSource(new ByteArrayInputStream(XML.getBytes("UTF-8"))), new StreamResult(page));
                        }
                    } else {
                        add2cache = true;
                    }
                }
                if (transformer == null) {
                    transformer = TransformerFactory.newInstance().newTransformer(new StreamSource(path));
                    transformer.transform(new StreamSource(new ByteArrayInputStream(XML.getBytes("UTF-8"))), new StreamResult(page));
                }
                sPage = page.toString();
                sPage = sPage.replaceAll("&lt;", "<");
                sPage = sPage.replaceAll("&gt;", ">");
                sPage = replaceLinks(sPage, request);
                if (this.nCachedPages > 0) {
                    synchronized (pages) {
                        pages.put(dig, sPage);
                        if (pages.size() > nCachedPages) {
                            Iterator<String> i = pages.values().iterator();
                            i.next();
                            i.remove();
                        }
                    }
                }
                if (add2cache) {
                    synchronized (this.transformers) {
                        this.transformers.put(action + xslFileName + modifyTime, transformer);
                        if (this.transformers.size() > this.nCachedTransformers) {
                            Iterator<Transformer> it = this.transformers.values().iterator();
                            it.next();
                            it.remove();
                        }
                    }
                }
            } catch (TransformerException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "---------------------------------------------");
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, ("XSL file: " + xslFileName));
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, XML);
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "---------------------------------------------");
                throw ex;
            }
        }
        return sPage;
    }

    String getActionsFileName() {
        return actionsFileName;
    }

    void setActionsFileName(String actionsFileName) {
        this.actionsFileName = actionsFileName;
    }

    String getMessagesFileName() {
        return messagesFileNames;
    }

    void setMessagesFileName(String messagesFileName) {
        this.messagesFileNames = messagesFileName;
    }

    String getQuery(String queryName, String[] parameters) {
        return queries.getQuery(queryName, parameters);
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            return null;
        }
        return dataSource.getConnection();
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public Map<String, String> getReverseLinks() {
        return reverseLinks;
    }

    String replaceLinks(String sPage, HttpServletRequest request) {
        if (links.isEmpty()) {
            return sPage;
        }
        HTMLDocument doc = null;
        HTMLEditorKit hek = new HTMLEditorKit();
        doc = (HTMLDocument) hek.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        try {
            hek.read(new StringReader(sPage), doc, 0);
            sPage = modifyPageContent(request.getRequestURL().toString(), request.getSession().getServletContext().getContextPath(), sPage, doc.getIterator(HTML.Tag.A), HTML.Attribute.HREF);
            sPage = modifyPageContent(request.getRequestURL().toString(), request.getSession().getServletContext().getContextPath(), sPage, doc.getIterator(HTML.Tag.IMG), HTML.Attribute.SRC);
        } catch (IOException ex) {
            Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (BadLocationException ex) {
            Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return sPage;
    }

    private String addSecure(String href, String localName, int localPort, String context) {
        String newHref = href;
        try {
            String action = null;
            int ndx = href.indexOf("action=");
            int ndx1 = -1;
            if (ndx > -1) {
                ndx1 = href.indexOf("&", ndx);
            }
            if (ndx > -1 && ndx1 > -1) {
                action = href.substring(ndx, ndx1);
            }
            if (action == null || actions.getProtocol(action).equals("https://")) {
                if (!href.startsWith("http")) {
                    newHref = "https://" + localName + ":" + localPort + "/" + context + "/" + href;
                } else {
                    newHref = href.replace("http://", "https://");
                }
            } else {
                if (!href.startsWith("http")) {
                    newHref = "http://" + localName + ":" + localPort + "/" + context + "/" + href;
                } else {
                    newHref = href.replace("https://", "http://");
                }
            }
            System.out.println("href: " + newHref);
        } catch (ActionNotFoundException ex) {
            Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        return href;
    }

    public void readLinks(String path, String path1) {
        if (links.size() > 0) {
            return;
        }
        ObjectInputStream ois = null;
        ObjectInputStream ois1 = null;
        try {
            FileInputStream fis = new FileInputStream(path);
            ois = new ObjectInputStream(fis);
            links = (HashMap<String, String>) ois.readObject();
            FileInputStream fis1 = new FileInputStream(path1);
            ois1 = new ObjectInputStream(fis1);
            reverseLinks = (HashMap<String, String>) ois1.readObject();
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException e) {
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ex) {
                    Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (ois1 != null) {
                try {
                    ois1.close();
                } catch (IOException ex) {
                    Logger.getLogger(ControlBean.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private String modifyPageContent(String url, String context, String sPage, HTMLDocument.Iterator it, HTML.Attribute atr) {
        if (it != null) {
            while (it.isValid()) {
                String href = ((String) it.getAttributes().getAttribute(atr));
                if (href != null) {
                    if (href.startsWith(url)) {
                        if (context.length() > 0) {
                            int ndx = href.indexOf('?');
                            if (ndx > -1) {
                                href = href.substring(ndx + context.length() + 1);
                            }
                        } else {
                            int ndx = href.indexOf('/', 8);
                            if (ndx > -1) {
                                href = href.substring(ndx + 1);
                            }
                        }
                    }
                    String newHref = links.get(href);
                    if (newHref == null) {
                        newHref = href;
                    }
                    sPage = sPage.replace(href.replace("&", "&amp;"), newHref);
                    sPage = sPage.replace(href, newHref);
                }
                it.next();
            }
        }
        return sPage;
    }

    String getPageSubmitedForm(JoinRequest jRequest) {
        String token = jRequest.getParameter("imivic.formToken");
        if (token == null) {
            return null;
        }
        return formTokens.get(token);
    }

    void addFormToken(String sPage, JoinRequest jRequest) {
        String token = jRequest.getParameter("imivic.formToken");
        if (token != null) {
            if (sPage == null) {
                sPage = "";
            }
            formTokens.put(token, sPage);
            if (formTokens.size() > FORMTOKENSCOUNT) {
                Iterator<String> it = formTokens.values().iterator();
                it.next();
                it.remove();
            }
        }
    }
}
