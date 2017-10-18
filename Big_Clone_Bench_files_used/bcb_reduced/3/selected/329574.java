package com.shengyijie.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.shengyijie.context.ContextApplication;
import com.shengyijie.model.database.dataoperate.AccountInfoDB;
import com.shengyijie.model.object.baseobject.Category;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.telephony.TelephonyManager;

public class Utility {

    public static int getRes(Activity activity, String name) {
        ApplicationInfo appInfo = activity.getApplicationInfo();
        int resID = activity.getResources().getIdentifier(name, "drawable", appInfo.packageName);
        return resID;
    }

    public static void saveImage(String url, Drawable d) {
        try {
            if (url.length() > 0 && d != null) {
                String fileNameString = url;
                FileOutputStream fileOutputStream = null;
                if (d != null) {
                    if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                        File destDir = new File(Environment.getExternalStorageDirectory(), "shengyijie");
                        if (!destDir.exists()) {
                            destDir.mkdirs();
                        }
                        File file = new File(Environment.getExternalStorageDirectory(), "shengyijie/" + fileNameString);
                        fileOutputStream = new FileOutputStream(file);
                        BitmapDrawable bd = (BitmapDrawable) d;
                        Bitmap bm = bd.getBitmap();
                        bm.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                }
            }
        } catch (Exception e) {
        }
    }

    public static boolean isEmail(String line) {
        Pattern p = Pattern.compile("\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*");
        Matcher m = p.matcher(line);
        return m.find();
    }

    public static boolean isMobile(String mobiles) {
        Pattern p = Pattern.compile("^((13[0-9])|(14[0-9])|(15[0-9])|(16[0-9])|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$");
        Matcher m = p.matcher(mobiles);
        return m.matches();
    }

    public static String getPictureFileName(String url) {
        String name = "";
        try {
            String[] paramstrs = url.split("/");
            String temp = paramstrs[paramstrs.length - 1];
            String[] paramstr = temp.split("\\.");
            name = paramstrs[paramstrs.length - 2] + paramstr[0];
        } catch (Exception e) {
        }
        return name;
    }

    public static byte[] drawableToBytes(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        return baos.toByteArray();
    }

    public static String getTimeGap(String createTime) {
        try {
            String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(System.currentTimeMillis()));
            SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date nowDate = myFormatter.parse(now);
            java.util.Date createDate = myFormatter.parse(createTime);
            int miao = (int) ((nowDate.getTime() - createDate.getTime()) / (1000));
            if (miao <= 0) {
                return "刚刚更新";
            } else if (miao < 60) {
                return miao + "分钟前";
            } else {
                int minutes = miao / 60;
                if (minutes < 60) {
                    return minutes + "分钟前";
                } else if (minutes < 60 * 24) {
                    return (minutes / 60) + "小时前";
                } else {
                    return (minutes / 60 / 24) + "天前";
                }
            }
        } catch (Exception e) {
            return 1 + "分钟前";
        }
    }

    public static String encodeUrl(Bundle parameters) {
        if (parameters == null) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String key : parameters.keySet()) {
            if (first) first = false; else sb.append("&");
            sb.append(key + "=" + parameters.getString(key));
        }
        return sb.toString();
    }

    public static String read(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
        for (String line = r.readLine(); line != null; line = r.readLine()) {
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }

    public static String md5(String string) {
        if (string == null || string.trim().length() < 1) {
            return null;
        }
        try {
            return getMD5(string.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private static String getMD5(byte[] source) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            StringBuffer result = new StringBuffer();
            for (byte b : md5.digest(source)) {
                result.append(Integer.toHexString((b & 0xf0) >>> 4));
                result.append(Integer.toHexString(b & 0x0f));
            }
            return result.toString();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    /** 
	* 半角转全角 
	* @param input String. 
	* @return 全角字符串. 
	*/
    public static String ToSBC(String input) {
        char c[] = input.toCharArray();
        for (int i = 0; i < c.length; i++) {
            if (c[i] == ' ') {
                c[i] = '　';
            } else if (c[i] < '\177') {
                c[i] = (char) (c[i] + 65248);
            }
        }
        return new String(c);
    }

    public static String getLimitLengthString(String str, int num) {
        num = num * 2;
        char[] cs = str.toCharArray();
        int count = 0;
        int last = cs.length;
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] > 255) count += 2; else count++;
            if (count > num) {
                last = i + 1;
                break;
            }
        }
        if (count < num) return str;
        num -= 3;
        for (int i = last - 1; i >= 0; i--) {
            if (cs[i] > 255) count -= 2; else count--;
            if (count <= num) {
                return str.substring(0, i) + "...";
            }
        }
        return "...";
    }

    public static void saveUserSession(Context context, int isRember) {
        ContextApplication.user.setIsRemember(isRember);
        ContextApplication.isUserLogin = true;
        AccountInfoDB dh = new AccountInfoDB(context, "shengyijie", 1);
        dh.openWrite();
        dh.insertItem(ContextApplication.user);
        dh.close();
    }

    public static void clearUserSession(Context context) {
        ContextApplication.isUserLogin = false;
        AccountInfoDB dh = new AccountInfoDB(context, "shengyijie", 1);
        dh.openWrite();
        dh.deleteAllRecords();
        dh.close();
    }

    public static String getIMEI(Context context) {
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String imei = tm.getDeviceId();
        return imei;
    }

    public static String getPhoneType() {
        String phoneType = android.os.Build.MODEL;
        return phoneType;
    }

    public static String getIndustryName(int id) {
        String name = "";
        switch(id) {
            case 1:
                name = "服饰箱包";
                break;
            case 2:
                name = "美容保健";
                break;
            case 3:
                name = "美酒饮料";
                break;
            case 4:
                name = "家居环保";
                break;
            case 5:
                name = "餐饮娱乐";
                break;
            case 6:
                name = "教育培训";
                break;
            case 7:
                name = "新锐创意";
                break;
        }
        return name;
    }
}
