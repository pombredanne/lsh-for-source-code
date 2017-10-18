package com.umc.helper;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.LongBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import com.umc.plugins.moviedb.AbstractFanartPlugin;
import com.umc.plugins.moviedb.AbstractImagePlugin;
import com.umc.plugins.moviedb.AbstractMovieDBPlugin;
import com.umc.plugins.moviedb.AbstractSeriesDBPlugin;
import com.umc.plugins.moviedb.AbstractWebSearchPlugin;

/**
 * Utility-Klasse
 * 
 * @author DonGyros
 *
 */
public class UMCUtils {

    private static Logger log = Logger.getLogger(Class.class);

    /**
     * Size of the chunks that will be hashed in bytes (64 KB)
     */
    private static final int HASH_CHUNK_SIZE = 64 * 1024;

    /**
	 * Durch diesen Aufruf kann für Online-Vebrindungen ein Proxy angegeben werden.
	 * 
	 * @param host Proxy-Host
	 * @param port Proxy-Port
	 * @param usr User für proxy-Authentifizierung
	 * @param pwd Passwort für proxy-Authentifizierung
	 *
	 */
    public static void useProxy(String host, String port, String usr, String pwd) {
        System.setProperty("proxySet", "true");
        System.setProperty("proxyPort", port);
        System.setProperty("proxyHost", host);
        System.setProperty("http.proxyUser", usr);
        System.setProperty("http.proxyPassword", pwd);
    }

    /**
	 * Mit Hilfe dieser Methode kann überprüft werden ob eine Onlinve-Verbindugn exisitiert.
	 * @return
	 */
    public static boolean testConnectivity() {
        return false;
    }

    public static AbstractImagePlugin loadImagePlugin(String aClassName) {
        try {
            AbstractImagePlugin imageInt = (AbstractImagePlugin) Class.forName(aClassName, true, ClassLoader.getSystemClassLoader()).newInstance();
            return imageInt;
        } catch (Exception exc) {
            log.error("Fehler im ClassLoader", exc);
            return null;
        }
    }

    public static AbstractMovieDBPlugin loadMovieDBPlugin(String aClassName) {
        try {
            AbstractMovieDBPlugin movieDBInt = (AbstractMovieDBPlugin) Class.forName(aClassName, true, ClassLoader.getSystemClassLoader()).newInstance();
            return movieDBInt;
        } catch (Exception exc) {
            log.error("Fehler im ClassLoader", exc);
            return null;
        }
    }

    public static AbstractSeriesDBPlugin loadSeriesDBPlugin(String aClassName) {
        try {
            AbstractSeriesDBPlugin seriesDBInt = (AbstractSeriesDBPlugin) Class.forName(aClassName, true, ClassLoader.getSystemClassLoader()).newInstance();
            return seriesDBInt;
        } catch (Exception exc) {
            log.error("Fehler im ClassLoader", exc);
            return null;
        }
    }

    public static AbstractFanartPlugin loadFanartPlugin(String aClassName) {
        try {
            AbstractFanartPlugin fanartInt = (AbstractFanartPlugin) Class.forName(aClassName, true, ClassLoader.getSystemClassLoader()).newInstance();
            return fanartInt;
        } catch (Exception exc) {
            log.error("Fehler im ClassLoader", exc);
            return null;
        }
    }

    public static AbstractWebSearchPlugin loadWebSearchPlugin(String aClassName) {
        try {
            AbstractWebSearchPlugin imageInt = (AbstractWebSearchPlugin) Class.forName(aClassName, true, ClassLoader.getSystemClassLoader()).newInstance();
            return imageInt;
        } catch (Exception exc) {
            log.error("Fehler im ClassLoader", exc);
            return null;
        }
    }

