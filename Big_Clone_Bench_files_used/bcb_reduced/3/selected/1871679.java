package cdc.standard.pbe;

import java.security.*;
import java.security.spec.*;
import javax.crypto.*;
import java.math.BigInteger;

/**
* This is the base class for all classes which implement
* passphrase based encryption (PBE).
* The key derivation function follows the
* <a href="http://www.rsasecurity.com/rsalabs/pkcs/pkcs-5/index.html">PKCS#5 version 2.0</a>
* standard.
* <p>
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version 0.1
*/
public abstract class PBEBasicCipher extends CipherSpi {

    /**
	* A reference to the underlying cipher e.g. DES.
	*/
    protected static Cipher cipher_ = null;

    /**
	* A reference to the underlying message digest e.g. SHA-1.
	*/
    protected static MessageDigest messageDigest_ = null;

    /**
	 * Encrypts or decrypts data in a single-part operation, or
	 * finishes a multiple-part operation. The data is encrypted or
	 * decrypted, depending on how this cipher was initialised.
	 * <p>
	 * The first inputLen bytes in the input buffer, starting at
	 * inputOffset, and any input bytes that may have been buffered during
	 * a previous update operation, are processed, with padding (if
	 * requested) being applied. The result is stored in a new buffer.
	 * <p>
	 * The cipher is reset to its initial state (uninitialised) after
	 * this call.
	 *
	 * @param input the input buffer
	 * @param inputOffset the offset in input where the input starts
	 * @param inputLen the input length
	 *
	 * @exception IllegalBlockSizeException if this cipher is a block
	 * 	cipher, no padding has been requested (only in encryption
	 *	mode), and the total input length of the data processed by
	 *	this cipher is not a multiple of block size
	 * @exception BadPaddingException if this cipher is in decryption mode,
	 * 	and (un)padding has been requested, but the decrypted data is
	 *	not bounded by the appropriate padding bytes.
	 *
	 * @return the new buffer with the result
	 */
    public byte[] engineDoFinal(byte input[], int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        return cipher_.doFinal(input, inputOffset, inputLen);
    }

