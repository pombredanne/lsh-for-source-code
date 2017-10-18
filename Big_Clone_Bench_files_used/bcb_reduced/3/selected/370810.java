package cornell.herbivore.system;

import xjava.security.Cipher;
import java.io.*;
import java.net.*;
import java.util.*;
import java.math.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.security.*;
import java.security.spec.*;
import xjava.security.*;
import cryptix.provider.rsa.*;
import cornell.herbivore.util.*;

public class HerbivoreClique {

    private static final boolean DEBUG = false;

    public static final int TYPE_UNKNOWN = 0x00;

    public static final int TYPE_ROUND = 0x01;

    public static final int TYPE_JOIN = 0x02;

    public static final int TYPE_LEAVE = 0x03;

    public static final int SOCK_BUF_SIZE = 32000;

    public static final int MINCLIQUESIZE = 1000;

    public static final int MAXCLIQUESIZE = 1000;

    public static final int EXPECTEDCLIQUESIZE = (MINCLIQUESIZE + MAXCLIQUESIZE) / 2;

    public static final int MAX_BACKOFF = 1;

    public static final int MAX_MEMBERSHIP_BACKOFF = 5000;

    public static final int INITIAL_MEMBERSHIP_BACKOFF = 1000;

    static final int CHALLENGE_LENGTH = 127;

    static final int INITIAL_SEED_SIZE = 128;

    static final int PUZZLE_DIFFICULTY = 1;

    private String localHostName;

    private int totalBytesSent = 0;

    protected int nSendersLastRound = 0;

    protected Vector members;

    protected int curepoch = 0;

    protected int nextepoch = 0;

    protected Herbivore herbivore = null;

    public HerbivoreChallenge hc = null;

    protected HerbivorePastryNode hpn = null;

    protected HerbivoreCliqueControlLoop hccl = null;

    private HerbivoreQueue outgoingPackets;

    private Hashtable serverSockets = null;

    protected Hashtable knownHostsBySocket;

    protected Hashtable knownHostsByLocation;

    protected short transmissionLength;

    public static final int vetoLen = 8;

    protected int backOffStep;

    protected boolean isRunning = true;

    protected int round = 0;

    protected int aborted = 0;

    protected int droppedpackets = 0;

    protected String name = null;

    protected HerbivoreCliqueID cliqueID;

    private HerbivoreHostDescriptor getMe(int controlport, HerbivoreChallenge hc) throws UnknownHostException {
        HerbivoreHostDescriptor me;
        me = new HerbivoreHostDescriptor(InetAddress.getLocalHost().getHostName(), InetAddress.getLocalHost(), controlport);
        me.setPuzzleKey(hc.getY());
        me.setPublicKey(hc.getKeyPair().getPublicKey());
        me.setLocation(hc.location());
        me.setMe();
        return me;
    }

    private static void eat200(HerbivoreConnection hconn) throws Exception {
        if (hconn.readInt() != 200) {
            throw new Exception("malformed clique control input");
        }
    }

    private static void eatOK(HerbivoreConnection hconn) throws Exception {
        String str = hconn.readStr();
        if (!str.equals("OK")) throw new Exception("Malformed clique control input :" + str);
    }

    private static void eat200OK(HerbivoreConnection hconn) throws Exception {
        eat200(hconn);
        eatOK(hconn);
    }

