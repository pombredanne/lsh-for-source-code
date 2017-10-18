package cn.ac.ntarl.umt.utils.pwd;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;
import cn.ac.ntarl.umt.config.Config;

/**
 * ͨ�������ļ���ȡ�Կ���ļ����㷨���
 * @author yhw
 *Oct 13, 2008
 */
public class PwdEncryptor {

    public static final String PASSWORDS_ENCRYPTION_ALGORITHM = Config.getInstance().getStringProp("PASSWORDS_ENCRYPTION_ALGORITHM", "NONE").trim();

    public static final String TYPE_MD2 = "MD2";

    public static final String TYPE_MD5 = "MD5";

    public static final String TYPE_NONE = "NONE";

    public static final String TYPE_SHA = "SHA";

    public static final String TYPE_SHA_256 = "SHA-256";

    public static final String TYPE_SHA_384 = "SHA-384";

    public static final String TYPE_SSHA = "SSHA";

    /**
	 * ���ü��ܷ���
	 * @param clearTextPwd
	 * @return
	 * @throws PwdEncryptorException
	 */
    public static String encrypt(String clearTextPwd) throws PwdEncryptorException {
        if (clearTextPwd == null || clearTextPwd.equals("")) {
            return clearTextPwd;
        }
        return encrypt(PASSWORDS_ENCRYPTION_ALGORITHM, clearTextPwd, null);
    }

    public static String encrypt(String clearTextPwd, String currentEncPwd) throws PwdEncryptorException {
        return encrypt(PASSWORDS_ENCRYPTION_ALGORITHM, clearTextPwd, currentEncPwd);
    }

    public static String encrypt(String algorithm, String clearTextPwd, String currentEncPwd) throws PwdEncryptorException {
        if (algorithm.equals(TYPE_NONE)) {
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
            if (algorithm.equals(TYPE_SSHA)) {
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
