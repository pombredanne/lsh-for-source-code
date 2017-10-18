package net.sf.mailsomething.mail.parsers;

import net.sf.mailsomething.mail.*;
import java.io.*;
import java.security.*;

/**
 * Right now some of the methods which invokes methods on the session is
 * synchronized. It will probably be better just to synchronize the methods in
 * the sessionclass. The PopController should implement a BasicMailChecker
 * Interface.
 * 
 * @author stig
 * @created October 3, 2001
 * @version 0.1
 * @date 16/7
 */
public class PopController extends SessionAdapter {

    public static final String OK = "+OK";

    public static final String ERR = "-ERR";

    private static char statdelimiter = ' ';

    private PopSession session = null;

    private UIDMarker[] uids = null;

    private boolean isLoggedIn = false;

    private int extensions = -1;

    private int loginDelay = -1;

    private MailAccount account;

    /**
	 * Constructor for the PopController object.
	 * 
	 * @param account
	 *            Description of Parameter
	 */
    public PopController(MailAccount account) {
        super(account.getServerTimeout() * 1000);
        this.account = account;
        session = new PopSession(account.getMaxMessageSize());
        setExtention(Extension.APOP);
    }

    /**
	 * Description of the Method
	 */
    public synchronized void deleteAll() {
        initSession();
        String stat = session.stat();
        int nrmails = parseStat(stat);
        for (int i = 0; i < nrmails; i++) {
            session.dele(i + 1);
        }
    }

    /**
	 * Method for deleting a message on server.
	 * 
	 * @param index
	 *            the index of the message at the server
	 * @return boolean true if deleted, otherwise false
	 */
    public synchronized boolean deleteMessage(int index) {
        initSession();
        try {
            String reply = session.write("DELE " + index);
            if (reply.indexOf(OK) != -1) {
                notifyListeners(new String[] { "DELE " + index, reply }, "Message with id " + index + " deleted " + " for account: " + account.getAccountName(), SessionEvent.DEBUG);
                return true;
            } else {
                notifyListeners(new String[] { "DELE " + index, reply }, "Message with id " + index + " wasnt deleted " + " for account: " + account.getAccountName(), SessionEvent.DEBUG);
                return false;
            }
        } catch (IOException f) {
            notifyIOException(new String[] { "DELE " + index });
            return false;
        }
    }

    /**
	 * Returns all uid�s at server. The uid and the index of the message at the
	 * server ARE NOT necessesarely the same, which is why I use the inner class
	 * UIDMarker, which contains the uid, and the index. The index changes, when
	 * messages gets deleted etc, the uid does not. Deleting of messages in this
	 * design relies on the uid of the message which is used to find the index,
	 * since the DELE command takes the index and not the uid.
	 * 
	 *  
	 */
    public synchronized UIDMarker[] searchUid() {
        if (uids != null) {
            return uids;
        }
        initSession();
        if (!isLoggedIn || !isConnected) return new UIDMarker[] {};
        try {
            String[] list = session.uidl(null);
            if (list[0].indexOf(OK) != -1) {
                notifyListeners(new String[] { "UIDL", list[0] }, "Getting list of messages from account: " + account.getAccountName(), SessionEvent.DEBUG);
                uids = parseUidl(list);
                return uids;
            } else {
                notifyListeners(new String[] { "UIDL", "+ERR" }, "Getting list of messages from account: " + account.getAccountName() + " resultet in server returning error message", SessionEvent.DEBUG);
                return null;
            }
        } catch (IOException f) {
            notifyIOException();
            return null;
        }
    }

    /**
	 * Method getSizeList.
	 * 
	 * @return int[]
	 */
    public synchronized int[] getSizeList() {
        try {
            String[] reply = session.LIST();
            if (reply[0].indexOf(OK) != -1) {
                int[] sizes = new int[reply.length - 2];
                for (int i = 0; i < sizes.length; i++) {
                    try {
                        sizes[i] = Integer.parseInt(reply[i + 1].substring(reply[i + 1].lastIndexOf(" ") + 1));
                    } catch (NumberFormatException f) {
                        f.printStackTrace();
                    }
                }
                return sizes;
            } else if (reply[0].indexOf(ERR) != -1) {
            }
            return null;
        } catch (IOException f) {
            notifyIOException(new String[] { "LIST " });
            return null;
        }
    }

