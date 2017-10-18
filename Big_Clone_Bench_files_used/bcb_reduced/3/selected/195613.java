package com.liferay.portal.security.pwd;

import com.liferay.portal.PwdEncryptorException;
import com.liferay.portal.kernel.util.Digester;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.StringMaker;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.util.PropsUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.vps.crypt.Crypt;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

/**
 * <a href="PwdEncryptor.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 * @author Scott Lee
 *
 */
public class PwdEncryptor {

    public static final String PASSWORDS_ENCRYPTION_ALGORITHM = GetterUtil.getString(PropsUtil.get(PropsUtil.PASSWORDS_ENCRYPTION_ALGORITHM)).toUpperCase();

    public static final String TYPE_CRYPT = "CRYPT";

    public static final String TYPE_MD2 = "MD2";

    public static final String TYPE_MD5 = "MD5";

    public static final String TYPE_NONE = "NONE";

    public static final String TYPE_SHA = "SHA";

    public static final String TYPE_SHA_256 = "SHA-256";

    public static final String TYPE_SHA_384 = "SHA-384";

    public static final String TYPE_SSHA = "SSHA";

    public static final char[] saltChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./".toCharArray();

    public static String encrypt(String clearTextPwd) throws PwdEncryptorException {
        return encrypt(PASSWORDS_ENCRYPTION_ALGORITHM, clearTextPwd, null);
    }

    public static String encrypt(String clearTextPwd, String currentEncPwd) throws PwdEncryptorException {
        return encrypt(PASSWORDS_ENCRYPTION_ALGORITHM, clearTextPwd, currentEncPwd);
    }

    public static String encrypt(String algorithm, String clearTextPwd, String currentEncPwd) throws PwdEncryptorException {
        if (algorithm.equals(TYPE_CRYPT)) {
            byte[] saltBytes = _getSaltFromCrypt(currentEncPwd);
            return encodePassword(algorithm, clearTextPwd, saltBytes);
        } else if (algorithm.equals(TYPE_NONE)) {
            return clearTextPwd;
        } else if (algorithm.equals(TYPE_SSHA)) {
            byte[] saltBytes = _getSaltFromSSHA(currentEncPwd);
            return encodePassword(algorithm, clearTextPwd, saltBytes);
        } else {
            return encodePassword(algorithm, clearTextPwd, null);
        }
    }

    protected static String encodePassword(String algorithm, String clearTextPwd, byte[] saltBytes) throws PwdEncryptorException {
        try {
            if (algorithm.equals(TYPE_CRYPT)) {
                return Crypt.crypt(clearTextPwd.getBytes(Digester.ENCODING), saltBytes);
            } else if (algorithm.equals(TYPE_SSHA)) {
                byte[] clearTextPwdBytes = clearTextPwd.getBytes(Digester.ENCODING);
                byte[] pwdPlusSalt = new byte[clearTextPwdBytes.length + saltBytes.length];
                System.arraycopy(clearTextPwdBytes, 0, pwdPlusSalt, 0, clearTextPwdBytes.length);
                System.arraycopy(saltBytes, 0, pwdPlusSalt, clearTextPwdBytes.length, saltBytes.length);
                MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
                byte[] pwdPlusSaltHash = sha1Digest.digest(pwdPlusSalt);
                byte[] digestPlusSalt = new byte[pwdPlusSaltHash.length + saltBytes.length];
                System.arraycopy(pwdPlusSaltHash, 0, digestPlusSalt, 0, pwdPlusSaltHash.length);
                System.arraycopy(saltBytes, 0, digestPlusSalt, pwdPlusSaltHash.length, saltBytes.length);
                BASE64Encoder encoder = new BASE64Encoder();
                return encoder.encode(digestPlusSalt);
            } else {
                return Digester.digest(algorithm, clearTextPwd);
            }
        } catch (NoSuchAlgorithmException nsae) {
            throw new PwdEncryptorException(nsae.getMessage());
        } catch (UnsupportedEncodingException uee) {
            throw new PwdEncryptorException(uee.getMessage());
        }
    }

    private static byte[] _getSaltFromCrypt(String cryptString) throws PwdEncryptorException {
        byte[] saltBytes = new byte[2];
        try {
            if (Validator.isNull(cryptString)) {
                Random randomGenerator = new Random();
                int numSaltChars = saltChars.length;
                StringMaker sm = new StringMaker();
                int x = Math.abs(randomGenerator.nextInt()) % numSaltChars;
                int y = Math.abs(randomGenerator.nextInt()) % numSaltChars;
                sm.append(saltChars[x]);
                sm.append(saltChars[y]);
                String salt = sm.toString();
                saltBytes = salt.getBytes(Digester.ENCODING);
            } else {
                String salt = cryptString.substring(0, 3);
                saltBytes = salt.getBytes(Digester.ENCODING);
            }
        } catch (UnsupportedEncodingException uee) {
            throw new PwdEncryptorException("Unable to extract salt from encrypted password: " + uee.getMessage());
        }
        return saltBytes;
    }

    private static byte[] _getSaltFromSSHA(String sshaString) throws PwdEncryptorException {
        byte[] saltBytes = new byte[8];
        if (Validator.isNull(sshaString)) {
            Random random = new SecureRandom();
            random.nextBytes(saltBytes);
        } else {
            BASE64Decoder decoder = new BASE64Decoder();
            try {
                byte[] digestPlusSalt = decoder.decodeBuffer(sshaString);
                byte[] digestBytes = new byte[digestPlusSalt.length - 8];
                System.arraycopy(digestPlusSalt, 0, digestBytes, 0, digestBytes.length);
                System.arraycopy(digestPlusSalt, digestBytes.length, saltBytes, 0, saltBytes.length);
            } catch (IOException ioe) {
                throw new PwdEncryptorException("Unable to extract salt from encrypted password: " + ioe.getMessage());
            }
        }
        return saltBytes;
    }
}
