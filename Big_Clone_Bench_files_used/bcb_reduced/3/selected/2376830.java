package org.openje.http.client;

import java.net.*;
import java.io.*;
import java.util.*;
import java.security.*;

/**
 * This class is created in order to test if the digest authenticatio of 
 * jasper works well.
 * <pre>
 * Usage   : java org.openje.http.client.AuthClient <URL> [<user> <passwd>]
 * 
 * Example : java org.openje.http.client.AuthClient http://localhost hiro pass
 * </pre>
 */
public class AuthClient {

    static boolean debug = true;

    public static void main(String argv[]) {
        try {
            if (argv.length != 1 && argv.length != 3) {
                usage();
                System.exit(1);
            }
            URL url = new URL(argv[0]);
            URLConnection conn;
            conn = url.openConnection();
            if (conn.getHeaderField("WWW-Authenticate") != null) {
                auth(conn, argv[1], argv[2]);
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) System.out.println(line);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Called when needed to be authenticated.
     * 
     * @param conn connection
     * @param user user name
     * @param pass password
     */
    protected static void auth(URLConnection conn, String user, String pass) {
        String authheader = conn.getHeaderField("WWW-Authenticate").trim();
        String method = null, params = null;
        if (debug) System.out.println("WWW-Authenticate:" + authheader);
        if (authheader.startsWith("Digest") && Character.isWhitespace(authheader.charAt("Digest".length()))) {
            method = "Digest";
            params = authheader.substring("Digest".length() + 1);
        } else if (authheader.startsWith("Basic") && Character.isWhitespace(authheader.charAt("Basic".length()))) {
            method = "Basic";
            params = authheader.substring("Basic".length() + 1);
        } else {
            System.err.println("Unknown authentication method");
            System.exit(2);
        }
        if (debug) {
            System.out.println("method:" + method);
            System.out.println("params:" + params);
        }
        Hashtable table = new Hashtable();
        StringTokenizer st = new StringTokenizer(params, ",");
        StringTokenizer st2;
        while (st.hasMoreTokens()) {
            st2 = new StringTokenizer(st.nextToken(), "=");
            if (st2.countTokens() != 2) continue;
            String key = st2.nextToken().trim();
            table.put(key, st2.nextToken().trim());
        }
        if (method.equals("Digest")) {
            String nonce = (String) table.get("nonce");
            String realm = (String) table.get("realm");
            if ((nonce = (String) table.get("nonce")) != null && (realm = (String) table.get("realm")) != null && (nonce = makeUnquoted(nonce)) != null && (realm = makeUnquoted(realm)) != null) {
                URL url = conn.getURL();
                String uri = url.toString();
                String http_method = "GET";
                String A1 = user + ":" + realm + ":" + pass;
                String A2 = http_method + ":" + uri;
                String responseDigest;
                System.out.println("user:" + user);
                System.out.println("pass:" + pass);
                System.out.println("realm:" + realm);
                System.out.println("method:" + http_method);
                System.out.println("uri:" + uri);
                try {
                    System.out.println("H(A1) = " + H(A1));
                    responseDigest = KD(H(A1), nonce + ":" + H(A2));
                } catch (NoSuchAlgorithmException ex) {
                    ex.printStackTrace();
                    return;
                }
                try {
                    String host = url.getHost();
                    int port = url.getPort();
                    if (port <= 0) port = 80;
                    Socket sock = new Socket(host, port);
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(sock.getOutputStream()));
                    writer.write("GET " + url.getFile() + " HTTP\n");
                    writer.write("Authorization:Digest " + "username=\"" + user + "\"," + "realm=\"" + realm + "\"," + "nonce=\"" + nonce + "\"," + "uri=\"" + url.toString() + "\"," + "response=\"" + responseDigest + "\"\n\n");
                    System.out.println("Authorization:Digest " + "username=\"" + user + "\"," + "realm=\"" + realm + "\"," + "nonce=\"" + nonce + "\"," + "uri=\"" + url.toString() + "\"," + "response=\"" + responseDigest + "\"\n\n");
                    writer.flush();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) System.out.println(line);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } else {
            System.err.println("yet not implemented...have fun! happy hack.");
        }
    }

    /**
     * Unquote if needed.
     *
     * @param s string to be unquoted
     * @return unquoted String
     */
    public static String makeUnquoted(String s) {
        return (s.length() >= 2 && s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"') ? s.substring(1, s.length() - 1) : null;
    }

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

    protected static void usage() {
    }
}
