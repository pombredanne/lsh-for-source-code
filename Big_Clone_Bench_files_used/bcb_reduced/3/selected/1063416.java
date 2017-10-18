package purej.core.module.random;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Random;

/**
 * 
 * This class represents a Pseudo Random Number Generator (PRNG) as specified by
 * the Yarrow documentation (www.counterpane.com/yarrow)
 * 
 * Most of the code in this class is based on FreeNet's Yarrow implementation.
 * 
 * @author Scott G. Miller <scgmille@indiana.edu>
 * @author Victor Salaman <salaman@teknos.com>
 * 
 */
public final class Yarrow extends Random {

    private static final long serialVersionUID = 1L;

    private static final int Pt = 5;

    private static final int Pg = 10;

    private static String seedfile;

    private static final int FAST_THRESHOLD = 100;

    private static final int SLOW_THRESHOLD = 160;

    private static final int SLOW_K = 2;

    static final int[][] bitTable = { { 0, 0x0 }, { 1, 0x1 }, { 1, 0x3 }, { 1, 0x7 }, { 1, 0xf }, { 1, 0x1f }, { 1, 0x3f }, { 1, 0x7f }, { 1, 0xff }, { 2, 0x1ff }, { 2, 0x3ff }, { 2, 0x7ff }, { 2, 0xfff }, { 2, 0x1fff }, { 2, 0x3fff }, { 2, 0x7fff }, { 2, 0xffff }, { 3, 0x1ffff }, { 3, 0x3ffff }, { 3, 0x7ffff }, { 3, 0xfffff }, { 3, 0x1fffff }, { 3, 0x3fffff }, { 3, 0x7fffff }, { 3, 0xffffff }, { 4, 0x1ffffff }, { 4, 0x3ffffff }, { 4, 0x7ffffff }, { 4, 0xfffffff }, { 4, 0x1fffffff }, { 4, 0x3fffffff }, { 4, 0x7fffffff }, { 4, 0xffffffff } };

    static {
        try {
            seedfile = new File(new File(System.getProperty("java.io.tmpdir")), "prng.seed." + System.currentTimeMillis()).toString();
        } catch (Throwable t) {
            seedfile = "prng.seed";
        }
    }

    public byte[] ZERO_ARRAY = new byte[16384];

    private Hashtable<EntropySource, Integer> entropySeen;

    private MessageDigest fast_pool;

    private MessageDigest reseed_ctx;

    private MessageDigest slow_pool;

    private Rijndael cipher_ctx;

    private byte[] allZeroString;

    private byte[] counter;

    private byte[] output_buffer;

    private byte[] tmp;

    private boolean fast_select;

    private int fast_entropy;

    private int fetch_counter;

    private int output_count;

    private int slow_entropy;

