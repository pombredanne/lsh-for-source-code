package net.jxta.impl.id.CBID;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.logging.Logger;

/**
 * An implementation of the {@link net.jxta.peer.PeerID} ID Type.
 */
public class PeerID extends net.jxta.impl.id.UUID.PeerID {

    /**
     * Log4J Logger
     */
    private static final transient Logger LOG = Logger.getLogger(PeerID.class.getName());

    /**
     * Used only internally.
     */
    protected PeerID() {
        super();
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPeerID(net.jxta.peergroup.PeerGroupID)}.
     *
     * @param groupID the PeerGroupID
     */
    public PeerID(PeerGroupID groupID) {
        this(groupID.getUUID(), UUIDFactory.newUUID());
    }

    /**
     * Intializes contents from provided ID.
     *
     * @param id the ID data
     */
    protected PeerID(IDBytes id) {
        super(id);
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPeerID(net.jxta.peergroup.PeerGroupID,byte[])}.
     *
     * @param groupID the PeerGroupID
     * @param seed the seed
     */
    public PeerID(PeerGroupID groupID, byte[] seed) {
        this();
        UUID groupUUID = groupID.getUUID();
        id.longIntoBytes(PeerID.groupIdOffset, groupUUID.getMostSignificantBits());
        id.longIntoBytes(PeerID.groupIdOffset + 8, groupUUID.getLeastSignificantBits());
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException caught) {
            digester = null;
        }
        if (digester == null) {
            throw new ProviderException("SHA1 digest algorithm not found");
        }
        byte[] digest = digester.digest(seed);
        byte[] buf16 = new byte[16];
        System.arraycopy(digest, 0, buf16, 0, 16);
        UUID peerCBID = new UUID(buf16);
        id.longIntoBytes(PeerID.idOffset, peerCBID.getMostSignificantBits());
        id.longIntoBytes(PeerID.idOffset + 8, peerCBID.getLeastSignificantBits());
    }

    /**
     * Creates a PeerID. A PeerGroupID is provided
     *
     * @param groupUUID the group to which this will belong.
     * @param peerUUID  id of this peer
     */
    protected PeerID(UUID groupUUID, UUID peerUUID) {
        super(groupUUID, peerUUID);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIDFormat() {
        return IDFormat.INSTANTIATOR.getSupportedIDFormat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public net.jxta.id.ID getPeerGroupID() {
        UUID groupCBID = new UUID(id.bytesIntoLong(PeerID.groupIdOffset), id.bytesIntoLong(PeerID.groupIdOffset + 8));
        PeerGroupID groupID = new PeerGroupID(groupCBID);
        return IDFormat.translateToWellKnown(groupID);
    }

    /**
     * Returns the UUID associated with this PeerID.
     *
     * @return The UUID associated with this PeerID.
     */
    public UUID getUUID() {
        return new UUID(id.bytesIntoLong(PeerID.idOffset), id.bytesIntoLong(PeerID.idOffset + 8));
    }
}
