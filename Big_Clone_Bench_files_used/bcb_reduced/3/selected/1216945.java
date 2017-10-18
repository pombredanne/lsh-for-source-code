package com.founder.android.cache;

import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Bitmap;

public class MemoryCache implements Cacheable {

    private static Map<String, SoftReference<Bitmap>> cache = new HashMap<String, SoftReference<Bitmap>>();

    private static MemoryCache _this = new MemoryCache();

    private final Object LOCK = new Object();

    private MemoryCache() {
    }

    public static MemoryCache getInstance() {
        return _this;
    }

    public static String getHash(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(key.getBytes());
            return new BigInteger(digest.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return key;
        }
    }

    @Override
    public Bitmap getCacheImage(String url) {
        if (url == null) {
            return null;
        }
        String key = getHash(url);
        Bitmap value = null;
        synchronized (LOCK) {
            SoftReference<Bitmap> obj = cache.get(key);
            if (obj != null) {
                value = obj.get();
                if (value == null) {
                    cache.remove(key);
                } else {
                }
            }
        }
        return value;
    }

    @Override
    public void setCacheImage(String key, Bitmap value) {
        if (key == null || value == null) {
            return;
        }
        key = getHash(key);
        synchronized (LOCK) {
            cache.put(key, new SoftReference<Bitmap>(value));
        }
    }
}
