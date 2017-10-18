package org.bluesock.bluemud.driver;

import java.security.Permission;
import java.security.Permissions;
import java.security.MessageDigest;

/**
 * This is the security token that we base our permission system on.
 * FIXME - needs to be documented.
 */
class SecurityToken implements java.io.Serializable {

    static final SecurityToken EMPTY_TOKEN = new SecurityToken();

    private String userName = null;

    private byte[] passwordDigest = null;

    private Permissions securityPermissions = null;

    static SecurityToken newUser(String givenUserName, String plaintextPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            SecurityToken st = new SecurityToken();
            st.userName = givenUserName;
            st.passwordDigest = md.digest(plaintextPassword.getBytes());
            st.securityPermissions = new Permissions();
            return st;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean authenticate(String givenUserName, String plaintextPassword) {
        if (this == EMPTY_TOKEN) {
            return false;
        }
        try {
            if (!userName.equals(givenUserName)) {
                return false;
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(plaintextPassword.getBytes());
            if (!(md.isEqual(hash, passwordDigest))) {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    void grantPermission(Permission p) {
        if (this != EMPTY_TOKEN) {
            securityPermissions.add(p);
        }
    }

    boolean implies(Permission p) {
        if (this == EMPTY_TOKEN) {
            return false;
        } else {
            return securityPermissions.implies(p);
        }
    }
}
