package DE.FhG.IGD.semoa.net;

import codec.x501.*;
import java.io.*;
import java.util.*;
import java.security.*;

/**
 * Manages and keeps track on ongoing votes. Elections can
 * be held on any topic. Each topic is identified by a
 * label. If the local host wins an election then the
 * victory is proclaimed to the other hosts.<p>
 *
 * If some host sends an OK though no VOTE_NEW was sent before
 * then all hosts wait for him to try and take over. If this
 * does not happen then a new vote is initialted after the
 * timeout. If the other host sends a PROCLAIM and we are
 * bigger then we initiate a vote ourselves.<p>
 *
 * So far, comparisons of sender names are based on the string
 * representations of the senders' X.501 Distinguished Names.
 * This works as long as all implementations do proper conversion
 * of the DER encoded names as defined in RFC1779. However, the
 * comparisons could be done more robustly if the <code>Name</code>
 * instances are compared by means of the <code>equals(Object)
 * </code> method. This requires an additional encoding step of
 * one name, though, which is less efficient than the string
 * comparison. So for the time being, I stick to the strings.
 * Just bear this comment in mind.
 *
 * @author Volker Roth
 * @version "$Id: Voted.java 271 2001-03-25 15:20:29Z vroth $"
 */
public class Voted extends Thread {

    /**
     * Switches verbosity on and off.
     */
    protected static volatile boolean debug_ = false;

    /**
     * Switches one shot warning on I/O exceptions on.
     */
    protected static boolean warn_ = true;

    /**
     * The list of current topics indexed by topic label.
     */
    protected Map entries_ = new HashMap();

    /**
     * The cache for unique voter IDs.
     */
    protected Map cache_ = new HashMap();

    /**
     * The string representation of the distinguished name
     * of the local host.
     */
    protected Name name_;

    /**
     * The {@link Multicastd Multicastd} of this vote manager.
     */
    protected Multicastd controller_;

    /**
     * The timeout delay for ongoing elections.
     */
    protected long electionTimeout_ = 10000L;

    /**
     * The timeout until we proclaim victory if no other
     * server wants to take over.
     */
    protected long proclaimTimeout_ = 2000L;

    /**
     * Creates an instance that is bound to the given
     * {@link Multicastd Multicastd}.
     *
     * @param controller The controller of this table.
     */
    public Voted(Multicastd controller) {
        super("Voting and Election Daemon");
        if (controller == null) {
            throw new NullPointerException("Multicastd");
        }
        controller_ = controller;
        name_ = controller.name_;
    }

    /**
     * Registers the given topic. If the given topic is already
     * registered then the <code>active</code> flag in the
     * existing entry will be set to the given one.
     *
     * @param topic The topic label.
     * @param active if <code>true</code> the we try to become
     *   coordinator for the given topic. In that case, this
     *   method initiates an election if there is not already
     *   an entry for this topic.
     * @exception IOException if a network packet could not
     *   be sent successfully.
     */
    public void registerTopic(String topic, boolean active) throws IOException {
        Entry entry;
        boolean vote;
        synchronized (entries_) {
            vote = false;
            entry = (Entry) entries_.get(topic);
            if (entry == null) {
                entry = new Entry(topic, active);
                entries_.put(topic, entry);
                vote = true;
            } else {
                vote = active ^ entry.active_;
                if (vote) {
                    entry.active_ = active;
                }
            }
        }
        if (vote) {
            startElection(topic);
        }
    }

    /**
     * Removes a topic. If we are coordinator for that topic then
     * a new election is triggered in which we do not participate
     * any more.
     *
     * @param topic The label of the topic to unregister.
     * @exception IOException if a network packet could not
     *   be sent successfully.
     */
    public void unregisterTopic(String topic) throws IOException {
        PingPacket packet;
        Entry entry;
        synchronized (entries_) {
            entry = (Entry) entries_.remove(topic);
            if (entry == null) {
                return;
            }
        }
        synchronized (entry) {
            if ((entry.status_ == Entry.COORDINATOR) && (!name_.getName().equals(entry.coordinator_))) {
                return;
            }
            if (!entry.active_) {
                return;
            }
            entry.status_ = Entry.ELECTION;
            packet = new PingPacket(PingPacket.VOTE_NEW, name_, entry.label_);
        }
        controller_.send(packet.getEncoded());
    }

