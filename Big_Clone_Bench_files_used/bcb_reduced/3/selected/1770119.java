package pl.edu.agh.iosr.gamblingzone.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.persistence.AttributeOverride;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import org.jboss.seam.annotations.Name;
import org.hibernate.validator.Length;
import org.hibernate.validator.Email;

/**
 * The Class User.
 */
@AttributeOverride(name = "id", column = @Column(name = "USER_ID"))
@Entity(name = "USERS")
@Name("user")
@Inheritance(strategy = InheritanceType.JOINED)
public class User extends EntityModel {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -539255632801149469L;

    /** The first name. */
    @Column(name = "USER_FIRST_NAME", nullable = false)
    private String firstName;

    /** The surname. */
    @Column(name = "USER_SURNAME", nullable = false)
    private String surname;

    /** The login name. */
    @Length(min = 5, max = 15)
    @Column(name = "USER_LOGIN_NAME", nullable = false, unique = true)
    private String loginName;

    /** The email. */
    @Email
    @Column(name = "USER_EMAIL", nullable = false)
    private String email;

    /** The birth date. */
    @Column(name = "USER_BIRTHDAY", nullable = false)
    private Date birthDate;

    @Column(name = "USER_PASSWORD", nullable = false)
    private String password;

    /** The register date. */
    @Column(name = "USER_REGISTER_DATE", nullable = false)
    private Date registerDate = new Date();

    /** The last visit date. */
    @Column(name = "USER_LAST_VISIT_DATE", nullable = false)
    private Date lastVisitDate = new Date();

    /** The is blocked. */
    @Column(name = "USER_IS_BLOCKED", nullable = false)
    private boolean isBlocked = false;

    /** The is canceled. */
    @Column(name = "USER_IS_CANCELLED", nullable = false)
    private boolean isCanceled = false;

    /** The balance. */
    @Column(name = "USER_BALANCE", nullable = false)
    private Long balance = 0L;

