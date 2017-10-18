package org.exist.http.servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.exist.security.MessageDigester;
import org.exist.security.SecurityManager;
import org.exist.security.Subject;
import org.exist.security.internal.AccountImpl;
import org.exist.security.internal.SubjectAccreditedImpl;
import org.exist.storage.BrokerPool;

/**
 * An Authenticator that uses MD5 Digest Authentication.
 * 
 * @author wolf
 */
public class DigestAuthenticator implements Authenticator {

    private BrokerPool pool;

    public DigestAuthenticator(BrokerPool pool) {
        this.pool = pool;
    }

    public Subject authenticate(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return authenticate(request, response, true);
    }

    @Override
    public Subject authenticate(HttpServletRequest request, HttpServletResponse response, boolean sendChallenge) throws IOException {
        String credentials = request.getHeader("Authorization");
        if (credentials == null) {
            sendChallenge(request, response);
            return null;
        }
        Digest digest = new Digest(request.getMethod());
        parseCredentials(digest, credentials);
        SecurityManager secman = pool.getSecurityManager();
        AccountImpl user = (AccountImpl) secman.getAccount(null, digest.username);
        if (user == null) {
            if (sendChallenge) sendChallenge(request, response);
            return null;
        }
        if (!digest.check(user.getDigestPassword())) {
            if (sendChallenge) sendChallenge(request, response);
            return null;
        }
        return new SubjectAccreditedImpl(user, this);
    }

    @Override
    public void sendChallenge(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("WWW-Authenticate", "Digest realm=\"exist\", " + "nonce=\"" + createNonce(request) + "\", " + "domain=\"" + request.getContextPath() + "\", " + "opaque=\"" + MessageDigester.md5(Integer.toString(hashCode(), 27), false) + '"');
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private String createNonce(HttpServletRequest request) {
        return MessageDigester.md5(request.getRemoteAddr() + ':' + Long.toString(System.currentTimeMillis()) + ':' + Integer.toString(hashCode()), false);
    }

    private static void parseCredentials(Digest digest, String credentials) {
        credentials = credentials.substring("Digest ".length());
        StringBuilder current = new StringBuilder();
        String name = null, value;
        boolean inQuotedString = false;
        for (int i = 0; i < credentials.length(); i++) {
            char ch = credentials.charAt(i);
            switch(ch) {
                case ' ':
                    break;
                case '"':
                case '\'':
                    if (inQuotedString) {
                        value = current.toString();
                        current.setLength(0);
                        inQuotedString = false;
                        if ("username".equalsIgnoreCase(name)) digest.username = value; else if ("realm".equalsIgnoreCase(name)) digest.realm = value; else if ("nonce".equalsIgnoreCase(name)) digest.nonce = value; else if ("uri".equalsIgnoreCase(name)) digest.uri = value; else if ("response".equalsIgnoreCase(name)) digest.response = value;
                    } else {
                        value = null;
                        inQuotedString = true;
                    }
                    break;
                case ',':
                    name = null;
                    break;
                case '=':
                    name = current.toString();
                    current.setLength(0);
                    break;
                default:
                    current.append(ch);
                    break;
            }
        }
    }

    private static class Digest {

        String method = null;

        String username = null;

        @SuppressWarnings("unused")
        String realm = null;

        String nonce = null;

        String uri = null;

        String response = null;

        public Digest(String method) {
            this.method = method;
        }

        public boolean check(String credentials) throws IOException {
            if (credentials == null) return true;
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                md.reset();
                md.update(method.getBytes("ISO-8859-1"));
                md.update((byte) ':');
                md.update(uri.getBytes("ISO-8859-1"));
                byte[] ha2 = md.digest();
                md.update(credentials.getBytes("ISO-8859-1"));
                md.update((byte) ':');
                md.update(nonce.getBytes("ISO-8859-1"));
                md.update((byte) ':');
                md.update(MessageDigester.byteArrayToHex(ha2).getBytes("ISO-8859-1"));
                byte[] digest = md.digest();
                return (MessageDigester.byteArrayToHex(digest).equalsIgnoreCase(response));
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("MD5 not supported");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Encoding not supported");
            }
        }
    }
}
