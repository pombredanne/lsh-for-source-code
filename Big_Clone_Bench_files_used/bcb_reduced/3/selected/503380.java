package com.c0nflict.server;

import java.util.Date;
import java.security.*;
import java.util.Random;

public class CRandom {

    private Random r = new Random();

    private long seed;

    public boolean flip = false;

    public int randomBaseOne(int cap) {
        changeSeed(cap);
        return r.nextInt(cap) + 1;
    }

    public int randomBaseOne(double cap) {
        int icap = (int) cap;
        changeSeed(icap);
        return r.nextInt(icap) + 1;
    }

    public double random() {
        return r.nextDouble();
    }

    public int randomBaseZero(int cap) {
        return r.nextInt(cap);
    }

    public void newSeed() {
        r = new Random(seedData());
    }

    private int count = 0;

    private void changeSeed(int i) {
        if (flip) seed += i * Math.random() * 10000; else seed -= i;
        flip = !flip;
        if (count > 500) {
            r = new Random(seed);
            count = 0;
        }
        count++;
    }

    private long seedData() {
        long news = seed;
        seed = new Date().getTime();
        return news;
    }

    public String MD5(String in) {
        byte[] defaultbytes = in.getBytes();
        StringBuffer hex = new StringBuffer();
        try {
            MessageDigest alg = MessageDigest.getInstance("MD5");
            alg.reset();
            alg.update(defaultbytes);
            byte[] md = alg.digest();
            for (int i = 0; i < md.length; i++) {
                String h = Integer.toHexString(0xFF & md[i]);
                if (h.length() == 1) hex.append("0");
                hex.append(h);
            }
        } catch (Exception e) {
        }
        return hex.toString();
    }
}
