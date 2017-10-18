package gnu.java.security.jce.hash;

import gnu.java.security.hash.IMessageDigest;
import gnu.java.security.hash.HashFactory;
import java.security.DigestException;
import java.security.MessageDigestSpi;

/**
 * The implementation of a generic {@link java.security.MessageDigest} adapter
 * class to wrap GNU hash instances.
 * <p>
 * This class defines the <i>Service Provider Interface</i> (<b>SPI</b>) for
 * the {@link java.security.MessageDigest} class, which provides the
 * functionality of a message digest algorithm, such as MD5 or SHA. Message
 * digests are secure one-way hash functions that take arbitrary-sized data and
 * output a fixed-length hash value.
 * <p>
 * All the abstract methods in the {@link MessageDigestSpi} class are
 * implemented by this class and all its sub-classes.
 * <p>
 * All the implementations which subclass this object, and which are serviced by
 * the GNU provider implement the {@link Cloneable} interface.
 */
class MessageDigestAdapter extends MessageDigestSpi implements Cloneable {

    /** Our underlying hash instance. */
    private IMessageDigest adaptee;

    /**
   * Trivial protected constructor.
   * 
   * @param mdName the canonical name of the hash algorithm.
   */
    protected MessageDigestAdapter(String mdName) {
        this(HashFactory.getInstance(mdName));
    }

    /**
   * Private constructor for cloning purposes.
   * 
   * @param adaptee a clone of the underlying hash algorithm instance.
   */
    private MessageDigestAdapter(IMessageDigest adaptee) {
        super();
        this.adaptee = adaptee;
    }

    public Object clone() {
        return new MessageDigestAdapter((IMessageDigest) adaptee.clone());
    }

    public int engineGetDigestLength() {
        return adaptee.hashSize();
    }

    public void engineUpdate(byte input) {
        adaptee.update(input);
    }

    public void engineUpdate(byte[] input, int offset, int len) {
        adaptee.update(input, offset, len);
    }

    public byte[] engineDigest() {
        return adaptee.digest();
    }

    public int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        int result = adaptee.hashSize();
        if (len < result) throw new DigestException();
        byte[] md = adaptee.digest();
        System.arraycopy(md, 0, buf, offset, result);
        return result;
    }

    public void engineReset() {
        adaptee.reset();
    }
}
