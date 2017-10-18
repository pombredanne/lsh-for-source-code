package org.homedns.krolain.MochaJournal.Protocol;

import java.net.*;
import java.nio.charset.*;
import java.io.*;
import org.xml.sax.*;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.homedns.krolain.util.InstallInfo;
import org.homedns.krolain.XMLRPC.*;
import org.homedns.krolain.swing.ProgressBarInputStream;
import java.util.*;
import org.homedns.krolain.MochaJournal.LJData.*;
import java.nio.ByteBuffer;
import java.util.regex.*;
import org.homedns.krolain.MochaJournal.EntryFrame;

/**
 *
 * @author  jsmith
 */
public class XMLProtocol implements java.lang.Runnable, java.lang.Cloneable {

    protected static final int GET_CHALL_STEP = 1;

    protected static final int SEND_REQ_STEP = 2;

    protected static final int DNLD_RESP_STEP = 3;

    protected static final int PROCESS_RESP_STEP = 4;

    protected static final int DONE_STEP = 5;

    HttpURLConnection m_Conn = null;

    XMLRPCLJ m_Request = null;

    XMLRPCObject m_Response = null;

    String m_szRequestName = null;

    String m_szUname;

    String m_szPwd;

    boolean m_bUseMD5 = false;

    boolean m_bLoggedIn;

    ProtProgress m_ProgDlg = null;

    int m_iVersion = 0;

    boolean m_bFastServer;

    BufferedWriter m_Writer;

    protected String m_szChallenge = "";

    javax.swing.JFrame m_Parent = null;

    protected static int m_iProxyPort = 80;

    protected static String m_szProxyAddr = "";

    protected static boolean m_bProxyOn = false;

    protected byte[] m_bytePwd = null;

    public class RPCChallenge extends XMLRPCObject {

        public RPCChallenge() {
            super(null);
        }

        public String m_auth_scheme = null;

        public String m_challenge = null;

        public int m_expire_time = -1;

        public int m_server_time = -1;
    }

    /** Creates a new instance of XMLProtocol */
    public XMLProtocol() {
        this(null);
    }

    public XMLProtocol(javax.swing.JFrame parent) {
        m_bLoggedIn = false;
        m_iVersion = Charset.isSupported("UTF-8") ? 1 : 0;
        m_Writer = null;
        m_bFastServer = false;
        if (parent != null) m_ProgDlg = new ProtProgress(parent, true);
    }

    public Object clone() {
        XMLProtocol clone = new XMLProtocol();
        clone.m_bUseMD5 = m_bUseMD5;
        clone.m_bLoggedIn = m_bLoggedIn;
        clone.m_szPwd = m_szPwd;
        clone.m_szUname = m_szUname;
        clone.m_ProgDlg = m_ProgDlg;
        return clone;
    }

    public void setParent(javax.swing.JFrame parent) {
        m_Parent = parent;
        if (m_ProgDlg != null) {
            m_ProgDlg.setVisible(false);
            m_ProgDlg.dispose();
            m_ProgDlg = null;
        }
        if (parent != null) m_ProgDlg = new ProtProgress(parent, true);
    }

