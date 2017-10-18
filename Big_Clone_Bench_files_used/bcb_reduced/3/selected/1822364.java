package cornell.herbivore.system;

import cornell.herbivore.util.Log;
import cornell.herbivore.util.*;
import xjava.security.Cipher;
import java.math.*;
import javax.crypto.spec.*;
import javax.crypto.*;
import java.security.*;
import java.security.spec.*;
import java.io.*;
import xjava.security.*;
import cryptix.provider.rsa.*;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.*;
import java.io.*;
import java.net.*;
import java.util.*;
import rice.pastry.*;
import rice.pastry.direct.*;
import rice.pastry.standard.*;
import rice.pastry.join.*;
import rice.pastry.client.*;
import rice.pastry.security.*;
import rice.pastry.messaging.*;
import rice.pastry.routing.*;

class HerbivoreCliqueControlLoopThread implements Runnable {

    private HerbivoreCliqueControlLoop loop;

    private Thread myThread = null;

    /**
     * @param aloop is a CliqueControlLoop that this thread will monitor
     */
    HerbivoreCliqueControlLoopThread(HerbivoreCliqueControlLoop aloop) {
        loop = aloop;
        myThread = new Thread(this);
        myThread.start();
    }

    public void run() {
        loop.loop();
    }
}

/**
 * Every node has a clique control loop associated with each clique in
 * which it participates. The clique control loop helps it coordinate
 * joins into the clique.
 */
public class HerbivoreCliqueControlLoop extends PastryAppl {

    static final int READY = 0;

    static final int JOIN_IN_PROGRESS = 666;

    static final int N_WORKER_THREADS = 2;

    static final boolean DEBUG = false;

    public HerbivoreClique clique = null;

    String joinername = null;

    int joinerport = 0;

    byte[] joinerkey = null;

    byte[] joinerpuzzle = null;

    byte[] joinerchallenge = null;

    private ServerSocket controlsocket = null;

    private int state = READY;

    private int msgid = 0;

    private HerbivoreRendezvous hr;

    private HerbivoreLookupResponseMsg hlrm;

    private Vector threads;

    private static Address addr = new HerbivoreAddress();

    private static Credentials cred = new PermissiveCredentials();

    public HerbivoreCliqueControlLoop(PastryNode pn, HerbivoreClique clique, int controlport) {
        super(pn);
        try {
            this.clique = clique;
            this.controlsocket = new ServerSocket(controlport, 5);
            threads = new Vector();
            for (int i = 0; i < N_WORKER_THREADS; ++i) {
                threads.add(new HerbivoreCliqueControlLoopThread(this));
            }
            if (DEBUG) Log.info("Listening on control port: " + controlport);
        } catch (IOException ioe) {
            Log.exception(ioe);
            System.exit(-1);
        }
    }

    private int getIPdigit(int i) {
        return (i < 0) ? (i + 256) : i;
    }

