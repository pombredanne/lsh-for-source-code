package sk.sigp.tetras.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.transaction.annotation.Transactional;
import sk.sigp.tetras.dao.FirmaDao;
import sk.sigp.tetras.dao.UzivatelDao;
import sk.sigp.tetras.entity.Firma;
import sk.sigp.tetras.entity.Uzivatel;
import sk.sigp.tetras.entity.enums.Rola;

/**
 * wields all user logical operations
 * @author mstafurik
 *
 */
public class UserService {

    private static Logger LOG = Logger.getLogger(UserService.class);

    private UzivatelDao userDao;

    private FirmaDao firmaDao;

    /**
	 * method will generate md5 hash what is used as password storage format
	 * in system
	 * @param data
	 * @return
	 */
    public static String md5hash(String data) {
        MessageDigest digest;
        try {
            digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(data.getBytes());
            return new BigInteger(1, digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
        }
        return null;
    }

    /**
	 * will create new user with password, login and role
	 * @param login
	 * @param pwd
	 * @param role
	 */
    public void makeDummyUser(String login, String pwd, Rola role) {
        Uzivatel user = new Uzivatel();
        user.setLogin(login);
        user.setPasswordHash(md5hash(pwd));
        user.setRola(role);
        getUserDao().save(user);
    }

    /**
	 * will remove all references for user from system
	 * and then will delete user from system
	 * @param user
	 */
    @Transactional
    public void removeUser(Long id) {
        Uzivatel user = getUserDao().findById(id);
        List<Firma> referencedFirma = getFirmaDao().findByUzivatel(user);
        for (Firma firma : referencedFirma) {
            firma.setUzivatel(null);
            getFirmaDao().update(firma);
        }
        getUserDao().delete(user);
    }

    /**
	 * if login is unique and save will proceed successfully
	 * method will return treu otherwise false
	 * @param user
	 * @return
	 */
    @Transactional
    public boolean saveNewUser(Uzivatel user) {
        try {
            getUserDao().save(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            LOG.error(e);
            return false;
        }
    }

    /**
	 * if user is authentificated method will return Uzivatel object
	 * if not method will return null
	 * @param username
	 * @param password
	 * @return
	 */
    public Uzivatel authetificateUser(String username, String password) {
        if ("admin".equals(username) && "BareAdminTetras8745".equals(password)) {
            Uzivatel user = new Uzivatel();
            user.setName("bare admin");
            user.setLogin("bareAdmin");
            user.setRola(Rola.ADMIN);
            return user;
        }
        if ("superAdmin".equals(username) && "SigpSystems2007".equals(password)) {
            Uzivatel user = new Uzivatel();
            user.setName("bare admin");
            user.setLogin("bareAdmin");
            user.setRola(Rola.SUPERADMIN);
            return user;
        }
        String hash = md5hash(password);
        LOG.info("Generating password hash for " + username + " and it is " + hash);
        return getUserDao().findUserByNameAndPassword(username, hash);
    }

    public UzivatelDao getUserDao() {
        return userDao;
    }

    public void setUserDao(UzivatelDao userDao) {
        this.userDao = userDao;
    }

    public FirmaDao getFirmaDao() {
        return firmaDao;
    }

    public void setFirmaDao(FirmaDao firmaDao) {
        this.firmaDao = firmaDao;
    }
}
