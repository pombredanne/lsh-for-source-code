package br.net.woodstock.rockframework.security.test;

import junit.framework.TestCase;
import br.net.woodstock.rockframework.security.digest.DigestType;
import br.net.woodstock.rockframework.security.digest.Digester;
import br.net.woodstock.rockframework.security.digest.impl.BasicDigester;
import br.net.woodstock.rockframework.security.digest.impl.HexDigester;
import br.net.woodstock.rockframework.security.util.MD5PasswordEncoder;
import br.net.woodstock.rockframework.security.util.PasswordEncoder;
import br.net.woodstock.rockframework.security.util.SHA1PasswordEncoder;

public class DigestTest extends TestCase {

    public void xtest1() throws Exception {
        Digester digester = new BasicDigester(DigestType.SHA_256);
        Digester delegate = new HexDigester(digester);
        System.out.println(new String(delegate.digest("Teste da Silva Sauro\n".getBytes())));
    }

    public void xtest2() throws Exception {
        PasswordEncoder encoder = new MD5PasswordEncoder();
        System.out.println(encoder.encode("lourival"));
    }

    public void xtest3() throws Exception {
        PasswordEncoder encoder = new SHA1PasswordEncoder();
        System.out.println(encoder.encode("lourival"));
    }
}
