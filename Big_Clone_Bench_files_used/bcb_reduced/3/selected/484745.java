package net.sf.agentopia.platform;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import net.sf.agentopia.core.AgentopiaConstants;
import net.sf.agentopia.util.ClassedObjectTransmitter;
import net.sf.agentopia.util.FileFinder;
import net.sf.agentopia.util.Logger;
import net.sf.agentopia.util.OS;

/**
 * Represents the treasure heap on the market place.
 * <p>
 * On this heap, a lot of agent treasures (including "Am I Home?" keys) are
 * stored, and deleted after some time has passed.
 * 
 * @author <a href="mailto:kain@land-of-kain.de">Kai Ruhl</a>
 * @since 8 Nov 2009
 */
public class TreasureHeap {

    /** The market place. */
    private MarketPlace marketPlace;

    /** Treasure heap (for agent treasures). */
    private Map<String, TreasureHeap.Treasure> treasureMap = new HashMap<String, TreasureHeap.Treasure>();

    /** Message Digester for signing the depositor of treasures. */
    private MessageDigest messageDigest;

    /** The slaves digest. */
    private String[] slaveDigestArray;

    /** The current time stamp. */
    private long currentTimeStamp = System.currentTimeMillis();

    /**
     * Class for a treasure. Includes signature on agent class.
     * 
     * @since 2001
     */
    protected static class Treasure implements Serializable {

        /** The time stamp of creation. */
        public long creationTimeStamp;

        /** The time stamp of when the treasure is to be removed. */
        public long obsoletionTimeStamp;

        /** The actual treasure content. */
        public Serializable treasureContent;

        /** The signature of the agent class. */
        public String agentClassSignature;

        /** The maximum time that a treasure is allowed to exist: 24 hours. */
        private static final long TREASURE_PERSIST_TIME = 24L * 60L * 60L * 1000L;

        /** The delay before deleting a treasure after it is obsolete. */
        private static final long TREASURE_DELETION_DELAY = 10L * 1000L;

        /**
         * A new treasure.
         * 
         * @param treasureHeap The treasure heap to bury on.
         * @param creatorAgent The creator agent.
         * @param treasureContent The data content. May be null.
         */
        public Treasure(TreasureHeap treasureHeap, IAgentopiaAgent creatorAgent, Object treasureContent) {
            if (null != treasureContent && !(treasureContent instanceof Serializable)) {
                throw new IllegalArgumentException("Treasure content (" + treasureContent.getClass().getSimpleName() + ") must be serializable.");
            }
            this.creationTimeStamp = treasureHeap.currentTimeStamp;
            this.obsoletionTimeStamp = this.creationTimeStamp + TREASURE_PERSIST_TIME;
            this.treasureContent = (Serializable) treasureContent;
            this.agentClassSignature = treasureHeap.getCodeSignature(creatorAgent);
        }

        /**
         * Indicates when the treasure shall be removed from the heap.
         * 
         * @return The time (in millis) of planned removal.
         */
        public long getRemovalTime() {
            return obsoletionTimeStamp;
        }

        /**
         * Schedules the treasure chest for removal from the heap.
         * <p>
         * It will be removed with the next <code>runTick()</code>.
         * 
         * @param removalDelay The time to pass (in milliseconds) before the
         *        treasure is removed. 0 means now.
         */
        public void scheduleForRemoval(long removalDelay) {
            if (removalDelay < 0) {
                removalDelay = TREASURE_DELETION_DELAY;
            }
            obsoletionTimeStamp = System.currentTimeMillis() + removalDelay;
        }
    }

    /**
     * A new treasure heap.
     * 
     * @param marketPlace The market place that the treasure is on.
     * @param registeredAgentList The list of registered sub agents (messenger,
     *        interrogator, treasure retriever).
     */
    public TreasureHeap(MarketPlace marketPlace, List<IAgentopiaAgent> registeredAgentList) {
        this.marketPlace = marketPlace;
        initDigest(registeredAgentList);
    }

