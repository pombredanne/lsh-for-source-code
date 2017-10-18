package ar.com.omnipresence.security.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javax.annotation.security.PermitAll;
import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.security.auth.login.LoginException;
import ar.com.omnipresence.common.server.SessionBean;
import ar.com.omnipresence.configuration.server.PrivateVariableServices;
import ar.com.omnipresence.configuration.server.Variables;

/**
 * Session Bean implementation class PrivateSecurityServicesImpl
 */
@Stateless(name = "PrivateSecurityServicesBean")
@Local(PrivateSecurityServices.class)
public class PrivateSecurityServicesBean extends SessionBean implements PrivateSecurityServices {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(PrivateSecurityServicesBean.class.getName());

    private static final String DEFAULT_PASSWORD_ENCRIPTION_ALGORITHM = "SHA-1";

    private static final String DEFAULT_VALID_USERNAME_PATTERN = "^[A-Za-z0-9]{8,}$";

    @EJB
    private PrivateVariableServices variableServices;

    /**
     * Default constructor.
     */
    public PrivateSecurityServicesBean() {
    }

    @Override
    public User findUserByUsername(String userName) {
        return em.find(User.class, userName);
    }

    @Override
    public User getCurrentUser() {
        Principal principal = sessionContext.getCallerPrincipal();
        String userName = principal != null ? principal.getName() : null;
        return findUserByUsername(userName);
    }

    @PermitAll
    public User registerUser(String userName, String password, String realName, String email) {
        User user = new User(userName, password, realName, email);
        em.persist(user);
        return user;
    }

    public void authenticateUser(String userName, String password) throws LoginException {
        User user = em.find(User.class, userName);
        if (user != null) {
            try {
                user.authenticate(cypherPassword(password));
            } catch (NoSuchAlgorithmException e) {
                LogRecord logRecord = new LogRecord(Level.SEVERE, "Internal error");
                logRecord.setThrown(e);
                logger.log(logRecord);
                throw new LoginException("Internal error");
            }
        } else {
            throw new LoginException();
        }
    }

    private String cypherPassword(String password) throws NoSuchAlgorithmException {
        return new String(MessageDigest.getInstance(getPasswordEncryptionAlgorithm()).digest(password.getBytes()));
    }

    @Override
    public String getValidUserNamePattern() {
        return variableServices.getStringVariableValue(Variables.VALID_USERNAME_PATTERN, DEFAULT_VALID_USERNAME_PATTERN);
    }

    public String getPasswordEncryptionAlgorithm() {
        return variableServices.getStringVariableValue(Variables.PASSWORD_ENCRYPTION_ALGORITHM, DEFAULT_PASSWORD_ENCRIPTION_ALGORITHM);
    }

    public void checkUserConfirmation(String userName, String hash) throws AccountConfirmationException {
        User user = em.find(User.class, userName);
        if (user != null) {
            user.checkConfirmation(hash);
        } else {
            throw new AccountConfirmationException();
        }
    }
}
