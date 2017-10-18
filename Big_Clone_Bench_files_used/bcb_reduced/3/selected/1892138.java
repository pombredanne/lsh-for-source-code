package br.net.woodstock.rockframework.security.digest.impl;

import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.util.Assert;

public class AsStringDigester extends DelegateDigester {

    public AsStringDigester(final Digester digester) {
        super(digester);
    }

    public String digestAsString(final byte[] data) {
        byte[] digest = super.digest(data);
        return new String(digest);
    }

    public String digestAsString(final String str) {
        Assert.notEmpty(str, "str");
        byte[] data = str.getBytes();
        byte[] digest = super.digest(data);
        return new String(digest);
    }
}
