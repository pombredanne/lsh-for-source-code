package lt.bsprendimai.ddesk.servlets;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.X509Certificate;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lt.bsprendimai.ddesk.UserHandler;
import lt.bsprendimai.ddesk.dao.CertificateEntry;
import lt.bsprendimai.ddesk.dao.SessionHolder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.util.encoders.Hex;
import org.hibernate.Query;

/**
 * Only for JSF
 *
 * Automated login based on client certificate.
 *
 * @author Aleksandr Panzin (JAlexoid) alex@activelogic.eu
 * @version
 */
public class SSLAutoAuthFilter implements Filter {

    private FilterConfig filterConfig = null;

    public SSLAutoAuthFilter() {
    }

    private boolean doBeforeProcessing(RequestWrapper request, ResponseWrapper response) throws IOException, ServletException {
        if (!request.isSecure()) return true;
        try {
            UserHandler uh = (UserHandler) request.getSession().getAttribute("userHandler");
            if (uh == null) {
                uh = new UserHandler();
                request.getSession().setAttribute("userHandler", uh);
            }
            if (!uh.isLoggedIn()) {
                Security.addProvider(new BouncyCastleProvider());
                InputStreamReader rd = new InputStreamReader(SSLAutoAuthFilter.class.getResourceAsStream("/desk.pem"));
                PEMReader reader = new PEMReader(rd);
                Object oo = reader.readObject();
                KeyPair kp = (KeyPair) oo;
                X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
                if (certs != null) {
                    for (X509Certificate cert : certs) {
                        try {
                            cert.checkValidity();
                            cert.verify(kp.getPublic());
                            byte[] pwdMD5 = Hex.encode(MessageDigest.getInstance("MD5").digest(cert.getEncoded()));
                            String code = new String(pwdMD5);
                            if (code.length() < 32) {
                                for (int i = (32 - code.length()); i > 0; i--) {
                                    code = "0" + code;
                                }
                            }
                            Query q = SessionHolder.currentSession().getSess().createQuery(" FROM " + CertificateEntry.class.getName() + "  WHERE md5Key = ? AND person IS NOT NULL AND valid = true ");
                            q.setString(0, code);
                            CertificateEntry ce = (CertificateEntry) q.uniqueResult();
                            uh.loginNoPw(ce.getPerson());
                        } catch (Exception ex) {
                        }
                    }
                }
                SessionHolder.closeSession();
                if (!uh.isLoggedIn()) {
                    return true;
                }
                if (request.getRequestURI().length() <= request.getContextPath().length() + 1 && uh.getUser().getCompany() == 0) {
                    response.sendRedirect(request.getContextPath() + "/intranet/");
                    SessionHolder.closeSession();
                    return false;
                } else {
                    SessionHolder.closeSession();
                    return true;
                }
            } else {
                SessionHolder.closeSession();
                return true;
            }
        } catch (Exception excasdasd) {
            SessionHolder.closeSession();
            return true;
        }
    }

    private void doAfterProcessing(RequestWrapper request, ResponseWrapper response) throws IOException, ServletException {
    }

    /**
     *
     * @param request
     *            The servlet request we are processing
     * @param result
     *            The servlet response we are creating
     * @param chain
     *            The filter chain we are processing
     *
     * @exception IOException
     *                if an input/output error occurs
     * @exception ServletException
     *                if a servlet error occurs
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        RequestWrapper wrappedRequest = new RequestWrapper((HttpServletRequest) request);
        ResponseWrapper wrappedResponse = new ResponseWrapper((HttpServletResponse) response);
        if (!doBeforeProcessing(wrappedRequest, wrappedResponse)) {
            return;
        }
        Throwable problem = null;
        try {
            chain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Throwable t) {
            problem = t;
            t.printStackTrace();
        }
        doAfterProcessing(wrappedRequest, wrappedResponse);
        if (problem != null) {
            SessionHolder.endSession();
            if (problem instanceof ServletException) throw (ServletException) problem;
            if (problem instanceof IOException) throw (IOException) problem;
            sendProcessingError(problem, response);
        }
    }

    /**
     * Return the filter configuration object for this filter.
     */
    public FilterConfig getFilterConfig() {
        return (this.filterConfig);
    }

    /**
     * Set the filter configuration object for this filter.
     *
     * @param filterConfig
     *            The filter configuration object
     */
    public void setFilterConfig(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    /**
     * Destroy method for this filter
     *
     */
    public void destroy() {
    }

    /**
     * Init method for this filter
     *
     */
    public void init(FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
        if (filterConfig != null) {
            if (debug) {
                log("SSLAutoAuthFilter: Initializing filter");
            }
        }
    }

    /**
     * Return a String representation of this object.
     */
    public String toString() {
        if (filterConfig == null) return ("SSLAutoAuthFilter()");
        StringBuffer sb = new StringBuffer("SSLAutoAuthFilter(");
        sb.append(filterConfig);
        sb.append(")");
        return (sb.toString());
    }

    private void sendProcessingError(Throwable t, ServletResponse response) {
        String stackTrace = getStackTrace(t);
        if (stackTrace != null && !stackTrace.equals("")) {
            try {
                response.setContentType("text/html");
                PrintStream ps = new PrintStream(response.getOutputStream());
                PrintWriter pw = new PrintWriter(ps);
                pw.print("<html>\n<head>\n<title>Error</title>\n</head>\n<body>\n");
                pw.print("<h1>The resource did not process correctly</h1>\n<pre>\n");
                pw.print(stackTrace);
                pw.print("</pre></body>\n</html>");
                pw.close();
                ps.close();
                response.getOutputStream().close();
                ;
            } catch (Exception ex) {
            }
        } else {
            try {
                PrintStream ps = new PrintStream(response.getOutputStream());
                t.printStackTrace(ps);
                ps.close();
                response.getOutputStream().close();
                ;
            } catch (Exception ex) {
            }
        }
    }

    public static String getStackTrace(Throwable t) {
        String stackTrace = null;
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sw.close();
            stackTrace = sw.getBuffer().toString();
        } catch (Exception ex) {
        }
        return stackTrace;
    }

    public void log(String msg) {
        filterConfig.getServletContext().log(msg);
    }

    private static final boolean debug = false;
}
