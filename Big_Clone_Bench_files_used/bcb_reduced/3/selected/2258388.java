package org.jabber.jabberbeans.serverside;

import org.jabber.jabberbeans.*;
import org.jabber.jabberbeans.util.*;
import java.io.IOException;
import java.net.*;

/**
 * <code>Launcher</code> is some simple code to negotiate a connection with a
 * Jabber server as a component. After negotiation, it launches a component.
 *
 * <b>Important:</b> a base_exec connection uses stdin/stdout for communication.
 * It is very important that System.err is used over System.out, as System.out
 * will be considered outgoing data to the server.
 *
 * @author  David Waite <a href="mailto:dwaite@jabber.com">
 *                      <i>&lt;dwaite@jabber.com&gt;</i></a>
 * @author  $Author: shawnxb $
 * @version $Revision: 1.3 $ 
 */
public class Launcher {

    /** builder for initial XML stream */
    XMLStreamHeaderBuilder xsbuilder;

    /** true if we are running a 'base_accept' connection */
    boolean bAccept = false;

    /** true if we are running a 'base_connect' connection */
    boolean bConnect = true;

    /** true if we are running a 'base_exec' connection */
    boolean bExec = false;

    /** hostname of the server we connect to with base_accept (not an IP)*/
    String servername = "localhost";

    int port = 4001;

    /** local component name */
    String host = "javaarchive";

    /** session id used for seeding with the server (this should be a random # */
    String sessionID = "a1b2c3d4e5";

    /** secret known between this component and the server */
    String secret = "argh!";

    /** class name of the ServerModule to launch */
    String servermodule = "full.class.name";

    /** connection object */
    ConnectionBean cb;

    private static final String HANDSHAKE_TIMEOUT = "Error: timeout while waiting for server handshake";

    private static final String BASE_ACCEPT_XMLNS = "jabber:component:accept";

    private static final String BASE_EXEC_XMLNS = "jabber:component:exec";

    /**
	 * <code>HandshakeValidator</code> does validation of a key sent
	 * incoming, for instance on a base_connect.
	 *
	 * @author <a href="mailto:dwaite@jabber.com">
	 *         <i>&lt;dwaite@jabber.com&gt;</i></a>
	 * @see PacketListener
	 */
    private class HandshakeValidator implements PacketListener {

        private String validResponse;

        private boolean bValid;

        public HandshakeValidator() {
            bValid = false;
            SHA1Helper sha;
            try {
                sha = new SHA1Helper();
                validResponse = sha.digest(sessionID, secret);
            } catch (InstantiationException e) {
                throw new RuntimeException("Cannot create key value");
            }
        }

        public boolean isValid() {
            return bValid;
        }

        public void receivedPacket(PacketEvent pe) {
            if (!(pe.getPacket() instanceof Handshake)) return;
            if (pe.getSource() != cb) return;
            System.err.println("received our handshake!");
            Handshake h = (Handshake) pe.getPacket();
            bValid = (h.getContent().equals(validResponse));
            synchronized (Launcher.this) {
                Launcher.this.notify();
            }
        }

        public void sentPacket(PacketEvent pe) {
        }

        /** this error condition is handled elsewhere **/
        public void sendFailed(PacketEvent pe) {
        }
    }

    public static final void main(String args[]) {
        new Launcher().start();
    }

    public final void start() {
        cb = new ConnectionBean();
        xsbuilder = new XMLStreamHeaderBuilder();
        if (bAccept == true) {
            System.err.println("starting accept handler");
            AcceptHandler();
            return;
        }
        if (bConnect == true) {
            System.err.println("starting connect handler");
            ConnectHandler();
            return;
        }
        if (bExec == true) {
            System.err.println("starting exec handler");
            ExecHandler();
            return;
        }
    }

    /**
	 * Support routine for base_accept. base_accept will cause the server to
	 * listen in on a port, waiting for connections. After a connection is
	 * established, there is a limited amount of time to complete 
	 * authenication.
	 *
	 * Steps:
	 *    1. set-up protocol handler (done previous)
	 *    2. connect to server
	 *    3. send <stream:stream ...> header
	 *    4. wait for <stream:stream ...> response
	 *    5. read in session ID from stream header.
	 *    6. hash based on id + secret
	 *    7. send handshake with hash
	 *    8. wait for response:
	 *       - if <stream:error>, resend hash (step 7)
	 *       - if disconnect/timeout, go back to step 2.
	 *       - if <handshake/>, continue
	 *    9. connection established, start custom logic.
	 */
    protected void AcceptHandler() {
        cb.addPacketListener(new PacketDebug());
        SyncPacketListener sync = new SyncPacketListener(cb);
        Packet p;
        SHA1Helper sha;
        try {
            sha = new SHA1Helper();
        } catch (InstantiationException e) {
            System.err.println("error in creating SHA helper class");
            return;
        }
        cb.disableStreamHeader();
        try {
            cb.connect(InetAddress.getByName(servername), port);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("error in communication for connect request");
            return;
        }
        xsbuilder.setXMLNS(BASE_ACCEPT_XMLNS);
        xsbuilder.setIdentifier(sessionID);
        xsbuilder.setFromAddress(new JID(host));
        xsbuilder.setToAddress(new JID(servername));
        Packet xmlpacket = xsbuilder.build();
        synchronized (sync) {
            System.err.println("sending header");
            cb.send(xmlpacket);
            try {
                System.err.println("waiting for response");
                p = sync.waitForType(xmlpacket, 5000);
                System.err.println("got response");
            } catch (InterruptedException e) {
                p = null;
            }
        }
        if (p == null) throw new RuntimeException("unable to verify server on port");
        System.err.println("computing handshake");
        String SessionID = cb.getSessionID();
        Handshake handshake = new Handshake(sha.digest(SessionID, secret));
        p = null;
        System.err.println("created handshake");
        sync.reset();
        try {
            synchronized (sync) {
                cb.send(handshake);
                System.err.println("waiting for handshake response");
                p = sync.waitForType(handshake, 5000);
                System.err.println("got handshake?!");
            }
        } catch (InterruptedException e) {
            p = null;
        }
        if (p == null) throw new RuntimeException("unable to handshake with server");
        ServerConnect();
    }

