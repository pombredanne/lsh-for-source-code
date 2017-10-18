package cdc.standard.rsa;

import codec.asn1.*;
import cdc.standard.spec.*;
import cdc.standard.basic.AsymmetricBasicCipher;
import cdc.standard.basic.AsymmetricBlockCipher;
import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.*;
import java.security.spec.AlgorithmParameterSpec;
import javax.crypto.BadPaddingException;

/**
* This class implements the RSA algorithm as defined in
* <a href=http://www.rsasecurity.com/rsalabs/pkcs/pkcs-1/index.html>PKCS#1 version 2.1</a>
* in the OAEP (Optimal Asymmetric Encryption Padding) mode.
* The OAEP mode is recommended for new applications. <p>
*
* If one wants to encrypt a message the following steps  must be performed:
* <pre>
*  // The message which should be encrypted
*  String message = "secret message";
*  byte[] messageBytes = message.getBytes();
*
*  // The source of randomness
*  SecureRandom secureRandom = new SecureRandom();
*
*  // Obtain a RSA Cipher Object
*  Cipher rsaCipher = Cipher.getInstance("RSA_PKCS1_v2_1");
*
*  // Obtain the corresponding key pair generator
*  KeyPairGenerator rsaKPG = KeyPairGenerator.getInstance("RSA");
*
*  // Initialize the key pair generator with the desired strength
*  rsaKPG.initialize(1024);
*
*  // Generate a key pair
*  KeyPair rsaKeyPair = rsaKPG.genKeyPair();
*
*  // Initialize the cipher
*  cipher.init(Cipher.ENCRYPT_MODE, rsaKeyPair.getPublic(), secureRandom);
*
*  // Finally encrypt the message
*  byte[] ciphertextBytes = cipher.doFinal(messageBytes);
* </pre>
* Decrypting a message is similar to encryption, except the <TT>Cipher</TT> must be initialized
* with <TT>Cipher.DECRYPT_MODE</TT> and the private key (<TT>rsaKeyPair.getPrivate()</TT>).
* <p>
* On an AMD Athlon 550 MHz the encryption rate is about 90 kB/sec and the decryption rate is about
* 1 kB/sec.
* <p>
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version       0.5
*/
public class RSA_PKCS1_v2_1 extends AsymmetricBasicCipher implements AsymmetricBlockCipher {

    /**
	* The seed for the correctness test.
	*/
    private int[] seedInt = { 0xaa, 0xfd, 0x12, 0xf6, 0x59, 0xca, 0xe6, 0x34, 0x89, 0xb4, 0x79, 0xe5, 0x07, 0x6d, 0xde, 0xc2, 0xf0, 0x6c, 0xb5, 0x8f };

    /**
	* The seed for the correctness test.
	*/
    private byte[] testSeed = intToByteArray(seedInt);

    /**
	* The source of randomness.
	*/
    private SecureRandom secureRandom_;

    /**
	* A reference to the public key.
	*/
    private RSAPublicKey rsaPublicKey_;

    /**
	* A reference to the private key.
	*/
    private RSAPrivateKey rsaPrivateKey_;

    /**
	* The size of a cipherblock.
	*/
    private int cipherBlockSize_;

    /**
	* The blocksize.
	*/
    private int blockSize_;

    /**
	* A BigInteger with the value 3.
	*/
    private static final BigInteger THREE = BigInteger.valueOf(3);

    /**
	* A reference to the used MessageDigest algorithm.
	*/
    private MessageDigest md_;

    /**
	* The result of the operation digest(parameters). It is calculated with the
	* initalization.
	*/
    private byte[] pHash_;

    /**
	* This method returns the blocksize, the algorithm uses. This method will
	* normaly be called by the padding scheme or the mode. It must be ensured, that this
	* method is exclusivly called, when the algorithm is either in encryption
	* or in decryption mode.
	* <p>
	*
	* @return the used blocksize.
	*/
    public int getBlockSize() {
        return blockSize_;
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
	* This method returns the cipherBlocksize, the algorithm uses.
	* <p>
	*
	* @return the used cipherBlocksize.
	*/
    public int getCipherBlockSize() {
        return cipherBlockSize_;
    }

    /**
	* This is the only overwritten method from <TT>AsymmetricBasicCipher</TT>.
	* It simply retruns a reference to this class instance.
	* <p>
	* @return a reference to an instance of this class.
	*/
    public AsymmetricBlockCipher getCipherObject() {
        return this;
    }

    /**
	* This method initializes the block cipher with a certain key and parameters
	* for data encryption.
	* <p>
	*
	* @param key  the key which has to be used to decrypt data.
	* @param params the algorithm parameters.
	*
	* @exception InvalidKeyException if the given key is inappropriate
	*            for initializing this cipher.
	*/
    public void initDecrypt(Key key, AlgorithmParameterSpec params) throws InvalidKeyException {
        if (key instanceof RSAPrivateCrtKey || key instanceof RSAPrivateKey) {
            rsaPrivateKey_ = (RSAPrivateKey) key;
            rsaPublicKey_ = null;
            secureRandom_ = new SecureRandom();
            try {
                md_ = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("SHA-1 Algorithm not found");
                nsae.printStackTrace();
            }
            cipherBlockSize_ = ((rsaPrivateKey_.getModulus().bitLength()) + 7) / 8;
            blockSize_ = cipherBlockSize_ - 2 * (md_.getDigestLength()) - 2;
        } else {
            throw new InvalidKeyException("Key is not a RSAPrivateKey!");
        }
        if (params instanceof RSAOAEPAlgorithmParameterSpec) {
            ASN1Sequence paramSeq = (ASN1Sequence) ((RSAOAEPAlgorithmParameterSpec) params).getParameters();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DEREncoder dec = new DEREncoder(baos);
                paramSeq.encode(dec);
                byte[] paramBytes = baos.toByteArray();
                dec.close();
                md_.update(paramBytes);
                pHash_ = md_.digest();
            } catch (IOException ioe) {
                System.out.println("shouldn't happen");
                ioe.printStackTrace();
            } catch (ASN1Exception ae) {
                System.out.println("shouldn't happen");
                ae.printStackTrace();
            }
        }
    }

