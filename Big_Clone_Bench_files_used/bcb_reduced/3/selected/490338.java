package gnu.mail.providers.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.URLName;
import javax.mail.event.TransportEvent;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.TrustManager;
import gnu.inet.smtp.Parameter;
import gnu.inet.smtp.ParameterList;
import gnu.inet.smtp.SMTPConnection;
import gnu.inet.util.BASE64;

/** 
 * This transport handles communications with an SMTP server.
 *
 * @author Andrew Selkirk
 * @author Ben Speakmon
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @author Arend Freije
 * @version 2.0
 */
public class SMTPTransport extends Transport {

    /**
   * The connection used to communicate with the server.
   */
    protected SMTPConnection connection;

    protected String localHostName;

    private List extensions = null;

    private List authenticationMechanisms = null;

    /**
   * Creates a new <code>SMTPTransport</code> instance.
   *
   * @param session a <code>Session</code> value
   * @param urlName an <code>URLName</code> value
   */
    public SMTPTransport(Session session, URLName urlName) {
        super(session, urlName);
        localHostName = getProperty("localhost");
        if (localHostName == null) {
            try {
                localHostName = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                localHostName = "localhost";
            }
        }
    }

    /**
   * Connects to the SMTP server.
   */
    protected boolean protocolConnect(String host, int port, String username, String password) throws MessagingException {
        if (connection != null) {
            return true;
        }
        if (host == null) {
            host = getProperty("host");
        }
        if (port < 0) {
            port = getIntProperty("port");
        }
        if (username == null) {
            username = getProperty("user");
        }
        if (host == null) {
            host = "localhost";
        }
        try {
            int connectionTimeout = getIntProperty("connectiontimeout");
            int timeout = getIntProperty("timeout");
            if (session.getDebug()) {
                SMTPConnection.logger.setLevel(SMTPConnection.SMTP_TRACE);
            }
            boolean tls = "stmps".equals(url.getProtocol());
            TrustManager tm = null;
            if (tls) {
                tm = getTrustManager();
            }
            connection = new SMTPConnection(host, port, connectionTimeout, timeout, tls, tm);
            if (propertyIsFalse("ehlo")) {
                if (!connection.helo(localHostName)) throw new MessagingException("HELO failed: " + connection.getLastResponse());
            } else {
                extensions = connection.ehlo(localHostName);
                if (extensions == null) {
                    if (!connection.helo(localHostName)) {
                        throw new MessagingException("HELO failed: " + connection.getLastResponse());
                    }
                } else {
                    if (!tls && extensions.contains("STARTTLS")) {
                        if (!propertyIsFalse("tls")) {
                            tm = getTrustManager();
                            if (tm == null) {
                                tls = connection.starttls();
                            } else {
                                tls = connection.starttls(tm);
                            }
                            if (tls) {
                                extensions = connection.ehlo(localHostName);
                            }
                        }
                    }
                    if (!tls && "required".equals(getProperty("tls"))) {
                        throw new MessagingException("TLS not available");
                    }
                    for (Iterator i = extensions.iterator(); i.hasNext(); ) {
                        String extension = (String) i.next();
                        if (extension.startsWith("AUTH ")) {
                            String m = extension.substring(5);
                            authenticationMechanisms = Collections.list(new StringTokenizer(m));
                        }
                    }
                }
            }
            String auth = getProperty("auth");
            boolean authRequired = "required".equals(auth);
            if (authenticationMechanisms == null || authenticationMechanisms.isEmpty()) {
                return !authRequired;
            }
            if (authRequired || propertyIsTrue("auth")) {
                if (username == null || password == null) {
                    PasswordAuthentication pa = session.getPasswordAuthentication(url);
                    if (pa == null) {
                        InetAddress addr = InetAddress.getByName(host);
                        pa = session.requestPasswordAuthentication(addr, port, url.getProtocol(), null, null);
                    }
                    if (pa != null) {
                        username = pa.getUserName();
                        password = pa.getPassword();
                    }
                }
                if (username != null && password != null) {
                    String authPrefs = getProperty("auth.mechanisms");
                    Iterator i = null;
                    if (authPrefs == null) {
                        i = authenticationMechanisms.iterator();
                    } else {
                        List authPrefList = Collections.list(new StringTokenizer(authPrefs, ","));
                        i = authPrefList.iterator();
                    }
                    while (i.hasNext()) {
                        String mechanism = (String) i.next();
                        if (authenticationMechanisms.contains(mechanism) && connection.authenticate(mechanism, username, password)) {
                            return true;
                        }
                    }
                } else {
                    if (session.getDebug()) {
                        debugWarning("server requested AUTH, " + "but authentication principal " + "and credentials are not available");
                    }
                }
                return false;
            } else {
                if (session.getDebug()) {
                    debugWarning("server requested AUTH, " + "but authentication is not enabled");
                }
            }
            return !authRequired;
        } catch (IOException e) {
            throw new MessagingException(e.getMessage(), e);
        }
    }

