package jp.gr.java_conf.roadster.net.pop;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.net.ssl.SSLSocketFactory;

/**
 * POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 * POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 * ꆼꆼꆼꆼꆼꆼꆼjavax.mail.* ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 * <BR>
 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 * <OL>
 * <LI>POP3Clientꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
 * ꆼꆼꆼꆼꆼ.<BR>
 * <UL><CODE>
 *  POP3Client pc = new POP3Client();<BR>
 *  pc.setHost("pop3.host.ne.jp");<BR>
 *  pc.setUser("user");<BR>
 *  pc.setPassword("password");<BR>
 *  pc.setAuthorizationMode(POP3Client.APOP_AUTHORIZATION);<BR>
 * </CODE></UL>
 * <LI>ꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *   <UL><CODE>
 *    pc.connect();<BR>
 *   </CODE></UL>
 * <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *   <UL><CODE>
 *    int count = pc.getMessageCount();<BR>
 *   </CODE></UL>
 * <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *   <UL><CODE>
 *    InputStream ins = pc.retrieve(i); // i ꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *    while ((c = ins.read()) != -1) {
 *    <UL>
 *       // ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 *    </UL>
 *    }<BR>
 *    ins.close();<BR>
 *   </CODE></UL>
 * <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 *   <UL><CODE>
 *    pc.delete(i); // i ꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *   </CODE></UL>
 * <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *   <UL><CODE>
 *    pc.disconnect();<BR>
 *   </CODE></UL>
 * </OL>
 *
 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 * <OL>
 *  <LI>TCP/IPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ(greeting)ꆼꆼꆼꆼꆼꆼꆼꆼ.
 *  <LI>USERꆼꆼꆼꆼꆼPASSꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *      ꆼꆼꆼꆼAPOPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 *  <LI>LIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *      +OK ...<BR>
 *      1 203[CRLF]<BR>
 *      2 345[CRLF]<BR>
 *      (ꆼꆼ)<BR>
 *      .[CRLF]<BR>
 *  <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ512 ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
 *      ꆼꆼꆼꆼRETR ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
 * </OL>
 *
 */
public class POP3Client implements Serializable {

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    static final long serialVersionUID = -3928372169782263062L;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ( = 512ꆼꆼꆼ).
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private static final int MAX_LINE_LENGTH = 512;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private static final String ENCODE = "8859_1";

    /**
	 * ꆼꆼꆼꆼꆼꆼPOPꆼꆼꆼ(=110)
	 */
    protected static final int DEFAULT_POP3_PORT = 110;

    /**
	 * CRꆼꆼꆼ.
	 */
    protected static final byte CR = 0x0d;

    /**
	 * LFꆼꆼꆼ.
	 */
    protected static final byte LF = 0x0a;

    /**
	 * USER ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_USER = "USER";

    /**
	 * PASS ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_PASS = "PASS";

    /**
	 * QUIT ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_QUIT = "QUIT";

    /**
	 * STAT ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_STAT = "STAT";

    /**
	 * LIST ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_LIST = "LIST";

    /**
	 * RETR ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_RETR = "RETR";

    /**
	 * DELE ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_DELE = "DELE";

    /**
	 * NOOP ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_NOOP = "NOOP";

    /**
	 * RSET ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_RSET = "RSET";

    /**
	 * APOP ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_APOP = "APOP";

    /**
	 * TOP ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_TOP = "TOP";

    /**
	 * UIDL ꆼꆼꆼꆼ.
	 */
    protected static final String COMMAND_UIDL = "UIDL";

    /**
	 * ꆼꆼꆼꆼ.
	 */
    protected static final String RESPONSE_OK = "+OK";

