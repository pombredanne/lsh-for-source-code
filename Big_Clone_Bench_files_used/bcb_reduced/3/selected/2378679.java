package ru.spbspu.staub.service;

import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import ru.spbspu.staub.entity.RoleEnum;
import ru.spbspu.staub.entity.User;
import ru.spbspu.staub.exception.RemoveException;
import ru.spbspu.staub.model.list.FormProperties;
import ru.spbspu.staub.model.list.FormTable;
import javax.ejb.Stateless;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Stateless EJB Service for manipulations with <code>User</code> entity.
 *
 * @author Konstantin Grigoriev
 */
@Name("userService")
@AutoCreate
@Stateless
public class UserServiceBean extends GenericServiceBean<User, Integer> implements UserService {

    public FormTable find(FormProperties formProperties, RoleEnum role) {
        logger.debug("> find(FormProperties=#0, RoleEnum=#1)", formProperties, role);
        StringBuilder query = new StringBuilder();
        query.append("select u from User u");
        Map<String, Object> parameters = new HashMap<String, Object>();
        if (role != null) {
            query.append(" where u.role = :role");
            parameters.put("role", role);
        }
        String queryString = query.toString();
        logger.debug("*  Query: #0", queryString);
        FormTable formTable = findAll(queryString, formProperties, parameters);
        logger.debug("< find(FormProperties, RoleEnum)");
        return formTable;
    }

    public User find(String username, String password) {
        User user = null;
        try {
            Query q = getEntityManager().createQuery("select u from User u where u.username = :username and u.password = :password");
            q.setParameter("username", username);
            q.setParameter("password", calculateHash(password));
            user = (User) q.getSingleResult();
        } catch (NoResultException ex) {
        }
        return user;
    }

    public User save(User user, String password) {
        logger.debug("> save(User=#0, String=#1)", user, ((password != null) ? "*****" : null));
        if (password != null) {
            user.setPassword(calculateHash(password));
        }
        User result = makePersistent(user);
        logger.debug("< save(User, String)");
        return result;
    }

    public boolean isUsernameUnique(String username) {
        logger.debug("> isUsernameUnique(String=#0)", username);
        Query q = getEntityManager().createQuery("select count(u) from User u where u.username = :username");
        q.setParameter("username", username);
        boolean unique = ((Long) q.getSingleResult() == 0);
        if (unique) {
            logger.debug("*  Username is unique.");
        } else {
            logger.debug("*  Username is not unique.");
        }
        logger.debug("< isUsernameUnique(String)");
        return unique;
    }

    @Override
    public void remove(User user) throws RemoveException {
        logger.debug("> remove(User=#0)", user);
        User u = getEntityManager().merge(user);
        getEntityManager().remove(u);
        logger.debug("< remove(User=#0)", user);
    }

    private String calculateHash(String s) {
        if (s == null) {
            return null;
        }
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            logger.error("Could not find a message digest algorithm.");
            return null;
        }
        messageDigest.update(s.getBytes());
        byte[] hash = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
