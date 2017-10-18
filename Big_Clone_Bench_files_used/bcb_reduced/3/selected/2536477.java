package br.net.woodstock.rockframework.security.digest.impl;

import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.util.Assert;

public abstract class DelegateDigester implements Digester {

    private Digester digester;

    public DelegateDigester(final Digester digester) {
        super();
        Assert.notNull(digester, "digester");
        this.digester = digester;
    }

    @Override
    public byte[] digest(final byte[] data) {
        return this.digester.digest(data);
    }
}
