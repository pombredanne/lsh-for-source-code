package edu.vt.middleware.crypt.digest;

import java.io.InputStream;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import edu.vt.middleware.crypt.util.Base64Converter;
import edu.vt.middleware.crypt.util.Converter;
import edu.vt.middleware.crypt.util.HexConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Unit test for all subclasses of {@link DigestAlgorithm}.
 *
 * @author  Middleware Services
 * @version  $Revision: 84 $
 */
public class DigestAlgorithmTest {

    /** Data for testing. */
    private static final String CLEARTEXT = "Able was I ere I saw Elba";

    /** Classpath location of large plaintext data file. */
    private static final String BIG_FILE_PATH = "/edu/vt/middleware/crypt/plaintext.txt";

    /**
   * Map of hash alg names to hash values calculated with GNU utility like
   * md5sum, sha1sum, etc on CLEARTEXT string.
   */
    private static final Map<String, String> REFERENCE_HASHES = new HashMap<String, String>();

    /**
   * Initialize reference hash map.
   */
    static {
        REFERENCE_HASHES.put("MD5", "837248175fec7faf267ff5864bbaa9fd");
        REFERENCE_HASHES.put("SHA-1", "b3b976e951f32db9e688218c8d6408e62b361859");
    }

    /** Logger instance. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** @return  Test data. */
    @DataProvider(name = "testdata")
    public Object[][] createTestData() {
        final SecureRandom secrnd = new SecureRandom();
        final MD2 md2 = new MD2();
        md2.setRandomProvider(secrnd);
        final MD4 md4 = new MD4();
        md4.setRandomProvider(secrnd);
        final MD5 md5 = new MD5();
        md5.setRandomProvider(secrnd);
        final RipeMD128 ripeMD128 = new RipeMD128();
        ripeMD128.setRandomProvider(secrnd);
        final RipeMD160 ripeMD160 = new RipeMD160();
        ripeMD160.setRandomProvider(secrnd);
        final RipeMD256 ripeMD256 = new RipeMD256();
        ripeMD256.setRandomProvider(secrnd);
        final RipeMD320 ripeMD320 = new RipeMD320();
        ripeMD320.setRandomProvider(secrnd);
        final SHA1 sha1 = new SHA1();
        sha1.setRandomProvider(secrnd);
        final SHA256 sha256 = new SHA256();
        sha256.setRandomProvider(secrnd);
        final SHA384 sha384 = new SHA384();
        sha384.setRandomProvider(secrnd);
        final SHA512 sha512 = new SHA512();
        sha512.setRandomProvider(secrnd);
        final Tiger tiger = new Tiger();
        tiger.setRandomProvider(secrnd);
        final Whirlpool whirlpool = new Whirlpool();
        whirlpool.setRandomProvider(secrnd);
        return new Object[][] { { md2, null }, { md2, md2.getRandomSalt() }, { md4, null }, { md4, md4.getRandomSalt() }, { md5, null }, { md5, md5.getRandomSalt() }, { ripeMD128, null }, { ripeMD128, ripeMD128.getRandomSalt() }, { ripeMD160, null }, { ripeMD160, ripeMD160.getRandomSalt() }, { ripeMD256, null }, { ripeMD256, ripeMD256.getRandomSalt() }, { ripeMD320, null }, { ripeMD320, ripeMD320.getRandomSalt() }, { sha1, null }, { sha1, sha1.getRandomSalt() }, { sha256, null }, { sha256, sha256.getRandomSalt() }, { sha384, null }, { sha384, sha384.getRandomSalt() }, { sha512, null }, { sha512, sha512.getRandomSalt() }, { tiger, null }, { tiger, tiger.getRandomSalt() }, { whirlpool, null }, { whirlpool, whirlpool.getRandomSalt() } };
    }

    /** @return  Test data. */
    @DataProvider(name = "testdataconv")
    public Object[][] createTestDataForConvert() {
        final MD5 md5 = new MD5();
        final SHA1 sha1 = new SHA1();
        return new Object[][] { { md5, new HexConverter() }, { md5, new Base64Converter() }, { sha1, new HexConverter() }, { sha1, new Base64Converter() } };
    }

    /** @return  Test data. */
    @DataProvider(name = "testdataref")
    public Object[][] createTestDataForRefTest() {
        return new Object[][] { { new MD5() }, { new SHA1() } };
    }

    /**
   * @param  digest  A digest instance to test.
   * @param  salt  Initial salt data.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "digest" }, dataProvider = "testdata")
    public void testDigest(final DigestAlgorithm digest, final byte[] salt) throws Exception {
        logger.info("Testing digest algorithm {}", digest);
        final DigestAlgorithm copy = new DigestAlgorithm(digest.getDigest());
        if (salt != null) {
            digest.setSalt(salt);
            copy.setSalt(salt);
        }
        AssertJUnit.assertEquals(digest.digest(CLEARTEXT.getBytes()), copy.digest(CLEARTEXT.getBytes()));
    }

    /**
   * @param  digest  A digest instance to test.
   * @param  converter  Converter used to convert digest output bytes to string.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "digest" }, dataProvider = "testdataconv")
    public void testDigestConvert(final DigestAlgorithm digest, final Converter converter) throws Exception {
        logger.info("Testing digest output conversion for {}", digest);
        final DigestAlgorithm copy = new DigestAlgorithm(digest.getDigest());
        AssertJUnit.assertEquals(digest.digest(CLEARTEXT.getBytes(), converter), copy.digest(CLEARTEXT.getBytes(), converter));
    }

    /**
   * @param  digest  A digest instance to test.
   * @param  converter  Converter used to convert digest output bytes to string.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "digest" }, dataProvider = "testdataconv")
    public void testDigestStream(final DigestAlgorithm digest, final Converter converter) throws Exception {
        logger.info("Testing digest stream handling for {} using converter {}", digest, converter);
        final InputStream in1 = getClass().getResourceAsStream(BIG_FILE_PATH);
        final InputStream in2 = getClass().getResourceAsStream(BIG_FILE_PATH);
        try {
            final DigestAlgorithm copy = new DigestAlgorithm(digest.getDigest());
            final String refHash = digest.digest(in1, converter);
            final String testHash = copy.digest(in2, converter);
            AssertJUnit.assertEquals(refHash, testHash);
        } finally {
            if (in1 != null) {
                in1.close();
            }
            if (in2 != null) {
                in2.close();
            }
        }
    }

    /**
   * @param  digest  A digest instance to test.
   *
   * @throws  Exception  On test failure.
   */
    @Test(groups = { "functest", "digest" }, dataProvider = "testdataref")
    public void testHashCalculationAgainstReference(final DigestAlgorithm digest) throws Exception {
        logger.info("Testing {} calculation against reference value", digest);
        final String testHash = digest.digest(CLEARTEXT.getBytes("ASCII"), new HexConverter());
        AssertJUnit.assertEquals(REFERENCE_HASHES.get(digest.getAlgorithm()), testHash);
    }
}
