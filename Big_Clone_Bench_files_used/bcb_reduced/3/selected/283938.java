package org.isurf.cpfr.security;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import sun.misc.BASE64Encoder;

@Entity
@Table
public class User {

    private static final long serialVersionUID = 1L;

    /** The name of this User object. */
    @Id
    @Column
    private String username = null;

    @ManyToOne
    @JoinColumn(name = "USER_ROLE")
    private Role role;

    /** The login password of the user */
    @Column(unique = false)
    private String password = null;

    /** The GTIN of the users organisation (needed for data level access) */
    @Column
    private String gln = null;

    /** The first name of the user */
    @Column
    private String firstName = null;

    /** The last name of the user */
    @Column
    private String lastName = null;

    /** Academic title of the user */
    @Column
    private String title = null;

    /** Street and house number of the site */
    @Column(unique = false)
    private String address = null;

    /** ZIP code of the site */
    @Column
    private String zip = null;

    /** City where the site is located */
    @Column(unique = false)
    private String city = null;

    /** State where the site is located */
    @Column(unique = false)
    private String state = null;

    /** Country where the site is located */
    @Column(unique = false)
    private String country = null;

    /** Company where the user works */
    @Column(unique = false)
    private String company = null;

    /** E-mail address of the user */
    @Column(unique = false)
    private String email = null;

    /** Phone number of the user */
    @Column(unique = false)
    private String phone = null;

    /** Mobile number of the user */
    @Column(unique = false)
    private String mobile = null;

    /** Date and time when the user was registered */
    @Column(unique = false)
    private Date creationDate = null;

    /** Date and time of the last login */
    @Column(unique = false)
    private Date lastLogin = null;

    public User() {
    }

    public User(String username, String password) throws EncryptionExecption {
        setCreationDate(new Date());
        setUsername(username);
        password = encryptPassword(password);
        setPassword(password);
    }

    public boolean checkPassword(String password) throws EncryptionExecption {
        password = encryptPassword(password);
        return this.getPassword().equals(password);
    }

    public boolean updatePassword(String oldPW, String newPW) throws EncryptionExecption {
        if (checkPassword(oldPW) == true) {
            newPW = encryptPassword(newPW);
            setPassword(newPW);
            return true;
        } else return false;
    }

    private final String encryptPassword(final String password) throws EncryptionExecption {
        if ((password == null) || (password.length() == 0)) {
            throw new NullPointerException();
        }
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            md.update((password).getBytes("UTF-8"));
            return new BASE64Encoder().encode(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new EncryptionExecption(e);
        } catch (UnsupportedEncodingException e) {
            throw new EncryptionExecption(e);
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    private String getPassword() {
        return password;
    }

    private void setPassword(String password) {
        this.password = password;
    }

    public String getGLN() {
        return gln;
    }

    public void setGLN(String gln) {
        this.gln = gln;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date creationDate) {
        this.creationDate = creationDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
