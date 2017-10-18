package visitpc.webserver;

import java.util.*;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;

/**
 * Process a Http Post login attempt
 */
public class LoginHttpPostProcessor extends HttpPostProcessor {

    public static final String LOGIN = "Login";

    public static final String USERNAME = "USERNAME";

    public static final String PASSWORD = "PASSWORD";

    public static final String HASH = "hash";

    public static final String SALT = "salt";

    private WebServer webServer;

    private String srcAddress;

    public LoginHttpPostProcessor(WebServer webServer) {
        super(webServer);
        this.webServer = webServer;
    }

    public void setNetworkAddress(String srcAddress) {
        this.srcAddress = srcAddress;
    }

    /**
   * Return true if this is a login post
   */
    public boolean isHandled(String httpHeader, Hashtable<String, String> variableHashtable) {
        boolean isLogin = false;
        String action;
        try {
            action = (String) variableHashtable.get("submit");
            if (action != null && action.equals(LoginHttpPostProcessor.LOGIN)) {
                isLogin = true;
            }
        } catch (Exception e) {
        }
        return isLogin;
    }

    public HttpPostResponse processHttpPost(String httpHeader, Hashtable<String, String> variableHashtable, PrintStream ps) {
        HttpPostResponse httpPostResponse = new HttpPostResponse();
        String clientHash = null;
        String clientSalt = null;
        String serverHash = null;
        boolean allowAccess = false;
        Calendar calendar = null;
        try {
            if (webServer.getLoginSecurity()) {
                clientHash = (String) variableHashtable.get(HASH);
                clientSalt = (String) variableHashtable.get(SALT);
                Vector<Client> validClientList = webServer.getValidClientList();
                for (Client client : validClientList) {
                    serverHash = GetSHA1(client.username + client.password + clientSalt);
                    if (clientHash.equals(serverHash)) {
                        client.cookieValue = clientSalt;
                        webServer.getClientLogger().addToLoggedInClientList(srcAddress, client.username.trim(), client.password.trim(), client.userTimeoutMs, client.cookieValue, serverHash);
                        calendar = Calendar.getInstance();
                        calendar.add(Calendar.MINUTE, 1);
                        SimpleDateFormat sdf = new SimpleDateFormat("EEEEEEEEEEEEEE, dd-MMM-yy kk:mm:ss");
                        String dateStr = sdf.format(calendar.getTime());
                        String setCookiePage = "<META HTTP-EQUIV=\"Set-Cookie\"CONTENT=\"value=" + client.cookieValue + ";expires=" + dateStr + " GMT; path=/\">";
                        ps.write(setCookiePage.getBytes());
                        allowAccess = true;
                        webServer.log(WebServer.LOG_DEST.ACCESS_FILE, WebServer.LOG_TYPE.INFO, "LOGIN SUCCESS: " + client);
                    }
                }
                if (allowAccess) {
                    httpPostResponse.fileContents = getFileContents(webServer.getRootDir().getAbsolutePath(), HttpPostProcessor.DEFAULT_FILE, false);
                    httpPostResponse.fileContents = new String(httpPostResponse.fileContents.getBytes());
                } else {
                    webServer.log(WebServer.LOG_DEST.ACCESS_FILE, WebServer.LOG_TYPE.INFO, "LOGIN FAILED: " + this.srcAddress);
                    httpPostResponse.fileContents = getFileContents(webServer.getRootDir().getAbsolutePath(), HttpPostProcessor.LOGIN_FILE, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                httpPostResponse.fileContents = getFileContents(webServer.getRootDir().getAbsolutePath(), HttpPostProcessor.LOGIN_FAILED_FILE, false);
            } catch (IOException ex) {
                httpPostResponse.fileContents = "Failed to find " + HttpPostProcessor.UNHANDLED_POST_FILE;
            }
        }
        return httpPostResponse;
    }

    private static String ConvertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    private static String GetSHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return LoginHttpPostProcessor.ConvertToHex(sha1hash);
    }
}
