package net.derquinse.common.base;

import java.security.MessageDigest;
import java.security.SecureRandom;
import net.derquinse.common.test.EqualityTests;
import net.derquinse.common.test.HessianSerializabilityTests;
import net.derquinse.common.test.SerializabilityTests;
import org.testng.annotations.Test;

/**
 * Tests for ByteString
 * @author Andres Rodriguez
 */
public class ByteStringTest {

    private static final SecureRandom R = new SecureRandom();

    private ByteString createRandom() {
        final MessageDigest d = Digests.sha1();
        final byte[] bytes = new byte[2048];
        for (int i = 0; i < 1500; i++) {
            R.nextBytes(bytes);
            d.update(bytes);
        }
        return ByteString.copyFrom(d.digest());
    }

    /**
	 * Simple test.
	 */
    @Test
    public void simple() throws Exception {
        ByteString s1 = createRandom();
        String s = s1.toHexString();
        System.out.println();
        ByteString s2 = ByteString.fromHexString(s);
        EqualityTests.two(s1, s2);
    }

    /**
	 * Serialization.
	 */
    @Test
    public void serialization() throws Exception {
        ByteString s = createRandom();
        SerializabilityTests.check(s);
        HessianSerializabilityTests.both(s);
    }
}
