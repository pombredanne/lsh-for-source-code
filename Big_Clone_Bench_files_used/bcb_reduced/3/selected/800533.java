package com.googlecode.webduff.authentication.provider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.googlecode.webduff.WebDuffConfiguration;
import com.googlecode.webduff.WebdavStatus;
import com.googlecode.webduff.exceptions.MethodResponseError;
import com.googlecode.webduff.util.MD5Encoder;

public class DigestWebdavAuthenticationProvider implements WebdavAuthenticationProvider {

    protected String privateKey = "Homer J. Simpson";

    protected static final MD5Encoder md5Encoder = new MD5Encoder();

    private MessageDigest md5Helper;

    private String theRealm;

    public DigestWebdavAuthenticationProvider() {
        try {
            if (md5Helper == null) md5Helper = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new IllegalStateException();
        }
    }

    public void init(WebDuffConfiguration config) {
        theRealm = config.getConf().getString("realm");
    }

    public Credential getCredential(HttpServletRequest request, HttpServletResponse response) throws MethodResponseError {
        setAuthenticateHeader(request, response);
        String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.toUpperCase().startsWith("DIGEST ")) {
            Hashtable<String, String> data = new Hashtable<String, String>();
            StringTokenizer commaTokenizer = new StringTokenizer(authorizationHeader.substring(7), ",");
            while (commaTokenizer.hasMoreTokens()) {
                String currentToken = commaTokenizer.nextToken();
                int equalSign = currentToken.indexOf('=');
                if (equalSign >= 0) {
                    data.put(currentToken.substring(0, equalSign).trim(), DigestWebdavAuthenticationProvider.removeQuotes(currentToken.substring(equalSign + 1).trim()));
                }
            }
            try {
                return new DigestCredential(request, data.get("username"), data.get("realm"), data.get("response"), data.get("nonce"), data.get("nc"), data.get("cnonce"), data.get("qop"), data.get("uri"));
            } catch (Exception e) {
                throw new MethodResponseError(WebdavStatus.SC_UNAUTHORIZED);
            }
        }
        throw new MethodResponseError(WebdavStatus.SC_UNAUTHORIZED);
    }

    /**
	 * Generate a unique token. The token is generated according to the
	 * following pattern. NOnceToken = Base64 ( MD5 ( client-IP ":" time-stamp
	 * ":" private-key ) ).
	 * 
	 * @param request
	 *            HTTP Servlet request
	 */
    protected String generateNOnce(HttpServletRequest request) {
        long currentTime = System.currentTimeMillis();
        String nOnceValue = request.getRemoteAddr() + ":" + currentTime + ":" + privateKey;
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
	 * 
	 * <pre>
	 *      WWW-Authenticate    = &quot;WWW-Authenticate&quot; &quot;:&quot; &quot;Digest&quot;
	 *                            digest-challenge
	 *      digest-challenge    = 1#( realm | [ domain ] | nOnce |
	 *                  [ digest-opaque ] |[ stale ] | [ algorithm ] )
	 *      realm               = &quot;realm&quot; &quot;=&quot; realm-value
	 *      realm-value         = quoted-string
	 *      domain              = &quot;domain&quot; &quot;=&quot; &lt;&quot;&gt; 1#URI &lt;&quot;&gt;
	 *      nonce               = &quot;nonce&quot; &quot;=&quot; nonce-value
	 *      nonce-value         = quoted-string
	 *      opaque              = &quot;opaque&quot; &quot;=&quot; quoted-string
	 *      stale               = &quot;stale&quot; &quot;=&quot; ( &quot;true&quot; | &quot;false&quot; )
	 *      algorithm           = &quot;algorithm&quot; &quot;=&quot; ( &quot;MD5&quot; | token )
	 * </pre>
	 * 
	 * @param request
	 *            HTTP Servlet request
	 * @param response
	 *            HTTP Servlet response
	 * @param config
	 *            Login configuration describing how authentication should be
	 *            performed
	 * @param nOnce
	 *            nonce token
	 */
    protected void setAuthenticateHeader(HttpServletRequest request, HttpServletResponse response) {
        byte[] buffer = null;
        String nOnce = generateNOnce(request);
        synchronized (md5Helper) {
            buffer = md5Helper.digest(nOnce.getBytes());
        }
        String authenticateHeader = "Digest realm=\"" + theRealm + "\", " + "qop=\"auth\", nonce=\"" + nOnce + "\", " + "opaque=\"" + md5Encoder.encode(buffer) + "\"";
        response.setHeader("WWW-Authenticate", authenticateHeader);
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
}
