package org.openje.http.servlet;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.security.*;
import org.openje.misc.*;
import org.openje.http.server.*;
import org.openje.http.server.config.*;

public class ShowConfigServlet extends HttpServlet {

    protected String encryptedPassword;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        encryptedPassword = config.getInitParameter("password");
        if (encryptedPassword == null) throw new ServletException("Specity an encrypted password to web.xml file");
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        String pw = req.getParameter("pw");
        String cmd = req.getParameter("cmd");
        if ("encrypt".equals(cmd) && pw != null) {
            showEncryptedPassword(req, res, pw);
            writer.flush();
            return;
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        String pw = req.getParameter("pw");
        String cmd = req.getParameter("cmd");
        try {
            if ("login".equals(cmd) && pw != null && isPasswordCollect(encryptedPassword, pw)) {
                showConfig(req, res);
                return;
            }
        } catch (NoSuchAlgorithmException ex) {
            throw new ServletException(ex);
        }
        writer.println("<html>");
        writer.println("<!-- " + encryptedPassword + "-->");
        writer.println("<body>");
        writer.println("<b>Show Configuration(Experimental)</b>");
        writer.println("<p>Login:<br />");
        writer.println("<form>");
        writer.println("Password:<input type=password name=pw size=20>");
        writer.println("<input type=hidden name=cmd value=login>");
        writer.println("<input type=submit value=Login>");
        writer.println("</form></p>");
        writer.println("<p>Create an encrypted password.:<br />");
        writer.println("<form method=post>");
        writer.println("Password:<input type=password name=pw size=20>");
        writer.println("<input type=hidden name=cmd value=encrypt>");
        writer.println("<input type=submit value=Encrypt>");
        writer.println("</form></p>");
        writer.close();
    }

    protected static boolean isPasswordCollect(String encryptedPasswordWithSalt, String password) throws NoSuchAlgorithmException {
        int index = encryptedPasswordWithSalt.indexOf(':');
        String salt = encryptedPasswordWithSalt.substring(0, index);
        return encryptedPasswordWithSalt.equals(hashPassword(password, salt));
    }

    protected static String hashPassword(String password, String salt) throws NoSuchAlgorithmException {
        String s = salt + password;
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(s.getBytes());
        byte bs[] = md.digest();
        String s1 = BASE64Encoder.encode(bs);
        return new StringBuffer(salt).append(':').append(s1).toString();
    }