    /**
	 * ꆼꆼꆼꆼꆼ.
	 */
    protected static final String RESPONSE_ERR = "-ERR";

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼ(USER/PASS ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 *
	 * @see #setAuthorizationMode(int)
	 */
    public static final int NORMAL_AUTHORIZATION = 1;

    /**
	 * APOPꆼꆼꆼꆼꆼ.
	 *
	 * @see #setAuthorizationMode(int)
	 */
    public static final int APOP_AUTHORIZATION = 2;

    /**
	 * @serial	POPꆼꆼꆼꆼ. ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private String user;

    /**
	 * ꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ transient ꆼꆼꆼ.
	 */
    private transient String password;

    /**
	 * @serial	POPꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private String host;

    /**
	 * @serial	POPꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private int port = -1;

    /**
	 * @serial	ꆼꆼꆼꆼꆼ. ꆼꆼꆼꆼꆼꆼ NORMAL_AUTHORIZATION
	 */
    private int authorizationMode = NORMAL_AUTHORIZATION;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected transient Socket socket;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ Stream.
	 */
    protected transient InputStream in;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ Stream.
	 */
    protected transient OutputStream out;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected transient boolean connected = false;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private transient byte responseBytes[];

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private transient String status;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ(greeting).
	 */
    private transient String greeting;

    /**
	 * LIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected transient String messageList[] = null;

    /**
	 * UIDL ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected transient String uidList[] = null;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected transient POP3MessageInputStream messageInput = null;

    /**
	 * @serial ꆼꆼꆼꆼꆼꆼꆼ.
	 */
    private boolean debug;

    /**
	 * @serial	ꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 */
    protected int timeout;

    /**
	 * @serial	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 */
    private String preConnectCommand;

    /**
	 * 是否使用ssl
	 */
    private boolean ssl;

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼPOP3Clientꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public POP3Client() {
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ POP3Clientꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	host	ꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalArgumentException	host ꆼ null ꆼꆼꆼ.
	 */
    public POP3Client(String host) {
        this(host, DEFAULT_POP3_PORT);
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ POP3Clientꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	host	ꆼꆼꆼꆼ.
	 * @param	port	ꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalArgumentException	host ꆼ null ꆼꆼꆼ.
	 */
    public POP3Client(String host, int port) {
        setHost(host);
        setPort(port);
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼSystem.err ꆼꆼꆼ.
	 *
	 * @param	msg	ꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected void debugOut(String msg) {
        if (debug) {
            StringBuffer buff = new StringBuffer("DEBUG:");
            buff.append(msg);
            System.err.println(buff.toString());
        }
    }

    public void setDebug(boolean mode) {
        this.debug = mode;
    }

    public boolean getDebug() {
        return debug;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * CRLF ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 *          ꆼꆼꆼꆼꆼꆼꆼCRLFꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized String readResponseLine() throws IOException {
        boolean cr = false;
        int i;
        for (i = 0; i < responseBytes.length; ++i) {
            int c = in.read();
            if (c == -1) {
                break;
            }
            responseBytes[i] = (byte) c;
            if (responseBytes[i] == CR) {
                cr = true;
            } else if (responseBytes[i] == LF && cr == true) {
                break;
            } else {
                cr = false;
            }
        }
        if (i == 0) return "+OK";
        return new String(responseBytes, 0, i - 1, ENCODE);
    }

    /**
	 * ꆼꆼꆼꆼꆼTCP/IPꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 *
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized void openConnection() throws IOException {
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }
        String cmd = getPreConnectCommand();
        if (cmd != null) {
            debugOut("Execute preconnect command:" + cmd);
            try {
                Runtime runtime = Runtime.getRuntime();
                Process process = runtime.exec(cmd);
                process.waitFor();
                debugOut("Preconnect command done.");
            } catch (InterruptedException ie) {
                throw new InterruptedIOException(ie.getMessage());
            }
        }
        if (ssl) {
            socket = SSLSocketFactory.getDefault().createSocket(getHost(), (getPort() == -1) ? DEFAULT_POP3_PORT : getPort());
        } else {
            socket = new Socket(getHost(), (getPort() == -1) ? DEFAULT_POP3_PORT : getPort());
        }
        socket.setSoTimeout(getTimeout());
        in = new BufferedInputStream(socket.getInputStream());
        out = new BufferedOutputStream(socket.getOutputStream());
        if (responseBytes == null) {
            responseBytes = new byte[MAX_LINE_LENGTH];
        }
        status = readResponseLine();
        greeting = status;
        connected = true;
        debugOut("S:" + greeting);
    }

    /**
	 * ꆼꆼꆼꆼꆼTCP/IPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @exception java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized void closeConnection() throws IOException {
        try {
            socket.close();
        } finally {
            socket = null;
            in = null;
            out = null;
            status = null;
            greeting = null;
            connected = false;
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	command	ꆼꆼꆼꆼꆼꆼꆼ.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼOKꆼꆼꆼꆼꆼ true. ꆼꆼꆼꆼꆼꆼꆼ false.
	 * @exception java.io.IOException IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized boolean sendCommand(String command) throws IOException {
        if (messageInput != null) {
            messageInput.close();
            messageInput = null;
        }
        debugOut("C:" + command);
        byte data[] = command.getBytes(ENCODE);
        out.write(data, 0, data.length);
        out.write(CR);
        out.write(LF);
        out.flush();
        status = readResponseLine();
        debugOut("S:" + status);
        return isStatusOK();
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼ LIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 *
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3NegativeResponseException	LISTꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized void initList() throws POP3NegativeResponseException, IOException {
        if (sendCommand(COMMAND_LIST)) {
            Vector tmplist = new Vector();
            String line = readResponseLine();
            while (!".".equals(line)) {
                tmplist.addElement(line);
                line = readResponseLine();
            }
            messageList = new String[tmplist.size()];
            tmplist.copyInto(messageList);
        } else {
            messageList = null;
            throw new POP3NegativeResponseException(getStatusString());
        }
    }

    /**
	 * UID(unique-id)ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼ UIDL ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 *
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3NegativeResponseException	LISTꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized void initUIDList() throws POP3NegativeResponseException, IOException {
        if (sendCommand(COMMAND_UIDL)) {
            Vector tmplist = new Vector();
            String line = readResponseLine();
            while (!".".equals(line)) {
                tmplist.addElement(line);
                line = readResponseLine();
            }
            uidList = new String[tmplist.size()];
            tmplist.copyInto(uidList);
        } else {
            uidList = null;
            throw new POP3NegativeResponseException(getStatusString());
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ LIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * <UL>
	 *   3  4231<BR>
	 * </UL>
	 *
	 * @param	pos	LISTꆼꆼꆼꆼ.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized String getListItem(int pos) throws IOException {
        if (messageList == null) {
            initList();
        }
        return new String(messageList[pos]);
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ LIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * <UL>
	 *   <CODE>
	 *   String ret[] = client.getListItems();<BR>
	 *   ret[0] -> "1 23435"<BR>
	 *   ret[1] -> "2 4352"<BR>
	 *   ret[2] -> "3 45454"<BR>
	 *   </CODE>
	 * </UL>
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    protected synchronized String[] getListItems() throws IOException {
        if (messageList == null) {
            initList();
        }
        String ret[] = new String[messageList.length];
        for (int i = 0; i < ret.length; ++i) {
            ret[i] = new String(messageList[i]);
        }
        return ret;
    }

    /**
	 * POP ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼIllegalStateExceptionꆼthrowꆼꆼꆼ.
	 *
	 * @param	user	ꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized void setUser(String user) {
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }
        this.user = user;
    }

    /**
	 * POP ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	POPꆼꆼꆼꆼ.
	 */
    public synchronized String getUser() {
        return user;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * nullꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	password	ꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized void setPassword(String password) {
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }
        if (password == null) {
            password = "";
        }
        this.password = password;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼ.
	 */
    public synchronized String getPassword() {
        return password;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	mode	ꆼꆼꆼꆼꆼ.<BR>
	 *                  NORMAL_AUTHORIZATION ꆼꆼꆼ APOP_AUTHORIZATION ꆼꆼꆼꆼꆼ.
	 */
    public void setAuthorizationMode(int mode) {
        authorizationMode = mode;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼ. NORMAL_AUTHORIZATION ꆼꆼꆼ APOP_AUTHORIZATION.
	 */
    public int getAuthorizationMode() {
        return authorizationMode;
    }

    /**
	 * POPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	host	POPꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalArgumentException	host ꆼ null ꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public void setHost(String host) {
        if (host == null) {
            throw new IllegalArgumentException("host == null.");
        }
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }
        this.host = host;
    }

    /**
	 * POPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	POPꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized String getHost() {
        return host;
    }

    /**
	 * POPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	port	POPꆼꆼꆼꆼꆼꆼꆼꆼ. -1 ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalArgumentException	port < -1 ꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized void setPort(int port) {
        if (port < -1) {
            throw new IllegalArgumentException("port < -1.");
        }
        if (isConnected()) {
            throw new IllegalStateException("Already connected.");
        }
        this.port = port;
    }

    /**
	 * POPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	POPꆼꆼꆼꆼꆼꆼꆼꆼ. -1 ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized int getPort() {
        return port;
    }

    /**
	 * greeting(ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ)ꆼꆼꆼ.
	 * isConnected() == true ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼ APOP ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ digest ꆼꆼꆼꆼꆼ.
	 *
	 * @return	greeting.
	 */
    protected String getGreeting() {
        return greeting;
    }

    /**
	 * APOP ꆼ digest ꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	digest ꆼꆼꆼ.
	 */
    private String createDigest() {
        StringBuffer buff = new StringBuffer();
        try {
            String key = "";
            String g = getGreeting();
            int spos = g.indexOf('<');
            int epos = g.indexOf('>');
            if (spos != -1 && epos != -1 && spos < epos) {
                key = g.substring(spos, epos + 1);
            }
            key = key + getPassword();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(ENCODE));
            for (int i = 0; i < digest.length; ++i) {
                char c = (char) ((digest[i] >> 4) & 0xf);
                if (c > 9) {
                    c = (char) ((c - 10) + 'a');
                } else {
                    c = (char) (c + '0');
                }
                buff.append(c);
                c = (char) (digest[i] & 0xf);
                if (c > 9) {
                    c = (char) ((c - 10) + 'a');
                } else {
                    c = (char) (c + '0');
                }
                buff.append(c);
            }
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        }
        return buff.toString();
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * TCP/IPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼ.
	 *
	 * @exception	POP3AuthenticationFailedException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.IllegalStateException	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized void connect() throws POP3AuthenticationFailedException, IOException {
        openConnection();
        boolean authorized = false;
        if (isStatusOK()) {
            if (getAuthorizationMode() == APOP_AUTHORIZATION) {
                String digest = createDigest();
                if (sendCommand(COMMAND_APOP + ' ' + getUser() + ' ' + digest)) {
                    authorized = true;
                }
            } else {
                if (sendCommand(COMMAND_USER + ' ' + getUser()) && sendCommand(COMMAND_PASS + ' ' + getPassword())) {
                    authorized = true;
                }
            }
        }
        if (!authorized) {
            String stat = getStatusString();
            closeConnection();
            throw new POP3AuthenticationFailedException(stat);
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * QUITꆼꆼꆼꆼꆼꆼꆼꆼꆼTCP/IPꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized void disconnect() throws POP3Exception, IOException {
        if (!isConnected()) {
            return;
        }
        try {
            if (sendCommand(COMMAND_QUIT)) {
                closeConnection();
            } else {
                throw new POP3NegativeResponseException(getStatusString());
            }
        } finally {
            messageList = null;
            uidList = null;
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼtrue.
	 */
    public synchronized boolean isConnected() {
        return connected;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized String getStatusString() {
        return status;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	OKꆼꆼꆼꆼꆼ true.
	 */
    public synchronized boolean isStatusOK() {
        if (status != null && status.startsWith(RESPONSE_OK)) {
            return true;
        }
        return false;
    }

    /**
	 * ꆼꆼꆼꆼ STAT ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ true.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized boolean stat() throws POP3Exception, IOException {
        return sendCommand(COMMAND_STAT);
    }

    /**
	 * ꆼꆼꆼꆼ NOOP ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ true.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized boolean noop() throws POP3Exception, IOException {
        return sendCommand(COMMAND_NOOP);
    }

    /**
	 * ꆼꆼꆼꆼ RSET ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ true.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized boolean rset() throws POP3Exception, IOException {
        return sendCommand(COMMAND_RSET);
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * STAT ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼLIST ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼ.
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized int getMessageCount() throws POP3Exception, IOException {
        if (messageList == null) {
            initList();
        }
        return messageList.length;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	pos	ꆼꆼꆼꆼꆼꆼꆼ.ꆼꆼꆼꆼꆼꆼꆼꆼꆼ0.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼ.(ꆼꆼꆼꆼꆼꆼꆼꆼ.ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ-1ꆼꆼꆼ.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized int getMessageSize(int pos) throws POP3Exception, IOException {
        String item = getListItem(pos);
        StringTokenizer st = new StringTokenizer(item);
        String num = st.nextToken();
        if (st.countTokens() > 0) {
            String size = st.nextToken();
            return Integer.parseInt(size);
        } else {
            return -1;
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼPOPꆼꆼꆼꆼꆼRETRꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 * <OL>
	 *  <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ (CRLF.CRLF ꆼꆼꆼꆼꆼꆼꆼꆼ.CRLF ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ)ꆼ
	 *  <LI>ꆼꆼꆼꆼꆼꆼꆼꆼꆼ (CRLF.. -> CRLF. ꆼꆼꆼꆼꆼ)
	 * </OL>
	 *
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼclose() ꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	pos	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ 0.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼ InputStream.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized InputStream retrieve(int pos) throws POP3Exception, IOException {
        if (messageInput != null) {
            messageInput.close();
            messageInput = null;
        }
        if (messageList == null) {
            initList();
        }
        StringTokenizer tokenizer = new StringTokenizer(getListItem(pos));
        String num = tokenizer.nextToken();
        if (sendCommand(COMMAND_RETR + " " + num)) {
            messageInput = new POP3MessageInputStream(in);
            return messageInput;
        } else {
            throw new POP3NegativeResponseException(getStatusString());
        }
    }

    /**
	 * TOP ꆼꆼꆼꆼ. ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ InputStream ꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	pos	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ 0.
	 * @param	number	ꆼꆼꆼꆼꆼꆼ.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼ InputStream.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized InputStream top(int pos, int number) throws POP3NegativeResponseException, IOException {
        if (messageInput != null) {
            messageInput.close();
            messageInput = null;
        }
        if (messageList == null) {
            initList();
        }
        StringTokenizer tokenizer = new StringTokenizer(getListItem(pos));
        String num = tokenizer.nextToken();
        if (sendCommand(COMMAND_TOP + ' ' + num + ' ' + number)) {
            messageInput = new POP3MessageInputStream(in);
            return messageInput;
        } else {
            throw new POP3NegativeResponseException(getStatusString());
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	pos	ꆼꆼꆼꆼꆼꆼꆼ.ꆼꆼꆼꆼꆼꆼꆼꆼꆼ 0.
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ true.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized boolean delete(int pos) throws POP3NegativeResponseException, IOException {
        if (messageList == null) {
            initList();
        }
        StringTokenizer tokenizer = new StringTokenizer(getListItem(pos));
        String num = tokenizer.nextToken();
        return sendCommand(COMMAND_DELE + " " + num);
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼUID(unique-id)ꆼꆼꆼ.
	 *
	 * @param	pos	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @return	UID.
	 * @exception	jp.gr.java_conf.roadster.net.pop.POP3Exception	POP3ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public synchronized String getUID(int pos) throws POP3Exception, IOException {
        if (uidList == null) {
            initUIDList();
        }
        String item = uidList[pos];
        StringTokenizer st = new StringTokenizer(item);
        String num = st.nextToken();
        String uid = st.nextToken();
        return uid;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 *
	 * @param	timeout	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 */
    public synchronized void setTimeout(int timeout) {
        this.timeout = timeout;
        if (socket != null) {
            try {
                socket.setSoTimeout(timeout);
            } catch (SocketException se) {
                se.printStackTrace();
            }
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 */
    public synchronized int getTimeout() {
        return timeout;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 *
	 * @param	command	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 */
    public synchronized void setPreConnectCommand(String command) {
        this.preConnectCommand = command;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 *
	 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼnullꆼ
	 */
    public String getPreConnectCommand() {
        return preConnectCommand;
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * CRLF.CRLFꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
	 * CRLF..CRLF ꆼ CRLF.CRLFꆼꆼꆼꆼꆼ.
	 */
    protected class POP3MessageInputStream extends InputStream {

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼ.
		 */
        private final int PRE_READ_LENGTH = 4;

        private byte tmpbuff[] = new byte[PRE_READ_LENGTH];

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼPushbackInputStream ꆼꆼꆼꆼꆼ.
		 */
        protected PushbackInputStream pin;

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
		 */
        protected boolean finalByte = false;

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
		 */
        protected boolean eof = false;

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼInputStreamꆼꆼꆼꆼꆼ.
		 * @param	in	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
		 *              ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 */
        public POP3MessageInputStream(InputStream in) {
            this.pin = new PushbackInputStream(in, PRE_READ_LENGTH * 2);
        }

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 * RFC1939ꆼꆼꆼꆼꆼCRLF.CRLF ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.<BR>
		 * ꆼꆼꆼCRLF.. ꆼꆼ CRLF. ꆼꆼꆼꆼꆼꆼ.
		 *
		 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ .CRLF ꆼꆼꆼꆼꆼꆼ.
		 *          ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ -1.
		 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 */
        public synchronized int read() throws IOException {
            int ret;
            if (eof == true) {
                return -1;
            } else {
                ret = pin.read();
                if (ret == -1 || finalByte == true) {
                    eof = true;
                    return ret;
                }
            }
            byte b = (byte) ret;
            if (b == CR) {
                for (int i = 0; i < tmpbuff.length; ++i) {
                    int c = pin.read();
                    if (c == -1) {
                        throw new EOFException();
                    }
                    tmpbuff[i] = (byte) c;
                }
                boolean needsUnread = true;
                if (tmpbuff[0] == LF && tmpbuff[1] == 46) {
                    if (tmpbuff[2] == CR && tmpbuff[3] == LF) {
                        finalByte = true;
                        pin.unread(LF);
                        needsUnread = false;
                    } else if (tmpbuff[2] == 46) {
                        pin.unread(tmpbuff[3]);
                        pin.unread(46);
                        pin.unread(LF);
                        needsUnread = false;
                    }
                }
                if (needsUnread == true) {
                    pin.unread(tmpbuff, 0, tmpbuff.length);
                }
            }
            return ret;
        }

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼ close() ꆼꆼ.
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 *
		 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 */
        public void close() throws IOException {
            int i;
            while ((i = read()) != -1) {
            }
            messageInput = null;
        }

        /**
		 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 *
		 * @return	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 * @exception	java.io.IOException	IOꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
		 */
        public synchronized int available() throws IOException {
            return pin.available();
        }
    }

    /**
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼPOPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ
	 * ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 *
	 * @param	args	ꆼꆼꆼꆼꆼPOPꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 * @exception	java.lang.Exception	ꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼꆼ.
	 */
    public static void main(String args[]) throws Exception {
        POP3Client pop = new POP3Client();
        pop.setDebug(true);
        pop.setHost(args[0]);
        pop.setUser(args[1]);
        pop.setPassword(args[2]);
        if (args.length >= 4 && "APOP".equals(args[3])) {
            pop.setAuthorizationMode(POP3Client.APOP_AUTHORIZATION);
        }
        pop.connect();
        int count = pop.getMessageCount();
        System.out.println("Message count = " + count);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (int i = 0; i < count; ++i) {
            int messageSize = pop.getMessageSize(i);
            System.out.println("Message size = " + messageSize + " octet.");
            System.out.println("Message UID  = " + pop.getUID(i) + ".");
            InputStream in = pop.retrieve(i);
            int c;
            int bytesRead = 0;
            while ((c = in.read()) != -1) {
                System.out.print((char) c);
                bytesRead++;
            }
            if (bytesRead == messageSize) {
                System.out.println("bytesRead == messageSize");
            } else {
                System.out.println("bytesRead != messageSize");
                System.out.println("\tbytesRead = " + bytesRead);
                System.out.println("\tmessageSize = " + messageSize);
            }
            System.out.flush();
            System.err.print("Enter TOP command line number > ");
            String line = br.readLine();
            in = pop.top(i, Integer.parseInt(line));
            while ((c = in.read()) != -1) {
                System.out.print((char) c);
            }
            System.err.print("Delete this message ? (y/N) >");
            System.err.flush();
            line = br.readLine();
            if ("y".equals(line)) {
                pop.delete(i);
            }
        }
        pop.disconnect();
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }
}
