package org.gimec.msnj;

import java.io.*;
import java.net.*;
import java.util.*;
import java.security.*;

/**
* The Connection class is the heart of library. This is where you login, send
* messages, etc.
*/
public class Connection {

    private static final boolean DEBUG = true;

    private static final boolean LOOP_DEBUG = false;

    private static final String DEFAULT_HOST = "messenger.hotmail.com";

    private static final int DEFAULT_PORT = 1863;

    private static final String DEFAULT_PROTOCOL = "MSNP2";

    private static final String DEFAULT_DOMAIN = "@hotmail.com";

    /** Maximum amount of time to wait for a response to a command. */
    private static final int REQUEST_TIMEOUT = 30000;

    private String m_handle = "";

    private UserState m_state = UserState.FLN;

    private String m_passwd = "";

    private String m_commonName = "";

    private String m_cookie = "";

    private String m_format = "FN=MS%20Sans%20Serif; EF=; CO=0; CS=0; PF=0";

    /**
	 * The list of users this Connection can contact.
	 */
    private List m_chatUsers = new ArrayList();

    /**
	 * The list of users for whom a given user wants to receive state change
	 * notifications. The Forward List is what is most commonly referred to as
	 * the user's "contact list."
	 */
    private List m_forwardList = new ArrayList();

    /**
	 * The list of users who the user has explicitly allowed to see state
	 * change notifications and establish client-to-client sessions via a
	 * Switchboard Server.
	 */
    private List m_allowList = new ArrayList();

    /**
	 * The list of users who the user has explicitly prevented from seeing
	 * state change notifications and establishing client-to-client sessions
	 * via a Switchboard Server.
	 */
    private List m_blockList = new ArrayList();

    /**
	 * The list of users who have registered an interest in seeing this user's
	 * state change notifications.
	 */
    private List m_reverseList = new ArrayList();

    /**
	 * Connections to different users, their String user name being the key.
	 */
    private Map m_connections = new HashMap();

    private Map m_requests = Collections.synchronizedMap(new HashMap());

    private Map m_responses = Collections.synchronizedMap(new HashMap());

    private BufferedReader m_reader;

    private PrintWriter m_writer;

    private boolean m_closed = true;

    private boolean m_inInit = false;

    private List m_listeners;

    private Watcher m_watcher;

    private boolean m_running = false;

    public Connection() {
        m_listeners = new ArrayList();
    }

    private Connection(String address, List listeners) throws ConnectionException {
        m_listeners = listeners;
        connectToServer(address);
    }

    private Connection(String host, int port, List listeners) throws ConnectionException {
        m_listeners = listeners;
        connectToServer(host, port);
    }

    /**
	 * Logs in to the default server.
	 */
    public void login(String handle, String passwd) throws ConnectionException {
        login(handle, passwd, DEFAULT_HOST, DEFAULT_PORT);
    }

    /**
	 * Logs into the specified server.
	 */
    public void login(String handle, String passwd, String host) throws ConnectionException {
        login(handle, passwd, host, DEFAULT_PORT);
    }

    /**
	 * Logs into the specified server.
	 */
    public void login(String handle, String passwd, String host, int port) throws ConnectionException {
        final int MAX_RETRIES = 5;
        boolean connected = false;
        m_inInit = true;
        int retries = 0;
        while (!connected && retries++ < MAX_RETRIES) {
            try {
                connectToServer(host, port);
                startWatcher();
                setProtocol(DEFAULT_PROTOCOL);
                String sp = getServerPolicyInfo();
                authenticateUserMD5(handle, passwd);
                connected = true;
            } catch (ConnectionException ex) {
                if (DEBUG) ex.printStackTrace();
                stopWatcher();
            }
        }
        m_inInit = false;
        if (!connected) throw new ConnectionException("could not connect");
        synchronize();
        changeState(UserState.NLN);
    }

