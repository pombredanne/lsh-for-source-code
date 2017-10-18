package com.atech.utils.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

public class CheckSumUtility {

    public static final String FILE_SEP = System.getProperties().getProperty("file.separator");

    public ArrayList fileList;

    public long getChecksumValue(String fname) throws Exception {
        return getChecksumValue(new File(fname));
    }

    public long getChecksumValue(File fname) throws Exception {
        Checksum checksum = new CRC32();
        checksum.reset();
        BufferedInputStream inputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(fname));
            byte[] bytes = new byte[1024];
            int len = 0;
            while ((len = inputStream.read(bytes)) >= 0) {
                checksum.update(bytes, 0, len);
            }
        } catch (Exception ex) {
            return -1;
        } finally {
            try {
                inputStream.close();
            } catch (Exception ex) {
            }
        }
        return checksum.getValue();
    }

    public MessageDigest generateChecksum(File filename) throws Exception {
        MessageDigest complete = MessageDigest.getInstance("MD5");
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filename);
            byte[] buffer = new byte[1024];
            int numRead = -1;
            while ((numRead = inputStream.read(buffer)) > 0) {
                complete.digest(buffer, 0, numRead);
            }
        } finally {
            try {
                inputStream.close();
            } catch (Exception ex) {
            }
        }
        return complete;
    }
}