    /**
     * Starts a new election on the given topic. The election is
     * started only if the topic is registered.
     *
     * @param topic The label of the topic on which a new
     *   election shall be started.
     */
    public void startElection(String topic) throws IOException {
        PingPacket packet;
        Entry ve;
        packet = null;
        synchronized (entries_) {
            ve = (Entry) entries_.get(topic);
            if (ve != null && ve.status_ == Entry.COORDINATOR) {
                ve.status_ = ve.active_ ? Entry.TAKEOVER : Entry.ELECTION;
                ve.timestamp_ = System.currentTimeMillis();
                packet = new PingPacket(PingPacket.VOTE_NEW, name_, topic);
            }
        }
        if (packet != null) {
            controller_.send(packet.getEncoded());
        }
    }

    /**
     * Retrieves the name of the coordinator for the given
     * topic. Coordinators are negotiated among all sensed
     * hosts on the network that are interested in becoming
     * the coordinator (hosts that are <i>active</i> for
     * this topic).
     *
     * @param topic The topic for whom the coordinator shall
     *   be returned.
     * @return The string representation of the X.501
     *   Distinguished Name of the coordinator for the
     *   given topic, or <code>null</code> if either
     *   the coordinator or the topic is not known.
     */
    public String getCoordinator(String topic) {
        Entry ve;
        synchronized (entries_) {
            ve = (Entry) entries_.get(topic);
            if (ve != null) {
                try {
                    return ve.coordinator_;
                } catch (Exception e) {
                }
            }
            return null;
        }
    }

    /**
     * Handles an incoming vote with the given topic which
     * was sent by the given sender.<p>
     *
     * @param sender The sender of the new vote.
     * @param topic The topic of the vote.
     */
    public void doVoteNew(String sender, String topic) throws IOException {
        Entry entry;
        if (sender.equals(name_.getName())) {
            return;
        }
        synchronized (entries_) {
            entry = (Entry) entries_.get(topic);
            if (entry == null || !entry.active_) {
                return;
            }
        }
        synchronized (entry) {
            entry.timestamp_ = System.currentTimeMillis();
            if (!biggerThan(sender, topic)) {
                entry.status_ = Entry.ELECTION;
                return;
            }
            entry.status_ = Entry.TAKEOVER;
        }
        PingPacket packet;
        packet = new PingPacket(PingPacket.VOTE_OK, name_, topic);
        controller_.send(packet.getEncoded());
    }

    /**
     * Handles an incoming vote reply with the given topic
     * sent by the given sender.
     *
     * @param sender The sender of the new vote.
     * @param topic The topic of the vote.
     */
    public void doVoteOK(String sender, String topic) throws IOException {
        Entry entry;
        if (sender.equals(name_.getName())) {
            return;
        }
        synchronized (entries_) {
            entry = (Entry) entries_.get(topic);
            if (entry == null || !entry.active_) {
                return;
            }
        }
        synchronized (entry) {
            if (!biggerThan(sender, topic)) {
                entry.timestamp_ = System.currentTimeMillis();
                entry.status_ = Entry.ELECTION;
                return;
            }
        }
    }

    /**
     * Handles an incoming PROCLAIM with the given topic
     * sent by the given sender.
     *
     * @param sender The sender of the new vote.
     * @param topic The topic of the vote.
     */
    public void doProclaim(String sender, String topic) throws IOException {
        Entry entry;
        if (sender.equals(name_.getName())) {
            return;
        }
        synchronized (entries_) {
            entry = (Entry) entries_.get(topic);
            if (entry == null) {
                return;
            }
        }
        PingPacket packet;
        synchronized (entry) {
            if (!biggerThan(sender, topic)) {
                entry.timestamp_ = System.currentTimeMillis();
                entry.status_ = Entry.COORDINATOR;
                entry.coordinator_ = sender;
                if (debug_) {
                    System.out.println("Accepted " + sender + " as Coordinator for \"" + topic + "\"");
                }
                return;
            }
            if (!entry.active_) {
                return;
            }
            entry.timestamp_ = System.currentTimeMillis();
            entry.status_ = Entry.TAKEOVER;
        }
        packet = new PingPacket(PingPacket.VOTE_NEW, name_, topic);
        controller_.send(packet.getEncoded());
    }

