package org.tcpfile.updater;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import javax.swing.JProgressBar;

/**
 * @author JD-Team Webupdater lädt pfad und hash infos von einem server und
 *         vergleicht sie mit den lokalen versionen
 */
public class WebUpdater implements Serializable {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1946622313175234371L;

    /**
	 * Pfad zur lis.php auf dem updateserver
	 */
    public String onlinePath = Main.updatelocation;

    /**
	 * anzahl der aktualisierten Files
	 */
    private transient int updatedFiles = 0;

    /**
	 * Anzahl der ganzen Files
	 */
    private transient int totalFiles = 0;

    private static StringBuffer logger;

    private JProgressBar progresslist = null;

    private JProgressBar progressload = null;

    /**
	 * @param path
	 *            (Dir Pfad zum Updateserver)
	 */
    public WebUpdater(String path) {
        logger = new StringBuffer();
        if (path != null) {
            this.setListPath(path);
        } else {
            setListPath("http://localhost/updates/release/");
        }
    }

    public void setLogger(StringBuffer log) {
        logger = log;
    }

    public static void log(String buf) {
        System.out.println(buf);
        if (logger != null) logger.append(new Date() + ":" + buf + System.getProperty("line.separator"));
    }

    public StringBuffer getLogger() {
        return logger;
    }

    public static String getLocalHash(File f) {
        try {
            if (!f.exists()) return null;
            MessageDigest md;
            md = MessageDigest.getInstance("SHA1");
            byte[] b = new byte[1024];
            InputStream in = new FileInputStream(f);
            for (int n = 0; (n = in.read(b)) > -1; ) {
                md.update(b, 0, n);
            }
            byte[] digest = md.digest();
            String ret = "";
            for (int i = 0; i < digest.length; i++) {
                String tmp = Integer.toHexString(digest[i] & 0xFF);
                if (tmp.length() < 2) tmp = "0" + tmp;
                ret += tmp;
            }
            in.close();
            return ret;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String htmlDecode(String str) {
        if (str == null) return null;
        String pattern = "\\&\\#x(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find(); ) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 16);
                str = str.replaceFirst("\\&\\#x(.*?)\\;", c + "");
            }
        }
        pattern = "\\&\\#(.*?)\\;";
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(str); r.find(); ) {
            if (r.group(1).length() > 0) {
                char c = (char) Integer.parseInt(r.group(1), 10);
                str = str.replaceFirst("\\&\\#(.*?)\\;", c + "");
            }
        }
        try {
            str = URLDecoder.decode(str, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        return null;
    }

    /**
	 * @author JD-Team
	 * @param str
	 * @return str als UTF8Decodiert
	 */
    private String UTF8Decode(String str) {
        try {
            return new String(str.getBytes(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * LIest eine webseite ein und gibt deren source zurück
	 * 
	 * @param urlStr
	 * @return String inhalt von urlStr
	 */
    public String getRequest(String url) {
        try {
            URL link = new URL(url);
            HttpURLConnection httpConnection = (HttpURLConnection) link.openConnection();
            httpConnection.setReadTimeout(10000);
            httpConnection.setReadTimeout(10000);
            httpConnection.setInstanceFollowRedirects(true);
            httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
            BufferedReader rd;
            if (httpConnection.getHeaderField("Content-Encoding") != null && httpConnection.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
                rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(httpConnection.getInputStream())));
            } else {
                rd = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
            }
            String line;
            StringBuffer htmlCode = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                htmlCode.append(line + "\n");
            }
            httpConnection.disconnect();
            return htmlCode.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Lädt fileurl nach filepath herunter
	 * 
	 * @param filepath
	 * @param fileurl
	 * @return true/False
	 */
    public static boolean downloadBinary(String filepath, String fileurl) {
        try {
            File file = new File(filepath);
            if (file.isFile()) {
                if (!file.delete()) {
                    log("Konnte Datei nicht löschen " + file);
                    return false;
                }
            }
            if (file.getParentFile() != null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
            int i = fileurl.lastIndexOf('/');
            fileurl = fileurl.substring(0, i + 1) + fileurl.substring(i + 1).replaceAll(" ", "%20");
            URL url = new URL(fileurl);
            URLConnection con = url.openConnection();
            BufferedInputStream input = new BufferedInputStream(con.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file, true));
            byte[] b = new byte[1024];
            int len;
            while ((len = input.read(b)) != -1) {
                output.write(b, 0, len);
            }
            output.close();
            input.close();
            return true;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
	 * @author JD-Team Macht ein urlRawEncode und spart dabei die angegebenen
	 *         Zeichen aus
	 * @param str
	 * @return str URLCodiert
	 */
    private static String urlEncode(String str) {
        try {
            str = URLDecoder.decode(str, "UTF-8");
            String allowed = "1234567890QWERTZUIOPASDFGHJKLYXCVBNMqwertzuiopasdfghjklyxcvbnm-_.?/\\:&=;";
            String ret = "";
            int i;
            for (i = 0; i < str.length(); i++) {
                char letter = str.charAt(i);
                if (allowed.indexOf(letter) >= 0) {
                    ret += letter;
                } else {
                    ret += "%" + Integer.toString(letter, 16);
                }
            }
            return ret;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;
    }

    /**
	 * @return the listPath
	 */
    public String getListPath() {
        return onlinePath;
    }

    /**
	 * @param listPath
	 *            the listPath to set
	 */
    public void setListPath(String listPath) {
        this.onlinePath = listPath;
        log("Update from " + listPath);
    }

    public void setListProgress(JProgressBar progresslist) {
        this.progresslist = progresslist;
    }

    public void setDownloadProgress(JProgressBar progresslist) {
        this.progressload = progresslist;
    }

    public Collection<UpdateFile> getAllFiles() {
        String list = getList();
        String[] files = list.replaceAll("\r", "").split("\n");
        ArrayList<UpdateFile> out = new ArrayList<UpdateFile>();
        for (String s : files) {
            String cur = s.trim();
            if (cur.matches(".*?\\:[a-fA-F0-9]{40}")) {
                int i = cur.lastIndexOf(':');
                String relpath = cur.substring(0, i);
                String hash = cur.substring(i + 1);
                UpdateFile uf = new UpdateFile(hash, relpath);
                out.add(uf);
            }
        }
        return out;
    }

    public String getList() {
        String out = getRequest(onlinePath + "list.txt");
        log("Getting list from " + onlinePath + "list.txt");
        if (progresslist != null) progresslist.setValue(100);
        return out;
    }

    public void filterAvailableUpdates(Collection<UpdateFile> files) {
        filterAvailableUpdates(files, new File("."));
    }

    public void filterAvailableUpdates(Collection<UpdateFile> files, File toplevel) {
        log("Listed: " + files.size());
        for (Iterator<UpdateFile> iterator = files.iterator(); iterator.hasNext(); ) {
            UpdateFile updateFile = iterator.next();
            if (!Main.updatePlugins && updateFile.getRelativePath().startsWith("plugins")) {
                iterator.remove();
                continue;
            }
            if (updateFile.compareWithLocal(toplevel) || updateFile.getRelativePath().equals("updater.jar")) {
                iterator.remove();
            }
        }
        log("After Filtering: " + files.size());
    }

    public void updateFiles(Collection<UpdateFile> files, boolean overwriteFiles) {
        for (UpdateFile uf : files) {
            if (overwriteFiles) uf.updateLocal(onlinePath);
            log("Updated " + uf);
            updatedFiles++;
            if (progressload != null) progressload.setValue(updatedFiles);
        }
    }
}
