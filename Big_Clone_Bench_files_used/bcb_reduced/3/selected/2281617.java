package com.ibm.awb.misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Hashtable;

public class Archive implements java.io.Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1400757867680413756L;

    public static final class Entry implements java.io.Serializable {

        /**
	 * 
	 */
        private static final long serialVersionUID = -733774250305285238L;

        Entry(String n, long d, byte[] b) {
            this.name = n;
            this.digest = d;
            this.data = b;
        }

        String name;

        byte data[] = null;

        long digest = 0;

        public String name() {
            return this.name;
        }

        public long digest() {
            if (this.digest == 0) {
                this.digest = _hashcode(digestGen.digest(this.data));
            }
            return this.digest;
        }

        public byte[] data() {
            return this.data;
        }

        static final long _hashcode(byte[] b) {
            long h = 0;
            for (byte element : b) {
                h += (h * 37) + element;
            }
            return h;
        }
    }

    ;

    Hashtable cache = new Hashtable();

    protected static MessageDigest digestGen;

    static {
        try {
            digestGen = MessageDigest.getInstance("SHA");
        } catch (java.security.NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    public synchronized Entry[] entries() {
        Entry[] r = new Entry[this.cache.size()];
        Enumeration e = this.cache.elements();
        int i = 0;
        while (e.hasMoreElements()) {
            r[i++] = (Entry) e.nextElement();
        }
        return r;
    }

    public synchronized Entry getEntry(String name) {
        return (Entry) this.cache.get(name);
    }

    public byte[] getResourceAsByteArray(String name) {
        return this.getResourceInCache(name);
    }

    public InputStream getResourceAsStream(String name) {
        byte[] b = this.getResourceInCache(name);
        if (b != null) {
            return new java.io.ByteArrayInputStream(b);
        }
        return null;
    }

    protected byte[] getResourceInCache(String name) {
        Entry e = this.getEntry(name);
        if (e != null) {
            return e.data;
        }
        return null;
    }

    public synchronized void putResource(String name, byte[] res) {
        this.cache.put(name, new Entry(name, 0, res));
    }

    public synchronized void putResource(String name, long d, byte[] res) {
        this.cache.put(name, new Entry(name, d, res));
    }

    private void readObject(ObjectInputStream s) throws IOException, ClassNotFoundException {
        s.defaultReadObject();
    }

    public synchronized void removeResource(String name) {
        this.cache.remove(name);
    }

    @Override
    public String toString() {
        Enumeration e = this.cache.elements();
        StringBuffer buffer = new StringBuffer();
        int i = 1;
        while (e.hasMoreElements()) {
            Entry en = (Entry) e.nextElement();
            buffer.append("[" + i + "] " + en.name + ", " + en.digest() + '\n');
            i++;
        }
        return buffer.toString();
    }
}
