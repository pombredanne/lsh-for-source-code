package com.triplekona.tktk.weblogic;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import weblogic.security.acl.BasicRealm;
import weblogic.security.acl.User;
import weblogic.utils.encoders.BASE64Encoder;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import com.triplekona.tktk.UserInfo;

/**
 * This is a class that represents database-persisted users in
 * the WebLogic server.  This class is WebLogic specific; most of
 * the existing implementation mimics the WebLogic RDBMSRealm example.
 *
 * @author Dhiren Patel, Triple Kona Software, Inc.
 */
class DBUser extends User implements UserInfo {

    /**
   * The default password generation algorithm.
   */
    protected static final String ALGORITHM = "SHA";

    /**
   * The realm that created this user object.
   */
    private transient DBRealm realm;

    /**
   * The user's password.  If the password is hashed, the md field will
   * contain an instance of an object that performs the hashing
   * algorithm.
   */
    private transient String passwd;

    /**
   * The digest algorithm used to one-way hash the user's password.
   * If the password is not hashed with a known algorithm, or is in
   * plain text, this will be null.
   */
    private transient MessageDigest md;

    /**
     * The first name of the user, as parsed by this class.  Currently
     * it is the first word in the Name string stored in tblAccounts.
     */
    private String firstName;

    /**
	 * The last name of the user, as parsed by this class.  Currently
	 * it is the last word in the Name string stored in tblAccounts.
	 */
    private String lastName;

    /**
   * Creates a user with the given name and hashed password
   * from the given realm. The name parameter is a string
   * of the format <firstname> <middlename> <lastname>.  This
   * class assumes that the first word in the name string is
   * the first name and the last word in the string is the
   * last name.  This is not ideal behavior and should be
   * changed to support robust handling of first, last, and
   * middle names.
   */
    DBUser(String username, String passwd, String name, DBRealm realm) {
        super(username.trim());
        this.realm = realm;
        firstName = parseFirstName(name);
        lastName = parseLastName(name);
        if (passwd == null) {
            return;
        }
        passwd = passwd.trim();
        int rightCurly = passwd.indexOf("}");
        if (rightCurly > 0 && passwd.charAt(0) == '{') {
            this.passwd = passwd.substring(rightCurly + 1);
            String algorithm = passwd.substring(1, rightCurly);
            try {
                md = MessageDigest.getInstance(algorithm.toUpperCase());
            } catch (NoSuchAlgorithmException e) {
                if (realm.log != null) {
                    realm.log.error("digest algorithm \"" + algorithm + "\" not found - assuming plaintext password");
                } else {
                    System.err.println("Error: digest algorithm \"" + algorithm + "\" not found - assuming plaintext password");
                }
            }
        } else {
            this.passwd = passwd;
            this.md = null;
        }
    }

    /**
	 * Return the username, i.e. the login
	 */
    public String getUsername() {
        return getName();
    }

    /**
	 * Return the password stored in this object.  It may or may not
	 * be hashed.  You must inspect the returned string to determine the
	 * format.
	 */
    public String getPassword() {
        return passwd;
    }

    /**
	 * Return the first name of the User
	 */
    public String getFirstName() {
        return firstName;
    }

    /**
	 * Return the last name of the user
	 */
    public String getLastName() {
        return lastName;
    }

    public String toString() {
        return "UserID: " + getUsername() + "\t\tPassword: " + getPassword() + "\t\tName: " + getFirstName() + " " + getLastName();
    }

    /**
	 * Take a name string of the form <firstname> <middlename> <lastname>
	 * and parst out the first name.  Currently, this returns the
	 * first word in the name string.
	 */
    private static String parseFirstName(String fullName) {
        StringTokenizer st = new StringTokenizer(fullName);
        return getToken(st, 0);
    }

    /**
	 * Take a name string of the form <firstname> <middlename> <lastname>
	 * and parst out the last name.  Currently, this returns the
	 * last word in the name string.
	 */
    private static String parseLastName(String fullName) {
        StringTokenizer st = new StringTokenizer(fullName);
        return getToken(st, st.countTokens() - 1);
    }

    /**
	 * Returns the nth token for the given <code>StringTokenizer</code>.
	 * If the token cannot be retrieved (e.g. there are no more tokens),
	 * an empty string is returned.  The index starts at zero.
	 */
    private static String getToken(StringTokenizer st, int n) {
        String retVal = "";
        try {
            while (n >= 0) {
                retVal = st.nextToken();
                n--;
            }
        } catch (NoSuchElementException e) {
            retVal = "";
        }
        return retVal;
    }

    /**
   * Returns the realm that created this object.
   */
    public BasicRealm getRealm() {
        return realm;
    }

    /**
   * Hashes the given plain text with the given digest algorithm, and
   * base64-encode the result.
   *
   * @param md message digest algorithm to hash with
   * @param plaintext text to hash
   * @return base64-encoded hashed text
   */
    protected static String hash(MessageDigest md, String plaintext) {
        BASE64Encoder enc = new BASE64Encoder();
        return enc.encodeBuffer(md.digest(plaintext.getBytes()));
    }

    /**
   * Checks a plain text password against the user's password.  If the
   * object containing the password is not known, authentication will
   * fail.
   *
   * @param plaintext the plaintext password to check
   * @return true if matched, false otherwise
   */
    boolean authenticate(String plaintext) {
        plaintext = plaintext.trim();
        String hashed = md != null ? hash(md, plaintext) : plaintext;
        return hashed.equals(passwd);
    }

    /**
   * Hashes passwords according to the given algorithm.  Plain text
   * passwords are read from stdin, and the encrypted passwords are
   * printed to stdout.  If no algorithm is specified on the command
   * line, the one specified in ALGORITHM is used.
   *
   * @see #ALGORITHM
   */
    public static void main(String[] args) throws IOException {
        String algorithm = (args.length >= 1 ? args[0] : ALGORITHM).toUpperCase();
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        MessageDigest md = null;
        String prefix = null;
        String plaintext;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
        }
        if (md == null) {
            System.err.println("Error: unknown algorithm \"" + algorithm + "\"");
            System.exit(1);
        }
        System.err.println("Enter plaintext passwords, separated by newlines.");
        while ((plaintext = r.readLine()) != null) {
            String passwd = "{" + algorithm + "}" + hash(md, plaintext);
            if (System.out.checkError()) {
                throw new IOException("output error");
            }
        }
        r.close();
    }
}
