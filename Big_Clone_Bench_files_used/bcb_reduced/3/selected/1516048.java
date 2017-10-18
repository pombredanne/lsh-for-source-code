package com.gpfcomics.android.cryptnos;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.spec.AlgorithmParameterSpec;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.RIPEMD160Digest;
import org.bouncycastle.crypto.digests.TigerDigest;
import org.bouncycastle.crypto.digests.WhirlpoolDigest;
import org.bouncycastle.util.encoders.Base64;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

/**
 * Encapsulates an atomic set of site parameters for Cryptnos.
 * @author Jeffrey T. Darlington
 * @version 1.0
 * @since 1.0
 */
public class SiteParameters {

    /** The site name or token. */
    private String site = null;

    /** The cryptographic hash to use to generate the passphrase. */
    private String hash = "SHA-1";

    /** The number of iterations of the cryptographic hash to perform. */
    private int iterations = 1;

    /** The type of characters to include/exclude from the final passphrase. */
    private int charTypes = 0;

    /** A limit on the number of characters returned in the passphrase.
	 * Should be -1 for no limit. */
    private int charLimit = -1;

    /** A unique but obscured version of the site token. */
    private String key = null;

    /** A reference to the top-level application.  This is primarily used to
	 *  get access to the error string resources of the application and
	 *  certain constants for encryption. */
    private CryptnosApplication theApp = null;

    /**
	 * Create a new, empty SiteParameters.  This is primarily intended for
	 * creating SiteParameters from parsing XML.
	 * @param theApp A reference to the full Cryptnos application, used
	 * primarily for notifications
	 */
    public SiteParameters(CryptnosApplication theApp) {
        this.theApp = theApp;
    }

    /**
	 * Create a new SiteParameters object from individual parameters.
	 * @param theApp A reference to the full Cryptnos application, used
	 * primarily for notifications
	 * @param site The site name or token.
	 * @param charTypes The type of characters to include/exclude from the
	 * final passphrase.
	 * @param charLimit A limit on the number of characters returned in the 
	 * passphrase.  Should be -1 for no limit.
	 * @param hash The cryptographic hash to use to generate the passphrase.
	 * @param iterations The number of iterations of the cryptographic hash to
	 * perform.
	 * @throws Exception Thrown when any parameter is incorrect.
	 */
    public SiteParameters(CryptnosApplication theApp, String site, int charTypes, int charLimit, String hash, int iterations) throws Exception {
        this.theApp = theApp;
        this.site = site;
        this.charTypes = charTypes;
        this.charLimit = charLimit;
        this.hash = hash;
        if (iterations <= 0) {
            throw new Exception(theApp.getResources().getString(R.string.error_bad_iterations));
        }
        this.iterations = iterations;
        key = generateKeyFromSite(site, theApp);
    }

