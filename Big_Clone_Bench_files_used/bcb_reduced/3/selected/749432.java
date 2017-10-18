package com.krobothsoftware.network.authorization;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.Locale;
import com.krobothsoftware.network.NetworkHelper;
import com.krobothsoftware.network.util.NetworkUtils;
import com.krobothsoftware.network.values.NameValuePair;

/**
 * The Class DigestAuthorization using the Digest Scheme.
 * 
 * @since 1.0
 * @author Kyle Kroboth
 */
public class DigestAuthorization extends Authorization {

    /**
	 */
    private String nonce;

    /**
	 */
    private String realm;

    /**
	 */
    private String algorithm;

    /**
	 */
    private String qop;

    /**
	 */
    private String charset;

    /**
	 */
    private String nc;

    /**
	 */
    private int nonceCount;

    /**
	 * Hexa values used when creating 32 character long digest in HTTP
	 * DigestScheme in case of authentication. Credit goes to Apache Commons
	 * HttpClient
	 * 
	 * @see #encode(byte[])
	 */
    private static final char[] HEXADECIMAL = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	 * Instantiates a new digest authorization.
	 * 
	 * @param username
	 *            the username
	 * @param password
	 *            the password
	 */
    public DigestAuthorization(String username, String password) {
        super(username, password);
    }

    @Override
    public void setup(HttpURLConnection urlConnection) throws IOException {
        String header = nonceUsable(urlConnection);
        if (header != null) {
            urlConnection.setRequestProperty("Authorization", header);
            return;
        }
        HttpURLConnection tmpUrlConnection = (HttpURLConnection) urlConnection.getURL().openConnection();
        try {
            tmpUrlConnection.getInputStream();
        } catch (IOException e) {
            if (tmpUrlConnection.getResponseCode() != 401) {
                throw new IOException(e);
            }
        }
        nonceCount = 1;
        realm = getHeaderValueByType("realm", tmpUrlConnection.getHeaderField("WWW-Authenticate"));
        nonce = getHeaderValueByType("nonce", tmpUrlConnection.getHeaderField("WWW-Authenticate"));
        algorithm = getHeaderValueByType("algorithm", tmpUrlConnection.getHeaderField("WWW-Authenticate"));
        qop = getHeaderValueByType("qop", tmpUrlConnection.getHeaderField("WWW-Authenticate"));
        charset = getHeaderValueByType("charset", tmpUrlConnection.getHeaderField("WWW-Authenticate"));
        urlConnection.setRequestProperty("Authorization", createHeader(urlConnection));
    }

    @Override
    public void reset() {
        nonce = null;
        realm = null;
        algorithm = null;
        qop = null;
        charset = null;
        nc = null;
        nonceCount = 0;
    }

    private String nonceUsable(HttpURLConnection urlConnection) {
        if (nonce == null) return null;
        nonceCount++;
        return createHeader(urlConnection);
    }

    private String createHeader(HttpURLConnection urlConnection) {
        MessageDigest messageDigest = null;
        StringBuffer buffer = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
            StringBuilder sb = new StringBuilder(256);
            Formatter formatter = new Formatter(sb, Locale.US);
            formatter.format("%08x", nonceCount);
            nc = sb.toString();
            if (charset == null) {
                charset = NetworkHelper.DEFAULT_ELEMENT_CHARSET;
            }
            String cnonce = createCnonce();
            buffer = new StringBuffer();
            buffer.append(username).append(":").append(realm).append(":").append(password);
            String hash1 = buffer.toString();
            hash1 = encode(messageDigest.digest(hash1.getBytes(charset)));
            buffer = new StringBuffer();
            buffer.append(urlConnection.getRequestMethod()).append(":").append(urlConnection.getURL().getPath());
            String hash2 = buffer.toString();
            hash2 = encode(messageDigest.digest(hash2.getBytes(charset)));
            buffer = new StringBuffer();
            buffer.append(hash1).append(":").append(nonce).append(":").append(nc).append(":").append(cnonce).append(":").append(qop).append(":").append(hash2);
            String response = buffer.toString();
            response = encode(messageDigest.digest(response.getBytes(charset)));
            ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("username", username));
            params.add(new NameValuePair("realm", realm));
            params.add(new NameValuePair("nonce", nonce));
            params.add(new NameValuePair("uri", urlConnection.getURL().getPath()));
            params.add(new NameValuePair("algorithm", algorithm));
            params.add(new NameValuePair("response", response));
            params.add(new NameValuePair("qop", qop));
            params.add(new NameValuePair("nc", nc));
            params.add(new NameValuePair("cnonce", cnonce));
            buffer = new StringBuffer();
            buffer.append("Digest ");
            for (NameValuePair pair : params) {
                if (!pair.getName().equalsIgnoreCase("username")) buffer.append(", ");
                if (pair.getName().equalsIgnoreCase("nc") || pair.getName().equals("qop")) {
                    buffer.append(pair.getPair());
                } else {
                    buffer.append(pair.getQuotedPair());
                }
            }
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return buffer.toString();
    }

    private String getHeaderValueByType(String type, String headerText) {
        String header = headerText.replaceFirst("Digest ", "");
        header = header.replaceFirst("Basic ", "");
        String[] values = header.split(",");
        for (String value : values) {
            if (type.equalsIgnoreCase(value.substring(0, value.indexOf("=")).trim())) {
                return NetworkUtils.trim(value.substring(value.indexOf("=") + 1), '"');
            }
        }
        return null;
    }

    /**
	 * Encodes the 128 bit (16 bytes) MD5 digest into a 32 characters long
	 * <CODE>String</CODE> according to RFC 2617. Credit goes to Apache Commons
	 * HttpClient
	 * 
	 * @param binaryData
	 *            array containing the digest
	 * @return encoded MD5, or <CODE>null</CODE> if encoding failed
	 */
    private static String encode(byte[] binaryData) {
        int n = binaryData.length;
        char[] buffer = new char[n * 2];
        for (int i = 0; i < n; i++) {
            int low = (binaryData[i] & 0x0f);
            int high = ((binaryData[i] & 0xf0) >> 4);
            buffer[i * 2] = HEXADECIMAL[high];
            buffer[(i * 2) + 1] = HEXADECIMAL[low];
        }
        return new String(buffer);
    }

    /**
	 * Creates a random cnonce value based on the current time. Credit goes to
	 * Apache Commons HttpClient
	 * 
	 * @return The cnonce value as String.
	 */
    public static String createCnonce() {
        SecureRandom rnd = new SecureRandom();
        byte[] tmp = new byte[8];
        rnd.nextBytes(tmp);
        return encode(tmp);
    }
}
