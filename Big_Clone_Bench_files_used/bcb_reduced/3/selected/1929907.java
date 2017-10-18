package org.vexi.framework;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.Vector;
import org.apache.commons.codec.binary.Base64;

/**
 * A generic session.
 * 
 * @author mike
 */
public class GenericSession<U extends IUser> implements ISession<U> {

    protected String sessionid;

    protected long timeLastUsed;

    protected LinkedList<Vector<?>> outgoingMessageQueue;

    protected U user;

    public GenericSession(U user) {
        this.user = user;
        timeLastUsed = System.currentTimeMillis();
        outgoingMessageQueue = new LinkedList<Vector<?>>();
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            sessionid = new String(Base64.encodeBase64(md5.digest((user.getUserName() + timeLastUsed).getBytes())));
            System.out.println("User logged in: " + user + "(" + sessionid + ")");
        } catch (NoSuchAlgorithmException e) {
            sessionid = "";
            e.printStackTrace();
        }
    }

    /**
   * Get the next message. Blocks when no messages are available, so needs to be done
   * in a seperate thread.
   */
    public synchronized Vector getNextMessage() {
        while (outgoingMessageQueue.isEmpty()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Vector v = (Vector) outgoingMessageQueue.removeFirst();
        return v;
    }

    /**
   * Adds a message to the outgoing queue of for this sessions. Notifies for blocked
   * threads in the getNextMessage() function 
   * 
   * @param message
   */
    public synchronized void addMessage(Vector message) {
        outgoingMessageQueue.addLast(message);
        System.out.println("adding:" + message);
        this.notifyAll();
    }

    /**
   * @return Returns the user.
   */
    public U getUser() {
        return user;
    }

    /**
   * @return Returns the sessionid.
   */
    public String getSessionid() {
        return sessionid;
    }

    /**
   * @param timeLastUsed The timeLastUsed to set.
   */
    public void setTimeLastUsed() {
        this.timeLastUsed = System.currentTimeMillis();
    }

    /**
   * @return Returns the timeLastUsed.
   */
    public long getTimeLastUsed() {
        return timeLastUsed;
    }

    public void stealSession(ISession session) {
    }
}
