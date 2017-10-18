package jp.go.aist.six.oval.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import com.google.code.morphia.annotations.Transient;

/**
 * A container which can contain OvalElement objects.
 *
 * @author  Akihito Nakamura, AIST
 * @version $Id: ElementContainer.java 2263 2012-03-28 09:53:36Z nakamura5akihito@gmail.com $
 * @see <a href="http://oval.mitre.org/language/">OVAL Language</a>
 */
public abstract class ElementContainer<E extends Element> extends Container<E> {

    /**
     * Constructor.
     */
    public ElementContainer() {
    }

    public ElementContainer(final Collection<? extends E> elements) {
        super(elements);
    }

    public ElementContainer(final E[] elements) {
        super(elements);
    }

    /**
     */
    public E find(final String id) {
        if (id == null) {
            throw new IllegalArgumentException();
        }
        for (E e : _getElement()) {
            if (id.equals(e.getOvalID())) {
                return e;
            }
        }
        return null;
    }

    /**
     * The default digest algorithm.
     */
    public static final String DIGEST_ALGORITHM = "MD5";

    @Transient
    private String _digest;

    @Transient
    private transient int _hashOnDigest = 0;

    /**
     */
    public void setDigest(final String digest) {
        _digest = digest;
    }

    public String getDigest() {
        int thisHash = hashCode();
        if (_digest == null || thisHash != _hashOnDigest) {
            _digest = _computeDigest(_getElement());
            _hashOnDigest = thisHash;
        }
        return _digest;
    }

    /**
     */
    protected String _computeDigest(final Collection<E> elements) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        if (elements == null || elements.size() == 0) {
            _updateDigest(digest, "");
        } else {
            ArrayList<E> list = new ArrayList<E>(elements);
            Collections.sort(list, new OvalElementComparator());
            for (Element element : list) {
                _updateDigest(digest, element);
            }
        }
        return _byteArrayToHexString(digest.digest());
    }

    /**
     */
    private static void _updateDigest(final MessageDigest digest, final Element element) {
        _updateDigest(digest, element.ovalGetGlobalRef());
    }

    /**
     */
    private static void _updateDigest(final MessageDigest digest, final String s) {
        final String ss = (s == null ? "" : s);
        digest.update(ss.getBytes());
    }

    /**
     */
    private static final String[] _HEX_DIGITS_ = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /**
     */
    private static String _byteToHexString(final byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return _HEX_DIGITS_[d1] + _HEX_DIGITS_[d2];
    }

    /**
     */
    private static String _byteArrayToHexString(final byte[] bytes) {
        StringBuilder s = new StringBuilder();
        final int length = bytes.length;
        for (int i = 0; i < length; i++) {
            s.append(_byteToHexString(bytes[i]));
        }
        return s.toString();
    }

    private static class OvalElementComparator implements Comparator<Element> {

        public OvalElementComparator() {
        }

        @Override
        public int compare(final Element e1, final Element e2) {
            String id1 = e1.getOvalID();
            String id2 = e2.getOvalID();
            int order = id1.compareTo(id2);
            if (order != 0) {
                return order;
            }
            int version1 = e1.getOvalVersion();
            int version2 = e2.getOvalVersion();
            return (version1 - version2);
        }

        @Override
        public boolean equals(final Object obj) {
            return (obj instanceof OvalElementComparator);
        }
    }
}
