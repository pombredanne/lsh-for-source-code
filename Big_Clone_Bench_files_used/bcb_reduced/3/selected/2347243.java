package net.sourceforge.strategema.games;

import net.sourceforge.strategema.common.ProgramConfig;
import net.sourceforge.strategema.util.ByteUtils;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.CollationKey;
import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Loads a dictionary of words from a text file. Each word should be on its own line. Lines
 * beginning with # indicate comments.
 * 
 * @author Lizzy
 * 
 */
public class FileBasedDictionary implements Dictionary {

    /** Serialization */
    private static final long serialVersionUID = -3494198834668176431L;

    /** Logging */
    private static final Logger LOG = Logger.getLogger(FileBasedDictionary.class.getName());

    /** Indicates whether words are ALL CAPS, all lower-case or unmodified. */
    public enum Case {

        /** Words in lower-case */
        LOWERCASE, /** Words in capitals */
        UPPERCASE, /** Words unmodified */
        MIXED_CASE
    }

    /** The title of the dictionary. */
    private final String title;

    /** The name of the Java resource or words file. */
    private final String resourceName;

    /** The character set that the dictionary's resource uses. */
    private final String charsetName;

    /** True if the words are stored as a Java resource, or false if they come from a file elsewhere. */
    private final boolean isExternalFile;

    /** The words contained in the dictionary */
    private transient Set<CollationKey> words;

    /** The comments in the dictionary file. */
    private transient String comments;

    /** Whether the words are written as ALL CAPS, all lower-case or may be mixed case. */
    private final Case casing;

    /** The locale of the dictionary. */
    private final Locale locale;

    /** The collator used to compare a potential word to the words in the dictionary. */
    private final Collator collator;

    /**
	 * Create a Dictionary object for a built-in dictionary.
	 * @param title The title of the dictionary.
	 * @param resource The name of the dictionary to load.
	 * @param casing Whether the words have been converted to upper-case, converted to lower-case or
	 * unmodified.
	 * @param locale The locale to use.
	 * @throws IOException If the dictionary file cannot be read.
	 * @throws NullPointerException If the dictionary cannot be found.
	 */
    public FileBasedDictionary(final String title, final String resource, final Case casing, final Locale locale) throws IOException, NullPointerException {
        this(title, resource, casing, locale, getDefaultCollator(locale));
    }

    /**
	 * Gets the default collation order for a specified locale.
	 * @param locale The locale to find the collation order for.
	 * @return The default collation order for a specified locale.
	 */
    private static Collator getDefaultCollator(final Locale locale) {
        final Collator collator = Collator.getInstance(locale);
        collator.setStrength(Collator.TERTIARY);
        collator.setDecomposition(Collator.FULL_DECOMPOSITION);
        return collator;
    }

    /**
	 * Create a Dictionary object for a built-in dictionary.
	 * @param title The title of the dictionary.
	 * @param resource The name of the dictionary to load.
	 * @param casing Whether the words have been converted to upper-case, converted to lower-case or
	 * unmodified.
	 * @param locale The locale to use.
	 * @param collator The collator to use to compare a potential word to the words in the dictionary.
	 * @throws IOException If the dictionary file cannot be read.
	 * @throws NullPointerException If the dictionary cannot be found.
	 */
    public FileBasedDictionary(final String title, final String resource, final Case casing, final Locale locale, final Collator collator) throws IOException, NullPointerException {
        this.title = title;
        this.resourceName = resource;
        this.charsetName = "UTF-8";
        this.isExternalFile = false;
        this.casing = casing;
        this.locale = locale;
        this.collator = collator;
        this.load(FileBasedDictionary.class.getResourceAsStream(Dictionary.DICTIONARY_PATH + resource), Charset.forName("UTF-8"));
    }

    /**
	 * Create a Dictionary object from words contained in an external file.
	 * @param title The title of the dictionary.
	 * @param file The file to retrieve the words from.
	 * @param charset The character set the file is written in.
	 * @param casing Whether the words have been converted to upper-case, converted to lower-case or
	 * unmodified.
	 * @param locale The locale to use.
	 * @throws IOException If the dictionary file is missing or cannot be read.
	 */
    public FileBasedDictionary(final String title, final String file, final Charset charset, final Case casing, final Locale locale) throws IOException {
        this(title, file, charset, casing, locale, getDefaultCollator(locale));
    }