    protected boolean SendXMLRequest(String methodName, XMLRPCObject value) {
        int i;
        String contentlen;
        if (methodName == null) return false;
        if (methodName.length() == 0) return false;
        System.out.print("Sending request(" + methodName + ")");
        if (value != null) System.out.println(": " + value.toString(true)); else System.out.println();
        try {
            URL LJUrl = null;
            if (m_bProxyOn) LJUrl = new URL("http", m_szProxyAddr, m_iProxyPort, "http://www.livejournal.com/interface/xmlrpc"); else LJUrl = new URL("http", "www.livejournal.com", 80, "/interface/xmlrpc");
            m_Conn = (HttpURLConnection) LJUrl.openConnection();
            if (m_bProxyOn) m_Conn.setRequestProperty("Host", m_szProxyAddr); else m_Conn.setRequestProperty("Host", "www.livejournal.com");
            m_Conn.setRequestMethod("POST");
            m_Conn.setRequestProperty("Content-type", "text/xml");
            String szUserAgent = "JAVA-";
            szUserAgent += InstallInfo.getString("app.title");
            szUserAgent += '/';
            szUserAgent += InstallInfo.getString("app.version.number");
            m_Conn.setRequestProperty("User-Agent", szUserAgent);
            if (m_bFastServer) {
                m_Conn.setRequestProperty("Cookie", "ljfastserver=1");
            }
            String szRequest = "<?xml version=\"1.0\"";
            if (m_iVersion == 1) szRequest += " encoding=\"UTF-8\"";
            szRequest += "?><methodCall><methodName>" + methodName + "</methodName>";
            if (value != null) szRequest += "<params><param><value>" + value.toString(false) + "</value></param></params>";
            szRequest += "</methodCall>";
            if (m_iVersion == 1) i = szRequest.getBytes("UTF-8").length; else i = szRequest.length();
            m_Conn.setRequestProperty("Content-length", Integer.toString(i));
            m_Conn.setAllowUserInteraction(true);
            m_Conn.setInstanceFollowRedirects(true);
            m_Conn.setDoInput(true);
            m_Conn.setDoOutput(true);
            m_Conn.setUseCaches(false);
            if (m_iVersion == 1) m_Writer = new BufferedWriter(new OutputStreamWriter(m_Conn.getOutputStream(), Charset.forName("UTF-8"))); else m_Writer = new BufferedWriter(new OutputStreamWriter(m_Conn.getOutputStream()));
            m_Writer.write(szRequest);
            m_Writer.flush();
            m_Writer.close();
        } catch (java.io.IOException e) {
            System.err.println(e.getMessage());
            if (m_Response != null) m_Response.m_faultstring = InstallInfo.getString("string.error.no.connect");
            m_Writer = null;
            m_Conn.disconnect();
            return false;
        }
        System.out.println("Request sent.");
        return true;
    }

    public static String toHex(byte[] MD5Digest) {
        String szResult = new String();
        for (int i = 0; i < MD5Digest.length; i++) {
            byte b = MD5Digest[i];
            int iVal = new Byte(b).intValue();
            String hex = Integer.toHexString(iVal);
            if (hex.length() == 1) szResult += "0" + hex; else if (hex.length() > 2) szResult += hex.substring(hex.length() - 2, hex.length()); else szResult += hex;
        }
        return szResult;
    }

    protected boolean fillLoginValues(XMLRPCLJ XMLObj) {
        XMLObj.m_username = m_szUname;
        XMLObj.m_ver = new Integer(1);
        if (m_bUseMD5) {
            if (!getchallenge()) {
                XMLObj.m_auth_method = "clear";
                XMLObj.m_hpassword = m_szPwd;
            } else {
                try {
                    java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
                    md.reset();
                    byte[] MD5PWD = md.digest((m_szChallenge + m_szPwd.toLowerCase()).getBytes());
                    String szMD5 = toHex(MD5PWD);
                    XMLObj.m_auth_method = "challenge";
                    XMLObj.m_auth_challenge = m_szChallenge;
                    XMLObj.m_auth_response = szMD5;
                } catch (java.security.NoSuchAlgorithmException e) {
                    System.err.println("No MD5 Encryption algorithm found.");
                    XMLObj.m_auth_method = "clear";
                    XMLObj.m_hpassword = m_szPwd;
                }
            }
        } else {
            XMLObj.m_auth_method = "clear";
            XMLObj.m_password = m_szPwd;
        }
        return true;
    }

    protected boolean getchallenge() {
        if (m_ProgDlg != null) m_ProgDlg.update(GET_CHALL_STEP, InstallInfo.getString("progress.step.challenge"));
        if (!SendXMLRequest("LJ.XMLRPC.getchallenge", null)) return false;
        try {
            if (m_Conn.getResponseMessage().compareToIgnoreCase("OK") == 0) {
                int i = 1;
                String szField;
                int len = 0;
                while ((szField = m_Conn.getHeaderFieldKey(i)) != null) {
                    if (szField.compareToIgnoreCase("Content-length") == 0) len = new Integer(m_Conn.getHeaderField(i)).intValue();
                    i++;
                }
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                RPCChallenge challenge = new RPCChallenge();
                XMLRPCHandler handler = new XMLRPCHandler(challenge);
                System.out.print("Response Received: ");
                handler.setLen(len);
                saxParser.parse(m_Conn.getInputStream(), handler);
                System.out.println();
                if (handler.allProcessed()) m_szChallenge = challenge.m_challenge; else return false;
            } else return false;
        } catch (IOException e) {
            System.err.println(e);
            return false;
        } catch (Exception e2) {
            System.err.println(e2);
            return false;
        }
        return true;
    }

