package net.jxta.impl.id.CBID;

import net.jxta.impl.id.UUID.IDBytes;
import net.jxta.impl.id.UUID.UUID;
import net.jxta.impl.id.UUID.UUIDFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.ProviderException;
import java.util.logging.Logger;

/**
 * An implementation of the {@link net.jxta.codat.CodatID} ID Type.
 */
public class CodatID extends net.jxta.impl.id.UUID.CodatID {

    /**
     * Log4J Logger
     */
    private static final transient Logger LOG = Logger.getLogger(CodatID.class.getName());

    /**
     * Internal constructor
     */
    protected CodatID() {
        super();
    }

    /**
     * Intializes contents from provided bytes.
     *
     * @param id the ID data
     */
    protected CodatID(IDBytes id) {
        super(id);
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID)}.
     *
     * @param groupID the GroupID
     */
    public CodatID(PeerGroupID groupID) {
        super(groupID.getUUID(), UUIDFactory.newUUID());
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,byte[])}.
     *
     * @param groupID the GroupID
     * @param seed the seed
     */
    public CodatID(PeerGroupID groupID, byte[] seed) {
        this();
        UUID groupCBID = groupID.getUUID();
        id.longIntoBytes(CodatID.groupIdOffset, groupCBID.getMostSignificantBits());
        id.longIntoBytes(CodatID.groupIdOffset + 8, groupCBID.getLeastSignificantBits());
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
        UUID peerCBID = UUIDFactory.newUUID(buf16);
        id.longIntoBytes(CodatID.idOffset, peerCBID.getMostSignificantBits());
        id.longIntoBytes(CodatID.idOffset + 8, peerCBID.getLeastSignificantBits());
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,InputStream)}.
     *
     * @param groupID the GroupID
     * @param in the input stream
     * @throws IOException if an io error occurs
     */
    public CodatID(PeerGroupID groupID, InputStream in) throws IOException {
        super(groupID, in);
    }

    /**
     * See {@link net.jxta.id.IDFactory.Instantiator#newCodatID(net.jxta.peergroup.PeerGroupID,InputStream)}.
     *
     * @param groupID the GroupID
     * @param seed the seed
     * @param in the input stream
     * @throws IOException if an io error occurs
     */
    public CodatID(PeerGroupID groupID, byte[] seed, InputStream in) throws IOException {
        this(groupID, seed);
        setHash(in);
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
        UUID groupCBID = new UUID(id.bytesIntoLong(CodatID.groupIdOffset), id.bytesIntoLong(CodatID.groupIdOffset + 8));
        PeerGroupID groupID = new PeerGroupID(groupCBID);
        return IDFormat.translateToWellKnown(groupID);
    }

    /**
     * Returns the UUID associated with this CodatID.
     *
     * @return The UUID associated with this CodatID.
     */
    public UUID getUUID() {
        return new UUID(id.bytesIntoLong(CodatID.idOffset), id.bytesIntoLong(CodatID.idOffset + 8));
    }
}
