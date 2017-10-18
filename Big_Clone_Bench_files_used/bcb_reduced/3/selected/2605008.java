package hu.bme.aait.picstore.um;

import hu.bme.aait.picstore.entities.Group;
import hu.bme.aait.picstore.entities.ImageSet;
import hu.bme.aait.picstore.entities.User;
import hu.bme.aait.picstore.exceptions.AlreadyRegisteredException;
import hu.bme.aait.picstore.exceptions.NoSuchGroupException;
import hu.bme.aait.picstore.exceptions.WrongPasswordException;
import hu.bme.aait.picstore.interfaces.GroupFacadeLocal;
import hu.bme.aait.picstore.interfaces.ImageSetFacadeLocal;
import hu.bme.aait.picstore.interfaces.UserFacadeLocal;
import hu.bme.aait.picstore.interfaces.UserManagerLocal;
import hu.bme.aait.picstore.util.TextUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import javax.annotation.security.DeclareRoles;
import javax.annotation.security.RolesAllowed;
import javax.ejb.EJB;
import javax.ejb.Stateless;

@Stateless
@DeclareRoles({ "ADMIN", "USER" })
public class UserManager implements UserManagerLocal {

    Logger logger = Logger.getLogger(getClass().getName());

    @EJB
    UserFacadeLocal userFacade;

    @EJB
    GroupFacadeLocal groupFacade;

    @EJB
    ImageSetFacadeLocal imageSetFacade;

    @Override
    public User register(String emailAddress, String password, String fullName) throws NoSuchGroupException, AlreadyRegisteredException {
        logger.info("Registering user: " + emailAddress);
        User user = userFacade.findByEmail(emailAddress);
        if (user != null) {
            throw new AlreadyRegisteredException(user);
        }
        Group group = getGroup(USERGROUP);
        if (group == null) {
            group = groupFacade.create(USERGROUP);
            groupFacade.create(ADMINGROUP);
        }
        String digestedPassword = TextUtil.digest(password);
        if (digestedPassword == null) {
            throw new RuntimeException();
        }
        String digestedActivationKey = TextUtil.digest(emailAddress + ":" + fullName + ":" + TextUtil.getRandomString(10));
        if (digestedActivationKey == null) {
            throw new RuntimeException();
        }
        user = userFacade.createUser(emailAddress, digestedPassword, fullName, group, digestedActivationKey);
        Set<ImageSet> sets = user.getImageSets();
        if (sets == null) {
            sets = new HashSet<ImageSet>();
        }
        ImageSet set = imageSetFacade.createImageSet(user, ImageSetFacadeLocal.DEFAULT, ImageSetFacadeLocal.DEFAULT);
        sets.add(set);
        user.setImageSets(sets);
        userFacade.modify(user);
        logger.info("User registered: " + emailAddress);
        return user;
    }

    @Override
    public void modify(User user, String currentPassword, String newPassword) throws WrongPasswordException {
        String digestedCurrent = TextUtil.digest(currentPassword);
        if (!digestedCurrent.equals(user.getPassword())) {
            throw new WrongPasswordException();
        }
        if (newPassword != null && !newPassword.isEmpty()) {
            String digestedPwd = TextUtil.digest(newPassword);
            userFacade.modify(user, digestedPwd);
        } else {
            userFacade.modify(user);
        }
    }

    @Override
    @RolesAllowed({ "ADMIN" })
    public void promoteAdmin(String emailAddress) {
        logger.info("Promoting user to ADMIN: " + emailAddress);
        userFacade.setGroup(getUser(emailAddress), getGroup(ADMINGROUP));
        logger.info("User promoted to ADMIN: " + emailAddress);
    }

    @Override
    public Group getGroup(String name) {
        return groupFacade.find(name);
    }

    public User getUser(String email) {
        return userFacade.findByEmail(email);
    }

    @Override
    public void modify(User user) {
        userFacade.modify(user);
    }

    @Override
    public List<User> getAllUser() {
        return getUsersInGroup(USERGROUP);
    }

    @Override
    public List<User> getAllAdmin() {
        return getUsersInGroup(ADMINGROUP);
    }

    private List<User> getUsersInGroup(String groupname) {
        List<User> users = userFacade.getAllUser();
        List<User> ret = new ArrayList<User>();
        Group usergroup = groupFacade.find(groupname);
        for (User user : users) {
            if (user.getGroup().equals(usergroup)) {
                ret.add(user);
            }
        }
        return ret;
    }
}
