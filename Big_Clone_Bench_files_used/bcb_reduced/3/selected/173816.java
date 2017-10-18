package com.simonepezzano.hshare;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.PlatformUI;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import com.simonepezzano.hshare.servlets.DFile;
import com.simonepezzano.hshare.servlets.GetResource;
import com.simonepezzano.hshare.servlets.ListFiles;
import com.simonepezzano.hshare.servlets.Templater;

/**
 * Static, useful methods
 * @author Simone Pezzano
 *
 */
public class Statics {

    private static Server server = null;

    private static DecimalFormat twoDec = new DecimalFormat("#0.00");

    public static final String DIRECTORY = "dir";

    public static final String FILE = "file";

    /**
	 * @return the Jetty server instance
	 */
    public static Server getServerInstance() {
        if (server == null) {
            server = new Server(Integer.valueOf(Conf.getInstance().getPort()));
            Context root = new Context(server, "/", Context.SESSIONS);
            root.addServlet(new ServletHolder(new ListFiles()), "/*");
            root.addServlet(new ServletHolder(new DFile()), "/Download/*");
            root.addServlet(new ServletHolder(new GetResource()), "/Resources/*");
            try {
                fillBaseTemplates();
            } catch (IOException e) {
                HLog.iologger.fatal("Cannot copy resources from jar to resources directory", e);
            }
        }
        return server;
    }

    /**
	 * Calculates the MD5 of the given string
	 * @param input the string to be encoded
	 * @return the encoded string
	 * @throws Exception
	 */
    public static String MD5(String input) throws Exception {
        MessageDigest m = MessageDigest.getInstance("MD5");
        m.update(input.getBytes(), 0, input.length());
        input = new BigInteger(1, m.digest()).toString(16);
        if (input.length() == 31) input = "0" + input;
        return input;
    }

    /**
	 * Returns the size of a file, in a readable format
	 * @param file the file we want the size of
	 * @return the size, in a readable format
	 */
    public static String getSize(File file) {
        long len = file.length();
        double kb = len / 1024.0;
        double mb = kb / 1024.0;
        if (mb > 0.5) return twoDec.format(mb).toString() + " MB";
        return twoDec.format(kb).toString() + " KB";
    }

    /**
	 * Checks if a directory exists, and creates it if it doesn't
	 * @param dir the file descriptor of a directory 
	 * @return true, if the directory has been created
	 */
    public static boolean checkDir(File dir) {
        if (!dir.exists()) return dir.mkdir();
        return false;
    }

    /**
	 * Checks if the resources directory exists, and creates it if it doesn't
	 * @return true, if the directory has been created
	 */
    public static boolean checkResDir() {
        return checkDir(Conf.getInstance().getResDir());
    }

    /**
	 * Checks if the templates directory exists, and creates it if it doesn't
	 * @return true, if the directory has been created
	 */
    public static boolean checkDirTemplatesDir() {
        return checkDir(Conf.getInstance().getDirTemplates());
    }

    /**
	 * @return true, if templates have been properly installed
	 */
    public static boolean areTemplatesReady() {
        return Templater.headFile().exists();
    }

    /**
	 * Copies all templates files from jar to templates directory
	 * @throws IOException
	 */
    public static void fillBaseTemplates() throws IOException {
        if (areTemplatesReady()) return;
        copyJarFileToFile(Templater.headFile(), "/w_res/head.htm.templ");
        copyJarFileToFile(Templater.bottomFile(), "/w_res/bottom.htm.templ");
        copyJarFileToFile(Templater.dirFile(), "/w_res/dir.htm.templ");
        copyJarFileToFile(Templater.fileFile(), "/w_res/file.htm.templ");
        copyJarFileToFile(Conf.getInstance().styleFile(), "/w_res/resources/style.css");
        copyJarFileToFile(Conf.getInstance().logoFile(), "/w_res/resources/logo.png");
    }

    /**
	 * @param filename a filename
	 * @return the extension of the given filename
	 */
    public static String getExt(String filename) {
        int index = filename.lastIndexOf('.');
        if (index > 0) return filename.substring(index + 1);
        return "";
    }

