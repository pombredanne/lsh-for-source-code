package com.diipo.weibo;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * 异步加载图像<br>
 * 图像保存规则：<br>
 * 		保存头像：sdcard-pina-portrait 以用户id命名<br>
 * 		保存微博图片：sdcard-pina-pre 以图片的url的最后一段命名
 * @author starry
 *
 */
public class AsyncImageLoader {

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    private Map<String, SoftReference<Drawable>> imageCache = new HashMap<String, SoftReference<Drawable>>();

    private Handler handler = new Handler();

    private static AsyncImageLoader asyncImageLoader;

    public static AsyncImageLoader getInstance() {
        if (asyncImageLoader == null) {
            asyncImageLoader = new AsyncImageLoader();
        }
        return asyncImageLoader;
    }

    /**
	 * 加载头像
	 * @param url 头像url
	 * @param iv 头像ImageView
	 */
    public void loadPortrait(final String url, final ImageView iv) {
        if (null == url || "".equals(url)) {
            return;
        }
        final String id = md5(url);
        if (imageCache.containsKey(id)) {
            SoftReference<Drawable> softReference = imageCache.get(id);
            if (softReference != null) {
                if (softReference.get() != null) {
                    log("image::: id = " + id + " load portrait not null , ");
                    iv.setImageDrawable(softReference.get());
                    return;
                }
            }
        }
        executorService.submit(new Runnable() {

            BitmapDrawable bitmapDrawable = null;

            @Override
            public void run() {
                try {
                    bitmapDrawable = new BitmapDrawable(new URL(url).openStream());
                    imageCache.put(id, new SoftReference<Drawable>(bitmapDrawable));
                    log("image::: put:  id = " + id + " ");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        iv.setImageDrawable(bitmapDrawable);
                    }
                });
            }
        });
    }

    private String md5(String s) {
        try {
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++) hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    void log(String msg) {
        Log.i("ImageLoader", "AsyncImageLoader--  " + msg);
    }
}
