package com.belmont.backup;

import java.math.BigInteger;
import java.io.*;
import java.net.*;
import java.util.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.marimba.intf.util.IConfig;
import com.marimba.intf.application.IApplicationContext;

public class Utils implements IBackupConstants {

    static String hostname;

    static String hostip;

    static InetAddress localhost;

    static PrintStream logStream;

    static File logFile;

    static Hashtable<String, String> cancelRequests = new Hashtable<String, String>();

    static final String ALL_CANCEL = "*all*";

    public static void clearCancel(String label) {
        synchronized (cancelRequests) {
            if (label == null) {
                cancelRequests.clear();
            } else {
                cancelRequests.remove(label);
                if (cancelRequests.get(ALL_CANCEL) != null) {
                    cancelRequests.remove(ALL_CANCEL);
                }
            }
        }
    }

    public static void requestCancel(String label) {
        synchronized (cancelRequests) {
            if (label == null) {
                cancelRequests.put(ALL_CANCEL, ALL_CANCEL);
            } else {
                cancelRequests.put(label, label);
            }
        }
    }

    public static void checkCancel() throws InterruptedException {
        checkCancel(null);
    }

    public static void checkCancel(String label) throws InterruptedException {
        synchronized (cancelRequests) {
            if (label == null) {
                if (!cancelRequests.isEmpty()) {
                    throw new InterruptedException("Operation cancelled");
                }
            } else if (cancelRequests.get(label) != null || cancelRequests.get(ALL_CANCEL) != null) {
                throw new InterruptedException("Operation cancelled");
            }
        }
    }

    public static File getLogFile() {
        return logFile;
    }

