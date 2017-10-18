package ru.adv.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Buffer of bytes.
 */
public class ByteBuffer {

    /**
	 * The value is used for character storage.
	 */
    private byte _value[];

    /**
	 * The count is the number of characters in the buffer.
	 */
    private int _count = 0;

    private int _hash = 0;

    /**
	 * Constructs object.
	 */
    public ByteBuffer() {
        this(16);
    }

    /**
	 * Constructs object.
	 * @param length initial capacity of buffer.
	 */
    public ByteBuffer(int length) {
        _value = new byte[length];
    }

    /**
	 * Constructs object.
	 */
    public ByteBuffer(byte[] array, boolean copy) {
        _count = array.length;
        if (copy) {
            _value = new byte[_count];
            System.arraycopy(array, 0, _value, 0, _count);
        } else {
            _value = array;
        }
    }

    public ByteBuffer(ByteBuffer buffer, boolean copy) {
        _count = buffer.length();
        if (copy) {
            _value = new byte[_count];
            System.arraycopy(buffer._value, 0, _value, 0, _count);
        } else {
            _value = buffer._value;
        }
    }

    /**
	 * Returns actual length of buffer's data.
	 */
    public int length() {
        return _count;
    }

    /**
	 * Return capacity of buffer;
	 */
    public int capacity() {
        return _value.length;
    }

    /**
	 * Grows capacity if given capacity more than buffer's one.
	 */
    public synchronized void ensureCapacity(int minimumCapacity) {
        if (minimumCapacity > _value.length) {
            expandCapacity(minimumCapacity);
        }
    }

    private void expandCapacity(int minimumCapacity) {
        int newCapacity = (_value.length + 1) * 2;
        if (newCapacity < 0) {
            newCapacity = Integer.MAX_VALUE;
        } else {
            if (minimumCapacity > newCapacity) {
                newCapacity = minimumCapacity;
            }
        }
        byte newValue[] = new byte[newCapacity];
        System.arraycopy(_value, 0, newValue, 0, _count);
        _value = newValue;
    }

    /**
	 * Sets length of buffer's data.
	 */
    public synchronized void setLength(int newLength) {
        if (newLength < 0) {
            throw new ArrayIndexOutOfBoundsException(newLength);
        }
        if (newLength > _value.length) {
            expandCapacity(newLength);
        }
        if (_count != newLength) {
            _hash = 0;
        }
        if (_count < newLength) {
            for (; _count < newLength; _count++) {
                _value[_count] = 0;
            }
        } else {
            _count = newLength;
        }
    }

    /**
	 * Returns byte by given index.
	 */
    public synchronized byte byteAt(int index) {
        if ((index < 0) || (index >= _count)) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return _value[index];
    }

    /**
	 * Sets byte at position by given index.
	 */
    public synchronized void setByteAt(int index, byte b) {
        if ((index < 0) || (index >= _count)) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        _value[index] = b;
        _hash = 0;
    }

    /**
	 * Appends data to end of buffer.
	 */
    public synchronized ByteBuffer append(byte str[]) {
        int len = str.length;
        int newcount = _count + len;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(str, 0, _value, _count, len);
        _count = newcount;
        _hash = 0;
        return this;
    }

    /**
	 * Appends data to end of buffer.
	 */
    public synchronized ByteBuffer append(ByteBuffer buf) {
        int len = buf._count;
        int newcount = _count + len;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(buf._value, 0, _value, _count, len);
        _count = newcount;
        _hash = 0;
        return this;
    }

    /**
	 * Appends data to end of buffer.
	 */
    public synchronized ByteBuffer append(byte str[], int offset, int len) {
        int newcount = _count + len;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(str, offset, _value, _count, len);
        _count = newcount;
        _hash = 0;
        return this;
    }

    /**
	 * Appends data at end of buffer.
	 */
    public synchronized ByteBuffer append(byte b) {
        int newcount = _count + 1;
        if (newcount > _value.length) expandCapacity(newcount);
        _value[_count++] = b;
        _hash = 0;
        return this;
    }

