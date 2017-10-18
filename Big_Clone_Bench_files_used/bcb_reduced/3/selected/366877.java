package clonefinder;

import java.security.*;
import net.jxta.id.*;

/**
 *
 * @author  Daniel Brookshier
 */
public class MD5ID {

    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(MD5ID.class.getName());

    public static final String functionSeperator = "~";

    /**
     * Create a PipeID based on the BinaryID type with a digest of the clearTextID and function.
     *
     * @param peerGroupID Parent peer group ID.
     * @param clearTextID String used as the significant part of the address
     * @param function String used to diferentiate different clearTextID addresses (can be null).
     * @return PipeBinaryID with the digest hash of the string: clearTextID+"~"+function.
     */
    public static final net.jxta.pipe.PipeID createPipeID(net.jxta.peergroup.PeerGroupID peerGroupID, String clearTextID, String function) {
        LOG.info("Creatig pipe ID = peerGroupID:" + peerGroupID + ", clearText:'" + clearTextID + "' , function:'" + function + "'");
        byte[] digest = generateHash(clearTextID, function);
        return (net.jxta.pipe.PipeID) net.jxta.id.IDFactory.newPipeID(peerGroupID, digest);
    }

    /**
     * Create a PeerGroupID based on the BinaryID type with a digest of the clearTextID and function.
     *
     * @param peerGroupID Parent peer group ID.
     * @param clearTextID String used as the significant part of the address
     * @param function String used to diferentiate different clearTextID addresses (can be null).
     * @return PeerGroupBinaryID with the digest hash of the string: clearTextID+"~"+function.
     */
    public static final net.jxta.peergroup.PeerGroupID createPeerGroupID(net.jxta.peergroup.PeerGroupID parentPeerGroupID, String clearTextID, String function) {
        LOG.info("Creating peer group ID = peerGroupID:" + parentPeerGroupID + ", clearText:'" + clearTextID + "' , function:'" + function + "'");
        byte[] digest = generateHash(clearTextID, function);
        System.out.println("parentPeerGroupID:" + parentPeerGroupID.getClass().getName());
        net.jxta.peergroup.PeerGroupID peerGroupID = IDFactory.newPeerGroupID(parentPeerGroupID, digest);
        return peerGroupID;
    }

    public static final net.jxta.peergroup.PeerGroupID createInftrastructurePeerGroupID(String clearTextID, String function) {
        LOG.info("Creating peer group ID =  clearText:'" + clearTextID + "' , function:'" + function + "'");
        byte[] digest = generateHash(clearTextID, function);
        net.jxta.peergroup.PeerGroupID peerGroupID = IDFactory.newPeerGroupID(digest);
        return peerGroupID;
    }

    /**
     * Generates an SHA-1 digest hash of the string: clearTextID+"-"+function or: clearTextID if function was blank.<p>
     *
     * Note that the SHA-1 used only creates a 20 byte hash.<p>
     *
     * @param clearTextID A string that is to be hashed. This can be any string used for hashing or hiding data.
     * @param function A function related to the clearTextID string. This is used to create a hash associated with clearTextID so that it is a uique code.
     *
     * @return array of bytes containing the hash of the string: clearTextID+"-"+function or clearTextID if function was blank. Can return null if SHA-1 does not exist on platform.
     */
    public static final byte[] generateHash(String clearTextID, String function) {
        String id;
        if (function == null) {
            id = clearTextID;
        } else {
            id = clearTextID + functionSeperator + function;
        }
        byte[] buffer = id.getBytes();
        MessageDigest algorithm = null;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            LOG.error("Cannot load selected Digest Hash implementation", e);
            return null;
        }
        algorithm.reset();
        algorithm.update(buffer);
        try {
            byte[] digest1 = algorithm.digest();
            return digest1;
        } catch (Exception de) {
            LOG.error("Failed to creat a digest.", de);
            return null;
        }
    }
}
