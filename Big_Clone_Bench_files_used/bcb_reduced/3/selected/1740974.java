package hu.bme.aait.picstore.um;

import hu.bme.aait.picstore.entities.AccountState;
import hu.bme.aait.picstore.entities.Group;
import hu.bme.aait.picstore.entities.User;
import hu.bme.aait.picstore.exceptions.NoUserForActivationLinkException;
import hu.bme.aait.picstore.exceptions.TooOftenPasswordRecoveryException;
import hu.bme.aait.picstore.interfaces.UserFacadeLocal;
import hu.bme.aait.picstore.interfaces.UserFacadeRemote;
import hu.bme.aait.picstore.util.AbstractFacade;
import hu.bme.aait.picstore.util.TextUtil;
import java.util.Calendar;
import java.util.List;
import javax.ejb.ApplicationException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 * Session Bean implementation class UserManager
 */
@Stateless
@ApplicationException(rollback = true)
public class UserFacade extends AbstractFacade<User> implements UserFacadeLocal, UserFacadeRemote {

    @PersistenceContext(unitName = "PicStoreUnit")
    private EntityManager em;

    public UserFacade() {
        this(User.class);
    }

    /**
	 * @see AbstractFacade#AbstractFacade(Class<T>)
	 */
    public UserFacade(Class<User> entityClass) {
        super(entityClass);
    }

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Override
    public User createUser(String emailAddress, String password, String fullName, Group group, String digestedActivationKey) {
        User user = new User();
        user.setEmailAddress(emailAddress);
        user.setPassword(password);
        user.setActivationKey(digestedActivationKey);
        user.setAccountState(AccountState.NOT_CONFIRMED);
        user.setName(fullName);
        user.setGroup(group);
        create(user);
        return user;
    }

    @Override
    public void activateAccount(String activationId) throws NoUserForActivationLinkException {
        User accountToActivate = findUserByActivationLink(activationId);
        if (accountToActivate == null) {
            throw new NoUserForActivationLinkException(activationId);
        } else {
            accountToActivate.setAccountState(AccountState.ACTIVE);
            edit(accountToActivate);
        }
    }

    @Override
    public String requestNewPassword(String emailAddress) throws TooOftenPasswordRecoveryException {
        User user = find(emailAddress);
        Calendar currentTime = Calendar.getInstance();
        Calendar lastRequestTime = user.getRecoveryStarted();
        if (lastRequestTime != null) {
            Calendar allowedRequestTime = lastRequestTime;
            allowedRequestTime.add(Calendar.MINUTE, 30);
            if (currentTime.before(allowedRequestTime)) {
                throw new TooOftenPasswordRecoveryException();
            }
        }
        user.setRecoveryStarted(currentTime);
        String digestedConfirmKey = TextUtil.digest(emailAddress + ":" + user.getName() + ":" + TextUtil.getRandomString(10));
        if (digestedConfirmKey == null) {
            throw new RuntimeException();
        }
        user.setPasswordGenerationConfirmKey(digestedConfirmKey);
        edit(user);
        return digestedConfirmKey;
    }

    @Override
    public String generateNewPassword(String confirmKey) throws NoUserForActivationLinkException {
        User accountToResetPW = findUserByConfirmKey(confirmKey);
        if (accountToResetPW == null) {
            throw new NoUserForActivationLinkException(confirmKey);
        }
        String newPassword = TextUtil.getRandomString(8);
        accountToResetPW.setPassword(newPassword);
        edit(accountToResetPW);
        return newPassword;
    }

    @SuppressWarnings("rawtypes")
    private User findUserByActivationLink(String activationKey) {
        Query queryUserByActivationLink = getEntityManager().createNamedQuery("findUserByActivationLink");
        queryUserByActivationLink.setParameter("actkey", activationKey);
        List users = queryUserByActivationLink.getResultList();
        if (users.size() != 1) {
            return null;
        } else {
            return (User) users.get(0);
        }
    }

    @SuppressWarnings("rawtypes")
    private User findUserByConfirmKey(String confirmKey) {
        Query queryUserByConfirmKey = getEntityManager().createNamedQuery("findUserByConfirmKey");
        queryUserByConfirmKey.setParameter("actkey", confirmKey);
        List users = queryUserByConfirmKey.getResultList();
        if (users.size() != 1) {
            return null;
        } else {
            return (User) users.get(0);
        }
    }

    @Override
    public User findByEmail(String email) {
        return find(email);
    }

    @Override
    public List<User> getAllUser() {
        return findAll();
    }

    @Override
    public void modify(User user, String password) {
        if (user == null || password == null) {
            throw new IllegalArgumentException();
        }
        user.setPassword(password);
        edit(user);
    }

    @Override
    public void modify(User user) {
        edit(user);
    }

    @Override
    public void setGroup(User user, Group group) {
        user.setGroup(group);
        edit(user);
    }
}
