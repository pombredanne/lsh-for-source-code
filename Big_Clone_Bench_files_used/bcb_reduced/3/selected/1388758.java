package cdc.standard.pbe;

import java.security.*;

/**
* The HMAC-SHA-1 Pseudo Random Funktion (PRF) as defined in RFC 2104.
*
* @author
* <a href="mailto:twahren@cdc.informatik.tu-darmstadt.de">Thomas Wahrenbruch</a>
* @version 0.1
* @see cdc.standard.pbe.PRF
*/
public class HMACsha1PRF extends PRF {

    /**
	* The block size of the underlying message digest algorithm SHA-1 (64).
	*/
    private static final int MD_BLOCKSIZE = 64;

    /**
	* The standard constructor tries to initialize the message digest algorithm (SHA-1)
	* and sets mdBlockSize.
	* <p>
	*/
    public HMACsha1PRF() {
        mdBlockSize_ = MD_BLOCKSIZE;
        ;
        initIOPad();
        try {
            messageDigest_ = MessageDigest.getInstance("SHA-1", "CDCStandard");
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        } catch (NoSuchProviderException nspe) {
            nspe.printStackTrace();
        }
    }

    /**
	* Generates the pseudorandom bytes. If the key is longer than the block size of the
	* underlying hashfunction, then hashfunction(key) is used as key.
	* <p>
	*
	* @param p the "key".
	* @param data the "text".
	* @return the pseudorandom bytes.
	*/
    public byte[] gen(byte[] key, byte[] text) {
        if (key.length > mdBlockSize_) {
            messageDigest_.update(key);
            key = messageDigest_.digest();
        }
        byte[] extKey = new byte[mdBlockSize_];
        System.arraycopy(key, 0, extKey, 0, key.length);
        byte[] keyXorIpad = xor(key, ipad_);
        messageDigest_.update(keyXorIpad);
        messageDigest_.update(text);
        byte[] tmp = messageDigest_.digest();
        byte[] keyXorOpad = xor(key, opad_);
        messageDigest_.update(keyXorOpad);
        messageDigest_.update(tmp);
        byte[] out = messageDigest_.digest();
        return out;
    }

    /**
	* Returns the lenght of the output. (20 for SHA-1).
	* <p>
	*
	* @return the output length.
	*/
    public int getOutputLength() {
        return messageDigest_.getDigestLength();
    }
}
