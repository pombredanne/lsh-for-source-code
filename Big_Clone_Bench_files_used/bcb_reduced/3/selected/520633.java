package com.tensegrity.palorcp.security;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.tensegrity.palorcp.PalorcpPlugin;

/**
 * {@<describe>}
 * <p>
 * This class manages locally stored users and there passwords.
 * </p>
 * {@</describe>}
 *
 * @author ArndHouben
 * @version $Id$
 */
public class PasswordManager {

    private final String LINE_END = "\r\n";

    private final File passwd;

    private final HashMap passwdList;

    private static PasswordManager instance;

    public static final PasswordManager getInstance() {
        if (instance == null) instance = new PasswordManager();
        return instance;
    }

    private PasswordManager() {
        String directory = PalorcpPlugin.getDefault().getStateLocation().toOSString();
        passwd = new File(directory + File.separator + "passwd");
        passwdList = new HashMap();
        reload();
    }

    /**
	 * Checks if the given user with the given password is known. 
	 * @param user the username
	 * @return true if the user and password are known, false otherwise
	 */
    public final boolean contains(String user) {
        return passwdList.containsKey(user);
    }

    /**
	 * Checks if the given user with the given password is known. 
	 * <b>Note:</b> that the password should not be encrypted
	 * @param user the username
	 * @param pass the not encrypted password 
	 * @return true if the user and password are known, false otherwise
	 */
    public final boolean isPasswordCorrect(String user, String pass) {
        if (passwdList.containsKey(user)) {
            try {
                String password = encrypt(pass);
                return ((String) passwdList.get(user)).equals(password);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
	 * Returns all known user names.
	 * @return the user names.
	 */
    public final String[] getUsers() {
        Set users = passwdList.keySet();
        return (String[]) users.toArray(new String[users.size()]);
    }

    /**
	 * Reads in the password file again.
	 * @return true if reading password file was successful, false otherwise
	 */
    public final boolean reload() {
        boolean successful = true;
        try {
            readPasswordFile();
        } catch (PasswordFileException e) {
            e.printStackTrace();
            successful = false;
        }
        return successful;
    }

    /**
	 * Returns a mapping with user name as key and encrypted password as value
	 * @return
	 */
    private final void readPasswordFile() throws PasswordFileException {
        try {
            checkPasswordFile();
        } catch (Exception e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.PasswordFileException"), e);
        }
        BufferedReader reader = null;
        try {
            String line;
            reader = new BufferedReader(new FileReader(passwd));
            while ((line = reader.readLine()) != null) {
                int cutIndex = line.indexOf(":");
                if (cutIndex > 0) {
                    String name = line.substring(0, cutIndex);
                    String pass = line.substring(cutIndex + 1);
                    passwdList.put(name, pass);
                }
            }
        } catch (FileNotFoundException e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.NoPasswordFileException"), e);
        } catch (IOException e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.ReadPasswordFileException"), e);
        } finally {
            if (reader != null) try {
                reader.close();
            } catch (IOException e) {
            }
        }
    }

    /**
	 * Writes the given user to the specified password file
	 * @param passwd
	 * @param user
	 * @param pass
	 * @param encrypt true to encrypt the password, false if no encryption is required 
	 * @throws PasswordFileException
	 */
    public final void add(String user, String pass, boolean encrypt) throws PasswordFileException {
        StringBuffer login = new StringBuffer();
        PrintWriter writer = null;
        String password = pass;
        try {
            writer = new PrintWriter(new FileWriter(passwd, true), true);
            login.append(user);
            login.append(":");
            if (encrypt) password = encrypt(pass);
            login.append(password);
            writer.println(login.toString());
        } catch (Exception e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.WritePasswordFileException"), e);
        } finally {
            passwdList.put(user, password);
            if (writer != null) writer.close();
        }
    }

    public final void remove(String user) throws PasswordFileException {
        try {
            internal_replace(user, "");
        } catch (IOException e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.RemoveUserFailed") + user, e);
        }
    }

    /**
	 * Updates the password of the given user. 
	 * @param user the user to update
	 * @param newpass the new password
	 * @param encrypt set to true to store new password encrypted, false otherwise
	 * @throws PasswordFileException
	 */
    public final void update(String user, String newpass, boolean encrypt) throws PasswordFileException {
        try {
            String password = encrypt ? encrypt(newpass) : newpass;
            if (((String) passwdList.get(user)).equals(password)) return;
            internal_update(user, password);
            passwdList.put(user, password);
        } catch (Exception e) {
            throw new PasswordFileException(SecurityMessages.getString("PasswordManager.UpdatingUserFailed"), e);
        }
    }

    /**
	 * Creates a passwd file with default login if it does not exist.
	 * @param passwd
	 * @throws IOException 
	 * @throws NoSuchAlgorithmException 
	 * @throws PasswordFileException 
	 */
    private final void checkPasswordFile() throws PasswordFileException {
        if (!passwd.exists()) {
            add("admin", "21232f297a57a5a743894a0e4a801fc3", false);
        }
    }

    private final String encrypt(String str) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes());
        return asHexString(md.digest());
    }

    private final String asHexString(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            result.append(Integer.toHexString(0x0100 + (bytes[i] & 0x00FF)).substring(1));
        }
        return result.toString();
    }

    private final void internal_update(String user, String pass) throws IOException {
        StringBuffer replacement = new StringBuffer();
        replacement.append(user);
        replacement.append(":");
        replacement.append(pass);
        replacement.append(LINE_END);
        internal_replace(user, replacement.toString());
    }

    /**
	 * Replaces a user:password line specified by user parameter with the given
	 * replacement
	 * @param user specifies the user:password line
	 * @param replacement replaces the complete line
	 * @throws IOException
	 */
    private final synchronized void internal_replace(String user, String replacement) throws IOException {
        StringBuffer pattern = new StringBuffer();
        pattern.append(user);
        pattern.append(":");
        pattern.append(".*");
        pattern.append(LINE_END);
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(passwd));
        int fileLength = (int) passwd.length();
        byte[] content = new byte[fileLength];
        in.read(content, 0, fileLength);
        in.close();
        Pattern p = Pattern.compile(pattern.toString());
        Matcher m = p.matcher(new String(content));
        String replaced = m.replaceAll(replacement);
        PrintStream ps = new PrintStream(new FileOutputStream(passwd.getAbsolutePath()));
        ps.print(replaced);
        ps.close();
    }
}
