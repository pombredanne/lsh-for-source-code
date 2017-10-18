package com.pandmservices.core;

import java.sql.*;
import java.lang.*;
import java.util.*;
import java.util.Vector;
import java.security.*;

public class UniMD5 {

    private int id;

    private String description = "";

    private String idcode = "";

    private String passwd = "";

    private String fullname = "";

    private String ulogin = "";

    private boolean done = false;

    public static String doMakeMD5(String message) throws NoSuchAlgorithmException, NoSuchProviderException {
        byte[] data = message.getBytes();
        MessageDigest md;
        md = MessageDigest.getInstance("MD5", "Cryptix");
        md.update(data);
        md.update(data);
        byte[] digest1 = md.digest();
        String hashmessage = toHexString(digest1);
        return hashmessage;
    }

    public static String toHexString(byte[] data) {
        String ret = "";
        final String stuff[] = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };
        for (int i = 0; i < data.length; i++) {
            int l = ((((int) data[i]) + 128) & 0xF0) >> 4;
            int r = ((int) data[i]) + 128 & 0x0F;
            ret += stuff[l] + stuff[r];
        }
        return ret;
    }
}
