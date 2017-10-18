package com.sts.webmeet.content.client.appshare;

import java.security.MessageDigest;

public class HashChecker {

    private byte[] baHashPrevious;

    private static final String ALGO = "MD5";

    private MessageDigest md;

    public HashChecker() {
        try {
            md = MessageDigest.getInstance(ALGO);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean dataChanged(byte[] ba) {
        boolean bRet = true;
        byte[] baHashNew = md.digest(ba);
        md.reset();
        if (null != baHashPrevious) {
            if (MessageDigest.isEqual(baHashNew, baHashPrevious)) {
                bRet = false;
            }
        }
        baHashPrevious = baHashNew;
        return bRet;
    }
}
