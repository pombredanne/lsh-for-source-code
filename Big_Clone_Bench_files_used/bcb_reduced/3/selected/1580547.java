package com.sun.crypto.provider;

import java.util.Arrays;
import java.security.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import sun.security.internal.spec.TlsPrfParameterSpec;

/**
 * KeyGenerator implementation for the TLS PRF function.
 *
 * @author  Andreas Sterbenz
 * @since   1.6
 */
public final class TlsPrfGenerator extends KeyGeneratorSpi {

    private static final byte[] B0 = new byte[0];

    static final byte[] LABEL_MASTER_SECRET = { 109, 97, 115, 116, 101, 114, 32, 115, 101, 99, 114, 101, 116 };

    static final byte[] LABEL_KEY_EXPANSION = { 107, 101, 121, 32, 101, 120, 112, 97, 110, 115, 105, 111, 110 };

    static final byte[] LABEL_CLIENT_WRITE_KEY = { 99, 108, 105, 101, 110, 116, 32, 119, 114, 105, 116, 101, 32, 107, 101, 121 };

    static final byte[] LABEL_SERVER_WRITE_KEY = { 115, 101, 114, 118, 101, 114, 32, 119, 114, 105, 116, 101, 32, 107, 101, 121 };

    static final byte[] LABEL_IV_BLOCK = { 73, 86, 32, 98, 108, 111, 99, 107 };

    private static final byte[] HMAC_ipad = genPad((byte) 0x36, 64);

    private static final byte[] HMAC_opad = genPad((byte) 0x5c, 64);

    static final byte[][] SSL3_CONST = genConst();

    static byte[] genPad(byte b, int count) {
        byte[] padding = new byte[count];
        Arrays.fill(padding, b);
        return padding;
    }

    static byte[] concat(byte[] b1, byte[] b2) {
        int n1 = b1.length;
        int n2 = b2.length;
        byte[] b = new byte[n1 + n2];
        System.arraycopy(b1, 0, b, 0, n1);
        System.arraycopy(b2, 0, b, n1, n2);
        return b;
    }

    private static byte[][] genConst() {
        int n = 10;
        byte[][] arr = new byte[n][];
        for (int i = 0; i < n; i++) {
            byte[] b = new byte[i + 1];
            Arrays.fill(b, (byte) ('A' + i));
            arr[i] = b;
        }
        return arr;
    }

    private static final String MSG = "TlsPrfGenerator must be " + "initialized using a TlsPrfParameterSpec";

    private TlsPrfParameterSpec spec;

    public TlsPrfGenerator() {
    }

    protected void engineInit(SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    protected void engineInit(AlgorithmParameterSpec params, SecureRandom random) throws InvalidAlgorithmParameterException {
        if (params instanceof TlsPrfParameterSpec == false) {
            throw new InvalidAlgorithmParameterException(MSG);
        }
        this.spec = (TlsPrfParameterSpec) params;
        SecretKey key = spec.getSecret();
        if ((key != null) && ("RAW".equals(key.getFormat()) == false)) {
            throw new InvalidAlgorithmParameterException("Key encoding format must be RAW");
        }
    }

    protected void engineInit(int keysize, SecureRandom random) {
        throw new InvalidParameterException(MSG);
    }

    protected SecretKey engineGenerateKey() {
        if (spec == null) {
            throw new IllegalStateException("TlsPrfGenerator must be initialized");
        }
        SecretKey key = spec.getSecret();
        byte[] secret = (key == null) ? null : key.getEncoded();
        try {
            byte[] labelBytes = spec.getLabel().getBytes("UTF8");
            int n = spec.getOutputLength();
            byte[] prfBytes = doPRF(secret, labelBytes, spec.getSeed(), n);
            return new SecretKeySpec(prfBytes, "TlsPrf");
        } catch (GeneralSecurityException e) {
            throw new ProviderException("Could not generate PRF", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new ProviderException("Could not generate PRF", e);
        }
    }

    static final byte[] doPRF(byte[] secret, byte[] labelBytes, byte[] seed, int outputLength) throws NoSuchAlgorithmException, DigestException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha = MessageDigest.getInstance("SHA1");
        return doPRF(secret, labelBytes, seed, outputLength, md5, sha);
    }

    static final byte[] doPRF(byte[] secret, byte[] labelBytes, byte[] seed, int outputLength, MessageDigest md5, MessageDigest sha) throws DigestException {
        if (secret == null) {
            secret = B0;
        }
        int off = secret.length >> 1;
        int seclen = off + (secret.length & 1);
        byte[] output = new byte[outputLength];
        expand(md5, 16, secret, 0, seclen, labelBytes, seed, output);
        expand(sha, 20, secret, off, seclen, labelBytes, seed, output);
        return output;
    }

    private static final void expand(MessageDigest digest, int hmacSize, byte[] secret, int secOff, int secLen, byte[] label, byte[] seed, byte[] output) throws DigestException {
        byte[] pad1 = HMAC_ipad.clone();
        byte[] pad2 = HMAC_opad.clone();
        for (int i = 0; i < secLen; i++) {
            pad1[i] ^= secret[i + secOff];
            pad2[i] ^= secret[i + secOff];
        }
        byte[] tmp = new byte[hmacSize];
        byte[] aBytes = null;
        int remaining = output.length;
        int ofs = 0;
        while (remaining > 0) {
            digest.update(pad1);
            if (aBytes == null) {
                digest.update(label);
                digest.update(seed);
            } else {
                digest.update(aBytes);
            }
            digest.digest(tmp, 0, hmacSize);
            digest.update(pad2);
            digest.update(tmp);
            if (aBytes == null) {
                aBytes = new byte[hmacSize];
            }
            digest.digest(aBytes, 0, hmacSize);
            digest.update(pad1);
            digest.update(aBytes);
            digest.update(label);
            digest.update(seed);
            digest.digest(tmp, 0, hmacSize);
            digest.update(pad2);
            digest.update(tmp);
            digest.digest(tmp, 0, hmacSize);
            int k = Math.min(hmacSize, remaining);
            for (int i = 0; i < k; i++) {
                output[ofs++] ^= tmp[i];
            }
            remaining -= k;
        }
    }
}
