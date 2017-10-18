package eu.medsea.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import eu.medsea.filemanager.FileBean;
import eu.medsea.filemanager.FileComparator;

public class Util {

    public static final String DIRECTORIES = "directories";

    public static final String FILES = "files";

    public Util() {
    }

    public static Map getDirectoryContents(File file) {
        return getDirectoryContents(null, file);
    }

    public static Map getDirectoryContents(String acceptMimeTypes, File file) {
        Map map = new HashMap();
        String[] fileList = file.list();
        Set directories = new TreeSet(new FileComparator());
        Set files = new TreeSet(new FileComparator());
        if (fileList != null) {
            for (int i = 0; i < fileList.length; i++) {
                File f = new File(file, fileList[i]);
                if (f.isDirectory()) {
                    directories.add(new FileBean(acceptMimeTypes, f));
                } else {
                    files.add(new FileBean(acceptMimeTypes, f));
                }
            }
        }
        map.put(DIRECTORIES, directories);
        map.put(FILES, files);
        return map;
    }

    public static String getDate(long date, DateFormat df) {
        Date d = new Date(date);
        if (df == null) return d.toString().substring(4);
        String string;
        try {
            string = df.format(d);
        } catch (Exception exception) {
            return d.toString().substring(4);
        }
        return string;
    }

    public static String getResourcePath(String resource) {
        String path = ClassLoader.getSystemResource(resource).getPath();
        return path;
    }

    public static String expandEnvironmentVariables(String s) {
        if (s != null) {
            StringBuffer buf = new StringBuffer();
            while (true) {
                int offset = s.indexOf("${");
                if (offset == -1) {
                    break;
                }
                int endOffset = s.indexOf("}");
                if (endOffset < 0 || endOffset < offset) {
                    break;
                }
                String token = s.substring(offset + 2, endOffset);
                String envValue = System.getenv(token);
                if (envValue == null) {
                    break;
                }
                buf.append(s.substring(0, offset)).append(envValue).append(s.substring(endOffset + 1));
                s = buf.toString();
            }
        }
        return s;
    }

    public static String getHash() {
        try {
            return getHash(Class.forName(Util.class.toString()));
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static boolean checkHash(String hash) {
        return hash.equals(getHash());
    }

    public static String getHash(Object o) {
        try {
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            mdAlgorithm.update(baos.toByteArray());
            byte[] digest = mdAlgorithm.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < digest.length; i++) {
                String x = Integer.toHexString(0xFF & digest[i]);
                if (x.length() < 2) x = "0" + x;
                hexString.append(x);
            }
            return (hexString.toString());
        } catch (NoSuchAlgorithmException e) {
            return (null);
        } catch (IOException e) {
            return (null);
        }
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") < 0) {
            return "";
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (extension.contains(File.separator)) {
            extension = "";
        }
        return extension;
    }

    public static String getFileName(String s) {
        int i = s.lastIndexOf("\\");
        if (i < 0 || i >= s.length() - 1) {
            i = s.lastIndexOf("/");
            if (i < 0 || i >= s.length() - 1) return s;
        }
        return s.substring(i + 1);
    }

    public static boolean canView(String fname) {
        String mimeType = MimeUtil.getMimeType(fname);
        if (mimeType.contains("text/") || mimeType.contains("image/")) {
            return true;
        }
        return false;
    }

    public static String readFile(File file) {
        StringBuffer stringbuffer = new StringBuffer("");
        try {
            BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            String line;
            while ((line = bufferedreader.readLine()) != null) {
                stringbuffer.append(line).append("\n");
            }
            bufferedreader.close();
            if ("".equals(stringbuffer.toString().trim())) {
                stringbuffer = new StringBuffer("EMPTY FILE: " + file.getName());
            }
        } catch (Exception exception) {
            stringbuffer = new StringBuffer("CANNOT READ: " + file.getName());
        }
        return stringbuffer.toString();
    }
}