    /**
     * Returns the next time the given schedule will fire relative to the current time.
     * schedule is specified as:
     *   onetime:<long> -- true if current time == <long>
     *   hourly:<N> -- true every N hours
     *   daily:HH:MM -- true each day at the given time
     *   weekly:{Mon,Tue,Wed,Thu,Fri,Sat,Sun}+;HH:MM
     */
    public static long nextScheduleTime(String schedule) {
        int idx = schedule.indexOf(':');
        if (idx == -1) {
            Utils.log(LOG_ERROR, "Bad schedule string: " + schedule);
            return 0;
        }
        String type = schedule.substring(0, idx).toLowerCase();
        schedule = schedule.substring(idx + 1);
        if ("onetime".equals(type)) {
            try {
                return Long.parseLong(schedule);
            } catch (NumberFormatException ex) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule, ex);
                return 0;
            }
        } else if ("hourly".equals(type)) {
            try {
                int h = Integer.parseInt(schedule);
                long tm = System.currentTimeMillis();
                return tm + ((long) h * 60 * 60 * 1000);
            } catch (NumberFormatException ex) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule, ex);
                return 0;
            }
        } else if ("daily".equals(type)) {
            GregorianCalendar cal = new GregorianCalendar();
            int ch = cal.get(Calendar.HOUR_OF_DAY);
            int cm = cal.get(Calendar.MINUTE);
            idx = schedule.indexOf(':');
            if (idx == -1) {
                return 0;
            }
            try {
                int h = Integer.parseInt(schedule.substring(0, idx));
                int m = Integer.parseInt(schedule.substring(idx + 1));
                return calcWeeklySched(cal, cal.get(Calendar.DAY_OF_WEEK), h, m, 1);
            } catch (NumberFormatException ex) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule, ex);
                return 0;
            }
        } else if ("weekly".equals(type)) {
            GregorianCalendar cal = new GregorianCalendar();
            int days[] = new int[8];
            idx = schedule.indexOf(';');
            if (idx == 0 || idx == -1) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule);
                return 0;
            }
            String ds = schedule.substring(0, idx).trim();
            String hours = schedule.substring(idx + 1);
            if (ds.length() == 0) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule);
                return 0;
            }
            int schedDay = convertDayString(ds);
            idx = hours.indexOf(':');
            if (idx == -1) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule);
                return 0;
            }
            try {
                int h = Integer.parseInt(hours.substring(0, idx));
                int m = Integer.parseInt(hours.substring(idx + 1));
                return calcWeeklySched(cal, schedDay, h, m, 7);
            } catch (NumberFormatException ex) {
                Utils.log(LOG_ERROR, "Bad schedule string: " + schedule, ex);
                return 0;
            }
        } else {
            Utils.log(LOG_ERROR, "Unknown schedule type: " + type);
            return 0;
        }
    }

    static long calcWeeklySched(GregorianCalendar c, int day, int hour, int min, int offset) {
        int today = c.get(Calendar.DAY_OF_WEEK);
        int th = c.get(Calendar.HOUR_OF_DAY);
        int tm = c.get(Calendar.MINUTE);
        int ts = c.get(Calendar.SECOND);
        int dayOffset = day - today;
        int hOffset = hour - th;
        int mOffset = min - tm;
        if (dayOffset < 0) {
            dayOffset += offset;
        } else if ((dayOffset == 0) && (hOffset < 0)) {
            dayOffset = offset;
        } else if ((hOffset == 0) && (mOffset < 0)) {
            dayOffset = offset;
        } else if ((mOffset == 0) && (ts > 0)) {
            dayOffset = offset;
        }
        if (dayOffset > 0) {
            c.add(Calendar.DAY_OF_WEEK, dayOffset);
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.set(Calendar.MINUTE, min);
        } else {
            c.add(Calendar.HOUR_OF_DAY, hOffset);
            c.add(Calendar.MINUTE, mOffset);
        }
        c.set(Calendar.SECOND, 0);
        return c.getTimeInMillis();
    }

    public static String expandVariables(String stmt, Map<String, String> props, boolean useEnv) {
        StringBuffer sb = new StringBuffer();
        do {
            int idx = stmt.indexOf('$');
            if (idx == -1) {
                sb.append(stmt);
                return sb.toString();
            }
            int ide = stmt.indexOf('$', idx + 1);
            String key = stmt.substring(idx + 1, ide);
            String var = null;
            if (props != null) {
                var = props.get(key);
            } else {
                var = System.getProperty(key);
            }
            if (useEnv && var == null) {
                var = System.getenv(key);
            }
            sb.append(stmt.substring(0, idx));
            if (var != null) {
                sb.append(var);
            } else {
                sb.append("$" + key + "$");
            }
            stmt = stmt.substring(ide + 1);
        } while (stmt.length() > 0);
        return null;
    }

    static int convertDayString(String d) {
        if ("Mon".equals(d)) {
            return Calendar.MONDAY;
        } else if ("Tue".equals(d)) {
            return Calendar.TUESDAY;
        } else if ("Wed".equals(d)) {
            return Calendar.WEDNESDAY;
        } else if ("Thu".equals(d)) {
            return Calendar.THURSDAY;
        } else if ("Fri".equals(d)) {
            return Calendar.FRIDAY;
        } else if ("Sat".equals(d)) {
            return Calendar.SATURDAY;
        } else if ("Sun".equals(d)) {
            return Calendar.SUNDAY;
        } else {
            return 0;
        }
    }

    public static PrintStream openLog(File logdir, String logName) {
        if (logStream != null) {
            return logStream;
        }
        if (logdir == null || logName == null) {
            return logStream;
        }
        logFile = new File(logdir, logName);
        if (!logdir.isDirectory()) {
            logdir.mkdirs();
        }
        try {
            logStream = new PrintStream(new FileOutputStream(logFile, true));
        } catch (IOException ex) {
            logdir = new File("/var/tmp/log");
            logdir.mkdirs();
            logFile = new File(logdir, logName);
            try {
                logStream = new PrintStream(new FileOutputStream(logFile, true));
            } catch (IOException e) {
                logStream = System.err;
                log(LOG_ERROR, "Couldn't create log file: " + logFile.toString());
            }
        }
        return logStream;
    }

    public static void flushLog() {
        if (logStream != null) {
            logStream.flush();
        }
    }

    public static void closeLog() {
        if (logStream != null && logStream != System.err) {
            logStream.close();
        }
    }

    public static void log(int type, String message) {
        log(type, message, null);
    }

    public static void log(int type, String message, Throwable e) {
        PrintStream out = (logStream == null) ? System.err : logStream;
        Date d = new Date(System.currentTimeMillis());
        String typeString = "";
        switch(type) {
            case LOG_INFO:
                typeString = "INFO";
                break;
            case LOG_WARNING:
                typeString = "WARNING";
                break;
            case LOG_ERROR:
                typeString = "ERROR";
                break;
            default:
                typeString = "???";
                break;
        }
        out.format("%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS:%1$tL: [%2$s] %3$s", d, typeString, message);
        if (e != null) {
            e.printStackTrace(out);
        }
        out.format("%n");
    }

    public static String getHostName() {
        if (hostname == null) {
            if (localhost == null) {
                try {
                    localhost = InetAddress.getLocalHost();
                } catch (UnknownHostException ex) {
                }
            }
            if (localhost == null) {
                hostname = "unknown";
            } else {
                hostname = localhost.getHostName();
            }
        }
        return hostname;
    }

    public static String getHostIP() {
        if (hostip == null) {
            if (localhost == null) {
                try {
                    localhost = InetAddress.getLocalHost();
                } catch (UnknownHostException ex) {
                }
            }
            if (localhost == null) {
                return "0.0.0.0";
            } else {
                hostip = localhost.getHostAddress();
                return hostip;
            }
        }
        return hostip;
    }

    public static String toHexString(byte bytes[]) {
        if (bytes == null) {
            return null;
        }
        StringBuffer sb = new StringBuffer();
        for (int iter = 0; iter < bytes.length; iter++) {
            byte high = (byte) ((bytes[iter] & 0xf0) >> 4);
            byte low = (byte) (bytes[iter] & 0x0f);
            sb.append(nibble2char(high));
            sb.append(nibble2char(low));
        }
        return sb.toString();
    }

    private static char nibble2char(byte b) {
        byte nibble = (byte) (b & 0x0f);
        if (nibble < 10) {
            return (char) ('0' + nibble);
        }
        return (char) ('a' + nibble - 10);
    }

    public static String formatDigest(byte digest[]) {
        String dg = toHexString(digest);
        return dg;
    }

    public static void checkInterrupt() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Operation aborted.");
        }
    }

    public static boolean delete(final File f) {
        if (f.isDirectory()) {
            final String files[] = f.list();
            boolean ok = true;
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    ok &= Utils.delete(new File(f, files[i]));
                }
            }
            return f.delete() && ok;
        } else {
            return f.delete();
        }
    }

    public static boolean matchFilter(String s, String filters[]) {
        if (filters == null || s == null) {
            return false;
        }
        for (int i = 0; i < filters.length; i++) {
            if (s.equals(filters[i]) || s.endsWith(filters[i])) {
                return true;
            }
        }
        return false;
    }

    public static MessageDigest getDigest(String algorithm) throws IOException {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(algorithm + " not found");
        }
        return md;
    }

    public static String checksum(File file, MessageDigest md) throws InterruptedException {
        try {
            md.reset();
            FileInputStream in = new FileInputStream(file);
            byte buf[] = BufferPool.getInstance().get(1024);
            int len;
            try {
                while ((len = in.read(buf)) != -1) {
                    md.update(buf, 0, len);
                    Utils.checkCancel();
                }
                byte digestBytes[] = md.digest();
                return Utils.formatDigest(digestBytes);
            } finally {
                BufferPool.getInstance().put(buf);
                in.close();
            }
        } catch (IOException ex) {
            Utils.log(LOG_ERROR, "Error in checksum", ex);
        }
        return null;
    }

    public static String formatSize(long bytes) {
        try {
            long size = bytes;
            if (size > 1073741824) {
                String str = new Float((size + 52428.8) / 1073741824F).toString();
                return str.substring(0, str.indexOf(".") + 2) + " GB";
            } else if (size >= 1048576) {
                String str = new Float((size + 52428.8) / 1048576F).toString();
                return str.substring(0, str.indexOf(".") + 2) + " MB";
            } else if (size == 0) {
                return "0";
            } else {
                String str = new Float((size + 51.2) / 1024F).toString();
                return str.substring(0, str.indexOf(".") + 2) + " KB";
            }
        } catch (Exception e) {
            return "0 KB";
        }
    }

    public static void installChannelDLLs(IConfig tunerConfig, IApplicationContext context) {
        String wsdir = tunerConfig.getProperty("runtime.workspace.dir");
        String tdir = tunerConfig.getProperty("marimba.tuner.install.dir");
        boolean installed = false;
        if (wsdir != null) {
            File dest = new File(wsdir + File.separator + "ext" + File.separator + "sqlite3.dll");
            installed = copyChannelFile("lib/sqlite3.dll", dest, context);
        }
        if (tdir != null) {
            File dest = new File(tdir + File.separator + "etc" + File.separator + "sqlite3.dll");
            installed = copyChannelFile("lib/sqlite3.dll", dest, context);
        }
        if (!installed) {
            Utils.log(LOG_ERROR, "Failed to install DLLs.");
        }
    }

    public static boolean copyChannelFile(String chfile, File dest, IApplicationContext context) {
        File parent = new File(dest.getParent());
        String l[] = parent.list();
        boolean found = false;
        int idx = chfile.lastIndexOf('/');
        String name = chfile;
        if (idx != -1) {
            name = chfile.substring(idx + 1);
        }
        if (l != null) {
            for (int i = 0; i < l.length; i++) {
                if (name.equals(l[i])) {
                    found = true;
                    break;
                }
            }
        }
        if (found) {
            return true;
        }
        parent.mkdirs();
        try {
            InputStream in = context.getInputStream(chfile);
            try {
                OutputStream out = new FileOutputStream(dest);
                try {
                    copy(in, out);
                    return true;
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            Utils.log(LOG_ERROR, "Failed to copy sqlite3.dll to " + dest.toString(), ex);
            return false;
        }
    }

    public static Vector<String> vectorFromString(String l, char delimiter) {
        if (l == null) {
            return null;
        }
        l = l.trim();
        int idx;
        Vector<String> r = null;
        while ((idx = l.indexOf(delimiter)) != -1) {
            if (r == null) {
                r = new Vector<String>();
            }
            r.addElement(l.substring(0, idx));
            l = l.substring(idx + 1);
        }
        if (l.length() > 0) {
            if (r == null) {
                r = new Vector<String>();
            }
            r.addElement(l);
        }
        return r;
    }

    public static String stringFromVector(Vector<String> v, char delimiter) {
        if (v == null || v.size() == 0) {
            return null;
        }
        int l = v.size();
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < l - 1; i++) {
            sb.append(v.elementAt(i) + Character.toString(delimiter));
        }
        sb.append(v.elementAt(l - 1));
        return sb.toString();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte buf[] = BufferPool.getInstance().get(4096);
        int n;
        try {
            while ((n = in.read(buf, 0, buf.length)) > 0) {
                out.write(buf, 0, n);
            }
        } finally {
            BufferPool.getInstance().put(buf);
        }
    }

    public static void main(String args[]) {
        long tm = Utils.nextScheduleTime(args[0]);
        System.out.println("NEXT: " + new Date(tm).toString());
    }
}
