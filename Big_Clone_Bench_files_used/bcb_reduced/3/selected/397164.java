package com.aide.simplification.global;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.configuration.ConfigurationException;
import org.directwebremoting.annotations.RemoteProxy;

@RemoteProxy(name = "JavaUtils")
public class Utils {

    public static String getMd5(String str) {
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            final byte b[] = md.digest();
            int i;
            final StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0) {
                    i += 256;
                }
                if (i < 16) {
                    buf.append("0");
                }
                buf.append(Integer.toHexString(i));
            }
            return buf.toString();
        } catch (final NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getUUID() {
        String uuid = "";
        uuid = java.util.UUID.randomUUID().toString();
        return uuid;
    }

    public static String getConfig(String cpath) {
        try {
            GlobalConfig config = new GlobalConfig();
            return config.getString(cpath);
        } catch (ConfigurationException e) {
            return "";
        }
    }

    public static String getConfigSafe(String cpath) {
        try {
            GlobalConfig config = new GlobalConfig();
            return config.getString(cpath);
        } catch (ConfigurationException e) {
            return null;
        }
    }

    public static boolean saveConfig(String name, String value) {
        try {
            GlobalConfig config = new GlobalConfig();
            config.setProperty(name, value);
            return config.save();
        } catch (ConfigurationException e) {
            return false;
        }
    }

    public static List<String> findAll(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        List<String> list = new ArrayList<String>();
        while (matcher.find()) {
            list.add(matcher.group());
        }
        return list;
    }

    public static String find(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        if (matcher.find()) {
            return matcher.group();
        }
        return "";
    }
}
