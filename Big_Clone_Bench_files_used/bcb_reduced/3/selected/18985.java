package com.mindbright.ssh2;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.io.IOException;
import java.io.FileOutputStream;
import com.mindbright.jca.security.MessageDigest;
import com.mindbright.jca.security.SecureRandom;
import com.mindbright.jca.security.InvalidKeyException;
import com.mindbright.jce.crypto.Cipher;
import com.mindbright.jce.crypto.Mac;
import com.mindbright.jce.crypto.spec.SecretKeySpec;
import com.mindbright.jce.crypto.spec.IvParameterSpec;
import com.mindbright.jce.crypto.ShortBufferException;
import com.mindbright.util.SecureRandomAndPad;
import com.mindbright.util.Queue;
import com.mindbright.util.Log;

/**
 * This class implements the transport layer of the secure shell version 2
 * (ssh2) protocol stack. It handles the initial negotiation of algorithms for
 * key exchange, host key type, encryption, message authentication and
 * compression. It also handles server authentication (through the provided
 * <code>SSH2TransportEventHandler</code>).
 * <p>
 * To create a <code>SSH2Transport</code> instance a TCP connection to the ssh2
 * server must first be established using a <code>java.net.Socket</code>. This
 * socket is passed to the constructor. The constructor is passive in that it
 * doesn't start any communication. To start the protocol engine and begin
 * communcation with the server the method <code>boot</code> must be called. In
 * this method the version information echange is done and two threads are
 * started which handles the protocol engine and all communication with the
 * server.
 * <p>
 * The operation of the transport layer can be controlled and monitored with
 * instances of the classes <code>SSH2Preferences</code> and
 * <code>SSH2TransportEventHandler</code> which are provided to the constructor.
 * <p>
 * After initial negotiation the chosen key exchange algorithm is handled by a
 * subclass of the abstract class <code>SSH2KeyExchanger</code>. When the key
 * exchange is complete, keys for encryption and message authentication can be
 * derived.  The communciation at this point is secured with the selected
 * cipher/mac algorithms. The actual encryption, message authentication and
 * compression is handled in the class <code>SSH2TransportPDU</code> which is
 * the data container implementing the specific formatting defined in the
 * protocol.
 * <p>
 * Before the upper layers (i.e. the user authentication and connection layers)
 * of the protocol can be started the key exchange stage must be completed. This
 * must be checked by calling the blocking method
 * <code>waitForKEXComplete</code>. When the key exchange is complete a secure
 * connection to an authenticated server has been established. The function of
 * the transport layer at this point is the multiplexing of protocol data units
 * (referred to as PDU or packet) between the server and the upper layers which
 * are implemented in the classes <code>SSH2UserAuth</code> and
 * <code>SSH2Connection</code>.
 *
 * @see SSH2TransportEventHandler
 * @see SSH2Preferences
 * @see SSH2UserAuth
 * @see SSH2Connection
 */
public final class SSH2Transport {

    private class KeepAliveThread implements Runnable {

        private volatile int interval;

        private volatile boolean keepRunning;

        protected KeepAliveThread(int interval) {
            this.interval = interval;
            this.keepRunning = true;
            Thread heartbeat = new Thread(this, "SSH2TransportKeepAlive");
            heartbeat.setDaemon(true);
            heartbeat.setPriority(Thread.MIN_PRIORITY);
            heartbeat.start();
        }

        protected synchronized void setInterval(int interval) {
            if (interval < 1) {
                stop();
            } else {
                this.interval = interval;
            }
        }

        public void run() {
            int inactive = 0;
            while (keepRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (activity) {
                    inactive = 0;
                    activity = false;
                } else {
                    if (++inactive >= interval) {
                        sendIgnore("heartbeat".getBytes());
                        Thread.yield();
                        inactive = 0;
                        activity = false;
                    }
                }
            }
        }

        protected void stop() {
            keepRunning = false;
        }

        public boolean isRunning() {
            return keepRunning;
        }
    }

    /**
     * Context for transport tranceiver/receiver. Holds the encryption
     * and compression states.
     */
    public static class TranceiverContext {

        protected Mac mac;

        protected Cipher cipher;

        protected SSH2Compressor compressor;

        public int getCipherBlockSize() {
            return cipher.getBlockSize();
        }

        public void init(byte[] cKey, byte[] iv, byte[] mKey, int compLevel, boolean transmitter) throws SSH2Exception {
            try {
                if (cipher != null) {
                    cipher.init(transmitter ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, new SecretKeySpec(cKey, cipher.getAlgorithm()), new IvParameterSpec(iv));
                }
                if (mac != null) {
                    mac.init(new SecretKeySpec(mKey, mac.getAlgorithm()));
                }
                if (compressor != null) {
                    compressor.init((transmitter ? SSH2Compressor.COMPRESS_MODE : SSH2Compressor.UNCOMPRESS_MODE), compLevel);
                }
            } catch (InvalidKeyException e) {
                throw new SSH2FatalException("Invalid key in TranceiverContext.init");
            }
        }
    }

    private static final boolean DEBUG_ALL_TX = false;

    private static final boolean DEBUG_ALL_RX = false;

    private boolean weAreAServer;

    private String clientVersion;

    private String serverVersion;

    private SSH2Preferences ourPrefs;

    private SSH2Preferences peerPrefs;

    private SSH2TransportEventHandler eventHandler;

    private SSH2KeyExchanger keyExchanger;

    private volatile SSH2UserAuth userAuth;

    private volatile SSH2Connection connection;

    private Log tpLog;

    protected Socket tpSocket;

    protected InputStream tpIn;

    protected OutputStream tpOut;

    private Thread transmitter;

    private Thread receiver;

    private Queue txQueue;

    private SecureRandomAndPad tpRand;

    private KeepAliveThread heartbeat;

    private byte[] sessionId;

    private volatile boolean keyExchangeInProgress;

    private boolean keyExchangeOk;

    private Object keyExchangeMonitor;

    private SSH2TransportPDU clientKEXINITPkt;

    private SSH2TransportPDU serverKEXINITPkt;

    private byte[] serverPublicKeyBlob;

    private TranceiverContext rxContext;

    private TranceiverContext txContext;

    private int rxSeqNum;

    private int txSeqNum;

    private int rxNumPacketsSinceKEX;

    private int txNumPacketsSinceKEX;

    private int rxNumBytesSinceKEX;

    private int txNumBytesSinceKEX;

    private static final int PACKETS_BEFORE_REKEY = 2147483647;

    private static final int BYTES_BEFORE_REKEY = 1073741824;

    private Object disconnectMonitor;

    private volatile boolean isConnected;

    private volatile boolean isTxUp;

    private volatile boolean isRxUp;

    private String disconnectMessage;

    private boolean activity;

    public boolean incompatibleSignature;

    public boolean incompatibleServiceAccept;