    /**
	* This method initializes the block cipher with a certain key and parameters
	* for data encryption.
	* <p>
	*
	* @param key the key which has to be used to encrypt data.
	* @param secureRandom the source of randomness.
	* @param params the algorithm parameters.
	*
	* @exception InvalidKeyException if the given key is inappropriate
	*            for initializing this cipher.
	*
	*/
    public void initEncrypt(Key key, SecureRandom secureRandom, AlgorithmParameterSpec params) throws InvalidKeyException {
        if (key instanceof RSAPublicKey) {
            rsaPublicKey_ = (RSAPublicKey) key;
            rsaPrivateKey_ = null;
            secureRandom_ = secureRandom;
            try {
                md_ = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException nsae) {
                System.err.println("SHA-1 Algorithm not found");
                nsae.printStackTrace();
            }
            cipherBlockSize_ = ((rsaPublicKey_.getModulus().bitLength()) + 7) / 8;
            blockSize_ = cipherBlockSize_ - 2 * (md_.getDigestLength()) - 2;
        } else {
            throw new InvalidKeyException("Key is not a RSAPublicKey!");
        }
        if (params instanceof RSAOAEPAlgorithmParameterSpec) {
            ASN1Sequence paramSeq = (ASN1Sequence) ((RSAOAEPAlgorithmParameterSpec) params).getParameters();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DEREncoder dec = new DEREncoder(baos);
                paramSeq.encode(dec);
                byte[] paramBytes = baos.toByteArray();
                dec.close();
                md_.update(paramBytes);
                pHash_ = md_.digest();
            } catch (IOException ioe) {
                System.out.println("shouldn't happen");
                ioe.printStackTrace();
            } catch (ASN1Exception ae) {
                System.out.println("shouldn't happen");
                ae.printStackTrace();
            }
        }
    }

    private byte[] intToByteArray(int[] in) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = (byte) in[i];
        }
        return out;
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
	* RSA-OAEP pads itself - so the padding scheme has nothing to do.
	* <p>
	*
	* @return false.
	*/
    public boolean pad() {
        return false;
    }

    /**
	* This method decrypts a single block of data, and may only be called,
	* when the block cipher is in decrytion mode. It has to be ensured, too, that
	* the array <TT>in</TT> contains a whole block starting at <TT>inOffset</TT>
	* and that <TT>out</TT> is large enogh to hold an decrypted block starting
	* at <TT>outOffset</TT>.
	* <p>
	*
	* @param  in        array of bytes which contains the ciphertext to be
	*                   decrypted.
	* @param  inOffset  index in array in, where the ciphertext block starts.
	* @param  out       array of bytes which will contain the plaintext starting
	*                   at outOffset.
	* @param  outOffset index in array out, where the plaintext block will start.
	* @return the index of the last plaintextbyte in the output array <TT>out</TT>.
	* @throws BadPaddingException if the block is not padded correctly.
	*/
    public int singleBlockDecrypt(byte[] in, int inOffset, byte[] out, int outOffset) throws BadPaddingException {
        if (rsaPrivateKey_ != null) {
            BigInteger m;
            byte[] cBytes = new byte[cipherBlockSize_];
            System.arraycopy(in, inOffset, cBytes, 0, cipherBlockSize_);
            BigInteger c = new BigInteger(1, cBytes);
            if (rsaPrivateKey_ instanceof RSAPrivateKey) {
                BigInteger n = rsaPrivateKey_.getModulus();
                BigInteger d = rsaPrivateKey_.getPrivateExponent();
                m = c.modPow(d, n);
            } else {
                RSAPrivateCrtKey rsaPrivCrtKey = (RSAPrivateCrtKey) rsaPrivateKey_;
                BigInteger d = rsaPrivCrtKey.getPrivateExponent();
                BigInteger p = rsaPrivCrtKey.getPrimeP();
                BigInteger q = rsaPrivCrtKey.getPrimeQ();
                BigInteger dP = rsaPrivCrtKey.getPrimeExponentP();
                BigInteger dQ = rsaPrivCrtKey.getPrimeExponentQ();
                BigInteger qInv = rsaPrivCrtKey.getCrtCoefficient();
                BigInteger m_1, m_2, h;
                m_1 = (c.remainder(p)).modPow(dP, p);
                m_2 = (c.remainder(q)).modPow(dQ, q);
                h = (qInv.multiply((m_1.subtract(m_2)).remainder(p))).mod(p);
                m = ((h.multiply(q)).add(m_2));
            }
            byte[] mBytes = new byte[cipherBlockSize_];
            byte[] mtmp = getBytes(m);
            System.arraycopy(mtmp, 0, mBytes, mBytes.length - mtmp.length, mtmp.length);
            int digestLength = md_.getDigestLength();
            if (mBytes.length < (digestLength * 2) + 1) {
                throw new BadPaddingException("decoding error !");
            }
            byte[] maskedSeed = new byte[digestLength];
            System.arraycopy(mBytes, 1, maskedSeed, 0, digestLength);
            byte[] maskedDB = new byte[mBytes.length - digestLength - 1];
            System.arraycopy(mBytes, digestLength + 1, maskedDB, 0, mBytes.length - digestLength - 1);
            byte[] seedMask = mgf(maskedDB, digestLength);
            byte[] seed = xor(maskedSeed, seedMask);
            byte[] dbMask = mgf(seed, mBytes.length - digestLength);
            byte[] db = xor(maskedDB, dbMask);
            byte[] pHashDec = new byte[digestLength];
            System.arraycopy(db, 0, pHashDec, 0, digestLength);
            int i = digestLength;
            while (db[i] != 0x01) {
                if (db[i] != 0x00) {
                    throw new BadPaddingException("decoding error !");
                }
                i++;
            }
            i++;
            byte[] message = new byte[db.length - i];
            System.arraycopy(db, i, message, 0, db.length - i);
            for (int j = 0; j < pHash_.length; j++) {
                if (pHash_[j] != pHashDec[j]) {
                    throw new BadPaddingException("decoding error !");
                }
            }
            System.arraycopy(message, 0, out, outOffset, message.length);
            return message.length;
        }
        return 0;
    }

    /**
	* This method encrypts a single block of data, and may only be called,
	* when the block cipher is in encrytion mode. It has to be ensured, too, that
	* the array <TT>in</TT> contains a whole block starting at <TT>inOffset</TT>
	* and that <TT>out</TT> is large enogh to hold an encrypted block starting
	* at <TT>outOffset</TT>.
	* <p>
	*
	* @param  in        array of bytes which contains the plaintext to be
	*                   encrypted.
	* @param  inOffset  index in array in, where the plaintext block starts.
	* @param  out       array of bytes which will contain the ciphertext startig
	*                   at outOffset.
	* @param  outOffset index in array out, where the ciphertext block will start.
	*/
    public void singleBlockEncrypt(byte[] in, int inOffset, byte[] out, int outOffset) {
        if (rsaPublicKey_ != null) {
            int digestLength = md_.getDigestLength();
            int inLenSubInOffset = in.length - inOffset;
            if (inLenSubInOffset > blockSize_) {
                inLenSubInOffset = blockSize_;
            }
            byte[] ps = new byte[cipherBlockSize_ - (inLenSubInOffset) - 2 * digestLength - 2];
            byte[] db = new byte[cipherBlockSize_ - digestLength - 1];
            System.arraycopy(pHash_, 0, db, 0, digestLength);
            System.arraycopy(ps, 0, db, digestLength, ps.length);
            db[digestLength + ps.length] = 0x01;
            System.arraycopy(in, inOffset, db, digestLength + ps.length + 1, inLenSubInOffset);
            byte[] seed = new byte[digestLength];
            secureRandom_.nextBytes(seed);
            byte[] dbMask = mgf(seed, cipherBlockSize_ - digestLength - 1);
            byte[] maskedDB = xor(db, dbMask);
            byte[] seedMask = mgf(maskedDB, digestLength);
            byte[] maskedSeed = xor(seed, seedMask);
            byte[] em = new byte[cipherBlockSize_];
            em[0] = 0x00;
            System.arraycopy(maskedSeed, 0, em, 1, maskedSeed.length);
            System.arraycopy(maskedDB, 0, em, maskedSeed.length + 1, maskedDB.length);
            BigInteger mBigInt = new BigInteger(1, em);
            BigInteger n = rsaPublicKey_.getModulus();
            BigInteger e = rsaPublicKey_.getPublicExponent();
            BigInteger x;
            if (e.equals(THREE)) {
                x = (((mBigInt.multiply(mBigInt)).mod(n)).multiply(mBigInt)).mod(n);
            } else {
                x = mBigInt.modPow(e, n);
            }
            byte[] cBytes = getBytes(x);
            System.arraycopy(cBytes, 0, out, outOffset + cipherBlockSize_ - cBytes.length, cBytes.length);
        }
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
