package com.jot.system.pjson;

import java.io.Serializable;
import java.security.MessageDigest;
import java.util.Random;
import mojasi.Mojasi;
import mojasi.MojasiException;
import mojasi.MojasiFactory;
import mojasi.MojasiName;
import mojasi.MojasiParser;
import mojasi.MojasiToken;
import mojasi.MojasiWriter;
import org.apache.log4j.Logger;
import com.jot.system.utils.Crypto;

/**
 * An object with 160 random bits.
 * 
 */
@PJSONName("Guid")
@MojasiName("Guid")
public final class Guid implements Comparable<Object>, Serializable, Pjson {

    private static final long serialVersionUID = -43982979016022367L;

    public static Logger logger = Logger.getLogger(Guid.class);

    private long long1;

    private long long2;

    private int int1;

    private static final boolean tests = false;

    public Guid() {
    }

    public Guid(Random rand) {
        long1 = rand.nextLong();
        long2 = rand.nextLong();
        int1 = (int) rand.nextLong();
    }

    public Guid(String in) {
        initFromString_dontUseThis(in);
    }

    @Override
    public int hashCode() {
        return (int) (long1 >> 32);
    }

    /**
     * Generate a fairly well hashed supposedly unique guid from this guid using the index supplied We only plan to use
     * this up to an i of 4096 and level of 5
     * 
     * @param i
     * @return
     */
    public Guid getSequence(int i, int level) {
        Guid nn = new Guid();
        nn.int1 = int1 + level * 1103515245;
        nn.long2 = long2;
        nn.long1 = long1 + i * ((0x5DEECE66DL << 30));
        return nn;
    }

