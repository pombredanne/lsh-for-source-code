package de.boardgamesonline.bgo2.webserver.model;

import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import javax.mail.Message;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.masukomi.aspirin.core.MailQue;

/**
 * This class is responsible for providing access to the list of running games,
 * to the list of online users and to the database. It uses the Singleton
 * pattern so that only one instance can be created.
 * 
 * @author Marc Lindenberg/Peter Lohmann
 */
public final class DataProvider {

    /**
	 * The only instance of <code>DataProvider</code>.
	 */
    private static final DataProvider INSTANCE = new DataProvider();

    /**
	 * The hibernate session factory needed to create database calls.
	 */
    private SessionFactory hibernateSessionFactory;

    /**
	 * The global games list.
	 */
    private final GameList games;

    /**
	 * The global list of online players.
	 */
    private final PlayerList onlinePlayers;

    /**
	 * The total number of users stored in the database
	 */
    private int numberOfUsers;

    /**
	 * Private constructor to ensure Singleton.
	 */
    private DataProvider() {
        this.games = new GameList();
        this.onlinePlayers = new PlayerList();
        this.hibernateSessionFactory = null;
        this.numberOfUsers = 0;
    }

    /**
	 * Returns the Singleton <code>DataProvider</code> object.
	 * 
	 * @return The one and only instance of the <code>DataProvider</code>
	 *         class.
	 */
    public static DataProvider getInstance() {
        return DataProvider.INSTANCE;
    }

    /**
	 * @param input
	 *            String to convert to byte array.
	 * @return The byte array.
	 */
    public static byte[] stringToBytes(String input) {
        char[] chars = input.toCharArray();
        byte[] bytes = new byte[2 * chars.length];
        for (int i = 0; i < chars.length; i++) {
            bytes[2 * i] = (byte) (chars[i] >> 8);
            bytes[2 * i + 1] = (byte) chars[i];
        }
        return bytes;
    }

