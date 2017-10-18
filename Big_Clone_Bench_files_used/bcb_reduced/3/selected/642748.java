package org.opengeotracker.hunting.db;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ListIterator;
import java.util.Random;
import java.util.logging.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

public class PeopleDAO extends HibernateDaoSupport {

    public static int SALT_LENGTH = 10;

    public static String saltUniverse = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrtsuvwxyz0123456789";

    public static Random rnd = new Random();

    public static String generateSalt() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < SALT_LENGTH; i++) {
            sb.append(saltUniverse.charAt(rnd.nextInt(saltUniverse.length())));
        }
        return sb.toString();
    }

    public static String generateMD5(String clear) {
        byte hash[] = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(clear.getBytes());
            hash = md5.digest();
        } catch (NoSuchAlgorithmException e) {
        }
        if (hash != null) {
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                String tmp = Integer.toHexString(0xFF & hash[i]);
                if (tmp.length() == 1) {
                    tmp = "0" + tmp;
                }
                hexString.append(tmp);
            }
            return hexString.toString();
        } else {
            return null;
        }
    }

    public void save(People people) {
        getHibernateTemplate().saveOrUpdate(people);
    }

    public People findById(long id) {
        return (People) getHibernateTemplate().get(People.class, id);
    }

    @SuppressWarnings("unchecked")
    public People findByLogin(String login) {
        Session session = this.getSession();
        Query query = session.getNamedQuery("people.findByLogin");
        query.setString("login", login);
        ListIterator iter = query.list().listIterator();
        if (iter.hasNext()) {
            return (People) iter.next();
        }
        return null;
    }

    public void delete(long id) {
        People people = (People) getHibernateTemplate().get(People.class, id);
        getHibernateTemplate().delete(people);
        getHibernateTemplate().flush();
    }

    public boolean verifyPassword(People people, String password) {
        String md5hex = generateMD5(people.getSalt() + password);
        return md5hex.equals(people.getMd5hex());
    }

    public void setPassword(People people, String password) {
        String salt = generateSalt();
        String md5hex = generateMD5(salt + password);
        people.setSalt(salt);
        people.setMd5hex(md5hex);
    }
}
