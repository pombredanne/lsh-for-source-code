package edu.washington.mysms.security;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Random;

/**
 * A validated login is created by a LoginValidator from a Login
 * object.  A validated login represents a login whose existence and
 * password have been checked against some internal database or other
 * service.  An appropriate SQL account should be attached to a
 * validated login and other relevant permissions should also be added
 * by the validator.
 * 
 * @author Anthony Poon
 */
public abstract class ValidatedLogin extends Login {

    private static final char HEX[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    private static final char SALT[] = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private static final int SALT_LENGTH = 15;

    private SqlAccount sqlAccount;

    protected HashSet<String> permissionTokens;

    protected ValidatedLogin(String username, String passwordHash, SqlAccount sqlAccount) {
        super(username, passwordHash);
        permissionTokens = new HashSet<String>();
        this.sqlAccount = sqlAccount;
    }

    public SqlAccount getSqlAccount() {
        return this.sqlAccount;
    }

    protected void setSqlAccount(SqlAccount sqlAccount) {
        this.sqlAccount = sqlAccount;
    }

    /**
	 * Give this validated login permissions on a particular object with
	 * the given name.
	 * 
	 * @param name The name of the object.
	 */
    protected void grantPermission(String name) {
        this.permissionTokens.add(name);
    }

    /**
	 * Check to see if the login has permissions on the given name.
	 * 
	 * @param name the name of the object.
	 * @return True if has permission.
	 */
    public boolean hasPermission(String name) {
        return this.permissionTokens.contains(name);
    }

    /**
	 * Creates a new salt.  This utility method is called when creating
	 * a user for the first time.
	 * 
	 * @return
	 */
    public static String getNewRandomSalt() {
        Random rand = new Random();
        StringBuffer buf = new StringBuffer(SALT_LENGTH);
        for (int i = 0; i < SALT_LENGTH; i++) {
            buf.append(SALT[rand.nextInt(SALT.length)]);
        }
        return buf.toString();
    }

    /**
	 * Hash the given password with the given salt.  If salt is null, no salt is used.
	 * 
	 * @param password
	 * @param salt
	 * @return
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 */
    public static String getPasswordHash(String password, String salt) throws IOException, NoSuchAlgorithmException {
        if (password == null) {
            throw new NullPointerException("Cannot hash null password.");
        }
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        String text = (salt == null) ? password : password + salt;
        ByteArrayOutputStream ba_out = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(ba_out, Charset.forName("UTF-8"));
        writer.write(text);
        writer.close();
        byte[] bytes = ba_out.toByteArray();
        byte[] hash = new byte[40];
        md.update(bytes, 0, bytes.length);
        hash = md.digest();
        return toHexString(hash);
    }

    /**
	 * Converts an array of bytes into a hex-based String representation.
	 * 
	 * @param bytes
	 * @return
	 */
    public static String toHexString(byte[] bytes) {
        StringBuffer buf = new StringBuffer();
        for (byte b : bytes) {
            buf.append(HEX[(b >>> 4) & 0x0F]);
            buf.append(HEX[b & 0x0F]);
        }
        return buf.toString();
    }
}
