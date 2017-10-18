package com.casheen.springsecurity.cas.persistence;

import java.security.MessageDigest;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.stereotype.Repository;
import com.casheen.springsecurity.cas.model.User;

@Repository
public class UserDaoImpl extends HibernateDaoSupport implements UserDao {

    private static final String[] hexDigits = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    private SessionFactory sessionFacotry;

    @Resource
    public void setSessionFacotry(SessionFactory sessionFacotry) {
        System.out.println("Get the resource sessionFactory.");
        this.sessionFacotry = sessionFacotry;
    }

    @PostConstruct
    public void injectSessionFactory() {
        System.out.println("Inject the sessionFactory.");
        super.setSessionFactory(sessionFacotry);
    }

    private static String MD5Encode(String origin) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
        } catch (Exception ex) {
        }
        return resultString;
    }

    /** convert byte array to hex string */
    private static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) n = 256 + n;
        int d1 = n / 16;
        int d2 = n % 16;
        return hexDigits[d1] + hexDigits[d2];
    }

    public void saveOrUpdate(User user) {
        user.setPassword(MD5Encode(user.getPassword()));
        super.getHibernateTemplate().saveOrUpdate(user);
    }

    public User get(Integer id) {
        Criteria criteria = super.getSession().createCriteria(User.class);
        criteria.add(Restrictions.eq("id", id));
        criteria.setFetchMode("roles", FetchMode.JOIN);
        return (User) criteria.uniqueResult();
    }

    @SuppressWarnings("unchecked")
    public List<User> list() {
        Criteria criteria = super.getSession().createCriteria(User.class);
        criteria.addOrder(Order.asc("id"));
        return criteria.list();
    }

    /** 根据帐号查找用户 */
    public User getByAccount(String account) {
        Criteria criteria = super.getSession().createCriteria(User.class);
        criteria.add(Restrictions.eq("account", account));
        criteria.setFetchMode("roles", FetchMode.JOIN);
        return (User) criteria.uniqueResult();
    }
}
