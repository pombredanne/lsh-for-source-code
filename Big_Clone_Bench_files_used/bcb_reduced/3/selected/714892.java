package org.p2pvpn.network;

import org.p2pvpn.network.bittorrent.BitTorrentTracker;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.p2pvpn.network.bandwidth.TokenBucket;
import org.p2pvpn.network.bittorrent.DHT;
import org.p2pvpn.network.bittorrent.bencode.BencodeString;
import org.p2pvpn.tools.AdvProperties;
import org.p2pvpn.tools.CryptoUtils;
import org.p2pvpn.tools.SocketAddrStr;

/**
 * The ConnectionManager is the central point of the P2PVPN network. It
 * coordinates the different layers of the network.
 * @author Wolfgang Ginolas
 */
public class ConnectionManager implements Runnable {

    private static final String WHATISMYIP_URL = "http://automation.whatismyip.com/n09230945.asp";

    private static final long WHATISMYIP_REFRESH_S = 10 * 60;

    private static final double SEND_BUCKET_SIZE = 10 * 1024;

    private static DHT dht = null;

    private ServerSocket server;

    private int serverPort;

    private PeerID localAddr;

    private ScheduledExecutorService scheduledExecutor;

    private Router router;

    private Connector connector;

    private UPnPPortForward uPnPPortForward;

    private BitTorrentTracker bitTorrentTracker;

    private String whatIsMyIP;

    private AdvProperties accessCfg;

    private byte[] networkKey;

    private TokenBucket sendLimit, recLimit;

    private Pinger pinger;

    private int sendBufferSize;

    private boolean tcpFlush;

    /**
	 * Create a new ConnectionManager
	 * @param accessCfg the access invitation
	 * @param serverPort the local server port
	 */
    public ConnectionManager(AdvProperties accessCfg, int serverPort) {
        sendBufferSize = TCPConnection.DEFAULT_MAX_QUEUE;
        tcpFlush = TCPConnection.DEFAULT_TCP_FLUSH;
        this.serverPort = serverPort;
        this.accessCfg = accessCfg;
        scheduledExecutor = Executors.newScheduledThreadPool(10);
        localAddr = new PeerID(accessCfg.getPropertyBytes("access.publicKey", null), true);
        router = new Router(this);
        connector = new Connector(this);
        bitTorrentTracker = null;
        whatIsMyIP = null;
        sendLimit = new TokenBucket(0, SEND_BUCKET_SIZE);
        recLimit = new TokenBucket(0, SEND_BUCKET_SIZE);
        pinger = new Pinger(this);
        calcNetworkKey();
        (new Thread(this, "ConnectionManager")).start();
        scheduledExecutor.schedule(new Runnable() {

            public void run() {
                checkWhatIsMyIP();
            }
        }, 1, TimeUnit.SECONDS);
        try {
            if (dht == null) {
                dht = new DHT(new DatagramSocket(serverPort));
                dht.start();
            }
            dht.setSearchID(calcDHTKey());
            dht.setConnectionManager(this);
        } catch (SocketException e) {
            Logger.getLogger("").log(Level.SEVERE, "Could not start DHT", e);
        }
    }

    /**
	 * Calculate the network key used for encryption
	 */
    private void calcNetworkKey() {
        byte[] b = accessCfg.getPropertyBytes("network.signature", null);
        MessageDigest md = CryptoUtils.getMessageDigest();
        md.update("secretKey".getBytes());
        networkKey = md.digest(b);
    }

    private BencodeString calcDHTKey() {
        byte[] b = accessCfg.getPropertyBytes("network.signature", null);
        MessageDigest md = CryptoUtils.getMessageDigest();
        md.update("secretKeyDHT".getBytes());
        return new BencodeString(md.digest(b));
    }

    /**
	 * Find out the IPs of the local network adapters.
	 */
    public void updateLocalIPs() {
        String ipList = "";
        String ipv6List = "";
        try {
            Enumeration<NetworkInterface> is = NetworkInterface.getNetworkInterfaces();
            while (is.hasMoreElements()) {
                NetworkInterface i = is.nextElement();
                if (!i.getName().toLowerCase().startsWith("tap") && !i.getDisplayName().toLowerCase().startsWith("tap") && !i.getName().equals("lo")) {
                    System.out.println("if: '" + i.getName() + "', '" + i.getDisplayName() + "'");
                    Enumeration<InetAddress> as = i.getInetAddresses();
                    while (as.hasMoreElements()) {
                        InetAddress a = as.nextElement();
                        if (a instanceof Inet4Address) {
                            String s = a.getHostAddress();
                            ipList = ipList + " " + s;
                        }
                        if (a instanceof Inet6Address) {
                            String s = a.getHostAddress();
                            ipv6List = ipv6List + " " + s;
                        }
                    }
                }
            }
        } catch (SocketException ex) {
            Logger.getLogger("").log(Level.WARNING, "", ex);
        }
        if (whatIsMyIP != null) ipList = ipList + " " + whatIsMyIP;
        router.setLocalPeerInfo("local.port", "" + serverPort);
        router.setLocalPeerInfo("local.ips", ipList.substring(1));
        router.setLocalPeerInfo("local.ip6s", ipv6List.substring(1));
    }

