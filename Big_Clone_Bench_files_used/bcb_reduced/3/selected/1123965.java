package gnu.classpath.tools.jarsigner;

import gnu.classpath.Configuration;
import gnu.java.security.hash.Sha160;
import gnu.java.security.util.Base64;
import gnu.java.util.jar.JarUtils;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Logger;

/**
 * Collection of utility methods used in JAR file signing and verification.
 */
class HashUtils {

    private static final Logger log = Logger.getLogger(HashUtils.class.getName());

    private Sha160 sha = new Sha160();

    /**
   * @param stream the input stream to digest.
   * @return a base-64 representation of the resulting SHA-1 digest of the
   *         contents of the designated input stream.
   * @throws IOException if an I/O related exception occurs during the process.
   */
    String hashStream(InputStream stream) throws IOException {
        BufferedInputStream bis = new BufferedInputStream(stream, 4096);
        byte[] buffer = new byte[4096];
        int count = 0;
        int n;
        while ((n = bis.read(buffer)) != -1) if (n > 0) {
            sha.update(buffer, 0, n);
            count += n;
        }
        byte[] hash = sha.digest();
        if (Configuration.DEBUG) log.finest("Hashed " + count + " byte(s)");
        String result = Base64.encode(hash);
        return result;
    }

    /**
   * @param ba the byte array to digest.
   * @return a base-64 representation of the resulting SHA-1 digest of the
   *         contents of the designated buffer.
   */
    String hashByteArray(byte[] ba) throws IOException {
        sha.update(ba);
        byte[] hash = sha.digest();
        if (Configuration.DEBUG) log.finest("Hashed " + ba.length + " byte(s)");
        String result = Base64.encode(hash);
        return result;
    }

    /**
   * @param name the JAR entry name
   * @param entryHash the hash of the entry file which appears in the
   *          manifest.
   * @return the base-64 encoded form of the hash of the corresponding Manifest
   *         JAR entry which will appear in the SF file under the entry with the
   *         same name.
   * @throws UnsupportedEncodingException If UTF-8 character encoding is not
   *           supported on this platform.
   */
    String hashManifestEntry(String name, String entryHash) throws UnsupportedEncodingException {
        sha.update((JarUtils.NAME + ": " + name).getBytes("UTF-8"));
        sha.update(JarUtils.CRLF);
        sha.update((Main.DIGEST + ": " + entryHash).getBytes("UTF-8"));
        sha.update(JarUtils.CRLF);
        sha.update(JarUtils.CRLF);
        byte[] sfHash = sha.digest();
        String result = Base64.encode(sfHash);
        return result;
    }
}
