package com.mrroman.linksender.filesender.server;

import com.mrroman.linksender.filesender.authenticator.HttpAuthenticator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;

/**
 *
 * @author gorladam
 */
public class HttpTools {

    public static final String BR = "\r\n";

    public static final String CONNECTION = "Connection";

    public static final String CONTENTLENGTH = "Content-Length";

    public static final String CONTENTTYPE = "Content-Type";

    public static final String CONTENTDISPOSITION = "Content-Disposition";

    public static final String SERVER = "Server";

    public static final String LOCATION = "Location";

    public static final String RETRYAFTER = "Retry-After";

    public static final String WWWAUTHENTICATE = "WWW-Authenticate";

    public static final String ACCEPTRANGES = "Accept-Ranges";

    public static final String CONTENTRANGE = "Content-Range";

    public static final String AUTHORIZATION = "Authorization";

    public static final String RANGE = "Range";

    public static final String COLON = ":";

    public static final String COLONSPACE = ": ";

    public static final String REFERER = "Referer";

    public static final String HOST = "Host";

    public static final String ICYMETADATA = "Icy-MetaData";

    public static final String XHTMLHEADER = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">" + BR + "<html xmlns=\"http://www.w3.org/1999/xhtml\">";

    public static final String CONTENTTYPE_TEXT_HTML = "text/html";

    public static final HttpAuthenticator NULLAUTHENTICATOR = new HttpAuthenticator() {

        @Override
        public void addUser(String user, String password) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String allowed(String authorization) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String getAuthenticate() {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    };

    public static String createHttpResponse(int code, String server, long contentLength, String contentType, String body) {
        StringBuilder result = new StringBuilder(createHttpResponse(code, server, contentLength, contentType));
        if (body != null) {
            result.append(BR + body);
        }
        return result.toString();
    }

    public static String createHttpResponse(int code, String server, long rangeStart, long rangeStop, long contentLength, String contentType) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.1");
        result.append(" " + code + " ");
        String message = getHttpCodes().get(code);
        if (message == null) {
            message = "No information";
        }
        result.append(message + BR);
        result.append(CONNECTION + COLONSPACE + "close" + BR);
        if (server != null) {
            result.append(SERVER + COLONSPACE + server + BR);
        }
        if (contentType != null) {
            result.append(CONTENTTYPE + COLONSPACE + contentType + BR);
        }
        if (rangeStart < 0) {
            rangeStart = 0;
        }
        if (rangeStop < 0 || rangeStop > contentLength - 1) {
            rangeStop = contentLength - 1;
        }
        result.append(CONTENTRANGE + COLONSPACE + "bytes " + (rangeStart < 0 ? "" : rangeStart) + "-" + (rangeStop < 0 ? "" : rangeStop) + "/" + contentLength + BR);
        result.append(CONTENTLENGTH + COLONSPACE + (rangeStop - rangeStart + 1) + BR);
        return result.toString();
    }

