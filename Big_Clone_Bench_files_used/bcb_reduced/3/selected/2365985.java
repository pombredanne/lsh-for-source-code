package cgl.shindig.security;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import cgl.shindig.common.Text;

public class CryptedSimpleCredentials implements Credentials {

    private final String algorithm;

    private final String cryptedPassword;

    private final String userId;

    private final Map<String, Object> attributes;

    public CryptedSimpleCredentials(SimpleCredentials credentials) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        userId = credentials.getUserID();
        if (userId == null || userId.length() == 0) {
            throw new IllegalArgumentException();
        }
        char[] pwd = credentials.getPassword();
        if (pwd == null) {
            throw new IllegalArgumentException();
        }
        String password = new String(pwd);
        String algo = getAlgorithm(password);
        if (algo == null) {
            algorithm = SecurityConstants.DEFAULT_DIGEST;
            cryptedPassword = crypt(password, algorithm);
        } else {
            algorithm = algo;
            cryptedPassword = password;
        }
        String[] attNames = credentials.getAttributeNames();
        attributes = new HashMap<String, Object>(attNames.length);
        for (String attName : attNames) {
            attributes.put(attName, credentials.getAttribute(attName));
        }
    }

    public CryptedSimpleCredentials(String userId, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (userId == null || userId.length() == 0 || password == null) {
            throw new IllegalArgumentException("Invalid userID or password. Neither may be null, the userID must have a length > 0.");
        }
        this.userId = userId;
        String algo = getAlgorithm(password);
        if (algo == null) {
            algorithm = SecurityConstants.DEFAULT_DIGEST;
            cryptedPassword = crypt(password, algorithm);
        } else {
            algorithm = algo;
            cryptedPassword = password;
        }
        attributes = Collections.emptyMap();
    }

    public String getUserID() {
        return userId;
    }

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public String[] getAttributeNames() {
        return attributes.keySet().toArray(new String[attributes.size()]);
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getPassword() {
        return cryptedPassword;
    }

    /**
     * Compair this instance with an instance of SimpleCredentials.
     * If one the other Credentials' Password is plain-text treies to encode
     * it with the current Digest.
     *
     * @param credentials
     * @return true if {@link SimpleCredentials#getUserID() UserID} and
     * {@link SimpleCredentials#getPassword() Password} match.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public boolean matches(SimpleCredentials credentials) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        if (getUserID().matches(credentials.getUserID())) {
            String toMatch = new String(credentials.getPassword());
            String algr = getAlgorithm(toMatch);
            if (algr != null) {
                return false;
            }
            if (algr == null && algorithm != null) {
                return crypt(toMatch, algorithm).equals(cryptedPassword);
            }
            return toMatch.equals(cryptedPassword);
        }
        return false;
    }

    private static String crypt(String pwd, String algorithm) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        StringBuffer password = new StringBuffer();
        password.append("{").append(algorithm).append("}");
        password.append(Text.digest(algorithm, pwd.getBytes("UTF-8")));
        return password.toString();
    }

    private static String getAlgorithm(String password) {
        int end = password.indexOf("}");
        if (password.startsWith("{") && end > 0) {
            return password.substring(1, end);
        } else {
            return null;
        }
    }
}
