package de.juwimm.cms.authorization.test;

import java.security.MessageDigest;
import junit.framework.Assert;
import junit.framework.TestCase;
import de.juwimm.util.Base64;

/**
 * <p>Title: ConQuest</p>
 * <p>Description: Enterprise Content Management</p>
 * <p>Copyright: Copyright (c) 2004</p>
 * @author <a href="sascha.kulawik@juwimm.com">Sascha-Matthias Kulawik</a>
 * @version $Revision: 3012 $
 */
public class TestAuthorization extends TestCase {

    /**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 * @throws Exception
	 */
    protected void setUp() throws Exception {
    }

    public void testPasswordEncoding() throws Throwable {
        String atHashAlgorithm = "SHA-1";
        String newPassword = "joekel";
        byte[] hash = MessageDigest.getInstance(atHashAlgorithm).digest(newPassword.getBytes());
        Assert.assertEquals("zFaW6CdmxKJMxNPVannhwAywmWo=", Base64.encodeBytes(hash));
    }
}
