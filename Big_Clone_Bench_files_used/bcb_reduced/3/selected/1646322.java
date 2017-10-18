package edu.cmu.vlis.wassup.databean;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class User implements Comparable<User>, Serializable {

    private String userName = null;

    private String hashedPassword = "*";

    private int salt = 0;

    private String firstName = null;

    private String lastName = null;

    private String street = null;

    private String city = null;

    private String state = null;

    private String country = null;

    private String zipcode = null;

    private boolean activated = false;

    public User(String userName) {
        this.userName = userName;
    }

    public boolean isPasswordMatch(String password) {
        return hashedPassword.equals(hash(password));
    }

    public int compareTo(User other) {
        int c = lastName.compareTo(other.lastName);
        if (c != 0) return c;
        c = firstName.compareTo(other.firstName);
        if (c != 0) return c;
        return userName.compareTo(other.userName);
    }

    public boolean equals(Object obj) {
        if (obj instanceof User) {
            User other = (User) obj;
            return userName.equals(other.userName);
        }
        return false;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public String getUserName() {
        return userName;
    }

    public int getSalt() {
        return salt;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int hashCode() {
        return userName.hashCode();
    }

    public void setHashedPassword(String x) {
        hashedPassword = x;
    }

    public void setPassword(String s) {
        salt = newSalt();
        hashedPassword = hash(s);
    }

    public void setSalt(int x) {
        salt = x;
    }

    public void setFirstName(String s) {
        firstName = s;
    }

    public void setLastName(String s) {
        lastName = s;
    }

    public String toString() {
        return "userid # " + userName + " status = " + activated;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    private String hash(String clearPassword) {
        if (salt == 0) return null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Can't find the SHA1 algorithm in the java.security package");
        }
        String saltString = String.valueOf(salt);
        md.update(saltString.getBytes());
        md.update(clearPassword.getBytes());
        byte[] digestBytes = md.digest();
        StringBuffer digestSB = new StringBuffer();
        for (int i = 0; i < digestBytes.length; i++) {
            int lowNibble = digestBytes[i] & 0x0f;
            int highNibble = (digestBytes[i] >> 4) & 0x0f;
            digestSB.append(Integer.toHexString(highNibble));
            digestSB.append(Integer.toHexString(lowNibble));
        }
        String digestStr = digestSB.toString();
        return digestStr;
    }

    private int newSalt() {
        Random random = new Random();
        return random.nextInt(8192) + 1;
    }
}
