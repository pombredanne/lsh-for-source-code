package net.sf.jnclib.tp.ssh2.crai;

import java.math.BigInteger;
import net.sf.jnclib.tp.ssh2.crai.cipher.Blowfish;
import net.sf.jnclib.tp.ssh2.crai.cipher.CBC;
import net.sf.jnclib.tp.ssh2.crai.cipher.CTR;
import net.sf.jnclib.tp.ssh2.crai.cipher.IBlockCipher;
import net.sf.jnclib.tp.ssh2.crai.cipher.IMode;
import net.sf.jnclib.tp.ssh2.crai.cipher.NullCipher;
import net.sf.jnclib.tp.ssh2.crai.cipher.Rijndael;
import net.sf.jnclib.tp.ssh2.crai.cipher.TripleDES;
import net.sf.jnclib.tp.ssh2.crai.hash.HMac;
import net.sf.jnclib.tp.ssh2.crai.hash.IMac;
import net.sf.jnclib.tp.ssh2.crai.hash.IMessageDigest;
import net.sf.jnclib.tp.ssh2.crai.hash.MD5;
import net.sf.jnclib.tp.ssh2.crai.hash.Sha160;
import net.sf.jnclib.tp.ssh2.crai.hash.Sha256;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Random;

/**
 * Default Crai implementation that just wraps classes.
 */
public class CraiImpl implements Crai {

    private static class OurRandom extends Random implements CraiRandom {

        private static byte[] buf;

        private static byte boff = 0;

        private static Sha160 md = new Sha160();

        private static BigInteger jk;

        public OurRandom() {
            synchronized (md) {
                if (buf == null) {
                    md.update(("" + System.currentTimeMillis()).getBytes());
                    buf = md.digest();
                    jk = new BigInteger(1, buf);
                }
            }
        }

        public void getBytes(byte[] b) {
            for (int i = 0; i < b.length; i++) {
                b[i] = getByte();
            }
        }

        public byte[] getBytes(int len) {
            byte[] buf = new byte[len];
            getBytes(buf);
            return buf;
        }

        public short getShort() {
            return (short) (getByte() << 8 | getByte());
        }

        public int getInt() {
            return (int) (getShort() << 16 | getShort());
        }

        public byte getByte() {
            synchronized (md) {
                boff++;
                if (boff % buf.length == 0) {
                    BigInteger Z = BigInteger.ONE.shiftLeft(buf.length * 9).subtract(BigInteger.ONE);
                    jk = jk.modPow(BigInteger.valueOf(3141526L), Z).shiftRight(9);
                    boff = 0;
                    md.update(buf);
                    md.update(jk.toByteArray());
                    buf = md.digest();
                }
                return buf[boff];
            }
        }
    }

    private class OurPrivateDSAKey implements CraiPrivateKey {

        public OurPrivateDSAKey(BigInteger x, BigInteger p, BigInteger q, BigInteger g) {
            mX = x;
            mP = p;
            mQ = q;
            mG = g;
        }

        public byte[] sign(byte[] b, int off, int len) throws CraiException {
            try {
                IMessageDigest md = new Sha160();
                md.update(b, off, len);
                byte[] sig = md.digest();
                BigInteger[] rs = computeRS(sig);
                byte[] rb = rs[0].toByteArray();
                byte[] sb = rs[1].toByteArray();
                sig = new byte[40];
                System.arraycopy(rb, rb.length - 20, sig, 0, 20);
                System.arraycopy(sb, sb.length - 20, sig, 20, 20);
                return sig;
            } catch (Exception e) {
                throw new CraiException("error performing DSA signature: " + e);
            }
        }

        private BigInteger[] computeRS(final byte[] digestBytes) {
            final BigInteger m = new BigInteger(1, digestBytes);
            BigInteger k, r, s;
            final byte[] kb = new byte[20];
            while (true) {
                mCraiRandom.getBytes(kb);
                k = new BigInteger(1, kb);
                k.clearBit(159);
                r = mG.modPow(k, mP).mod(mQ);
                if (r.equals(BigInteger.ZERO) || r.bitLength() >= 160) {
                    continue;
                }
                s = m.add(mX.multiply(r)).multiply(k.modInverse(mQ)).mod(mQ);
                if (s.equals(BigInteger.ZERO) || s.bitLength() >= 160) {
                    continue;
                }
                break;
            }
            return new BigInteger[] { r, s };
        }

