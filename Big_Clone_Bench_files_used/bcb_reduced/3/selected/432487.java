package cdc.standard.dsa;

import codec.asn1.*;
import codec.pkcs1.*;
import codec.pkcs8.*;
import codec.x509.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.DSAParameterSpec;

/**
* This class implements the DSA signature algorithm as defined in
* <a href="http://csrc.nist.gov/fips/fips186-2.pdf">FIPS 186-2</a> with SHA-1 as
* message digest algorithm.
* <p>
* On an Athlon 550 Mhz the time for signing a message is about 20 ms and for verifying
* a signature about 41 ms.
* <p>
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version 0.91
*/
public class DSASignature extends SignatureSpi {

    /**
	* The public key for verifying a signature.
	*
	*/
    private DSAPublicKey dsaPublicKey_;

    /**
	* The private key for signing a message.
	*/
    private DSAPrivateKey dsaPrivateKey_;

    /**
	* The message digest algorithm (SHA-1).
	*/
    private MessageDigest md_;

    /**
	* The source of randomness.
	*/
    private SecureRandom secureRandom_;

    /**
	* The prime p, obtained from the DSAparameters of the key.
	*/
    private BigInteger p_;

    /**
	* The generator g, obtained from the DSAparameters of the key.
	*/
    private BigInteger g_;

    /**
	* The subprime q, obtained from the DSAparameters of the key.
	*/
    private BigInteger q_;

    /**
	* The DSA OID ("1.2.840.10040.4.3").
	*/
    private ASN1ObjectIdentifier dsaOid_ = null;

    /**
	* The DSA AlgorithmIdentifier.
	*/
    private AlgorithmIdentifier dsaAid_ = null;

    private static final char[] bcdLookup = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
	* The default constructor tries to initalize the DSA-AlgorithmIdentifier.
	*
	*
	*/
    public DSASignature() {
        try {
            dsaOid_ = new ASN1ObjectIdentifier("1.2.840.10040.4.3");
            dsaAid_ = new AlgorithmIdentifier(dsaOid_, new ASN1Null());
        } catch (ASN1Exception e) {
            System.out.println("shouldnt happen:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static final String bytesToHexStr(byte[] bcd) {
        StringBuffer s = new StringBuffer(bcd.length * 2);
        for (int i = 0; i < bcd.length; i++) {
            s.append(bcdLookup[(bcd[i] >>> 4) & 0x0f]);
            s.append(bcdLookup[bcd[i] & 0x0f]);
        }
        return s.toString();
    }

    /**
	* This function does nothing.
	*
	* @deprecated Not implemented.
	*/
    protected Object engineGetParameter(String parameter) throws InvalidParameterException {
        return null;
    }

    /**
	* Initializes the signature algorithm for signing a message.
	* Every private key that implements the DSAPrivateKey interface is accepted.
	* <p>
	* @param privateKey the private key of the signer.
	* @throws InvalidKeyException if the key is not an instance of DSAPrivKey.
	*/
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        if (privateKey instanceof DSAPrivateKey) {
            dsaPrivateKey_ = (DSAPrivateKey) privateKey;
            DSAParams parameter = (DSAParams) dsaPrivateKey_.getParams();
            p_ = parameter.getP();
            g_ = parameter.getG();
            q_ = parameter.getQ();
            secureRandom_ = new SecureRandom();
            try {
                md_ = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("DSASignature: SHA Algorithm not found");
            }
        } else {
            throw new InvalidKeyException("Error - PrivateKey is not an instance of DSAPrivateKey");
        }
    }

    /**
	* Initializes the signature algorithm for signing a message.
	* <p>
	* @param privateKey the private key of the signer.
	* @param secureRandom the source of randomness.
	* @throws InvalidKeyException if the key is not an instance of DSAPrivKey.
	*/
    protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        if (privateKey instanceof DSAPrivateKey) {
            dsaPrivateKey_ = (DSAPrivateKey) privateKey;
            DSAParams parameter = (DSAParams) dsaPrivateKey_.getParams();
            p_ = parameter.getP();
            g_ = parameter.getG();
            q_ = parameter.getQ();
            secureRandom_ = secureRandom;
            try {
                md_ = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("DSASignature: SHA Algorithm not found");
            }
        } else {
            throw new InvalidKeyException("Error - PrivateKey is not an instance of DSAPrivKey");
        }
    }

