package com.sun.mail.smtp;

import java.io.*;
import java.util.*;
import java.security.*;
import com.sun.mail.util.*;

/**
 * DIGEST-MD5 authentication support.
 *
 * @author Dean Gibson
 * @author Bill Shannon
 */
public class DigestMD5 {

    private PrintStream debugout;

    private MessageDigest md5;

    private String uri;

    private String clientResponse;

    public DigestMD5(PrintStream debugout) {
        this.debugout = debugout;
        if (debugout != null) debugout.println("DEBUG DIGEST-MD5: Loaded");
    }

    /**
     * Return client's authentication response to server's challenge.
     *
     * @return byte array with client's response
     */
    public byte[] authClient(String host, String user, String passwd, String realm, String serverChallenge) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream b64os = new BASE64EncoderStream(bos, Integer.MAX_VALUE);
        SecureRandom random;
        try {
            random = new SecureRandom();
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            if (debugout != null) debugout.println("DEBUG DIGEST-MD5: " + ex);
            throw new IOException(ex.toString());
        }
        StringBuffer result = new StringBuffer();
        uri = "smtp/" + host;
        String nc = "00000001";
        String qop = "auth";
        byte[] bytes = new byte[32];
        int resp;
        if (debugout != null) debugout.println("DEBUG DIGEST-MD5: Begin authentication ...");
        Hashtable map = tokenize(serverChallenge);
        if (realm == null) {
            String text = (String) map.get("realm");
            realm = text != null ? new StringTokenizer(text, ",").nextToken() : host;
        }
        String nonce = (String) map.get("nonce");
        random.nextBytes(bytes);
        b64os.write(bytes);
        b64os.flush();
        String cnonce = bos.toString();
        bos.reset();
        md5.update(md5.digest(ASCIIUtility.getBytes(user + ":" + realm + ":" + passwd)));
        md5.update(ASCIIUtility.getBytes(":" + nonce + ":" + cnonce));
        clientResponse = toHex(md5.digest()) + ":" + nonce + ":" + nc + ":" + cnonce + ":" + qop + ":";
        md5.update(ASCIIUtility.getBytes("AUTHENTICATE:" + uri));
        md5.update(ASCIIUtility.getBytes(clientResponse + toHex(md5.digest())));
        result.append("username=\"" + user + "\"");
        result.append(",realm=\"" + realm + "\"");
        result.append(",qop=" + qop);
        result.append(",nc=" + nc);
        result.append(",nonce=\"" + nonce + "\"");
        result.append(",cnonce=\"" + cnonce + "\"");
        result.append(",digest-uri=\"" + uri + "\"");
        result.append(",response=" + toHex(md5.digest()));
        if (debugout != null) debugout.println("DEBUG DIGEST-MD5: Response => " + result.toString());
        b64os.write(ASCIIUtility.getBytes(result.toString()));
        b64os.flush();
        return bos.toByteArray();
    }

    /**
     * Allow the client to authenticate the server based on its
     * response.
     *
     * @return	true if server is authenticated
     */
    public boolean authServer(String serverResponse) throws IOException {
        Hashtable map = tokenize(serverResponse);
        md5.update(ASCIIUtility.getBytes(":" + uri));
        md5.update(ASCIIUtility.getBytes(clientResponse + toHex(md5.digest())));
        String text = toHex(md5.digest());
        if (!text.equals((String) map.get("rspauth"))) {
            if (debugout != null) debugout.println("DEBUG DIGEST-MD5: " + "Expected => rspauth=" + text);
            return false;
        }
        return true;
    }

    /**
     * Tokenize a response from the server.
     *
     * @return	Hashtable containing key/value pairs from server
     */
    private Hashtable tokenize(String serverResponse) throws IOException {
        Hashtable map = new Hashtable();
        byte[] bytes = serverResponse.getBytes();
        String key = null;
        int ttype;
        StreamTokenizer tokens = new StreamTokenizer(new InputStreamReader(new BASE64DecoderStream(new ByteArrayInputStream(bytes, 4, bytes.length - 4))));
        tokens.ordinaryChars('0', '9');
        tokens.wordChars('0', '9');
        while ((ttype = tokens.nextToken()) != StreamTokenizer.TT_EOF) {
            switch(ttype) {
                case StreamTokenizer.TT_WORD:
                    if (key == null) {
                        key = tokens.sval;
                        break;
                    }
                case '"':
                    if (debugout != null) debugout.println("DEBUG DIGEST-MD5: Received => " + key + "='" + tokens.sval + "'");
                    if (map.containsKey(key)) {
                        map.put(key, map.get(key) + "," + tokens.sval);
                    } else {
                        map.put(key, tokens.sval);
                    }
                    key = null;
                    break;
            }
        }
        return map;
    }

    private static char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Convert a byte array to a string of hex digits representing the bytes.
     */
    private static String toHex(byte[] bytes) {
        char[] result = new char[bytes.length * 2];
        for (int index = 0, i = 0; index < bytes.length; index++) {
            int temp = bytes[index] & 0xFF;
            result[i++] = digits[temp >> 4];
            result[i++] = digits[temp & 0xF];
        }
        return new String(result);
    }
}
