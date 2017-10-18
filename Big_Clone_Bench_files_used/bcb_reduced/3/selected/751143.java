package org.sepp.security.implementation;

import iaik.me.security.Cipher;
import iaik.me.security.CryptoBag;
import iaik.me.security.CryptoException;
import iaik.me.security.Mac;
import iaik.me.security.MessageDigest;
import iaik.me.security.PBE;
import iaik.me.security.PrivateKey;
import iaik.me.security.PublicKey;
import iaik.me.security.SecureRandom;
import iaik.me.security.Signature;
import iaik.me.utils.CryptoUtils;
import java.nio.ByteBuffer;
import java.security.SignatureException;
import org.sepp.exceptions.SecurityServiceException;
import org.sepp.security.SecurityConstants;
import org.smepp.logger.Logger;

public class IAIKMESecurityPrimitives {

    private Logger log = Logger.getLogger(this.getClass().getName());

    private SecureRandom secureRandom;

    private CryptoBag currentIV;

    public IAIKMESecurityPrimitives(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public byte[] createSignature(byte[] data, String algorithm, PrivateKey privateKey) throws SecurityServiceException {
        try {
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(privateKey, secureRandom);
            signer.update(data);
            byte[] signed = signer.sign();
            log.finest("Signature created successfully but will not be verified because no public key is specified!");
            return signed;
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't sign data. Reason: " + e.getMessage());
        }
    }

    public byte[] createSignature(byte[] data, String algorithm, PrivateKey privateKey, PublicKey publicKey) throws SecurityServiceException {
        try {
            Signature signer = Signature.getInstance(algorithm);
            signer.initSign(privateKey, secureRandom);
            signer.update(data);
            byte[] signed = signer.sign();
            log.finest("Signature created successfully now it must be verified to be correct!");
            signer.initVerify(publicKey);
            signer.update(data);
            if (signer.verify(signed)) {
                log.finest("Signature also verified correctly therefore the created signature is correct!");
                return signed;
            } else {
                throw new SignatureException("Couldn't verify signed data therefore signing failed!");
            }
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't sign data. Reason: " + e.getMessage());
        }
    }

    public boolean verifySignature(byte[] data, byte[] signature, String algorithm, PublicKey publicKey) throws SecurityServiceException {
        try {
            Signature verifier = Signature.getInstance(algorithm);
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't verify signature. Reason: " + e.getMessage());
        }
    }

    /**
	 * Computes a HMAC on the given data and compares if the result is the same.
	 * 
	 * @param algorithm
	 *            The HMAC algorithm to be used, e.g. "HMAC/SHA-1"
	 * @param data
	 *            The data on which the MAC shall be computed
	 * @param key
	 *            The secret key which should be used to create the MAC.
	 * 
	 * @return <code>true</code> if the test succeeds, <code>false</code>
	 *         otherwise
	 */
    public byte[] createHMAC(String algorithm, byte[] data, CryptoBag key) throws SecurityServiceException {
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(key, key);
            byte[] result = mac.doFinal(data);
            return result;
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't create MAC from provided data. Reason: " + e.getMessage());
        }
    }

    /**
	 * Computes a HMAC on the given data and compares if the result is the same.
	 * 
	 * @param algorithm
	 *            The HMAC algorithm to be used, e.g. "HMAC/SHA-1".
	 * @param data
	 *            The data on which the MAC has been computed.
	 * @param mac
	 *            The MAC of the data.
	 * @param key
	 *            The secret key which should be used to create the MAC.
	 * 
	 * @return <code>true</code> if the test succeeds, <code>false</code>
	 *         otherwise
	 */
    public boolean verifyHMAC(String algorithm, byte[] data, byte[] mac, CryptoBag key) throws SecurityServiceException {
        try {
            Mac verifyMAC = Mac.getInstance(algorithm);
            verifyMAC.init(key, key);
            byte[] result = verifyMAC.doFinal(data);
            return CryptoUtils.equalsBlock(result, mac);
        } catch (Exception nsae) {
            throw new SecurityServiceException("Couldn't create MAC from provided data. Reason: " + nsae.getMessage());
        }
    }

