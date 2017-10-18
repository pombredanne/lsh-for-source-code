import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.Vector;
import java.util.Calendar;

/**
 * SAMZIndexWriter.java
 * 
 * This is an implementation of a CorpusIndexWriter that writes to disk an index
 * in the SAMZ index file format.
 * 
 * @author Zachary M. Allen
 */
public class SAMZIndexWriter implements CorpusIndexWriter {

    /**
	 * Number of bits to represent a month
	 */
    private static final int MONTH_BITS = 4;

    /**
	 * Number of bits to represent a date
	 */
    private static final int DATE_BITS = 5;

    /**
	 * Number of bits to represent a year
	 */
    private static final int YEAR_BITS = 14;

    /**
	 * Number of bits to represent hours
	 */
    private static final int HOUR_BITS = 5;

    /**
	 * Number of bits to represent minutes
	 */
    private static final int MINUTE_BITS = 6;

    /**
	 * Number of bits to represent seconds
	 */
    private static final int SECOND_BITS = 6;

    /**
	 * Number of bits used per line bitmask
	 */
    private static final int BITS_PER_BITMASK_BITS = 8;

    /**
	 * Number of bits used per line bitmask
	 */
    private static final int DEFAULT_BITS_PER_BITMASK = 64;

    /**
	 * Number of bits used to represent a file length
	 */
    private static final int FILE_LENGTH_BITS = 8;

    /**
	 * The number of bits in one ASCII character
	 */
    private static final int BITS_PER_ASCII_BYTE = 8;

    /**
	 * Number of bits used to represent total corpus chars
	 */
    private static final int TOTAL_CORPUS_CHARS_BITS = 32;

    /**
	 * Number of bits used to represent total corpus words
	 */
    private static final int TOTAL_CORPUS_WORDS_BITS = 32;

    /**
	 * Number of bits used to represent total corpus lines
	 */
    private static final int TOTAL_CORPUS_LINES_BITS = 32;

    /**
	 * Number of bits representing the number of input files
	 */
    private static final int NUM_INPUT_FILES_BITS = 16;

    /**
	 * Number of bits used to represent file chars
	 */
    private static final int FILE_CHARS_BITS = 32;

    /**
	 * Number of bits used to represent file words
	 */
    private static final int FILE_WORDS_BITS = 32;

    /**
	 * Number of bits used to represent file lines
	 */
    private static final int FILE_LINES_BITS = 32;

    /**
	 * Current position in file
	 */
    private int position;

    /**
	 * The data array that holds the data chunk
	 */
    private char[] bitmask;

    /**
	 * Current size of bitmask
	 */
    private int size;

    /**
	 * Writes an index to disk using the SAMZ Index file format
	 * 
	 * @param fileIndexes
	 *            FileIndex array to read from
	 * @param fileName
	 *            File to write to
	 */
    public void writeIndex(FileIndex fileIndexes, String fileName) throws AIMException {
        position = 0;
        bitmask = new char[1];
        size = bitmask.length;
        addBits(1 + fileIndexes.getTime().get(Calendar.MONTH), MONTH_BITS);
        addBits(fileIndexes.getTime().get(Calendar.DAY_OF_MONTH), DATE_BITS);
        addBits(fileIndexes.getTime().get(Calendar.YEAR), YEAR_BITS);
        addBits(fileIndexes.getTime().get(Calendar.HOUR_OF_DAY), HOUR_BITS);
        addBits(fileIndexes.getTime().get(Calendar.MINUTE), MINUTE_BITS);
        addBits(fileIndexes.getTime().get(Calendar.SECOND), SECOND_BITS);
        char[] variableBreakCharacters = fileIndexes.getVariableBreaks().toCharArray();
        Vector chars = new Vector();
        for (int i = 0; i < variableBreakCharacters.length; i++) {
            chars.add(new Integer(variableBreakCharacters[i]));
        }
        for (int j = 32; j < 127; j++) {
            if (chars.contains(new Integer(j))) {
                addBits(1, 1);
            } else {
                addBits(0, 1);
            }
        }
        if (fileIndexes.getStopWordFile() == null) {
            addBits(0, FILE_LENGTH_BITS);
        } else {
            addBits(fileIndexes.getStopWordFile().length(), FILE_LENGTH_BITS);
            for (int k = 0; k < fileIndexes.getStopWordFile().length(); k++) {
                addBits(fileIndexes.getStopWordFile().charAt(k), BITS_PER_ASCII_BYTE);
            }
        }
        fileIndexes.setCorpusStats();
        addBits(fileIndexes.getCorpusTotalChars(), TOTAL_CORPUS_CHARS_BITS);
        addBits(fileIndexes.getCorpusTotalWords(), TOTAL_CORPUS_WORDS_BITS);
        addBits(fileIndexes.getCorpusTotalLines(), TOTAL_CORPUS_LINES_BITS);
        addBits(fileIndexes.getCorpusTotalFiles(), NUM_INPUT_FILES_BITS);
        for (int a = 0; a < fileIndexes.getFileIndexInstances().size(); a++) {
            FileIndexInstance f = (FileIndexInstance) fileIndexes.getFileIndexInstances().get(a);
            addBits(f.filename.length(), FILE_LENGTH_BITS);
            for (int b = 0; b < f.filename.length(); b++) {
                addBits(f.filename.charAt(b), BITS_PER_ASCII_BYTE);
            }
            addBits(f.charSize, FILE_CHARS_BITS);
            addBits(f.wordSize, FILE_WORDS_BITS);
            addBits(f.lineSize, FILE_LINES_BITS);
            int bitsPerBitmask = DEFAULT_BITS_PER_BITMASK;
            addBits(bitsPerBitmask, BITS_PER_BITMASK_BITS);
            long[] bitmasks = new long[f.lineSize];
            Iterator wordIterator = fileIndexes.getWordInstances().keySet().iterator();
            while (wordIterator.hasNext()) {
                String word = (String) wordIterator.next();
                WordIndexInstance wii = (WordIndexInstance) fileIndexes.getWordInstances().get(word);
                WordInFileIndexInstance wifii = (WordInFileIndexInstance) wii.matchFiles.get(f);
                if (wifii == null) {
                    continue;
                }
                Iterator lineIterator = wifii.getLineNumberIterator();
                while (lineIterator.hasNext()) {
                    int lineNumber = ((Integer) lineIterator.next()).intValue() - 1;
                    processWord(word, lineNumber, bitmasks, bitsPerBitmask);
                }
            }
            for (int d = 0; d < f.lineSize; d++) {
                addBits(bitmasks[d], bitsPerBitmask);
            }
        }
        save(fileName);
    }

