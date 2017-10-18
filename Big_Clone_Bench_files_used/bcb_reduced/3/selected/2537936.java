package masc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Vector;
import mas.common.AuthenticationException;
import mas.common.Digest;
import mas.common.LogicFactory;
import mas.common.Login;
import mas.common.Meeting;
import mas.common.MeetingDocument;
import mas.common.MeetingLogic;
import mas.common.User;
import mas.common.UserLogic;
import net.sf.lipermi.handler.CallHandler;
import net.sf.lipermi.handler.filter.GZipFilter;
import net.sf.lipermi.net.Client;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author hypersquirrel
 */
public class JUnitTest {

    static MeetingLogic meetinglogic;

    static UserLogic userlogic;

    static Login login;

    static Vector<User> users = null;

    static Meeting meeting;

    static MeetingDocument meetingdoc;

    @BeforeClass
    public static void setUp() throws Exception {
        CallHandler chandler = new CallHandler();
        final Client c;
        String host = "localhost";
        File keys;
        try {
            byte b[] = new byte[4096];
            int len;
            keys = File.createTempFile("keys", null);
            keys.deleteOnExit();
            InputStream in = Main.class.getResourceAsStream("/rmi.keys");
            FileOutputStream out = new FileOutputStream(keys);
            while ((len = in.read(b)) > 0) {
                out.write(b, 0, len);
            }
            in.close();
            out.close();
            System.setProperty("javax.net.ssl.trustStore", keys.getAbsolutePath());
            System.setProperty("javax.net.ssl.trustStorePassword", "123456");
            c = new Client(host, 3333, chandler, new GZipFilter());
            login = (Login) c.getGlobal(Login.class);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    @Test(expected = AuthenticationException.class)
    public void test10_loginFail() throws AuthenticationException {
        try {
            login.authenticate("f", Digest.digest("XXXXX".getBytes()));
        } catch (NoSuchAlgorithmException e) {
            fail();
        }
    }

    @Test
    public void test20_login() {
        try {
            LogicFactory factory = login.authenticate("f", Digest.digest("12345".getBytes()));
            userlogic = factory.getUserLogic();
            meetinglogic = factory.getMeetingLogic();
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void test30_createUserFail() {
        try {
            assertFalse(userlogic.addUser("f", "", "", Digest.digest("12345".getBytes()), ""));
        } catch (NoSuchAlgorithmException e) {
            fail();
        }
    }

    @Test
    public void test40_getAllUsers() {
        users = userlogic.getAllUsers();
        assertNotNull(users);
    }

    @Test
    public void test50_createMeeting() {
        User mod = users.get(users.indexOf(new User(userlogic.getUsername(), null, null, null)));
        User rec = users.lastElement();
        Vector<Meeting> meetings;
        assertNotNull(mod);
        meeting = new Meeting("Label", "Description: JUnit Test", "Location", new Timestamp(0), mod, rec, users);
        meetinglogic.createMeeting(meeting);
        meetings = meetinglogic.getMeetings(new Timestamp(0), new Timestamp(0));
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
        meetinglogic.setLabel(meeting.getId(), "abcab");
        meetinglogic.setDescription(meeting.getId(), "abcab");
        meetinglogic.setLocation(meeting.getId(), "abcab");
        meetings = meetinglogic.getMeetings(new Timestamp(0), new Timestamp(0));
        assertEquals(meetings.lastElement().getLabel(), "abcab");
        assertEquals(meetings.lastElement().getDescription(), "abcab");
        assertEquals(meetings.lastElement().getLocation(), "abcab");
        meeting = meetings.lastElement();
    }

    @Test
    public void test70_saveMeetingDoc() {
        MeetingDocument meetingdoc2;
        meetingdoc = meetinglogic.getMeetingDoc(meeting.getId());
        assertNull(meetingdoc);
        meetingdoc = new MeetingDocument(meeting.getId(), null);
        meetingdoc.addTOP("label#1", "agenda#1", "protocol#1");
        meetingdoc.addTOP("label#2", "agenda#2", "protocol#2");
        meetingdoc.moveDown(0);
        assertEquals(meetingdoc.getAgenda(1), "agenda#1");
        assertEquals(meetingdoc.getAgenda(0), "agenda#2");
        assertEquals(meetingdoc.size(), 2);
        meetinglogic.setMeetingDoc(meeting.getId(), meetingdoc);
        meetingdoc2 = meetinglogic.getMeetingDoc(meeting.getId());
        assertEquals(meetingdoc.size(), meetingdoc2.size());
        assertEquals(meetingdoc.getAgenda(0), meetingdoc2.getAgenda(0));
        assertEquals(meetingdoc.getProtocol(1), meetingdoc2.getProtocol(1));
    }

    @Test
    public void test90_deleteMeeting() {
        Vector<Meeting> meetings;
        meetinglogic.deleteMeeting(meeting.getId());
        meetings = meetinglogic.getMeetings(new Timestamp(0), new Timestamp(0));
        assertEquals(0, meetings.size());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        userlogic.logout();
        meetinglogic = null;
        userlogic = null;
    }
}
