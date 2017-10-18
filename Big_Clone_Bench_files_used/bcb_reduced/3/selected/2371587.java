package com.frostwire;

import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.File;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.Serializable;
import javax.swing.JOptionPane;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.util.CommonUtils;
import com.frostwire.BuyAction;
import java.security.MessageDigest;
import com.limegroup.gnutella.security.SHA1;

/**
 * Reads an update.xml file from frostwire.com
 * The update xml file can also come with special announcements for the community.
 * 
 * The correct syntax of the update.xml file should be as follows:
 * <update time="${server_time_here}" version="${update_manager_version}" buyUrl="${proxy_buy_url}">
 * <message type="{update | announcement}"
 *          value="<text message goes here>"
 *          [version="${version_string}"] //version if mandatory for message type = 'update'
 *          [url="${url}"] //version is mandatory for message type = 'update'. Optional for announcements
 *          [expires="${server_timestamp}] //mandatory only for message type = announcement, otherwise the announcement will be shown forever.
 *          [torrent=${torrent_url}] //optional, the message suggests a torrent should be downloaded
 *          [os=${os_string}] //optional, filters message per os, valid os_strings are 'windows','linux','mac'

 *          />
 * <![CDATA[
 * Put all the text you want here, with tags, it doesnt matter cause its CDATA.
 * Could be HTML if you want in theory. Just dont put a ]]> in it.
 * ]]>
 * <!-- there can be many messages -->
 * </message>
 * </update>
 * @author gubatron
 *
 * 
 *
 */
public final class UpdateManager implements Serializable {

    private static final int OPTION_OPEN_URL = 1;

    private static final int OPTION_LATER = 0;

    private static final int OPTION_DOWNLOAD_TORRENT = 2;

    private static transient HashSet<UpdateMessage> _seenMessages;

    transient Timer _timer = null;

    transient UpdateMessage _updateMessage = null;

    transient HashSet<UpdateMessage> _announcements = null;

    /**
	 * Starts an Update Task in <secondsAfter> seconds after.
	 *
	 */
    public static void scheduleUpdateCheckTask(int secondsAfter) {
        TimerTask checkForUpdatesTask = new TimerTask() {

            public void run() {
                UpdateManager um = UpdateManager.getInstance();
                um.checkForUpdates();
                um.terminateUpdateCheckTask();
            }
        };
        UpdateManager.getInstance().getTimer().schedule(checkForUpdatesTask, secondsAfter * 1000);
    }

    public Timer getTimer() {
        if (_timer == null) {
            _timer = new Timer();
        }
        return _timer;
    }

    public void terminateUpdateCheckTask() {
        _timer.cancel();
    }

    /** The singleton instance */
    private static UpdateManager INSTANCE = null;

    private Date _serverTime = null;

    private UpdateManager() {
    }

    public void setServerTime(String serverTime) {
        _serverTime = null;
        try {
            _serverTime = new Date(Long.parseLong(serverTime));
        } catch (Exception e) {
            System.out.println("Warning: UpdateManager.setServerTime(): Could not set time from server, using local time");
        }
        if (_serverTime == null) _serverTime = Calendar.getInstance().getTime();
    }

    public Date getServerTime() {
        return _serverTime;
    }

    /**
	 * Checks for updates, and shows message dialogs if needed.
	 */
    public void checkForUpdates() {
        UpdateMessageReader umr = new UpdateMessageReader();
        umr.readUpdateFile();
        if (umr.hasUpdateMessage() && umr.getUpdateMessage().getVersion() != null && !umr.getUpdateMessage().getVersion().trim().equals("") && UpdateManager.isFrostWireOld(umr.getUpdateMessage().getVersion())) {
            showUpdateMessage(umr.getUpdateMessage());
        }
        if (umr.hasAnnouncements()) {
            attemptShowAnnouncements(umr.getAnnouncements());
        }
    }

