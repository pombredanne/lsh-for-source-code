package cdc.standard.rsa;

import codec.asn1.*;
import codec.pkcs1.*;
import codec.x509.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
* This class implements the RSASSA-PSS algorithm as defined in
* <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-1/index.html>PKCS#1 version 2.1 </a>.
* <p>
*
* The signature is an ASN1 DigestInfo structure.
* <p>
*
* On an AMD Athlon 550 MHz the time for signing a message is about 120 ms and for verifying
* a signature about 1 ms.
* <p>
*
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version 0.3
*/
public class RSASignature extends SignatureSpi {

    /**
	* The source of randomness.
	*/
    private SecureRandom secureRandom_;

    /**
	* The message digest - here SHA-1.
	*/
    private MessageDigest md_;

    /**
	* A BigInteger with the value 3.
	*/
    private static final BigInteger THREE = BigInteger.valueOf(3);

    /**
	* The size of a cipherblock.
	*/
    private int cipherBlockSize_;

    /**
	* The blocksize.
	*/
    private int blockSize_;

    /**
	* A reference to the RSA public key.
	*/
    private RSAPublicKey rsaPublicKey_;

    /**
	* A reference to the RSA private key.
	*/
    private RSAPrivateKey rsaPrivateKey_;

    /**
	* The ByteArrayOutputStream collects the input bytes (the message).
	*/
    private ByteArrayOutputStream baos_;

    /**
	* The RSASSA_PSS OID.
	*/
    private ASN1ObjectIdentifier rsaSsaPssOid_ = null;

    /**
	* The RSASSA_PSS AlgorithmIdentifier.
	*/
    private AlgorithmIdentifier rsaSsaPssAid_ = null;

    /**
	* The standard constructor tries to generate the RSA-SSA-PSS AlgorithmIdentifier
	* (with the Object ID 1.2.840.113549.1.1.10).
	*/
    public RSASignature() {
        try {
            rsaSsaPssOid_ = new ASN1ObjectIdentifier("1.2.840.113549.1.1.10");
            rsaSsaPssAid_ = new AlgorithmIdentifier(rsaSsaPssOid_, new ASN1Null());
        } catch (Exception e) {
            System.out.println("shouldnt happen:" + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
	* This function does nothing.
	* <p>
	* @deprecated Not implemented.
	*/
    protected Object engineGetParameter(String parameter) throws InvalidParameterException {
        return null;
    }

    /**
	* Initializes the signature algorithm for signing a message.
	* <p>
	* @param privateKey the private key of the signer.
	* @throws InvalidKeyException if the key is not an instance of RSAPrivateKey or RSAPrivateCrtKey.
	*/
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        secureRandom_ = new SecureRandom();
        try {
            md_ = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: SHA-1 algorithm not found");
            nsae.printStackTrace();
        }
        if (privateKey instanceof RSAPrivateCrtKey || privateKey instanceof RSAPrivateKey) {
            rsaPrivateKey_ = (RSAPrivateKey) privateKey;
            cipherBlockSize_ = ((rsaPrivateKey_.getModulus().bitLength()) + 7) / 8;
            blockSize_ = cipherBlockSize_ - 2 * (md_.getDigestLength()) - 2;
            baos_ = new ByteArrayOutputStream();
        } else {
            throw new InvalidKeyException("key is not a RSAPrivateKey");
        }
    }

    /**
	* Initializes the signature algorithm for signing a message.
	* <p>
	* @param privateKey the private key of the signer.
	* @param secureRandom the source of randomness.
	* @throws InvalidKeyException if the key is not an instance of RSAPrivateKey or RSAPrivateCrtKey.
	*/
    protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        secureRandom_ = secureRandom;
        try {
            md_ = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: SHA-1 algorithm not found");
            nsae.printStackTrace();
        }
        if (privateKey instanceof RSAPrivateCrtKey || privateKey instanceof RSAPrivateKey) {
            rsaPrivateKey_ = (RSAPrivateKey) privateKey;
            cipherBlockSize_ = ((rsaPrivateKey_.getModulus().bitLength()) + 7) / 8;
            blockSize_ = cipherBlockSize_ - 2 * (md_.getDigestLength()) - 2;
            baos_ = new ByteArrayOutputStream();
        } else {
            throw new InvalidKeyException("key is not a RSAPrivateKey");
        }
    }

    /**
	* Initializes the signature algorithm for verifing a signature.
	* <p>
	* @param publicKey the public key of the signer.
	* @throws InvalidKeyException if the public key is not an instance of RSAPublicKey.
	*/
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        try {
            md_ = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: SHA-1 algorithm not found");
            nsae.printStackTrace();
        }
        if (publicKey instanceof RSAPublicKey) {
            rsaPublicKey_ = (RSAPublicKey) publicKey;
            cipherBlockSize_ = ((rsaPublicKey_.getModulus().bitLength()) + 7) / 8;
            blockSize_ = cipherBlockSize_ - 2 * (md_.getDigestLength()) - 2;
            baos_ = new ByteArrayOutputStream();
        } else {
            throw new InvalidKeyException("key is not a RSAPublicKey");
        }
    }

