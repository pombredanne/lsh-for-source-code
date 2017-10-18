package scam.webdav.util;

import scam.FatalException;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Implements Digest HTTP Access Authentication according to RFC2617.
 *
 * @author J�ran Stark
 * @author Jan Danils
 * @version $Revision: 1.1.1.1 $
 */
public class MD5Digest {

    /**
     * Calculates the session key.
     * @param algortim the algortithm to be used. 
     * @param username the name of the principal to be verfied.
     * @param realm Context identifier for username and password.
     * @param password password associated with the username.
     * @param nonce serverside chalenge sent to the client. 
     * @param cnonce clientside chalenge response.
     * @return a session key. This value can be calculated once per session and
     * resused for each subsequent request.
     * @exception FatalException is thrown if an internal error ocurres.
     * @exception MD5DigestException is thrown if the given values is incocrrect.
     */
    public static String calcHA1(String algorithm, String username, String realm, String password, String nonce, String cnonce) throws FatalException, MD5DigestException {
        MD5Encoder encoder = new MD5Encoder();
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new FatalException(e);
        }
        if (username == null || realm == null) {
            throw new MD5DigestException(WebdavStatus.SC_BAD_REQUEST, "username or realm");
        }
        if (password == null) {
            System.err.println("No password has been provided");
            throw new IllegalStateException();
        }
        if (algorithm != null && algorithm.equals("MD5-sess") && (nonce == null || cnonce == null)) {
            throw new MD5DigestException(WebdavStatus.SC_BAD_REQUEST, "nonce or cnonce");
        }
        md5.update((username + ":" + realm + ":" + password).getBytes());
        if (algorithm != null && algorithm.equals("MD5-sess")) {
            md5.update((":" + nonce + ":" + cnonce).getBytes());
        }
        return encoder.encode(md5.digest());
    }

    /**
     * Calculates the digest of the response. This value should correspond with
     * the provided repsponse digest value from the clilent. If they do not
     * correspond some of the values may been manipulated during transmission.
     * @param ha1 the session key.
     * @param nonce serverside chalenge sent to the client. 
     * @param cnonce clientside chalenge response.
     * @param qop quality of protection.
     * @param method the name of the requested method.
     * @param uri the URI that is requested.
     * @return the calculated digest of the response.
     * @exception FatalException is thrown if an internal error ocurres.
     * @exception MD5DigestException is thrown if the given values is incocrrect.
     */
    public static String calcResponse(String ha1, String nonce, String nonceCount, String cnonce, String qop, String method, String uri) throws FatalException, MD5DigestException {
        MD5Encoder encoder = new MD5Encoder();
        String ha2 = null;
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new FatalException(e);
        }
        if (method == null || uri == null) {
            throw new MD5DigestException(WebdavStatus.SC_BAD_REQUEST, "method or uri");
        }
        if (qop != null && qop.equals("auth-int")) {
            throw new MD5DigestException(WebdavStatus.SC_UNSUPPORTED_MEDIA_TYPE);
        }
        if (nonce == null) {
            throw new MD5DigestException(WebdavStatus.SC_BAD_REQUEST, "nonce");
        }
        if (qop != null && (qop.equals("auth") || qop.equals("auth-int"))) {
            if (nonceCount == null || cnonce == null) {
                throw new MD5DigestException(WebdavStatus.SC_BAD_REQUEST, "nc or cnonce");
            }
        }
        md5.update((method + ":" + uri).getBytes());
        ha2 = encoder.encode(md5.digest());
        md5.update((ha1 + ":" + nonce + ":").getBytes());
        if (qop != null && (qop.equals("auth") || qop.equals("auth-int"))) {
            md5.update((nonceCount + ":" + cnonce + ":" + qop + ":").getBytes());
        }
        md5.update(ha2.getBytes());
        String response = encoder.encode(md5.digest());
        return response;
    }

    public static void main(String argv[]) {
        String ha1, response;
        String nonce = "scam_nonce";
        String cnonce = "RnJpLCAxOSBPY3QgMjAwMSAwOToyOToxNSBHTVQ=";
        String user = "jand";
        String realm = "ulldb";
        String pass = "jand";
        String alg = "MD5";
        String nonceCount = "00000001";
        String method = "MKCOL";
        String qop = "auth";
        String uri = "/scam2/users/jand/private/";
        try {
            ha1 = calcHA1(alg, user, realm, pass, nonce, cnonce);
            response = calcResponse(ha1, nonce, nonceCount, cnonce, qop, method, uri);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
