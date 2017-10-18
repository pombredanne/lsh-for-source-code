package edu.cmu.vlis.wassup.db;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.mybeans.editor.BeanEditor;
import org.mybeans.factory.BeanFactory;
import org.mybeans.factory.BeanTable;
import org.mybeans.factory.MatchArg;
import org.mybeans.factory.RollbackException;
import org.mybeans.factory.Transaction;

/**
 * The class is used to access the database table Announcement
 * @author Jassica
 *
 */
public class UnprocessedAnnouncementDao {

    private BeanTable<UnprocessedAnnouncement> announcementTable;

    private BeanFactory<UnprocessedAnnouncement> announcementFactory;

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

    /** Constructor of Editor
	 * The default is used to store records in MYSQL database
	 * without JDBC connection information
	 *  
	 */
    public UnprocessedAnnouncementDao() {
        BeanTable.useJDBC("com.mysql.jdbc.Driver", System.getProperty("org.mybeans.factory.jdbcURL") + "?user=root&password=wassup");
        UserDao ud = new UserDao();
        announcementTable = BeanTable.getInstance(UnprocessedAnnouncement.class, "UnprocessedAnnouncement", ud.getFactory());
        createTable();
    }

    /** Constructor of Editor
	 * The default is used to store records in MYSQL database
	 * 
	 * @param URL
	 */
    public UnprocessedAnnouncementDao(String URL) {
        System.clearProperty("org.mybeans.factory.csvDirectory");
        System.setProperty("org.mybeans.factory.jdbcDriver", "com.mysql.jdbc.Driver");
        System.setProperty("org.mybeans.factory.jdbcURL", "jdbc:mysql://" + URL + "/wassup");
        System.setProperty("org.mybeans.factory.user", "root");
        System.setProperty("org.mybeans.factory.password", "wassup");
        UserDao ud = new UserDao();
        announcementTable = BeanTable.getInstance(UnprocessedAnnouncement.class, "UnprocessedAnnouncement", ud.getFactory());
        createTable();
    }

    /** Create an announcement beanTable 
	 * 1) if it already exists, keep the old one.
	 * 2) if it does not exist, create a new one.
	 * 
	 */
    public void createTable() {
        if (!announcementTable.exists()) announcementTable.create("announcement_id");
        announcementFactory = announcementTable.getFactory();
    }

    public void createEditor() {
        BeanEditor<UnprocessedAnnouncement> editor = BeanEditor.getInstance(announcementFactory);
        editor.start();
    }

    /**
	 * To check whether the given announcement_id exists
	 * @param announcement_id
	 * @return
	 * @throws RollbackException 
	 */
    public String validate(String announcement_id) throws RollbackException {
        UnprocessedAnnouncement announcement = announcementFactory.lookup(announcement_id);
        if (announcement == null) return null;
        return "Announcement Found";
    }

