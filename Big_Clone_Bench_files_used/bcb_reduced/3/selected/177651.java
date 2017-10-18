package securus.entity;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.hibernate.annotations.ForeignKey;
import org.hibernate.annotations.Type;
import org.hibernate.validator.Email;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotNull;
import org.jboss.seam.annotations.security.management.UserFirstName;
import org.jboss.seam.annotations.security.management.UserLastName;
import org.jboss.seam.annotations.security.management.UserPassword;
import org.jboss.seam.annotations.security.management.UserPrincipal;
import org.jboss.seam.annotations.security.management.UserRoles;
import org.jboss.seam.security.management.PasswordHash;
import org.jboss.seam.util.Hex;
import securus.services.SecurusException;

/**
 * An instance of a user of the application. Used by JAAS to authenticate a
 * user. There should be one instance per user.
 * 
 * @author benjaminfayle
 * 
 */
@Entity
@Table(name = "s_user", uniqueConstraints = @UniqueConstraint(columnNames = "user_name"))
public class User extends MutableEntity implements java.io.Serializable, Comparable<User> {

    static final long serialVersionUID = 5617334495706397826L;

    private static final int MAX_DEVICES = 10;

    private Long userId;

    private Account account;

    private String userName;

    private String email;

    private String activationKey;

    private String firstName;

    private String lastName;

    private boolean active;

    private String passwordHash;

    private byte[] password2;

    private String language;

    @Column(name = "password2", length = 128)
    public byte[] getPassword2() {
        return password2;
    }

    public void setPassword2(byte[] encryptedPassword) {
        this.password2 = encryptedPassword;
    }

    private boolean temporaryPassword;

    private Set<Role> userRoles = new HashSet<Role>(0);

    private List<Device> devices = new ArrayList<Device>();

    private List<Note> notes = new LinkedList<Note>();

    private List<Event> ownedEvents = new LinkedList<Event>();

    private List<Event> invitedEvents = new LinkedList<Event>();

    private boolean storageServerEnabled;

