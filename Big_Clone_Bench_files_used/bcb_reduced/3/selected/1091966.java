package net.jxta.impl.id.CBID;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.logging.Logger;

/**
 * An implementation of the {@link net.jxta.peergroup.PeerGroupID} ID Type.
 */
public class PeerGroupID extends net.jxta.impl.id.UUID.PeerGroupID {

    /**
     * Log4J categorgy
     */
    private static final transient Logger LOG = Logger.getLogger(PeerGroupID.class.getName());

    /**
     * Intializes contents from provided ID.
     *
     * @param id the ID data
     */
    protected PeerGroupID(IDBytes id) {
        super(id);
    }

    /**
     * Creates a PeerGroupID. A PeerGroupID is provided
     *
     * @param groupUUID the PeerGroupID to use to construct the new PeerGroupID
     */
    protected PeerGroupID(UUID groupUUID) {
        super(groupUUID);
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPeerGroupID()}.
     */
    public PeerGroupID() {
        this(UUIDFactory.newUUID());
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPeerGroupID(byte[])}.
     * @param seed the seed
     */
    public PeerGroupID(byte[] seed) {
        super();
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
        UUID groupUUID = UUIDFactory.newUUID(buf16);
        id.longIntoBytes(PeerGroupID.groupIdOffset, groupUUID.getMostSignificantBits());
        id.longIntoBytes(PeerGroupID.groupIdOffset + 8, groupUUID.getLeastSignificantBits());
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newPeerGroupID(net.jxta.peergroup.PeerGroupID,byte[])}.
     *
     * @param parent Parent PeerGroupID
     * @param seed  the seed
     */
    public PeerGroupID(PeerGroupID parent, byte[] seed) {
        this(seed);
        UUID parentUUID = parent.getUUID();
        id.longIntoBytes(PeerGroupID.parentgroupIdOffset, parentUUID.getMostSignificantBits());
        id.longIntoBytes(PeerGroupID.parentgroupIdOffset + 8, parentUUID.getLeastSignificantBits());
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
    public net.jxta.peergroup.PeerGroupID getParentPeerGroupID() {
        UUID parentUUID = new UUID(id.bytesIntoLong(PeerGroupID.parentgroupIdOffset), id.bytesIntoLong(PeerGroupID.parentgroupIdOffset + 8));
        if ((0 == parentUUID.getMostSignificantBits()) && (0 == parentUUID.getLeastSignificantBits())) {
            return null;
        }
        PeerGroupID groupID = new PeerGroupID(parentUUID);
        return (net.jxta.peergroup.PeerGroupID) IDFormat.translateToWellKnown(groupID);
    }

    /**
     * Returns the UUID associated with this PeerGroupID.
     *
     * @return The UUID associated with this PeerGroupID.
     */
    @Override
    public UUID getUUID() {
        return super.getUUID();
    }
}
