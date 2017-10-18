package es.unav.informesgoogleanalytics;

import java.io.*;
import java.net.Proxy;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.*;

/**
 * Utils
 * 
 * @author eriknorvelle
 */
public class Utils {

    /**
     * Initialize use of proxies, if we're behind a firewall
     */
    public static void initProxies() throws Exception {
        try {
            System.setProperty("java.net.useSystemProxies", "true");
            String hostname = new String();
            String port = new String();
            String url = "http://www.yahoo.com/";
            boolean proxyFound = false;
            List<Proxy> l = ProxySelector.getDefault().select(new URI(url));
            for (Iterator<Proxy> iter = l.iterator(); iter.hasNext(); ) {
                Proxy proxy = iter.next();
                InetSocketAddress addr = (InetSocketAddress) proxy.address();
                if (addr == null) {
                } else {
                    hostname = addr.getHostName();
                    port = String.valueOf(addr.getPort());
                    proxyFound = true;
                }
            }
            if (proxyFound) {
                Properties systemProperties = System.getProperties();
                systemProperties.setProperty("http.proxyHost", hostname);
                systemProperties.setProperty("http.proxyPort", port);
                systemProperties.setProperty("https.proxyHost", hostname);
                systemProperties.setProperty("https.proxyPort", port);
            }
        } catch (Exception e) {
            throw new Exception("Couldn't set proxy", e);
        }
    }

    /**
     * Find the location of a given object in an Iterator.  Useful for finding the numeric 
     * position of an object in a sorted Map.
     * @param i The Iterator that contains the objects to be compared against
     * @param o The Object that is to be located
     * @return The position of the Object o in the Iterator, or -1 if not found.
     */
    public static int getPosInIterator(Iterator<?> i, Object o) {
        boolean found = false;
        int pos = 0;
        while (!found && i.hasNext()) {
            Object oc = i.next();
            if (oc == o) {
                found = true;
            } else {
                pos++;
            }
        }
        if (!found) {
            return -1;
        }
        return pos;
    }

    /**
     * Find the location of a given object in an Enumeration.  Useful for finding the numeric 
     * position of an object in a set of node children
     * @param i The Enumeration that contains the objects to be compared against
     * @param o The Object that is to be located
     * @return The position of the Object o in the Iterator, or -1 if not found.
     */
    public static int getPosInEnumeration(Enumeration<?> e, Object o) {
        boolean found = false;
        int pos = 0;
        while (!found && e.hasMoreElements()) {
            Object oc = e.nextElement();
            if (oc == o) {
                found = true;
            } else {
                pos++;
            }
        }
        if (!found) {
            return -1;
        }
        return pos;
    }

    /**
     * Given a numeric position, cycle through the given Iterator until the position
     * is reached, and return the Object found there.
     * 
     * @param i The Iterator to be cycled through.
     * @param pos The numeric position of the Object to be returned
     * @return The Object at the indicated position
     */
    public static Object getObjectAtIteratorPos(Iterator<?> i, int pos) throws ArrayIndexOutOfBoundsException {
        int currPos = 0;
        Object oc;
        while (i.hasNext()) {
            oc = i.next();
            if (currPos == pos) {
                return oc;
            } else {
                currPos++;
            }
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    /**
     * Get the extension of a file.
     * @param f The File object to extract the extension from
     * @return The extension (minus the period) in lower case.
     */
    public static String getExtension(java.io.File f) {
        if (f == null) {
            return null;
        }
        String s = f.getName();
        return getExtension(s);
    }

    /**
     * Get the extension of a file.
     * @param f The File object to extract the extension from
     * @return The extension (minus the period) in lower case.
     */
    public static String getExtension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');
        if (i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    /**
     * Get the extension of a file.
     * @param f The File object to extract the extension from
     * @return The extension (minus the period) in lower case.
     */
    public static String getDirectoryFromFilepath(String s) {
        String path = null;
        int i = s.lastIndexOf(File.separator);
        if (i > 0 && i < s.length() - 1) {
            path = s.substring(0, i + 1);
        }
        return path;
    }

    /**
     * Useful for situations where one needs to use a string, while being sure
     * that it isn't null.
     */
    public static String noNullString(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    /**
     * Produce a string version of a stacktrace.
     * @param e The Exception containing the stacktrace
     * @return The stacktrace as a String.
     */
    public static String getStacktraceAsString(Exception e) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        return result.toString();
    }

    /**
     * Returns a string with the date and time formatted for human consumption
     */
    public static String getDateTimeString() {
        DateFormat fDateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG);
        return fDateFormat.format(new Date());
    }

    /**
     * Return the MD5 digest of a given string.  In case of exception, fails by returning a copy of the string.
     */
    public static String getMD5(String s) throws NoSuchAlgorithmException {
        MessageDigest digester = MessageDigest.getInstance("MD5");
        byte[] bytes = s.getBytes();
        digester.update(bytes);
        return digester.digest().toString();
    }

    /**
     * Devolver una cadena localizada
     */
    public static String getMensajeLocalizado(String clave) {
        return java.util.ResourceBundle.getBundle("es/unav/informesgoogleanalytics/vista/Bundle").getString(clave);
    }
}
