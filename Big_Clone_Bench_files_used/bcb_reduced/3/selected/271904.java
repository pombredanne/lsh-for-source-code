package org.openymsg.network.challenge;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Many (most?) other YMSG library developers refer to this code as part of the v11 protocol. The reason for this seems
 * to be that this code first became active while eleven was the latest version. However, Yahoo servers seem to support
 * at least the two most recent protocol versions, meaning that this code was actually first added into their v10
 * clients, but not activated immediately. Activation came much later, when version nine was retired, by which time
 * version eleven was already on the scene.
 * 
 * I think it is more correct to label this challenge/response code as version ten. (The current v11 probably has some
 * hidden gems which won't be activated until twelve is with us, and ten is finally end-of-line'd!) If you study other
 * YMSG libraries you should be aware of the labeling differences.
 * 
 * Yahoo has repeatedly tweaked their login protocol, by designing the challenge/response code in such a way that
 * aspects of the algorithm will remain unused until the challenge key is tweaked to cause them to swing into effect.
 * The major change came around September 2003, when Yahoo dropped support for version nine clients, allowing them to
 * switch on a few nasties they had added into the version ten c/r code. Then in January 2004 they switched on a new way
 * of encoding their messages using a fake mathematical expression. Again in June 2004 they tweaked the c/r to take
 * advantage of a flaw in the way many third party libraries had implemented the algorithm (most of whom base their code
 * on work done by the Gaim and Trillian projects).
 * 
 * Yahoo claim that they do these things to protect their users from spam and the like - when will they learn that third
 * party developers are (almost always) not the enemy...? They don't like spammers any more than Yahoo do (indeed many
 * of them have added advanced filtering and security technologies into their clients above-and-beyond that which Yahoo
 * supports). If only they would stop wasting so much time fighting third party developers, and co-operate with them, so
 * together we can devise ways of shutting out spammers for good!
 * 
 * Here endeth the rant ;-)
 * 
 * @author G. der Kinderen, Nimbuzz B.V. guus@nimbuzz.com
 * @author S.E. Morris
 */
public class ChallengeResponseV10 extends ChallengeResponseUtility implements ChallengeResponseV10Tables {

    /**
     * These lookup tables are used in decoding the challenge string.
     */
    private static final String ALPHANUM_LOOKUP = "qzec2tb3um1olpar8whx4dfgijknsvy5";

    private static final String OPERATORS_LOOKUP = "+|&%/*^-";

    /**
     * These lookup tables are used in encoding the response strings
     */
    private static final String ENCODE1_LOOKUP = "FBZDWAGHrJTLMNOPpRSKUVEXYChImkwQ";

    private static final String ENCODE2_LOOKUP = "F0E1D2C3B4A59687abcdefghijklmnop";

    private static final String ENCODE3_LOOKUP = ",;";

    private static final String BINARY_DATA = "/challenge.bin";

    private static byte[] data = null;