    /**
     * Handles the case that some host resigned, crashed, or
     * was shut down. In that case we have to check if that
     * host was coordinator for some topic, and we have to
     * start a vote.
     *
     * @param host The name of the expired host. This must be
     *   the string representation of the distinguished name
     *   of that host.
     * @param time The local time when the host with the given
     *   name was last known to be alive.
     */
    public void doExpire(String host, long time) {
        PingPacket packet;
        Entry ve;
        ArrayList list;
        Iterator i;
        synchronized (entries_) {
            list = new ArrayList(entries_.size());
            list.addAll(entries_.values());
        }
        for (i = list.iterator(); i.hasNext(); ) {
            ve = (Entry) i.next();
            synchronized (ve) {
                if (ve.coordinator_ == null || !ve.coordinator_.equals(host)) {
                    continue;
                }
                if (ve.status_ != Entry.COORDINATOR) {
                    continue;
                }
                if (ve.active_) {
                    ve.status_ = Entry.TAKEOVER;
                } else {
                    ve.status_ = Entry.ELECTION;
                }
                ve.timestamp_ = System.currentTimeMillis();
                packet = new PingPacket(PingPacket.VOTE_NEW, name_, ve.label_);
            }
            try {
                controller_.send(packet.getEncoded());
            } catch (IOException e) {
                if (warn_) {
                    System.err.println("[Election daemon] WARING: " + "I/O error on sending packet!\n" + "(This message is shown only once.)");
                    warn_ = false;
                }
            }
        }
    }

    /**
      * Runs the vote manager's main loop. This method can
      * be terminated by means of an interrupt.
      */
    public void run() {
        ArrayList list;
        Iterator i;
        Entry entry;
        long delta;
        long last;
        long time;
        long res;
        long min;
        if (Thread.currentThread() != this) {
            return;
        }
        list = new ArrayList();
        while (true) {
            synchronized (entries_) {
                list.addAll(entries_.values());
            }
            min = proclaimTimeout_;
            time = System.currentTimeMillis();
            for (i = list.iterator(); i.hasNext(); ) {
                try {
                    entry = (Entry) i.next();
                    res = checkEntry(entry);
                    last = time;
                    time = System.currentTimeMillis();
                } catch (IOException e) {
                    if (warn_) {
                        System.err.println("[Election daemon] WARING: " + "I/O error on sending packet!\n" + "(This message is show only once.)");
                        warn_ = false;
                    }
                    continue;
                }
                delta = time - last;
                if (min > 0) {
                    min = min - delta;
                }
                if (res > 0 && res < min) {
                    min = res;
                }
            }
            try {
                if (min > 0) {
                    Thread.currentThread().sleep(min);
                }
            } catch (InterruptedException e) {
                System.out.println("[voted] terminated.");
                break;
            }
        }
    }

    /**
     * Checks the given entry whether or not an action is
     * required. Depending on timeouts for running elections
     * and missing PROCLAIM messages we re-start an election
     * or PROCLAIM victory.
     *
     * This method returns the minimum absolute time at which
     * the next check for this entry must be scheduled. A value
     * of zero means never.
     *
     * @param entry The entry to check.
     * @return The absolute time the next check must be
     *   scheduled or zero if none is required.
     * @exception IOException if some packet could not be
     *   transmitted.
     */
    private long checkEntry(Entry entry) throws IOException {
        PingPacket packet;
        long time;
        long ltmp;
        packet = null;
        time = System.currentTimeMillis();
        synchronized (entry) {
            if (entry.status_ == Entry.ELECTION) {
                ltmp = entry.timestamp_ + electionTimeout_ - time;
                if (ltmp <= 0) {
                    entry.timestamp_ = time;
                    if (!entry.active_) {
                        entry.coordinator_ = null;
                        entry.status_ = Entry.COORDINATOR;
                        return 0L;
                    }
                    entry.status_ = Entry.TAKEOVER;
                    if (debug_) {
                        System.out.println("Election timed out, sending NEW.");
                    }
                    packet = new PingPacket(PingPacket.VOTE_NEW, name_, entry.label_);
                    ltmp = proclaimTimeout_;
                }
            } else if (entry.status_ == Entry.TAKEOVER) {
                ltmp = entry.timestamp_ + proclaimTimeout_ - time;
                if (ltmp <= 0) {
                    entry.timestamp_ = time;
                    entry.status_ = Entry.COORDINATOR;
                    entry.coordinator_ = name_.getName();
                    if (debug_) {
                        System.out.println("Takeover timed out, sending PROCLAIM.");
                    }
                    packet = new PingPacket(PingPacket.PROCLAIM, name_, entry.label_);
                    ltmp = 0L;
                }
            } else {
                return 0L;
            }
        }
        if (packet != null) {
            controller_.send(packet.getEncoded());
        }
        return ltmp;
    }

