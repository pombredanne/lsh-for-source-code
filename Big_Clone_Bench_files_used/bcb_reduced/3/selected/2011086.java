package br.net.woodstock.rockframework.security.digest.impl;

import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.utils.HexUtils;

public class HexDigester extends DelegateDigester {

    public HexDigester(final Digester digester) {
        super(digester);
    }

    @Override
    public byte[] digest(final byte[] data) {
        byte[] digested = super.digest(data);
        byte[] hex = HexUtils.toHex(digested);
        return hex;
    }
}
