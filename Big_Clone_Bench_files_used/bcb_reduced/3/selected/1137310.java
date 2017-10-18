package net.jxta.impl.id.CBID;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.logging.Logger;

/**
 * An implementation of the {@link net.jxta.pipe.PipeID} ID Type.
 */
public class PipeID extends net.jxta.impl.id.UUID.PipeID {

    /**
     * Log4J categorgy
     */
    private static final transient Logger LOG = Logger.getLogger(PipeID.class.getName());

    /**
     * Used only internally
     */
    protected PipeID() {
        super();
    }

    /**
     * Constructor.
     * Intializes contents from provided ID.
     *
     * @param id the ID data
     */
    protected PipeID(IDBytes id) {
        super(id);
    }

    /**
     * Creates a PipeID. A PeerGroupID is provided
     *
     * @param groupUUID the UUID of the group to which this will belong.
     * @param idUUID    the UUID which will be used for this pipe.
     */
    protected PipeID(UUID groupUUID, UUID idUUID) {
        super(groupUUID, idUUID);
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPipeID(net.jxta.peergroup.PeerGroupID)}.
     * @param groupID the PeerGroupID
     */
    public PipeID(PeerGroupID groupID) {
        this(groupID.getUUID(), UUIDFactory.newUUID());
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPipeID(net.jxta.peergroup.PeerGroupID,byte[])}.
     * @param groupID the PeerGroupID
     * @param seed the seed
     */
    public PipeID(PeerGroupID groupID, byte[] seed) {
        this();
        UUID groupCBID = groupID.getUUID();
        id.longIntoBytes(PipeID.groupIdOffset, groupCBID.getMostSignificantBits());
        id.longIntoBytes(PipeID.groupIdOffset + 8, groupCBID.getLeastSignificantBits());
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
        UUID pipeCBID = UUIDFactory.newUUID(buf16);
        id.longIntoBytes(PipeID.idOffset, pipeCBID.getMostSignificantBits());
        id.longIntoBytes(PipeID.idOffset + 8, pipeCBID.getLeastSignificantBits());
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
        UUID groupCBID = new UUID(id.bytesIntoLong(PipeID.groupIdOffset), id.bytesIntoLong(PipeID.groupIdOffset + 8));
        PeerGroupID groupID = new PeerGroupID(groupCBID);
        return IDFormat.translateToWellKnown(groupID);
    }

    /**
     * Returns the UUID associated with this PipeID.
     *
     * @return The UUID associated with this PipeID.
     */
    public UUID getUUID() {
        return new UUID(id.bytesIntoLong(PipeID.idOffset), id.bytesIntoLong(PipeID.idOffset + 8));
    }
}
