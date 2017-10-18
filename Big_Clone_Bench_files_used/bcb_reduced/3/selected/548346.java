package au.gov.naa.digipres.dpr.model.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.CollectionOfElements;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.IndexColumn;
import au.gov.naa.digipres.dpr.core.Constants;
import au.gov.naa.digipres.dpr.dao.UserDAO;
import au.gov.naa.digipres.dpr.model.permission.Permission;

/**
 * This class represents a user within the DPR application.
 * A user consists of a unique ID, a user name, first name,
 * second name and a list of permissions.
 * 
 * When a user performs an action, the user id, rather than the
 * user name, is recorded in the database, to allow for usernames
 * changing in the future.
 */
@Entity
@Table(name = "dpr_user")
public class User implements Cloneable, Comparable<User> {

    /**
	 * When this User was created.
	 */
    private Date dateCreated;

    /**
	 * The User that created this User.
	 */
    private User createdBy;

    /**
	 * The set of Permissions that have been granted to this User.
	 */
    private Collection<UserPermission> permissions;

    /**
	 * The userName of this User. The userName is guarranteed to be unique.
	 */
    private String userName;

    /**
	 * The first name of this User.
	 */
    private String firstName;

    /**
	 * The last name of the User.
	 */
    private String lastName;

    /**
	 * The unique identifier of this User in the database
	 */
    private String id;

    /**
	 * If true, then this User has been disabled and should not be permitted
	 * to perform any tasks in the system.
	 */
    private boolean disabled;

    /**
	 * The sessionId that this user is currently using.
	 */
    private String sessionId;

    private String encryptedPassword;

    /**
	 * Create a new User object. The empty constuctor to be used by hibernate
	 */
    public User() {
        permissions = new HashSet<UserPermission>();
    }

    @Override
    public Object clone() {
        User newUser = new User();
        newUser.setId(id);
        newUser.setUserName(userName);
        newUser.setLastName(lastName);
        newUser.setFirstName(firstName);
        newUser.setDisabled(disabled);
        newUser.setDateCreated(dateCreated);
        newUser.setCreatedBy(createdBy);
        newUser.setPermissions(new HashSet<UserPermission>(permissions));
        return newUser;
    }

    /**
	 * Get all the 'parent' user objects for this user, including
	 * the current user. That is, get the current user, the 
	 * user that created the current user, then the user
	 * that created that one, and so on until the 'root' user is 
	 * obtained. The root user being one which has 'createdBy' set
	 * to null.
	 * @param user for which we want the user tree for
	 * @return A set of users starting at the root user and ending
	 * with the supplied user.
	 */
    @Transient
    public static List<User> getUserTree(User user) {
        List<User> users = new ArrayList<User>();
        User parentUser = user.getCreatedBy();
        if (parentUser != null) {
            users.addAll(0, getUserTree(parentUser));
        }
        users.add(user);
        return users;
    }

    @Override
    public boolean equals(Object arg0) {
        if (this == arg0) {
            return true;
        }
        if (arg0 instanceof User) {
            User other = (User) arg0;
            if (!other.getId().equals(id)) {
                return false;
            }
            if (!other.getUserName().equals(userName)) {
                return false;
            }
            if (!other.getFirstName().equals(firstName)) {
                return false;
            }
            if (!other.getLastName().equals(lastName)) {
                return false;
            }
            if (other.isDisabled() != disabled) {
                return false;
            }
            if (createdBy != null) {
                if (other.getCreatedBy() == null) {
                    return false;
                } else if (!other.getCreatedBy().equals(createdBy)) {
                    return false;
                }
            } else if (other.getCreatedBy() != null) {
                return false;
            }
            if (!other.getDateCreated().equals(dateCreated)) {
                if (other.getDateCreated().getTime() != dateCreated.getTime()) {
                    return false;
                }
            }
            return true;
        }
        return super.equals(arg0);
    }

    /** 
	 * @see java.lang.Object#hashCode(java.lang.Object)
	 * @return hashcode of username and datecreated, or -1 if null.
	 */
    @Override
    public int hashCode() {
        if (userName != null && dateCreated != null) {
            return userName.hashCode() + dateCreated.hashCode() + id.hashCode();
        }
        return -1;
    }

