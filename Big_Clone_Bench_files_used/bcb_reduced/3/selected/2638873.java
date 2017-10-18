package com.tegsoft.pbx;

import static org.asteriskjava.manager.ManagerConnectionState.CONNECTED;
import static org.asteriskjava.manager.ManagerConnectionState.DISCONNECTED;
import static org.asteriskjava.manager.ManagerConnectionState.INITIAL;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.asteriskjava.AsteriskVersion;
import org.asteriskjava.manager.AuthenticationFailedException;
import org.asteriskjava.manager.EventTimeoutException;
import org.asteriskjava.manager.ExpectedResponse;
import org.asteriskjava.manager.ManagerConnectionState;
import org.asteriskjava.manager.ManagerEventListener;
import org.asteriskjava.manager.ResponseEvents;
import org.asteriskjava.manager.SendActionCallback;
import org.asteriskjava.manager.TimeoutException;
import org.asteriskjava.manager.action.ChallengeAction;
import org.asteriskjava.manager.action.EventGeneratingAction;
import org.asteriskjava.manager.action.LoginAction;
import org.asteriskjava.manager.action.LogoffAction;
import org.asteriskjava.manager.action.ManagerAction;
import org.asteriskjava.manager.action.StatusAction;
import org.asteriskjava.manager.action.UserEventAction;
import org.asteriskjava.manager.event.ConnectEvent;
import org.asteriskjava.manager.event.DisconnectEvent;
import org.asteriskjava.manager.event.ManagerEvent;
import org.asteriskjava.manager.event.ProtocolIdentifierReceivedEvent;
import org.asteriskjava.manager.event.ResponseEvent;
import org.asteriskjava.manager.event.StatusEvent;
import org.asteriskjava.manager.internal.Dispatcher;
import org.asteriskjava.manager.internal.ManagerReader;
import org.asteriskjava.manager.internal.ManagerReaderImpl;
import org.asteriskjava.manager.internal.ManagerUtil;
import org.asteriskjava.manager.internal.ManagerWriter;
import org.asteriskjava.manager.internal.ManagerWriterImpl;
import org.asteriskjava.manager.internal.ResponseEventsImpl;
import org.asteriskjava.manager.response.ChallengeResponse;
import org.asteriskjava.manager.response.ManagerError;
import org.asteriskjava.manager.response.ManagerResponse;
import org.asteriskjava.util.DateUtil;
import org.asteriskjava.util.SocketConnectionFacade;
import org.asteriskjava.util.internal.SocketConnectionFacadeImpl;
import com.tegsoft.tobe.db.Counter;
import com.tegsoft.tobe.util.Compare;
import com.tegsoft.tobe.util.message.MessageUtil;

public class ManagerConnection implements org.asteriskjava.manager.ManagerConnection, Dispatcher {

    private boolean logoffCalled = false;

    private String PBXID;

    private String hostname = "localhost";

    private int port = 5038;

    protected String username;

    protected String password;

    private long defaultResponseTimeout = 2000;

    private long defaultEventTimeout = 5000;

    private SocketConnectionFacade socket;

    private Thread readerThread;

    private ManagerReader reader;

    private ManagerWriter writer;

    private ArrayList<StatusEvent> channels = new ArrayList<StatusEvent>();

    private boolean channelsPrepared = false;

    private final ProtocolIdentifierWrapper protocolIdentifier;

    private AsteriskVersion version;

    private final Map<String, SendActionCallback> responseListeners;

    private final Map<String, ManagerEventListener> responseEventListeners;

    private final List<ManagerEventListener> eventListeners;

    public ManagerConnection() {
        this.responseListeners = new HashMap<String, SendActionCallback>();
        this.responseEventListeners = new HashMap<String, ManagerEventListener>();
        this.eventListeners = new ArrayList<ManagerEventListener>();
        this.protocolIdentifier = new ProtocolIdentifierWrapper();
    }