    public static String createHttpResponse(int code, String server, long contentLength, String contentType) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.0");
        result.append(" " + code + " ");
        String message = getHttpCodes().get(code);
        if (message == null) {
            message = "No information";
        }
        result.append(message + BR);
        result.append(CONNECTION + COLONSPACE + "close" + BR);
        if (server != null) {
            result.append(SERVER + COLONSPACE + server + BR);
        }
        if (contentLength > -1) {
            result.append(CONTENTLENGTH + COLONSPACE + contentLength + BR);
        }
        if (contentType != null) {
            result.append(CONTENTTYPE + COLONSPACE + contentType + BR);
        }
        result.append(ACCEPTRANGES + COLONSPACE + "bytes" + BR);
        return result.toString();
    }

    public static String createHttpResponse(int code, String[][] headerValues) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.0");
        result.append(" " + code + " ");
        String message = getHttpCodes().get(code);
        if (message == null) {
            message = "No information";
        }
        result.append(message + BR);
        if (headerValues != null) {
            for (String[] value : headerValues) {
                result.append(value[0] + COLONSPACE + value[1] + BR);
            }
        }
        return result.toString();
    }

    public static String createHttpResponse(int code, String... headerValues) {
        StringBuilder result = new StringBuilder();
        result.append("HTTP/1.0");
        result.append(" " + code + " ");
        String message = getHttpCodes().get(code);
        if (message == null) {
            message = "No information";
        }
        result.append(message + BR);
        if (headerValues != null) {
            boolean start = true;
            for (String value : headerValues) {
                if (start) {
                    result.append(value + COLONSPACE);
                } else {
                    result.append(value + BR);
                }
                start = !start;
            }
        }
        return result.toString();
    }

    public static String createHttpResponse(int code, String server, boolean withBody) {
        if (withBody) {
            return createHttpResponse(code, server, -1, CONTENTTYPE_TEXT_HTML, "<h1>" + code + " " + getHttpCodes().get(code) + " </h1><hr><i>" + server + "</i>");
        } else {
            return createHttpResponse(code, server, -1, null, "<h1>" + code + " " + getHttpCodes().get(code) + " </h1><hr><i>" + server + "</i>");
        }
    }

    public static String createIcecastResponse(String server, int chunkSize) {
        StringBuilder result = new StringBuilder();
        String[] greetingICY = new String[] { "ICY 200 OK", "Cache-Control: no-cache", "icy-notice1: <BR>This stream requires <a href=\"http://www.icecast.org/3rdparty.php\">a media player that support Icecast</a><BR>", "icy-notice2: " + server + "<BR>", "icy-name: " + server, "icy-genre: various", "icy-metaint: " + chunkSize, "icy-pub: 1" };
        for (String s : greetingICY) {
            result.append(s + BR);
        }
        return result.toString();
    }

    public static String convertToMultiline(String... text) {
        StringBuilder builder = new StringBuilder();
        for (String s : text) {
            builder.append(s + BR);
        }
        return builder.toString();
    }

    private static String defaultCSS = null;

    private static HashMap<Integer, String> httpCodes = null;

    public static HashMap<Integer, String> getHttpCodes() {
        if (httpCodes == null) {
            httpCodes = new HashMap<Integer, String>();
            httpCodes.put(200, "OK");
            httpCodes.put(403, "Forbidden");
            httpCodes.put(400, "Bad Request");
            httpCodes.put(401, "Authorization Required");
            httpCodes.put(301, "Moved Permanently");
            httpCodes.put(503, "Server too busy");
            httpCodes.put(404, "Not found");
            httpCodes.put(408, "Request Timeout");
            httpCodes.put(206, "Partial content");
            httpCodes.put(416, "Requested range not satisfiable");
        }
        return httpCodes;
    }

    public static String base64encode(String string) {
        byte[] stringArray;
        try {
            stringArray = string.getBytes("UTF-8");
        } catch (Exception ignored) {
            stringArray = string.getBytes();
        }
        return new sun.misc.BASE64Encoder().encode(stringArray);
    }

    public static String md5sum(String string) {
        try {
            byte[] stringArray;
            try {
                stringArray = string.getBytes("UTF-8");
            } catch (Exception ignored) {
                stringArray = string.getBytes();
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] raw = md.digest(stringArray);
            StringBuffer result = new StringBuffer(32);
            for (byte b : raw) {
                int i = (b & 0xFF);
                if (i < 16) {
                    result.append("0");
                }
                result.append(Integer.toHexString(i));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    public static String randomNonce() {
        StringBuffer result = new StringBuffer(32);
        char[] availableChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        Random r = new Random();
        for (int i = 0; i < 32; i++) {
            result.append(availableChars[r.nextInt(16)]);
        }
        return result.toString();
    }

    public static String getDefaultCSS() {
        if (defaultCSS == null) {
            String[] css = { "<style type=\"text/css\">", "/* <![CDATA[ */", "body {", "font-family: \"DejaVu Sans\", \"Lucida Sans\", Verdana, Tahoma, Arial, sans-serif;", "outline: 0px;", "color: #000;", "font-size: 12px;", "}", "/*************/", "a.dir {", "color: #000;", "font-weight: bold;", "}", "a.file {", "color: #000;", "text-decoration: bold;", "}", "/*************/", "a:link {", "color: #000;", "text-decoration: none;", "}", "a:visited {", "color: #000;", "text-decoration: none;", "}", "a:hover, a:active {", "text-decoration: none;", "color: #000;", "}", "/*************/", "ul.ls {", "font-family: \"DejaVu Sans Mono\", \"Bitstream Vera Sans Mono\", Monaco, \"Liberation Mono\", \"Lucida Console\", monospace;", "}", "ul.ls, ul.ls li {", "display: block;", "list-style: none;", "margin: 0;", "padding: 0;", "}", "ul.ls a:link, ul.ls a:visited {", "display: block;", "text-decoration: none;", "padding: 1px;", "}", "ul.ls a:hover {", "display: block;", "padding: 0;", "background-color: #eee;", "border: 1px solid #bbb;", "}", "/*************/", "ul.ls li a.file span.size {", "color: #666 !important;", "font-size: 10px;", "}", "/*************/", "#CONTENT {", "margin-left: 150px;", "margin-right: 150px;", "background-color: #fff;", "}", "#HEADER, #HEADER a {", "background-color: #888;", "color: #FFF;", "padding: 5px;", "}", "#HEADER a, #FOTTER a {", "color: #FFF;", "padding: 0px;", "}", "#HEADER a:hover, #FOTTER a:hover {", "font-weight: bold;", "}", "#MENU {", "width: 140px;", "float: left;", "overflow: hidden;", "position: relative;", "background-color: #fff;", "border: solid;", "border-width: 1px;", "}", "#MENU ul, #MENU ul li {", "display: block;", "list-style: none;", "margin: 0;", "padding: 0;", "}", "#MENU ul a:link, #MENU ul a:visited {", "display: block;", "text-decoration: none;", "padding: 5px;", "}", "#MENU ul a:hover {", "border: 1px solid #bbb;", "padding: 4px;", "background-color: #eee;", "}", "#INFORMATIONS {", "width: 150px;", "float: right;", "overflow: hidden;", "position: relative;", "background-color: #ccc;", "}", "#FOTTER {", "background-color: #888;", "color: #FFF;", "padding: 5px;", "font-size: 10px;", "}", "/* ]]> */", "</style>" };
            defaultCSS = convertToMultiline(css);
        }
        return defaultCSS;
    }
}
