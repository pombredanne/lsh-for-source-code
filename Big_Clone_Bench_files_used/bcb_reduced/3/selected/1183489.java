package com.hdmm.mediaserver.service;

import java.io.Serializable;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import com.hdmm.mediaserver.development.DevLogger;
import com.hdmm.mediaserver.entities.UserData;

/**
 * UserDataService.java
 * Stellt die Verbindung zur Datenbank her. 
 * Bereitstellung der Dienste fuer die UserDataView.java
 * @author Daniel Schnelle (20966593), Michael Tiede (20966946)
 * @version 7
 * @date 21.11.2011
 * @created 11.11.2011
 */
@Stateless
public class UserDataService implements Serializable {

    private static final long serialVersionUID = 1522203180347723475L;

    private static DevLogger logger = new DevLogger(UserDataService.class);

    @PersistenceContext
    EntityManager em;

    /**
	 * Ueberprueft Userdaten aus Datenbank mit Eingabe
	 * @author Daniel Schnelle (20966593)
	 * @param user
	 * @param password
	 * @return
	 */
    public boolean login(String user, String password) {
        password = getMD5Hash(password);
        try {
            UserData tmp = (UserData) em.createQuery("SELECT u FROM UserData u " + "WHERE u.username=:username").setParameter("username", user).getSingleResult();
            if (tmp.getUsername().equals(user) && tmp.getPassword().equals(password)) {
                return true;
            } else {
                logger.logInfoMessage("login failed as " + user + ", wrong pw: " + password);
                return false;
            }
        } catch (Exception e) {
            logger.logInfoMessage("login failed as " + user + ", user doesn't exist");
            return false;
        }
    }

    /**
	 * Zurzeit ungenutzt
	 * @author Daniel Schnelle (20966593)
	 * @return
	 */
    public String lastLoginDate() {
        Date date = Calendar.getInstance().getTime();
        DateFormat formatter = new SimpleDateFormat("HH:mm");
        DateFormat formatter2 = new SimpleDateFormat("dd.MM.yyyy");
        String today = "Am " + formatter2.format(date) + " um " + formatter.format(date) + " Uhr";
        return today;
    }

    /**
	 * Sucht Nutzer
	 * @author Daniel Schnelle (20966593)
	 * @param search - Suchbegriff
	 * @return
	 */
    @SuppressWarnings("unchecked")
    public List<UserData> searchUser(String search, String searchAll) {
        Query query = em.createQuery("SELECT u FROM UserData u WHERE u.username LIKE '%" + search + "%' " + "OR u.eMail LIKE '%" + search + "%' ");
        return query.getResultList();
    }

    /**
	 * Erstellt User, mit verschl�sseltem Passwort
	 * @author Daniel Schnelle (20966593)
	 * @param user
	 */
    public void createUser(UserData user) {
        user.setPassword(getMD5Hash(user.getPassword()));
        em.persist(user);
    }

    /**
	 * Holt sich durch Username ein Objekt aus der Datenbank
	 * @author Daniel Schnelle (20966593)
	 * @param username
	 * @return
	 */
    public UserData findUserByName(String username) {
        return (UserData) em.createQuery("SELECT u FROM UserData u " + "WHERE u.username=:username").setParameter("username", username).getSingleResult();
    }

    /**
	 * Wandelt einen String in einen MD5 Hash um. Verwendet die
	 * java.security.MessageDigest Klasse.
	 * @author Michael Tiede (20966946)
	 * @param password - das zu verschluesselnde Passwort
	 * @return
	 */
    public String getMD5Hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytesToEncrypt = md.digest(password.getBytes());
            return new BigInteger(1, bytesToEncrypt).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveUser(UserData user) {
        em.persist(user);
    }

    public void updateUser(UserData user) {
        em.merge(user);
    }

    public void deleteUser(UserData user) {
        em.remove(em.find(UserData.class, user.getUsername()));
    }

    @SuppressWarnings("unchecked")
    public List<UserData> getUser() {
        return em.createQuery("SELECT u FROM UserData u").getResultList();
    }

    @PostConstruct
    public void createDefaultUser() {
        UserData test = new UserData();
        test.setUsername("test");
        test.setPassword(getMD5Hash("test"));
        this.saveUser(test);
        test = new UserData();
        test.setUsername("admin");
        test.setPassword(getMD5Hash("a"));
        this.saveUser(test);
    }
}