        public CraiPrivateKey.Contents getContents() {
            return new CraiPrivateKey.DSAContents() {

                public BigInteger getP() {
                    return mP;
                }

                public BigInteger getQ() {
                    return mQ;
                }

                public BigInteger getG() {
                    return mG;
                }

                public BigInteger getX() {
                    return mX;
                }
            };
        }

        private BigInteger mX;

        private BigInteger mP;

        private BigInteger mQ;

        private BigInteger mG;
    }

    private class OurPublicDSAKey implements CraiPublicKey {

        public OurPublicDSAKey(BigInteger y, BigInteger p, BigInteger q, BigInteger g) {
            mY = y;
            mP = p;
            mQ = q;
            mG = g;
        }

        private boolean checkRS(final BigInteger r, final BigInteger s, final byte[] digestBytes) {
            final BigInteger w = s.modInverse(mQ);
            final BigInteger u1 = w.multiply(new BigInteger(1, digestBytes)).mod(mQ);
            final BigInteger u2 = r.multiply(w).mod(mQ);
            final BigInteger v = mG.modPow(u1, mP).multiply(mY.modPow(u2, mP)).mod(mP).mod(mQ);
            return v.equals(r);
        }

        public boolean verify(byte[] data, int off, int len, byte[] signature) throws CraiException {
            try {
                byte[] arg = new byte[20];
                System.arraycopy(signature, 0, arg, 0, 20);
                final BigInteger r = new BigInteger(1, arg);
                System.arraycopy(signature, 20, arg, 0, 20);
                final BigInteger s = new BigInteger(1, arg);
                IMessageDigest md = new Sha160();
                md.update(data, off, len);
                byte[] sig = md.digest();
                return checkRS(r, s, sig);
            } catch (Exception e) {
                throw new CraiException("error verifying DSA signature: " + e);
            }
        }

        public CraiPublicKey.Contents getContents() {
            return new CraiPublicKey.DSAContents() {

                public BigInteger getP() {
                    return mP;
                }

                public BigInteger getQ() {
                    return mQ;
                }

                public BigInteger getG() {
                    return mG;
                }

                public BigInteger getY() {
                    return mY;
                }
            };
        }

        private BigInteger mY;

        private BigInteger mP;

        private BigInteger mQ;

        private BigInteger mG;
    }

    private static class OurDigest implements CraiDigest {

        public OurDigest(IMessageDigest d) {
            mDigest = d;
        }

        public void reset() {
            mDigest.reset();
        }

        public void update(byte[] data, int off, int len) {
            mDigest.update(data, off, len);
        }

        public byte[] finish() {
            return mDigest.digest();
        }

        public void finish(byte[] out, int off) throws CraiException {
            try {
                byte[] buf = mDigest.digest();
                System.arraycopy(buf, 0, out, off, buf.length);
            } catch (Exception x) {
                throw new CraiException(x.toString());
            }
        }

        private IMessageDigest mDigest;
    }

    private static class OurHMAC implements CraiDigest {

        public OurHMAC(IMac mac) {
            mMac = mac;
        }

        public void reset() {
            mMac.reset();
        }

        public void update(byte[] data, int off, int len) {
            mMac.update(data, off, len);
        }

        public byte[] finish() {
            return mMac.digest();
        }

        public void finish(byte[] out, int off) throws CraiException {
            try {
                byte[] buf = mMac.digest();
                System.arraycopy(buf, 0, out, off, buf.length);
            } catch (Exception x) {
                throw new CraiException(x.toString());
            }
        }

        private IMac mMac;
    }

    private static class OurCipher implements CraiCipher {

        public OurCipher(IBlockCipher c) throws CraiException {
            mCipher = c;
            mJavaName = mCipher.name();
        }

        public void initEncrypt(byte[] key, byte[] iv) throws CraiException {
            try {
                if (mCipher instanceof IMode) {
                    ((IMode) mCipher).initEncrypt(key, iv);
                } else {
                    mCipher.init(key);
                }
                mDoEncrypt = true;
            } catch (Exception x) {
                throw new CraiException("cipher " + mJavaName + " encrypt init error: " + x);
            }
        }

