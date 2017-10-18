package org.openacs;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.MessagingException;
import javax.servlet.*;
import javax.servlet.http.*;
import org.openacs.message.GetParameterValuesResponse;
import org.openacs.message.GetRPCMethodsResponse;
import org.openacs.utils.Ejb;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import org.openacs.message.GetParameterNamesResponse;

/**
 *
 * @author Administrator
 * @version
 */
public class client extends HttpServlet {

    /** Processes requests for both HTTP <code>GET</code> and <code>POST</code> methods.
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        PrintWriter out = response.getWriter();
        if (false) {
            HttpAuthentication.Authenticate("testuser", "testpass", HttpAuthentication.AUTHTYPE_MD5, request, response);
        }
        if (true) {
            System.out.println("src: addr=" + request.getRemoteAddr() + " port=" + request.getRemotePort());
            String auth = request.getHeader("Authorization");
            if (auth == null) {
                System.out.println("CLIENT: PorcessRequest " + response.SC_UNAUTHORIZED + " " + request.getHeader("Authorization"));
                byte[] nonce = new byte[16];
                Random r = new Random();
                r.nextBytes(nonce);
                response.setHeader("WWW-Authenticate", "Digest realm=\"OpenACS\",qop=\"auth,auth-int\",nonce=\"" + cvtHex(nonce) + "\"");
                response.setStatus(response.SC_UNAUTHORIZED);
            } else {
                if (auth.startsWith("Basic ")) {
                    String up = auth.substring(6);
                    String ds = null;
                    try {
                        InputStream i = javax.mail.internet.MimeUtility.decode(new ByteArrayInputStream(up.getBytes()), "base64");
                        byte[] d = new byte[i.available()];
                        i.read(d);
                        ds = new String(d);
                    } catch (MessagingException ex) {
                        Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (up.endsWith("==")) {
                        ds = ds.substring(0, ds.length() - 2);
                    } else if (up.endsWith("=")) {
                        ds = ds.substring(0, ds.length() - 1);
                    }
                    String[] upa = ds.split(":");
                    System.out.println("CLIENT: up=" + up + " d='" + ds + "' user=" + upa[0] + " pass=" + upa[1]);
                } else if (auth.startsWith("Digest ")) {
                    if (auth.indexOf("nc=00000001") != -1) {
                        byte[] nonce = new byte[16];
                        Random r = new Random();
                        r.nextBytes(nonce);
                        System.out.println("Saying it is stale: " + auth);
                        response.setHeader("WWW-Authenticate", "Digest realm=\"OpenACS\",qop=\"auth,auth-int\",stale=true,nonce=\"" + cvtHex(nonce) + "\"");
                        response.setStatus(response.SC_UNAUTHORIZED);
                        return;
                    }
                    ByteArrayInputStream bi = new ByteArrayInputStream(auth.substring(6).replace(',', '\n').replaceAll("\"", "").getBytes());
                    Properties p = new Properties();
                    p.load(bi);
                    p.setProperty("method", request.getMethod());
                    for (Entry<Object, Object> e : p.entrySet()) {
                        System.out.println("Entry " + e.getKey() + " -> " + e.getValue());
                    }
                    MessageDigest digest = null;
                    try {
                        digest = MessageDigest.getInstance("MD5");
                    } catch (NoSuchAlgorithmException ex) {
                        Logger.getLogger(client.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    postDigest(digest, p);
                    String udigest = (String) p.getProperty("response");
                    String dd = cvtHex(digest.digest());
                    System.out.println("respone: got='" + udigest + "' expected: '" + dd + "'");
                }
            }
            out.println("Hello");
            out.close();
            return;
        }
        int i;
        String oui = "00147F";
        String sn = "CP0713JTP7W";
        CPELocal cpe = Ejb.lookupCPEBean();
        out.println("Lookup bean .....\n" + cpe);
        GetParameterNamesResponse gpnr = null;
        Enumeration pnks = gpnr.names.keys();
        while (pnks.hasMoreElements()) {
            String k = (String) pnks.nextElement();
            out.println(k + " = " + gpnr.names.get(k));
        }
        if (true) {
            String[] n = new String[6];
            n[0] = "InternetGatewayDevice.DeviceSummary";
            n[1] = "InternetGatewayDevice.DeviceInfo.Manufacturer";
            n[2] = "InternetGatewayDevice.DeviceInfo.ManufacturerOUI";
            n[3] = "InternetGatewayDevice.IPPingDiagnostics.DiagnosticsState";
            n[4] = "InternetGatewayDevice.IPPingDiagnostics.SuccessCount";
            n[5] = "InternetGatewayDevice.DeviceInfo.ProductClass";
            GetParameterValuesResponse values = null;
            if (values == null) {
                out.println("No response .....");
            }
            Enumeration ve = values.values.keys();
            out.println("-----------------------------------------------------------------");
            while (ve.hasMoreElements()) {
                String k = (String) ve.nextElement();
                String v = (String) values.values.get(k);
                out.println(k + "=" + v);
            }
            out.println("-----------------------------------------------------------------");
            GetRPCMethodsResponse methods = null;
            if (methods == null) {
                out.println("No response .....");
            }
            for (i = 0; i < methods.methods.length; i++) {
                out.println(methods.methods[i]);
            }
        }
        out.close();
    }

    private String username;

    private String password;

    private boolean passwordIsA1Hash;

    public void postDigest(MessageDigest digest, Properties p) {
        username = "Mufasa";
        password = "Circle Of Life";
        username = "testuser";
        password = "testpass";
        passwordIsA1Hash = false;
        String qop = (String) p.getProperty("qop");
        String realm = (String) p.getProperty("realm");
        String algorithm = (String) p.getProperty("algorithm");
        String nonce = (String) p.getProperty("nonce");
        String cnonce = (String) p.getProperty("cnonce");
        String method = (String) p.getProperty("method");
        String nc = (String) p.getProperty("nc");
        String digestURI = (String) p.getProperty("uri");
        if (algorithm == null) algorithm = digest.getAlgorithm();
        digest.reset();
        String hA1 = null;
        if (algorithm == null || algorithm.equals("MD5")) {
            if (passwordIsA1Hash) hA1 = password; else {
                String A1 = username + ":" + realm + ":" + password;
                hA1 = H(A1, digest);
            }
        } else if (algorithm.equals("MD5-sess")) {
            if (passwordIsA1Hash) {
                hA1 = password + ":" + nonce + ":" + cnonce;
            } else {
                String A1 = username + ":" + realm + ":" + password;
                hA1 = H(A1, digest) + ":" + nonce + ":" + cnonce;
            }
        } else {
            throw new IllegalArgumentException("Unsupported algorigthm: " + algorithm);
        }
        String hA2 = null;
        if (hA2 == null) {
            String A2 = null;
            if (qop == null || qop.equals("auth")) {
                A2 = method + ":" + digestURI;
            } else {
                throw new IllegalArgumentException("Unsupported qop=" + qop);
            }
            hA2 = H(A2, digest);
        }
        if (qop == null) {
            String extra = nonce + ":" + hA2;
            KD(hA1, extra, digest);
        } else if (qop.equals("auth")) {
            String extra = nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + hA2;
            KD(hA1, extra, digest);
        }
    }

    private static String H(String data, MessageDigest digest) {
        digest.reset();
        byte[] x = digest.digest(data.getBytes());
        return cvtHex(x);
    }

    private static char[] MD5_HEX = "0123456789abcdef".toCharArray();

    static String cvtHex(byte[] data) {
        char[] hash = new char[32];
        for (int i = 0; i < 16; i++) {
            int j = (data[i] >> 4) & 0xf;
            hash[i * 2] = MD5_HEX[j];
            j = data[i] & 0xf;
            hash[i * 2 + 1] = MD5_HEX[j];
        }
        return new String(hash);
    }

    private static void KD(String secret, String data, MessageDigest digest) {
        String x = secret + ":" + data;
        digest.reset();
        digest.update(x.getBytes());
    }

    /** Handles the HTTP <code>GET</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Handles the HTTP <code>POST</code> method.
     * @param request servlet request
     * @param response servlet response
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /** Returns a short description of the servlet.
     */
    public String getServletInfo() {
        return "Short description";
    }
}
