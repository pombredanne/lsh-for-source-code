package nacad.lemm.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import javax.xml.parsers.ParserConfigurationException;
import nacad.lemm.web.io.UserRetriever;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import org.xml.sax.SAXException;

/**
 * This class takes care of login a LEMMing user in the system checking if
 * the provided Login and Password are valid.
 * @author Jonas Dias
 */
public class Login {

    String login;

    String pwdHash;

    /**
     * Generates the md5 sum for the given String
     * @param pwd The string you want to make a hash
     * @return the md5 sum for the given string
     */
    public static String md5(String pwd) {
        String sen = "";
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        BigInteger hash = new BigInteger(1, md.digest(pwd.getBytes()));
        sen = hash.toString(16);
        return sen;
    }

    public Login(String login, String pwd) {
        this.login = login;
        this.pwdHash = md5(pwd);
    }

    /**
     * Check if the login and password informed are valid according to data
     * stored on the files
     * @return true if the login and password are valid and false if they are not
     * @throws nacad.lemm.exception.LEMMingException if the system could not access the user list file
     */
    public boolean isValid() throws ParserConfigurationException, SAXException, URISyntaxException, FileNotFoundException, IOException {
        UserRetriever ur = new UserRetriever();
        Map<String, String> usersMap = ur.getUsersMap();
        if (usersMap.containsKey(this.login)) {
            if (usersMap.get(this.login).equals(this.pwdHash)) {
                return true;
            }
            return false;
        }
        return false;
    }

    /**
     * Recalculates the session hash code internaly to be compared
     * with value stored in the browser session.
     * @return The session code for that specific user.
     */
    public String getSessionCode() {
        String session = md5(this.login + this.pwdHash);
        return session;
    }
}
