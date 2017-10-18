package net.jawe.scriptbot.impl.users;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import net.jawe.scriptbot.factory.Factory;
import net.jawe.scriptbot.users.BotUser;
import net.jawe.scriptbot.users.UserManager;

public class BotUserImpl implements Principal, BotUser {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(BotUserImpl.class);

    /**
     * Used building output as Hex
     */
    private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private String _name;

    private String _password;

    private final Set<String> _hostmasks;

    private Date _lastLogin;

    private String _lastKnownNick;

    /**
     * 
     */
    public BotUserImpl() {
        _hostmasks = new HashSet<String>();
    }

    public String getName() {
        return _name;
    }

    public void setName(String name) {
        _name = name;
    }

    public String getPassword() {
        return _password;
    }

    public void setPassword(String password) {
        _password = hashPassword(password);
    }

    public Set<String> getHostmasks() {
        return _hostmasks;
    }

    public Date getLastLogin() {
        return _lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        _lastLogin = lastLogin;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj != null && obj.getClass().equals(getClass())) {
            BotUser other = (BotUser) obj;
            return _name.equals(other.getName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _name != null ? _name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return _name;
    }

    public boolean verifyPassword(String password) {
        boolean success = _password.equals(hashPassword(password));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Password ok: " + success);
        }
        return success;
    }

    public boolean matchHostmask(String nick, String login, String hostname) {
        UserManager users = (UserManager) Factory.getInstance().getBean("users");
        return users.hostmaskMatches(nick, login, hostname, _hostmasks);
    }

    public String getLastKnownNick() {
        return _lastKnownNick;
    }

    public void setLastKnownNick(String nick) {
        _lastKnownNick = nick;
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(password.getBytes());
            return new String(encodeHex(hash));
        } catch (NoSuchAlgorithmException e) {
            LOG.error("WTF? SHA-1 not available", e);
            return password;
        }
    }

    private static char[] encodeHex(byte[] data) {
        int l = data.length;
        char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }
        return out;
    }
}
