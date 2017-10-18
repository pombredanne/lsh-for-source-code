package net.webpasswordsafe.server.plugin.encryption;

import net.webpasswordsafe.server.plugin.encryption.JasyptDigester;
import org.jasypt.util.password.StrongPasswordEncryptor;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Josh Drummond
 *
 */
public class JasyptDigesterTest {

    /**
     * Test method for {@link net.webpasswordsafe.server.plugin.encryption.JasyptDigester#check(java.lang.String, java.lang.String)}.
     */
    @Test
    public void testCheck() {
        JasyptDigester digester = new JasyptDigester();
        digester.setPasswordEncryptor(new StrongPasswordEncryptor());
        String password1 = digester.digest("josh");
        System.out.println("password1=" + password1);
        String password2 = digester.digest("josh");
        System.out.println("password2=" + password2);
        assertTrue(digester.check("josh", password1));
        assertTrue(digester.check("josh", password2));
        assertFalse(password1.equals(password2));
    }
}