    public boolean incompatiblePublicKeyAuth;

    public boolean incompatibleHMACKeyLength;

    public boolean incompatiblePublicKeyUserId;

    public boolean incompatibleChannelOpenFail;

    public boolean incompatibleRijndael;

    public boolean incompatibleCantReKey;

    public boolean incompatibleBuggyChannelClose;

    /**
     * This is the basic constructor used when default preferences is ok and no
     * logging or event handling is needed.
     *
     * @param tpSocket the connection to the ssh2 server
     * @param rand the source of randomness for keys and padding
     */
    public SSH2Transport(Socket tpSocket, SecureRandomAndPad rand) {
        this(tpSocket, new SSH2Preferences(), rand);
    }

    /**
     * This is the basic constructor used when no logging or event handling is
     * needed.
     *
     * @param tpSocket the connection to the ssh2 server
     * @param prefs    the protocol preferences
     * @param rand     the source of randomness for keys and padding
     */
    public SSH2Transport(Socket tpSocket, SSH2Preferences prefs, SecureRandomAndPad rand) {
        this(tpSocket, prefs, null, rand);
    }

    /**
     * This is the constructor used when an event handler is needed but no
     * logging.
     *
     * @param tpSocket     the connection to the ssh2 server
     * @param prefs        the protocol preferences
     * @param eventHandler the event handler which receives callbacks
     * @param rand         the source of randomness for keys and padding
     */
    public SSH2Transport(Socket tpSocket, SSH2Preferences prefs, SSH2TransportEventHandler eventHandler, SecureRandomAndPad rand) {
        this(tpSocket, prefs, eventHandler, rand, new Log(prefs.getIntPreference(SSH2Preferences.LOG_LEVEL)));
        String logFile = prefs.getPreference(SSH2Preferences.LOG_FILE);
        if (logFile != null) {
            try {
                boolean append = "true".equals(prefs.getPreference(SSH2Preferences.LOG_APPEND));
                FileOutputStream log = new FileOutputStream(logFile, append);
                tpLog.setLogOutputStream(log);
            } catch (IOException e) {
                tpLog.error("SSH2Transport", "<init>", "could't open log file: " + e.getMessage());
            }
        }
    }

    /**
     * This is the constructor used when both an event handler and logging is
     * needed.
     *
     * @param tpSocket     the connection to the ssh2 server
     * @param prefs        the protocol preferences
     * @param eventHandler the event handler which receives callbacks
     * @param rand         the source of randomness for keys and padding
     * @param log          the log handler which receives all logs
     */
    public SSH2Transport(Socket tpSocket, SSH2Preferences prefs, SSH2TransportEventHandler eventHandler, SecureRandomAndPad rand, Log log) {
        this.disconnectMonitor = new Object();
        this.keyExchangeMonitor = new Object();
        this.isConnected = false;
        this.isTxUp = false;
        this.isRxUp = false;
        this.ourPrefs = prefs;
        this.eventHandler = (eventHandler != null ? eventHandler : new SSH2TransportEventAdapter());
        this.tpSocket = tpSocket;
        this.tpRand = rand;
        this.tpLog = log;
        SSH2TransportPDU.pktDefaultSize = ourPrefs.getIntPreference(SSH2Preferences.DEFAULT_PKT_SZ);
        SSH2TransportPDUPool.POOL_SIZE = ourPrefs.getIntPreference(SSH2Preferences.PKT_POOL_SZ);
        try {
            setSocketOptions(SSH2Preferences.SOCK_OPT_TRANSPORT, tpSocket);
            this.rxContext = SSH2TransportPDU.createTranceiverContext("none", "none", "none");
            this.txContext = SSH2TransportPDU.createTranceiverContext("none", "none", "none");
            this.tpIn = tpSocket.getInputStream();
            this.tpOut = tpSocket.getOutputStream();
        } catch (Exception e) {
        }
    }

    /**
     * Starts the protocol engine and begins communication with the server. It
     * completes the version negotiation and starts two threads which handles
     * the protocol engine and all communication with the server. The key
     * exchange is started here.
     *
     * @exception SSH2Exception if a fatal error occurs such as an I/O error or
     * a protocol mismatch.
     */
    public void boot() throws SSH2Exception {
        synchronized (disconnectMonitor) {
            if (isConnected) {
                throw new SSH2FatalException("Already booted");
            }
            isConnected = true;
        }
        try {
            negotiateVersion();
        } catch (IOException e) {
            throw new SSH2FatalException("I/O error in version negotiation", e);
        }
        transmitter = new Thread(new Runnable() {

            public void run() {
                transportTransmitLoop();
            }
        }, "SSH2TransportTX");
        txQueue = new Queue(ourPrefs.getIntPreference(SSH2Preferences.QUEUE_DEPTH), ourPrefs.getIntPreference(SSH2Preferences.QUEUE_HIWATER));
        transmitter.start();
        startKeyExchange();
        receiver = new Thread(new Runnable() {

            public void run() {
                transportReceiveLoop();
            }
        }, "SSH2TransportRX");
        receiver.start();
    }

    public void setSocketOptions(String desc, Socket sock) throws IOException {
        String prefix = SSH2Preferences.SOCK_OPT + desc;
        String val = ourPrefs.getPreference(prefix + "." + SSH2Preferences.SO_TCP_NODELAY);
        if (val != null) {
            sock.setTcpNoDelay(Boolean.valueOf(val).booleanValue());
        }
    }

    /**
     * Gets the session identifier calculated at key exchange as defined in the
     * protool spec.
     *
     * @return the session identifier as a byte array.
     */
    public byte[] getSessionId() {
        byte[] id = sessionId;
        if (!incompatiblePublicKeyUserId) {
            SSH2DataBuffer buf = new SSH2DataBuffer(sessionId.length + 4);
            buf.writeString(sessionId);
            id = buf.readRestRaw();
        }
        return id;
    }

    /**
     * Gets the PDU containing the key exchange initialization (KEXINIT) sent by
     * the client.
     *
     * @return the PDU containing the KEXINIT packet
     */
    public SSH2TransportPDU getClientKEXINITPDU() {
        return clientKEXINITPkt;
    }

    /**
     * Gets the PDU containing the key exchange initialization (KEXINIT) sent by
     * the server.
     *
     * @return the PDU containing the KEXINIT packet.
     */
    public SSH2TransportPDU getServerKEXINITPDU() {
        return serverKEXINITPkt;
    }

    /**
     * Gets the client's version string
     *
     * @return the client's version string
     */
    public String getClientVersion() {
        return clientVersion;
    }

    /**
     * Gets the server's version string.
     *
     * @return the server's version string.
     */
    public String getServerVersion() {
        return serverVersion;
    }

    /**
     * Gets our preferences.
     *
     * @return our preferences.
     */
    public SSH2Preferences getOurPreferences() {
        return ourPrefs;
    }

