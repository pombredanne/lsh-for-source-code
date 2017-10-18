package com.kongur.star.venus.common.password.impl;

import com.eyeieye.melody.util.crypto.Crypto;
import com.eyeieye.melody.util.digest.MessageDigest;
import com.kongur.star.venus.common.password.PasswordValidator;

/**
 * ����ǩ���У����ʵ��
 * @author gaojf
 * @version $Id: PasswordValidatorImpl.java,v 0.1 2012-2-8 ����09:07:59 gaojf Exp $
 */
public class PasswordValidatorImpl implements PasswordValidator {

    private MessageDigest messageDigest;

    private Crypto crypto;

    @Override
    public boolean validate(String cipherText, String originalText, int pwdCryptType) {
        String newText = "";
        if (pwdCryptType == 1) {
            newText = crypto.encrypt(originalText, Crypto.Encoding.Base64);
        } else {
            newText = messageDigest.digest(originalText);
        }
        return newText.equals(cipherText);
    }

    @Override
    public String digest(String originalText, int pwdCryptType) {
        String pinBlock = "";
        if (pwdCryptType == 1) {
            pinBlock = crypto.encrypt(originalText, Crypto.Encoding.Base64);
        } else {
            pinBlock = messageDigest.digest(originalText);
        }
        return pinBlock;
    }

    @Override
    public String dectypt(String cipherText, int pwdCryptType) {
        String pinBlock = "";
        if (pwdCryptType == 1) {
            pinBlock = crypto.dectypt(cipherText, Crypto.Encoding.Base64);
        } else {
            pinBlock = cipherText;
        }
        return pinBlock;
    }

    public void setMessageDigest(MessageDigest messageDigest) {
        this.messageDigest = messageDigest;
    }

    public void setCrypto(Crypto crypto) {
        this.crypto = crypto;
    }
}
