package com.i3sp.sso.security;

import java.security.*;
import java.lang.IndexOutOfBoundsException;
import com.i3sp.util.logging.Log;
import com.i3sp.sso.Logs;
import com.mortbay.Util.Code;

/** 
 * <code>PasswordSecure</code> performs one way encryption on a password.  It
 * uses <code>SaltGenerator</code> to obtain a salt for the password.  It can
 * also check an encrypted password against a non-encrypted one.
 * @see SaltGenerator
 * @version $Revision: 680 $ $Date: 2001-03-18 22:32:16 -0500 (Sun, 18 Mar 2001) $
 * @author Aoife Kavanagh (aoife)
 */
public class PasswordSecure {

    private MessageDigest digest;

    /**
     * Given a salt and a string password, create a new byte array, with
     * the salt at the start followed by the cleartext password.
     * @param salt the salt to be prepended to the password
     * @param password  the string password in string format.
     * @return the joined salt and password in byte array format.  The salt
     * will be the first part of the array.
     */
    public static byte[] prependSalt(byte[] salt, String password) {
        byte[] clear = new byte[salt.length + password.length()];
        System.arraycopy(salt, 0, clear, 0, salt.length);
        System.arraycopy(password.getBytes(), 0, clear, salt.length, password.length());
        return clear;
    }

    /**
     * Given a salt and a byte array, create a new byte array, with
     * the salt at the start followed by the byte array.  The byte array may
     * represent an unencrypted password or it may be the result of already calling digest.
     * @param salt the salt to be prepended to the password.
     * @param password the byte array to be appended.
     * @return the joined <code>salt</code> and <code>password</code> in byte
     * array format.
     */
    public static byte[] prependSalt(byte[] salt, byte[] password) {
        byte[] prepended = new byte[salt.length + password.length];
        System.arraycopy(salt, 0, prepended, 0, salt.length);
        System.arraycopy(password, 0, prepended, salt.length, password.length);
        return prepended;
    }

    /**
     * Give two byte arrays, check each individual entry to see if they match.
     * @param initial 
     * @param validate 
     * @return true if the byte arrays contain the same information, false otherwise.
     */
    public static boolean comparePasswords(byte[] initial, byte[] validate) {
        if (initial.length != validate.length) return false;
        for (int i = 0; i < initial.length; i++) {
            if (initial[i] != validate[i]) return false;
        }
        return true;
    }

    /**
     * Constructor. Obtains an algorithm for the specified encryption format
     * to be used. 
     * @param algorithm MD5 etc.
     * @exception PasswordSecureException 
     */
    public PasswordSecure(String algorithm) throws PasswordSecureException {
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsa) {
            Log.event(Logs.noMD5, nsa);
            throw new PasswordSecureException("Invalid hash algorithm");
        }
    }

    /**
     * Constructor.
     * Use the default MD5 algorithm. 
     * @exception PasswordSecureException 
     */
    public PasswordSecure() throws PasswordSecureException {
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException nsa) {
            Log.event(Logs.noMD5, nsa);
            throw new PasswordSecureException("Invalid hash algorithm");
        }
    }

    /**
     * Encrypt a cleartext password.  The salt is first added on to the
     * password before encryption occurs.  This allows for the scenario where
     * two different users may have the same password.  The new string is
     * then encrypted using a one way algorithm.  Before returning, the
     * cleartext string is prepended to the front of the encrypted string.
     * @param salt 
     * @param passwd 
     * @return The encrypted password in string format.  
     * @exception PasswordSecureException 
     */
    public synchronized String encrypt(byte[] salt, String passwd) throws PasswordSecureException {
        byte[] encrypted = encryptAsByte(salt, passwd);
        return new String(encrypted);
    }

    /**
     * As <code>encrypt ( byte[] salt, String passwd )</code> but returns the
     * encrypted password as a byte array instead of a string.
     * @param salt 
     * @param passwd 
     * @return The encrypted password as a byte array.
     * @exception PasswordSecureException 
     */
    public synchronized byte[] encryptAsByte(byte[] salt, String passwd) throws PasswordSecureException {
        digest.reset();
        if (salt == null || passwd == null) {
            Code.debug("Null salt or password entered");
            Log.event(Logs.encryptionFailure, new PasswordSecureException("Null salt or password"));
            throw new PasswordSecureException("Null salt or password passed");
        }
        byte[] cleartext = prependSalt(salt, passwd);
        byte[] digested = digest.digest(cleartext);
        byte[] encryptedPassword = prependSalt(salt, digested);
        return encryptedPassword;
    }

    /**
     * Encrypted the provided plain text password.  No salt has been provided
     * so it will create a new salt and then encrypt the password plus the
     * salt.  The plain text salt is appended on to the front of the returned value.
     * @param passwd 
     * @param saltLength 
     * @return The encrypted password with the prepended salt.
     * @exception PasswordSecureException 
     */
    public synchronized String encrypt(String passwd, int saltLength) throws PasswordSecureException {
        byte[] salt = SaltGenerator.createSalt(saltLength);
        return encrypt(salt, passwd);
    }

    /**
     * Take a clear text and an encrypted password and check to see if they
     * are the same.  As one way encryption is used it is not possible to
     * decrypt the encrypted password.  Instead, the plain password must be
     * encrypted and then compared.  The first <code>saltLength</code>
     * charactors of the encrypted password are the plaintext salt.  This is
     * prepended to the cleartext password which is then encrypted.  The
     * cleartext salt is then prepended to the newly encrypted password and a
     * comparison with <code>encrypted</code> can then take place.
     * @param encrypted the encrypted password retrieved from store.
     * @param plain the cleartext password to be compared against the stored password.
     * @param saltLength the length of the salt.
     * @return true if the passwords match, false otherwise.
     */
    public synchronized boolean matchPassword(byte[] encrypted, String plain, int saltLength) {
        if (encrypted == null) {
            return false;
        }
        if (saltLength == 0) return false;
        byte[] salt = new byte[saltLength];
        System.arraycopy(encrypted, 0, salt, 0, saltLength);
        try {
            byte[] validate = encryptAsByte(salt, plain);
            return comparePasswords(validate, encrypted);
        } catch (PasswordSecureException pse) {
            return false;
        }
    }
}
