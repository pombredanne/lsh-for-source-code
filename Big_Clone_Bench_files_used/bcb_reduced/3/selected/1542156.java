package galao.crypto.hashing;

import galao.core.logging.Logging;
import galao.core.properties.Registry;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

public class HashUtil {

    private static HashUtil instance = new HashUtil();

    private int hashCounter = 0;

    private MessageDigest md5;

    private HashUtil() {
        try {
            this.md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Logging.warning("Could not find MD5-Algorithm for Id-generation");
        }
    }

    public static HashUtil getInstance() {
        return HashUtil.instance;
    }

    public BigInteger hash(BigInteger... data) {
        ++hashCounter;
        this.md5.reset();
        for (int i = 0; i < data.length; i++) {
            this.md5.update(data[i].toByteArray());
        }
        byte[] erg = this.md5.digest();
        return new BigInteger(erg);
    }

    public BigInteger hash(byte[] input) {
        ++hashCounter;
        this.md5.reset();
        this.md5.update(input);
        byte[] result = this.md5.digest();
        BigInteger erg = new BigInteger(result);
        return erg;
    }

    public BigInteger hash(String input) {
        return hash(input.getBytes(Registry.getCharset()));
    }

    public BigInteger hash(String... in) {
        StringBuffer buf = new StringBuffer(in[0]);
        for (int i = 1; i < in.length; i++) {
            buf.append(in[i]);
        }
        return hash(buf.toString());
    }

    public BigInteger randomHash() {
        double rand = Math.random();
        return hash(rand + "" + hashCounter);
    }
}
