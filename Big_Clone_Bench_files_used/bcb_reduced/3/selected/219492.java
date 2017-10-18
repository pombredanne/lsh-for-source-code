package NET.sourceforge.BplusJ.BplusJ;

public class hBplusTreeBytes extends xBplusTreeBytes {

    public hBplusTreeBytes(BplusTreeBytes tree, int hashLength) throws Exception {
        super(tree, hashLength);
        this.tree.NoCulture();
    }

    public static xBplusTreeBytes Initialize(String treefileName, String blockfileName, int PrefixLength, int CultureId, int nodesize, int buffersize) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefileName, blockfileName, PrefixLength, CultureId, nodesize, buffersize), PrefixLength);
    }

    public static xBplusTreeBytes Initialize(String treefileName, String blockfileName, int PrefixLength, int CultureId) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefileName, blockfileName, PrefixLength, CultureId), PrefixLength);
    }

    public static xBplusTreeBytes Initialize(String treefileName, String blockfileName, int PrefixLength) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefileName, blockfileName, PrefixLength), PrefixLength);
    }

    public static xBplusTreeBytes Initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength, int CultureId, int nodesize, int buffersize) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefile, blockfile, PrefixLength, CultureId, nodesize, buffersize), PrefixLength);
    }

    public static xBplusTreeBytes Initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength, int CultureId) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefile, blockfile, PrefixLength, CultureId), PrefixLength);
    }

    public static xBplusTreeBytes Initialize(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile, int PrefixLength) throws Exception {
        return new hBplusTreeBytes(BplusTreeBytes.Initialize(treefile, blockfile, PrefixLength), PrefixLength);
    }

    public static xBplusTreeBytes ReOpen(java.io.RandomAccessFile treefile, java.io.RandomAccessFile blockfile) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.ReOpen(treefile, blockfile);
        int prefixLength = tree.MaxKeyLength();
        return new hBplusTreeBytes(tree, prefixLength);
    }

    public static xBplusTreeBytes ReOpen(String treefileName, String blockfileName) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.ReOpen(treefileName, blockfileName);
        int prefixLength = tree.MaxKeyLength();
        return new hBplusTreeBytes(tree, prefixLength);
    }

    public static xBplusTreeBytes ReadOnly(String treefileName, String blockfileName) throws Exception {
        BplusTreeBytes tree = BplusTreeBytes.ReadOnly(treefileName, blockfileName);
        int prefixLength = tree.MaxKeyLength();
        return new hBplusTreeBytes(tree, prefixLength);
    }

    public String PrefixForByteCount(String s, int maxbytecount) throws Exception {
        byte[] inputbytes = BplusTree.StringToBytes(s);
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
        String result = BplusTree.BytesToString(resultbytes);
        return result;
    }
}
