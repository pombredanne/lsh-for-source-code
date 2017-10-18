package org.openje.http.server;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;
import java.security.*;

/**
 * This servlet services the authorization function. It uses the basic
 * authentication. (RFC2617)
 */
class HDigestAccessAuthManager extends HAuthManager {

    private static final boolean debug = Jasper.debug;

    private long privateKey;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        privateKey = (new Random()).nextLong();
    }

    /**
     * Sends back an authentication error page.
     */
    protected void sendAuthError(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        try {
            StringBuffer sb = new StringBuffer();
            sb.append("Digest");
            sb.append(" realm=\"");
            sb.append(request.getRequestURI());
            sb.append('\"');
            String nonce = H(request.getRemoteAddr() + ":" + Long.toString(System.currentTimeMillis(), 16) + ":" + Long.toString(privateKey));
            sb.append(",nonce=\"");
            sb.append(nonce);
            sb.append('\"');
            if (debug) System.out.println("HDigestAccessAuthManager: " + sb.toString());
            response.setHeader("WWW-Authenticate", sb.toString());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Authorization Required");
        } catch (NoSuchAlgorithmException ex) {
            response.sendError(HResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
        }
    }

    /**
     * Calculates the MD5. (RFC1321)
     *
     * @param data data to be callculated 
     * @return MD5 value
     */
    static byte[] md5(byte data[]) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data);
        return md.digest();
    }

    static String H(String data) throws NoSuchAlgorithmException {
        byte md[] = md5(data.getBytes());
        String s;
        StringBuffer sb = new StringBuffer();
        int len;
        for (int i = 0; i < md.length; i++) {
            s = Integer.toString((int) md[i] & 0xff, 16);
            len = s.length();
            if (len == 1) {
                sb.append("0");
                sb.append(s);
            } else if (len == 2) sb.append(s); else sb.append("00");
        }
        return sb.toString();
    }

    static String KD(String secret, String data) throws NoSuchAlgorithmException {
        StringBuffer sb = new StringBuffer();
        sb.append(secret);
        sb.append(':');
        sb.append(data);
        return H(sb.toString());
    }

    public String getServletInfo() {
        return "a Digest Access Authorization Servlet";
    }

    /**
     * Makes a string be unquoted.
     *
     * @param s a string to be unquoted.
     * @return unquoted string
     */
    public static String makeUnquoted(String s) {
        return (s.length() >= 2 && s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"') ? s.substring(1, s.length() - 1) : null;
    }

    /**
     * Authenticate the user.
     *
     * @param request the request from the client
     * @param response the response to the client
     * @param users the array of users to be authenticated
     * @return in case of success of authentication, return the user name,
     *         otherwise return null
     */
    protected Principal checkAuth(HttpServletRequest request, HttpServletResponse response, String[] users) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        StringTokenizer tk;
        String authUser = null;
        Hashtable table = parseHeader(authHeader.trim());
        String username, realm, nonce, digestUri, responseDigest, opaque;
        if (table != null && (username = (String) table.get("username")) != null && (realm = (String) table.get("realm")) != null && (nonce = (String) table.get("nonce")) != null && (digestUri = (String) table.get("uri")) != null && (responseDigest = (String) table.get("response")) != null && (username = makeUnquoted(username)) != null && (realm = makeUnquoted(realm)) != null && (nonce = makeUnquoted(nonce)) != null && (digestUri = makeUnquoted(digestUri)) != null && (responseDigest = makeUnquoted(responseDigest)) != null) {
            for (int i = 0; i < users.length; i++) {
                if (username.equals(users[i])) {
                    String passwd = (String) passwds.get(username);
                    String expectedResponse, A1, A2;
                    A1 = username + ":" + realm + ":" + passwd;
                    A2 = request.getMethod() + ":" + digestUri;
                    try {
                        expectedResponse = KD(H(A1), nonce + ":" + H(A2));
                    } catch (NoSuchAlgorithmException ex) {
                        response.sendError(HResponse.SC_INTERNAL_SERVER_ERROR, ex.toString());
                        return null;
                    }
                    if (expectedResponse.equals(responseDigest)) authUser = username;
                    break;
                }
            }
        }
        return authUser == null ? null : new HPrincipal(authUser);
    }

    static Hashtable parseHeader(String header) {
        if (!header.startsWith("Digest") || !Character.isWhitespace(header.charAt("Digest".length()))) return null;
        String params = header.substring("Digest".length() + 1);
        String key;
        Hashtable table = new Hashtable();
        StringTokenizer st = new StringTokenizer(params, ",");
        StringTokenizer st2;
        while (st.hasMoreTokens()) {
            st2 = new StringTokenizer(st.nextToken(), "=");
            if (st2.countTokens() != 2) continue;
            key = st2.nextToken().trim();
            table.put(key, st2.nextToken().trim());
        }
        return table;
    }
}
