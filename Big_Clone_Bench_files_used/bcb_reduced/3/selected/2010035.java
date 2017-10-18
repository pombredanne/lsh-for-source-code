package agentopia.container;

import java.io.*;
import java.util.*;
import java.lang.reflect.*;
import java.security.*;
import agentopia.util.*;

/**
 * This is where to do the work.
 * There is exactly one marketplace per host, and on this all the agents and
 * serviteurs meet and negotiate.<p>
 *
 * But why is this a thread on its own? Well, in order to keep updated
 * with its exits and agents, it has to do continuous checking.
 *
 * @author: <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
 */
public class MarketPlace extends Thread implements IRunnable {

    /** The ID of this host. */
    private HostID oHomeID;

    /** The list of serviteurs on this host. */
    private ArrayList vServiteurs = new ArrayList(20);

    /** The list of other hosts, in form of HostIDs. */
    private ArrayList vExits = new ArrayList(20);

    /** The list of other agents on this host. */
    private ArrayList vAgents = new ArrayList(40);

    /** The maximum number of exits this marketplace will handle. */
    private int iMaxExits = 8;

    /** For the thread. */
    private boolean tRunning = true;

    /** If it is alredy in creation. */
    private boolean tInCreation = false;

    /** Treasure heap. Is not serialized at shutdown, but should be... */
    private HashMap hTreasureHeap = new HashMap();

    /** The current time stamp. */
    private long lCurrentTimeStamp = new Date().getTime();

    /** The maximum time that a treasure is allowed to exist. */
    public final long TREASURE_PERSIST_TIME = 24 * 60 * 60 * 1000;

    /** Message Digester for signing the depositor of treasures. */
    private MessageDigest oDigest;

    /** The slaves digest. */
    private String[] asSlaveDigests;

    /** Class for a treasure. Includes signature on agent class. */
    private class Treasure {

        public long lTimeStamp;

        public String sSignature;

        public Object oContent;

        public Treasure(AbstractAgent oCreator, Object oContent) {
            this.lTimeStamp = lCurrentTimeStamp;
            this.oContent = oContent;
            this.sSignature = getCodeSignature(oCreator);
        }
    }

    /**
 * MarketPlace for given home ID.
 *
 * @param oHomeID The home ID.
 */
    private MarketPlace(HostID oHomeID) {
        this.oHomeID = oHomeID;
        createServiteurs();
        setName("Market Place");
        long lMark1 = 0, lMark2 = 0;
        if (Base.PERFORMANCE_DEBUG) {
            lMark1 = new Date().getTime();
        }
        final String sDigestAlgo = "SHA";
        try {
            this.oDigest = MessageDigest.getInstance(sDigestAlgo);
            this.asSlaveDigests = new String[3];
            this.asSlaveDigests[0] = getCodeSignature(AbstractAgent.createAgentMessenger(null));
            this.asSlaveDigests[1] = getCodeSignature(AbstractAgent.createAgentInterrogator(null, null));
            this.asSlaveDigests[2] = getCodeSignature(AbstractAgent.createAgentTreasureHunter(null, null, null));
        } catch (NoSuchAlgorithmException oExc) {
            Base.err("MarketPlace: No " + sDigestAlgo + " available!");
            Base.exit();
        }
        if (Base.PERFORMANCE_DEBUG) {
            lMark2 = new Date().getTime();
            Base.status("MarketPlace digest stuff took " + (lMark2 - lMark1) + " millies.");
        }
    }

    /**
 * Adds another agent to the list.
 * Only listed agents can perform most actions.
 * Listing occurs automatically, so the only purpose is to prevent agents from
 * creating subagents en masse.
 *
 * @param oAgent The agent to be added.
 */
    protected synchronized void addAgent(AbstractAgent oAgent) {
        SecurityManager oSecMan = System.getSecurityManager();
        if (null != oSecMan) {
            oSecMan.checkPermission(new RuntimePermission("accessMarketPlace"));
        }
        this.vAgents.add(oAgent);
    }

