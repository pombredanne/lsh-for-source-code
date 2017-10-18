package fr.uhp.wiki;

import java.security.MessageDigest;
import java.util.List;
import javax.persistence.*;
import org.hibernate.Query;
import org.hibernate.Session;
import fr.uhp.wiki.tools.HibernateUtil;

@Entity
public class User implements java.io.Serializable {

    private static final long serialVersionUID = 7116833596299756026L;

    private int id;

    private String name;

    private String password;

    public User() {
        super();
    }

    @Id
    @Column(name = "id", unique = true, nullable = false)
    @GeneratedValue(strategy = GenerationType.AUTO)
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Column(name = "name", nullable = false, length = 10)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "password", nullable = false, length = 64)
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object p) {
        if (!(p instanceof User)) return false;
        if (this.getName().equals(((User) p).getName()) && this.getPassword().equals(((User) p).getPassword())) return true;
        return false;
    }

    @Override
    public int hashCode() {
        return name.hashCode() ^ password.hashCode();
    }

    private static boolean persist(User user) throws Exception {
        HibernateUtil hibernateUtil = new HibernateUtil();
        Session s = hibernateUtil.getSession();
        s.beginTransaction();
        boolean persist = false;
        if (!alreadyExists(user)) {
            s.save(user);
            persist = true;
            System.out.println("Operation reussie !");
        } else System.out.println("Impossible d'enregistrer " + user.getName() + " car il existe deja dans la table");
        s.flush();
        s.getTransaction().commit();
        s.close();
        return persist;
    }

    @SuppressWarnings("unchecked")
    private static boolean alreadyExists(User user) throws Exception {
        HibernateUtil hibernateUtil = new HibernateUtil();
        Session s = hibernateUtil.getSession();
        s.beginTransaction();
        boolean exists = false;
        Query q = s.createQuery("from User");
        List<User> l = q.list();
        for (User u : l) {
            if (user.equals(u)) {
                exists = true;
                return exists;
            }
        }
        s.close();
        return exists;
    }

    public String userValidation() {
        System.out.println("-- userValidation --");
        try {
            if (alreadyExists(this)) {
                return "user.signed.in";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(this.name + " doesn't exist");
        return "user.not.signed";
    }

    private static String sha1(String password) {
        String res = "";
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA1");
            byte[] digest = sha1.digest((password).getBytes());
            res = bytes2String(digest);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return res;
    }

    private static String bytes2String(byte[] bytes) {
        StringBuilder string = new StringBuilder();
        for (byte b : bytes) {
            String hexString = Integer.toHexString(0x00FF & b);
            string.append(hexString.length() == 1 ? "0" + hexString : hexString);
        }
        return string.toString();
    }
}
