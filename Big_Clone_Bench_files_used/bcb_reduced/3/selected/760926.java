package br.net.woodstock.rockframework.security.digest.impl;

import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.utils.Base64Utils;

public class Base64Digester extends DelegateDigester {

    public Base64Digester(final Digester digester) {
        super(digester);
    }

    @Override
    public byte[] digest(final byte[] data) {
        byte[] digested = super.digest(data);
        byte[] base64 = Base64Utils.toBase64(digested);
        return base64;
    }
}
