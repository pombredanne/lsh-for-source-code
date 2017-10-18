package org.photovault.imginfo;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
   FileUtils is a static class that implements common file handling operations that are often needed.
*/
public class FileUtils {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PhotoInfo.class.getName());

    /**
       Copies a file
       @param src Source file
       @param dst Destination file
    */
    public static void copyFile(File src, File dst) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(src);
            out = new FileOutputStream(dst);
            byte buf[] = new byte[1024];
            int nRead = 0;
            int offset = 0;
            while ((nRead = in.read(buf)) > 0) {
                out.write(buf, 0, nRead);
                offset += nRead;
            }
        } catch (IOException e) {
            throw e;
        } finally {
            IOException outEx = null;
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    outEx = ex;
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ex) {
                    throw ex;
                }
            }
            if (outEx != null) {
                throw outEx;
            }
        }
    }

    /**
     Utility function to calculate the hash of a specific file
     @param f The file
     @return Hash of f
     */
    public static byte[] calcHash(File f) {
        FileInputStream is = null;
        byte hash[] = null;
        try {
            is = new FileInputStream(f);
            byte readBuffer[] = new byte[4096];
            MessageDigest md = MessageDigest.getInstance("MD5");
            int bytesRead = -1;
            while ((bytesRead = is.read(readBuffer)) > 0) {
                md.update(readBuffer, 0, bytesRead);
            }
            hash = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            log.error("MD5 algorithm not found");
        } catch (FileNotFoundException ex) {
            log.error(f.getAbsolutePath() + "not found");
        } catch (IOException ex) {
            log.error("IOException while calculating hash: " + ex.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error("Cannot close stream after calculating hash");
            }
        }
        return hash;
    }

    /**
       Deleter a directory tree and all files stored in it.
       @param root Root directory of the tree
       @return true if deletion was successful, false otherwise
    */
    public static boolean deleteTree(File root) {
        boolean success = true;
        if (root.isDirectory()) {
            File entries[] = root.listFiles();
            for (int n = 0; n < entries.length; n++) {
                if (!deleteTree(entries[n])) {
                    success = false;
                }
            }
        }
        if (!root.delete()) {
            success = false;
        }
        return success;
    }
}
