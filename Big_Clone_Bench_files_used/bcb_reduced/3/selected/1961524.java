package collection.Security;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import javax.swing.*;
import sun.misc.BASE64Encoder;
import sun.misc.CharacterEncoder;

public class User {

    String username, passwordIn, passwordHash, passwordExpected;

    boolean authenticated;

    UserSubset userSubset;

    JFrame frame;

    String regex = "[\\w+|\\d+]+[\\w*|\\d*]*";

    String space = "\\s*";

    public User(String usernameIn, String passwordEntered) {
        username = usernameIn;
        passwordIn = passwordEntered;
        passwordHash = null;
        passwordExpected = null;
        authenticated = false;
    }

    public void setUsername(String usernameIn) {
        username = usernameIn;
    }

    public void setPasswordIn(String passwordEntered) {
        passwordIn = passwordEntered;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordIn() {
        return passwordIn;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public UserSubset getUserSubset() {
        return userSubset;
    }

    public boolean newUser() {
        if (this.scanForUser()) {
            JOptionPane.showMessageDialog(frame, "Username entered already exists in database. Please select another.", "Username Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (username.matches(regex) == false && username.matches(space) == true) {
            JOptionPane.showMessageDialog(frame, "Username has invalid characters.  Use only alphanumeric characters.", "Username Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } else {
            try {
                BufferedWriter userFile = new BufferedWriter(new FileWriter("users.txt", true));
                userFile.write(username + " ");
                this.encryptPassword();
                userFile.write(passwordHash + " ");
                userFile.close();
                authenticated = true;
                userFile.close();
                userSubset = new UserSubset(this);
                userSubset.initializeSubset();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    public void encryptPassword() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.out.print(e);
        }
        try {
            digest.update(passwordIn.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            System.out.println("cannot find char set for getBytes");
        }
        byte digestBytes[] = digest.digest();
        passwordHash = (new BASE64Encoder()).encode(digestBytes);
    }

    public void authenticate() {
        if (this.scanForUser()) ;
        {
            this.encryptPassword();
            if (passwordHash.equalsIgnoreCase(passwordExpected)) {
                authenticated = true;
            } else {
                JOptionPane.showMessageDialog(frame, "You have typed an incorrect username or password.", "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean scanForUser() {
        Scanner scan;
        String token;
        try {
            scan = new Scanner(new FileReader("users.txt"));
        } catch (FileNotFoundException notFoundException) {
            System.out.println("Missing users file, scanForUsername will abort.");
            return false;
        }
        while (scan.hasNext()) {
            token = scan.next();
            if (token.equalsIgnoreCase(username)) {
                passwordExpected = scan.next();
                System.out.println("scanning for username - current token: " + token);
                scan.close();
                return true;
            }
        }
        if (username.matches(space) == false) {
            System.out.println("scan returns false => *invalid* username.");
        } else {
            System.out.println("scan returns false => usernameentered not found.");
        }
        scan.close();
        return false;
    }
}