    /**
	* Initializes the signature algorithm for verifying a signature.
	* <p>
	* @param publicKey the public key of the signer.
	* @throws InvalidKeyException if the public key is not an instance of DSAPubKey.
	*/
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        if (publicKey instanceof DSAPublicKey) {
            dsaPublicKey_ = (DSAPublicKey) publicKey;
            DSAParams parameter = (DSAParams) dsaPublicKey_.getParams();
            p_ = parameter.getP();
            g_ = parameter.getG();
            q_ = parameter.getQ();
            try {
                md_ = MessageDigest.getInstance("SHA");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("DSASignature: SHA Algorithm not found");
            }
        } else {
            throw new InvalidKeyException("Error - PublicKey is not an instance of DSAPubKey");
        }
    }

    /**
	* This function does nothing.
	* @deprecated Not implemented.
	*/
    protected void engineSetParameter(String parameter, Object value) throws InvalidParameterException {
    }

    /**
	* Signs a message.
	* The result is an ASN1 Sequence containing the Integers r and s.
	* With r = (g<sup>k</sup> mod p) mod q
	* and s = (k<sup>-1</sup> (SHA(M) + x*r)) mod q.
	* <p>
	*
	* @see cdc.standard.dsa.ASN1DSASignature
	* @return the signature (an ASN1 Sequence containing the Integers r and s).
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected byte[] engineSign() throws SignatureException {
        BigInteger one = BigInteger.ONE;
        BigInteger k, kInv, x, r, s, shaM;
        int qBitLength = 160;
        byte[] out, rBytes, sBytes;
        byte[] shaMBytes = md_.digest();
        shaM = new BigInteger(1, shaMBytes);
        do {
            k = new BigInteger(qBitLength, secureRandom_);
        } while (k.compareTo(one) <= 0 || k.compareTo(q_) >= 0);
        kInv = k.modInverse(q_);
        x = dsaPrivateKey_.getX();
        r = (g_.modPow(k, p_)).mod(q_);
        s = (kInv.multiply((shaM.add(x.multiply(r))))).mod(q_);
        rBytes = r.toByteArray();
        sBytes = s.toByteArray();
        try {
            ASN1DSASignature asn1signature = new ASN1DSASignature(r, s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DEREncoder encoder = new DEREncoder(baos);
            asn1signature.encode(encoder);
            byte[] os = baos.toByteArray();
            baos.close();
            return os;
        } catch (ConstraintException ce) {
            ce.printStackTrace();
            throw new SignatureException("shouldn't happen");
        } catch (ASN1Exception ae) {
            ae.printStackTrace();
            throw new SignatureException("shouldn't happen");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new SignatureException("shouldn't happen");
        }
    }

    /**
	* Signs the message and stores the signature in the provided buffer.
	* The signature is an ASN1 Sequence containing the Integers r and s.
	* With r = (g<sup>k</sup> mod p) mod q
	* and s = (k<sup>-1</sup> (SHA(M) + x*r)) mod q.
	* <p>
	* @see cdc.standard.dsa.ASN1DSASignature
	*
	* @param outbuffer buffer for the signature result.
	* @param offset offset into outbuffer where the signature is stored.
	* @param length number of bytes within outbuffer allotted for the signature.
	* @return the number of bytes placed into outbuffer.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected int engineSign(byte[] outbuffer, int offset, int length) throws SignatureException {
        BigInteger one = BigInteger.ONE;
        BigInteger k, kInv, x, r, s, shaM;
        int qBitLength = 160;
        byte[] out, rBytes, sBytes;
        byte[] shaMBytes = md_.digest();
        shaM = new BigInteger(1, shaMBytes);
        k = new BigInteger(qBitLength - 1, secureRandom_);
        kInv = k.modInverse(q_);
        x = dsaPrivateKey_.getX();
        r = (g_.modPow(k, p_)).mod(q_);
        s = kInv.multiply((shaM.add(x.multiply(r)))).mod(q_);
        rBytes = r.toByteArray();
        sBytes = s.toByteArray();
        try {
            ASN1DSASignature asn1signature = new ASN1DSASignature(r, s);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DEREncoder encoder = new DEREncoder(baos);
            asn1signature.encode(encoder);
            byte[] os = baos.toByteArray();
            baos.close();
            int osLength = os.length;
            if (length >= osLength) {
                System.arraycopy(os, 0, outbuffer, offset, osLength);
                return osLength;
            } else {
                return 0;
            }
        } catch (ConstraintException ce) {
            ce.printStackTrace();
            throw new SignatureException("shouldn't happen");
        } catch (ASN1Exception ae) {
            ae.printStackTrace();
            throw new SignatureException("shouldn't happen");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new SignatureException("shouldn't happen");
        }
    }

    /**
	* Passes message bytes to the message digest.
	* <p>
	* @param b the message byte.
	* @param offset the index, where the message bytes starts.
	* @param length the number of message bytes.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected void engineUpdate(byte[] b, int offset, int length) throws SignatureException {
        md_.update(b, offset, length);
    }

    /**
	* Passes a message byte to the message digest.
	* <p>
	* @param b the message byte.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected void engineUpdate(byte b) throws SignatureException {
        md_.update(b);
    }

    /**
	* Verifies a signature.
	* <p>
	* @param signature the signature to be verified.
	* @return True if the signature is correct - false otherwise.
	*/
    protected boolean engineVerify(byte[] signature) {
        try {
            ASN1DSASignature asn1Signature = new ASN1DSASignature();
            ByteArrayInputStream bais = new ByteArrayInputStream(signature);
            DERDecoder decoder = new DERDecoder(bais);
            asn1Signature.decode(decoder);
            decoder.close();
            byte[] rBytes, sBytes, shaMBytes;
            BigInteger r, s, w, u1, u2, shaM, v, y;
            y = dsaPublicKey_.getY();
            r = asn1Signature.getR();
            s = asn1Signature.getS();
            if (r.compareTo(q_) >= 0 || s.compareTo(q_) >= 0) {
                return false;
            }
            shaMBytes = md_.digest();
            shaM = new BigInteger(1, shaMBytes);
            w = s.modInverse(q_);
            u1 = (shaM.multiply(w)).mod(q_);
            u2 = (r.multiply(w)).mod(q_);
            v = (((g_.modPow(u1, p_)).multiply(y.modPow(u2, p_))).mod(p_)).mod(q_);
            if (r.compareTo(v) == 0) return true; else {
                return false;
            }
        } catch (ASN1Exception ae) {
            System.out.println("shouldn't happen");
            ae.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("shouldn't happen");
            ioe.printStackTrace();
        }
        return false;
    }
}
