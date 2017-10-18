package org.jpos.space;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Arrays;
import jdbm.helper.DefaultSerializer;

public class MD5Template implements Template, Serializable {

    private static final long serialVersionUID = -1204861759575740048L;

    byte[] digest;

    Object key;

    public MD5Template(Object key, Object value) {
        super();
        this.key = key;
        this.digest = digest(value);
    }

    public MD5Template(Object key, byte[] digest) {
        super();
        this.key = key;
        this.digest = digest;
    }

    public static byte[] digest(Object obj) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return md.digest(DefaultSerializer.INSTANCE.serialize(obj));
        } catch (Exception e) {
            throw new SpaceError(e);
        }
    }

    public boolean equals(Object obj) {
        return Arrays.equals(digest(obj), digest);
    }

    public Object getKey() {
        return key;
    }
}