    private void encode(String filename, Object obj) {
        String szPath = System.getProperty("user.home");
        szPath += System.getProperty("file.separator") + "MochaJournal" + System.getProperty("file.separator") + m_szUname + System.getProperty("file.separator") + filename;
        File groupxml = new File(szPath);
        try {
            if (!groupxml.exists()) groupxml.createNewFile();
            java.beans.XMLEncoder encode = new java.beans.XMLEncoder(new java.io.FileOutputStream(groupxml));
            encode.writeObject(obj);
            encode.close();
        } catch (java.io.FileNotFoundException e) {
            System.err.println(e);
        } catch (java.io.IOException e) {
            System.err.println(e);
        } catch (java.lang.Exception e) {
            System.err.println(e);
        }
    }

    public void saveGroups(LJGroups groups) {
        encode("groups.xml", groups);
    }

    public void saveFriends(LJFriends friends) {
        encode("friends.xml", friends);
    }

    private Object decode(String filename) {
        Object obj = null;
        String szPath = System.getProperty("user.home");
        szPath += System.getProperty("file.separator") + "MochaJournal" + System.getProperty("file.separator") + m_szUname + System.getProperty("file.separator") + filename;
        File groupxml = new File(szPath);
        try {
            if (groupxml.exists()) {
                java.beans.XMLDecoder decode = new java.beans.XMLDecoder(new java.io.FileInputStream(groupxml));
                obj = decode.readObject();
                decode.close();
            }
        } catch (java.io.FileNotFoundException e) {
            System.err.println(e);
        } catch (java.io.IOException e) {
            System.err.println(e);
        } catch (java.lang.Exception e) {
            System.err.println(e);
        }
        return obj;
    }

    private Vector loadGroups() {
        Vector result = new Vector();
        LJGroups groups = null;
        groups = (LJGroups) decode("groups.xml");
        if (groups != null) {
            int iSize = groups.size();
            for (int i = 0; i < iSize; i++) {
                XMLfriendgroup group = new XMLfriendgroup();
                LJGroups.LJGroup savedGroup = (LJGroups.LJGroup) groups.get(i);
                group.m_id = new Integer(savedGroup.m_iBit);
                group.m_sortorder = new Integer(savedGroup.m_iSortOrder);
                group.m_name = savedGroup.m_szName;
                group.m_public = new Integer(savedGroup.m_bPublic ? 1 : 0);
                result.add(group);
            }
        }
        return result;
    }

    public LJFriends loadFriends() {
        LJFriends ljFriends = (LJFriends) decode("friends.xml");
        return ljFriends;
    }

