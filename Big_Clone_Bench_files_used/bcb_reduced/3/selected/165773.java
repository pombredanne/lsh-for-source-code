package enigma.crypto.random;

import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import enigma.crypto.digest.MD5Stream;
import enigma.crypto.stream.RC4Cipher;
import enigma.util.Bytes;

public class RC4PrngCipher extends RC4Cipher {

    private static final String STORE_NAME = ".rnd";

    private static MD5Stream md5 = new MD5Stream();

    private static RC4PrngCipher instance = new RC4PrngCipher();

    public static RC4PrngCipher getInstance() {
        return instance;
    }

    private static byte[] getSeed() {
        byte[] seed = new byte[32];
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE_NAME, true, RecordStore.AUTHMODE_PRIVATE, false);
            RecordEnumeration re = rs.enumerateRecords(null, null, false);
            if (re.hasNextElement()) {
                int recordId = re.nextRecordId();
                if (rs.getRecordSize(recordId) == 32) {
                    seed = rs.getRecord(recordId);
                } else {
                    rs.setRecord(recordId, seed, 0, 32);
                }
            } else {
                rs.addRecord(seed, 0, 32);
            }
            rs.closeRecordStore();
        } catch (Exception e) {
        }
        long milliTime = System.currentTimeMillis();
        long nanoTime = 0;
        while (System.currentTimeMillis() < milliTime + 100) {
            nanoTime++;
        }
        byte[] seed1 = md5.digest(Bytes.fromLong(milliTime));
        byte[] seed2 = md5.digest(Bytes.fromLong(nanoTime));
        for (int i = 0; i < 16; i++) {
            seed[i] ^= seed1[i];
            seed[16 + i] ^= seed2[i];
        }
        byte[] result = Bytes.clone(seed);
        seed1 = md5.digest(Bytes.fromLong(nanoTime, Bytes.LITTLE_ENDIAN));
        seed2 = md5.digest(Bytes.fromLong(milliTime, Bytes.LITTLE_ENDIAN));
        for (int i = 0; i < 16; i++) {
            seed[i] ^= seed1[i];
            seed[16 + i] ^= seed2[i];
        }
        try {
            RecordStore rs = RecordStore.openRecordStore(STORE_NAME, true, RecordStore.AUTHMODE_PRIVATE, false);
            RecordEnumeration re = rs.enumerateRecords(null, null, false);
            if (re.hasNextElement()) {
                int recordId = re.nextRecordId();
                rs.setRecord(recordId, seed, 0, 32);
            } else {
                rs.addRecord(seed, 0, 32);
            }
            rs.closeRecordStore();
        } catch (Exception e) {
        }
        return result;
    }

    private RC4PrngCipher() {
        super(getSeed());
    }

    public synchronized int output() {
        return super.output();
    }

    public synchronized void output(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++) {
            b[off + i] = (byte) super.output();
        }
    }

    public int update(int b) {
        throw new RuntimeException();
    }

    public void update(byte[] src, int srcPos, byte[] dest, int destPos, int length) {
        throw new RuntimeException();
    }

    public OutputStream encrypt(final OutputStream out) {
        throw new RuntimeException();
    }

    public InputStream decrypt(final InputStream in) {
        throw new RuntimeException();
    }
}