    public synchronized ByteBuffer append(short v) {
        int newcount = _count + 2;
        if (newcount > _value.length) expandCapacity(newcount);
        _value[_count] = (byte) ((v >>> 8) & 0xFF);
        _value[_count + 1] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
        return this;
    }

    public synchronized ByteBuffer append(char v) {
        int newcount = _count + 2;
        if (newcount > _value.length) expandCapacity(newcount);
        _value[_count] = (byte) ((v >>> 8) & 0xFF);
        _value[_count + 1] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
        return this;
    }

    public synchronized ByteBuffer append(String s) {
        int newcount = _count + s.length();
        if (newcount > _value.length) expandCapacity(newcount);
        for (int i = 0; i < s.length(); ++i) {
            append(s.charAt(i));
        }
        return this;
    }

    public synchronized ByteBuffer append(int v) {
        int newcount = _count + 4;
        if (newcount > _value.length) expandCapacity(newcount);
        _value[_count] = (byte) ((v >>> 24) & 0xFF);
        _value[_count + 1] = (byte) ((v >>> 16) & 0xFF);
        _value[_count + 2] = (byte) ((v >>> 8) & 0xFF);
        _value[_count + 3] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
        return this;
    }

    public synchronized ByteBuffer append(long v) {
        int newcount = _count + 8;
        if (newcount > _value.length) expandCapacity(newcount);
        _value[_count] = (byte) ((int) (v >>> 56) & 0xFF);
        _value[_count + 1] = (byte) ((int) (v >>> 48) & 0xFF);
        _value[_count + 2] = (byte) ((int) (v >>> 40) & 0xFF);
        _value[_count + 3] = (byte) ((int) (v >>> 32) & 0xFF);
        _value[_count + 4] = (byte) ((int) (v >>> 24) & 0xFF);
        _value[_count + 5] = (byte) ((int) (v >>> 16) & 0xFF);
        _value[_count + 6] = (byte) ((int) (v >>> 8) & 0xFF);
        _value[_count + 7] = (byte) ((int) (v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
        return this;
    }

    /**
	 * Removes data from buffer.
	 */
    public synchronized void delete(int start, int end) {
        if (start < 0) throw new ArrayIndexOutOfBoundsException(start);
        if (end > _count) end = _count;
        if (start > end) throw new ArrayIndexOutOfBoundsException();
        int len = end - start;
        if (len > 0) {
            System.arraycopy(_value, start + len, _value, start, _count - end);
            _count -= len;
        }
        _hash = 0;
    }

    /**
	 * Removes byte from buffer by given index.
	 */
    public synchronized void deleteByteAt(int index) {
        if ((index < 0) || (index >= _count)) throw new ArrayIndexOutOfBoundsException();
        System.arraycopy(_value, index + 1, _value, index, _count - index - 1);
        _count--;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int index, byte str[], int offset, int len) {
        if ((index < 0) || (index > _count)) throw new ArrayIndexOutOfBoundsException();
        if ((offset < 0) || (offset + len < 0) || (offset + len > str.length)) throw new ArrayIndexOutOfBoundsException(offset);
        if (len < 0) throw new ArrayIndexOutOfBoundsException(len);
        int newCount = _count + len;
        if (newCount > _value.length) expandCapacity(newCount);
        System.arraycopy(_value, index, _value, index + len, _count - index);
        System.arraycopy(str, offset, _value, index, len);
        _count = newCount;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int offset, byte str[]) {
        if ((offset < 0) || (offset > _count)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        int len = str.length;
        int newcount = _count + len;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(_value, offset, _value, offset + len, _count - offset);
        System.arraycopy(str, 0, _value, offset, len);
        _count = newcount;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int offset, byte c) {
        int newcount = _count + 1;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(_value, offset, _value, offset + 1, _count - offset);
        _value[offset] = c;
        _count = newcount;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int offset, short v) {
        int newcount = _count + 2;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(_value, offset, _value, offset + 3, _count - offset);
        _value[offset] = (byte) ((v >>> 8) & 0xFF);
        _value[offset + 1] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int offset, int v) {
        int newcount = _count + 4;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(_value, offset, _value, offset + 5, _count - offset);
        _value[offset] = (byte) ((v >>> 24) & 0xFF);
        _value[offset + 1] = (byte) ((v >>> 16) & 0xFF);
        _value[offset + 2] = (byte) ((v >>> 8) & 0xFF);
        _value[offset + 3] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
    }

    /**
	 * Inserts data into buffer.
	 */
    public synchronized void insert(int offset, long v) {
        int newcount = _count + 8;
        if (newcount > _value.length) expandCapacity(newcount);
        System.arraycopy(_value, offset, _value, offset + 9, _count - offset);
        _value[offset] = (byte) ((v >>> 56) & 0xFF);
        _value[offset + 1] = (byte) ((v >>> 48) & 0xFF);
        _value[offset + 2] = (byte) ((v >>> 40) & 0xFF);
        _value[offset + 3] = (byte) ((v >>> 32) & 0xFF);
        _value[offset + 4] = (byte) ((v >>> 24) & 0xFF);
        _value[offset + 5] = (byte) ((v >>> 16) & 0xFF);
        _value[offset + 6] = (byte) ((v >>> 8) & 0xFF);
        _value[offset + 7] = (byte) ((v >>> 0) & 0xFF);
        _count = newcount;
        _hash = 0;
    }

    /**
	 * Returns copy of buffer's data.
	 */
    public synchronized void getBytes(int srcBegin, int srcEnd, byte dst[], int dstBegin) {
        if (srcBegin < 0) {
            throw new ArrayIndexOutOfBoundsException(srcBegin);
        }
        if ((srcEnd < 0) || (srcEnd > _count)) {
            throw new ArrayIndexOutOfBoundsException(srcEnd);
        }
        if (srcBegin > srcEnd) {
            throw new ArrayIndexOutOfBoundsException("srcBegin > srcEnd");
        }
        System.arraycopy(_value, srcBegin, dst, dstBegin, srcEnd - srcBegin);
    }

    /**
	 * Returns copy of buffer's data.
	 */
    public synchronized byte[] getBytes() {
        byte[] dst = new byte[_count];
        System.arraycopy(_value, 0, dst, 0, _count);
        return dst;
    }

    /**
	 * Converts buffer's data to <code>String</code>.
	 */
    public String toString() {
        return new String(getBytes());
    }

    /**
	 * Converts buffer's data to <code>String</code>.
	 */
    public String toString(String encoding) throws UnsupportedEncodingException {
        return new String(getBytes(), encoding);
    }

    /**
	 * Coverts buffer's data to sequence of numeric string of
	 * specified radix.
	 */
    public String toString(int radix) {
        return toString(radix, null);
    }

    /**
     * like {@link #toString(int)}
     * @param radix
     * @param delimeter разделитель между представлением byte
     * @return
     */
    public String toString(int radix, String delimeter) {
        StringBuffer sb = new StringBuffer(_count * 3);
        for (int i = 0; i < _count; i++) {
            int b = _value[i] & 0xFF;
            if (b < radix && delimeter == null) {
                sb.append('0');
            }
            sb.append(Integer.toString(b, radix));
            if (delimeter != null && (i + 1) < _count) {
                sb.append(delimeter);
            }
        }
        return sb.toString();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ByteBuffer)) {
            return false;
        }
        final ByteBuffer byteBuffer = (ByteBuffer) o;
        if (_count != byteBuffer._count) {
            return false;
        }
        for (int i = 0; i < _count; i++) {
            if (_value[i] != byteBuffer._value[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int h = _hash;
        if (h == 0) {
            for (int i = 0; i < _count; i++) {
                h = 31 * h + _value[i];
            }
            _hash = h;
        }
        return h;
    }

    public ByteBuffer getDigest() {
        return new ByteBuffer(4).append(hashCode());
    }

    public ByteBuffer getMD5Digest() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return new ByteBuffer(md.digest(getBytes()), false);
        } catch (NoSuchAlgorithmException e) {
            throw new ADVRuntimeException(e);
        }
    }
}