    /**
	 * Create a new SiteParameters object from encrypted data, presumably
	 * loaded from a database.
	 * @param theApp A reference to the full Cryptnos application, used
	 * primarily for notifications
	 * @param siteKey An obscured site "token" that uniquely identifies the
	 * site parameters in the database.
	 * @param encryptedData A Base64-encoded encrypted string containing the
	 * bulk of the parameter data.
	 * @throws Exception Thrown when any error occurs reconstituting the
	 * encrypted data.
	 */
    public SiteParameters(CryptnosApplication theApp, String siteKey, String encryptedData) throws Exception {
        try {
            this.theApp = theApp;
            Cipher cipher = createCipher(siteKey, Cipher.DECRYPT_MODE);
            String combinedParams = new String(cipher.doFinal(Base64.decode(encryptedData.getBytes(theApp.getTextEncoding()))));
            String[] bits = combinedParams.split("\\|");
            if (bits.length == 5) {
                site = URLDecoder.decode(bits[0], CryptnosApplication.TEXT_ENCODING_UTF8);
                charTypes = Integer.parseInt(URLDecoder.decode(bits[1], CryptnosApplication.TEXT_ENCODING_UTF8));
                charLimit = Integer.parseInt(URLDecoder.decode(bits[2], CryptnosApplication.TEXT_ENCODING_UTF8));
                iterations = Integer.parseInt(URLDecoder.decode(bits[3], CryptnosApplication.TEXT_ENCODING_UTF8));
                hash = URLDecoder.decode(bits[4], CryptnosApplication.TEXT_ENCODING_UTF8);
                key = generateKeyFromSite(site, theApp);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(theApp.getResources().getString(R.string.error_bad_decrypt));
        }
    }

    /**
	 * Create a new SiteParameters object from unencrypted but compacted data,
	 * presumably read from an import file.
	 * @param theApp A reference to the full Cryptnos application, used
	 * primarily for notifications
	 * @param unEncryptedData A String on parameter data combined in a specific
	 * format
	 * @throws Exception Thrown when any error occurs reconstituting the
	 * parameter data.
	 */
    public SiteParameters(CryptnosApplication theApp, String unEncryptedData) throws Exception {
        try {
            this.theApp = theApp;
            String[] bits = unEncryptedData.split("\\|");
            if (bits.length == 5) {
                site = URLDecoder.decode(bits[0], CryptnosApplication.TEXT_ENCODING_UTF8);
                charTypes = Integer.parseInt(URLDecoder.decode(bits[1], CryptnosApplication.TEXT_ENCODING_UTF8));
                charLimit = Integer.parseInt(URLDecoder.decode(bits[2], CryptnosApplication.TEXT_ENCODING_UTF8));
                iterations = Integer.parseInt(URLDecoder.decode(bits[3], CryptnosApplication.TEXT_ENCODING_UTF8));
                hash = URLDecoder.decode(bits[4], CryptnosApplication.TEXT_ENCODING_UTF8);
                key = generateKeyFromSite(site, theApp);
            } else {
                throw new Exception();
            }
        } catch (Exception e) {
            throw new Exception(theApp.getResources().getString(R.string.error_bad_decrypt));
        }
    }

    /** Return the site name or token. */
    public String getSite() {
        return site;
    }

    /** Return the character types value. */
    public int getCharTypes() {
        return charTypes;
    }

    /** Return the character limit value.  This should be -1 for no limit. */
    public int getCharLimit() {
        return charLimit;
    }

    /** Return the cryptographic hash. */
    public String getHash() {
        return hash;
    }

    /** Return the number of iterations of the hash to perform. */
    public int getIterations() {
        return iterations;
    }

    /** Return the unique, obscured site token. */
    public String getKey() {
        if (key != null) return key;
        if (site != null) {
            key = generateKeyFromSite(site, theApp);
            return key;
        } else return null;
    }

    /**
	 * Set the site name or token.
	 * @param site The site name or token string to be used.
	 */
    public void setSite(String site) {
        this.site = site;
        key = generateKeyFromSite(site, theApp);
    }

    /**
	 * Set the character types value.
	 * @param charTypes The integer value to set.
	 */
    public void setCharTypes(int charTypes) {
        this.charTypes = charTypes;
    }

    /**
	 * Set the character limit value.
	 * @param charLimit The integer value to set.
	 */
    public void setCharLimit(int charLimit) {
        this.charLimit = charLimit;
    }

    /**
	 * Set the cryptographic hash.
	 * @param hash A string containing the name of the cryptographic hash.
	 */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
	 * Set the number of iterations of the cryptographic hash to perform.
	 * @param iterations The integer value to set.  This value must be greater
	 * than zero.
	 * @throws Exception Thrown when the value is less than or equal to zero.
	 */
    public void setIterations(int iterations) throws Exception {
        if (iterations > 0) this.iterations = iterations; else throw new Exception(theApp.getResources().getString(R.string.error_bad_iterations));
    }

    /**
	 * Export the current state of the site parameters as a compact single-line
	 * string of data.  The intended use of this string is to be fed to the
	 * export code, which will combine this value with other site parameters
	 * to create an export file.
	 * @return The encoded string
	 * @throws Exception Thrown if any error occurs during encoding
	 */
    public String exportUnencryptedString() throws Exception {
        try {
            String combinedParams = URLEncoder.encode(site, CryptnosApplication.TEXT_ENCODING_UTF8) + "|" + URLEncoder.encode(Integer.toString(charTypes), CryptnosApplication.TEXT_ENCODING_UTF8) + "|" + URLEncoder.encode(Integer.toString(charLimit), CryptnosApplication.TEXT_ENCODING_UTF8) + "|" + URLEncoder.encode(Integer.toString(iterations), CryptnosApplication.TEXT_ENCODING_UTF8) + "|" + URLEncoder.encode(hash, CryptnosApplication.TEXT_ENCODING_UTF8);
            return combinedParams;
        } catch (Exception e) {
            throw new Exception(theApp.getResources().getString(R.string.error_bad_encrypt));
        }
    }

    /**
	 * Export the current state of the site parameters as a Base64-encoded
	 * encrypted string, suitable for storing in a database.  Use the second
	 * constructor to reconstitute this string into its original form.
	 * @return A Base64-encoded encrypted string.
	 * @throws Exception Throw if an error occurs while encrypting the data.
	 */
    public String exportEncryptedString() throws Exception {
        try {
            String combinedParams = exportUnencryptedString();
            Cipher cipher = createCipher(key, Cipher.ENCRYPT_MODE);
            return base64String(cipher.doFinal(combinedParams.getBytes(theApp.getTextEncoding())));
        } catch (Exception e) {
            throw new Exception(theApp.getResources().getString(R.string.error_bad_encrypt));
        }
    }

    /**
	 * Given the user's secret passphrase, combine it with all the other
	 * site parameters saved within to produce the generated password and
	 * return it to the theApp.
	 * @param secret The user's secret passphrase, which is never stored.
	 * @return A pseudo-random password generated from the site parameters.
	 * @throws Exception Thrown when any error occurs.
	 */
    public String generatePassword(String secret) throws Exception {
        try {
            return generatePassword(secret, null);
        } catch (Exception e) {
            throw e;
        }
    }

    /**
	 * Given the user's secret passphrase, combine it with all the other
	 * site parameters saved within to produce the generated password and
	 * return it to the theApp.
	 * @param secret The user's secret passphrase, which is never stored.
	 * @param handler If not null, this handler will be notified of the
	 * progress of the generation process, for the purpose of updating a
	 * progress dialog, for example.
	 * @return A pseudo-random password generated from the site parameters.
	 * @throws Exception Thrown when any error occurs.
	 */
    public String generatePassword(String secret, Handler handler) throws Exception {
        Message msg = null;
        Bundle b = null;
        ;
        try {
            if (charLimit >= 0 && iterations > 0) {
                byte[] result = site.concat(secret).getBytes(theApp.getTextEncoding());
                MessageDigest internalHasher = null;
                Digest bcHasher = null;
                if (hash.compareTo("MD5") == 0 || hash.compareTo("SHA-1") == 0 || hash.compareTo("SHA-256") == 0 || hash.compareTo("SHA-384") == 0 || hash.compareTo("SHA-512") == 0) {
                    internalHasher = MessageDigest.getInstance(hash);
                } else if (hash.compareTo("RIPEMD-160") == 0) {
                    bcHasher = new RIPEMD160Digest();
                } else if (hash.compareTo("Tiger") == 0) {
                    bcHasher = new TigerDigest();
                } else if (hash.compareTo("Whirlpool") == 0) {
                    bcHasher = new WhirlpoolDigest();
                }
                if (internalHasher != null) {
                    for (int i = 0; i < iterations; i++) {
                        result = internalHasher.digest(result);
                        if (handler != null) {
                            msg = handler.obtainMessage();
                            b = new Bundle();
                            b.putInt("iteration", i);
                            b.putString("password", null);
                            msg.setData(b);
                            handler.sendMessage(msg);
                        }
                    }
                } else if (bcHasher != null) {
                    for (int i = 0; i < iterations; i++) {
                        bcHasher.update(result, 0, result.length);
                        result = new byte[bcHasher.getDigestSize()];
                        bcHasher.doFinal(result, 0);
                        bcHasher.reset();
                        if (handler != null) {
                            msg = handler.obtainMessage();
                            b = new Bundle();
                            b.putInt("iteration", i);
                            b.putString("password", null);
                            msg.setData(b);
                            handler.sendMessage(msg);
                        }
                    }
                }
                if (result != null) {
                    String b64hash = base64String(result);
                    Pattern p = null;
                    switch(charTypes) {
                        case 1:
                            p = Pattern.compile("\\W");
                            b64hash = p.matcher(b64hash).replaceAll("_");
                            break;
                        case 2:
                            p = Pattern.compile("[^a-zA-Z0-9]");
                            b64hash = p.matcher(b64hash).replaceAll("");
                            break;
                        case 3:
                            p = Pattern.compile("[^a-zA-Z]");
                            b64hash = p.matcher(b64hash).replaceAll("");
                            break;
                        case 4:
                            p = Pattern.compile("\\D");
                            b64hash = p.matcher(b64hash).replaceAll("");
                            break;
                        default:
                            break;
                    }
                    if (charLimit > 0 && b64hash.length() > charLimit) b64hash = b64hash.substring(0, charLimit);
                    if (handler != null) {
                        msg = handler.obtainMessage();
                        b = new Bundle();
                        b.putInt("iteration", -100);
                        b.putString("password", b64hash);
                        msg.setData(b);
                        handler.sendMessage(msg);
                    }
                    return b64hash;
                } else {
                    throw new Exception(theApp.getResources().getString(R.string.error_null_hash));
                }
            } else {
                if (iterations <= 0) throw new Exception(theApp.getResources().getString(R.string.error_bad_iterations)); else if (charLimit < 0) throw new Exception(theApp.getResources().getString(R.string.error_bad_charlimit)); else throw new Exception(theApp.getResources().getString(R.string.error_unknown));
            }
        } catch (Exception e) {
            throw e;
        }
    }

    /**
	 * Generate a unique, obscured "site key" from the specified site token
	 * or name.  This key allows us to uniquely identify the site parameters
	 * without revealing the actual site token value.  Note that if an error
	 * occurs while the key is being generated, the site value passed in will
	 * be returned instead, offering no additional security.
	 * @param theSite The site name or token to generate the key from.
	 * @return A Base64-encoded site key string.
	 */
    public static String generateKeyFromSite(String theSite, CryptnosApplication theApp) {
        try {
            if (theSite != null && theSite.length() > 0) {
                MessageDigest hasher = MessageDigest.getInstance("SHA-512");
                return base64String(hasher.digest(theSite.concat("android_id").getBytes(theApp.getTextEncoding())));
            } else return theSite;
        } catch (Exception e) {
            return theSite;
        }
    }

    /**
	 * Given a byte array, return a Base64-encoded string of its value.  This
	 * method was added because there's no simple, single method way to do
	 * this, and we need to perform this task multiple times.  Abstraction
	 * and code reuse is good. ;)
	 * @param bytes The byte array to encode.
	 * @return A Base64-encoded string.
	 */
    private static String base64String(byte[] bytes) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Base64.encode(bytes, baos);
            baos.flush();
            baos.close();
            return baos.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
	 * Create the cipher used to encrypt or decrypt site parameter data.
	 * @param password A "secret" String value, usually the derived site
	 * "key".  This is specified as an input parameter rather than using the
	 * member variable because this method will be needed for one of the
	 * constructors.
	 * @param mode The Cipher encrypt/decryption mode.  This should be either
	 * Cipher.ENCRYPT_MODE or Cipher.DECRYPT_MODE.
	 * @return A Cipher suitable for the encryption/decryption task.
	 * @throws Exception Thrown if the mode is invalid or if any error occurs
	 * while creating the cipher.
	 */
    private static Cipher createCipher(String password, int mode) throws Exception {
        try {
            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), CryptnosApplication.PARAMETER_SALT, CryptnosApplication.KEY_ITERATION_COUNT, CryptnosApplication.KEY_LENGTH);
            SecretKeyFactory keyFac = SecretKeyFactory.getInstance(CryptnosApplication.KEY_FACTORY);
            SecretKey key = keyFac.generateSecret(pbeKeySpec);
            AlgorithmParameterSpec aps = new PBEParameterSpec(CryptnosApplication.PARAMETER_SALT, CryptnosApplication.KEY_ITERATION_COUNT);
            Cipher cipher = Cipher.getInstance(CryptnosApplication.KEY_FACTORY);
            switch(mode) {
                case Cipher.ENCRYPT_MODE:
                    cipher.init(Cipher.ENCRYPT_MODE, key, aps);
                    break;
                case Cipher.DECRYPT_MODE:
                    cipher.init(Cipher.DECRYPT_MODE, key, aps);
                    break;
                default:
                    throw new Exception("Invalid cipher mode");
            }
            return cipher;
        } catch (Exception e) {
            throw e;
        }
    }
}
