package hypercast;

import hypercast.events.NOTIFICATION_EVENT;
import hypercast.util.XmlUtil;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Iterator;
import java.util.Random;
import java.util.Hashtable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This class is a finite state machine which handles synchronization.
 * 
 * @author HyperCast Team
 * @author Guimin Zhang
 * @version 2.0, May. 23, 2001
 */
public class MessageStoreFSM_Sync extends I_MessageStoreFSM {

    /** OL_Socket object */
    private OL_Socket socket;

    /** MessageStore */
    private MessageStore messageStore;

    /** logical address of this node */
    private I_LogicalAddress mylogicaladdress;

    /** root logical address */
    private I_LogicalAddress root = null;

    /** logical address of neighbor from which message was obtained */
    private I_LogicalAddress neighboraddress = null;

    /** Message identifier */
    private byte[] messageid = null;

    /** Application payload */
    private byte[] data;

    /** The state information of the message in the finite state machine (FSM) */
    private byte myState;

    /** ADF_Payload for Synchronization */
    public static final byte PAYLOAD = -1;

    /** ADF_Control Types for Syncronization */
    public static final byte QUERY_SYNC_ALL = 10;

    public static final byte QUERY_MSG_SYNC = 11;

    public static final byte HAVE_IT = 12;

    public static final byte DONT_HAVE_IT = 13;

    public static final byte NACK_SYNC = 14;

    /** Size of hash (in integers) that is created for the message from message identifier.
     *  (Messages are kept in a hashtable.) */
    protected static final int DIGEST_SIZE = 4;

    /** The state of the message,Init */
    public static final byte Init = 0;

    /** The state of the message,Don't have payload  and try to recover*/
    public static final byte NoPayload_Recover = 1;

    /** The state of the message,Don't have payload and don't recover*/
    public static final byte NoPayload_NoRecover = 2;

    /** The state of the message,Wait for Payload*/
    public static final byte WaitforPayload = 3;

    /** The state of the message,have payload */
    public static final byte HavePayload = 4;

    /**the NACK timeout id */
    private static final int NACK_TIMER_INDEX = 0;

    /**the max NACK timeout id */
    private static final int MAXNACK_TIMER_INDEX = 1;

    /**the Recover timeout id */
    private static final int REC_TIMER_INDEX = 2;

    /**the delete timeout id */
    private static final int DELETE_TIMER_INDEX = 3;

    /**the final delete timeout id */
    private static final int FINALDELETE_TIMER_INDEX = 4;

    /** The timeout const,timeout_NACK(in millisecond) */
    private static long timeout_NACK = -1;

    /** The timeout const,max_NACK(in millisecond) */
    private static long max_NACK;

    /** The timeout const,timeout_NACK(in millisecond) */
    private static long timeout_REC;

    /** The timeout const,delete(in millisecond) */
    private static long delete;

    /** The timeout const,final_delete(in millisecond) */
    private static long final_delete;

    protected static long synchronizationPeriod;

    /** Maxium transmission rate */
    protected static long maximumTransmissionRate;

    /**
     * Used to keep track of the amount of information received via synchronization messages
     */
    private static long totalSynchronizedPayloadSize = 0;

    private static FiniteStateMachineMetaOperations metaOps;

    protected static FiniteStateMachineMetaOperations registerMetaOperations() {
        if (metaOps == null) {
            metaOps = new SyncMetaOperations();
        }
        return metaOps;
    }

