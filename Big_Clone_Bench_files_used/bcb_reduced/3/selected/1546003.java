package net.sf.josser.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import net.sf.josser.Josser;

/**
 * @author Copyright © Giovanni Novelli. All rights reserved.
 */
public class Topics extends Hashtable<String, Integer> {

    private static final long serialVersionUID = 3354902040309523015L;

    public Topics() {
        super();
    }

    public Topics(final int initialCapacity) {
        super(initialCapacity);
    }

    private String extractPath(final String str) {
        String result = "";
        final int pos = str.lastIndexOf("/");
        if (pos > 0) {
            result = str.substring(0, pos);
        }
        return result;
    }

    public int findParent(final String topic) {
        final String path = this.extractPath(topic);
        final String pdigest = this.getDigest(path.getBytes());
        final Integer parent = this.get(pdigest);
        if (parent != null) {
            return parent.intValue();
        } else {
            return 0;
        }
    }

    private String hex(final byte[] array) {
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < array.length; ++i) {
            sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).toLowerCase().substring(1, 3));
        }
        return sb.toString();
    }

    private String getDigest(final byte[] buffer) {
        try {
            final MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(buffer);
            final byte[] hash = md5.digest();
            String w = this.hex(hash);
            w = w.toLowerCase();
            return w;
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public int findTopic(final String topic) {
        final String path = topic;
        final String pdigest = this.getDigest(path.getBytes());
        final Integer parent = this.get(pdigest);
        if (parent != null) {
            return parent.intValue();
        } else {
            return 0;
        }
    }

    public Integer addTopic(String topic, Integer catid) {
        final String md5 = this.getDigest(topic.getBytes());
        return this.put(md5, catid);
    }
}
