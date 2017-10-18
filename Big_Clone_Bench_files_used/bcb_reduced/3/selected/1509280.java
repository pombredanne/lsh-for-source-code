package edu.cmu.vlis.wassup.db;

import java.io.Serializable;
import java.security.*;
import java.util.*;

public class User implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String e_mail = "";

    private String password = "";

    private String first_name = "";

    private String last_name = "";

    private String phone_number = "";

    private String street = "";

    private String city = "";

    private String state = "";

    private String country = "";

    private String zipcode = "";

    private int osalt;

    public User(String e_mail) {
        this.e_mail = e_mail;
    }

    public String getE_mail() {
        return e_mail;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name.toUpperCase();
    }

    public String getLast_name() {
        return last_name;
    }

    public void setLast_name(String last_name) {
        this.last_name = last_name.toUpperCase();
    }

    public String getPhone_number() {
        return phone_number;
    }

    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
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
        this.city = city.toUpperCase();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state.toUpperCase();
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country.toUpperCase();
    }

    public String getZipcode() {
        return zipcode;
    }

    public void setZipcode(String zipcode) {
        this.zipcode = zipcode;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getOsalt() {
        return osalt;
    }

    public void setOsalt(int osalt) {
        this.osalt = osalt;
    }

    /**
	 * Create a new salt for a certain user
	 * @return
	 */
    public int newSalt() {
        Random random = new Random();
        return random.nextInt(8192) + 1;
    }

    /**
	 * Get the hashvalue of user input clear-text password
	 * Stroe the hashvalue into the database to achieve the most degree of privacy
	 * @param clearPassword	the user input password
	 * @return hash value of the input
	 */
    public String hash(String clearPassword) {
        if (osalt == 0) return null;
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Can't find the SHA1 algorithm in the java.security package");
        }
        String saltString = String.valueOf(osalt);
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

    public boolean checkPassword(String p) {
        return password.equals(hash(p));
    }
}