    /**
	 * Given a descriptor type, it checks if an HShare descriptor belongs to that time
	 * @param el a HShare descriptor
	 * @param type a descriptor type
	 * @return true, if the HShare descriptor belongs to the give type
	 */
    public static boolean isA(Element el, String type) {
        return el.getName().equals(type);
    }

    /**
	 * @param el an HShare descriptor
	 * @return true if the given descriptor is a File descriptor
	 */
    public static boolean isAFile(Element el) {
        return isA(el, FILE);
    }

    /**
	 * @param el an HShsare descriptor
	 * @return true if the given descriptor is a Directory descriptor
	 */
    public static boolean isADir(Element el) {
        return isA(el, DIRECTORY);
    }

    /**
	 * Fetches an attribute value from an Element and returns a default, if the attribute doesn't exist 
	 * @param el an Element
	 * @param attrName the attribute name
	 * @param def the default
	 * @return the attribute value or the default
	 */
    public static String attributeValue(Element el, String attrName, String def) {
        Attribute a = el.attribute(attrName);
        if (a == null) return def; else return a.getStringValue();
    }

    /**
	 * Fetches an attribute value from an Element and returns an empty string if the attribute doesn't exist
	 * @param el an Element
	 * @param attrName the attribute name
	 * @return the attribute value or an empty string
	 */
    public static String attributeValue(Element el, String attrName) {
        return attributeValue(el, attrName, "");
    }

    /**
	 * Converts a string to UTF-8
	 * @param string a string
	 * @return the string converted to UTF-8
	 */
    public static String toUTF8(String string) {
        try {
            return URLEncoder.encode(string, "utf-8").replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }

    /**
	 * Creates an URI from a path and a filename
	 * @param path a path
	 * @param filename a filename
	 * @return an URI
	 */
    public static URI toURI(String path, String filename) {
        return new File(path).toURI().resolve(toUTF8(filename));
    }

    /**
	 * Prints an InputStream to an OutputStream
	 * @param is an inputStream
	 * @param os an outputStream
	 * @throws IOException
	 */
    public static void printToStream(InputStream is, OutputStream os) throws IOException {
        byte[] buff = new byte[4096];
        int len = 0;
        while ((len = is.read(buff)) != -1) os.write(buff, 0, len);
        is.close();
    }

    /**
	 * Writes some text to file
	 * @param dest destination file
	 * @param text the text to be written
	 * @throws IOException
	 */
    public static void printToFile(File dest, String text) throws IOException {
        FileWriter fw = new FileWriter(dest);
        fw.write(text);
        fw.close();
    }

    /**
	 * Writes some bytes to file
	 * @param dest destination file
	 * @param data bytes to be written
	 * @throws IOException
	 */
    public static void printToFile(File dest, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(dest);
        fos.write(data);
    }

    /**
	 * Gets all computer local IP addresses
	 * @return a String with all IP addresses in it
	 * @throws SocketException
	 */
    public static String getLocalIP() throws SocketException {
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        String res = "";
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            Enumeration<InetAddress> addrs = ni.getInetAddresses();
            while (addrs.hasMoreElements()) {
                InetAddress address = addrs.nextElement();
                if (address instanceof Inet4Address) res += " " + address.getHostAddress();
            }
            if (interfaces.hasMoreElements()) res += " -";
        }
        return res;
    }

    /**
	 * Fetches the remote IP
	 * @return the remote IP
	 * @throws IOException
	 */
    public static String getRemoteIP() throws IOException {
        URL url = new URL(Conf.getInstance().getExternalIpServiceURL());
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        HLog.netlogger.debug("fetching remote IP");
        String res = new BufferedReader(new InputStreamReader(conn.getInputStream())).readLine();
        conn.disconnect();
        return res;
    }

    /**
	 * @return the coordinates of the monitor center
	 */
    public static Point getAppCenter() {
        return new Point(PlatformUI.getWorkbench().getDisplay().getClientArea().width / 2, PlatformUI.getWorkbench().getDisplay().getClientArea().height / 2);
    }

