package com.ibm.aglets.tahiti;

import java.security.MessageDigest;
import com.ibm.maf.ClassName;

public final class DigestTable {

    int num = 0;

    int max = 0;

    String names[] = null;

    long digests[] = null;

    private static MessageDigest digestGen;

    static {
        try {
            digestGen = MessageDigest.getInstance("SHA");
        } catch (java.security.NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }

    public DigestTable() {
        this(20);
    }

    public DigestTable(int n) {
        this.max = n;
        this.names = new String[n];
        this.digests = new long[n];
    }

    static final int _hashcode(byte[] b) {
        int h = 0;
        for (byte element : b) {
            h += (h * 37) + element;
        }
        return h;
    }

    private static void debug(String msg) {
        if (false) {
            System.out.println(msg);
        }
    }

    public ClassName[] getClassNames(Class classes[]) {
        ClassName[] cn = new ClassName[classes.length];
        int c = 0;
        for (Class classe : classes) {
            String filename = classe.getName().replace('.', '/') + ".class";
            long l = this.getDigest(filename);
            if (l != 0) {
                cn[c++] = new ClassName(filename, toByteArray(l));
            }
        }
        ClassName[] r = new ClassName[c];
        System.arraycopy(cn, 0, r, 0, c);
        return r;
    }

    public long getDigest(int i) {
        return this.digests[i];
    }

    public long getDigest(String name) {
        if (name == null) {
            return 0;
        }
        for (int i = 0; i < this.num; i++) {
            if (name.equals(this.names[i])) {
                return this.digests[i];
            }
        }
        return 0;
    }

    public byte[] getDigestAsByte(String name) {
        long l = this.getDigest(name);
        if (l == 0) {
            return toByteArray(l);
        }
        return null;
    }

    public String getName(int i) {
        return this.names[i];
    }

    private void grow(int new_max) {
        int old_max = this.max;
        this.max = new_max;
        String old_names[] = this.names;
        long old_digests[] = this.digests;
        this.names = new String[this.max];
        this.digests = new long[this.max];
        System.arraycopy(old_names, 0, this.names, 0, old_max);
        System.arraycopy(old_digests, 0, this.digests, 0, old_max);
    }

    public synchronized boolean match(ClassName[] table, boolean _import) {
        int num = table.length;
        DigestTable merge = new DigestTable(num);
        for (int i = 0; i < num; i++) {
            String name = table[i].name;
            long d1 = toLong(table[i].descriminator);
            long d2 = this.getDigest(name);
            if (d2 == 0) {
                if (_import) {
                    merge.setDigest(name, d1);
                } else {
                    return false;
                }
            } else if (d1 != d2) {
                debug(name + " has different versions");
                return false;
            }
        }
        if (_import) {
            this.simpleMerge(merge);
        }
        return true;
    }

    public synchronized long setData(String name, byte[] data) {
        long l = _hashcode(digestGen.digest(data));
        this.setDigest(name, l);
        return l;
    }

    public synchronized void setDigest(String name, long val) {
        if (this.num >= this.max) {
            this.grow(this.max * 2);
        }
        for (int i = 0; i < this.num; i++) {
            if (this.names[i].equals(name)) {
                if (this.digests[i] == val) {
                    return;
                }
                Thread.dumpStack();
                System.out.println(this);
                throw new RuntimeException("digest conflist occur! : " + name + "," + val);
            }
        }
        this.names[this.num] = name;
        this.digests[this.num++] = val;
    }

    private void simpleMerge(DigestTable table) {
        if ((this.num + table.num) >= this.max) {
            this.grow((this.num + table.num) * 2);
        }
        try {
            System.arraycopy(table.names, 0, this.names, this.num, table.num);
            System.arraycopy(table.digests, 0, this.digests, this.num, table.num);
            this.num += table.num;
        } catch (Exception ex) {
            System.out.println("error in mergig digest table.");
            System.out.println("---table to be added---");
            System.out.println(table);
            System.out.println("---current---");
            System.out.println(this);
        }
    }

    public int size() {
        return this.num;
    }

    public static byte[] toByteArray(long l) {
        byte b[] = new byte[8];
        b[0] = (byte) (int) ((l >>> 56) & 0xFF);
        b[1] = (byte) (int) ((l >>> 48) & 0xFF);
        b[2] = (byte) (int) ((l >>> 40) & 0xFF);
        b[3] = (byte) (int) ((l >>> 32) & 0xFF);
        b[4] = (byte) (int) ((l >>> 24) & 0xFF);
        b[5] = (byte) (int) ((l >>> 16) & 0xFF);
        b[6] = (byte) (int) ((l >>> 8) & 0xFF);
        b[7] = (byte) (int) ((l >>> 0) & 0xFF);
        return b;
    }

    public static long toLong(byte[] b) {
        int ch1 = b[0] & 0xFF;
        int ch2 = b[1] & 0xFF;
        int ch3 = b[2] & 0xFF;
        int ch4 = b[3] & 0xFF;
        int h = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        int ch5 = b[4] & 0xFF;
        int ch6 = b[5] & 0xFF;
        int ch7 = b[6] & 0xFF;
        int ch8 = b[7] & 0xFF;
        int l = ((ch5 << 24) + (ch6 << 16) + (ch7 << 8) + (ch8 << 0));
        long v = ((long) (h) << 32) + (l & 0xFFFFFFFFL);
        return v;
    }

    @Override
    public String toString() {
        return this.toString("DigestTable");
    }

    public String toString(String title) {
        StringBuffer r = new StringBuffer(title + "(" + this.num + ")\n");
        for (int i = 0; i < this.num; i++) {
            r.append("[" + i + "] " + this.names[i] + " = " + this.digests[i] + "\n");
        }
        return r.toString();
    }
}
