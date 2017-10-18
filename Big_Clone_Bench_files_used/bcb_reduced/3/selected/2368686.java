package net.anydigit.jiliu.hash;

import java.security.MessageDigest;

/**
 * @author xingfei [xingfei0831 AT gmail.com]
 *
 */
public class Md5 extends IntegerHash {

    private MessageDigest md5;

    public Md5() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            throw new NoSuchAlgorithmException("md5", e);
        }
    }

    @Override
    public long digest() {
        byte[] d = md5.digest();
        long hash = d[3] & 0xFF;
        hash += (d[2] & 0xFF) << 8;
        hash += (d[1] & 0xFF) << 16;
        hash += ((long) (d[0] & 0xFF)) << 24;
        return hash;
    }

    @Override
    public void reset() {
        md5.reset();
    }

    @Override
    public void update(byte[] src) {
        md5.update(src);
    }

    public static void main(String[] args) {
        Md5 m = new Md5();
        String k = args[0];
        m.update(k.getBytes());
        long h = m.digest();
        System.out.println(h);
    }
}
