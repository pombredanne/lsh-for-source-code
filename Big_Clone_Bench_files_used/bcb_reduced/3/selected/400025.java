package org.apps.butler.util.encypt;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.*;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
 * 加密解密
 *
 * @author shy.qiu
 * @since  http://blog.csdn.net/qiushyfm
 */
public class CryptTest {

    /**
     * 进行MD5加密
     *
     * @param info
     *            要加密的信息
     * @return String 加密后的字符串
     */
    public String encryptToMD5(String info) {
        byte[] digesta = null;
        try {
            MessageDigest alga = MessageDigest.getInstance("MD5");
            alga.update(info.getBytes());
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String rs = byte2hex(digesta);
        return rs;
    }

    /**
     * 进行SHA加密
     *
     * @param info
     *            要加密的信息
     * @return String 加密后的字符串
     */
    public String encryptToSHA(String info) {
        byte[] digesta = null;
        try {
            MessageDigest alga = MessageDigest.getInstance("SHA-1");
            alga.update(info.getBytes());
            digesta = alga.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        String rs = byte2hex(digesta);
        return rs;
    }

    /**
     * 创建密匙
     *
     * @param algorithm
     *            加密算法,可用 DES,DESede,Blowfish
     * @return SecretKey 秘密（对称）密钥
     */
    public SecretKey createSecretKey(String algorithm) {
        KeyGenerator keygen;
        SecretKey deskey = null;
        try {
            keygen = KeyGenerator.getInstance(algorithm);
            deskey = keygen.generateKey();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return deskey;
    }

    /**
     * 根据密匙进行DES加密
     *
     * @param key
     *            密匙
     * @param info
     *            要加密的信息
     * @return String 加密后的信息
     */
    public String encryptToDES(SecretKey key, String info) {
        String Algorithm = "DES";
        SecureRandom sr = new SecureRandom();
        byte[] cipherByte = null;
        try {
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.ENCRYPT_MODE, key, sr);
            cipherByte = c1.doFinal(info.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return byte2hex(cipherByte);
    }

    /**
     * 根据密匙进行DES解密
     *
     * @param key
     *            密匙
     * @param sInfo
     *            要解密的密文
     * @return String 返回解密后信息
     */
    public String decryptByDES(SecretKey key, String sInfo) {
        String Algorithm = "DES";
        SecureRandom sr = new SecureRandom();
        byte[] cipherByte = null;
        try {
            Cipher c1 = Cipher.getInstance(Algorithm);
            c1.init(Cipher.DECRYPT_MODE, key, sr);
            cipherByte = c1.doFinal(hex2byte(sInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new String(cipherByte);
    }

    /**
     * 创建密匙组，并将公匙，私匙放入到指定文件中
     *
     * 默认放入mykeys.bat文件中
     */
    public void createPairKey() {
        try {
            KeyPairGenerator keygen = KeyPairGenerator.getInstance("DSA");
            SecureRandom random = new SecureRandom();
            random.setSeed(1000);
            keygen.initialize(512, random);
            KeyPair keys = keygen.generateKeyPair();
            PublicKey pubkey = keys.getPublic();
            PrivateKey prikey = keys.getPrivate();
            doObjToFile("mykeys.bat", new Object[] { prikey, pubkey });
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /**
     * 利用私匙对信息进行签名 把签名后的信息放入到指定的文件中
     *
     * @param info
     *            要签名的信息
     * @param signfile
     *            存入的文件
     */
    public void signToInfo(String info, String signfile) {
        PrivateKey myprikey = (PrivateKey) getObjFromFile("mykeys.bat", 1);
        PublicKey mypubkey = (PublicKey) getObjFromFile("mykeys.bat", 2);
        try {
            Signature signet = Signature.getInstance("DSA");
            signet.initSign(myprikey);
            signet.update(info.getBytes());
            byte[] signed = signet.sign();
            doObjToFile(signfile, new Object[] { signed, mypubkey, info });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取数字签名文件 根据公匙，签名，信息验证信息的合法性
     *
     * @return true 验证成功 false 验证失败
     */
    public boolean validateSign(String signfile) {
        PublicKey mypubkey = (PublicKey) getObjFromFile(signfile, 2);
        byte[] signed = (byte[]) getObjFromFile(signfile, 1);
        String info = (String) getObjFromFile(signfile, 3);
        try {
            Signature signetcheck = Signature.getInstance("DSA");
            signetcheck.initVerify(mypubkey);
            signetcheck.update(info.getBytes());
            System.out.println(info);
            return signetcheck.verify(signed);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 将二进制转化为16进制字符串
     *
     * @param b
     *            二进制字节数组
     * @return String
     */
    public String byte2hex(byte[] b) {
        String hs = "";
        String stmp = "";
        for (int n = 0; n < b.length; n++) {
            stmp = (java.lang.Integer.toHexString(b[n] & 0XFF));
            if (stmp.length() == 1) {
                hs = hs + "0" + stmp;
            } else {
                hs = hs + stmp;
            }
        }
        return hs.toUpperCase();
    }

    /**
     * 十六进制字符串转化为2进制
     *
     * @param hex
     * @return
     */
    public byte[] hex2byte(String hex) {
        byte[] ret = new byte[8];
        byte[] tmp = hex.getBytes();
        for (int i = 0; i < 8; i++) {
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        }
        return ret;
    }

    /**
     * 将两个ASCII字符合成一个字节； 如："EF"--> 0xEF
     *
     * @param src0
     *            byte
     * @param src1
     *            byte
     * @return byte
     */
    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[] { src0 })).byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[] { src1 })).byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * 将指定的对象写入指定的文件
     *
     * @param file
     *            指定写入的文件
     * @param objs
     *            要写入的对象
     */
    public void doObjToFile(String file, Object[] objs) {
        ObjectOutputStream oos = null;
        try {
            FileOutputStream fos = new FileOutputStream(file);
            oos = new ObjectOutputStream(fos);
            for (int i = 0; i < objs.length; i++) {
                oos.writeObject(objs[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 返回在文件中指定位置的对象
     *
     * @param file
     *            指定的文件
     * @param i
     *            从1开始
     * @return
     */
    public Object getObjFromFile(String file, int i) {
        ObjectInputStream ois = null;
        Object obj = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            for (int j = 0; j < i; j++) {
                obj = ois.readObject();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                ois.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    /**
     * 测试
     *
     * @param args
     */
    public static void main(String[] args) {
        CryptTest jiami = new CryptTest();
        System.out.println("Hello经过MD5:" + jiami.encryptToMD5("Hello"));
        SecretKey key = jiami.createSecretKey("DES");
        String str1 = jiami.encryptToDES(key, "Hello");
        System.out.println("使用des加密信息Hello为:" + str1);
        String str2 = jiami.decryptByDES(key, str1);
        System.out.println("解密后为：" + str2);
        jiami.createPairKey();
        jiami.signToInfo("Hello", "mysign.bat");
        if (jiami.validateSign("mysign.bat")) {
            System.out.println("Success!");
        } else {
            System.out.println("Fail!");
        }
    }
}
