package br.net.woodstock.rockframework.security.digest.impl;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import br.net.woodstock.rockframework.security.digest.DigestType;
import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.security.digest.DigesterException;
import br.net.woodstock.rockframework.util.Assert;

public class BasicDigester implements Digester {

    private DigestType type;

    public BasicDigester(final DigestType type) {
        super();
        Assert.notNull(type, "type");
        this.type = type;
    }

    @Override
    public byte[] digest(final byte[] data) {
        Assert.notEmpty(data, "data");
        try {
            MessageDigest digest = MessageDigest.getInstance(this.type.getAlgorithm());
            digest.update(data);
            byte[] digested = digest.digest();
            return digested;
        } catch (NoSuchAlgorithmException e) {
            throw new DigesterException(e);
        }
    }
}
