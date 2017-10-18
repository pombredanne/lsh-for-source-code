package navigators.smart.tom.core;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignedObject;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import navigators.smart.clientsmanagement.ClientsManager;
import navigators.smart.clientsmanagement.RequestList;
import navigators.smart.communication.ServerCommunicationSystem;
import navigators.smart.communication.client.RequestReceiver;
import navigators.smart.paxosatwar.Consensus;
import navigators.smart.paxosatwar.executionmanager.TimestampValuePair;
import navigators.smart.paxosatwar.executionmanager.Execution;
import navigators.smart.paxosatwar.executionmanager.ExecutionManager;
import navigators.smart.paxosatwar.executionmanager.LeaderModule;
import navigators.smart.paxosatwar.executionmanager.Round;
import navigators.smart.paxosatwar.roles.Acceptor;
import navigators.smart.reconfiguration.ServerViewManager;
import navigators.smart.statemanagment.StateManager;
import navigators.smart.tom.TOMReceiver;
import navigators.smart.tom.core.messages.TOMMessage;
import navigators.smart.tom.core.messages.TOMMessageType;
import navigators.smart.tom.core.timer.RequestsTimer;
import navigators.smart.tom.core.timer.ForwardedMessage;
import navigators.smart.tom.util.BatchBuilder;
import navigators.smart.tom.util.BatchReader;
import navigators.smart.tom.util.Logger;
import navigators.smart.tom.util.TOMUtil;
import navigators.smart.tom.leaderchange.LCMessage;
import navigators.smart.tom.leaderchange.CollectData;
import navigators.smart.tom.leaderchange.LCManager;
import navigators.smart.tom.leaderchange.LastEidData;
import navigators.smart.tom.server.Recoverable;
import org.apache.commons.codec.binary.Base64;

/**
 * This class implements a thread that uses the PaW algorithm to provide the application
 * a layer of total ordered messages
 */
public final class TOMLayer extends Thread implements RequestReceiver {

    public ExecutionManager execManager;

    public LeaderModule lm;

    public Acceptor acceptor;

    private ServerCommunicationSystem communication;

    private DeliveryThread dt;

    private StateManager stateManager = null;

    /** Manage timers for pending requests */
    public RequestsTimer requestsTimer;

    /** Store requests received but still not ordered */
    public ClientsManager clientsManager;

    /** The id of the consensus being executed (or -1 if there is none) */
    private int inExecution = -1;

    private int lastExecuted = -1;

    private MessageDigest md;

    private Signature engine;

    private BatchBuilder bb = new BatchBuilder();

    private ReentrantLock leaderLock = new ReentrantLock();

    private Condition iAmLeader = leaderLock.newCondition();

    private ReentrantLock messagesLock = new ReentrantLock();

    private Condition haveMessages = messagesLock.newCondition();

    private ReentrantLock proposeLock = new ReentrantLock();

    private Condition canPropose = proposeLock.newCondition();

    /** THIS IS JOAO'S CODE, RELATED TO LEADER CHANGE */
    private LCManager lcManager;

    private boolean leaderChanged = true;

    private PrivateKey prk;

    private ServerViewManager reconfManager;

