package com.core.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileHash {

    StringBuffer buf = new StringBuffer();

    int bufSize = 1024;

    public String getFileHash(File f) {
        try {
            byte[] bytes = getBytesFromFile(f);
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(bytes);
            byte[] digest = mdAlgorithm.digest();
            StringBuffer hexString = new StringBuffer();
            String plainText;
            for (int i = 0; i < digest.length; i++) {
                plainText = Integer.toHexString(0xFF & digest[i]);
                if (plainText.length() < 2) {
                    plainText = "0" + plainText;
                }
                hexString.append(plainText);
            }
            return hexString.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            System.out.println("com.core.util.FileHash.getBytesFormFile(): file " + file.getName() + " is too large.");
            return null;
        }
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }
        is.close();
        return bytes;
    }
}