    /**
     * Returns the unique host identifier. This host identifier
     * is computed by means of hashing the host address string
     * and the election topic with the SHA1 digest. It is used
     * by the <i>Bully Algorithm</i> in order to determine who
     * the winner of an election is. Hashing boththe name and
     * the topic assures that coordinatior roles for different
     * topics are spread among the hosts. The hash results are
     * cached in order to improve performance.
     *
     * @param name The distinguished name of the host's key.
     * @param topic The topic on which is voted.
     * @return The host identifier.
     */
    public byte[] uniqueID(String name, String topic) {
        String key;
        byte[] id;
        synchronized (cache_) {
            key = name + "|" + topic;
            id = (byte[]) cache_.get(key);
            if (id == null) {
                MessageDigest md;
                try {
                    md = MessageDigest.getInstance("SHA");
                    md.update(name.getBytes());
                    md.update(topic.getBytes());
                    id = md.digest();
                    cache_.put(key, id);
                    if (debug_) {
                        System.out.println("Cached " + key + " [" + id[0] + "," + id[1] + ",...]");
                    }
                } catch (NoSuchAlgorithmException e) {
                    throw new Error("SHA not available!");
                }
            }
        }
        return id;
    }

    /**
     * Returns <code>true</code> if this host is
     * &quot;bigger&quot; than the given one with
     * regard to the <i>Bully Algorithm</i>.
     *
     * @param sender The name of the peer host.
     * @param topic The topic.
     */
    protected boolean biggerThan(String sender, String topic) {
        byte[] a;
        byte[] b;
        int n;
        a = uniqueID(name_.getName(), topic);
        b = uniqueID(sender, topic);
        n = compare(a, b);
        return (n > 0) ? true : false;
    }

    /**
     * Compares the two byte arrays. The arrays must have the
     * same length. Only the minimum number of octets of both
     * will be compared otherwise.
     */
    public int compare(byte[] a, byte[] b) {
        int n;
        int i;
        int x;
        int y;
        n = Math.min(a.length, b.length);
        for (i = 0; i < n; i++) {
            x = a[i] & 0xff;
            y = b[i] & 0xff;
            if (x != y) {
                return (x - y);
            }
        }
        return 0;
    }

    /**
     * Represents information on ongoing elections, votes, and
     * coordinators.<p>
     *
     * Topic entries can have various states:
     * <dl>
     * <dt> COORDINATOR
     * <dd> There is a coordinator and no votes are pending.
     * <dt> ELECTION
     * <dd> The is an ongoing election but the local host is
     *    not big enough to take over.
     * <dt> TAKEOVER
     * <dd> There is an ongoing election and the local host is
     *   the biggest guy in town so far.
     * </dl>
     *
     * @author Volker Roth
     */
    public class Entry extends Object {

        public static final int COORDINATOR = 0;

        public static final int ELECTION = 1;

        public static final int TAKEOVER = 2;

        /**
         * The label of the topic. Ongoing elections are identified
         * by their label. Different labels denote different topics
         * and consequently different elections.
         */
        protected String label_;

        /**
         * Status of the topic. We start with COORDINATOR.
         * So everything is peaceful until the first poor
         * guy twists the door knob.
         */
        protected int status_ = COORDINATOR;

        /**
         * The timestamp of the last operation related to this
         * topic.
         */
        protected long timestamp_ = System.currentTimeMillis();

        /**
         * The name of the current coordinator for that topic.
         */
        protected String coordinator_;

        /**
         * A flag that signals whether we want to become
         * coordinator of this topic if possible. If not
         * then the <code>Voted</code> is in monitoring
         * mode for this topic. We get to know who is
         * coordinator eventually, but we do not run a
         * campaign ourselves.
         */
        protected boolean active_;

        protected Entry(String label, boolean active) {
            label_ = label;
            active_ = active;
        }
    }
}
