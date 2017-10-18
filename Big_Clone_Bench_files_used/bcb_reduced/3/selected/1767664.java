package com.twilio4j.twism;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.twilio4j.util.Base64;

/**
 * <p>A cookie represented by {@link CookieTwism} is used to persist state during a call. It houses a HashMap called
 * userParams. These parameters are flattened and encoded into a single string. This string is signed with a SHA1
 * hash, whose checksum is stored in the cookie along with the flattened userParams. Upon reading the cookie from
 * the HTTP headers, if it cannot be checksummed properly, then a {@link CookieTamperedException} is thrown.</p>
 * 
 * <p>You should not need to access this class</p>
 * 
 * @author broc.seib@gentomi.com
 */
public class CookieTwism {

    private static final String COOKIE_NAME = "twism";

    private String cookiePayload;

    private String validatedPayload;

    private String[] encodedFields;

    private HashMap<String, String> userParams;

    public CookieTwism(HashMap<String, String> userParams) {
        this.userParams = userParams;
    }

    private CookieTwism(String cookiePayload) {
        this.cookiePayload = cookiePayload;
    }

    public void verifyPayload(String cookieProof) throws CookieTamperedException {
        if (!cookieProof.equals(this.cookiePayload)) {
            throw new CookieTamperedException("Cookie payload does not match for verification.");
        }
    }

    private String encodeIntoCookiePayload(String SECRET_HASH_INGREDIENT) {
        StringBuilder buf = new StringBuilder();
        buf.append("|");
        if ((userParams != null) && (userParams.size() > 0)) {
            StringBuilder buf2 = new StringBuilder();
            ArrayList<String> keys = new ArrayList<String>(userParams.keySet());
            for (int i = 0; i < keys.size(); i++) {
                if (i > 0) {
                    buf2.append("&");
                }
                String k = keys.get(i);
                buf2.append(encodeUTF8(k)).append("=").append(encodeUTF8(userParams.get(k)));
            }
            buf.append(Base64.encodeToString(buf2.toString().getBytes(), false));
        }
        String mac = hexHashSHA1(buf.toString() + SECRET_HASH_INGREDIENT);
        buf.insert(0, mac);
        return buf.toString();
    }

    private void validatePayload(String SECRET_HASH_INGREDIENT) throws CookieTamperedException {
        if (this.cookiePayload == null) {
            throw new CookieTamperedException("no cookie read yet.");
        }
        int pipe1 = this.cookiePayload.indexOf("|");
        if (pipe1 < 0) {
            throw new CookieTamperedException("invalid cookie payload: " + this.cookiePayload);
        }
        String macReceived = this.cookiePayload.substring(0, pipe1);
        String macComputed = hexHashSHA1(this.cookiePayload.substring(pipe1) + SECRET_HASH_INGREDIENT);
        if (!macReceived.equals(macComputed)) {
            throw new CookieTamperedException("invalid cookie.");
        }
        this.validatedPayload = this.cookiePayload.substring(1 + pipe1);
    }

    private String getEncodedField(int index, String SECRET_HASH_INGREDIENT) throws CookieTamperedException {
        if (encodedFields == null) {
            if (validatedPayload == null) {
                validatePayload(SECRET_HASH_INGREDIENT);
            }
            this.encodedFields = validatedPayload.split("\\|");
        }
        try {
            return encodedFields[index];
        } catch (IndexOutOfBoundsException e) {
            throw new CookieTamperedException("invalid number of fields");
        }
    }

    public HashMap<String, String> recoverUserParamsFromCookiePayload(String SECRET_HASH_INGREDIENT) throws CookieTamperedException {
        String chunk = new String(Base64.decode(getEncodedField(0, SECRET_HASH_INGREDIENT).toCharArray()));
        String[] keyValPairs = chunk.split("&");
        HashMap<String, String> map = new HashMap<String, String>();
        for (String pair : keyValPairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2) {
                map.put(decodeUTF8(kv[0]), decodeUTF8(kv[1]));
            }
        }
        this.userParams = map;
        return this.userParams;
    }

    private String decodeUTF8(String val) {
        try {
            return URLDecoder.decode(val, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
    }

    private String encodeUTF8(String val) {
        try {
            return URLEncoder.encode(val, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException();
        }
    }

    private String hexHashSHA1(String phrase) {
        byte[] phraseBytes = phrase.getBytes();
        try {
            MessageDigest hasher = MessageDigest.getInstance("SHA");
            hasher.reset();
            hasher.update(phraseBytes);
            BigInteger digest = new BigInteger(1, hasher.digest());
            return digest.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static CookieTwism checkForCookie(HttpServletRequest req, String SECRET_HASH_INGREDIENT) throws CookieTamperedException {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (COOKIE_NAME.equals(c.getName())) {
                    CookieTwism me = new CookieTwism(c.getValue());
                    me.validatePayload(SECRET_HASH_INGREDIENT);
                    return me;
                }
            }
        }
        return null;
    }

    public static void removeHttpCookie(HttpServletResponse response) {
        Cookie c = new Cookie(COOKIE_NAME, "");
        c.setMaxAge(0);
        c.setPath("/");
        response.addCookie(c);
    }

    static Cookie setHttpCookie(HttpServletResponse response, CookieTwism cookie, String SECRET_HASH_INGREDIENT) {
        String payload = cookie.encodeIntoCookiePayload(SECRET_HASH_INGREDIENT);
        Cookie c = new Cookie(COOKIE_NAME, payload);
        c.setMaxAge(-1);
        c.setPath("/");
        response.addCookie(c);
        return c;
    }
}