    /**
 * Adds a sustainer to the list.
 *
 * @param oExitID The new sustainer.
 */
    protected void addExit(Sustainer oExit) throws IOException {
        if (oExit == null) {
            throw new IOException("Will not set null exit.");
        } else if (oExit.getTargetID().equals(getHomeID())) {
            throw new IOException("Will not set home as an exit.");
        }
        if (vExits.contains(oExit)) {
            throw new IOException("Will not set one exit twice.");
        } else {
            vExits.add(oExit);
        }
    }

    /**
 * Adds a serviteur to the list.
 *
 * @param oServiteur The new serviteur.
 */
    protected void addServiteur(AbstractServiteur oServiteur) {
        vServiteurs.add(oServiteur);
    }

    /**
 * Buries the treasure under given key.
 * A hashprint of the agent is take so that only she or a treasure hunter on
 * her behalf can dig out the treasure.
 *
 * @param oAgent The calling agent whose hashprint is taken.
 * @param sKey The key under which the treasure is buried.
 * @param oTreasure The treasure.
 */
    public void buryTreasure(AbstractAgent oAgent, String sKey, Object oTreasure) throws IllegalArgumentException {
        if (!sKey.startsWith(oAgent.getUID())) {
            if (Base.DEBUG) {
                Base.println("Get lost treasure burier \"" + oAgent + "\" ! Key must start with your UID.");
            }
            throw new IllegalArgumentException("Key must start with agents UID!");
        }
        synchronized (hTreasureHeap) {
            hTreasureHeap.put(sKey, new Treasure(oAgent, oTreasure));
        }
    }

    /**
 * Consists of two steps:<p>
 * (1) Try to create the hosts in the config.<br>
 * (2) Use the routers from config to get their exits.<p>
 */
    private void createExits() {
        String sHost;
        HostID oHostID;
        Config oConf = Base.getConfig();
        String[] asHosts = oConf.keys("host");
        for (int iCnt = 0; iCnt < asHosts.length; iCnt++) {
            if (getExitCount() >= this.iMaxExits) {
                break;
            }
            sHost = oConf.get(asHosts[iCnt], null);
            if (null != sHost) {
                try {
                    oHostID = new HostID(sHost);
                    if (!isHostKnown(oHostID)) {
                        Sustainer.createSustainer(this, oHostID, this.oHomeID);
                    }
                } catch (IOException oExc) {
                }
            }
        }
        String[] asRouters = oConf.keys("router");
        for (int iCnt = 0; iCnt < asRouters.length; iCnt++) {
            sHost = oConf.get(asRouters[iCnt], null);
            if (null != sHost) {
                try {
                    oHostID = new HostID(sHost);
                    createExitsFromRouter(oHostID);
                } catch (IOException oExc) {
                    if (Base.NETWORK_DEBUG) {
                        Base.println("MarketPlace.createExits(): Host \"" + sHost + "\" not reachable. Skipping.");
                    }
                }
            }
        }
    }

    /**
 * Asks the router for its known exits and adds them here.
 */
    private void createExitsFromRouter(HostID oRouter) {
        Communicator oCom = null;
        try {
            oCom = Communicator.createCommunicator(oRouter, Constants.HEADER_HOST_SEEKER);
            int iFlag = oCom.readInt();
            if (Constants.HEADER_HOST_SEEKER != iFlag) {
                oCom.writeInt(Constants.MESSAGE_TRANSFER_FAILED);
                oCom.shutDown();
                throw new IOException("Flag was not HEADER_HOST_SEEKER!");
            }
            final int iHosts = oCom.readInt();
            String[] asHosts = new String[iHosts];
            for (int iHost = 0; iHost < iHosts; iHost++) {
                iFlag = oCom.readInt();
                if (Constants.MESSAGE_STRING_COMING != iFlag) {
                    oCom.shutDown();
                    throw new IOException("Flag was not for STRING_COMING, but I expected a host name!");
                }
                asHosts[iHost] = oCom.readString();
            }
            oCom.writeInt(Constants.MESSAGE_TRANSFER_COMPLETED);
            oCom.shutDown();
            oCom = null;
            HostID oHostID;
            Sustainer oSustainer;
            for (int iHost = 0; iHost < iHosts; iHost++) {
                if (getExitCount() >= this.iMaxExits) {
                    return;
                }
                try {
                    oHostID = new HostID(asHosts[iHost]);
                    if (!isHostKnown(oHostID)) {
                        Sustainer.createSustainer(this, oHostID, this.oHomeID);
                    }
                } catch (IOException oExc) {
                }
            }
        } catch (IOException oExc) {
            if (null != oCom) {
                oCom.shutDown();
            }
        }
    }

