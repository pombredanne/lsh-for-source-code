package Server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import TransmitterS.Intelligence;
import TransmitterS.User;

/**
 * @author LK13
 */
public class UserImp implements User {

    private static final long serialVersionUID = -7273221034817380799L;

    private transient boolean loggedin = false;

    private transient long last_action = 0;

    private final ServerImp myServer;

    private final String name;

    private String password;

    private Vector<Intelligence> myIntelligence;

    /**
	 * Creates a new User setting his name to the provided String and his reference to the Server Object.
	 */
    public UserImp(String name, ServerImp myServer) {
        this.myServer = myServer;
        this.name = name;
        myIntelligence = new Vector<Intelligence>();
    }

    /**
	 * Returns {@code true} if the Password {@code testPassword} matches the MD5 Print in the Server's Database and {@code false} otherwise.
	 */
    public boolean checkPassword(String testPassword) {
        return password.equals(getMD5Hash(testPassword));
    }

    @Override
    public boolean getLoggedin() {
        return loggedin;
    }

    @Override
    public void setLoggedin(boolean _loggedin) {
        loggedin = _loggedin;
    }

    @Override
    public void updateTime() {
        last_action = System.currentTimeMillis();
    }

    @Override
    public boolean inTime() {
        if (System.currentTimeMillis() - last_action < 3000) return true; else return false;
    }

    /**
	 * Generates and returns an MD5 Hash of {@code password}
	 */
    private String getMD5Hash(String password) {
        String plainText = password;
        MessageDigest mdAlgorithm;
        StringBuffer hexString = new StringBuffer();
        try {
            mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(plainText.getBytes());
            byte[] digest = mdAlgorithm.digest();
            for (int i = 0; i < digest.length; i++) {
                plainText = Integer.toHexString(0xFF & digest[i]);
                if (plainText.length() < 2) {
                    plainText = "0" + plainText;
                }
                hexString.append(plainText);
            }
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return hexString.toString();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getUserID() {
        return myServer.allUsers.indexOf(this);
    }

    @Override
    public Vector<Intelligence> getIntelligence() {
        return myIntelligence;
    }

    @Override
    public void setPassword(String newPassword) {
        password = getMD5Hash(newPassword);
    }
}
