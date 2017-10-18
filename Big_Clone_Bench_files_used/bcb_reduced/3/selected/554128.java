package org.bplusj.hash;

import org.bplusj.BplusTree;
import org.bplusj.bytetree.BplusTreeBytes;
import org.bplusj.xbplus.XBplusTreeBytes;

/**
 * Btree mapping unlimited length key Strings to fixed length hash values
 * 
 * @author Aaron Watters
 */
public class HBplusTreeBytes extends XBplusTreeBytes {

    /**
     * @param tree
     * @param hashLength
     * @throws Exception
     */
    public HBplusTreeBytes(BplusTreeBytes tree, int hashLength) throws Exception {
        super(tree, hashLength);
        this.tree.NoCulture();
    }

    public static XBplusTreeBytes initialize(String treefileName, String blockfileName, int PrefixLength, int CultureId, int nodesize, int buffersize) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefileName, blockfileName, PrefixLength, CultureId, nodesize, buffersize), PrefixLength);
    }

    public static XBplusTreeBytes initialize(String treefileName, String blockfileName, int PrefixLength, int CultureId) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefileName, blockfileName, PrefixLength, CultureId), PrefixLength);
    }

    public static XBplusTreeBytes initialize(String treefileName, String blockfileName, int PrefixLength) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefileName, blockfileName, PrefixLength), PrefixLength);
    }

    public static XBplusTreeBytes initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength, int CultureId, int nodesize, int buffersize) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefile, blockfile, PrefixLength, CultureId, nodesize, buffersize), PrefixLength);
    }

    public static XBplusTreeBytes initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength, int CultureId) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefile, blockfile, PrefixLength, CultureId), PrefixLength);
    }

    public static XBplusTreeBytes initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength) throws Exception {
        return new HBplusTreeBytes(BplusTreeBytes.initialize(treefile, blockfile, PrefixLength), PrefixLength);
    }

    public static XBplusTreeBytes reOpen(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.reOpen(treefile, blockfile);
        int prefixLength = tree.MaxKeyLength();
        return new HBplusTreeBytes(tree, prefixLength);
    }

    public static XBplusTreeBytes reOpen(String treefileName, String blockfileName) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.reOpen(treefileName, blockfileName);
        int prefixLength = tree.MaxKeyLength();
        return new HBplusTreeBytes(tree, prefixLength);
    }

    public static XBplusTreeBytes readOnly(String treefileName, String blockfileName) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.readOnly(treefileName, blockfileName);
        int prefixLength = tree.MaxKeyLength();
        return new HBplusTreeBytes(tree, prefixLength);
    }

    public String prefixForByteCount(String s, int maxbytecount) throws Exception {
        byte[] inputbytes = BplusTree.stringToBytes(s);
        java.security.MessageDigest D = java.security.MessageDigest.getInstance("MD5");
        byte[] digest = D.digest(inputbytes);
        byte[] resultbytes = new byte[maxbytecount];
        for (int i = 0; i < maxbytecount; i++) {
            int r = digest[i % digest.length];
            if (r < 0) {
                r = -r;
            }
            r = r % 79 + 40;
            resultbytes[i] = (byte) r;
        }
        String result = BplusTree.bytesToString(resultbytes);
        return result;
    }
}
