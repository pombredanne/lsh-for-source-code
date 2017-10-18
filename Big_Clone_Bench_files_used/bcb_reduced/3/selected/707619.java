package passwd;

import java.lang.String;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.lang.Error;
import java.lang.System;

/**
 * This is the Visitor.
 */
class CheckPass {

    MessageDigest m;

    byte[] md5sum;

    boolean found;

    CheckPass(byte[] md5sum) {
        this.md5sum = md5sum;
        try {
            m = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            throw new Error(e);
        }
    }

    void check(String pass) {
        m.update(pass.getBytes());
        byte dig[] = m.digest();
        for (int i = 0; i < 16; i++) {
            if (dig[i] == md5sum[i]) {
                if (i == 15) {
                    System.out.println(pass);
                    found = true;
                }
            } else break;
        }
    }
}
