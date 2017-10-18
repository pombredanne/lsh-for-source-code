package com.voztele.sipspy.connections;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import com.voztele.sipspy.MessageListener;
import com.voztele.sipspy.MessageProvider;

public class Connection implements ActionListener, MessageProvider {

    private Logger log = Logger.getLogger(this.getClass());

    public static final int ERROR = 0;

    public static final int DISCONNECTED = 1;

    public static final int CONNECTED = 2;

    public static final int DEVICES = 3;

    private ArrayList listeners = new ArrayList();

    private ArrayList messageListeners = new ArrayList();

    private int state = DISCONNECTED;

    private static int recvdMsgs = 0;

    private Element configElement;

    private ConnectionManager connections;

    private Socket controlSocket = null;

    private DataThread dataThread = null;

    private Boolean connected = new Boolean(false);

    private boolean lockedDevice = false;

    private boolean lockedFilter = false;

    private ArrayList devices = new ArrayList();

    private ArrayList ips = new ArrayList();

    private String nonce;

    private long timeout = 5000;

    private List sessionList;

    public Connection(ConnectionManager cm) {
        this.connections = cm;
        Document doc = cm.getConfigDoc();
        this.controlSocket = new Socket();
        this.configElement = doc.createElement("connection");
        cm.getConfigElem().appendChild(configElement);
        this.messageListeners.add(cm);
    }

    public Connection(ConnectionManager cm, Element configElement) {
        this.controlSocket = new Socket();
        this.connections = cm;
        this.configElement = configElement;
        this.messageListeners.add(cm);
    }

    public void addListener(ConnectionListener model) {
        listeners.add(model);
    }

    public void removeListener(ConnectionsTableModel model) {
        listeners.remove(model);
    }

    public int getState() {
        return this.state;
    }

