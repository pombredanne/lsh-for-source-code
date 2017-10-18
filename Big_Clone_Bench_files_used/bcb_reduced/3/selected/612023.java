package net.esle.sinadura.core;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import net.esle.sinadura.core.firma.exceptions.SinaduraCoreException;
import org.bouncycastle.util.encoders.Base64;
import sun.misc.BASE64Encoder;

/**
 * @author zylk.net
 */
public final class PasswordController {

    private static final String CHARSET_UTF8 = "UTF-8";

    private static final String TRANSFORMATION_MODE = "PBEWithMD5AndDES";

    private static Logger logger = Logger.getLogger(PasswordController.class.getName());

    /**
	 * @param passGeneral
	 * @return
	 * @throws SinaduraCoreException
	 */
    private static SecretKey getSecretKey(String passGeneral) throws SinaduraCoreException {
        SecretKey pbeKey = null;
        try {
            PBEKeySpec pbeKeySpec;
            SecretKeyFactory keyFac;
            pbeKeySpec = new PBEKeySpec(passGeneral.toCharArray());
            keyFac = SecretKeyFactory.getInstance(TRANSFORMATION_MODE);
            pbeKey = keyFac.generateSecret(pbeKeySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (InvalidKeySpecException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return pbeKey;
    }

    /**
	 * @return
	 */
    private static PBEParameterSpec getParameterSpec() {
        PBEParameterSpec pbeParamSpec;
        byte[] salt = { (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c, (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99 };
        int count = 20;
        pbeParamSpec = new PBEParameterSpec(salt, count);
        return pbeParamSpec;
    }

    /**
	 * @param passGeneral
	 * @param textoCifrado
	 * @return
	 * @throws SinaduraCoreException
	 */
    public static synchronized String desCifrar(String passGeneral, String textoCifrado) throws SinaduraCoreException {
        String textoPlano = null;
        try {
            SecretKey pbeKey = getSecretKey(passGeneral);
            PBEParameterSpec pbeParamSpec = getParameterSpec();
            Cipher desCipher;
            desCipher = Cipher.getInstance(TRANSFORMATION_MODE);
            desCipher.init(Cipher.DECRYPT_MODE, pbeKey, pbeParamSpec);
            byte[] cleartext1 = desCipher.doFinal(Base64.decode(textoCifrado.getBytes(CHARSET_UTF8)));
            textoPlano = new String(cleartext1, CHARSET_UTF8);
        } catch (InvalidKeyException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (NoSuchAlgorithmException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (IllegalBlockSizeException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (BadPaddingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return textoPlano;
    }

    /**
	 * @param passGeneral
	 * @param textoPlano
	 * @return
	 * @throws SinaduraCoreException
	 */
    public static synchronized String cifrar(String passGeneral, String textoPlano) throws SinaduraCoreException {
        String textoCifrado = null;
        try {
            byte[] cleartext = textoPlano.getBytes(CHARSET_UTF8);
            SecretKey pbeKey = getSecretKey(passGeneral);
            PBEParameterSpec pbeParamSpec = getParameterSpec();
            Cipher desCipher;
            desCipher = Cipher.getInstance(TRANSFORMATION_MODE);
            desCipher.init(Cipher.ENCRYPT_MODE, pbeKey, pbeParamSpec);
            byte[] ciphertext = desCipher.doFinal(cleartext);
            textoCifrado = new String(Base64.encode(ciphertext), CHARSET_UTF8);
        } catch (NoSuchAlgorithmException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (NoSuchPaddingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (InvalidKeyException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (IllegalBlockSizeException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (BadPaddingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (InvalidAlgorithmParameterException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return textoCifrado;
    }

    /**
	 * @param plaintext
	 * @return
	 * @throws SinaduraCoreException
	 */
    public static synchronized String encrypt(String plaintext) throws SinaduraCoreException {
        MessageDigest md = null;
        String hash = null;
        try {
            md = MessageDigest.getInstance("SHA");
            try {
                md.update(plaintext.getBytes(CHARSET_UTF8));
            } catch (UnsupportedEncodingException e) {
                throw new SinaduraCoreException(e.getMessage(), e);
            }
            byte raw[] = md.digest();
            hash = (new BASE64Encoder()).encode(raw);
        } catch (NoSuchAlgorithmException e) {
            throw new SinaduraCoreException(e.getMessage(), e);
        }
        return hash;
    }
}