    /**
     * Creates a new instance of TOMulticastLayer
     * @param manager Execution manager
     * @param receiver Object that receives requests from clients
     * @param lm Leader module
     * @param a Acceptor role of the PaW algorithm
     * @param cs Communication system between replicas
     * @param recManager Reconfiguration Manager
     */
    public TOMLayer(ExecutionManager manager, TOMReceiver receiver, Recoverable recoverer, LeaderModule lm, Acceptor a, ServerCommunicationSystem cs, ServerViewManager recManager) {
        super("TOM Layer");
        this.execManager = manager;
        this.lm = lm;
        this.acceptor = a;
        this.communication = cs;
        this.reconfManager = recManager;
        if (reconfManager.getStaticConf().getRequestTimeout() == 0) {
            this.requestsTimer = null;
        } else this.requestsTimer = new RequestsTimer(this, communication, reconfManager);
        this.clientsManager = new ClientsManager(reconfManager, requestsTimer);
        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
        try {
            this.engine = Signature.getInstance("SHA1withRSA");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        this.prk = reconfManager.getStaticConf().getRSAPrivateKey();
        this.lcManager = new LCManager(this, recManager, md);
        this.dt = new DeliveryThread(this, receiver, recoverer, this.reconfManager);
        this.dt.start();
        this.stateManager = new StateManager(this.reconfManager, this, dt, lcManager, execManager);
    }

    ReentrantLock hashLock = new ReentrantLock();

    /**
     * Computes an hash for a TOM message
     * @param data Data from which to generate the hash
     * @return Hash for the specified TOM message
     */
    public final byte[] computeHash(byte[] data) {
        byte[] ret = null;
        hashLock.lock();
        ret = md.digest(data);
        hashLock.unlock();
        return ret;
    }

    public SignedObject sign(Serializable obj) {
        try {
            return new SignedObject(obj, prk, engine);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return null;
        }
    }

    /**
     * Verifies the signature of a signed object
     * @param so Signed object to be verified
     * @param sender Replica id that supposably signed this object
     * @return True if the signature is valid, false otherwise
     */
    public boolean verifySignature(SignedObject so, int sender) {
        try {
            return so.verify(reconfManager.getStaticConf().getRSAPublicKey(sender), engine);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Retrieve Communication system between replicas
     * @return Communication system between replicas
     */
    public ServerCommunicationSystem getCommunication() {
        return this.communication;
    }

    public void imAmTheLeader() {
        leaderLock.lock();
        iAmLeader.signal();
        leaderLock.unlock();
    }

    /**
     * Sets which consensus was the last to be executed
     * @param last ID of the consensus which was last to be executed
     */
    public void setLastExec(int last) {
        this.lastExecuted = last;
    }

    /**
     * Gets the ID of the consensus which was established as the last executed
     * @return ID of the consensus which was established as the last executed
     */
    public int getLastExec() {
        return this.lastExecuted;
    }

    /**
     * Sets which consensus is being executed at the moment
     *
     * @param inEx ID of the consensus being executed at the moment
     */
    public void setInExec(int inEx) {
        proposeLock.lock();
        Logger.println("(TOMLayer.setInExec) modifying inExec from " + this.inExecution + " to " + inEx);
        this.inExecution = inEx;
        if (inEx == -1 && !isRetrievingState()) {
            canPropose.signalAll();
        }
        proposeLock.unlock();
    }

    /**
     * This method blocks until the PaW algorithm is finished
     */
    public void waitForPaxosToFinish() {
        proposeLock.lock();
        canPropose.awaitUninterruptibly();
        proposeLock.unlock();
    }

    /**
     * Gets the ID of the consensus currently beign executed
     *
     * @return ID of the consensus currently beign executed (if no consensus ir executing, -1 is returned)
     */
    public int getInExec() {
        return this.inExecution;
    }

    /**
     * This method is invoked by the comunication system to deliver a request.
     * It assumes that the communication system delivers the message in FIFO
     * order.
     *
     * @param msg The request being received
     */
    @Override
    public void requestReceived(TOMMessage msg) {
        boolean readOnly = (msg.getReqType() == TOMMessageType.UNORDERED_REQUEST);
        if (readOnly) {
            dt.deliverUnordered(msg, lcManager.getLastReg());
        } else {
            if (clientsManager.requestReceived(msg, true, communication)) {
                messagesLock.lock();
                haveMessages.signal();
                messagesLock.unlock();
            } else {
                Logger.println("(TOMLayer.requestReceive) the received TOMMessage " + msg + " was discarded.");
            }
        }
    }

    /**
     * Creates a value to be proposed to the acceptors. Invoked if this replica is the leader
     * @return A value to be proposed to the acceptors
     */
    private byte[] createPropose(Consensus cons) {
        RequestList pendingRequests = clientsManager.getPendingRequests();
        int numberOfMessages = pendingRequests.size();
        int numberOfNonces = this.reconfManager.getStaticConf().getNumberOfNonces();
        if (cons.getId() > -1) {
            cons.firstMessageProposed = pendingRequests.getFirst();
            cons.firstMessageProposed.consensusStartTime = System.nanoTime();
        }
        cons.batchSize = numberOfMessages;
        Logger.println("(TOMLayer.run) creating a PROPOSE with " + numberOfMessages + " msgs");
        return bb.makeBatch(pendingRequests, numberOfNonces, System.currentTimeMillis(), reconfManager);
    }

    /**
     * This is the main code for this thread. It basically waits until this replica becomes the leader,
     * and when so, proposes a value to the other acceptors
     */
    @Override
    public void run() {
        Logger.println("Running.");
        while (true) {
            leaderLock.lock();
            Logger.println("Next leader for eid=" + (getLastExec() + 1) + ": " + lm.getCurrentLeader());
            if (lm.getCurrentLeader() != this.reconfManager.getStaticConf().getProcessId()) {
                iAmLeader.awaitUninterruptibly();
            }
            leaderLock.unlock();
            proposeLock.lock();
            if (getInExec() != -1) {
                Logger.println("(TOMLayer.run) Waiting for consensus " + getInExec() + " termination.");
                canPropose.awaitUninterruptibly();
            }
            proposeLock.unlock();
            Logger.println("(TOMLayer.run) I'm the leader.");
            messagesLock.lock();
            if (!clientsManager.havePendingRequests()) {
                haveMessages.awaitUninterruptibly();
            }
            messagesLock.unlock();
            Logger.println("(TOMLayer.run) There are messages to be ordered.");
            Logger.println("(TOMLayer.run) I can try to propose.");
            if ((lm.getCurrentLeader() == this.reconfManager.getStaticConf().getProcessId()) && (clientsManager.havePendingRequests()) && (getInExec() == -1)) {
                int execId = getLastExec() + 1;
                setInExec(execId);
                execManager.getProposer().startExecution(execId, createPropose(execManager.getExecution(execId).getLearner()));
            }
        }
    }

    /**
     * Called by the current consensus's execution, to notify the TOM layer that a value was decided
     * @param cons The decided consensus
     */
    public void decided(Consensus cons) {
        this.dt.delivery(cons);
    }

    /**
     * Verify if the value being proposed for a round is valid. It verifies the
     * client signature of all batch requests.
     *
     * TODO: verify timestamps and nonces
     *
     * @param proposedValue the value being proposed
     * @return Valid messages contained in the proposed value
     */
    public TOMMessage[] checkProposedValue(byte[] proposedValue, boolean addToClientManager) {
        Logger.println("(TOMLayer.isProposedValueValid) starting");
        BatchReader batchReader = new BatchReader(proposedValue, this.reconfManager.getStaticConf().getUseSignatures() == 1);
        TOMMessage[] requests = null;
        try {
            requests = batchReader.deserialiseRequests(this.reconfManager);
            if (addToClientManager) {
                for (int i = 0; i < requests.length; i++) {
                    if (!clientsManager.requestReceived(requests[i], false)) {
                        clientsManager.getClientsLock().unlock();
                        Logger.println("(TOMLayer.isProposedValueValid) finished, return=false");
                        System.out.println("failure in deserialize batch");
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            clientsManager.getClientsLock().unlock();
            Logger.println("(TOMLayer.isProposedValueValid) finished, return=false");
            return null;
        }
        Logger.println("(TOMLayer.isProposedValueValid) finished, return=true");
        return requests;
    }

    public void forwardRequestToLeader(TOMMessage request) {
        int leaderId = lm.getCurrentLeader();
        if (this.reconfManager.isCurrentViewMember(leaderId)) {
            System.out.println("(TOMLayer.forwardRequestToLeader) forwarding " + request + " to " + leaderId);
            communication.send(new int[] { leaderId }, new ForwardedMessage(this.reconfManager.getStaticConf().getProcessId(), request));
        }
    }

    public boolean isRetrievingState() {
        boolean result = stateManager != null && stateManager.getWaiting() != -1;
        return result;
    }

    public void setNoExec() {
        Logger.println("(TOMLayer.setNoExec) modifying inExec from " + this.inExecution + " to " + -1);
        proposeLock.lock();
        this.inExecution = -1;
        canPropose.signalAll();
        proposeLock.unlock();
    }

    public void processOutOfContext() {
        for (int nextExecution = getLastExec() + 1; execManager.receivedOutOfContextPropose(nextExecution); nextExecution = getLastExec() + 1) {
            execManager.processOutOfContextPropose(execManager.getExecution(nextExecution));
        }
    }

    public StateManager getStateManager() {
        return stateManager;
    }

    public LCManager getLCManager() {
        return lcManager;
    }

    /**
     * This method is called when there is a timeout and the request has already been forwarded to the leader
     * @param requestList List of requests that the replica wanted to order but didn't manage to
     */
    public void triggerTimeout(List<TOMMessage> requestList) {
        ObjectOutputStream out = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        requestsTimer.stopTimer();
        requestsTimer.Enabled(false);
        if (lcManager.getNextReg() == lcManager.getLastReg()) {
            Logger.println("(TOMLayer.triggerTimeout) initialize synch phase");
            lcManager.setNextReg(lcManager.getLastReg() + 1);
            int regency = lcManager.getNextReg();
            lcManager.setCurrentRequestTimedOut(requestList);
            lcManager.addStop(regency, this.reconfManager.getStaticConf().getProcessId());
            execManager.stop();
            try {
                out = new ObjectOutputStream(bos);
                if (lcManager.getCurrentRequestTimedOut() != null) {
                    byte[] msgs = bb.makeBatch(lcManager.getCurrentRequestTimedOut(), 0, 0, reconfManager);
                    List<TOMMessage> temp = lcManager.getCurrentRequestTimedOut();
                    out.writeBoolean(true);
                    out.writeObject(msgs);
                } else {
                    out.writeBoolean(false);
                }
                byte[] payload = bos.toByteArray();
                out.flush();
                bos.flush();
                out.close();
                bos.close();
                Logger.println("(TOMLayer.triggerTimeout) sending STOP message to install regency " + regency);
                communication.send(this.reconfManager.getCurrentViewOtherAcceptors(), new LCMessage(this.reconfManager.getStaticConf().getProcessId(), TOMUtil.STOP, regency, payload));
            } catch (IOException ex) {
                ex.printStackTrace();
                java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    out.close();
                    bos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            evaluateStops(regency);
        }
    }

    private void evaluateStops(int nextReg) {
        ObjectOutputStream out = null;
        ByteArrayOutputStream bos = null;
        if (lcManager.getStopsSize(nextReg) > this.reconfManager.getQuorumF() && lcManager.getNextReg() == lcManager.getLastReg()) {
            Logger.println("(TOMLayer.evaluateStops) initialize synch phase");
            requestsTimer.Enabled(false);
            requestsTimer.stopTimer();
            lcManager.setNextReg(lcManager.getLastReg() + 1);
            int regency = lcManager.getNextReg();
            lcManager.addStop(regency, this.reconfManager.getStaticConf().getProcessId());
            execManager.stop();
            try {
                bos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bos);
                if (lcManager.getCurrentRequestTimedOut() != null) {
                    out.writeBoolean(true);
                    byte[] msgs = bb.makeBatch(lcManager.getCurrentRequestTimedOut(), 0, 0, reconfManager);
                    out.writeObject(msgs);
                } else {
                    out.writeBoolean(false);
                }
                out.flush();
                bos.flush();
                byte[] payload = bos.toByteArray();
                out.close();
                bos.close();
                Logger.println("(TOMLayer.evaluateStops) sending STOP message to install regency " + regency);
                communication.send(this.reconfManager.getCurrentViewOtherAcceptors(), new LCMessage(this.reconfManager.getStaticConf().getProcessId(), TOMUtil.STOP, regency, payload));
            } catch (IOException ex) {
                ex.printStackTrace();
                java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    out.close();
                    bos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        if (lcManager.getStopsSize(nextReg) > this.reconfManager.getQuorum2F() && lcManager.getNextReg() > lcManager.getLastReg()) {
            Logger.println("(TOMLayer.evaluateStops) installing regency " + lcManager.getNextReg());
            lcManager.setLastReg(lcManager.getNextReg());
            int regency = lcManager.getLastReg();
            lcManager.removeStops(nextReg);
            requestsTimer.Enabled(true);
            requestsTimer.setShortTimeout(-1);
            requestsTimer.startTimer();
            int leader = lcManager.getNewLeader();
            int in = getInExec();
            int last = getLastExec();
            lm.setNewReg(regency);
            lm.setNewLeader(leader);
            if (leader != this.reconfManager.getStaticConf().getProcessId()) {
                try {
                    bos = new ByteArrayOutputStream();
                    out = new ObjectOutputStream(bos);
                    if (last > -1) {
                        out.writeBoolean(true);
                        out.writeInt(last);
                        Execution exec = execManager.getExecution(last);
                        if (exec.getDecisionRound() == null || exec.getDecisionRound().propValue == null) {
                            System.out.println("[DEBUG INFO FOR LAST EID #1]");
                            if (exec.getDecisionRound() == null) System.out.println("No decision round for eid " + last); else System.out.println("round for eid: " + last + ": " + exec.getDecisionRound().toString());
                            if (exec.getDecisionRound().propValue == null) System.out.println("No propose for eid " + last); else {
                                System.out.println("Propose hash for eid " + last + ": " + Base64.encodeBase64String(computeHash(exec.getDecisionRound().propValue)));
                            }
                            return;
                        }
                        byte[] decision = exec.getDecisionRound().propValue;
                        out.writeObject(decision);
                    } else out.writeBoolean(false);
                    if (in > -1) {
                        Execution exec = execManager.getExecution(in);
                        TimestampValuePair quorumWeaks = exec.getQuorumWeaks();
                        HashSet<TimestampValuePair> writeSet = exec.getWriteSet();
                        CollectData collect = new CollectData(this.reconfManager.getStaticConf().getProcessId(), in, quorumWeaks, writeSet);
                        SignedObject signedCollect = sign(collect);
                        out.writeObject(signedCollect);
                    } else {
                        CollectData collect = new CollectData(this.reconfManager.getStaticConf().getProcessId(), -1, new TimestampValuePair(-1, new byte[0]), new HashSet<TimestampValuePair>());
                        SignedObject signedCollect = sign(collect);
                        out.writeObject(signedCollect);
                    }
                    out.flush();
                    bos.flush();
                    byte[] payload = bos.toByteArray();
                    out.close();
                    bos.close();
                    int[] b = new int[1];
                    b[0] = leader;
                    Logger.println("(TOMLayer.evaluateStops) sending STOPDATA of regency " + regency);
                    communication.send(b, new LCMessage(this.reconfManager.getStaticConf().getProcessId(), TOMUtil.STOPDATA, regency, payload));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                } finally {
                    try {
                        out.close();
                        bos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                Logger.println("(TOMLayer.evaluateStops) I'm the leader for this new regency");
                LastEidData lastData = null;
                CollectData collect = null;
                if (last > -1) {
                    Execution exec = execManager.getExecution(last);
                    if (exec.getDecisionRound() == null || exec.getDecisionRound().propValue == null) {
                        System.out.println("[DEBUG INFO FOR LAST EID #2]");
                        if (exec.getDecisionRound() == null) System.out.println("No decision round for eid " + last); else System.out.println("round for eid: " + last + ": " + exec.getDecisionRound().toString());
                        if (exec.getDecisionRound().propValue == null) System.out.println("No propose for eid " + last); else {
                            System.out.println("Propose hash for eid " + last + ": " + Base64.encodeBase64String(computeHash(exec.getDecisionRound().propValue)));
                        }
                        return;
                    }
                    byte[] decision = exec.getDecisionRound().propValue;
                    lastData = new LastEidData(this.reconfManager.getStaticConf().getProcessId(), last, decision, null);
                } else lastData = new LastEidData(this.reconfManager.getStaticConf().getProcessId(), last, null, null);
                lcManager.addLastEid(regency, lastData);
                if (in > -1) {
                    Execution exec = execManager.getExecution(in);
                    TimestampValuePair quorumWeaks = exec.getQuorumWeaks();
                    HashSet<TimestampValuePair> writeSet = exec.getWriteSet();
                    collect = new CollectData(this.reconfManager.getStaticConf().getProcessId(), in, quorumWeaks, writeSet);
                } else collect = new CollectData(this.reconfManager.getStaticConf().getProcessId(), -1, new TimestampValuePair(-1, new byte[0]), new HashSet<TimestampValuePair>());
                SignedObject signedCollect = sign(collect);
                lcManager.addCollect(regency, signedCollect);
            }
        }
    }

    /**
     * This method is called by the MessageHandler each time it received messages related
     * to the leader change
     * @param msg Message recevied from the other replica
     */
    public void deliverTimeoutRequest(LCMessage msg) {
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        switch(msg.getType()) {
            case TOMUtil.STOP:
                {
                    if (msg.getReg() == lcManager.getLastReg() + 1) {
                        Logger.println("(TOMLayer.deliverTimeoutRequest) received regency change request");
                        try {
                            bis = new ByteArrayInputStream(msg.getPayload());
                            ois = new ObjectInputStream(bis);
                            boolean hasReqs = ois.readBoolean();
                            clientsManager.getClientsLock().lock();
                            if (hasReqs) {
                                byte[] temp = (byte[]) ois.readObject();
                                BatchReader batchReader = new BatchReader(temp, reconfManager.getStaticConf().getUseSignatures() == 1);
                                TOMMessage[] requests = batchReader.deserialiseRequests(reconfManager);
                            }
                            clientsManager.getClientsLock().unlock();
                            ois.close();
                            bis.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                            java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        lcManager.addStop(msg.getReg(), msg.getSender());
                        evaluateStops(msg.getReg());
                    }
                }
                break;
            case TOMUtil.STOPDATA:
                {
                    int regency = msg.getReg();
                    if (regency == lcManager.getLastReg() && this.reconfManager.getStaticConf().getProcessId() == lm.getCurrentLeader()) {
                        Logger.println("(TOMLayer.deliverTimeoutRequest) I'm the new leader and I received a STOPDATA");
                        LastEidData lastData = null;
                        SignedObject signedCollect = null;
                        int last = -1;
                        byte[] lastValue = null;
                        int in = -1;
                        TimestampValuePair quorumWeaks = null;
                        HashSet<TimestampValuePair> writeSet = null;
                        try {
                            bis = new ByteArrayInputStream(msg.getPayload());
                            ois = new ObjectInputStream(bis);
                            if (ois.readBoolean()) {
                                last = ois.readInt();
                                lastValue = (byte[]) ois.readObject();
                            }
                            lastData = new LastEidData(msg.getSender(), last, lastValue, null);
                            lcManager.addLastEid(regency, lastData);
                            signedCollect = (SignedObject) ois.readObject();
                            ois.close();
                            bis.close();
                            lcManager.addCollect(regency, signedCollect);
                            int bizantineQuorum = (reconfManager.getCurrentViewN() + reconfManager.getCurrentViewF()) / 2;
                            if (lcManager.getLastEidsSize(regency) > bizantineQuorum && lcManager.getCollectsSize(regency) > bizantineQuorum) {
                                catch_up(regency);
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace(System.err);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace(System.err);
                        }
                    }
                }
                break;
            case TOMUtil.SYNC:
                {
                    int regency = msg.getReg();
                    if (msg.getReg() == lcManager.getLastReg() && msg.getReg() == lcManager.getNextReg() && msg.getSender() == lm.getCurrentLeader()) {
                        LastEidData lastHighestEid = null;
                        int currentEid = -1;
                        HashSet<SignedObject> signedCollects = null;
                        byte[] propose = null;
                        int batchSize = -1;
                        try {
                            bis = new ByteArrayInputStream(msg.getPayload());
                            ois = new ObjectInputStream(bis);
                            lastHighestEid = (LastEidData) ois.readObject();
                            currentEid = ois.readInt();
                            signedCollects = (HashSet<SignedObject>) ois.readObject();
                            propose = (byte[]) ois.readObject();
                            batchSize = ois.readInt();
                            lcManager.setCollects(regency, signedCollects);
                            if (lcManager.sound(lcManager.selectCollects(regency, currentEid))) {
                                finalise(regency, lastHighestEid, currentEid, signedCollects, propose, batchSize, false);
                            }
                            ois.close();
                            bis.close();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (ClassNotFoundException ex) {
                            ex.printStackTrace();
                            java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
        }
    }

    private void catch_up(int regency) {
        Logger.println("(TOMLayer.catch_up) verify STOPDATA info");
        ObjectOutputStream out = null;
        ByteArrayOutputStream bos = null;
        LastEidData lastHighestEid = lcManager.getHighestLastEid(regency);
        int currentEid = lastHighestEid.getEid() + 1;
        HashSet<SignedObject> signedCollects = null;
        byte[] propose = null;
        int batchSize = -1;
        if (lcManager.sound(lcManager.selectCollects(regency, currentEid))) {
            Logger.println("(TOMLayer.catch_up) sound predicate is true");
            signedCollects = lcManager.getCollects(regency);
            Consensus cons = new Consensus(-1);
            propose = createPropose(cons);
            batchSize = cons.batchSize;
            try {
                bos = new ByteArrayOutputStream();
                out = new ObjectOutputStream(bos);
                out.writeObject(lastHighestEid);
                out.writeInt(currentEid);
                out.writeObject(signedCollects);
                out.writeObject(propose);
                out.writeInt(batchSize);
                out.flush();
                bos.flush();
                byte[] payload = bos.toByteArray();
                out.close();
                bos.close();
                Logger.println("(TOMLayer.catch_up) sending SYNC message for regency " + regency);
                communication.send(this.reconfManager.getCurrentViewOtherAcceptors(), new LCMessage(this.reconfManager.getStaticConf().getProcessId(), TOMUtil.SYNC, regency, payload));
                finalise(regency, lastHighestEid, currentEid, signedCollects, propose, batchSize, true);
            } catch (IOException ex) {
                ex.printStackTrace();
                java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    out.close();
                    bos.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                    java.util.logging.Logger.getLogger(TOMLayer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    private int tempRegency = -1;

    private LastEidData tempLastHighestEid = null;

    private int tempCurrentEid = -1;

    private HashSet<SignedObject> tempSignedCollects = null;

    private byte[] tempPropose = null;

    private int tempBatchSize = -1;

    private boolean tempIAmLeader = false;

    public void resumeLC() {
        Execution exec = execManager.getExecution(tempLastHighestEid.getEid());
        Round r = exec.getLastRound();
        if (r == null) {
            r = exec.createRound(reconfManager);
        } else {
            r.clear();
        }
        byte[] hash = computeHash(tempLastHighestEid.getEidDecision());
        r.propValueHash = hash;
        r.propValue = tempLastHighestEid.getEidDecision();
        r.deserializedPropValue = checkProposedValue(tempLastHighestEid.getEidDecision(), false);
        finalise(tempRegency, tempLastHighestEid, tempCurrentEid, tempSignedCollects, tempPropose, tempBatchSize, tempIAmLeader);
    }

    private void finalise(int regency, LastEidData lastHighestEid, int currentEid, HashSet<SignedObject> signedCollects, byte[] propose, int batchSize, boolean iAmLeader) {
        Logger.println("(TOMLayer.finalise) final stage of LC protocol");
        int me = this.reconfManager.getStaticConf().getProcessId();
        Execution exec = null;
        Round r = null;
        if (getLastExec() + 1 < lastHighestEid.getEid()) {
            System.out.println("NEEDING TO USE STATE TRANSFER!! (" + lastHighestEid.getEid() + ")");
            tempRegency = regency;
            tempLastHighestEid = lastHighestEid;
            tempCurrentEid = currentEid;
            tempSignedCollects = signedCollects;
            tempPropose = propose;
            tempBatchSize = batchSize;
            tempIAmLeader = iAmLeader;
            execManager.getStoppedMsgs().add(acceptor.getFactory().createPropose(currentEid, 0, propose, null));
            stateManager.requestAppState(lastHighestEid.getEid());
            return;
        } else if (getLastExec() + 1 == lastHighestEid.getEid()) {
            System.out.println("I'm still at the eid before the most recent one!!! (" + lastHighestEid.getEid() + ")");
            exec = execManager.getExecution(lastHighestEid.getEid());
            r = exec.getLastRound();
            if (r == null) {
                r = exec.createRound(reconfManager);
            } else {
                r.clear();
            }
            byte[] hash = computeHash(lastHighestEid.getEidDecision());
            r.propValueHash = hash;
            r.propValue = lastHighestEid.getEidDecision();
            r.deserializedPropValue = checkProposedValue(lastHighestEid.getEidDecision(), false);
            exec.decided(r, hash);
        }
        byte[] tmpval = null;
        HashSet<CollectData> selectedColls = lcManager.selectCollects(signedCollects, currentEid);
        tmpval = lcManager.getBindValue(selectedColls);
        if (tmpval == null && lcManager.unbound(selectedColls)) {
            Logger.println("(TOMLayer.finalise) did not found a value that might have already been decided");
            tmpval = propose;
        } else Logger.println("(TOMLayer.finalise) found a value that might have been decided");
        if (tmpval != null) {
            Logger.println("(TOMLayer.finalise) resuming normal phase");
            lcManager.removeCollects(regency);
            exec = execManager.getExecution(currentEid);
            exec.incEts();
            exec.removeWritten(tmpval);
            exec.addWritten(tmpval);
            r = exec.getLastRound();
            if (r == null) {
                r = exec.createRound(reconfManager);
            } else {
                r.clear();
            }
            byte[] hash = computeHash(tmpval);
            r.propValueHash = hash;
            r.propValue = tmpval;
            r.deserializedPropValue = checkProposedValue(tmpval, false);
            if (exec.getLearner().firstMessageProposed == null) {
                if (r.deserializedPropValue != null && r.deserializedPropValue.length > 0) exec.getLearner().firstMessageProposed = r.deserializedPropValue[0]; else exec.getLearner().firstMessageProposed = new TOMMessage();
            }
            r.setWeak(me, hash);
            lm.setNewReg(regency);
            execManager.restart();
            setInExec(currentEid);
            if (iAmLeader) {
                Logger.println("(TOMLayer.finalise) wake up proposer thread");
                imAmTheLeader();
            }
            Logger.println("(TOMLayer.finalise) sending WEAK message");
            communication.send(this.reconfManager.getCurrentViewOtherAcceptors(), acceptor.getFactory().createWeak(currentEid, r.getNumber(), r.propValueHash));
        } else Logger.println("(TOMLayer.finalise) sync phase failed for regency" + regency);
    }
}
