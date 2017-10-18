package org.mitre.jsip;

import org.mitre.jsip.util.Base64;
import java.security.MessageDigest;
import java.util.*;

public class Sip {

    /**
     * Sip
     */
    public Sip() {
    }

    /**
     * getMethodString
     * @param m
     * @return String
     */
    public static String getMethodString(int m) {
        switch(m) {
            case INVITE:
                return "INVITE";
            case ACK:
                return "ACK";
            case BYE:
                return "BYE";
            case OPTIONS:
                return "OPTIONS";
            case CANCEL:
                return "CANCEL";
            case REGISTER:
                return "REGISTER";
            case MESSAGE:
                return "MESSAGE";
            case SUBSCRIBE:
                return "SUBSCRIBE";
            case NOTIFY:
                return "NOTIFY";
            case INFO:
                return "INFO";
            case BadMethod:
                return "BAD";
        }
        ;
        return null;
    }

    /**
     * matchMethod
     * @param m
     * @return Method
     */
    public static int matchMethod(String m) {
        if (m.compareTo(getMethodString(INVITE)) == 0) {
            return INVITE;
        }
        if (m.compareTo(getMethodString(ACK)) == 0) {
            return ACK;
        }
        if (m.compareTo(getMethodString(BYE)) == 0) {
            return BYE;
        }
        if (m.compareTo(getMethodString(OPTIONS)) == 0) {
            return OPTIONS;
        }
        if (m.compareTo(getMethodString(CANCEL)) == 0) {
            return CANCEL;
        }
        if (m.compareTo(getMethodString(REGISTER)) == 0) {
            return REGISTER;
        }
        if (m.compareTo(getMethodString(MESSAGE)) == 0) {
            return MESSAGE;
        }
        if (m.compareTo(getMethodString(SUBSCRIBE)) == 0) {
            return SUBSCRIBE;
        }
        if (m.compareTo(getMethodString(NOTIFY)) == 0) {
            return NOTIFY;
        }
        if (m.compareTo(getMethodString(INFO)) == 0) {
            return INFO;
        }
        if (m.compareTo(getMethodString(MESSAGE)) == 0) {
            return MESSAGE;
        }
        return BadMethod;
    }

    /**
     * getLocalAddress
     * @return String
     */
    public static String getLocalAddress() {
        if (dissipate_ouraddress == null) {
            dissipate_ouraddress = System.getProperty("sip.dissipate.address", SipUtil.getLocalIP());
        }
        return dissipate_ouraddress;
    }

    /**
     * Get the port we will be listening to
     *
     * @return a sip port number
     */
    public static int getLocalPort() {
        if (dissipate_ourport == 0) {
            String portnum = System.getProperty("sip.dissipate.port", "5060");
            try {
                dissipate_ourport = Integer.parseInt(portnum);
            } catch (NumberFormatException nfe) {
                System.err.println("Error setting port to desired value: " + portnum + ". Going with default value (5060)");
                dissipate_ourport = 5060;
            }
        }
        return dissipate_ourport;
    }

    /**
     * setLocalAddress
     * @param localaddr
     */
    public static void setLocalAddress(String localaddr) {
        dissipate_ouraddress = localaddr;
    }

    /**
     * getDigestResponse
     * @param user
     * @param password
     * @param method
     * @param requri
     * @param authstr
     * @return String
     */
    public static String getDigestResponse(String user, String password, String method, String requri, String authstr) {
        String realm = "";
        String nonce = "";
        String opaque = "";
        String algorithm = "";
        String qop = "";
        StringBuffer digest = new StringBuffer();
        String cnonce;
        String noncecount;
        String pAuthStr = authstr;
        int ptr = 0;
        String response = "";
        int i = 0;
        StringTokenizer st = new StringTokenizer(pAuthStr, ",");
        StringTokenizer stprob = null;
        String str = null;
        String key = null;
        String value = null;
        Properties probs = new Properties();
        while (st.hasMoreTokens()) {
            String nextToken = st.nextToken();
            stprob = new StringTokenizer(nextToken, "=");
            key = stprob.nextToken();
            value = stprob.nextToken();
            if (value.charAt(0) == '"' || value.charAt(0) == '\'') {
                value = value.substring(1, value.length() - 1);
            }
            probs.put(key, value);
        }
        digest.append("Digest username=\"" + user + "\", ");
        digest.append("realm=\"");
        digest.append(probs.getProperty("realm"));
        digest.append("\", ");
        digest.append("nonce=\"");
        digest.append(probs.getProperty("nonce"));
        digest.append("\", ");
        digest.append("uri=\"" + requri + "\", ");
        cnonce = "abcdefghi";
        noncecount = "00000001";
        String toDigest = user + ":" + realm + ":" + password;
        byte[] digestbuffer = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toDigest.getBytes());
            digestbuffer = md.digest();
        } catch (Exception e) {
            System.err.println("Error creating digest request: " + e);
            return null;
        }
        digest.append("qop=\"auth\", ");
        digest.append("cnonce=\"" + cnonce + "\", ");
        digest.append("nc=" + noncecount + ", ");
        digest.append("response=\"" + response + "\"");
        if (probs.getProperty("opaque") != null) {
            digest.append(", opaque=\"" + probs.getProperty("opaque") + "\"");
        }
        System.out.println("SipProtocol: Digest calculated.");
        return digest.toString();
    }

    /**
   * getBasicResponse
   * @param user
   * @param password
   * @return String
   */
    public static String getBasicResponse(String user, String password) {
        String basic;
        String userpass = "";
        basic = "Basic ";
        userpass += user;
        userpass += ":";
        userpass += password;
        basic += Base64.encode(userpass);
        return basic;
    }

    private static String dissipate_ouraddress;

    private static int dissipate_ourport;

    public static final int INVITE = 0;

    public static final int ACK = 1;

    public static final int BYE = 2;

    public static final int OPTIONS = 3;

    public static final int CANCEL = 4;

    public static final int REGISTER = 5;

    public static final int MESSAGE = 6;

    public static final int SUBSCRIBE = 7;

    public static final int NOTIFY = 8;

    public static final int INFO = 9;

    public static final int REFER = 10;

    public static final int BadMethod = 99;
}
