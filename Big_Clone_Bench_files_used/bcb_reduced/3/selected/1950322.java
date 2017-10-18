package com.jmonkey.universal.shared;

import java.awt.Color;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public final class Utility {

    private class Streams extends Thread {

        private InputStream in;

        private OutputStream out;

        private Reader read;

        private Writer write;

        private void copy(InputStream in, OutputStream out, long length) throws IOException {
            try {
                byte buffer[] = new byte[4096];
                int len = 4096;
                if (length >= 0L) while (length > 0L) {
                    if (length < 4096L) len = in.read(buffer, 0, (int) length); else len = in.read(buffer, 0, 4096);
                    if (len == -1) break;
                    length -= len;
                    out.write(buffer, 0, len);
                } else do {
                    len = in.read(buffer, 0, 4096);
                    if (len == -1) break;
                    out.write(buffer, 0, len);
                } while (true);
            } finally {
                out.flush();
            }
        }

        private void copy(Reader in, Writer out, long length) throws IOException {
            try {
                char buffer[] = new char[4096];
                int len = 4096;
                if (length >= 0L) while (length > 0L) {
                    if (length < 4096L) len = in.read(buffer, 0, (int) length); else len = in.read(buffer, 0, 4096);
                    if (len == -1) break;
                    length -= len;
                    out.write(buffer, 0, len);
                } else do {
                    len = in.read(buffer, 0, 4096);
                    if (len == -1) break;
                    out.write(buffer, 0, len);
                } while (true);
            } finally {
                out.flush();
            }
        }

        public void run() {
            try {
                if (in != null) copy(in, out, -1L); else copy(read, write, -1L);
            } catch (IOException _ex) {
                try {
                    out.close();
                } catch (IOException _ex2) {
                }
            } finally {
                try {
                    out.close();
                } catch (IOException _ex) {
                }
            }
        }

        Streams(InputStream in, OutputStream out) {
            this.in = null;
            this.out = null;
            read = null;
            write = null;
            this.in = in;
            this.out = out;
            start();
        }

        Streams(Reader in, Writer out) {
            this.in = null;
            this.out = null;
            read = null;
            write = null;
            read = in;
            write = out;
            start();
        }
    }

    private static final int _BUFFER_SIZE = 4096;

    public Utility() {
        super();
    }

    public static final String colorToHex(Color colour) {
        String colorstr = new String("#");
        String str = Integer.toHexString(colour.getRed());
        if (str.length() > 2) throw new Error("invalid red value");
        if (str.length() < 2) colorstr = colorstr + "0" + str; else colorstr = colorstr + str;
        str = Integer.toHexString(colour.getGreen());
        if (str.length() > 2) throw new Error("invalid green value");
        if (str.length() < 2) colorstr = colorstr + "0" + str; else colorstr = colorstr + str;
        str = Integer.toHexString(colour.getBlue());
        if (str.length() > 2) throw new Error("invalid green value");
        if (str.length() < 2) colorstr = colorstr + "0" + str; else colorstr = colorstr + str;
        return colorstr.toUpperCase();
    }

    public static final void copy(InputStream in, OutputStream out) {
        (new Utility()).new Streams(in, out);
    }

    public static final void copy(Reader in, Writer out) {
        (new Utility()).new Streams(in, out);
    }

    public static final char[] decodeCharArray(String encodedStr, String delim) {
        char result[] = null;
        StringTokenizer toker = new StringTokenizer(encodedStr, delim);
        int count = toker.countTokens();
        result = new char[count];
        for (int i = 0; i < count; i++) {
            try {
                result[i] = toker.nextToken().charAt(0);
                continue;
            } catch (NoSuchElementException _ex) {
                result = null;
            }
            break;
        }
        return result;
    }

    public static final String[] decodeStringArray(String encodedStr, String delim) {
        String result[] = null;
        StringTokenizer toker = null;
        toker = new StringTokenizer(encodedStr, delim);
        int count = toker.countTokens();
        result = new String[count];
        for (int i = 0; i < count; i++) {
            try {
                result[i] = toker.nextToken();
                continue;
            } catch (NoSuchElementException _ex) {
                result = null;
            }
            break;
        }
        return result;
    }

    public static final String encodeCharArray(char decoded[], String delim) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < decoded.length; i++) {
            buffer.append(decoded[i]);
            buffer.append(delim);
        }
        return buffer.toString();
    }

    public static final String encodeStringArray(String decodedStr[], String delim) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < decodedStr.length; i++) {
            buffer.append(decodedStr[i]);
            buffer.append(delim);
        }
        return buffer.toString();
    }

    public static final String encryptMD5(String decrypted) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(decrypted.getBytes());
            byte hash[] = md5.digest();
            md5.reset();
            return hashToHex(hash);
        } catch (NoSuchAlgorithmException _ex) {
            return null;
        }
    }

    public static final byte[] encryptMD5(byte decrypted[]) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(decrypted);
            byte hash[] = md5.digest();
            md5.reset();
            return hash;
        } catch (NoSuchAlgorithmException _ex) {
            return null;
        }
    }

    public static final String encryptSHA(String decrypted) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-1");
            sha.reset();
            sha.update(decrypted.getBytes());
            byte hash[] = sha.digest();
            sha.reset();
            return hashToHex(hash);
        } catch (NoSuchAlgorithmException _ex) {
            return null;
        }
    }

    public static final String ensureDirectory(String directory) {
        File dir = new File(directory);
        if (!dir.exists() || dir.exists() && !dir.isDirectory()) dir.mkdirs();
        return dir.getAbsolutePath();
    }

    public static final File[] findFiles(String file_name, String addPaths[]) {
        String propNames[] = { "user.dir", "user.home", "java.io.tmpdir", "java.class.path", "java.home", "java.ext.dirs", "java.library.path", "sun.boot.class.path", "sun.boot.library.path" };
        ArrayList filelist = new ArrayList();
        if (addPaths != null) {
            for (int a = 0; a < addPaths.length; a++) {
                String working = addPaths[a];
                if (working != null) if (working.indexOf(File.pathSeparator) > -1) {
                    if (!working.endsWith(File.pathSeparator)) working = working + File.pathSeparator;
                    for (StringTokenizer tok = new StringTokenizer(working, File.pathSeparator); tok.hasMoreTokens(); ) {
                        File file = new File(tok.nextToken() + File.separator + file_name);
                        if (file.exists() && file.isFile()) filelist.add(file);
                    }
                } else {
                    File file = new File(working + File.separator + file_name);
                    if (file.exists() && file.isFile()) filelist.add(file);
                }
            }
        }
        for (int i = 0; i < propNames.length; i++) {
            String working = System.getProperty(propNames[i]);
            if (working != null) if (working.indexOf(File.pathSeparator) > -1) {
                if (!working.endsWith(File.pathSeparator)) working = working + File.pathSeparator;
                for (StringTokenizer tok = new StringTokenizer(working, File.pathSeparator); tok.hasMoreTokens(); ) {
                    File file = new File(tok.nextToken() + File.separator + file_name);
                    if (file.exists() && file.isFile()) filelist.add(file);
                }
            } else {
                File file = new File(working + File.separator + file_name);
                if (file.exists() && file.isFile()) filelist.add(file);
            }
        }
        File output[] = new File[filelist.size()];
        for (int f = 0; f < filelist.size(); f++) output[f] = (File) filelist.get(f);
        return output;
    }

    public static final String hashToHex(byte hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            if ((hash[i] & 0xff) < 16) buf.append("0");
            buf.append(Integer.toHexString(hash[i] & 0xff));
        }
        return buf.toString();
    }

    public static final String hashToHex(char hash[]) {
        StringBuffer buf = new StringBuffer(hash.length * 2 * 2);
        for (int i = 0; i < hash.length; i++) {
            int result = hash[i] & 0xffff;
            if (result < 16) buf.append("000"); else if (result < 256) buf.append("00"); else if (result < 4096) buf.append("0");
            buf.append(Integer.toHexString(hash[i] & 0xffff));
        }
        return buf.toString();
    }

    public static final Object loadObject(File file) throws IOException, FileNotFoundException, ClassNotFoundException {
        if (!file.exists() || file.exists() && file.isDirectory()) {
            throw new FileNotFoundException("The file: " + file.getAbsolutePath() + " was not found...");
        } else {
            ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object out = oin.readObject();
            oin.close();
            return out;
        }
    }

    public static final Object loadObject(String filePath) throws IOException, FileNotFoundException, ClassNotFoundException {
        File file = new File(filePath);
        if (!file.exists() || file.exists() && file.isDirectory()) {
            throw new FileNotFoundException("The file: " + filePath + " was not found...");
        } else {
            ObjectInputStream oin = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
            Object out = oin.readObject();
            oin.close();
            return out;
        }
    }

    public static final Properties loadProperties(File file) throws IOException, FileNotFoundException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        Properties props = new Properties();
        props.load(bis);
        bis.close();
        return props;
    }

    public static final Properties loadProperties(String file) throws IOException, FileNotFoundException {
        return loadProperties(new File(file));
    }

    public static final void loadProperties(Properties properties, File file) throws IOException, FileNotFoundException {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
        properties.load(bis);
        bis.close();
    }

    public static final void loadProperties(Properties properties, String file) throws IOException, FileNotFoundException {
        loadProperties(properties, new File(file));
    }

    public static final String padStringEnd(String unpadded, char padChar, int length) {
        StringBuffer buffer;
        for (buffer = new StringBuffer(unpadded); buffer.length() < length; buffer.append(padChar)) ;
        return buffer.toString();
    }

    public static final String padStringStart(String unpadded, char padChar, int length) {
        StringBuffer buffer;
        for (buffer = new StringBuffer(unpadded); buffer.length() < length; buffer.insert(0, padChar)) ;
        return buffer.toString();
    }

    public static final void saveObject(File file, Serializable object) throws IOException, FileNotFoundException {
        ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        oout.writeObject(object);
        oout.flush();
        oout.close();
    }

    public static final void saveObject(String filePath, Serializable object) throws IOException, FileNotFoundException {
        File file = new File(filePath);
        ObjectOutputStream oout = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
        oout.writeObject(object);
        oout.flush();
        oout.close();
    }

    public static final void saveProperties(Properties properties, File file) throws IOException {
        saveProperties(properties, file, "Core Properties");
    }

    public static final void saveProperties(Properties properties, File file, String description) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        properties.store(bos, description);
        bos.close();
    }
}
