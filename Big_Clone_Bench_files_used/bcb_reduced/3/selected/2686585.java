package rhul.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import org.bouncycastle.util.encoders.Base64;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import rhul.bignat.Bignat;
import rhul.bignat.Convenience;
import rhul.util.BigIntUtil;
import com.gieseckedevrient.offcard.terminal.TerminalException;
import com.gieseckedevrient.offcard.util.Bytes;
import com.gieseckedevrient.offcard.util.CommandApdu;
import com.gieseckedevrient.offcard.util.ResponseApdu;

/**
 * JUnit Test class to test AsymOperation benchmark test module (asymmetric and
 * other cryptographic operations).
 * 
 * 
 * @author gruna
 * @version 0.3
 *
 */
public class AsymOperationTest extends CardTest {

    /**
	 * The currently used modulus size for asymmetric operations 
	 * in byte and bit
	 */
    private static int sizeInByte = 128;

    private static int sizeInBit = 1024;

    /**
	 * The amount of bytes that a bignat is larger than the 
	 * currently used modulus size in byte.
	 */
    private static short bnOversize = 0;

    /**
	 * Public key object for key initialisation and reference computations.
	 * The value is static as it is used within the static JUnit 4.0 setUpBeforeClass() 
	 * method.
	 */
    static RSAPublicKey pk;

    /**
	 * Private key object for key initialisation and reference computations.
	 */
    static RSAPrivateKey sk;

    /**
	 * Random object references to get random challenges.
	 */
    static SecureRandom random;

    /**
	 * Objects to store random values and modulus as big integer for 
	 * reference comparisons.
	 */
    static BigInteger r1, n;

    /**
	 * To store responses
	 */
    static ResponseApdu resp;

    /**
	 * To store commands
	 */
    static CommandApdu apdu[] = new CommandApdu[10];

    /**
	 * Buffer to contain temporary values before sending them to the card/simulation
	 */
    static byte[] buffer = new byte[sizeInByte];

    /**
	 * constants used in communication with AsymOperation JavaCard test module
	 */
    private static final byte METHOD_RSA_PKCS = 0x11;

    private static final byte METHOD_RSA_NOPAD = 0x02;

    private static final byte METHOD_RSA_SIGNATURE_PKCS = 0x03;

    private static final byte SET_RSA_N = 0x31;

    private static final byte SET_RSA_D = 0x32;

    private static final byte SET_RSA_E = 0x33;

    private static final byte SET_R = 0x34;

    private static final byte METHOD_TRUE_RANDOM = 0x40;

    private static final byte METHOD_PSEUDO_RANDOM = 0x41;

    private static final byte METHOD_SHA_HASH = 0x30;

    private static final byte GET_OUTPUT = 0x20;

    private static final byte INITIALIZED = 0x7c;