    /**
	 * @param input
	 *            Byte array to convert to string.
	 * @return The string.
	 */
    public static String bytesToString(byte[] input) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < input.length; i++) {
            result.append(input[i]);
        }
        return new String(result);
    }

    /**
	 * Used to one-way encrypt user passwords before storing them in user
	 * objects.
	 * 
	 * @param plainPassword
	 *            The password to hash with SHA-1.
	 * @return The SHA-1 hash of <code>plainPassword</code>.
	 */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            return null;
        } else {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                digest.update(stringToBytes(plainPassword));
                byte[] result = digest.digest();
                return bytesToString(result);
            } catch (NoSuchAlgorithmException e) {
                return String.valueOf(plainPassword.hashCode());
            }
        }
    }

    /**
	 * Loads database properties from a XML file given as a String.
	 * <code>filename</code> is resolved from the classpath point of this
	 * class outwards.
	 * 
	 * @param filename
	 *            The name of the XML file were to load the properties from.
	 * @return The properties object containing the properties defined in the
	 *         given file. If there are no properties or the file is not found
	 *         an empty {@link java.util.Properties} object is returned.
	 */
    public static Properties loadProperties(String filename) {
        Properties props = new Properties();
        InputStream stream = DataProvider.class.getResourceAsStream(filename);
        if (stream != null) {
            try {
                props.loadFromXML(stream);
            } catch (Exception e) {
                ;
            }
        }
        if (props.size() == 0) {
            stream = DataProvider.class.getResourceAsStream("/" + filename);
            if (stream != null) {
                try {
                    props.loadFromXML(stream);
                } catch (Exception e) {
                    ;
                }
            }
        }
        if (props.size() == 0) {
            try {
                stream = (new File((new File(DataProvider.class.getResource("/").toURI())).getParent(), filename)).toURL().openStream();
                props.loadFromXML(stream);
            } catch (Exception e) {
                ;
            }
        }
        if (props.size() == 0) {
            try {
                stream = (new File((new File((new File((new File(DataProvider.class.getResource("/").toURI())).getParent())).getParent())).getParent(), "bgowebserver." + filename)).toURL().openStream();
                props.loadFromXML(stream);
            } catch (Exception e) {
                ;
            }
        }
        return props;
    }

    /**
	 * Sends an email over a specified smtp server or an internal smtp server.
	 * 
	 * @param to
	 *            The email address to which the email shall be sent.
	 * @param from
	 *            The email address of the sender.
	 * @param subject
	 *            The message's subject.
	 * @param body
	 *            The message body.
	 * @param smtpServer
	 *            The address of the smtp server over which to send the email.
	 *            If this is <em>null</em> then a local mail server is created
	 *            and used.
	 * @param username
	 *            The username for authentication on the smtp server.
	 * @param password
	 *            The password for authentication on the smtp server.
	 */
    public static void sendEmail(String to, String from, String subject, String body, String smtpServer, String username, String password) {
        boolean useLocalServer = ((smtpServer == null) || (smtpServer.equals("")));
        boolean authenticate = ((username != null) && (password != null));
        if (useLocalServer) {
            smtpServer = "localhost";
        }
        Properties props = new Properties();
        props.setProperty("mail.smtp.host", smtpServer);
        if (authenticate) {
            props.setProperty("mail.smtp.auth", "true");
        }
        try {
            javax.mail.Session session = javax.mail.Session.getDefaultInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
            msg.setSubject(subject);
            msg.setText(body);
            msg.setSentDate(new Date());
            if (useLocalServer) {
                MailQue.queMail(msg);
            } else {
                if (authenticate) {
                    Transport t = session.getTransport("smtp");
                    try {
                        t.connect(username, password);
                        t.sendMessage(msg, msg.getAllRecipients());
                    } finally {
                        t.close();
                    }
                } else {
                    Transport.send(msg);
                }
            }
        } catch (Exception ex) {
            System.err.println("Could not send email to " + to);
        }
    }

    /**
	 * Initializes the database. May only be called once.
	 * 
	 * @param databasePath
	 *            The absolute filename of the database files.
	 * @param databaseProperties
	 *            The hibernate database properties.
	 */
    public void init(String databasePath, Properties databaseProperties) {
        if (this.hibernateSessionFactory == null) {
            Properties defaultProperties = new Properties();
            defaultProperties.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
            defaultProperties.setProperty("hibernate.connection.url", databasePath);
            defaultProperties.setProperty("hibernate.connection.username", "sa");
            defaultProperties.setProperty("hibernate.connection.password", "");
            defaultProperties.setProperty("hibernate.connection.pool_size", "1");
            defaultProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
            defaultProperties.setProperty("hibernate.current_session_context_class", "thread");
            defaultProperties.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.NoCacheProvider");
            defaultProperties.setProperty("hibernate.hbm2ddl.auto", "update");
            if (databaseProperties != null && databaseProperties.size() > 0) {
                Properties databasePropertiesWithDefaults = new Properties();
                Enumeration<?> e = defaultProperties.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    databasePropertiesWithDefaults.setProperty(key, defaultProperties.getProperty(key));
                }
                e = databaseProperties.propertyNames();
                while (e.hasMoreElements()) {
                    String key = (String) e.nextElement();
                    databasePropertiesWithDefaults.setProperty(key, databaseProperties.getProperty(key));
                }
                this.hibernateSessionFactory = new AnnotationConfiguration().addAnnotatedClass(User.class).addAnnotatedClass(Game.class).setProperties(databasePropertiesWithDefaults).buildSessionFactory();
            } else {
                System.err.println("No Database Properties could be found." + " Using default values.");
                this.hibernateSessionFactory = new AnnotationConfiguration().addAnnotatedClass(User.class).addAnnotatedClass(Game.class).setProperties(defaultProperties).buildSessionFactory();
                System.err.println("Created database in " + databasePath);
            }
            updateNumberOfUsers();
        } else {
            System.err.println("DataProvider.init(): Database already initialized.");
        }
    }

    /**
	 *computes the total number of users stored in the database
	 */
    private void updateNumberOfUsers() {
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        this.numberOfUsers = ((Long) session.createQuery("select count(*) from User").uniqueResult()).intValue();
        session.getTransaction().commit();
    }

    /**
	 * Do a clean SHUTDOWN of the database.
	 */
    public void destroy() {
        Session sessiondown = this.getHibernateSessionFactory().getCurrentSession();
        sessiondown.beginTransaction();
        sessiondown.createSQLQuery("SHUTDOWN").executeUpdate();
        sessiondown.getTransaction().commit();
        this.hibernateSessionFactory = null;
        System.err.println("Database shut down.");
    }

    /**
	 * Returns Hibernate's session factory.
	 * 
	 * @return The hibernate session factory.
	 */
    public SessionFactory getHibernateSessionFactory() {
        return this.hibernateSessionFactory;
    }

    /**
	 * Gets all the saved games of a given user.
	 * @param creator The user whose games to get.
	 * @return The games that the user had saved as their creator before.
	 */
    @SuppressWarnings("unchecked")
    public synchronized Set<Game> getGames(User creator) {
        Set<Game> result = new HashSet<Game>();
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        result.addAll(session.createCriteria(Game.class).add(Restrictions.ilike("creatorName", creator.getName())).addOrder(Order.desc("created")).list());
        session.getTransaction().commit();
        Iterator<Game> it = result.iterator();
        while (it.hasNext()) {
            it.next().setCreator(creator);
        }
        return result;
    }

    /**
	 * Searches for a given user in the database and returns its user object
	 * when found and successfully validated the password.
	 * 
	 * @param name
	 *            Username of the user who wants to login.
	 * @param password
	 *            Password of the user who wants to login.
	 * @param sessionID
	 *            The wicket session.
	 * @return The user Object fitting to name and password if the user exists,
	 *         else null.
	 */
    public synchronized User loginUser(String name, String password, String sessionID) {
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        User user = (User) session.createCriteria(User.class).add(Restrictions.ilike("name", name)).uniqueResult();
        session.getTransaction().commit();
        if ((user != null) && user.getPassword().equals(DataProvider.hashPassword(password))) {
            user.setTemporaryAccessCode(null);
            user.setSession(sessionID);
            this.onlinePlayers.add(user);
            return user;
        }
        return null;
    }

    /**
	 * Searches for a given user in the database and returns its user object
	 * when found and successfully validated the temporary access code.
	 * 
	 * @param name
	 *            Username of the user who wants to change his password.
	 * @param temporaryAccessCode
	 *            The temporary access code.
	 * @param sessionID
	 *            The wicket session.
	 * @return The user Object fitting to name and temporary access code if the
	 *         user exists, else null.
	 */
    public synchronized User loginUserWithTemporaryAccessCode(String name, String temporaryAccessCode, String sessionID) {
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        User user = (User) session.createCriteria(User.class).add(Restrictions.ilike("name", name)).uniqueResult();
        session.getTransaction().commit();
        if ((user != null) && user.getTemporaryAccessCode() != null && !user.getTemporaryAccessCode().equals("") && user.getTemporaryAccessCode().equals(temporaryAccessCode)) {
            return user;
        }
        return null;
    }

    /**
	 * Adds a new user to the database.
	 * 
	 * @param name
	 *            The new users name, should differ from all existing user
	 *            names.
	 * @param password
	 *            The password for the new user, should be at least 6 Characters
	 *            long.
	 * @param email
	 *            The EMailadress for the new user, must be valid to add user.
	 * @return <code>True</code> if the user was added, <code> false</code>
	 *         else, perhaps the name already exists.
	 */
    public synchronized boolean addUser(String name, String password, String email) {
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        User user = new User(name, password, email);
        try {
            if (session.createCriteria(User.class).add(Restrictions.ilike("name", name)).list().size() == 0) {
                session.save(user);
                session.getTransaction().commit();
                numberOfUsers++;
                return true;
            }
        } catch (HibernateException e) {
            session.getTransaction().rollback();
        }
        return false;
    }

    /**
	 * Returns a list containing all users fitting the specified pattern. It is
	 * tried to match <code>search</code> to a username or to an email
	 * address.
	 * 
	 * @param search
	 *            The user's name or email address.
	 * @return A {@link java.util.List} containing all users fitting to the
	 *         specified pattern.
	 */
    @SuppressWarnings("unchecked")
    public synchronized Set<User> listUsers(String search) {
        Set<User> result = new HashSet<User>();
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        result.addAll(session.createCriteria(User.class).add(Restrictions.or(Restrictions.ilike("email", search, MatchMode.EXACT), Restrictions.ilike("name", search, MatchMode.EXACT))).list());
        session.getTransaction().commit();
        return result;
    }

    /**
	 * Returns a list containing all users fitting the specified pattern. It is
	 * tried to match <code>search</code> to any of name, realName, email,
	 * homePage, instantMessaging, city, state, country, birthDate.
	 * 
	 * @param search
	 *            The user's name or email address or any other data of the
	 *            user.
	 * @param firstResult The index of the first user to return (counting from 0).
	 * @param numberOfResults The maximum number of users to return.
	 * @return A {@link java.util.List} containing all users fitting to the
	 *         specified pattern but at most
	 *         DataProvider.MAX_USERS_IN_SEARCH_RESULT users.
	 */
    @SuppressWarnings("unchecked")
    public synchronized Set<User> searchUsers(String search, int firstResult, int numberOfResults) {
        Set<User> result = new HashSet<User>();
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        Disjunction disjunction = Restrictions.disjunction();
        disjunction.add(Restrictions.ilike("name", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("realName", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("email", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("homePage", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("instantMessaging", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("city", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("state", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("country", search, MatchMode.ANYWHERE));
        disjunction.add(Restrictions.ilike("birthDate", search, MatchMode.ANYWHERE));
        result.addAll(session.createCriteria(User.class).add(disjunction).setFirstResult(firstResult).setMaxResults(numberOfResults).list());
        session.getTransaction().commit();
        return result;
    }

    /**
	 * Get the first 200 users in a specified order.
	 * @param propertyName Name of the property to sort by.
	 * @param ascending Whether the ordering should be ascending.
	 * @param firstResult The index of the first user to return (counting from 0).
	 * @param numberOfResults The maximum number of users to return.
	 * @return The list of users.
	 */
    @SuppressWarnings("unchecked")
    public synchronized List<User> getSortedUsers(String propertyName, boolean ascending, int firstResult, int numberOfResults) {
        List<User> result = new Vector<User>();
        Order ordering = (ascending ? Order.asc(propertyName) : Order.desc(propertyName));
        Session session = this.getHibernateSessionFactory().getCurrentSession();
        session.beginTransaction();
        result = session.createCriteria(User.class).addOrder(ordering).setFirstResult(firstResult).setMaxResults(numberOfResults).list();
        session.getTransaction().commit();
        return result;
    }

    /**
	 * Returns the list of games.
	 * 
	 * @return The list of games.
	 */
    public GameList getGameList() {
        return this.games;
    }

    /**
	 * Returns the list of online players.
	 * 
	 * @return The list of online players.
	 */
    public PlayerList getPlayerList() {
        return this.onlinePlayers;
    }

    /**
	 * @return the total number of users stored in the database
	 */
    public int getNumberOfUsers() {
        return numberOfUsers;
    }
}