    /**
	 * Given an update message, it checks the frostwire version on it,
	 * if we have a lower version, then we show the message.
	 * @param msg
	 */
    public void showUpdateMessage(UpdateMessage msg) {
        String title = (msg.getMessageType().equals("update")) ? "New FrostWire Update Available" : "FrostWire Team Announcement";
        int optionType = JOptionPane.CANCEL_OPTION;
        if (msg.getUrl() != null && !msg.getUrl().trim().equals("")) {
            System.out.println("\t" + msg.getUrl());
            optionType = optionType | JOptionPane.OK_OPTION;
        }
        String[] options = new String[3];
        if (msg.getTorrent() != null) {
            options[OPTION_DOWNLOAD_TORRENT] = new String("Download Torrent");
        } else {
            options = new String[2];
        }
        options[OPTION_LATER] = new String("Thanks, but not now");
        options[OPTION_OPEN_URL] = new String("Go to webpage");
        int result = JOptionPane.showOptionDialog(null, msg.getMessage(), title, optionType, JOptionPane.INFORMATION_MESSAGE, null, options, null);
        if (result == OPTION_OPEN_URL) {
            GUIMediator.openURL(msg.getUrl());
        } else if (result == OPTION_DOWNLOAD_TORRENT) {
            openTorrent(msg.getTorrent());
        }
    }

    /**
	 * Given announcements it will show them.
	 * @param announcements
	 */
    public void attemptShowAnnouncements(HashSet<UpdateMessage> announcements) {
        System.out.println("ABOUT TO SHOW SOME ANNOUNCEMENTS");
        java.util.Iterator<UpdateMessage> it = announcements.iterator();
        while (it.hasNext()) {
            UpdateMessage msg = it.next();
            if (msg.isShownOnce() && haveShownMessageBefore(msg)) {
                continue;
            }
            if (msg.getUrl() != null && !msg.getUrl().trim().equals("")) {
                showUpdateMessage(msg);
            }
        }
    }

