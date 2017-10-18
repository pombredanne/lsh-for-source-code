package net.sf.jiproute.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import org.apache.log4j.Logger;

public class SecurityUtils {

    private static Properties db;

    private static boolean isDbOpen;

    private static Logger log = Logger.getLogger(SecurityUtils.class.getName());

    public static boolean authenticate(String userId, String passwd) {
        String pw = null;
        SecurityUtils.openDb();
        pw = SecurityUtils.db.getProperty(userId);
        if (pw != null) {
            if (SecurityUtils.makeHash(passwd).equals(pw)) {
                log.info("User " + userId + " authenticated");
                return true;
            } else {
                log.info("Antempt to authenticate user " + userId + " failed");
                return false;
            }
        } else {
            log.info("User " + userId + " does not exist in the user database");
            return false;
        }
    }

    public static boolean changePasswd(String userId, String currentPasswd, String newPasswd) {
        if (!authenticate(userId, currentPasswd)) {
            log.info("Antempt to change password for user " + userId + " failed");
            return false;
        } else {
            db.setProperty(userId, makeHash(newPasswd));
            saveDb();
            log.info("Changed password for user " + userId);
            return true;
        }
    }

    private static void openDb() {
        if (SecurityUtils.isDbOpen) return;
        SecurityUtils.db = new Properties();
        try {
            FileInputStream f = new FileInputStream(new File("etc/passwd.db"));
            BufferedInputStream reader = new BufferedInputStream(f);
            SecurityUtils.db.load(reader);
        } catch (Exception e) {
            log.error("Error on opening the user database. Error Description: [" + e.getMessage() + "]");
        }
    }

    private static void saveDb() {
        try {
            FileOutputStream f = new FileOutputStream(new File(SecurityUtils.class.getResource("/etc/passwd.db").toURI()));
            BufferedOutputStream writer = new BufferedOutputStream(f);
            SecurityUtils.db.store(writer, "jIPRoute User's Database");
        } catch (Exception e) {
            log.error("Error on saving the user database. Error Description: [" + e.getMessage() + "]");
        }
    }

    public static String makeHash(String senha) {
        MessageDigest md = null;
        byte[] senhaHash = null;
        StringBuilder sb = new StringBuilder();
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
        }
        senhaHash = md.digest(senha.getBytes());
        for (int i = 0; i < senhaHash.length; i++) {
            int j = (int) (senhaHash[i] & 0x000000ff);
            if (j > 0xf) {
                sb.append(Integer.toHexString(j));
            } else {
                sb.append('0');
                sb.append(Integer.toHexString(j));
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
            System.exit(1);
        } else {
            openDb();
            db.setProperty(args[0], makeHash(args[1]));
            saveDb();
            log.info("New user or password set for user " + args[0]);
            System.exit(0);
        }
    }

    private static void usage() {
        System.out.println("Use java -cp jiproute.jar net.sf.jiproute.util.SecurityUtils [user] [password]");
        System.out.println("Example: java -cp jiproute.jar net.sf.jiproute.util.SecurityUtils admin admin");
        System.out.println("This will set a user named 'admin' whose password is 'admin' on etc/passwd.db");
    }
}