    /**
     * Given a username, password and challenge string, this code returns the two valid response strings needed to login
     * to Yahoo.
     * 
     * @param username
     *            Username of the session that is trying to authenticate
     * @param password
     *            Password that validates <tt>username</tt>
     * @param challenge
     *            The challenge as received from the Yahoo network.
     * @return The two valid response Strings needed to finish authenticating.
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static String[] getStrings(final String username, final String password, final String challenge) throws NoSuchAlgorithmException, IOException {
        int operand = 0, i;
        int cnt = 0;
        for (i = 0; i < challenge.length(); i++) {
            if (isOperator(challenge.charAt(i))) {
                cnt++;
            }
        }
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
        for (i = magic.length - 2; i >= 0; i--) {
            int a = magic[i];
            int b = magic[i + 1];
            a = ((a * 0xcd) ^ b) & 0xff;
            magic[i + 1] = a;
        }
        byte[] comparison = _part3Munge(magic);
        long seed = 0;
        byte[] binLookup = new byte[7];
        for (i = 0; i < 4; i++) {
            seed = seed << 8;
            seed += (comparison[3 - i] & 0xff);
            binLookup[i] = (byte) (comparison[i] & 0xff);
        }
        int table = 0, depth = 0;
        MessageDigest localMd5 = MessageDigest.getInstance("MD5");
        for (i = 0; i < 0xffff; i++) {
            for (int j = 0; j < 5; j++) {
                binLookup[4] = (byte) (i & 0xff);
                binLookup[5] = (byte) ((i >> 8) & 0xff);
                binLookup[6] = (byte) j;
                localMd5.reset();
                byte[] result = localMd5.digest(binLookup);
                if (_part3Compare(result, comparison) == true) {
                    depth = i;
                    table = j;
                    i = 0xffff;
                    j = 5;
                }
            }
        }
        byte[] magicValue = new byte[4];
        seed = _part3Lookup(table, depth, seed);
        seed = _part3Lookup(table, depth, seed);
        for (i = 0; i < magicValue.length; i++) {
            magicValue[i] = (byte) (seed & 0xff);
            seed = seed >> 8;
        }
        String regular = yahoo64(md5(password));
        String crypted = yahoo64(md5(md5Crypt(password, "$1$_2S43d5f")));
        boolean hackSha1 = (table >= 3);
        String[] s = new String[2];
        s[0] = _part4Encode(_part4Hash(regular, magicValue, hackSha1));
        s[1] = _part4Encode(_part4Hash(crypted, magicValue, hackSha1));
        return s;
    }

    private static byte[] _part3Munge(int[] magic) {
        int res, i = 1;
        byte[] comparison = new byte[20];
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
        return comparison;
    }

    private static int _part3Lookup(int table, int depth, long seed) throws IOException {
        int offset = 0;
        long a, b, c;
        long idx = seed;
        int iseed = (int) seed;
        for (int i = 0; i < depth; i++) {
            if (table == 0) return iseed;
            if (idx < 0) idx += 0x100000000L;
            int[] opArr = OPS[table][(int) (idx % 96)];
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
            a = 0;
            c = iseed;
            for (int j = 0; j < 4; j++) {
                a = (a ^ c & 0xff) * 0x9e3779b1;
                c = c >> 8;
            }
            idx = (int) ((((a ^ (a >> 8)) >> 16) ^ a) ^ (a >> 8)) & 0xff;
            iseed = iseed * 0x00010dcd;
        }
        return iseed;
    }

    /**
     * Returns the requested data, loading the data file if it wasn't loaded before.
     * 
     * @param offset
     * @param idx
     * @return
     * @throws IOException
     */
    private static final int _data(int offset, int idx) throws IOException {
        if (data == null) {
            loadData();
        }
        return (data[offset + idx] & 0xff);
    }

    /**
     * Replaces the 'data' field with the data found in BINARY_DATA
     * 
     * @throws IOException
     */
    private static final void loadData() throws IOException {
        final int size = TABLE_OFFSETS[TABLE_OFFSETS.length - 1];
        final InputStream stream;
        if ((stream = ChallengeResponseV10.class.getResourceAsStream(BINARY_DATA)) == null) {
            throw new IOException("BINARY_DATA does not seem to exist [" + BINARY_DATA + "]");
        }
        data = new byte[size];
        BufferedInputStream is = null;
        try {
            is = new BufferedInputStream(stream);
            if (is.read(data) != size) {
                throw new IllegalStateException("Data too short?");
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private static final boolean _part3Compare(byte[] a, byte[] b) {
        for (int i = 0; i < 16; i++) {
            if (a[i] != b[i + 4]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Each 16 bit value (2 bytes) is encoded into three chars, 5 bits per char, with the least sig bit either ',' or
     * ';' - a equals is inserted in the middle to make it look like an assignment.
     */
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

    private static byte[] _part4Hash(String target, byte[] magicValue, boolean hackSha1) {
        byte[] xor1 = _part4Xor(target, 0x36);
        byte[] xor2 = _part4Xor(target, 0x5c);
        SHA1 sha1 = new SHA1();
        sha1.update(xor1);
        if (hackSha1) sha1.setBitCount(0x1ff);
        sha1.update(magicValue);
        byte[] digest1 = sha1.digest();
        sha1.reset();
        sha1.update(xor2);
        sha1.update(digest1);
        byte[] digest2 = sha1.digest();
        return digest2;
    }

    private static byte[] _part4Xor(String s, int op) {
        byte[] arr = new byte[64];
        for (int i = 0; i < s.length(); i++) arr[i] = (byte) (s.charAt(i) ^ op);
        for (int i = s.length(); i < arr.length; i++) arr[i] = (byte) op;
        return arr;
    }

    /**
     * Checks if the character is one of the eight operator characters.
     * 
     * @param c
     * @return ''true'' if c is one of the eight operator chars, ''false'' otherwise.
     */
    private static boolean isOperator(char c) {
        return (OPERATORS_LOOKUP.indexOf(c) >= 0);
    }
}