    private TrustManager getTrustManager() throws MessagingException {
        String tmt = getProperty("trustmanager");
        if (tmt != null) {
            try {
                Class t = Class.forName(tmt);
                return (TrustManager) t.newInstance();
            } catch (Exception e) {
                throw new MessagingException(e.getMessage(), e);
            }
        }
        return null;
    }

    private void debugWarning(final String warning) {
        System.err.println(url.getProtocol() + ": WARNING: " + warning);
    }

    /**
   * Returns the greeting banner.
   */
    public String getGreeting() throws MessagingException {
        if (!isConnected()) {
            throw new MessagingException("not connected");
        }
        synchronized (connection) {
            return connection.getGreeting();
        }
    }

    /**
   * Send the specified message to the server.
   */
    public void sendMessage(Message message, Address[] addresses) throws MessagingException, SendFailedException {
        if (!isConnected()) {
            throw new MessagingException("not connected");
        }
        if (!(message instanceof MimeMessage)) {
            throw new SendFailedException("only MimeMessages are supported");
        }
        MimeMessage mimeMessage = (MimeMessage) message;
        int len = addresses.length;
        List sent = new ArrayList(len);
        List unsent = new ArrayList(len);
        List invalid = new ArrayList(len);
        int deliveryStatus = TransportEvent.MESSAGE_NOT_DELIVERED;
        ParameterList params = null;
        synchronized (connection) {
            try {
                String from0 = getProperty("from");
                InternetAddress from = null;
                if (from0 != null) {
                    InternetAddress[] from1 = InternetAddress.parse(from0);
                    if (from1 != null && from1.length > 0) {
                        from = from1[0];
                    }
                }
                if (from == null) {
                    Address[] from2 = mimeMessage.getFrom();
                    if (from2 != null && from2.length > 0 && from2[0] instanceof InternetAddress) {
                        from = (InternetAddress) from2[0];
                    }
                }
                if (from == null) {
                    from = InternetAddress.getLocalAddress(session);
                }
                String reversePath = from.getAddress();
                String dsnRet = getProperty("dsn.ret");
                if (dsnRet != null && extensions != null && extensions.contains("DSN")) {
                    String FULL = "FULL", HDRS = "HDRS";
                    String value = null;
                    if (FULL.equalsIgnoreCase(dsnRet)) {
                        value = FULL;
                    } else if (HDRS.equalsIgnoreCase(dsnRet)) {
                        value = HDRS;
                    }
                    if (value != null) {
                        if (params == null) params = new ParameterList();
                        params.add(new Parameter("RET", value));
                    }
                }
                String mtrk = getProperty("mtrk");
                if ("true".equals(mtrk) && extensions != null && extensions.contains("MTRK")) {
                    int mtrkTimeout = 0;
                    String mt = getProperty("mtrk.timeout");
                    if (mt != null) mtrkTimeout = Integer.parseInt(mt);
                    try {
                        Random r = new Random();
                        byte[] a = new byte[256];
                        r.nextBytes(a);
                        MessageDigest md = MessageDigest.getInstance("SHA-1");
                        md.update(a);
                        byte[] b = md.digest();
                        byte[] certifier = BASE64.encode(b);
                        if (params == null) params = new ParameterList();
                        String value = new String(certifier, "US-ASCII");
                        if (mtrkTimeout > 0) {
                            value += ":" + mtrkTimeout;
                        }
                        params.add(new Parameter("MTRK", value));
                        String envid = mimeMessage.getMessageID();
                        if (envid != null) {
                            int ai = envid.indexOf('@');
                            if (ai != -1) envid = envid.substring(0, ai);
                        } else {
                            envid = "";
                        }
                        envid += Long.toHexString(System.currentTimeMillis());
                        envid += "@";
                        String lha = InetAddress.getLocalHost().getHostAddress();
                        if (envid.length() + lha.length() > 100) {
                            b = lha.getBytes("UTF-8");
                            md.reset();
                            md.update(b);
                            b = md.digest();
                            b = BASE64.encode(b);
                            lha = new String(b, "US-ASCII");
                        }
                        envid += lha;
                        params.add(new Parameter("ENVID", envid));
                    } catch (NoSuchAlgorithmException e) {
                        MessagingException e2 = new MessagingException(e.getMessage());
                        e2.initCause(e);
                        throw e2;
                    } catch (UnsupportedEncodingException e) {
                        MessagingException e2 = new MessagingException(e.getMessage());
                        e2.initCause(e);
                        throw e2;
                    }
                }
                if (!connection.mailFrom(reversePath, params)) {
                    throw new SendFailedException(connection.getLastResponse());
                }
                params = null;
                String dsnNotify = getProperty("dsn.notify");
                if (dsnNotify != null && extensions != null && extensions.contains("DSN")) {
                    String NEVER = "NEVER", SUCCESS = "SUCCESS";
                    String FAILURE = "FAILURE", DELAY = "DELAY";
                    String value = null;
                    if (NEVER.equalsIgnoreCase(dsnNotify)) {
                        value = NEVER;
                    } else {
                        StringBuffer buf = new StringBuffer();
                        StringTokenizer st = new StringTokenizer(dsnNotify, " ,");
                        while (st.hasMoreTokens()) {
                            String token = st.nextToken();
                            if (SUCCESS.equalsIgnoreCase(token)) {
                                if (buf.length() > 0) {
                                    buf.append(',');
                                }
                                buf.append(SUCCESS);
                            } else if (FAILURE.equalsIgnoreCase(token)) {
                                if (buf.length() > 0) {
                                    buf.append(',');
                                }
                                buf.append(FAILURE);
                            } else if (DELAY.equalsIgnoreCase(token)) {
                                if (buf.length() > 0) {
                                    buf.append(',');
                                }
                                buf.append(DELAY);
                            }
                        }
                        if (buf.length() > 0) {
                            value = buf.toString();
                        }
                    }
                    if (value != null) {
                        params = new ParameterList();
                        params.add(new Parameter("NOTIFY", value));
                    }
                }
                for (int i = 0; i < addresses.length; i++) {
                    Address address = addresses[i];
                    if (address instanceof InternetAddress) {
                        String forwardPath = ((InternetAddress) address).getAddress();
                        if (connection.rcptTo(forwardPath, params)) {
                            sent.add(address);
                        } else {
                            invalid.add(address);
                        }
                    } else {
                        invalid.add(address);
                    }
                }
            } catch (IOException e) {
                try {
                    connection.rset();
                } catch (IOException e2) {
                }
                throw new SendFailedException(e.getMessage());
            }
            if (sent.size() > 0) {
                try {
                    OutputStream dataStream = connection.data();
                    if (dataStream == null) {
                        String msg = connection.getLastResponse();
                        throw new MessagingException(msg);
                    }
                    mimeMessage.writeTo(dataStream);
                    dataStream.flush();
                    if (!connection.finishData()) {
                        unsent.addAll(sent);
                        sent.clear();
                        deliveryStatus = TransportEvent.MESSAGE_NOT_DELIVERED;
                    } else {
                        deliveryStatus = invalid.isEmpty() ? TransportEvent.MESSAGE_DELIVERED : TransportEvent.MESSAGE_PARTIALLY_DELIVERED;
                    }
                } catch (IOException e) {
                    try {
                        if (connection.finishData()) {
                            connection.rset();
                        }
                    } catch (IOException e2) {
                    }
                    throw new SendFailedException(e.getMessage());
                }
            }
        }
        Address[] a_sent = new Address[sent.size()];
        sent.toArray(a_sent);
        Address[] a_unsent = new Address[unsent.size()];
        unsent.toArray(a_unsent);
        Address[] a_invalid = new Address[invalid.size()];
        invalid.toArray(a_invalid);
        notifyTransportListeners(deliveryStatus, a_sent, a_unsent, a_invalid, mimeMessage);
    }

    /**
   * Close this transport.
   */
    public void close() throws MessagingException {
        if (isConnected()) {
            synchronized (connection) {
                try {
                    connection.quit();
                } catch (IOException e) {
                    throw new MessagingException(e.getMessage(), e);
                } finally {
                    connection = null;
                }
            }
        }
        super.close();
    }

    private int getIntProperty(String key) {
        String value = getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
            }
        }
        return -1;
    }

    private boolean propertyIsFalse(String key) {
        return "false".equals(getProperty(key));
    }

    private boolean propertyIsTrue(String key) {
        return "true".equals(getProperty(key));
    }

    private String getProperty(String key) {
        String value = session.getProperty("mail." + url.getProtocol() + "." + key);
        if (value == null) {
            value = session.getProperty("mail." + key);
        }
        return value;
    }
}
