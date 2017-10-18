package org.apache.harmony.security.provider.crypto;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.InvalidKeyException;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.MessageDigest;
import org.apache.harmony.security.internal.nls.Messages;

public class SHA1withDSA_SignatureImpl extends Signature {

    private MessageDigest msgDigest;

    private DSAKey dsaKey;

    /**
     * The solo constructor.
     */
    public SHA1withDSA_SignatureImpl() throws NoSuchAlgorithmException {
        super("SHA1withDSA");
        msgDigest = MessageDigest.getInstance("SHA1");
    }

    /**
     * Deprecated method.
     *
     * @return
     *    null
     */
    protected Object engineGetParameter(String param) throws InvalidParameterException {
        if (param == null) {
            throw new NullPointerException(Messages.getString("security.01"));
        }
        return null;
    }

    /**
     * Initializes this signature object with PrivateKey object 
     * passed as argument to the method.
     *
     * @param
     *    privateKey DSAPrivateKey object
     * @throws
     *    InvalidKeyException if privateKey is not DSAPrivateKey object
     */
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        DSAParams params;
        BigInteger p, q, x;
        int n;
        if (privateKey == null || !(privateKey instanceof DSAPrivateKey)) {
            throw new InvalidKeyException(Messages.getString("security.168"));
        }
        params = ((DSAPrivateKey) privateKey).getParams();
        p = params.getP();
        q = params.getQ();
        x = ((DSAPrivateKey) privateKey).getX();
        n = p.bitLength();
        if (p.compareTo(BigInteger.valueOf(1)) != 1 || n < 512 || n > 1024 || (n & 077) != 0) {
            throw new InvalidKeyException(Messages.getString("security.169"));
        }
        if (q.signum() != 1 && q.bitLength() != 160) {
            throw new InvalidKeyException(Messages.getString("security.16A"));
        }
        if (x.signum() != 1 || x.compareTo(q) != -1) {
            throw new InvalidKeyException(Messages.getString("security.16B"));
        }
        dsaKey = (DSAKey) privateKey;
        msgDigest.reset();
    }

    /**
     * Initializes this signature object with PublicKey object 
     * passed as argument to the method.
     *
     * @param
     *    publicKey DSAPublicKey object
     * @throws
     *    InvalidKeyException if publicKey is not DSAPublicKey object
     */
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        BigInteger p, q, y;
        int n1;
        if (publicKey == null || !(publicKey instanceof DSAPublicKey)) {
            throw new InvalidKeyException(Messages.getString("security.16C"));
        }
        DSAParams params = ((DSAPublicKey) publicKey).getParams();
        p = params.getP();
        q = params.getQ();
        y = ((DSAPublicKey) publicKey).getY();
        n1 = p.bitLength();
        if (p.compareTo(BigInteger.valueOf(1)) != 1 || n1 < 512 || n1 > 1024 || (n1 & 077) != 0) {
            throw new InvalidKeyException(Messages.getString("security.169"));
        }
        if (q.signum() != 1 || q.bitLength() != 160) {
            throw new InvalidKeyException(Messages.getString("security.16A"));
        }
        if (y.signum() != 1) {
            throw new InvalidKeyException(Messages.getString("security.16D"));
        }
        dsaKey = (DSAKey) publicKey;
        msgDigest.reset();
    }

    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        if (param == null) {
            throw new NullPointerException(Messages.getString("security.83", "param"));
        }
        throw new InvalidParameterException(Messages.getString("security.16E"));
    }

    /**
     * Returns signature bytes as byte array containing 
     * ASN1 representation for two BigInteger objects
     * which is SEQUENCE of two INTEGERS.
     * Length of sequence varies from less than 46 to 48.
     *
     * Resets object to the state it was in 
     * when previous call to either "initSign" method was called.
     *
     * @return
     *    byte array containing signature in ASN1 representation
     * @throws
     *    SignatureException if object's state is not SIGN or 
     *                       signature algorithm cannot process data
     */
    protected byte[] engineSign() throws SignatureException {
        BigInteger r = null;
        BigInteger s = null;
        BigInteger k = null;
        BigInteger p, q, g, x;
        BigInteger digestBI;
        byte randomBytes[];
        byte rBytes[], sBytes[], signature[];
        int n, n1, n2;
        DSAParams params;
        if (appRandom == null) {
            appRandom = new SecureRandom();
        }
        params = dsaKey.getParams();
        p = params.getP();
        q = params.getQ();
        g = params.getG();
        x = ((DSAPrivateKey) dsaKey).getX();
        digestBI = new BigInteger(1, msgDigest.digest());
        randomBytes = new byte[20];
        for (; ; ) {
            appRandom.nextBytes(randomBytes);
            k = new BigInteger(1, randomBytes);
            if (k.compareTo(q) != -1) {
                continue;
            }
            r = g.modPow(k, p).mod(q);
            if (r.signum() == 0) {
                continue;
            }
            s = k.modInverse(q).multiply(digestBI.add(x.multiply(r)).mod(q)).mod(q);
            if (s.signum() != 0) {
                break;
            }
        }
        rBytes = r.toByteArray();
        n1 = rBytes.length;
        if ((rBytes[0] & 0x80) != 0) {
            n1++;
        }
        sBytes = s.toByteArray();
        n2 = sBytes.length;
        if ((sBytes[0] & 0x80) != 0) {
            n2++;
        }
        signature = new byte[6 + n1 + n2];
        signature[0] = (byte) 0x30;
        signature[1] = (byte) (4 + n1 + n2);
        signature[2] = (byte) 0x02;
        signature[3] = (byte) n1;
        signature[4 + n1] = (byte) 0x02;
        signature[5 + n1] = (byte) n2;
        if (n1 == rBytes.length) {
            n = 4;
        } else {
            n = 5;
        }
        System.arraycopy(rBytes, 0, signature, n, rBytes.length);
        if (n2 == sBytes.length) {
            n = 6 + n1;
        } else {
            n = 7 + n1;
        }
        System.arraycopy(sBytes, 0, signature, n, sBytes.length);
        return signature;
    }

    /**
     * Updates data to sign or to verify.
     *
     * @param
     *    b byte to update
     * @throws
     *    SignatureException if object was not initialized for signing or verifying
     */
    protected void engineUpdate(byte b) throws SignatureException {
        msgDigest.update(b);
    }

    /**
     * Updates data to sign or to verify.
     *
     * @param
     *    b byte array containing bytes to update
     * @param
     *    off offset in byte array to start from
     * @param
     *    len number of bytes to use for updating
     * @throws
     *    SignatureException if object was not initialized for signing or verifying
     */
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        msgDigest.update(b, off, len);
    }

    private boolean checkSignature(byte[] sigBytes, int offset, int length) throws SignatureException {
        BigInteger r, s, w;
        BigInteger u1, u2, v;
        BigInteger p, q, g, y;
        DSAParams params;
        int n1, n2;
        byte bytes[];
        byte digest[];
        try {
            byte dummy;
            n1 = sigBytes[offset + 3];
            n2 = sigBytes[offset + n1 + 5];
            if (sigBytes[offset + 0] != 0x30 || sigBytes[offset + 2] != 2 || sigBytes[offset + n1 + 4] != 2 || sigBytes[offset + 1] != (n1 + n2 + 4) || n1 > 21 || n2 > 21 || (length != 0 && (sigBytes[offset + 1] + 2) > length)) {
                throw new SignatureException(Messages.getString("security.16F"));
            }
            dummy = sigBytes[5 + n1 + n2];
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new SignatureException(Messages.getString("security.170"));
        }
        digest = msgDigest.digest();
        bytes = new byte[n1];
        System.arraycopy(sigBytes, offset + 4, bytes, 0, n1);
        r = new BigInteger(bytes);
        bytes = new byte[n2];
        System.arraycopy(sigBytes, offset + 6 + n1, bytes, 0, n2);
        s = new BigInteger(bytes);
        params = dsaKey.getParams();
        p = params.getP();
        q = params.getQ();
        g = params.getG();
        y = ((DSAPublicKey) dsaKey).getY();
        if (r.signum() != 1 || r.compareTo(q) != -1 || s.signum() != 1 || s.compareTo(q) != -1) {
            return false;
        }
        w = s.modInverse(q);
        u1 = (new BigInteger(1, digest)).multiply(w).mod(q);
        u2 = r.multiply(w).mod(q);
        v = g.modPow(u1, p).multiply(y.modPow(u2, p)).mod(p).mod(q);
        if (v.compareTo(r) != 0) {
            return false;
        }
        return true;
    }

    /**
     * Verifies the signature bytes.
     *
     * @param
     *    sigBytes byte array with signature bytes to verify.
     * @return
     *    true if signature bytes were verified, false otherwise
     * @throws
     *    SignatureException if object's state is not VERIFY or
     *                       signature format is not ASN1 representation or
     *                       signature algorithm cannot process data
     */
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (sigBytes == null) {
            throw new NullPointerException(Messages.getString("security.83", "sigBytes"));
        }
        return checkSignature(sigBytes, 0, 0);
    }

    /**
     * Verifies the signature bytes.
     *
     * @param
     *    sigBytes byte array with signature bytes to verify.
     * @param
     *    offset index in sigBytes to start from
     * @param
     *    length number of bytes allotted for signature
     * @return
     *    true if signature bytes were verified, false otherwise
     * @throws
     *    SignatureException if object's state is not VERIFY or
     *                       signature format is not ASN1 representation or
     *                       signature algorithm cannot process data
     */
    protected boolean engineVerify(byte[] sigBytes, int offset, int length) throws SignatureException {
        return checkSignature(sigBytes, offset, length);
    }
}
