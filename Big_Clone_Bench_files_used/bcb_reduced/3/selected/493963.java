package net.sourceforge.javautil.common;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import net.sourceforge.javautil.common.exception.ThrowableManagerRegistry;
import net.sourceforge.javautil.common.io.IVirtualArtifactIO;
import net.sourceforge.javautil.common.io.IVirtualArtifactIOHandler;

/**
 * A simple utility that wraps common checksum routines.
 *
 * @author elponderador
 * @author $Author: ponderator $
 * @version $Id: ChecksumUtil.java 2297 2010-06-16 00:13:14Z ponderator $
 */
public class ChecksumUtil {

    public static final Pattern checksumPattern = Pattern.compile("^[0-9A-Fa-f]{10,}$");

    public static final String SHA1 = "SHA-1";

    public static final String MD5 = "MD5";

    /**
	 * @param url The url to download
	 * @param checksumTarget The target output stream for the checksum, or null if post processing of checksum is not desired
	 * @param downloadTarget The target output stream for the url
	 * @param requireChecksum True if a checksum file is required, which will abort the download if none are present, otherwise false
	 * @return Returns the type of checksum that was validated, null if no checksum
	 * 
	 * TODO: create specific download exception types
	 * @throws RuntimeException This will be thrown if no checksum is found or is invalid and require checksum is true
	 */
    public static String downloadWithChecksum(URL url, OutputStream checksumTarget, OutputStream downloadTarget, boolean requireChecksum) throws IOException, NoSuchAlgorithmException {
        URL checksumURL = null;
        String extension = null;
        for (String cse : new String[] { "sha1", "md5" }) {
            checksumURL = new URL(url.toExternalForm() + "." + cse);
            if (URLUtil.exists(checksumURL)) {
                extension = cse;
                break;
            }
        }
        if (checksumURL == null && requireChecksum) throw new RuntimeException("Required checksum not found for: " + url);
        if (checksumURL == null) {
            IOUtil.transfer(url.openStream(), downloadTarget);
        } else {
            ByteArrayOutputStream checksumOut = new ByteArrayOutputStream();
            IOUtil.transfer(checksumURL.openStream(), checksumTarget == null ? checksumOut : new IOUtil.MultiplexedOutputStream(checksumOut, checksumTarget));
            ChecksumUtil.ChecksumInputStream input = new ChecksumUtil.ChecksumInputStream(url.openStream(), ChecksumUtil.getAlgorithmFromExtension(extension));
            String checksumRaw = new String(checksumOut.toByteArray());
            String checksum = parseChecksumLine(checksumRaw);
            if (checksum == null) throw new RuntimeException("Checksum invalid: " + checksumRaw);
            IOUtil.transfer(input, downloadTarget);
            String calculated = ((ChecksumUtil.ChecksumInputStream) input).getChecksumHexed();
            if (!checksum.equals(calculated)) throw new RuntimeException("Checksum did not match: " + calculated + "/" + checksumRaw);
        }
        return ChecksumUtil.getAlgorithmFromExtension(extension);
    }

    /**
	 * Many checksum files have extra info like the filename and such, this will
	 * attempt to get the actual calculated checksum from the line.
	 * 
	 * @param checksumRaw
	 * @return
	 */
    public static String parseChecksumLine(String checksumRaw) {
        String[] items = checksumRaw.split(" ");
        for (int i = 0; i < items.length; i++) {
            items[i] = items[i].trim();
            if (checksumPattern.matcher(items[i]).matches()) {
                return items[i];
            }
        }
        return null;
    }

    /**
	 * @param extension The file extension
	 * @return The corresponding algorithm name, otherwise null
	 */
    public static String getAlgorithmFromExtension(String extension) {
        if (extension.equalsIgnoreCase("md5")) return MD5;
        if (extension.equalsIgnoreCase("sha1")) return SHA1;
        return null;
    }

    /**
	 * This will call {@link StringUtil#hexify(byte[])} on the results of a 
	 * call to {@link #createChecksum(String, byte[])}.
	 */
    public static String createChecksumHexed(String algorithm, byte[] source) {
        return StringUtil.hexify(createChecksum(algorithm, source));
    }

