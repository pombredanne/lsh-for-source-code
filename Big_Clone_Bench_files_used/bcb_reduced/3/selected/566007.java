package wsl.fw.security;

import java.security.Principal;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import wsl.fw.util.Util;
import wsl.fw.util.Config;
import wsl.fw.util.CKfw;
import wsl.fw.resource.ResourceManager;
import wsl.fw.resource.ResId;

/**
 * Basic secured identity, used for verifying identity and checking permissions.
 * This is a key that may be passed to the BootstrapSecurityManager to get
 * (identify) a user and look up access rights.
 */
public class SecurityId implements Principal, Serializable {

    private static final String _ident = "$Date: 2002/06/11 23:11:42 $  $Revision: 1.1.1.1 $ " + "$Archive: /Framework/Source/wsl/fw/security/SecurityId.java $ ";

    public static final ResId USERNAME = new ResId("SecurityId.UserName");

    public static final ResId PASSWORD = new ResId("SecurityId.Password");

    public static final ResId MAIN1 = new ResId("SecurityId.Main1");

    public static final ResId MAIN2 = new ResId("SecurityId.Main2");

    public static final ResId MAIN_DUMMY = new ResId("SecurityId.MainDummy");

    public static final ResId MAIN_ENCODED_PASSWORD = new ResId("SecurityId.MainEncodedPassword");

    private String _username;

    private String _password;

    /**
     * Standard constructor specifying name and password. The password is
     * stored in encoded form.
     * @param username, the username, may not be null or empty.
     * @param password, the password, may be empty.
     * @throws IllegalArgumentException if a parameter is invalid.
     * @throws UnsupportedOperationException if the SHA digest is not available.
     */
    public SecurityId(String username, String password) {
        this(username, password, true);
    }

    /**
     * Extended constructor specifying name and password, the password may be
     * stored in encided form.
     * @param username, the username, may not be null or empty.
     * @param password, the password, may be empty.
     * @param bEncode, if true the password is stored in encoded form.
     * @throws IllegalArgumentException if a parameter is invalid.
     * @throws UnsupportedOperationException if the SHA digest is not available.
     */
    public SecurityId(String username, String password, boolean bEncode) {
        Util.argCheckEmpty(username, USERNAME.getText());
        Util.argCheckNull(password, PASSWORD.getText());
        _username = username;
        if (bEncode) try {
            MessageDigest md = MessageDigest.getInstance("SHA");
            byte digestBytes[] = md.digest(password.getBytes());
            _password = Util.bytesToHex(digestBytes);
        } catch (Exception e) {
            throw new UnsupportedOperationException(e.toString());
        } else _password = password;
    }

    /**
     * Check for equality.
     * @param other, the other SecurityId to compare with.
     * @return true if same class and equal value.
     */
    public boolean equals(Object other) {
        if (other instanceof SecurityId) {
            SecurityId osid = (SecurityId) other;
            if (_username.equals(osid._username) && _password.equals(osid._password)) return true;
        }
        return false;
    }

    /**
     * Calculate the hashcode.
     * @return the hashcode.
     */
    public int hashCode() {
        return _username.hashCode() ^ _password.hashCode();
    }

    /**
     * String representation.
     * @return the username.
     */
    public String toString() {
        return _username;
    }

    /**
     * @return the username.
     */
    public String getName() {
        return _username;
    }

    /**
     * Get the password, which may be encrypted as a digest depending on the
     * object construction.
     * @return the password.
     */
    public String getPassword() {
        return _password;
    }

    /**
     * Static factory method to create a default system SecurityId from
     * information in Config.
     * @return the system SecurityId, or null if the Config does not have the
     *   information to create the system id.
     */
    public static SecurityId getSystemId() {
        String username = Config.getProp(CKfw.SYSTEMID_NAME);
        String password = Config.getProp(CKfw.SYSTEMID_PASSWORD, "");
        return ((username == null) ? null : new SecurityId(username, password));
    }

    /**
     * Main function, provides a command line interface to generate the
     * encrypted password from the plaintext. Takes a single argument which is
     * the password.
     */
    public static void main(String args[]) {
        ResourceManager.set(new ResourceManager());
        if (args.length != 1) {
            System.out.println(MAIN1.getText());
            System.out.println(MAIN2.getText());
        } else {
            SecurityId sid = new SecurityId(MAIN_DUMMY.getText(), args[0]);
            System.out.println(MAIN_ENCODED_PASSWORD.getText() + " >>>" + sid.getPassword() + "<<<");
        }
    }
}
