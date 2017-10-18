package com.bitzi.util;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang.SystemUtils;
import phex.common.log.NLogger;

/**
 * Implementation of THEX tree hash algorithm, with Tiger as the internal
 * algorithm (using the approach as revised in December 2002, to add unique
 * prefixes to leaf and node operations)
 * 
 * For simplicity, calculates one entire generation before starting on the next.
 * A more space-efficient approach would use a stack, and calculate each node as
 * soon as its children ara available.
 */
public class TigerTree extends MessageDigest {

    private static final int BLOCKSIZE = 1024;

    private static final int HASHSIZE = 24;

    private static final boolean USE_CRYPTIX = SystemUtils.isJavaVersionAtLeast(140) && SystemUtils.IS_OS_MAC_OSX && SystemUtils.OS_VERSION.startsWith("10.2");

    /**
     * Set up the CryptixCrypto provider if we're on a platform that requires
     * it.
     */
    static {
        if (USE_CRYPTIX) {
            try {
                Class clazz = Class.forName("cryptix.jce.provider.CryptixCrypto");
                Object o = clazz.newInstance();
                Security.addProvider((Provider) o);
            } catch (ClassNotFoundException e) {
                NLogger.error(TigerTree.class, e, e);
            } catch (IllegalAccessException e) {
                NLogger.error(TigerTree.class, e, e);
            } catch (InstantiationException e) {
                NLogger.error(TigerTree.class, e, e);
            } catch (ExceptionInInitializerError e) {
                NLogger.error(TigerTree.class, e, e);
            } catch (SecurityException e) {
                NLogger.error(TigerTree.class, e, e);
            } catch (ClassCastException e) {
                NLogger.error(TigerTree.class, e, e);
            }
        }
    }

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private MessageDigest tiger;

    /** Interim tree node hash values */
    private List nodes;

    /**
     * Constructor
     */
    public TigerTree() {
        super("tigertree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        nodes = new ArrayList();
        if (USE_CRYPTIX) {
            try {
                tiger = MessageDigest.getInstance("Tiger", "CryptixCrypto");
            } catch (NoSuchAlgorithmException nsae) {
                tiger = new Tiger();
            } catch (NoSuchProviderException nspe) {
                tiger = new Tiger();
            }
        } else {
            tiger = new Tiger();
        }
    }

    protected int engineGetDigestLength() {
        return HASHSIZE;
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
        blockUpdate();
        while (nodes.size() > 1) {
            List newNodes = new ArrayList();
            Iterator iter = nodes.iterator();
            while (iter.hasNext()) {
                byte[] left = (byte[]) iter.next();
                if (iter.hasNext()) {
                    byte[] right = (byte[]) iter.next();
                    tiger.reset();
                    tiger.update((byte) 1);
                    tiger.update(left, 0, left.length);
                    tiger.update(right, 0, right.length);
                    newNodes.add(tiger.digest());
                } else {
                    newNodes.add(left);
                }
            }
            nodes = newNodes;
        }
        System.arraycopy(nodes.get(0), 0, buf, offset, HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        nodes = new ArrayList();
        tiger.reset();
    }

    /**
     * Method overrides MessageDigest.clone()
     * 
     * @see java.security.MessageDigest#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
     * Update the internal state with a single block of size 1024 (or less, in
     * final block) from the internal buffer.
     */
    protected void blockUpdate() {
        tiger.reset();
        tiger.update((byte) 0);
        tiger.update(buffer, 0, bufferOffset);
        if ((bufferOffset == 0) && (nodes.size() > 0)) return;
        nodes.add(tiger.digest());
    }
}
