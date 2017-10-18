package ao.util.math.crypt;

import ao.util.math.Calc;
import java.io.*;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

/**
 * Date: Nov 16, 2008
 * Time: 3:40:54 PM
 */
public class MD5 implements SecureHash {

    private static final long serialVersionUID = 1L;

    public static void main(String[] args) {
        MD5 hash = new MD5();
        long before = System.currentTimeMillis();
        hash.feed(new File("C:\\~\\proj\\log_sniffer\\tmp_logs\\" + "brampton\\2009-06-03--21.44.40\\sme01\\" + "2009-06-03--22.10.11_sme01_Audit.jar"));
        System.out.println("took: " + (System.currentTimeMillis() - before));
        System.out.println(hash.hexDigest());
        System.out.println(hash.hexDigest());
    }

    public static String hexDigest(String data) {
        MD5 instance = new MD5();
        instance.feed(data.getBytes());
        return instance.hexDigest();
    }

    private final MessageDigest md5;

    public MD5() {
        MessageDigest digestInstance;
        try {
            digestInstance = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            digestInstance = null;
        }
        md5 = digestInstance;
    }

    public void feed(File file) {
        if (!(file.exists() && file.canRead())) return;
        feed(file, file.length());
    }

    public void feed(File file, long upTo) {
        assert upTo >= 0;
        try {
            doFeed(file, upTo);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void doFeed(File file, long upTo) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        byte buffer[] = new byte[1024];
        for (long i = 0; i < upTo; i++) {
            int read = in.read(buffer);
            if (read == -1) break;
            feed((read == buffer.length) ? buffer : Arrays.copyOf(buffer, read));
        }
        in.close();
    }

    public void feed(byte value) {
        md5.update(value);
    }

    public void feed(byte[] values) {
        md5.update(values);
    }

    public void feed(char[] values) {
        byte b[] = new byte[(Character.SIZE >> 3) * values.length];
        ByteBuffer buf = ByteBuffer.wrap(b);
        for (char value : values) buf.putChar(value);
        md5.update(b);
    }

    public void feed(int value) {
        byte b[] = new byte[Integer.SIZE >> 3];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putInt(value);
        md5.update(b);
    }

    public void feed(long value) {
        byte b[] = new byte[Long.SIZE >> 3];
        ByteBuffer buf = ByteBuffer.wrap(b);
        buf.putLong(value);
        md5.update(b);
    }

    public byte[] digest() {
        return md5.digest();
    }

    public BigInteger bigDigest() {
        return new BigInteger(digest());
    }

    public String hexDigest() {
        StringBuilder str = new StringBuilder();
        for (byte d : digest()) {
            String hexDigits = Integer.toHexString(Calc.unsigned(d));
            str.append(hexDigits.length() == 1 ? "0" : "").append(hexDigits);
        }
        return str.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MD5 md51 = (MD5) o;
        return md5.equals(md51.md5);
    }

    @Override
    public int hashCode() {
        return md5.hashCode();
    }

    @Override
    public String toString() {
        return md5.toString();
    }
}