    /**
     * Constructs a MessageStoreFSM_Sync object with OL_Socket and MessageStore. 
     * @param   socket OL_Socket
     * @param   messageStore MessageStore
     */
    public MessageStoreFSM_Sync(final OL_Socket socket, final MessageStore messageStore, final HyperCastConfig config) {
        this.socket = socket;
        this.messageStore = messageStore;
        this.myState = Init;
        this.mylogicaladdress = socket.getLogicalAddress();
        if (timeout_NACK < 0) {
            timeout_NACK = config.getPositiveLongAttribute(XmlUtil.createXPath("/Public/NetworkService/EnhancedDelivery/Sync/Synchronization/TimeoutNACK"));
            max_NACK = config.getPositiveLongAttribute(XmlUtil.createXPath("/Public/NetworkService/EnhancedDelivery/Sync/Synchronization/MaxTimeoutNACK"));
            timeout_REC = config.getPositiveLongAttribute(XmlUtil.createXPath("/Public/NetworkService/EnhancedDelivery/Sync/Synchronization/TimeoutREC"));
            delete = config.getPositiveLongAttribute(XmlUtil.createXPath("/Public/NetworkService/EnhancedDelivery/Sync/Synchronization/Delete"));
            final_delete = config.getPositiveLongAttribute(XmlUtil.createXPath("/Public/NetworkService/EnhancedDelivery/Sync/Synchronization/FinalDelete"));
        }
        messageStore.setTimer(this, FINALDELETE_TIMER_INDEX, final_delete);
    }

    /** check the change of the neighborhood
     */
    public void changingNeighbor() {
    }

    /**
     * @return the finite state machine id
     */
    public short getFSMID() {
        return MessageStore.FSM_SYNCHRONIZATION;
    }

    /**Set the new ADF_Data message entry
     * the information buffered includes the message id, the root of the tree
     * and the payload data 
     * @param msg OL_Message
     */
    private void setADMsg(OL_Message msg) {
        messageid = msg.getMessageIdentifier();
        data = msg.getPayload();
        root = msg.getSourceAddress();
    }

    /**Set the new ADF_Control message entry
     * the information buffered includes the message id, the root of the tree
     * @param msg OL_Message
     */
    private void setACMsg(OL_Message msg) {
        messageid = msg.getMessageIdentifier();
        root = socket.createLogicalAddress(((FSM_Extension) msg.getFirstExtensionByType(Extension.FSM)).getSenderAddress(), 0);
    }

    /**
     * This is the method used to send nack or don't have it or have it or nack sync message.
     * @param type  
     * @param la  routing logical address
     */
    private void sendToNode(byte type, I_LogicalAddress la) {
        OL_Message msg = (OL_Message) socket.createMessage(null);
        ;
        msg.setHopLimit((short) 1);
        msg.setDeliveryMode(OL_Message.DELIVERY_MODE_UNICAST);
        msg.setDestinationAddress(la);
        FSM_Extension ee = new FSM_Extension(MessageStore.FSM_SYNCHRONIZATION, type, messageid, root);
        msg.addExtension(ee);
        socket.forwardToParent(msg);
    }

    /**get the message ID
     */
    public byte[] getID() {
        return messageid;
    }

    /**get the state of this finite state machine
     */
    public byte getState() {
        return myState;
    }

    /**get the sender's logical address
     */
    public I_LogicalAddress getRoot() {
        return root;
    }

    /**get the payload length
     */
    protected int getPayloadLength() {
        if (data == null) {
            return 0;
        } else {
            return data.length;
        }
    }

    /**process new ADF_Data message
     * @param msg OL_Message
     */
    public void newADMsg(OL_Message msg) {
        socket.appmsgArrived(msg, socket.callback);
        setADMsg(msg);
        myState = HavePayload;
        messageStore.setTimer(this, DELETE_TIMER_INDEX, delete);
    }

    /**Process the new ADF_Control message 
     * @param msg OL_Message
     */
    public void newACMsg(OL_Message msg) {
        setACMsg(msg);
        FSM_Extension e = ((FSM_Extension) msg.getFirstExtensionByType(Extension.FSM));
        switch(e.getType()) {
            case DONT_HAVE_IT:
                {
                    myState = NoPayload_NoRecover;
                    messageStore.setTimer(this, REC_TIMER_INDEX, timeout_REC);
                }
                break;
            case HAVE_IT:
                {
                    sendToNode(NACK_SYNC, msg.getSourceAddress());
                    neighboraddress = msg.getSourceAddress();
                    myState = WaitforPayload;
                    messageStore.setTimer(this, NACK_TIMER_INDEX, timeout_NACK);
                    messageStore.setTimer(this, MAXNACK_TIMER_INDEX, max_NACK);
                }
                break;
            case NACK_SYNC:
            case QUERY_MSG_SYNC:
                {
                    sendToNode(DONT_HAVE_IT, msg.getSourceAddress());
                    myState = NoPayload_NoRecover;
                    messageStore.setTimer(this, REC_TIMER_INDEX, timeout_REC);
                }
                break;
        }
    }

