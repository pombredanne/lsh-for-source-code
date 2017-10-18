package net.sourceforge.epoint.pgp;

import java.io.*;
import java.security.*;
import java.math.BigInteger;
import net.sourceforge.epoint.io.PacketHeader;
import net.sourceforge.epoint.io.MPI;

/**
 * class for verifying RSA signatures
 * a workaround for Sun's buggy implementation
 *
 * @author <a href="mailto:nagydani@users.sourceforge.net">Daniel A. Nagy</a>
 */
public class Signature extends java.security.Signature {

    private MessageDigest md;

    private RSAPublicKey pubKey;

    protected Signature(String s) {
        super(s);
    }

    public static java.security.Signature getInstance(String algorithm) throws NoSuchAlgorithmException {
        String[] algo = algorithm.split("with");
        if (algo[1].equals("RSA")) {
            Signature s = new Signature(algorithm);
            s.md = MessageDigest.getInstance(algo[0]);
            return s;
        } else {
            return java.security.Signature.getInstance(algorithm);
        }
    }

    protected Object engineGetParameter(String param) throws InvalidParameterException {
        return null;
    }

    protected void engineSetParameter(java.security.spec.AlgorithmParameterSpec params) throws InvalidAlgorithmParameterException {
        return;
    }

    protected void engineSetParameter(String param, Object value) throws InvalidParameterException {
        return;
    }

    protected boolean engineVerify(byte[] sigBytes) {
        BigInteger sig = new BigInteger(1, sigBytes);
        byte[] m = sig.modPow(pubKey.getPublicExponent(), pubKey.getModulus()).toByteArray();
        byte[] hash = md.digest();
        int i = 1;
        if (m[0] != 1) return false;
        try {
            while (m[i] == (byte) 0xFF) i++;
        } catch (IndexOutOfBoundsException e) {
            return false;
        }
        if (m[i] != 0) return false;
        i++;
        if (m[i] != 0x30) return false;
        i++;
        if ((i + m[i]) != (m.length - 1)) return false;
        i++;
        if (m[i] != 0x30) return false;
        i++;
        i += m[i];
        i++;
        if (m[i] != 4) return false;
        i++;
        if (m[i] != hash.length) return false;
        if ((i + m[i]) != m.length - 1) return false;
        i++;
        int j = hash.length;
        while (j-- > 0) if (m[i + j] != hash[j]) return false;
        return true;
    }

    protected void engineUpdate(byte b) {
        md.update(b);
    }

    protected void engineUpdate(byte[] b, int off, int len) {
        md.update(b, off, len);
    }

    protected void engineInitVerify(java.security.PublicKey publicKey) throws InvalidKeyException {
        if (publicKey.getAlgorithm().equals("RSA")) pubKey = (RSAPublicKey) publicKey; else throw new InvalidKeyException("RSA only.");
    }

    protected byte[] engineSign() throws SignatureException {
        throw new SignatureException("Verification only.");
    }

    protected void engineInitSign(java.security.PrivateKey privateKey) throws InvalidKeyException {
        throw new InvalidKeyException("Verification only.");
    }
}
