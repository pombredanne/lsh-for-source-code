package local.server;

import org.zoolu.sip.address.*;
import org.zoolu.sip.header.WwwAuthenticateHeader;
import org.zoolu.sip.header.AuthorizationHeader;
import org.zoolu.sip.header.AuthenticationInfoHeader;
import org.zoolu.sip.header.ProxyAuthenticateHeader;
import org.zoolu.sip.header.ProxyAuthorizationHeader;
import org.zoolu.sip.message.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.authentication.DigestAuthentication;
import org.zoolu.tools.EventLogger;
import org.zoolu.tools.LogLevel;
import org.zoolu.tools.Parser;
import org.zoolu.tools.MD5;

/** Class AuthenticationServerImpl implements an AuthenticationServer
  * for HTTP Digest authentication.
  */
public class AuthenticationServerImpl implements AuthenticationServer {

    /** Server authentication. */
    protected static final int SERVER_AUTHENTICATION = 0;

    /** Proxy authentication. */
    protected static final int PROXY_AUTHENTICATION = 1;

    /** Event logger. */
    protected EventLogger log = null;

    /** The repository of users's authentication data. */
    protected AuthenticationService authentication_service;

    /** The authentication realm. */
    protected String realm;

    /** The authentication scheme. */
    protected String authentication_scheme = "Digest";

    /** The authentication qop-options. */
    protected String qop_options = "auth,auth-int";

    /** The current random value. */
    protected byte[] rand;

    /** Costructs a new AuthenticationServerImpl. */
    public AuthenticationServerImpl(String realm, AuthenticationService authentication_service, EventLogger log) {
        init(realm, authentication_service, log);
    }

    /** Inits the AuthenticationServerImpl. */
    private void init(String realm, AuthenticationService authentication_service, EventLogger log) {
        this.log = log;
        this.realm = realm;
        this.authentication_service = authentication_service;
        this.rand = pickRandBytes();
    }

    /** Authenticates a SIP request.
     * @param msg is the SIP request to be authenticated
     * @return it returns the error Message in case of authentication failure,
     * or null in case of authentication success. */
    public Message authenticateRequest(Message msg) {
        return authenticateRequest(msg, SERVER_AUTHENTICATION);
    }

    /** Authenticates a SIP request.
     * @param msg is the SIP request to be authenticated
     * @return it returns the error Message in case of authentication failure,
     * or null in case of authentication success. */
    public Message authenticateProxyRequest(Message msg) {
        return authenticateRequest(msg, PROXY_AUTHENTICATION);
    }

    /** Authenticates a SIP request.
     * @param msg is the SIP request to be authenticated
     * @param proxy_authentication whether performing Proxy-Authentication or simple Authentication
     * @return it returns the error Message in case of authentication failure,
     * or null in case of authentication success. */
    protected Message authenticateRequest(Message msg, int type) {
        Message err_resp = null;
        AuthorizationHeader ah;
        if (type == SERVER_AUTHENTICATION) ah = msg.getAuthorizationHeader(); else ah = msg.getProxyAuthorizationHeader();
        if (ah != null && ah.getNonceParam().equals(HEX(rand))) {
            String realm = ah.getRealmParam();
            String nonce = ah.getNonceParam();
            String username = ah.getUsernameParam();
            String scheme = ah.getAuthScheme();
            String user = username + "@" + realm;
            if (authentication_service.hasUser(user)) {
                if (authentication_scheme.equalsIgnoreCase(scheme)) {
                    DigestAuthentication auth = new DigestAuthentication(msg.getRequestLine().getMethod(), ah, null, keyToPasswd(authentication_service.getUserKey(user)));
                    boolean is_authorized = auth.checkResponse();
                    rand = pickRandBytes();
                    if (!is_authorized) {
                        int result = 403;
                        err_resp = MessageFactory.createResponse(msg, result, SipResponses.reasonOf(result), null);
                        printLog("LOGIN ERROR: Authentication of '" + user + "' failed", LogLevel.HIGH);
                    } else {
                        printLog("Authentication of '" + user + "' successed", LogLevel.HIGH);
                    }
                } else {
                    int result = 400;
                    err_resp = MessageFactory.createResponse(msg, result, SipResponses.reasonOf(result), null);
                    printLog("Authentication method '" + scheme + "' not supported.", LogLevel.HIGH);
                }
            } else {
                int result = 404;
                err_resp = MessageFactory.createResponse(msg, result, SipResponses.reasonOf(result), null);
            }
        } else {
            int result;
            if (type == SERVER_AUTHENTICATION) result = 401; else result = 407;
            err_resp = MessageFactory.createResponse(msg, result, SipResponses.reasonOf(result), null);
            WwwAuthenticateHeader wah;
            if (type == SERVER_AUTHENTICATION) wah = new WwwAuthenticateHeader("Digest"); else wah = new ProxyAuthenticateHeader("Digest");
            wah.addRealmParam(realm);
            wah.addQopOptionsParam(qop_options);
            wah.addNonceParam(HEX(rand));
            err_resp.setWwwAuthenticateHeader(wah);
        }
        return err_resp;
    }

    /** Gets AuthenticationInfoHeader. */
    public AuthenticationInfoHeader getAuthenticationInfoHeader() {
        AuthenticationInfoHeader aih = new AuthenticationInfoHeader();
        aih.addRealmParam(realm);
        aih.addQopOptionsParam(qop_options);
        aih.addNextnonceParam(HEX(rand));
        return aih;
    }

    /** Picks a random array of 16 bytes. */
    private static byte[] pickRandBytes() {
        return MD5(Long.toHexString(org.zoolu.tools.Random.nextLong()));
    }

    /** Converts the byte[] key in a String passwd. */
    private static String keyToPasswd(byte[] key) {
        return new String(key);
    }

    /** Calculates the MD5 of a String. */
    private static byte[] MD5(String str) {
        return MD5.digest(str);
    }

    /** Calculates the MD5 of an array of bytes. */
    private static byte[] MD5(byte[] bb) {
        return MD5.digest(bb);
    }

    /** Calculates the HEX of an array of bytes. */
    private static String HEX(byte[] bb) {
        return MD5.asHex(bb);
    }

    /** Adds a new string to the default EventLogger */
    protected void printLog(String str, int level) {
        if (log != null) log.println("AuthenticationServer: " + str, level + SipStack.LOG_LEVEL_UA);
    }
}
