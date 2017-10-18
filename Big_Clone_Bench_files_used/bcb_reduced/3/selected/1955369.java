package com.live.spaces.shanboli;

import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;

/**
 * A simple and re-usable exmaple of creating various JXTA IDs
 * <p/>
 * This is a two part tutorial :
 * <ol>
 * <li>Illustrates the creation of predictable ID's based upon the hash of a
 * provided string. This method provides an independent and deterministic
 * method for generating IDs. However using this method does require care
 * in the choice of the seed expression in order to prevent ID collisions
 * (duplicate IDs).</li>
 * <p/>
 * <li>New random ID's encoded with a specific GroupID.</li>
 * </ol>
 */
public class MyID {

    private static final String SEED = "IDTuorial";

    /**
     * Returns a SHA1 hash of string.
     *
     * @param expression to hash
     * @return a SHA1 hash of string or {@code null} if the expression could
     *         not be hashed.
     */
    private static byte[] hash(final String expression) {
        byte[] result;
        MessageDigest digest;
        if (expression == null) {
            throw new IllegalArgumentException("Invalid null expression");
        }
        try {
            digest = MessageDigest.getInstance("SHA1");
        } catch (NoSuchAlgorithmException failed) {
            failed.printStackTrace(System.err);
            RuntimeException failure = new IllegalStateException("Could not get SHA-1 Message");
            failure.initCause(failed);
            throw failure;
        }
        try {
            byte[] expressionBytes = expression.getBytes("UTF-8");
            result = digest.digest(expressionBytes);
        } catch (UnsupportedEncodingException impossible) {
            RuntimeException failure = new IllegalStateException("Could not encode expression as UTF8");
            failure.initCause(impossible);
            throw failure;
        }
        return result;
    }

    /**
     * Given a pipe name, it returns a PipeID who's value is chosen based upon that name.
     *
     * @param pipeName instance name
     * @param pgID     the group ID encoding
     * @return The pipeID value
     */
    public static PipeID createPipeID(PeerGroupID pgID, String pipeName) {
        String seed = pipeName + SEED;
        return IDFactory.newPipeID(pgID, hash(seed.toLowerCase()));
    }

    /**
     * Creates group encoded random PipeID.
     *
     * @param pgID the group ID encoding
     * @return The pipeID value
     */
    public static PipeID createNewPipeID(PeerGroupID pgID) {
        return IDFactory.newPipeID(pgID);
    }

    /**
     * Creates group encoded random PeerID.
     *
     * @param pgID the group ID encoding
     * @return The PeerID value
     */
    public static PeerID createNewPeerID(PeerGroupID pgID) {
        return IDFactory.newPeerID(pgID);
    }

    /**
     * Given a peer name generates a Peer ID who's value is chosen based upon that name.
     *
     * @param peerName instance name
     * @param pgID     the group ID encoding
     * @return The PeerID value
     */
    public static PeerID createPeerID(PeerGroupID pgID, String peerName) {
        String seed = peerName + SEED;
        return IDFactory.newPeerID(pgID, hash(seed.toLowerCase()));
    }

    /**
     * Creates group encoded random PeerGroupID.
     *
     * @param pgID the group ID encoding
     * @return The PeerGroupID value
     */
    public static PeerGroupID createNewPeerGroupID(PeerGroupID pgID) {
        return IDFactory.newPeerGroupID(pgID);
    }

    /**
     * Given a group name generates a Peer Group ID who's value is chosen based upon that name.
     *
     * @param groupName group name encoding value
     * @return The PeerGroupID value
     */
    public static PeerGroupID createPeerGroupID(final String groupName) {
        return IDFactory.newPeerGroupID(PeerGroupID.defaultNetPeerGroupID, hash(SEED + groupName.toLowerCase()));
    }

    /**
     * Contructs and returns an string encoded Infrastructure PeerGroupID.
     *
     * @param groupName the string encoding
     * @return The infraPeerGroupID PeerGroupID
     */
    public static PeerGroupID createInfraPeerGroupID(String groupName) {
        return createPeerGroupID(groupName);
    }

    /**
     * Main method
     *
     * @param args command line arguments.  None defined
     */
    public static void main(String args[]) {
        PeerGroupID infra = createInfraPeerGroupID("infra");
        PeerID peerID = createPeerID(infra, "peer");
        PipeID pipeID = createPipeID(PeerGroupID.defaultNetPeerGroupID, "pipe");
        System.out.println(MessageFormat.format("\n\nAn infrastucture PeerGroupID: {0}", infra.toString()));
        System.out.println(MessageFormat.format("PeerID with the above infra ID encoding: {0}", peerID.toString()));
        System.out.println(MessageFormat.format("PipeID with the default defaultNetPeerGroupID encoding: {0}", pipeID.toString()));
        peerID = createNewPeerID(PeerGroupID.defaultNetPeerGroupID);
        pipeID = createNewPipeID(PeerGroupID.defaultNetPeerGroupID);
        PeerGroupID pgid = createNewPeerGroupID(PeerGroupID.defaultNetPeerGroupID);
        System.out.println(MessageFormat.format("\n\nNew PeerID created : {0}", peerID.toString()));
        System.out.println(MessageFormat.format("New PipeID created : {0}", pipeID.toString()));
        System.out.println(MessageFormat.format("New PeerGroupID created : {0}", pgid.toString()));
    }
}