    private void loadSeenMessages() {
        File f = new File("seenMessages.dat");
        _seenMessages = new HashSet<UpdateMessage>();
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                System.out.println("UpdateManager.loadSeenMessages() - Cannot create file to deserialize");
            }
            return;
        }
        if (f.length() == 0) return;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            _seenMessages = (HashSet<UpdateMessage>) ois.readObject();
            ois.close();
        } catch (Exception e) {
            System.out.println("UpdateManager.loadSeenMessages() - Cannot deserialize - ");
            System.out.println(e);
        }
    }

    private void saveSeenMessages() {
        if (_seenMessages == null || _seenMessages.size() < 1) return;
        File f = new File("seenMessages.dat");
        if (!f.exists()) {
            try {
                f.createNewFile();
            } catch (Exception e) {
                System.out.println("UpdateManager.saveSeenMessages() cannot create file to serialize seen messages");
            }
        }
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject((HashSet<UpdateMessage>) _seenMessages);
            oos.close();
        } catch (Exception e) {
            System.out.println("UpdateManager.saveSeenMessages() - Cannot serialize.");
            e.printStackTrace();
        }
    }

    /**
	 * Checks on a Message map, if we've seen this message before.
	 * The message map is serialized on disk everytime we write to it.
	 * Its initialized from disk when we start the Update Manager.
	 */
    public boolean haveShownMessageBefore(UpdateMessage msg) {
        if (!msg.isShownOnce()) return false;
        loadSeenMessages();
        if (_seenMessages == null || _seenMessages.size() == 0 || !_seenMessages.contains(msg)) {
            if (_seenMessages == null) _seenMessages = new HashSet<UpdateMessage>();
            _seenMessages.add(msg);
            saveSeenMessages();
            return false;
        }
        return true;
    }

    /**
	 * Given a version string, it compares against the current frostwire version.
	 * If frostwire is old, it will return true.
	 * 
	 * A valid version string looks like this:
	 * "MAJOR.RELEASE.SERVICE"
	 * 
	 * 4.13.1
	 * 4.13.2
	 * ...
	 * 4.13.134

	 * It will compare each number of the current version to the version published by the
	 * update message.
	 * @param messageVersion
	 */
    public static boolean isFrostWireOld(String messageVersion) {
        String currentVersion = CommonUtils.getFrostWireVersion();
        if (currentVersion.equals(messageVersion)) {
            return false;
        }
        try {
            String[] fwVersionParts = currentVersion.split("\\.");
            int fw_major = Integer.parseInt(fwVersionParts[0]);
            int fw_release = Integer.parseInt(fwVersionParts[1]);
            int fw_service = Integer.parseInt(fwVersionParts[2]);
            String[] msgVersionParts = messageVersion.split("\\.");
            int msg_major = Integer.parseInt(msgVersionParts[0]);
            int msg_release = Integer.parseInt(msgVersionParts[1]);
            int msg_service = Integer.parseInt(msgVersionParts[2]);
            if (fw_major < msg_major) {
                return true;
            }
            if (fw_major == msg_major && fw_release < msg_release) {
                return true;
            }
            if (fw_major == msg_major && fw_release == msg_release && fw_service < msg_service) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    public static synchronized UpdateManager getInstance() {
        if (UpdateManager.INSTANCE == null) {
            UpdateManager.INSTANCE = new UpdateManager();
        }
        return UpdateManager.INSTANCE;
    }

    /**
	 * Starts a torrent download
	 * @param uriStr
	 */
    public static void openTorrent(String uriStr) {
        try {
            URI uri = new URI(uriStr);
            String scheme = uri.getScheme();
            if (scheme == null || !scheme.equalsIgnoreCase("http")) {
                return;
            }
            String authority = uri.getAuthority();
            if (authority == null || authority.equals("") || authority.indexOf(' ') != -1) {
                return;
            }
            GUIMediator.instance().openTorrentURI(uri);
        } catch (URIException e) {
            System.out.println(e);
        }
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    }

    private void writeObject(ObjectOutputStream oos) throws IOException {
    }

    /** POJO to represent an UpdateMessage. */
    private class UpdateMessage extends Object implements Serializable {

        private int _hashCode = -1;

        public static final long serialVersionUID = 44L;

        private String _message;

        private String _url;

        private String _messageType;

        private String _version = null;

        private Date _expiration = null;

        private String _torrent = null;

        private String _os = null;

        private String _showOnce = "false";

        public String getMessage() {
            return _message;
        }

        public void setMessage(String m) {
            _message = m;
        }

        public String getUrl() {
            return _url;
        }

        public void setUrl(String u) {
            _url = u;
        }

        public String getMessageType() {
            return _messageType;
        }

        public String getOs() {
            return _os;
        }

        /**
		 * If it receives a valid os string ("windows", "mac", "linux")
		 * it will set it.
		 * If it receives null or *, it will set _os to null.
		 * Having getOS() return null, means this message is for every OS instance.
		 * @param os
		 */
        public void setOs(String os) {
            _os = null;
            if (os != null) {
                os = os.trim();
                if (os.equalsIgnoreCase("windows") || os.equalsIgnoreCase("linux") || os.equalsIgnoreCase("mac")) {
                    _os = os.toLowerCase();
                } else if (os.equals("*")) {
                    _os = null;
                }
            }
        }

        public String getTorrent() {
            return _torrent;
        }

        public void setTorrent(String t) {
            _torrent = t;
        }

        public void setMessageType(String mt) {
            if (mt == null || !(mt.equalsIgnoreCase("update") || mt.equalsIgnoreCase("announcement"))) {
                _messageType = new String("update");
                return;
            }
            _messageType = mt.toLowerCase();
        }

        public Date getExpiration() {
            return _expiration;
        }

        public void setExpiration(Date exp) {
            _expiration = exp;
        }

        /** Sets the expiration date out of a string with the timestamp 
		 * Pass null, and it means this message has no expiration date.
		 * */
        public void setExpiration(String expTimestamp) {
            if (expTimestamp == null || expTimestamp.equals("0")) {
                _expiration = null;
                return;
            }
            try {
                _expiration = new Date(Long.parseLong(expTimestamp));
            } catch (NumberFormatException e) {
                System.out.println("Expiration passed cannot be converted to a long");
                _expiration = null;
            }
        }

        public boolean hasExpired() {
            if (getExpiration() == null) return false;
            long serverTimestamp = UpdateManager.getInstance().getServerTime().getTime();
            long myTimestamp = _expiration.getTime();
            return myTimestamp < serverTimestamp;
        }

        public String getVersion() {
            if (_version != null && _version.equals("")) _version = null;
            return _version;
        }

        public void setVersion(String v) {
            _version = v;
        }

        public boolean isShownOnce() {
            return _showOnce.equalsIgnoreCase("true");
        }

        public void setShowOnce(String s) {
            if (s != null) _showOnce = s;
        }

        public UpdateMessage(String msgType, String message) {
            setMessageType(msgType);
            setMessage(message);
        }

        public boolean equals(Object obj) {
            return obj.hashCode() == this.hashCode();
        }

        public int hashCode() {
            if (_hashCode <= 0) {
                MessageDigest md = new SHA1();
                String byteString = _message + _url + _messageType + _version + _torrent + _os + _showOnce;
                md.update(byteString.getBytes());
                byte[] digest = md.digest();
                _hashCode = 0;
                for (int n : digest) {
                    _hashCode += Math.abs((int) n);
                }
            }
            return _hashCode;
        }

        private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
            ois.defaultReadObject();
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            oos.defaultWriteObject();
        }

        public String toString() {
            String s = new String();
            s = "\n";
            s += "UpdateMessage @" + String.valueOf(super.hashCode());
            s += "{";
            s += "_hashCode : " + String.valueOf(hashCode()) + ", \n";
            s += "_message : " + getMessage() + ", \n";
            s += "_url : " + getUrl() + ", \n";
            s += "_messageType : " + getMessageType() + ", \n";
            s += "_version : " + getVersion() + ", \n";
            s += "_expiration : " + String.valueOf(getExpiration()) + ", \n";
            s += "_torrent : " + getTorrent() + ", \n";
            s += "_os : " + getOs() + ", \n";
            s += "_showOnce : " + isShownOnce() + ", \n";
            s += "}\n";
            return s;
        }
    }

    private class UpdateMessageReader implements ContentHandler {

        public void UpdateMessageReader() {
        }

        public UpdateMessage _bufferMessage = null;

        public UpdateMessage _updateMessage = null;

        public HashSet<UpdateMessage> _announcements = null;

        public boolean hasUpdateMessage() {
            return _updateMessage != null;
        }

        public boolean hasAnnouncements() {
            return _announcements != null && _announcements.size() > 0;
        }

        /** Sets only the first update message it finds.
		 *  Make sure to put only have a single update message everytime on the server,
		 *  If you plan to leave old messages there, keep the newest one at the beginning of the file.
		 * @param msg
		 */
        public void setUpdateMessage(UpdateMessage msg) {
            if (_updateMessage == null && msg != null && msg.getMessageType().equals("update")) {
                _updateMessage = msg;
            }
        }

        public UpdateMessage getUpdateMessage() {
            return _updateMessage;
        }

        /**
		 * Only ads Announcements that have not expired
		 * @param msg
		 */
        public void addAnnouncement(UpdateMessage msg) {
            if (_announcements == null) {
                _announcements = new HashSet<UpdateMessage>();
            }
            if (msg.getMessageType().equals("announcement") && !msg.hasExpired()) {
                _announcements.add(msg);
            }
        }

        public HashSet<UpdateMessage> getAnnouncements() {
            return _announcements;
        }

        /**
		 * The XML Parser is created here, and this class is passed
		 * as the content handler implementation.
		 */
        public void readUpdateFile() {
            HttpURLConnection connection = null;
            InputSource src = null;
            try {
                connection = (HttpURLConnection) (new URL("http://update.frostwire.com/")).openConnection();
                String userAgent = "FrostWire/" + CommonUtils.getOS() + "/" + CommonUtils.getFrostWireVersion();
                connection.setRequestProperty("User-Agent", userAgent);
                src = new InputSource(connection.getInputStream());
                XMLReader rdr = XMLReaderFactory.createXMLReader("com.sun.org.apache.xerces.internal.parsers.SAXParser");
                rdr.setContentHandler(this);
                rdr.parse(src);
                connection.getInputStream().close();
                connection.disconnect();
            } catch (IOException e) {
                System.out.println("UpdateMessageReader.readUpdateFile() exception " + e.toString());
            } catch (SAXException e2) {
                System.out.println("UpdateMessageReader.readUpdateFile() exception " + e2.toString());
            }
        }

        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            if (localName.equalsIgnoreCase("update")) {
                UpdateManager.getInstance().setServerTime(atts.getValue("time"));
                if (atts.getValue("buyUrl") != null) {
                    BuyAction.setRedirectUrl(atts.getValue("buyUrl"));
                }
            } else if (localName.equalsIgnoreCase("message")) {
                String type = atts.getValue("type");
                String message = atts.getValue("value");
                String url = atts.getValue("url");
                String torrent = atts.getValue("torrent");
                String os = atts.getValue("os");
                String showOnce = atts.getValue("showOnce");
                String version = atts.getValue("version");
                _bufferMessage = new UpdateMessage(type, message);
                _bufferMessage.setUrl(url);
                _bufferMessage.setTorrent(torrent);
                _bufferMessage.setOs(os);
                _bufferMessage.setShowOnce(showOnce);
                _bufferMessage.setVersion(version);
                if (_bufferMessage.getMessageType().equals("announcement")) {
                    _bufferMessage.setExpiration(atts.getValue("expiration"));
                }
            }
        }

        public void characters(char[] ch, int start, int length) throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        /**
		 * Tells me if I'm supposed to keep the given update message.
		 * Compares the message's OS string against the current operating
		 * system.
		 * 
		 * If the message is an announcement, it cares about the version number
		 * not being outdated.
		 * 
		 * @param msg
		 * @return
		 */
        private boolean isMessageForMe(UpdateMessage msg) {
            if (msg.getOs() == null) {
                return true;
            }
            boolean im_mac_msg_for_me = msg.getOs().equals("mac") && CommonUtils.isMacOSX();
            boolean im_windows_msg_for_me = msg.getOs().equals("windows") && (CommonUtils.isWindows() || CommonUtils.isWindows2000orXP() || CommonUtils.isWindowsNTor2000orXP() || CommonUtils.isWindowsXP());
            boolean im_linux_msg_for_me = msg.getOs().equals("linux") && CommonUtils.isLinux();
            boolean i_have_eligible_os = im_mac_msg_for_me || im_windows_msg_for_me || im_linux_msg_for_me;
            if (msg.getVersion() != null && msg.getMessageType() == "announcement") {
                return i_have_eligible_os && !UpdateManager.isFrostWireOld(msg.getVersion());
            }
            return i_have_eligible_os;
        }

        /**
		 * When the tag closes, if we have a _buffer message,
		 * we check what type it is and set it as the Update Message (if its for this client/os)
		 * or if its an announcement, we add it to the announcements collection.
		 * 
		 * This function will make use of 'isMessageForMe', to try to discard the message
		 * in case its addressed to another OS different than the one where this
		 * client is running on top of. 
		 * 
		 * If its an update message and no update message has been spotted
		 * so far, the UpdateReader sets it as the update message object available
		 * 
		 * If its an announcement we attempt adding it as another announcement.
		 * That method will only add it if the message has not expired.
		 * 
		 */
        public void endElement(String uri, String name, String qName) throws SAXException {
            if (_bufferMessage != null && !isMessageForMe(_bufferMessage)) {
                System.out.println("Discarding message - " + _bufferMessage);
                _bufferMessage = null;
                return;
            }
            if (_bufferMessage != null && name.equalsIgnoreCase("message")) {
                if (_bufferMessage.getMessageType().equals("update")) {
                    setUpdateMessage(_bufferMessage);
                } else if (_bufferMessage.getMessageType().equals("announcement")) {
                    addAnnouncement(_bufferMessage);
                }
                _bufferMessage = null;
            }
        }

        public void endPrefixMapping(String arg0) throws SAXException {
        }

        public void ignorableWhitespace(char[] arg0, int arg1, int arg2) throws SAXException {
        }

        public void processingInstruction(String arg0, String arg1) throws SAXException {
        }

        public void setDocumentLocator(Locator arg0) {
        }

        public void skippedEntity(String arg0) throws SAXException {
        }

        public void startDocument() throws SAXException {
        }

        public void startPrefixMapping(String arg0, String arg1) throws SAXException {
        }
    }

    /**
	 * You can compile and run this file by itself from standing on gui/
	 * javac -cp .:../core:../lib/jars/commons-httpclient.jar com/frostwire/UpdateManager.java 
	 * java -cp .:../core:../lib/jars/commons-httpclient.jar com/frostwire/UpdateManager
	 * @param args
	 */
    public static void main(String[] args) {
        System.out.println("Testing UpdateManager");
        UpdateManager.scheduleUpdateCheckTask(0);
    }
}
