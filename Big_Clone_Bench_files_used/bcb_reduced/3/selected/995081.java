package rice.scribe.testing;

import rice.pastry.*;
import rice.pastry.rmi.*;
import rice.pastry.standard.*;
import rice.pastry.join.*;
import rice.pastry.client.*;
import rice.pastry.messaging.*;
import rice.pastry.security.*;
import rice.pastry.routing.*;
import rice.pastry.leafset.*;
import rice.pastry.dist.*;
import rice.scribe.*;
import rice.scribe.maintenance.*;
import java.util.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.security.*;

/**
 * @(#) DistScribeRegrTest.java
 *
 * A test suite for Scribe with RMI/WIRE.
 *
 * @version $Id: DistScribeRegrTest.java,v 1.1.1.1 2003/06/17 21:10:46 egs Exp $
 *
 * @author Animesh Nandi
 * @author Atul Singh
 */
public class DistScribeRegrTest {

    private PastryNodeFactory factory;

    private Vector pastryNodes;

    private Random rng;

    public Vector distClients;

    public Vector localNodes;

    private static int port = 5009;

    private static String bshost = null;

    private static int bsport = 5009;

    private static int numNodes = 5;

    public Integer num = new Integer(0);

    public static int NUM_TOPICS = 5;

    public static int UNSUBSCRIBE_LIMIT = 50;

    public static double UNSUBSCRIBE_PROBABILITY = 0.1;

    private static Hashtable numUnsubscribed = null;

    public static double fractionUnsubscribedAllowed = 0.5;

    public static Object LOCK = new Object();

    public static int IDLE_TIME = 120;

    public static int protocol = DistPastryNodeFactory.PROTOCOL_RMI;

    public DistScribeRegrTest() {
        int i;
        NodeId topicId;
        numUnsubscribed = new Hashtable();
        for (i = 0; i < NUM_TOPICS; i++) {
            topicId = generateTopicId(new String("Topic " + i));
            numUnsubscribed.put(topicId, new Integer(0));
        }
        factory = DistPastryNodeFactory.getFactory(new RandomNodeIdFactory(), protocol, port);
        pastryNodes = new Vector();
        distClients = new Vector();
        rng = new Random(PastrySeed.getSeed());
        localNodes = new Vector();
    }

    public static int getNumUnsubscribed(NodeId topicId) {
        return ((Integer) numUnsubscribed.get(topicId)).intValue();
    }

    public static void incrementNumUnsubscribed(NodeId topicId) {
        int count;
        count = ((Integer) numUnsubscribed.get(topicId)).intValue();
        numUnsubscribed.remove(topicId);
        numUnsubscribed.put(topicId, new Integer(count + 1));
    }

    private NodeHandle getBootstrap() {
        InetSocketAddress addr = null;
        if (bshost != null) addr = new InetSocketAddress(bshost, bsport); else {
            try {
                addr = new InetSocketAddress(InetAddress.getLocalHost().getHostName(), bsport);
            } catch (UnknownHostException e) {
                System.out.println(e);
            }
        }
        NodeHandle bshandle = ((DistPastryNodeFactory) factory).getNodeHandle(addr);
        return bshandle;
    }

    public static NodeId generateTopicId(String topicName) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("No SHA support!");
        }
        md.update(topicName.getBytes());
        byte[] digest = md.digest();
        NodeId newId = new NodeId(digest);
        return newId;
    }

    /**
     * process command line args, set the security manager
     */
    private static void doInitstuff(String args[]) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-help")) {
                System.out.println("Usage: DistScribeRegrTest [-port p] [-protocol (rmi|wire)] [-bootstrap host[:port]] [-nodes n] [-help]");
                System.exit(1);
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-port") && i + 1 < args.length) {
                int p = Integer.parseInt(args[i + 1]);
                if (p > 0) port = p;
                break;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-bootstrap") && i + 1 < args.length) {
                String str = args[i + 1];
                int index = str.indexOf(':');
                if (index == -1) {
                    bshost = str;
                    bsport = port;
                } else {
                    bshost = str.substring(0, index);
                    bsport = Integer.parseInt(str.substring(index + 1));
                    if (bsport <= 0) bsport = port;
                }
                break;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-nodes") && i + 1 < args.length) {
                int n = Integer.parseInt(args[i + 1]);
                if (n > 0) numNodes = n;
                break;
            }
        }
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-protocol") && i + 1 < args.length) {
                String s = args[i + 1];
                if (s.equalsIgnoreCase("wire")) protocol = DistPastryNodeFactory.PROTOCOL_WIRE; else if (s.equalsIgnoreCase("rmi")) protocol = DistPastryNodeFactory.PROTOCOL_RMI; else System.out.println("ERROR: Unsupported protocol: " + s);
                break;
            }
        }
    }

    /**
     * Create a Pastry node and add it to pastryNodes. Also create a client
     * application for this node, and spawn off a separate thread for it.
     *
     * @return the PastryNode on which the Scribe application exists
     */
    public PastryNode makeScribeNode() {
        NodeHandle bootstrap = getBootstrap();
        PastryNode pn = factory.newNode(bootstrap);
        pastryNodes.addElement(pn);
        localNodes.addElement(pn.getNodeId());
        Credentials cred = new PermissiveCredentials();
        Scribe scribe = new Scribe(pn, cred);
        scribe.setTreeRepairThreshold(3);
        DistScribeRegrTestApp app = new DistScribeRegrTestApp(pn, scribe, cred, this);
        distClients.addElement(app);
        return pn;
    }

    /**
     * Usage: DistScribeRegrTest [-nodes n] [-port p] [-bootstrap bshost[:bsport]] [-protocol [wire,rmi]]
     *                      [-help].
     *
     * Ports p and bsport refer to WIRE/RMI port numbers (default = 5009).
     * Without -bootstrap bshost[:bsport], only localhost:p is used for bootstrap.
     */
    public static void main(String args[]) {
        int seed;
        PastryNode pn;
        Log.init(args);
        doInitstuff(args);
        seed = (int) System.currentTimeMillis();
        PastrySeed.setSeed(seed);
        System.out.println("seed used=" + seed);
        DistScribeRegrTest driver = new DistScribeRegrTest();
        pn = driver.makeScribeNode();
        bshost = null;
        synchronized (pn) {
            while (!pn.isReady()) {
                try {
                    pn.wait();
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }
        }
        for (int i = 1; i < numNodes; i++) {
            driver.makeScribeNode();
        }
        if (Log.ifp(5)) System.out.println(numNodes + " nodes constructed");
    }
}
