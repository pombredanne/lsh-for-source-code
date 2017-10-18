package com.threerings.s3.client;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A representation of a array-backed object stored in S3.
 */
public class S3ByteArrayObject extends S3Object {

    /**
     * Instantiate an S3 byte object with the given key and data.
     * The data is not copied, and a reference is retained.
     *
     * @param key S3 object key.
     * @param data Object data.
     */
    public S3ByteArrayObject(String key, byte[] data) {
        this(key, data, S3Object.DEFAULT_MEDIA_TYPE);
    }

    public S3ByteArrayObject(String key, byte[] data, MediaType mediaType) {
        this(key, data, 0, data.length, mediaType);
    }

    public S3ByteArrayObject(String key, byte[] data, int offset, int length) {
        this(key, data, offset, length, S3Object.DEFAULT_MEDIA_TYPE);
    }

    /**
     * Instantiate an S3 byte object.
     * The data is not copied, and a reference is retained.
     *
     * @param key S3 object key.
     * @param data Object data;
     * @param mediaType Object's media type.
     */
    public S3ByteArrayObject(String key, byte[] data, int offset, int length, MediaType mediaType) {
        super(key, mediaType);
        MessageDigest md;
        _data = data;
        _offset = offset;
        _length = length;
        try {
            md = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException nsa) {
            throw new RuntimeException(nsa);
        }
        md.update(_data, _offset, _length);
        _md5 = md.digest();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(_data, _offset, _length);
    }

    @Override
    public byte[] getMD5() {
        return _md5;
    }

    @Override
    public long length() {
        return _length;
    }

    /** Backing byte array. */
    private byte[] _data;

    /** Data length. */
    private int _length;

    /** Data offset. */
    private int _offset;

    /** MD5 Digest. */
    private byte[] _md5;
}