    /**
     * Gets the preferences peer want.
     *
     * @return peer's preferences.
     */
    public SSH2Preferences getPeerPreferences() {
        return peerPrefs;
    }

    /**
     * Sets the event handler to use.
     *
     * @param eventHandler the event handler to use.
     */
    public void setEventHandler(SSH2TransportEventHandler eventHandler) {
        if (eventHandler != null) {
            this.eventHandler = eventHandler;
        }
    }

    /**
     * Gets the event handler currently in use.
     *
     * @return the event handler currently in use.
     */
    public SSH2TransportEventHandler getEventHandler() {
        return eventHandler;
    }

    /**
     * Gets the log handler currently in use.
     *
     * @return the log handler currently in use.
     */
    public Log getLog() {
        return tpLog;
    }

    /**
     * Sets the log handler to use.
     *
     * @param the log handler to use
     */
    public void setLog(Log log) {
        tpLog = log;
    }

    /**
     * Checks whether we are a server or a client.
     *
     * @return a boolean indicating if we are a server or not.
     */
    public boolean isServer() {
        return weAreAServer;
    }

    /**
     * Gets the <code>SecureRandom</code> currently in use.
     *
     * @return the <code>SecureRandom</code> in use.
     */
    public SecureRandom getSecureRandom() {
        return tpRand;
    }

    /**
     * Gets the <code>SSH2Compressor</code> currently in use for the
     * receiver.
     *
     * @return the <code>SSH2Compressor</code> in use,
     * <code>null</code> if none.
     */
    public SSH2Compressor getRxCompressor() {
        return rxContext.compressor;
    }

    /**
     * Gets the <code>SSH2Compressor</code> currently in use for the
     * transmitter.
     *
     * @return the <code>SSH2Compressor</code> in use,
     * <code>null</code> if none.
     */
    public SSH2Compressor getTxCompressor() {
        return txContext.compressor;
    }

    /**
     * Sets the <code>SSH2UserAuth</code> to use for the user authenticaton
     * stage. The user authentication service is started from the class
     * <code>SSH2UserAuth</code> through the method <code>requestService</code>.
     *
     * @param userAuth the userAuth layer
     */
    public void setUserAuth(SSH2UserAuth userAuth) {
        this.userAuth = userAuth;
    }