    /**
	 * JUnit 4.0 class executed once at the beginning of a test run. Initialises
	 * the used values for the tests and writes the keys to the card, if not already
	 * installed
	 * 
	 * @throws Exception
	 */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        CardTest.setUpBeforeClass(CardTest.CARD, "AB CD EF FE DC 12 34 5A");
        KeyFactory kf = KeyFactory.getInstance("RSA");
        random = SecureRandom.getInstance("SHA1PRNG");
        if (sizeInBit == 512) {
            X509EncodedKeySpec pkspec = new X509EncodedKeySpec(Base64.decode(pk512.getBytes()));
            PKCS8EncodedKeySpec skspec = new PKCS8EncodedKeySpec(Base64.decode(sk512.getBytes()));
            pk = (RSAPublicKey) kf.generatePublic(pkspec);
            sk = (RSAPrivateKey) kf.generatePrivate(skspec);
        } else if (sizeInBit == 550) {
            X509EncodedKeySpec pkspec = new X509EncodedKeySpec(Base64.decode(pk550.getBytes()));
            PKCS8EncodedKeySpec skspec = new PKCS8EncodedKeySpec(Base64.decode(sk550.getBytes()));
            pk = (RSAPublicKey) kf.generatePublic(pkspec);
            sk = (RSAPrivateKey) kf.generatePrivate(skspec);
        } else if (sizeInBit == 1024) {
            X509EncodedKeySpec pkspec = new X509EncodedKeySpec(Base64.decode(pk1024.getBytes()));
            PKCS8EncodedKeySpec skspec = new PKCS8EncodedKeySpec(Base64.decode(sk1024.getBytes()));
            pk = (RSAPublicKey) kf.generatePublic(pkspec);
            sk = (RSAPrivateKey) kf.generatePrivate(skspec);
        } else if (sizeInBit == 2048) {
            X509EncodedKeySpec pkspec = new X509EncodedKeySpec(Base64.decode(pk2048.getBytes()));
            PKCS8EncodedKeySpec skspec = new PKCS8EncodedKeySpec(Base64.decode(sk2048.getBytes()));
            pk = (RSAPublicKey) kf.generatePublic(pkspec);
            sk = (RSAPrivateKey) kf.generatePrivate(skspec);
        } else if (sizeInBit == 544) {
            X509EncodedKeySpec pkspec = new X509EncodedKeySpec(Base64.decode(pk544.getBytes()));
            PKCS8EncodedKeySpec skspec = new PKCS8EncodedKeySpec(Base64.decode(sk544.getBytes()));
            pk = (RSAPublicKey) kf.generatePublic(pkspec);
            sk = (RSAPrivateKey) kf.generatePrivate(skspec);
        } else {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(sizeInBit);
            KeyPair kp = generator.generateKeyPair();
            pk = (RSAPublicKey) kp.getPublic();
            sk = (RSAPrivateKey) kp.getPrivate();
            System.out.println(pk.getFormat());
            System.out.println(sk.getFormat());
            System.out.println(new String(Base64.encode(pk.getEncoded())));
            System.out.println(new String(Base64.encode(sk.getEncoded())));
            assertTrue(false);
        }
        n = pk.getModulus();
        r1 = new BigInteger(sizeInBit, random);
        while (r1.compareTo(n) > 0) r1 = new BigInteger(sizeInBit, random);
        System.out.println("r1: " + r1.toString());
        resp = send(applet, "", create_apdu(0xb0, 0x00, 0x00, 0x00, 0, new byte[] {}));
        System.out.println("get status: " + resp);
        resp = send(applet, "", create_apdu(0xb0, 0x01, 0x01, 0x00, 0, new byte[] {}));
        System.out.println("select: " + resp);
        System.out.println("Initialize card:");
        initCard();
    }

    /**
	 * JUnit 4.0 method called before every test. It calls the INITIALIZED function to 
	 * ensure a state change from PREPARE state of test module to RUN state. 
	 * @throws Exception
	 */
    @Before
    public void setUp() throws Exception {
        CommandApdu outAPDU = new CommandApdu(0xb0, INITIALIZED, 0x00, 0x0, new Bytes(), 0);
        resp = send(applet, "initialized?", outAPDU);
        System.out.println("initalized: " + resp);
    }

    /**
	 * Helper method to initialise the keys to the card, if not already initialised
	 * 
	 * @assert that the writing was successful for each value
	 */
    private static void initCard() {
        CommandApdu outAPDU = new CommandApdu(0xb0, INITIALIZED, 0x00, 0x0, new Bytes(), 0);
        resp = send(applet, "initialized?", outAPDU);
        System.out.println(resp);
        if (resp.getData().get(0) == 0x00) {
            System.out.println("Initializing card...");
            Bignat n_bn = Convenience.bn_from_bi(sizeInByte + bnOversize, n);
            Bignat d_bn = Convenience.bn_from_bi(sizeInByte + bnOversize, sk.getPrivateExponent());
            Bignat e_bn = Convenience.bn_from_bi(sizeInByte + bnOversize, pk.getPublicExponent());
            n_bn.to_byte_array((short) sizeInByte, bnOversize, buffer, (short) 0);
            resp = createAndSend(applet, 0xb0, SET_RSA_N, 0x00, 0x0, 0, buffer.clone());
            System.out.println(resp);
            assertTrue(resp.getStatusByte1() == 0x90);
            assertTrue(resp.getStatusByte2() == 0x00);
            d_bn.to_byte_array((short) sizeInByte, bnOversize, buffer, (short) 0);
            resp = createAndSend(applet, 0xb0, SET_RSA_D, 0x00, 0x0, 0, buffer.clone());
            System.out.println(resp);
            assertTrue(resp.getStatusByte1() == 0x90);
            assertTrue(resp.getStatusByte2() == 0x00);
            e_bn.to_byte_array((short) sizeInByte, bnOversize, buffer, (short) 0);
            resp = createAndSend(applet, 0xb0, SET_RSA_E, 0x00, 0x0, 0, buffer.clone());
            System.out.println(resp);
            assertTrue(resp.getStatusByte1() == 0x90);
            assertTrue(resp.getStatusByte2() == 0x00);
        } else {
            System.out.println("Card already initialized");
        }
    }

    /**
	 * R represents the challenge which is sent which each authentication test.
	 * R is stored in transient memory on the card using this  method.
	 * 
	 * @assert that the writing was successful
	 */
    private static void setR() {
        Bignat r1_bn = Convenience.bn_from_bi(sizeInByte + bnOversize, r1);
        r1_bn.to_byte_array((short) sizeInByte, bnOversize, buffer, (short) 0);
        resp = createAndSend(applet, 0xb0, SET_R, 0x00, 0, 0, buffer.clone());
        System.out.println("write r" + resp);
        assertTrue(resp.getStatusByte1() == 0x90);
        assertTrue(resp.getStatusByte2() == 0x00);
    }

    /**
	 * JUnit 4.0 test using the RSA interface without padding.
	 * 
	 * @throws Exception	 
	 * @asserts that the decryption and encryption in the applet was correct using 
	 * reference computation as BigInteger and with the bouncycastle rsa implementation.
	 */
    @Test
    public void testRSANOPAD() throws Exception {
        System.out.println("*nopad*");
        setR();
        byte[] input, cipherText, plainText;
        Cipher cipher;
        BigInteger bix = r1.modPow(pk.getPublicExponent(), n);
        input = Convenience.bn_from_bi(sizeInByte + bnOversize, r1).get_digit_array();
        cipher = Cipher.getInstance("RSA/None/NoPadding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        cipherText = cipher.doFinal(input, 0, sizeInByte);
        cipher.init(Cipher.DECRYPT_MODE, sk);
        plainText = cipher.doFinal(cipherText, 0, sizeInByte);
        assertArrayEquals(plainText, input);
        System.out.print("encrypt\t");
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_RSA_NOPAD, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x01, 0x01, 0x00 });
        assertArrayEquals(cipherText, res1);
        assertArrayEquals(Convenience.bn_from_bi(sizeInByte, bix).get_digit_array(), res1);
        System.out.print("set ciphertext\t");
        resp = createAndSend(applet, 0xb0, SET_R, 0x00, 0, 0, cipherText);
        assertTrue(resp.getStatusByte1() == 0x90);
        assertTrue(resp.getStatusByte2() == 0x00);
        System.out.print("decrypt\t");
        byte[] res2 = createAndReceive(applet, 0xb0, METHOD_RSA_NOPAD, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x00, 0x01, 0x00 });
        assertArrayEquals(plainText, res2);
        assertArrayEquals(Convenience.bn_from_bi(sizeInByte, r1).get_digit_array(), res2);
    }

    /**
	 * JUnit 4.0 test using the RSA interface with padding and random according to PKCS#1.
	 * 
	 * @throws Exception	 
	 * @asserts that the decryption and encryption in the applet was correct using 
	 * reference computation with the bouncycastle rsa implementation.
	 */
    @Test
    public void testRSAPKCS() throws Exception {
        System.out.println("*pkcs*");
        setR();
        byte[] input, cipherText, plainText;
        Cipher cipher;
        input = Convenience.bn_from_bi(sizeInByte + bnOversize, r1).get_digit_array();
        cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, pk);
        cipherText = cipher.doFinal(input, 0, sizeInByte - 11);
        cipher.init(Cipher.DECRYPT_MODE, sk);
        plainText = cipher.doFinal(cipherText, 0, sizeInByte);
        byte[] inputRev = new byte[plainText.length];
        System.arraycopy(input, 0, inputRev, 0, inputRev.length);
        assertArrayEquals(plainText, inputRev);
        System.out.print("encrypt\t");
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_RSA_PKCS, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x01, 0x01, 0x00 });
        cipher.init(Cipher.DECRYPT_MODE, sk);
        plainText = cipher.doFinal(res1, 0, sizeInByte);
        inputRev = new byte[plainText.length];
        System.arraycopy(input, 0, inputRev, 0, inputRev.length);
        assertArrayEquals(plainText, inputRev);
        System.out.print("set ciphertext\t");
        resp = createAndSend(applet, 0xb0, SET_R, 0x00, 0, 0, cipherText);
        assertTrue(resp.getStatusByte1() == 0x90);
        assertTrue(resp.getStatusByte2() == 0x00);
        System.out.print("decrypt\t");
        byte[] res2 = createAndReceive(applet, 0xb0, METHOD_RSA_PKCS, GET_OUTPUT, 0x00, 0x00, cipherText.length, new byte[] { 0x01, 0x00, 0x01, 0x00 });
        inputRev = new byte[sizeInByte - 11];
        System.arraycopy(input, 0, inputRev, 0, inputRev.length);
        byte[] resRev = new byte[sizeInByte - 11];
        System.arraycopy(res2, 0, resRev, 0, resRev.length);
        assertArrayEquals(resRev, inputRev);
    }

    /**
	 * JUnit 4.0 test using the RSA signature interface.
	 * 
	 * @throws Exception	 
	 * @asserts that the signature is correct and that the applet also verifies a correct
	 * signature as correct.
	 */
    @Test
    public void testRSASign() throws Exception {
        System.out.println("*sign*");
        byte[] input;
        input = Convenience.bn_from_bi(sizeInByte + bnOversize, r1).get_digit_array();
        Signature signature = Signature.getInstance("SHA1withRSA", "BC");
        signature.initSign(sk, random);
        signature.update(input);
        byte[] sigBytes = signature.sign();
        signature.initVerify(pk);
        signature.update(input);
        assertTrue(signature.verify(sigBytes));
        System.out.print("set challenge\t");
        setR();
        System.out.print("sign\t");
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_RSA_SIGNATURE_PKCS, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x00, 0x01, 0x00 });
        signature.initVerify(pk);
        signature.update(input);
        assertTrue(signature.verify(res1));
        System.out.print("set signature\t");
        resp = createAndSend(applet, 0xb0, SET_R, 0x00, 0, 0, res1);
        assertTrue(resp.getStatusByte1() == 0x90);
        assertTrue(resp.getStatusByte2() == 0x00);
        System.out.print("verify\t");
        byte[] res2 = createAndReceive(applet, 0xb0, METHOD_RSA_SIGNATURE_PKCS, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x01, 0x01, 0x00 });
        assertTrue(res2.length == sizeInByte);
    }

    /**
	 * Tests if the SHA-1 hash operation is calculating correctly.
	 * 
	 * @throws Exception
	 * @assert that the hash value is correct compared to the reference value
	 * computed using the Java Cryptography Extension/bouncycastle.
	 */
    @Test
    public void testShaHash() throws Exception {
        setR();
        System.out.println("hash\t");
        Bignat r1_bn = Convenience.bn_from_bi(sizeInByte + bnOversize, r1);
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] sha1res = md.digest(r1_bn.get_digit_array());
        System.out.println(new String(BigIntUtil.to_byte_hex_string(new BigInteger(sha1res))));
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_SHA_HASH, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x00, 0x01, 0x00 });
        System.out.println(new String(BigIntUtil.to_byte_hex_string(new BigInteger(sha1res))));
        byte[] resRev = new byte[sha1res.length];
        System.arraycopy(res1, 0, resRev, 0, resRev.length);
        assertArrayEquals(resRev, sha1res);
    }

    /**
	 * Tests the secure random value without any assertion. It is recommended to add some
	 * statistical analysis here.
	 */
    @Test
    public void testTrueRandom() {
        setR();
        System.out.print("random\t");
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_TRUE_RANDOM, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x00, 0x01, 0x00 });
    }

    /**
	 * Tests the psuedo random value without any assertion. It is recommended to add some
	 * statistical analysis here.
	 */
    @Test
    public void testPseudoRandom() {
        setR();
        System.out.print("random\t");
        byte[] res1 = createAndReceive(applet, 0xb0, METHOD_PSEUDO_RANDOM, GET_OUTPUT, 0x00, 0x00, sizeInByte, new byte[] { 0x01, 0x00, 0x01, 0x00 });
    }

    /**
	 * As last test, printing all commands so far.
	 * @throws TerminalException
	 */
    @Test
    public void printComm() throws TerminalException {
        printCommands();
    }
}
