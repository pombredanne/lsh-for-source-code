package libretunes;

import java.awt.Component;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.JOptionPane;

/**
 * Various functions
 * @author Daniel Dreibrodt
 */
public class Helper {

    private static Desktop desktop;

    /**
   * Checks whether the Java desktop support is available
   */
    static {
        if (Desktop.isDesktopSupported()) {
            desktop = Desktop.getDesktop();
        }
    }

    /**
   * Gets the content of a website
   * @param url The website url
   * @param args GET arguments
   * @return The website's source code
   */
    public static String getURL(String url, Hashtable<String, String> args) {
        try {
            String get = "";
            Enumeration e = args.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement().toString();
                if (get.length() == 0) {
                    get += key + "=" + URLEncoder.encode(args.get(key), "UTF-8");
                } else {
                    get += "&" + key + "=" + URLEncoder.encode(args.get(key), "UTF-8");
                }
            }
            URL uri = new URL(url + "?" + get);
            HttpURLConnection con = (HttpURLConnection) uri.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            con.setUseCaches(false);
            InputStream in = con.getInputStream();
            int c = 0;
            StringBuffer incoming = new StringBuffer();
            while (c >= 0) {
                c = in.read();
                incoming.append((char) c);
            }
            con.disconnect();
            return incoming.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Could not GET " + url);
            return null;
        }
    }

    /**
   * Posts arguments to a website and gets its reply
   * @param url The website's URL
   * @param args The POST arguments
   * @return The website's source code
   */
    public static String postURL(String url, Hashtable<String, String> args) {
        try {
            String post = "";
            Enumeration e = args.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement().toString();
                if (post.length() == 0) {
                    post += key + "=" + URLEncoder.encode(args.get(key), "UTF-8");
                } else {
                    post += "&" + key + "=" + URLEncoder.encode(args.get(key), "UTF-8");
                }
            }
            URL uri = new URL(url);
            HttpURLConnection con = (HttpURLConnection) uri.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);
            con.setUseCaches(false);
            PrintWriter out = new PrintWriter(con.getOutputStream());
            out.print(post);
            out.close();
            InputStream in = con.getInputStream();
            int c = 0;
            StringBuffer incoming = new StringBuffer();
            while (c >= 0) {
                c = in.read();
                incoming.append((char) c);
            }
            con.disconnect();
            return incoming.toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.err.println("Could not POST data to " + url);
            return null;
        }
    }

    /**
   * Creates a MD5 hash
   * @param s The string to hash
   * @return The hash
   */
    public static String hashMD5(String s) {
        byte[] bytearr = s.getBytes();
        MessageDigest md;
        String hash = "";
        try {
            md = MessageDigest.getInstance("md5");
            md.reset();
            md.update(bytearr);
            byte[] result = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < result.length; i++) {
                hexString.append(Integer.toHexString((0xFF & result[i]) | 0x100).substring(1, 3));
            }
            hash = hexString.toString();
        } catch (NoSuchAlgorithmException nsa) {
            System.out.println(nsa.getMessage());
        }
        return hash;
    }

    /**
   * Gets the greatest width of the given components
   * @param compos The components that should be checked
   * @return The maximum width
   */
    public static int maxWidth(Component[] compos) {
        int w = 0;
        for (Component c : compos) {
            int cw = c.getPreferredSize().width;
            if (cw > w) {
                w = cw;
            }
        }
        return w;
    }

    /**
   * Opens the given webpage in the systems default browser
   * @param url The URL to the webpage
   */
    public static void browse(final String url) {
        new Thread() {

            @Override
            public void run() {
                if (desktop != null) {
                    try {
                        desktop.browse(new java.net.URI(url));
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, ex.toString(), ex.getMessage(), JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(null, Language.get("ERROR_BROWSE_MSG").replaceAll("%u", url), Language.get("ERROR_BROWSE_TITLE"), JOptionPane.WARNING_MESSAGE);
                }
            }
        }.start();
    }

    /**
   * Taken from http://www.rgagnon.com/javadetails/java-0064.html
   * Code is licensed under Creative Commons BY-NC-SA 2.5
   * @author Real Gagnon
   * @param in Source File
   * @param out Destination File
   * @throws java.io.IOException
   */
    public static void copyFile(File in, File out) throws IOException {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    /**
   * Unzips a ZIP file in the current working directory
   * @param zip The ZIP file
   * @throws java.io.FileNotFoundException
   * @throws java.io.IOException
   */
    public static void unzip(File zip) throws FileNotFoundException, IOException {
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zip));
        ZipEntry entry;
        while ((entry = zin.getNextEntry()) != null) {
            if (!(System.getProperty("os.name").indexOf("Windows") == -1 && (entry.getName().endsWith("exe") || entry.getName().endsWith("dll")))) {
                File outf = new File(entry.getName());
                System.out.println(outf.getAbsoluteFile());
                if (entry.isDirectory()) {
                    outf.mkdirs();
                } else {
                    outf.createNewFile();
                    OutputStream out = new FileOutputStream(outf);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = zin.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.close();
                }
            }
        }
    }

    /**
   * Checks whether a process is running or not.
   * Taken from http://www.rgagnon.com/javadetails/java-0593.html
   * Code is licensed under Creative Commons BY-NC-SA 2.5
   * @author Real Gagnon
   * @param process The case-sensitive process name (e.g. iTunes.exe)
   * @return True if the process is running
   */
    public static boolean isRunning(String process) {
        System.out.println("Checking whether process " + process + " is running...");
        boolean found = false;
        try {
            File file = File.createTempFile("processes", ".vbs");
            FileWriter fw = new java.io.FileWriter(file);
            String vbs = "Set WshShell = WScript.CreateObject(\"WScript.Shell\")\n" + "Set locator = CreateObject(\"WbemScripting.SWbemLocator\")\n" + "Set service = locator.ConnectServer()\n" + "Set processes = service.ExecQuery _\n" + " (\"select name from Win32_Process where name='" + process + "'\")\n" + "For Each process in processes\n" + "wscript.echo process.Name \n" + "Next\n" + "Set WSHShell = Nothing\n";
            fw.write(vbs);
            fw.close();
            Process p = Runtime.getRuntime().exec("cscript //NoLogo " + file.getPath());
            BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line;
            line = input.readLine();
            if (line != null) {
                if (line.equals(process)) {
                    found = true;
                }
            }
            System.out.println("Process " + ((found) ? "" : "not ") + "found!");
            input.close();
            p.destroy();
            file.delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return found;
    }
}