    /**
		 * This function does nothing.
		 * <p>
		 *
		 * @deprecated Not implemented.
		*/
    protected void engineSetParameter(String parameter, Object value) throws InvalidParameterException {
    }

    /**
	* Signs a message.
	* <p>
	* @return the signature.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected byte[] engineSign() throws SignatureException {
        int digestLength = md_.getDigestLength();
        byte[] salt = new byte[digestLength];
        secureRandom_.nextBytes(salt);
        byte[] message = baos_.toByteArray();
        try {
            baos_.close();
        } catch (IOException ioe) {
            System.out.println("shouldn't happen");
            ioe.printStackTrace();
        }
        md_.update(salt);
        byte[] h = md_.digest(message);
        byte[] ps = new byte[cipherBlockSize_ - 1 - 2 * digestLength];
        byte[] db = new byte[cipherBlockSize_ - 1 - digestLength];
        System.arraycopy(salt, 0, db, 0, digestLength);
        System.arraycopy(ps, 0, db, digestLength, cipherBlockSize_ - 1 - 2 * digestLength);
        byte[] dbMask = mgf(h, cipherBlockSize_ - 1 - digestLength);
        byte[] maskedDB = xor(db, dbMask);
        byte[] em = new byte[cipherBlockSize_ - 1];
        System.arraycopy(h, 0, em, 0, digestLength);
        System.arraycopy(maskedDB, 0, em, digestLength, cipherBlockSize_ - 1 - digestLength);
        BigInteger m = new BigInteger(1, em);
        BigInteger c;
        if (rsaPrivateKey_ instanceof RSAPrivateKey) {
            BigInteger n = rsaPrivateKey_.getModulus();
            BigInteger d = rsaPrivateKey_.getPrivateExponent();
            c = m.modPow(d, n);
        } else {
            RSAPrivateCrtKey rsaPrivCrtKey = (RSAPrivateCrtKey) rsaPrivateKey_;
            BigInteger d = rsaPrivCrtKey.getPrivateExponent();
            BigInteger p = rsaPrivCrtKey.getPrimeP();
            BigInteger q = rsaPrivCrtKey.getPrimeQ();
            BigInteger dP = rsaPrivCrtKey.getPrimeExponentP();
            BigInteger dQ = rsaPrivCrtKey.getPrimeExponentQ();
            BigInteger qInv = rsaPrivCrtKey.getCrtCoefficient();
            BigInteger m_1, m_2, hBI;
            m_1 = (m.remainder(p)).modPow(dP, p);
            m_2 = (m.remainder(q)).modPow(dQ, q);
            hBI = (qInv.multiply((m_1.subtract(m_2)).remainder(p))).mod(p);
            c = ((hBI.multiply(q)).add(m_2));
        }
        return getBytes(c);
    }

    /**
	* Signs the message and stores the signature in the provided buffer.
	* <p>
	* @param outbuffer buffer for the signature result.
	* @param offset offset in outbuffer where the signature is stored.
	* @param length number of bytes within outbuffer allotted for the signature.
	*
	* @return the number of bytes placed into outbuffer, if length >= signature.length.
	* If length < signature.length then outbuffer is leaved unchanged and 0 is returned.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected int engineSign(byte[] outbuffer, int offset, int length) throws SignatureException {
        byte[] c = engineSign();
        if (length >= c.length) {
            System.arraycopy(c, 0, outbuffer, offset, c.length);
            return c.length;
        } else {
            return 0;
        }
    }

    /**
	* Writes length bytes beginning at offset into the ByteArrayOutputStream.
	* <p>
	* @param b The message byte.
	* @param offset The index, where the message bytes starts.
	* @param length The number of message bytes.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected void engineUpdate(byte[] b, int offset, int length) throws SignatureException {
        baos_.write(b, offset, length);
    }

    /**
	* Writes a byte into the ByteArrayOutputStream.
	* <p>
	* @param b the message byte.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected void engineUpdate(byte b) throws SignatureException {
        baos_.write(b);
    }

    /**
	* Verifies a signature.
	* <p>
	* @param signature the signature to be verified.
	* @return true if the signature is correct - false otherwise.
	*/
    protected boolean engineVerify(byte[] signature) {
        int digestLength = md_.getDigestLength();
        BigInteger c = new BigInteger(1, signature);
        BigInteger n = rsaPublicKey_.getModulus();
        BigInteger e = rsaPublicKey_.getPublicExponent();
        BigInteger message;
        if (c.compareTo(n) >= 0) {
            return false;
        }
        if (e.equals(THREE)) {
            message = (((c.multiply(c)).mod(n)).multiply(c)).mod(n);
        } else {
            message = c.modPow(e, n);
        }
        byte[] em = getBytes(message);
        byte[] h = new byte[digestLength];
        System.arraycopy(em, 0, h, 0, digestLength);
        byte[] maskedDB = new byte[em.length - digestLength];
        System.arraycopy(em, digestLength, maskedDB, 0, em.length - digestLength);
        byte[] dbMask = mgf(h, em.length - digestLength);
        byte[] db = xor(maskedDB, dbMask);
        byte[] salt = new byte[digestLength];
        System.arraycopy(db, 0, salt, 0, digestLength);
        byte[] ps = new byte[em.length - 2 * digestLength];
        System.arraycopy(db, digestLength, ps, 0, em.length - 2 * digestLength);
        for (int i = 0; i < (em.length - 2 * digestLength); i++) {
            if (ps[i] != 0x00) {
                return false;
            }
        }
        byte[] msg = baos_.toByteArray();
        try {
            baos_.close();
        } catch (IOException ioe) {
            System.out.println("shouldn't happen");
            ioe.printStackTrace();
        }
        md_.update(salt);
        byte[] hs = md_.digest(msg);
        for (int i = 0; i < digestLength; i++) {
            if (hs[i] != h[i]) {
                return false;
            }
        }
        return true;
    }

