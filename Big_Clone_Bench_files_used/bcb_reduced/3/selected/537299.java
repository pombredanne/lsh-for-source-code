package com.learningmate.itunesu;

import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

/**
 * The <CODE>ITunesU</CODE> class permits the secure transmission
 * of user credentials and identity between an institution's
 * authentication and authorization system and iTunes U.<P>
 * 
 * The code in this class can be tested by
 * running it with the following commands:
 * <PRE>
 *     javac ITunesU.java
 *     java ITunesU</PRE>
 *
 * Changes to values defined in this class' main() method must
 * be made before it will succesfully communicate with iTunes U.
 *
 */
public class iTunesUUtilities extends Object {

    java.io.OutputStream out = null;

    /**
	 * Generate the HMAC-SHA256 signature of a message string, as defined in
     * <A HREF="http://www.ietf.org/rfc/rfc2104.txt">RFC 2104</A>.
     *
     * @param message The string to sign.
     * @param key The bytes of the key to sign it with.
     *
     * @return A hexadecimal representation of the signature.
     */
    public String hmacSHA256(String message, byte[] key) {
        MessageDigest sha256 = null;
        try {
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new java.lang.AssertionError(this.getClass().getName() + ".hmacSHA256(): SHA-256 algorithm not found!");
        }
        if (key.length > 64) {
            sha256.update(key);
            key = sha256.digest();
            sha256.reset();
        }
        byte block[] = new byte[64];
        for (int i = 0; i < key.length; ++i) block[i] = key[i];
        for (int i = key.length; i < block.length; ++i) block[i] = 0;
        for (int i = 0; i < 64; ++i) block[i] ^= 0x36;
        sha256.update(block);
        try {
            sha256.update(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new java.lang.AssertionError("ITunesU.hmacSH256(): UTF-8 encoding not supported!");
        }
        byte[] hash = sha256.digest();
        sha256.reset();
        for (int i = 0; i < 64; ++i) block[i] ^= (0x36 ^ 0x5c);
        sha256.update(block);
        sha256.update(hash);
        hash = sha256.digest();
        char[] hexadecimals = new char[hash.length * 2];
        for (int i = 0; i < hash.length; ++i) {
            for (int j = 0; j < 2; ++j) {
                int value = (hash[i] >> (4 - 4 * j)) & 0xf;
                char base = (value < 10) ? ('0') : ('a' - 10);
                hexadecimals[i * 2 + j] = (char) (base + value);
            }
        }
        return new String(hexadecimals);
    }

    /**
	 * Combine user credentials into an appropriately formatted string.
     *
     * @param credentials An array of credential strings. Credential
     *                    strings may contain any character but ';'
     *                    (semicolon), '\\' (backslash), and control
     *                    characters (with ASCII codes 0-31 and 127).
     *
     * @return <CODE>null</CODE> if and only if any of the credential strings 
     *         are invalid.
     */
    public String getCredentialsString(String[] credentials) {
        StringBuffer buffer = new StringBuffer();
        if (credentials != null) {
            for (int i = 0; i < credentials.length; ++i) {
                if (i > 0) buffer.append(';');
                for (int j = 0, n = credentials[i].length(); j < n; ++j) {
                    char c = credentials[i].charAt(j);
                    if (c != ';' && c != '\\' && c >= ' ' && c != 127) {
                        buffer.append(c);
                    } else {
                        return null;
                    }
                }
            }
        }
        return buffer.toString();
    }

    /**
	 * Combine user identity information into an appropriately formatted string.
     *
     * @param displayName The user's name (optional).
     * @param emailAddress The user's email address (optional).
     * @param username The user's username (optional).
     * @param userIdentifier A unique identifier for the user (optional).
     *
     * @return A non-<CODE>null</CODE> user identity string.
     */
    public String getIdentityString(String displayName, String emailAddress, String username, String userIdentifier) {
        StringBuffer buffer = new StringBuffer();
        String[] values = { displayName, emailAddress, username, userIdentifier };
        char[][] delimiters = { { '"', '"' }, { '<', '>' }, { '(', ')' }, { '[', ']' } };
        for (int i = 0; i < values.length; ++i) {
            if (values[i] != null) {
                if (buffer.length() > 0) buffer.append(' ');
                buffer.append(delimiters[i][0]);
                for (int j = 0, n = values[i].length(); j < n; ++j) {
                    char c = values[i].charAt(j);
                    if (c == delimiters[i][1] || c == '\\') buffer.append('\\');
                    buffer.append(c);
                }
                buffer.append(delimiters[i][1]);
            }
        }
        return buffer.toString();
    }

    /**
	 * Generate an iTunes U digital signature for a user's credentials
     * and identity. Signatures are usually sent to iTunes U along
     * with the credentials, identity, and a time stamp to warrant
     * to iTunes U that the credential and identity values are
     * officially sanctioned. For such uses, it will usually makes
     * more sense to use an authorization token obtained from the
     * {@link #getAuthorizationToken(java.lang.String, java.lang.String, java.util.Date, byte[])}
     * method than to use a signature directly: Authorization
     * tokens include the signature but also the credentials, identity,
     * and time stamp, and have those conveniently packaged in
     * a format that is easy to send to iTunes U over HTTPS.
     *
     * @param credentials The user's credentials string, as
     *                    obtained from getCredentialsString().
     * @param identity The user's identity string, as
     *                 obtained from getIdentityString().
     * @param time Signature time stamp.
     * @param key The bytes of your institution's iTunes U shared secret key.
     *
     * @return A hexadecimal representation of the signature.
     */
    public String getSignature(String credentials, String identity, Date time, byte[] key) {
        StringBuffer buffer = new StringBuffer();
        try {
            buffer.append("credentials=");
            buffer.append(URLEncoder.encode(credentials, "UTF-8"));
            buffer.append("&identity=");
            buffer.append(URLEncoder.encode(identity, "UTF-8"));
            buffer.append("&time=");
            buffer.append(time.getTime() / 1000);
        } catch (UnsupportedEncodingException e) {
            throw new java.lang.AssertionError("ITunesU.getSignature():  UTF-8 encoding not supported!");
        }
        String signature = this.hmacSHA256(buffer.toString(), key);
        return signature;
    }

    /**
	 * Generate and sign an authorization token that you can use to securely
     * communicate to iTunes U a user's credentials and identity. The token
     * includes all the data you need to communicate to iTunes U as well as
     * a creation time stamp and a digital signature for the data and time.
     *
     * @param credentials The user's credentials string, as
     *                    obtained from getCredentialsString().
     * @param identity The user's identity string, as
     *                 obtained from getIdentityString().
     * @param time Token time stamp. The token will only be valid from
     *             its time stamp time and for a short time thereafter
     *             (usually 90 seconds thereafter, this "transfer
     *             timeout" being configurable in the iTunes U server).
     * @param key The bytes of your institution's iTunes U shared secret key.
     *
     * @return The authorization token. The returned token will
     *         be URL-encoded and can be sent to iTunes U with
     *         a <A HREF="http://www.ietf.org/rfc/rfc1866.txt">form
     *         submission</A>. iTunes U will typically respond with
     *         HTML that should be sent to the user's browser.
     */
    public String getAuthorizationToken(String credentials, String identity, Date time, byte[] key) {
        StringBuffer buffer = new StringBuffer();
        try {
            buffer.append("credentials=");
            buffer.append(URLEncoder.encode(credentials, "UTF-8"));
            buffer.append("&identity=");
            buffer.append(URLEncoder.encode(identity, "UTF-8"));
            buffer.append("&time=");
            buffer.append(time.getTime() / 1000);
            String data = buffer.toString();
            buffer.append("&signature=");
            buffer.append(this.hmacSHA256(data, key));
        } catch (UnsupportedEncodingException e) {
            throw new java.lang.AssertionError("ITunesU.getAuthorizationToken(): " + "UTF-8 encoding not supported!");
        }
        return buffer.toString();
    }

    /**
	 * Send a request for an action to iTunes U with an authorization token. 
     *
     * @param url URL defining how to communicate with iTunes U and
     *            identifying which iTunes U action to invoke and which iTunes
     *            U page or item to apply the action to. Such URLs have a
     *            format like <CODE>[PREFIX]/[ACTION]/[DESTINATION]</CODE>,
     *            where <CODE>[PREFIX]</CODE> is a value like
     *            "https://deimos.apple.com/WebObjects/Core.woa" which defines
     *            how to communicate with iTunes U, <CODE>[ACTION]</CODE>
     *            is a value like "Browse" which identifies which iTunes U
     *            action to invoke, and <CODE>[DESTINATION]</CODE> is a value
     *            like "example.edu" which identifies which iTunes U page
     *            or item to apply the action to. The destination string
     *            "example.edu" refers to the root page of the iTunes U site
     *            identified by the domain "example.edu". Destination strings
     *            for other items within that site contain the site domain
     *            followed by numbers separated by periods. For example:
     *            "example.edu.123.456.0789". You can find these
     *            strings in the items' URLs, which you can obtain from
     *            iTunes. See the iTunes U documentation for details.
     * @param token Authorization token generated by getAuthorizationToken().
     *
     * @return The iTunes U response, which may be HTML or
     *         text depending on the type of action invoked.
     */
    public String invokeAction(String url, String token) {
        StringBuffer response = null;
        try {
            if (!url.startsWith("https")) {
                throw new MalformedURLException("ITunesU.invokeAction(): URL \"" + url + "\" does not use HTTPS.");
            }
            System.out.println("url in iTunesUUtilities " + url);
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            connection.connect();
            OutputStream output = connection.getOutputStream();
            output.write(token.getBytes("UTF-8"));
            output.flush();
            output.close();
            response = new StringBuffer();
            InputStream input = connection.getInputStream();
            Reader reader = new InputStreamReader(input, "UTF-8");
            reader = new BufferedReader(reader);
            char[] buffer = new char[16 * 1024];
            for (int n = 0; n >= 0; ) {
                n = reader.read(buffer, 0, buffer.length);
                if (n > 0) response.append(buffer, 0, n);
            }
            input.close();
            connection.disconnect();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new java.lang.AssertionError("ITunesU.invokeAction(): UTF-8 encoding not supported!");
        } catch (IOException e) {
            e.printStackTrace();
            throw new java.lang.AssertionError("ITunesU.invokeAction(): I/O Exception " + e);
        }
        return response.toString();
    }
}
