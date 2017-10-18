package org.asteriskjava.manager.internal;

import static org.asteriskjava.manager.ManagerConnectionState.CONNECTED;
import static org.asteriskjava.manager.ManagerConnectionState.CONNECTING;
import static org.asteriskjava.manager.ManagerConnectionState.DISCONNECTED;
import static org.asteriskjava.manager.ManagerConnectionState.DISCONNECTING;
import static org.asteriskjava.manager.ManagerConnectionState.INITIAL;
import static org.asteriskjava.manager.ManagerConnectionState.RECONNECTING;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.asteriskjava.AsteriskVersion;
import org.asteriskjava.manager.*;
import org.asteriskjava.manager.action.ChallengeAction;
import org.asteriskjava.manager.action.CommandAction;
import org.asteriskjava.manager.action.EventGeneratingAction;
import org.asteriskjava.manager.action.LoginAction;
import org.asteriskjava.manager.action.LogoffAction;
import org.asteriskjava.manager.action.ManagerAction;
import org.asteriskjava.manager.event.ConnectEvent;
import org.asteriskjava.manager.event.DisconnectEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.ProtocolIdentifierReceivedEvent;
import org.asteriskjava.manager.event.ResponseEvent;
import org.asteriskjava.manager.response.ChallengeResponse;
import org.asteriskjava.manager.response.CommandResponse;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.asteriskjava.util.DateUtil;
import org.asteriskjava.util.Log;
import org.asteriskjava.util.LogFactory;
import org.asteriskjava.util.SocketConnectionFacade;
import org.asteriskjava.util.internal.SocketConnectionFacadeImpl;
import org.asteriskjava.manager.action.UserEventAction;

/**
 * Internal implemention of the ManagerConnection interface.
 *
 * @author srt
 * @version $Id: ManagerConnectionImpl.java,v 1.4 2008/12/12 07:05:02 zacw Exp $
 * @see org.asteriskjava.manager.ManagerConnectionFactory
 */
public class ManagerConnectionImpl implements ManagerConnection, Dispatcher {

    protected static final int RECONNECTION_INTERVAL_1 = 50;

    protected static final int RECONNECTION_INTERVAL_2 = 5000;

    protected static final String DEFAULT_HOSTNAME = "localhost";

    protected static final int DEFAULT_PORT = 5038;

    protected static final int RECONNECTION_VERSION_INTERVAL = 500;

    protected static final int MAX_VERSION_ATTEMPTS = 4;

    protected static final AtomicLong idCounter = new AtomicLong(0);

    /**
     * Instance logger.
     */
    protected final Log logger = LogFactory.getLog(getClass());

    protected final long id;

    /**
     * Used to construct the internalActionId.
     */
    protected AtomicLong actionIdCounter = new AtomicLong(0);

    /**
     * Hostname of the Asterisk server to connect to.
     */
    protected String hostname = DEFAULT_HOSTNAME;

    /**
     * TCP port to connect to.
     */
    protected int port = DEFAULT_PORT;

    /**
     * <code>true</code> to use SSL for the connection, <code>false</code>
     * for a plain text connection.
     */
    protected boolean ssl = false;

    /**
     * The username to use for login as defined in Asterisk's
     * <code>manager.conf</code>.
     */
    protected String username;

    /**
     * The password to use for login as defined in Asterisk's
     * <code>manager.conf</code>.
     */
    protected String password;

    /**
     * The default timeout to wait for a ManagerResponse after sending a
     * ManagerAction.
     */
    protected long defaultResponseTimeout = 2000;

    /**
     * The default timeout to wait for the last ResponseEvent after sending an
     * EventGeneratingAction.
     */
    protected long defaultEventTimeout = 5000;

    /**
     * The timeout to use when connecting the the Asterisk server.
     */
    protected int socketTimeout = 0;

    /**
     * Closes the connection (and reconnects) if no input has been read for the given amount
     * of milliseconds. A timeout of zero is interpreted as an infinite timeout. 
     *
     * @see Socket#setSoTimeout(int)
     */
    protected int socketReadTimeout = 0;

    /**
     * <code>true</code> to continue to reconnect after an authentication failure.
     */
    protected boolean keepAliveAfterAuthenticationFailure = true;

    /**
     * The socket to use for TCP/IP communication with Asterisk.
     */
    protected SocketConnectionFacade socket;