    /**
 * Public method to create a new market place.
 * Also handles thread stuff.
 *
 * @param oHomeID The home of the market place.
 * @return The market place.
 */
    public static MarketPlace createMarketPlace(HostID oHomeID) {
        MarketPlace oMarketPlace = new MarketPlace(oHomeID);
        oMarketPlace.setPriority(Constants.MARKET_PRIORITY);
        return oMarketPlace;
    }

    /**
 * Creates all serviteurs as stated in the config file.
 */
    private void createServiteurs() {
        Config oConf = Base.getConfig();
        String[] asServices = oConf.keys("service");
        String sService;
        AbstractServiteur oServiteur;
        for (int iCnt = 0; iCnt < asServices.length; iCnt++) {
            sService = oConf.get(asServices[iCnt], null);
            if (null != sService) {
                try {
                    oServiteur = (AbstractServiteur) Class.forName(sService).newInstance();
                    addServiteur(oServiteur);
                } catch (Exception oExc) {
                    Base.err("Warning: Could not create Serviteur \"" + sService + "\".");
                }
            }
        }
    }

    /**
 * Returns an agent - if his correct name is known.
 *
 * @param sAgentName The agents complete name.
 * @return The agent.
 */
    public AbstractAgent getAgent(String sAgentName) {
        AbstractAgent oAgent;
        for (Iterator eAgent = vAgents.iterator(); eAgent.hasNext(); ) {
            oAgent = (AbstractAgent) eAgent.next();
            if (oAgent.getUID().equals(sAgentName)) {
                return oAgent;
            }
        }
        return null;
    }

    /**
 * Gets the class code signature of a given object.
 *
 * @param oTarget The object whose signature (hashprint) is to be taken.
 * @return The hashprint, or "" if it is not possible.
 */
    private String getCodeSignature(Object oTarget) {
        try {
            byte[] abCode = Base.findClassResource(oTarget.getClass());
            oDigest.update(abCode);
            byte[] abRes = oDigest.digest();
            return new java.math.BigInteger(abRes).toString(32);
        } catch (IOException oExc) {
            Base.println("Warning: MarketPlace unable to load code (to digest).");
            return "";
        }
    }

    /**
 * Returns a sustainer at given position in the list.
 *
 * @param iPos The position inside the list.
 * @return The sustainer.
 */
    public Sustainer getExitAt(int iPos) {
        return (Sustainer) vExits.get(iPos);
    }

    /**
 * Returns the number of sustainers available.
 *
 * @return Sustainer count.
 */
    public int getExitCount() {
        return vExits.size();
    }

    /**
 * Returns the complete list of sustainers.
 *
 * @return The sustainer list.
 */
    public Sustainer[] getExitList() {
        synchronized (vExits) {
            final int iLen = vExits.size();
            Sustainer[] aoExits = new Sustainer[iLen];
            for (int iCnt = 0; iCnt < iLen; iCnt++) {
                aoExits[iCnt] = (Sustainer) vExits.get(iCnt);
            }
            return aoExits;
        }
    }

    /**
 * Returns the home ID.
 *
 * @return The home ID.
 */
    public HostID getHomeID() {
        return this.oHomeID;
    }

    /**
 * Returns the number of serviteurs.
 *
 * @return Serviteur count.
 */
    public int getServiteurCount() {
        return vServiteurs.size();
    }

