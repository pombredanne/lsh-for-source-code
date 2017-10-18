package net.fdukedom.epicurus.domain.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.security.*;

/**
 * Implements user entity.
 *
 * @author Sergey Vishnyakov
 */
public final class User {

    /**
     * User's sex is undefinied.
     */
    public static final short SEX_UNDEFINIED = 0;

    /**
     * User is male.
     */
    public static final short SEX_MALE = 1;

    /**
     * User is female.
     */
    public static final short SEX_FEMALE = 2;

    /**
     * User's id.
     */
    private int id = 0;

    /**
     * User's name.
     */
    private String name;

    /**
     * User's id.
     */
    private String email;

    /**
     * User's password.
     */
    private String password;

    /**
     * User's sex.
     */
    private short sex = SEX_UNDEFINIED;

    /**
     * User's rating.
     */
    private int rating = 0;

    /**
     * If user is root.
     */
    private boolean isRoot = false;

    /**
     * If user is root.
     */
    private boolean isActivated = false;

    /**
     * User's registration date.
     */
    private Date registered = new Date();

    /**
     * Groups.
     */
    private Set<Group> groups = new HashSet<Group>();

    /**
     * Default constructor. Required by hibernate.
     */
    public User() {
    }

    /**
     * General constructor. Sets all required data.
     *
     * @param name user name
     * @param email user email
     * @param password user password
     */
    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    /**
     * Returns email.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets email.
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Returns id.
     *
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * Sets id.
     *
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * Returns if user is root.
     *
     * @return if user is root
     */
    public boolean getIsRoot() {
        return isRoot;
    }

    /**
     * Sets user root rights.
     *
     * @param isRoot the isRoot to set
     */
    public void setIsRoot(boolean isRoot) {
        this.isRoot = isRoot;
    }

    /**
     * Returns name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns password.
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets password.
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns rating.
     *
     * @return the rating
     */
    public int getRating() {
        return rating;
    }

    /**
     * Sets rating.
     *
     * @param rating the rating to set
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Returns registered date.
     *
     * @return the registered date
     */
    public Date getRegistered() {
        return registered;
    }

    /**
     * Sets registered date.
     *
     * @param registered the registred to set
     */
    public void setRegistered(Date registered) {
        this.registered = registered;
    }

    /**
     * Returns sex.
     *
     * @return the sex
     */
    public short getSex() {
        return sex;
    }

    /**
     * Sets sex.
     *
     * @param sex the sex to set
     */
    public void setSex(short sex) {
        this.sex = sex;
    }

    /**
     * Returns if user is activated.
     *
     * @return if user is activated
     */
    public boolean getIsActivated() {
        return isActivated;
    }

    /**
     * Sets if user is activated.
     *
     * @param isActivated the isActivated to set
     */
    public void setIsActivated(boolean isActivated) {
        this.isActivated = isActivated;
    }

    /**
     * Hash function.
     *
     * @param pass password
     *
     * @return password's hash 
     */
    public static String hash(String pass) {
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
            byte[] hash = algorithm.digest(pass.getBytes());
            hash = algorithm.digest(hash);
            StringBuffer buf = new StringBuffer();
            for (byte aHash : hash) {
                buf.append(Byte.toString(aHash));
            }
            return buf.toString();
        } catch (NoSuchAlgorithmException e) {
            return pass;
        }
    }

    /**
     * @return the groups
     */
    public Set<Group> getGroups() {
        return groups;
    }

    /**
     * @param groups the groups to set
     */
    public void setGroups(Set<Group> groups) {
        this.groups = groups;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final User user = (User) o;
        if (!email.equals(user.email)) return false;
        return true;
    }

    public int hashCode() {
        int result;
        result = id;
        result = 29 * result + email.hashCode();
        return result;
    }

    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append("{id: ");
        text.append(id);
        text.append(", name: ");
        text.append(name);
        text.append(", e-mail: ");
        text.append(email);
        text.append(", rating: ");
        text.append(rating);
        text.append(", is activated: ");
        text.append(isActivated);
        text.append(", is root: ");
        text.append(isRoot);
        text.append(", registered at: ");
        text.append(registered);
        text.append(", sex: ");
        text.append(sex);
        text.append("}");
        return text.toString();
    }
}