    /**
	 * Encrypts or decrypts data in a single-part operation, or finishes
	 * a multiple-part operation. The data is encrypted or decrypted,
	 * depending on how this cipher was initialised.
	 * <p>
	 * The first inputLen bytes in the input buffer, starting at
	 * inputOffset, and any input bytes that may have been buffered during
	 * a previous update operation, are processed, with padding (if
	 * requested) being applied. The result is stored in the output buffer,
	 * starting at outputOffset.
	 * <p>
	 * If the output buffer is too small to hold the result, a
	 * ShortBufferException is thrown.  In this case, repeat this call
	 * with a larger output buffer. Use getOutputSize to determine how
	 * big the output buffer should be.
	 *
	 * @param input the input buffer
	 * @param inputOffset - the offset in input where the input starts
	 * @param inputLen - the input length
	 * @param output - the buffer for the result
	 * @param outputOffset - the offset in output where the result is stored
	 *
	 * @exception IllegalBlockSizeException if this cipher is a block
	 *    cipher, no padding has been requested (only in encryption mode),
	 *    and the total input length of the data processed by this cipher
	 *    is not a multiple of block size
	 * @exception ShortBufferException if the given output buffer is too
	 *    small to hold the result
	 * @exception BadPaddingException if this cipher is in decryption mode,
	 *    and (un)padding has been requested, but the decrypted data is
	 *    not bounded by the appropriate padding bytes
	 * @return the number of bytes stored in output
	 */
    public int engineDoFinal(byte[] input, int inputOff, int inputLen, byte[] output, int outputOff) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        return cipher_.doFinal(input, inputOff, inputLen, output, outputOff);
    }

    /**
	 * Returns the block size (in bytes) of the underlying cipher.
	 *
	 * @return the block size (in bytes), or 0 if the underlying
	 *	algorithm is not a block cipher
	 */
    public int engineGetBlockSize() {
        return cipher_.getBlockSize();
    }

    /**
	 * Returns the initialisation vector (IV) in a new buffer.
	 * <p>
	 * This is useful in the context of password-based encryption or
	 * decryption, where the IV is derived from a user-provided passphrase.
	 *
	 * @return the initialisation vector in a new buffer, or null if the
	 * 	underlying algorithm does not use an IV, or if the IV has
	 * 	not yet been set.
	 */
    public byte[] engineGetIV() {
        return cipher_.getIV();
    }

    /**
	 * Returns the length in bytes that an output buffer would need to be
	 * in order to hold the result of the next update or doFinal operation,
	 * given the input length inputLen (in bytes).
	 * <p>
	 * This call takes into account any unprocessed (buffered) data from a
	 * previous update call, and padding.
	 * <p>
	 * The actual output length of the next update or doFinal call may be
	 * smaller than the length returned by this method.
	 *
	 * @param inputLen the input length (in bytes)
	 * @return the required output buffer size (in bytes)
	 */
    public int engineGetOutputSize(int inputLen) {
        return cipher_.getOutputSize(inputLen);
    }

    /**
	 * Returns the parameters used with this cipher.
	 * <p>
	 * The returned parameters may be the same that were used to initialise
	 * this cipher, or may contain the default set of parameters or a set of
	 * randomly generated parameters used by the underlying cipher
	 * implementation (provided that the underlying cipher implementation
	 * uses a default set of parameters or creates new parameters if it needs
	 * parameters but was not initialised with any).
	 *
	 * @return the parameters used with this cipher, or null if this cipher
	 *     does not use any parameters.
	 */
    public AlgorithmParameters engineGetParameters() {
        return cipher_.getParameters();
    }

    /**
	 * Sets the mode of this cipher. This method should never be called. It
	 * always throws an exception.
	 * <p>
	 * @param mode the cipher mode
	 * @exception NoSuchAlgorithmException	if the requested cipher mode
	 *					does not exist
	 */
    public void engineSetMode(String mode) throws NoSuchAlgorithmException {
        throw new NoSuchAlgorithmException("This method should never be called");
    }

    /**
	 * Sets the padding mechanism of this cipher. This method should never be called. It
	 * always throws an exception.
	 *
	 * @param padding the padding mechanism
	 * @exception NoSuchPaddingException	if the requested padding
	 *					mechanism does not exist.
	 */
    public void engineSetPadding(String padding) throws NoSuchPaddingException {
        throw new NoSuchPaddingException("This method should never be called");
    }

    /**
	 * Continues a multiple-part encryption or decryption operation
	 * (depending on how this cipher was initialised), processing another
	 * data part.
	 * <p>
	 * The first inputLen bytes in the input buffer, starting at
	 * inputOffset, are processed, and the result is stored in a new buffer.
	 *
	 * @param input the input buffer
	 * @param inputOffset the offset in input where the input starts
	 * @param inputLen the input length
	 * @return the new buffer with the result, or null if the underlying
	 *    cipher is a block cipher and the input data is too short to
	 *    result in a new block.
	 */
    public byte[] engineUpdate(byte[] input, int inputOff, int inputLen) {
        return cipher_.update(input, inputOff, inputLen);
    }

    /**
	 * Continues a multiple-part encryption or decryption operation
	 * (depending on how this cipher was initialised), processing another
	 * data part.
	 * <p>
	 * The first inputLen bytes in the input buffer, starting at
	 * inputOffset, are processed, and the result is stored in the output
	 * buffer, starting at outputOffset.
	 * <p>
	 * If the output buffer is too small to hold the result, a
	 * ShortBufferException is thrown. In this case, repeat this call with
	 * a larger output buffer. Use getOutputSize to determine how big the
	 * output buffer should be.
	 *
	 * @param input the input buffer
	 * @param inputOffset the offset in input where the input starts
	 * @param inputLen the input length
	 * @param output the buffer for the result
	 * @param outputOffset the offset in output where the result is stored
	 *
	 * @exception  ShortBufferException if the given output buffer is too
	 *     small to hold the result
	 *
	 * @return the number of bytes stored in output
	 */
    public int engineUpdate(byte[] input, int inputOff, int inputLen, byte[] output, int outputOff) throws ShortBufferException {
        return cipher_.update(input, inputOff, inputLen, output, outputOff);
    }

    /**
	* This function takes the passphrase from the PBEKey and the salt
	* and applies the message digest iterction count times on them.
	* Then the first keyLength bytes are returned.
	* <p>
	* @param pbeKey the PBEKey.
	* @param salt the salt.
	* @param iterationCount the iteration count.
	* @param keyLength the length of the generated key.
	* @return the bytes representing a key for the underlying cipher.
	*/
    protected byte[] generateKeyBytes(PBEKey pbeKey, byte[] salt, int iterationCount, int keyLength) {
        byte[] out;
        byte[] outCut = new byte[keyLength];
        messageDigest_.update(pbeKey.getEncoded());
        messageDigest_.update(salt);
        out = messageDigest_.digest();
        for (int i = 1; i < iterationCount; i++) {
            messageDigest_.update(out);
            out = messageDigest_.digest();
        }
        System.arraycopy(out, 0, outCut, 0, keyLength);
        return outCut;
    }

    /**
	* This function takes the passphrase from the PBEKey and the salt
	* according to the key generation scheme described in PKCS12.
	*
	* <p>
	* @param pbeKey the PBEKey.
	* @param salt the salt.
	* @param iterationCount the iteration count.
	* @param keyLength the length of the generated key.
	* @param id the ID byte. If the byte string is to be used as key material for encryption/decryption ID is 1, in case it is to be used as an iv vector ID is 2 and in case of integrity protection ID is 3.
	* @return the bytes representing a key for the underlying cipher.
	*/
    protected byte[] generateKeyBytesPKCS12(PBEBMPKey pbeKey, byte[] salt, int iterationCount, int keyLength, int id) {
        byte[] passwd = pbeKey.getEncoded();
        byte[] mD = new byte[64];
        for (int i = 0; i < mD.length; i++) mD[i] = (byte) id;
        byte[] mP = augment(passwd);
        byte[] mS = augment(salt);
        byte[] mI = new byte[mP.length + mS.length];
        System.arraycopy(mS, 0, mI, 0, mS.length);
        System.arraycopy(mP, 0, mI, mS.length, mP.length);
        byte[] mA;
        byte[] outCut = new byte[keyLength];
        int k = 1;
        do {
            messageDigest_.update(mD);
            messageDigest_.update(mI);
            mA = messageDigest_.digest();
            for (int i = 1; i < iterationCount; i++) {
                messageDigest_.update(mA);
                mA = messageDigest_.digest();
            }
            if (keyLength < mA.length) {
                System.arraycopy(mA, 0, outCut, 0, keyLength);
                break;
            } else if ((keyLength < k * mA.length) || (keyLength == k * mA.length)) {
                int rem = mA.length - (k * mA.length - keyLength);
                System.arraycopy(mA, 0, outCut, (k - 1) * mA.length, rem);
                break;
            } else System.arraycopy(mA, 0, outCut, (k - 1) * mA.length, mA.length);
            byte[] mB = augment(mA);
            BigInteger b = new BigInteger(mB);
            byte[] ij = new byte[64];
            byte[] modByte = new byte[65];
            byte[] one = { 1 };
            System.arraycopy(one, 0, modByte, 0, 1);
            BigInteger modulo = new BigInteger(modByte);
            BigInteger tmp = null;
            for (int j = 0; j < mI.length / 64; j++) {
                System.arraycopy(mI, j * 64, ij, 0, 64);
                BigInteger ivint = new BigInteger(ij);
                tmp = ((b.add(ivint)).add(b.ONE)).mod(modulo);
                byte[] tmp1 = tmp.toByteArray();
                System.arraycopy(tmp1, tmp1.length - 64, mI, j * 64, 64);
            }
            k++;
        } while ((k - 1) * mA.length < keyLength);
        return outCut;
    }

    private byte[] augment(byte[] in) {
        int n = in.length;
        int v = 64;
        int tmp;
        int amount;
        int iter;
        byte[] out;
        tmp = n / v;
        if (n % v != 0) tmp++;
        amount = v * tmp;
        out = new byte[amount];
        iter = amount / n;
        for (int i = 0; i < iter; i++) System.arraycopy(in, 0, out, i * n, n);
        if (amount % n != 0) System.arraycopy(in, 0, out, iter * n, amount % n);
        return out;
    }
}