    /**
     * The thread that runs the reader.
     */
    protected Thread readerThread;

    protected final AtomicLong readerThreadCounter = new AtomicLong(0);

    private final AtomicLong reconnectThreadCounter = new AtomicLong(0);

    /**
     * The reader to use to receive events and responses from asterisk.
     */
    protected ManagerReader reader;

    /**
     * The writer to use to send actions to asterisk.
     */
    protected ManagerWriter writer;

    /**
     * The protocol identifer Asterisk sends on connect wrapped into an object
     * to be used as mutex.
     */
    protected final ProtocolIdentifierWrapper protocolIdentifier;

    /**
     * The version of the Asterisk server we are connected to.
     */
    protected AsteriskVersion version;

    /**
     * Contains the registered handlers that process the ManagerResponses.
     * <p/>
     * Key is the internalActionId of the Action sent and value the
     * corresponding ResponseListener.
     */
    protected final Map<String, SendActionCallback> responseListeners;

    /**
     * Contains the event handlers that handle ResponseEvents for the
     * sendEventGeneratingAction methods.
     * <p/>
     * Key is the internalActionId of the Action sent and value the
     * corresponding EventHandler.
     */
    protected final Map<String, ManagerEventListener> responseEventListeners;

    /**
     * Contains the event handlers that users registered.
     */
    protected final List<ManagerEventListener> eventListeners;

    /**
     * <code>true</code> while reconnecting.
     */
    protected boolean reconnecting = false;

    protected ManagerConnectionState state = INITIAL;

    protected String eventMask;

    /**
     * Creates a new instance.
     */
    public ManagerConnectionImpl() {
        this.id = idCounter.getAndIncrement();
        this.responseListeners = new HashMap<String, SendActionCallback>();
        this.responseEventListeners = new HashMap<String, ManagerEventListener>();
        this.eventListeners = new ArrayList<ManagerEventListener>();
        this.protocolIdentifier = new ProtocolIdentifierWrapper();
    }

    protected ManagerReader createReader(Dispatcher dispatcher, Object source) {
        return new ManagerReaderImpl(dispatcher, source);
    }

    protected ManagerWriter createWriter() {
        return new ManagerWriterImpl();
    }

    /**
     * Sets the hostname of the asterisk server to connect to.
     * <p/>
     * Default is <code>localhost</code>.
     *
     * @param hostname the hostname to connect to
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * Sets the port to use to connect to the asterisk server. This is the port
     * specified in asterisk's <code>manager.conf</code> file.
     * <p/>
     * Default is 5038.
     *
     * @param port the port to connect to
     */
    public void setPort(int port) {
        if (port <= 0) {
            this.port = DEFAULT_PORT;
        } else {
            this.port = port;
        }
    }

    /**
     * Sets whether to use SSL.
     * <p/>
     * Default is false.
     *
     * @param ssl <code>true</code> to use SSL for the connection,
     *            <code>false</code> for a plain text connection.
     * @since 0.3
     */
    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    /**
     * Sets the username to use to connect to the asterisk server. This is the
     * username specified in asterisk's <code>manager.conf</code> file.
     *
     * @param username the username to use for login
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Sets the password to use to connect to the asterisk server. This is the
     * password specified in Asterisk's <code>manager.conf</code> file.
     *
     * @param password the password to use for login
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the time in milliseconds the synchronous method
     * {@link #sendAction(ManagerAction)} will wait for a response before
     * throwing a TimeoutException.
     * <p/>
     * Default is 2000.
     *
     * @param defaultResponseTimeout default response timeout in milliseconds
     * @since 0.2
     */
    public void setDefaultResponseTimeout(long defaultResponseTimeout) {
        this.defaultResponseTimeout = defaultResponseTimeout;
    }

    /**
     * Sets the time in milliseconds the synchronous method
     * {@link #sendEventGeneratingAction(EventGeneratingAction)} will wait for a
     * response and the last response event before throwing a TimeoutException.
     * <p/>
     * Default is 5000.
     *
     * @param defaultEventTimeout default event timeout in milliseconds
     * @since 0.2
     */
    public void setDefaultEventTimeout(long defaultEventTimeout) {
        this.defaultEventTimeout = defaultEventTimeout;
    }