    public XMLlogin LogIn(String uName, String pwd, int iMaxMoodID) {
        XMLlogin login = new XMLlogin();
        if (!EntryFrame.m_bOnline) {
            m_szUname = uName;
            login.m_bSentOK = true;
            login.m_friendgroups = loadGroups();
            Object obj;
            obj = decode("journal.xml");
            if (obj != null) login.m_usejournals = (Vector) obj;
            obj = decode("pickw.xml");
            if (obj != null) login.m_pickws = (Vector) obj;
            return login;
        }
        XMLlogin.loginRequest loginRequest = new XMLlogin.loginRequest();
        loginRequest.m_username = uName;
        loginRequest.m_getmoods = new Integer(iMaxMoodID);
        loginRequest.m_getpickws = new Integer(1);
        loginRequest.m_getpickwurls = new Integer(1);
        loginRequest.m_getmenus = new Integer(1);
        String szMD5Pwd = new String();
        m_szUname = uName;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            md.reset();
            m_bytePwd = md.digest(pwd.getBytes());
            szMD5Pwd = toHex(m_bytePwd);
            if (!getchallenge()) {
                loginRequest.m_auth_method = "clear";
                loginRequest.m_hpassword = szMD5Pwd;
            } else {
                md.reset();
                byte[] MD5PWD = md.digest((m_szChallenge + szMD5Pwd.toLowerCase()).getBytes());
                String szMD5 = toHex(MD5PWD);
                loginRequest.m_auth_method = "challenge";
                loginRequest.m_auth_challenge = m_szChallenge;
                loginRequest.m_auth_response = szMD5;
            }
            m_bUseMD5 = true;
        } catch (java.security.NoSuchAlgorithmException e) {
            System.err.println("No MD5 Encryption algorithm found.");
            loginRequest.m_auth_method = "clear";
            loginRequest.m_password = pwd;
        }
        if (m_bUseMD5) m_szPwd = szMD5Pwd; else m_szPwd = pwd;
        m_Request = loginRequest;
        m_Response = login;
        m_szRequestName = "LJ.XMLRPC.login";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_bLoggedIn = login.m_bSentOK;
        if (login.m_bSentOK && EntryFrame.m_bOnline) {
            encode("groups.xml", login.getGroups());
            encode("journal.xml", login.m_usejournals);
            encode("pickw.xml", login.m_pickws);
        }
        m_Request = null;
        m_Response = null;
        if (login.m_fastserver != null) m_bFastServer = (((Integer) login.m_fastserver).intValue() == 1); else m_bFastServer = false;
        return login;
    }

    public XMLgetfriends getFriends() {
        XMLgetfriends friends = new XMLgetfriends();
        XMLgetfriends.Request request = new XMLgetfriends.Request();
        request.m_includefriendof = new Integer(1);
        request.m_includebdays = new Integer(1);
        fillLoginValues(request);
        m_Request = request;
        m_Response = friends;
        m_szRequestName = "LJ.XMLRPC.getfriends";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        if (m_Response.m_bSentOK && EntryFrame.m_bOnline) encode("friends.xml", friends.getFriends());
        m_Request = null;
        m_Response = null;
        return friends;
    }

    public XMLeditfriendgroups editGroups(LJFriends friends, LJGroups groups) {
        XMLeditfriendgroups editfriendgroups = new XMLeditfriendgroups();
        XMLeditfriendgroups.Request request = new XMLeditfriendgroups.Request();
        Hashtable friendTable = new Hashtable();
        Hashtable setTable = new Hashtable();
        Vector delGroup = new Vector();
        int iSize = friends.size();
        for (int i = 0; i < iSize; i++) {
            LJFriends.LJFriend friend = (LJFriends.LJFriend) friends.get(i);
            if (friend.m_bUpdated) friendTable.put(friend.m_szUsername, Integer.toString(friend.m_iGroupMask.getIntValue()));
        }
        iSize = groups.size();
        for (int i = 0; i < iSize; i++) {
            LJGroups.LJGroup group = (LJGroups.LJGroup) groups.get(i);
            if ((group.m_bNew) || (group.m_bUpdated)) {
                XMLeditfriendgroups.Request.XMLSet set = new XMLeditfriendgroups.Request.XMLSet();
                set.m_public = new Integer(group.m_bPublic ? 1 : 0);
                set.m_name = group.m_szName;
                setTable.put(Integer.toString(group.m_iBit), set);
            }
            if (group.m_bDeleted) delGroup.add(new Integer(group.m_iBit));
        }
        if (friendTable.size() > 0) request.m_groupmasks = friendTable;
        if (setTable.size() > 0) request.m_set = setTable;
        if (delGroup.size() > 0) request.m_delete = delGroup;
        fillLoginValues(request);
        m_Request = request;
        m_Response = editfriendgroups;
        m_szRequestName = "LJ.XMLRPC.editfriendgroups";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return editfriendgroups;
    }

    public XMLeditfriends addNewFriend(String szname) {
        XMLeditfriends.Request.ReqAdd[] addUsers = new XMLeditfriends.Request.ReqAdd[1];
        addUsers[0] = new XMLeditfriends.Request.ReqAdd();
        addUsers[0].m_username = szname;
        return editfriends(null, addUsers);
    }

    public XMLeditfriends editfriends(LJFriends friends) {
        Vector delUsers = new Vector();
        Vector addUsers = new Vector();
        int iSize = friends.size();
        for (int i = 0; i < iSize; i++) {
            LJFriends.LJFriend friend = (LJFriends.LJFriend) friends.get(i);
            if (friend.m_bUpdated) {
                if (!friend.m_iGroupMask.get(0)) delUsers.add(friend.m_szUsername); else {
                    XMLeditfriends.Request.ReqAdd update = new XMLeditfriends.Request.ReqAdd();
                    update.m_username = friend.m_szUsername;
                    String szColor = "#000000";
                    String szTemp = Integer.toHexString(friend.m_bgColor.getRGB() & 0xFFFFFF);
                    update.m_bgcolor = szColor.substring(0, szColor.length() - szTemp.length()) + szTemp;
                    szTemp = Integer.toHexString(friend.m_fgColor.getRGB() & 0xFFFFFF);
                    update.m_fgcolor = szColor.substring(0, szColor.length() - szTemp.length()) + szTemp;
                    update.m_groupmask = new Integer(friend.m_iGroupMask.getIntValue() & 0xFFFFFF);
                    addUsers.add(update);
                }
            }
        }
        String[] delUsersList = null;
        XMLeditfriends.Request.ReqAdd[] addUsersList = null;
        if (delUsers.size() > 0) {
            delUsersList = new String[delUsers.size()];
            delUsers.toArray(delUsersList);
        }
        if (addUsers.size() > 0) {
            addUsersList = new XMLeditfriends.Request.ReqAdd[addUsers.size()];
            addUsers.toArray(addUsersList);
        }
        return editfriends(delUsersList, addUsersList);
    }

    protected XMLeditfriends editfriends(String[] delUsers, XMLeditfriends.Request.ReqAdd[] addUsers) {
        XMLeditfriends editfriends = new XMLeditfriends();
        XMLeditfriends.Request request = new XMLeditfriends.Request();
        if (delUsers != null) request.m_delete = new java.util.Vector(java.util.Arrays.asList(delUsers));
        if (addUsers != null) request.m_add = new java.util.Vector(java.util.Arrays.asList(addUsers));
        fillLoginValues(request);
        m_Request = request;
        m_Response = editfriends;
        m_szRequestName = "LJ.XMLRPC.editfriends";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return editfriends;
    }

    private String XMLEncode(String szString) {
        String szResult = "";
        szResult = szString.replaceAll("&", "&amp;");
        szResult = szResult.replaceAll("\"", "&quot;");
        szResult = szResult.replaceAll("'", "&apos;");
        szResult = szResult.replaceAll("<", "&lt;");
        szResult = szResult.replaceAll(">", "&gt;");
        return szResult;
    }

    private java.nio.ByteBuffer XMLbase64Enc(String szText) {
        try {
            byte[] bytes = szText.getBytes("UTF-8");
            return java.nio.ByteBuffer.wrap(bytes);
        } catch (java.io.UnsupportedEncodingException e) {
            System.err.println(e);
        }
        return null;
    }

    private XMLpostEvent.editRequest getRequest(PostEventInfo event) {
        XMLpostEvent.editRequest request = new XMLpostEvent.editRequest();
        request.m_event = XMLEncode(event.m_szEvent);
        request.m_lineendings = "unix";
        if (event.m_szSubject != null) request.m_subject = XMLEncode(event.m_szSubject);
        request.m_security = PostEventInfo.SEC_STRING[event.m_iSecurity];
        request.m_allowmask = null;
        if (event.m_iSecurity == PostEventInfo.SEC_MASK) request.m_allowmask = new Integer(event.m_iSec_Mask);
        Hashtable props = new Hashtable();
        if (event.m_szMusic != null) props.put("current_music", XMLbase64Enc(event.m_szMusic));
        if ((event.m_iMoodID == -1) && (event.m_szMood != null)) props.put("current_mood", XMLbase64Enc(event.m_szMood)); else if (event.m_iMoodID > -1) props.put("current_moodid", new Integer(event.m_iMoodID));
        props.put("opt_nocomments", new Boolean(event.m_bNoComment));
        props.put("opt_preformatted", new Boolean(event.m_bFormated));
        if (event.m_szPickKW != null) props.put("picture_keyword", event.m_szPickKW);
        props.put("opt_backdated", new Boolean(event.m_bBackdate));
        props.put("opt_noemail", new Boolean(event.m_bNoEmail));
        request.m_props = props;
        request.m_usejournal = event.m_szUseJournal;
        java.util.Calendar date = java.util.Calendar.getInstance();
        if (event.m_Date != null) date.setTime(event.m_Date);
        request.m_year = new Integer(date.get(date.YEAR));
        request.m_mon = new Integer(date.get(date.MONTH) + 1);
        request.m_day = new Integer(date.get(date.DAY_OF_MONTH));
        request.m_hour = new Integer(date.get(date.HOUR_OF_DAY));
        request.m_min = new Integer(date.get(date.MINUTE));
        return request;
    }

    public XMLpostEvent editEvent(OldEventInfo event) {
        XMLpostEvent post = new XMLpostEvent();
        XMLpostEvent.editRequest request = getRequest(event);
        request.m_itemid = new Integer(event.m_iItemID);
        fillLoginValues(request);
        m_Request = request;
        m_Response = post;
        m_szRequestName = "LJ.XMLRPC.editevent";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return post;
    }

    public XMLpostEvent postEvent(PostEventInfo event) {
        XMLpostEvent post = new XMLpostEvent();
        XMLpostEvent.Request request = getRequest(event);
        fillLoginValues(request);
        m_Request = request;
        m_Response = post;
        m_szRequestName = "LJ.XMLRPC.postevent";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return post;
    }

    public XMLgetdaycounts getDayCounts(String szJournal) {
        XMLgetdaycounts days = new XMLgetdaycounts();
        XMLgetdaycounts.Request request = new XMLgetdaycounts.Request();
        request.m_usejournal = szJournal;
        fillLoginValues(request);
        m_Request = request;
        m_Response = days;
        m_szRequestName = "LJ.XMLRPC.getdaycounts";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return days;
    }

    public XMLgetEvents XMLdnldEntries(String szLastSync, String szJournal) {
        return getEvents(-1, false, false, "syncitems", szLastSync, -1, -1, -1, -1, null, -1, null);
    }

    public XMLgetEvents getDayPosts(int iDay, int iMonth, int iYear, String szUseJournal) {
        return getEvents(-1, false, false, "day", null, iYear, iMonth, iDay, -1, null, -1, szUseJournal);
    }

    public XMLgetEvents getEvents(int iTruncate, boolean bSubject, boolean bnoprops, String selecttype, String lastSync, int year, int month, int day, int iHowMany, Date beforeDate, int iItemID, String szUseJournal) {
        XMLgetEvents event = new XMLgetEvents();
        XMLgetEvents.Request request = new XMLgetEvents.Request();
        if (iTruncate > -1) request.m_truncate = new Integer(iTruncate);
        request.m_prefersubject = new Integer(bSubject ? 1 : 0);
        request.m_noprops = new Integer(bnoprops ? 1 : 0);
        request.m_selecttype = selecttype;
        request.m_lastsync = lastSync;
        if (year > -1) request.m_year = new Integer(year);
        if (month > -1) request.m_month = new Integer(month);
        if (day > -1) request.m_day = new Integer(day);
        if (iHowMany > -1) request.m_howmany = new Integer(iHowMany);
        if (beforeDate != null) {
            java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            request.m_beforedate = df.format(beforeDate);
        }
        if (iItemID > -1) request.m_itemid = new Integer(iItemID);
        request.m_usejournal = szUseJournal;
        fillLoginValues(request);
        m_Request = request;
        m_Response = event;
        m_szRequestName = "LJ.XMLRPC.getevents";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return event;
    }

    public XMLsyncItems XMLsyncItems(String lastSync) {
        XMLsyncItems items = new XMLsyncItems();
        XMLsyncItems.Request request = new XMLsyncItems.Request();
        request.m_lastsync = lastSync;
        fillLoginValues(request);
        m_Request = request;
        m_Response = items;
        m_szRequestName = "LJ.XMLRPC.syncitems";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return items;
    }

    public XMLcheckfriends checkFriends(Object lastUpdate, int iMask) {
        XMLcheckfriends check = new XMLcheckfriends();
        XMLcheckfriends.Request request = new XMLcheckfriends.Request();
        request.m_lastupdate = lastUpdate;
        if (iMask > -1) request.m_mask = new Integer(iMask);
        fillLoginValues(request);
        m_Request = request;
        m_Response = check;
        m_szRequestName = "LJ.XMLRPC.checkfriends";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return check;
    }

    public void run() {
        if (m_Response == null) return;
        if (m_szRequestName == null) return;
        if (m_ProgDlg != null) m_ProgDlg.update(SEND_REQ_STEP, InstallInfo.getString("progress.step.send"));
        if (!SendXMLRequest(m_szRequestName, m_Request)) {
            if (m_ProgDlg != null) {
                m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                m_ProgDlg.setVisible(false);
                m_ProgDlg.dispose();
            }
            return;
        }
        try {
            if (m_Conn.getResponseMessage().compareToIgnoreCase("OK") == 0) {
                int i = 1;
                String szField;
                int len = 0;
                while ((szField = m_Conn.getHeaderFieldKey(i)) != null) {
                    if (szField.compareToIgnoreCase("Content-length") == 0) len = new Integer(m_Conn.getHeaderField(i)).intValue();
                    i++;
                }
                if (m_ProgDlg != null) m_ProgDlg.update(PROCESS_RESP_STEP, InstallInfo.getString("progress.step.process"));
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                XMLRPCHandler handler = new XMLRPCHandler(m_Response);
                ProgressBarInputStream strm = new ProgressBarInputStream(new BufferedInputStream(m_Conn.getInputStream()));
                if (m_ProgDlg != null) m_ProgDlg.setByteProgressBar(strm.getProgressBar());
                strm.getProgressBar().setMaximum(len);
                System.out.print("Response Received: ");
                handler.setLen(len);
                saxParser.parse(strm, handler);
                System.out.println();
                if (!m_Response.m_bSentOK) {
                    if (m_Response.m_faultstring != null) javax.swing.JOptionPane.showMessageDialog(m_Parent, m_Response.m_faultstring, InstallInfo.getString("app.title"), javax.swing.JOptionPane.INFORMATION_MESSAGE); else javax.swing.JOptionPane.showMessageDialog(m_Parent, InstallInfo.getString("string.server.error"), InstallInfo.getString("app.title"), javax.swing.JOptionPane.ERROR_MESSAGE);
                }
                if (m_ProgDlg != null) {
                    m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                    m_ProgDlg.setVisible(false);
                    m_ProgDlg.dispose();
                }
            } else {
            }
        } catch (Exception e2) {
            System.err.println(e2);
            if (m_ProgDlg != null) {
                m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                m_ProgDlg.setVisible(false);
                m_ProgDlg.dispose();
            }
        }
    }

    public String getUserName() {
        return m_szUname;
    }

    public static ProxySettings getProxySettings() {
        ProxySettings prox = new ProxySettings();
        prox.m_iPort = m_iProxyPort;
        prox.m_szProxyAddress = m_szProxyAddr;
        prox.m_bEnabled = m_bProxyOn;
        return prox;
    }

    public static void setProxySettings(ProxySettings prox) {
        m_iProxyPort = prox.m_iPort;
        m_szProxyAddr = prox.m_szProxyAddress;
        m_bProxyOn = prox.m_bEnabled;
    }

    public static String cleanHTML(String szText) {
        String szResult;
        if (szText == null) return null;
        szResult = szText;
        Pattern p = Pattern.compile("\n<LI([^>]*)>", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(szResult);
        while (m.find()) {
            String szReplace = "";
            if (m.groupCount() > 0) szReplace += m.group(1);
            szResult = m.replaceFirst("<LI" + szReplace + ">");
            m = p.matcher(szResult);
        }
        p = Pattern.compile("\n<([U|O]L)([^>]*)>", Pattern.CASE_INSENSITIVE);
        m = p.matcher(szResult);
        while (m.find()) {
            String szReplace = m.group(1);
            if (m.groupCount() > 1) szReplace += m.group(2);
            szResult = m.replaceFirst("<" + szReplace + ">");
            m = p.matcher(szResult);
        }
        p = Pattern.compile("\n+</([U|O]L)>\n*", Pattern.CASE_INSENSITIVE);
        m = p.matcher(szResult);
        while (m.find()) {
            szResult = m.replaceFirst("</" + m.group(1) + ">");
            m = p.matcher(szResult);
        }
        return szResult;
    }

    public XMLsessiongen genSession(String expire, boolean ipFixed) {
        XMLsessiongen session = new XMLsessiongen();
        XMLsessiongen.Request request = new XMLsessiongen.Request();
        request.m_ipfixed = new Integer(ipFixed ? 1 : 0);
        request.m_expiration = expire;
        fillLoginValues(request);
        m_Request = request;
        m_Response = session;
        m_szRequestName = "LJ.XMLRPC.sessiongenerate";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return session;
    }

    public XMLsessionexp Sessionexpire(String[] cookies, boolean bAll) {
        XMLsessionexp session = new XMLsessionexp();
        XMLsessionexp.Request request = new XMLsessionexp.Request();
        if (cookies != null) {
            request.m_expire = new java.util.Vector();
            request.m_expire.addAll(java.util.Arrays.asList(cookies));
        }
        request.m_expireall = new Integer(bAll ? 1 : 0);
        fillLoginValues(request);
        m_Request = request;
        m_Response = session;
        m_szRequestName = "LJ.XMLRPC.sessionexpire";
        if (m_ProgDlg != null) {
            java.lang.Thread td = new java.lang.Thread(this);
            m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
            m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
            m_ProgDlg.show();
        } else {
            run();
        }
        m_Request = null;
        m_Response = null;
        return session;
    }

    private class commentThread implements java.lang.Runnable {

        XMLcommentHandler m_Handler = null;

        URL m_URL = null;

        String m_szCookie = null;

        public commentThread(URL LJUrl, XMLcommentHandler handler, String szCookie) {
            m_URL = LJUrl;
            m_Handler = handler;
            m_szCookie = szCookie;
        }

        public void run() {
            if (m_ProgDlg != null) m_ProgDlg.update(SEND_REQ_STEP, InstallInfo.getString("progress.step.send"));
            try {
                HttpURLConnection Conn = (HttpURLConnection) m_URL.openConnection();
                Conn.setRequestMethod("GET");
                Conn.setRequestProperty("X-LJ-Auth", "cookie");
                Conn.setRequestProperty("Cookie", "ljsession=" + m_szCookie);
                Conn.setAllowUserInteraction(true);
                Conn.setDoInput(true);
                Conn.setUseCaches(false);
                Conn.setInstanceFollowRedirects(true);
                if (Conn.getResponseMessage().compareToIgnoreCase("OK") == 0) {
                    int i = 1;
                    String szField;
                    int len = 0;
                    while ((szField = Conn.getHeaderFieldKey(i)) != null) {
                        if (szField.compareToIgnoreCase("Content-length") == 0) len = new Integer(Conn.getHeaderField(i)).intValue();
                        i++;
                    }
                    if (m_ProgDlg != null) m_ProgDlg.update(PROCESS_RESP_STEP, InstallInfo.getString("progress.step.process"));
                    ProgressBarInputStream strm = new ProgressBarInputStream(new BufferedInputStream(Conn.getInputStream()));
                    if (m_ProgDlg != null) m_ProgDlg.setByteProgressBar(strm.getProgressBar());
                    strm.getProgressBar().setMaximum(len);
                    SAXParserFactory factory = SAXParserFactory.newInstance();
                    SAXParser saxParser = factory.newSAXParser();
                    saxParser.parse(strm, m_Handler);
                    if (m_ProgDlg != null) {
                        m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                        m_ProgDlg.setVisible(false);
                        m_ProgDlg.dispose();
                    }
                } else {
                }
            } catch (Exception e) {
                System.err.println(e);
                if (m_ProgDlg != null) {
                    m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                    m_ProgDlg.setVisible(false);
                    m_ProgDlg.dispose();
                }
            }
        }
    }

    public XMLcomments getComments(int iStartID, String szCookie, XMLcomments comments, String type) {
        XMLcomments commentList;
        if (comments == null) commentList = new XMLcomments(); else commentList = comments;
        try {
            URL LJUrl = null;
            if (m_bProxyOn) LJUrl = new URL("http", m_szProxyAddr, m_iProxyPort, "http://www.livejournal.com/export_comments.bml?get=comment_" + type + "&startid=" + iStartID); else LJUrl = new URL("http", "www.livejournal.com", 80, "/export_comments.bml?get=comment_" + type + "&startid=" + iStartID);
            XMLcommentHandler handler = new XMLcommentHandler(commentList);
            commentThread thread = new commentThread(LJUrl, handler, szCookie);
            if (m_ProgDlg != null) {
                m_ProgDlg = new ProtProgress((java.awt.Frame) m_ProgDlg.getParent(), m_ProgDlg.isModal());
                java.lang.Thread td = new java.lang.Thread(thread);
                m_ProgDlg.reset(0, DONE_STEP, InstallInfo.getString("progress.step.negotiate"), InstallInfo.getString("progress.title"), td);
                m_ProgDlg.show();
            } else {
                thread.run();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            if (m_ProgDlg != null) {
                m_ProgDlg.update(DONE_STEP, InstallInfo.getString("progress.step.done"));
                m_ProgDlg.setVisible(false);
                m_ProgDlg.dispose();
            }
        }
        return commentList;
    }
}
