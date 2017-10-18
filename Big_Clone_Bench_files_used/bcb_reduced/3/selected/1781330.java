package br.com.visualmidia.business;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileDescriptorMD5 implements Serializable {

    private static final long serialVersionUID = 1444716133774966004L;

    private String name;

    private String location;

    private long size;

    private String md5;

    public FileDescriptorMD5(File file) {
        this.name = file.getName();
        this.location = file.getParent();
        this.size = file.getTotalSpace();
        generateMD5ofFile(file);
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return this.location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public long getSize() {
        return this.size;
    }

    public String getMD5() {
        return this.md5;
    }

    public void generateMD5ofFile(File file) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            BigInteger hash = new BigInteger(1, md.digest(file.toString().getBytes()));
            this.name = file.getName();
            this.location = file.getParent();
            this.size = file.getTotalSpace();
            this.md5 = hash.toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