    /**
	 * Periodically check the local IPs.
	 */
    private void checkWhatIsMyIP() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(new URL(WHATISMYIP_URL).openConnection().getInputStream()));
            InetAddress a = InetAddress.getByName(in.readLine());
            if (a instanceof Inet4Address) whatIsMyIP = a.getHostAddress();
        } catch (Exception ex) {
            Logger.getLogger("").log(Level.WARNING, "can not determine external address", ex);
        }
        updateLocalIPs();
        scheduledExecutor.schedule(new Runnable() {

            public void run() {
                checkWhatIsMyIP();
            }
        }, WHATISMYIP_REFRESH_S, TimeUnit.SECONDS);
    }

    public PeerID getLocalAddr() {
        return localAddr;
    }

    public AdvProperties getAccessCfg() {
        return accessCfg;
    }

    public ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    /**
	 * Called, when a TCPConnection is established.
	 * @param connection the connection
	 */
    public void newConnection(TCPConnection connection) {
        new P2PConnection(this, connection);
    }

    /**
	 * Called, when a new P2PConnectrion is established.
	 * @param p2pConnection
	 */
    public void newP2PConnection(P2PConnection p2pConnection) {
        router.newP2PConnection(p2pConnection);
    }

    /**
	 * A thread to accept connections from other peers.
	 */
    @Override
    public void run() {
        try {
            server = new ServerSocket(serverPort);
            serverPort = server.getLocalPort();
            Logger.getLogger("").log(Level.INFO, "listening on port " + server.getLocalPort());
            while (true) {
                Socket s = server.accept();
                new TCPConnection(this, s, networkKey);
            }
        } catch (Exception e) {
            Logger.getLogger("").log(Level.SEVERE, "Not listening on " + server.getLocalPort() + " anymore", e);
        }
    }

    /**
	 * Add the IPs stored in the access invitation to the known hosts list.
	 * @param accessCfg the access invitation
	 */
    public void addIPs(AdvProperties accessCfg) {
        connector.addIPs(accessCfg);
        String tracker = accessCfg.getProperty("network.bootstrap.tracker");
        if (tracker != null) bitTorrentTracker = new BitTorrentTracker(this, tracker);
    }

    /**
	 * Try to connect to the given host.
	 * @param host the host
	 * @param port the port
	 */
    public void connectTo(String host, int port) {
        try {
            connectTo(InetAddress.getByName(host), port);
        } catch (UnknownHostException ex) {
            Logger.getLogger("").log(Level.WARNING, "", ex);
        }
    }

    /**
	 * Try to connect to the given host.
	 * @param host the host
	 * @param port the port
	 */
    public void connectTo(InetAddress host, int port) {
        new ConnectTask(host, port);
    }

    /**
	 * Try to connect to the given host.
	 * @param addr the host and port using the format "hist:port"
	 */
    public void connectTo(String addr) {
        try {
            InetSocketAddress a = SocketAddrStr.parseSocketAddr(addr);
            connectTo(a.getAddress(), a.getPort());
        } catch (Exception e) {
            Logger.getLogger("").log(Level.WARNING, "", e);
        }
    }

    /**
	 * Stop this network and close all connections
	 */
    public void close() {
        try {
            scheduledExecutor.shutdownNow();
            router.close();
            if (server != null) server.close();
        } catch (IOException e) {
            Logger.getLogger("").log(Level.WARNING, "", e);
        }
    }

    /**
	 * A Task which tries to connect another peer
	 */
    private class ConnectTask implements Runnable {

        private InetAddress host;

        private int port;

        /**
		 * Try to connect another peer.
		 * @param host the host
		 * @param port the port
		 */
        public ConnectTask(InetAddress host, int port) {
            this.host = host;
            this.port = port;
            (new Thread(this, "ConnectTask")).start();
        }

        @Override
        public void run() {
            Socket s;
            try {
                s = new Socket(host, port);
                new TCPConnection(ConnectionManager.this, s, networkKey);
            } catch (Throwable e) {
            }
        }
    }

    public int getServerPort() {
        return serverPort;
    }

    public Router getRouter() {
        return router;
    }

    public Connector getConnector() {
        return connector;
    }

    public UPnPPortForward getUPnPPortForward() {
        return uPnPPortForward;
    }

    public TokenBucket getSendLimit() {
        return sendLimit;
    }

    public TokenBucket getRecLimit() {
        return recLimit;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }

    public boolean isTCPFlush() {
        return tcpFlush;
    }

    public void setTCPFlush(boolean tcpFlush) {
        this.tcpFlush = tcpFlush;
    }
}
