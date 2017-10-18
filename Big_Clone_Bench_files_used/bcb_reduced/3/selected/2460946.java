package starcraft.gameserver.impl;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import starcraft.gamemodel.GameException;

public class UserDatabase {

    private final Log log = LogFactory.getLog(getClass());

    private final List<UserAccount> userAccounts = new ArrayList<UserAccount>();

    public boolean exists(String username) {
        for (UserAccount account : userAccounts) {
            if (account.username.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    public void createAccount(String username, String password, String displayName) throws GameException {
        if (exists(username)) {
            throw new GameException("An account with username '" + username + "' already exists.");
        }
        try {
            userAccounts.add(new UserAccount(username, hashPassword(password), displayName));
        } catch (Exception e) {
            throw new GameException("Error hashing password.", e);
        }
        log.debug("created account for '" + username + "'.");
    }

    public UserAccount authenticateUser(String username, String password) throws GameException {
        for (UserAccount account : userAccounts) {
            if (account.username.equalsIgnoreCase(username)) {
                try {
                    if (verifyPassword(account.password, password)) {
                        return account;
                    } else {
                        throw new GameException("Invalid password.");
                    }
                } catch (Exception e) {
                    throw new GameException("Error validating password.", e);
                }
            }
        }
        throw new GameException("Unknown username '" + username + "'.");
    }

    public List<UserAccount> getUserAccounts() {
        return userAccounts;
    }

    private static String hashPassword(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(password.getBytes("UTF-8"));
        String hashString = new String(Base64.encodeBase64(hashBytes), "UTF-8");
        return hashString;
    }

    private static boolean verifyPassword(String hashedPassword, String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hashPassword(password).equals(hashedPassword);
    }
}
