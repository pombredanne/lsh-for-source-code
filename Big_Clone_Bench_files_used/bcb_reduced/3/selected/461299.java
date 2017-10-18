package org.jtools.util.auth.digest;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public final class DigestUtils {

    public static final String HEADER_AUTHORIZATION = "Authorization";

    public static final String HEADER_AUTHENTICATION_INFO = "Authentication-Info";

    public static final String HEADER_AUTHENTICATE = "WWW-Authenticate";

    public static final byte BYTE_AUTHORIZATION = 0x71;

    public static final byte BYTE_AUTHENTICATION_INFO = 0x72;

    public static final byte BYTE_AUTHENTICATE = 0x74;

    public static final String NONHTTPMETHOD = "Plain";

    private static final char hexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final SecureRandom defaultRandom = new SecureRandom();

    public static String digest_challenge(String realm, String domain, String nonce, String opaque, boolean stale, Algorithm algorithm, Qop[] qop) {
        return new Challenge(realm, domain, nonce, opaque, stale, algorithm, qop).toString();
    }

    public static boolean authenticate(Authorization authorization, String secret, String entityBody) throws IOException {
        return authorization.getResponse().equals(rfc2617_Digest(secret, authorization, entityBody, true));
    }

    public static String authentication_info(Authorization authorization, String secret, String entityBody, String nextnonce) throws IOException {
        String nn = nextnonce == null ? authorization.getNonce() : nextnonce;
        AuthenticationInfo info = authorization.getQop() != null ? new AuthenticationInfo(nn, authorization.getQop(), rfc2617_Digest(secret, authorization, entityBody, false), authorization.getCnonce(), authorization.getNc()) : new AuthenticationInfo(nn, null, null, null, null);
        return info.toString();
    }

    public static Authorization authorization(URL url, String challengeStr) throws Exception {
        Challenge challenge = new Challenge(challengeStr);
        PasswordAuthentication passwordAuthentication = Authenticator.requestPasswordAuthentication(url == null ? null : url.getHost(), null, url == null ? 0 : url.getPort(), null, challenge.getRealm(), url == null ? null : url.getProtocol());
        if (passwordAuthentication == null) return null;
        String secret = toSecret(challenge.getAlgorithm().getDigest(), passwordAuthentication.getUserName(), challenge.getRealm(), new String(passwordAuthentication.getPassword()));
        return new Authorization(challenge, passwordAuthentication.getUserName(), secret, url == null ? null : url.toURI().toString(), newNonce(null));
    }

    public static boolean auth(Authorization authorization, String authInfoStr) throws Exception {
        AuthenticationInfo authInfo = new AuthenticationInfo(authInfoStr);
        if (authorization.getQop() == null) return true;
        String shouldBe = rfc2617_Digest(authorization.getSecret(), authorization, null, false);
        return shouldBe.equals(authInfo.getRspauth());
    }

    public static final String toSecret(String digest, String user, String realm, String password) throws IOException {
        return rfc2617_H(digest, user + ":" + realm + ":" + password);
    }

    private static String getSecret(String plainSecrect, Authorization params) throws IOException {
        String non_sess_secret = plainSecrect;
        if (Algorithm.MD5sess.equals(params.getAlgorithm())) return rfc2617_H(params.getAlgorithm().getDigest(), non_sess_secret + ":" + params.getNonce() + ":" + params.getCnonce());
        return non_sess_secret;
    }

    static String rfc2617_Digest(String plainSecret, Authorization params, String entityBody, boolean request) throws IOException {
        String A2 = rfc2617_A2(params, entityBody, request);
        String HA2 = rfc2617_H(params.getAlgorithm().getDigest(), A2);
        String secret = getSecret(plainSecret, params);
        String data;
        if (params.getQop() != null) data = params.getNonce() + ":" + params.getNc() + ":" + params.getCnonce() + ":" + params.getQop() + ":" + HA2; else data = params.getNonce() + ":" + HA2;
        String result = rfc2617_KD(params.getAlgorithm().getDigest(), secret, data);
        return result;
    }

    private static String rfc2617_A2(Authorization params, String entityBody, boolean request) throws IOException {
        if (Qop.auth_int.equals(params.getQop())) return (request ? params.getMethod() : "") + ":" + params.getUri() + ":" + rfc2617_H(params.getAlgorithm().getDigest(), entityBody);
        return (request ? params.getMethod() : "") + ":" + params.getUri();
    }

    public static String newNonce(Random random) {
        if (random == null) random = defaultRandom;
        byte bytes[] = new byte[20];
        char ac[] = new char[40];
        synchronized (random) {
            random.nextBytes(bytes);
        }
        for (int i = 0; i < 20; i++) {
            int j = bytes[i] + 128;
            ac[i * 2] = (char) (65 + j / 16);
            ac[i * 2 + 1] = (char) (65 + j % 16);
        }
        return new String(ac, 0, 40);
    }

    private static String md5(String digest, String data) throws IOException {
        MessageDigest messagedigest;
        try {
            messagedigest = MessageDigest.getInstance(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        messagedigest.update(data.getBytes("ISO-8859-1"));
        byte[] bytes = messagedigest.digest();
        StringBuilder stringbuffer = new StringBuilder(bytes.length * 2);
        for (int j = 0; j < bytes.length; j++) {
            int k = bytes[j] >>> 4 & 0x0f;
            stringbuffer.append(hexChars[k]);
            k = bytes[j] & 0x0f;
            stringbuffer.append(hexChars[k]);
        }
        return stringbuffer.toString();
    }

    private static String rfc2617_H(String digest, String data) throws IOException {
        return md5(digest, data);
    }

    private static String rfc2617_KD(String digest, String secret, String data) throws IOException {
        return md5(digest, new StringBuilder(secret.length() + data.length() + 1).append(secret).append(':').append(data).toString());
    }
}
