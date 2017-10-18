package org.openeye.action;

import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import java.security.MessageDigest;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.Pattern;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.core.SeamResourceBundle;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.management.PasswordHash;
import org.jboss.seam.util.Hex;
import org.openeye.model.Gender;
import org.openeye.model.Preferences;
import org.openeye.model.ProfilePolicy;
import org.openeye.model.Role;
import org.openeye.model.User;

@Name("userService")
@Scope(ScopeType.EVENT)
public class UserService {

    @Logger
    private static Log log;

    @In(required = false)
    private FacesMessages facesMessages;

    @In(create = true, value = "entityManager")
    private EntityManager em;

    @In(required = false, create = true)
    private StatusMessages statusMessages;

    @NotEmpty
    @Length(min = 4, max = 12)
    @Pattern(regex = "^[a-zA-Z\\d_]{4,12}$", message = "{invalid_screen_name}")
    private String registrationScreenName;

    @NotEmpty
    @Length(min = 8, max = 12)
    @Pattern(regex = "(?=^.{8,}$)((?=.*\\d)|(?=.*\\W+))(?![.\\n])(?=.*[A-Z])(?=.*[a-z]).*$", message = "{insecure_password}")
    private String password = null;

    private String passwordConfirm;

    private String email;

    private String firstName;

    private String lastName;

    private String loginUserId;

    @In(required = false)
    private Credentials credentials;

    @Transactional
    public void createUser() {
        boolean passwordsConfirmed = true;
        if (password == null) {
            passwordsConfirmed = false;
        } else if (this.passwordConfirm == null) {
            passwordsConfirmed = false;
        } else if (!password.equals(passwordConfirm)) {
            passwordsConfirmed = false;
        }
        if (!passwordsConfirmed) {
            String message = SeamResourceBundle.getBundle().getString("openeye.passwordNotConfimed");
            statusMessages.addToControlFromResourceBundle("passwordInputField", ERROR, message);
            statusMessages.addToControlFromResourceBundle("passwordConfirmInputField", ERROR, message);
            return;
        }
        try {
            User newUser = new User();
            newUser.setUserName(registrationScreenName);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setEmail(email);
            newUser.setActive(true);
            newUser.setLanguage("en");
            newUser.setCreatedOn(System.currentTimeMillis());
            newUser.setPasswordHash(createPasswordHash(registrationScreenName));
            newUser.setActivationKey(getMD5Hash(newUser.getLastName() + newUser.getUserName() + newUser.getEmail() + newUser.getFirstName()) + System.currentTimeMillis());
            em.persist(newUser);
            Preferences prefs = new Preferences();
            prefs.setUserId(newUser.getUserId());
            prefs.setCountry("SE");
            prefs.setProfilePolicy(ProfilePolicy.PRIVATE);
            prefs.setGender(Gender.SECRET);
            prefs.setStartPage("Personal Tasks");
            em.persist(prefs);
            newUser.setPreferences(prefs);
            Role userRole = getRole("user");
            if (userRole == null) {
                userRole = new Role();
                userRole.setName("user");
                userRole.setDescription("General user role.");
                em.persist(userRole);
            }
            newUser.addRole(userRole);
            doActivate(newUser.getActivationKey());
            facesMessages.add("Added new user '" + firstName + " " + lastName + "'");
            clearFields();
        } catch (Exception e) {
            log.error("Registration failed for {0}", e, String.valueOf(this.registrationScreenName));
            String message = SeamResourceBundle.getBundle().getString("openeye.generalRegError");
            statusMessages.addToControlFromResourceBundle("registerButton", ERROR, message);
        }
    }

    @Transactional
    public void doActivate(String activationKey) {
        Query q = em.createQuery("from User u where u.active=0 AND u.activationKey=:activationKey");
        q.setParameter("activationKey", activationKey);
        User activatedUser = null;
        try {
            activatedUser = (User) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
        }
        if (activatedUser != null) {
            activatedUser.setActive(true);
            activatedUser.setActivationKey(null);
            loginUserId = activatedUser.getUserName();
            credentials.setUsername(loginUserId);
        }
    }

    public void close() {
        statusMessages.clear();
        clearFields();
    }

    private void clearFields() {
        registrationScreenName = null;
        password = null;
        passwordConfirm = null;
        email = null;
        firstName = null;
        lastName = null;
    }

    @SuppressWarnings({ "deprecation" })
    private String createPasswordHash(String saltPhrase) {
        return PasswordHash.instance().generateSaltedHash(password, saltPhrase, "SHA");
    }

    private boolean hasUser(String userName) {
        boolean hasUser = false;
        if (userName != null) {
            Query q = em.createQuery("select u.userName from User u where u.userName = :userName");
            q.setParameter("userName", userName);
            hasUser = (q.getResultList().size() > 0);
        }
        return hasUser;
    }

    private String getMD5Hash(final String msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            return new String(Hex.encodeHex(md5.digest(msg.getBytes("UTF-8"))));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    private Role getRole(String roleName) {
        try {
            Query q = em.createQuery("from Role r where r.name = :roleName");
            q.setParameter("roleName", roleName);
            return (Role) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            return null;
        }
    }

    public String getRegistrationScreenName() {
        return registrationScreenName;
    }

    public void setRegistrationScreenName(String screenName) {
        if (hasUser(screenName)) {
            String message = SeamResourceBundle.getBundle().getString("openeye.userIdIsTaken");
            statusMessages.addToControlFromResourceBundle("userName", ERROR, message, screenName);
        } else {
            this.registrationScreenName = screenName;
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String value) {
        this.password = (value != null && value.trim().length() > 0) ? value.trim() : null;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean getRegisterError() {
        return FacesSupport.hasErrorMessage(FacesContext.getCurrentInstance(), "registerForm:");
    }
}
