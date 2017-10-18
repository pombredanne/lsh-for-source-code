package net.sf.javaguard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import net.sf.javaguard.classfile.ClassConstants;

/** Manifest file container for the obfuscator. Manages the Manifest file entry
 * names and attributes taken from at least one input stream so they can be
 * manipulated by the obfuscator when the obfuscated contents are written to
 * the output stream.
 *
 * @author <a href="mailto:theit@gmx.de">Thorsten Heit</a>
 */
public class ManifestContainer {

    /** Used when encoding a byte array as a Base64 string. */
    private static final char[] base64 = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/' };

    /** Used when encoding a byte array as a Base64 string. */
    private static final char pad = '=';

    /** Manifest file version number. */
    private static final String MANIFEST_VERSION_VALUE = "1.0";

    /** Manifest file digest algorithm tag. */
    private static final String MANIFEST_DIGESTALG_TAG = "Digest-Algorithms";

    /** Manifest file 'Created-By' tag. */
    private static final String MANIFEST_CREATEDBY_TAG = "Created-By";

    /** Holds the Manifest. */
    private Manifest manifest = null;

    /** Encode a byte[] as a Base64 string (see RFC1521, Section 5.2).
   * @param b the byte array
   * @return the Base64 string for the byte array
   */
    public static String toBase64(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int ptr = 0; ptr < b.length; ptr += 3) {
            sb.append(base64[(b[ptr] >> 2) & 0x3F]);
            if (ptr + 1 < b.length) {
                sb.append(base64[((b[ptr] << 4) & 0x30) | ((b[ptr + 1] >> 4) & 0x0F)]);
                if (ptr + 2 < b.length) {
                    sb.append(base64[((b[ptr + 1] << 2) & 0x3C) | ((b[ptr + 2] >> 6) & 0x03)]);
                    sb.append(base64[b[ptr + 2] & 0x3F]);
                } else {
                    sb.append(base64[(b[ptr + 1] << 2) & 0x3C]);
                    sb.append(pad);
                }
            } else {
                sb.append(base64[((b[ptr] << 4) & 0x30)]);
                sb.append(pad);
                sb.append(pad);
            }
        }
        return sb.toString();
    }

    /** Creates a new empty manifest file container.
   */
    public ManifestContainer() {
    }

    /** Creates a new manifest file container and adds the given manifest to the
   * internal manifest container.
   * @param mf the manifest
   */
    public ManifestContainer(Manifest mf) {
        addManifest(manifest);
    }

    /** Adds the given Manifest to the manifest container. The entry names and
   * attributes read will be merged in with the current manifest entries.
   * @param mf the manifest to add
   */
    public void addManifest(Manifest mf) {
        if (null != mf) {
            try {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                mf.write(os);
                byte[] bytes = os.toByteArray();
                os.close();
                ByteArrayInputStream is = new ByteArrayInputStream(bytes);
                getManifest().read(is);
                is.close();
            } catch (IOException ioex) {
                System.err.println("Unexpected I/O exception:" + ioex.getMessage());
                ioex.printStackTrace();
            }
        }
    }

    /** Writes the Manifest to the specified OutputStream.
   * Attributes.Name.MANIFEST_VERSION must be set in
   * MainAttributes prior to invoking this method.
   * @param os the output stream
   * @throws IOException if an I/O error has occurred
   */
    public void write(OutputStream os) throws IOException {
        String vername = Attributes.Name.MANIFEST_VERSION.toString();
        String version = getManifest().getMainAttributes().getValue(vername);
        if (null == version) {
            version = MANIFEST_VERSION_VALUE;
            getManifest().getMainAttributes().putValue(vername, version);
        }
        getManifest().getMainAttributes().putValue(MANIFEST_CREATEDBY_TAG, Version.getProgramName());
        getManifest().write(os);
    }

    /** Update an entry in the manifest file.
   * @param oldName the file name of the original file
   * @param newName the output file name
   * @param digests the message digests
   */
    public void updateManifest(String oldName, String newName, MessageDigest[] digests) {
        Attributes attrs = getManifest().getAttributes(oldName);
        if (null == attrs) {
            attrs = new Attributes();
        }
        if (digests != null && digests.length > 0) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < digests.length; i++) {
                if (i > 0) {
                    sb.append(" ");
                }
                sb.append(digests[i].getAlgorithm());
            }
            attrs.putValue(MANIFEST_DIGESTALG_TAG, sb.toString());
            for (int i = 0; i < digests.length; i++) {
                attrs.putValue(digests[i].getAlgorithm() + "-Digest", toBase64(digests[i].digest()));
            }
        }
        getManifest().getEntries().remove(oldName);
        getManifest().getEntries().put(newName, attrs);
        if (oldName.endsWith(ClassConstants.CLASS_EXT)) {
            Attributes mainAttrs = getManifest().getMainAttributes();
            if (null != mainAttrs) {
                String str = mainAttrs.getValue(Attributes.Name.MAIN_CLASS);
                int len = ClassConstants.CLASS_EXT.length();
                if (null != str && str.equals(oldName.substring(0, oldName.length() - len))) {
                    mainAttrs.put(Attributes.Name.MAIN_CLASS, newName.substring(0, newName.length() - len));
                }
            }
        }
    }

    /** Returns the current Manifest file parser.
   * @return current Manifest file parser
   */
    private Manifest getManifest() {
        if (null == manifest) {
            manifest = new Manifest();
        }
        return manifest;
    }
}
