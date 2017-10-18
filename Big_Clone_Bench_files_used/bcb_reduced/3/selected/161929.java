package net.sf.arcus_judge;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.HashMap;
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity(version = 1)
public class Account implements Serializable, Cloneable {

    /**
	 * 
	 */
    private static final long serialVersionUID = -6300160477016727831L;

    public static final String ROOT_USERNAME = "root";

    public static final String ROOT_DEFAULT_PASSWORD = "root";

    public static final String ATTRIBUTE_MAIL_ADDRESS = "mailAddress";

    public static final String ATTRIBUTE_DISPLAY_NAME = "displayName";

    public static final String ATTRIBUTE_ORGANIZATION = "organization";

    public static final String ATTRIBUTE_COMMENT = "comment";

    public static final String USERNAME_REGEX = "[A-Za-z0-9]{2,16}";

    @PrimaryKey
    private String username;

    private String digestPassword;

    private UserRight right;

    private HashMap<String, String> attrs;

    private Account() {
    }

    public Account(String username, String password, UserRight right, Map<String, String> attrs) {
        if (username == null || password == null || right == null) throw new NullPointerException();
        this.username = username;
        this.digestPassword = calculateDigest(password);
        this.right = right;
        if (attrs != null) this.attrs = new HashMap<String, String>(attrs); else this.attrs = new HashMap<String, String>();
    }

    public static Account getAnonymousAccount() {
        Account anon = new Account();
        anon.username = null;
        anon.digestPassword = null;
        anon.right = new UserRight();
        anon.attrs = new HashMap<String, String>();
        return anon;
    }

    public static Account createDefaultRootAccount() {
        Account root = new Account();
        root.username = ROOT_USERNAME;
        root.digestPassword = calculateDigest(ROOT_DEFAULT_PASSWORD);
        root.right = new UserRight(new String[] { "user", "admin" });
        root.attrs = new HashMap<String, String>();
        return root;
    }

    public String getUsername() {
        return this.username;
    }

    public boolean matchPassword(String password) {
        return (calculateDigest(password).equals(this.digestPassword));
    }

    public void updatePassword(String password) {
        this.digestPassword = calculateDigest(password);
    }

    public String getAttribute(String attr) {
        return this.attrs.get(attr);
    }

    public void setAttribute(String attr, String value) {
        this.attrs.put(attr, value);
    }

    public UserRight getUserRight() {
        return this.right;
    }

    public void setUserRight(UserRight right) {
        this.right = right;
    }

    public Object clone() {
        Account that = new Account();
        that.username = this.username;
        that.digestPassword = this.digestPassword;
        that.right = this.right;
        that.attrs = new HashMap<String, String>(this.attrs);
        return that;
    }

    public static String calculateDigest(String plain) {
        try {
            byte[] bytes = getDigest().digest(plain.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02d", ((int) b) & 0xff));
            return sb.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static MessageDigest getDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