        public void initDecrypt(byte[] key, byte[] iv) throws CraiException {
            try {
                if (mCipher instanceof IMode) {
                    ((IMode) mCipher).initDecrypt(key, iv);
                } else {
                    mCipher.init(key);
                }
                mDoEncrypt = false;
            } catch (Exception x) {
                throw new CraiException("cipher " + mJavaName + " decrypt init error: " + x);
            }
        }

        public void process(byte[] in, int off, int len, byte[] out, int off_out) throws CraiException {
            try {
                while (len >= mCipher.currentBlockSize()) {
                    if (mCipher instanceof IMode) {
                        ((IMode) mCipher).update(in, off, out, off_out);
                    } else if (mDoEncrypt) {
                        mCipher.encryptBlock(in, off, out, off_out);
                    } else {
                        mCipher.decryptBlock(in, off, out, off_out);
                    }
                    len -= mCipher.currentBlockSize();
                    off += mCipher.currentBlockSize();
                    off_out += mCipher.currentBlockSize();
                }
                if (len > 0) {
                    throw new IllegalStateException("Buffer not multiple of CipherBlockSize: " + len);
                }
            } catch (Exception x) {
                throw new CraiException("cipher " + mJavaName + " process error: " + x);
            }
        }

        protected boolean mDoEncrypt;

        protected String mJavaName;

        protected IBlockCipher mCipher;
    }

    public CraiRandom getPRNG() {
        return mCraiRandom;
    }

    public CraiPrivateKey makePrivateDSAKey(BigInteger x, BigInteger p, BigInteger q, BigInteger g) {
        return new OurPrivateDSAKey(x, p, q, g);
    }

    public CraiPublicKey makePublicDSAKey(BigInteger y, BigInteger p, BigInteger q, BigInteger g) {
        return new OurPublicDSAKey(y, p, q, g);
    }

    private static final BigInteger KP_512_P = new BigInteger("fca682ce8e12caba26efccf7110e526db078b05edecbcd1eb4a208f3ae1617ae01f35b91a47e6df63413c5e12ed0899bcd132acd50d99151bdc43ee737592e17", 16);

    private static final BigInteger KP_512_Q = new BigInteger("962eddcc369cba8ebb260ee6b6a126d9346e38c5", 16);

    private static final BigInteger KP_512_G = new BigInteger("678471b27a9cf44ee91a49c5147db1a9aaf244f05a434d6486931d2d14271b9e35030b71fd73da179069b32e2935630e1c2062354d0da20a6c416e50be794ca4", 16);

    private static final BigInteger KP_1024_P = new BigInteger("fd7f53811d75122952df4a9c2eece4e7f611b7523cef4400c31e3f80b6512669455d402251fb593d8d58fabfc5f5ba30f6cb9b556cd7813b801d346ff26660b76b9950a5a49f9fe8047b1022c24fbba9d7feb7c61bf83b57e7c6a8a6150f04fb83f6d3c51ec3023554135a169132f675f3ae2b61d72aeff22203199dd14801c7", 16);

    private static final BigInteger KP_1024_Q = new BigInteger("9760508f15230bccb292b982a2eb840bf0581cf5", 16);

    private static final BigInteger KP_1024_G = new BigInteger("f7e1a085d69b3ddecbbcab5c36b857b97994afbbfa3aea82f9574c0b3d0782675159578ebad4594fe67107108180b449167123e84c281613b7cf09328cc8a6e13c167a8b547c8d28e0a3ae1e2bb3a675916ea37f0bfa213562f1fb627a01243bcca4f1bea8519089a883dfe15ae59f06928b665e807b552564014c3bfecf492a", 16);

