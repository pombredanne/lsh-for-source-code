package com.siteeval.auth;

import java.security.*;
import javax.crypto.*;

public class HmacMD5 {

    public String ByteArrayToString(byte[] tk, int iLen) {
        byte[] tk1;
        tk1 = new byte[iLen * 2 + 1];
        int i, iTemp;
        byte bTemp;
        String strTemp;
        for (i = 0; i < iLen; i++) {
            iTemp = tk[i];
            if (tk[i] < 0) {
                iTemp = tk[i] + 256;
            } else {
                iTemp = tk[i];
            }
            bTemp = (byte) (iTemp / 16);
            if (bTemp < 10) {
                tk1[i * 2] = (byte) (bTemp + '0');
            } else {
                tk1[i * 2] = (byte) (bTemp - 10 + 'A');
            }
            bTemp = (byte) (iTemp % 16);
            if (bTemp < 10) {
                tk1[i * 2 + 1] = (byte) (bTemp + '0');
            } else {
                tk1[i * 2 + 1] = (byte) (bTemp - 10 + 'A');
            }
        }
        tk1[iLen * 2] = 0;
        strTemp = new String(tk1);
        return strTemp;
    }

    public byte[] StringToByteArray(String strHex, int iLen) {
        byte[] tk;
        tk = new byte[iLen];
        int i, iTemp;
        int bTemp, bValue;
        int strLen;
        char c;
        strLen = strHex.length();
        if (strLen < 2 * iLen) {
            ;
        }
        for (i = 0; i < iLen; i++) {
            bValue = (int) strHex.charAt(i * 2);
            if (('0' <= bValue) && (bValue <= '9')) {
                bTemp = (bValue - '0') * 16;
            } else if (('a' <= bValue) && (bValue <= 'f')) {
                bTemp = (bValue - 'a' + 10) * 16;
            } else if (('A' <= bValue) && (bValue <= 'F')) {
                bTemp = (bValue - 'A' + 10) * 16;
            } else {
                bTemp = 0;
                ;
            }
            bValue = (int) strHex.charAt(i * 2 + 1);
            if (('0' <= bValue) && (bValue <= '9')) {
                bTemp += (bValue - '0');
            } else if (('a' <= bValue) && (bValue <= 'f')) {
                bTemp += (bValue - 'a' + 10);
            } else if (('A' <= bValue) && (bValue <= 'F')) {
                bTemp += ((bValue - 'A' + 10));
            } else {
                ;
            }
            if (bTemp > 128) {
                bTemp = bTemp - 256;
            }
            tk[i] = (byte) bTemp;
        }
        return tk;
    }

    /**
     * �� String ����HMAC-md5����
     * @param str Ҫ����HMAC-md5��������
     �� @param str1 HMAC-md5��Կ
     * @return ����ǩ���� byte ����
     */
    public String GenerateDigest(String str, String str1) {
        byte[] KeyBytes;
        byte[] KeyBytes1;
        byte[] Temp;
        MessageDigest Digest, Digest1;
        int key_len = 0;
        int data_len = 0;
        int i = 0;
        int iTemp;
        byte[] k_ipad, k_opad, tk, tk1;
        String strTemp;
        k_ipad = new byte[64];
        k_opad = new byte[64];
        tk = new byte[64];
        tk1 = new byte[64];
        key_len = str1.length();
        KeyBytes = new byte[key_len];
        KeyBytes = str1.getBytes();
        Temp = new byte[key_len];
        if (key_len > 64) {
            try {
                Digest = MessageDigest.getInstance("MD5");
                Digest.update(KeyBytes);
                tk = Digest.digest();
                key_len = 16;
                for (i = 0; i < 16; i++) {
                    KeyBytes[i] = tk[i];
                }
            } catch (NoSuchAlgorithmException ex) {
                ex.printStackTrace();
                return "";
            }
        }
        for (i = 0; i < key_len; i++) {
            Temp[i] = KeyBytes[i];
            KeyBytes[i] ^= 0x36;
            k_ipad[i] = KeyBytes[i];
            Temp[i] ^= 0x5c;
            k_opad[i] = Temp[i];
        }
        while (i < 64) {
            k_ipad[i] = 0x36;
            k_opad[i] = 0x5c;
            i++;
        }
        try {
            data_len = str.length() / 2;
            KeyBytes1 = new byte[data_len];
            KeyBytes1 = StringToByteArray(str, data_len);
            Digest = MessageDigest.getInstance("MD5");
            Digest.update(k_ipad);
            Digest.update(KeyBytes1);
            tk = Digest.digest();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return "";
        }
        try {
            Digest1 = MessageDigest.getInstance("MD5");
            Digest1.update(k_opad);
            Digest1.update(tk);
            tk1 = Digest1.digest();
            return (ByteArrayToString(tk1, 16));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return "";
        }
    }
}