    public int get4kBucket(int level) {
        if (level == 0) {
            return 0xFFF & (int) (long1 >> (64 - 12));
        } else {
            return 0xFFF & (int) (long1 >> (64 - 12 * level));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        Guid guid = (Guid) o;
        if (guid.long1 != this.long1) return false;
        if (guid.long2 != this.long2) return false;
        if (guid.int1 != this.int1) return false;
        return true;
    }

    public int compareTo(Object o) {
        Guid guid = (Guid) o;
        long tmp = this.long1 - guid.long1;
        if (tmp != 0) return tmp > 0 ? 1 : -1;
        tmp = this.long2 - guid.long2;
        if (tmp != 0) return tmp > 0 ? 1 : -1;
        return this.int1 - guid.int1;
    }

    public void initFromString_dontUseThis(String in) {
        byte[] base64bytes = in.getBytes();
        if (base64bytes.length < 28) {
            byte[] newb = new byte[28];
            System.arraycopy(base64bytes, 0, newb, 0, base64bytes.length);
            for (int i = base64bytes.length; i < 28; i++) newb[i] = (byte) 'A';
            base64bytes = newb;
        }
        byte[] bytes = Base64Coder.decode(base64bytes, 0, 28);
        setFrom20bytes(bytes, 0);
    }

    public void setFrom20bytes(byte[] bytes, int offset) {
        long1 = ((int) bytes[offset++]);
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long1 <<= 8;
        long1 |= ((int) bytes[offset++]) & 0xFF;
        long2 = ((int) bytes[offset++]);
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        long2 <<= 8;
        long2 |= ((int) bytes[offset++]) & 0xFF;
        int1 = ((int) bytes[offset++]);
        int1 <<= 8;
        int1 |= ((int) bytes[offset++]) & 0xFF;
        int1 <<= 8;
        int1 |= ((int) bytes[offset++]) & 0xFF;
        int1 <<= 8;
        int1 |= ((int) bytes[offset++]) & 0xFF;
    }

    public void writeTo20Bytes(byte[] bytes, int offset) {
        long tmp = long1;
        bytes[7 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[6 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[5 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[4 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[3 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[2 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[1 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[0 + offset] = (byte) tmp;
        tmp = long2;
        bytes[15 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[14 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[13 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[12 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[11 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[10 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[9 + offset] = (byte) tmp;
        tmp >>= 8;
        bytes[8 + offset] = (byte) tmp;
        int i = int1;
        bytes[19 + offset] = (byte) i;
        i >>= 8;
        bytes[18 + offset] = (byte) i;
        i >>= 8;
        bytes[17 + offset] = (byte) i;
        i >>= 8;
        bytes[16 + offset] = (byte) i;
    }

    public String toStringTrimmed() {
        byte[] bytesin = new byte[20];
        writeTo20Bytes(bytesin, 0);
        byte[] resbytes = Base64Coder.encode(bytesin);
        String res = new String(resbytes).substring(0, 27);
        while (res.endsWith("AAAAAAAA")) res = res.substring(0, res.length() - 8);
        while (res.endsWith("AAAA")) res = res.substring(0, res.length() - 4);
        while (res.endsWith("A")) res = res.substring(0, res.length() - 1);
        return res;
    }

    @Override
    public String toString() {
        return toB64String();
    }

    public String toLongString() {
        byte[] bytesin = new byte[20];
        writeTo20Bytes(bytesin, 0);
        byte[] resbytes = Base64Coder.encode(bytesin);
        String res = new String(resbytes).substring(0, 27);
        return res;
    }

    public String toB64String() {
        byte[] bb = new byte[27];
        toBase64(bb, 0);
        String res = new String(bb);
        if (tests) {
            String test = toLongString();
            if (!test.equals(res)) System.out.println("fatal");
        }
        return res;
    }

    private final void do3bytes(int val, byte[] dest, int offset) {
        dest[offset - 0] = Base64Coder.map1[val & 0x3F];
        val >>= 6;
        dest[offset - 1] = Base64Coder.map1[val & 0x3F];
        val >>= 6;
        dest[offset - 2] = Base64Coder.map1[val & 0x3F];
        val >>= 6;
        dest[offset - 3] = Base64Coder.map1[val & 0x3F];
    }

    public final void toBase64(byte[] dest, int offset) {
        int pos = offset + 27;
        int tmp = int1;
        dest[pos - 1] = Base64Coder.map1[(tmp << 2) & 0x03F];
        tmp >>= 4;
        dest[pos - 2] = Base64Coder.map1[(tmp) & 0x3F];
        tmp >>= 6;
        dest[pos - 3] = Base64Coder.map1[(tmp) & 0x3F];
        tmp >>= 6;
        tmp = (tmp & 0xFFFF) + ((int) long2 << 16);
        do3bytes(tmp, dest, pos - 4);
        tmp = (int) long2 >> 8;
        do3bytes(tmp, dest, pos - 8);
        tmp = (int) (long2 >> 32);
        do3bytes(tmp, dest, pos - 12);
        tmp = ((tmp >> 24) & 0xFF) + ((int) long1 << 8);
        do3bytes(tmp, dest, pos - 16);
        tmp = (int) (long1 >> 16);
        do3bytes(tmp, dest, pos - 20);
        tmp = (int) (long1 >> 40);
        do3bytes(tmp, dest, pos - 24);
        if (tests) {
            String res = new String(dest, offset, 27);
            String test = toLongString();
            if (!test.equals(res)) {
                System.out.println("fatal");
                test = toLongString();
                res = new String(dest, offset, 27);
                toBase64(new byte[27], 0);
            }
        }
    }

    private final int do4base64bytes(byte[] src, int offset) {
        int tmp = Base64Coder.map2[src[offset]];
        tmp <<= 6;
        tmp |= Base64Coder.map2[src[offset + 1]];
        tmp <<= 6;
        tmp |= Base64Coder.map2[src[offset + 2]];
        tmp <<= 6;
        tmp |= Base64Coder.map2[src[offset + 3]];
        return tmp;
    }

    public final void fromBase64(byte[] src, int offset) {
        int pos = offset;
        long tmp1 = do4base64bytes(src, pos);
        long tmp2 = do4base64bytes(src, pos + 4);
        int tmp3 = do4base64bytes(src, pos + 8);
        long1 = (tmp1 << 40) + (tmp2 << 16) + (tmp3 >> 8);
        long tmp4 = do4base64bytes(src, pos + 12);
        long tmp5 = do4base64bytes(src, pos + 16);
        int tmp6 = do4base64bytes(src, pos + 20);
        long2 = ((long) tmp3 << 56) + (tmp4 << 32) + (tmp5 << 8) + (tmp6 >> 16);
        int tmp7 = Base64Coder.map2[src[pos + 24]];
        int tmp8 = Base64Coder.map2[src[pos + 25]];
        int tmp9 = Base64Coder.map2[src[pos + 26]];
        int1 = (tmp6 << 16) + (tmp7 << 10) + (tmp8 << 4) + (tmp9 >> 2);
        if (tests) {
            String b64 = new String(src, offset, 27);
            Guid tmp = new Guid(b64);
            if (!tmp.equals(this)) System.out.println("fatal fatal fatal fatal fatal fatal ");
        }
    }

    public void writePjson(PjsonWriteUtil dest) {
        dest.writeGUID(this);
    }

    public static class GuidFactory extends PjsonFactory {

        public Object make(PjsonParseUtil parser) throws Exception {
            com.jot.system.pjson.Guid tmp = parser.readGuid();
            return tmp;
        }
    }

    public static PjsonFactory factory = new GuidFactory();

    public static Guid SHA1(String stuff) {
        Guid guid = null;
        MessageDigest digest = Crypto.messageDigestThreadLocal.get();
        digest.reset();
        digest.update(stuff.getBytes());
        byte[] key = digest.digest();
        guid = new Guid();
        guid.setFrom20bytes(key, 0);
        return guid;
    }

    public static Guid createSecretGuid() {
        byte[] bytes = Crypto.createSecretKeyBytes();
        Guid guid = new Guid();
        guid.setFrom20bytes(bytes, 0);
        if (false && tests) {
        }
        return guid;
    }

    public static Guid createHash(String seed) {
        return SHA1(seed);
    }

    /**
      This is not generated - it's custom. atw.
     */
    public static class GuidMFactory extends MojasiFactory {

        @SuppressWarnings("unchecked")
        public GuidMFactory() {
            super(new MojasiToken("Guid"), Guid.class);
            isCompound = false;
            lenAmount = 2;
        }

        @Override
        public Object read(MojasiParser parser, MojasiToken token, Object obj) throws MojasiException {
            Guid me = new Guid();
            me.fromBase64(token.getChars(), token.getValPos() + 1);
            return me;
        }

        ;

        @Override
        public void write(MojasiWriter w, Object obj1) throws MojasiException {
            Guid me = (Guid) obj1;
            w.checkSize(27);
            me.toBase64(w.getDest(), w.getPos());
            w.incPos(27);
        }

        ;

        @Override
        public Object make(MojasiParser parser, MojasiToken tok) {
            return new Guid();
        }

        ;
    }

    ;

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            Guid g = createSecretGuid();
            Guid g2 = createSecretGuid();
            assert !g.equals(g2);
        }
        {
            Guid g = createHash("test string");
            Guid g2 = createHash("test string");
            assert g.equals(g2);
        }
        Guid g = createSecretGuid();
        byte[] serialized = Mojasi.write(g);
        System.out.println(new String(serialized));
        Guid g2 = (Guid) MojasiParser.bytes2Object(serialized);
        assert g.equals(g2);
    }
}
