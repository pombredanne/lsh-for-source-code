package com.fogas.koll3ctions.tools;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KHash {

    private static final String FILENAME_ASIN_PATTERN = "asin_(.*)-type_(.*)-";

    private static final String AZW_PATTERN = "[@|\\<]([a-z0-9-]*azw)";

    private static final String AMAZON_PATTERN = "AMAZON_";

    private static final String ORIG_PATTERN = "(B[0-9][0-9A-Z]{5,})";

    private static final String CALIBRE_URL_PATTERN = "http://calibre-ebook.com";

    private static final String CALIBRE_PATTERN = "([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})";

    private Matcher fileNameAsinMatcher;

    private Matcher amazonMatcher;

    private Matcher origMatcher;

    private Matcher azwMatcher;

    private Matcher calibreUrlMatcher;

    private Matcher calibreMatcher;

    private MessageDigest md;

    private String charset;

    private String pathRoot;

    private int cacheSize;

    private boolean debugMode;

    public KHash(String algorithm, String charset, String pathRoot) throws NoSuchAlgorithmException {
        this(algorithm, charset, pathRoot, 16384, false);
    }

    public KHash(String algorithm, String charset, String pathRoot, boolean debugMode) throws NoSuchAlgorithmException {
        this(algorithm, charset, pathRoot, 16384, debugMode);
    }

    public KHash(String algorithm, String charset, String pathRoot, int cacheSize, boolean debugMode) throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance(algorithm);
        fileNameAsinMatcher = Pattern.compile(FILENAME_ASIN_PATTERN).matcher("");
        azwMatcher = Pattern.compile(AZW_PATTERN).matcher("");
        amazonMatcher = Pattern.compile(AMAZON_PATTERN).matcher("");
        origMatcher = Pattern.compile(ORIG_PATTERN).matcher("");
        calibreUrlMatcher = Pattern.compile(CALIBRE_URL_PATTERN).matcher("");
        calibreMatcher = Pattern.compile(CALIBRE_PATTERN).matcher("");
        this.charset = charset;
        this.pathRoot = pathRoot;
        this.cacheSize = cacheSize;
        this.debugMode = debugMode;
    }

    public String getHash(String s) {
        try {
            return getHash(s.getBytes(charset));
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    public String getHash(File file, String path, String root) {
        StringBuffer sbuff = new StringBuffer();
        if (debugMode) {
            sbuff.append(pathRoot).append(root).append("/").append(path);
            sbuff.append(file.getName());
            sbuff.insert(0, "*");
            return sbuff.toString();
        }
        if (file.getName().endsWith("azw") || file.getName().endsWith("mobi")) {
            fileNameAsinMatcher.reset(file.getName());
            if (fileNameAsinMatcher.find()) {
                sbuff.append("#").append(fileNameAsinMatcher.group(1)).append("^");
                sbuff.append(fileNameAsinMatcher.group(2));
                return sbuff.toString();
            } else {
                RandomAccessFile raf = null;
                try {
                    raf = new RandomAccessFile(file, "r");
                    byte[] b = new byte[cacheSize];
                    raf.read(b, 0, b.length);
                    raf.close();
                    final String text = new String(b);
                    amazonMatcher.reset(text);
                    int start = 0;
                    if (amazonMatcher.find()) {
                        start = amazonMatcher.start();
                        azwMatcher.reset(text);
                        if (azwMatcher.find(start)) {
                            sbuff.append("#").append(azwMatcher.group(1));
                            sbuff.append("^PDOC");
                            return sbuff.toString();
                        }
                        origMatcher.reset(text);
                        if (origMatcher.find(start)) {
                            sbuff.append("#").append(origMatcher.group(1));
                            sbuff.append("^EBOK");
                            return sbuff.toString();
                        }
                    } else {
                        calibreUrlMatcher.reset(text);
                        if (calibreUrlMatcher.find()) {
                            start = calibreUrlMatcher.start();
                            calibreMatcher.reset(text);
                            if (calibreMatcher.find(start)) {
                                sbuff.append("#").append(calibreMatcher.group(1));
                                sbuff.append("^EBOK");
                                return sbuff.toString();
                            }
                        }
                        origMatcher.reset(text);
                        if (origMatcher.find()) {
                            sbuff.append("#").append(origMatcher.group(1));
                            sbuff.append("^EBOK");
                            return sbuff.toString();
                        }
                    }
                } catch (IOException e) {
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e1) {
                        }
                    }
                }
            }
        } else if (file.getName().endsWith("aax")) {
            fileNameAsinMatcher.reset(file.getName());
            if (fileNameAsinMatcher.find()) {
                sbuff.append("#").append(fileNameAsinMatcher.group(1)).append("^");
                sbuff.append(fileNameAsinMatcher.group(2));
                return sbuff.toString();
            }
        } else if (file.getName().endsWith("tpz")) {
            RandomAccessFile raf = null;
            try {
                raf = new RandomAccessFile(file, "r");
                byte[] b = new byte[cacheSize];
                StringBuffer text = new StringBuffer();
                while (raf.read(b, 0, b.length) > -1) {
                    text.append(new String(b));
                }
                raf.close();
                origMatcher.reset(text.toString());
                if (origMatcher.find(0)) {
                    sbuff.append("#").append(origMatcher.group(1));
                    sbuff.append("^EBOK");
                    return sbuff.toString();
                }
            } catch (IOException e) {
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e1) {
                    }
                }
            }
        }
        sbuff.append(pathRoot).append(root).append("/").append(path).append(file.getName());
        return "*" + getHash(sbuff.toString());
    }

    public String getHash(byte[] bytes) {
        md.reset();
        return toHex(md.digest(bytes));
    }

    private String toHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }
}