    /**
	 * Method for getting the message size of the message with the argument
	 * index.
	 * 
	 * In case we are fetching a large list, use getSizeList() instead which
	 * basicly works much faster. A simple test on my own pop3 account showed
	 * that I cutted 20 seconds off from 1 minut to 40 seconds when fetching 270
	 * messages, by getting all sizes in one piece instead of one pr time.
	 * 
	 * 
	 * @param index
	 * @return int
	 */
    public synchronized int getMessageSize(int index) {
        initSession();
        try {
            String reply = session.write("LIST " + index);
            if (reply.indexOf(OK) != -1) {
                String size = reply.substring(reply.lastIndexOf(" ") + 1);
                int nsize = 0;
                try {
                    nsize = Integer.parseInt(size);
                } catch (NumberFormatException f) {
                    nsize = 0;
                }
                notifyListeners(new String[] { "LIST " + index, reply }, "Request of message size returned: " + nsize + " for account: " + account.getAccountName(), SessionEvent.DEBUG);
                return nsize;
            } else if (reply.indexOf(ERR) != -1) {
                notifyListeners(new String[] { "LIST " + index, reply }, "Error fetching message size" + " for account: " + account.getAccountName(), SessionEvent.DEBUG);
                return 0;
            }
            return 0;
        } catch (IOException f) {
            notifyIOException(new String[] { "LIST " + index });
            return 0;
        }
    }

    /**
	 * Returns the message which is at index, ie, this is NOT the same as the
	 * uid number of the message.
	 * 
	 *  
	 */
    public synchronized Message getMessage(int index) {
        initSession();
        String reply = session.retrMail(index);
        Message m = MailDecoder.decodeMail(session.retrBody());
        return m;
    }

    /**
	 * Description of the Method
	 * 
	 * @exception IOException
	 *                Description of Exception
	 */
    private synchronized void tryToConnect() {
        String banner = "";
        connect(session, account);
        if (!isConnected) return;
        notifyListeners(new String[] { banner }, "Connecting to account", SessionEvent.DEBUG);
        banner = getBanner();
        if ((getExtensions() & Extension.APOP) == Extension.APOP) {
            try {
                if (apopLogin(banner)) {
                    setExtention(Extension.APOP);
                    return;
                }
                removeExtension(Extension.APOP);
                plainLogin();
            } catch (IOException f) {
                SessionEvent e = new SessionEvent(null, "Timeout or socket-error " + "while connecting to popaccount: " + account.getAccountName(), SessionEvent.INFO);
                isLoggedIn = false;
                notifyListeners(e);
                return;
            }
        } else {
            try {
                plainLogin();
            } catch (IOException f) {
                SessionEvent e = new SessionEvent(null, "Timeout or socket-error " + "while connecting to popaccount: " + account.getAccountName(), SessionEvent.INFO);
                isLoggedIn = false;
                notifyListeners(e);
                return;
            }
        }
    }

    /**
	 * Method apopLogin.
	 * 
	 * @param banner
	 * @return boolean
	 * @throws IOException
	 */
    private boolean apopLogin(String banner) throws IOException {
        boolean isAPOP = false;
        int start = banner.indexOf("<");
        int end = banner.lastIndexOf(">");
        if ((end - start) <= 1) {
            return isAPOP;
        }
        String timestamp = banner.substring(start);
        if (timestamp == null || timestamp.length() == 0) return isAPOP;
        String md5Apop = getMD5Hash(timestamp + account.getPassword());
        String reply = session.APOP(account.getUserName() + " " + md5Apop);
        if (reply.indexOf("-ERR") != -1) {
            String reply2 = null;
            try {
                reply2 = session.noop();
            } catch (IOException ioe) {
            }
            if (reply2 == null) {
                reconnect();
                isLoggedIn = false;
                return isAPOP;
            }
            notifyListeners(new String[] { "APOP " + account.getUserName(), reply }, "Server replied with 'permission denied or not supported command' while " + "logging into popaccount: " + account.getAccountName(), SessionEvent.ERROR);
            isLoggedIn = false;
            return isAPOP;
        } else if (reply.indexOf("+OK") != -1) {
            notifyListeners(new String[] { "APOP " + account.getUserName(), reply }, "Successfully logged into account: " + account.getAccountName(), SessionEvent.DEBUG);
            isLoggedIn = true;
            isAPOP = true;
        }
        return isAPOP;
    }

    private String getMD5Hash(String timestamp) {
        try {
            byte[] i1 = timestamp.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(i1);
            byte[] digest = md5.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                String token = Integer.toHexString(0xFF & digest[i]);
                String zero = "";
                if (token.length() < 2) zero = "0";
                hexString.append(zero + token);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
            return null;
        }
    }

