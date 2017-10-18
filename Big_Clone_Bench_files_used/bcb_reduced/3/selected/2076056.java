package edu.arsc.fullmetal.server;

import edu.arsc.fullmetal.commons.UserElement;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static edu.arsc.fullmetal.server.SQLNames.*;

public class UserRecord {

    public static final String ROLE_ADMIN = "admin";

    private final int id;

    private final String username;

    private final String fullname;

    private final String email;

    final byte[] salt;

    final byte[] hash;

    UserRecord(int id, String username, byte[] salt, byte[] hash, String fullname, String email) {
        this.id = id;
        this.username = username;
        this.salt = salt;
        this.hash = hash;
        this.fullname = fullname;
        this.email = email;
    }

    public UserElement toElement() {
        return new UserElement(username, fullname, getRoles().contains(ROLE_ADMIN));
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullname;
    }

    public String getEmail() {
        return email;
    }

    public int getID() {
        return id;
    }

    public Set<String> getRoles() {
        PreparedStatement ps = Statement.ROLES_BYUSER.asJdbc();
        Set<String> roles = new HashSet<String>();
        try {
            ps.setInt(PI_USERID, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                roles.add(rs.getString(F_NAME));
            }
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            return Collections.unmodifiableSet(new HashSet<String>());
        } finally {
            try {
                ps.clearParameters();
            } catch (SQLException ex) {
                Logger.getLogger(UserRecord.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return Collections.unmodifiableSet(roles);
    }

    /**
     * 
     * @param attempt
     * @note
     *     Passwords are encoded as UTF-8 strings for the purpose of hashing.
     * @return
     */
    public boolean isPasswordCorrect(String attempt) {
        try {
            MessageDigest digest = MessageDigest.getInstance(attempt);
            digest.update(salt);
            digest.update(attempt.getBytes("UTF-8"));
            byte[] attemptHash = digest.digest();
            return attemptHash.equals(hash);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(UserRecord.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(UserRecord.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
    }

    @Override
    public String toString() {
        return username + " (" + fullname + ")";
    }
}