    /**
	 * Send a message to the specified user.
	 */
    public void sendMessage(String handle, MimeMessage message) throws ConnectionException {
        handle = addDomain(handle);
        Connection conn = (Connection) m_connections.get(handle);
        if (conn == null) {
            conn = requestSwitchboardSession();
            m_connections.put(handle, conn);
        }
        if (!conn.m_chatUsers.contains(new User(handle))) {
            try {
                conn.sendCall(handle);
                conn.waitForJoin(handle);
            } catch (ConnectionException ex) {
                if (DEBUG) ex.printStackTrace();
                throw new ConnectionException("Could not call user");
            }
        }
        conn.sendMessage(message);
    }

    /**
	 * Add the specified user to the forward list.
	 */
    public void addContact(String handle) throws ConnectionException {
        handle = addDomain(handle);
        String[] args = { "FL", handle, handle };
        sendCmd(Command.ADD, args);
    }

    /**
	 * Remove the specified user from a forward list.
	 */
    public void removeContact(String handle) throws ConnectionException {
        handle = addDomain(handle);
        String[] args = { "FL", handle };
        sendCmd(Command.REM, args);
    }

    /**
	 * Add the specified user to the allow list.
	 */
    public void allowContact(String handle) throws ConnectionException {
        handle = addDomain(handle);
        String[] args = { "AL", handle, handle };
        sendCmd(Command.ADD, args);
    }

    /**
	 * Add the specified user to the block list.
	 */
    public void blockContact(String handle) throws ConnectionException {
        handle = addDomain(handle);
        String[] args = { "BL", handle, handle };
        sendCmd(Command.ADD, args);
    }

    /**
	 * Close the Connection to the specified user.
	 */
    public void endChat(String handle) throws ConnectionException {
        Connection conn = (Connection) m_connections.get(handle);
        if (conn != null) {
            conn.destroy();
        }
    }

    /**
	 * Log out.
	 */
    public void logout() throws ConnectionException {
        sendBYE();
        m_running = false;
        m_watcher = null;
        destroy();
    }

    /**
	 * Change the user's state.
	 */
    public void changeState(UserState state) throws ConnectionException {
        sendCmd(Command.CHG, state.toString());
    }

    /**
	 * Returns a copy of the forward list. Note, changes to this list will not be
	 * reflected elsewhere. You must use the addContact, removeContact methods.
	 */
    public List getForwardList() {
        return Util.deepCopy(m_forwardList);
    }

    /**
	 * Returns a copy of the reverse list. Note, changes to this list will not be
	 * reflected elsewhere.
	 */
    public List getReverseList() {
        return Util.deepCopy(m_reverseList);
    }

    /**
	 * Returns a copy of the allow list. Note, changes to this list will not be
	 * reflected elsewhere. You must use the allowContact, blockContact methods.
	 */
    public List getAllowList() {
        return Util.deepCopy(m_allowList);
    }

    /**
	 * Returns a copy of the block list. Note, changes to this list will not be
	 * reflected elsewhere. You must use the allowContact, blockContact methods.
	 */
    public List getBlockList() {
        return Util.deepCopy(m_blockList);
    }

    public String getHandle() {
        return m_handle;
    }

    public String getCommonName() {
        return m_commonName;
    }

    public UserState getState() {
        return m_state;
    }

    /**
	 * Add a listener to this list of listeners.
	 */
    public void addListener(ConnectionListener listener) {
        m_listeners.add(listener);
    }

    /**
	 * Remove a listener from the list of listeners.
	 */
    public void removeListener(ConnectionListener listener) {
        m_listeners.remove(listener);
    }

    private void destroy() {
        m_closed = true;
        m_chatUsers.clear();
        m_forwardList.clear();
        m_allowList.clear();
        m_blockList.clear();
        m_reverseList.clear();
        m_connections.clear();
        try {
            m_reader.close();
            m_writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        fireClose();
    }

    private void fireMessageReceived(User fromUser, String message) {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            User newUser = (User) fromUser.clone();
            MimeMessage mmsg = new MimeMessage(message);
            ConnectionEvent ev = new ConnectionEvent(this, newUser, mmsg);
            ((ConnectionListener) iter.next()).messageReceived(ev);
        }
    }

