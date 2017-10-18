package com.threerings.s3.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;

/**
 * A representation of a (locally file-backed) object stored in S3.
 */
public class S3FileObject extends S3Object {

    /**
     * Instantiate an S3 file-backed object with the given key.
     * @param key S3 object key.
     * @param file File backing.
     */
    public S3FileObject(String key, File file) {
        super(key);
        _file = file;
    }

    /**
     * Instantiate an S3 file-backed object with the given key.
     * @param key S3 object key.
     * @param file File backing.
     * @param mediaType Object's media type.
     */
    public S3FileObject(String key, File file, MediaType mediaType) {
        super(key, mediaType);
        _file = file;
    }

    @Override
    public InputStream getInputStream() throws S3ClientException {
        try {
            return new FileInputStream(_file);
        } catch (FileNotFoundException fnf) {
            throw new S3ClientException("File was not found.", fnf);
        }
    }

    @Override
    public byte[] getMD5() throws S3ClientException {
        InputStream input;
        MessageDigest md;
        byte data[];
        int nbytes;
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException(nsa);
        }
        input = getInputStream();
        data = new byte[1024];
        try {
            while ((nbytes = input.read(data)) > 0) {
                md.update(data, 0, nbytes);
            }
        } catch (IOException ioe) {
            throw new S3ClientException("Failure reading input file: " + ioe, ioe);
        }
        return md.digest();
    }

    @Override
    public long lastModified() {
        return _file.lastModified();
    }

    @Override
    public long length() {
        return _file.length();
    }

    /** File path. */
    private final File _file;
}