    /**
     * Requests the given service (currently the only service defined is the
     * "ssh-userauth" service which starts the user authentication).
     *
     * @param service the name of the service to request
     */
    public void requestService(String service) {
        SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_SERVICE_REQUEST);
        pdu.writeString(service);
        transmit(pdu);
    }

    /**
     * Sets the <code>SSH2Connection</code> to use for the connection layer. All
     * actions after this stage are made through the connection layer.
     *
     * @param connection the connection layer
     */
    public void setConnection(SSH2Connection connection) {
        this.connection = connection;
    }

    /**
     * Starts a key exchange with the preferences set in the constructor. If a
     * key exchange is allready in progress this call have no effect.
     *
     * @exception SSH2Exception if a fatal error occurs.
     */
    public void startKeyExchange() throws SSH2Exception {
        this.startKeyExchange(ourPrefs);
    }

    /**
     * Starts a key exchange with the given preferences (i.e. change to new
     * preferences and negotiate these with the peer). If a key exchange is
     * allready in progress this call have no effect.
     *
     * @param newPrefs the new preferences to use
     *
     * @exception SSH2Exception if a fatal error occurs
     */
    public void startKeyExchange(SSH2Preferences newPrefs) throws SSH2Exception {
        synchronized (keyExchangeMonitor) {
            if (!keyExchangeInProgress) {
                if (incompatibleCantReKey && (peerPrefs != null)) {
                    throw new SSH2FatalException("Error, peer '" + (weAreAServer ? clientVersion : serverVersion) + "' doesn't support re-keying");
                }
                ourPrefs = newPrefs;
                keyExchangeInProgress = true;
                if (incompatibleRijndael) {
                    removeRijndael();
                }
                txQueue.disable();
                rxNumPacketsSinceKEX = 0;
                txNumPacketsSinceKEX = 0;
                rxNumBytesSinceKEX = 0;
                txNumBytesSinceKEX = 0;
                sendKEXINIT();
            }
        }
    }

    /**
     * Waits (blocks) for key exchange to complete (if not in progress returns
     * immediately).
     *
     * @return a boolean indicating if key exchange was successful or not.  */
    public boolean waitForKEXComplete() {
        synchronized (keyExchangeMonitor) {
            if (keyExchangeInProgress) {
                try {
                    keyExchangeMonitor.wait();
                } catch (InterruptedException e) {
                }
            }
            return keyExchangeOk;
        }
    }

    /**
     * Checks if key exchange is currently in progress.
     *
     * @return a boolean indicating if key exchange is in progress or not.
     */
    public boolean keyExchangeInProgress() {
        return keyExchangeInProgress;
    }

    /**
     * Checks if currently connected to a server.
     *
     * @return a boolean indicating if we are connected or not.
     */
    public boolean isConnected() {
        return isConnected;
    }

    /**
     * Gets the message describing why transport was disconnected. Useful when
     * an error occurs and we are not hooked up with an eventhandler to see it
     * (e.g. when an error occurs in the key exchange it's only reported as a
     * disconnect reason).
     *
     * @return the disconnect message or <code>null</code> if still connected
     */
    public String getDisconnectMessage() {
        String msg = null;
        synchronized (disconnectMonitor) {
            if (!isConnected) {
                msg = disconnectMessage;
            }
        }
        return msg;
    }

    /**
     * Sends an IGNORE packet (as defined in the protocol spec.) with the given
     * data as payload.
     *
     * @param data a byte array containing the payload
     */
    public void sendIgnore(byte[] data) {
        sendIgnore(data, 0, data.length);
    }

    /**
     * Sends an IGNORE packet (as defined in the protocol spec.) with the given
     * data as payload.
     *
     * @param data a byte array containing the payload
     * @param off  offset in <code>data</code> where payload starts
     * @param len  length of payload
     */
    public void sendIgnore(byte[] data, int off, int len) {
        SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_IGNORE);
        pdu.writeString(data, off, len);
        transmit(pdu);
    }

    /**
     * Sends a DEBUG packet (as defined in the protocol spec.) with the given
     * message.
     *
     * @param alwaysDisp boolean indicating whether this message must always be
     * displayed or not.
     * @param message    the debug message to send
     * @param language   the language tag to use
     */
    public void sendDebug(boolean alwaysDisp, String message, String language) {
        SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_DEBUG);
        pdu.writeBoolean(alwaysDisp);
        pdu.writeString(message);
        pdu.writeString(language);
        transmit(pdu);
    }

    /**
     * Enables keep-alive function which sends IGNORE packets on the given time
     * interval to prevent the connection from beeing timed out (e.g. because of
     * TCP connection subject to idle-timeouts in a firewall).
     *
     * @param intervalSeconds interval time in seconds
     */
    public void enableKeepAlive(int intervalSeconds) {
        if (heartbeat != null && heartbeat.isRunning()) {
            heartbeat.setInterval(intervalSeconds);
        } else if (intervalSeconds > 0) {
            heartbeat = new KeepAliveThread(intervalSeconds);
        }
    }

    /**
     * Disables keep-alive function.
     */
    public void disableKeepAlive() {
        if (heartbeat != null) {
            heartbeat.stop();
        }
        heartbeat = null;
    }

    private void kexComplete(boolean noError) {
        synchronized (keyExchangeMonitor) {
            keyExchangeInProgress = false;
            keyExchangeOk = noError;
            keyExchangeMonitor.notifyAll();
            if (noError) {
                eventHandler.kexComplete(this);
            }
        }
    }

    private void authTerminate() {
        if (userAuth != null) {
            userAuth.terminate();
        }
    }

    /**
     * Transmits the given PDU if we are connected. The PDU is put in a queue
     * which is processed by the internal threads hence this call is can only
     * block in extreme cases since all internal queues are subject to flow
     * control to prevent memory from beeing exhausted.
     *
     * @param pdu PDU to send
     */
    public void transmit(SSH2TransportPDU pdu) {
        if (isConnected) {
            txQueue.putLast(pdu);
        }
    }

    /**
     * Transmits the given PDU without checking if we are connected. This
     * version of transmit writes the PDU directly to the
     * <code>OutputStream</code> to the peer, hence it can only be used when
     * the transmitter is not running.
     *
     * @param pdu PDU to send
     *
     * @exception SSH2Exception if an I/O exception or other fatal error occurs
     */
    public synchronized void transmitInternal(SSH2TransportPDU pdu) throws SSH2Exception {
        if (DEBUG_ALL_TX) tpLog.debug2("SSH2Transport", "transmitInternal", "sending message of type: " + SSH2.msgTypeString(pdu.pktType), pdu.getData(), pdu.getPayloadOffset(), pdu.getPayloadLength());
        try {
            pdu.writeTo(tpOut, txSeqNum++, txContext, tpRand);
        } catch (ShortBufferException e) {
            throw new SSH2FatalException("Internal error/bug: " + e.getMessage());
        } catch (IOException e) {
            throw new SSH2FatalException("Couldn't write packet of type " + SSH2.msgTypeString(pdu.pktType), e);
        }
    }

    /**
     * Disconnects from peer using the DISCONNECT packet type with the given
     * reason and description. See the class <code>SSH2</code> for reason codes.
     *
     * @param reason      the reason code
     * @param description the textual description for the cause of disconnect
     *
     * @see SSH2
     */
    public void fatalDisconnect(int reason, String description) {
        disconnectInternal(reason, description, "", false);
    }

    /**
     * Disconnects from peer using the DISCONNECT packet type with the
     * reason code DISCONNECT_BY_APPLICATION and the given description.
     *
     * @param description the textual description for the cause of disconnect
     */
    public void normalDisconnect(String description) {
        disconnectInternal(SSH2.DISCONNECT_BY_APPLICATION, description, "", false);
    }

    private void disconnectInternal(int reason, String description, String languageTag, boolean fromPeer) {
        synchronized (disconnectMonitor) {
            if (!isConnected) {
                return;
            }
            isConnected = false;
            disconnectMessage = description;
        }
        if (fromPeer) {
            eventHandler.peerDisconnect(this, reason, description, languageTag);
            tpLog.warning("SSH2Transport", "disconnect: " + description);
        } else if (reason == SSH2.DISCONNECT_BY_APPLICATION) {
            eventHandler.normalDisconnect(this, description, languageTag);
            tpLog.notice("SSH2Transport", "disconnect: " + description);
        } else {
            eventHandler.fatalDisconnect(this, reason, description, languageTag);
            tpLog.error("SSH2Transport", "disconnectInternal", "disconnect: " + description);
        }
        if (!fromPeer && isTxUp) {
            txQueue.disable();
            SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_DISCONNECT);
            pdu.writeInt(reason);
            pdu.writeString(description);
            pdu.writeString("");
            try {
                transmitInternal(pdu);
            } catch (SSH2Exception e) {
                tpLog.message(Log.LEVEL_ERROR, "SSH2Transport", "disconnectInternal", "error writing disconnect msg: " + e);
            }
        }
        disableKeepAlive();
        shutdownRx();
        shutdownTx();
        if (connection != null) {
            connection.terminate();
        }
        tpLog.close();
    }

    private void negotiateVersion() throws IOException, SSH2Exception {
        String idString;
        String ourVersion = SSH2.getVersionId(ourPrefs.getPreference(SSH2Preferences.PKG_VERSION));
        if (weAreAServer) {
            serverVersion = ourVersion;
            idString = serverVersion + "\r\n";
            tpOut.write(idString.getBytes());
            tpOut.flush();
            clientVersion = idString;
            tpLog.notice("SSH2Transport", "peer's version is '" + clientVersion + "'");
        } else {
            clientVersion = ourVersion;
            idString = clientVersion + "\r\n";
            tpOut.write(idString.getBytes());
            tpOut.flush();
            while (!(idString = readIdString()).startsWith("SSH-")) {
                eventHandler.gotConnectInfoText(this, idString);
            }
            serverVersion = idString;
            tpLog.notice("SSH2Transport", "peer's version is '" + serverVersion + "'");
        }
        checkPeerVersion(clientVersion, serverVersion);
    }

    private void checkPeerVersion(String clientVersion, String serverVersion) throws SSH2Exception {
        String cliPackage = extractPackageVersion(clientVersion);
        String srvPackage = extractPackageVersion(serverVersion);
        int cliMajor = extractMajor(clientVersion);
        int cliMinor = extractMinor(clientVersion);
        int srvMajor = extractMajor(serverVersion);
        int srvMinor = extractMinor(serverVersion);
        if (weAreAServer) {
            eventHandler.gotPeerVersion(this, clientVersion, cliMajor, cliMinor, cliPackage);
        } else {
            eventHandler.gotPeerVersion(this, serverVersion, srvMajor, srvMinor, srvPackage);
        }
        if (cliMajor != srvMajor && !(srvMajor == 1 && srvMinor == 99)) {
            String msg;
            if (weAreAServer) {
                msg = "Can't serve a client with version " + clientVersion;
            } else {
                msg = "Can't connect to a server with version " + serverVersion;
            }
            throw new SSH2FatalException(msg);
        }
        String peerPackage = (weAreAServer ? cliPackage : srvPackage);
        if (peerPackage.startsWith("2.0.7 ") || peerPackage.startsWith("2.0.8 ") || peerPackage.startsWith("2.0.9 ")) {
            throw new SSH2FatalException("Peer's version is too old: " + peerPackage);
        }
        incompatibleServiceAccept = peerPackage.startsWith("2.0.11 ") || peerPackage.startsWith("2.0.12 ") || peerPackage.startsWith("2.0.13 ");
        incompatiblePublicKeyAuth = incompatibleServiceAccept;
        incompatibleChannelOpenFail = incompatibleServiceAccept;
        incompatibleSignature = peerPackage.startsWith("2.1.0 SSH") || (peerPackage.startsWith("2.1.0") && peerPackage.indexOf("F-SECURE") != -1) || incompatibleServiceAccept;
        incompatibleHMACKeyLength = incompatibleSignature || peerPackage.startsWith("2.2.0 SSH") || peerPackage.startsWith("2.3.0 SSH") || ((peerPackage.startsWith("2.2.0") || peerPackage.startsWith("2.3.0")) && peerPackage.indexOf("F-SECURE") != -1);
        incompatibleBuggyChannelClose = incompatibleHMACKeyLength || peerPackage.startsWith("2.4.0 SSH");
        incompatiblePublicKeyUserId = incompatibleSignature || peerPackage.startsWith("OpenSSH_2.0") || peerPackage.startsWith("OpenSSH_2.1") || peerPackage.startsWith("OpenSSH_2.2");
        incompatibleRijndael = peerPackage.startsWith("OpenSSH_2.5.1p1") || peerPackage.startsWith("OpenSSH_2.5.0") || peerPackage.startsWith("OpenSSH_2.3");
        incompatibleCantReKey = incompatiblePublicKeyUserId || peerPackage.startsWith("OpenSSH_2.3") || peerPackage.startsWith("OpenSSH_2.5.1") || peerPackage.startsWith("OpenSSH_2.5.2") || !("true".equals(ourPrefs.getPreference(SSH2Preferences.QUEUED_RX_CHAN)));
        if (incompatibleServiceAccept) {
            tpLog.notice("SSH2Transport", "enabling draft incompatible SERVICE_ACCEPT");
            tpLog.notice("SSH2Transport", "enabling draft incompatible publickey method");
            tpLog.notice("SSH2Transport", "enabling draft incompatible CHANNEL_OPEN_FAILURE");
        }
        if (incompatibleSignature) {
            tpLog.notice("SSH2Transport", "enabling draft incompatible signature format");
        }
        if (incompatibleHMACKeyLength) {
            tpLog.notice("SSH2Transport", "enabling rfc incompatible hmac key length");
        }
        if (incompatiblePublicKeyUserId) {
            tpLog.notice("SSH2Transport", "enabling draft incompatible session id for signature");
        }
        if (incompatibleRijndael) {
            tpLog.notice("SSH2Transport", "disabling aes/rijndael cipher, peer has buggy implementation");
        }
        if (incompatibleCantReKey) {
            tpLog.notice("SSH2Transport", "disabling key re-exchange, not implemented in peer");
        }
    }

    /**
     * Extracts the major version from a version string (as defined in the
     * protocol spec.)
     *
     * @param the full version string
     *
     * @return the major version number
     *
     * @exception SSH2Exception if there is a format error
     */
    public static int extractMajor(String versionStr) throws SSH2Exception {
        try {
            int r = versionStr.indexOf('.', 4);
            return Integer.parseInt(versionStr.substring(4, r));
        } catch (NumberFormatException e) {
            throw new SSH2FatalException("Corrupt version string: " + versionStr);
        }
    }

    /**
     * Extracts the minor version from a version string (as defined in the
     * protocol spec.)
     *
     * @param the full version string
     *
     * @return the minor version number
     *
     * @exception SSH2Exception if there is a format error
     */
    public static int extractMinor(String versionStr) throws SSH2Exception {
        try {
            int l = versionStr.indexOf('.', 4) + 1;
            int r = versionStr.indexOf('-', l);
            return Integer.parseInt(versionStr.substring(l, r));
        } catch (NumberFormatException e) {
            throw new SSH2FatalException("Corrupt version string: " + versionStr);
        }
    }

    /**
     * Extracts the package version (defined as softwareversion and comments in
     * the protocol spec.) from a version string.
     *
     * @param the full version string
     *
     * @return the package version (i.e. software version and comments)
     *
     * @exception SSH2Exception if there is a format error
     */
    public static String extractPackageVersion(String versionStr) throws SSH2Exception {
        try {
            int i = versionStr.indexOf('-', 4) + 1;
            return versionStr.substring(i);
        } catch (Exception e) {
            throw new SSH2FatalException("Corrupt version string: " + versionStr);
        }
    }

    private String readIdString() throws IOException, SSH2Exception {
        byte[] buf = new byte[256];
        int len = 0;
        int c;
        while (true) {
            c = tpIn.read();
            if (c == -1) {
                throw new SSH2EOFException("Server closed connection before sending identification");
            }
            if (c == '\r') continue;
            if (c != '\n') {
                if (len >= buf.length) {
                    throw new SSH2FatalException("Too long id string: " + new String(buf));
                }
                buf[len++] = (byte) c;
            } else {
                return new String(buf, 0, len);
            }
        }
    }

    private void sendKEXINIT() throws SSH2Exception {
        SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_KEXINIT);
        byte[] cookie = new byte[16];
        tpRand.nextBytes(cookie);
        pdu.writeRaw(cookie);
        ourPrefs.writeTo(pdu);
        pdu.writeBoolean(false);
        pdu.writeInt(0);
        if (weAreAServer) {
            serverKEXINITPkt = pdu.makeCopy();
        } else {
            clientKEXINITPkt = pdu.makeCopy();
        }
        transmitInternal(pdu);
        eventHandler.kexStart(this);
    }

    private void processKEXINIT(SSH2TransportPDU pdu) throws SSH2Exception {
        startKeyExchange();
        if (weAreAServer) {
            clientKEXINITPkt = pdu;
        } else {
            serverKEXINITPkt = pdu;
        }
        pdu.readRaw(16);
        peerPrefs = new SSH2Preferences();
        peerPrefs.readFrom(pdu);
        boolean firstKEXFollows = pdu.readBoolean();
        pdu.readInt();
        tpLog.info("SSH2Transport", "peer kex algorithms: " + peerPrefs.getPreference(SSH2Preferences.KEX_ALGORITHMS));
        tpLog.info("SSH2Transport", "peer host key algorithms: " + peerPrefs.getPreference(SSH2Preferences.HOST_KEY_ALG));
        tpLog.info("SSH2Transport", "peer enc. alg. cli2srv: " + peerPrefs.getPreference(SSH2Preferences.CIPHERS_C2S));
        tpLog.info("SSH2Transport", "peer enc. alg. srv2cli: " + peerPrefs.getPreference(SSH2Preferences.CIPHERS_S2C));
        tpLog.info("SSH2Transport", "peer mac alg. cli2srv: " + peerPrefs.getPreference(SSH2Preferences.MACS_C2S));
        tpLog.info("SSH2Transport", "peer mac alg. srv2cli: " + peerPrefs.getPreference(SSH2Preferences.MACS_S2C));
        tpLog.info("SSH2Transport", "peer comp. alg. cli2srv: " + peerPrefs.getPreference(SSH2Preferences.COMP_C2S));
        tpLog.info("SSH2Transport", "peer comp. alg. srv2cli: " + peerPrefs.getPreference(SSH2Preferences.COMP_S2C));
        tpLog.info("SSH2Transport", "our kex algorithms: " + ourPrefs.getPreference(SSH2Preferences.KEX_ALGORITHMS));
        tpLog.info("SSH2Transport", "our host key algorithms: " + ourPrefs.getPreference(SSH2Preferences.HOST_KEY_ALG));
        tpLog.info("SSH2Transport", "our enc. alg. cli2srv: " + ourPrefs.getPreference(SSH2Preferences.CIPHERS_C2S));
        tpLog.info("SSH2Transport", "our enc. alg. srv2cli: " + ourPrefs.getPreference(SSH2Preferences.CIPHERS_S2C));
        tpLog.info("SSH2Transport", "our mac alg. cli2srv: " + ourPrefs.getPreference(SSH2Preferences.MACS_C2S));
        tpLog.info("SSH2Transport", "our mac alg. srv2cli: " + ourPrefs.getPreference(SSH2Preferences.MACS_S2C));
        tpLog.info("SSH2Transport", "our comp. alg. cli2srv: " + ourPrefs.getPreference(SSH2Preferences.COMP_C2S));
        tpLog.info("SSH2Transport", "our comp. alg. srv2cli: " + ourPrefs.getPreference(SSH2Preferences.COMP_S2C));
        keyExchanger = ourPrefs.selectKEXAlgorithm(peerPrefs, weAreAServer);
        tpLog.info("SSH2Transport", "KEX algorithm chosen: " + ourPrefs.getAgreedKEXAlgorithm());
        tpLog.info("SSH2Transport", "same KEX guessed? " + ourPrefs.sameKEXGuess());
        tpLog.info("SSH2Transport", "first KEX follows? " + firstKEXFollows);
        if (!ourPrefs.canAgree(peerPrefs, weAreAServer)) {
            String errType = ourPrefs.getDisagreeType();
            String errtxt = "No match in kex params '" + errType + "', our's: " + ourPrefs.getPreference(errType) + ", peer's: " + peerPrefs.getPreference(errType);
            throw new SSH2FatalException(errtxt);
        }
        if (firstKEXFollows && !ourPrefs.sameKEXGuess()) {
            try {
                receiveInternal();
            } catch (IOException e) {
                throw new SSH2FatalException("I/O error when reading guessed " + "packet", e);
            } catch (ShortBufferException e) {
                throw new SSH2FatalException("Internal error/bug: " + e.getMessage());
            }
            tpLog.notice("SSH2Transport", "first KEX packet discarded, " + "wrong initial guess");
        }
        eventHandler.kexAgreed(this, ourPrefs, peerPrefs);
        keyExchanger.init(this);
    }

    private void removeRijndael() {
        boolean removedAES = false;
        String l1, l2;
        l1 = ourPrefs.getPreference(SSH2Preferences.CIPHERS_C2S);
        l2 = ourPrefs.getPreference(SSH2Preferences.CIPHERS_S2C);
        int l1l = l1.length();
        int l2l = l2.length();
        l1 = SSH2ListUtil.removeAllPrefixFromList(l1, "aes");
        l1 = SSH2ListUtil.removeAllPrefixFromList(l1, "rijndael");
        l2 = SSH2ListUtil.removeAllPrefixFromList(l2, "aes");
        l2 = SSH2ListUtil.removeAllPrefixFromList(l2, "rijndael");
        if (l1.length() != l1l) {
            ourPrefs.setPreference(SSH2Preferences.CIPHERS_C2S, l1);
            removedAES = true;
        }
        if (l2.length() != l2l) {
            ourPrefs.setPreference(SSH2Preferences.CIPHERS_S2C, l2);
            removedAES = true;
        }
        if (removedAES) {
            tpLog.warning("SSH2Transport", "removed AES cipher from our preferences" + " due to bug in peer's implementation");
        }
    }

    /**
     * Sends the NEWKEYS paket type and changes the transmitter keys according
     * to the current prefs (as negotiated before). Typically used from a
     * subclass to <code>SSH2KeyExchanger</code>.
     *
     * @exception SSH2Exception if an error occurs while sending the packet.
     */
    public void sendNewKeys() throws SSH2Exception {
        SSH2TransportPDU pdu = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_NEWKEYS);
        transmitInternal(pdu);
        changeKeys(true);
        txQueue.enable();
    }

    /**
     * Authenticates the server through its host key. Typically used from a
     * subclass to <code>SSH2KeyExchanger</code>.
     *
     * @param serverHostKey byte array containing server's host key (e.g. a
     * public key blob or a certificate).
     * @param serverSigH    byte array containing server's signature of the
     * exchange hash which should be verified.
     * @param the exchange hash
     *
     * @exception SSH2Exception if an error occurs
     */
    public void authenticateHost(byte[] serverHostKey, byte[] serverSigH, byte[] exchangeHash_H) throws SSH2Exception {
        tpLog.debug2("SSH2Transport", "authenticateHost", "Server's public host key: ", serverHostKey);
        tpLog.debug2("SSH2Transport", "authenticateHost", "Signature over H: ", serverSigH);
        tpLog.debug2("SSH2Transport", "authenticateHost", "Exchange hash H", exchangeHash_H);
        boolean verified = false;
        SSH2Signature signature = SSH2Signature.getInstance(ourPrefs.getAgreedHostKeyAlgorithm());
        signature.initVerify(serverHostKey);
        signature.setIncompatibility(this);
        verified = signature.verify(serverSigH, exchangeHash_H);
        if (verified) {
            tpLog.info("SSH2Transport", "server's signature verified");
        } else {
            String msg = "server's signature didn't verify";
            tpLog.error("SSH2Transport", "authenticateHost", msg);
            fatalDisconnect(SSH2.DISCONNECT_HOST_KEY_NOT_VERIFIABLE, msg);
            throw new SSH2FatalException(msg);
        }
        if (serverPublicKeyBlob == null) {
            if (!eventHandler.kexAuthenticateHost(this, signature)) {
                throw new SSH2SignatureException("Host authentication failed");
            }
            serverPublicKeyBlob = signature.getPublicKeyBlob();
        } else {
            byte[] blob = signature.getPublicKeyBlob();
            boolean equals = (blob.length == serverPublicKeyBlob.length);
            for (int i = 0; equals && i < blob.length; i++) {
                equals = (blob[i] == serverPublicKeyBlob[i]);
            }
            if (!equals) {
                disconnectInternal(SSH2.DISCONNECT_CONNECTION_LOST, "Server host key changed", "", false);
            }
        }
    }

    private void transportTransmitLoop() {
        isTxUp = true;
        tpLog.debug("SSH2Transport", "transportTransmitLoop", "starting");
        try {
            SSH2TransportPDU pdu;
            while ((pdu = (SSH2TransportPDU) txQueue.getFirst()) != null) {
                if (DEBUG_ALL_TX) tpLog.debug2("SSH2Transport", "transportTransmitLoop", "sending message of type: " + SSH2.msgTypeString(pdu.pktType), pdu.getData(), pdu.getPayloadOffset(), pdu.getPayloadLength());
                txNumPacketsSinceKEX++;
                txNumBytesSinceKEX += pdu.getPayloadLength();
                pdu.writeTo(tpOut, txSeqNum++, txContext, tpRand);
                activity = true;
                if (txNumPacketsSinceKEX >= PACKETS_BEFORE_REKEY || txNumBytesSinceKEX >= BYTES_BEFORE_REKEY) {
                    startKeyExchange();
                }
            }
        } catch (ShortBufferException e) {
            String msg = "Internal error/bug: " + e.getMessage();
            tpLog.error("SSH2Transport", "transportTransmitLoop", msg);
            disconnectInternal(SSH2.DISCONNECT_CONNECTION_LOST, msg, "", false);
        } catch (IOException e) {
            String msg = "I/O error: " + e.getMessage();
            if (isTxUp) {
                tpLog.error("SSH2Transport", "transportTransmitLoop", msg);
            }
            disconnectInternal(SSH2.DISCONNECT_CONNECTION_LOST, msg, "", false);
        } catch (SSH2CompressionException e) {
            String msg = "Internal error/bug: " + e.getMessage();
            tpLog.error("SSH2Transport", "transportTransmitLoop", msg);
            disconnectInternal(SSH2.DISCONNECT_COMPRESSION_ERROR, msg, "", false);
        } catch (SSH2Exception e) {
            String msg = "Key reexchange failed: " + e.getMessage();
            tpLog.error("SSH2Transport", "transportTransmitLoop", msg);
            disconnectInternal(SSH2.DISCONNECT_COMPRESSION_ERROR, msg, "", false);
        } finally {
            shutdownTx();
            kexComplete(false);
            authTerminate();
        }
        tpLog.debug("SSH2Transport", "transportTransmitLoop", "stopping");
    }

    private void transportReceiveLoop() {
        isRxUp = true;
        tpLog.debug("SSH2Transport", "transportReceiveLoop", "starting");
        try {
            while (isRxUp) {
                processRxPacket(receiveInternal());
            }
        } catch (ShortBufferException e) {
            String msg = "Internal error/bug: " + e.getMessage();
            disconnectInternal(SSH2.DISCONNECT_CONNECTION_LOST, msg, "", false);
        } catch (SSH2MacCheckException e) {
            String msg = e.getMessage();
            disconnectInternal(SSH2.DISCONNECT_MAC_ERROR, msg, "", false);
        } catch (SSH2CompressionException e) {
            String msg = e.getMessage();
            disconnectInternal(SSH2.DISCONNECT_COMPRESSION_ERROR, msg, "", false);
        } catch (SSH2SignatureException e) {
            String msg = e.getMessage();
            disconnectInternal(SSH2.DISCONNECT_KEY_EXCHANGE_FAILED, msg, "", false);
        } catch (SSH2Exception e) {
            if (isRxUp) {
                String msg = e.getMessage();
                if (e.getRootCause() != null) {
                    msg += " (rootcause: " + e.getRootCause() + ")";
                }
                disconnectInternal(SSH2.DISCONNECT_PROTOCOL_ERROR, msg, "", false);
            }
        } catch (IOException e) {
            if (isRxUp) {
                String msg = "I/O error: " + e.getMessage();
                disconnectInternal(SSH2.DISCONNECT_CONNECTION_LOST, msg, "", false);
            }
        } finally {
            shutdownRx();
            kexComplete(false);
            authTerminate();
        }
        tpLog.debug("SSH2Transport", "transportReceiveLoop", "stopping");
    }

    private void processRxPacket(SSH2TransportPDU pdu) throws ShortBufferException, IOException, SSH2Exception {
        rxNumPacketsSinceKEX++;
        rxNumBytesSinceKEX += pdu.getPayloadLength();
        switch(pdu.pktType) {
            case SSH2.MSG_DISCONNECT:
                {
                    int reason = pdu.readInt();
                    String description = pdu.readJavaString();
                    String languageTag = pdu.readJavaString();
                    disconnectInternal(reason, description, languageTag, true);
                    break;
                }
            case SSH2.MSG_IGNORE:
                byte[] data = pdu.readString();
                eventHandler.msgIgnore(this, data);
                break;
            case SSH2.MSG_UNIMPLEMENTED:
                int rejectedSeqNum = pdu.readInt();
                eventHandler.msgUnimplemented(this, rejectedSeqNum);
                break;
            case SSH2.MSG_DEBUG:
                {
                    boolean alwaysDisplay = pdu.readBoolean();
                    String message = pdu.readJavaString();
                    String languageTag = pdu.readJavaString();
                    eventHandler.msgDebug(this, alwaysDisplay, message, languageTag);
                    break;
                }
            case SSH2.MSG_SERVICE_REQUEST:
                break;
            case SSH2.MSG_SERVICE_ACCEPT:
                userAuth.processMessage(pdu);
                pdu = null;
                break;
            case SSH2.MSG_KEXINIT:
                processKEXINIT(pdu);
                pdu = null;
                break;
            case SSH2.MSG_NEWKEYS:
                if (!keyExchangeInProgress) throw new SSH2CorruptPacketException("Received MSG_NEWKEYS while not doing key exchange");
                changeKeys(false);
                kexComplete(true);
                break;
            case SSH2.FIRST_KEX_PACKET:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
            case 38:
            case 39:
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
            case 48:
            case SSH2.LAST_KEX_PACKET:
                if (!keyExchangeInProgress) throw new SSH2CorruptPacketException("Received KEX packet while not doing key exchange");
                keyExchanger.processKEXMethodPDU(pdu);
                break;
            case SSH2.MSG_USERAUTH_REQUEST:
            case SSH2.MSG_USERAUTH_FAILURE:
            case SSH2.MSG_USERAUTH_SUCCESS:
            case SSH2.MSG_USERAUTH_BANNER:
            case SSH2.FIRST_USERAUTH_METHOD_PACKET:
            case 61:
            case 62:
            case 63:
            case 64:
            case 65:
            case 66:
            case 67:
            case 68:
            case 69:
            case 70:
            case 71:
            case 72:
            case 73:
            case 74:
            case 75:
            case 76:
            case 77:
            case 78:
            case SSH2.LAST_USERAUTH_METHOD_PACKET:
                userAuth.processMessage(pdu);
                pdu = null;
                break;
            case SSH2.MSG_GLOBAL_REQUEST:
            case SSH2.MSG_REQUEST_SUCCESS:
            case SSH2.MSG_REQUEST_FAILURE:
            case SSH2.MSG_CHANNEL_OPEN:
                connection.processGlobalMessage(pdu);
                pdu = null;
                break;
            case SSH2.MSG_CHANNEL_DATA:
            case SSH2.MSG_CHANNEL_EXTENDED_DATA:
            case SSH2.MSG_CHANNEL_OPEN_CONFIRMATION:
            case SSH2.MSG_CHANNEL_OPEN_FAILURE:
            case SSH2.MSG_CHANNEL_WINDOW_ADJUST:
            case SSH2.MSG_CHANNEL_EOF:
            case SSH2.MSG_CHANNEL_CLOSE:
            case SSH2.MSG_CHANNEL_REQUEST:
            case SSH2.MSG_CHANNEL_SUCCESS:
            case SSH2.MSG_CHANNEL_FAILURE:
                connection.processChannelMessage(pdu);
                pdu = null;
                break;
            default:
                tpLog.warning("SSH2Transport", "received packet of unknown type: " + pdu.pktType);
                SSH2TransportPDU pduUnimp = SSH2TransportPDU.createOutgoingPacket(SSH2.MSG_UNIMPLEMENTED);
                pduUnimp.writeInt(rxSeqNum);
                if (keyExchangeInProgress) {
                    transmitInternal(pduUnimp);
                } else {
                    transmit(pduUnimp);
                }
                eventHandler.peerSentUnknownMessage(this, pdu.pktType);
                break;
        }
        if (pdu != null) {
            pdu.release();
        }
        if (rxNumPacketsSinceKEX >= PACKETS_BEFORE_REKEY || rxNumBytesSinceKEX >= BYTES_BEFORE_REKEY) {
            startKeyExchange();
        }
    }

    /**
     * Receives a PDU directly from the <code>InputStream</code> from the peer
     * without checking if we are connected. This method can only be used when
     * the receiver is not running.
     *
     * @return the PDU which was read
     *
     * @exception SSH2Exception
     * @exception ShortBufferException
     * @exception IOException
     */
    public SSH2TransportPDU receiveInternal() throws SSH2Exception, ShortBufferException, IOException {
        SSH2TransportPDU pdu = SSH2TransportPDU.createIncomingPacket();
        pdu.readFrom(tpIn, rxSeqNum++, rxContext);
        activity = true;
        if (DEBUG_ALL_RX) tpLog.debug2("SSH2Transport", "receiveInternal", "received message of type: " + SSH2.msgTypeString(pdu.pktType), pdu.getData(), pdu.getPayloadOffset(), pdu.getPayloadLength());
        return pdu;
    }

    private void shutdownTx() {
        if (isTxUp) {
            isTxUp = false;
            try {
                tpOut.close();
            } catch (IOException e) {
            }
            txQueue.disable();
            txQueue.setBlocking(false);
        }
    }

    private void shutdownRx() {
        if (isRxUp) {
            isRxUp = false;
            try {
                tpIn.close();
            } catch (IOException e) {
            }
        }
    }

    private synchronized void changeKeys(boolean transmitter) throws SSH2Exception {
        try {
            String cipherName = ourPrefs.getAgreedCipher(transmitter, weAreAServer);
            String macName = ourPrefs.getAgreedMac(transmitter, weAreAServer);
            String compName = ourPrefs.getAgreedCompression(transmitter, weAreAServer);
            int cKeyLen = ourPrefs.getCipherKeyLen(cipherName);
            int mKeyLen = ourPrefs.getMacKeyLen(macName);
            tpLog.info("SSH2Transport", "new " + (transmitter ? "transmitter" : "receiver") + " context (" + cipherName + "," + macName + "," + compName + ")");
            cipherName = ourPrefs.ssh2ToJCECipher(cipherName);
            macName = ourPrefs.ssh2ToJCEMac(macName);
            TranceiverContext ctx = SSH2TransportPDU.createTranceiverContext(cipherName, macName, compName);
            initTranceiverContext(ctx, cKeyLen, (incompatibleHMACKeyLength ? 16 : mKeyLen), transmitter);
            if (transmitter) {
                txContext = ctx;
            } else {
                rxContext = ctx;
            }
        } catch (Exception e) {
            throw new SSH2FatalException("Error in changeKeys", e);
        }
    }

    private void initTranceiverContext(TranceiverContext context, int ckLen, int mkLen, boolean transmitter) throws SSH2Exception {
        byte[] iv, cKey, mKey;
        char[] ids;
        if (weAreAServer ^ transmitter) {
            ids = new char[] { 'A', 'C', 'E' };
        } else {
            ids = new char[] { 'B', 'D', 'F' };
        }
        iv = deriveKey(ids[0], context.getCipherBlockSize());
        cKey = deriveKey(ids[1], ckLen);
        mKey = deriveKey(ids[2], mkLen);
        int compressionLevel = 6;
        try {
            compressionLevel = Integer.parseInt(ourPrefs.getPreference(SSH2Preferences.COMP_LEVEL));
        } catch (Exception e) {
            compressionLevel = 6;
        }
        context.init(cKey, iv, mKey, compressionLevel, transmitter);
    }

    byte[] deriveKey(char id, int len) {
        byte[] key = new byte[len];
        byte[] sharedSecret_K = keyExchanger.getSharedSecret_K();
        byte[] exchangeHash_H = keyExchanger.getExchangeHash_H();
        if (sessionId == null) {
            sessionId = new byte[exchangeHash_H.length];
            System.arraycopy(exchangeHash_H, 0, sessionId, 0, sessionId.length);
        }
        MessageDigest sha1 = keyExchanger.getExchangeHashAlgorithm();
        sha1.update(sharedSecret_K);
        sha1.update(exchangeHash_H);
        sha1.update(new byte[] { (byte) id });
        sha1.update(sessionId);
        byte[] material = sha1.digest();
        int curLen = material.length;
        System.arraycopy(material, 0, key, 0, (curLen < len ? curLen : len));
        while (curLen < len) {
            sha1.reset();
            sha1.update(sharedSecret_K);
            sha1.update(exchangeHash_H);
            sha1.update(key, 0, curLen);
            material = sha1.digest();
            if (len - curLen > material.length) System.arraycopy(material, 0, key, curLen, material.length); else System.arraycopy(material, 0, key, curLen, len - curLen);
            curLen += material.length;
        }
        tpLog.debug2("SSH2Transport", "deriveKey", "key id " + id, key);
        return key;
    }
}