    /**
	* A little helper method. It cuts off the first byte containing the sign of the BigInteger,
	* when it needs an extra byte,
	* so that the BigInteger matches into the destination array.
	* <p>
	* @param bigInt the BigInteger
	* @return the byte array containing the (modified) BigIntegers byte representation.
	*/
    protected byte[] getBytes(BigInteger bigInt) {
        byte[] bigIntBytes = bigInt.toByteArray();
        if ((bigInt.bitLength() % 8) != 0) {
            return bigIntBytes;
        } else {
            byte[] smallerBytes = new byte[bigInt.bitLength() / 8];
            System.arraycopy(bigIntBytes, 1, smallerBytes, 0, smallerBytes.length);
            return smallerBytes;
        }
    }

    /**
	* The mask generating function as defined in PKCS#1 version 2.1 chapter B.2.1 .
	*
	* @param seed a sequence of random bytes.
	* @param length the length of the returned mask.
	*
	* @return the mask.
	*/
    private byte[] mgf(byte[] seed, int length) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] c = new byte[4];
        int end = length / md_.getDigestLength();
        for (int counter = 0; counter <= end; counter++) {
            int tmp = counter;
            c[3] = (byte) tmp;
            tmp = tmp >> 8;
            c[2] = (byte) tmp;
            tmp = tmp >> 8;
            c[1] = (byte) tmp;
            tmp = tmp >> 8;
            c[0] = (byte) tmp;
            md_.update(seed);
            try {
                baos.write(md_.digest(c));
            } catch (IOException ioe) {
                System.out.println("shouldn't happen");
                ioe.printStackTrace();
            }
        }
        try {
            baos.flush();
        } catch (IOException ioe) {
            System.out.println("shouldn't happen");
            ioe.printStackTrace();
        }
        byte[] t = baos.toByteArray();
        byte[] out = new byte[length];
        try {
            baos.close();
        } catch (IOException ioe) {
            System.out.println("shouldn't happen");
            ioe.printStackTrace();
        }
        System.arraycopy(t, 0, out, 0, length);
        return out;
    }

    /**
	* A little helper method. The input arrays srcOne and srcTwo
	* must be of the same length. The result is srcOne xor srcTwo.
	*
	* @param srcOne the first bytearray.
	* @param srcTwo the second bytearray.
	*
	* @return srcOne xor srcTwo
	*/
    private byte[] xor(byte[] srcOne, byte[] srcTwo) {
        byte[] out = new byte[srcOne.length];
        for (int i = 0; i < srcOne.length; i++) {
            out[i] = (byte) (srcOne[i] ^ srcTwo[i]);
        }
        return out;
    }
}
