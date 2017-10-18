package peer.net.util;

import java.util.Random;
import net.jxta.id.ID;
import net.jxta.id.IDFactory;
import net.jxta.peer.PeerID;
import net.jxta.peergroup.PeerGroup;
import net.jxta.peergroup.PeerGroupID;
import net.jxta.pipe.PipeID;
import net.jxta.platform.ModuleClassID;
import peer.util.Thumbprint;

/**
 *
 * @author tpasquie
 */
public class IDGenerator {

    private static final String SEED = "18dc25c92e085218e9262de79ef4984b8942de1a";

    private static final String ENCODING = "UTF-8";

    private static final Random rand = new Random(System.currentTimeMillis());

    public static PeerGroupID getInfrastructureGroupID(String infrastructureseed) throws Exception {
        return IDFactory.newPeerGroupID(Thumbprint.digest(infrastructureseed.toLowerCase().getBytes(ENCODING)));
    }

    public static PeerGroupID createSubPeerGroupID(String infrastructureseed, PeerGroup parentGroup, String subgroupName) throws Exception {
        String seed = subgroupName + SEED;
        return IDFactory.newPeerGroupID(getInfrastructureGroupID(seed), Thumbprint.digest(seed.toLowerCase().getBytes(ENCODING)));
    }

    public static ModuleClassID generateServiceID() {
        return IDFactory.newModuleClassID();
    }

    public static PeerGroupID createPeerGroupID(final PeerGroup parent, final String groupName) throws Exception {
        String seed = groupName + SEED;
        return IDFactory.newPeerGroupID(parent.getPeerGroupID(), Thumbprint.digest(seed.toLowerCase().getBytes(ENCODING)));
    }

    public static PipeID createPipeID(PeerGroupID pgID, String data) throws Exception {
        String seed = data + SEED;
        return IDFactory.newPipeID(pgID, Thumbprint.digest(seed.toLowerCase().getBytes(ENCODING)));
    }

    public static PeerID createPeerID(String infrastructureseed, String peerName) throws Exception {
        String seed = rand.nextLong() + peerName + SEED;
        return IDFactory.newPeerID(getInfrastructureGroupID(infrastructureseed), Thumbprint.digest(seed.toLowerCase().getBytes(ENCODING)));
    }

    public static ID createResourceID(PeerGroupID pgID, String name) throws Exception {
        String seed = name + SEED;
        ID id = IDFactory.newContentID(pgID, true, Thumbprint.digest(seed.toLowerCase().getBytes(ENCODING)));
        return id;
    }
}