    /**
	 * Check to see if the user has the permission supplied.
	 * @param permission 
	 * @return True if the user has the permission, false otherwise.
	 */
    public boolean hasPermission(Permission permission) {
        for (UserPermission userPermission : permissions) {
            if (userPermission.getPermission().equals(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Check to see if the user has the permission supplied.
	 * @param permissionid
	 * @return True if the user has the permission, false otherwise.
	 */
    public boolean hasPermission(int permissionId) {
        for (UserPermission userPermission : permissions) {
            if (userPermission.getPermission().getPermissionId() == permissionId) {
                return true;
            }
        }
        return false;
    }

    /**
	 * Add a permission to the current user. This will record who granted the permission.
	 * If the user already has the permission, no action will be taken.
	 * @param permission The permission to add
	 * @param grantingUser The user who is granting the permission
	 */
    public void addPermission(Permission permission, User grantingUser) {
        if (!hasPermission(permission)) {
            UserPermission userPermission = new UserPermission();
            userPermission.setDateGranted(new Date());
            boolean unique = false;
            while (!unique) {
                unique = true;
                for (UserPermission oldPermission : permissions) {
                    if (oldPermission.getDateGranted().equals(userPermission.getDateGranted())) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(userPermission.getDateGranted());
                        calendar.set(Calendar.SECOND, calendar.get(Calendar.SECOND) + 1);
                        userPermission.setDateGranted(calendar.getTime());
                        unique = false;
                        break;
                    }
                }
            }
            userPermission.setGrantedBy(grantingUser);
            userPermission.setPermissionId(permission.getPermissionId());
            permissions.add(userPermission);
        }
    }

    /**
	 * Remove the supplied permission from the user.
	 * @param permission The permission to remove
	 */
    public void removePermission(Permission permission) {
        UserPermission userPermission;
        for (Iterator<UserPermission> iter = permissions.iterator(); iter.hasNext(); ) {
            userPermission = iter.next();
            if (userPermission.getPermission().equals(permission)) {
                permissions.remove(userPermission);
                break;
            }
        }
    }

    @Override
    public String toString() {
        return userName;
    }

    /**
	 * Return the username of the current user
	 * @return the user name for this user.
	 */
    @Column(name = "user_name")
    public String getUserName() {
        return userName;
    }

    /**
	 * Set the user name for the the user.
	 * @param userName the new username for this user
	 */
    public void setUserName(String userName) {
        this.userName = userName.toLowerCase();
    }

    /**
	 * Get the ID for this user
	 * @return The ID
	 */
    @Id
    @Column(name = "user_id")
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "uuid")
    public String getId() {
        return id;
    }

    /**
	 * Set the ID to the supplied value
	 * @param id The new ID to set
	 */
    public void setId(String id) {
        this.id = id;
    }

    /**
	 * Get the date the user was created
	 * @return The date created for the user
	 */
    @Column(name = "date_created")
    public Date getDateCreated() {
        return dateCreated;
    }

    /**
	 * Set the date created for this user.
	 * @param dateCreated the Date the user was created
	 */
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    /**
	 * Return the User that created this user. In the
	 * case of the built in super user, this may be null.
	 * @return The user that created this one.
	 */
    @ManyToOne(optional = true)
    @JoinColumn(name = "created_by")
    public User getCreatedBy() {
        return createdBy;
    }

    /**
	 * Set the user that created this user to be the supplied user object
	 * @param user The user that created this user.
	 */
    public void setCreatedBy(User user) {
        createdBy = user;
    }

    /**
	 * Return whether or not this user has been disabled.
	 * @return true if user is disabled, false otherwise
	 */
    @Column
    public boolean isDisabled() {
        return disabled;
    }

    /**
	 * Set the user's disabled status.
	 * @param b true if the user is disabled, false otherwise
	 */
    public void setDisabled(boolean b) {
        disabled = b;
    }

    /**
	 * Get the first name of the user.
	 * @return the first name of the user
	 */
    @Column(name = "first_name")
    public String getFirstName() {
        return firstName;
    }

    /**
	 * Set the first name of the user.
	 * @param string the new first name to set.
	 */
    public void setFirstName(String string) {
        firstName = string;
    }

    /**
	 * Get the last name of the user.
	 * @return the last name of the user
	 */
    @Column(name = "last_name")
    public String getLastName() {
        return lastName;
    }

    /**
	 * Set the last name of the user.
	 * @param string the new last name to set.
	 */
    public void setLastName(String string) {
        lastName = string;
    }

    /**
	 * This string is an encrypted representation of the users password.
	 * @return
	 */
    @Column(name = "encrypted_password")
    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    /**
	 * Set the password for the user.
	 * @param parram
	 */
    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public void setNewPassword(String password) throws InvalidPasswordException {
        encryptedPassword = encryptPassword(password);
    }

    /**
	 * Check if a password matches the stored password.
	 * @param password
	 * @return
	 */
    public boolean checkPasswordisValid(String password) {
        boolean result = false;
        try {
            String hashedSuppliedPassword = encryptPassword(password);
            if (hashedSuppliedPassword.equals(encryptedPassword)) {
                result = true;
            }
        } catch (InvalidPasswordException ipe) {
            result = false;
        }
        return result;
    }

    /**
	 * Hash the password, using a salt so reverse hashing tables cannot be used.
	 * @param password The String to be encrypted.
	 * @return The encrypted version of the password.
	 */
    private String encryptPassword(String password) throws InvalidPasswordException {
        if (password == null || password.length() == 0) {
            throw new InvalidPasswordException();
        }
        String currentEncryptedPassword = password;
        try {
            MessageDigest md = MessageDigest.getInstance(Constants.DEFAULT_CHECKSUM_ALGORITHM);
            currentEncryptedPassword = new String(currentEncryptedPassword + "NAA-digipres");
            byte[] byteArray = md.digest(currentEncryptedPassword.getBytes());
            String s;
            String currentHexString = "";
            for (byte element : byteArray) {
                s = Integer.toHexString(element & 0xFF);
                if (s.length() == 1) {
                    s = "0" + s;
                }
                currentHexString = currentHexString + s;
            }
            currentEncryptedPassword = currentHexString;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        return currentEncryptedPassword;
    }

    /**
	 * Get the session Id for this user. This is not persisted; it will
	 * be forgotten once the user logs out.
	 * @return The session id for this user.
	 */
    @Transient
    public String getSessionId() {
        return sessionId;
    }

    /**
	 * Set the session Id for this user. This is done during the logon method in the UserDAO.
	 * @see UserDAO#logon()
	 * 
	 * @param string The new session id.
	 */
    public void setSessionId(String string) {
        sessionId = string;
    }

    /**
	 * Get the user permissions for this user. 
	 * 
	 * <p>These are persisted as a collection of elements,
	 * the 'UserPermission' are an embeddable object, so we define a new table (users_permissions)
	 * and join it using the primary key of this table, naming it "user_id". That means that this
	 * table does not need a primary key.
	 * 
	 * 
	 * @return the list of permissions for this user.
	 */
    @CollectionOfElements(fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    @IndexColumn(name = "permission_id")
    @JoinTable(name = "users_permissions", joinColumns = { @JoinColumn(name = "user_id") })
    @Cascade(value = { CascadeType.ALL })
    public Collection<UserPermission> getPermissions() {
        return permissions;
    }

    /**
	 * Set the permissions to be the supplied list of permissions.
	 * @param permissions The permissions to set.
	 */
    public void setPermissions(Collection<UserPermission> permissions) {
        this.permissions = permissions;
    }

    public int compareTo(User user) {
        if (user != null) {
            return getUserName().compareTo(user.getUserName());
        }
        return -1;
    }

    public static class InvalidPasswordException extends Exception {

        private static final long serialVersionUID = 1L;

        public InvalidPasswordException() {
            super();
        }

        public InvalidPasswordException(String message) {
            super(message);
        }
    }
}
