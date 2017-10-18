package com.blindeye.model;

import static org.jboss.seam.ScopeType.SESSION;
import static org.jboss.seam.ScopeType.EVENT;
import java.security.*;
import java.io.Serializable;
import java.util.List;
import java.util.Date;
import javax.persistence.*;
import org.hibernate.validator.Length;
import org.hibernate.validator.NotNull;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Role;
import org.hibernate.validator.*;
import javax.persistence.GeneratedValue;

@Entity
@Name("portInfo")
@Scope(EVENT)
@Table(name = "portInfos")
public class PortInfo implements Serializable {

    private String id;

    private String serviceName;

    private String serviceProduct;

    private String serviceVersion;

    private String serviceExtraInfo;

    private String serviceFp;

    private String method;

    private String conf;

    @Transient
    private static final char kHexChars[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

    @Id
    @Length(max = 33)
    @Column(length = 33)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getServiceProduct() {
        return serviceProduct;
    }

    public void setServiceProduct(String serviceProduct) {
        this.serviceProduct = serviceProduct;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getServiceExtraInfo() {
        return serviceExtraInfo;
    }

    public void setServiceExtraInfo(String serviceExtraInfo) {
        this.serviceExtraInfo = serviceExtraInfo;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getConf() {
        return conf;
    }

    public void setConf(String conf) {
        this.conf = conf;
    }

    @Lob
    public String getServiceFp() {
        return serviceFp;
    }

    public void setServiceFp(String serviceFp) {
        this.serviceFp = serviceFp;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("PortInfo ID: " + id + " serviceName: " + serviceName + " serviceProduct: " + serviceProduct + " serviceVersion: " + serviceVersion + " serviceExtraInfo: " + serviceExtraInfo);
        return sb.toString();
    }

    @Transient
    public String nte(String text) {
        if (text == null) {
            return "";
        }
        return text;
    }

    @Transient
    public String md5() {
        return md5sum(nte(serviceName) + nte(serviceProduct) + nte(serviceVersion) + nte(serviceExtraInfo) + nte(serviceFp) + nte(method) + nte(conf));
    }

    @Transient
    private String md5sum(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(text.getBytes());
            byte messageDigest[] = md.digest();
            return bufferToHex(messageDigest, 0, messageDigest.length);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Transient
    public String bufferToHex(byte buffer[], int startOffset, int length) {
        StringBuilder hexString = new StringBuilder(2 * length);
        int endOffset = startOffset + length;
        for (int i = startOffset; i < endOffset; i++) {
            appendHexPair(buffer[i], hexString);
        }
        return hexString.toString();
    }

    @Transient
    public void appendHexPair(byte b, StringBuilder hexString) {
        char highNibble = kHexChars[(b & 0xF0) >> 4];
        char lowNibble = kHexChars[b & 0x0F];
        hexString.append(highNibble);
        hexString.append(lowNibble);
    }
}
