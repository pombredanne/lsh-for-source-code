package org.dasein.cloud.euca.old;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.log4j.Logger;
import org.dasein.cloud.CloudException;
import org.dasein.cloud.InternalException;
import org.dasein.cloud.euca.old.storage.WalrusAction;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class WalrusMethod {

    public static final String CLOUD_FRONT_URL = "https://cloudfront.amazonaws.com";

    public static final String CF_VERSION = "2009-04-02";

    public static class S3Response {

        public long contentLength;

        public String contentType;

        public Document document;

        public Header[] headers;

        public InputStream input;

        public HttpMethod method;

        public void close() {
            try {
                input.close();
            } catch (Throwable ignore) {
            }
            try {
                method.releaseConnection();
            } catch (Throwable ignore) {
            }
        }
    }

    public static byte[] computeMD5Hash(String str) throws NoSuchAlgorithmException, IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(str.getBytes("utf-8"));
        return computeMD5Hash(input);
    }

    public static byte[] computeMD5Hash(InputStream is) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream bis = new BufferedInputStream(is);
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] buffer = new byte[16384];
            int bytesRead = -1;
            while ((bytesRead = bis.read(buffer, 0, buffer.length)) != -1) {
                messageDigest.update(buffer, 0, bytesRead);
            }
            return messageDigest.digest();
        } finally {
            try {
                bis.close();
            } catch (Exception e) {
                System.err.println("Unable to close input stream of hash candidate: " + e);
            }
        }
    }

    public static String getChecksum(File file) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        return toBase64(computeMD5Hash(new FileInputStream(file)));
    }

    public static String toBase64(byte[] data) {
        byte[] b64 = Base64.encodeBase64(data);
        return new String(b64);
    }

    private WalrusAction action = null;

    private String body = null;

    private String contentType = null;

    private Map<String, String> headers = null;

    private Map<String, String> parameters = null;

    private Eucalyptus provider = null;

    private File uploadFile = null;

    public WalrusMethod(Eucalyptus provider, WalrusAction action) {
        this.action = action;
        this.headers = new HashMap<String, String>();
        this.provider = provider;
    }

    public WalrusMethod(Eucalyptus provider, WalrusAction action, Map<String, String> parameters, Map<String, String> headers) {
        this.action = action;
        this.headers = (headers == null ? new HashMap<String, String>() : headers);
        this.provider = provider;
        this.parameters = parameters;
    }

    public WalrusMethod(Eucalyptus provider, WalrusAction action, Map<String, String> parameters, Map<String, String> headers, String contentType, String body) {
        this.action = action;
        this.headers = (headers == null ? new HashMap<String, String>() : headers);
        this.contentType = contentType;
        this.body = body;
        this.provider = provider;
        this.parameters = parameters;
    }

    public WalrusMethod(Eucalyptus provider, WalrusAction action, Map<String, String> parameters, Map<String, String> headers, String contentType, File uploadFile) {
        this.action = action;
        this.headers = (headers == null ? new HashMap<String, String>() : headers);
        this.contentType = contentType;
        this.uploadFile = uploadFile;
        this.provider = provider;
        this.parameters = parameters;
    }

    protected HttpClient getClient() {
        String proxyHost = provider.getContext().getCustomProperties().getProperty("proxyHost");
        String proxyPort = provider.getContext().getCustomProperties().getProperty("proxyPort");
        HttpClient client = new HttpClient();
        if (proxyHost != null) {
            int port = 0;
            if (proxyPort != null && proxyPort.length() > 0) {
                port = Integer.parseInt(proxyPort);
            }
            client.getHostConfiguration().setProxy(proxyHost, port);
        }
        return client;
    }

    private String getDate() throws CloudException {
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        return fmt.format(new Date());
    }

    public S3Response invoke(String bucket, String object) throws WalrusException, CloudException, InternalException {
        Logger logger = Eucalyptus.getLogger(WalrusMethod.class, "std");
        if (logger.isTraceEnabled()) {
            logger.trace("enter - " + WalrusMethod.class.getName() + ".invoke(" + bucket + "," + object + ")");
        }
        try {
            return invoke(bucket, object, null);
        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("exit - " + WalrusMethod.class.getName() + ".invoke()");
            }
        }
    }

    S3Response invoke(String bucket, String object, String temporaryEndpoint) throws WalrusException, CloudException, InternalException {
        Logger logger = Eucalyptus.getLogger(WalrusMethod.class, "std");
        Logger wire = Eucalyptus.getLogger(EucalyptusMethod.class, "wire");
        if (logger.isTraceEnabled()) {
            logger.trace("enter - " + WalrusMethod.class.getName() + ".invoke(" + bucket + "," + object + "," + temporaryEndpoint + ")");
        }
        if (wire.isDebugEnabled()) {
            wire.debug("[" + (new Date()) + "] -------------------------------------------------------------- [" + temporaryEndpoint + "]");
            wire.debug("");
        }
        try {
            StringBuilder url = new StringBuilder();
            boolean leaveOpen = false;
            HttpMethod method;
            HttpClient client;
            int status;
            int idx = 0;
            if (!provider.getContext().getEndpoint().startsWith("http")) {
                url.append("https://");
            } else {
                idx = provider.getContext().getEndpoint().indexOf("https://");
                if (idx == -1) {
                    idx = "http://".length();
                    url.append("http://");
                } else {
                    idx = "https://".length();
                    url.append("https://");
                }
            }
            if (temporaryEndpoint == null) {
                url.append(provider.getContext().getEndpoint().substring(idx));
                if (!provider.getContext().getEndpoint().endsWith("/")) {
                    url.append("/Walrus/");
                } else {
                    url.append("Walrus/");
                }
            } else {
                url.append(temporaryEndpoint);
                url.append("/Walrus/");
            }
            if (bucket != null) {
                url.append(bucket);
                url.append("/");
            }
            if (object != null) {
                url.append(object);
            }
            if (parameters != null) {
                boolean first = true;
                if (object != null && object.indexOf('?') != -1) {
                    first = false;
                }
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    String key = entry.getKey();
                    String val = entry.getValue();
                    if (first) {
                        url.append("?");
                        first = false;
                    } else {
                        url.append("&");
                    }
                    if (val != null) {
                        url.append(Eucalyptus.encode(key, false));
                        url.append("=");
                        url.append(Eucalyptus.encode(val, false));
                    } else {
                        url.append(Eucalyptus.encode(key, false));
                    }
                }
            }
            headers.put(Eucalyptus.P_DATE, getDate());
            method = action.getMethod(url.toString());
            method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    method.addRequestHeader(entry.getKey(), entry.getValue());
                }
            }
            try {
                String hash = null;
                String signature;
                signature = provider.signS3(new String(provider.getContext().getAccessPublic(), "utf-8"), provider.getContext().getAccessPrivate(), method.getName().toUpperCase(), hash, contentType, headers, bucket, object);
                method.addRequestHeader(Eucalyptus.P_CFAUTH, signature);
            } catch (UnsupportedEncodingException e) {
                Eucalyptus.getLogger(Eucalyptus.class, "std").error("invoke(): Unsupported encoding: " + e.getMessage());
                if (Eucalyptus.getLogger(Eucalyptus.class, "std").isDebugEnabled()) {
                    e.printStackTrace();
                }
                throw new InternalException(e);
            }
            if (wire.isDebugEnabled()) {
                wire.debug(method.getName() + " " + method.getPath() + (method.getQueryString() == null ? "" : "?" + method.getQueryString()));
                for (Header header : method.getRequestHeaders()) {
                    wire.debug(header.getName() + ": " + header.getValue());
                }
                wire.debug("");
            }
            if (body != null) {
                if (wire.isDebugEnabled()) {
                    String[] lines = body.split("\n");
                    if (lines.length < 1) {
                        lines = new String[] { body.toString() };
                    }
                    for (String l : lines) {
                        wire.debug(l);
                    }
                }
                if (method instanceof EntityEnclosingMethod) {
                    try {
                        ((EntityEnclosingMethod) method).setRequestEntity(new StringRequestEntity(body, contentType, "utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        logger.error("invoke(): Unsupported encoding: " + e.getMessage());
                        if (logger.isDebugEnabled()) {
                            e.printStackTrace();
                        }
                        throw new InternalException(e);
                    }
                }
            } else if (uploadFile != null) {
                wire.debug("[FILE:" + contentType + "]");
                if (method instanceof EntityEnclosingMethod) {
                    ((EntityEnclosingMethod) method).setRequestEntity(new FileRequestEntity(uploadFile, contentType));
                }
            }
            client = getClient();
            S3Response response = new S3Response();
            try {
                try {
                    status = client.executeMethod(method);
                    if (logger.isDebugEnabled()) {
                        logger.debug("HTTP STATUS: " + status);
                    }
                } catch (HttpException e) {
                    logger.error("invoke(): " + url + ": " + e.getMessage());
                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                    throw new CloudException(e);
                } catch (IOException e) {
                    logger.error("invoke(): " + url + ": " + e.getMessage());
                    if (logger.isTraceEnabled()) {
                        e.printStackTrace();
                    }
                    throw new InternalException(e);
                }
                response.headers = method.getResponseHeaders();
                if (wire.isDebugEnabled()) {
                    wire.debug(method.getStatusLine().toString());
                    for (Header h : response.headers) {
                        if (h.getValue() != null) {
                            wire.debug(h.getName() + ": " + h.getValue().trim());
                        } else {
                            wire.debug(h.getName() + ":");
                        }
                    }
                    wire.debug("");
                }
                if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_ACCEPTED) {
                    Header clen = method.getResponseHeader("Content-Length");
                    long len = -1L;
                    if (clen != null) {
                        len = Long.parseLong(clen.getValue());
                    }
                    if (len != 0L) {
                        try {
                            Header ct = method.getResponseHeader("Content-Type");
                            if (ct != null && (ct.getValue().startsWith("application/xml") || ct.getValue().startsWith("text/xml"))) {
                                if (wire.isDebugEnabled()) {
                                    wire.debug(method.getResponseBodyAsString());
                                }
                                InputStream input = method.getResponseBodyAsStream();
                                try {
                                    response.document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
                                } catch (SAXException e) {
                                    throw new CloudException("Invalid XML: " + e.getMessage());
                                } catch (IOException e) {
                                    throw new InternalException(e);
                                } catch (ParserConfigurationException e) {
                                    throw new InternalException(e);
                                } finally {
                                    input.close();
                                }
                                return response;
                            } else if (ct != null && ct.getValue().startsWith("application/octet-stream") && len < 1) {
                                return null;
                            } else {
                                response.contentLength = len;
                                if (ct != null) {
                                    response.contentType = ct.getValue();
                                }
                                response.input = method.getResponseBodyAsStream();
                                response.method = method;
                                leaveOpen = true;
                                return response;
                            }
                        } catch (IOException e) {
                            Eucalyptus.getLogger(Eucalyptus.class, "std").error(e.getMessage());
                            e.printStackTrace();
                            throw new CloudException(e);
                        }
                    } else {
                        return response;
                    }
                } else if (status == HttpStatus.SC_NO_CONTENT) {
                    return response;
                } else if (status == HttpStatus.SC_NOT_FOUND) {
                    throw new WalrusException(status, null, null, "Object not found.");
                } else {
                    try {
                        if (wire.isDebugEnabled()) {
                            wire.debug(method.getResponseBodyAsString());
                        }
                        InputStream input = method.getResponseBodyAsStream();
                        try {
                            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(input);
                            WalrusException e = parseError(status, doc);
                            logger.error("invoke(): Server error is " + e.getCode());
                            if (temporaryEndpoint != null && e.getEndpoint() != null) {
                                logger.error("invoke(): Redirect chain is too deep");
                                throw new WalrusException(status, e.getRequestId(), e.getCode(), "Too deep redirects (" + temporaryEndpoint + "/" + e.getEndpoint() + ")");
                            } else if (e.getEndpoint() != null) {
                                if (logger.isInfoEnabled()) {
                                    logger.info("invoke(): Redirect to " + e.getEndpoint());
                                }
                                return invoke(bucket, object, e.getEndpoint());
                            }
                            throw e;
                        } catch (SAXException e) {
                            throw new CloudException("Invalid XML: " + e.getMessage());
                        } catch (IOException e) {
                            throw new InternalException(e);
                        } catch (ParserConfigurationException e) {
                            throw new InternalException(e);
                        } finally {
                            input.close();
                        }
                    } catch (IOException e) {
                        if (status == HttpStatus.SC_FORBIDDEN) {
                            throw new WalrusException(status, "", "AccessForbidden", "Access was denied without explanation.");
                        }
                        throw new CloudException(e);
                    } catch (RuntimeException e) {
                        throw new CloudException(e);
                    } catch (Error e) {
                        throw new CloudException(e);
                    }
                }
            } finally {
                if (!leaveOpen) {
                    method.releaseConnection();
                }
            }
        } finally {
            if (wire.isDebugEnabled()) {
                wire.debug("");
                wire.debug("[" + (new Date()) + "] -------------------------------------------------------------- [" + temporaryEndpoint + "]");
            }
            if (logger.isTraceEnabled()) {
                logger.trace("exit - " + WalrusMethod.class.getName() + ".invoke()");
            }
        }
    }

    private WalrusException parseError(int status, Document doc) {
        Logger logger = Eucalyptus.getLogger(WalrusMethod.class, "std");
        if (logger.isTraceEnabled()) {
            logger.trace("enter - " + WalrusMethod.class.getName() + ".parseError(" + doc + ")");
        }
        try {
            try {
                NodeList blocks = doc.getElementsByTagName("Error");
                String code = null, message = null, requestId = null, endpoint = null;
                if (blocks.getLength() > 0) {
                    Node error = blocks.item(0);
                    NodeList attrs;
                    attrs = error.getChildNodes();
                    for (int i = 0; i < attrs.getLength(); i++) {
                        Node attr = attrs.item(i);
                        if (attr.getNodeName().equals("Code")) {
                            code = attr.getFirstChild().getNodeValue().trim();
                        } else if (attr.getNodeName().equals("Message")) {
                            message = attr.getFirstChild().getNodeValue().trim();
                        } else if (attr.getNodeName().equals("Endpoint")) {
                            endpoint = attr.getFirstChild().getNodeValue().trim();
                        }
                    }
                } else {
                    blocks = doc.getElementsByTagName("Exception");
                    if (blocks.getLength() > 0) {
                        Node error = blocks.item(0);
                        message = error.getFirstChild().getNodeValue();
                        if (message != null) {
                            message = message.trim();
                            String[] parts = message.split(":");
                            if (parts.length > 1) {
                                code = parts[1].trim();
                            } else {
                                code = "InternalError";
                            }
                        }
                    }
                }
                blocks = doc.getElementsByTagName("RequestID");
                if (blocks.getLength() > 0) {
                    Node id = blocks.item(0);
                    requestId = id.getFirstChild().getNodeValue().trim();
                }
                if (message == null && code == null) {
                    logger.error("parseError(): Query errored without an understandable error condition: " + requestId + " (" + status + ")");
                    return new WalrusException(status, requestId, "unknown", "Unable to identify error condition: " + status + "/" + requestId + "/" + code);
                } else if (message == null) {
                    message = code;
                }
                if (code == null || !code.equals("TemporaryRedirect")) {
                    if (endpoint != null) {
                        endpoint = null;
                    }
                    logger.error("parseError(): Walrus error from " + requestId + " (" + status + "," + code + "): " + message);
                } else {
                    logger.info("parseError(): Redirect to " + endpoint);
                }
                return new WalrusException(status, requestId, code, message, endpoint);
            } catch (RuntimeException e) {
                return new WalrusException(status, "", "Local", e.getMessage(), null);
            } catch (Error e) {
                return new WalrusException(status, "", "Local", e.getMessage(), null);
            }
        } finally {
            if (logger.isTraceEnabled()) {
                logger.trace("exit - " + WalrusMethod.class.getName() + ".parseError()");
            }
        }
    }
}