    /**
	 * Create a Dictionary object from words contained in an external file.
	 * @param title The title of the dictionary.
	 * @param file The file to retrieve the words from.
	 * @param charset The character set the file is written in.
	 * @param casing Whether the words have been converted to upper-case, converted to lower-case or
	 * unmodified.
	 * @param locale The locale to use.
	 * @param collator The collator to use to compare a potential word to the words in the dictionary.
	 * @throws IOException If the dictionary file is missing or cannot be read.
	 */
    public FileBasedDictionary(final String title, final String file, final Charset charset, final Case casing, final Locale locale, final Collator collator) throws IOException {
        this.title = title;
        this.resourceName = file;
        this.charsetName = charset.name();
        this.isExternalFile = true;
        this.casing = casing;
        this.locale = locale;
        this.collator = collator;
        this.load(new FileInputStream(file), charset);
    }

    /**
	 * Loads a dictionary using words read from an InputStream.
	 * @param in The InputStream to retrieve the words from.
	 * @param charset The character set the file is written in.
	 * @throws IOException If the dictionary file is missing or cannot be read.
	 */
    private void load(final InputStream in, final Charset charset) throws IOException {
        this.words = new HashSet<CollationKey>();
        final StringBuilder commentLines = new StringBuilder();
        commentLines.append("<p>");
        final BufferedReader reader = new BufferedReader(new InputStreamReader(in, charset));
        try {
            String word;
            do {
                word = reader.readLine();
                if (word != null) {
                    if (word.startsWith("#")) {
                        commentLines.append(word.substring(1));
                        commentLines.append('\n');
                    } else {
                        this.words.add(this.collator.getCollationKey(word));
                    }
                }
            } while (word != null);
        } finally {
            try {
                reader.close();
                commentLines.append("</p>");
                this.comments = commentLines.toString().replaceAll("\n[\\s]*\n", "\n</p>\n<p>\n");
            } catch (final IOException e) {
                FileBasedDictionary.LOG.warning(e.toString());
            }
        }
    }

    /**
	 * Writes the dictionary to the given output stream.
	 * @param out The stream to write the dictionary in.
	 * @throws IOException If an I/O error occurs.
	 */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(this.getDigest());
    }

    /**
	 * Restores a dictionary from a stream.
	 * @param in The stream to read the dictionary from.
	 * @throws IOException If an I/O occurs or the digital signature in the stream does not match the
	 * signature of the local words file.
	 * @throws ClassNotFoundException If a serialization error occurs.
	 */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Charset charset;
        byte[] storedDigest;
        final Object obj = in.readObject();
        if (obj == null) {
            throw new InvalidClassException(null, "Not a valid digest");
        } else if (!(obj instanceof byte[])) {
            throw new InvalidClassException(obj.getClass().getName(), "Not a valid digest");
        } else {
            storedDigest = (byte[]) obj;
        }
        try {
            charset = Charset.forName(this.charsetName);
        } catch (final IllegalArgumentException e) {
            throw new IOException(e);
        }
        if (this.isExternalFile) {
            this.load(new FileInputStream(this.resourceName), charset);
        } else {
            this.load(FileBasedDictionary.class.getResourceAsStream(Dictionary.DICTIONARY_PATH + this.resourceName), charset);
        }
        final byte[] computedDigest = this.getDigest();
        if (!Arrays.equals(storedDigest, computedDigest)) {
            throw new IOException("The local dictionary file does not match the one used by the Dictionary object.");
        }
    }

    @Override
    public final byte[] getDigest() {
        try {
            final MessageDigest hashing = MessageDigest.getInstance("SHA-256");
            final Charset utf16 = Charset.forName("UTF-16");
            for (final CollationKey wordKey : this.words) {
                hashing.update(wordKey.toByteArray());
            }
            hashing.update(this.locale.toString().getBytes(utf16));
            hashing.update(ByteUtils.toBytesLE(this.collator.getStrength()));
            hashing.update(ByteUtils.toBytesLE(this.collator.getDecomposition()));
            return hashing.digest();
        } catch (final NoSuchAlgorithmException e) {
            FileBasedDictionary.LOG.severe(e.toString());
            return new byte[0];
        }
    }

    @Override
    public String getTitle() {
        return this.title;
    }

    @Override
    public boolean isInDictionary(final String word) {
        final String wordInCorrectCase;
        switch(this.casing) {
            case UPPERCASE:
                wordInCorrectCase = word.toUpperCase(this.locale);
                break;
            case LOWERCASE:
                wordInCorrectCase = word.toLowerCase(this.locale);
                break;
            case MIXED_CASE:
            default:
                wordInCorrectCase = word;
        }
        return this.words.contains(this.collator.getCollationKey(wordInCorrectCase));
    }

    @Override
    public Set<GameFile> getExternalResources() {
        if (this.isExternalFile) {
            return Collections.singleton(new GameFile(ProgramConfig.DICTIONARY_PATH, this.resourceName));
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public String getCredits() {
        return this.comments;
    }
}
