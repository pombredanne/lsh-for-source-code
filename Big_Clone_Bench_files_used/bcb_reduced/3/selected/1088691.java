package org.netbeans.saas.facebook;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.netbeans.saas.RestConnection;

/**
 *
 * @author balbir kaur
 */
public class FacebookSocialNetworkingServiceAuthenticator {

    private static String apiKey;

    private static String secret;

    private static final String PROP_FILE = FacebookSocialNetworkingServiceAuthenticator.class.getSimpleName().toLowerCase() + ".properties";

    static {
        try {
            Properties props = new Properties();
            props.load(FacebookSocialNetworkingServiceAuthenticator.class.getResourceAsStream(PROP_FILE));
            apiKey = props.getProperty("api_key");
            secret = props.getProperty("secret");
        } catch (IOException ex) {
            Logger.getLogger(FacebookSocialNetworkingServiceAuthenticator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static String getApiKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (apiKey == null || apiKey.length() == 0) {
        }
        return apiKey;
    }

    public static String getSessionKey(HttpServletRequest request, HttpServletResponse response) throws IOException {
        assert request != null;
        String sessionKey = (String) request.getSession().getAttribute("facebook_session_key");
        if (sessionKey == null || sessionKey.length() == 0) {
        }
        return sessionKey;
    }

    private static String getSecret() throws IOException {
        if (secret == null || secret.length() == 0) {
        }
        return secret;
    }

    static void login(HttpServletRequest request, HttpServletResponse response) throws IOException {
        assert request != null;
        assert response != null;
        HttpSession session = request.getSession(true);
        String sessionKey = (String) session.getAttribute("facebook_session_key");
        if (sessionKey != null) {
            return;
        }
        String authToken = (String) session.getAttribute("facebook_auth_token");
        if (authToken != null) {
            session.removeAttribute("facebook_auth_token");
            String method = "facebook.auth.getSession";
            String v = "1.0";
            String sig = sign(new String[][] { { "method", method }, { "v", v }, { "api_key", apiKey }, { "auth_token", authToken } });
            RestConnection conn = new RestConnection("http://api.facebook.com/restserver.php", new String[][] { { "method", method }, { "api_key", apiKey }, { "sig", sig }, { "v", v }, { "auth_token", authToken } });
            String result = conn.get().getDataAsString();
            try {
                sessionKey = result.substring(result.indexOf("<session_key>") + 13, result.indexOf("</session_key>"));
                session.setAttribute("facebook_session_key", sessionKey);
            } catch (Exception ex) {
            }
            String returnUrl = (String) session.getAttribute("facebook_return_url");
            if (returnUrl != null) {
                session.removeAttribute("facebook_return_url");
                response.sendRedirect(returnUrl);
            }
        } else {
            session.setAttribute("facebook_return_url", request.getRequestURI());
            response.sendRedirect(request.getContextPath() + "/FacebookSocialNetworkingServiceLogin");
        }
    }

    private static void logout() {
    }

    public static String sign(String[][] params) throws IOException {
        TreeMap<String, String> map = new TreeMap<String, String>();
        for (int i = 0; i < params.length; i++) {
            String key = params[i][0];
            String value = params[i][1];
            if (value != null) {
                map.put(key, value);
            }
        }
        String signature = "";
        Set<Map.Entry<String, String>> entrySet = map.entrySet();
        for (Map.Entry<String, String> entry : entrySet) {
            signature += entry.getKey() + "=" + entry.getValue();
        }
        signature += getSecret();
        BigInteger bigInt;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] sum = md.digest(signature.getBytes("UTF-8"));
            bigInt = new BigInteger(1, sum);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
        return bigInt.toString(16);
    }
}
