import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.GregorianCalendar;

/**
 * SAMZIndexReader.java
 * 
 * This is an implementation of a CorpusIndexReader reads from disk an
 * index in the SAMZ index file format. 
 * 
 * @author Zachary M. Allen
 */
public class SAMZIndexReader implements CorpusIndexReader {

    /**
	 * Bit offset into each file
	 */
    private int[] fileBitmaskOffsets;

    /**
	 * Factory that created this reader
	 */
    private SAMZIndexRWFactory parent;

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
	 * Number of bits per bitmask
	 */
    private int bitsPerBitmask;

    /**
	 * Constructor
	 * @param    f    Parent factory
	 */
    public SAMZIndexReader(SAMZIndexRWFactory f) {
        parent = f;
    }

    /**
	 * Reads an index from disk using the SAMZ Index file format
	 * @param    fileName    File read from
	 * @return    newly created FileIndex object 
	 */
    public FileIndex readIndex(String fileName) throws AIMException {
        position = 0;
        load(fileName);
        FileIndex fileIndexes = new FileIndex(parent);
        int month = (int) getBits(MONTH_BITS) - 1;
        int date = (int) getBits(DATE_BITS);
        int year = (int) getBits(YEAR_BITS);
        int hours = (int) getBits(HOUR_BITS);
        int minutes = (int) getBits(MINUTE_BITS);
        int seconds = (int) getBits(SECOND_BITS);
        GregorianCalendar cal = new GregorianCalendar(year, month, date, hours, minutes, seconds);
        fileIndexes.setTime(cal.getTimeInMillis());
        String variableBreakCharacters = "";
        for (int j = 32; j < 127; j++) {
            if (getBits(1) == 1) {
                variableBreakCharacters += String.valueOf((char) j);
            }
        }
        int stopFileLength = (int) getBits(FILE_LENGTH_BITS);
        String stopWordFile = "";
        for (int k = 0; k < stopFileLength; k++) {
            stopWordFile += String.valueOf((char) getBits(BITS_PER_ASCII_BYTE));
        }
        fileIndexes.setCommandLineArgumentsInfo(stopWordFile, variableBreakCharacters);
        fileIndexes.setCorpusTotalChars((int) getBits(TOTAL_CORPUS_CHARS_BITS));
        fileIndexes.setCorpusTotalWords((int) getBits(TOTAL_CORPUS_WORDS_BITS));
        fileIndexes.setCorpusTotalLines((int) getBits(TOTAL_CORPUS_LINES_BITS));
        fileIndexes.setCorpusTotalFiles((int) getBits(NUM_INPUT_FILES_BITS));
        fileBitmaskOffsets = new int[fileIndexes.getCorpusTotalFiles()];
        for (int a = 0; a < fileIndexes.getCorpusTotalFiles(); a++) {
            int corpusFileLength = (int) getBits(FILE_LENGTH_BITS);
            String corpusFileName = "";
            for (int b = 0; b < corpusFileLength; b++) {
                corpusFileName += String.valueOf((char) getBits(BITS_PER_ASCII_BYTE));
            }
            FileIndexInstance f = new FileIndexInstance(corpusFileName);
            f.charSize = (int) getBits(FILE_CHARS_BITS);
            f.wordSize = (int) getBits(FILE_WORDS_BITS);
            f.lineSize = (int) getBits(FILE_LINES_BITS);
            bitsPerBitmask = (int) getBits(BITS_PER_BITMASK_BITS);
            fileBitmaskOffsets[a] = position;
            getBits(f.lineSize * bitsPerBitmask);
            fileIndexes.getFileIndexInstances().add(f);
        }
        return fileIndexes;
    }

    /**
	 * Checks the proper bitmask bits for the given word on the given line
	 * @param    word    Word
	 * @param    fileIndex    Index of file
	 * @param    lineNumber    Line number
	 * @return    true is word is on line, false otherwise 
	 */
    public boolean isWordOnLine(String word, int fileIndex, int lineNumber) {
        boolean retval = true;
        long testBitmask = bloomFilterMask(word, lineNumber);
        int startPosition = fileBitmaskOffsets[fileIndex] + (lineNumber * bitsPerBitmask);
        long lineBitmask = getBits(bitsPerBitmask, startPosition);
        if ((testBitmask & lineBitmask) != testBitmask) {
            retval = false;
        }
        return retval;
    }

    /**
	 * Returns the Bloom filter mask that is appropriate for the given word on
	 * the given line
	 * @param    word    Given word
	 * @param    lineNumber    Given line number
	 * @return    Bloom filter mask
	 */
    private long bloomFilterMask(String word, int lineNumber) {
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

    /**
	 * Loads a file for dumping
	 * @param    file    File to load
	 * @throws    IOException
	 */
    public void load(String file) throws AIMException {
        File f = new File(file);
        BufferedInputStream fis;
        try {
            fis = new BufferedInputStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new AIMException(AIMException.FILE_NOT_FOUND);
        }
        bitmask = new char[(int) f.length()];
        for (int i = 0; i < f.length(); i++) {
            try {
                bitmask[i] = (char) fis.read();
            } catch (IOException e) {
                throw new AIMException(AIMException.CANNOT_READ_FILE);
            }
        }
        try {
            fis.close();
        } catch (IOException e) {
            throw new AIMException(AIMException.CANNOT_READ_FILE);
        }
    }

    /**
	 * Retrieves data bits from the block
	 * @param    numBits    Number of bits to retrieve
	 * @return    Requested bits from the data block
	 */
    public long getBits(int numBits) {
        long retval = getBits(numBits, position);
        position += numBits;
        return retval;
    }

    /**
	 * Retrieves data bits from the block
	 * @param    numBits    Number of bits to retrieve
	 * @param    startPosition  Bitmask position to start at
	 * @return    Requested bits from the data block
	 */
    public long getBits(int numBits, int startPosition) {
        long retval = 0;
        for (int i = startPosition; i < startPosition + numBits; i++) {
            int index = i / 8;
            if (index >= bitmask.length) {
                return 0;
            }
            long bit = (bitmask[index] >> (7 - (i % 8))) & 1;
            retval = (retval << 1) | bit;
        }
        return retval;
    }
}