    private Vector getCliqueMembershipList(String bootstrapHost, int bootstrapControlPort, Vector cliquemembers) {
        int backoff = INITIAL_MEMBERSHIP_BACKOFF;
        boolean allok = false;
        HerbivoreConnection hconn = null;
        while (!allok) {
            try {
                hconn = new HerbivoreConnection(bootstrapHost, bootstrapControlPort);
                int connectbackoff = 1000;
                while (true) {
                    if (DEBUG) Log.info("Getting membership list for clique \"" + HerbivoreUtil.createHexStringFromBytes(hc.location()) + "\"\n");
                    hconn.writeStr("GETCLIQUELIST \"" + HerbivoreUtil.createHexStringFromBytes(hc.getKeyPair().getPublicKey()) + "\"" + " \"" + HerbivoreUtil.createHexStringFromBytes(hc.getY()) + "\n");
                    int response = hconn.readInt();
                    String explanation = hconn.readStr();
                    if (response >= 200 && response < 300) {
                        break;
                    } else {
                        try {
                            if (hconn != null) {
                                hconn.close();
                                hconn = null;
                            }
                            Log.info("Clique is busy, waiting...");
                            Thread.sleep(connectbackoff);
                            if (connectbackoff < 5000) connectbackoff += 1000;
                        } catch (Exception e) {
                        }
                    }
                }
                eat200(hconn);
                int nmembers = hconn.readInt();
                int epoch = hconn.readInt();
                Log.info("Number of previous members " + nmembers);
                for (int i = 0; i < nmembers; ++i) {
                    String hname, ipaddrstr;
                    int port;
                    if (DEBUG) Log.info("Reading a host");
                    eat200(hconn);
                    hname = hconn.readStr();
                    ipaddrstr = hconn.readStr();
                    InetAddress ia = InetAddress.getByName(ipaddrstr);
                    if (ia == null) ia = InetAddress.getByName(hname);
                    if (ia == null) {
                        System.out.println("Cannot find host " + hname);
                    }
                    port = hconn.readInt();
                    HerbivoreHostDescriptor host;
                    host = new HerbivoreHostDescriptor(hname, ia, port);
                    host.setPublicKey(hconn.readBytes());
                    host.setPuzzleKey(hconn.readBytes());
                    host.setLocation(hconn.readBytes());
                    if (DEBUG) Log.info("Adding node " + hname + " " + ipaddrstr + " " + port);
                    cliquemembers.add(host);
                }
                allok = true;
            } catch (Exception e) {
                if (hconn != null) {
                    hconn.close();
                    hconn = null;
                }
                Log.info("Clique busy, will retry in " + backoff / 1000 + " seconds...");
                try {
                    Thread.sleep(backoff);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                backoff = ((backoff * 2) > MAX_MEMBERSHIP_BACKOFF) ? MAX_MEMBERSHIP_BACKOFF : backoff * 2;
            }
        }
        if (hconn != null) {
            hconn.close();
            hconn = null;
        }
        return cliquemembers;
    }

    private void writeAll(Vector clique, boolean usecontrol, String str) throws Exception {
        for (int i = 0; i < clique.size(); ++i) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.elementAt(i);
            HerbivoreConnection conn = usecontrol ? hde.controlconn : hde.dataconn;
            conn.writeStr(str);
        }
    }

