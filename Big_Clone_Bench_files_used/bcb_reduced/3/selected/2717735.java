package com.litt.core.security;

import java.security.MessageDigest;
import sun.misc.BASE64Encoder;

/**
 * BASE64Encoder编码加密，可配合MessageDigestFactoryBean使用
 * MD5+BASE64的双重加密
 * 
 * @author <a href="mailto:littcai@hotmail.com">空心大白菜</a>
 * @since 2006-08-30
 * @version 1.0
 *
 */
public class MessageDigester {

    private MessageDigest digest = null;

    public void setDigest(MessageDigest digest) {
        this.digest = digest;
    }

    public String digest(String msg) {
        return digest(msg, digest);
    }

    private String digest(String msg, MessageDigest digest) {
        digest.reset();
        byte[] bytes = msg.getBytes();
        byte[] out = digest.digest(bytes);
        BASE64Encoder enc = new BASE64Encoder();
        return enc.encode(out);
    }
}