    public String getIp() {
        Element l = getElement("ip");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public String getPort() {
        Element l = getElement("port");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public int getRecvdMsgs() {
        return this.recvdMsgs;
    }

    public Element getElement(String ele) {
        if (configElement == null) return null;
        NodeList nl = configElement.getElementsByTagName(ele);
        return nl != null ? (Element) nl.item(0) : null;
    }

    public Element getConfigElm() {
        return this.configElement;
    }

    public String getRegexp() {
        Element l = getElement("regexp");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public void start() {
        this.state = CONNECTED;
        fireEvent(CONNECTED);
    }

    private void fireEvent(int type) {
        for (int i = 0; i < listeners.size(); i++) {
            ((ConnectionListener) listeners.get(i)).connectionEvent(this, type);
        }
    }

    public void actionPerformed(ActionEvent arg0) {
        String com = arg0.getActionCommand();
        try {
            if (com.equals("start")) {
                setConnected(new Boolean(true));
            } else if (com.equals("stop")) {
                setConnected(new Boolean(false));
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fireEvent(this.state);
    }

    public Boolean isConnected() {
        return this.connected;
    }

    public void setConnected(Boolean connect) throws UnknownHostException, IOException {
        log.debug("Connecting to " + getIp() + ":" + getPort());
        if (connected.booleanValue() == connect.booleanValue()) return;
        if (connect.booleanValue()) {
            devices.clear();
            ips.clear();
            try {
                controlSocket = new Socket(getIp(), Integer.parseInt(getPort()));
            } catch (IOException e) {
                setState(DISCONNECTED);
                throw e;
            }
            try {
                this.dataThread = new DataThread(this, controlSocket);
            } catch (ParserConfigurationException e) {
                error(e.getMessage(), e.getClass().getName());
            } catch (SAXException e) {
                error(e.getMessage(), e.getClass().getName());
            }
            setState(CONNECTED);
            connected = new Boolean(true);
            setUsername(getUsername());
            setPassword(getPassword());
            setRegexp(getRegexp());
            setFilter(getFilter());
            setDevice(getDevice());
        } else {
            setState(DISCONNECTED);
            connected = new Boolean(false);
            try {
                if (!controlSocket.isClosed()) ;
                sendCommand("bye", "");
            } catch (IOException e) {
                controlSocket.close();
                throw e;
            }
            controlSocket.close();
        }
    }

    public void add() {
        this.connections.addConnection(this);
    }

    public String getFilter() {
        Element l = getElement("filter");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public String getDevice() {
        Element l = getElement("device");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public Element addConfigElement(String name, String value) {
        Element ele = connections.getConfigDoc().createElement(name);
        Text t = connections.getConfigDoc().createTextNode(value);
        ele.appendChild(t);
        return ele;
    }

    public Attributes sendCommand(String command, String arg) throws IOException {
        return dataThread.sendCommand(command, arg);
    }

    public void addMessageListener(MessageListener ml) {
        this.messageListeners.add(ml);
    }

    public void removeMessageListener(MessageListener ml) {
        this.messageListeners.remove(ml);
    }

    public ArrayList getMessageListeners() {
        return messageListeners;
    }

    public void exception(Exception e) {
        error(e.getMessage(), e.getClass().getName());
        e.printStackTrace();
    }

    public void error(String msg, String title) {
        JOptionPane.showMessageDialog(connections.getConnectionsFrame(), msg, title, JOptionPane.ERROR_MESSAGE);
    }

    public void setIp(String string) {
        NodeList nl = this.configElement.getElementsByTagName("ip");
        Node n = nl.item(0);
        ((Text) n.getFirstChild()).setData(string);
    }

    public void setPort(String string) {
        NodeList nl = this.configElement.getElementsByTagName("port");
        Node n = nl.item(0);
        ((Text) n.getFirstChild()).setData(string);
    }

    public boolean setRegexp(String string) {
        Attributes result = null;
        NodeList nl;
        Node n;
        if (connected.booleanValue()) try {
            result = sendCommand("regexp", string);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!connected.booleanValue()) {
            nl = this.configElement.getElementsByTagName("regexp");
            n = nl.item(0);
            ((Text) n.getFirstChild()).setData(string);
            return true;
        } else {
            String status = result.getValue("status");
            if (status == null) {
                error("Wrong Regexp (" + result + ")", "ERROR");
                return false;
            } else if (status.equals("ok")) {
                nl = this.configElement.getElementsByTagName("regexp");
                n = nl.item(0);
                ((Text) n.getFirstChild()).setData(string);
                return true;
            } else if (status.equals("error")) {
                error("Wrong Regexp:" + result.getValue("reason"), "ERROR");
                return false;
            } else return false;
        }
    }

    public void setDevice(String string) {
        if (string == null) return;
        if (string.length() == 0) return;
        Attributes result = null;
        NodeList nl;
        Node n;
        if (connected.booleanValue()) try {
            log.debug("sending device command:" + string);
            result = sendCommand("device", string);
        } catch (IOException e) {
            e.printStackTrace();
        }
        log.debug("result:" + result);
        if (!connected.booleanValue()) {
            nl = this.configElement.getElementsByTagName("device");
            n = nl.item(0);
            if (n.getFirstChild() == null) {
                Text t = configElement.getOwnerDocument().createTextNode(string);
                n.appendChild(t);
            } else ((Text) n.getFirstChild()).setData(string);
            lockedDevice = false;
        } else {
            String status = result.getValue("status");
            if (status == null) {
                error("Wrong Device (" + result + ")", "ERROR");
            } else if (status.equals("ok")) {
                nl = this.configElement.getElementsByTagName("device");
                n = nl.item(0);
                if (n.getFirstChild() == null) {
                    Text t = configElement.getOwnerDocument().createTextNode(string);
                    n.appendChild(t);
                } else ((Text) n.getFirstChild()).setData(string);
                lockedDevice = false;
            } else if (status.equals("locked")) {
                lockedDevice = true;
            } else if (status.equals("error")) {
                String reason = result.getValue("reason");
                error("Error setting device :" + reason, "ERROR");
            } else {
                error("Wrong Device (" + result + ")", "ERROR");
            }
        }
    }

    public void setFilter(String string) {
        Attributes result = null;
        NodeList nl;
        Node n;
        if (connected.booleanValue()) try {
            result = sendCommand("filter", string);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!connected.booleanValue()) {
            nl = this.configElement.getElementsByTagName("filter");
            n = nl.item(0);
            if (n.getFirstChild() == null) {
                Text t = configElement.getOwnerDocument().createTextNode(string);
                n.appendChild(t);
            } else ((Text) n.getFirstChild()).setData(string);
            lockedFilter = false;
        } else {
            String status = result.getValue("status");
            if (status == null) {
                lockedFilter = true;
            } else {
                if (status.equals("ok")) {
                    nl = this.configElement.getElementsByTagName("filter");
                    n = nl.item(0);
                    if (n.getFirstChild() == null) {
                        Text t = configElement.getOwnerDocument().createTextNode(string);
                        n.appendChild(t);
                    } else ((Text) n.getFirstChild()).setData(string);
                    lockedFilter = false;
                } else if (status.equals("locked")) {
                    lockedFilter = true;
                } else if (status.equals("error")) {
                    error("Error reason:" + result.getValue("reason"), "ERROR");
                } else {
                    error("Wrong filter (" + result + ")", "ERROR");
                }
            }
        }
    }

    /**
    * @param state
    *           The state to set.
    */
    public void setState(int state) {
        this.state = state;
        fireEvent(state);
    }

    /**
    * @return Returns the connectionManager.
    */
    public ConnectionManager getConnectionManager() {
        return connections;
    }

    /**
    * @return Returns the lockedDevice.
    */
    public boolean isLockedDevice() {
        return lockedDevice;
    }

    /**
    * @param lockedDevice
    *           The lockedDevice to set.
    */
    public void setLockedDevice(boolean lockedDevice) {
        this.lockedDevice = lockedDevice;
    }

    /**
    * @return Returns the lockedFilter.
    */
    public boolean isLockedFilter() {
        return lockedFilter;
    }

    /**
    * @param lockedFilter
    *           The lockedFilter to set.
    */
    public void setLockedFilter(boolean lockedFilter) {
        this.lockedFilter = lockedFilter;
    }

    public void addDevice(String device, String ip) {
        log.debug("Adding device:" + device + ":" + ip);
        this.devices.add(device);
        this.ips.add(ip);
        fireEvent(DEVICES);
    }

    public String[] getDevices() {
        return (String[]) (devices.size() == 0 ? new String[] { "" } : castarray(devices.toArray()));
    }

    public String[] getIps() {
        return (String[]) (ips.size() == 0 ? new String[] { "" } : castarray(ips.toArray()));
    }

    public String[] castarray(Object[] os) {
        String[] ret = new String[os.length];
        for (int i = 0; i < os.length; i++) {
            ret[i] = (String) os[i];
        }
        return ret;
    }

    public boolean setUsername(String string) {
        Attributes result = null;
        NodeList nl;
        Node n;
        if (connected.booleanValue()) try {
            result = sendCommand("username", string);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (!connected.booleanValue()) {
            nl = this.configElement.getElementsByTagName("username");
            n = nl.item(0);
            ((Text) n.getFirstChild()).setData(string);
            return true;
        } else {
            String status = result.getValue("status");
            if (status == null) {
                error("Wrong Username (" + result + ")", "ERROR");
                return false;
            } else if (status.equals("ok")) {
                nonce = result.getValue("nonce");
                nl = this.configElement.getElementsByTagName("username");
                n = nl.item(0);
                ((Text) n.getFirstChild()).setData(string);
                log.debug("received nonce=" + nonce);
                return true;
            } else if (status.equals("error")) {
                String reason = result.getValue("reason");
                error("Error reason:" + reason, "ERROR");
                return false;
            } else {
                error("Wrong Username (" + result + ")", "ERROR");
                return false;
            }
        }
    }

    public boolean setPassword(String string) {
        Attributes result = null;
        NodeList nl;
        Node n;
        if (!connected.booleanValue()) {
            nl = this.configElement.getElementsByTagName("password");
            n = nl.item(0);
            ((Text) n.getFirstChild()).setData(string);
            return true;
        } else {
            try {
                String codified = calculatePassword(string);
                result = sendCommand("password", codified);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            String status = result.getValue("status");
            if (status == null) {
                error("Unable to send password (" + result + ")", "ERROR");
                return false;
            } else if (status.equals("ok")) {
                log.warn("EUREKA! password accepted");
                return true;
            } else {
                error("Authentication error:" + result.getValue("reason"), "ERROR");
                return false;
            }
        }
    }

    public static String toHexString(byte[] array) {
        StringBuffer result = new StringBuffer(2 * array.length);
        for (int i = 0; i < array.length; i++) {
            String hex = Integer.toHexString(array[i] & 0xff);
            if (hex.length() == 1) {
                result.append('0');
            }
            result.append(hex);
        }
        return result.toString();
    }

    private String calculatePassword(String string) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("md5");
            md5.update(nonce.getBytes());
            md5.update(string.getBytes());
            return toHexString(md5.digest());
        } catch (NoSuchAlgorithmException e) {
            error("MD5 digest is no supported !!!", "ERROR");
            return null;
        }
    }

    public String getUsername() {
        Element l = getElement("username");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public String getPassword() {
        Element l = getElement("password");
        if (l == null) return null;
        Text e = (Text) l.getFirstChild();
        return e != null ? e.getNodeValue() : null;
    }

    public String toString() {
        return this.getIp();
    }

    public boolean sendFile(String name, byte[] bytes) {
        Attributes result;
        if (connected.booleanValue()) {
            try {
                log.debug("sending file name:" + name);
                result = sendCommand("save", name + ":" + bytes.length);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else return false;
        log.debug("result:" + result);
        if (!connected.booleanValue()) {
            return false;
        } else {
            String status = result.getValue("status");
            if (status == null) {
                error("Unable to send file (" + result + ")", "ERROR");
            } else if (status.equals("ok")) {
                try {
                    dataThread.sendArray(bytes);
                } catch (IOException e) {
                    error("Network error while sending file!" + e.getMessage(), "ERROR");
                    return false;
                }
                return true;
            } else if (status.equals("error")) {
                String reason = result.getValue("reason");
                error("Error sending file:" + reason, "ERROR");
            } else {
                error("Error sending file(" + result + ")", "ERROR");
            }
        }
        return false;
    }

    public long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public List listSessions() {
        Attributes result = null;
        NodeList nl;
        Node n;
        if (!connected.booleanValue()) {
            return null;
        } else {
            try {
                this.sessionList = new LinkedList();
                result = sendCommand("list", "");
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
            String status = result.getValue("status");
            if (status == null) {
                error("Unable get list (" + result + ")", "ERROR");
                return null;
            } else if (status.equals("ok")) {
                log.warn("we got list!");
                return sessionList;
            } else {
                error("unable to get list:" + result.getValue("reason"), "ERROR");
                return null;
            }
        }
    }

    public void addSessionElem(String name) {
        sessionList.add(name);
    }

    public void stop() {
        try {
            setConnected(false);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
