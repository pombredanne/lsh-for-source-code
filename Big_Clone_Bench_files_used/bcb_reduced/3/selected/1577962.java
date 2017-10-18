package cn.shining365.webclips.trimmer;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;

/**
 * Servlet implementation class FoxtidyServlet
 */
public class TrimmerServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html; charset=UTF-8");
        String url = request.getRequestURI().replace(request.getContextPath() + request.getServletPath(), "");
        if ("".equals(url) || "/".equals(url)) {
            reportError(response, "Illegal request");
            return;
        }
        if (request.getQueryString() != null) {
            url += "?" + request.getQueryString();
        }
        url = url.substring(1);
        if (!url.toLowerCase().startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            reportError(response, "Malformed URL");
            return;
        }
        logger.debug("url: " + url);
        outputHtml(request, response, request.getContextPath() + request.getServletPath(), url);
    }

    protected void outputHtml(HttpServletRequest request, HttpServletResponse response, String servletUrl, String url) throws IOException {
        try {
            response.getWriter().print(new Trimmer(servletUrl, url).digest());
        } catch (Exception e) {
            reportError(response, "digest failed. Exception: " + e);
            return;
        }
    }

    protected void reportError(HttpServletResponse response, String msg) throws IOException {
        PrintWriter out = response.getWriter();
        out.print(msg);
    }

    private static final long serialVersionUID = 1L;

    private static Logger logger = Logger.getLogger(TrimmerServlet.class);
}
