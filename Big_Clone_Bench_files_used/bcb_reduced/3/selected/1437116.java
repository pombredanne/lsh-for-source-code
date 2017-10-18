package org.makagiga.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;

/**
 * @since 2.0
 */
public final class Checksum {

    public static Result get(final String algorithm, final InputStream input) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        DigestInputStream dis = new DigestInputStream(input, digest);
        FS.copyStream(dis, null);
        Result result = new Result();
        result.digest = digest.digest();
        return result;
    }

    public static Result get(final String algorithm, final File file) throws IOException, NoSuchAlgorithmException {
        FS.BufferedFileInput input = null;
        try {
            input = new FS.BufferedFileInput(file);
            return get(algorithm, input);
        } finally {
            FS.close(input);
        }
    }

    /**
	 * CREDITS: http://blogs.sun.com/roller/page/andreas?entry=hashing_a_file_in_3
	 */
    public static String toString(final byte[] digest) {
        Formatter f = new Formatter(Locale.US);
        return f.format("%0" + (digest.length * 2) + "x", new BigInteger(1, digest)).toString();
    }

    @Uninstantiable
    private Checksum() {
    }

    public static final class Result {

        private byte[] digest;

        private String string;

        public byte[] getDigest() {
            return Arrays.copyOf(digest, digest.length);
        }

        @Override
        public String toString() {
            if (string == null) string = Checksum.toString(digest);
            return string;
        }
    }
}
