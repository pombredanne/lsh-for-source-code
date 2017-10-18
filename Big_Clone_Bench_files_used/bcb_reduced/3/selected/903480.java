package mass.DBConnection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Timestamp;
import java.util.Vector;
import mas.common.Digest;
import mas.common.Meeting;
import mas.common.MeetingDocument;
import mas.common.User;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class DBTest {

    static UserDB userdb;

    static MeetingDB meetingdb;

    static Vector<User> users;

    static Meeting meeting;

    static MeetingDocument meetingdoc;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        System.out.println("Loading Driver");
        Class.forName("com.mysql.jdbc.Driver");
        String host = "localhost";
        String dbuser = "hypersquirrel";
        String dbpass = "12345";
        Connection c = DriverManager.getConnection("jdbc:mysql://" + host + ":3306/hypersquirrel", dbuser, dbpass);
        DriverManager.setLogWriter(new PrintWriter(System.out));
        System.out.println("calling db");
        userdb = new UserDB(c);
        meetingdb = new MeetingDB(c, userdb);
    }

    @Test
    public void test10_checkRolesFail() {
        assertFalse(meetingdb.isMember(0, "gibts nicht"));
        assertFalse(meetingdb.isModerator(0, "gibts nicht"));
        assertFalse(meetingdb.isRecorder(0, "gibts nicht"));
    }

    @Test
    public void test20_changeUserInfos() {
        userdb.setEMail("f", "a@b.c");
        assertEquals(userdb.getUser("f").getEmail(), "a@b.c");
        userdb.setEMail("f", "");
        assertEquals(userdb.getUser("f").getEmail(), "");
    }

    @Test
    public void test30_createUserFail() {
        try {
            assertFalse(userdb.addUser("f", "", "", Digest.digest("12345".getBytes()), ""));
        } catch (NoSuchAlgorithmException e) {
            fail();
        }
    }

    @Test
    public void test40_getAllUsers() {
        users = userdb.getAllUsers();
        assertNotNull(users);
    }

    @Test
    public void test50_createMeeting() {
        User mod = users.get(users.indexOf(userdb.getUser("f")));
        User rec = users.lastElement();
        Vector<Meeting> meetings;
        assertNotNull(mod);
        meeting = new Meeting("Label", "Description: JUnit Test", "Location", new Timestamp(0), mod, rec, users);
        meetingdb.createMeeting(meeting);
        meetings = meetingdb.getMeetings("f", new Timestamp(0), new Timestamp(0));
        assertEquals(meeting.getLabel(), meetings.lastElement().getLabel());
        assertEquals(meeting.getDescription(), meetings.lastElement().getDescription());
        assertEquals(meeting.getLocation(), meetings.lastElement().getLocation());
        assertEquals(meeting.getDate(), meetings.lastElement().getDate());
        assertEquals(meeting.getModerator(), meetings.lastElement().getModerator());
        assertEquals(meeting.getRecorder(), meetings.lastElement().getRecorder());
        assertEquals(meeting.getUser().size(), meetings.lastElement().getUser().size());
        meeting = meetings.lastElement();
    }

    @Test
    public void test60_changeMeeting() {
        Vector<Meeting> meetings;
        meetingdb.setLabel(meeting.getId(), "abcab");
        meetingdb.setDescription(meeting.getId(), "abcab");
        meetingdb.setLocation(meeting.getId(), "abcab");
        meetings = meetingdb.getMeetings("f", new Timestamp(0), new Timestamp(0));
        assertEquals(meetings.lastElement().getLabel(), "abcab");
        assertEquals(meetings.lastElement().getDescription(), "abcab");
        assertEquals(meetings.lastElement().getLocation(), "abcab");
        meeting = meetings.lastElement();
    }

    @Test
    public void test70_saveMeetingDoc() {
        MeetingDocument meetingdoc2;
        meetingdoc = meetingdb.getMeetingDoc(meeting.getId());
        assertNull(meetingdoc);
        meetingdoc = new MeetingDocument(meeting.getId(), null);
        meetingdoc.addTOP("label#1", "agenda#1", "protocol#1");
        meetingdoc.addTOP("label#2", "agenda#2", "protocol#2");
        meetingdoc.moveDown(0);
        assertEquals(meetingdoc.getAgenda(1), "agenda#1");
        assertEquals(meetingdoc.getAgenda(0), "agenda#2");
        assertEquals(meetingdoc.size(), 2);
        meetingdb.setMeetingDoc(meeting.getId(), meetingdoc);
        meetingdoc2 = meetingdb.getMeetingDoc(meeting.getId());
        assertEquals(meetingdoc.size(), meetingdoc2.size());
        assertEquals(meetingdoc.getAgenda(0), meetingdoc2.getAgenda(0));
        assertEquals(meetingdoc.getProtocol(1), meetingdoc2.getProtocol(1));
    }

    @Test
    public void test90_deleteMeeting() {
        Vector<Meeting> meetings;
        meetingdb.deleteMeeting(meeting.getId());
        meetings = meetingdb.getMeetings("f", new Timestamp(0), new Timestamp(0));
        assertEquals(0, meetings.size());
        userdb.logout("f");
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }
}
