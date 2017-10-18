package org.wportal.jspwiki.dbprovider;

import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.WikiUserManager;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;
import com.ecyrd.jspwiki.WikiUser;
import sun.misc.BASE64Encoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: SimonLei
 * Date: 2004-10-3
 * Time: 9:30:01
 * $Id: WikiUserManagerImpl.java,v 1.1 2004/10/19 07:31:58 echou Exp $
 */
public class WikiUserManagerImpl extends HibernateDaoSupport implements WikiUserManager {

    public void setPasswd(String userName, String passwd) throws ProviderException {
        WikiUser user = getUser(userName);
        try {
            user.setPasswd(digestPasswd(passwd));
        } catch (NoSuchAlgorithmException e) {
            throw new ProviderException("Can't update passwd", e);
        }
        getHibernateTemplate().saveOrUpdate(user);
    }

    public List<String> getUserGroups(String name) {
        String theGroup = getUser(name).getTheGroup();
        if (theGroup == null) return new ArrayList<String>();
        return Arrays.asList(theGroup.split(":"));
    }

    public List getAllUsers() {
        return getHibernateTemplate().find("from WikiUser user");
    }

    private String digestPasswd(String orig) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        return new BASE64Encoder().encode(digest.digest(orig.getBytes()));
    }

    public boolean userExist(String userName, String passwd) {
        WikiUser user = getUser(userName);
        if (user.getPasswd() == null) return false;
        try {
            if (user.getPasswd().equals(digestPasswd(passwd))) return true;
        } catch (NoSuchAlgorithmException e) {
        }
        return false;
    }

    public WikiUser getUser(String userName) {
        List users = getHibernateTemplate().find("from WikiUser user where name=?", userName);
        if (users.size() == 0) {
            WikiUser wikiUser = new WikiUser();
            wikiUser.setName(userName);
            return wikiUser;
        }
        return (WikiUser) users.get(0);
    }
}