    /**
	 * This method dynamically loads a requested plugin.
	 * 
	 * @param path Relative path to the plugin
	 * @param className Plugin class name
	 * @return The loaded plugin
	 * @throws Exception
	 */
    public static Object loadPlugin(String path, String className) throws Exception {
        URL url = new File(path).toURI().toURL();
        URLClassLoader cl = new URLClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader());
        Class c = cl.loadClass("com.umc.plugins.seriesparser." + className);
        return c.newInstance();
    }

    /**
	 * This method dynamically loads a requested widget.
	 * 
	 * @param path Relative path to the widget
	 * @param className Widget class name
	 * @return The loaded widget
	 * @throws Exception
	 */
    public static Object loadWidget(String path, String packageName, String className) throws Exception {
        try {
            URL url = new File(path).toURI().toURL();
            URLClassLoader cl = new URLClassLoader(new URL[] { url }, ClassLoader.getSystemClassLoader());
            Class c = cl.loadClass(packageName + "." + className);
            return c.newInstance();
        } catch (ClassNotFoundException exc) {
            throw new Exception(exc);
        } catch (Exception exc) {
            throw new Exception(exc);
        } catch (Error exc) {
            throw new Exception(exc);
        }
    }

    /**
	 * Gets the hard disk serial number -> Win OS ONLY !!!!
	 * 
	 * This serial number is created by the OS where formatting the drive and it's not the manufacturer 
	 * serial number. It's unique, because it is created on the fly based on the current time information. 
	 * AFAIK, there is no API that return that the manufacturer SN. At best, the SN of the HD firmware can 
	 * be read but this will involve some very low-level API calls. Keep in mind that even if you get that 
	 * number, there is no warranty that it will be unique since each manufacturer can assign the SN as they 
	 * wish.
	 * 	
	 * Example: 
	 * 
	 * String serialNumber = DiskUtils.getSerialNumber("C");//Serialnumber for the C Drive
	 * 
	 * @param drive
	 * @return
	 */
    public static String getSerialNumber(String drive) {
        String result = "";
        try {
            File file = File.createTempFile("realhowto", ".vbs");
            file.deleteOnExit();
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set objFSO = CreateObject(\"Scripting.FileSystemObject\")\n" + "Set colDrives = objFSO.Drives\n" + "Set objDrive = colDrives.item(\"" + drive + "\")\n" + "Wscript.Echo objDrive.SerialNumber";
            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            while ((line = input.readLine()) != null) {
                result += line;
            }
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result.trim();
    }

    /**
	 * Diese Methode entfernt aus einem ermittelten Filmnamen alle Sonderzeichen.
	 * Dies is nötig um z.B. ein Cover oder Poster mit Hilfe des Filmttitel auf der Festplatte
	 * speichern zu können.
	 *  
	 * @param aTile Ein Titel der bereinigt werden soll
	 */
    public static String getAdjustedTitle(String aTitle) {
        if (aTitle != null && !aTitle.equals("")) {
            for (int h = 0; h < UMCConstants.charsNotAllowedInFilename.length; h++) {
                aTitle = aTitle.replaceAll(UMCConstants.charsNotAllowedInFilename[h], "");
            }
        }
        return aTitle;
    }

    /**
     * Liefert alle Zeichensätze die auf dem System installiert sind.
     * 
     * @return Liste mit allen gefundenen Zeichensätzen (z.B. Arial ,Arial Black , Arial Narrow , ...., Wingdings ,Wingdings 2 ,...)
     */
    public static Collection<String> getAvailableFontFamilyNames() {
        Collection<String> result = new ArrayList<String>();
        for (String fonts : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            result.add(fonts);
        }
        return result;
    }

    /**
     * Deletes all files and subdirectories under dir.
     * Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    public static String escapePattern(String pattern) {
        Pattern p = Pattern.compile("[\\[\\]\\{\\}\\.\\+\\*\\?\\(\\)\\^]");
        Matcher m = p.matcher(pattern.replaceAll("\\\\", "\\\\\\\\"));
        StringBuffer sb = new StringBuffer();
        boolean result = m.find();
        while (result) {
            m.appendReplacement(sb, "\\\\" + m.group(0));
            result = m.find();
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /**
     * Creates a real so called 'deep' copy of a given object.
     * 
     * @param o a object to be copied
     * @return the copied object
     */
    public static Object deepCopy(Object o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(o);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            return new ObjectInputStream(bais).readObject();
        } catch (IOException e) {
            log.error("IOException while deep copying object", e);
            return null;
        } catch (ClassNotFoundException e) {
            log.error("ClassNotFoundException while deep copying object", e);
            return null;
        }
    }

    /**
	 * Creates an MD5 hashcode for a given file. The hashcode will be generated by reading the first 1024 bytes
	 * of the file.
	 * 
	 * @param file
	 * @return MD5 hash or null
	 */
    public static String createHashCode(String file) {
        try {
            File f = new File(file);
            if (f.exists()) {
                InputStream is = null;
                is = new FileInputStream(f);
                byte[] bytes = new byte[(int) 256];
                int offset = 0;
                int numRead = 0;
                while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                    offset += numRead;
                }
                is.close();
                MessageDigest m = MessageDigest.getInstance("MD5");
                m.update(bytes, 0, 256);
                return new BigInteger(1, m.digest()).toString();
            }
            return null;
        } catch (Exception exc) {
            log.error("Could not create MD5 has for " + file, exc);
            return null;
        }
    }

    /**
	 * Will decode a given base64 encodet string.
	 * 
	 * @param encoded An Base64 encodet string
	 * @return byte[]
	 */
    public static byte[] decodeBase64(String encoded) {
        try {
            byte[] decoded = Base64.decodeBase64(encoded.getBytes());
            return decoded;
        } catch (Exception exc) {
            log.error("Could not decode Base64 encodet string", exc);
            return null;
        }
    }

    /**
     * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
     * checksum of the first and last 64k (even if they overlap because the file is smaller than
     * 128k).
     */
    public static String computeHash(String file) {
        try {
            return computeHash(new File(file));
        } catch (IOException exc) {
            log.error("Could not compute hash for " + file, exc);
            return null;
        }
    }

    /**
     * Hash code is based on Media Player Classic. In natural language it calculates: size + 64bit
     * checksum of the first and last 64k (even if they overlap because the file is smaller than
     * 128k).
     */
    public static String computeHash(File file) throws IOException {
        long size = file.length();
        long chunkSizeForFile = Math.min(HASH_CHUNK_SIZE, size);
        FileChannel fileChannel = new FileInputStream(file).getChannel();
        try {
            long head = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, 0, chunkSizeForFile));
            long tail = computeHashForChunk(fileChannel.map(MapMode.READ_ONLY, Math.max(size - HASH_CHUNK_SIZE, 0), chunkSizeForFile));
            return String.format("%016x", size + head + tail);
        } finally {
            fileChannel.close();
        }
    }

    private static long computeHashForChunk(ByteBuffer buffer) {
        LongBuffer longBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asLongBuffer();
        long hash = 0;
        while (longBuffer.hasRemaining()) {
            hash += longBuffer.get();
        }
        return hash;
    }

    public static void extract(String zipfile, String destDir) {
        try {
            FileInputStream fis = new FileInputStream(zipfile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry zipentry;
            while ((zipentry = zis.getNextEntry()) != null) {
                int slash = zipentry.getName().lastIndexOf('/');
                int dot = 0;
                dot = zipentry.getName().indexOf('.');
                if (slash > 0) {
                    if (dot == -1) {
                        String filename = destDir + zipentry.getName().substring(0, zipentry.getName().lastIndexOf('/'));
                        File f = new File(filename);
                        if (!f.exists()) {
                            f.mkdirs();
                        }
                    }
                }
                if (dot > 0) {
                    int size;
                    byte[] buffer = new byte[2048];
                    FileOutputStream fos = new FileOutputStream(destDir + zipentry.getName());
                }
            }
        } catch (Exception ex) {
        }
    }

    public static final void unzip(String zip, String destination) {
        Enumeration entries;
        ZipFile zipFile;
        if (StringUtils.isEmpty(zip) || StringUtils.isEmpty(destination)) {
            log.error("Needed params not given");
            return;
        }
        try {
            zipFile = new ZipFile(zip);
            entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (entry.isDirectory()) {
                    (new File(destination + UMCConstants.fileSeparator + entry.getName())).mkdir();
                    continue;
                }
                copyInputStream(zipFile.getInputStream(entry), new BufferedOutputStream(new FileOutputStream(destination + UMCConstants.fileSeparator + entry.getName())));
            }
            zipFile.close();
        } catch (IOException ioe) {
            System.err.println("Unhandled exception:");
            ioe.printStackTrace();
            return;
        }
    }

    private static final void copyInputStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int len;
        while ((len = in.read(buffer)) >= 0) out.write(buffer, 0, len);
        in.close();
        out.close();
    }

    public static void extractArchive(File archive, File destDir) throws Exception {
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipFile zipFile = new ZipFile(archive);
        Enumeration entries = zipFile.entries();
        byte[] buffer = new byte[16384];
        int len;
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String entryFileName = entry.getName();
            File dir = buildDirectoryHierarchyFor(entryFileName, destDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (!entry.isDirectory()) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(new File(destDir, entryFileName)));
                BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                while ((len = bis.read(buffer)) > 0) {
                    bos.write(buffer, 0, len);
                }
                bos.flush();
                bos.close();
                bis.close();
            }
        }
    }

    private static File buildDirectoryHierarchyFor(String entryName, File destDir) {
        int lastIndex = entryName.lastIndexOf('/');
        String entryFileName = entryName.substring(lastIndex + 1);
        String internalPathToEntry = entryName.substring(0, lastIndex + 1);
        return new File(destDir, internalPathToEntry);
    }

    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
    }
}
