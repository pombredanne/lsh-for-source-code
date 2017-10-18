package com.calfater.mailcarbon;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.commons.codec.binary.Hex;

public class Test {

    public static void main(String[] args) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        System.out.println(Hex.encodeHex(MessageDigest.getInstance("MD5").digest("plop2".getBytes("UTF-8"))));
    }
}
