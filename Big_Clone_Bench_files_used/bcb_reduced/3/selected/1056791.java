package phex.thex;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Vector;
import com.bitzi.util.Base32;
import com.bitzi.util.Tiger;

/**
 * Implementation of THEX tree hash algorithm, with Tiger
 * as the internal algorithm (using the approach as revised
 * in December 2002, to add unique prefixes to leaf and node
 * operations)
 *
 * For simplicity, calculates one entire generation before
 * starting on the next. A more space-efficient approach
 * would use a stack, and calculate each node as soon as
 * its children ara available.
 */
public class TigerTree extends MessageDigest {

    private static final int BLOCKSIZE = 1024;

    private static final double fblockSize = 1024.0;

    private static final int HASHSIZE = 24;

    /** 1024 byte buffer */
    private final byte[] buffer;

    int count = 0;

    /** Buffer offset */
    private int bufferOffset;

    /** Offset in serialization*/
    private int serializationOffset = 0;

    private int serializationLeavesOffset = 0;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    /** Interim tree node hash values */
    private Vector nodes;

    /**
     * The file size ( file.length() )
     */
    private long fileSize;

    /**
     * A array containig the serialization of the TigerTree
     */
    private byte[] serialization = null;

    private Thex t = null;

    private int levelsLeft = 0;

    private boolean check = false;

    /**
     * Constructor
     */
    public TigerTree(long filesize, int serializSize, int levelsLeft) throws NoSuchAlgorithmException {
        super("TigerTree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        this.fileSize = filesize;
        this.levelsLeft = levelsLeft;
        int numberLeafs = (int) (Math.ceil(fileSize / fblockSize));
        serialization = new byte[serializSize];
        t = new Thex();
        {
            tiger = new Tiger();
        }
        nodes = new Vector();
    }

    public TigerTree(int levelsLeft, int digestSize, int serializSize, Vector nodes, boolean check) throws NoSuchAlgorithmException {
        super("TigerTree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        this.levelsLeft = levelsLeft;
        this.nodes = nodes;
        this.check = true;
        serialization = new byte[serializSize];
        t = new Thex();
        {
            tiger = new Tiger();
        }
    }

    public TigerTree() throws NoSuchAlgorithmException {
        super("TigerTree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        {
            tiger = new Tiger();
        }
        nodes = new Vector();
    }

    public byte[] calculate(byte[] buf) {
        tiger.reset();
        tiger.update((byte) 0);
        tiger.update(buf, 0, 0);
        Object dig = (Object) tiger.digest();
        return (byte[]) dig;
    }

    protected int engineGetDigestLength() {
        return HASHSIZE;
    }

    protected byte[] getSerialization() {
        return serialization;
    }

    protected void engineUpdate(byte in) {
        byteCount += 1;
        buffer[bufferOffset++] = in;
        if (bufferOffset == BLOCKSIZE) {
            blockUpdate();
            bufferOffset = 0;
        }
    }

    protected void engineUpdate(byte[] in, int offset, int length) {
        byteCount += length;
        int remaining;
        while (length >= (remaining = BLOCKSIZE - bufferOffset)) {
            System.arraycopy(in, offset, buffer, bufferOffset, remaining);
            bufferOffset += remaining;
            blockUpdate();
            length -= remaining;
            offset += remaining;
            bufferOffset = 0;
        }
        System.arraycopy(in, offset, buffer, bufferOffset, length);
        bufferOffset += length;
    }

    protected byte[] engineDigest() {
        byte[] hash = new byte[HASHSIZE];
        try {
            engineDigest(hash, 0, HASHSIZE);
        } catch (DigestException e) {
            return null;
        }
        return hash;
    }

    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        if (len < HASHSIZE) throw new DigestException();
        if (!check) blockUpdate();
        int rows = 0;
        while (nodes.size() > 1) {
            rows++;
            if (check && rows > levelsLeft) {
                t.setSerializationByte(serialization);
                return HASHSIZE;
            }
            Vector newNodes = new Vector();
            Enumeration iter = nodes.elements();
            while (iter.hasMoreElements()) {
                byte[] left = (byte[]) iter.nextElement();
                if (iter.hasMoreElements()) {
                    byte[] right = (byte[]) iter.nextElement();
                    tiger.reset();
                    tiger.update((byte) 1);
                    tiger.update(left);
                    tiger.update(right);
                    Object dig = (Object) tiger.digest();
                    newNodes.addElement(dig);
                    if ((levelsLeft != -1) && (rows == levelsLeft)) {
                        byte[] digs = (byte[]) dig;
                        System.arraycopy(digs, 0, serialization, serializationOffset, digs.length);
                        serializationOffset += digs.length;
                    }
                } else {
                    Object obj = (Object) left;
                    newNodes.addElement(obj);
                    if ((levelsLeft != -1) && (rows == levelsLeft)) {
                        byte[] digs = (byte[]) obj;
                        System.arraycopy(digs, 0, serialization, serializationOffset, digs.length);
                        serializationOffset += digs.length;
                    }
                }
            }
            nodes = newNodes;
        }
        byte[] root = (byte[]) nodes.elementAt(0);
        System.arraycopy(root, 0, buf, offset, HASHSIZE);
        engineReset();
        if (t != null) {
            t.setHashSize(HASHSIZE);
            t.setSerialization(Base32.encode(serialization));
            t.setRoot(Base32.encode(root));
        }
        return HASHSIZE;
    }

    public Thex getThex() {
        return t;
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        nodes = new Vector();
        tiger.reset();
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Update the internal state with a single block of size 1024
     * (or less, in final block) from the internal buffer.
     */
    protected void blockUpdate() {
        tiger.reset();
        tiger.update((byte) 0);
        tiger.update(buffer, 0, bufferOffset);
        if ((bufferOffset == 0) && (nodes.size() > 0)) return;
        Object dig = (Object) tiger.digest();
        nodes.addElement(dig);
        count++;
        if (levelsLeft == -1) {
            byte[] digst = (byte[]) dig;
            System.arraycopy(digst, 0, serialization, serializationLeavesOffset, digst.length);
            serializationLeavesOffset += digst.length;
        }
    }
}
