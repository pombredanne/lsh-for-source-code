package net.sareweb.acab.session;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import javax.persistence.EntityManager;
import net.sareweb.acab.components.session.AcabIdentity;
import net.sareweb.acab.entity.Avatar;
import net.sareweb.acab.entity.Role;
import net.sareweb.acab.entity.User;
import net.sareweb.acab.entity.manager.AvatarManager;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.core.Events;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;

@Name("authenticator")
public class Authenticator {

    @Logger
    private Log log;

    @In
    AcabIdentity identity;

    @In
    Credentials credentials;

    @In
    private EntityManager entityManager;

    private final String query = "from User where login=:login and password = :password and enabled=true";

    public boolean authenticate() {
        log.info("authenticating {0}, ({1})", credentials.getUsername(), credentials.getPassword());
        try {
            User result = (User) entityManager.createQuery(query).setParameter("login", credentials.getUsername()).setParameter("password", SHA1(credentials.getPassword())).getSingleResult();
            identity.setUser(result);
            if (result.getRoles() != null) {
                for (Role role : result.getRoles()) {
                    identity.addRole(role.getName());
                }
            }
            identity.setAvatar(result.getAvatar());
            log.debug("Authentication successful:" + credentials.getUsername());
            Events.instance().raiseEvent("userLoggedEvent");
            return true;
        } catch (Exception e) {
            log.debug("Failed to authenticate user:" + credentials.getUsername());
            return false;
        }
    }

    private static String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while (two_halfs++ < 1);
        }
        return buf.toString();
    }

    public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-1");
        byte[] sha1hash = new byte[40];
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();
        return convertToHex(sha1hash);
    }
}