    private void fireStateChange(User user) {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            User newUser = (User) user.clone();
            ConnectionEvent ev = new ConnectionEvent(this, newUser);
            ((ConnectionListener) iter.next()).stateChange(ev);
        }
    }

    private void fireStateChange() {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            ConnectionEvent ev = new ConnectionEvent(this);
            ((ConnectionListener) iter.next()).stateChange(ev);
        }
    }

    private void fireAuthorizeUser(User user) {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            User newUser = (User) user.clone();
            ConnectionEvent ev = new ConnectionEvent(this, newUser);
            ((ConnectionListener) iter.next()).authorizeUser(ev);
        }
    }

    private void fireExitChat(User user) {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            User newUser = (User) user.clone();
            ConnectionEvent ev = new ConnectionEvent(this, newUser);
            ((ConnectionListener) iter.next()).exitChat(ev);
        }
    }

    private void fireClose() {
        Iterator iter = m_listeners.iterator();
        while (iter.hasNext()) {
            ConnectionEvent ev = new ConnectionEvent(this);
            ((ConnectionListener) iter.next()).close(ev);
        }
    }

    private void setCookie(String cookie) {
        m_cookie = cookie;
    }

    private void connectToServer(String address) throws ConnectionException {
        int i = address.indexOf(':');
        String host = address.substring(0, i);
        int port;
        if (i == -1) {
            port = DEFAULT_PORT;
        } else {
            port = Integer.parseInt(address.substring(i + 1));
        }
        connectToServer(host, port);
    }

    private void connectToServer(String host, int port) throws ConnectionException {
        try {
            if (DEBUG) System.err.print("connecting to " + host + ":" + port + " ... ");
            Socket socket = new Socket(host, port);
            m_reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            m_writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())));
            m_closed = false;
            if (DEBUG) System.err.println("successful");
        } catch (IOException ex) {
            throw new ConnectionException(ex.getMessage());
        }
    }

    private void setProtocol(String protocol) throws ConnectionException {
        Response res = getRes(Command.VER, protocol);
        if (res == null) {
            throw new ConnectionException("Protocol error while setting protocol (no response)");
        } else if (!Command.VER.equals(res.getCommand())) {
            Command cmd = res.getCommand();
            if (ErrorCode.isErrorCode(cmd)) throw new ConnectionException("Protocol error while setting protocol", ErrorCode.toErrorCode(cmd)); else throw new ConnectionException("Protocol error while setting protocol");
        }
        if (!protocol.equals(res.getArg(0))) {
            throw new ConnectionException("Could not set protocol");
        }
    }

    private String getServerPolicyInfo() throws ConnectionException {
        Response res = getRes(Command.INF);
        return res.getArg(0);
    }

    private void authenticateUserMD5(String handle, String passwd) throws ConnectionException {
        handle = addDomain(handle);
        String[] args = { "MD5", "I", handle };
        Response res = getRes(Command.USR, args);
        if (Command.XFR.equals(res.getCommand())) {
            handleXFR(res);
            res = getRes(Command.USR, args);
        }
        if (!Command.USR.equals(res.getCommand())) {
            throw new ConnectionException("Protocol error during authentication");
        }
        String hashRes = res.getArg(2) + passwd;
        byte[] digest;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            digest = md.digest(hashRes.getBytes("US-ASCII"));
        } catch (NoSuchAlgorithmException ex) {
            throw new ConnectionException(ex.getMessage());
        } catch (UnsupportedEncodingException ex) {
            throw new ConnectionException(ex.getMessage());
        }
        args[1] = "S";
        args[2] = Util.bytesToString(digest, 0, 16);
        res = getRes(Command.USR, args);
        if (res == null) {
            throw new ConnectionException("Protocol error during authentication");
        }
        if (!Command.USR.equals(res.getCommand())) {
            throw new ConnectionException("Protocol error during authentication");
        }
        m_commonName = res.getArg(2);
        m_handle = handle;
        m_passwd = passwd;
    }

    private void synchronize() throws ConnectionException {
        sendCmd(Command.SYN, "1");
    }

    private void sendMessage(MimeMessage message) throws ConnectionException {
        if (message.getHeader("X-MMS-IM-Format") == null) message.setHeader("X-MMS-IM-Format", m_format);
        String msgStr = message.toString();
        String[] args = { "N", String.valueOf(msgStr.length()), "\r\n" + msgStr };
        sendCmd(Command.MSG, args);
    }

    private Connection requestSwitchboardSession() throws ConnectionException {
        Response res = getRes(Command.XFR, "SB");
        if (null == res || !Command.XFR.equals(res.getCommand())) {
            throw new ConnectionException("Protocol error while requesting switchboard connection");
        }
        String address = res.getArg(1);
        Connection conn = new Connection(address, m_listeners);
        String cookie = res.getArg(3);
        conn.setCookie(cookie);
        m_watcher.addConnection(conn);
        String[] args = { m_handle, cookie };
        res = conn.getRes(Command.USR, args);
        if (!Command.USR.equals(res.getCommand())) {
            throw new ConnectionException("Protocol error while authenticating with new switchboard connection");
        }
        return conn;
    }

    private void sendCall(String handle) throws ConnectionException {
        Response res = getRes(Command.CAL, handle);
        if (!Command.CAL.equals(res.getCommand())) {
            throw new ConnectionException("Protocol error while calling " + handle);
        }
    }

    private void addResponse(Response res) throws ConnectionException {
        long trid = res.getTrID();
        Request req = (Request) m_requests.get(new Long(trid));
        if (req == null) {
            processResponse(res);
        } else {
            m_responses.put(new Long(res.getTrID()), res);
            synchronized (m_responses) {
                m_responses.notifyAll();
            }
        }
    }

    private void handleJOI(Response res) throws ConnectionException {
        String handle = res.getArg(0);
        String commonName = res.getArg(1);
        m_chatUsers.add(new User(handle, commonName, UserState.NLN));
        synchronized (this) {
            notify();
        }
    }

    private void handleIRO(Response res) throws ConnectionException {
        String handle = res.getArg(2);
        String commonName = res.getArg(3);
        m_chatUsers.add(new User(handle, commonName, UserState.NLN));
        synchronized (this) {
            notify();
        }
    }

    private void handleRNG(Response res) throws ConnectionException {
        String handle = addDomain(m_handle);
        String sessionId = String.valueOf(res.getTrID());
        String address = res.getArg(0);
        String authChallengeInfo = res.getArg(2);
        String callingUserHandle = res.getArg(3);
        String callingUserCommonName = res.getArg(4);
        Connection conn = new Connection(address, m_listeners);
        m_connections.put(callingUserHandle, conn);
        m_watcher.addConnection(conn);
        String[] newArgs = { handle, authChallengeInfo, sessionId };
        Request req = new Request(Command.ANS, newArgs);
        m_requests.put(new Long(req.getTrID()), req);
        conn.sendCmd(req);
    }

    private void handleMSG(Response res) throws ConnectionException {
        StringTokenizer tok = res.getTokenizer();
        User fromUser = new User(tok.nextToken(), tok.nextToken(), UserState.NLN);
        int len = Integer.parseInt(tok.nextToken());
        String msg = null;
        try {
            char[] cbuf = new char[len];
            m_reader.read(cbuf, 0, len);
            msg = String.valueOf(cbuf);
        } catch (IOException ex) {
            throw new ConnectionException(ex.getMessage());
        }
        fireMessageReceived(fromUser, msg);
    }

    private void handleBYE(Response res) throws ConnectionException {
        User user = new User(res.getTokenizer().nextToken());
        fireExitChat(user);
        m_chatUsers.remove(user);
        if (m_chatUsers.size() == 0) destroy();
    }

    private void handleILN(Response res) throws ConnectionException {
        UserState state = new UserState(res.getArg(0));
        User user = new User(res.getArg(1), res.getArg(2), state);
        int i = m_forwardList.indexOf(user);
        if (i != -1) {
            user = (User) m_forwardList.get(i);
            user.setState(state);
        } else {
            if (DEBUG) System.err.println("warning: got ILN for user that is not in the forward list");
        }
        fireStateChange(user);
    }

    private void handleFLN(Response res) throws ConnectionException {
        User user = new User(res.getArg(0));
        int i = m_forwardList.indexOf(user);
        if (i != -1) {
            user = (User) m_forwardList.get(i);
            user.setState(UserState.FLN);
            Connection conn = (Connection) m_connections.get(user.getHandle());
            if (conn != null) {
                conn.m_chatUsers.remove(user);
                if (conn.m_chatUsers.size() == 0) {
                    conn.destroy();
                    m_connections.remove(user.getHandle());
                }
            }
        } else {
            if (DEBUG) System.err.println("warning: got FLN for user that is not in the forward list");
        }
        fireStateChange(user);
    }

    private void handleNLN(Response res) throws ConnectionException {
        UserState state = new UserState(res.getArg(0));
        User user = new User(res.getArg(1), res.getArg(2), state);
        int i = m_forwardList.indexOf(user);
        if (i != -1) {
            user = (User) m_forwardList.get(i);
            user.setState(state);
        } else {
            if (DEBUG) System.err.println("warning: got NLN for user that is not in the forward list");
        }
        fireStateChange(user);
    }

    private void handleLST(Response res) throws ConnectionException {
        if (res.countArgs() != 6) return;
        String list = res.getArg(0);
        int index = Integer.parseInt(res.getArg(2));
        String handle = res.getArg(4);
        String commonName = res.getArg(5);
        User user = new User(handle, commonName, index, UserState.FLN);
        if ("FL".equals(list)) {
            m_forwardList.add(user);
        } else if ("AL".equals(list)) {
            m_allowList.add(user);
        } else if ("BL".equals(list)) {
            m_blockList.add(user);
        } else if ("RL".equals(list)) {
            m_reverseList.add(user);
            if (!m_allowList.contains(user)) {
                fireAuthorizeUser(user);
            }
        }
        fireStateChange(user);
    }

    private void handleADD(Response res) throws ConnectionException {
        if (res.getTrID() > 0) {
            throw new ConnectionException("Tried to process ADD response as asynchronous");
        }
        String list = res.getArg(0);
        User user = new User(res.getArg(2), res.getArg(3), 0, UserState.FLN);
        List userList = null;
        if ("FL".equals(list)) userList = m_forwardList; else if ("AL".equals(list)) userList = m_allowList; else if ("BL".equals(list)) userList = m_blockList; else if ("RL".equals(list)) {
            userList = m_reverseList;
            fireAuthorizeUser(user);
        }
        userList.add(user);
    }

    private void handleREM(Response res) throws ConnectionException {
        if (res.getTrID() > 0) {
            throw new ConnectionException("Tried to process REM response as asynchronous");
        }
        String list = res.getArg(0);
        List userList = null;
        if ("FL".equals(list)) userList = m_forwardList; else if ("AL".equals(list)) userList = m_allowList; else if ("BL".equals(list)) userList = m_blockList; else if ("RL".equals(list)) userList = m_reverseList;
        userList.remove(new User(res.getArg(2)));
    }

    private void handleOUT(Response res) throws ConnectionException {
        logout();
    }

    private void handleXFR(Response res) throws ConnectionException {
        connectToServer(res.getArg(1));
        setProtocol(DEFAULT_PROTOCOL);
    }

    private void handleCHG(Response res) throws ConnectionException {
        m_state = new UserState(res.getArg(0));
        fireStateChange();
    }

    private void handleError(Response res) throws ConnectionException {
        ErrorCode err = ErrorCode.toErrorCode(res.getCommand());
        if (ErrorCode.SERVER_UNAVAILABLE.equals(err)) {
            Request req = null;
            boolean found = false;
            Iterator iter = m_requests.values().iterator();
            while (!found && iter.hasNext()) {
                req = (Request) iter.next();
                found = Command.VER.equals(req.getCommand());
            }
            if (found) {
                res = new Response(ErrorCode.SERVER_UNAVAILABLE + " " + req.getTrID());
                addResponse(res);
            }
        }
    }

    private void sendBYE() throws ConnectionException {
        sendCmd(Command.OUT);
    }

    private void waitForJoin(String handle) throws ConnectionException {
        int MAX_WAIT = 30000;
        long now = System.currentTimeMillis();
        User user = new User(handle);
        while (!m_chatUsers.contains(user) && System.currentTimeMillis() - now < MAX_WAIT) {
            try {
                synchronized (this) {
                    wait(100);
                }
            } catch (InterruptedException ex) {
                if (DEBUG) ex.printStackTrace();
            }
        }
        if (!m_chatUsers.contains(user)) {
            throw new ConnectionException("Timed out waiting for " + handle + " to join session");
        }
    }

    private void sendCmd(Command cmd) throws ConnectionException {
        sendCmd(new Request(cmd));
    }

    private void sendCmd(Command cmd, String arg) throws ConnectionException {
        sendCmd(new Request(cmd, arg));
    }

    private void sendCmd(Command cmd, String[] args) throws ConnectionException {
        sendCmd(new Request(cmd, args));
    }

    private void sendCmd(Request req) throws ConnectionException {
        if (DEBUG) {
            System.err.println(">>> " + req);
            try {
                if (m_reader.ready()) System.err.println("warning: reader is ready before send");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        m_writer.write(req.toString());
        m_writer.flush();
        if (m_writer.checkError()) {
            throw new ConnectionException("An IO error has occurred");
        }
    }

    private Response getRes(Command cmd) throws ConnectionException {
        return getRes(new Request(cmd));
    }

    private Response getRes(Command cmd, String arg) throws ConnectionException {
        return getRes(new Request(cmd, arg));
    }

    private Response getRes(Command cmd, String[] args) throws ConnectionException {
        return getRes(new Request(cmd, args));
    }

    private Response getRes(Request req) throws ConnectionException {
        sendCmd(req);
        Long trid = new Long(req.getTrID());
        m_requests.put(trid, req);
        long now = System.currentTimeMillis();
        Response res = null;
        while (res == null && System.currentTimeMillis() - now < REQUEST_TIMEOUT) {
            res = (Response) m_responses.get(trid);
            if (res == null) {
                try {
                    synchronized (this) {
                        wait(100);
                    }
                } catch (InterruptedException ex) {
                    if (DEBUG) ex.printStackTrace();
                }
            }
        }
        if (res == null) System.err.println("warning: got null response to " + req);
        if (DEBUG) System.err.println("got response: " + res);
        return res;
    }

    private String addDomain(String handle) {
        if (handle.indexOf('@') > -1) return handle;
        return handle + DEFAULT_DOMAIN;
    }

    private void processResponse(Response res) throws ConnectionException {
        Command cmd = res.getCommand();
        if (ErrorCode.isErrorCode(cmd)) handleError(res); else if (Command.RNG.equals(cmd)) handleRNG(res); else if (Command.JOI.equals(cmd)) handleJOI(res); else if (Command.IRO.equals(cmd)) handleIRO(res); else if (Command.MSG.equals(cmd)) handleMSG(res); else if (Command.BYE.equals(cmd)) handleBYE(res); else if (Command.ILN.equals(cmd)) handleILN(res); else if (Command.FLN.equals(cmd)) handleFLN(res); else if (Command.NLN.equals(cmd)) handleNLN(res); else if (Command.LST.equals(cmd)) handleLST(res); else if (Command.ADD.equals(cmd)) handleADD(res); else if (Command.REM.equals(cmd)) handleREM(res); else if (Command.OUT.equals(cmd)) handleOUT(res); else if (Command.XFR.equals(cmd)) handleXFR(res); else if (Command.CHG.equals(cmd)) handleCHG(res);
    }

    private void startWatcher() {
        m_running = true;
        m_watcher = new Watcher();
        m_watcher.addConnection(this);
        m_watcher.start();
        try {
            synchronized (this) {
                wait(1000);
            }
        } catch (InterruptedException ex) {
            if (DEBUG) ex.printStackTrace();
        }
    }

    private void stopWatcher() {
        m_running = false;
        try {
            if (m_watcher != null) m_watcher.join();
        } catch (InterruptedException ex) {
            if (DEBUG) ex.printStackTrace();
        }
        m_watcher = null;
    }

    private class Watcher extends Thread {

        private List m_watchConns = Collections.synchronizedList(new ArrayList());

        public Watcher() {
            super("Watcher");
        }

        public void run() {
            synchronized (Connection.this) {
                Connection.this.notify();
            }
            while (m_running) {
                if (LOOP_DEBUG) System.err.println("top of running loop");
                List connListCopy = new ArrayList(m_watchConns);
                Iterator iter = connListCopy.iterator();
                while (iter.hasNext()) {
                    Connection conn = (Connection) iter.next();
                    if (LOOP_DEBUG) System.err.println("about to synchronize on a connection");
                    synchronized (conn) {
                        if (LOOP_DEBUG) System.err.println("synchronized on connection");
                        if (conn.m_closed) {
                            m_watchConns.remove(conn);
                        } else {
                            BufferedReader reader = conn.m_reader;
                            boolean ready = false;
                            try {
                                ready = reader.ready();
                            } catch (IOException ex) {
                                if (DEBUG) ex.printStackTrace();
                            }
                            while (ready) {
                                if (LOOP_DEBUG) System.err.println("top of ready loop");
                                try {
                                    String line = reader.readLine();
                                    if (line == null) throw new IOException("Read EOF");
                                    if (DEBUG) System.err.println("<<< " + line);
                                    Response res = new Response(line);
                                    conn.addResponse(res);
                                    ready = reader.ready();
                                } catch (ConnectionException ex) {
                                    ex.printStackTrace();
                                } catch (IOException ex) {
                                    if (DEBUG) ex.printStackTrace();
                                    ready = false;
                                    if (conn.m_inInit) {
                                        Response res = new Response(ErrorCode.SERVER_UNAVAILABLE + " 0");
                                        try {
                                            conn.handleError(res);
                                        } catch (ConnectionException exx) {
                                            exx.printStackTrace();
                                        }
                                    } else {
                                        Iterator keyIter = m_connections.keySet().iterator();
                                        while (keyIter.hasNext()) {
                                            Object key = keyIter.next();
                                            if (m_connections.get(key) == conn) m_connections.remove(key);
                                        }
                                        m_watchConns.remove(conn);
                                        conn.destroy();
                                    }
                                }
                            }
                        }
                    }
                    if (LOOP_DEBUG) System.err.println("no longer synchronized on connection");
                }
                try {
                    sleep(200);
                } catch (InterruptedException ex) {
                    if (DEBUG) ex.printStackTrace();
                }
            }
        }

        public void addConnection(Connection conn) {
            m_watchConns.add(conn);
        }

        public void removeConnection(Connection conn) {
            m_watchConns.remove(conn);
        }
    }

    private void peekAt(BufferedReader reader) throws IOException {
        reader.mark(10000);
        StringBuffer buf = new StringBuffer();
        System.out.print("peek: ");
        while (reader.ready()) {
            buf.append((char) reader.read());
        }
        System.out.println(buf);
        reader.reset();
    }
}