    /**
	 * @param algorithm The name of the algorithm to use
	 * @param source The byte array to use in calculating the checksum
	 * @return The checksum of the source
	 */
    public static byte[] createChecksum(String algorithm, byte[] source) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(source);
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * This will call {@link StringUtil#hexify(byte[])} on the results of a
	 * call to {@link #createChecksum(String, InputStream)}.
	 */
    public static String createChecksumHexed(String algorithm, InputStream is) {
        return StringUtil.hexify(createChecksum(algorithm, is));
    }

    /**
	 * This will read the entire input stream into memory, if this is not
	 * practical for your solution, use {@link ChecksumInputStream}.
	 * 
	 * @param algorithm The name of the algorithm
	 * @param is The input stream to read from
	 * @return The calculated checksum of the input stream read byte[]
	 */
    public static byte[] createChecksum(String algorithm, InputStream is) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            digest.update(IOUtil.read(is));
            return digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw ThrowableManagerRegistry.caught(e);
        }
    }

    /**
	 * This allows one to adapt a check sum calculator on a 
	 * {@link IVirtualArtifactIO} so as to calculate checksums
	 * on input streams. 
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ChecksumUtil.java 2297 2010-06-16 00:13:14Z ponderator $
	 */
    public static class ChecksumAdapter implements IVirtualArtifactIOHandler<IVirtualArtifactIO> {

        protected String algorithm;

        protected ChecksumInputStream input;

        public InputStream getInputStream(IVirtualArtifactIO artifact, InputStream original) {
            try {
                return input = new ChecksumInputStream(original, algorithm);
            } catch (NoSuchAlgorithmException e) {
                throw ThrowableManagerRegistry.caught(e);
            }
        }

        public OutputStream getOutputStream(IVirtualArtifactIO artifact, OutputStream original) {
            return original;
        }

        /**
		 * @return The algorithm that is being used by this adapter
		 */
        public String getAlgorithm() {
            return algorithm;
        }

        /**
		 * @return The input stream last created by this adapter
		 */
        public ChecksumInputStream getInput() {
            return input;
        }

        /**
		 * @return The hexified form of the checksum, or null if it has not been calculated yet
		 */
        public String getHexifiedChecksum() {
            return input == null ? null : StringUtil.hexify(input.getDigest());
        }
    }

    /**
	 * An input stream that will calculate a checksum at the same time
	 * an input stream is being read.
	 *
	 * @author elponderador
	 * @author $Author: ponderator $
	 * @version $Id: ChecksumUtil.java 2297 2010-06-16 00:13:14Z ponderator $
	 */
    public static class ChecksumInputStream extends FilterInputStream {

        /**
		 * The message digest used to calculate the checksum
		 */
        private MessageDigest digest;

        /**
		 * This will create a new digest using the named algorithm.
		 * See {@link MessageDigest#getInstance(String)}.
		 * 
		 * @see #ChecksumUtil(InputStream, MessageDigest)
		 * @throws NoSuchAlgorithmException
		 */
        public ChecksumInputStream(InputStream is, String algorithm) throws NoSuchAlgorithmException {
            this(is, MessageDigest.getInstance(algorithm));
        }

        /**
		 * @param is The input stream to read from
		 * @param digest The {@link #digest}
		 */
        public ChecksumInputStream(InputStream is, MessageDigest digest) {
            super(is);
            this.digest = digest;
        }

        @Override
        public int read() throws IOException {
            int b = super.read();
            if (b > -1) digest.update((byte) b);
            return b;
        }

        @Override
        public int read(byte[] b) throws IOException {
            int read = super.read(b);
            if (read != -1) {
                digest.update(b, 0, read);
            }
            return read;
        }

        /**
		 * @return The {@link #digest}
		 */
        public MessageDigest getMessageDigest() {
            return this.digest;
        }

        /**
		 * This should only be called once and only after the input stream
		 * has been fully read. This should not be called if you have already
		 * called {@link #getChecksumHexed()} and vice-versa.
		 * 
		 * @return The calculated checksum
		 * 
		 * @see MessageDigest#digest()
		 */
        public byte[] getDigest() {
            return digest.digest();
        }

        /**
		 * This should only be called once, see {@link #getDigest()}.
		 * 
		 * @return The hexed string representation of the byte[] digest.
		 */
        public String getChecksumHexed() {
            return StringUtil.hexify(digest.digest());
        }
    }
}
