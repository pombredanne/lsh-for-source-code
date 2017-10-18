package org.interlogy;

import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.jboss.seam.ScopeType;
import org.jboss.seam.faces.FacesMessages;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by IntelliJ IDEA.
 * User: naryzhny
 * Date: 22.08.2007
 * Time: 13:27:00
 * To change this template use File | Settings | File Templates.
 */
@Name("authService")
@Scope(ScopeType.APPLICATION)
public class AuthService {

    @Logger
    private Log log;

    @In
    private EntityManager entityManager;

    @Out(scope = ScopeType.SESSION, required = false)
    private User interlogyCurrentUser;

    public User findUser(String username, String password) {
        try {
            User user = (User) entityManager.createQuery("select object(u) from User u where u.username=:username").setParameter("username", username).getSingleResult();
            String hashPasswd = user.getHashPassword();
            if (hashPasswd.startsWith("{md5}")) {
                if (password != null && hashPassword(password).equals(hashPasswd)) {
                    return user;
                }
            } else {
                if (password != null && password.equals(hashPasswd)) {
                    return user;
                }
            }
            return null;
        } catch (NoResultException exc) {
            return null;
        }
    }

    public boolean authenticate() {
        log.info("try to authenticate");
        String username = Identity.instance().getUsername();
        String password = Identity.instance().getPassword();
        log.info("username: " + username);
        log.info("pass:" + password);
        User user = findUser(username, password);
        if (user == null) {
            FacesMessages.instance().add("Invalid username/password");
            return false;
        }
        log.info("user=" + user);
        if (user.getUserRole() != null) {
            log.info("userrole=" + user.getUserRole());
            Identity.instance().addRole(user.getUserRole().toString());
            if (user.getUserRole().equals(UserRole.ADMIN)) {
                Identity.instance().addRole(UserRole.USER.toString());
                Identity.instance().addRole(UserRole.MODERATOR.toString());
            }
            if (user.getUserRole().equals(UserRole.MODERATOR)) {
                Identity.instance().addRole(UserRole.USER.toString());
            }
        }
        interlogyCurrentUser = user;
        return true;
    }

    public static String hashPassword(String password) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(password.getBytes());
            byte result[] = md5.digest("InTeRlOgY".getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                String s = Integer.toHexString(result[i]);
                int length = s.length();
                if (length >= 2) {
                    sb.append(s.substring(length - 2, length));
                } else {
                    sb.append("0");
                    sb.append(s);
                }
            }
            return "{md5}" + sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return password;
        }
    }
}
