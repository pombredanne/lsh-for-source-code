package openadmin.model.control;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import openadmin.dao.Base;
import openadmin.jaas.Digest;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotNull;
import javax.faces.bean.*;

@ManagedBean(name = "user")
@Entity
@Table(name = "user", schema = "control", uniqueConstraints = @UniqueConstraint(columnNames = "description"))
public class User implements Base, java.io.Serializable {

    /** attribute that contains the user identifier*/
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** attribute that contains the user name*/
    @NotNull
    @Length(min = 1, max = 15)
    private String description;

    /** attribute that contains the password*/
    @NotNull
    @Length(min = 4, max = 100)
    private String password;

    /** attribute that contains the full name user*/
    @NotNull
    @Length(min = 4, max = 40)
    private String fullname;

    /** attribute that contains the identification card user*/
    @NotNull
    @Length(min = 8, max = 15)
    private String identifier;

    /** attribute that contains the date begin*/
    @Temporal(TemporalType.TIMESTAMP)
    @NotNull
    private Date datebegin;

    /** attribute that contains the date low */
    @Temporal(TemporalType.TIMESTAMP)
    private Date dateend;

    /** attribute that contains the language for application
	 * example ca; es; .....*/
    @NotNull
    @Length(min = 2, max = 2)
    private String language;

    /** attribute that contains if user is active*/
    @NotNull
    private Boolean active;

    /** Transient attribute that means that the system should make a log on any JPA operation of this class*/
    @Transient
    private boolean debuglog = true;

    /** Transient attribute that means that the system should make a historic on any JPA operation of this class*/
    @Transient
    boolean historiclog = false;

    /**
	 * Constructor of the class User.
	 * without parameters
	 */
    public User() {
    }

    /**
	 *  Constructor of the class User.
	 *  @param pDescription, is the description, name user.
	 *  @param password, is the password for user.
	 *  @param fullname, is the full name for user.
	 */
    public User(String pDescription, String pPassword, String pFullName) {
        this.description = pDescription;
        Digest di = new Digest();
        this.password = di.digest(pPassword);
        this.fullname = pFullName;
    }

    /** Getters and setters*/
    public Integer getId() {
        return this.id;
    }

    public void setId(int pId) {
        this.id = pId;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String pDescription) {
        this.description = pDescription;
    }

    public String getPassword() {
        return this.password;
    }

    /**
	 * <desc> Accessor of writing which gives us the encrypted password</desc>
	 * <pre> x is an instance of user</pre>
	 * <post> the encrypted password is stored</post>
	 * @param pPassword, is the password
	 */
    public void setPassword(String pPassword) throws NoSuchAlgorithmException {
        Digest di = new Digest();
        this.password = di.digest(pPassword);
    }

    public String getFullName() {
        return this.fullname;
    }

    public void setFullName(String pFullName) {
        this.fullname = pFullName.toUpperCase();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String pIdentifier) {
        this.identifier = pIdentifier.toUpperCase();
    }

    public Date getDateBegin() {
        return this.datebegin;
    }

    public void setDateBegin(Date pDateBegin) {
        this.datebegin = pDateBegin;
    }

    public Date getDateEnd() {
        return this.dateend;
    }

    public void setDateEnd(Date pDateEnd) {
        this.dateend = pDateEnd;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String pLanguage) {
        this.language = pLanguage;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean pActive) {
        this.active = pActive;
    }

    public boolean getDebugLog() {
        return debuglog;
    }

    public void setDebugLog(boolean pDebuglog) {
        debuglog = pDebuglog;
    }

    public boolean getHistoricLog() {
        return historiclog;
    }

    public void setHistoricLog(boolean historiclog) {
        this.historiclog = historiclog;
    }

    @Override
    public String toString() {
        return description + " " + fullname + " " + datebegin + " " + active;
    }
}