    /**
	 * Calculates the origin of a window, centering the screen
	 * @param width
	 * @param height
	 * @return the coordinates of the window origin
	 */
    public static Point getOriginForCenter(int width, int height) {
        Point center = getAppCenter();
        return new Point(center.x - (width / 2), center.y - (height / 2));
    }

    /**
	 * Gets the text inside a resource located in the jar
	 * @param s the resource name
	 * @return the text in the resource
	 */
    public static String getStringContentFromJar(String s) {
        String thisLine = null;
        String res = "";
        try {
            InputStream is = Statics.class.getResourceAsStream(s);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            while ((thisLine = br.readLine()) != null) res += thisLine + '\n';
        } catch (Exception e) {
            return "";
        }
        return res;
    }

    /**
	 * Copies a file from the jar to an external location
	 * @param dest file descriptor of the external location
	 * @param filename the filename inside the jar
	 * @throws IOException
	 */
    public static void copyJarFileToFile(File dest, String filename) throws IOException {
        CopyThread ct = new CopyThread(dest, filename);
        ct.start();
    }

    /**
	 * Gets the bytes inside a resource located in the jar
	 * @param s the resource name
	 * @return the bytes in the resource
	 * @throws IOException
	 */
    public static byte[] getBytesContentFromJar(String s) throws IOException {
        InputStream is = Statics.class.getResourceAsStream(s);
        ByteArrayOutputStream bab = new ByteArrayOutputStream();
        byte[] buff = new byte[4096];
        int len = 0;
        while ((len = is.read(buff)) != -1) bab.write(buff, 0, len);
        return bab.toByteArray();
    }

    /**
	 * Applies an attribute to an element
	 * @param element an element
	 * @param attributeName the attribute name
	 * @param value the value
	 */
    public static void manageAttribute(Element element, String attributeName, String value) {
        Attribute attr = element.attribute(attributeName);
        if (value == null || value.length() == 0) {
            if (attr != null) element.remove(attr);
        } else if (attr == null) element.addAttribute(attributeName, value); else attr.setText(value);
    }

    /**
	 * Given a comma separated list of users, it splits it into list tokens
	 * @param users a comma separated list of users
	 * @return a linkedlist of users
	 */
    public static LinkedList<String> splitUsers(String users) {
        LinkedList<String> usrs = new LinkedList<String>();
        StringTokenizer st = new StringTokenizer(users, ",");
        while (st.hasMoreTokens()) {
            usrs.add(st.nextToken().trim());
        }
        return usrs;
    }

    /**
	 * Given an HShare directory descriptor, a username and a password, it determines if the user
	 * is allowed to access the directory
	 * @param dir the HShare directory descriptor
	 * @param username the username
	 * @param password the password
	 * @return true if the user is allowed to access the directory
	 */
    public static boolean verifyUser(Element dir, String username, String password) {
        try {
            Attribute users = dir.attribute("users");
            if (users == null || users.getStringValue().length() == 0) return true;
            LinkedList<String> usrs = splitUsers(users.getStringValue().trim());
            if (usrs.size() == 0 || !usrs.contains(username)) return false;
            Document udoc = new HUsers().getDocument();
            Element user = (Element) udoc.selectSingleNode("//user[@username='" + username + "']");
            if (user == null) return false;
            return password.equals(user.attributeValue("password"));
        } catch (Exception e) {
            HLog.doclogger.error("Could not read users file properly", e);
            return false;
        }
    }
}

/**
 * Thread for copying a resource from jar to a destination
 * @author Simone Pezzano
 *
 */
class CopyThread extends Thread {

    private File dest;

    private String filename;

    /**
	 * Default constructor
	 * @param dest a file descriptor to the destination
	 * @param filename the name of the resource
	 */
    public CopyThread(File dest, String filename) {
        this.dest = dest;
        this.filename = filename;
    }

    public void run() {
        try {
            InputStream is = Statics.class.getResourceAsStream(filename);
            FileOutputStream fos = new FileOutputStream(dest);
            Statics.printToStream(is, fos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
