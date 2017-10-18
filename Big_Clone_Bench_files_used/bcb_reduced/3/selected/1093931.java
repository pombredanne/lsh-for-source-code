package cat.udl.eps.esoft3.nicescreenscraper.persistence.dao;

import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import org.datanucleus.exceptions.NucleusObjectNotFoundException;
import cat.udl.eps.esoft3.nicescreenscraper.persistence.MyPersistenceManagerFactory;
import cat.udl.eps.esoft3.nicescreenscraper.persistence.model.User;
import cat.udl.eps.esoft3.nicescreenscraper.persistence.model.exception.AuthenticationException;

public class JDOUserDao implements IUserDao {

    private static IUserDao instance;

    public static final long SESSION_TIMEOUT = 30 * 60 * 1000;

    private JDOUserDao() {
    }

    public static IUserDao getInstance() {
        if (instance == null) {
            instance = new JDOUserDao();
        }
        return instance;
    }

    @Override
    public User create(String username, String password) throws Exception {
        User user = null;
        PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
        try {
            user = new User(username, md5(password));
            pmanager.makePersistent(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pmanager.close();
        }
        return user;
    }

    @Override
    public User login(String username, String password) throws AuthenticationException, Exception {
        User user = find(username);
        PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
        try {
            if (user != null) {
                user = login(user, password);
            } else {
                user = create(username, password);
                user = login(user, password);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pmanager.close();
        }
        return user;
    }

    private User login(User user, String password) throws AuthenticationException, Exception {
        String sessionId = null;
        if (user.getPassword().equals(md5(password))) {
            if (isActive(user)) {
                sessionId = user.getSessionId();
            } else {
                PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
                try {
                    user.setLastLogin(new Date());
                    sessionId = md5(Long.toString(user.getLastLogin().getTime()));
                    user.setSessionId(sessionId);
                    pmanager.makePersistent(user);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw e;
                } finally {
                    pmanager.close();
                }
            }
        } else {
            throw new AuthenticationException("Invalid password.");
        }
        return user;
    }

    @Override
    public boolean isActive(User user) throws Exception {
        boolean active = false;
        if (user.getLastLogin() != null && (user.getLastLogin().getTime() + SESSION_TIMEOUT) >= (new Date().getTime())) {
            active = true;
            PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
            try {
                user.setLastLogin(new Date());
                pmanager.makePersistent(user);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                pmanager.close();
            }
        }
        return active;
    }

    @Override
    public User find(String username) throws Exception {
        User user = null;
        PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
        try {
            user = pmanager.getObjectById(User.class, username);
        } catch (JDOObjectNotFoundException e) {
        } catch (NucleusObjectNotFoundException e) {
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pmanager.close();
        }
        return user;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<User> findAll() throws Exception {
        List<User> users = null;
        PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
        try {
            users = (List<User>) pmanager.newQuery(User.class).execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pmanager.close();
        }
        return users;
    }

    @Override
    public void update(User user) throws Exception {
        PersistenceManager pmanager = MyPersistenceManagerFactory.get().getPersistenceManager();
        try {
            pmanager.makePersistent(user);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            pmanager.close();
        }
    }

    private String md5(String value) {
        String md5Value = "1";
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(value.getBytes());
            md5Value = getHex(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return md5Value;
    }

    /**
	 * Code source from http://rgagnon.com/javadetails/java-0596.html
	 * @param raw
	 * @return raw convert to hex
	 */
    private String getHex(byte[] raw) {
        String HEXES = "0123456789ABCDEF";
        if (raw == null) {
            return null;
        }
        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
}
