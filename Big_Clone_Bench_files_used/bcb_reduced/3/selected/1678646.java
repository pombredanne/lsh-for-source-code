package org.grailrtls.server.security;

import java.security.*;
import java.util.*;
import org.grailrtls.server.*;

/**
 * @author Robert S. Moore II
 * 
 */
public class SecurityManager {

    private final LocalizationServer server;

    private final Map<String, User> accounts = Collections.synchronizedMap(new HashMap<String, User>());

    private final Map<String, SecurityRole> roles = Collections.synchronizedMap(new HashMap<String, SecurityRole>());

    public SecurityManager(LocalizationServer server) {
        this.server = server;
    }

    public void addRole(SecurityRole role) throws IllegalArgumentException {
        if (this.roles.containsKey(role.name)) throw new IllegalArgumentException("A role with the name \"" + role.name + "\" is already defined.");
        this.roles.put(role.name, role);
    }

    public SecurityRole getRole(String name) {
        return this.roles.get(name);
    }

    public void addUser(String username, String password, SecurityRole role) throws IllegalArgumentException {
        if (username == null || username.length() == 0 || password == null || password.length() == 0 || role == null) throw new IllegalArgumentException("Cannot add a user with a null/zero-length username or password.");
        if (this.accounts.containsKey(username)) throw new IllegalArgumentException("Username \"" + username + "\" already exists.");
        MessageDigest md5digest;
        try {
            md5digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            this.server.io.logError("Could not generate password hash.");
            nsae.printStackTrace(this.server.io.getLog());
            throw new IllegalArgumentException("Could not generate password hash.");
        }
        byte[] pass_bytes = password.getBytes();
        byte[] hash_bytes = md5digest.digest(pass_bytes);
        User user = new User(username, hash_bytes, role);
        this.accounts.put(username, user);
    }

    public void addUser(String username, byte[] passhash, SecurityRole role) throws IllegalArgumentException {
        if (username == null || username.length() == 0 || passhash == null || passhash.length == 0 || role == null) throw new IllegalArgumentException("Cannot add a user with a null/zero-length username or password.");
        if (this.accounts.containsKey(username)) throw new IllegalArgumentException("Username \"" + username + "\" already exists.");
        User user = new User(username, passhash, role);
        this.accounts.put(username, user);
    }

    public boolean removeUser(String username) {
        if (username == null || username.length() == 0) return false;
        return (this.accounts.remove(username) != null);
    }

    public void removeAllUsers() {
        this.accounts.clear();
    }

    public User validateLogin(String username, String password) {
        if (username == null || username.length() == 0 || password == null || password.length() == 0) return null;
        MessageDigest md5digest;
        try {
            md5digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            this.server.io.logError("Could not generate password hash.");
            nsae.printStackTrace(this.server.io.getLog());
            return null;
        }
        byte[] hash_bytes = md5digest.digest(password.getBytes());
        md5digest.reset();
        User user = this.accounts.get(username);
        if (user == null) return null;
        if (hash_bytes.length != user.passhash.length) return null;
        for (int i = 0; i < hash_bytes.length; i++) if (hash_bytes[i] != user.passhash[i]) return null;
        return user;
    }

    public Collection<User> getUserList() {
        return this.accounts.values();
    }
}