    /**
     * Set to <code>true</code> to try reconnecting to ther asterisk serve
     * even if the reconnection attempt threw an AuthenticationFailedException.
     * <p/>
     * Default is <code>true</code>.
     *
     * @param keepAliveAfterAuthenticationFailure
     *         <code>true</code> to try reconnecting to ther asterisk serve
     *         even if the reconnection attempt threw an AuthenticationFailedException,
     *         <code>false</code> otherwise.
     */
    public void setKeepAliveAfterAuthenticationFailure(boolean keepAliveAfterAuthenticationFailure) {
        this.keepAliveAfterAuthenticationFailure = keepAliveAfterAuthenticationFailure;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public AsteriskVersion getVersion() {
        return version;
    }

    public String getHostname() {
        return hostname;
    }

    public int getPort() {
        return port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public InetAddress getLocalAddress() {
        return socket.getLocalAddress();
    }

    public int getLocalPort() {
        return socket.getLocalPort();
    }

    public InetAddress getRemoteAddress() {
        return socket.getRemoteAddress();
    }

    public int getRemotePort() {
        return socket.getRemotePort();
    }

    public void registerUserEventClass(Class<? extends ManagerEvent> userEventClass) {
        if (reader == null) {
            reader = createReader(this, this);
        }
        reader.registerEventClass(userEventClass);
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setSocketReadTimeout(int socketReadTimeout) {
        this.socketReadTimeout = socketReadTimeout;
    }

    public synchronized void login() throws IOException, AuthenticationFailedException, TimeoutException {
        login(null);
    }

    public synchronized void login(String eventMask) throws IOException, AuthenticationFailedException, TimeoutException {
        if (state != INITIAL && state != DISCONNECTED) {
            throw new IllegalStateException("Login may only be perfomed when in state " + "INITIAL or DISCONNECTED, but connection is in state " + state);
        }
        state = CONNECTING;
        this.eventMask = eventMask;
        try {
            doLogin(defaultResponseTimeout, eventMask);
        } finally {
            if (state != CONNECTED) {
                state = DISCONNECTED;
            }
        }
    }

    /**
     * Does the real login, following the steps outlined below.
     * <p/>
     * <ol>
     * <li>Connects to the asterisk server by calling {@link #connect()} if not
     * already connected
     * <li>Waits until the protocol identifier is received but not longer than
     * timeout ms.
     * <li>Sends a {@link ChallengeAction} requesting a challenge for authType
     * MD5.
     * <li>When the {@link ChallengeResponse} is received a {@link LoginAction}
     * is sent using the calculated key (MD5 hash of the password appended to
     * the received challenge).
     * </ol>
     *
     * @param timeout   the maximum time to wait for the protocol identifier (in
     *                  ms)
     * @param eventMask the event mask. Set to "on" if all events should be
     *                  send, "off" if not events should be sent or a combination of
     *                  "system", "call" and "log" (separated by ',') to specify what
     *                  kind of events should be sent.
     * @throws IOException                   if there is an i/o problem.
     * @throws AuthenticationFailedException if username or password are
     *                                       incorrect and the login action returns an error or if the MD5
     *                                       hash cannot be computed. The connection is closed in this
     *                                       case.
     * @throws TimeoutException              if a timeout occurs while waiting for the
     *                                       protocol identifier. The connection is closed in this case.
     */
    protected synchronized void doLogin(long timeout, String eventMask) throws IOException, AuthenticationFailedException, TimeoutException {
        ChallengeAction challengeAction;
        ManagerResponse challengeResponse;
        String challenge;
        String key;
        LoginAction loginAction;
        ManagerResponse loginResponse;
        if (socket == null) {
            connect();
        }
        synchronized (protocolIdentifier) {
            if (protocolIdentifier.value == null) {
                try {
                    protocolIdentifier.wait(timeout);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            if (protocolIdentifier.value == null) {
                disconnect();
                if (reader != null && reader.getTerminationException() != null) {
                    throw reader.getTerminationException();
                } else {
                    throw new TimeoutException("Timeout waiting for protocol identifier");
                }
            }
        }
        challengeAction = new ChallengeAction("MD5");
        try {
            challengeResponse = sendAction(challengeAction);
        } catch (Exception e) {
            disconnect();
            throw new AuthenticationFailedException("Unable to send challenge action", e);
        }
        if (challengeResponse instanceof ChallengeResponse) {
            challenge = ((ChallengeResponse) challengeResponse).getChallenge();
        } else {
            disconnect();
            throw new AuthenticationFailedException("Unable to get challenge from Asterisk. ChallengeAction returned: " + challengeResponse.getMessage());
        }
        try {
            MessageDigest md;
            md = MessageDigest.getInstance("MD5");
            if (challenge != null) {
                md.update(challenge.getBytes());
            }
            if (password != null) {
                md.update(password.getBytes());
            }
            key = ManagerUtil.toHexString(md.digest());
        } catch (NoSuchAlgorithmException ex) {
            disconnect();
            throw new AuthenticationFailedException("Unable to create login key using MD5 Message Digest", ex);
        }
        loginAction = new LoginAction(username, "MD5", key, eventMask);
        try {
            loginResponse = sendAction(loginAction);
        } catch (Exception e) {
            disconnect();
            throw new AuthenticationFailedException("Unable to send login action", e);
        }
        if (loginResponse instanceof ManagerError) {
            disconnect();
            throw new AuthenticationFailedException(loginResponse.getMessage());
        }
        state = CONNECTED;
        logger.info("Successfully logged in");
        version = determineVersion();
        writer.setTargetVersion(version);
        logger.info("Determined Asterisk version: " + version);
        ConnectEvent connectEvent = new ConnectEvent(this);
        connectEvent.setProtocolIdentifier(getProtocolIdentifier());
        connectEvent.setDateReceived(DateUtil.getDate());
        fireEvent(connectEvent);
    }

    protected AsteriskVersion determineVersion() throws IOException, TimeoutException {
        int attempts = 0;
        if ("Asterisk Call Manager/1.1".equals(protocolIdentifier.value)) {
            return AsteriskVersion.ASTERISK_1_6;
        }
        while (attempts++ < MAX_VERSION_ATTEMPTS) {
            final ManagerResponse showVersionFilesResponse;
            final List<String> showVersionFilesResult;
            showVersionFilesResponse = sendAction(new CommandAction("show version files pbx.c"), defaultResponseTimeout * 2);
            if (!(showVersionFilesResponse instanceof CommandResponse)) {
                break;
            }
            showVersionFilesResult = ((CommandResponse) showVersionFilesResponse).getResult();
            if (showVersionFilesResult != null && showVersionFilesResult.size() > 0) {
                final String line1;
                line1 = showVersionFilesResult.get(0);
                if (line1 != null && line1.startsWith("File")) {
                    final String rawVersion;
                    rawVersion = getRawVersion();
                    if (rawVersion != null && rawVersion.startsWith("Asterisk 1.4")) {
                        return AsteriskVersion.ASTERISK_1_4;
                    }
                    return AsteriskVersion.ASTERISK_1_2;
                } else if (line1 != null && line1.contains("No such command")) {
                    try {
                        Thread.sleep(RECONNECTION_VERSION_INTERVAL);
                    } catch (Exception ex) {
                    }
                } else {
                    break;
                }
            }
        }
        return AsteriskVersion.ASTERISK_1_6;
    }

    protected String getRawVersion() {
        final ManagerResponse showVersionResponse;
        try {
            showVersionResponse = sendAction(new CommandAction("show version"), defaultResponseTimeout * 2);
        } catch (Exception e) {
            return null;
        }
        if (showVersionResponse instanceof CommandResponse) {
            final List<String> showVersionResult;
            showVersionResult = ((CommandResponse) showVersionResponse).getResult();
            if (showVersionResult != null && showVersionResult.size() > 0) {
                return showVersionResult.get(0);
            }
        }
        return null;
    }

    protected synchronized void connect() throws IOException {
        logger.info("Connecting to " + hostname + ":" + port);
        if (reader == null) {
            logger.debug("Creating reader for " + hostname + ":" + port);
            reader = createReader(this, this);
        }
        if (writer == null) {
            logger.debug("Creating writer");
            writer = createWriter();
        }
        logger.debug("Creating socket");
        socket = createSocket();
        logger.debug("Passing socket to reader");
        reader.setSocket(socket);
        if (readerThread == null || !readerThread.isAlive() || reader.isDead()) {
            logger.debug("Creating and starting reader thread");
            readerThread = new Thread(reader);
            readerThread.setName("Asterisk-Java ManagerConnection-" + id + "-Reader-" + readerThreadCounter.getAndIncrement());
            readerThread.setDaemon(true);
            readerThread.start();
        }
        logger.debug("Passing socket to writer");
        writer.setSocket(socket);
    }

    protected SocketConnectionFacade createSocket() throws IOException {
        return new SocketConnectionFacadeImpl(hostname, port, ssl, socketTimeout, socketReadTimeout);
    }

    public synchronized void logoff() throws IllegalStateException {
        if (state != CONNECTED && state != RECONNECTING) {
            throw new IllegalStateException("Logoff may only be perfomed when in state " + "CONNECTED or RECONNECTING, but connection is in state " + state);
        }
        state = DISCONNECTING;
        if (socket != null) {
            try {
                sendAction(new LogoffAction());
            } catch (Exception e) {
                logger.warn("Unable to send LogOff action", e);
            }
        }
        cleanup();
        state = DISCONNECTED;
    }

    /**
     * Closes the socket connection.
     */
    protected synchronized void disconnect() {
        if (socket != null) {
            logger.info("Closing socket.");
            try {
                socket.close();
            } catch (IOException ex) {
                logger.warn("Unable to close socket: " + ex.getMessage());
            }
            socket = null;
        }
        protocolIdentifier.value = null;
    }

    public ManagerResponse sendAction(ManagerAction action) throws IOException, TimeoutException, IllegalArgumentException, IllegalStateException {
        return sendAction(action, defaultResponseTimeout);
    }

    public ManagerResponse sendAction(ManagerAction action, long timeout) throws IOException, TimeoutException, IllegalArgumentException, IllegalStateException {
        ResponseHandlerResult result;
        SendActionCallback callbackHandler;
        result = new ResponseHandlerResult();
        callbackHandler = new DefaultSendActionCallback(result);
        synchronized (result) {
            sendAction(action, callbackHandler);
            if (action instanceof UserEventAction) {
                return null;
            }
            if (result.getResponse() == null) {
                try {
                    result.wait(timeout);
                } catch (InterruptedException ex) {
                    logger.warn("Interrupted while waiting for result");
                    Thread.currentThread().interrupt();
                }
            }
        }
        if (result.getResponse() == null) {
            throw new TimeoutException("Timeout waiting for response to " + action.getAction() + (action.getActionId() == null ? "" : " (actionId: " + action.getActionId() + ")"));
        }
        return result.getResponse();
    }

    public void sendAction(ManagerAction action, SendActionCallback callback) throws IOException, IllegalArgumentException, IllegalStateException {
        final String internalActionId;
        if (action == null) {
            throw new IllegalArgumentException("Unable to send action: action is null.");
        }
        if ((state == CONNECTING || state == RECONNECTING) && (action instanceof ChallengeAction || action instanceof LoginAction)) {
        } else if (state == DISCONNECTING && action instanceof LogoffAction) {
        } else if (state != CONNECTED) {
            throw new IllegalStateException("Actions may only be sent when in state " + "CONNECTED, but connection is in state " + state);
        }
        if (socket == null) {
            throw new IllegalStateException("Unable to send " + action.getAction() + " action: socket not connected.");
        }
        internalActionId = createInternalActionId();
        if (callback != null) {
            synchronized (this.responseListeners) {
                this.responseListeners.put(internalActionId, callback);
            }
        }
        Class<? extends ManagerResponse> responseClass = getExpectedResponseClass(action.getClass());
        if (responseClass != null) {
            reader.expectResponseClass(internalActionId, responseClass);
        }
        writer.sendAction(action, internalActionId);
    }

    private Class<? extends ManagerResponse> getExpectedResponseClass(Class<? extends ManagerAction> actionClass) {
        final ExpectedResponse annotation = actionClass.getAnnotation(ExpectedResponse.class);
        if (annotation == null) {
            return null;
        }
        return annotation.value();
    }

    public ResponseEvents sendEventGeneratingAction(EventGeneratingAction action) throws IOException, EventTimeoutException, IllegalArgumentException, IllegalStateException {
        return sendEventGeneratingAction(action, defaultEventTimeout);
    }

    public ResponseEvents sendEventGeneratingAction(EventGeneratingAction action, long timeout) throws IOException, EventTimeoutException, IllegalArgumentException, IllegalStateException {
        final ResponseEventsImpl responseEvents;
        final ResponseEventHandler responseEventHandler;
        final String internalActionId;
        if (action == null) {
            throw new IllegalArgumentException("Unable to send action: action is null.");
        } else if (action.getActionCompleteEventClass() == null) {
            throw new IllegalArgumentException("Unable to send action: actionCompleteEventClass for " + action.getClass().getName() + " is null.");
        } else if (!ResponseEvent.class.isAssignableFrom(action.getActionCompleteEventClass())) {
            throw new IllegalArgumentException("Unable to send action: actionCompleteEventClass (" + action.getActionCompleteEventClass().getName() + ") for " + action.getClass().getName() + " is not a ResponseEvent.");
        }
        if (state != CONNECTED) {
            throw new IllegalStateException("Actions may only be sent when in state " + "CONNECTED but connection is in state " + state);
        }
        responseEvents = new ResponseEventsImpl();
        responseEventHandler = new ResponseEventHandler(responseEvents, action.getActionCompleteEventClass());
        internalActionId = createInternalActionId();
        synchronized (this.responseListeners) {
            this.responseListeners.put(internalActionId, responseEventHandler);
        }
        synchronized (this.responseEventListeners) {
            this.responseEventListeners.put(internalActionId, responseEventHandler);
        }
        synchronized (responseEvents) {
            writer.sendAction(action, internalActionId);
            if ((responseEvents.getResponse() == null || !responseEvents.isComplete())) {
                try {
                    responseEvents.wait(timeout);
                } catch (InterruptedException e) {
                    logger.warn("Interrupted while waiting for response events.");
                    Thread.currentThread().interrupt();
                }
            }
        }
        if ((responseEvents.getResponse() == null || !responseEvents.isComplete())) {
            synchronized (this.responseEventListeners) {
                this.responseEventListeners.remove(internalActionId);
            }
            throw new EventTimeoutException("Timeout waiting for response or response events to " + action.getAction() + (action.getActionId() == null ? "" : " (actionId: " + action.getActionId() + ")"), responseEvents);
        }
        synchronized (this.responseEventListeners) {
            this.responseEventListeners.remove(internalActionId);
        }
        return responseEvents;
    }

    /**
     * Creates a new unique internal action id based on the hash code of this
     * connection and a sequence.
     *
     * @return a new internal action id
     * @see ManagerUtil#addInternalActionId(String,String)
     * @see ManagerUtil#getInternalActionId(String)
     * @see ManagerUtil#stripInternalActionId(String)
     */
    protected String createInternalActionId() {
        final StringBuffer sb;
        sb = new StringBuffer();
        sb.append(this.hashCode());
        sb.append("_");
        sb.append(actionIdCounter.getAndIncrement());
        return sb.toString();
    }

    public void addEventListener(final ManagerEventListener listener) {
        synchronized (this.eventListeners) {
            if (!this.eventListeners.contains(listener)) {
                this.eventListeners.add(listener);
            }
        }
    }

    public void removeEventListener(final ManagerEventListener listener) {
        synchronized (this.eventListeners) {
            if (this.eventListeners.contains(listener)) {
                this.eventListeners.remove(listener);
            }
        }
    }

    public String getProtocolIdentifier() {
        return protocolIdentifier.value;
    }

    public ManagerConnectionState getState() {
        return state;
    }

    /**
     * This method is called by the reader whenever a {@link ManagerResponse} is
     * received. The response is dispatched to the associated
     * {@link SendActionCallback}.
     *
     * @param response the response received by the reader
     * @see ManagerReader
     */
    public void dispatchResponse(ManagerResponse response) {
        final String actionId;
        String internalActionId;
        SendActionCallback listener;
        if (response == null) {
            logger.error("Unable to dispatch null response. This should never happen. Please file a bug.");
            return;
        }
        actionId = response.getActionId();
        internalActionId = null;
        listener = null;
        if (actionId != null) {
            internalActionId = ManagerUtil.getInternalActionId(actionId);
            response.setActionId(ManagerUtil.stripInternalActionId(actionId));
        }
        logger.debug("Dispatching response with internalActionId '" + internalActionId + "':\n" + response);
        if (internalActionId != null) {
            synchronized (this.responseListeners) {
                listener = responseListeners.get(internalActionId);
                if (listener != null) {
                    this.responseListeners.remove(internalActionId);
                } else {
                    logger.debug("No response listener registered for " + "internalActionId '" + internalActionId + "'");
                }
            }
        } else {
            logger.error("Unable to retrieve internalActionId from response: " + "actionId '" + actionId + "':\n" + response);
        }
        if (listener != null) {
            try {
                listener.onResponse(response);
            } catch (Exception e) {
                logger.warn("Unexpected exception in response listener " + listener.getClass().getName(), e);
            }
        }
    }

    /**
     * This method is called by the reader whenever a ManagerEvent is received.
     * The event is dispatched to all registered ManagerEventHandlers.
     *
     * @param event the event received by the reader
     * @see #addEventListener(ManagerEventListener)
     * @see #removeEventListener(ManagerEventListener)
     * @see ManagerReader
     */
    public void dispatchEvent(ManagerEvent event) {
        if (event == null) {
            logger.error("Unable to dispatch null event. This should never happen. Please file a bug.");
            return;
        }
        logger.debug("Dispatching event:\n" + event.toString());
        if (event instanceof ResponseEvent) {
            ResponseEvent responseEvent;
            String internalActionId;
            responseEvent = (ResponseEvent) event;
            internalActionId = responseEvent.getInternalActionId();
            if (internalActionId != null) {
                synchronized (responseEventListeners) {
                    ManagerEventListener listener;
                    listener = responseEventListeners.get(internalActionId);
                    if (listener != null) {
                        try {
                            listener.onManagerEvent(event);
                        } catch (Exception e) {
                            logger.warn("Unexpected exception in response event listener " + listener.getClass().getName(), e);
                        }
                    }
                }
            } else {
            }
        }
        if (event instanceof DisconnectEvent) {
            if (state == CONNECTED) {
                state = RECONNECTING;
                cleanup();
                Thread reconnectThread = new Thread(new Runnable() {

                    public void run() {
                        reconnect();
                    }
                });
                reconnectThread.setName("Asterisk-Java ManagerConnection-" + id + "-Reconnect-" + reconnectThreadCounter.getAndIncrement());
                reconnectThread.setDaemon(true);
                reconnectThread.start();
            } else {
                return;
            }
        }
        if (event instanceof ProtocolIdentifierReceivedEvent) {
            ProtocolIdentifierReceivedEvent protocolIdentifierReceivedEvent;
            String protocolIdentifier;
            protocolIdentifierReceivedEvent = (ProtocolIdentifierReceivedEvent) event;
            protocolIdentifier = protocolIdentifierReceivedEvent.getProtocolIdentifier();
            setProtocolIdentifier(protocolIdentifier);
            return;
        }
        fireEvent(event);
    }

    /**
     * Notifies all {@link ManagerEventListener}s registered by users.
     *
     * @param event the event to propagate
     */
    protected void fireEvent(ManagerEvent event) {
        synchronized (eventListeners) {
            for (ManagerEventListener listener : eventListeners) {
                try {
                    listener.onManagerEvent(event);
                } catch (RuntimeException e) {
                    logger.warn("Unexpected exception in eventHandler " + listener.getClass().getName(), e);
                }
            }
        }
    }

    /**
     * This method is called when a {@link ProtocolIdentifierReceivedEvent} is
     * received from the reader. Having received a correct protocol identifier
     * is the precodition for logging in.
     *
     * @param identifier the protocol version used by the Asterisk server.
     */
    protected void setProtocolIdentifier(final String identifier) {
        logger.info("Connected via " + identifier);
        if (!"Asterisk Call Manager/1.0".equals(identifier) && !"Asterisk Call Manager/1.1".equals(identifier) && !"Asterisk Call Manager/1.2".equals(identifier) && !"OpenPBX Call Manager/1.0".equals(identifier) && !"CallWeaver Call Manager/1.0".equals(identifier) && !(identifier != null && identifier.startsWith("Asterisk Call Manager Proxy/"))) {
            logger.warn("Unsupported protocol version '" + identifier + "'. Use at your own risk.");
        }
        synchronized (protocolIdentifier) {
            protocolIdentifier.value = identifier;
            protocolIdentifier.notifyAll();
        }
    }

    /**
     * Reconnects to the asterisk server when the connection is lost.
     * <p/>
     * While keepAlive is <code>true</code> we will try to reconnect.
     * Reconnection attempts will be stopped when the {@link #logoff()} method
     * is called or when the login after a successful reconnect results in an
     * {@link AuthenticationFailedException} suggesting that the manager
     * credentials have changed and keepAliveAfterAuthenticationFailure is not
     * set.
     * <p/>
     * This method is called when a {@link DisconnectEvent} is received from the
     * reader.
     */
    protected void reconnect() {
        int numTries;
        numTries = 0;
        while (state == RECONNECTING) {
            try {
                if (numTries < 10) {
                    Thread.sleep(RECONNECTION_INTERVAL_1);
                } else {
                    Thread.sleep(RECONNECTION_INTERVAL_2);
                }
            } catch (InterruptedException e1) {
                Thread.currentThread().interrupt();
            }
            try {
                connect();
                try {
                    doLogin(defaultResponseTimeout, eventMask);
                    logger.info("Successfully reconnected.");
                    break;
                } catch (AuthenticationFailedException e1) {
                    if (keepAliveAfterAuthenticationFailure) {
                        logger.error("Unable to log in after reconnect: " + e1.getMessage());
                    } else {
                        logger.error("Unable to log in after reconnect: " + e1.getMessage() + ". Giving up.");
                        state = DISCONNECTED;
                    }
                } catch (TimeoutException e1) {
                    logger.error("TimeoutException while trying to log in " + "after reconnect.");
                }
            } catch (IOException e) {
                logger.warn("Exception while trying to reconnect: " + e.getMessage());
            }
            numTries++;
        }
    }

    protected void cleanup() {
        disconnect();
        this.readerThread = null;
    }

    @Override
    public String toString() {
        StringBuffer sb;
        sb = new StringBuffer("ManagerConnection[");
        sb.append("id='").append(id).append("',");
        sb.append("hostname='").append(hostname).append("',");
        sb.append("port=").append(port).append(",");
        sb.append("systemHashcode=").append(System.identityHashCode(this)).append("]");
        return sb.toString();
    }

    /**
     * A simple data object to store a ManagerResult.
     */
    protected static class ResponseHandlerResult implements Serializable {

        /**
         * Serializable version identifier.
         */
        protected static final long serialVersionUID = 7831097958568769220L;

        protected ManagerResponse response;

        public ResponseHandlerResult() {
        }

        public ManagerResponse getResponse() {
            return this.response;
        }

        public void setResponse(ManagerResponse response) {
            this.response = response;
        }
    }

    /**
     * A simple response handler that stores the received response in a
     * ResponseHandlerResult for further processing.
     */
    protected static class DefaultSendActionCallback implements SendActionCallback, Serializable {

        /**
         * Serializable version identifier.
         */
        protected static final long serialVersionUID = 2926598671855316803L;

        protected final ResponseHandlerResult result;

        /**
         * Creates a new instance.
         *
         * @param result the result to store the response in
         */
        public DefaultSendActionCallback(ResponseHandlerResult result) {
            this.result = result;
        }

        public void onResponse(ManagerResponse response) {
            synchronized (result) {
                result.setResponse(response);
                result.notifyAll();
            }
        }
    }

    /**
     * A combinded event and response handler that adds received events and the
     * response to a ResponseEvents object.
     */
    @SuppressWarnings("unchecked")
    private static class ResponseEventHandler implements ManagerEventListener, SendActionCallback {

        protected final ResponseEventsImpl events;

        protected final Class actionCompleteEventClass;

        /**
         * Creates a new instance.
         *
         * @param events                   the ResponseEventsImpl to store the events in
         * @param actionCompleteEventClass the type of event that indicates that
         *                                 all events have been received
         */
        public ResponseEventHandler(ResponseEventsImpl events, Class actionCompleteEventClass) {
            this.events = events;
            this.actionCompleteEventClass = actionCompleteEventClass;
        }

        public void onManagerEvent(ManagerEvent event) {
            synchronized (events) {
                if (event instanceof ResponseEvent) {
                    ResponseEvent responseEvent;
                    responseEvent = (ResponseEvent) event;
                    events.addEvent(responseEvent);
                }
                if (actionCompleteEventClass.isAssignableFrom(event.getClass())) {
                    events.setComplete(true);
                    if (events.getResponse() != null) {
                        events.notifyAll();
                    }
                }
            }
        }

        public void onResponse(ManagerResponse response) {
            synchronized (events) {
                events.setRepsonse(response);
                if (response instanceof ManagerError) {
                    events.setComplete(true);
                }
                if (events.isComplete()) {
                    events.notifyAll();
                }
            }
        }
    }

    protected class ProtocolIdentifierWrapper {

        String value;
    }
}
