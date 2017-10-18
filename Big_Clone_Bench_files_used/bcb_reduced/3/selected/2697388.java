package com.m4f.utils.link.impl;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.m4f.utils.link.ifc.URLShortener;

public class MD5URLShortener implements URLShortener {

    public String shortURL(String url) throws NoSuchAlgorithmException, IOException {
        MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
        digest.update(url.getBytes());
        return new String(digest.digest());
    }
}