    private void plainLogin() throws IOException {
        String reply = session.USER(account.getUserName());
        if (reply.indexOf("-ERR") != -1) {
            notifyListeners(new String[] { "USER " + account.getUserName(), reply }, "Server replied with 'unknown username and/or password' while " + "logging into popaccount: " + account.getAccountName(), SessionEvent.ERROR);
            isLoggedIn = false;
            return;
        }
        String reply2 = session.PASS(account.getPassword());
        if (reply2.indexOf("-ERR") != -1) {
            notifyListeners(new String[] { "USER " + account.getUserName(), reply, "PASS " + account.getPassword(), reply2 }, "Server replied with unknown password, or that the account is locked, while " + "logging into popaccount: " + account.getAccountName(), SessionEvent.ERROR);
            isLoggedIn = false;
            return;
        }
        if (reply.indexOf("+OK") != -1) {
            notifyListeners(new String[] { "USER " + account.getUserName(), reply, "PASS ******", reply2 }, "Successfull logged into account: " + account.getAccountName(), SessionEvent.INFO);
            isLoggedIn = true;
            return;
        }
    }

    public synchronized void logof() {
        if (socket == null || socket.isClosed() || socket.isInputShutdown() || socket.isOutputShutdown()) isConnected = false;
        if (!isConnected) return;
        try {
            String reply = session.write("QUIT");
            socket.close();
            isConnected = false;
        } catch (IOException e) {
            notifyIOException(e);
        }
    }

    /**
	 * Method extracts the number of mails in inbox from the stat-reply
	 * 
	 * @param serverreply
	 *            Description of Parameter
	 * @return Description of the Returned Value
	 */
    private int parseStat(String serverreply) {
        String[] temp = new String[10];
        char[] array = serverreply.trim().toCharArray();
        int cur = 0;
        temp[0] = "";
        for (int i = 0; i < array.length; i++) {
            if (array[i] == statdelimiter) {
                cur++;
                temp[cur] = "";
            } else {
                temp[cur] += array[i];
            }
        }
        return Integer.parseInt(temp[1]);
    }

    /**
	 * Takes a list of replies and returns a list of UIDMarkers.
	 * 
	 * @param reply
	 * @return UIDMarker[]
	 */
    private static UIDMarker[] parseUidl(String[] reply) {
        if (reply.length == 0) return null;
        UIDMarker[] uids = new UIDMarker[reply.length - 1];
        int i = 1;
        while (i < reply.length) {
            reply[i] = reply[i].trim();
            int k = 0;
            if ((k = reply[i].indexOf(" ")) != -1) {
                try {
                    int j = Integer.parseInt(reply[i].substring(0, k));
                    uids[i - 1] = new UIDMarker(j, reply[i].substring(k));
                } catch (NumberFormatException e) {
                }
            }
            i++;
        }
        return uids;
    }

    private void initSession() {
        if (isConnected) return;
        tryToConnect();
    }

    /**
	 * Method clearUidBuffer.
	 */
    public synchronized void clearUidBuffer() {
        if (uids != null) {
            uids = null;
            logof();
        }
    }

    /**
	 * This method tries to connect to server. Maybe it shouldnt.
	 * 
	 * @return
	 */
    public synchronized boolean isConnected() {
        if (!isConnected) {
            tryToConnect();
            if (isConnected) return true;
            return false;
        } else {
            return true;
        }
    }

    private void notifyIOException() {
        SessionEvent e = new SessionEvent(new String[] { "IOException" }, "Timeout or socket-error " + " while negotiating with popaccount: " + account.getAccountName(), SessionEvent.WARN);
        notifyListeners(e);
    }

    private void notifyIOException(String[] conversation) {
        SessionEvent e = new SessionEvent(null, "Timeout or socket-error " + " while negotiating with popaccount: " + account.getAccountName(), SessionEvent.INFO);
        notifyListeners(e);
    }

    /**
	 * A simple class which encapsulate an uidmarker. The mark is the index of
	 * the message. Message 1, 2, 3 etc.
	 *  
	 */
    public static class UIDMarker {

        int index;

        String uid;

        public UIDMarker(int index, String uid) {
            this.index = index;
            this.uid = uid;
        }

        public int getIndex() {
            return index;
        }

        public String getUID() {
            return uid;
        }
    }

    public void setExtensions(int extensions) {
        this.extensions = extensions;
    }

    /**
	 * Method setExtention. For setting weather an extention is supported or
	 * not.
	 * 
	 * @param extension
	 */
    public void setExtention(int extension) {
        extensions = extensions | extension;
    }

