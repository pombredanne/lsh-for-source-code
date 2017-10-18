package org.openeye.action;

import java.security.MessageDigest;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotEmpty;
import org.hibernate.validator.Pattern;
import org.jboss.seam.ScopeType;
import org.jboss.seam.core.Events;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.international.StatusMessages;
import static org.jboss.seam.international.StatusMessage.Severity.ERROR;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;
import org.jboss.seam.util.Hex;
import org.openeye.model.Gender;
import org.openeye.model.Preferences;
import org.openeye.model.ProfilePolicy;
import org.openeye.model.Role;
import org.openeye.model.User;

@Name("guest")
@Scope(ScopeType.EVENT)
public class GuestSupport {

    @Logger
    private Log log;

    @In
    private EntityManager entityManager;

    @In
    private Credentials credentials;

    @In(required = false)
    private PasswordSupport passwordSupport;

    @In(create = true)
    private StatusMessages statusMessages;

    @Out(required = false, scope = ScopeType.SESSION)
    private User currentUser;

    private User newUser;

    private String lostPasswordUserId;

    private String lostPasswordEmail;

    private String loginUserId;

    private String recoveredPasswordEmail;

    private String resetPassword;

    private boolean agreedToTermsOfUse = false;

    private boolean sendEmailConfirmation = false;

    @NotEmpty
    @Length(min = 4, max = 12)
    @Pattern(regex = "^[a-zA-Z\\d_]{4,12}$", message = "{invalid_screen_name}")
    private String registrationScreenName;

    public User getUser() {
        if (newUser == null) {
            newUser = new User();
            newUser.setPreferences(null);
            newUser.setUserName(null);
            newUser.setTemporaryPassword(false);
            newUser.setActive(false);
        }
        return newUser;
    }

