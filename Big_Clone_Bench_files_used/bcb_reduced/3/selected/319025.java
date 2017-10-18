package org.allcolor.ywt.filter;

import org.allcolor.xml.parser.CShaniDomParser;
import org.allcolor.alc.filesystem.Handler;
import org.allcolor.ywt.permission.Permission;
import org.allcolor.ywt.security.SubjectManager;
import org.allcolor.ywt.utils.LOGGERHelper;
import org.allcolor.ywt.utils.crypto.base64.CBASE64Codec;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * DOCUMENT ME!
 *
 * @author Quentin Anciaux
 * @version 0.1.0
 */
public class CResourceFilter implements Filter {

    /** DOCUMENT ME! */
    private static CResourceFilter handle = null;

    /** DOCUMENT ME! */
    private static final Logger LOG = LOGGERHelper.getLogger(CResourceFilter.class);

    /** DOCUMENT ME! */
    private final Map<Pattern, Boolean> authorizedDirectoriesListing = new HashMap<Pattern, Boolean>();

    /** DOCUMENT ME! */
    private volatile boolean isDefaultListing = true;

    /**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static CResourceFilter getInstance() {
        return CResourceFilter.handle;
    }

    /**
	 * DOCUMENT ME!
	 */
    public void destroy() {
        CResourceFilter.handle = null;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param arg0 DOCUMENT ME!
	 * @param arg1 DOCUMENT ME!
	 * @param chain DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 * @throws ServletException DOCUMENT ME!
	 */
    public void doFilter(final ServletRequest arg0, final ServletResponse arg1, final FilterChain chain) throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) arg0;
        final HttpServletResponse response = (HttpServletResponse) arg1;
        try {
            this.sendResource(request, response);
        } catch (final Exception e) {
            response.reset();
            chain.doFilter(request, response);
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param config DOCUMENT ME!
	 *
	 * @throws ServletException DOCUMENT ME!
	 */
    public void init(final FilterConfig config) throws ServletException {
        CResourceFilter.handle = this;
        this.loadConfigFile();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param request DOCUMENT ME!
	 * @param response DOCUMENT ME!
	 *
	 * @throws IOException DOCUMENT ME!
	 * @throws ServletException DOCUMENT ME!
	 */
    @SuppressWarnings(value = "unchecked")
    public void sendResource(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {
        final String uri = CResourceFilter.getURI(request);
        final URL resource = CContext.getInstance().getContext().getResource(uri);
        final URLConnection conn = resource.openConnection();
        response.setDateHeader("Last-Modified", conn.getLastModified());
        final String etag = CResourceFilter.getETag(uri, conn.getLastModified());
        response.setHeader("ETag", etag);
        if (etag.equals(request.getHeader("If-None-Match"))) {
            CResourceFilter.LOG.debug("Not Modified HTTP(304): " + uri);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            try {
                conn.getInputStream().close();
            } catch (Exception ignore) {
            }
            return;
        }
        if (request.getDateHeader("If-Modified-Since") == conn.getLastModified()) {
            CResourceFilter.LOG.debug("Not Modified HTTP(304): " + uri);
            response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            try {
                conn.getInputStream().close();
            } catch (Exception ignore) {
            }
            return;
        }
        final String mimetype = conn.getContentType();
        CResourceFilter.LOG.debug(uri + " - " + mimetype);
        if (mimetype != null) {
            if (mimetype.equals("application/x-directory")) {
                try {
                    conn.getInputStream().close();
                } catch (final Exception ignore) {
                    ;
                }
                if (!this.canList(uri)) {
                    CResourceFilter.LOG.debug("Forbidden listing HTTP(403): " + uri);
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
                final SortedMap<String, List<String>> dirContent = new TreeMap<String, List<String>>();
                request.setAttribute("directory.name", uri);
                request.setAttribute("directory.list", dirContent);
                final String path = Handler.decodeURL(resource.getFile());
                final Set set = CContext.getInstance().getContext().getResourcePaths(path);
                String thePath = path;
                if (!thePath.endsWith("/")) {
                    thePath = thePath + "/";
                }
                if (thePath.startsWith("/")) {
                    thePath = thePath.substring(1);
                }
                if (thePath.length() > 0) {
                    String mypath = thePath;
                    if (mypath.endsWith("/")) {
                        mypath = mypath.substring(0, mypath.length() - 1);
                    }
                    if (mypath.indexOf("/") != -1) {
                        dirContent.put("..", Arrays.asList(new String[] { mypath.substring(0, mypath.lastIndexOf("/") + 1), "parent directory", "", "" }));
                    } else {
                        dirContent.put("..", Arrays.asList(new String[] { "", "parent directory", "", "" }));
                    }
                }
                if (set != null) {
                    for (String file : (Set<String>) set) {
                        if (file.startsWith("/")) {
                            file = file.substring(1);
                        }
                        String contentType = "unknown";
                        String contentLength = "0";
                        String date = "unknown";
                        if (file.endsWith("/")) {
                            try {
                                final URL res = CContext.getInstance().getContext().getResource(file);
                                if (res != null) {
                                    final URLConnection rconn = res.openConnection();
                                    contentLength = rconn.getContentLength() + "";
                                    date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(rconn.getLastModified()));
                                    try {
                                        rconn.getInputStream().close();
                                    } catch (final Exception ignore) {
                                        ;
                                    }
                                }
                            } catch (final Exception ignore) {
                                ;
                            }
                            file = file.substring(0, file.length() - 1);
                            file = file.substring(file.lastIndexOf("/") + 1) + "/";
                        } else if (file.trim().length() > 0) {
                            try {
                                final URL res = CContext.getInstance().getContext().getResource(file);
                                if (res != null) {
                                    final URLConnection rconn = res.openConnection();
                                    contentLength = rconn.getContentLength() + "";
                                    contentType = rconn.getContentType();
                                    date = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date(rconn.getLastModified()));
                                    try {
                                        rconn.getInputStream().close();
                                    } catch (final Exception ignore) {
                                        ;
                                    }
                                }
                            } catch (final Exception ignore) {
                                ;
                            }
                            file = file.substring(file.lastIndexOf("/") + 1);
                        }
                        if (!file.endsWith("/")) {
                            try {
                                final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
                                nf.setMaximumFractionDigits(1);
                                nf.setGroupingUsed(false);
                                double value = Double.parseDouble(contentLength) / 1024d / 1024d;
                                if (value < 0.5d) {
                                    value = Double.parseDouble(contentLength) / 1024d;
                                    if (value < 1.0d) {
                                        value = Double.parseDouble(contentLength);
                                        dirContent.put(file, Arrays.asList(new String[] { thePath + file, contentType, nf.format(value) + " B.", date }));
                                    } else {
                                        dirContent.put(file, Arrays.asList(new String[] { thePath + file, contentType, nf.format(value) + " KB.", date }));
                                    }
                                } else {
                                    dirContent.put(file, Arrays.asList(new String[] { thePath + file, contentType, nf.format(value) + " MB.", date }));
                                }
                            } catch (final Exception ignore) {
                                dirContent.put(file, Arrays.asList(new String[] { thePath + file, contentType, "unknown.", date }));
                            }
                        } else {
                            dirContent.put(file, Arrays.asList(new String[] { thePath + file, "directory", "", date }));
                        }
                    }
                }
                response.setContentType("text/html");
                response.setCharacterEncoding("utf-8");
                request.getRequestDispatcher("/WEB-INF/view/default/resources/lister.view").forward(request, response);
                return;
            } else {
                response.setContentType(mimetype);
                CResourceFilter.LOG.debug("Sending: " + uri + " - mimetype: " + mimetype);
            }
        } else {
            response.setContentType("application/octet-stream");
            CResourceFilter.LOG.debug("Sending: " + uri);
        }
        response.setIntHeader("Content-Length", conn.getContentLength());
        final InputStream in = conn.getInputStream();
        final byte buffer[] = new byte[2048];
        int inb = -1;
        final OutputStream out = response.getOutputStream();
        while ((inb = in.read(buffer)) != -1) {
            out.write(buffer, 0, inb);
        }
        out.flush();
        in.close();
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param uri DOCUMENT ME!
	 * @param lastModified DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static String getETag(final String uri, final long lastModified) {
        try {
            final MessageDigest dg = MessageDigest.getInstance("MD5");
            dg.update(uri.getBytes("utf-8"));
            dg.update(new byte[] { (byte) ((lastModified >> 24) & 0xFF), (byte) ((lastModified >> 16) & 0xFF), (byte) ((lastModified >> 8) & 0xFF), (byte) (lastModified & 0xFF) });
            return CBASE64Codec.encode(dg.digest());
        } catch (final Exception ignore) {
            return uri + lastModified;
        }
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param request DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    public static String getURI(final HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        String uri = request.getRequestURI();
        if (uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        return uri;
    }

    /**
	 * DOCUMENT ME!
	 *
	 * @param uri DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
    private boolean canList(final String uri) {
        if (SubjectManager.getSubjectManager() != null && SubjectManager.getSubjectManager().checkPermission(new Permission("directory.listing", uri))) {
            return true;
        }
        boolean ok = false;
        synchronized (this.authorizedDirectoriesListing) {
            for (final Map.Entry<Pattern, Boolean> auth : this.authorizedDirectoriesListing.entrySet()) {
                if (auth.getKey().matcher(uri).matches()) {
                    if (!auth.getValue()) {
                        return false;
                    } else {
                        ok = true;
                    }
                }
            }
        }
        return (ok || this.isDefaultListing);
    }

    /**
	 * DOCUMENT ME!
	 */
    private void loadConfigFile() {
        try {
            final CShaniDomParser parser = new CShaniDomParser();
            final Document doc = parser.parse(CContext.getInstance().getContext().getResource("/WEB-INF/config/directory.listing.config.xml"));
            final Element directories = doc.getDocumentElement();
            this.isDefaultListing = "true".equalsIgnoreCase(directories.getAttribute("default-listing"));
            final org.w3c.dom.NodeList nl = doc.getElementsByTagNameNS("http://www.allcolor.org/xmlns/directory-listing", "directory");
            for (int i = 0; i < nl.getLength(); i++) {
                final Element directory = (Element) nl.item(i);
                final String pattern = directory.getAttribute("pattern");
                final boolean allow = "true".equalsIgnoreCase(directory.getAttribute("allow"));
                synchronized (this.authorizedDirectoriesListing) {
                    this.authorizedDirectoriesListing.put(Pattern.compile(pattern), allow);
                }
            }
        } catch (final Exception ignore) {
            CResourceFilter.LOG.error("Error while parsing config file..", ignore);
        }
    }
}