    public void removeExtension(int extension) {
        extensions = extensions - extension;
    }

    /**
	 * Method for getting available extensions. They are returned as an integer,
	 * calculated upon which bits are set. See the Extension class. 0 means that
	 * no extension is supported, 1 means top, 3 means top and user, etc. -1
	 * means that the extension has been tested, could be because of an
	 * ioexception or similar.
	 *  
	 */
    public int getExtensions() {
        if (extensions == -1) {
            initSession();
            if (!isConnected) return 0;
            int ex = 0;
            String[] reply = null;
            try {
                reply = session.CAPA();
                if (reply.length == 0) return extensions;
                if (reply[0].startsWith(ERR)) {
                    ex = ex | Extension.APOP;
                    ex = ex | Extension.UIDL;
                    notifyListeners(new String[] { "CAPA", reply[0] }, "Getting capabilities of account: " + account.getAccountName(), SessionEvent.DEBUG);
                    getLoginDelay();
                } else {
                    for (int i = 0; i < reply.length; i++) {
                        if (reply[i].startsWith("TOP")) {
                            ex = ex | Extension.TOP;
                            continue;
                        }
                        if (reply[i].startsWith("UIDL")) {
                            ex = ex | Extension.UIDL;
                            continue;
                        }
                        if (reply[i].startsWith("EXPIRE")) {
                            ex = ex | Extension.EXPIRE;
                            continue;
                        }
                        if (reply[i].startsWith("USER")) {
                            ex = ex | Extension.USER;
                            continue;
                        }
                        if (reply[i].startsWith("LOGIN-DELAY")) {
                            String time = reply[i].substring(reply[i].indexOf(" "));
                            try {
                                loginDelay = Integer.parseInt(time);
                            } catch (NumberFormatException f) {
                                loginDelay = -1;
                            }
                            ex = ex | Extension.LOGIN_DELAY;
                            continue;
                        }
                        if (reply[i].startsWith("USER")) {
                            ex = ex | Extension.USER;
                            continue;
                        }
                        if (reply[i].startsWith("PIPELINING")) {
                            ex = ex | Extension.PIPELINING;
                            continue;
                        }
                    }
                    String[] conv = new String[reply.length + 1];
                    conv[0] = "CAPA";
                    for (int i = 0; i < reply.length; i++) {
                        conv[i + 1] = reply[i];
                    }
                    notifyListeners(conv, "Getting list of messages from account: " + account.getAccountName(), SessionEvent.DEBUG);
                }
                extensions = ex;
                return extensions;
            } catch (IOException f) {
                f.printStackTrace();
                if (reply != null) notifyIOException(reply); else notifyIOException();
            }
        }
        return extensions;
    }

    /**
	 * This gets the login delay time from the server, IF the server supports
	 * this. If it doesnt support it, or an error happens, returns -1. Otherwise
	 * returns login delay in seconds. Login delay is the time that needs to
	 * pass from one login to the next.
	 * 
	 *  
	 */
    public int getLoginDelay() {
        try {
            String reply = session.write("LOGIN-DELAY");
            if (reply.startsWith(OK)) {
                extensions = extensions | Extension.LOGIN_DELAY;
                String time = reply.substring(reply.indexOf(" "));
                try {
                    loginDelay = Integer.parseInt(time);
                } catch (NumberFormatException f) {
                    loginDelay = -1;
                }
            } else {
                loginDelay = -1;
            }
            notifyListeners(new String[] { "LOGIN-DELAY", reply }, "Getting login delay attribute", SessionEvent.INFO);
        } catch (IOException f) {
            notifyListeners(new String[] { "LOGIN-DELAY" }, "Getting login delay attribute", SessionEvent.DEBUG);
            loginDelay = -1;
        }
        return loginDelay;
    }

    public static class Extension {

        public static final int TOP = 1;

        public static final int USER = 2;

        public static final int PIPELINING = 4;

        public static final int UIDL = 8;

        public static final int EXPIRE = 16;

        public static final int LOGIN_DELAY = 32;

        public static final int CAPA = 64;

        public static final int APOP = 128;
    }

    /**
	 * Method for setting the timeout value of the connection.
	 * 
	 *  
	 */
    public void setConnectionTimeout(int seconds) {
    }

    /**
	 * Some servers close connection when APOP authentication fails, so we have
	 * to close the socket and try to reconnect.
	 */
    private void reconnect() {
        logof();
        connect(session, account);
    }

    public boolean useSSL() {
        return account.useSSL();
    }
}