    /**Update the known ADF_Data message entry
     * @param msg OL_Message
     */
    public void updateADMsg(OL_Message msg) {
        if (myState != HavePayload) {
            socket.appmsgArrived(msg, socket.callback);
            data = msg.getPayload();
            myState = HavePayload;
            messageStore.setTimer(this, DELETE_TIMER_INDEX, delete);
        }
    }

    /**Update the new ADF_Control message 
     * @param msg OL_Message
     */
    public void updateACMsg(OL_Message msg) {
        FSM_Extension e = ((FSM_Extension) msg.getFirstExtensionByType(Extension.FSM));
        switch(myState) {
            case NoPayload_Recover:
                {
                    switch(e.getType()) {
                        case DONT_HAVE_IT:
                            {
                                myState = NoPayload_NoRecover;
                                messageStore.setTimer(this, REC_TIMER_INDEX, timeout_REC);
                            }
                            break;
                        case HAVE_IT:
                            {
                                neighboraddress = msg.getSourceAddress();
                                sendToNode(NACK_SYNC, msg.getSourceAddress());
                                myState = WaitforPayload;
                                messageStore.setTimer(this, NACK_TIMER_INDEX, timeout_NACK);
                                messageStore.setTimer(this, MAXNACK_TIMER_INDEX, max_NACK);
                            }
                            break;
                        case NACK_SYNC:
                        case QUERY_MSG_SYNC:
                            {
                                sendToNode(DONT_HAVE_IT, msg.getSourceAddress());
                                myState = NoPayload_NoRecover;
                                messageStore.setTimer(this, REC_TIMER_INDEX, timeout_REC);
                            }
                            break;
                    }
                }
                break;
            case NoPayload_NoRecover:
                {
                    switch(e.getType()) {
                        case HAVE_IT:
                            {
                                neighboraddress = msg.getSourceAddress();
                                sendToNode(NACK_SYNC, msg.getSourceAddress());
                                myState = WaitforPayload;
                                messageStore.setTimer(this, NACK_TIMER_INDEX, timeout_NACK);
                                messageStore.setTimer(this, MAXNACK_TIMER_INDEX, max_NACK);
                            }
                            break;
                        case NACK_SYNC:
                        case QUERY_MSG_SYNC:
                            {
                                sendToNode(DONT_HAVE_IT, msg.getSourceAddress());
                            }
                            break;
                    }
                }
                break;
            case WaitforPayload:
                {
                    switch(e.getType()) {
                        case DONT_HAVE_IT:
                            {
                                if (neighboraddress != msg.getSourceAddress() && neighboraddress != null) {
                                    sendToNode(NACK_SYNC, neighboraddress);
                                } else {
                                    myState = NoPayload_NoRecover;
                                    messageStore.setTimer(this, REC_TIMER_INDEX, timeout_REC);
                                }
                            }
                            break;
                        case HAVE_IT:
                            {
                                neighboraddress = msg.getSourceAddress();
                            }
                            break;
                        case NACK_SYNC:
                        case QUERY_MSG_SYNC:
                            {
                                sendToNode(DONT_HAVE_IT, msg.getSourceAddress());
                            }
                            break;
                    }
                }
                break;
            case HavePayload:
                {
                    if (e.getType() == NACK_SYNC) {
                        OL_Message payload = (OL_Message) socket.createMessage(data);
                        ;
                        msg.setHopLimit((short) 1);
                        FSM_Extension ee = new FSM_Extension(MessageStore.FSM_SYNCHRONIZATION, PAYLOAD, messageid);
                        payload.addExtension(ee);
                        payload.setDeliveryMode(OL_Message.DELIVERY_MODE_UNICAST);
                        payload.setDestinationAddress(msg.getSourceAddress());
                        socket.forwardToParent(payload);
                    } else if (e.getType() == QUERY_MSG_SYNC) {
                        sendToNode(HAVE_IT, msg.getSourceAddress());
                    }
                }
                break;
        }
    }

