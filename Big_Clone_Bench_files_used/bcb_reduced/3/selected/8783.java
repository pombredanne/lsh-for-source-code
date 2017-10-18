package rice.scribe.testing;

import rice.pastry.*;
import rice.pastry.join.*;
import rice.pastry.direct.*;
import rice.pastry.messaging.*;
import rice.scribe.*;
import rice.scribe.messaging.*;
import rice.pastry.security.*;
import rice.pastry.standard.*;
import rice.pastry.leafset.*;
import rice.pastry.routing.*;
import java.util.*;
import java.io.*;
import java.security.*;

/**
 * @(#) DirectScribeMaintenanceTest.java
 *
 * A maintenance test suite for Scribe. It tests if the tree maintenance
 * activities automatically repair the multicast trees for the topics, in 
 * a dynamic scenario of concurrent node failures and node joins.
 *
 * @version $Id: DirectScribeMaintenanceTest.java,v 1.1.1.1 2003/06/17 21:10:46 egs Exp $
 *
 * @author Atul Singh
 * @author Animesh Nandi 
 */
public class DirectScribeMaintenanceTest {

    private DirectPastryNodeFactory factory;

    private NetworkSimulator simulator;

    private Vector pastryNodes;

    public Vector scribeClients;

    public Hashtable nodeIdToApp;

    private Random rng;

    private int appCount = 0;

    private Vector topicIds;

    private int n = 100;

    private int numTopics = 5;

    private int numIterations = 5;

    private int concurrentFailures = 5;

    private int concurrentJoins = 5;

    private int treeRepairThreshold = 2;

    private int nodesCurrentlyAlive = 0;

    private boolean currentAckFlagState;

    private class Data implements Serializable {

        String name = null;

        public Data(NodeId hostname) {
            name = new String("Scribe : Data transferred." + hostname.toString());
        }

        public String toString() {
            return name;
        }
    }

    public DirectScribeMaintenanceTest() {
        simulator = new EuclideanNetwork();
        factory = new DirectPastryNodeFactory(new RandomNodeIdFactory(), simulator);
        pastryNodes = new Vector();
        scribeClients = new Vector();
        nodeIdToApp = new Hashtable();
        rng = new Random(PastrySeed.getSeed());
        topicIds = new Vector();
    }

    private NodeHandle getBootstrap() {
        NodeHandle bootstrap = null;
        try {
            PastryNode lastnode = (PastryNode) pastryNodes.lastElement();
            bootstrap = lastnode.getLocalHandle();
        } catch (NoSuchElementException e) {
        }
        return bootstrap;
    }

    public void makeScribeNode() {
        PastryNode pn = factory.newNode(getBootstrap());
        pastryNodes.addElement(pn);
        Credentials cred = new PermissiveCredentials();
        Scribe scribe = new Scribe(pn, cred);
        DirectScribeMaintenanceTestApp scribeApp = new DirectScribeMaintenanceTestApp(pn, scribe, cred);
        scribeApp.m_scribe.setTreeRepairThreshold(treeRepairThreshold);
        scribeApp.m_appCount = appCount;
        nodeIdToApp.put(pn.getNodeId(), scribeApp);
        scribeClients.addElement(scribeApp);
        appCount++;
    }

    /**
     * get authoritative information about liveness of node.
     */
    private boolean isReallyAlive(NodeId id) {
        return simulator.isAlive(id);
    }

    /**
     * murder the node. comprehensively.
     */
    private void killNode(PastryNode pn) {
        NetworkSimulator enet = (NetworkSimulator) simulator;
        enet.setAlive(pn.getNodeId(), false);
    }

