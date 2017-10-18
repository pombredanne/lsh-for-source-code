package org.insia.teamexperts.dao.implementation.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;
import org.insia.teamexperts.dao.implementation.common.ImplCommonDAO;
import org.insia.teamexperts.dao.interfaces.user.IUserDAO;
import org.insia.teamexperts.model.user.User;

/**
 * Classe DAO pour la gestion des utilisateurs
 * 
 * @author sok hout
 *
 */
public class ImplUserDAO extends ImplCommonDAO<User, Long> implements IUserDAO {

    @SuppressWarnings("unused")
    private static Logger _logger = Logger.getLogger(ImplUserDAO.class);

    public ImplUserDAO() {
        type = User.class;
    }

    public ImplUserDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
        type = User.class;
    }

    /**
	 * Recherche un utilisaeur correspondant au login et au mot de passe
	 * 
	 * @param login login
	 * @param password mot de passe non crypté
	 * @return retourne l'utilisateur trouvé, sinon null;
	 */
    public User getUser(String login, String password) {
        User rst = null;
        try {
            beginTransaction();
            String cryptedPwd = getEncodedPassword(password);
            Criteria cri = _session.createCriteria(User.class);
            cri.add(Restrictions.eq("login", login));
            cri.add(Restrictions.eq("password", cryptedPwd));
            List userList = cri.list();
            if (userList.size() == 1) rst = (User) userList.get(0);
            commit();
        } catch (Throwable t) {
            rollback();
            log.error(t);
        }
        return rst;
    }

    /**
	 * Crypte le mot de passe
	 * 
	 * @param key le mot de passe à crypter
	 * @return retourne le mot de passe crypté
	 */
    public static String getEncodedPassword(String key) {
        byte[] uniqueKey = key.getBytes();
        byte[] hash = null;
        try {
            hash = MessageDigest.getInstance("MD5").digest(uniqueKey);
        } catch (NoSuchAlgorithmException e) {
            throw new Error("no MD5 support in this VM");
        }
        StringBuffer hashString = new StringBuffer();
        for (int i = 0; i < hash.length; ++i) {
            String hex = Integer.toHexString(hash[i]);
            if (hex.length() == 1) {
                hashString.append('0');
                hashString.append(hex.charAt(hex.length() - 1));
            } else {
                hashString.append(hex.substring(hex.length() - 2));
            }
        }
        return hashString.toString();
    }
}
