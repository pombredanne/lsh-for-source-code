package org.hyperimage.service.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;

/**
 * @author Jens-Martin Loebel
 */
@Entity
public class HIUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    String firstName;

    String lastName;

    @Column(unique = true, nullable = false)
    String userName;

    String passwordHash;

    String email;

    public HIUser() {
    }

    public HIUser(String firstName, String lastName, String email, String userName, String password) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.userName = userName;
        this.setPassword(password);
    }

    /**
	 * Returns a HexString representing the MD5 hash value of the given input string.
	 * @param inputString string for which to calculate the MD5 hash
	 * @return HexString containing the MD5 hash
	 */
    public static String getMD5HashString(String inputString) {
        String hashString = "";
        MessageDigest md5;
        byte[] digest;
        int curNum;
        final char[] hexTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            md5 = MessageDigest.getInstance("MD5");
            digest = md5.digest(inputString.getBytes());
            for (int i = 0; i < digest.length; i++) {
                curNum = digest[i];
                if (curNum < 0) curNum = curNum + 256;
                hashString = hashString + hexTable[curNum / 16] + hexTable[curNum % 16];
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5 not supported by current Java VM!");
            System.exit(1);
        }
        return hashString;
    }

    @Transient
    @XmlTransient
    public void setPassword(String password) {
        passwordHash = getMD5HashString(password);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @XmlTransient
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
