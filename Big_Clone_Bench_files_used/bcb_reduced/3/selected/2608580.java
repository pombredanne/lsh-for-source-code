package ade;

import ade.ADEGlobals;
import java.io.Serializable;
import java.util.*;
import java.security.*;

/**
The user representation in the ADE system.
*/
public class ADEUser implements Serializable {

    String uid;

    String pass;

    HashSet<String> userAccess;

    boolean isAdmin;

    boolean isServer;

    public ADEUser(String id, String p) {
        this(id, p, false, true, null);
        HashSet<String> acc = new HashSet<String>();
        userAccess = acc;
    }

    public ADEUser(String id, String p, HashSet<String> acc) {
        this(id, p, false, true, acc);
    }

    public ADEUser(String id, String p, boolean a, boolean s) {
        this(id, p, false, true, null);
        HashSet<String> acc = new HashSet<String>();
        if (a) acc.add(ADEGlobals.ALL_ACCESS);
        userAccess = acc;
    }

    /** Create a user object.
     * @param id the user id
     * @param p the (plaintext) password
     * @param a is the user an administrator?
     * @param s is the user an ADEServer?
     * @param acc the user's access level */
    public ADEUser(String id, String p, boolean a, boolean s, HashSet<String> acc) {
        uid = id;
        pass = SHA1it(p);
        userAccess = acc;
        isAdmin = a;
        isServer = s;
    }

    public String getUID() {
        return uid;
    }

    public void setUID(String id) {
        uid = id;
    }

    public String getPwd() {
        return pass;
    }

    public void setPassword(String p) {
        pass = SHA1it(p);
    }

    public boolean validPassword(String p) {
        return pass.equalsIgnoreCase(SHA1it(p));
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean b) {
        isAdmin = b;
    }

    public boolean isServer() {
        return isServer;
    }

    public HashSet<String> getAllowances() {
        return userAccess;
    }

    public Iterator<String> getUserAccess() {
        return userAccess.iterator();
    }

    public boolean setUserAccess(HashSet<String> acc) {
        userAccess = acc;
        return (userAccess != null);
    }

    public boolean addUserAccess(String acc) {
        userAccess.add(acc);
        return (userAccess.contains(acc));
    }

    public void removeUserAccess(String acc) {
        if (userAccess.contains(acc)) {
            userAccess.remove(acc);
        }
    }

    /**
     * Computes the SHA-1 digest of a string and returns it as a
     * hex-encoded string.
     *
     * @param message The string to be digested
     * @return a <code>String</code> with the hex-encoded SHA-1 digest
     * of the message
     */
    protected static String SHA1it(String message) {
        MessageDigest encryptor;
        StringBuffer encrypted;
        byte[] bmsg = message.getBytes();
        boolean more2 = true;
        try {
            encryptor = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("This JVM stinks. Get one that implements SHA.");
            System.err.println("Error: " + nsae);
            return null;
        }
        for (int i = 0; i < bmsg.length; i++) encryptor.update(bmsg[i]);
        bmsg = encryptor.digest();
        encrypted = new StringBuffer();
        for (int i = 0; i < bmsg.length; i++) {
            int t = bmsg[i] & 0xFF;
            if (t <= 0x0F) encrypted.append("0");
            encrypted.append(Integer.toHexString(t));
        }
        return encrypted.toString();
    }
}
