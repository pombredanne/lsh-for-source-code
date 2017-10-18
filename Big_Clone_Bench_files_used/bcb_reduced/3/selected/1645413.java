package be.lalameliegeoise.website.tools.sync.signature;

import static be.lalameliegeoise.website.tools.sync.FileUtils.readBinaryFile;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import be.lalameliegeoise.website.tools.sync.FileUtils;

/**
 * @author Cyril Briquet
 */
public class FileSignatureFactory {

    private final String websiteBaseDirectory;

    public FileSignatureFactory(String websiteBaseDirectory) {
        if (websiteBaseDirectory == null) {
            throw new NullPointerException("illegal website base directory");
        }
        this.websiteBaseDirectory = websiteBaseDirectory;
    }

    public FileSignature createFileSignature(String filePath) throws IOException {
        if (filePath == null) {
            throw new NullPointerException("null file path");
        }
        if (filePath.endsWith(FileUtils.FILE_SEP)) {
            throw new IllegalArgumentException("target file is a directory");
        }
        final byte[] rawFileContents = readBinaryFile(filePath);
        final byte[] rawDigitalSignature = computeChecksum(rawFileContents);
        return createFileSignature(filePath, byteArrayToHexStringArray(rawDigitalSignature));
    }

    public FileSignature createDirectorySignature(String filePath) {
        if (filePath == null) {
            throw new NullPointerException("null file path");
        }
        return createFileSignature(filePath, "no-digital-signature");
    }

    public FileSignature createFileSignature(String filePath, String digitalSignature) {
        if (filePath.startsWith(this.websiteBaseDirectory)) {
            filePath = filePath.substring(this.websiteBaseDirectory.length());
        }
        return new FileSignature(filePath, digitalSignature);
    }

    private static byte[] computeChecksum(byte[] data) {
        if (data == null) {
            throw new NullPointerException("illegal data");
        }
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String byteToHexString(byte b) {
        final StringBuilder sb = new StringBuilder();
        final int i = b + (2 << 7);
        sb.append("0x").append(Integer.toHexString(i).toUpperCase());
        return sb.toString();
    }

    private static String byteArrayToHexStringArray(byte[] b) {
        if (b == null) {
            throw new NullPointerException("illegal byte array");
        }
        final StringBuilder sbDigitalSignature = new StringBuilder();
        for (int i = 0; i < b.length; ++i) {
            sbDigitalSignature.append(byteToHexString(b[i]));
            if (i < b.length - 1) {
                sbDigitalSignature.append(",");
            }
        }
        return sbDigitalSignature.toString();
    }
}
