package com.luzan.common.httprpc;

import org.apache.log4j.Logger;
import org.apache.commons.codec.binary.Base64;
import java.util.StringTokenizer;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.io.IOException;

/**
 * WSSEToken
 *
 * @author Alexander Bondar
 */
public class WSSEToken {

    private static final Logger logger = Logger.getLogger(WSSEToken.class);

    String username;

    String passwordDigest;

    String created;

    String nonce;

    public WSSEToken(String authToken) throws IOException {
        StringTokenizer st = new StringTokenizer(authToken, ", ");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.toLowerCase().startsWith("username=")) {
                username = trimDoubleQuotes(token.substring("username=".length()));
            } else if (token.toLowerCase().startsWith("passworddigest=")) {
                String passwordEncoded = trimDoubleQuotes(token.substring("passworddigest=".length()));
                passwordDigest = new String(Base64.decodeBase64(passwordEncoded.getBytes()));
            } else if (token.toLowerCase().startsWith("created=")) {
                created = trimDoubleQuotes(token.substring("created=".length()));
            } else if (token.toLowerCase().startsWith("nonce=")) {
                String nonceEncoded = trimDoubleQuotes(token.substring("nonce=".length()));
                nonce = new String(Base64.decodeBase64(nonceEncoded.getBytes()));
            }
        }
    }

    public UserProfileImpl authenticate(UserAccessorImpl psswAccessor) {
        if (username == null || username.length() == 0 || passwordDigest == null || passwordDigest.length() == 0) return null;
        UserProfileImpl user = psswAccessor.getUserByPassword(username);
        if (user == null || user.getPassword() == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            String pswdStr = nonce + created + user.getPassword();
            String userPasswordDigest = new String(md.digest(pswdStr.getBytes()));
            if (userPasswordDigest.equalsIgnoreCase(passwordDigest)) return user;
        } catch (NoSuchAlgorithmException x) {
            logger.error("Unable to authenticate. SHA-1 algorithm is not present", x);
        }
        return null;
    }

    private String trimDoubleQuotes(String str) {
        if (str == null) return null;
        int len = str.length();
        if (len < 2) return str;
        int bi = (str.charAt(0) == '\"') ? 1 : 0;
        int ei = (str.charAt(len - 1) == '\"') ? len - 1 : len;
        return str.substring(bi, ei);
    }

    public String toString() {
        StringBuffer str = new StringBuffer().append("Username = ").append(username).append(" Password Digest").append(passwordDigest).append(" Created = ").append(created).append(" Nonce = ").append(nonce);
        return str.toString();
    }
}
