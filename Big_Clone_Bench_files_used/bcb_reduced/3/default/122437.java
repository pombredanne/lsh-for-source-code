import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Servlet implementation of Jabber/XMPP JEP-0025, HTTP Polling:
 *
 * http://www.jabber.org/jeps/jep-0025.html
 *
 * This JEP specifies a gateway module between Jabber/XMPP clients
 * that, because of client resource/connection limitationss,
 * poll the server via HTTP. This servlet handles the special request
 * body formatting that is used to tie XMPP packet contents to
 * specific server connections, and to provide minimal verification
 * that new polling requests come from the same clients as previous
 * requests with the same session id.
 *
 * @author Sam Douglass
 * @version 1.0.0
 *
 */
public class JabberHttpPollingServlet extends HttpServlet {

    private static final String ERR_IDENTIFIER_SERVER = "-1:0";

    private static final String ERR_IDENTIFIER_BADREQUEST = "-2:0";

    private static final String ERR_IDENTIFIER_KEYSEQ = "-3:0";

    private static final String DEFAULT_JABBER_SERVER = "127.0.0.1";

    private static final int DEFAULT_JABBER_PORT = 5222;

    private static final int BUFFER_SIZE = 1024;

    private static boolean serverConnectionBusy = false;

    private String jabberServer;

    private int jabberPort;

    /**
   * Initializes the servlet. Sets the jabberServer and jabberPort fields.
   */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        jabberServer = config.getInitParameter("jabberServer");
        if (jabberServer == null) {
            jabberServer = DEFAULT_JABBER_SERVER;
        }
        try {
            jabberPort = Integer.parseInt(config.getInitParameter("jabberPort"));
        } catch (NumberFormatException e) {
            jabberPort = DEFAULT_JABBER_PORT;
        }
    }

    /** Destroys the servlet.
   */
    public void destroy() {
        JabberServerConnectionRegistry.getInstance().closeAll();
    }

    /** Handles the HTTP <code>POST</code> method.
   * @param request servlet request
   * @param response servlet response
   */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/xml;charset=UTF-8");
        String requestBodyStr;
        try {
            requestBodyStr = inputStreamToString(request.getInputStream());
        } catch (IOException e) {
            handleError(response, ERR_IDENTIFIER_SERVER, "Error getting input from client.");
            return;
        }
        HttpPollingRequestBody body;
        try {
            body = new HttpPollingRequestBody(requestBodyStr);
        } catch (Exception e) {
            handleError(response, ERR_IDENTIFIER_BADREQUEST, "Bad request data.");
            return;
        }
        JabberServerConnection jsc;
        if (body.getSessionId() == null) {
            try {
                jsc = JabberServerConnectionRegistry.getInstance().createConnection(jabberServer, jabberPort);
            } catch (JabberException e) {
                handleError(response, ERR_IDENTIFIER_SERVER, e.getMessage());
                return;
            }
            jsc.setClientKey(body.getKey());
        } else {
            jsc = JabberServerConnectionRegistry.getInstance().getConnection(body.getSessionId());
            if (jsc == null) {
                handleError(response, ERR_IDENTIFIER_SERVER, "Bad session id.");
                return;
            }
            if (!keysMatch(body.getKey(), jsc.getClientKey())) {
                handleError(response, ERR_IDENTIFIER_KEYSEQ, "Invalid client key.");
                return;
            }
            if (body.getNewKey() != null) {
                jsc.setClientKey(body.getNewKey());
            } else {
                jsc.setClientKey(body.getKey());
            }
        }
        if (!serverConnectionBusy) {
            serverConnectionBusy = true;
            boolean syncRequest = body.getXmlData() != null;
            if (syncRequest) {
                try {
                    jsc.send(body.getXmlData().getBytes(), 0, body.getXmlData().length());
                } catch (JabberException e) {
                    handleError(response, ERR_IDENTIFIER_SERVER, e.getMessage());
                    serverConnectionBusy = false;
                    return;
                }
            }
            Cookie sessionIdCookie = new HttpPollingSessionIdCookie(jsc.getSessionId());
            response.addCookie(sessionIdCookie);
            try {
                jsc.receive(response.getOutputStream(), syncRequest);
            } catch (JabberException e) {
                handleError(response, ERR_IDENTIFIER_SERVER, e.getMessage());
                serverConnectionBusy = false;
                return;
            }
            serverConnectionBusy = false;
        } else {
        }
    }

    /**
   * Reads the given InputStream and returns its contents as a String.
   *
   * @param in the input stream to read into a String
   * @return the contents of the given InputStream as a String
   * @throws IOException
   */
    private String inputStreamToString(InputStream in) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(in));
        StringWriter sw = new StringWriter(BUFFER_SIZE);
        int charsRead = -1;
        char[] buffer = new char[BUFFER_SIZE];
        while ((charsRead = r.read(buffer)) != -1) {
            sw.write(buffer, 0, charsRead);
        }
        return sw.toString();
    }

    /**
   * Handles an error. Sets the identifier cookie value to the error type, and
   * puts the error message into some XML in the response body according to the
   * JEP.
   *
   * @param response the servlet response to the polling XMPP client
   * @param errorType the error type (should be one of the constants defined in this class)
   * @param errorMessage the message for the client to display
   */
    private void handleError(HttpServletResponse response, String errorType, String errorMessage) {
        Cookie errorCookie = new HttpPollingSessionIdCookie(errorType);
        response.addCookie(errorCookie);
        try {
            PrintWriter responseWriter = response.getWriter();
            responseWriter.write("<?xml version=\"1.0\"?>");
            responseWriter.write("<error>" + errorMessage + "</error>");
        } catch (IOException e) {
        }
    }

    /** Returns a short description of the servlet.
   */
    public String getServletInfo() {
        return "A Java servlet implementation of Jabber JEP-0025 HTTP Polling (http://www.jabber.org/jeps/jep-0025.html).";
    }

    /** Handles the HTTP <code>GET</code> method.
   * @param request servlet request
   * @param response servlet response
   */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This servlet accepts only POST requests.");
    }

    /**
   * Checks to see if the key coming in with a client request matches
   * the key stored from the previous client request, once the new key
   * has been sha-1 then base64 encoded, as specified in the JEP:
   *
   * K(n, seed) = Base64Encode(SHA1(K(n - 1, seed))), for n > 0
   * K(0, seed) = seed, which is client-determined
   *
   * @param keyNMinusOne the key from the latest client request, K[n-1]
   * @param keyN the key from the previous client request, K[n]
   * @return
   */
    private boolean keysMatch(String keyNMinusOne, String keyN) {
        boolean match = false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.reset();
            digest.update(keyNMinusOne.getBytes());
            byte[] hashedBytes = digest.digest();
            String encodedHashedKey = new String(com.Ostermiller.util.Base64.encode(hashedBytes));
            match = encodedHashedKey.equals(keyN);
        } catch (NoSuchAlgorithmException e) {
        }
        return match;
    }
}
