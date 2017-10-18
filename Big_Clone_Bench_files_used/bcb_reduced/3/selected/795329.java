package com.organizadordeeventos.core.utils;

import java.security.MessageDigest;
import org.apache.commons.codec.binary.Base64;

/**
 * @autor Fernando Diaz
 */
public class PasswordUtils {

    public static String encrypt(final String pass) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            md.update(pass.getBytes("UTF-8"));
            return new String(Base64.encodeBase64(md.digest()));
        } catch (final Exception e) {
            throw new RuntimeException("No se pudo encriptar el password.", e);
        }
    }
}