    /**Set the time expired events
     * @param timerIndex
     */
    public void timerExpired(int timerIndex) {
        switch(myState) {
            case (Init):
                {
                    if (timerIndex == FINALDELETE_TIMER_INDEX) {
                    }
                }
                break;
            case (NoPayload_Recover):
                {
                }
                break;
            case (NoPayload_NoRecover):
                {
                    if (timerIndex == REC_TIMER_INDEX) {
                    }
                }
                break;
            case (WaitforPayload):
                {
                    if (timerIndex == NACK_TIMER_INDEX) {
                        if (neighboraddress != null) {
                            sendToNode(NACK_SYNC, neighboraddress);
                        }
                        messageStore.setTimer(this, NACK_TIMER_INDEX, timeout_NACK);
                    } else if (timerIndex == MAXNACK_TIMER_INDEX) {
                        myState = NoPayload_Recover;
                    }
                }
                break;
            case (HavePayload):
                {
                    if (timerIndex == DELETE_TIMER_INDEX) {
                    }
                }
                break;
        }
    }

    /**
     * This method sends a QUERY_SYNC_ALL message to a
     * MessageStore contained in a neighbor socket.
     *
     * The format for this message is specified in
     * HyperCast3.0-MessageFormats.doc Section 5.4.2
     */
    protected static void sendQuery(Hashtable store, OL_Socket socket) {
        int payloadlength = FSM_Extension.EXTENSION_HEADER_LENGTH + DIGEST_SIZE * 4;
        byte[] payload = new byte[payloadlength];
        int index = 0;
        System.arraycopy(ByteArrayUtility.toByteArray(MessageStore.FSM_SYNCHRONIZATION), 0, payload, index, 2);
        index += 2;
        payload[index] = MessageStoreFSM_Sync.QUERY_SYNC_ALL;
        index += 1;
        byte[] id = new byte[FSM_Extension.ID_HASH_LENGTH + FSM_Extension.ID_COUNTER_LENGTH];
        (new Random()).nextBytes(id);
        System.arraycopy(id, 0, payload, index, id.length);
        index += FSM_Extension.ID_HASH_LENGTH + FSM_Extension.ID_COUNTER_LENGTH;
        int[] digest = new int[DIGEST_SIZE];
        Enumeration e = store.keys();
        while (e.hasMoreElements()) {
            I_MessageStoreFSM fsm = (I_MessageStoreFSM) store.get(e.nextElement());
            if (fsm.getFSMID() == MessageStore.FSM_SYNCHRONIZATION) {
                MessageStoreFSM_Sync FSM = (MessageStoreFSM_Sync) fsm;
                if (FSM.getState() == MessageStoreFSM_Sync.HavePayload) {
                    int[] h = new int[4];
                    generateHashkey(FSM.getID(), h);
                    for (int i = 0; i < 4; i++) {
                        int temp = (int) h[i] / 32;
                        int shift = h[i] % 32;
                        digest[temp] = (digest[temp] | 1 << shift);
                    }
                }
            }
        }
        for (int i = 0; i < DIGEST_SIZE; i++) {
            System.arraycopy(ByteArrayUtility.toByteArray(digest[i]), 0, payload, index + i * 4, 4);
        }
        OL_Message msg = (OL_Message) socket.createMessage(null);
        ;
        msg.setHopLimit((short) 1);
        msg.setDeliveryMode(OL_Message.DELIVERY_MODE_UNICAST);
        if (socket.getNeighbors() != null) {
            int randomIndex = (int) (Math.random() * socket.getNeighbors().length);
            msg.setDestinationAddress(socket.getNeighbors()[randomIndex]);
            msg.addExtension(Extension.createExtension(Extension.FSM, payload));
            socket.forwardToParent(msg);
        }
    }

