package com.tomczarniecki.s3.rest;

import com.tomczarniecki.s3.ProgressListener;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;

public class Files {

    public static Properties loadProperties(File file) {
        FileInputStream input = null;
        try {
            Properties props = new Properties();
            input = new FileInputStream(file);
            props.load(input);
            return props;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(input);
        }
    }

    public static void saveProperties(File file, Properties props) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(file);
            props.store(out, null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

    public static String computeMD5(File file) {
        try {
            return computeMD5(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String computeMD5(InputStream input) {
        InputStream digestStream = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            digestStream = new DigestInputStream(input, md5);
            IOUtils.copy(digestStream, new NullOutputStream());
            return new String(Base64.encodeBase64(md5.digest()), "UTF-8");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(digestStream);
        }
    }

    public static void writeToFile(InputStream input, File file, ProgressListener listener, long length) {
        OutputStream output = null;
        try {
            output = new CountingOutputStream(new FileOutputStream(file), listener, length);
            IOUtils.copy(input, output);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
    }
}