    private void readAll(Vector clique, boolean usecontrol) throws Exception {
        for (int i = 0; i < clique.size(); ++i) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.elementAt(i);
            HerbivoreConnection conn = usecontrol ? hde.controlconn : hde.dataconn;
            try {
                eat200OK(conn);
            } catch (Exception e) {
                throw new Exception("Host not ok with commit");
            }
        }
    }

    public void killAll() {
        Vector clique = (Vector) members.get(curepoch);
        try {
            writeAll(clique, true, "DEBUGDIE");
        } catch (Exception e) {
        }
        for (int i = 0; i < clique.size(); ++i) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.elementAt(i);
            if (hde.controlconn != null) {
                hde.controlconn.close();
                hde.controlconn = null;
            }
            if (hde.dataconn != null) {
                hde.dataconn.close();
                hde.dataconn = null;
            }
        }
    }

    private Vector readResponses(Vector clique, boolean usecontrol) throws Exception {
        Vector responses = new Vector();
        for (int i = 0; i < clique.size(); ++i) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.elementAt(i);
            HerbivoreConnection conn = usecontrol ? hde.controlconn : hde.dataconn;
            try {
                eat200(conn);
                responses.add(i, conn.readBytes());
            } catch (Exception e) {
                throw new Exception("Host not ok with commit");
            }
        }
        return responses;
    }

    protected void enqueueOutgoing(HerbivorePacket hd) {
        this.outgoingPackets.enqueue(hd);
    }

    protected HerbivoreClique(Herbivore herb, String name, String bootstrapHost, int bootstrapPort, int bootstrapControlPort, int controlport, short transmissionLength, int backOffStep) throws IOException {
        boolean notamember = true;
        this.herbivore = herb;
        this.name = name;
        this.transmissionLength = transmissionLength;
        this.backOffStep = backOffStep;
        this.localHostName = InetAddress.getLocalHost().getHostName();
        this.cliqueID = new HerbivoreCliqueID(name);
        this.knownHostsBySocket = new Hashtable();
        this.knownHostsByLocation = new Hashtable();
        this.outgoingPackets = new HerbivoreQueue(EXPECTEDCLIQUESIZE, false);
        this.serverSockets = new Hashtable();
        this.members = new Vector();
        Vector hosts = new Vector();
        hc = HerbivoreChallenge.compute(localHostName + ":" + controlport + ":" + name, PUZZLE_DIFFICULTY);
        boolean firstnode = false;
        int pastrybootstrapbackoff = 1000;
        while (true) {
            try {
                Socket s = new Socket(bootstrapHost, bootstrapPort);
                s.close();
                break;
            } catch (Exception e) {
                if (localHostName.equals(bootstrapHost)) {
                    firstnode = true;
                    break;
                } else {
                    try {
                        Log.info("Waiting for bootstrapHost " + bootstrapHost + ":" + bootstrapPort + " to boot up.");
                        Thread.sleep(pastrybootstrapbackoff);
                        if (pastrybootstrapbackoff < 5000) pastrybootstrapbackoff += 1000;
                    } catch (InterruptedException ie) {
                        Log.exception(ie);
                    }
                }
            }
        }
        if (firstnode) {
            if (DEBUG) Log.info("The first clique is " + HerbivoreUtil.createHexStringFromBytes(hc.location()));
            hpn = new HerbivorePastryNode(bootstrapHost, bootstrapPort, hc);
            hosts.add(getMe(controlport, hc));
            members.add(curepoch, hosts);
            hccl = new HerbivoreCliqueControlLoop(hpn.pn, this, controlport);
            (new cliqueLoop(this)).start();
            return;
        }
        while (notamember) {
            int phase = 1;
            try {
                getCliqueMembershipList(bootstrapHost, bootstrapControlPort, hosts);
                phase++;
                for (int i = 0; i < hosts.size(); ++i) {
                    HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                    if (DEBUG) Log.info("Connecting to " + hde.hostname + " " + hde.controlport);
                    HerbivoreConnection newconn = null;
                    ;
                    while (true) {
                        try {
                            newconn = new HerbivoreConnection(hde);
                            break;
                        } catch (ConnectException ce) {
                            if (newconn != null) {
                                newconn.close();
                                newconn = null;
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ie) {
                            }
                        }
                    }
                    hde.setControlConnection(newconn);
                    newconn = null;
                    while (true) {
                        try {
                            newconn = new HerbivoreConnection(hde);
                            break;
                        } catch (ConnectException ce) {
                            if (newconn != null) {
                                newconn.close();
                                newconn = null;
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException ie) {
                            }
                        }
                    }
                    hde.setDataConnection(newconn);
                }
                phase++;
                byte[] challenge = new byte[CHALLENGE_LENGTH];
                herbivore.random.nextBytes(challenge);
                writeAll(hosts, false, "INITCONNECTION \"" + InetAddress.getLocalHost().getHostName() + "\" " + controlport + " \"" + HerbivoreUtil.createHexStringFromBytes(hc.getKeyPair().getPublicKey()) + "\" \"" + HerbivoreUtil.createHexStringFromBytes(hc.getY()) + "\" \"" + HerbivoreUtil.createHexStringFromBytes(challenge) + "\" " + "\n");
                for (int i = 0; i < hosts.size(); ++i) {
                    HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                    HerbivoreConnection conn = hde.dataconn;
                    eat200OK(conn);
                    eat200(conn);
                    byte[] encryptedresponse = conn.readBytes();
                    Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS#7", "Cryptix");
                    cipher.initDecrypt(new RawRSAPrivateKey(new ByteArrayInputStream(hc.getKeyPair().getPrivateKey())));
                    byte[] decryptedresponse = cipher.crypt(encryptedresponse);
                    ByteArrayInputStream bis = new ByteArrayInputStream(decryptedresponse);
                    ObjectInputStream ois = new ObjectInputStream(bis);
                    HerbivoreCliqueJoinResponse hcjr = (HerbivoreCliqueJoinResponse) ois.readObject();
                    Signature sig = Signature.getInstance("SHA-1/RSA", "Cryptix");
                    sig.initVerify(new RawRSAPublicKey(new ByteArrayInputStream(hde.publicKey)));
                    sig.update(challenge);
                    if (!sig.verify(hcjr.signedchallenge)) {
                        throw new Exception("Peer authentication failure - caught an impostor peer who could not meet my challenge");
                    } else {
                        if (DEBUG) Log.info("Peer " + hde.hostname + " authenticated");
                    }
                    if (!HerbivoreChallenge.check(hde.location, hcjr.publickey, hcjr.puzzlekey, PUZZLE_DIFFICULTY)) throw new Exception("Peer authentication failure - caught an impostor peer connected at an address for which it lacks credentials");
                    MessageDigest sha = MessageDigest.getInstance("SHA-1");
                    sha.update(hcjr.seed);
                    byte[] hash = sha.digest();
                    hde.setSeed(hcjr.seed);
                    conn.writeStr("SEEDSETUP \"" + HerbivoreUtil.createHexStringFromBytes(hash) + "\"");
                    eat200OK(conn);
                }
                phase++;
                writeAll(hosts, true, "ADDME \"" + HerbivoreUtil.createHexStringFromBytes(hc.location()) + "\" " + "\n");
                readAll(hosts, true);
                phase++;
                int newepoch = -1;
                for (int i = 0; i < hosts.size(); ++i) {
                    HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                    HerbivoreConnection conn = hde.controlconn;
                    int suggestedepoch = conn.readInt();
                    if (newepoch < 0) newepoch = suggestedepoch;
                    if (newepoch != suggestedepoch) throw new Exception("Join failure - clique disagrees about the current epoch" + newepoch + " " + suggestedepoch);
                }
                if (newepoch < 0) throw new Exception("Join failure - no epoch found");
                curepoch = newepoch;
                if (DEBUG) Log.info("Clique epoch is ..." + curepoch);
                hosts.add(getMe(controlport, hc));
                computeCliqueMembers(hosts);
                Log.info("New clique membership is...");
                dumpMembers(hosts);
                if (members.size() < curepoch) {
                    for (int i = 0; i < curepoch; ++i) members.add(i, null);
                }
                members.add(curepoch, hosts);
                notamember = false;
            } catch (Exception e) {
                Log.exception(e);
                System.exit(1);
                if (phase == 1) {
                    Log.error("Cannot connect to the bootstrap node\n");
                    Log.exception(e);
                    System.exit(-1);
                }
                if (phase > 1) {
                    Log.error("Cannot connect to all members of the clique\n");
                    Log.exception(e);
                    try {
                        for (int i = 0; i < hosts.size(); ++i) {
                            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                            if (hde != null && hde.dataconn != null) {
                                hde.dataconn.close();
                                hde.dataconn = null;
                            }
                            if (hde != null && hde.controlconn != null) {
                                hde.controlconn.close();
                                hde.controlconn = null;
                            }
                        }
                        hosts.clear();
                    } catch (Exception e2) {
                    }
                }
            }
        }
        hpn = new HerbivorePastryNode(bootstrapHost, bootstrapPort, hc);
        hccl = new HerbivoreCliqueControlLoop(hpn.pn, this, controlport);
        (new cliqueLoop(this)).start();
    }

    public final short getMsgLength() {
        return transmissionLength;
    }

    /**
     * Returns the number of hosts participating in this clique
     */
    public final synchronized int size() {
        Vector hosts = (Vector) members.get(curepoch);
        return hosts.size();
    }

    protected static void computeCliqueMembers(Vector hosts) {
        sortMembers(hosts);
        if (hosts.size() > MAXCLIQUESIZE && hosts.size() % 2 == 0) {
            int nHosts = hosts.size();
            int myNumber = 0;
            int myHalf = 0;
            int theOtherHalf = 0;
            for (int i = 0; i < nHosts; i++) {
                HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                if (hde.isThisMe()) {
                    myNumber = i;
                    break;
                }
            }
            myHalf = myNumber / (nHosts / 2);
            theOtherHalf = 1 - myHalf;
            for (int i = nHosts / 2 - 1; i >= 0; --i) {
                hosts.remove(theOtherHalf * nHosts / 2 + i);
            }
        }
    }

    private static void sortMembers(Vector hosts) {
        for (int i = 0; i < hosts.size(); i++) {
            for (int j = i + 1; j < hosts.size(); j++) {
                HerbivoreHostDescriptor hdetemp, hde1, hde2;
                hde1 = (HerbivoreHostDescriptor) hosts.elementAt(i);
                hde2 = (HerbivoreHostDescriptor) hosts.elementAt(j);
                if (hde1 == null || hde2 == null || hde1.ipaddr == null || hde2.ipaddr == null) {
                    System.out.println("Malformed host list, no ip address");
                }
                if (HerbivoreUtil.compareInetAddresses(hde1.ipaddr, hde2.ipaddr) > 0 || (HerbivoreUtil.compareInetAddresses(hde1.ipaddr, hde2.ipaddr) == 0 && hde1.controlport > hde2.controlport)) {
                    hdetemp = (HerbivoreHostDescriptor) hosts.elementAt(i);
                    hosts.setElementAt(hosts.elementAt(j), i);
                    hosts.setElementAt(hdetemp, j);
                }
            }
        }
    }

    /**
     * create a DatagramSocket, suitable for sending packets from
     * this clique
     *
     * @param port recipient host port number, used for demultiplexing on the receiving side
     * @param hre  descriptor for the receiving host. Contains the public
     *              key of the recipient for unicast packets and is used
     *              to encrypt the packet header. If hre is null, indicates
     *              clique-level broadcast.
     */
    public HerbivoreUnicastDatagramSocket createUnicastDatagramSocket(short port, HerbivoreRemoteEndpoint hre) {
        return new HerbivoreUnicastDatagramSocket(this, port, hre);
    }

    public HerbivoreBroadcastDatagramSocket createBroadcastDatagramSocket(short port) {
        return new HerbivoreBroadcastDatagramSocket(this, port);
    }

    /**
     * create a StreamSocket, suitable for sending packets from
     * this clique
     *
     * @param port recipient host port number, used for demultiplexing on the receiving side
     * @param hre  descriptor for the receiving host. Contains the public
     *              key of the recipient for unicast packets and is used
     *              to encrypt the packet header. If hre is null, indicates
     *              clique-level broadcast.
     */
    public HerbivoreUnicastStreamSocket createUnicastStreamSocket(short port, HerbivoreRemoteEndpoint hre) {
        return new HerbivoreUnicastStreamSocket(this, port, hre);
    }

    public HerbivoreBroadcastStreamSocket createBroadcastStreamSocket(short port) {
        return new HerbivoreBroadcastStreamSocket(this, port);
    }

    /**
     * create a server-side datagram socket
     *
     * @param port port to listen to
     */
    public HerbivoreUnicastDatagramServerSocket createUnicastDatagramServerSocket(short port) {
        HerbivoreUnicastDatagramServerSocket hdss = new HerbivoreUnicastDatagramServerSocket(this, port);
        serverSockets.put(new Short(port), hdss);
        return hdss;
    }

    public HerbivoreBroadcastDatagramServerSocket createBroadcastDatagramServerSocket(short port) {
        HerbivoreBroadcastDatagramServerSocket hdss = new HerbivoreBroadcastDatagramServerSocket(this, port);
        serverSockets.put(new Short(port), hdss);
        return hdss;
    }

    /**
     * create a server-side stream socket
     *
     * @param port port to listen to
     * @param backlog number of connections to this port that can be active
     *                without a thread doing an accept. If more than backlog 
     *                streams are formed to a server stream socket without 
     *                the server performing an accept, the rest will be discarded.
     */
    public HerbivoreUnicastStreamServerSocket createUnicastStreamServerSocket(short port, int backlog) {
        HerbivoreUnicastStreamServerSocket hdss = new HerbivoreUnicastStreamServerSocket(this, port, backlog);
        serverSockets.put(new Short(port), hdss);
        return hdss;
    }

    public HerbivoreBroadcastStreamServerSocket createBroadcastStreamServerSocket(short port, int backlog) {
        HerbivoreBroadcastStreamServerSocket hdss = new HerbivoreBroadcastStreamServerSocket(this, port, backlog);
        serverSockets.put(new Short(port), hdss);
        return hdss;
    }

    /**
     * used when closing a server socket to remove dangling references
     */
    protected void removeServerSocket(HerbivoreServerSocket socket) {
        serverSockets.remove(new Short(socket.getPort()));
    }

    /**
     * Dump members of a clique
     */
    private void dumpMembers(Vector hosts) {
        if (hosts == null) return;
        for (int i = 0; i < hosts.size(); i++) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
            if (hde == null) return;
            Log.info("\t" + i + (hde.isThisMe() ? "ME" : "") + ": " + hde.hostname + ":" + hde.controlport + "/" + hde.ipaddr + " " + ((hde.dataconn != null) ? hde.dataconn.s.toString() : "null data socket"));
        }
    }

    protected boolean isMemberOfThisClique(HerbivoreRemoteEndpoint hre) {
        Vector hosts = (Vector) members.get(curepoch);
        boolean ismember = false;
        for (int i = 0; i < hosts.size(); i++) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
            if (HerbivoreUtil.equalsBytes(hde.publicKey, hre.getPublicKey())) ismember = true;
        }
        return ismember;
    }

    private void dumpParameters() {
        Log.info(this, "*** Host: " + localHostName + " ***");
        Log.info(this, "[Hosts-in-clique] " + ((Vector) members.get(curepoch)).size());
        Log.info(this, "[Transmission-block-length] " + transmissionLength);
        Log.info(this, "[Backoff-step-size] " + backOffStep);
        Log.info(this, "[Max-backoff-multiplier] " + MAX_BACKOFF);
    }

    public String toString() {
        return "[" + name + "]";
    }

    public void dumpInfo() {
        Log.info(this, "[Bytes-sent] " + totalBytesSent);
        Log.info(this, "[Current-round] " + round);
    }

    public boolean isRunning() {
        return isRunning;
    }

    public HerbivoreCliqueID getID() {
        return cliqueID;
    }

    public synchronized byte[] getRandomMemberPublicKey() {
        Vector hosts = (Vector) members.get(curepoch);
        int i = HerbivoreRandom.nextInt(hosts.size());
        HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
        return hde.publicKey;
    }

    public synchronized boolean isMember(byte[] participantID) {
        Enumeration e = ((Vector) members.get(curepoch)).elements();
        while (e.hasMoreElements()) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) e.nextElement();
            if (HerbivoreUtil.equalsBytes(hde.hostname.getBytes(), participantID)) return true;
        }
        return false;
    }

    private class cliqueLoop extends Thread {

        HerbivoreClique clique = null;

        int nhosts = -1;

        HerbivorePacket[] hpackets = null;

        HerbivorePacket[] nextHPackets = null;

        byte[][] msgs = null;

        byte[][] nextMsgs = null;

        byte[] receivedMsg = new byte[transmissionLength];

        int nsenders = 0;

        int[] pos = null;

        int backOff = 0;

        int sqrtTransmissionLength = (int) Math.sqrt(transmissionLength * 8);

        public cliqueLoop(HerbivoreClique clique) {
            this.clique = clique;
        }

        private void updateSizes(int newSize) {
            nhosts = newSize;
            hpackets = new HerbivorePacket[newSize];
            nextHPackets = new HerbivorePacket[newSize];
            msgs = new byte[newSize][];
            nextMsgs = new byte[newSize][];
            pos = new int[newSize];
            for (int c = 0; c < newSize; c++) pos[c] = -1;
        }

        private void backOff() {
            if (nsenders == 0) {
                if (backOff == 0) backOff = 1; else if (backOff < MAX_BACKOFF) backOff = backOff * 2;
                try {
                    Thread.sleep(backOff * backOffStep);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            } else {
                backOff = 0;
            }
        }

        private HerbivoreHostDescriptor getHost(Vector hosts, int i) {
            return (HerbivoreHostDescriptor) hosts.elementAt(i);
        }

        private void processPacket(byte[] receivedMsg) {
            HerbivorePacket hp = null;
            try {
                switch(receivedMsg[0]) {
                    case HerbivorePacket.TYPE_UNICAST_DATAGRAM:
                        hp = new HerbivoreUnicastDatagramPacket(receivedMsg, clique, hc.getKeyPair().getPrivateKey());
                        break;
                    case HerbivorePacket.TYPE_BROADCAST_DATAGRAM:
                        hp = new HerbivoreBroadcastDatagramPacket(receivedMsg, clique);
                        break;
                    case HerbivorePacket.TYPE_UNICAST_STREAM:
                    case HerbivorePacket.TYPE_UNICAST_STREAM_TERMINATE:
                        hp = new HerbivoreUnicastStreamPacket(receivedMsg, clique, hc.getKeyPair().getPrivateKey());
                        break;
                    case HerbivorePacket.TYPE_BROADCAST_STREAM:
                    case HerbivorePacket.TYPE_BROADCAST_STREAM_TERMINATE:
                        hp = new HerbivoreBroadcastStreamPacket(receivedMsg, clique);
                        break;
                    default:
                        Log.error("Unknown packet type!");
                        return;
                }
            } catch (HerbivorePacketException hpe) {
            }
            if (hp != null && hp.isRecipient()) {
                if (DEBUG) System.out.println("Packet arrived for port " + hp.getDestPort());
                HerbivoreServerSocket hss = (HerbivoreServerSocket) serverSockets.get(new Short(hp.getDestPort()));
                if (hss != null) hss.receivedPacket(hp); else {
                    if (true || DEBUG) System.out.println("Dropping packet for port " + hp.getDestPort());
                    clique.droppedpackets++;
                }
            }
        }

        public void run() {
            if (DEBUG) Log.info("CliqueLoop started");
            Vector hosts = null;
            int myCenterTurn = -1;
            if (isRunning && nextepoch < curepoch) nextepoch = curepoch;
            while (isRunning) {
                synchronized (clique) {
                    if (hosts == null || (curepoch < nextepoch && (round % ((Vector) members.get(curepoch + 1)).size()) == 0)) {
                        if (hosts != null) {
                            curepoch++;
                            round = 0;
                            clique.notifyAll();
                        }
                        hosts = (Vector) members.get(curepoch);
                        if (nhosts != hosts.size()) {
                            updateSizes(hosts.size());
                        }
                        for (int i = 0; i < hosts.size(); i++) {
                            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                            if (hde.isThisMe()) {
                                myCenterTurn = i;
                                break;
                            }
                        }
                    }
                }
                if (nhosts <= 1) continue;
                if (DEBUG) System.out.println("XXX curepoch is " + curepoch + " nmembers " + members.size());
                if (DEBUG) dumpMembers(hosts);
                backOff();
                int myCurrentCenterTurn = (myCenterTurn - (round % nhosts)) % nhosts;
                if (myCurrentCenterTurn < 0) myCurrentCenterTurn += nhosts;
                int npackets = (nsenders < nhosts) ? nsenders : nhosts;
                if (npackets == 0) npackets = 1;
                try {
                    int reservationLength = (((nsenders == 0) ? 1 : nsenders) * sqrtTransmissionLength) / 8;
                    int[] packetLengths = new int[npackets];
                    for (int c = 0; c < packetLengths.length; c++) packetLengths[c] = 0;
                    for (int c = 0; c < nsenders; c++) packetLengths[c % nhosts] += transmissionLength;
                    packetLengths[0] += (vetoLen + reservationLength);
                    if (DEBUG) {
                        Log.info("round #: " + round);
                        Log.info("npackets: " + npackets);
                        Log.info("nsenders: " + nsenders);
                        Log.info("myCenterTurn: " + myCenterTurn);
                        Log.info("myCurrentCenterTurn: " + myCurrentCenterTurn);
                        Log.info("transmissionLength: " + transmissionLength);
                    }
                    if (DEBUG) for (int c = 0; c < npackets; c++) Log.info("cliquesize " + nhosts + " length of packet " + (c + 1) + ":" + packetLengths[c]);
                    byte[][] packets = new byte[npackets][];
                    for (int c = 0; c < npackets; c++) packets[c] = new byte[packetLengths[c]];
                    backOff();
                    for (int c = 0; c < nhosts; c++) {
                        if (msgs[c] == null) {
                            msgs[c] = nextMsgs[c];
                            hpackets[c] = nextHPackets[c];
                            HerbivorePacket hp = (HerbivorePacket) outgoingPackets.dequeue();
                            if (DEBUG) Log.info("Picked an item " + ((hp != null) ? "YES" : "NO"));
                            nextHPackets[c] = hp;
                            nextMsgs[c] = (hp != null) ? hp.toByteArray() : null;
                            if (DEBUG) Log.info("Requested msg, got one?" + ((nextMsgs[c] != null) ? "Y" : "N"));
                        }
                    }
                    for (int c = 0; c < nhosts; c++) if ((msgs[c] != null) && (pos[c] != -1)) HerbivoreUtil.copyBytes(packets[pos[c] % nhosts], ((pos[c] / nhosts) * transmissionLength), msgs[c]);
                    int[] bit = new int[nhosts];
                    for (int c = 0; c < nextMsgs.length; c++) {
                        bit[c] = -1;
                        if (nextMsgs[c] != null) {
                            bit[c] = HerbivoreUtil.setRandomBit(packets[0], packetLengths[0] - reservationLength, reservationLength);
                        }
                    }
                    for (int c = 0; c < npackets; c++) {
                        for (int i = 0; i < hosts.size(); i++) {
                            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) hosts.elementAt(i);
                            if (hde.seed != null) hde.seed.crypt(packets[c]);
                        }
                    }
                    boolean isCenter = false;
                    for (int c = 0; c < npackets; c++) {
                        if (c == myCurrentCenterTurn) {
                            isCenter = true;
                            continue;
                        }
                        HerbivoreHostDescriptor hde = getHost(hosts, (c + round) % nhosts);
                        HerbivoreIO.writePacket(hde.dataconn.os, packets[c]);
                    }
                    if (isCenter) {
                        byte[][] centerPackets = new byte[nhosts][packetLengths[myCurrentCenterTurn]];
                        HerbivoreUtil.copyBytes(centerPackets[myCenterTurn], packets[myCurrentCenterTurn]);
                        HerbivoreIO.readPacket(hosts, myCenterTurn, centerPackets);
                        HerbivoreUtil.xor(centerPackets, packets[myCurrentCenterTurn]);
                        if (DEBUG) {
                            System.out.println("Received packet contents when center: ");
                            for (int i = 0; i < 100; ++i) {
                                if (i < packets[myCurrentCenterTurn].length) System.out.print(packets[myCurrentCenterTurn][i] + " ");
                            }
                            System.out.println("");
                        }
                        HerbivoreIO.writePacket(hosts, myCenterTurn, packets[myCurrentCenterTurn]);
                    }
                    for (int c = 0; c < npackets; c++) {
                        if (c == myCurrentCenterTurn) {
                            isCenter = true;
                            continue;
                        }
                        HerbivoreHostDescriptor hde = getHost(hosts, (c + round) % nhosts);
                        HerbivoreIO.readPacket(hde.dataconn.is, packets[c]);
                        if (DEBUG) {
                            System.out.println("Received packet contents from someone else: ");
                            for (int i = 0; i < 100; ++i) {
                                if (i < packets[c].length) System.out.print(packets[c][i] + " ");
                            }
                            System.out.println("");
                        }
                    }
                    for (int c = 0; c < npackets; c++) totalBytesSent += ((myCurrentCenterTurn == c) ? ((nhosts - 1) * packetLengths[c]) : packetLengths[c]);
                    for (int c = 0; c < msgs.length; c++) {
                        boolean transmissionSuccess = (msgs[c] == null) || ((pos[c] > -1) && HerbivoreUtil.equalsBytes(packets[pos[c] % nhosts], (pos[c] / nhosts) * transmissionLength, transmissionLength, msgs[c]));
                        if (transmissionSuccess && (msgs[c] != null)) {
                            processPacket(msgs[c]);
                            msgs[c] = null;
                            hpackets[c] = null;
                        } else {
                            outgoingPackets.enqueue(hpackets[c]);
                        }
                    }
                    for (int c = 0; c < nsenders; c++) {
                        if (!(HerbivoreUtil.arrayContainsInt(pos, c))) {
                            HerbivoreUtil.copyBytes(receivedMsg, 0, packets[c % nhosts], ((c / nhosts) * transmissionLength), transmissionLength);
                            processPacket(receivedMsg);
                        }
                    }
                    for (int c = 0; c < pos.length; c++) {
                        pos[c] = -1;
                        if (HerbivoreUtil.isSet(packets[0], packetLengths[0] - reservationLength, reservationLength, bit[c])) pos[c] = HerbivoreUtil.countHighBits(packets[0], packetLengths[0] - reservationLength, reservationLength, bit[c]);
                    }
                    nsenders = HerbivoreUtil.countHighBits(packets[0], packetLengths[0] - reservationLength, reservationLength);
                    round += npackets;
                } catch (IOException ie) {
                    Log.exception(ie);
                    isRunning = false;
                    System.exit(-1);
                }
            }
            Log.info("CliqueLoop terminated");
        }
    }
}