    /** This method is used to process the received QUERY_SYNC_ALL message from neighbor messagestore.
     *  Each message stored in this MessageStore is compared with the message digestcontained in the 
     *  received QUERY_SYNC_ALL message. If the message id is not contained in the message digest, meaning
     *  that the neighbor messagestore does not have this message. Therefore, a HAVE_IT message is send to the
     *  neighbor messagestore.   
     */
    protected static void receiveQuery(Hashtable store, OL_Socket socket, OL_Message querymsg) {
        System.out.println("+++++++Query Received+++++++++++");
        byte[] querypayload = querymsg.getFirstExtensionByType(Extension.FSM).toByteArray();
        int index = 0;
        index += FSM_Extension.EXTENSION_HEADER_LENGTH;
        int[] queryDigest = new int[DIGEST_SIZE];
        for (int i = 0; i < DIGEST_SIZE; i++) {
            queryDigest[i] = ByteArrayUtility.toInteger(querypayload, index + i * 4);
        }
        Vector tobeSent = new Vector();
        Enumeration e = store.keys();
        while (e.hasMoreElements()) {
            I_MessageStoreFSM fsm = (I_MessageStoreFSM) store.get(e.nextElement());
            if (fsm.getFSMID() == MessageStore.FSM_SYNCHRONIZATION) {
                MessageStoreFSM_Sync FSM = (MessageStoreFSM_Sync) fsm;
                if (FSM.getState() == MessageStoreFSM_Sync.HavePayload) {
                    if (!compare(queryDigest, FSM.getID())) {
                        OL_Message msg = (OL_Message) socket.createMessage(null);
                        ;
                        msg.setHopLimit((short) 1);
                        msg.setDeliveryMode(OL_Message.DELIVERY_MODE_UNICAST);
                        msg.setDestinationAddress(querymsg.getSourceAddress());
                        FSM_Extension ee = new FSM_Extension(MessageStore.FSM_SYNCHRONIZATION, MessageStoreFSM_Sync.HAVE_IT, FSM.getID(), FSM.getRoot());
                        msg.addExtension(ee);
                        tobeSent.addElement(msg);
                        totalSynchronizedPayloadSize += FSM.getPayloadLength();
                    }
                }
            }
        }
        if (totalSynchronizedPayloadSize > 0) {
            if (synchronizationPeriod < totalSynchronizedPayloadSize / maximumTransmissionRate) {
                synchronizationPeriod = totalSynchronizedPayloadSize / maximumTransmissionRate;
            }
        }
        for (int i = 0; i < tobeSent.size(); i++) {
            OL_Message message = (OL_Message) tobeSent.elementAt(tobeSent.size() - i - 1);
            socket.forwardToParent(message);
        }
    }

    /**
     * Calculate hash-key for message ID. The "return value" of this
     * method is the second parameter which is assumed to be
     * initialized to 4 element array of ints.
     */
    private static void generateHashkey(byte[] id, int[] hash_key) {
        MessageDigest sha;
        try {
            sha = MessageDigest.getInstance("md5");
            sha.update(id);
            byte[] out = sha.digest();
            int offset = 0;
            for (int i = 0; i < 4; i++) {
                int temp = ByteArrayUtility.toInteger(out, offset);
                offset += 4;
                if (temp < 0) {
                    temp = (-1) * temp + 2 ^ 31;
                }
                hash_key[i] = temp % (DIGEST_SIZE * 32);
            }
        } catch (NoSuchAlgorithmException e) {
            throw new HyperCastFatalRuntimeException(e);
        }
    }

    /**
     * Compare the query
     *
     * @param digest  int[]
     * @param id UniqueID
     * return true if there is match
     */
    private static boolean compare(int[] digest, byte[] id) {
        int[] h = new int[4];
        generateHashkey(id, h);
        for (int i = 0; i < 4; i++) {
            int emp = (int) h[i] / 32;
            int sh = h[i] % 32;
            if (((digest[emp] >> sh) & 0x01) != 1) {
                return false;
            }
        }
        return true;
    }
}

class SyncMetaOperations implements FiniteStateMachineMetaOperations {

    public boolean messageStoreWillForwardMessage(OL_Message message) {
        return false;
    }

    public boolean isValidMessage(OL_Message message) {
        return true;
    }

    public boolean processIntermediateUnicastMessage(OL_Message message) {
        return false;
    }

    public NOTIFICATION_EVENT notifyOnSend(OL_Message message) {
        return null;
    }
}
