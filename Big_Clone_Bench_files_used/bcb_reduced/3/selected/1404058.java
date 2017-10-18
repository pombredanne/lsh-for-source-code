package pelore;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class User {

    private String name;

    private byte[] passHash;

    public User(String name, String pass) throws NoSuchAlgorithmException {
        setName(name);
        passHash = generateHash(pass);
    }

    public String getName() {
        return name;
    }

    public boolean testPassword(String password) throws NoSuchAlgorithmException {
        return Arrays.equals(generateHash(password), passHash);
    }

    public boolean changePassword(String oldPass, String newPass) throws NoSuchAlgorithmException {
        if (testPassword(oldPass)) {
            this.passHash = generateHash(newPass);
            return true;
        }
        return false;
    }

    private void setName(String name) {
        if (name == null || name.equals("")) {
            throw new IllegalArgumentException("Name must be non-null and non-empty");
        }
        this.name = name;
    }

    private byte[] generateHash(String s) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(s.getBytes());
        return md.digest();
    }

    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User other = (User) obj;
            return other.getName().equals(this.getName());
        }
        return false;
    }

    public String toString() {
        return getName();
    }
}
