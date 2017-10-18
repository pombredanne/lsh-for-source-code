package com.genia.toolbox.utils.manager.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;
import javax.annotation.Resource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.genia.toolbox.basics.exception.BundledException;
import com.genia.toolbox.basics.exception.technical.TechnicalEncodingException;
import com.genia.toolbox.basics.exception.technical.TechnicalException;
import com.genia.toolbox.basics.exception.technical.TechnicalIOException;
import com.genia.toolbox.basics.manager.FileManager;
import com.genia.toolbox.basics.manager.impl.CryptoManagerImpl;
import com.genia.toolbox.constants.client.Charset;
import com.genia.toolbox.spring.initializer.PluginContextLoader;

/**
 * Class for test {@link com.genia.toolbox.basics.manager.CryptoManager}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = PluginContextLoader.class)
public class CryptoManagerTest {

    /**
   * get the encryption methods.
   */
    @Resource
    private CryptoManagerImpl cryptoManager = null;

    /**
   * for test with method encryption files.
   */
    private File testFile = null;

    /**
   * for test with method encryption files.
   */
    private File tmpFile = null;

    /**
   * the {@link FileManager} to use.
   */
    @Resource
    private FileManager fileManager;

    /**
   * initialization of different objects for test.
   * 
   * @throws BundledException
   *           if the spring configuration file is not found.
   * @throws BeansException
   *           if the expected bean is not found.
   * @throws TechnicalIOException
   *           if the file was not created.
   */
    @Before
    public void init() throws BeansException, BundledException, TechnicalIOException {
        try {
            testFile = fileManager.createAutoDeletableTempFile("test", ".tmp");
            tmpFile = fileManager.createAutoDeletableTempFile("otherTest", ".xml");
            FileWriter writer = new FileWriter(tmpFile);
            writer.write("test file encryption");
            writer.close();
        } catch (IOException e) {
            throw new TechnicalIOException(e);
        }
    }

    /**
   * test method encryption for array of bytes and String with algorithm of type
   * SHA-1.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   */
    @Test
    public void testSha() throws TechnicalException {
        byte[] testHash = cryptoManager.hash("password");
        byte[] testHash2 = cryptoManager.sha1("password");
        byte[] testHash3 = cryptoManager.sha1("password".getBytes());
        byte[] testWrongPwd = cryptoManager.hash("passwor");
        Assert.assertTrue(MessageDigest.isEqual(testHash2, testHash));
        Assert.assertTrue(MessageDigest.isEqual(testHash2, testHash3));
        Assert.assertFalse(MessageDigest.isEqual(testWrongPwd, testHash));
    }

    /**
   * test method encryption for array of bytes and String with algorithm of type
   * MD5.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   */
    @Test
    public void testMd5() throws TechnicalException {
        cryptoManager.setDefaultHash(CryptoManagerImpl.Hash.MD5);
        byte[] testMD5Hash = cryptoManager.hash("testPassword");
        byte[] testWrongPwd = cryptoManager.hash("passwor");
        byte[] testMd1 = cryptoManager.md5("testPassword");
        byte[] testMd2 = cryptoManager.md5("testPassword".getBytes());
        Assert.assertTrue(MessageDigest.isEqual(testMd1, testMd2));
        Assert.assertTrue(MessageDigest.isEqual(testMD5Hash, testMd1));
        Assert.assertTrue(MessageDigest.isEqual(testMD5Hash, testMd2));
        Assert.assertFalse(MessageDigest.isEqual(testWrongPwd, testMD5Hash));
    }

    /**
   * test method encryption files with algorithm of type SHA-1.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   * @throws TechnicalIOException
   *           if file is not found.
   */
    @Test
    public void testFileEncryptionSHA() throws TechnicalIOException, TechnicalException {
        cryptoManager.setDefaultHash(CryptoManagerImpl.Hash.SHA1);
        byte[] shaTestFile = cryptoManager.hash(testFile);
        byte[] shaTestFile2 = cryptoManager.sha1(testFile);
        byte[] shaTmpFile = cryptoManager.hash(tmpFile);
        byte[] shaTmpFile2 = cryptoManager.sha1(tmpFile);
        byte[] md5Test = cryptoManager.md5(testFile);
        Assert.assertTrue(MessageDigest.isEqual(shaTestFile, shaTestFile2));
        Assert.assertTrue(MessageDigest.isEqual(shaTmpFile, shaTmpFile2));
        Assert.assertFalse(MessageDigest.isEqual(md5Test, shaTestFile));
        Assert.assertFalse(MessageDigest.isEqual(md5Test, shaTestFile2));
        Assert.assertFalse(MessageDigest.isEqual(shaTmpFile, shaTestFile));
    }

    /**
   * test method encryption files with algorithm of type MD5.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   * @throws TechnicalIOException
   *           if file is not found.
   */
    @Test
    public void testFileEncryptionMD5() throws TechnicalIOException, TechnicalException {
        cryptoManager.setDefaultHash(CryptoManagerImpl.Hash.MD5);
        byte[] md5TmpFile = cryptoManager.hash(tmpFile);
        byte[] md5TmpFile2 = cryptoManager.md5(tmpFile);
        byte[] md5TestFile = cryptoManager.hash(testFile);
        byte[] md5TestFile2 = cryptoManager.hash(testFile);
        Assert.assertTrue(MessageDigest.isEqual(md5TmpFile, md5TmpFile2));
        Assert.assertTrue(MessageDigest.isEqual(md5TestFile, md5TestFile2));
        Assert.assertFalse(MessageDigest.isEqual(md5TestFile, md5TmpFile));
        Assert.assertFalse(MessageDigest.isEqual(md5TestFile2, md5TmpFile2));
    }

    /**
   * other implementation of compute of messageDigest for test.
   * 
   * @param string
   *          the string to transform.
   * @param algorithm
   *          the type of algorithm
   * @return the string to digest.
   * @throws TechnicalException
   *           if problems with digest process.
   */
    public static byte[] messageDigestForTest(String string, String algorithm) throws TechnicalException {
        MessageDigest msgTest = null;
        byte[] bytes = null;
        try {
            msgTest = MessageDigest.getInstance(algorithm);
            bytes = string.getBytes(Charset.UTF8);
        } catch (NoSuchAlgorithmException e) {
            throw new TechnicalException(e);
        } catch (UnsupportedEncodingException e) {
            throw new TechnicalEncodingException(e);
        }
        msgTest.reset();
        return msgTest.digest(bytes);
    }

    /**
   * test messageDigest.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   */
    public void testMsgDigest(String test, String algorithm) throws TechnicalException {
        Assert.assertTrue(MessageDigest.isEqual(messageDigestForTest(test, algorithm), cryptoManager.hash(test)));
    }

    /**
   * test that a new {@link Random} does not always send the same order of
   * aleatory numbers.
   */
    public void testRandom() {
        Random r1 = new Random();
        Random r2 = new Random();
        Assert.assertFalse(r1.nextLong() == r2.nextLong());
    }

    /**
   * test messageDigest with many parameters.
   * 
   * @throws TechnicalException
   *           if problems with digest process.
   */
    @Test
    public void testMsgDigestAllParameters() throws TechnicalException {
        cryptoManager.setDefaultHash(CryptoManagerImpl.Hash.SHA1);
        testMsgDigest("abc", "SHA-1");
        testMsgDigest("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq", "SHA-1");
        testMsgDigest("1", "SHA-1");
        cryptoManager.setDefaultHash(CryptoManagerImpl.Hash.MD5);
        testMsgDigest("", "MD5");
        testMsgDigest("a", "MD5");
        testMsgDigest("abc", "MD5");
        testMsgDigest("abcdefghijklmnopqrstuvwxyz", "MD5");
        testMsgDigest("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789", "MD5");
        testMsgDigest("1", "MD5");
    }
}
