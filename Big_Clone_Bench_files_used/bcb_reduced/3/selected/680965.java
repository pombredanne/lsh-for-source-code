package android.net.http;

import android.net.ParseException;
import android.net.WebAddress;
import android.security.Md5MessageDigest;
import junit.framework.Assert;
import android.webkit.CookieManager;
import org.apache.commons.codec.binary.Base64;
import java.io.InputStream;
import java.lang.Math;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * RequestHandle: handles a request session that may include multiple
 * redirects, HTTP authentication requests, etc.
 * 
 * {@hide}
 */
public class RequestHandle {

    private String mUrl;

    private WebAddress mUri;

    private String mMethod;

    private Map<String, String> mHeaders;

    private RequestQueue mRequestQueue;

    private Request mRequest;

    private InputStream mBodyProvider;

    private int mBodyLength;

    private int mRedirectCount = 0;

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String PROXY_AUTHORIZATION_HEADER = "Proxy-Authorization";

    public static final int MAX_REDIRECT_COUNT = 16;

    /**
     * Creates a new request session.
     */
    public RequestHandle(RequestQueue requestQueue, String url, WebAddress uri, String method, Map<String, String> headers, InputStream bodyProvider, int bodyLength, Request request) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        mHeaders = headers;
        mBodyProvider = bodyProvider;
        mBodyLength = bodyLength;
        mMethod = method == null ? "GET" : method;
        mUrl = url;
        mUri = uri;
        mRequestQueue = requestQueue;
        mRequest = request;
    }

    /**
     * Cancels this request
     */
    public void cancel() {
        if (mRequest != null) {
            mRequest.cancel();
        }
    }

    /**
     * Handles SSL error(s) on the way down from the user (the user
     * has already provided their feedback).
     */
    public void handleSslErrorResponse(boolean proceed) {
        if (mRequest != null) {
            mRequest.handleSslErrorResponse(proceed);
        }
    }

    /**
     * @return true if we've hit the max redirect count
     */
    public boolean isRedirectMax() {
        return mRedirectCount >= MAX_REDIRECT_COUNT;
    }

    public int getRedirectCount() {
        return mRedirectCount;
    }

    public void setRedirectCount(int count) {
        mRedirectCount = count;
    }

    /**
     * Create and queue a redirect request.
     *
     * @param redirectTo URL to redirect to
     * @param statusCode HTTP status code returned from original request
     * @param cacheHeaders Cache header for redirect URL
     * @return true if setup succeeds, false otherwise (redirect loop
     * count exceeded, body provider unable to rewind on 307 redirect)
     */
    public boolean setupRedirect(String redirectTo, int statusCode, Map<String, String> cacheHeaders) {
        if (HttpLog.LOGV) {
            HttpLog.v("RequestHandle.setupRedirect(): redirectCount " + mRedirectCount);
        }
        mHeaders.remove(AUTHORIZATION_HEADER);
        mHeaders.remove(PROXY_AUTHORIZATION_HEADER);
        if (++mRedirectCount == MAX_REDIRECT_COUNT) {
            if (HttpLog.LOGV) HttpLog.v("RequestHandle.setupRedirect(): too many redirects " + mRequest);
            mRequest.error(EventHandler.ERROR_REDIRECT_LOOP, com.android.internal.R.string.httpErrorRedirectLoop);
            return false;
        }
        if (mUrl.startsWith("https:") && redirectTo.startsWith("http:")) {
            if (HttpLog.LOGV) {
                HttpLog.v("blowing away the referer on an https -> http redirect");
            }
            mHeaders.remove("Referer");
        }
        mUrl = redirectTo;
        try {
            mUri = new WebAddress(mUrl);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        mHeaders.remove("Cookie");
        String cookie = CookieManager.getInstance().getCookie(mUri);
        if (cookie != null && cookie.length() > 0) {
            mHeaders.put("Cookie", cookie);
        }
        if ((statusCode == 302 || statusCode == 303) && mMethod.equals("POST")) {
            if (HttpLog.LOGV) {
                HttpLog.v("replacing POST with GET on redirect to " + redirectTo);
            }
            mMethod = "GET";
        }
        if (statusCode == 307) {
            try {
                if (mBodyProvider != null) mBodyProvider.reset();
            } catch (java.io.IOException ex) {
                if (HttpLog.LOGV) {
                    HttpLog.v("setupAuthResponse() failed to reset body provider");
                }
                return false;
            }
        } else {
            mHeaders.remove("Content-Type");
            mBodyProvider = null;
        }
        mHeaders.putAll(cacheHeaders);
        createAndQueueNewRequest();
        return true;
    }

    /**
     * Create and queue an HTTP authentication-response (basic) request.
     */
    public void setupBasicAuthResponse(boolean isProxy, String username, String password) {
        String response = computeBasicAuthResponse(username, password);
        if (HttpLog.LOGV) {
            HttpLog.v("setupBasicAuthResponse(): response: " + response);
        }
        mHeaders.put(authorizationHeader(isProxy), "Basic " + response);
        setupAuthResponse();
    }

    /**
     * Create and queue an HTTP authentication-response (digest) request.
     */
    public void setupDigestAuthResponse(boolean isProxy, String username, String password, String realm, String nonce, String QOP, String algorithm, String opaque) {
        String response = computeDigestAuthResponse(username, password, realm, nonce, QOP, algorithm, opaque);
        if (HttpLog.LOGV) {
            HttpLog.v("setupDigestAuthResponse(): response: " + response);
        }
        mHeaders.put(authorizationHeader(isProxy), "Digest " + response);
        setupAuthResponse();
    }

    private void setupAuthResponse() {
        try {
            if (mBodyProvider != null) mBodyProvider.reset();
        } catch (java.io.IOException ex) {
            if (HttpLog.LOGV) {
                HttpLog.v("setupAuthResponse() failed to reset body provider");
            }
        }
        createAndQueueNewRequest();
    }

    /**
     * @return HTTP request method (GET, PUT, etc).
     */
    public String getMethod() {
        return mMethod;
    }

    /**
     * @return Basic-scheme authentication response: BASE64(username:password).
     */
    public static String computeBasicAuthResponse(String username, String password) {
        Assert.assertNotNull(username);
        Assert.assertNotNull(password);
        return new String(Base64.encodeBase64((username + ':' + password).getBytes()));
    }

    public void waitUntilComplete() {
        mRequest.waitUntilComplete();
    }

    /**
     * @return Digest-scheme authentication response.
     */
    private String computeDigestAuthResponse(String username, String password, String realm, String nonce, String QOP, String algorithm, String opaque) {
        Assert.assertNotNull(username);
        Assert.assertNotNull(password);
        Assert.assertNotNull(realm);
        String A1 = username + ":" + realm + ":" + password;
        String A2 = mMethod + ":" + mUrl;
        String nc = "000001";
        String cnonce = computeCnonce();
        String digest = computeDigest(A1, A2, nonce, QOP, nc, cnonce);
        String response = "";
        response += "username=" + doubleQuote(username) + ", ";
        response += "realm=" + doubleQuote(realm) + ", ";
        response += "nonce=" + doubleQuote(nonce) + ", ";
        response += "uri=" + doubleQuote(mUrl) + ", ";
        response += "response=" + doubleQuote(digest);
        if (opaque != null) {
            response += ", opaque=" + doubleQuote(opaque);
        }
        if (algorithm != null) {
            response += ", algorithm=" + algorithm;
        }
        if (QOP != null) {
            response += ", qop=" + QOP + ", nc=" + nc + ", cnonce=" + doubleQuote(cnonce);
        }
        return response;
    }

    /**
     * @return The right authorization header (dependeing on whether it is a proxy or not).
     */
    public static String authorizationHeader(boolean isProxy) {
        if (!isProxy) {
            return AUTHORIZATION_HEADER;
        } else {
            return PROXY_AUTHORIZATION_HEADER;
        }
    }

    /**
     * @return Double-quoted MD5 digest.
     */
    private String computeDigest(String A1, String A2, String nonce, String QOP, String nc, String cnonce) {
        if (HttpLog.LOGV) {
            HttpLog.v("computeDigest(): QOP: " + QOP);
        }
        if (QOP == null) {
            return KD(H(A1), nonce + ":" + H(A2));
        } else {
            if (QOP.equalsIgnoreCase("auth")) {
                return KD(H(A1), nonce + ":" + nc + ":" + cnonce + ":" + QOP + ":" + H(A2));
            }
        }
        return null;
    }

    /**
     * @return MD5 hash of concat(secret, ":", data).
     */
    private String KD(String secret, String data) {
        return H(secret + ":" + data);
    }

    /**
     * @return MD5 hash of param.
     */
    private String H(String param) {
        if (param != null) {
            Md5MessageDigest md5 = new Md5MessageDigest();
            byte[] d = md5.digest(param.getBytes());
            if (d != null) {
                return bufferToHex(d);
            }
        }
        return null;
    }

    /**
     * @return HEX buffer representation.
     */
    private String bufferToHex(byte[] buffer) {
        final char hexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        if (buffer != null) {
            int length = buffer.length;
            if (length > 0) {
                StringBuilder hex = new StringBuilder(2 * length);
                for (int i = 0; i < length; ++i) {
                    byte l = (byte) (buffer[i] & 0x0F);
                    byte h = (byte) ((buffer[i] & 0xF0) >> 4);
                    hex.append(hexChars[h]);
                    hex.append(hexChars[l]);
                }
                return hex.toString();
            } else {
                return "";
            }
        }
        return null;
    }

    /**
     * Computes a random cnonce value based on the current time.
     */
    private String computeCnonce() {
        Random rand = new Random();
        int nextInt = rand.nextInt();
        nextInt = (nextInt == Integer.MIN_VALUE) ? Integer.MAX_VALUE : Math.abs(nextInt);
        return Integer.toString(nextInt, 16);
    }

    /**
     * "Double-quotes" the argument.
     */
    private String doubleQuote(String param) {
        if (param != null) {
            return "\"" + param + "\"";
        }
        return null;
    }

    /**
     * Creates and queues new request.
     */
    private void createAndQueueNewRequest() {
        mRequest = mRequestQueue.queueRequest(mUrl, mUri, mMethod, mHeaders, mRequest.mEventHandler, mBodyProvider, mBodyLength).mRequest;
    }
}
