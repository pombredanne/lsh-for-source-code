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
import edu.cmu.vlis.wassup.databean.Event;
import edu.cmu.vlis.wassup.databean.EventTag;
import edu.cmu.vlis.wassup.databean.EventsForUser;
import edu.cmu.vlis.wassup.databean.User;

/**
 * The class is used to access the database table Event
 * @author Jassica
 *
 */
public class EventDAO {

    private BeanFactory<Event> factory;

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

    public EventDAO() throws DAOException {
        try {
            String jdbcDriverName = "com.mysql.jdbc.Driver";
            String jdbcURL = System.getProperty("org.mybeans.factory.jdbcURL");
            BeanTable.useJDBC("com.mysql.jdbc.Driver", System.getProperty("org.mybeans.factory.jdbcURL") + "?user=" + System.getProperty("org.mybeans.factory.user") + "&password=" + System.getProperty("org.mybeans.factory.password"));
            UserDAO userDAO = new UserDAO();
            BeanTable<Event> eventTable = BeanTable.getInstance(Event.class, "events", userDAO.getFactory());
            if (!eventTable.exists()) {
                eventTable.create("hashId");
                String query = "alter table events modify description varchar(2048) default NULL";
                try {
                    Class.forName("com.mysql.jdbc.Driver").newInstance();
                    Connection con = DriverManager.getConnection(System.getProperty("org.mybeans.factory.jdbcURL"), System.getProperty("org.mybeans.factory.user"), System.getProperty("org.mybeans.factory.password"));
                    Statement sta = (Statement) con.createStatement();
                    sta.executeUpdate(query);
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

    public EventDAO(String URL) throws DAOException {
        try {
            String jdbcDriverName = "com.mysql.jdbc.Driver";
            String jdbcURL = "jdbc:mysql://" + URL + "/wassup?user=root&password=mysql";
            BeanTable.useJDBC(jdbcDriverName, jdbcURL);
            UserDAO userDAO = new UserDAO();
            BeanTable<Event> eventTable = BeanTable.getInstance(Event.class, "events", userDAO.getFactory());
            if (!eventTable.exists()) {
                eventTable.create("hashId");
                System.out.println("table created");
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
        Event event;
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
    public String insert(Event event, String tag) throws RollbackException {
        System.out.println("insert called with " + event + " tag - " + tag);
        String message = createMessage(event);
        String hashId = hash(message);
        event.setHashId(hashId);
        Event dbEvent = factory.lookup(hashId);
        if (dbEvent == null) {
            if (event.getEndDate() == null || event.getEndDate().trim().equals("")) {
                if (event.getStartDate() != null) event.setEndDate(event.getStartDate());
            }
            if (event.getEndTime() == null || event.getEndTime().trim().equals("")) {
                if (event.getStartTime() != null) event.setEndTime(event.getStartTime());
            }
            if (event.getDescription() == null || event.getDescription().trim().equals("")) event.setDescription(event.getName());
            insert(event);
        }
        try {
            EventTagDAO eventTagDAO = new EventTagDAO();
            EventTag eventTag = eventTagDAO.lookup(event, tag);
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

    private void insert(Event event) throws RollbackException {
        String query = "insert into events values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        Connection con = null;
        PreparedStatement insert = null;
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            con = DriverManager.getConnection(System.getProperty("org.mybeans.factory.jdbcURL"), System.getProperty("org.mybeans.factory.user"), System.getProperty("org.mybeans.factory.password"));
            insert = con.prepareStatement(query);
            insert.setString(1, event.getHashId());
            insert.setInt(2, 0);
            insert.setString(3, event.getCity());
            insert.setString(4, event.getDescription());
            insert.setInt(5, 0);
            insert.setString(6, event.getEndDate());
            insert.setString(7, event.getEndTime());
            insert.setFloat(8, event.getLatitude());
            insert.setFloat(9, event.getLongitude());
            insert.setString(10, event.getName());
            insert.setInt(11, 0);
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
    private String createMessage(Event event) {
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
    public Event lookup(String eventId) throws DAOException {
        Event event = null;
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
        Event event = null;
        try {
            event = factory.lookup(eventId);
        } catch (RollbackException e) {
            throw new DAOException(e);
        }
        if (event != null) {
            EventTagDAO eventTagDAO = new EventTagDAO();
            EventTag[] eventTags = eventTagDAO.getEventTagsForEvent(event);
            if (eventTags != null) {
                for (EventTag eventTag : eventTags) {
                    eventTagDAO.delete(eventTag.getEventId(), eventTag.getTag());
                }
            }
            EventsForUserDAO eventsForUserDAO = new EventsForUserDAO();
            EventsForUser[] eventsForUsers = eventsForUserDAO.getEventsForUser(event);
            if (eventsForUsers != null) {
                for (EventsForUser eventsForUser : eventsForUsers) {
                    eventsForUserDAO.delete(eventsForUser.getEventId(), eventsForUser.getUserId());
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
    public Event[] getAll() {
        Event[] allEvents = null;
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
    public Event[] getFromSender(User me) {
        Event[] allEventsFromMe = null;
        try {
            allEventsFromMe = factory.match(MatchArg.equals("sender", me));
        } catch (RollbackException e) {
            e.printStackTrace();
        }
        return allEventsFromMe;
    }

    public BeanFactory<Event> getFactory() {
        return factory;
    }

    public void setAccept(String hashId) {
        try {
            Transaction.begin();
            Event event = lookup(hashId);
            int accepts = event.getAccepts();
            accepts++;
            event.setAccepts(accepts);
            Transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setReject(String hashId) {
        try {
            Transaction.begin();
            Event event = lookup(hashId);
            int rejects = event.getRejects();
            rejects++;
            event.setRejects(rejects);
            Transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setDontKnow(String hashId) {
        try {
            Transaction.begin();
            Event event = lookup(hashId);
            int dontKnow = event.getDontknow();
            dontKnow++;
            event.setDontknow(dontKnow);
            Transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws RollbackException, DAOException {
        loadProperties();
        EventDAO eventDAO = new EventDAO();
        UserDAO userDAO = null;
        String eventName = "GO INI Picnic";
        Event picnic = new Event("1");
        picnic.setName(eventName);
        picnic.setDescription("GO INI Spring Picnic - Bask in Sun");
        picnic.setStartDate("04-24-2009");
        picnic.setStartTime("12");
        picnic.setEndDate("04-24-2009");
        picnic.setCity("Pittsburgh");
        try {
            eventDAO = new EventDAO();
            userDAO = new UserDAO();
            User sender = userDAO.lookup("upcoming");
            picnic.setSender(sender);
            String hashId1 = eventDAO.insert(picnic, "music");
            System.out.println("Picnic inserted HashId 1 " + hashId1);
        } catch (DAOException e1) {
            e1.printStackTrace();
        }
        System.exit(0);
    }
}