    private static final BigInteger KP_4096_P = new BigInteger("0093e55e1c618c6538bd30a8eed5e86ecfcdc7ea6fab1bcdae2189b18d8724b861631bb5ed248fb4779dc5e5a51c32c3199a5f0202a37df8d71df20bea12fd97bb24869f50670c8dff6993ba47f6dd410c08193ef8a6903029c0dd3a63fe4656929ecd8f48417646a3eb31ddea0562436746b4b45aab5f175f86e01d15e28a49488a296b751e15c59d186fc0f04acaa32ed6a25c3bbaf1994229adb8c819e0c53fbd4e22cabc8259d9d42aa1aa40735f4475ca63a709c1037f20682bdbff75f483cf6f19d56bfc505aa12a3f13aa05bc1a5cb9307621fc244c326bb1c467a53774d892bc412b12abc798885629863604dea377bc2422cb6d6b93699b79fa28de05cc7176b134dffe78dfd02767021c47f2c960ca8464e13087ca524f6f241944b6d1687681ab7d67833a49ad3539d9eb4b290ab42f784e5166019192e26a1257e556c0ad946d36788f0ed737f3c590161e37be107f1704f71908ce774501355702b0693d8092d19f77eaa2a9aa51ae79eb87946dac512586da5b75965db0cc2d60506984f56d3454a05d9f5c513f62fd8d4b71d423c512c3453a3bf3099b590ae45fdd014d656f6fc88057da94a8ccb8457be6175d1ee0f395e922aa58c716e9e38b75b335f8f9fa1adf4837f30425820ec950ae92ae1b18fcf4b6ee226c44c479e909677a672b657ed12687984c6768a99a2c4d00969df8ea9981475c3b770f05", 16);

    private static final BigInteger KP_4096_Q = new BigInteger("0090c85060ba152861faba2914f271728157f880db", 16);

    private static final BigInteger KP_4096_G = new BigInteger("0f960968e8f1f036fbfdd3db8b88a195bb171bffbfda6920473be7ce018715dc15981b10f13f3a8350391d33978632f3ad14195cf2921205e2d60955d90a7c38619df080f96e6d6291d7bc368f5b923144e984e86abeac00775175db4ac830c413553ccd127520bfbcc6825dbd9ff06d8faa3a20168f7d35cd37d8eba149708eed1aa88dfddac13c4257be3a2d0038edacf15c09668dd5323dfa980ba9c3f5707316e8d2ffd288802491add5704a86d15ac8f7b448533edefa16395d7820105a7f4b6e1aa4fa74b77d483d41257c957560751a0da76940e2b1ea7d1891a4c46b19c4a23b2779a52bb1c41b39f1878a2069159721c86cc0fc5e37a0960f888424a33d0b99ff11085401ca81603bcea7a3d5661eb88dc0f48a8a67323046c43b427f3cc975b6557187427df8eb5cf14f303cdf167dfd005a902ad15853967553040c598a31da2e9d9a0575778666e5b36587eb2f45f29dbd2d75a816439a435e935113d5250c5cf5ee95a9361c636f4cb94cd5f7f76b09bf6128a220c0923de378bf5ac809225fe02d62ca73b18d6ab2b30e62ad3e7d277389a9034f092962ebfe9df9540d3206dc11ead7569b911c70c3097675152184cbd08105e24f156614fe6cbc1618eb38673d4a314061f967ca7e45e7b3ad1fbdd56108fe6c9a627ef69bff6d6dda7e2209203701dd93fd80835cfef3ffa55a29a85f8f2327d85070cbc0", 16);

    public CraiKeyPair generateDSAKeyPair(int bits) {
        BigInteger p, q, g, x, y;
        int use_bits;
        if (bits > 2048) {
            p = KP_4096_P;
            q = KP_4096_Q;
            g = KP_4096_G;
            use_bits = 4096;
        } else if (bits > 768) {
            p = KP_1024_P;
            q = KP_1024_Q;
            g = KP_1024_G;
            use_bits = 1024;
        } else {
            p = KP_512_P;
            q = KP_512_Q;
            g = KP_512_G;
            use_bits = 512;
        }
        x = nextX(q);
        y = g.modPow(x, p);
        return new CraiKeyPair(new OurPublicDSAKey(y, p, q, g), new OurPrivateDSAKey(x, p, q, g));
    }

    /** Initial SHS context. */
    private static final int[] T_SHS = new int[] { 0x67452301, 0xEFCDAB89, 0x98BADCFE, 0x10325476, 0xC3D2E1F0 };

    /** The BigInteger constant 2. */
    private static final BigInteger TWO = new BigInteger("2");