    private Long defaultDevice;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "user_id", unique = true, nullable = false)
    public Long getUserId() {
        return this.userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    @Column(name = "user_name", unique = true, nullable = false, length = 50)
    @NotNull
    @UserPrincipal
    @Length(min = 4, max = 50)
    public String getUserName() {
        return this.userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Column(name = "email", nullable = false, length = 60)
    @NotNull
    @Length(max = 60)
    @Email
    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Column(name = "activation_key", length = 60)
    @Length(max = 60)
    public String getActivationKey() {
        return this.activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    @Column(name = "first_name", nullable = false, length = 30)
    @NotNull
    @Length(max = 30)
    @UserFirstName
    public String getFirstName() {
        return this.firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    @Column(name = "last_name", nullable = false, length = 30)
    @NotNull
    @Length(max = 30)
    @UserLastName
    public String getLastName() {
        return this.lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    @Column(name = "active", nullable = false)
    @NotNull
    @Type(type = "yes_no")
    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Column(name = "password", nullable = false, length = 128)
    @NotNull
    @Length(max = 128)
    @UserPassword(hash = "SHA")
    public String getPasswordHash() {
        return this.passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Column(name = "temporary_password", nullable = false)
    @NotNull
    @Type(type = "yes_no")
    public boolean isTemporaryPassword() {
        return this.temporaryPassword;
    }

    public void setTemporaryPassword(boolean temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    @ManyToOne(cascade = { CascadeType.REFRESH }, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @ForeignKey(name = "user_account")
    public Account getAccount() {
        return this.account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "user")
    public List<Device> getDevices() {
        return devices;
    }

    public void setDevices(List<Device> devices) {
        this.devices = devices;
    }

    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "owner")
    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    @OneToMany(cascade = { CascadeType.ALL }, mappedBy = "owner")
    public List<Event> getOwnedEvents() {
        return ownedEvents;
    }

    public void setOwnedEvents(List<Event> ownedEvents) {
        this.ownedEvents = ownedEvents;
    }

    @ManyToMany(mappedBy = "invitedUsers")
    public List<Event> getInvitedEvents() {
        return invitedEvents;
    }

    public void setInvitedEvents(List<Event> invitedEvents) {
        this.invitedEvents = invitedEvents;
    }

    @Transient
    public int getDeviceCount() {
        return devices.size();
    }

    public boolean isUserInRole(String roleName) {
        Set<Role> myRoles = getUserRoles();
        for (Role next : myRoles) {
            if (next.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    public void removeRole(Role role) {
        getUserRoles().remove(role);
        role.getRoleUsers().remove(this);
    }

    public void addRole(Role role) {
        getUserRoles().add(role);
        role.getRoleUsers().add(this);
    }

    @Transient
    public Device getPublicDevice() {
        Device result = null;
        List<Device> devicesTemp = getDevices();
        if (devicesTemp != null) {
            for (Device device : devicesTemp) {
                if (device.isPublic()) {
                    result = device;
                    break;
                }
            }
        }
        return result;
    }

    @UserRoles
    @ManyToMany(targetEntity = Role.class, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    @JoinTable(name = "s_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    public Set<Role> getUserRoles() {
        if (userRoles == null) {
            userRoles = new HashSet<Role>();
        }
        return userRoles;
    }

    public void setUserRoles(Set<Role> userRoles) {
        this.userRoles = userRoles;
    }

    /**
	 * @return true if the storage server for this device is ready for use
	 */
    @Column(name = "storage_server_enabled", nullable = false)
    @Type(type = "yes_no")
    public boolean isStorageServerEnabled() {
        return storageServerEnabled;
    }

    /**
	 * @param storageServerEnabled
	 *            the storageServerEnabled to set
	 */
    public void setStorageServerEnabled(boolean fileSystemReady) {
        this.storageServerEnabled = fileSystemReady;
    }

    @Column(name = "default_device")
    public Long getDefaultDevice() {
        return defaultDevice;
    }

    public void setDefaultDevice(Long defaultDevice) {
        this.defaultDevice = defaultDevice;
    }

    @Transient
    public String getDisplayName() {
        return toString();
    }

    @SuppressWarnings("deprecation")
    public static final String hash(String saltPhrase, String plainTextPassword) {
        return PasswordHash.instance().generateSaltedHash(plainTextPassword, saltPhrase, "SHA");
    }

    public String createPasswordHash(String password) {
        return hash(getUserName(), password);
    }

    public String createActivationKey() {
        return getMD5Hash(getLastName() + getUserName() + getEmail() + getFirstName()) + System.currentTimeMillis();
    }

    protected String getMD5Hash(final String msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            return new String(Hex.encodeHex(md5.digest(msg.getBytes("UTF-8"))));
        } catch (Exception exc) {
            throw new SecurusException(exc);
        }
    }

    @Override
    public String toString() {
        String myFullName = null;
        if (firstName != null) {
            if (lastName != null) {
                myFullName = firstName + " " + lastName;
            }
        } else {
            if (lastName != null) {
                myFullName = lastName;
            } else {
                myFullName = userName;
            }
        }
        return myFullName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!getClass().equals(obj.getClass())) {
            return false;
        }
        User x = (User) obj;
        return equals(userName, x.userName);
    }

    protected final boolean equals(final Object left, final Object right) {
        return (left != null) ? left.equals(right) : (right == null);
    }

    @Override
    public int hashCode() {
        return userName == null ? 0 : userName.hashCode();
    }

    @Override
    public int compareTo(User o) {
        return userName.compareTo(o.userName);
    }

    @Column(name = "language", length = 5)
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Transient
    public boolean isFake() {
        return account == null || account.getCurrentSubscription() == null;
    }

    @Transient
    public int getMaxDevices() {
        return MAX_DEVICES;
    }

    @Transient
    public String getLocale() {
        return getLanguage() + "_" + getAccount().getCountry();
    }
}
