package ch.comtools.jsch.jcraft;

import java.security.MessageDigest;

class HMAC {

    private static final int B = 64;

    private byte[] k_ipad = null;

    private byte[] k_opad = null;

    private MessageDigest md = null;

    private int bsize = 0;

    protected void setH(MessageDigest md) {
        this.md = md;
        bsize = md.getDigestLength();
    }

    public int getBlockSize() {
        return bsize;
    }

    ;

    public void init(byte[] key) throws Exception {
        if (key.length > bsize) {
            byte[] tmp = new byte[bsize];
            System.arraycopy(key, 0, tmp, 0, bsize);
            key = tmp;
        }
        if (key.length > B) {
            md.update(key, 0, key.length);
            key = md.digest();
        }
        k_ipad = new byte[B];
        System.arraycopy(key, 0, k_ipad, 0, key.length);
        k_opad = new byte[B];
        System.arraycopy(key, 0, k_opad, 0, key.length);
        for (int i = 0; i < B; i++) {
            k_ipad[i] ^= (byte) 0x36;
            k_opad[i] ^= (byte) 0x5c;
        }
        md.update(k_ipad, 0, B);
    }

    private final byte[] tmp = new byte[4];

    public void update(int i) {
        tmp[0] = (byte) (i >>> 24);
        tmp[1] = (byte) (i >>> 16);
        tmp[2] = (byte) (i >>> 8);
        tmp[3] = (byte) i;
        update(tmp, 0, 4);
    }

    public void update(byte foo[], int s, int l) {
        md.update(foo, s, l);
    }

    public void doFinal(byte[] buf, int offset) {
        byte[] result = md.digest();
        md.update(k_opad, 0, B);
        md.update(result, 0, bsize);
        try {
            md.digest(buf, offset, bsize);
        } catch (Exception e) {
        }
        md.update(k_ipad, 0, B);
    }
}
