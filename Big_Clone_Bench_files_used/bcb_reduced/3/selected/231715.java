package com.gpfcomics.android.ppp;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.ParametersWithIV;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * This class encapsulates functionality common to all activities within the Perfect
 * Paper Passwords application.  It controls the user's preferences as well as a
 * single, common instance of the database.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class PPPApplication extends Application {

    /** This constant is used to specify the name of our preferences file. */
    private static final String PREF_FILE = "PPPPrefs";

    /** This constant is used in the preferences file to identify the version of
	    Perfect Paper Passwords that is last wrote to the file. */
    private static final String PREF_VERSION = "version";

    /** This constant is used in the preferences file to identify the user's
	    password, if set.  If this option is not found in the preferences, there
		is no current password. */
    private static final String PREF_PASSWORD = "password";

    /** This constant is used in the preferences file to identify the user's
	    preference with regard to whether or not passcodes should be copied to
		the clipboard when they are "struck through". */
    private static final String PREF_COPY_PASSCODES_TO_CLIPBOARD = "pass_to_clip";

    /** This constant is used in the preferences file to identify the salt used
	 *  for cryptogrphaic operations. */
    private static final String PREF_SALT = "salt";

    /** The number of iterations used for cryptographic key generation, such
	 *  as in creating an AlgorithmParameterSpec.  Ideally, this should be
	 *  fairly high, but we'll use a modest value for performance. */
    private static final int KEY_ITERATION_COUNT = 50;

    /** The cryptographic hash to use to generate encryption salts.  Pass this
	 *  into MessageDigest.getInstance() to get the MessageDigest for salt
	 *  generation. */
    private static final String SALT_HASH = "SHA-512";

    /** A random-ish string for salting our encryption salts. */
    private static final String SALT = "cSg6Vo1mV3hsENK6njMIkr8adrZ4lbGByu8fd8PClRknqhEC8DOmbDCtgUAtbir";

    /** The character encoding used to convert strings to binary data, primarily
	 *  in cryptographic hash operations. */
    private static final String ENCODING = "UTF-8";

    /** The size of the AES encryption key in bits */
    private static final int KEY_SIZE = 256;

    /** The size of the AES encryption initialization vector (IV) in bits */
    private static final int IV_SIZE = 128;

    /** A reference to the application's database helper.  Activities will get
	    copies of this reference, but the application will own the master copy. */
    private static CardDBAdapter DBHelper = null;

    /** A referenceto the application's shared preferences.  Activities will get
	    copies of this reference, but the application will own the master copy. */
    private static SharedPreferences prefs = null;

    /** Whether or not to copy passcodes to the clipboard when they are "struck
	    through" in the card view. */
    private static boolean copyPasscodes = true;

    /** A convenience reference to our numeric version code */
    private static int versionCode = -1;

    /** A convenience reference to our version "number" string */
    private static String versionName = null;

    /** An cipher for the encryption and decryption of sequence keys */
    private static BufferedBlockCipher cipher = null;

    /** The initialization vector (IV) used by our cipher */
    private static ParametersWithIV iv = null;

    private byte[] salt = null;

    @Override
    public void onCreate() {
        super.onCreate();
        DBHelper = new CardDBAdapter(this);
        DBHelper.open();
        try {
            prefs = getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_META_DATA);
            versionCode = info.versionCode;
            versionName = info.versionName;
            int oldVersion = prefs.getInt(PREF_VERSION, -1);
            if (oldVersion == -1) {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt(PREF_VERSION, versionCode);
                editor.putBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD, copyPasscodes);
                editor.commit();
            } else if (oldVersion > versionCode) {
            } else if (versionCode > oldVersion) {
            }
            copyPasscodes = prefs.getBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD, true);
            String saltString = prefs.getString(PREF_SALT, null);
            if (saltString == null) {
                SecureRandom devRandom = new SecureRandom();
                salt = new byte[512];
                devRandom.nextBytes(salt);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString(PREF_SALT, bytesToHexString(salt));
                editor.commit();
            } else {
                salt = hexStringToBytes(saltString);
            }
            if (promptForPassword()) createCipher();
        } catch (Exception e) {
        }
    }

    /**
	 * Get the common database helper
	 * @return The common database helper
	 */
    public CardDBAdapter getDBHelper() {
        return DBHelper;
    }

    /**
	 * Should we prompt the user for their password?
	 * @return True if the user has set a password and they should be prompted for
	 * it, false if no password has been set.
	 */
    public boolean promptForPassword() {
        String password = prefs.getString(PREF_PASSWORD, null);
        return password != null;
    }

    /**
	 * Validate the supplied password to make sure it matches the password stored
	 * in the preferences file
	 * @param password The plain-text password to validate
	 * @return True if the password matches, false otherwise
	 */
    public boolean isValidPassword(String password) {
        try {
            String stored_password = prefs.getString(PREF_PASSWORD, null);
            if (stored_password == null) return false;
            String enc_password = encryptPassword(password);
            return enc_password.compareTo(stored_password) == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Should we copy a passcode to the system clipboard when the user "strikes
	 * through" it in the card view activity?
	 * @return True or false
	 */
    public boolean copyPasscodesToClipboard() {
        return copyPasscodes;
    }

    /**
	 * Toggle the "copy passcode to the clipboard" setting and store the new value
	 * to the system preferences
	 * @return True on success, false on failure
	 */
    public boolean toggleCopyPasscodesSetting() {
        try {
            copyPasscodes = !copyPasscodes;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(PREF_COPY_PASSCODES_TO_CLIPBOARD, copyPasscodes);
            editor.commit();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Convert an array of bytes into the equivalent hexadecimal string
     * @param bytes An array of bytes
     * @return The equivalent string in upper-case hexadecimal digits.  If the
     * input array happens to be null, this will also return a null string.
     */
    public static String bytesToHexString(byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%1$02X", b));
        return sb.toString().toUpperCase();
    }

    /**
     * Convert a string of hexadecimal characters to the equivalent byte array
     * @param hex A string of hexadecimal characters.  The alphabetic characters
     * may be in upper or lower case, and the length of the string must be
     * divisible by two.
     * @return A byte array containing the decoded data.
     * @throws IllegalArgumentException Thrown if the input string is not a valid
     * hexadecimal string
     */
    public static byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.length() == 0 || hex.length() % 2 != 0) throw new IllegalArgumentException("Invalid hexadecimal string");
        if (!Pattern.matches("^[0-9a-fA-F]+$", hex)) throw new IllegalArgumentException("Invalid hexadecimal string");
        int outputSize = hex.length() / 2;
        byte[] out = new byte[outputSize];
        String temp = null;
        for (int i = 0; i < outputSize; ++i) {
            temp = hex.substring(i * 2, i * 2 + 2);
            try {
                int b = Integer.parseInt(temp, 16);
                out[i] = (byte) b;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hexadecimal string");
            }
        }
        return out;
    }

    /**
     * Get the numeric application version code number
     * @return
     */
    public static int getVersionCode() {
        return versionCode;
    }

    /**
     * Get the user-friendly application version "number" string for display
     * @return
     */
    public static String getVersionName() {
        return versionName;
    }

    /**
	 * Set the user's password and store its encrypted value to the preferences file
	 * @param password The new plain-text password
	 * @return True on success, false on failure
	 */
    boolean setPassword(String password) {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_PASSWORD, encryptPassword(password));
            editor.commit();
            createCipher();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Clear the user's stored password.  Note that this should only be called if
	 * the user's database has been cleared first.
	 * @return True on success, false on failure
	 */
    boolean clearPassword() {
        try {
            SharedPreferences.Editor editor = prefs.edit();
            editor.remove(PREF_PASSWORD);
            editor.commit();
            createCipher();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
	 * Encrypt the specified sequence key string and return the encrypted result.
	 * If an application password has not be specified or the encryption fails
	 * for whatever reason, the original string will be returned.
	 * @param original The sequence key string to encrypt
	 * @return The encrypted sequence key string
	 */
    String encryptSequenceKey(String original) {
        try {
            byte[] output = cryptSeqKey(original.getBytes(ENCODING), Cipher.ENCRYPT_MODE);
            if (output == null) return original; else return bytesToHexString(output);
        } catch (Exception e) {
            return original;
        }
    }

    /**
	 * Decrypt the specified sequence key string and return the plain text result.
	 * If an application password has not be specified or the decryption fails
	 * for whatever reason, the original string will be returned.
	 * @param original The sequence key string to decrypt
	 * @return The decrypted sequence key string
	 */
    String decryptSequenceKey(String original) {
        try {
            byte[] output = cryptSeqKey(hexStringToBytes(original), Cipher.DECRYPT_MODE);
            if (output == null) return original; else {
                String outString = new String(output, ENCODING);
                return outString.trim();
            }
        } catch (Exception e) {
            return original;
        }
    }

    /**
     * Encrypt the supplied password using a common one-way algorithm
     * @param password The plain-text password to encrypt
     * @return The encrypted password, or null on failure
     */
    private static String encryptPassword(String password) {
        try {
            MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
            byte[] digest = hasher.digest(password.getBytes(ENCODING));
            for (int i = 0; i < 9; i++) digest = hasher.digest(digest);
            return bytesToHexString(digest);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Create the encryption cipher needed to securely store and retrieve encrypted
     * sequence keys in the database.  Note that this cipher will only be created if
     * the user's password is set; otherwise, the cipher will default to null.
     */
    private void createCipher() {
        try {
            String password = prefs.getString(PREF_PASSWORD, null);
            if (password != null) {
                String uniqueID = null;
                try {
                    AndroidID id = AndroidID.newInstance(this);
                    uniqueID = id.getAndroidID();
                } catch (Exception e1) {
                }
                if (uniqueID == null) uniqueID = SALT; else uniqueID = uniqueID.concat(SALT);
                byte[] uniqueIDBytes = uniqueID.getBytes(ENCODING);
                byte[] finalSalt = new byte[uniqueIDBytes.length + salt.length];
                for (int i = 0; i < uniqueIDBytes.length; i++) {
                    finalSalt[i] = uniqueIDBytes[i];
                }
                for (int j = 0; j < salt.length; j++) {
                    finalSalt[uniqueIDBytes.length + j] = salt[j];
                }
                MessageDigest hasher = MessageDigest.getInstance(SALT_HASH);
                for (int i = 0; i < KEY_ITERATION_COUNT; i++) finalSalt = hasher.digest(finalSalt);
                byte[] pwd = password.concat(uniqueID).getBytes(ENCODING);
                for (int i = 0; i < KEY_ITERATION_COUNT; i++) pwd = hasher.digest(pwd);
                PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator();
                generator.init(pwd, finalSalt, KEY_ITERATION_COUNT);
                iv = ((ParametersWithIV) generator.generateDerivedParameters(KEY_SIZE, IV_SIZE));
                RijndaelEngine engine = new RijndaelEngine();
                cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(engine));
            } else {
                cipher = null;
                iv = null;
            }
        } catch (Exception e) {
            cipher = null;
            iv = null;
        }
    }

    /**
     * Either encrypt or decrypt the specified byte array.  Whichever mode
     * we use depends on the mode specified.  I pulled this out into a single
     * private method because the process is the same either way with the exception
     * of the mode (encrypt or decyrpt).  Other classes will use the protected
     * encryptSequenceKey() and decryptSequenceKey() methods instead, which will
     * make sure the right mode gets called.
	 * @param original The sequence key to encrypt/decrypt.  Note that this is a
	 * byte array and not a string.  When encrypting, use String.getBytes() to
	 * convert the string to raw bytes first.  When decrypting, use
	 * hexStringToBytes() to convert the encrypted string in hex format to bytes.
     * @param mode The mode.  Must be either Cipher.ENCRYPT_MODE or
     * Cipher.DECRYPT_MODE.
     * @return The encrypted or decrypted data as a byte array.  For encrypted data,
     * this may be converted to a string of hexadecimal characters suitable for
     * passing back into this method (via decryptSequenceKey()).  For decrypted data,
     * this data can be converted back to a string using the String(byte[], encoding)
     * constructor, although you should also do a String.trim() on that result to
     * remove extraneous nulls from the end.
     */
    private byte[] cryptSeqKey(byte[] original, int mode) {
        if (original == null || original.length == 0 || cipher == null) return null;
        try {
            if (mode == Cipher.ENCRYPT_MODE) cipher.init(true, iv); else cipher.init(false, iv);
            byte[] result = new byte[cipher.getOutputSize(original.length)];
            int bytesSoFar = cipher.processBytes(original, 0, original.length, result, 0);
            cipher.doFinal(result, bytesSoFar);
            return result;
        } catch (Exception e) {
            return null;
        }
    }
}
