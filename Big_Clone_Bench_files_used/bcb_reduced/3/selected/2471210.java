package javajabberc;

import javax.swing.*;
import java.security.*;

public class JabberUtil {

    static final String[][] xmlTokens = { { "amp", "&" }, { "apos", "'" }, { "gt", ">" }, { "lt", "<" }, { "quot", "\"" } };

    public static void errorMsg(String title, String text) {
        JOptionPane.showMessageDialog(null, text, title, JOptionPane.ERROR_MESSAGE);
    }

    public static String getDigest(String in) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            return (null);
        }
        byte data[] = new byte[40];
        data = md.digest(in.getBytes());
        StringBuffer output = new StringBuffer(40);
        for (int i = 0; i < 20; i++) {
            if ((data[i] > -1) && (data[i] < 16)) output.append('0');
            output.append(Integer.toHexString(((int) data[i] & 0xff)));
        }
        return (output.toString());
    }

    /**
	 * Extracts the group that the message is intended for.
	 * @param from The from string from the message element. E.g. group@host/user
	 * @return The group name. <br>Returns null is from is null, or there is no group in the string.
	 */
    public static String getGroup(String from) {
        if (from == null) return (null);
        int p = from.indexOf('@');
        if (p > -1) return (from.substring(0, p)); else return (null);
    }

    /**
	 * Extracts the hostname from the message.
	 * @param from The from string from the message element. E.g. user@host/resource or group@host/user
	 * @return The Group name.
	 */
    public static String getHost(String from) {
        if (from == null) return (null);
        int p = from.indexOf('@');
        if (p > -1) {
            int p2 = from.indexOf('/', p);
            if (p2 > -1) {
                return (from.substring(p, p2));
            }
        }
        return (null);
    }

    /**
	 * Extract the resource from the string.
	 * @param data A location string. E.g. user@host/resource
	 * @return The resource part of the string.<br>Returns null is from is null, or there is no resource in the string.
	 */
    public static String getResource(String data) {
        if (data == null) return (null);
        int p = data.indexOf('/');
        if (p > -1) return (data.substring(p + 1, data.length())); else return (null);
    }

    /**
	 * Extract the sender of the message from the string.
	 * @param from A from string. E.g. group@host/user
	 * @return The resource part of the string.<br>Returns null is from is null, or there is no resource in the string.
	 */
    public static String getSender(String from) {
        return (getResource(from));
    }

    public static String getUsername(String data) {
        if (data == null) return (null);
        int p = data.indexOf('@');
        if (p > -1) return (data.substring(0, p)); else return (data);
    }

    /**
	 * Extract the username from the string.
	 * @param data A user string. E.g. user@host/resource
	 * @return The username part of the string.<br>Returns null is from is null, or there is no name in the string.
	 */
    public static String getUserHostname(String data) {
        if (data == null) return (null);
        int p = data.indexOf('/');
        if (p > -1) return (data.substring(0, p)); else return ("");
    }

    /**
	 * Takes a string and changes all XML &; codes into characters. (Well, some of them at least).
	 */
    public static String deXML(String data) {
        StringBuffer sb = new StringBuffer(data);
        int pos = 0;
        int state = 0;
        int tokenStart = -1;
        StringBuffer token = new StringBuffer(32);
        char c;
        while (pos < sb.length()) {
            c = sb.charAt(pos);
            switch(state) {
                case 0:
                    switch(c) {
                        case '&':
                            tokenStart = pos;
                            state = 1;
                            token.delete(0, token.length());
                            break;
                    }
                    break;
                case 1:
                    switch(c) {
                        case ';':
                            sb.replace(tokenStart, pos + 1, getXMLToken(token.toString()));
                            state = 0;
                            pos = tokenStart;
                            break;
                        default:
                            token.append(c);
                    }
                    break;
            }
            pos++;
        }
        return (sb.toString());
    }

    public static String getXMLToken(String token) {
        int i, cmp;
        for (i = 0; i < xmlTokens.length; i++) {
            cmp = token.compareTo(xmlTokens[i][0]);
            if (cmp == 0) return (xmlTokens[i][1]); else if (cmp < 0) return (" ");
        }
        return (" ");
    }

    public static void infoMsg(String title, String text) {
        JOptionPane.showMessageDialog(null, text, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean okBox(String title, String text) {
        if (JOptionPane.showConfirmDialog(null, text, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) return (true); else return (false);
    }

    /**
	 * Validates a node name. Basically makes sure that it dosen't contain any illegal characters.
	 */
    public static boolean validateNode(String name) {
        if (name == null) return (false);
        int len = name.length();
        if (len > 255) return (false);
        char c;
        for (int i = 0; i < len; i++) {
            c = name.charAt(i);
            if (c <= ' ') return (false); else if (c == ':') return (false); else if (c == '@') return (false); else if (c == '"') return (false);
        }
        return (true);
    }
}
