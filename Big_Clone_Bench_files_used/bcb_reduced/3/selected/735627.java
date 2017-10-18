package net.krecan.ec;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.Test;

public class HashBreakerTest {

    @Test
    public void testGenerateNextInput() {
        assertArrayEquals(new byte[] { 2 }, HashBreaker.generateNextInput(new byte[] { 1 }));
        assertArrayEquals(new byte[] { 2, 1 }, HashBreaker.generateNextInput(new byte[] { 1, 1 }));
        assertArrayEquals(new byte[] { 0, 1 }, HashBreaker.generateNextInput(new byte[] { 127, 0 }));
        assertArrayEquals(new byte[] { 0, 0, 1 }, HashBreaker.generateNextInput(new byte[] { 127, 127, 0 }));
        assertArrayEquals(new byte[] { 0, 0, 1 }, HashBreaker.generateNextInput(new byte[] { 127, 127 }));
    }

    @Test
    public void testFindCollision() throws NoSuchAlgorithmException {
        byte[] dataToFind = new byte[] { 1, 2 };
        byte[] hash = MessageDigest.getInstance("MD5").digest(dataToFind);
        HashBreaker breaker = new HashBreaker(hash, new byte[] { 0, 0, 0 }, new byte[] { 127, 127, 127 });
        byte[] collision = breaker.findCollisionForHash();
        assertArrayEquals(dataToFind, collision);
    }
}
