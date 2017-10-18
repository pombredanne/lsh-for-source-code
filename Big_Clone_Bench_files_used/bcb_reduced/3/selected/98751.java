package com.limegroup.gnutella.tigertree;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.limegroup.gnutella.security.MerkleTree;

public class HashTreeUtils {

    private static final Log LOG = LogFactory.getLog(HashTreeUtils.class);

    public static final long KB = 1024;

    public static final long MB = 1024 * KB;

    public static final int BLOCK_SIZE = 1024;

    public static final byte INTERNAL_HASH_PREFIX = 0x01;

    public static List<List<byte[]>> createAllParentNodes(List<byte[]> nodes, MessageDigest messageDigest) {
        List<List<byte[]>> allNodes = new ArrayList<List<byte[]>>();
        allNodes.add(Collections.unmodifiableList(nodes));
        while (nodes.size() > 1) {
            nodes = HashTreeUtils.createParentGeneration(nodes, messageDigest);
            allNodes.add(0, nodes);
        }
        return allNodes;
    }

    public static List<byte[]> createParentGeneration(List<byte[]> nodes, MessageDigest md) {
        md.reset();
        int size = nodes.size();
        size = size % 2 == 0 ? size / 2 : (size + 1) / 2;
        List<byte[]> ret = new ArrayList<byte[]>(size);
        Iterator<byte[]> iter = nodes.iterator();
        while (iter.hasNext()) {
            byte[] left = iter.next();
            if (iter.hasNext()) {
                byte[] right = iter.next();
                md.reset();
                md.update(HashTreeUtils.INTERNAL_HASH_PREFIX);
                md.update(left, 0, left.length);
                md.update(right, 0, right.length);
                byte[] result = md.digest();
                ret.add(result);
            } else {
                ret.add(left);
            }
        }
        return ret;
    }

    public static List<byte[]> createTreeNodes(int nodeSize, long fileSize, InputStream is, MessageDigest messageDigest) throws IOException {
        List<byte[]> ret = new ArrayList<byte[]>((int) Math.ceil((double) fileSize / nodeSize));
        MessageDigest tt = new MerkleTree(messageDigest);
        byte[] block = new byte[HashTreeUtils.BLOCK_SIZE * 128];
        long offset = 0;
        int read = 0;
        while (offset < fileSize) {
            int nodeOffset = 0;
            long time = System.currentTimeMillis();
            tt.reset();
            while (nodeOffset < nodeSize && (read = is.read(block)) != -1) {
                tt.update(block, 0, read);
                nodeOffset += read;
                offset += read;
                try {
                    long sleep = (System.currentTimeMillis() - time) * 2;
                    if (sleep > 0) Thread.sleep(sleep);
                } catch (InterruptedException ie) {
                    throw new IOException("interrupted during hashing operation");
                }
                time = System.currentTimeMillis();
            }
            ret.add(tt.digest());
            if (offset == fileSize) {
                if (read != -1 && is.read() != -1) {
                    LOG.warn("More data than fileSize!");
                    throw new IOException("unknown file size.");
                }
            } else if (read == -1 && offset != fileSize) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("couldn't hash whole file. " + "read: " + read + ", offset: " + offset + ", fileSize: " + fileSize);
                }
                throw new IOException("couldn't hash whole file.");
            }
        }
        return ret;
    }

    /**
     * Calculates which depth we want to use for the HashTree. For small files
     * we can save a lot of memory by not creating such a large HashTree
     * 
     * @param size
     *            the fileSize
     * @return int the ideal generation depth for the fileSize
     */
    public static int calculateDepth(long size) {
        if (size < 256 * HashTreeUtils.KB) return 0; else if (size < 512 * HashTreeUtils.KB) return 1; else if (size < HashTreeUtils.MB) return 2; else if (size < 2 * HashTreeUtils.MB) return 3; else if (size < 4 * HashTreeUtils.MB) return 4; else if (size < 8 * HashTreeUtils.MB) return 5; else if (size < 16 * HashTreeUtils.MB) return 6; else if (size < 32 * HashTreeUtils.MB) return 7; else if (size < 64 * HashTreeUtils.MB) return 8; else if (size < 256 * HashTreeUtils.MB) return 9; else if (size < 1024 * HashTreeUtils.MB) return 10; else if (size < 4096 * HashTreeUtils.MB) return 11; else if (size < 64 * 1024 * HashTreeUtils.MB) return 12; else return 13;
    }

    /**
     *  Calculates a the node size based on the file size and the target depth.
     *  
     *   A tree of depth n has 2^(n-1) leaf nodes, so ideally the file will be
     *   split in that many chunks.  However, since chunks have to be powers of 2,
     *   we make the size of each chunk the closest power of 2 that is bigger than
     *   the ideal size.
     *   
     *   This ensures the resulting tree will have between 2^(n-2) and 2^(n-1) nodes.
     */
    public static int calculateNodeSize(long fileSize, int depth) {
        long maxNodes = 1 << depth;
        long idealNodeSize = fileSize / maxNodes;
        if (fileSize % maxNodes != 0) idealNodeSize++;
        int n = MerkleTree.log2Ceil(idealNodeSize);
        int nodeSize = 1 << n;
        if (LOG.isDebugEnabled()) {
            LOG.debug("fileSize " + fileSize);
            LOG.debug("depth " + depth);
            LOG.debug("nodeSize " + nodeSize);
        }
        assert nodeSize * maxNodes >= fileSize : "nodeSize: " + nodeSize + ", fileSize: " + fileSize + ", maxNode: " + maxNodes;
        assert nodeSize * maxNodes <= fileSize * 2 : "nodeSize: " + nodeSize + ", fileSize: " + fileSize + ", maxNode: " + maxNodes;
        return nodeSize;
    }
}
