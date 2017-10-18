package ymsg.network;

import java.io.DataInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class ChallengeResponseV10 extends ChallengeResponseUtility implements ChallengeResponseV10Tables {

    private static final String ALPHANUM_LOOKUP = "qzec2tb3um1olpar8whx4dfgijknsvy5";

    private static final String OPERATORS_LOOKUP = "+|&%/*^-";

    private static final String ENCODE1_LOOKUP = "FBZDWAGHrJTLMNOPpRSKUVEXYChImkwQ";

    private static final String ENCODE2_LOOKUP = "F0E1D2C3B4A59687abcdefghijklmnop";

    private static final String ENCODE3_LOOKUP = ",;";

    private static final boolean DB = false;

    private static final boolean DB2 = false;

    private static byte[] data;

    private static final String BIN_FILE = "challenge.bin";

    static {
        try {
            Class v10 = Class.forName("ymsg.network.ChallengeResponseV10");
            DataInputStream dis = new DataInputStream(v10.getResourceAsStream(BIN_FILE));
            data = new byte[dis.available()];
            if (data.length < TABLE_OFFSETS[TABLE_OFFSETS.length - 1]) throw new Exception("Data too short?");
            dis.readFully(data);
            dis.close();
        } catch (Exception e) {
            System.err.println("Error loading resource file: " + BIN_FILE);
            e.printStackTrace();
        }
    }

    static String[] getStrings(String username, String password, String challenge) throws NoSuchAlgorithmException {
        int operand = 0, i;
        int cnt = 0;
        for (i = 0; i < challenge.length(); i++) if (isOperator(challenge.charAt(i))) cnt++;
        int[] magic = new int[cnt];
        cnt = 0;
        for (i = 0; i < challenge.length(); i++) {
            char c = challenge.charAt(i);
            if (Character.isLetter(c) || Character.isDigit(c)) {
                operand = ALPHANUM_LOOKUP.indexOf(c) << 3;
            } else if (isOperator(c)) {
                int a = OPERATORS_LOOKUP.indexOf(c);
                magic[cnt] = (operand | a) & 0xff;
                cnt++;
            }
        }
        if (DB) dump("P1", magic);
        for (i = magic.length - 2; i >= 0; i--) {
            int a = magic[i], b = magic[i + 1];
            a = ((a * 0xcd) ^ b) & 0xff;
            magic[i + 1] = a;
        }
        if (DB) dump("P2", magic);
        byte[] comparison = _part3Munge(magic);
        long seed = 0;
        byte[] binLookup = new byte[7];
        for (i = 0; i < 4; i++) {
            seed = seed << 8;
            seed += (int) (comparison[3 - i] & 0xff);
            binLookup[i] = (byte) (comparison[i] & 0xff);
        }
        if (DB) dump("P3.1", comparison);
        int table = 0, depth = 0;
        synchronized (md5Obj) {
            for (i = 0; i < 0xffff; i++) {
                for (int j = 0; j < 5; j++) {
                    binLookup[4] = (byte) (i & 0xff);
                    binLookup[5] = (byte) ((i >> 8) & 0xff);
                    binLookup[6] = (byte) j;
                    byte[] result = md5Singleton(binLookup);
                    if (_part3Compare(result, comparison) == true) {
                        depth = i;
                        table = j;
                        i = 0xffff;
                        j = 5;
                    }
                }
            }
        }
        if (DB) System.out.println("P3.2: " + depth + " " + table + ": ");
        byte[] magicValue = new byte[4];
        if (DB) System.out.println("P3.3.a: " + seed);
        seed = _part3Lookup(table, depth, seed);
        if (DB) System.out.println("P3.3.b: " + seed);
        seed = _part3Lookup(table, depth, seed);
        if (DB) System.out.println("P3.3.c: " + seed);
        for (i = 0; i < magicValue.length; i++) {
            magicValue[i] = (byte) (seed & 0xff);
            seed = seed >> 8;
        }
        String regular = yahoo64(md5(password));
        String crypted = yahoo64(md5(md5Crypt(password, "$1$_2S43d5f")));
        if (DB) System.out.println("P4.1 " + regular + " " + crypted);
        boolean hackSha1 = (table >= 3);
        String[] s = new String[2];
        s[0] = _part4Encode(_part4Hash(regular, magicValue, hackSha1));
        s[1] = _part4Encode(_part4Hash(crypted, magicValue, hackSha1));
        if (DB) System.out.println("FINAL " + s[0] + " " + s[1]);
        return s;
    }

    private static byte[] _part3Munge(int[] magic) {
        int res, i = 1;
        byte[] comparison = new byte[20];
        try {
            for (int c = 0; c < comparison.length; c += 2) {
                int a, b;
                a = magic[i++];
                if (a <= 0x7f) {
                    res = a;
                } else {
                    if (a >= 0xe0) {
                        b = magic[i++];
                        a = (a & 0x0f) << 6;
                        b = b & 0x3f;
                        res = (a + b) << 6;
                    } else {
                        res = (a & 0x1f) << 6;
                    }
                    res += (magic[i++] & 0x3f);
                }
                comparison[c] = (byte) ((res & 0xff00) >> 8);
                comparison[c + 1] = (byte) (res & 0xff);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
        }
        return comparison;
    }

    private static int _part3Lookup(int table, int depth, long seed) {
        int offset = 0;
        long a, b, c;
        long idx = seed;
        int iseed = (int) seed;
        for (int i = 0; i < depth; i++) {
            if (table == 0) return iseed;
            if (idx < 0) idx += 0x100000000L;
            int[] opArr = OPS[table][(int) (idx % 96)];
            if (DB2) System.out.println("LOOK1:" + table + " " + depth + " " + iseed + ":" + idx + " " + opArr[OP]);
            switch(opArr[OP]) {
                case XOR:
                    iseed ^= opArr[ARG1];
                    break;
                case MULADD:
                    iseed = iseed * opArr[ARG1] + opArr[ARG2];
                    break;
                case LOOKUP:
                    offset = TABLE_OFFSETS[opArr[ARG1]];
                    b = _data(offset, (iseed & 0xff)) | _data(offset, (iseed >> 8) & 0xff) << 8 | _data(offset, (iseed >> 16) & 0xff) << 16 | _data(offset, (iseed >> 24) & 0xff) << 24;
                    iseed = (int) b;
                    break;
                case BITFLD:
                    offset = TABLE_OFFSETS[opArr[ARG1]];
                    c = 0;
                    for (int j = 0; j < 32; j++) {
                        a = ((iseed >> j) & 1) << _data(offset, j);
                        b = ~(1 << _data(offset, j)) & c;
                        c = a | b;
                    }
                    iseed = (int) c;
                    break;
            }
            if (depth - i <= 1) return iseed;
            if (DB2) System.out.println("LOOK2:" + iseed + ":" + idx);
            a = 0;
            c = iseed;
            for (int j = 0; j < 4; j++) {
                a = (a ^ c & 0xff) * 0x9e3779b1;
                c = c >> 8;
            }
            idx = (int) ((((a ^ (a >> 8)) >> 16) ^ a) ^ (a >> 8)) & 0xff;
            iseed = iseed * 0x00010dcd;
            if (DB2) System.out.println("LOOK3:" + iseed + ":" + idx);
        }
        return iseed;
    }

    private static final int _data(int offset, int idx) {
        return (int) (data[offset + idx] & 0xff);
    }

    private static final boolean _part3Compare(byte[] a, byte[] b) {
        for (int i = 0; i < 16; i++) if (a[i] != b[i + 4]) return false;
        return true;
    }

    private static String _part4Encode(byte[] buffer) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buffer.length; i += 2) {
            int a = ((buffer[i] & 0xff) << 8) + (buffer[i + 1] & 0xff);
            sb.append(ENCODE1_LOOKUP.charAt((a >> 11) & 0x1f));
            sb.append('=');
            sb.append(ENCODE2_LOOKUP.charAt((a >> 6) & 0x1f));
            sb.append(ENCODE2_LOOKUP.charAt((a >> 1) & 0x1f));
            sb.append(ENCODE3_LOOKUP.charAt(a & 0x01));
        }
        return sb.toString();
    }

    private static byte[] _part4Hash(String target, byte[] magicValue, boolean hackSha1) throws NoSuchAlgorithmException {
        byte[] xor1 = _part4Xor(target, 0x36);
        byte[] xor2 = _part4Xor(target, 0x5c);
        if (DB) {
            dump("P4.2", xor1);
            dump("P4.2", xor2);
            dump("P4.2", magicValue);
        }
        SHA1 sha1 = new SHA1();
        sha1.update(xor1);
        if (hackSha1) sha1.setBitCount(0x1ff);
        sha1.update(magicValue);
        byte[] digest1 = sha1.digest();
        sha1.reset();
        sha1.update(xor2);
        sha1.update(digest1);
        byte[] digest2 = sha1.digest();
        if (DB) {
            dump("P4.3", digest1);
            dump("P4.3", digest2);
        }
        return digest2;
    }

    private static byte[] _part4Xor(String s, int op) {
        byte[] arr = new byte[64];
        for (int i = 0; i < s.length(); i++) arr[i] = (byte) (s.charAt(i) ^ op);
        for (int i = s.length(); i < arr.length; i++) arr[i] = (byte) op;
        return arr;
    }

    private static boolean isOperator(char c) {
        return (OPERATORS_LOOKUP.indexOf(c) >= 0);
    }

    static void dump(String title, int[] data) {
        int idx = 0;
        System.out.println(title);
        while (idx < data.length) {
            String s = Integer.toHexString(data[idx]);
            if (s.length() < 2) s = "0" + s;
            System.out.print(s + " ");
            idx++;
            if ((idx % 20) == 0) System.out.print("\n");
        }
        if ((idx % 20) != 0) System.out.print("\n");
    }

    static void dump(String title, byte[] data) {
        int idx = 0;
        System.out.println(title);
        while (idx < data.length) {
            String s = Integer.toHexString(data[idx] & 0xff);
            if (s.length() < 2) s = "0" + s;
            System.out.print(s + " ");
            idx++;
            if ((idx % 20) == 0) System.out.print("\n");
        }
        if ((idx % 20) != 0) System.out.print("\n");
    }
}