    /**
	 * @param ann
	 * @return
	 */
    private String convertMessage(UnprocessedAnnouncement ann) {
        String message = "";
        message += "Subject: " + ann.getEvent_name();
        message += "; ";
        message += "Start Date: " + ann.getStart_date();
        message += "; ";
        message += "Start Time: " + ann.getStart_time();
        message += "; ";
        message += "End Date: ";
        if (ann.getEnd_date() == null || ann.getEnd_date().trim().equals("")) {
            message += ann.getStart_date();
        } else message += ann.getEnd_date();
        message += "; ";
        message += "End Time: ";
        if (ann.getEnd_time() == null || ann.getEnd_time().trim().equals("")) {
            message += ann.getStart_time();
        } else message += ann.getEnd_time();
        message += "; ";
        message += "Location: ";
        message += ann.getStreet_address() + " " + ann.getCity() + " " + ann.getState() + " " + ann.getURL();
        message += "; ";
        message += "Description: ";
        if (ann.getDescription() == null || ann.getDescription().trim().equals("")) message += ann.getEvent_name(); else message += ann.getDescription();
        message += "; ";
        message += "Sender: " + ann.getSender().getE_mail();
        message = message.trim();
        return message;
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

    /** 
	 * Insert a new tuple of announcement into the beanTable
	 * Returns error message if creation fails.
	 * Otherwise return a null upon success.
	 * 
	 * @param ann
	 * @param tags
	 * @return
	 * @throws RollbackException
	 */
    public String insert(UnprocessedAnnouncement ann, List<String> tags) throws RollbackException {
        String message = convertMessage(ann);
        String announcement_id = hash(message);
        ann.setAnnouncement_id(announcement_id);
        if (!Transaction.isActive()) Transaction.begin();
        if (validate(announcement_id) != null) {
            throw new RollbackException("UnprocessedAnnouncment already exist");
        }
        UnprocessedAnnouncement announcement = announcementFactory.create(announcement_id);
        announcementFactory.copyInto(ann, announcement);
        if (ann.getEnd_date() == null || ann.getEnd_date().trim().equals("")) {
            announcement.setEnd_date(ann.getStart_date());
        }
        if (ann.getEnd_time() == null || ann.getEnd_time().trim().equals("")) {
            announcement.setEnd_time(ann.getStart_time());
        }
        if (ann.getDescription() == null || ann.getDescription().trim().equals("")) announcement.setDescription(ann.getEvent_name());
        Transaction.commit();
        UnprocessedAnnouncement_Tag_Dao tagmanager = new UnprocessedAnnouncement_Tag_Dao();
        try {
            for (int i = 0; i < tags.size(); i++) tagmanager.insert(announcement, tags.get(i));
        } catch (RollbackException e) {
            delete(ann.getAnnouncement_id());
            throw new RollbackException("Announcement inserts successfully, while tag fails");
        }
        return announcement.getAnnouncement_id();
    }

    /** Lookup whether a typical announcement exists in the table
	 * Return Not Found if the announcements information is missing
	 * Otherwise return detailed information about announcement of a user
	 * 
	 * @param announcement_id
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedAnnouncement lookup(String announcement_id) throws RollbackException {
        UnprocessedAnnouncement announcement = announcementFactory.lookup(announcement_id);
        String result = "";
        if (announcement == null) {
            throw new RollbackException("Announcement does not exist!");
        } else {
            result += "SELECT: Content=" + announcement.getDescription() + ";Start_Date=" + announcement.getStart_date() + ";Start_Time=" + announcement.getStart_time() + ";End_Date=" + announcement.getEnd_date() + ";End_Time=" + announcement.getEnd_time() + ";Description=" + announcement.getDescription();
            result += " \nin " + announcementFactory.getBeanCount() + " results.";
        }
        System.out.println(result);
        return announcement;
    }

    /** Update an announcement record in the table
	 * Insert new tuple in the announcement table if the record does not exist
	 * Update the new tuple information 
	 * 
	 * @param announcement_id
	 * @param ann
	 * @param tags
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedAnnouncement update(String announcement_id, UnprocessedAnnouncement ann, List<String> tags) throws RollbackException {
        if (!Transaction.isActive()) Transaction.begin();
        UnprocessedAnnouncement announcement = announcementFactory.lookup(announcement_id);
        if (announcement == null) {
            throw new RollbackException("Announcement does not exist!");
        } else {
            announcementFactory.copyInto(ann, announcement);
            Transaction.commit();
            UnprocessedAnnouncement_Tag_Dao tagmanager = new UnprocessedAnnouncement_Tag_Dao();
            try {
                for (int i = 0; i < tags.size(); i++) tagmanager.insert(announcement, tags.get(i));
            } catch (RollbackException e) {
                delete(ann.getAnnouncement_id());
                throw new RollbackException("Announcement inserts successfully, while tag fails");
            }
        }
        return announcement;
    }

    /** Delete a tuple with primary key as announcement_id from announcement table
	 * return null if no tuple is affected
	 * 
	 * @param announcement_id
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedAnnouncement delete(String announcement_id) throws RollbackException {
        UnprocessedAnnouncement announcement = announcementFactory.lookup(announcement_id);
        String result = "";
        if (announcement == null) {
            throw new RollbackException("Announcement does not exist!");
        } else {
            result += "DELETE: Content=" + announcement.getDescription() + ";Start_Date=" + announcement.getStart_date() + ";End_Date=" + announcement.getEnd_date();
            result += " \nin " + announcementFactory.getBeanCount() + " results.";
            UnprocessedAnnouncement_Tag_Dao tagmanager = new UnprocessedAnnouncement_Tag_Dao();
            UnprocessedAnnouncement_Tag[] ats = tagmanager.getTuplesFromAnnouncement(announcement);
            for (UnprocessedAnnouncement_Tag at : ats) {
                tagmanager.delete(at.getAnnounce_id(), at.getTag());
            }
            if (!Transaction.isActive()) Transaction.begin();
            announcementFactory.delete(announcement_id);
            Transaction.commit();
        }
        System.out.println(result);
        return announcement;
    }

    /**
	 * Get all the announcements according to one user
	 * @param args
	 * @throws RollbackException 
	 * @throws RollbackException
	 */
    public UnprocessedAnnouncement[] getFromSender(User u) throws RollbackException {
        UnprocessedAnnouncement[] as = null;
        as = announcementFactory.match(MatchArg.equals("sender", u));
        return as;
    }

    /**
	 * Get all the entries in one table
	 * @return
	 * @throws RollbackException
	 */
    public UnprocessedAnnouncement[] getAll() throws RollbackException {
        UnprocessedAnnouncement[] as = null;
        as = announcementFactory.match();
        return as;
    }

    public BeanFactory<UnprocessedAnnouncement> getFactory() {
        return announcementFactory;
    }

    public static void main(String[] args) throws RollbackException {
        loadProperties();
        UnprocessedAnnouncementDao edit = new UnprocessedAnnouncementDao();
        UnprocessedAnnouncement ann = new UnprocessedAnnouncement("1");
        ann.setCity("Pittsburgh");
        ann.setDescription("Have a party!");
        ann.setStart_date("2009-03-10");
        ann.setStart_time("13:22:10");
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("music");
        tags.add("movie");
        ann.setLatitude((float) 1.00);
        ann.setLongitude((float) 0.99);
        UserDao userD = new UserDao();
        User user = userD.lookup("jassica.jiafei@gmail.com");
        ann.setSender(user);
        ann.setState("PA");
        ann.setStreet_address("CMU");
        ann.setURL("http://www.cmu.edu");
        tags.add("TV");
        String id = edit.insert(ann, tags);
        ann = edit.lookup(id);
    }
}
