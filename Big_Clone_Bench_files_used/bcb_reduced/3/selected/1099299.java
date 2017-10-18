package transFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Formatter;

public class HashUtil {

    public static String calculateHash(MessageDigest algorithm, File file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis);
        DigestInputStream dis = new DigestInputStream(bis, algorithm);
        while (dis.read() != -1) ;
        byte[] hash = algorithm.digest();
        return byteArray2Hex(hash);
    }

    public static String calculateHash(MessageDigest algorithm, InputStream is) throws Exception {
        BufferedInputStream bis = new BufferedInputStream(is);
        DigestInputStream dis = new DigestInputStream(bis, algorithm);
        while (dis.read() != -1) ;
        byte[] hash = algorithm.digest();
        return byteArray2Hex(hash);
    }

    private static String byteArray2Hex(byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
    }
}
