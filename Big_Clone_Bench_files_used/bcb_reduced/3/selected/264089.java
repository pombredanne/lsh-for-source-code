package common.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author risto
 */
public class Files {

    /**
     *
     * Read contents of File f into byte array.
     *
     * @param f - file to read
     * @return byte[] array of file contents.
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static byte[] fileBytes(File f) throws FileNotFoundException, IOException {
        byte data[] = new byte[(int) f.length()];
        FileInputStream fis = new FileInputStream(f);
        int toRead = (int) f.length();
        int r = 0;
        while (toRead > 0) {
            r = fis.read(data);
            if (r == -1) break;
            toRead -= r;
        }
        if (toRead != 0) throw new IOException("Partial read of " + f);
        fis.close();
        return data;
    }

    /**
     *
     * Compute SHA-1 digest of File f.
     *
     * @param f - file
     * @return String - base64 encoded SHA-1 of file contents
     */
    public static String digest(File f) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] data = fileBytes(f);
        byte[] digest = md.digest(data);
        String b64 = Base64.encodeBytes(digest);
        return b64;
    }

    /**
     *
     * Compute SHA-1 digest of String data.
     *
     * @param data - data
     * @return String - base64 encoded SHA-1 of string contents
     */
    public static String digest(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] digest = md.digest(data.getBytes());
        String b64 = Base64.encodeBytes(digest);
        return b64;
    }

    /**
     *
     * Open file f with CSV Reader.
     *
     * @param f
     * @return
     */
    public static CSVReader readCSV(File f) throws FileNotFoundException {
        return new CSVReaderImpl(f);
    }
}