    /**
     * Initialises the digest functions.
     * 
     * @param registeredAgentList The list of registered sub agents (messenger,
     *        interrogator, treasure retriever).
     */
    private void initDigest(List<IAgentopiaAgent> registeredAgentList) {
        long mark1 = 0, mark2 = 0;
        if (AgentopiaConstants.PERFORMANCE_DEBUG) {
            mark1 = System.currentTimeMillis();
        }
        final String digestAlgorithm = "SHA";
        try {
            final int registeredAgentCount = registeredAgentList.size();
            this.messageDigest = MessageDigest.getInstance(digestAlgorithm);
            this.slaveDigestArray = new String[registeredAgentCount];
            for (int pos = 0; pos < registeredAgentCount; pos++) {
                this.slaveDigestArray[pos] = getCodeSignature(registeredAgentList.get(pos));
            }
        } catch (NoSuchAlgorithmException exc) {
            Logger.getLogger().warn("TreasureHeap: No " + digestAlgorithm + " available!");
            OS.exit();
        }
        if (AgentopiaConstants.PERFORMANCE_DEBUG) {
            mark2 = System.currentTimeMillis();
            Logger.getLogger().info("TreasureHeap digest stuff took " + (mark2 - mark1) + " millies.");
        }
    }

    /**
     * Gets the class code signature of a given object.
     * 
     * @param targetObject The object whose signature (hashprint) is to be
     *        taken.
     * @return The hashprint, or "" if it is not possible.
     */
    private String getCodeSignature(Object targetObject) {
        try {
            Class<?> objectClass = targetObject.getClass();
            byte[] classBytes = FileFinder.findClassFile(objectClass);
            if (null == classBytes) {
                throw new IOException("Unable to find bytes for class " + objectClass.getName());
            }
            synchronized (messageDigest) {
                messageDigest.reset();
                messageDigest.update(classBytes);
                final byte[] digestBytes = messageDigest.digest();
                return new BigInteger(digestBytes).toString(32);
            }
        } catch (IOException exc) {
            Logger.getLogger().warn(exc, "Market place unable to load code (to digest).");
            return "";
        }
    }

