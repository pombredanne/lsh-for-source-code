package com.bebo.platform.lib;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.bebo.platform.lib.api.BeboMethod.Configuration;

/**
 * Signs the map provided according to the SNML spec. Each key and value in the map
 * first sorted alphabetically by key, then they are iterated over in order, appending keys to 
 * values with an "=" in between. Then the API secret is appended to the end of that string. The whole
 * String is then md5'd and hex encoded.
 * This is the signature. 
 * The signed map contains the signature under the key fb_sig. All the parameters that were used to generate the sig have
 * fb_sig prepended to them. 
 * 
 * @param unsigned
 * @return
 */
public class SignatureUtil {

    private static final String UTF_8 = "UTF-8";

    public static boolean isLoggedIn(HttpServletRequest request, String secret) {
        return isValid(request, secret) && !isEmpty(request.getParameter("fb_sig")) && !isEmpty(request.getParameter("fb_sig_user"));
    }

    public static boolean isAppUser(HttpServletRequest request) {
        return "1".equals(request.getParameter("fb_sig_added"));
    }

    public static void requireLogin(HttpServletRequest request, HttpServletResponse response, Configuration config, String next) throws IOException {
        if (!isLoggedIn(request, config.getApiSecret())) {
            response.getWriter().write("<sn:redirect url=\"http://www.bebo.com/SignIn.jsp?" + "ApiKey=" + config.getApiKey() + (!isEmpty(next) ? "&next=" + URLEncoder.encode(next, UTF_8) : "") + "&v=1.0" + "&canvas" + "\"/>");
        }
    }

    public static void requireAdd(HttpServletRequest request, HttpServletResponse response, Configuration config, String next) throws IOException {
        if (isLoggedIn(request, config.getApiSecret()) && !isAppUser(request)) {
            response.getWriter().write("<sn:redirect url=\"http://www.bebo.com/c/apps/add?" + "ApiKey=" + config.getApiKey() + (!isEmpty(next) ? "&next=" + URLEncoder.encode(next, UTF_8) : "") + "\"/>");
        }
    }

    public static Map<String, String> sign(Map<String, String> unsigned, String secret) {
        SortedMap<String, String> m = (unsigned instanceof SortedMap) ? (SortedMap<String, String>) unsigned : new TreeMap<String, String>(unsigned);
        SortedMap<String, String> signed = new TreeMap<String, String>();
        if (unsigned.containsKey("fb_sig")) {
            return m;
        }
        System.out.println(unsigned);
        StringBuilder sigParams = new StringBuilder();
        for (Map.Entry<String, String> entry : m.entrySet()) {
            sigParams.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sigParams.append(secret);
        signed.put("sig", md5(sigParams.toString()));
        signed.putAll(unsigned);
        return signed;
    }

    /**
	 * Applys the MD5 algorithm and then hex encodes the result
	 * so we can display the string.
	 * @param source
	 * @return
	 */
    public static String md5(String source) {
        MessageDigest md;
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        try {
            md = MessageDigest.getInstance("MD5");
            md.update(source.getBytes());
            byte[] digested = md.digest();
            for (int i = 0; i < digested.length; i++) {
                pw.printf("%02x", digested[i]);
            }
            pw.flush();
            return sw.getBuffer().toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
	 * Determines whether or not the request coming from Bebo is valid
	 * If the request is not signed with the fb_sig parameter, this method returns 
	 * true.
	 * @param request
	 * @param secret
	 * @return
	 */
    public static boolean isValid(HttpServletRequest request, String secret) {
        if (!request.getParameterMap().containsKey("fb_sig")) {
            return true;
        }
        TreeMap<String, String> sorted = new TreeMap<String, String>();
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.startsWith("fb_sig_")) {
                sorted.put(name.substring(7), request.getParameter(name));
            }
        }
        StringBuilder sig = new StringBuilder();
        for (Map.Entry<String, String> entry : sorted.entrySet()) {
            sig.append(entry.getKey()).append("=").append(entry.getValue());
        }
        sig.append(secret);
        System.out.println(sig.toString());
        String generatedSig = md5(sig.toString());
        return generatedSig.equals(request.getParameter("fb_sig"));
    }

    /**
	 * Turns the map into a query string without the leading ?
	 * Each variable name and value is url encoded.
	 * @param params
	 * @return
	 */
    public static String mapToQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            query.append(URLEncoder.encode(param.getKey()));
            query.append("=");
            query.append(URLEncoder.encode(param.getValue()));
            query.append("&");
        }
        query.deleteCharAt(query.length() - 1);
        return query.toString();
    }

    public static String mapToDebugString(Map<String, Object> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (entry.getValue() instanceof String[]) {
                String[] values = (String[]) entry.getValue();
                for (int i = 0; i < values.length; i++) {
                    sb.append(URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode(values[i]) + "<br>");
                }
            } else {
                sb.append(URLEncoder.encode(entry.getKey()) + "=" + URLEncoder.encode((String) entry.getValue()) + "<br>");
            }
        }
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().length() == 0;
    }
}
