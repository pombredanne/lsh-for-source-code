package eu.pisolutions.ocelot.document.file.identifier;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Calendar;
import eu.pisolutions.lang.Validations;
import eu.pisolutions.nio.charset.Charsets;
import eu.pisolutions.ocelot.document.Document;
import eu.pisolutions.ocelot.document.information.DocumentInformation;

/**
 * {@link FileIdentifierGenerator} that generates an identifier by computing the digest of some key values of the document.
 *
 * @author Laurent Pireyn
 */
public final class MessageDigestFileIdentifierGenerator implements FileIdentifierGenerator {

    private static final String ALGO_MD5 = "MD5";

    private static final String ALGO_SHA1 = "SHA1";

    public static MessageDigestFileIdentifierGenerator createMd5Instance() {
        try {
            return new MessageDigestFileIdentifierGenerator(MessageDigest.getInstance(MessageDigestFileIdentifierGenerator.ALGO_MD5));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public static MessageDigestFileIdentifierGenerator createSha1Instance() {
        try {
            return new MessageDigestFileIdentifierGenerator(MessageDigest.getInstance(MessageDigestFileIdentifierGenerator.ALGO_SHA1));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private final MessageDigest messageDigest;

    public MessageDigestFileIdentifierGenerator(MessageDigest messageDigest) {
        super();
        Validations.notNull(messageDigest, "message digest");
        this.messageDigest = messageDigest;
    }

    public byte[] generateIdentifier(Document document, String location, int size, Calendar time) {
        this.messageDigest.reset();
        final DocumentInformation documentInformation = document.getDocumentInformation();
        if (documentInformation != null) {
            this.updateMessageDigest(documentInformation.getTitle());
            this.updateMessageDigest(documentInformation.getAuthor());
            this.updateMessageDigest(documentInformation.getSubject());
            this.updateMessageDigest(documentInformation.getKeywords());
            this.updateMessageDigest(documentInformation.getCreator());
            this.updateMessageDigest(documentInformation.getProducer());
            this.updateMessageDigest(documentInformation.getCreationDate());
            this.updateMessageDigest(documentInformation.getModificationDate());
        }
        this.updateMessageDigest(location);
        this.updateMessageDigest(size);
        this.updateMessageDigest(time);
        return this.messageDigest.digest();
    }

    private void updateMessageDigest(int value) {
        this.updateMessageDigest(Integer.toString(value));
    }

    private void updateMessageDigest(long value) {
        this.updateMessageDigest(Long.toString(value));
    }

    private void updateMessageDigest(String string) {
        if (string != null) {
            this.messageDigest.update(string.getBytes(Charsets.CHARSET_UTF16BE));
        }
    }

    private void updateMessageDigest(Calendar calendar) {
        if (calendar != null) {
            this.updateMessageDigest(calendar.getTimeInMillis());
        }
    }
}
