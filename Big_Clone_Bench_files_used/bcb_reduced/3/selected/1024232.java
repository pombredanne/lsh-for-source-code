package edu.cmu.vlis.wassup.db;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;
import org.mybeans.dao.DAOException;
import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanFactoryException;
import org.mybeans.factory.BeanTable;
import org.mybeans.factory.MatchArg;
import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;
import com.mysql.jdbc.Statement;
import edu.cmu.vlis.wassup.databean.UnprocessedEvent;
import edu.cmu.vlis.wassup.databean.UnprocessedEventTag;
import edu.cmu.vlis.wassup.databean.User;

/**
 * The class is used to access the database table Event
 * @author Jassica
 *
 */
public class UnprocessedEventDAO {

    private BeanFactory<UnprocessedEvent> factory;

    private static void loadProperties() {
        Properties props = new Properties();
        try {
            props.load(new FileInputStream("src/properties"));
            System.getProperties().putAll(props);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public UnprocessedEventDAO() throws DAOException {
        try {
            BeanTable.useJDBC("com.mysql.jdbc.Driver", System.getProperty("org.mybeans.factory.jdbcURL") + "?user=" + System.getProperty("org.mybeans.factory.user") + "&password=" + System.getProperty("org.mybeans.factory.password"));
            UserDAO ud = new UserDAO();
            BeanTable<UnprocessedEvent> eventTable = BeanTable.getInstance(UnprocessedEvent.class, "unp_events", ud.getFactory());
            if (!eventTable.exists()) {
                eventTable.create("hashId");
                String query = "alter table unp_events modify description nvarchar(1024) default NULL";
                Connection con = null;
                try {
                    Class.forName("com.mysql.jdbc.Driver").newInstance();
                    con = DriverManager.getConnection(System.getProperty("org.mybeans.factory.jdbcURL"), System.getProperty("org.mybeans.factory.user"), System.getProperty("org.mybeans.factory.password"));
                    Statement sta = (Statement) con.createStatement();
                    int count = sta.executeUpdate(query);
                    sta.close();
                    con.close();
                } catch (Exception e) {
                    throw new DAOException(e);
                }
            }
            eventTable.setIdleConnectionCleanup(true);
            factory = eventTable.getFactory();
        } catch (BeanFactoryException e) {
            throw new DAOException(e);
        }
    }

    /**
	 * To check whether the given announcement_id exists
	 * @param announcement_id
	 * @return
	 * @throws DAOkException 
	 */
    public boolean isEventExist(String eventId) {
        boolean result = false;
        UnprocessedEvent event;
        try {
            event = factory.lookup(eventId);
            if (event != null) result = true;
        } catch (RollbackException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** 
	 * Insert a new tuple of announcement into the beanTable
	 * Returns error message if creation fails.
	 * Otherwise return a null upon success.
	 * 
	 * @param event
	 * @param tags
	 * @return
	 * @throws RollbackException
	 */
    public String insert(UnprocessedEvent event, String tag, String URL) throws RollbackException {
        System.out.println("insert called with " + event + " tag - " + tag);
        String message = createMessage(event);
        String hashId = hash(message);
        event.setHashId(hashId);
        UnprocessedEvent dbEvent = factory.lookup(hashId);
        if (dbEvent == null) {
            if (event.getEndDate() == null || event.getEndDate().trim().equals("")) {
                if (event.getStartDate() != null) event.setEndDate(event.getStartDate());
            }
            if (event.getEndTime() == null || event.getEndTime().trim().equals("")) {
                if (event.getStartTime() != null) event.setEndTime(event.getStartTime());
            }
            if (event.getDescription() == null || event.getDescription().trim().equals("")) event.setDescription(event.getName());
            insert(event, URL);
        }
        try {
            UnprocessedEventTagDAO eventTagDAO = new UnprocessedEventTagDAO();
            UnprocessedEventTag eventTag = eventTagDAO.lookup(event, tag);
            if (eventTag == null) {
                eventTagDAO.insert(event, tag);
            } else {
                throw new RollbackException("Insertion fails: Duplication");
            }
        } catch (DAOException e) {
            throw new RollbackException(e);
        }
        System.out.println("Success");
        return event.getHashId();
    }

    private void insert(UnprocessedEvent event, String URL) throws RollbackException {
        DBConnector dbConnector = new DBConnector("wassup", System.getProperty("org.mybeans.factory.jdbcLocation"));
        String query = "insert into unp_events values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection con = dbConnector.getConnection();
        PreparedStatement insert = null;
        try {
            insert = con.prepareStatement(query);
            insert.setString(1, event.getHashId());
            insert.setInt(2, event.getAccepts());
            insert.setString(3, event.getCity());
            insert.setString(4, event.getDescription());
            insert.setInt(5, event.getDontknow());
            insert.setString(6, event.getEndDate());
            insert.setString(7, event.getEndTime());
            insert.setFloat(8, event.getLatitude());
            insert.setFloat(9, event.getLongitude());
            insert.setString(10, event.getName());
            insert.setInt(11, event.getRejects());
            insert.setString(12, event.getSender().getUserName());
            insert.setString(13, event.getStartDate());
            insert.setString(14, event.getStartTime());
            insert.setString(15, event.getState());
            insert.setString(16, event.getStreet());
            insert.setString(17, event.getUrl());
            insert.executeUpdate();
            insert.close();
            con.close();
        } catch (Exception e) {
            throw new RollbackException(e);
        }
    }

    /**
	 * @param event
	 * @return
	 */
    private String createMessage(UnprocessedEvent event) {
        String message = "";
        message += "Subject: " + event.getName();
        message += "; ";
        message += "Start Date: " + event.getStartDate();
        message += "; ";
        message += "Start Time: " + event.getStartTime();
        message += "; ";
        message += "End Date: ";
        if (event.getEndDate() == null || event.getEndDate().trim().equals("")) {
            message += event.getStartDate();
        } else message += event.getEndDate();
        message += "; ";
        message += "End Time: ";
        if (event.getEndTime() == null || event.getEndTime().trim().equals("")) {
            message += event.getStartTime();
        } else message += event.getEndTime();
        message += "; ";
        message += "Location: ";
        message += event.getStreet() + " " + event.getCity() + " " + event.getState() + " " + event.getUrl();
        message += "; ";
        message += "Description: ";
        if (event.getDescription() == null || event.getDescription().trim().equals("")) message += event.getName(); else message += event.getDescription();
        message += "; ";
        if (event.getSender() != null) message += "Sender: " + event.getSender().getUserName();
        message = message.trim();
        return message;
    }

    /** Lookup whether a typical announcement exists in the table
	 * Return Not Found if the announcements information is missing
	 * Otherwise return detailed information about announcement of a user
	 * 
	 * @param announcement_id
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedEvent lookup(String eventId) throws DAOException {
        UnprocessedEvent event = null;
        try {
            event = factory.lookup(eventId);
            System.out.println(event);
        } catch (RollbackException e) {
            throw new DAOException(e);
        }
        return event;
    }

    /**
	 * Get the hashvalue of the input message
	 * Stroe the hashvalue into the database to achieve primary key of the announcement
	 * @param message the user input clear-text message
	 * @return hash value of the input
	 */
    private String hash(String message) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Can't find the SHA1 algorithm in the java.security package");
        }
        String saltString = String.valueOf(12345);
        md.update(saltString.getBytes());
        md.update(message.getBytes());
        byte[] digestBytes = md.digest();
        StringBuffer digestSB = new StringBuffer();
        for (int i = 0; i < digestBytes.length; i++) {
            int lowNibble = digestBytes[i] & 0x0f;
            int highNibble = (digestBytes[i] >> 4) & 0x0f;
            digestSB.append(Integer.toHexString(highNibble));
            digestSB.append(Integer.toHexString(lowNibble));
        }
        String digestStr = digestSB.toString().trim();
        return digestStr;
    }

    /** Delete a tuple with primary key as announcement_id from announcement table
	 * return null if no tuple is affected
	 * 
	 * @param announcement_id
	 * @return
	 * @throws RollbackException
	 */
    public void delete(String eventId) throws DAOException {
        UnprocessedEvent event = null;
        try {
            event = factory.lookup(eventId);
        } catch (RollbackException e) {
            throw new DAOException(e);
        }
        if (event != null) {
            UnprocessedEventTagDAO eventTagDAO = new UnprocessedEventTagDAO();
            UnprocessedEventTag[] eventTags = eventTagDAO.getEventTagsForEvent(event);
            if (eventTags != null) {
                for (UnprocessedEventTag eventTag : eventTags) {
                    eventTagDAO.delete(eventTag.getEventId(), eventTag.getTag());
                }
            }
            try {
                if (!Transaction.isActive()) Transaction.begin();
                factory.delete(eventId);
                Transaction.commit();
            } catch (RollbackException e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * Get all the entries in one table
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedEvent[] getAll() {
        UnprocessedEvent[] allEvents = null;
        try {
            allEvents = factory.match();
        } catch (RollbackException e) {
            e.printStackTrace();
        }
        return allEvents;
    }

    /**
	 * Get all the announcements according to one user
	 * @param args
	 * @throws RollbackException 
	 * @throws RollbackException
	 */
    public UnprocessedEvent[] getFromSender(User me) {
        UnprocessedEvent[] allEventsFromMe = null;
        try {
            allEventsFromMe = factory.match(MatchArg.equals("sender", me));
        } catch (RollbackException e) {
            e.printStackTrace();
        }
        return allEventsFromMe;
    }

    public BeanFactory<UnprocessedEvent> getFactory() {
        return factory;
    }

    public static void main(String[] args) throws RollbackException {
        loadProperties();
        UnprocessedEventDAO eventDAO = null;
        UserDAO userDAO = null;
        String eventName = "GO INI Picnic";
        UnprocessedEvent picnic = new UnprocessedEvent("1");
        picnic.setName(eventName);
        picnic.setDescription("GO INI Spring Picnic - Bask in Sun");
        picnic.setStartDate("04-24-2009");
        picnic.setStartTime("12");
        picnic.setEndDate("04-24-2009");
        picnic.setCity("Pittsburgh");
        try {
            eventDAO = new UnprocessedEventDAO();
            userDAO = new UserDAO();
            User sender = userDAO.lookup("upcoming");
            picnic.setSender(sender);
            String hashId1 = eventDAO.insert(picnic, "music", "CMU-291950.WV.CC.CMU.EDU");
            System.out.println("Picnic inserted HashId 1 " + hashId1);
        } catch (DAOException e1) {
            e1.printStackTrace();
        }
        System.exit(0);
    }
}