    /** The temporary betting slip. */
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "USER_TEMP_BETTING_SLIP_ID", nullable = false)
    private BettingSlip tempBettingSlip;

    /** The betting slips. */
    @OneToMany(mappedBy = "owner", fetch = FetchType.EAGER)
    private List<BettingSlip> bettingSlips = new ArrayList<BettingSlip>();

    /** The role. */
    @org.hibernate.annotations.CollectionOfElements(targetElement = String.class)
    @JoinTable(name = "USER_ROLE", joinColumns = @JoinColumn(name = "USER_ID"))
    @Column(name = "USER_ROLE", nullable = false)
    private Set<String> role = new HashSet<String>();

    /**
     * Gets the first name.
     * 
     * @return the first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the first name.
     * 
     * @param firstName the new first name
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the surname.
     * 
     * @return the surname
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Sets the surname.
     * 
     * @param surname the new surname
     */
    public void setSurname(String surname) {
        this.surname = surname;
    }

    /**
     * Gets the login name.
     * 
     * @return the login name
     */
    public String getLoginName() {
        return loginName;
    }

    /**
     * Sets the login name.
     * 
     * @param loginName the new login name
     */
    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    /**
     * Gets the email.
     * 
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     * 
     * @param email the new email
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the birth date.
     * 
     * @return the birth date
     */
    public Date getBirthDate() {
        return birthDate;
    }

    /**
     * Sets the birth date.
     * 
     * @param birthDate the new birth date
     */
    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    /**
     * Gets the password.
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Compute sh a1.
     * 
     * @param in the in
     * 
     * @return the string
     */
    public static String computeSHA1(String in) {
        String out = "";
        if (in != null || "".equals(in)) {
            byte[] textPassword = in.getBytes();
            try {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(textPassword);
                byte[] encryptedPassword = md.digest();
                for (Byte b : encryptedPassword) {
                    out += Integer.toHexString(b & 0xff);
                }
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

    /**
     * Sets the password.
     * 
     * @param password the new password
     */
    public void setPassword(String password) {
        this.password = computeSHA1(password);
    }

    /**
     * Gets the regiser date.
     * 
     * @return the regiser date
     */
    public Date getRegisterDate() {
        return registerDate;
    }

    /**
     * Sets the regiser date.
     * 
     * @param regiserDate the new regiser date
     */
    public void setRegisterDate(Date regiserDate) {
        this.registerDate = regiserDate;
    }

    /**
     * Gets the last visit date.
     * 
     * @return the last visit date
     */
    public Date getLastVisitDate() {
        return lastVisitDate;
    }

    /**
     * Sets the last visit date.
     * 
     * @param lastVisitDate the new last visit date
     */
    public void setLastVisitDate(Date lastVisitDate) {
        this.lastVisitDate = lastVisitDate;
    }

    /**
     * Checks if is blocked.
     * 
     * @return true, if is blocked
     */
    public boolean isBlocked() {
        return isBlocked;
    }

    /**
     * Checks if is blocked.
     * 
     * @return true, if is blocked
     */
    public boolean getIsBlocked() {
        return isBlocked();
    }

    /**
     * Sets the blocked.
     * 
     * @param isBlocked the new blocked
     */
    public void setBlocked(boolean isBlocked) {
        this.isBlocked = isBlocked;
    }

    /**
     * Sets the blocked.
     * 
     * @param isBlocked the new blocked
     */
    public void setIsBlocked(boolean isBlocked) {
        setBlocked(isBlocked);
    }

    /**
     * Checks if is cancelled.
     * 
     * @return true, if is cancelled
     */
    public boolean isCancelled() {
        return isCanceled;
    }

    /**
     * Checks if is cancelled.
     * 
     * @return true, if is cancelled
     */
    public boolean getIsCancelled() {
        return isCancelled();
    }

    /**
     * Sets the cancelled.
     * 
     * @param isCancelled the new cancelled
     */
    public void setCancelled(boolean isCancelled) {
        this.isCanceled = isCancelled;
    }

    /**
     * Sets the cancelled.
     * 
     * @param isCancelled the new cancelled
     */
    public void setIsCancelled(boolean isCancelled) {
        setCancelled(isCancelled);
    }

    /**
     * Gets the balance.
     * 
     * @return the balance
     */
    public Long getBalance() {
        return balance;
    }

    /**
     * Sets the balance.
     * 
     * @param balance the new balance
     */
    public void setBalance(Long balance) {
        this.balance = balance;
    }

    /**
     * Adds the betting slip.
     * 
     * Betting slip is added to accepted betting slip.
     * This operation creates bidirectional connection and automatically persist BettingSlip object.
     * 
     * @param b the b
     */
    public void addBettingSlip(BettingSlip b) {
        bettingSlips.add(b);
        b.setOwner(this);
    }

    /**
     * Gets the betting slips.
     * 
     * @return the betting slips
     */
    public List<BettingSlip> getBettingSlips() {
        return bettingSlips;
    }

    /**
     * Gets the temp betting slip.
     * 
     * @return the temp betting slip
     */
    public BettingSlip getTempBettingSlip() {
        return tempBettingSlip;
    }

    /**
     * Sets the temp betting slip.
     * 
     * @param tempBettingSlip the new temp betting slip
     */
    public void setTempBettingSlip(BettingSlip tempBettingSlip) {
        this.tempBettingSlip = tempBettingSlip;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        final User user = (User) o;
        if (!loginName.equals(user.loginName)) return false;
        if (!password.equals(user.password)) return false;
        return true;
    }

    public int hashCode() {
        int result = 17;
        result = 37 * result + getLoginName().hashCode();
        result = 37 * result + getPassword().hashCode();
        return result;
    }

    /**
	 * Gets the roles.
	 * 
	 * @return the roles
	 */
    public Set<String> getRoles() {
        return role;
    }

    /**
	 * Adds the role.
	 * 
	 * @param role the role
	 */
    public void addRole(String role) {
        this.role.add(role);
    }
}
