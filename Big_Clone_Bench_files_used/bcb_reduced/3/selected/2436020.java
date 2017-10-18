package org.apache.qpid.management.common.sasl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.harmony.javax.security.auth.callback.Callback;
import org.apache.harmony.javax.security.auth.callback.CallbackHandler;
import org.apache.harmony.javax.security.auth.callback.NameCallback;
import org.apache.harmony.javax.security.auth.callback.PasswordCallback;
import org.apache.harmony.javax.security.auth.callback.UnsupportedCallbackException;

public class UsernameHashedPasswordCallbackHandler implements CallbackHandler {

    private String user;

    private char[] pwchars;

    public UsernameHashedPasswordCallbackHandler(String user, String password) throws Exception {
        this.user = user;
        this.pwchars = getHash(password);
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback ncb = (NameCallback) callbacks[i];
                ncb.setName(user);
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback pcb = (PasswordCallback) callbacks[i];
                pcb.setPassword(pwchars);
            } else {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
        }
    }

    private void clearPassword() {
        if (pwchars != null) {
            for (int i = 0; i < pwchars.length; i++) {
                pwchars[i] = 0;
            }
            pwchars = null;
        }
    }

    protected void finalize() {
        clearPassword();
    }

    public static char[] getHash(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] data = text.getBytes("utf-8");
        MessageDigest md = MessageDigest.getInstance("MD5");
        for (byte b : data) {
            md.update(b);
        }
        byte[] digest = md.digest();
        char[] hash = new char[digest.length];
        int index = 0;
        for (byte b : digest) {
            hash[index++] = (char) b;
        }
        return hash;
    }
}