    protected void showConfig(HttpServletRequest req, HttpServletResponse res) throws IOException {
        PrintWriter writer = res.getWriter();
        writer.println("<html>");
        writer.println("<body>");
        Enumeration servletContextManagers = Jasper.getServletContextManagers();
        while (servletContextManagers.hasMoreElements()) {
            HServletContextManager servletContextManager = (HServletContextManager) servletContextManagers.nextElement();
            HAbstractServletContextManagerConfig scmc = servletContextManager.getServletContextManagerConfig();
            writer.println("<table border>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Max Threads</td><td bgcolor=white>" + scmc.getMaxThreads() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Start Threads</td><td bgcolor=white>" + scmc.getStartThreads() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Port</td><td bgcolor=white>" + scmc.getPort() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Servlet Context Manager</td><td bgcolor=white>");
            showServletContextConfig(servletContextManager, req, res);
            writer.println("</td></tr>");
            writer.println("</table>");
        }
        writer.println("</body>");
        writer.println("</html>");
    }

    protected void showServletContextConfig(HServletContextManager servletContextManager, HttpServletRequest req, HttpServletResponse res) throws IOException {
        PrintWriter writer = res.getWriter();
        writer.println("<table border><tr><td>");
        Enumeration servletContexts = servletContextManager.getServletContextManagerConfig().getServletContextConfigs();
        while (servletContexts.hasMoreElements()) {
            HAbstractServletContextConfig ascc = (HAbstractServletContextConfig) servletContexts.nextElement();
            writer.println("<table>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Initial Parameters</td><td bgcolor=white>");
            writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Name</td><td bgcolor=\"#99f097\">Value</td></tr>");
            Enumeration sip = ascc.getServerInitParameters().keys();
            while (sip.hasMoreElements()) {
                String key = (String) sip.nextElement();
                String value = (String) ascc.getServerInitParameters().get(key);
                writer.println("<tr><td bgcolor=\"#eaf2ae\">" + key + "</td><td bgcolor=white>" + value + "</td></tr>");
            }
            writer.println("</table>");
            writer.println("</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>CodeBase</td><td bgcolor=white>" + ascc.getCodeBase() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Display Name</td><td bgcolor=white>" + ascc.getDisplayName() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Description</td><td bgcolor=white>" + ascc.getDescription() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>DocumentRoot</td><td bgcolor=white>" + ascc.getDocumentRoot() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>ContextPath</td><td bgcolor=white>" + ascc.getContextPath() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Secure</td><td bgcolor=white>" + ascc.isSecure() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Authentication Manager</td><td bgcolor=white>" + ascc.getAuthManager() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Default Input Encoding</td><td bgcolor=white>" + ascc.getDefaultInputEncoding() + "</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Default Output Encoding</td><td bgcolor=white>" + ascc.getDefaultOutputEncoding() + "</td></tr>");
            HAbstractAccessLoggerConfig aalc = ascc.getAccessLogger();
            if (aalc == null) writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Access Logger</td><td bgcolor=white>null</td></tr>"); else {
                writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Access Logger Class</td><td bgcolor=white>" + aalc.getLoggerClassName() + "</td></tr>");
                writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Access Logger Initial Parameters</td><td bgcolor=white>");
                writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Name</td><td bgcolor=\"#99f097\">Value</td></tr>");
                Enumeration lip = aalc.getLoggerInitParameters().keys();
                while (lip.hasMoreElements()) {
                    String key = (String) lip.nextElement();
                    String value = (String) aalc.getLoggerInitParameters().get(key);
                    writer.println("<tr><td bgcolor=\"#eaf2ae\">" + key + "</td><td bgcolor=white>" + value + "</td></tr>");
                }
                writer.println("</table>");
                writer.println("</td></tr>");
            }
            Enumeration aslcs = ascc.getSystemLoggers().elements();
            while (aslcs.hasMoreElements()) {
                HAbstractSystemLoggerConfig aslc = (HAbstractSystemLoggerConfig) aslcs.nextElement();
                if (aslc == null) writer.println("<tr><td bgcolor=\"#babf8c\" width=200>System Logger</td><td bgcolor=white>null</td></tr>"); else {
                    writer.println("<tr><td bgcolor=\"#babf8c\" width=200>System Logger Class</td><td bgcolor=white>" + aslc.getLoggerClassName() + "</td></tr>");
                    writer.println("<tr><td bgcolor=\"#babf8c\" width=200>System Logger Initial Parameters</td><td bgcolor=white>");
                    writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Name</td><td bgcolor=\"#99f097\">Value</td></tr>");
                    Enumeration lip = aslc.getLoggerInitParameters().keys();
                    while (lip.hasMoreElements()) {
                        String key = (String) lip.nextElement();
                        String value = (String) aslc.getLoggerInitParameters().get(key);
                        writer.println("<tr><td bgcolor=\"#eaf2ae\">" + key + "</td><td bgcolor=white>" + value + "</td></tr>");
                    }
                    writer.println("</table>");
                    writer.println("</td></tr>");
                }
            }
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Mime Type</td><td bgcolor=white>");
            writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Extension</td><td bgcolor=\"#99f097\">Type</td></tr>");
            Enumeration mtm = ascc.getMimeTypeMap().keys();
            while (mtm.hasMoreElements()) {
                String key = (String) mtm.nextElement();
                String value = (String) ascc.getMimeTypeMap().get(key);
                writer.println("<tr><td bgcolor=\"#eaf2ae\">" + key + "</td><td bgcolor=white>" + value + "</td></tr>");
            }
            writer.println("</table>");
            writer.println("</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Mime Servlet</td><td bgcolor=white>");
            writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Extension</td><td bgcolor=\"#99f097\">Servlet</td></tr>");
            Enumeration sme = ascc.getServletMap().elements();
            while (sme.hasMoreElements()) {
                HServletMapElement smesme = (HServletMapElement) sme.nextElement();
                String key = smesme.getUrlPattern();
                String value = smesme.getServletName();
                writer.println("<tr><td bgcolor=\"#eaf2ae\" width=100>" + key + "</td><td bgcolor=white>" + value + "</td></tr>");
            }
            writer.println("</table>");
            writer.println("</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Trusted Hosts</td><td bgcolor=white>");
            writer.println("<table border>");
            Enumeration th = ascc.getTrustedHosts().elements();
            while (th.hasMoreElements()) {
                writer.println("<tr><td>" + th.nextElement() + "</td></tr>");
            }
            writer.println("</table>");
            writer.println("</td></tr>");
            writer.println("<tr><td bgcolor=\"#babf8c\" width=200>Servlet Configurations</td><td bgcolor=white>");
            Enumeration sc = ascc.getServletConfigs().keys();
            while (sc.hasMoreElements()) {
                String key = (String) sc.nextElement();
                HAbstractServletConfig value = (HAbstractServletConfig) ascc.getServletConfigs().get(key);
                writer.println("<table width=\"100%\" bgcolor=\"#bacbcd\">");
                writer.println("<tr><td bgcolor=\"#c2b6de\" width=100>Name</td><td bgcolor=\"white\">" + key + "</td></tr>");
                writer.println("<tr><td bgcolor=\"#c2b6de\" width=100>Code</td><td bgcolor=\"white\">" + value.getCode() + "</td></tr>");
                writer.println("<tr><td bgcolor=\"#c2b6de\" width=100>CodeBase</td><td bgcolor=\"white\">" + value.getCodeBase() + "</td></tr>");
                writer.println("<tr><td bgcolor=\"#c2b6de\" width=100>Initial Parameters</td><td bgcolor=\"white\">");
                writer.println("<table bgcolor=\"#bacbcd\" width=\"100%\"><tr><td bgcolor=\"#c2b6de\" width=100>Name</td><td bgcolor=\"#99f097\">Value</td></tr>");
                Enumeration ip = value.getInitParameters().keys();
                while (ip.hasMoreElements()) {
                    String _key = (String) ip.nextElement();
                    String _value = (String) value.getInitParameters().get(_key);
                    writer.println("<tr><td bgcolor=\"#eaf2ae\">" + _key + "</td><td bgcolor=white>" + _value + "</td></tr>");
                }
                writer.println("</table>");
                writer.println("</td></tr>");
                writer.println("</table>");
            }
            writer.println("</td></tr>");
            writer.println("</table>");
        }
        writer.println("</td></tr></table>");
    }

    protected static String saltChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    Random random = new Random();

    protected void showEncryptedPassword(HttpServletRequest req, HttpServletResponse res, String pw) throws IOException, ServletException {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 16; i++) sb.append(saltChars.charAt(Math.abs(random.nextInt()) % saltChars.length()));
        String salt = sb.toString();
        PrintWriter writer = res.getWriter();
        writer.println("<html>");
        writer.println("<body>");
        writer.println("Put this encrypted password to web.xml config file.<br />");
        try {
            writer.println(hashPassword(pw, salt));
        } catch (NoSuchAlgorithmException ex) {
            throw new ServletException(ex);
        }
        writer.println("</body>");
        writer.println("</html>");
    }
}