    public Yarrow() {
        try {
            accumulator_init();
            reseed_init();
            generator_init();
            entropy_init(seedfile);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public void acceptEntropy(EntropySource source, long data, int entropyGuess) {
        accept_entropy(data, source, Math.min(32, Math.min(estimateEntropy(source, data), entropyGuess)));
    }

    public void acceptTimerEntropy(EntropySource timer) {
        long now = System.currentTimeMillis();
        acceptEntropy(timer, now - timer.lastVal, 32);
    }

    public void makeKey(byte[] entropy, byte[] key, int offset, int len) {
        try {
            MessageDigest ctx = MessageDigest.getInstance("SHA1");
            int ic = 0;
            while (len > 0) {
                ic++;
                for (int i = 0; i < ic; i++) {
                    ctx.update((byte) 0);
                }
                ctx.update(entropy, 0, entropy.length);
                int bc;
                if (len > 20) {
                    ctx.digest(key, offset, 20);
                    bc = 20;
                } else {
                    byte[] hash = ctx.digest();
                    bc = Math.min(len, hash.length);
                    System.arraycopy(hash, 0, key, offset, bc);
                }
                offset += bc;
                len -= bc;
            }
            wipe(entropy);
        } catch (Exception e) {
            throw new RuntimeException("Could not generate key: " + e.getMessage());
        }
    }

    public void wipe(byte[] data) {
        System.arraycopy(ZERO_ARRAY, 0, data, 0, data.length);
    }

    @Override
    protected int next(int bits) {
        int[] parameters = bitTable[bits];
        int offset = getBytes(parameters[0]);
        int val = output_buffer[offset];
        if (parameters[0] == 4) {
            val += ((output_buffer[offset + 1] << 24) + (output_buffer[offset + 2] << 16) + (output_buffer[offset + 3] << 8));
        } else if (parameters[0] == 3) {
            val += ((output_buffer[offset + 1] << 16) + (output_buffer[offset + 2] << 8));
        } else if (parameters[0] == 2) {
            val += (output_buffer[offset + 2] << 8);
        }
        return val & parameters[1];
    }

    private synchronized int getBytes(int count) {
        if ((fetch_counter + count) > output_buffer.length) {
            fetch_counter = 0;
            generateOutput();
            return getBytes(count);
        }
        int rv = fetch_counter;
        fetch_counter += count;
        return rv;
    }

    private void accept_entropy(long data, EntropySource source, int actualEntropy) {
        MessageDigest pool = (fast_select ? fast_pool : slow_pool);
        pool.update((byte) data);
        pool.update((byte) (data >> 8));
        pool.update((byte) (data >> 16));
        pool.update((byte) (data >> 24));
        pool.update((byte) (data >> 32));
        pool.update((byte) (data >> 40));
        pool.update((byte) (data >> 48));
        pool.update((byte) (data >> 56));
        fast_select = !fast_select;
        if (fast_select) {
            fast_entropy += actualEntropy;
            if (fast_entropy > FAST_THRESHOLD) {
                fast_pool_reseed();
            }
        } else {
            slow_entropy += actualEntropy;
            if (source != null) {
                Integer contributedEntropy = entropySeen.get(source);
                if (contributedEntropy == null) {
                    contributedEntropy = new Integer(actualEntropy);
                } else {
                    contributedEntropy = new Integer(actualEntropy + contributedEntropy.intValue());
                }
                entropySeen.put(source, contributedEntropy);
                if (slow_entropy >= (SLOW_THRESHOLD * 2)) {
                    int kc = 0;
                    for (Enumeration<EntropySource> e = entropySeen.keys(); e.hasMoreElements(); ) {
                        Object key = e.nextElement();
                        Integer v = entropySeen.get(key);
                        if (v.intValue() > SLOW_THRESHOLD) {
                            kc++;
                            if (kc >= SLOW_K) {
                                slow_pool_reseed();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private void accumulator_init() throws NoSuchAlgorithmException {
        fast_pool = MessageDigest.getInstance("SHA1");
        slow_pool = MessageDigest.getInstance("SHA1");
        entropySeen = new Hashtable<EntropySource, Integer>();
    }

    private void consumeBytes(byte[] bytes) {
        if (fast_select) {
            fast_pool.update(bytes, 0, bytes.length);
        } else {
            slow_pool.update(bytes, 0, bytes.length);
        }
        fast_select = !fast_select;
    }

    private void consumeString(String str) {
        if (str == null) {
            return;
        }
        byte[] b = str.getBytes();
        consumeBytes(b);
    }

    private void counterInc() {
        for (int i = counter.length - 1; i >= 0; i--) {
            if (++counter[i] != 0) {
                break;
            }
        }
    }

    private void entropy_init(String seed) {
        Properties sys = System.getProperties();
        EntropySource startupEntropy = new EntropySource();
        for (Enumeration<?> e = sys.propertyNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            consumeString(key);
            consumeString(sys.getProperty(key));
        }
        try {
            consumeString(java.net.InetAddress.getLocalHost().toString());
        } catch (Exception e) {
        }
        acceptEntropy(startupEntropy, System.currentTimeMillis(), 0);
        read_seed(seed);
    }

    private int estimateEntropy(EntropySource source, long newVal) {
        int delta = (int) (newVal - source.lastVal);
        int delta2 = delta - source.lastDelta;
        source.lastDelta = delta;
        int delta3 = delta2 - source.lastDelta2;
        source.lastDelta2 = delta2;
        if (delta < 0) {
            delta = -delta;
        }
        if (delta2 < 0) {
            delta2 = -delta2;
        }
        if (delta3 < 0) {
            delta3 = -delta3;
        }
        if (delta > delta2) {
            delta = delta2;
        }
        if (delta > delta3) {
            delta = delta3;
        }
        delta >>= 1;
        delta &= ((1 << 12) - 1);
        delta |= (delta >> 8);
        delta |= (delta >> 4);
        delta |= (delta >> 2);
        delta |= (delta >> 1);
        delta >>= 1;
        delta -= ((delta >> 1) & 0x555);
        delta = (delta & 0x333) + ((delta >> 2) & 0x333);
        delta += (delta >> 4);
        delta += (delta >> 8);
        source.lastVal = newVal;
        return delta & 15;
    }

    private void fast_pool_reseed() {
        byte[] v0 = fast_pool.digest();
        byte[] vi = v0;
        for (byte i = 0; i < Pt; i++) {
            reseed_ctx.update(vi, 0, vi.length);
            reseed_ctx.update(v0, 0, v0.length);
            reseed_ctx.update(i);
            vi = reseed_ctx.digest();
        }
        makeKey(vi, tmp, 0, tmp.length);
        rekey(tmp);
        wipe(v0);
        fast_entropy = 0;
        write_seed(seedfile);
    }

    private void generateOutput() {
        counterInc();
        output_buffer = cipher_ctx.encipher(counter);
        if (output_count++ > Pg) {
            output_count = 0;
            nextBytes(tmp);
            rekey(tmp);
        }
    }

    private void generator_init() {
        cipher_ctx = new Rijndael();
        output_buffer = new byte[cipher_ctx.getBlockSize() / 8];
        counter = new byte[cipher_ctx.getBlockSize() / 8];
        allZeroString = new byte[cipher_ctx.getBlockSize() / 8];
        tmp = new byte[cipher_ctx.getKeySize() / 8];
        fetch_counter = output_buffer.length;
    }

    private void read_seed(String filename) {
        EntropySource seedFile = new EntropySource();
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(new FileInputStream(filename));
            for (int i = 0; i < 32; i++) {
                acceptEntropy(seedFile, dis.readLong(), 64);
            }
        } catch (Exception f) {
            Random rand = new Random();
            for (int i = 0; i < 32; i++) {
                acceptEntropy(seedFile, rand.nextLong(), 64);
            }
        } finally {
            if (dis != null) {
                try {
                    dis.close();
                } catch (IOException e) {
                }
            }
        }
        fast_pool_reseed();
    }

    private void rekey(byte[] key) {
        cipher_ctx.initialize(key);
        counter = cipher_ctx.encipher(allZeroString);
        wipe(key);
    }

    private void reseed_init() throws NoSuchAlgorithmException {
        reseed_ctx = MessageDigest.getInstance("SHA1");
    }

    private void slow_pool_reseed() {
        byte[] slow_hash = slow_pool.digest();
        fast_pool.update(slow_hash, 0, slow_hash.length);
        fast_pool_reseed();
        slow_entropy = 0;
        Integer ZERO = new Integer(0);
        for (Enumeration<EntropySource> e = entropySeen.keys(); e.hasMoreElements(); ) {
            entropySeen.put(e.nextElement(), ZERO);
        }
    }

    private void write_seed(String filename) {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new FileOutputStream(filename));
            for (int i = 0; i < 32; i++) {
                dos.writeLong(nextLong());
            }
            ;
        } catch (IOException ex) {
            System.out.println(getClass().getName() + " error writing seed file to " + filename + ": " + ex);
        } finally {
            if (dos != null) {
                try {
                    dos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public class EntropySource {

        public int lastDelta;

        public int lastDelta2;

        public long lastVal;
    }
}
