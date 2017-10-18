package com.mdt.rtm;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpClientConnection;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.HttpGet;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpExecutionContext;
import org.apache.http.protocol.HttpRequestExecutor;
import org.apache.http.protocol.RequestConnControl;
import org.apache.http.protocol.RequestContent;
import org.apache.http.protocol.RequestExpectContinue;
import org.apache.http.protocol.RequestTargetHost;
import org.apache.http.protocol.RequestUserAgent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Handles the details of invoking a method on the RTM REST API.
 * 
 * @author Will Ross Jun 21, 2007
 */
public class Invoker {

    private static final Log log = LogFactory.getLog("Invoker");

    private static final DocumentBuilder builder;

    static {
        DocumentBuilder aBuilder;
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            aBuilder = factory.newDocumentBuilder();
        } catch (Exception exception) {
            log.error("Unable to construct a document builder", exception);
            aBuilder = null;
        }
        builder = aBuilder;
    }

    public static final String REST_SERVICE_URL_POSTFIX = "/services/rest/";

    public static final String ENCODING = "UTF-8";

    public static String API_SIG_PARAM = "api_sig";

    public static final long INVOCATION_INTERVAL = 2000;

    private long lastInvocation;

    private final ApplicationInfo applicationInfo;

    private final MessageDigest digest;

    private String proxyHostName;

    private int proxyPortNumber;

    private String proxyLogin;

    private String proxyPassword;

    private String serviceRelativeUri;

    private HttpHost host;

    private HttpContext context;

    private BasicHttpParams globalHttpParams;

    private DefaultConnectionReuseStrategy connectionStrategy;

    private BasicHttpProcessor httpProcessor;

    private HttpRequestExecutor httpExecutor;

    private DefaultHttpClientConnection connection;

    public Invoker(String serverHostName, int serverPortNumber, String serviceRelativeUri, ApplicationInfo applicationInfo) throws ServiceInternalException {
        this.serviceRelativeUri = serviceRelativeUri;
        host = new HttpHost(serverHostName, serverPortNumber);
        context = new HttpExecutionContext(null);
        context.setAttribute(HttpExecutionContext.HTTP_TARGET_HOST, host);
        globalHttpParams = new BasicHttpParams();
        HttpProtocolParams.setVersion(globalHttpParams, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(globalHttpParams, ENCODING);
        HttpProtocolParams.setUserAgent(globalHttpParams, "Jakarta-HttpComponents/1.1");
        HttpProtocolParams.setUseExpectContinue(globalHttpParams, true);
        connectionStrategy = new DefaultConnectionReuseStrategy();
        httpProcessor = new BasicHttpProcessor();
        httpProcessor.addInterceptor(new RequestContent());
        httpProcessor.addInterceptor(new RequestTargetHost());
        httpProcessor.addInterceptor(new RequestConnControl());
        httpProcessor.addInterceptor(new RequestUserAgent());
        httpProcessor.addInterceptor(new RequestExpectContinue());
        httpExecutor = new HttpRequestExecutor(httpProcessor);
        httpExecutor.setParams(globalHttpParams);
        lastInvocation = System.currentTimeMillis();
        this.applicationInfo = applicationInfo;
        try {
            digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceInternalException("Could not create properly the MD5 digest", e);
        }
    }

    public void setHttpProxySettings(String proxyHostName, int proxyPortNumber, String proxyLogin, String proxyPassword) {
        this.proxyHostName = proxyHostName;
        this.proxyPortNumber = proxyPortNumber;
        this.proxyLogin = proxyLogin;
        this.proxyPassword = proxyPassword;
    }

    private void prepareConnection() throws ServiceInternalException {
        connection = new DefaultHttpClientConnection();
        try {
            if (connection.isOpen() == false) {
                final Socket socket = new Socket(host.getHostName(), host.getPort());
                connection.bind(socket, globalHttpParams);
            }
        } catch (Exception exception) {
            final StringBuffer message = new StringBuffer("Cannot open a socket connection to '").append(host.getHostName()).append("' on port number ").append(host.getPort()).append(": cannot execute query");
            log.error(message, exception);
            throw new ServiceInternalException(message.toString());
        }
    }

    private StringBuffer computeRequestUri(Param... params) throws ServiceInternalException {
        final StringBuffer requestUri = new StringBuffer(serviceRelativeUri);
        if (params.length > 0) {
            requestUri.append("?");
        }
        for (Param param : params) {
            try {
                requestUri.append(param.getName()).append("=").append(URLEncoder.encode(param.getValue(), ENCODING)).append("&");
            } catch (Exception exception) {
                final StringBuffer message = new StringBuffer("Cannot encode properly the HTTP GET request URI: cannot execute query");
                log.error(message, exception);
                throw new ServiceInternalException(message.toString());
            }
        }
        requestUri.append(API_SIG_PARAM).append("=").append(calcApiSig(params));
        return requestUri;
    }

    public Element invoke(Param... params) throws ServiceException {
        long timeSinceLastInvocation = System.currentTimeMillis() - lastInvocation;
        if (timeSinceLastInvocation < INVOCATION_INTERVAL) {
            try {
                Thread.sleep(INVOCATION_INTERVAL - timeSinceLastInvocation);
            } catch (InterruptedException e) {
                throw new ServiceInternalException("Unexpected interruption while attempting to pause for some time before invoking the RTM service back", e);
            }
        }
        log.debug("Invoker running at " + new Date());
        prepareConnection();
        final StringBuffer requestUri = computeRequestUri(params);
        HttpResponse response = null;
        final HttpGet request = new HttpGet(requestUri.toString());
        request.setHeader(new BasicHeader(HTTP.CHARSET_PARAM, ENCODING));
        final String methodUri = request.getRequestLine().getUri();
        Element result;
        try {
            log.info("Executing the method:" + methodUri);
            response = httpExecutor.execute(request, connection, context);
            final int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                log.error("Method failed: " + response.getStatusLine());
                throw new ServiceInternalException("method failed: " + response.getStatusLine());
            }
            final String responseBodyAsString = "";
            log.info("  Invocation response:\r\n" + responseBodyAsString);
            final Document responseDoc = builder.parse(response.getEntity().getContent());
            final Element wrapperElt = responseDoc.getDocumentElement();
            if (!wrapperElt.getNodeName().equals("rsp")) {
                throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
            } else {
                String stat = wrapperElt.getAttribute("stat");
                if (stat.equals("fail")) {
                    Node errElt = wrapperElt.getFirstChild();
                    while (errElt != null && (errElt.getNodeType() != Node.ELEMENT_NODE || !errElt.getNodeName().equals("err"))) {
                        errElt = errElt.getNextSibling();
                    }
                    if (errElt == null) {
                        throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
                    } else {
                        throw new ServiceException(Integer.parseInt(((Element) errElt).getAttribute("code")), ((Element) errElt).getAttribute("msg"));
                    }
                } else {
                    Node dataElt = wrapperElt.getFirstChild();
                    while (dataElt != null && (dataElt.getNodeType() != Node.ELEMENT_NODE || dataElt.getNodeName().equals("transaction") == true)) {
                        try {
                            Node nextSibling = dataElt.getNextSibling();
                            if (nextSibling == null) {
                                break;
                            } else {
                                dataElt = nextSibling;
                            }
                        } catch (IndexOutOfBoundsException exception) {
                            break;
                        }
                    }
                    if (dataElt == null) {
                        throw new ServiceInternalException("unexpected response returned by RTM service: " + responseBodyAsString);
                    } else {
                        result = (Element) dataElt;
                    }
                }
            }
        } catch (IOException e) {
            throw new ServiceInternalException("", e);
        } catch (SAXException e) {
            throw new ServiceInternalException("", e);
        } catch (HttpException e) {
            throw new ServiceInternalException("", e);
        } finally {
            if (connection != null && (response == null || connectionStrategy.keepAlive(response, context) == false)) {
                try {
                    connection.close();
                } catch (IOException exception) {
                    log.warn(new StringBuffer("Could not close properly the socket connection to '").append(connection.getRemoteAddress()).append("' on port ").append(connection.getRemotePort()), exception);
                }
            }
        }
        lastInvocation = System.currentTimeMillis();
        return result;
    }

    final String calcApiSig(Param... params) throws ServiceInternalException {
        try {
            digest.reset();
            digest.update(applicationInfo.getSharedSecret().getBytes(ENCODING));
            List<Param> sorted = Arrays.asList(params);
            Collections.sort(sorted);
            for (Param param : sorted) {
                digest.update(param.getName().getBytes(ENCODING));
                digest.update(param.getValue().getBytes(ENCODING));
            }
            return convertToHex(digest.digest());
        } catch (UnsupportedEncodingException e) {
            throw new ServiceInternalException("cannot hahdle properly the encoding", e);
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }
}
