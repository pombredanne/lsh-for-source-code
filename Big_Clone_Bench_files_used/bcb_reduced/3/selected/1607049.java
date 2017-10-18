package com.android.im.imps;

import com.android.im.engine.ImException;
import com.android.im.plugin.PasswordDigest;
import org.apache.commons.codec.binary.Base64;
import android.security.MessageDigest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

public class StandardPasswordDigest implements PasswordDigest {

    public String digest(String schema, String nonce, String password) throws ImException {
        byte[] digestBytes;
        byte[] inputBytes;
        try {
            inputBytes = (nonce + password).getBytes("ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new ImException(e);
        }
        try {
            if ("SHA".equals(schema)) schema = "SHA-1";
            MessageDigest md = MessageDigest.getInstance(schema);
            digestBytes = md.digest(inputBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new ImException("Unsupported schema: " + schema);
        }
        return new String(Base64.encodeBase64(digestBytes));
    }

    public String[] getSupportedDigestSchema() {
        return new String[] { "MD5", "SHA" };
    }
}
