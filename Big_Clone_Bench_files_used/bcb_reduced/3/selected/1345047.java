package org.echarts.jain.sip;

import java.security.*;

/**
 * Modified from original source in the public domain NIST JAIN SIP stack source.
 * src/examples/authorization/DigestClientAuthenticationMethod.java
 */
public class DigestClientAuthenticationMethod {

    private String realm;

    private String userName;

    private String uri;

    private String nonce;

    private String password;

    private String method;

    private String cnonce;

    private String nonce_count;

    private String qop_value;

    private MessageDigest messageDigest;

    /**
     * to hex converter
     */
    private static final char[] toHex = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * convert an array of bytes to an hexadecimal string
     * @return a string
     * @param b bytes array to convert to a hexadecimal
     * string
     */
    public static String toHexString(byte b[]) {
        int pos = 0;
        char[] c = new char[b.length * 2];
        for (int i = 0; i < b.length; i++) {
            c[pos++] = toHex[(b[i] >> 4) & 0x0F];
            c[pos++] = toHex[b[i] & 0x0f];
        }
        return new String(c);
    }

    public void initialize(String realm, String userName, String uri, String nonce, String nonce_count, String qop_value, String password, String method, String cnonce, String algorithm) throws Exception {
        if (realm == null) throw new Exception("The realm parameter is null");
        this.realm = realm;
        if (userName == null) throw new Exception("The userName parameter is null");
        this.userName = userName;
        if (uri == null) throw new Exception("The uri parameter is null");
        this.uri = uri;
        if (nonce == null) throw new Exception("The nonce parameter is null");
        this.nonce = nonce;
        if (password == null) throw new Exception("The password parameter is null");
        this.password = password;
        if (method == null) throw new Exception("The method parameter is null");
        this.method = method;
        this.cnonce = cnonce;
        this.qop_value = qop_value;
        this.nonce_count = nonce_count;
        if (algorithm == null) {
            algorithm = "MD5";
        }
        try {
            messageDigest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException ex) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, initialize(): " + "ERROR: Digest algorithm does not exist.");
            throw ex;
        }
    }

    /** 
      * generate the response
      */
    public String generateResponse() {
        if (userName == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no userName parameter");
            return null;
        }
        if (realm == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no realm parameter");
            return null;
        }
        System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "Trying to generate a response for the user: " + userName + " , with " + "the realm: " + realm);
        if (password == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no password parameter");
            return null;
        }
        if (method == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no method parameter");
            return null;
        }
        if (uri == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no uri parameter");
            return null;
        }
        if (nonce == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: no nonce parameter");
            return null;
        }
        if (messageDigest == null) {
            System.out.println("DEBUG, DigestClientAuthenticationMethod, generateResponse(): " + "ERROR: the algorithm is not set");
            return null;
        }
        String A1 = userName + ":" + realm + ":" + password;
        byte mdbytes[] = messageDigest.digest(A1.getBytes());
        String HA1 = toHexString(mdbytes);
        String A2 = method.toUpperCase() + ":" + uri;
        mdbytes = messageDigest.digest(A2.getBytes());
        String HA2 = toHexString(mdbytes);
        String KD;
        if (qop_value != null) {
            KD = HA1 + ":" + nonce + ":" + nonce_count;
            if (cnonce != null) {
                if (cnonce.length() > 0) KD += ":" + cnonce;
            }
            KD += ":" + qop_value;
            KD += ":" + HA2;
            mdbytes = messageDigest.digest(KD.getBytes());
        } else {
            KD = HA1 + ":" + nonce + ":" + HA2;
            mdbytes = messageDigest.digest(KD.getBytes());
        }
        String response = toHexString(mdbytes);
        System.out.println("DEBUG, DigestClientAlgorithm, generateResponse():" + " response generated: " + response);
        return response;
    }
}