    private static final BigInteger TWO_POW_160 = TWO.pow(160);

    private static BigInteger XKEY = null;

    private synchronized BigInteger nextX(BigInteger q) {
        if (XKEY == null) {
            byte[] kb = new byte[20];
            getPRNG().getBytes(kb);
            XKEY = new BigInteger(1, kb).setBit(159).setBit(0);
        }
        byte[] xk = XKEY.toByteArray();
        byte[] in = new byte[64];
        System.arraycopy(xk, 0, in, 0, xk.length);
        int[] H = Sha160.G(T_SHS[0], T_SHS[1], T_SHS[2], T_SHS[3], T_SHS[4], in, 0);
        byte[] h = new byte[20];
        for (int i = 0, j = 0; i < 5; i++) {
            h[j++] = (byte) (H[i] >>> 24);
            h[j++] = (byte) (H[i] >>> 16);
            h[j++] = (byte) (H[i] >>> 8);
            h[j++] = (byte) H[i];
        }
        BigInteger result = new BigInteger(1, h).mod(q);
        XKEY = XKEY.add(result).add(BigInteger.ONE).mod(TWO_POW_160);
        return result;
    }

    public CraiDigest makeSHA1() {
        return new OurDigest(new Sha160());
    }

    public CraiDigest makeSHA256() {
        return new OurDigest(new Sha256());
    }

    public CraiDigest makeMD5() {
        return new OurDigest(new MD5());
    }

    public CraiDigest makeSHA1HMAC(byte[] key) {
        try {
            IMac mac = new HMac(new Sha160());
            mac.init(key);
            return new OurHMAC(mac);
        } catch (Exception x) {
            throw new RuntimeException("Unable to find SHA-1 HMAC algorithm: " + x.toString());
        }
    }

    public CraiDigest makeSHA256HMAC(byte[] key) {
        try {
            IMac mac = new HMac(new Sha256());
            mac.init(key);
            return new OurHMAC(mac);
        } catch (Exception x) {
            throw new RuntimeException("Unable to find SHA-256 HMAC algorithm: " + x.toString());
        }
    }

    public CraiDigest makeMD5HMAC(byte[] key) {
        try {
            IMac mac = new HMac(new MD5());
            mac.init(key);
            return new OurHMAC(mac);
        } catch (Exception x) {
            throw new RuntimeException("Unable to find MD5 HMAC algorithm: " + x.toString());
        }
    }