    public void loop() {
        if (DEBUG) Log.info("CliqueControlLoop started for " + controlsocket);
        try {
            while (true) {
                Socket s = controlsocket.accept();
                HerbivoreConnection client = new HerbivoreConnection(s);
                boolean thereismore = true;
                boolean closesocket = false;
                while (thereismore) {
                    String command = client.readStr();
                    if (DEBUG) Log.info("Command is " + command + ", state=" + state);
                    if (command.equals("DEBUGDIE")) {
                        System.exit(0);
                    }
                    if (command.equals("QUIT")) {
                        thereismore = false;
                        closesocket = true;
                        continue;
                    }
                    if (command.equals("GETCLIQUELIST")) {
                        synchronized (clique) {
                            if (state != READY || clique.curepoch != clique.nextepoch) {
                                threads.add(new HerbivoreCliqueControlLoopThread(this));
                                while (state != READY || clique.curepoch != clique.nextepoch) {
                                    if (DEBUG) Log.info("Client going to sleep " + state + " " + clique.curepoch + " " + clique.nextepoch);
                                    clique.wait();
                                }
                            }
                            state = JOIN_IN_PROGRESS;
                            try {
                                byte[] newloc;
                                client.writeStr("200 OK\n");
                                String loc = client.readStr();
                                if (DEBUG) Log.info("Node wants to join at " + loc);
                                newloc = HerbivoreUtil.readBytesFromHexString(loc);
                                Vector ve = lookupCliqueMembers(newloc);
                                client.writeStr("200 " + ve.size() + " " + clique.curepoch + "\n");
                                Enumeration e = ve.elements();
                                while (e.hasMoreElements()) {
                                    HerbivoreSimpleHostDescriptor hde = (HerbivoreSimpleHostDescriptor) e.nextElement();
                                    byte[] ipa = hde.ipaddr.getAddress();
                                    client.writeStr("200 " + hde.hostname + " \"" + getIPdigit((int) ipa[0]) + "." + getIPdigit((int) ipa[1]) + "." + getIPdigit((int) ipa[2]) + "." + getIPdigit((int) ipa[3]) + "\" " + hde.controlport + " \"" + HerbivoreUtil.createHexStringFromBytes(hde.publicKey) + "\" \"" + HerbivoreUtil.createHexStringFromBytes(hde.puzzleKey) + "\" \"" + HerbivoreUtil.createHexStringFromBytes(hde.location) + "\"\n");
                                }
                                thereismore = false;
                            } catch (Exception e) {
                                Log.exception(e);
                                thereismore = false;
                            }
                        }
                        continue;
                    }
                    if (command.equals("INITCONNECTION")) {
                        joinername = client.readStr();
                        joinerport = client.readInt();
                        joinerkey = client.readBytes();
                        joinerpuzzle = client.readBytes();
                        byte[] challenge = client.readBytes();
                        if (DEBUG) Log.info("Node \"" + joinername + ":" + joinerport + "\" wants to connect");
                        client.writeStr("200 OK\n");
                        client.writeStr("200 ");
                        HerbivoreCliqueJoinResponse hcjr;
                        hcjr = new HerbivoreCliqueJoinResponse();
                        hcjr.publickey = clique.hc.getKeyPair().getPublicKey();
                        hcjr.puzzlekey = clique.hc.getY();
                        Signature sig = Signature.getInstance("SHA-1/RSA", "Cryptix");
                        sig.initSign(new RawRSAPrivateKey(new ByteArrayInputStream(clique.hc.getKeyPair().getPrivateKey())));
                        sig.update(challenge);
                        hcjr.signedchallenge = sig.sign();
                        hcjr.seed = new byte[HerbivoreClique.INITIAL_SEED_SIZE];
                        clique.herbivore.random.nextBytes(challenge);
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        ObjectOutputStream oos = new ObjectOutputStream(bos);
                        oos.writeObject(hcjr);
                        byte[] objbytes = bos.toByteArray();
                        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS#7", "Cryptix");
                        cipher.initEncrypt(new RawRSAPublicKey(new ByteArrayInputStream(joinerkey)));
                        byte[] encryptedresponse = cipher.crypt(objbytes);
                        HerbivoreHostDescriptor hde = new HerbivoreHostDescriptor(joinername, client.getRemoteIP(), joinerport);
                        hde.setDataConnection(client);
                        hde.setSeed(hcjr.seed);
                        hde.setPublicKey(joinerkey);
                        hde.setPuzzleKey(joinerpuzzle);
                        hde.setLocation(HerbivoreChallenge.computeLocation(joinerkey, joinerpuzzle));
                        synchronized (clique) {
                            clique.knownHostsBySocket.put(s, hde);
                            clique.knownHostsByLocation.put(new BigInteger(hde.location), hde);
                        }
                        if (DEBUG) System.out.println("Inserted element at " + hde.location);
                        client.writeBytes(encryptedresponse);
                        client.writeStr("\n");
                        continue;
                    }
                    if (command.equals("SEEDSETUP")) {
                        synchronized (clique) {
                            byte[] seedhash = client.readBytes();
                            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.knownHostsBySocket.get(s);
                            if (hde == null) {
                                throw new Exception("Host wants to join without initialization");
                            }
                            MessageDigest sha = MessageDigest.getInstance("SHA-1");
                            sha.update(hde.seedvalue);
                            byte[] hash = sha.digest();
                            if (seedhash == null || hash.length != seedhash.length) throw new Exception("Peer authentication failure - seed hash is of wrong length");
                            for (int i = 0; i < hash.length; ++i) if (hash[i] != seedhash[i]) throw new Exception("Peer authentication failure - seed hash is incorrect");
                            hde.authenticated();
                            if (DEBUG) Log.info("Peer " + hde.hostname + " authenticated");
                            client.writeStr("200 OK\n");
                            thereismore = false;
                            continue;
                        }
                    }
                    if (command.equals("PREPARETOCOMMIT")) {
                        continue;
                    }
                    if (command.equals("ABORT")) {
                        continue;
                    }
                    if (command.equals("COMMIT")) {
                        continue;
                    }
                    if (command.equals("ADDME")) {
                        byte[] location = client.readBytes();
                        synchronized (clique) {
                            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) clique.knownHostsByLocation.get(new BigInteger(location));
                            if (hde != null && hde.isAuthenticated()) {
                                if (DEBUG) Log.info("Adding to clique " + hde.hostname + ":" + hde.controlport + "");
                                synchronized (clique) {
                                    clique.nextepoch++;
                                    Vector oldhostlist = (Vector) clique.members.elementAt(clique.curepoch);
                                    Vector newhostlist = (Vector) oldhostlist.clone();
                                    newhostlist.add(hde);
                                    HerbivoreClique.computeCliqueMembers(newhostlist);
                                    clique.members.add(clique.nextepoch, newhostlist);
                                    state = READY;
                                    clique.notifyAll();
                                }
                                client.writeStr("200 OK " + clique.nextepoch + "\n");
                                thereismore = false;
                                closesocket = true;
                                continue;
                            }
                        }
                    }
                }
                if (closesocket) {
                    Log.info("Closing socket to " + client);
                    client.close();
                }
            }
        } catch (SocketException se) {
            Log.exception(se);
        } catch (Exception e) {
            Log.exception(e);
            System.exit(-1);
        }
    }

    private static class HerbivoreAddress implements Address {

        private int myCode = 0x1984abcd;

        public int hashCode() {
            return myCode;
        }

        public boolean equals(Object obj) {
            return (obj instanceof HerbivoreAddress);
        }

        public String toString() {
            return "[HerbivoreAddress]";
        }
    }

    public Vector lookupCliqueMembers(byte destination[]) {
        NodeId destid = new NodeId(destination);
        if (DEBUG) System.out.println("Sending lookup message from " + getNodeId() + " to destination " + destid);
        Message msg = new HerbivoreLookupMsg(addr, getNodeId(), destid, ++msgid);
        hr = new HerbivoreRendezvous();
        if (DEBUG) System.out.println("Waiting for response");
        routeMsg(destid, msg, cred, new SendOptions());
        hr.condwait();
        if (DEBUG) System.out.println("Signalled, returning");
        return hlrm.cliquemembers;
    }

    public Address getAddress() {
        return addr;
    }

    public Credentials getCredentials() {
        return cred;
    }

    public void messageForAppl(Message msg) {
        if (DEBUG) System.out.println("Received " + msg + " at " + getNodeId());
        if (msg instanceof HerbivoreLookupMsg) {
            if (DEBUG) System.out.println("Msg id " + msgid);
            HerbivoreLookupMsg hlm = (HerbivoreLookupMsg) msg;
            Message rmsg = new HerbivoreLookupResponseMsg(clique, addr, hlm.target, hlm.source, hlm.msgid);
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(rmsg);
            } catch (Exception e) {
                Log.exception(e);
            }
            routeMsg(hlm.source, rmsg, cred, new SendOptions());
        }
        if (msg instanceof HerbivoreLookupResponseMsg) {
            HerbivoreLookupResponseMsg ahlrm = (HerbivoreLookupResponseMsg) msg;
            if (DEBUG) System.out.println("Msg id " + msgid);
            if (ahlrm.msgid == msgid) {
                hlrm = ahlrm;
                hr.condnotify();
            }
        }
    }

    /**
     * Invoked on intermediate nodes in routing path.
     *
     * @param msg Message that's passing through this node.
     * @param key destination
     * @param nextHop next hop
     * @param opt send options
     * @return true if message needs to be forwarded according to plan.
     */
    public boolean enrouteMessage(Message msg, NodeId key, NodeId nextHop, SendOptions opt) {
        return true;
    }

    /**
     * Invoked upon change to leafset.
     *
     * @param nh node handle that got added/removed
     * @param wasAdded added (true) or removed (false)
     */
    public void leafSetChange(NodeHandle nh, boolean wasAdded) {
        if (DEBUG) {
            System.out.print("In " + getNodeId() + "'s leaf set, " + "node " + nh.getNodeId() + " was ");
            if (wasAdded) System.out.println("added"); else System.out.println("removed");
        }
    }

    /**
     * Invoked upon change to routing table.
     *
     * @param nh node handle that got added/removed
     * @param wasAdded added (true) or removed (false)
     */
    public void routeSetChange(NodeHandle nh, boolean wasAdded) {
        if (DEBUG) {
            System.out.print("In " + getNodeId() + "'s route set, " + "node " + nh.getNodeId() + " was ");
            if (wasAdded) System.out.println("added"); else System.out.println("removed");
        }
    }

    /**
     * Invoked by {RMI,Direct}PastryNode when the node has something in its
     * leaf set, and has become ready to receive application messages.
     */
    public void notifyReady() {
    }
}

