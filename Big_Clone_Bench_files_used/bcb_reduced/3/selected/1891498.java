package others;

import java.math.BigInteger;
import java.security.MessageDigest;
import junit.framework.TestCase;

public class BigIntegerTest extends TestCase {

    private MessageDigest digester;

    public void setUp() throws Exception {
        digester = MessageDigest.getInstance("SHA-1");
    }

    public void testMag() {
        String key = "Brazil#Sao Paulo#Sao Paulo";
        byte[] hashed = digester.digest(key.getBytes());
        BigInteger bigInt = new BigInteger(hashed);
        byte[] fromBigInt = bigInt.toByteArray();
        for (int i = 0; i < fromBigInt.length; i++) assertEquals(hashed[i], fromBigInt[i]);
        byte[] array = new byte[20];
        for (int pos = 0; pos < 20; pos++) {
            for (int pos2 = 0; pos2 <= pos; pos2++) {
                for (byte i = Byte.MIN_VALUE; i < Byte.MAX_VALUE; i++) {
                    if (i != 0) {
                        array[pos2] = i;
                        BigInteger bi = new BigInteger(array);
                        byte[] fromBi = normalizeArray(bi.toByteArray(), array.length);
                        assertEquals(array.length, fromBi.length);
                        for (int j = 0; j < fromBi.length; j++) assertEquals(array[j], fromBi[j]);
                    }
                }
            }
        }
    }

    private String normalizeString(String string, int size) {
        for (int i = 0; i < size; i++) string = "0" + string;
        return string;
    }

    private byte[] normalizeArray(byte[] array, int size) {
        byte[] normalized = new byte[size];
        System.arraycopy(array, 0, normalized, size - array.length, array.length);
        return normalized;
    }

    public static void main(String[] args) {
        BigIntegerTest test = new BigIntegerTest();
        test.testMag();
    }
}