    public CryptoBag getCurrentIV() {
        return currentIV;
    }

    public void setCurrentIV(CryptoBag currentIV) {
        this.currentIV = currentIV;
    }

    public byte[] encrypt(byte[] plainText, String alias) throws SecurityServiceException {
        return encrypt(plainText, SecurityConstants.symmetricAlgorithm, getAESSharedKey(alias));
    }

    public byte[] encrypt(byte[] plainText, String transformation, CryptoBag key) throws SecurityServiceException {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            currentIV = cipher.getIV();
            byte[] cipherText = cipher.doFinal(plainText);
            return cipherText;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityServiceException("Couldn't encrypt provided data. Reason: " + e.getMessage());
        }
    }

    public byte[] decrypt(byte[] cipherText, String alias) throws SecurityServiceException {
        return decrypt(cipherText, SecurityConstants.symmetricAlgorithm, getAESSharedKey(alias));
    }

    public byte[] decrypt(byte[] cipherText, String transformation, CryptoBag key) throws SecurityServiceException {
        try {
            Cipher cipher = Cipher.getInstance(transformation);
            if (key == null) throw new SecurityServiceException("Why the hell is it null?? DAVID DAVID");
            if (currentIV instanceof CryptoBag) {
                cipher.init(Cipher.DECRYPT_MODE, key, currentIV, null);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
            }
            byte[] plainText = cipher.doFinal(cipherText);
            return plainText;
        } catch (Exception e) {
            e.printStackTrace();
            throw new SecurityServiceException("Couldn't decrypt provided data. Reason: " + e.getMessage());
        }
    }

    public CryptoBag getAESSharedKey(String sharedSecret) throws SecurityServiceException {
        try {
            String cipherText = "This is the text";
            PBE pbe = PBE.getInstance(PBE.OID_PKCS12_DES_EDE_168_SHA);
            Cipher cipher = pbe.getCipher(Cipher.ENCRYPT_MODE, sharedSecret.toCharArray(), "test".getBytes(), 0, null);
            byte[] aesKey = cipher.doFinal(cipherText.getBytes());
            CryptoBag key = CryptoBag.makeSecretKey(aesKey);
            return key;
        } catch (CryptoException e) {
            throw new SecurityServiceException("Couldn't create AES key. Reason: " + e.getMessage());
        }
    }

    /**
	 * Creates an {@link CryptoBag} using the key generator from the IAIK JCE
	 * library. The returned {@link CryptoBag} is generated using the specified
	 * algorithm.
	 * 
	 * @param algorithm
	 *            The algorithm which should be used to create the secret key.
	 * @return The newly created {@link CryptoBag}.
	 * @throws SmeppSecurityException
	 *             Thrown if anything goes wrong.
	 */
    public CryptoBag createSecretKey() throws SecurityServiceException {
        try {
            byte[] aesKey = new byte[16];
            secureRandom.nextBytes(aesKey);
            CryptoBag key = CryptoBag.makeSecretKey(aesKey);
            return key;
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't create secret key. Reason: " + e.getMessage());
        }
    }

    public CryptoBag createSessionKey(String sharedKey) throws SecurityServiceException {
        try {
            byte[] aesKey = new byte[16];
            ByteBuffer buf = ByteBuffer.wrap(createHash("SHA-224", sharedKey.getBytes()));
            buf.get(aesKey, 0, aesKey.length);
            return CryptoBag.makeSecretKey(aesKey);
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't create secret key. Reason: " + e.getMessage());
        }
    }

    /**
	 * Generates an hash value from the provided data using the specified hash
	 * algorithm. The returned value is the hash value as byte array with the
	 * size of the used algorithm.
	 * 
	 * @param algorithm
	 *            The hash algorithm to use.
	 * @param data
	 *            The data which should be hashed.
	 * @return The hash value of the data using the specified algorithm.
	 * @throws Exception
	 */
    public byte[] createHash(String algorithm, byte[] data) throws SecurityServiceException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            return digest.digest(data);
        } catch (Exception e) {
            throw new SecurityServiceException("Couldn't create hash value provided data. Reason: " + e.getMessage());
        }
    }
}
