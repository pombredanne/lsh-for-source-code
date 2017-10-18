package org.scotlandyard.utils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * utilities that can be used by servlets
 * @author Hussain Al-Mutawa
 * @version 1.0
 * @since Sun Sep 18, 2011
 */
public class ServletUtils {

    /**
	 * NOT UNIT TESTED Returns the URL (including query parameters) minus the scheme, host, and
	 * context path.  This method probably be moved to a more general purpose
	 * class.
	 */
    public static String getRelativeUrl(HttpServletRequest request) {
        String formating = "://";
        String baseUrl = null;
        if ((request.getServerPort() == 80) || (request.getServerPort() == 443)) baseUrl = request.getScheme() + formating + request.getServerName() + request.getContextPath(); else baseUrl = request.getScheme() + formating + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
        StringBuffer buf = request.getRequestURL();
        if (request.getQueryString() != null) {
            buf.append("?");
            buf.append(request.getQueryString());
        }
        return buf.substring(baseUrl.length());
    }

    /**
	 * NOT UNIT TESTED Returns the base url (e.g, <tt>http://myhost:8080/myapp</tt>) suitable for
	 * using in a base tag or building reliable urls.
	 */
    public static String getBaseUrl(HttpServletRequest request) {
        String formating = "://";
        if ((request.getServerPort() == 80) || (request.getServerPort() == 443)) return request.getScheme() + formating + request.getServerName() + request.getContextPath(); else return request.getScheme() + formating + request.getServerName() + ":" + request.getServerPort() + request.getContextPath();
    }

    public static void showErrorPage(HttpServlet servlet, HttpServletRequest request, HttpServletResponse response, Throwable throwable) {
        request.setAttribute("javax.servlet.jsp.jspException", throwable);
        try {
            servlet.getServletContext().getRequestDispatcher("/pages/errorPage.jsp").forward(request, response);
        } catch (ServletException ex) {
            Logger.getLogger(ServletUtils.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ServletUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
	 * returns the current username from the cookies
	 * @param request
	 * @return 
	 */
    public static String getUserName(HttpServletRequest request) {
        return getCookie(request, "email");
    }

    public static String getCookie(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null || request.getCookies().length == 0) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie.getName().equals(cookieName)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    /**
	 * sets the current username
	 * @param response 
	 * @param username
	 */
    public static void setCookie(HttpServletResponse response, String cookieName, String value, int maxAge) {
        Cookie cookie = new Cookie(cookieName, value);
        cookie.setMaxAge(maxAge);
        response.addCookie(cookie);
    }

    /**
	 * hash function
	 * @param word
	 * @return 
	 */
    public static String md5(String word) {
        MessageDigest alg = null;
        try {
            alg = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ServletUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        alg.reset();
        alg.update(word.getBytes());
        byte[] digest = alg.digest();
        StringBuilder hashedWord = new StringBuilder();
        String hx;
        for (int i = 0; i < digest.length; i++) {
            hx = Integer.toHexString(0xFF & digest[i]);
            if (hx.length() == 1) {
                hx = "0" + hx;
            }
            hashedWord.append(hx);
        }
        return hashedWord.toString();
    }

    /**
	 * Returns the file specified by <tt>path</tt> as returned by
	 * <tt>ServletContext.getRealPath()</tt>.
	 */
    public static File getRealFile(HttpServletRequest request, String path) {
        return new File(request.getSession().getServletContext().getRealPath(path));
    }
}
