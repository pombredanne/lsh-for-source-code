package org.openxdm.xcap.server.slee;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.slee.ActivityContextInterface;
import javax.slee.RolledBackContext;
import javax.slee.SbbContext;
import javax.slee.facilities.Tracer;
import org.mobicents.slee.ChildRelationExt;
import org.mobicents.slee.enabler.userprofile.UserProfile;
import org.mobicents.slee.enabler.userprofile.UserProfileControlSbbLocalObject;
import org.mobicents.slee.xdm.server.ServerConfiguration;
import org.openxdm.xcap.common.error.InternalServerErrorException;
import org.openxdm.xcap.common.http.HttpConstant;
import org.openxdm.xcap.server.slee.auth.RFC2617AuthQopDigest;
import org.openxdm.xcap.server.slee.auth.RFC2617ChallengeParamGenerator;

/**
 * 
 * @author aayush.bhatnagar
 * @author martins
 * 
 *         The authentication proxy only authenticates remote requests, if local
 *         uses asserted id if present, if not defines no user but does not
 *         fails.
 * 
 *         From the OMA-TS-XDM-core specification:
 * 
 *         The Aggregation Proxy SHALL act as an HTTP Proxy defined in [RFC2616]
 *         with the following clarifications. The Aggregation Proxy:
 * 
 *         1. SHALL be configured as an HTTP reverse proxy (see [RFC3040]);
 * 
 *         2. SHALL support authenticating the XDM Client; in case the GAA is
 *         used according to [3GPP TS 33.222], the mutual authentication SHALL
 *         be supported; or SHALL assert the XDM Client identity by inserting
 *         the X-XCAPAsserted- Identity extension header to the HTTP requests
 *         after a successful HTTP Digest Authentication as defined in Section
 *         6.3.2, in case the GAA is not used.
 * 
 *         3. SHALL forward the XCAP requests to the corresponding XDM Server,
 *         and forward the response back to the XDM Client;
 * 
 *         4. SHALL protect the XCAP traffic by enabling TLS transport security
 *         mechanism. The TLS resumption procedure SHALL be used as specified in
 *         [RFC2818].
 * 
 *         When realized with 3GPP IMS or 3GPP2 MMD networks, the Aggregation
 *         Proxy SHALL act as an Authentication Proxy defined in [3GPP TS
 *         33.222] with the following clarifications. The Aggregation Proxy:
 *         SHALL check whether an XDM Client identity has been inserted in
 *         X-3GPP-Intended-Identity header of HTTP request.
 * 
 *         � If the X-3GPP-Intended-Identity is included , the Aggregation Proxy
 *         SHALL check the value in the header is allowed to be used by the
 *         authenticated identity.
 * 
 *         � If the X-3GPP-Intended-Identity is not included, the Aggregation
 *         Proxy SHALL insert the authenticated identity in the
 *         X-3GPP-Asserted-Identity header of the HTTP request.
 * 
 *         TODO: GAA is not supported as of now. It is FFS on how we go about
 *         GAA support. TODO: TLS is not supported as of now.
 */
public abstract class AuthenticationProxySbb implements javax.slee.Sbb, AuthenticationProxy {

    private static Tracer logger;

    private static final RFC2617ChallengeParamGenerator challengeParamGenerator = new RFC2617ChallengeParamGenerator();

    private static final ServerConfiguration CONFIGURATION = ServerConfiguration.getInstance();

    public static final String HEADER_X_3GPP_Asserted_Identity = "X-3GPP-Asserted-Identity";

    public static final String HEADER_X_XCAP_Asserted_Identity = "X-XCAP-Asserted-Identity";

