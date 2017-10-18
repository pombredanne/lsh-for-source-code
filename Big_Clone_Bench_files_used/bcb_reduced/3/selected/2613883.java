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
@Name("portScan")
@Scope(EVENT)
@Table(name = "portScans")
public class PortScan implements Serializable {

    private String id;

    private PortInfo portInfo;

    private String protocol;

    private Integer portNumber;

    private String state;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portInfo_fk")
    public PortInfo getPortInfo() {
        return portInfo;
    }

    public void setPortInfo(PortInfo portInfo) {
        this.portInfo = portInfo;
    }

    @Length(max = 75)
    @Column(length = 75)
    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    @Length(max = 10)
    @Column(length = 10)
    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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
        return md5sum(nte(portInfo.getId()) + nte(protocol) + nte(String.valueOf(portNumber)) + nte(state));
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
