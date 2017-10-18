package com.google.code.facebookwebapp.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import com.google.code.facebookwebapp.util.FacebookConstants;
import com.google.code.facebookwebapp.util.FacebookProperty;

/**
 * This filter should be used for handling facebook request specific data.
 *
 * More appropriate it should cast the basic request into some type of FacebookRequest
 * that can access facebook specific request data more easily.
 *
 * @author Cesar Arevalo
 * @since 0.1
 */
public class FacebookFilter implements Filter {

    public void init(FilterConfig arg0) throws ServletException {
    }

    public void destroy() {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest dRequest = ((HttpServletRequest) request);
        if (dRequest.getSession(true).getAttribute("fb_cookie") == null) {
            try {
                dRequest.getSession().setAttribute("fb_cookie", getFacebookCookie(dRequest));
            } catch (NoSuchAlgorithmException e) {
                dRequest.getSession().setAttribute("fb_cookie", null);
            }
        }
        filterChain.doFilter(request, response);
    }

    public Map<String, String> getFacebookCookie(HttpServletRequest request) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String appId = FacebookProperty.getString(FacebookConstants.PROPERTY_API_KEY);
        String applicationSecret = FacebookProperty.getString(FacebookConstants.PROPERTY_API_SECRET);
        if (appId == null || applicationSecret == null || request == null) {
            throw new UnsupportedOperationException("appId == null || applicationSecret == null || request == null");
        }
        Cookie cookies[] = request.getCookies();
        if (cookies == null) {
            return null;
        }
        String fbs = null;
        for (Cookie cookie : cookies) {
            if (("fbs_" + appId).equals(cookie.getName())) {
                fbs = URLDecoder.decode(cookie.getValue(), "UTF-8");
                ;
            }
        }
        if (fbs == null) {
            return null;
        }
        String[] args = fbs.split("&");
        Arrays.sort(args);
        String payload = "";
        String sig_arg = null;
        for (String argument : args) {
            if (!argument.startsWith("sig")) {
                payload += argument;
            } else {
                sig_arg = argument;
            }
        }
        sig_arg = sig_arg.split("=")[1];
        if (!sig_arg.equals(md5(payload + applicationSecret))) {
            return null;
        }
        Map<String, String> facebookCookie = new HashMap<String, String>();
        for (String argument : args) {
            String key = argument.split("=")[0];
            String value = argument.split("=")[1];
            facebookCookie.put(key, value);
        }
        facebookCookie.put("sig_arg", sig_arg);
        facebookCookie.put("md5_payload", md5(payload + applicationSecret));
        facebookCookie.put("payload", payload);
        return facebookCookie;
    }

    public String md5(String plainText) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(plainText.getBytes());
        byte[] digest = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < digest.length; i++) {
            plainText = Integer.toHexString(0xFF & digest[i]);
            if (plainText.length() < 2) {
                plainText = "0" + plainText;
            }
            hexString.append(plainText);
        }
        return hexString.toString();
    }
}
