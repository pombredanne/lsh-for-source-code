package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Range;
import org.limewire.util.Base32;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.http.HTTPConstants;
import com.limegroup.gnutella.security.MerkleTree;
import com.limegroup.gnutella.security.Tiger;

/**
 * This class stores HashTrees and is capable of verifying a file it is also
 * used for storing them in a file.
 *
 * Be careful when modifying any non transient variables, as this
 * class serialized to disk.
 * 
 * @author Gregorio Roper
 */
class HashTreeImpl implements Serializable, HashTree {

    private static final long serialVersionUID = -5752974896215224469L;

    private static final Log LOG = LogFactory.getLog(HashTreeImpl.class);

    /** An invalid HashTree. */
    public static final HashTreeImpl INVALID = new HashTreeImpl();

    /**
     * The lowest depth list of nodes.
     */
    private final List<byte[]> NODES;

    /**
     * The tigertree root hash.
     */
    private final byte[] ROOT_HASH;

    /**
     * The size of the file this hash identifies.
     */
    private final long FILE_SIZE;

    private final int DEPTH;

    /**
     * The URI for this hash tree.
     */
    private final String THEX_URI;

    /**
     * The size of each node
     */
    private transient int _nodeSize;

    /** Constructs an invalid HashTree. */
    private HashTreeImpl() {
        NODES = null;
        ROOT_HASH = null;
        FILE_SIZE = -1;
        DEPTH = -1;
        THEX_URI = null;
    }

    /**
     * Constructs a new HashTree out of the given nodes, root, sha1
     * filesize and chunk size.
     */
    HashTreeImpl(List<List<byte[]>> allNodes, String sha1, long fileSize, int nodeSize) {
        THEX_URI = HTTPConstants.URI_RES_N2X + sha1;
        NODES = allNodes.get(allNodes.size() - 1);
        FILE_SIZE = fileSize;
        ROOT_HASH = allNodes.get(0).get(0);
        DEPTH = allNodes.size() - 1;
        assert (MerkleTree.log2Ceil(NODES.size()) == DEPTH);
        assert (NODES.size() * (long) nodeSize >= fileSize);
        _nodeSize = nodeSize;
    }

    public boolean isCorrupt(Range in, byte[] data) {
        return isCorrupt(in, data, data.length);
    }

    public boolean isCorrupt(Range in, byte[] data, int length) {
        assert (in.getHigh() <= FILE_SIZE);
        if (in.getLow() % _nodeSize == 0 && in.getHigh() - in.getLow() + 1 <= _nodeSize && (in.getHigh() == in.getLow() + _nodeSize - 1 || in.getHigh() == FILE_SIZE - 1)) {
            MerkleTree digest = new MerkleTree(new Tiger());
            digest.update(data, 0, length);
            byte[] hash = digest.digest();
            byte[] treeHash = NODES.get((int) (in.getLow() / _nodeSize));
            boolean ok = Arrays.equals(treeHash, hash);
            if (LOG.isDebugEnabled()) LOG.debug("interval " + in + " verified " + ok);
            return !ok;
        }
        return true;
    }

    public boolean isCorrupt(Range in, RandomAccessFile raf, byte[] tmp) {
        assert in.getHigh() <= FILE_SIZE : "invalid range " + in + " vs " + FILE_SIZE;
        if (in.getLow() % _nodeSize == 0 && in.getHigh() - in.getLow() + 1 <= _nodeSize && (in.getHigh() == in.getLow() + _nodeSize - 1 || in.getHigh() == FILE_SIZE - 1)) {
            try {
                MerkleTree digest = new MerkleTree(new Tiger());
                long read = in.getLow();
                while (read <= in.getHigh()) {
                    int size = (int) Math.min(tmp.length, in.getHigh() - read + 1);
                    synchronized (raf) {
                        raf.seek(read);
                        raf.readFully(tmp, 0, size);
                    }
                    digest.update(tmp, 0, size);
                    read += size;
                }
                byte[] hash = digest.digest();
                byte[] treeHash = NODES.get((int) (in.getLow() / _nodeSize));
                boolean ok = Arrays.equals(treeHash, hash);
                if (LOG.isDebugEnabled()) LOG.debug("interval " + in + " verified " + ok);
                return !ok;
            } catch (IOException assumeCorrupt) {
                LOG.debug("iox while verifying ", assumeCorrupt);
                return true;
            }
        }
        return true;
    }

    public String httpStringValue() {
        return THEX_URI + ";" + Base32.encode(ROOT_HASH);
    }

    public boolean isGoodDepth() {
        return (DEPTH == HashTreeUtils.calculateDepth(FILE_SIZE));
    }

    public boolean isDepthGoodEnough() {
        return DEPTH >= HashTreeUtils.calculateDepth(FILE_SIZE) - 1;
    }

    public boolean isBetterTree(HashTree other) {
        if (other == null) return true; else if (other.isGoodDepth()) return false; else if (this.isGoodDepth()) return true; else {
            int ideal = HashTreeUtils.calculateDepth(FILE_SIZE);
            int diff1 = Math.abs(this.getDepth() - ideal);
            int diff2 = Math.abs(other.getDepth() - ideal);
            if (diff1 < diff2) return true; else return false;
        }
    }

    public long getFileSize() {
        return FILE_SIZE;
    }

    public String getRootHash() {
        return Base32.encode(ROOT_HASH);
    }

    public byte[] getRootHashBytes() {
        return ROOT_HASH;
    }

    public URN getTreeRootUrn() {
        try {
            return URN.createTTRootFromBytes(ROOT_HASH);
        } catch (IOException notTiger) {
        }
        return null;
    }

    public String getThexURI() {
        return THEX_URI;
    }

    public int getDepth() {
        return DEPTH;
    }

    public List<byte[]> getNodes() {
        return NODES;
    }

    public synchronized int getNodeSize() {
        if (_nodeSize == 0) {
            _nodeSize = HashTreeUtils.calculateNodeSize(FILE_SIZE, DEPTH);
        }
        return _nodeSize;
    }

    public int getNodeCount() {
        double last = NODES.size();
        int count = (int) last;
        for (int i = DEPTH - 1; i >= 0; i--) {
            last = Math.ceil(last / 2);
            count += (int) last;
        }
        return count;
    }
}