    @Transactional
    public void doActivate(String activationKey) {
        Query q = entityManager.createQuery("from User u where u.active=0 AND u.activationKey=:activationKey");
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
            log.info("User {0} activated successfully.", loginUserId);
        }
    }

    @SuppressWarnings("deprecation")
    @Observer(Identity.EVENT_POST_AUTHENTICATE)
    public void postAuthenticate(Identity identity) {
        if (identity != null && identity.isLoggedIn() && this.currentUser == null) {
            String userName = identity.getUsername();
            if (userName == null) userName = identity.getPrincipal().getName();
            if (userName != null) {
                this.currentUser = byUserName(userName);
            }
        }
    }

    @Observer(Identity.EVENT_LOGIN_SUCCESSFUL)
    public void onLogin() {
        this.currentUser = byUserName(credentials.getUsername());
        log.info("User {0} logged in successfully.", this.currentUser.getUserName());
    }

    @Observer(Identity.EVENT_LOGIN_FAILED)
    public void onLoginFailed() {
        this.currentUser = null;
        statusMessages.addToControlFromResourceBundle("loginBtn", ERROR, "login_error");
    }

    @Transactional
    public void doRecoverLostPassword() throws Exception {
        if (lostPasswordUserId == null || lostPasswordEmail == null) return;
        lostPasswordEmail = lostPasswordEmail.trim();
        this.recoveredPasswordEmail = null;
        this.resetPassword = null;
        User theUser = byUserName(lostPasswordUserId);
        if (theUser == null) {
            statusMessages.addToControlFromResourceBundle("resetLostPassword", ERROR, "user_not_exist", lostPasswordUserId);
            return;
        }
        if (!theUser.getEmail().equalsIgnoreCase(lostPasswordEmail)) {
            statusMessages.addToControlFromResourceBundle("resetLostPassword", ERROR, "email_not_recognized");
            return;
        }
        try {
            this.resetPassword = PasswordSupport.tempPassword();
            theUser.setPasswordHash(PasswordSupport.hash(theUser.getUserName(), this.resetPassword));
            theUser.setTemporaryPassword(true);
            entityManager.flush();
            if (credentials != null) {
                credentials.setUsername(theUser.getUserName());
            }
            this.recoveredPasswordEmail = lostPasswordEmail;
            this.lostPasswordEmail = null;
            Events.instance().raiseTransactionSuccessEvent("passwordReset");
        } catch (Exception exc) {
            statusMessages.addToControlFromResourceBundle("resetLostPassword", ERROR, "reset_failed_unknown");
        }
    }

    @Transactional
    public void doRegister() {
        if (!passwordSupport.isConfirmed()) {
            statusMessages.addToControlFromResourceBundle("password", ERROR, "password_not_confirmed");
            return;
        }
        if (!agreedToTermsOfUse) {
            statusMessages.addToControlFromResourceBundle("registerButton", ERROR, "please_agree_to_terms");
            return;
        }
        try {
            newUser.setUserName(this.registrationScreenName);
            newUser.setActive(false);
            newUser.setPasswordHash(passwordSupport.getPasswordHash(this.registrationScreenName));
            newUser.setActivationKey(getMD5Hash(newUser.getLastName() + newUser.getUserName() + newUser.getEmail() + newUser.getFirstName()) + System.currentTimeMillis());
            newUser.setLanguage("en");
            newUser.setCreatedOn(System.currentTimeMillis());
            entityManager.persist(newUser);
            Preferences prefs = new Preferences();
            prefs.setUserId(newUser.getUserId());
            prefs.setCountry("SE");
            prefs.setProfilePolicy(ProfilePolicy.PRIVATE);
            prefs.setGender(Gender.SECRET);
            prefs.setStartPage("Personal Tasks");
            entityManager.persist(prefs);
            newUser.setPreferences(prefs);
            Role userRole = getRole("user");
            if (userRole == null) {
                userRole = new Role();
                userRole.setName("user");
                userRole.setDescription("General user role.");
                entityManager.persist(userRole);
            }
            newUser.addRole(userRole);
            if (newUser.getUserName().equals("admin")) {
                Role adminRole = getRole("admin");
                if (adminRole == null) {
                    adminRole = new Role();
                    adminRole.setName("admin");
                    adminRole.setDescription("Administration role.");
                    entityManager.persist(adminRole);
                }
                newUser.addRole(adminRole);
            }
            if (isSendEmailConfirmation()) {
                ExternalContext extCtxt = FacesContext.getCurrentInstance().getExternalContext();
                String activationLink = ((javax.servlet.http.HttpServletRequest) extCtxt.getRequest()).getRequestURL().toString();
                String newUserLink = activationLink + ((activationLink.indexOf("?") != -1) ? "&act=" : "?act=") + newUser.getActivationKey();
                Contexts.getEventContext().set("inactiveNewUser", new InactiveNewUser(newUser.toString(), newUser.getUserName(), newUser.getEmail(), newUserLink));
                Events.instance().raiseEvent("userRegistered");
            } else {
                doActivate(newUser.getActivationKey());
            }
            this.registrationScreenName = null;
            this.newUser = null;
        } catch (Exception exc) {
            log.error("Registration failed for {0}", exc, String.valueOf(this.registrationScreenName));
            statusMessages.addToControlFromResourceBundle("registerButton", ERROR, "general_reg_error");
        }
    }

    public boolean getShowLogin() {
        if (getRegisterError()) return false;
        if (getForgotPasswordError()) return false;
        boolean showLogin = false;
        if (loginUserId != null) {
            showLogin = true;
        } else {
            if (FacesSupport.hasErrorMessage(FacesContext.getCurrentInstance(), "loginForm:")) {
                showLogin = true;
            }
        }
        return showLogin;
    }

    public boolean getForgotPasswordError() {
        return FacesSupport.hasErrorMessage(FacesContext.getCurrentInstance(), "resetLostPassword");
    }

    public boolean getRegisterError() {
        return FacesSupport.hasErrorMessage(FacesContext.getCurrentInstance(), "registerForm:");
    }

    public void setRegistrationScreenName(String screenName) {
        if (hasUser(screenName)) {
            statusMessages.addToControlFromResourceBundle("userName", ERROR, "user_id_is_taken", screenName);
        } else {
            this.registrationScreenName = screenName;
        }
    }

    public String getRegistrationScreenName() {
        return registrationScreenName;
    }

    public String getLostPasswordUserId() {
        return lostPasswordUserId;
    }

    public void setLostPasswordUserId(String value) {
        this.lostPasswordUserId = value;
    }

    public String getLostPasswordEmail() {
        return lostPasswordEmail;
    }

    public void setLostPasswordEmail(String value) {
        this.lostPasswordEmail = value;
    }

    public String getLoginUserId() {
        return loginUserId;
    }

    public void setLoginUserId(String loginUserId) {
        this.loginUserId = loginUserId;
    }

    public String getRecoveredPasswordEmail() {
        return this.recoveredPasswordEmail;
    }

    public void setRecoveredPasswordEmail(String value) {
        this.recoveredPasswordEmail = value;
    }

    public String getResetPassword() {
        return this.resetPassword;
    }

    public void setResetPassword(String value) {
        this.resetPassword = value;
    }

    public boolean getAgreedToTermsOfUse() {
        return agreedToTermsOfUse;
    }

    public void setAgreedToTermsOfUse(boolean agreedToTermsOfUse) {
        this.agreedToTermsOfUse = agreedToTermsOfUse;
    }

    protected String getMD5Hash(final String msg) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            return new String(Hex.encodeHex(md5.digest(msg.getBytes("UTF-8"))));
        } catch (Exception exc) {
            throw new RuntimeException(exc);
        }
    }

    protected Role getRole(String roleName) {
        try {
            Query q = entityManager.createQuery("from Role r where r.name = :roleName");
            q.setParameter("roleName", roleName);
            return (Role) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            return null;
        }
    }

    protected boolean hasUser(String userName) {
        boolean hasUser = false;
        if (userName != null) {
            Query q = entityManager.createQuery("select u.userName from User u where u.userName = :userName");
            q.setParameter("userName", userName);
            hasUser = (q.getResultList().size() > 0);
        }
        return hasUser;
    }

    protected User byUserName(String userName) {
        try {
            Query q = entityManager.createQuery("from User u where u.userName = :userName");
            q.setParameter("userName", userName);
            return (User) q.getSingleResult();
        } catch (javax.persistence.NoResultException nre) {
            return null;
        }
    }

    public void setSendEmailConfirmation(boolean sendEmailConfirmation) {
        this.sendEmailConfirmation = sendEmailConfirmation;
    }

    public boolean isSendEmailConfirmation() {
        return sendEmailConfirmation;
    }
}
