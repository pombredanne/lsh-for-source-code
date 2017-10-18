package com.netflexitysolutions.amazonws.s3;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.XMLGregorianCalendar;
import com.netflexitysolutions.amazonws.s3.internal.utils.S3Utils;

public class S3Object {

    private String key;

    private Map<String, String> metadata;

    private byte[] data;

    private List<S3Grant> acl;

    private String md5hash;

    private XMLGregorianCalendar lastModifiedDate;

    private String eTag;

    private String storageClass;

    private S3Owner owner;

    public S3Object(String key) {
        this.key = key;
    }

    public S3Object(String key, byte[] data) {
        this(key);
        this.data = data;
        this.md5hash = S3Utils.toHex(computeMD5Hash(data));
    }

    public S3Object(String key, byte[] data, Map<String, String> metadata) {
        this(key, data);
        this.metadata = metadata;
    }

    public long getContentLength() {
        return data == null ? 0l : data.length;
    }

    public String getKey() {
        return key;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public byte[] getData() {
        return data;
    }

    public List<S3Grant> getAcl() {
        return acl;
    }

    public void setAcl(List<S3Grant> acl) {
        this.acl = acl;
    }

    public String getMd5hash() {
        return md5hash;
    }

    public static byte[] computeMD5Hash(byte[] data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            byte[] hash = messageDigest.digest(data);
            return hash;
        } catch (NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }

    public XMLGregorianCalendar getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(XMLGregorianCalendar lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getETag() {
        return eTag;
    }

    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public void setStorageClass(String storageClass) {
        this.storageClass = storageClass;
    }

    public S3Owner getOwner() {
        return owner;
    }

    public void setOwner(S3Owner owner) {
        this.owner = owner;
    }
}