    /**
	 * Support routine for base_connect. base_connect will cause the server 
	 * to connect to a port on a remote machine.  After a connection is
	 * established, the server actually authenticates with us. There is a 
	 * limited amount of time to complete authenication.
	 *
	 * Steps:
	 *    1. set-up protocol handler (done previous)
	 *    2. open for connections
	 *    3. wait for <stream:stream ...> header
	 *    4. send <stream:stream ...> response with Session ID
	 *    5. compute handshake based on secret and session ID sent.
	 *    6. for ten seconds:
	 *    6a. wait for handshake to come in
	 *    6b. verify handshake 
	 *        - if valid, return empty handshake and exit loop
	 *        - if invalid, return stream:error
	 *    6c. if time expires without a valid handshake coming in, disconnect.
	 *        and go back to 3.
	 *    7. connection established, start custom logic.
	 */
    protected void ConnectHandler() {
        HandshakeValidator hv = new HandshakeValidator();
        cb.addPacketListener(new PacketDebug());
        ServerSocket ss;
        Socket s;
        try {
            ss = new ServerSocket(port);
            System.err.println("blocking for incoming connection");
            s = ss.accept();
            System.err.println("got incoming connection");
        } catch (IOException e) {
            System.err.println("cannot open listening port " + port);
            return;
        }
        try {
            cb.addPacketListener(hv);
            cb.disableStreamHeader();
            System.err.println("merging connection");
            cb.connect(s);
            System.err.println("connected!");
        } catch (java.io.IOException e) {
            java.lang.System.err.println("IO error while attempting to connect to server:");
            java.lang.System.err.println(e.toString());
            return;
        }
        xsbuilder.setXMLNS(BASE_ACCEPT_XMLNS);
        xsbuilder.setIdentifier(sessionID);
        xsbuilder.setToAddress(new JID(servername));
        xsbuilder.setFromAddress(new JID(host));
        Packet xmlstream = xsbuilder.build();
        synchronized (this) {
            cb.send(xmlstream);
            System.err.println("sent header");
            System.err.println("waiting");
            try {
                wait(15000);
            } catch (InterruptedException e) {
            }
        }
        if (!hv.isValid()) {
            System.err.println("error on handshake, timeout or invalid response.");
            cb.send(new XMLStreamError("Timout/ Error on handshake"));
            try {
                ss.close();
                cb.disconnect();
            } catch (java.io.IOException e) {
            }
            return;
        }
        cb.send(new Handshake(null));
        ServerConnect();
    }

    /**
     * Support routine for base_exec. base_exec will cause the client to be
     * launched on the local machine - the client will then do its I/O on
     * stdin/stdout..  We do not authenticate since we are local. Spoofing
     * is the least of our problems in that case.
     *
     * Steps:
     *    1. set-up protocol handler (done previous)
     *    2. set up connection on stdin/stdout
     *    3. send <stream:stream ...> response
     *    4. wait for <stream:stream ...> header
     *    5. connection established, start custom logic.
     */
    protected void ExecHandler() {
        SyncPacketListener sync = new SyncPacketListener(cb);
        Packet p;
        try {
            cb.disableStreamHeader();
            cb.connect(System.in, System.out);
        } catch (java.io.IOException e) {
            java.lang.System.err.println("IO error while attempting to connect to server:");
            java.lang.System.err.println(e.toString());
            return;
        }
        xsbuilder.setXMLNS(BASE_EXEC_XMLNS);
        xsbuilder.setToAddress(new JID(servername));
        xsbuilder.setFromAddress(new JID(host));
        xsbuilder.setIdentifier(sessionID);
        Packet xmlstream = xsbuilder.build();
        synchronized (this) {
            try {
                cb.send(xmlstream);
                p = sync.waitForType(xmlstream, 5000);
            } catch (InterruptedException e) {
                p = null;
            }
        }
        if (p == null) System.err.println("connection timed out");
        ServerConnect();
    }

    public void ServerConnect() {
        ServerModule module;
        try {
            module = (ServerModule) Class.forName(servermodule).newInstance();
        } catch (InstantiationException e) {
            System.err.println("unable to start serverside module");
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("unable to find serverside module");
            return;
        } catch (IllegalAccessException e) {
            System.err.println("access violation while instantiation serverside module");
            return;
        }
        cb.addPacketListener(module);
        System.err.println("instantiating serverside module");
        module.instantiate(cb, host);
    }
}
