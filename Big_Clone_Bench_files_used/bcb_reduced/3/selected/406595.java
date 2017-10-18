package cdc.standard.rsa;

import codec.*;
import codec.asn1.*;
import codec.pkcs1.*;
import codec.pkcs8.*;
import codec.x509.*;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
* This class implements the RSA signature algorithm as defined in
* <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-1/index.html>PKCS#1 version 2.1</a> with
* MD5 as message digest algorithm. It uses the RSA algorithm (RSAES-PKCS1v1_5) and
* the MD5 message digest algorithm.<p>
*
* <p>
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version 0.41
*/
public class MD5withRSA extends SignatureSpi {

    /**
	* The source of randomness.
	*/
    private SecureRandom secureRandom_;

    /**
	* The message digest - here MD5.
	*/
    private MessageDigest md_;

    /**
	* The cipher algorithm - here RSA.
	*/
    private Cipher cipher_;

    /**
	* The MD5 OID.
	*/
    private ASN1ObjectIdentifier md5oid_ = null;

    /**
	* The MD5 AlgorithmIdentifier.
	*/
    private AlgorithmIdentifier md5aid_ = null;

    /**
	* The standard constructor tries to generate the RSA-SSA-PSS Algorithm identifier
	* (with the Object ID 1.2.840.113549.2.5).
	*/
    public MD5withRSA() {
        try {
            md5oid_ = new ASN1ObjectIdentifier("1.2.840.113549.2.5");
            md5aid_ = new AlgorithmIdentifier(md5oid_, new ASN1Null());
        } catch (ASN1Exception ae) {
            System.out.println("shouldnt happen:" + ae.getMessage());
            ae.printStackTrace();
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
	* @throws InvalidKeyException if the key is not an instance of RSAPrivKey.
	*/
    protected void engineInitSign(PrivateKey privateKey) throws InvalidKeyException {
        secureRandom_ = new SecureRandom();
        try {
            md_ = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.out.println("RSASignature: MD Algorithm not found");
            nsae.printStackTrace();
        }
        try {
            cipher_ = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nsae.printStackTrace();
        } catch (NoSuchPaddingException nspe) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nspe.printStackTrace();
        }
        cipher_.init(Cipher.ENCRYPT_MODE, privateKey, secureRandom_);
    }

    /**
	* Initializes the signature algorithm for signing a message.
	* <p>
	* @param privateKey the private key of the signer.
	* @param secureRandom the source of randomness.
	* @throws InvalidKeyException if the key is not an instance of RSAPrivKey.
	*/
    protected void engineInitSign(PrivateKey privateKey, SecureRandom secureRandom) throws InvalidKeyException {
        secureRandom_ = secureRandom;
        try {
            md_ = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: MD Algorithm not found");
            nsae.printStackTrace();
        }
        try {
            cipher_ = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nsae.printStackTrace();
        } catch (NoSuchPaddingException nspe) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nspe.printStackTrace();
        }
        cipher_.init(Cipher.ENCRYPT_MODE, privateKey, secureRandom);
    }

    /**
	* Initializes the signature algorithm for verifying a signature.
	* <p>
	* @param publicKey the public key of the signer.
	* @throws InvalidKeyException if the public key is not an instance of RSAPubKey.
	*/
    protected void engineInitVerify(PublicKey publicKey) throws InvalidKeyException {
        try {
            md_ = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: MD Algorithm not found");
            nsae.printStackTrace();
        }
        try {
            cipher_ = Cipher.getInstance("RSA");
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nsae.printStackTrace();
        } catch (NoSuchPaddingException nspe) {
            System.err.println("RSASignature: RSA Algorithm not found");
            nspe.printStackTrace();
        }
        cipher_.init(Cipher.DECRYPT_MODE, publicKey);
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
        byte[] out = null;
        byte[] shaMBytes = md_.digest();
        byte[] plainSig = null;
        try {
            DigestInfo di = new DigestInfo(md5aid_, shaMBytes);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DEREncoder encoder = new DEREncoder(baos);
            di.encode(encoder);
            plainSig = baos.toByteArray();
            baos.close();
            out = cipher_.doFinal(plainSig);
            return out;
        } catch (ConstraintException ce) {
            System.out.println("internal error:");
            ce.printStackTrace();
        } catch (ASN1Exception ae) {
            System.out.println("internal error:");
            ae.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("internal error:");
            ioe.printStackTrace();
        } catch (IllegalBlockSizeException ibse) {
            System.err.println("RSASignature: cipher.doFinal");
            ibse.printStackTrace();
        } catch (BadPaddingException bpe) {
            System.err.println("RSASignature: cipher.doFinal");
            bpe.printStackTrace();
        }
        return null;
    }

    /**
	* Signs the message and stores the signature in the provided buffer.
	* <p>
	* @param outbuffer buffer for the signature result.
	* @param offset offset into outbuffer where the signature is stored.
	* @param length number of bytes within outbuffer allotted for the signature.
	* @return the number of bytes placed into outbuffer, if length >= signature.length.
	* If length < signature.length then outbuffer is leaved unchanged and 0 is returned.
	* @throws SignatureException if the signature is not initialized properly.
	*/
    protected int engineSign(byte[] outbuffer, int offset, int length) throws SignatureException {
        byte[] out = engineSign();
        if (out.length <= length) {
            System.arraycopy(out, 0, outbuffer, offset, out.length);
            return out.length;
        } else {
            return 0;
        }
    }

    /**
	* Passes message bytes to the message digest.
	* <p>
	* @param b The message byte.
	* @param offset The index, where the message bytes starts.
	* @param length The number of message bytes.
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
	* @return true if the signature is correct - false otherwise.
	*/
    protected boolean engineVerify(byte[] signature) {
        byte[] shaMBytes = md_.digest();
        byte[] plain;
        try {
            plain = cipher_.doFinal(signature);
            DigestInfo di = new DigestInfo();
            ByteArrayInputStream bais = new ByteArrayInputStream(plain);
            DERDecoder decoder = new DERDecoder(bais);
            di.decode(decoder);
            decoder.close();
            ASN1ObjectIdentifier oid = (di.getAlgorithmIdentifier()).getAlgorithmOID();
            if (!oid.equals(md5oid_)) return false;
            byte[] hashBytes = di.getDigest();
            if (hashBytes.length != shaMBytes.length) {
                return false;
            }
            for (int i = 0; i < hashBytes.length; i++) {
                if (hashBytes[i] != shaMBytes[i]) {
                    return false;
                }
            }
            return true;
        } catch (IllegalBlockSizeException ibse) {
            System.err.println("RSASignature: cipher.doFinal");
            ibse.printStackTrace();
        } catch (BadPaddingException bpe) {
            System.err.println("RSASignature: cipher.doFinal");
            bpe.printStackTrace();
        } catch (ASN1Exception ae) {
            System.out.println("internal error:");
            ae.printStackTrace();
        } catch (IOException ioe) {
            System.out.println("internal error:");
            ioe.printStackTrace();
        }
        return false;
    }
}
