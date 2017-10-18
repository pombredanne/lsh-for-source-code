package org.adore.didl.json;

import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;

/**
 * temporary writer for processing bytestream
 */
public class FileBytestreamHandler implements BytestreamHandler {

    private String dir = null;

    MessageDigest digester;

    public FileBytestreamHandler(String dir) {
        this.dir = dir;
        try {
            digester = MessageDigest.getInstance("MD5");
        } catch (java.security.NoSuchAlgorithmException ex) {
            System.err.println("MD5 cannot be found?");
        }
    }

    /**
     * write byte array to a directory, file name is the md5 value of
     * file content. return a file URI
     * 
     * 
     * @param input byte array to write
     * @return file URI of returned stream
     * 
     */
    public String write(byte[] input) throws java.io.IOException {
        byte[] digest = digester.digest(input);
        StringBuilder filename = new StringBuilder();
        for (byte b : digest) {
            filename.append(Integer.toHexString(b & 0xFF));
        }
        File file = new File(dir, filename.toString());
        FileOutputStream fw = new FileOutputStream(file);
        fw.write(input);
        fw.close();
        return "file://" + file.getAbsolutePath();
    }
}
