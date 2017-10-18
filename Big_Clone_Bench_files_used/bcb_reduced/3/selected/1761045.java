package net.sf.mailsomething.auth.impl;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Vector;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import net.sf.mailsomething.auth.Challenge;

/**
 * @author Stig Tanggaard
 * @created 07-05-2003
 * 
 */
public class ChallengeImpl implements Challenge {

    private int challengeType;

    private String keyType;

    private byte[] encodedChallenge;

    private byte[] decodedChallenge;

    private boolean isSolved = false;

    private Vector listeners;

    public ChallengeImpl(String password) {
        challengeType = Challenge.VERIFY_PASS;
        keyType = "MD5";
        MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("MD5");
            algorithm.reset();
        } catch (NoSuchAlgorithmException f) {
            return;
        }
        byte[] pass = password.getBytes();
        decodedChallenge = getRandomBytes();
        byte[] buffer = new byte[pass.length + decodedChallenge.length];
        for (int i = 0; i < decodedChallenge.length; i++) buffer[i] = decodedChallenge[i];
        for (int i = 0; i < pass.length; i++) buffer[i + decodedChallenge.length] = pass[i];
        algorithm.update(buffer);
        encodedChallenge = algorithm.digest();
    }

    public ChallengeImpl(Key key) {
        challengeType = Challenge.VERIFY_KEY;
        keyType = key.getAlgorithm();
        try {
            Cipher cipher = Cipher.getInstance(keyType);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            decodedChallenge = getRandomBytes();
            encodedChallenge = cipher.doFinal(decodedChallenge);
        } catch (NoSuchAlgorithmException f) {
        } catch (InvalidKeyException f) {
        } catch (NoSuchPaddingException f) {
        } catch (BadPaddingException f) {
        } catch (IllegalBlockSizeException f) {
        }
    }

    /**
	 * @see net.sf.mailsomething.auth.Challenge#getChallenge()
	 */
    public byte[] getChallenge() {
        if (challengeType == Challenge.VERIFY_KEY) {
            return encodedChallenge;
        } else if (challengeType == Challenge.VERIFY_PASS) {
            return decodedChallenge;
        }
        return new byte[] {};
    }

    /**
	 * @see net.sf.mailsomething.auth.Challenge#respondToChallenge(byte[])
	 */
    public void respondToChallenge(byte[] response) {
        if (challengeType == Challenge.VERIFY_KEY) {
            if (response.equals(decodedChallenge)) {
                isSolved = true;
            }
        } else if (challengeType == Challenge.VERIFY_PASS) {
            if (response.equals(encodedChallenge)) {
                isSolved = true;
            }
        }
        for (int i = 0; i < listeners.size(); i++) {
            if (isSolved) ((ChallengeListener) listeners.elementAt(i)).challengeSuccess(this); else ((ChallengeListener) listeners.elementAt(i)).challengeFailure(this);
        }
    }

    /**
	 * @see net.sf.mailsomething.auth.Challenge#isSolved()
	 */
    public boolean isSolved() {
        return isSolved;
    }

    /**
	 * @see net.sf.mailsomething.auth.Challenge#getChallengeType()
	 */
    public int getChallengeType() {
        return challengeType;
    }

    /**
	 * @see net.sf.mailsomething.auth.Challenge#getKeyType()
	 */
    public String getKeyType() {
        return keyType;
    }

    protected static byte[] getRandomBytes() {
        return new byte[] { (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6 };
    }

    public void addChallengeListener(ChallengeListener listener) {
        if (listeners == null) listeners = new Vector();
        listeners.add(listener);
    }
}