    public CraiCipher getCipher(CraiCipherAlgorithm algorithm) throws CraiException {
        if (algorithm == CraiCipherAlgorithm.DES3_CBC) {
            return new OurCipher(new CBC(new TripleDES(), TripleDES.BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.DES3_CTR) {
            return new OurCipher(new CTR(new TripleDES(), TripleDES.BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.AES_CBC) {
            return new OurCipher(new CBC(new Rijndael(), Rijndael.DEFAULT_BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.AES_CTR) {
            return new OurCipher(new CTR(new Rijndael(), Rijndael.DEFAULT_BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.BLOWFISH_CBC) {
            return new OurCipher(new CBC(new Blowfish(), Blowfish.DEFAULT_BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.BLOWFISH_CTR) {
            return new OurCipher(new CTR(new Blowfish(), Blowfish.DEFAULT_BLOCK_SIZE));
        } else if (algorithm == CraiCipherAlgorithm.NONE) {
            return new OurCipher(new NullCipher());
        } else {
            return null;
        }
    }

    public BigInteger modPow(BigInteger b, BigInteger e, BigInteger m) {
        return b.modPow(e, m);
    }

    public static synchronized byte[] randomBytes(int len) {
        byte[] buf = new byte[len];
        mCraiRandom.getBytes(buf);
        return buf;
    }

    protected static CraiRandom mCraiRandom = new OurRandom();

    @Override
    public CraiKeyPair generateRSAKeyPair(int strength) {
        BigInteger p, q, n, d, e, pSub1, qSub1, phi;
        int pbitlength = (strength + 1) / 2;
        int qbitlength = strength - pbitlength;
        int mindiffbits = strength / 3;
        e = BigInteger.valueOf(0x10001);
        for (; ; ) {
            p = new BigInteger(pbitlength, 1, (Random) mCraiRandom);
            if (p.mod(e).equals(BigInteger.ONE)) {
                continue;
            }
            if (!p.isProbablePrime(1024)) {
                continue;
            }
            if (e.gcd(p.subtract(BigInteger.ONE)).equals(BigInteger.ONE)) {
                break;
            }
        }
        for (; ; ) {
            for (; ; ) {
                q = new BigInteger(qbitlength, 1, (Random) mCraiRandom);
                if (q.subtract(p).abs().bitLength() < mindiffbits) {
                    continue;
                }
                if (q.mod(e).equals(BigInteger.ONE)) {
                    continue;
                }
                if (!q.isProbablePrime(1024)) {
                    continue;
                }
                if (e.gcd(q.subtract(BigInteger.ONE)).equals(BigInteger.ONE)) {
                    break;
                }
            }
            n = p.multiply(q);
            if (n.bitLength() == strength) {
                break;
            }
            p = p.max(q);
        }
        if (p.compareTo(q) < 0) {
            phi = p;
            p = q;
            q = phi;
        }
        pSub1 = p.subtract(BigInteger.ONE);
        qSub1 = q.subtract(BigInteger.ONE);
        phi = pSub1.multiply(qSub1);
        d = e.modInverse(phi);
        BigInteger dP, dQ, qInv;
        dP = d.remainder(pSub1);
        dQ = d.remainder(qSub1);
        qInv = q.modInverse(p);
        return new CraiKeyPair(new OurPublicRSAKey(n, e), new OurPrivateRSAKey(n, d, p, q));
    }

    @Override
    public CraiPrivateKey makePrivateRSAKey(BigInteger n, BigInteger d, BigInteger p, BigInteger q) {
        return new OurPrivateRSAKey(n, d, p, q);
    }

    @Override
    public CraiPublicKey makePublicRSAKey(BigInteger n, BigInteger e) {
        return new OurPublicRSAKey(n, e);
    }

    private class OurPrivateRSAKey implements CraiPrivateKey {

        public OurPrivateRSAKey(BigInteger n, BigInteger d, BigInteger p, BigInteger q) {
            mN = n;
            mD = d;
            mP = p;
            mQ = q;
        }

        public byte[] sign(byte[] b, int off, int len) throws CraiException {
            try {
                Signature s = Signature.getInstance("SHA1withRSA");
                KeyFactory keyFac = KeyFactory.getInstance("RSA");
                PrivateKey key = keyFac.generatePrivate(new RSAPrivateKeySpec(mN, mD));
                s.initSign(key);
                s.update(b, off, len);
                return s.sign();
            } catch (Exception e) {
                throw new CraiException("error performing RSA signature: " + e);
            }
        }

        public CraiPrivateKey.Contents getContents() {
            return new CraiPrivateKey.RSAContents() {

                public BigInteger getN() {
                    return mN;
                }

                public BigInteger getD() {
                    return mD;
                }

                public BigInteger getP() {
                    return mP;
                }

                public BigInteger getQ() {
                    return mQ;
                }
            };
        }

        private BigInteger mN;

        private BigInteger mD;

        private BigInteger mP;

        private BigInteger mQ;
    }

    private class OurPublicRSAKey implements CraiPublicKey {

        public OurPublicRSAKey(BigInteger n, BigInteger e) {
            mN = n;
            mE = e;
        }

        public boolean verify(byte[] data, int off, int len, byte[] signature) throws CraiException {
            try {
                Signature s = Signature.getInstance("SHA1withRSA");
                KeyFactory keyFac = KeyFactory.getInstance("RSA");
                PublicKey key = keyFac.generatePublic(new RSAPublicKeySpec(mN, mE));
                s.initVerify(key);
                s.update(data);
                return s.verify(signature);
            } catch (Exception e) {
                throw new CraiException("error verifying RSA signature: " + e);
            }
        }

        public CraiPublicKey.Contents getContents() {
            return new CraiPublicKey.RSAContents() {

                public BigInteger getN() {
                    return mN;
                }

                public BigInteger getE() {
                    return mE;
                }
            };
        }

        private BigInteger mN;

        private BigInteger mE;
    }
}
