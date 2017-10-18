package br.net.woodstock.rockframework.security.digest.impl;

import br.net.woodstock.rockframework.security.Encoder;
import br.net.woodstock.rockframework.security.digest.Digester;

public class DigesterEncoder extends DelegateDigester implements Encoder {

    public DigesterEncoder(final Digester digester) {
        super(digester);
    }

    @Override
    public byte[] decode(final byte[] data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] encode(final byte[] data) {
        return this.digest(data);
    }
}