    public synchronized void disconnect() {
        try {
            logoff();
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ex) {
                }
                socket = null;
            }
            protocolIdentifier.value = null;
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
    }

    public synchronized void logoff() throws IllegalStateException {
        if (socket == null) {
            return;
        }
        if (!socket.isConnected()) {
            return;
        }
        if (logoffCalled) {
            return;
        }
        logoffCalled = true;
        try {
            sendAction(new LogoffAction(), 10000);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        disconnect();
    }

    public synchronized void prepareChannels() throws Exception {
        channelsPrepared = false;
        channels.clear();
        sendAction(new StatusAction(), 5000);
        int maxwait = 10;
        while (maxwait > 0) {
            maxwait--;
            if (channelsPrepared) {
                break;
            }
            Thread.sleep(500);
        }
    }

    public synchronized StatusEvent getChannel(String channel) throws Exception {
        for (int i = 0; i < channels.size(); i++) {
            if (Compare.equal(channel, channels.get(i).getChannel())) {
                return channels.get(i);
            }
        }
        return null;
    }

    public String getPBXID() {
        return PBXID;
    }

    public void setPBXID(String pBXID) {
        PBXID = pBXID;
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
	 * @param hostname
	 *            the hostname to connect to
	 */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    /**
	 * Sets the username to use to connect to the asterisk server. This is the
	 * username specified in asterisk's <code>manager.conf</code> file.
	 * 
	 * @param username
	 *            the username to use for login
	 */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
	 * Sets the password to use to connect to the asterisk server. This is the
	 * password specified in Asterisk's <code>manager.conf</code> file.
	 * 
	 * @param password
	 *            the password to use for login
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
	 * @param defaultResponseTimeout
	 *            default response timeout in milliseconds
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
	 * @param defaultEventTimeout
	 *            default event timeout in milliseconds
	 * @since 0.2
	 */
    public void setDefaultEventTimeout(long defaultEventTimeout) {
        this.defaultEventTimeout = defaultEventTimeout;
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
        return false;
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
    }

    public void setSocketReadTimeout(int socketReadTimeout) {
    }

    public synchronized void login() throws IOException, AuthenticationFailedException, TimeoutException {
        login(null);
    }

    public synchronized void login(String eventMask) throws IOException, AuthenticationFailedException, TimeoutException {
        doLogin(defaultResponseTimeout, eventMask);
    }

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
        if (!socket.isConnected()) {
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
        version = determineVersion();
        writer.setTargetVersion(version);
        ConnectEvent connectEvent = new ConnectEvent(this);
        connectEvent.setProtocolIdentifier(getProtocolIdentifier());
        connectEvent.setDateReceived(DateUtil.getDate());
        fireEvent(connectEvent);
    }

    protected AsteriskVersion determineVersion() throws IOException, TimeoutException {
        return AsteriskVersion.ASTERISK_1_8;
    }

    protected synchronized void connect() throws IOException {
        if (reader == null) {
            reader = createReader(this, this);
        }
        if (writer == null) {
            writer = createWriter();
        }
        socket = createSocket();
        reader.setSocket(socket);
        if (readerThread == null || !readerThread.isAlive() || reader.isDead()) {
            readerThread = new Thread(reader);
            readerThread.setName("Asterisk-Java ManagerConnection- -- -Reader-");
            readerThread.setDaemon(true);
            readerThread.start();
        }
        writer.setSocket(socket);
    }

    protected SocketConnectionFacade createSocket() throws IOException {
        return new SocketConnectionFacadeImpl(hostname, port, false, 0, 0);
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
                    Thread.currentThread().interrupt();
                }
            }
        }
        return result.getResponse();
    }

    public void sendAction(ManagerAction action, SendActionCallback callback) throws IOException, IllegalArgumentException, IllegalStateException {
        final String internalActionId;
        if (action == null) {
            throw new IllegalArgumentException("Unable to send action: action is null.");
        }
        if (socket == null) {
            while (socket == null) {
                try {
                    login();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    MessageUtil.logMessage(ManagerConnection.class, Level.FATAL, "Waiting for connection");
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        if (!socket.isConnected()) {
            socket = null;
            while (socket == null) {
                try {
                    login();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                try {
                    MessageUtil.logMessage(ManagerConnection.class, Level.FATAL, "Waiting for connection");
                    Thread.sleep(1000);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        internalActionId = Counter.getUUIDString();
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
        responseEvents = new ResponseEventsImpl();
        responseEventHandler = new ResponseEventHandler(responseEvents, action.getActionCompleteEventClass());
        internalActionId = Counter.getUUIDString();
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
        if (socket == null) {
            return INITIAL;
        }
        if (socket.isConnected()) {
            return CONNECTED;
        } else {
            return DISCONNECTED;
        }
    }

    /**
	 * This method is called by the reader whenever a {@link ManagerResponse} is
	 * received. The response is dispatched to the associated
	 * {@link SendActionCallback}.
	 * 
	 * @param response
	 *            the response received by the reader
	 * @see ManagerReader
	 */
    public void dispatchResponse(ManagerResponse response) {
        final String actionId;
        String internalActionId;
        SendActionCallback listener;
        if (response == null) {
            return;
        }
        actionId = response.getActionId();
        internalActionId = null;
        listener = null;
        if (actionId != null) {
            internalActionId = ManagerUtil.getInternalActionId(actionId);
            response.setActionId(ManagerUtil.stripInternalActionId(actionId));
        }
        if (internalActionId != null) {
            synchronized (this.responseListeners) {
                listener = responseListeners.get(internalActionId);
                if (listener != null) {
                    this.responseListeners.remove(internalActionId);
                } else {
                }
            }
        }
        if (listener != null) {
            try {
                listener.onResponse(response);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
	 * This method is called by the reader whenever a ManagerEvent is received.
	 * The event is dispatched to all registered ManagerEventHandlers.
	 * 
	 * @param event
	 *            the event received by the reader
	 * @see #addEventListener(ManagerEventListener)
	 * @see #removeEventListener(ManagerEventListener)
	 * @see ManagerReader
	 */
    public void dispatchEvent(ManagerEvent event) {
        if (event == null) {
            return;
        }
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
                            e.printStackTrace();
                        }
                    }
                }
            } else {
            }
        }
        if (event instanceof StatusEvent) {
            channels.add((StatusEvent) event);
        }
        if (event instanceof StatusEvent) {
            channelsPrepared = true;
        }
        if (event instanceof DisconnectEvent) {
            cleanup();
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
	 * @param event
	 *            the event to propagate
	 */
    private void fireEvent(ManagerEvent event) {
        synchronized (eventListeners) {
            for (ManagerEventListener listener : eventListeners) {
                try {
                    listener.onManagerEvent(event);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
	 * This method is called when a {@link ProtocolIdentifierReceivedEvent} is
	 * received from the reader. Having received a correct protocol identifier
	 * is the precodition for logging in.
	 * 
	 * @param identifier
	 *            the protocol version used by the Asterisk server.
	 */
    private void setProtocolIdentifier(final String identifier) {
        synchronized (protocolIdentifier) {
            protocolIdentifier.value = identifier;
            protocolIdentifier.notifyAll();
        }
    }

    private void cleanup() {
        disconnect();
        this.readerThread = null;
    }

    @Override
    public String toString() {
        StringBuffer sb;
        sb = new StringBuffer("ManagerConnection[");
        sb.append("hostname='").append(hostname).append("',");
        sb.append("port=").append(port).append(",");
        sb.append("systemHashcode=").append(System.identityHashCode(this)).append("]");
        return sb.toString();
    }

    /**
	 * A simple data object to store a ManagerResult.
	 */
    private static class ResponseHandlerResult implements Serializable {

        /**
		 * Serializable version identifier.
		 */
        private static final long serialVersionUID = 7831097958568769220L;

        private ManagerResponse response;

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
    private static class DefaultSendActionCallback implements SendActionCallback, Serializable {

        /**
		 * Serializable version identifier.
		 */
        private static final long serialVersionUID = 2926598671855316803L;

        private final ResponseHandlerResult result;

        /**
		 * Creates a new instance.
		 * 
		 * @param result
		 *            the result to store the response in
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
    private static class ResponseEventHandler implements ManagerEventListener, SendActionCallback {

        private final ResponseEventsImpl events;

        private final Class<? extends ResponseEvent> actionCompleteEventClass;

        /**
		 * Creates a new instance.
		 * 
		 * @param events
		 *            the ResponseEventsImpl to store the events in
		 * @param actionCompleteEventClass
		 *            the type of event that indicates that all events have been
		 *            received
		 */
        public ResponseEventHandler(ResponseEventsImpl events, Class<? extends ResponseEvent> actionCompleteEventClass) {
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

    private static class ProtocolIdentifierWrapper {

        String value;
    }

    public boolean isChannelsPrepared() {
        return channelsPrepared;
    }

    public void setChannelsPrepared(boolean channelsPrepared) {
        this.channelsPrepared = channelsPrepared;
    }

    public ArrayList<StatusEvent> getChannels() {
        return channels;
    }

    public void setChannels(ArrayList<StatusEvent> channels) {
        this.channels = channels;
    }
}
