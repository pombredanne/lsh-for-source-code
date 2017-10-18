package com.uprizer.free.bloom;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A simple Bloom Filter (see http://en.wikipedia.org/wiki/Bloom_filter) that
 * uses java.util.Random as a primitive hash function, and which implements
 * Java's Set interface for convenience.
 * 
 * Only the add(), addAll(), contains(), and containsAll() methods are
 * implemented. Calling any other method will yield an
 * UnsupportedOperationException.
 * 
 * This code may be used, modified, and redistributed provided that the author
 * tag below remains intact.
 * 
 * @author Ian Clarke <ian@uprizer.com>
 * 
 * @param <E>
 *            The type of object the BloomFilter should contain
 */
public class SimpleBloomFilter<E> implements Set<E>, Serializable {

    private static final long serialVersionUID = 3527833637516712215L;

    public int bitArraySize, expectedElements;

    public BitSet bitSet;

    protected int k;

    transient MessageDigest md5;

    /**
	 * Construct a SimpleBloomFilter. You must specify the number of bits in the
	 * Bloom Filter, and also you should specify the number of items you expect
	 * to add. The latter is used to choose some optimal internal values to
	 * minimize the false-positive rate (which can be estimated with
	 * expectedFalsePositiveRate()).
	 * 
	 * @param bitArraySize
	 *            The number of bits in the bit array (often called 'm' in the
	 *            context of bloom filters).
	 * @param expectedElements
	 *            The typical number of items you expect to be added to the
	 *            SimpleBloomFilter (often called 'n').
	 */
    public SimpleBloomFilter(final int bitArraySize, int expectedElements) {
        this.bitArraySize = bitArraySize;
        expectedElements = Math.max(1, expectedElements);
        this.expectedElements = expectedElements;
        this.k = (int) Math.ceil((bitArraySize / this.expectedElements) * Math.log(2.0));
        bitSet = new BitSet(bitArraySize);
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            md5 = null;
        }
    }

    public boolean add(final E o) {
        for (int x = 0; x < k; x++) {
            final int bit = getBit(o, x);
            bitSet.set(bit, true);
        }
        return false;
    }

    /**
	 * @return This method will always return false
	 */
    public boolean addAll(final Collection<? extends E> c) {
        for (final E o : c) {
            add(o);
        }
        return false;
    }

    /**
	 * Clear the Bloom Filter
	 */
    public void clear() {
        bitSet.clear();
    }

    /**
	 * @return False indicates that o was definitely not added to this Bloom
	 *         Filter, true indicates that it probably was. The probability can
	 *         be estimated using the expectedFalsePositiveProbability() method.
	 */
    public boolean contains(final Object o) {
        for (int x = 0; x < k; x++) {
            if (!bitSet.get(getBit(o, x))) return false;
        }
        return true;
    }

    public boolean containsAll(final Collection<?> c) {
        for (final Object o : c) {
            if (!contains(o)) return false;
        }
        return true;
    }

    /**
	 * Calculates the approximate probability of the contains() method returning
	 * true for an object that had not previously been inserted into the bloom
	 * filter. This is known as the "false positive probability".
	 * 
	 * @return The estimated false positive rate
	 */
    public double expectedFalsePositiveProbability() {
        return Math.pow((1 - Math.exp(-k * (double) expectedElements / bitArraySize)), k);
    }

    public long hashCode(final String str, final byte add) {
        return hashCode(str.getBytes(), add);
    }

    /**
	 * Not implemented
	 */
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public boolean remove(final Object o) {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public boolean removeAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public boolean retainAll(final Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    /**
	 * Not implemented
	 */
    public <T> T[] toArray(final T[] a) {
        throw new UnsupportedOperationException();
    }

    private int getBit(final Object o, final int x) {
        return (int) (Math.abs(hashCode(o.toString(), (byte) x)) % bitArraySize);
    }

    private synchronized long hashCode(final byte[] bytes, final byte additional) {
        md5.reset();
        md5.update(additional);
        final byte[] digest = md5.digest(bytes);
        final ByteBuffer bb = ByteBuffer.wrap(digest);
        return bb.getLong();
    }

    private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            md5 = null;
        }
    }
}
