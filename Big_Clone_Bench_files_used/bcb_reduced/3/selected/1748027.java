package com.meisenberger.stealthnet.servlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.security.MessageDigest;
import com.meisenberger.stealthnet.app.StealthnetServer;

public class IOUtils {

    public static String getFileHashFromXML(File xml, File file) throws Exception {
        FileReader in = null;
        try {
            in = new FileReader(xml);
            char[] c = new char[1024 * 10];
            int read = 0;
            StringBuffer buf = new StringBuffer();
            while (read < xml.length()) {
                int r = in.read(c);
                if (r <= 0) continue;
                read += r;
                buf.append(new String(c, 0, r));
                int p = buf.lastIndexOf("</sharedfile>");
                String later = buf.substring(p + "</sharedfile>".length());
                buf.delete(p, buf.length());
                p = buf.indexOf(file.getAbsolutePath());
                if (p >= 0) {
                    p = buf.indexOf("<filehash>", p);
                    p = buf.indexOf(">", p) + 1;
                    int e = buf.indexOf("<", p);
                    String hex = buf.substring(p, e);
                    return hex;
                }
                buf.delete(0, buf.length());
                buf.append(later);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (in != null) in.close();
        }
        MessageDigest md5 = MessageDigest.getInstance("SHA-512");
        FileInputStream fin = new FileInputStream(file);
        int read = 0;
        while (read < file.length()) {
            byte[] da = new byte[1024];
            int r = fin.read(da);
            read += r;
            md5.update(da, 0, r);
        }
        fin.close();
        byte[] digest = md5.digest();
        String hex = "";
        for (int i = 0; i < digest.length; i++) {
            int bb = digest[i] & 0xff;
            if (Integer.toHexString(bb).length() == 1) hex = hex + "0";
            hex = hex + Integer.toHexString(bb);
        }
        return hex.toUpperCase();
    }

    /**
	 * returns the URI
	 * @param url
	 * @return
	 */
    public static String getURI(String url) {
        int sa = url.indexOf("://") + 3;
        int sb = url.indexOf('/', sa + 1);
        if (sb < sa || sb < 0) return "/";
        return url.substring(sb);
    }

    /**
	 * returns the server name, www.google.com, elibera.vcom;8080
	 * @param url
	 * @return
	 */
    public static String getServerName(String url) {
        int sa = url.indexOf("://") + 3;
        int sb = url.indexOf('/', sa + 1);
        if (sb < sa || sb < 0) return url;
        return url.substring(sa, sb);
    }

    /**
	 * returns the filename
	 * @param url
	 * @return
	 */
    public static String getFilename(String url) {
        int pos = url.lastIndexOf('/');
        if (pos > 0) return url.substring(pos + 1);
        return url;
    }

    public static String getFileendung(String url) {
        url = getFilename(url);
        int pos = url.lastIndexOf('.');
        if (pos > 0) return url.substring(pos + 1);
        return url;
    }

    /**
	 * writes the content to a file
	 * @param dest
	 * @param content
	 * @throws Exception
	 */
    public static void writeFile(String dest, String content) throws Exception {
        File f = new File(dest);
        if (!f.exists()) f.createNewFile();
        FileWriter fw = new FileWriter(f);
        fw.write(content);
        fw.close();
    }

    /**
	 * writes a serialiable object to a file
	 * @param dest
	 * @param content
	 * @throws Exception
	 */
    public static void writeFileObject(String dest, java.io.Serializable content) throws Exception {
        File f = new File(dest);
        if (!f.exists()) f.createNewFile();
        FileOutputStream fw = new FileOutputStream(f);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bout);
        out.writeObject(content);
        fw.write(bout.toByteArray());
        fw.close();
    }

    /**
	 * reads a serializable object from a file
	 * @param dest
	 * @param content
	 * @throws Exception
	 */
    public static Object readFileObject(String dest, java.io.Serializable content) throws Exception {
        File f = new File(dest);
        FileInputStream in = new FileInputStream(f);
        byte[] b = new byte[(int) f.length()];
        in.read(b);
        in.close();
        ByteArrayInputStream bout = new ByteArrayInputStream(b);
        ObjectInputStream out = new ObjectInputStream(bout);
        Object ob = out.readObject();
        out.close();
        return ob;
    }

    public static void writeFile(String dest, byte[] content) throws Exception {
        File f = new File(dest);
        if (!f.exists()) f.createNewFile();
        FileOutputStream fw = new FileOutputStream(f);
        fw.write(content);
        fw.close();
    }

    public static void writeFileContentToStream(File file, OutputStream out) throws Exception {
        FileInputStream fw = new FileInputStream(file);
        byte[] block = new byte[10000];
        int read = 0;
        while ((read = fw.read(block)) > 0) {
            out.write(block, 0, read);
        }
        fw.close();
    }

    /**
	 * reads the content of a file
	 * @param src
	 * @return
	 * @throws Exception
	 */
    public static String readFile(String src, String encoding) throws Exception {
        StringBuffer buffer = new StringBuffer();
        File f = new File(src);
        if (!f.exists()) {
            for (URL url : StealthnetServer.confSearchPath) {
                f = new File(url.getPath() + src);
                if (f.exists()) break;
            }
        }
        InputStreamReader fr = null;
        if (encoding == null) fr = new InputStreamReader(new FileInputStream(f)); else fr = new InputStreamReader(new FileInputStream(f), encoding);
        Reader in = new BufferedReader(fr);
        int ch;
        while ((ch = in.read()) > -1) {
            buffer.append((char) ch);
        }
        in.close();
        return buffer.toString();
    }

    /**
	 * reads a file out of the current JAr Archive
	 * @param res
	 * @param encoding
	 * @return
	 * @throws Exception
	 */
    public static String readJarRessource(String res, String encoding) throws Exception {
        StringBuffer buffer = new StringBuffer();
        InputStreamReader fr = null;
        InputStream inr = StealthnetServer.class.getResourceAsStream(res);
        if (encoding == null) fr = new InputStreamReader(inr); else fr = new InputStreamReader(inr, encoding);
        Reader in = new BufferedReader(fr);
        int ch;
        while ((ch = in.read()) > -1) {
            buffer.append((char) ch);
        }
        in.close();
        return buffer.toString();
    }

    public static byte[] readJarFile(String file) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = StealthnetServer.class.getResourceAsStream(file);
        byte[] block = new byte[5000];
        int read;
        while ((read = in.read(block)) > -1) {
            bout.write(block, 0, read);
        }
        in.close();
        return bout.toByteArray();
    }

    public static byte[] readURL(URL url) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        InputStream in = url.openStream();
        byte[] block = new byte[5000];
        int read;
        while ((read = in.read(block)) > -1) {
            bout.write(block, 0, read);
        }
        in.close();
        return bout.toByteArray();
    }

    /**
	 * liest ein Binary file
	 * @param src
	 * @return
	 * @throws Exception
	 */
    public static byte[] readBinaryFile(String src) throws Exception {
        File f = new File(src);
        if (!f.exists()) {
            for (URL url : StealthnetServer.confSearchPath) {
                f = new File(url.getPath() + src);
                if (f.exists()) break;
            }
        }
        FileInputStream fr = new FileInputStream(f);
        byte[] c = new byte[(int) f.length()];
        fr.read(c);
        fr.close();
        return c;
    }

    /**
	 * checkt ob der char in dem array ist
	 * @param c
	 * @param ar
	 * @return
	 */
    public static final boolean isCharInArray(char c, char[] ar) {
        for (int i = 0; i < ar.length; i++) {
            if (ar[i] == c) return true;
        }
        return false;
    }
}
