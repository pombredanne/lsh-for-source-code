package com.google.web.bindery.requestfactory.vm.impl;

import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.commons.Method;
import com.google.gwt.dev.util.StringKey;
import com.google.gwt.user.server.Base64Utils;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for encoding RequestFactory operation ids.
 */
public class OperationKey extends StringKey {

    /**
   * The expected length of values returned from {@link #hash(String)}.
   */
    public static final int HASH_LENGTH = 28;

    static {
        assert HASH_LENGTH == hash("").length();
    }

    /**
   * Compute a base64-encoded SHA1 hash of the input string's UTF8
   * representation. The result is a {@value #HASH_LENGTH}-character sequence.
   */
    public static String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            byte[] data = md.digest(raw.getBytes("UTF-8"));
            return Base64Utils.toBase64(data);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("No MD5 algorithm", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8", e);
        }
    }

    private static String key(String requestContextBinaryName, String methodName, String descriptor) {
        Method m = new Method(methodName, Type.VOID_TYPE, Type.getArgumentTypes(descriptor));
        String raw = requestContextBinaryName + "::" + methodName + m.getDescriptor();
        return raw.length() >= HASH_LENGTH ? hash(raw) : raw;
    }

    public OperationKey(String encoded) {
        super(encoded);
        assert encoded.length() == HASH_LENGTH : "Expecting only " + HASH_LENGTH + " characters, received " + encoded.length();
    }

    public OperationKey(String requestContextBinaryName, String methodName, String descriptor) {
        super(key(requestContextBinaryName, methodName, descriptor));
    }
}