    /**
 * Returns the complete list of serviteurs.
 *
 * @return Serviteur list.
 */
    public AbstractServiteur[] getServiteurList() {
        synchronized (vServiteurs) {
            final int iLen = vServiteurs.size();
            AbstractServiteur[] aoServiteurs = new AbstractServiteur[iLen];
            for (int iCnt = 0; iCnt < iLen; iCnt++) {
                aoServiteurs[iCnt] = (AbstractServiteur) vServiteurs.get(iCnt);
            }
            return aoServiteurs;
        }
    }

    /**
 * Checks whether the given host is among those that can be reached by sustainers.
 *
 * @param oHostID The host to be checked.
 * @return Whether the host is reachable by sustainer.
 */
    public boolean isHostKnown(HostID oHostID) {
        if (oHostID.equals(getHomeID())) {
            return true;
        }
        synchronized (vExits) {
            for (Iterator eExit = vExits.iterator(); eExit.hasNext(); ) {
                if (((Sustainer) eExit.next()).getTargetID().equals(oHostID)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
 * Same as <code>retrieveTreasure()</code>, but the treasure is not deleted.
 *
 * @param oAgent The calling agent.
 * @param sKey The key under which the treasure is buried.
 * @return The treasure.
 */
    public boolean lookupTreasure(AbstractAgent oAgent, String sKey) {
        if (null == oAgent) {
            if (Base.DEBUG) {
                Base.println("No treasure lookup for null agents!");
            }
            return false;
        } else if (null == sKey) {
            if (Base.DEBUG) {
                Base.println("No treasure lookup \"" + oAgent + "\" ! Wont accept null keys..");
            }
            return false;
        } else if (null == sKey || !vAgents.contains(oAgent)) {
            if (Base.DEBUG) {
                Base.println("No treasure lookup \"" + oAgent + "\" ! You are not enlisted.");
            }
            return false;
        }
        String sDigest = getCodeSignature(oAgent);
        synchronized (hTreasureHeap) {
            Treasure oTreasure = (Treasure) hTreasureHeap.get(sKey);
            if (null == oTreasure) {
                return false;
            }
            if (oTreasure.sSignature.equals(sDigest)) {
                return true;
            }
            for (int iCnt = 0; iCnt < this.asSlaveDigests.length; iCnt++) {
                if (sDigest.equals(this.asSlaveDigests[iCnt])) {
                    return true;
                }
            }
            if (Base.DEBUG) {
                Base.println("Treasure lookup denied for \"" + oAgent + "\".");
            }
            return false;
        }
    }

    /**
 * Removes an agent from the list.
 *
 * @param oAgent The agent to be removed.
 */
    protected void removeAgent(AbstractAgent oAgent) {
        vAgents.remove(oAgent);
    }

    /**
 * Removes a sustainer from the list.
 *
 * @param oExit The sustainer to be removed.
 */
    protected void removeExit(Sustainer oExit) {
        vExits.remove(oExit);
    }

    /**
 * Returns the treasure to a given key - but only if the agent has the same code
 * as the burying agent - or is a treasure hunter.
 *
 * @param oAgent The calling agent.
 * @param sKey The key under which the treasure is buried.
 * @return The treasure.
 */
    public Object retrieveTreasure(AbstractAgent oAgent, String sKey) {
        if (null == sKey) {
            if (Base.DEBUG) {
                Base.println("Get lost treasure stealer \"" + oAgent + "\" ! I wont accept null keys.");
            }
            return null;
        }
        if (!vAgents.contains(oAgent)) {
            if (Base.DEBUG) {
                Base.println("Get lost treasure stealer \"" + oAgent + "\" ! You are faked!");
            }
            return null;
        }
        String sDigest = getCodeSignature(oAgent);
        synchronized (hTreasureHeap) {
            Treasure oTreasure = (Treasure) hTreasureHeap.get(sKey);
            if (null == oTreasure) {
                return null;
            }
            boolean tAuthorized = oTreasure.sSignature.equals(sDigest);
            for (int iCnt = 0; !tAuthorized && iCnt < this.asSlaveDigests.length; iCnt++) {
                if (sDigest.equals(this.asSlaveDigests[iCnt])) {
                    tAuthorized = true;
                }
            }
            if (tAuthorized) {
                if (sKey.startsWith(oAgent.getUID())) {
                    hTreasureHeap.remove(sKey);
                }
                return oTreasure.oContent;
            } else {
                return null;
            }
        }
    }

    /**
 * Performs various tasks, sleeping a few seconds in between.<p>
 * (1) Check exits and create new ones if neccessary.<br>
 * (2) Create new status strings.
 * (3) Check treasure timestamps.
 */
    public void run() {
        String sHostList, sLastListOfHosts = "";
        String sThreadList, sLastListOfThreads = "";
        String sAgentList, sLastListOfAgents = "";
        long lCurrentTime;
        while (tRunning) {
            if (getExitCount() < this.iMaxExits && false == tInCreation) {
                tInCreation = true;
                createExits();
                tInCreation = false;
            }
            sHostList = vExits.toString();
            if (!sLastListOfHosts.equals(sHostList)) {
                sLastListOfHosts = sHostList;
                Base.status("Hosts: " + sLastListOfHosts);
            }
            sAgentList = vAgents.toString();
            if (!sLastListOfAgents.equals(sAgentList)) {
                sLastListOfAgents = sAgentList;
                Base.status("Agents: " + sLastListOfAgents);
            }
            if (Base.THREAD_DEBUG) {
                sThreadList = Base.getThreadList();
                if (!sLastListOfThreads.equals(sThreadList)) {
                    sLastListOfThreads = sThreadList;
                    Base.status("Threads: " + sLastListOfThreads);
                }
            }
            synchronized (hTreasureHeap) {
                this.lCurrentTimeStamp = new Date().getTime();
                for (Iterator eKey = hTreasureHeap.keySet().iterator(); eKey.hasNext(); ) {
                    Object oKey = eKey.next();
                    Treasure oTreasure = (Treasure) hTreasureHeap.get(oKey);
                    if (oTreasure.lTimeStamp + TREASURE_PERSIST_TIME < this.lCurrentTimeStamp) {
                        if (Base.DEBUG) {
                            Base.status("Clearing treasure \"" + oKey + "\" (TimeOut:" + (oTreasure.lTimeStamp + TREASURE_PERSIST_TIME) + ", Current:" + this.lCurrentTimeStamp + ").");
                        }
                        hTreasureHeap.remove(oKey);
                    }
                }
            }
            Base.sleep(3000);
        }
        Sustainer[] aoComs = getExitList();
        for (int iCnt = 0; iCnt < aoComs.length; iCnt++) {
            aoComs[iCnt].shutDown();
        }
    }

    /**
 * Tries to locate a serviteur that provides given service and then executes it.
 *
 * @param sService The requested service.
 * @param aoParameter The parameters for the service invocation.
 * @return The result of the service call, or null if there is no such service.
 */
    public Object runService(String sService, Object[] aoParameter) {
        AbstractServiteur[] aoServiteurs = getServiteurList();
        Method oService;
        Object oRes;
        final int iServiteurCount = aoServiteurs.length;
        for (int iCnt = 0; iCnt < iServiteurCount; iCnt++) {
            oService = aoServiteurs[iCnt].searchService(sService, aoParameter);
            if (null != oService) {
                oRes = aoServiteurs[iCnt].runService(oService, aoParameter);
                if (null != oRes) {
                    return oRes;
                }
            }
        }
        return null;
    }

    /**
 * Shuts the updating thread down.
 */
    public void shutDown() {
        this.tRunning = false;
    }

    /**
 * Returns the marketplaces name.
 *
 * @return The name.
 */
    public String toString() {
        return "MarketPlace on \"" + getHomeID() + "\"\nServiteurs: " + vServiteurs;
    }
}