    public String authenticate(HttpServletRequest request, HttpServletResponse response) throws InternalServerErrorException {
        if (logger.isFineEnabled()) {
            logger.fine("Authenticating request");
        }
        try {
            String user = null;
            if (!CONFIGURATION.getLocalXcapAuthentication() && request.getRemoteAddr().equals(request.getLocalAddr())) {
                if (logger.isInfoEnabled()) {
                    logger.info("Skipping authentication for local request.");
                }
                if (CONFIGURATION.getAllowAssertedUserIDs()) {
                    user = request.getHeader(HEADER_X_3GPP_Asserted_Identity);
                    if (user == null) {
                        user = request.getHeader(HEADER_X_XCAP_Asserted_Identity);
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("Asserted user: " + user);
                    }
                }
            } else {
                if (CONFIGURATION.getAllowAssertedUserIDs()) {
                    user = request.getHeader(HEADER_X_3GPP_Asserted_Identity);
                    if (user == null) {
                        user = request.getHeader(HEADER_X_XCAP_Asserted_Identity);
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info("Asserted user: " + user);
                    }
                }
                if (user == null) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Remote request without asserted user, using http digest authentication");
                    }
                    if (request.getHeader(HttpConstant.HEADER_AUTHORIZATION) == null) {
                        challengeRequest(request, response);
                    } else {
                        user = checkAuthenticatedCredentials(request, response);
                        if (user != null) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Authentication suceed");
                            }
                        } else {
                            if (logger.isFineEnabled()) {
                                logger.fine("Authentication failed");
                            }
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.getWriter().close();
                        }
                    }
                }
            }
            return user;
        } catch (Throwable e) {
            throw new InternalServerErrorException(e.getMessage(), e);
        }
    }

    /**
	 * 
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws NoSuchAlgorithmException
	 * @throws InternalServerErrorException 
	 */
    private void challengeRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, NoSuchAlgorithmException, InternalServerErrorException {
        if (logger.isFineEnabled()) logger.fine("Authorization header is missing...challenging the request");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        String opaque = challengeParamGenerator.generateOpaque();
        final String challengeParams = "Digest nonce=\"" + challengeParamGenerator.getNonce(opaque) + "\", realm=\"" + getRealm() + "\", opaque=\"" + opaque + "\", qop=\"auth\"";
        response.setHeader(HttpConstant.HEADER_WWW_AUTHENTICATE, challengeParams);
        if (logger.isFineEnabled()) {
            logger.fine("Sending response with header " + HttpConstant.HEADER_WWW_AUTHENTICATE + " challenge params: " + challengeParams);
        }
        response.getWriter().close();
    }

    /**
	 * 
	 * @param request
	 * @param response
	 * @return null if authentication failed, authenticated user@domain otherwise
	 * @throws InternalServerErrorException 
	 */
    private String checkAuthenticatedCredentials(HttpServletRequest request, HttpServletResponse response) throws InternalServerErrorException {
        String authHeaderParams = request.getHeader(HttpConstant.HEADER_AUTHORIZATION);
        if (logger.isFineEnabled()) {
            logger.fine("Authorization header included with value: " + authHeaderParams);
        }
        final int digestParamsStart = 6;
        if (authHeaderParams.length() > digestParamsStart) {
            authHeaderParams = authHeaderParams.substring(digestParamsStart);
        }
        String username = null;
        String password = null;
        String realm = null;
        String nonce = null;
        String uri = null;
        String cnonce = null;
        String nc = null;
        String qop = null;
        String resp = null;
        String opaque = null;
        for (String param : authHeaderParams.split(",")) {
            int i = param.indexOf('=');
            if (i > 0 && i < (param.length() - 1)) {
                String paramName = param.substring(0, i).trim();
                String paramValue = param.substring(i + 1).trim();
                if (paramName.equals("username")) {
                    if (paramValue.length() > 2) {
                        username = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Username param with value " + username);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("nonce")) {
                    if (paramValue.length() > 2) {
                        nonce = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Nonce param with value " + nonce);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("cnonce")) {
                    if (paramValue.length() > 2) {
                        cnonce = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("CNonce param with value " + cnonce);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("realm")) {
                    if (paramValue.length() > 2) {
                        realm = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Realm param with value " + realm);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("nc")) {
                    nc = paramValue;
                    if (logger.isFineEnabled()) {
                        logger.fine("Nonce-count param with value " + nc);
                    }
                } else if (paramName.equals("response")) {
                    if (paramValue.length() > 2) {
                        resp = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Response param with value " + resp);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("uri")) {
                    if (paramValue.length() > 2) {
                        uri = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Digest uri param with value " + uri);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("opaque")) {
                    if (paramValue.length() > 2) {
                        opaque = paramValue.substring(1, paramValue.length() - 1);
                        if (logger.isFineEnabled()) {
                            logger.fine("Opaque param with value " + opaque);
                        }
                    } else {
                        if (logger.isFineEnabled()) {
                            logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                        }
                    }
                } else if (paramName.equals("qop")) {
                    if (paramValue.charAt(0) == '"') {
                        if (paramValue.length() > 2) {
                            qop = paramValue.substring(1, paramValue.length() - 1);
                        } else {
                            if (logger.isFineEnabled()) {
                                logger.fine("Ignoring invalid param " + paramName + " value " + paramValue);
                            }
                        }
                    } else {
                        qop = paramValue;
                    }
                    if (logger.isFineEnabled()) {
                        logger.fine("Qop param with value " + qop);
                    }
                }
            } else {
                if (logger.isFineEnabled()) {
                    logger.fine("Ignoring invalid param " + param);
                }
            }
        }
        if (username == null || realm == null || nonce == null || cnonce == null || nc == null || uri == null || resp == null || opaque == null) {
            logger.severe("A required parameter is missing in the challenge response");
            return null;
        }
        if (challengeParamGenerator.getNonce(opaque).equals(nonce)) {
            if (logger.isFineEnabled()) logger.fine("Nonce provided matches the one generated using opaque as seed");
        } else {
            if (logger.isFineEnabled()) logger.fine("Authentication failed, nonce provided doesn't match the one generated using opaque as seed");
            return null;
        }
        if (!qop.equals("auth")) {
            if (logger.isFineEnabled()) logger.fine("Authentication failed, qop value " + qop + " unsupported");
            return null;
        }
        UserProfile userProfile = getUserProfileControlSbb().find(username);
        if (userProfile == null) {
            if (logger.isFineEnabled()) logger.fine("Authentication failed, profile not found for user " + username);
            return null;
        } else {
            password = userProfile.getPassword();
        }
        final String digest = new RFC2617AuthQopDigest(username, realm, password, nonce, nc, cnonce, request.getMethod().toUpperCase(), uri).digest();
        if (digest != null && digest.equals(resp)) {
            if (logger.isFineEnabled()) logger.fine("authentication response is matching");
            String params = "cnonce=\"" + cnonce + "\", nc=" + nc + ", qop=" + qop + ", rspauth=\"" + digest + "\"";
            response.addHeader("Authentication-Info", params);
            return username;
        } else {
            if (logger.isFineEnabled()) logger.fine("authentication response digest received (" + resp + ") didn't match the one calculated (" + digest + ")");
            return null;
        }
    }

    /**
	 * Get the authentication scheme
	 * 
	 * @return the scheme name
	 */
    public String getScheme() {
        return "Digest";
    }

    /**
	 * get the authentication realm
	 * 
	 * @return the realm name
	 */
    public String getRealm() {
        return CONFIGURATION.getAuthenticationRealm();
    }

    public abstract ChildRelationExt getUserProfileControlChildRelation();

    protected UserProfileControlSbbLocalObject getUserProfileControlSbb() {
        try {
            return (UserProfileControlSbbLocalObject) getUserProfileControlChildRelation().create(ChildRelationExt.DEFAULT_CHILD_NAME);
        } catch (Exception e) {
            logger.severe("Failed to create child sbb", e);
            return null;
        }
    }

    public void setSbbContext(SbbContext context) {
        sbbContext = context;
        if (logger == null) {
            logger = sbbContext.getTracer(this.getClass().getSimpleName());
        }
    }

    public void unsetSbbContext() {
    }

    public void sbbCreate() throws javax.slee.CreateException {
    }

    public void sbbPostCreate() throws javax.slee.CreateException {
    }

    public void sbbActivate() {
    }

    public void sbbPassivate() {
    }

    public void sbbRemove() {
    }

    public void sbbLoad() {
    }

    public void sbbStore() {
    }

    public void sbbExceptionThrown(Exception exception, Object event, ActivityContextInterface activity) {
    }

    public void sbbRolledBack(RolledBackContext context) {
    }

    protected SbbContext getSbbContext() {
        return sbbContext;
    }

    private SbbContext sbbContext;
}
