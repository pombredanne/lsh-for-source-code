package app.sentinel;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

/**
 *
 * @author Nuno Brito
 */
public class ScannerChecksum {

    protected static final boolean debug = false;

    @SuppressWarnings("empty-statement")
    public static String generateFileChecksum(String hash, String filename) {
        String checksum;
        FileInputStream fis = null;
        try {
            MessageDigest md = MessageDigest.getInstance(hash);
            fis = new FileInputStream(filename);
            byte[] dataBytes = new byte[1024];
            int nread = 0;
            while ((nread = fis.read(dataBytes)) != -1) {
                md.update(dataBytes, 0, nread);
            }
            ;
            byte[] mdbytes = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mdbytes.length; i++) {
                sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
            }
            checksum = sb.toString();
        } catch (IOException ex) {
            if (debug == true) Logger.getLogger(ScannerChecksum.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        } catch (NoSuchAlgorithmException ex) {
            if (debug == true) Logger.getLogger(ScannerChecksum.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        } finally {
            try {
                if (fis != null) fis.close();
            } catch (IOException ex) {
                if (debug == true) Logger.getLogger(ScannerChecksum.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            }
        }
        return checksum;
    }

    public static String generateFileCRC32(String fileName) {
        long checksum = 0;
        CheckedInputStream cis = null;
        try {
            cis = new CheckedInputStream(new FileInputStream(fileName), new CRC32());
            byte[] buf = new byte[128];
            while (cis.read(buf) >= 0) {
            }
            checksum = cis.getChecksum().getValue();
        } catch (IOException e) {
            return "";
        } finally {
            try {
                cis.close();
            } catch (IOException ex) {
                if (debug == true) Logger.getLogger(ScannerChecksum.class.getName()).log(Level.SEVERE, null, ex);
                return "";
            } finally {
                return Long.toString(checksum);
            }
        }
    }

    public static String generateStringSHA256(String content) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(ScannerChecksum.class.getName()).log(Level.SEVERE, null, ex);
        }
        md.update(content.getBytes());
        byte byteData[] = md.digest();
        @SuppressWarnings("StringBufferMayBeStringBuilder") StringBuffer sb = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
        }
        @SuppressWarnings("StringBufferMayBeStringBuilder") StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