    /**
     * Kills a number of randomly chosen nodes comprehensively and acoordingly
     * updates the data structures maintained by the test suite to keep track
     * of the currently active applications.
     */
    private void killNodes(int num) {
        int appcountKilled;
        Set keySet;
        Iterator it;
        NodeId key;
        int appcounter = 0;
        DirectScribeMaintenanceTestApp scribeApp;
        if (num == 0) return;
        System.out.println("Killing " + num + " nodes");
        for (int i = 0; i < num; i++) {
            int n = rng.nextInt(pastryNodes.size());
            PastryNode pn = (PastryNode) pastryNodes.get(n);
            pastryNodes.remove(n);
            scribeClients.remove(n);
            appcountKilled = ((DirectScribeMaintenanceTestApp) nodeIdToApp.get(pn.getNodeId())).m_appCount;
            nodeIdToApp.remove(pn.getNodeId());
            keySet = nodeIdToApp.keySet();
            it = keySet.iterator();
            while (it.hasNext()) {
                key = (NodeId) it.next();
                scribeApp = (DirectScribeMaintenanceTestApp) nodeIdToApp.get(key);
                appcounter = scribeApp.m_appCount;
                if (appcounter > appcountKilled) scribeApp.m_appCount--;
            }
            killNode(pn);
        }
        nodesCurrentlyAlive = nodesCurrentlyAlive - num;
        System.out.println("Initiating leafset/routeset maintenance");
        initiateLeafSetMaintenance();
        initiateRouteSetMaintenance();
    }

    /**
     * Creates the specified number of new nodes and joins them to the
     * existing Pastry network. We also make the nodes subscribe to all
     * the topics. We also have to initiate leafset and routeset maintenance
     * to make the presence of these newly created nodes be reflected in 
     * the leafsets and routesets of other nodes as required. 
     */
    public void joinNodes(int num) {
        int i, j;
        DirectScribeMaintenanceTestApp scribeApp;
        NodeId topicId;
        if (num == 0) return;
        System.out.println("Joining " + num + " nodes");
        appCount = nodesCurrentlyAlive;
        for (i = 0; i < num; i++) {
            makeScribeNode();
            while (simulate()) ;
        }
        while (simulate()) ;
        nodesCurrentlyAlive = nodesCurrentlyAlive + num;
        System.out.println("Initiating leafset/routeset maintenance");
        initiateLeafSetMaintenance();
        initiateRouteSetMaintenance();
        for (i = (nodesCurrentlyAlive - num); i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            scribeApp.m_scribe.m_ackOnSubscribeSwitch = currentAckFlagState;
        }
        for (i = (nodesCurrentlyAlive - num); i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            for (j = 0; j < topicIds.size(); j++) {
                topicId = (NodeId) topicIds.elementAt(j);
                Data data = new Data(scribeApp.m_scribe.getNodeId());
                scribeApp.join(topicId, data);
            }
        }
        while (simulate()) ;
        System.out.println("All newly joined nodes have subscribed");
    }

    public boolean simulate() {
        return simulator.simulate();
    }

    /**
     * Main entry point for the test suite.
     *
     * @return true if all the tests PASSED
     */
    public static boolean start() {
        DirectScribeMaintenanceTest mt = new DirectScribeMaintenanceTest();
        boolean ok;
        System.out.println(" \n\n DirectScribeMaintenanceTest : which tests if the tree maintenance activities of Scribe are successful in repairing the topic trees in a scenario of concurrent node failures and node joins is about to START. \n");
        ok = mt.doTesting();
        if (ok) System.out.println("SCRIBE MAINTENANCE TEST - PASSED"); else System.out.println("SCRIBE MAINTENANCE TEST - FAILED");
        return ok;
    }

