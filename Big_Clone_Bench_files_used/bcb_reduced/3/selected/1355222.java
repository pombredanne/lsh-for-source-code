package owchat.source;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

/**
 *
 * @author Hirantha Neranjan
 */
public class UserManager {

    private static ArrayList<User> users = new ArrayList<User>();

    public static String LoginUser(User user) {
        for (Iterator<User> it = users.iterator(); it.hasNext(); ) {
            User tempUser = it.next();
            if (tempUser.equals(user)) return tempUser.getKeyString();
        }
        users.add(user);
        return user.generateKeyString();
    }

    public static void LogoutUser(User user) {
        for (Iterator<User> it = users.iterator(); it.hasNext(); ) {
            User tempUser = it.next();
            if (tempUser.equals(user)) {
                users.remove(user);
                return;
            }
        }
    }

    public static User GetUserByKeyString(String keyString) {
        for (User user : users) {
            if (user.getKeyString().equals(keyString)) {
                return user;
            }
        }
        return null;
    }

    public static User GetUserByName(String userName) {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<User> userList = session.createQuery("select u from User as u where u.userName='" + userName + "'").list();
            tx.commit();
            if (userList.size() > 0) {
                User tempUser = userList.get(0);
                tempUser.setOnline(isLoggedIn(tempUser.getUserName()));
                return tempUser;
            } else return null;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return null;
        }
    }

    public static void ClearUsers() {
        users = new ArrayList<User>();
    }

    public static Boolean UserExcist(User newUser) {
        return GetUserByName(newUser.getUserName()) != null;
    }

    public static Boolean AddUser(String username, String password) {
        User newUser = new User(username, GetMd5Digest(password));
        if (UserExcist(newUser)) {
            return false;
        }
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(newUser);
            tx.commit();
            return true;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return false;
        }
    }

    public static Boolean RemoveUser(User newUser) {
        if (!UserExcist(newUser)) {
            return false;
        }
        LogoutUser(newUser);
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(newUser);
            tx.commit();
            return true;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return false;
        }
    }

    public static int UserCount() {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            int count = session.createQuery("select u from User as u").list().size();
            tx.commit();
            return count;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return 0;
        }
    }

    public static boolean isLoggedIn(String username) {
        for (Iterator<User> it = users.iterator(); it.hasNext(); ) {
            User user = it.next();
            if (user.getUserName().equals(username)) return true;
        }
        return false;
    }

    public static User[] GetUsers() {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<User> userList = session.createQuery("select u from User as u").list();
            tx.commit();
            User[] userArr = toUserArray(userList);
            for (int i = 0; i < userArr.length; i++) {
                userArr[i].setOnline(isLoggedIn(userArr[i].getUserName()));
            }
            return userArr;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return null;
        }
    }

    public static User[] Search(String searchContent) {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            List<User> userList = session.createQuery("select u from User as u where u.userName like '%" + searchContent + "%'").list();
            tx.commit();
            User[] userArr = toUserArray(userList);
            for (int i = 0; i < userArr.length; i++) {
                userArr[i].setOnline(isLoggedIn(userArr[i].getUserName()));
            }
            return userArr;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return null;
        }
    }

    private static User[] toUserArray(List<User> usersList) {
        Object[] userobjs = usersList.toArray();
        User[] userArr = new User[userobjs.length];
        for (int i = 0; i < userobjs.length; i++) {
            Object object = userobjs[i];
            userArr[i] = (User) object;
        }
        return userArr;
    }

    public static String GetMd5Digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            return number.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean AddFriend(User user, User friend) {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            user.addFriend(friend);
            friend.addFriend(user);
            session.update(user);
            session.update(friend);
            tx.commit();
            return true;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return false;
        }
    }

    public static boolean RemoveFriend(User user, User friend) {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            user.removeFriend(friend);
            friend.removeFriend(user);
            session.update(user);
            session.update(friend);
            tx.commit();
            return true;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return false;
        }
    }

    static User[] getFriends(User user) {
        SessionFactory sessionFactory = OwChatHibernateUtil.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Set<User> userList = user.getFriends();
            tx.commit();
            User[] userArr = new User[userList.size()];
            Iterator<User> userIterator = userList.iterator();
            for (int i = 0; i < userArr.length; i++) {
                userArr[i] = userIterator.next();
                userArr[i].setOnline(isLoggedIn(userArr[i].getUserName()));
            }
            return userArr;
        } catch (RuntimeException exception) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (HibernateException e1) {
                    throw exception;
                }
            }
            return null;
        }
    }

    static void SendMsg(String username, Message msg) {
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getUserName().equals(username)) {
                users.get(i).insertMessage(msg);
                return;
            }
        }
    }
}