class HerbivoreLookupMsg extends Message {

    public NodeId source;

    public NodeId target;

    public int msgid;

    public HerbivoreLookupMsg(Address addr, NodeId src, NodeId tgt, int mid) {
        super(addr);
        source = src;
        target = tgt;
        msgid = mid;
    }

    public String toString() {
        String s = "";
        s += "{Lookup #" + msgid + " for Clique#" + target + " from " + source + "}";
        return s;
    }
}

class HerbivoreSimpleHostDescriptor implements Serializable {

    public String hostname;

    public InetAddress ipaddr;

    public int controlport;

    public byte[] location;

    public byte[] publicKey;

    public byte[] puzzleKey;

    public HerbivoreSimpleHostDescriptor(HerbivoreHostDescriptor hde) {
        hostname = hde.hostname;
        ipaddr = hde.ipaddr;
        controlport = hde.controlport;
        location = hde.location;
        publicKey = hde.publicKey;
        puzzleKey = hde.puzzleKey;
    }
}

class HerbivoreLookupResponseMsg extends Message implements Serializable {

    public NodeId source;

    public NodeId target;

    public Vector cliquemembers;

    public int msgid;

    public HerbivoreLookupResponseMsg(HerbivoreClique clique, Address addr, NodeId src, NodeId tgt, int mid) {
        super(addr);
        source = src;
        target = tgt;
        msgid = mid;
        cliquemembers = new Vector();
        Vector hosts = (Vector) clique.members.get(clique.curepoch);
        Enumeration e = hosts.elements();
        while (e.hasMoreElements()) {
            HerbivoreHostDescriptor hde = (HerbivoreHostDescriptor) e.nextElement();
            cliquemembers.add(new HerbivoreSimpleHostDescriptor(hde));
        }
    }

    public String toString() {
        String s = "";
        s += "{LookupResponse #" + msgid + " for Clique#" + target + " from " + source + "}";
        return s;
    }
}