    /**
     * The system of nodes is set up and the testing is performed while
     * stepwise failing the desired number of nodes. 
     * 
     * @return true if all the tests PASSED
     */
    public boolean doTesting() {
        DirectScribeMaintenanceTestApp scribeApp;
        int i, j;
        int iteration;
        int index;
        NodeId topicId;
        int nodesInTree;
        Vector failedNodes;
        int failures;
        int heartbeatcount;
        boolean passed;
        boolean ok = true;
        int newJoinedNodes;
        Scribe scribe;
        for (i = 0; i < n; i++) {
            makeScribeNode();
            while (simulate()) ;
        }
        while (simulate()) ;
        System.out.println("All the nodes have been created");
        nodesCurrentlyAlive = n;
        System.out.println("Total Nodes currently alive = " + nodesCurrentlyAlive);
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            scribeApp.m_scribe.m_ackOnSubscribeSwitch = true;
        }
        index = rng.nextInt(n);
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(index);
        for (i = 0; i < numTopics; i++) {
            topicId = generateTopicId(new String("ScribeTest" + i));
            topicIds.add(topicId);
            scribeApp.create(topicId);
        }
        while (simulate()) ;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            for (j = 0; j < topicIds.size(); j++) {
                topicId = (NodeId) topicIds.elementAt(j);
                scribeApp.join(topicId);
            }
        }
        while (simulate()) ;
        passed = setParentAndaddChildTest();
        System.out.print("\nSET-PARENT_&_ADD-CHILD TEST:\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok &= passed;
        topicId = generateTopicId(new String("New Join with Data"));
        passed = checkNewJoin(topicId);
        System.out.print("\nNEW JOIN WITH DATA TEST:\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok &= passed;
        topicId = (NodeId) topicIds.elementAt(0);
        passed = checkRemoveChild(topicId);
        System.out.print("\nREMOVE CHILD TEST:\t\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok &= passed;
        passed = checkAllTrees();
        System.out.print("\nMEMBERSHIP TEST FOR ALL TREES:\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = distinctChildrenTableConsistencyTest(i);
        }
        System.out.print("\nDISTINCT CHILDREN TABLE CONSISTENCY:\t\t\t\t");
        if (passed == true) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = distinctParentTableConsistencyTest(i);
        }
        System.out.print("\nDISTINCT PARENT TABLE CONSISTENCY:\t\t\t\t");
        if (passed == true) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = checkParentPointerForAllTopics(i);
        }
        System.out.print("\nPARENT POINTER SET TEST:\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        passed = checkAnycastDFS((NodeId) topicIds.elementAt(0));
        System.out.print("\nANYCAST--DFS TEST:\t\t\t\t\t\t");
        if (passed) System.out.print("[ PASSED ]\n"); else System.out.print("[ FAILED ]\n");
        ok = ok && passed;
        currentAckFlagState = true;
        joinNodes(concurrentJoins);
        scheduleTROnAllNodes();
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = childParentViewConsistencyTest(i);
        }
        if (passed) System.out.println("\n CHECK : MessageAckOnSubscribe - Parent-Child relationship is consistent from view of PARENT as well as CHILD - [ PASSED ]\n"); else System.out.println("\n CHECK : MessageAckOnSubscribe - Parent-Child relationship is consistent from view of PARENT as well as CHILD - [ FAILED ]\n");
        ok = ok && passed;
        passed = checkAllTrees();
        if (passed) System.out.println("\n CHECK : MessageAckOnSubscribe - ALL the nodes are part of the required multicast trees - [ PASSED ]\n"); else System.out.println("\n CHECK : MessageAckOnSubscribe - All the nodes are part of the required multicast trees - [ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = checkParentPointerForAllTopics(i);
        }
        if (passed) System.out.println("\n CHECK : MessageAckOnSubscribe - Parent pointers are not-null - [ PASSED ]\n"); else System.out.println("\n CHECK : MessageAckOnSubscribe - Parent pointers are not-null - [ FAILED ]\n");
        ok = ok && passed;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            scribeApp.m_scribe.m_ackOnSubscribeSwitch = false;
        }
        currentAckFlagState = false;
        joinNodes(concurrentJoins);
        System.out.println("Total Nodes currently alive = " + nodesCurrentlyAlive);
        scheduleTROnAllNodes();
        scheduleHBOnAllNodes();
        passed = checkAllTrees();
        if (passed) System.out.println("\n CHECK : MessageHeartBeat correctly removes dangling child pointers - [ PASSED ]\n"); else System.out.println("\n CHECK : MessageHeartBeat correctly removes dangling child pointers - [ FAILED ]\n");
        ok = ok && passed;
        passed = true;
        for (i = 0; i < scribeClients.size(); i++) {
            passed = checkParentPointerForAllTopics(i);
        }
        if (passed) System.out.println("\n CHECK : MessageHeartBeat correctly sets parent pointers - [ PASSED ]\n"); else System.out.println("\n CHECK : MessageHeartBeat correctly sets parent pointers - [ FAILED ]\n");
        ok = ok && passed;
        for (iteration = 0; iteration < numIterations; iteration++) {
            killNodes(concurrentFailures);
            joinNodes(concurrentJoins);
            System.out.println("Total Nodes currently alive = " + nodesCurrentlyAlive);
            for (heartbeatcount = 0; heartbeatcount <= treeRepairThreshold; heartbeatcount++) {
                scheduleHBOnAllNodes();
            }
            passed = checkAllTrees();
            if (passed) System.out.println("CHECK : to see if after concurrent node failures and node joins the remaning nodes reconfigure to become part of the appropriate trees" + "-[ PASSED ]\n"); else System.out.println("CHECK : to see if after concurrent node failures and node joins the remaning nodes reconfigure to become part of the appropriate trees" + "-[ FAILED ]\n");
            ok = ok && passed;
        }
        scheduleHBOnAllNodes();
        for (j = 0; j < numTopics; j++) {
            topicId = (NodeId) topicIds.elementAt(j);
            for (i = 0; i < scribeClients.size(); i++) {
                scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
                if (!scribeApp.m_scribe.isRoot(topicId)) {
                    scribeApp.leave(topicId);
                }
            }
        }
        while (simulate()) ;
        passed = checkUnsubscribeTest();
        if (passed) System.out.println("CHECK : to see if after Unsubscribing each node, no one is part of multicast tree  - [ PASSED ]\n"); else System.out.println("CHECK : to see if after Unsubscribing each node, no one is part of multicast tree  - [ FAILED ]\n");
        ok = ok && passed;
        return ok;
    }

    public NodeId generateTopicId(String topicName) {
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
     * This returns the index of the application that is currently the 
     * root for the topic's multicast tree.
     *
     * @param topicId
     * the topic id of the multicast tree.
     *
     * @return index of application that is the root of topic's multicast tree.
     */
    public int rootApp(NodeId topicId) {
        int i;
        DirectScribeMaintenanceTestApp app;
        int rootApp = -1;
        for (i = 0; i < scribeClients.size(); i++) {
            app = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            if (app.m_scribe.isRoot(topicId)) {
                if (rootApp != -1) System.out.println("Warning::more than ONE application is root for the same topic's multicast tree"); else rootApp = i;
            }
        }
        if (rootApp == -1) System.out.println("Warning::No application is root for the topic's multicast tree");
        return rootApp;
    }

    /**
     * This does the Breadth First Traversal of the multicast tree for a topic.
     *
     * @param topicId
     * the topic id of the multicast tree to be traversed
     *
     * @return the total number of nodes in the tree OR returns -1 if the
     *         graph is not a TREE (it could be a DAG or maybe a cycle was
     *         detected.
     */
    public int BFS(NodeId topicId) {
        int appIndex;
        Vector toTraverse = new Vector();
        DirectScribeMaintenanceTestApp app;
        Topic topic;
        Vector children;
        NodeId childId;
        int rootAppIndex;
        NodeHandle child;
        int i;
        int depth = 0;
        Vector traversedList = new Vector();
        rootAppIndex = rootApp(topicId);
        traversedList.add(new Integer(rootAppIndex));
        toTraverse.add(new Integer(rootAppIndex));
        toTraverse.add(new Integer(-1));
        while (toTraverse.size() > 0) {
            appIndex = ((Integer) toTraverse.remove(0)).intValue();
            if (appIndex == -1) {
                depth++;
                if (toTraverse.size() != 0) toTraverse.add(new Integer(-1));
                continue;
            }
            app = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(appIndex);
            topic = app.m_scribe.getTopic(topicId);
            children = topic.getChildren();
            if (children.size() > 0) {
                for (i = 0; i < children.size(); i++) {
                    childId = ((NodeHandle) children.elementAt(i)).getNodeId();
                    app = (DirectScribeMaintenanceTestApp) nodeIdToApp.get(childId);
                    appIndex = ((DirectScribeMaintenanceTestApp) nodeIdToApp.get(childId)).m_appCount;
                    if (!traversedList.contains(new Integer(appIndex))) {
                        traversedList.add(new Integer(appIndex));
                        toTraverse.add(new Integer(appIndex));
                    } else {
                        System.out.println("Warning:: The graph being traversed is NOT a TREE" + childId);
                        return -1;
                    }
                }
            }
        }
        return traversedList.size();
    }

    /**
     * initiate leafset maintenance
     */
    private void initiateLeafSetMaintenance() {
        for (int i = 0; i < pastryNodes.size(); i++) {
            PastryNode pn = (PastryNode) pastryNodes.get(i);
            pn.receiveMessage(new InitiateLeafSetMaintenance());
            while (simulate()) ;
        }
    }

    /**
     * initiate routing table maintenance
     */
    private void initiateRouteSetMaintenance() {
        for (int i = 0; i < pastryNodes.size(); i++) {
            PastryNode pn = (PastryNode) pastryNodes.get(i);
            pn.receiveMessage(new InitiateRouteSetMaintenance());
            while (simulate()) ;
        }
    }

    /**
     * Check if all nodes are part of all multicast trees.
     * @return true if all nodes are part of all multicast
     *         trees, else false
     */
    private boolean checkAllTrees() {
        boolean result;
        NodeId topicId;
        int nodesInTree;
        result = true;
        for (int j = 0; j < topicIds.size(); j++) {
            topicId = (NodeId) topicIds.elementAt(j);
            nodesInTree = BFS(topicId);
            if (nodesInTree != nodesCurrentlyAlive) result = false;
        }
        return result;
    }

    /**
     * Check the consistency between distinctChildrenTable maintained
     * by scribe on a node and the children maintained by each Topic on that node.
     *
     * @param nodeIndex index of specfied node.
     * @return true if the test passes, else false
     */
    public boolean distinctChildrenTableConsistencyTest(int nodeIndex) {
        DirectScribeMaintenanceTestApp scribeApp;
        Scribe scribe;
        Vector children;
        NodeId topicId;
        NodeHandle child;
        Vector topics;
        int t, l;
        Topic topic;
        Vector topicsForChild;
        Vector distinctChildrenVector;
        boolean result = true;
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(nodeIndex);
        scribe = scribeApp.m_scribe;
        topics = scribe.getTopics();
        for (t = 0; t < topics.size(); t++) {
            topic = (Topic) topics.elementAt(t);
            children = (Vector) topic.getChildren();
            for (l = 0; l < children.size(); l++) {
                child = (NodeHandle) children.elementAt(l);
                topicsForChild = (Vector) scribe.getTopicsForChild((NodeHandle) child);
                if (!topicsForChild.contains(topic.getTopicId())) result = false;
            }
        }
        distinctChildrenVector = (Vector) scribe.getDistinctChildren();
        for (l = 0; l < distinctChildrenVector.size(); l++) {
            child = (NodeHandle) distinctChildrenVector.elementAt(l);
            topicsForChild = (Vector) scribe.getTopicsForChild((NodeHandle) child);
            for (t = 0; t < topicsForChild.size(); t++) {
                topicId = (NodeId) topicsForChild.elementAt(t);
                topic = scribe.getTopic(topicId);
                children = (Vector) topic.getChildren();
                if (!children.contains(child)) result = false;
            }
        }
        return result;
    }

    /**
     * Check the consistency between distinctParentTable maintained
     * by scribe on a node and the parent maintained by each Topic on that node.
     *
     * @param nodeIndex index of specfied node.
     *
     * @return true if test passes, else false
     */
    public boolean distinctParentTableConsistencyTest(int nodeIndex) {
        DirectScribeMaintenanceTestApp scribeApp;
        Scribe scribe;
        NodeId topicId;
        NodeHandle parent;
        Vector topics;
        Topic topic;
        int t, l;
        Vector topicsForParent;
        boolean result = true;
        Vector distinctParentVector;
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(nodeIndex);
        scribe = scribeApp.m_scribe;
        topics = scribe.getTopics();
        for (t = 0; t < topics.size(); t++) {
            topic = (Topic) topics.elementAt(t);
            parent = (NodeHandle) topic.getParent();
            if (parent != null) {
                topicsForParent = (Vector) scribe.getTopicsForParent((NodeHandle) parent);
                if (!topicsForParent.contains(topic.getTopicId())) result = false;
            }
        }
        distinctParentVector = (Vector) scribe.getDistinctParents();
        for (l = 0; l < distinctParentVector.size(); l++) {
            parent = (NodeHandle) distinctParentVector.elementAt(l);
            topicsForParent = (Vector) scribe.getTopicsForParent((NodeHandle) parent);
            for (t = 0; t < topicsForParent.size(); t++) {
                topicId = (NodeId) topicsForParent.elementAt(t);
                topic = (Topic) scribe.getTopic(topicId);
                if (!topic.getParent().equals(parent)) result = false;
            }
        }
        return result;
    }

    /**
     * Check if parent pointer is set for all topics on the specified node.
     *
     * @param nodeIndex index of specfied node.
     *
     * @return true if test passes, else false
     */
    public boolean checkParentPointerForAllTopics(int nodeIndex) {
        DirectScribeMaintenanceTestApp scribeApp;
        Scribe scribe;
        NodeHandle parent = null;
        NodeId topicId;
        Topic topic;
        boolean result = true;
        int i, j;
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(nodeIndex);
        scribe = scribeApp.m_scribe;
        for (j = 0; j < topicIds.size(); j++) {
            parent = null;
            topicId = (NodeId) topicIds.elementAt(j);
            topic = scribeApp.m_scribe.getTopic(topicId);
            parent = topic.getParent();
            if (parent == null) {
                if (!scribeApp.m_scribe.isRoot(topicId)) result = false;
            }
        }
        return result;
    }

    /**
     * Schedule a TreeRepair event on all nodes for all the topics.
     */
    public void scheduleTROnAllNodes() {
        int i, j;
        DirectScribeMaintenanceTestApp scribeApp;
        Topic topic;
        NodeId topicId;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            for (j = 0; j < topicIds.size(); j++) {
                topicId = (NodeId) topicIds.elementAt(j);
                topic = scribeApp.m_scribe.getTopic(topicId);
                scribeApp.m_scribe.m_maintainer.scheduleTR(topic);
                while (simulate()) ;
            }
        }
    }

    /**
     * Schedule a HeartBeat event on all nodes for all topics.
     */
    public void scheduleHBOnAllNodes() {
        int i;
        DirectScribeMaintenanceTestApp scribeApp;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            scribeApp.m_scribe.scheduleHB();
            while (simulate()) ;
        }
    }

    /**
     * Here we are trying to check that the parent-child relationship is
     * consistent from the view of the CHILD as well as the PARENT, on 
     * given node.
     * @param nodeIndex index of specfied node.
     *
     * @return true if the test passes, else false.
     */
    public boolean childParentViewConsistencyTest(int nodeIndex) {
        boolean result = true;
        DirectScribeMaintenanceTestApp scribeApp, parentApp, childApp;
        NodeId topicId;
        Topic topic, topicInParent, topicInChild;
        NodeHandle parent, child;
        Vector children;
        int j, k;
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(nodeIndex);
        for (j = 0; j < topicIds.size(); j++) {
            topicId = (NodeId) topicIds.elementAt(j);
            topic = scribeApp.m_scribe.getTopic(topicId);
            if (topic == null) continue;
            parent = topic.getParent();
            if (parent != null) {
                parentApp = (DirectScribeMaintenanceTestApp) nodeIdToApp.get(parent.getNodeId());
                topicInParent = parentApp.m_scribe.getTopic(topicId);
                children = topicInParent.getChildren();
                if (!children.contains(scribeApp.m_scribe.getLocalHandle())) result = false;
            }
            children = topic.getChildren();
            for (k = 0; k < children.size(); k++) {
                child = (NodeHandle) children.elementAt(k);
                childApp = (DirectScribeMaintenanceTestApp) nodeIdToApp.get(child.getNodeId());
                topicInChild = childApp.m_scribe.getTopic(topicId);
                if (topicInChild == null || !scribeApp.m_scribe.getLocalHandle().equals(topicInChild.getParent())) result = false;
            }
        }
        return result;
    }

    /**
     * Check if no node is part of a multicast trees except the root.
     * @return true if no node except the root is part of multicast
     *         tree for this topic
     *          else false
     */
    private boolean checkUnsubscribeTest() {
        boolean result;
        NodeId topicId;
        int nodesInTree;
        result = true;
        for (int j = 0; j < topicIds.size(); j++) {
            topicId = (NodeId) topicIds.elementAt(j);
            nodesInTree = BFS(topicId);
            if (nodesInTree != 1) result = false;
        }
        return result;
    }

    /**
     * Checks whether setParent and addChild works correctly.
     * A node(X) is picked and its parent is set to 
     * another random node(Y), and then X is added as a child
     * in Y's children table. Then, different trees are analysed to
     * see if everything is correct. This procedure is followed for
     * all nodes in system.
     */
    public boolean setParentAndaddChildTest() {
        boolean result = true;
        DirectScribeMaintenanceTestApp scribeAppX, scribeAppY;
        NodeId topicId;
        NodeHandle parent;
        for (int x = 0; x < this.n; x++) {
            scribeAppX = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(x);
            topicId = (NodeId) topicIds.elementAt(0);
            parent = scribeAppX.m_scribe.getParent(topicId);
            if (parent != null) {
                scribeAppY = (DirectScribeMaintenanceTestApp) nodeIdToApp.get((NodeId) parent.getNodeId());
                scribeAppX.m_scribe.setParent(scribeAppY.getLocalHandle(), topicId);
                scribeAppY.m_scribe.addChild(scribeAppX.getLocalHandle(), topicId);
                while (simulate()) ;
                result = checkAllTrees();
                result = result && childParentViewConsistencyTest(x);
                result = result && distinctParentTableConsistencyTest(x);
                result = result && distinctChildrenTableConsistencyTest(scribeAppY.m_appCount);
            }
        }
        return result;
    }

    /**
     * Checks the DFS implemented by MessageAnycast class. Every node sends
     * a dummy anycast message to a topic (and we make sure that no one responds
     * to it, i.e. no node is able to satisfy the request). Now, after it fails,
     * we print how many nodes did it travelled, which should be equal to number
     * of nodes alive in system, since every node subscribes to all topics.
     */
    public boolean checkAnycastDFS(NodeId topicId) {
        boolean result = true;
        DirectScribeMaintenanceTestApp scribeApp;
        MessageAnycast msg;
        Scribe scribe;
        int size;
        Credentials cred = new PermissiveCredentials();
        for (int x = 0; x < scribeClients.size(); x++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(x);
            scribe = scribeApp.getScribe();
            msg = new TestAnycastMessage(scribe.getAddress(), scribe.getNodeHandle(), topicId, cred);
            scribe.anycast(topicId, msg, cred);
            while (simulate()) ;
            size = msg.alreadySeenSize();
            if (size < nodesCurrentlyAlive) result &= false;
        }
        return result;
    }

    /**
     * A dummy test message used for checking if anycast is doing
     * DFS correctly.
     */
    public class TestAnycastMessage extends MessageAnycast {

        public TestAnycastMessage(Address address, NodeHandle nh, NodeId topicId, Credentials credentials) {
            super(address, nh, topicId, credentials);
        }

        public void faultHandler(Scribe scribe) {
        }
    }

    /**
     * A test for checking if data was correctly propogated along subscribe
     * messages if new join method was used, which takes data as a parameter.
     */
    public boolean checkNewJoin(NodeId topicId) {
        int check = 5000;
        int value = -1;
        Integer data = new Integer(check);
        DirectScribeMaintenanceTestApp scribeApp;
        int i;
        scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(0);
        if (scribeApp.m_scribe.isRoot(topicId)) scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(1);
        scribeApp.join(topicId, data);
        while (simulate()) ;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            if (scribeApp.m_scribe.isRoot(topicId)) break;
        }
        Object recvData = scribeApp.getJoinData(topicId);
        if (recvData instanceof Integer) {
            Integer integer = (Integer) recvData;
            value = integer.intValue();
        }
        if (value == check) return true; else return false;
    }

    /**
     * Tests the removeChild interface method.
     * A random node picks one of its children for given
     * topic, and removes it. Now, after this its list of 
     * children for this topic are checked and this child 
     * should not be there.
     */
    public boolean checkRemoveChild(NodeId topicId) {
        int i;
        DirectScribeMaintenanceTestApp scribeApp = null;
        for (i = 0; i < scribeClients.size(); i++) {
            scribeApp = (DirectScribeMaintenanceTestApp) scribeClients.elementAt(i);
            if (scribeApp.m_scribe.numChildren(topicId) > 1) break;
        }
        Vector children = scribeApp.m_scribe.getChildren(topicId);
        NodeHandle victimChild = (NodeHandle) children.elementAt(0);
        scribeApp.m_scribe.removeChild(victimChild, topicId);
        while (simulate()) ;
        children = scribeApp.m_scribe.getChildren(topicId);
        scribeApp.m_scribe.addChild(victimChild, topicId);
        while (simulate()) ;
        if (children.contains(victimChild)) return false; else return true;
    }
}
