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
import edu.cmu.vlis.wassup.db.Announcement_Tag_Dao;

/**
 * The class is used to access the database table Announcement
 * @author Jassica
 *
 */
public class AnnouncementDao {

    private BeanTable<Announcement> announcementTable;

    private BeanFactory<Announcement> announcementFactory;

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
    public AnnouncementDao() {
        BeanTable.useJDBC("com.mysql.jdbc.Driver", System.getProperty("org.mybeans.factory.jdbcURL") + "?user=root&password=wassup");
        UserDao ud = new UserDao();
        announcementTable = BeanTable.getInstance(Announcement.class, "Announcement", ud.getFactory());
        createTable();
    }

    /** Constructor of Editor
	 * The default is used to store records in MYSQL database
	 * 
	 * @param URL
	 */
    public AnnouncementDao(String URL) {
        System.clearProperty("org.mybeans.factory.csvDirectory");
        System.setProperty("org.mybeans.factory.jdbcDriver", "com.mysql.jdbc.Driver");
        System.setProperty("org.mybeans.factory.jdbcURL", "jdbc:mysql://" + URL + "/wassup");
        System.setProperty("org.mybeans.factory.user", "root");
        System.setProperty("org.mybeans.factory.password", "wassup");
        UserDao ud = new UserDao();
        announcementTable = BeanTable.getInstance(Announcement.class, "Announcement", ud.getFactory());
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
        BeanEditor<Announcement> editor = BeanEditor.getInstance(announcementFactory);
        editor.start();
    }

    /**
	 * To check whether the given announcement_id exists
	 * @param announcement_id
	 * @return
	 * @throws RollbackException 
	 */
    public String validate(String announcement_id) throws RollbackException {
        Announcement announcement = announcementFactory.lookup(announcement_id);
        if (announcement == null) return null;
        return "Announcement Found";
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
    public String insert(Announcement ann, String tag) throws RollbackException {
        String message = convertMessage(ann);
        String announcement_id = hash(message);
        ann.setAnnouncement_id(announcement_id);
        if (!Transaction.isActive()) Transaction.begin();
        Announcement announcement = announcementFactory.lookup(announcement_id);
        System.out.println("Have been inserted?");
        if (announcement == null) {
            System.out.println("NO");
            announcement = announcementFactory.create(announcement_id);
            announcementFactory.copyInto(ann, announcement);
            if (ann.getEnd_date() == null || ann.getEnd_date().trim().equals("")) {
                announcement.setEnd_date(ann.getStart_date());
            }
            if (ann.getEnd_time() == null || ann.getEnd_time().trim().equals("")) {
                announcement.setEnd_time(ann.getStart_time());
            }
            if (ann.getDescription() == null || ann.getDescription().trim().equals("")) announcement.setDescription(ann.getEvent_name());
            Transaction.commit();
            System.out.println("Inserted Success");
        }
        Announcement_Tag_Dao tagmanager = new Announcement_Tag_Dao();
        if (tagmanager.lookup(announcement, tag) == null) {
            System.out.println("Insert into tag!");
            tagmanager.insert(announcement, tag);
        } else {
            throw new RollbackException("Insertion fails");
        }
        System.out.println("Success");
        return announcement.getAnnouncement_id();
    }

    /**
	 * @param ann
	 * @return
	 */
    private String convertMessage(Announcement ann) {
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
        if (ann.getSender() != null) message += "Sender: " + ann.getSender().getE_mail();
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
    public Announcement lookup(String announcement_id) throws RollbackException {
        Announcement announcement = announcementFactory.lookup(announcement_id);
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
    public Announcement delete(String announcement_id) throws RollbackException {
        Announcement announcement = announcementFactory.lookup(announcement_id);
        String result = "";
        if (announcement == null) {
            throw new RollbackException("Announcement does not exist!");
        } else {
            result += "DELETE: Content=" + announcement.getDescription() + ";Start_Date=" + announcement.getStart_date() + ";End_Date=" + announcement.getEnd_date();
            result += " \nin " + announcementFactory.getBeanCount() + " results.";
            Announcement_Tag_Dao tagmanager = new Announcement_Tag_Dao();
            Announcement_Tag[] ats = tagmanager.getTuplesFromAnnouncement(announcement);
            for (Announcement_Tag at : ats) {
                tagmanager.delete(at.getAnnounce_id(), at.getTag());
            }
            Announcement_User_Dao aud = new Announcement_User_Dao();
            Announcement_User[] aus = aud.getMessageFromAnnouncement(announcement);
            for (Announcement_User au : aus) {
                aud.delete(au.getAnnouncement_id(), au.getReceiver_id());
            }
            if (!Transaction.isActive()) Transaction.begin();
            announcementFactory.delete(announcement_id);
            Transaction.commit();
        }
        System.out.println(result);
        return announcement;
    }

    /**
	 * Get all the entries in one table
	 * @return
	 * @throws RollbackException
	 */
    public Announcement[] getAll() throws RollbackException {
        Announcement[] as = null;
        as = announcementFactory.match();
        return as;
    }

    /**
	 * Get all the announcements according to one user
	 * @param args
	 * @throws RollbackException 
	 * @throws RollbackException
	 */
    public Announcement[] getFromSender(User u) throws RollbackException {
        Announcement[] as = null;
        as = announcementFactory.match(MatchArg.equals("sender", u));
        return as;
    }

    public BeanFactory<Announcement> getFactory() {
        return announcementFactory;
    }

    public static void main(String[] args) throws RollbackException {
        loadProperties();
        AnnouncementDao edit = new AnnouncementDao();
        Announcement ann = new Announcement("1");
        ann.setCity("Pittsburgh");
        ann.setDescription("Have a party!");
        ann.setStart_date("2009-03-10");
        ann.setStart_time("13:22:10");
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("music");
        tags.add("movie");
        ann.setLatitude((float) 1.00);
        ann.setLongitude((float) 0.99);
        ann.setState("PA");
        ann.setStreet_address("CMU");
        ann.setURL("http://www.cmu.edu");
        tags.add("TV");
        String id = edit.insert(ann, "movie");
        ann = edit.lookup(id);
        ann.setCity("new york");
        ann.setDescription("Happy hour in NY");
        edit.lookup(id);
        Announcement[] anns = edit.getAll();
        System.out.println(anns.length + " elements totally");
        Announcement_Tag_Dao ud = new Announcement_Tag_Dao();
        String tagss[] = ud.getTagFromAnnouncement(anns[0]);
        System.out.println("User has " + tagss.length + " interests");
    }
}
