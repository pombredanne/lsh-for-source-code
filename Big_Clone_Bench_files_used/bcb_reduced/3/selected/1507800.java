package org.xnap.commons.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Vector;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Tammo van Lessen
 * @author Steffen Pingel
 */
public class UncaughtExceptionManager implements UncaughtExceptionListener {

    public static String[] DEFAULT_MANGLE_PREFIXES = { "java.lang.IndexOutOfBoundsException", "java.lang.ArrayIndexOutOfBoundsException", "java.lang.RuntimeException" };

    private static Log logger = LogFactory.getLog(UncaughtExceptionManager.class);

    private static UncaughtExceptionListener defaultHandler = new NullExceptionHandler();

    private static String asHex(byte hash[]) {
        StringBuilder buf = new StringBuilder(hash.length * 2);
        int i;
        for (i = 0; i < hash.length; i++) {
            if (((int) hash[i] & 0xff) < 0x10) buf.append("0");
            buf.append(Long.toString((int) hash[i] & 0xff, 16));
        }
        return buf.toString();
    }

    public static String removeExceptionDescription(String trace, String prefix) {
        if (trace.startsWith(prefix + ": ")) {
            int i = trace.indexOf("\n");
            if (i != -1) {
                return prefix + trace.substring(i);
            }
        }
        return trace;
    }

    public static UncaughtExceptionListener getDefaultHandler() {
        return defaultHandler;
    }

    public static void setDefaultHandler(UncaughtExceptionListener handler) {
        if (handler == null) {
            defaultHandler = new NullExceptionHandler();
        } else {
            defaultHandler = handler;
        }
    }

    private HashSet<String> blacklist = new HashSet<String>();

    private File blacklistFile;

    private Vector<UncaughtExceptionListener> listeners = new Vector<UncaughtExceptionListener>();

    private String[] manglePrefixes;

    public UncaughtExceptionManager(File blacklistFile, String[] manglePrefixes) {
        this.blacklistFile = blacklistFile;
        this.manglePrefixes = manglePrefixes;
        readBlackList();
        setDefaultHandler(this);
    }

    public UncaughtExceptionManager(File blacklistFile) {
        this(blacklistFile, DEFAULT_MANGLE_PREFIXES);
    }

    public UncaughtExceptionManager() {
        this(null, DEFAULT_MANGLE_PREFIXES);
    }

    public void addExceptionListener(UncaughtExceptionListener l) {
        listeners.add(l);
    }

    public synchronized void addToBlacklist(Throwable e) {
        blacklist.add(buildMD5Hash(e));
        writeBlacklist();
    }

    private String buildMD5Hash(String s) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
            return asHex(md.digest(s.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace(System.err);
        }
        return "";
    }

    private String buildMD5Hash(Throwable e) {
        return buildMD5Hash(toString(e));
    }

    private String encode(Object o) {
        try {
            if (o != null) {
                return URLEncoder.encode(o.toString(), "UTF-8");
            }
        } catch (UnsupportedEncodingException e) {
            logger.error("UTF-8 encoding not supported", e);
        }
        return "";
    }

    private boolean isBlacklisted(Throwable e) {
        return blacklist.contains(buildMD5Hash(e));
    }

    /**
	 * 
	 */
    @SuppressWarnings("unchecked")
    private void readBlackList() {
        if (blacklistFile == null) {
            return;
        }
        if (!blacklistFile.exists()) {
            return;
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(blacklistFile);
            ObjectInputStream p = new ObjectInputStream(in);
            blacklist = (HashSet<String>) p.readObject();
        } catch (Throwable e) {
            logger.debug("Could not read exception blacklist file: " + blacklistFile.getAbsolutePath(), e);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void removeExceptionListener(UncaughtExceptionListener l) {
        listeners.remove(l);
    }

    /**
	 * @param destination
	 * @param thread
	 * @param throwable
	 * @param version
	 * @param plugin
	 * @throws IOException
	 */
    public void sendProblemReport(URL destination, Thread thread, Throwable throwable, String version, String plugin) throws IOException {
        Properties p = System.getProperties();
        StringBuffer report = new StringBuffer();
        report.append("version=" + encode(version));
        report.append("&plugin=" + encode(plugin));
        report.append("&locale=" + encode(Locale.getDefault()));
        report.append("&os_name=" + encode(p.get("os.name")));
        report.append("&os_version=" + encode(p.get("os.version")));
        report.append("&os_arch=" + encode(p.get("os.arch")));
        report.append("&java_vendor=" + encode(p.get("java.vendor")));
        report.append("&java_version=" + encode(p.get("java.version")));
        report.append("&stacktrace=" + encode(toString(throwable)));
        String hash = buildMD5Hash(report.toString());
        String problemHash = buildMD5Hash(throwable);
        report.append("&hash=" + encode(hash));
        report.append("&problem_hash=" + encode(problemHash));
        HttpURLConnection conn = (HttpURLConnection) destination.openConnection();
        try {
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setAllowUserInteraction(false);
            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.println(report);
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            try {
                String s;
                while ((s = in.readLine()) != null) {
                    logger.debug("Received: " + s + "\n");
                }
            } finally {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        } finally {
            conn.disconnect();
        }
    }

    /**
	 * Returns the stacktrace of <code>e</code> as a String.
	 * 
	 * @param e the exception
	 */
    public String toString(Throwable e) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter printWriter = new PrintWriter(baos);
        e.printStackTrace(printWriter);
        printWriter.close();
        String trace = baos.toString();
        for (int i = 0; i < manglePrefixes.length; i++) {
            trace = removeExceptionDescription(trace, manglePrefixes[i]);
        }
        return trace;
    }

    /**
	 * Handles e thrown by t. Notifies all listeners in case e is not
	 * blacklisted.
	 */
    public synchronized void uncaughtException(Thread t, Throwable e) {
        if (isBlacklisted(e)) {
            logger.debug("Blacklisted uncaught exception occured!", e);
            return;
        }
        UncaughtExceptionListener[] l = listeners.toArray(new UncaughtExceptionListener[0]);
        if (l != null && l.length > 0) {
            for (int i = l.length - 1; i >= 0; i--) {
                l[i].uncaughtException(t, e);
            }
        } else {
            e.printStackTrace(System.err);
        }
    }

    /**
	 * 
	 */
    private void writeBlacklist() {
        if (blacklistFile == null) {
            return;
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(blacklistFile);
            ObjectOutputStream p = new ObjectOutputStream(out);
            p.writeObject(blacklist);
        } catch (IOException e) {
            logger.debug("Could not write exception blacklist file: " + blacklistFile.getAbsolutePath(), e);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
            }
        }
    }

    private static class NullExceptionHandler implements UncaughtExceptionListener {

        public void uncaughtException(Thread t, Throwable e) {
        }
    }
}
