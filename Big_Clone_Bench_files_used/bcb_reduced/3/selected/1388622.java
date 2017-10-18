package com.anzsoft.server.JabberHTTPBind;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * An implementation of JEP-0124 (HTTP Binding). See
 * http://www.jabber.org/jeps/jep-0124.html for details.
 * 
 * @author Stefan Strigler <steve@zeank.in-berlin.de>
 */
public final class JHBServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public static final String APP_VERSION = "1.1";

    public static final String APP_NAME = "Jabber HTTP Binding Servlet";

    public static final boolean DEBUG = false;

    public static final int DEBUG_LEVEL = 2;

    private DocumentBuilder db;

    private Janitor janitor;

    public void init() throws ServletException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            log("failed to create DocumentBuilderFactory", e);
        }
        janitor = new Janitor();
        new Thread(janitor).start();
    }

    public void destroy() {
        Session.stopSessions();
        janitor.stop();
    }

    public static String hex(byte[] array) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toLowerCase().substring(1, 3));
        }
        return sb.toString();
    }

    public static String sha1(String message) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            return hex(sha.digest(message.getBytes()));
        } catch (NoSuchAlgorithmException e) {
        }
        return null;
    }

    public static void dbg(String msg) {
        dbg(msg, 0);
    }

    public static void dbg(String msg, int lvl) {
        if (!DEBUG) return;
        if (lvl > DEBUG_LEVEL) return;
        System.err.println("[" + lvl + "] " + msg);
    }

    /**
	 * We only need to respond to POST requests ...
	 * 
	 * @param request
	 *            The servlet request we are processing
	 * @param response
	 *            The servlet response we are producing
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 * @exception ServletException
	 *                if a servlet error occurs
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        long rid = 0;
        try {
            Document doc;
            synchronized (db) {
                doc = db.parse(request.getInputStream());
            }
            Node rootNode = doc.getDocumentElement();
            if (rootNode == null || !rootNode.getNodeName().equals("body")) response.sendError(HttpServletResponse.SC_BAD_REQUEST); else {
                NamedNodeMap attribs = rootNode.getAttributes();
                if (attribs.getNamedItem("sid") != null) {
                    Session sess = Session.getSession(attribs.getNamedItem("sid").getNodeValue());
                    if (sess != null) {
                        dbg("incoming request for " + sess.getSID(), 3);
                        if (attribs.getNamedItem("rid") == null) {
                            dbg("rid missing", 1);
                            response.sendError(HttpServletResponse.SC_NOT_FOUND);
                            sess.terminate();
                        } else {
                            try {
                                rid = Integer.parseInt(attribs.getNamedItem("rid").getNodeValue());
                            } catch (NumberFormatException e) {
                                dbg("rid not a number", 1);
                                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                                return;
                            }
                            Response r = sess.getResponse(rid);
                            if (r != null) {
                                dbg("resend rid " + rid, 2);
                                r.setAborted(true);
                                r.send(response);
                                return;
                            }
                            if (!sess.checkValidRID(rid)) {
                                dbg("invalid rid " + rid, 1);
                                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                sess.terminate();
                                return;
                            }
                        }
                        dbg("found valid rid " + rid, 3);
                        if (sess.numPendingRequests() >= Session.MAX_REQUESTS) {
                            dbg("too many simultaneous requests: " + sess.numPendingRequests(), 1);
                            response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            sess.terminate();
                            return;
                        }
                        Response jresp = new Response(db.newDocument());
                        jresp.setRID(rid);
                        jresp.setContentType(sess.getContent());
                        sess.addResponse(jresp);
                        try {
                            synchronized (sess.sock) {
                                long lastrid = sess.getLastDoneRID();
                                while (rid != lastrid + 1) {
                                    if (sess.isStatus(Session.SESS_TERM)) {
                                        dbg("session terminated for " + rid, 1);
                                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                        sess.sock.notifyAll();
                                        return;
                                    }
                                    try {
                                        dbg(rid + " waiting for " + (lastrid + 1), 2);
                                        sess.sock.wait();
                                        dbg("bell for " + rid, 2);
                                        lastrid = sess.getLastDoneRID();
                                    } catch (InterruptedException e) {
                                    }
                                }
                                dbg("handling response " + rid, 3);
                                String key = sess.getKey();
                                if (key != null) {
                                    dbg("checking keys for " + rid, 3);
                                    if (attribs.getNamedItem("key") == null || !sha1(attribs.getNamedItem("key").getNodeValue()).equals(key)) {
                                        dbg("Key sequence error", 1);
                                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                                        sess.terminate();
                                        return;
                                    }
                                    if (attribs.getNamedItem("newkey") != null) sess.setKey(attribs.getNamedItem("newkey").getNodeValue()); else sess.setKey(attribs.getNamedItem("key").getNodeValue());
                                    dbg("key valid for " + rid, 3);
                                }
                                if (attribs.getNamedItem("xmpp:restart") != null) {
                                    dbg("XMPP RESTART", 2);
                                    sess.setReinit(true);
                                }
                                if (rootNode.hasChildNodes()) sess.sendNodes(rootNode.getChildNodes()); else {
                                    long now = System.currentTimeMillis();
                                    if (sess.getHold() == 0 && now - sess.getLastPoll() < Session.MIN_POLLING * 1000) {
                                        dbg("polling too frequently! [now:" + now + ", last:" + sess.getLastPoll() + "(" + (now - sess.getLastPoll()) + ")]", 1);
                                        response.sendError(HttpServletResponse.SC_FORBIDDEN);
                                        sess.terminate();
                                        return;
                                    }
                                    sess.setLastPoll();
                                }
                                if (attribs.getNamedItem("type") != null) {
                                    String rType = attribs.getNamedItem("type").getNodeValue();
                                    if (rType.equals("terminate")) {
                                        sess.terminate();
                                        jresp.send(response);
                                        return;
                                    }
                                }
                                NodeList nl = sess.checkInQ(rid);
                                if (nl != null) for (int i = 0; i < nl.getLength(); i++) jresp.addNode(nl.item(i), "jabber:client");
                                if (sess.streamFeatures) {
                                    jresp.setAttribute("xmlns:stream", "http://etherx.jabber.org/streams");
                                    sess.streamFeatures = false;
                                }
                                if (!sess.authidSent && sess.getAuthid() != null) {
                                    sess.authidSent = true;
                                    jresp.setAttribute("authid", sess.getAuthid());
                                }
                                if (sess.isStatus(Session.SESS_TERM)) {
                                    jresp.setAttribute("type", "terminate");
                                    jresp.setAttribute("condition", "remote-stream-error");
                                }
                                jresp.send(response);
                                sess.setLastDoneRID(jresp.getRID());
                                sess.sock.notifyAll();
                            }
                        } catch (IOException ioe) {
                            sess.terminate();
                            jresp.setAttribute("type", "terminate");
                            jresp.setAttribute("condition", "remote-connection-failed");
                            jresp.send(response);
                        }
                    } else {
                        response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    }
                } else {
                    if (attribs.getNamedItem("rid") == null) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    } else {
                        try {
                            rid = Integer.parseInt(attribs.getNamedItem("rid").getNodeValue());
                        } catch (NumberFormatException e) {
                            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                            return;
                        }
                    }
                    Response jresp = new Response(db.newDocument(), request);
                    jresp.setRID(rid);
                    String route = null;
                    if (attribs.getNamedItem("route") != null && isValidRoute(attribs.getNamedItem("route").getNodeValue())) {
                        route = attribs.getNamedItem("route").getNodeValue().substring("xmpp:".length());
                    }
                    String to = null;
                    if ((attribs.getNamedItem("to") != null) && (attribs.getNamedItem("to").getNodeValue() != "")) {
                        to = attribs.getNamedItem("to").getNodeValue();
                    }
                    if (to == null || to.equals("")) {
                        if (attribs.getNamedItem("content") != null) {
                            jresp.setContentType(attribs.getNamedItem("content").getNodeValue());
                        } else {
                            jresp.setContentType(Session.DEFAULT_CONTENT);
                        }
                        jresp.setAttribute("type", "terminate");
                        jresp.setAttribute("condition", "improper-addressing");
                        jresp.send(response);
                        return;
                    }
                    try {
                        Session sess = new Session(to, route);
                        if (attribs.getNamedItem("content") != null) sess.setContent(attribs.getNamedItem("content").getNodeValue());
                        if (attribs.getNamedItem("wait") != null) sess.setWait(Integer.parseInt(attribs.getNamedItem("wait").getNodeValue()));
                        if (attribs.getNamedItem("hold") != null) sess.setHold(Integer.parseInt(attribs.getNamedItem("hold").getNodeValue()));
                        if (attribs.getNamedItem("xml:lang") != null) sess.setXMLLang(attribs.getNamedItem("xml:lang").getNodeValue());
                        if (attribs.getNamedItem("newkey") != null) sess.setKey(attribs.getNamedItem("newkey").getNodeValue());
                        if (attribs.getNamedItem("secure") != null && (attribs.getNamedItem("secure").getNodeValue().equals("true") || attribs.getNamedItem("secure").getNodeValue().equals("1"))) sess.setSecure(true);
                        sess.addResponse(jresp);
                        jresp.setContentType(sess.getContent());
                        NodeList nl = sess.checkInQ(jresp.getRID());
                        if (nl != null) for (int i = 0; i < nl.getLength(); i++) {
                            if (!nl.item(i).getNodeName().equals("starttls")) jresp.addNode(nl.item(i), "");
                        }
                        if (sess.streamFeatures) {
                            jresp.setAttribute("xmlns:stream", "http://etherx.jabber.org/streams");
                            sess.streamFeatures = false;
                        }
                        jresp.setAttribute("sid", sess.getSID());
                        jresp.setAttribute("wait", String.valueOf(sess.getWait()));
                        jresp.setAttribute("inactivity", String.valueOf(Session.MAX_INACTIVITY));
                        jresp.setAttribute("polling", String.valueOf(Session.MIN_POLLING));
                        jresp.setAttribute("requests", String.valueOf(Session.MAX_REQUESTS));
                        if (sess.getAuthid() != null) {
                            sess.authidSent = true;
                            jresp.setAttribute("authid", sess.getAuthid());
                        }
                        if (sess.isStatus(Session.SESS_TERM)) jresp.setAttribute("type", "terminate");
                        jresp.send(response);
                        sess.setLastDoneRID(jresp.getRID());
                    } catch (UnknownHostException uhe) {
                        if (attribs.getNamedItem("content") != null) jresp.setContentType(attribs.getNamedItem("content").getNodeValue()); else jresp.setContentType(Session.DEFAULT_CONTENT);
                        jresp.setAttribute("type", "terminate");
                        jresp.setAttribute("condition", "host-unknown");
                        jresp.send(response);
                    } catch (IOException ioe) {
                        if (attribs.getNamedItem("content") != null) jresp.setContentType(attribs.getNamedItem("content").getNodeValue()); else jresp.setContentType(Session.DEFAULT_CONTENT);
                        jresp.setAttribute("type", "terminate");
                        jresp.setAttribute("condition", "remote-connection-failed");
                        jresp.send(response);
                    } catch (NumberFormatException nfe) {
                        response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                        return;
                    }
                }
            }
        } catch (SAXException se) {
            dbg(se.toString(), 1);
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
        } catch (Exception e) {
            System.err.println(e.toString());
            e.printStackTrace();
            try {
                Response jresp = new Response(db.newDocument());
                jresp.setAttribute("type", "terminate");
                jresp.setAttribute("condition", "internal-server-error");
                jresp.send(response);
            } catch (Exception e2) {
                e2.printStackTrace();
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            }
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        String title = APP_NAME + " v" + APP_VERSION;
        writer.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">");
        writer.println("<html>\n<head>\n<title>" + title + "</title>\n</head>\n<body>\n");
        writer.println("<h1>" + title + "</h1>");
        writer.println("This is an implementation of JEP-0124 (HTTP-Binding). Please see <a href=\"http://www.jabber.org/jeps/jep-0124.html\">http://www.jabber.org/jeps/jep-0124.html</a> for details.<br/><br/>");
        writer.println("Active sessions: " + Session.getNumSessions());
        writer.println("\n</body>\n</html>");
    }

    /**
	 * Checks if supplied parameter is a valid host value. Valid host values
	 * should be in the form: "xmpp:" ihost [ ":" port ]
	 * 
	 * NOTE: RFC 3987 defines the form of ihost like this: ihost = IP-literal /
	 * IPv4address / ireg-name This function cuts corners and uses
	 * InetAddress.getByName() to check ihost validity.
	 * 
	 * 
	 * @see java.net.InetAddress
	 * @param route
	 *            hostname that should be checked
	 * @return 'true' if host is in the form "xmpp:" ihost [ ":" port ]
	 *         otherwise 'false'.
	 */
    private static boolean isValidRoute(String route) {
        if (!route.startsWith("xmpp:")) {
            return false;
        }
        route = route.substring("xmpp:".length());
        int port;
        if ((port = route.lastIndexOf(":")) != -1) {
            try {
                int p = Integer.parseInt(route.substring(port + 1));
                if (p < 0 || p > 65535) {
                    return false;
                }
            } catch (NumberFormatException nfe) {
                return false;
            }
            route = route.substring(0, port);
        }
        try {
            InetAddress.getByName(route);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
