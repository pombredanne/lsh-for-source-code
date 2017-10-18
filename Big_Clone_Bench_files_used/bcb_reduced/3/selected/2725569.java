package org.apache.catalina.authenticator;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletResponse;
import org.apache.catalina.Realm;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.deploy.LoginConfig;
import org.apache.catalina.util.MD5Encoder;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * An <b>Authenticator</b> and <b>Valve</b> implementation of HTTP DIGEST
 * Authentication (see RFC 2069).
 *
 * @author Craig R. McClanahan
 * @author Remy Maucherat
 * @version $Revision: 467222 $ $Date: 2006-10-24 05:17:11 +0200 (Tue, 24 Oct 2006) $
 */
public class DigestAuthenticator extends AuthenticatorBase {

    private static Log log = LogFactory.getLog(DigestAuthenticator.class);

    /**
     * The MD5 helper object for this class.
     */
    protected static final MD5Encoder md5Encoder = new MD5Encoder();

    /**
     * Descriptive information about this implementation.
     */
    protected static final String info = "org.apache.catalina.authenticator.DigestAuthenticator/1.0";

    public DigestAuthenticator() {
        super();
        try {
            if (md5Helper == null) md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest md5Helper;

    /**
     * Private key.
     */
    protected String key = "Catalina";

    /**
     * Return descriptive information about this Valve implementation.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Authenticate the user making this request, based on the specified
     * login configuration.  Return <code>true</code> if any specified
     * constraint has been satisfied, or <code>false</code> if we have
     * created a response challenge already.
     *
     * @param request Request we are processing
     * @param response Response we are creating
     * @param config    Login configuration describing how authentication
     *              should be performed
     *
     * @exception IOException if an input/output error occurs
     */
    public boolean authenticate(Request request, Response response, LoginConfig config) throws IOException {
        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            if (log.isDebugEnabled()) log.debug("Already authenticated '" + principal.getName() + "'");
            String ssoId = (String) request.getNote(Constants.REQ_SSOID_NOTE);
            if (ssoId != null) associate(ssoId, request.getSessionInternal(true));
            return (true);
        }
        String authorization = request.getHeader("authorization");
        if (authorization != null) {
            principal = findPrincipal(request, authorization, context.getRealm());
            if (principal != null) {
                String username = parseUsername(authorization);
                register(request, response, principal, Constants.DIGEST_METHOD, username, null);
                return (true);
            }
        }
        String nOnce = generateNOnce(request);
        setAuthenticateHeader(request, response, config, nOnce);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        return (false);
    }

    /**
     * Parse the specified authorization credentials, and return the
     * associated Principal that these credentials authenticate (if any)
     * from the specified Realm.  If there is no such Principal, return
     * <code>null</code>.
     *
     * @param request HTTP servlet request
     * @param authorization Authorization credentials from this request
     * @param realm Realm used to authenticate Principals
     */
    protected static Principal findPrincipal(Request request, String authorization, Realm realm) {
        if (authorization == null) return (null);
        if (!authorization.startsWith("Digest ")) return (null);
        authorization = authorization.substring(7).trim();
        String[] tokens = authorization.split(",(?=(?:[^\"]*\"[^\"]*\")+$)");
        String userName = null;
        String realmName = null;
        String nOnce = null;
        String nc = null;
        String cnonce = null;
        String qop = null;
        String uri = null;
        String response = null;
        String method = request.getMethod();
        for (int i = 0; i < tokens.length; i++) {
            String currentToken = tokens[i];
            if (currentToken.length() == 0) continue;
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0) return null;
            String currentTokenName = currentToken.substring(0, equalSign).trim();
            String currentTokenValue = currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName)) userName = removeQuotes(currentTokenValue);
            if ("realm".equals(currentTokenName)) realmName = removeQuotes(currentTokenValue, true);
            if ("nonce".equals(currentTokenName)) nOnce = removeQuotes(currentTokenValue);
            if ("nc".equals(currentTokenName)) nc = removeQuotes(currentTokenValue);
            if ("cnonce".equals(currentTokenName)) cnonce = removeQuotes(currentTokenValue);
            if ("qop".equals(currentTokenName)) qop = removeQuotes(currentTokenValue);
            if ("uri".equals(currentTokenName)) uri = removeQuotes(currentTokenValue);
            if ("response".equals(currentTokenName)) response = removeQuotes(currentTokenValue);
        }
        if ((userName == null) || (realmName == null) || (nOnce == null) || (uri == null) || (response == null)) return null;
        String a2 = method + ":" + uri;
        byte[] buffer = null;
        synchronized (md5Helper) {
            buffer = md5Helper.digest(a2.getBytes());
        }
        String md5a2 = md5Encoder.encode(buffer);
        return (realm.authenticate(userName, response, nOnce, nc, cnonce, qop, realmName, md5a2));
    }

    /**
     * Parse the username from the specified authorization string.  If none
     * can be identified, return <code>null</code>
     *
     * @param authorization Authorization string to be parsed
     */
    protected String parseUsername(String authorization) {
        if (authorization == null) return (null);
        if (!authorization.startsWith("Digest ")) return (null);
        authorization = authorization.substring(7).trim();
        StringTokenizer commaTokenizer = new StringTokenizer(authorization, ",");
        while (commaTokenizer.hasMoreTokens()) {
            String currentToken = commaTokenizer.nextToken();
            int equalSign = currentToken.indexOf('=');
            if (equalSign < 0) return null;
            String currentTokenName = currentToken.substring(0, equalSign).trim();
            String currentTokenValue = currentToken.substring(equalSign + 1).trim();
            if ("username".equals(currentTokenName)) return (removeQuotes(currentTokenValue));
        }
        return (null);
    }

    /**
     * Removes the quotes on a string. RFC2617 states quotes are optional for
     * all parameters except realm.
     */
    protected static String removeQuotes(String quotedString, boolean quotesRequired) {
        if (quotedString.length() > 0 && quotedString.charAt(0) != '"' && !quotesRequired) {
            return quotedString;
        } else if (quotedString.length() > 2) {
            return quotedString.substring(1, quotedString.length() - 1);
        } else {
            return new String();
        }
    }

    /**
     * Removes the quotes on a string.
     */
    protected static String removeQuotes(String quotedString) {
        return removeQuotes(quotedString, false);
    }

    /**
     * Generate a unique token. The token is generated according to the
     * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":"
     * time-stamp ":" private-key ) ).
     *
     * @param request HTTP Servlet request
     */
    protected String generateNOnce(Request request) {
        long currentTime = System.currentTimeMillis();
        String nOnceValue = request.getRemoteAddr() + ":" + currentTime + ":" + key;
        byte[] buffer = null;
        synchronized (md5Helper) {
            buffer = md5Helper.digest(nOnceValue.getBytes());
        }
        nOnceValue = md5Encoder.encode(buffer);
        return nOnceValue;
    }

    /**
     * Generates the WWW-Authenticate header.
     * <p>
     * The header MUST follow this template :
     * <pre>
     *      WWW-Authenticate    = "WWW-Authenticate" ":" "Digest"
     *                            digest-challenge
     *
     *      digest-challenge    = 1#( realm | [ domain ] | nOnce |
     *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
     *
     *      realm               = "realm" "=" realm-value
     *      realm-value         = quoted-string
     *      domain              = "domain" "=" <"> 1#URI <">
     *      nonce               = "nonce" "=" nonce-value
     *      nonce-value         = quoted-string
     *      opaque              = "opaque" "=" quoted-string
     *      stale               = "stale" "=" ( "true" | "false" )
     *      algorithm           = "algorithm" "=" ( "MD5" | token )
     * </pre>
     *
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response
     * @param config    Login configuration describing how authentication
     *              should be performed
     * @param nOnce nonce token
     */
    protected void setAuthenticateHeader(Request request, Response response, LoginConfig config, String nOnce) {
        String realmName = config.getRealmName();
        if (realmName == null) realmName = request.getServerName() + ":" + request.getServerPort();
        byte[] buffer = null;
        synchronized (md5Helper) {
            buffer = md5Helper.digest(nOnce.getBytes());
        }
        String authenticateHeader = "Digest realm=\"" + realmName + "\", " + "qop=\"auth\", nonce=\"" + nOnce + "\", " + "opaque=\"" + md5Encoder.encode(buffer) + "\"";
        response.setHeader("WWW-Authenticate", authenticateHeader);
    }
}