    /**
     * Determines whether given agent with digest is an authorized subagent,
     * i.e. a messenger, interrogator or treasure hunter.
     * 
     * @param agent The agent.
     * @param agentDigest The digest. May be null, will be computed then.
     * @return Whether the agent is an authorized subagent.
     */
    private boolean isAuthorizedSubAgent(IAgentopiaAgent agent, String agentDigest) {
        if (null == agent) {
            throw new IllegalArgumentException("Agent may not be null.");
        }
        if (null == agentDigest) {
            agentDigest = getCodeSignature(agent);
        }
        for (int pos = 0; pos < this.slaveDigestArray.length; pos++) {
            if (agentDigest.equals(this.slaveDigestArray[pos])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Buries the treasure under given key. A hash of the agent class code is
     * taken so that only she or a treasure hunter on her behalf can dig out the
     * treasure.
     * <p>
     * Note: The key must start with the agent UID.
     * 
     * @param agent The calling agent whose hashprint is taken.
     * @param key The key under which the treasure is buried.
     * @param treasure The treasure.
     */
    public void buryTreasure(IAgentopiaAgent agent, String key, Object treasure) {
        if (!key.startsWith(agent.getUID())) {
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("Get lost treasure burier \"" + agent + "\" ! Key must start with your UID.");
            }
            throw new IllegalArgumentException("Key must start with agents UID!");
        }
        synchronized (treasureMap) {
            treasureMap.put(key, new Treasure(this, agent, treasure));
        }
    }

    /**
     * Internal method: Returns a treasure chest from the heap, or null if the
     * agent is not legit, or no treasure is not found under that key.
     * 
     * @param agent The calling agent.
     * @param key The key under which the treasure is buried.
     * @return The treasure chest, or null.
     */
    private Treasure getTreasureFromHeap(IAgentopiaAgent agent, String key) {
        if (null == agent) {
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("No treasure lookup for null agents!");
            }
            return null;
        }
        if (null == key) {
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().warn("Get lost treasure stealer \"" + agent + "\" ! I wont accept null keys.");
            }
            return null;
        }
        if (!marketPlace.isAgentListed(agent)) {
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("No treasure lookup \"" + agent + "\" ! You are not enlisted.");
            }
            return null;
        }
        final String agentDigest = getCodeSignature(agent);
        synchronized (treasureMap) {
            final TreasureHeap.Treasure treasureChest = treasureMap.get(key);
            if (null == treasureChest) {
                return null;
            }
            if (treasureChest.agentClassSignature.equals(agentDigest)) {
                return treasureChest;
            }
            if (isAuthorizedSubAgent(agent, agentDigest)) {
                return treasureChest;
            }
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("Treasure lookup denied for \"" + agent + "\".");
            }
            return null;
        }
    }

    /**
     * Determines whether the given agent can lookup the treasure under given
     * key.
     * 
     * @param agent The calling agent.
     * @param key The key under which the treasure is buried.
     * @return The treasure.
     */
    public boolean lookupTreasure(IAgentopiaAgent agent, String key) {
        return null != getTreasureFromHeap(agent, key);
    }

    /**
     * Returns the treasure to a given key -- but only if the agent has the same
     * code as the burying agent -- or is a treasure hunter.
     * 
     * @param agent The calling agent.
     * @param key The key under which the treasure is buried.
     * @param removalDelay The time to pass (in milliseconds) before the
     *        treasure is removed. 0 means now.
     * @return The treasure.
     */
    public Object retrieveTreasure(IAgentopiaAgent agent, String key, long removalDelay) {
        final TreasureHeap.Treasure treasureChest = getTreasureFromHeap(agent, key);
        if (null == treasureChest) {
            return null;
        }
        final boolean isRemoveTreasure = key.startsWith(agent.getUID());
        if (isRemoveTreasure) {
            treasureChest.scheduleForRemoval(removalDelay);
        }
        return treasureChest.treasureContent;
    }

    /**
     * Returns the treasure to a given key, regardless of who is asking.
     * Intended for access by the local host only.
     * <p>
     * This method should not be given to <code>IMarketPlaceForAgents</code>,
     * because otherwise agents could ask for the treasures of strangers.
     * 
     * @param key The key under which the treasure is buried.
     * @param isRemove Whether to remove the treasure from the heap.
     * @return The treasure, or null if no treasure exists for that key.
     */
    public Object retrieveTreasureAsHost(String key, boolean isRemove) {
        if (null == key) {
            return null;
        }
        synchronized (treasureMap) {
            final TreasureHeap.Treasure treasureChest = treasureMap.get(key);
            if (null == treasureChest) {
                return null;
            }
            if (isRemove) {
                treasureMap.remove(key);
            }
            return treasureChest.treasureContent;
        }
    }

    /**
     * Deletes obsolete treasures from the heap.
     */
    public void maintainTreasures() {
        synchronized (treasureMap) {
            this.currentTimeStamp = System.currentTimeMillis();
            final Object[] currentKeys = treasureMap.keySet().toArray();
            for (Object treasureKey : currentKeys) {
                final TreasureHeap.Treasure treasure = (TreasureHeap.Treasure) treasureMap.get(treasureKey);
                final long removalTime = treasure.getRemovalTime();
                if (removalTime < this.currentTimeStamp) {
                    if (AgentopiaConstants.TREASURE_DEBUG) {
                        Logger.getLogger().info("Clearing treasure \"" + treasureKey + "\" (TimeOut:" + removalTime + ", Current:" + this.currentTimeStamp + ").");
                    }
                    treasureMap.remove(treasureKey);
                }
            }
        }
    }

    /**
     * Transforms the treasure heap into a string memento and returns it.
     * <p>
     * This memento is useful for shutdown/re-startup of a home host, which
     * follows with a treasure heap reconstruction; particularly important for
     * agents which rely on their "Am I Home?" token.
     * 
     * @return The treasure heap memento.
     * @throws IOException If saving failed.
     */
    public String backupToMemento() throws IOException {
        synchronized (treasureMap) {
            final byte[] dataBytes = new ClassedObjectTransmitter().turnClassedObjectToBytes(treasureMap, null);
            final String dataString = new BASE64Encoder().encode(dataBytes);
            final String memento = dataString.replaceAll("\\n", "\t");
            final int treasureCount = this.treasureMap.size();
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("Saved " + treasureCount + " treasures into memento.");
            }
            return memento;
        }
    }

    /**
     * Restores the treasure heap from given memento.
     * <p>
     * Useful for shutdown/re-startup of a home host.
     * 
     * @param memento The memento.
     * @return The number of loaded treasures.
     * @throws IOException If restoring failed.
     */
    @SuppressWarnings("unchecked")
    public int restoreFromMemento(String memento) throws IOException {
        final String dataString = memento.replaceAll("\\t", "\n");
        final byte[] dataBytes = new BASE64Decoder().decodeBuffer(dataString);
        final Object dataObject = new ClassedObjectTransmitter().turnClassedBytesToObject(dataBytes, null);
        if (dataObject instanceof Map) {
            this.treasureMap = (Map<String, TreasureHeap.Treasure>) dataObject;
            final int treasureCount = this.treasureMap.size();
            if (AgentopiaConstants.TREASURE_DEBUG) {
                Logger.getLogger().info("Restored " + treasureCount + " treasures from memento.");
            }
            return treasureCount;
        } else {
            throw new IOException("Memento result should be map, but is " + dataObject.getClass().getSimpleName());
        }
    }
}
