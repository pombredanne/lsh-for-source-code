package com.datas.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.datas.bean.model.system.SysCounter;
import sun.misc.BASE64Encoder;

/**
 * @author kimi
 * 
 */
public class Utility {

    private static final Logger LOG = Logger.getLogger(Utility.class);

    public static String encryptPassword(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e);
        }
        try {
            md.update(password.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOG.error(e);
        }
        return (new BASE64Encoder()).encode(md.digest());
    }

    public static String getCounterValue(SysCounter counter) {
        StringBuilder counterValueBuilder = new StringBuilder();
        if (!StringUtils.isEmpty(counter.getPrefix())) {
            counterValueBuilder.append(counter.getPrefix());
        }
        if (!StringUtils.isEmpty(counter.getPadCharacter())) {
            for (int i = 0; i < counter.getLength() - (String.valueOf(counter.getValue()).length() + (counter.getPrefix() != null ? counter.getPrefix().length() : 0)); i++) {
                counterValueBuilder.append(counter.getPadCharacter());
            }
        } else {
            for (int i = 0; i < counter.getLength() - String.valueOf(counter.getValue()).length(); i++) {
                counterValueBuilder.append(counter.getPadCharacter());
            }
        }
        counterValueBuilder.append(counter.getValue());
        return counterValueBuilder.toString();
    }

    public static Long stringToLong(String value) {
        Long result;
        try {
            result = Long.parseLong(value);
        } catch (Exception e) {
            result = 0L;
        }
        return result;
    }

    public static String objectToString(Object value) {
        if (value != null) {
            return String.valueOf(value);
        }
        return null;
    }
}