    /**
	 * Sets the proper bitmask bits for the given word on the given line
	 * 
	 * @param word
	 *            Word
	 * @param lineNumber
	 *            Line number
	 * @param bitmasks
	 *            Bitmask array to set bits in
	 * @param bitsPerBitmask
	 *            Bits per bitmask
	 */
    private void processWord(String word, int lineNumber, long[] bitmasks, int bitsPerBitmask) {
        bitmasks[lineNumber] |= bloomFilterMask(word, lineNumber, bitsPerBitmask);
    }

    /**
	 * Saves a file
	 * 
	 * @param fileName
	 *            Name of file to save to
	 * @throws AIMException
	 */
    private void save(String fileName) throws AIMException {
        BufferedOutputStream fos = null;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(fileName));
        } catch (FileNotFoundException e) {
            throw new AIMException(AIMException.FILE_NOT_FOUND);
        }
        for (int i = 0; i < size; i++) {
            try {
                fos.write(bitmask[i]);
            } catch (IOException e) {
                throw new AIMException(AIMException.CANT_WRITE);
            }
        }
        try {
            fos.close();
        } catch (IOException e) {
            throw new AIMException(AIMException.CANT_WRITE);
        }
    }

    /**
	 * Adds data to the data block
	 * 
	 * @param data
	 *            Data to add
	 * @param numBits
	 *            Number of bits to use to represent the new data
	 */
    private void addBits(long data, int numBits) {
        while ((numBits + position) >= (8 * bitmask.length)) {
            bitmask = resizeCharArray(bitmask, bitmask.length * 2);
        }
        long shift = numBits - 1;
        for (int i = position; i < position + numBits; i++) {
            int index = i / 8;
            long bit = ((data >> shift--) & 1);
            bitmask[index] = (char) ((bitmask[index]) | bit << (7 - (i % 8)));
        }
        position += numBits;
        size = 1 + (position / 8);
    }

    /**
	 * Resizes a char array when necessary
	 * 
	 * @param charArray
	 *            Char array to resize
	 * @param newSize
	 *            New size of array
	 */
    private static char[] resizeCharArray(char[] charArray, int newSize) {
        char[] newCharArray = new char[newSize];
        for (int i = 0; i < charArray.length; i++) {
            newCharArray[i] = charArray[i];
        }
        return newCharArray;
    }

    /**
	 * Returns the Bloom filter mask that is appropriate for the given word on
	 * the given line
	 * @param    word    Given word
	 * @param    lineNumber    Given line number
	 * @return    Bloom filter mask
	 */
    private long bloomFilterMask(String word, int lineNumber, int bitsPerBitmask) {
        long retval = 0;
        MessageDigest md5 = null;
        byte[] md5digest = null;
        MessageDigest sha1 = null;
        byte[] sha1digest = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
        }
        String firstHash = word + "|" + lineNumber;
        String secondHash = lineNumber + "|" + word;
        int bitOne = 0;
        md5digest = md5.digest(firstHash.getBytes());
        ;
        for (int i = 0; i < md5.getDigestLength(); i++) {
            bitOne ^= md5digest[i];
        }
        bitOne %= bitsPerBitmask;
        retval |= (long) 1 << bitOne;
        int bitTwo = 0;
        md5digest = md5.digest(secondHash.getBytes());
        ;
        for (int i = 0; i < md5.getDigestLength(); i++) {
            bitTwo ^= md5digest[i];
        }
        bitTwo %= bitsPerBitmask;
        retval |= (long) 1 << bitTwo;
        int bitThree = 0;
        sha1digest = sha1.digest(firstHash.getBytes());
        for (int i = 0; i < sha1.getDigestLength(); i++) {
            bitThree ^= sha1digest[i];
        }
        bitThree %= bitsPerBitmask;
        retval |= (long) 1 << bitThree;
        int bitFour = 0;
        sha1digest = sha1.digest(secondHash.getBytes());
        for (int i = 0; i < sha1.getDigestLength(); i++) {
            bitFour ^= sha1digest[i];
        }
        bitFour %= bitsPerBitmask;
        retval |= (long) 1 << bitFour;
        return retval;
    }
}
