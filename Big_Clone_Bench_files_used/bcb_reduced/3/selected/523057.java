package com.dbxml.db.core.security;

import com.dbxml.util.UTF8;
import com.dbxml.xml.XMLSerializable;
import com.dbxml.xml.dom.DOMUtils;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.TreeSet;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * User
 */
public class User implements XMLSerializable {

    private static final String[] EmptyStrings = new String[0];

    private static final char[] HexChars = "0123456789ABCDEF".toCharArray();

    private static final String DIGEST_ALGORITHM = "SHA";

    private static final String USER = "user";

    private static final String ID = "id";

    private static final String PASSWORD = "password";

    private static final String ROLE = "role";

    private String id = "";

    private String password = "";

    private Set roles = new TreeSet();

    public Element streamToXML(Document doc) throws DOMException {
        Element contentElem = doc.createElement(USER);
        contentElem.setAttribute(ID, id);
        contentElem.setAttribute(PASSWORD, password);
        DOMUtils.setToElements(contentElem, ROLE, roles);
        return contentElem;
    }

    public void streamFromXML(Element element) throws DOMException {
        id = element.getAttribute(ID);
        password = element.getAttribute(PASSWORD);
        DOMUtils.elementsToSet(element, ROLE, roles);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    private String toHexString(byte[] b) {
        char[] c = new char[b.length * 2];
        for (int i = 0, j = 0; i < b.length; i++, j += 2) {
            byte val = b[i];
            c[j] = HexChars[(val >>> 4) & 0xF];
            c[j + 1] = HexChars[val & 0xF];
        }
        return new String(c);
    }

    public void setPassword(String textPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] b = digest.digest(UTF8.toUTF8(textPassword));
            this.password = toHexString(b);
        } catch (NoSuchAlgorithmException e) {
            this.password = textPassword;
            e.printStackTrace(System.err);
        }
    }

    public boolean checkPassword(String textPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] b = digest.digest(UTF8.toUTF8(textPassword));
            return password.equals(toHexString(b));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
            return password.equals(textPassword);
        }
    }

    public void addRole(Role role) {
        String roleID = role.getId();
        if (!roles.contains(roleID)) roles.add(roleID);
    }

    public void removeRole(Role role) {
        String roleID = role.getId();
        roles.remove(roleID);
    }

    public String[] listRoles() {
        return (String[]) roles.toArray(EmptyStrings);
    }
}
