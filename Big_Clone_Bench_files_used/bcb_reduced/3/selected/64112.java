package com.keppardo.dyndns;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import sun.security.provider.SHA2;

public class IPChecker {

    static String GET_IP = "http://ip.dnsexit.com";

    static String URL_DNS = "http://freedns.afraid.org/api/?action=getdyndns&sha=";

    private static String[] startParams = new String[0];

    static FileOutputStream fout = null;

    private static PrintStream ps;

    static {
        try {
            fout = new FileOutputStream("ipchecker.log", true);
            ps = new PrintStream(fout);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void setParams(String[] params) {
        IPChecker.startParams = params;
    }

    public static void check() throws IOException, GeneralSecurityException {
        System.out.println("Check ip ....");
        Date date = new Date(System.currentTimeMillis());
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, Locale.ITALY);
        System.out.println("Date/Time Check: " + df.format(date));
        if (startParams.length < 3 && startParams.length == 0) {
            usage();
            return;
        }
        Hashtable<String, String> params = new Hashtable<String, String>();
        for (int i = 0; i < startParams.length; i++) {
            params.put(startParams[i], startParams[++i]);
        }
        if (params.get("-c") == null) {
            usage();
            return;
        }
        InputStream in = new FileInputStream(params.get("-c"));
        Properties p = new Properties();
        p.load(in);
        String sha = p.getProperty("sha-hash");
        if (sha == null) {
            if (params.get("-u") == null || params.get("-p") == null) {
                usage();
                return;
            }
            String user = params.get("-u");
            String pwd = params.get("-p");
            sha = user + "|" + pwd;
            byte[] b = MessageDigest.getInstance("SHA-1").digest(sha.getBytes());
            sha = hash(b);
            p.setProperty("sha-hash", sha);
            p.store(new FileOutputStream(params.get("-c"), true), "");
        }
        String oldIp = p.getProperty("ip");
        System.out.println("Ip Setted now: " + oldIp);
        String actualIp = getActualIp();
        System.out.println("NEW IP: " + actualIp);
        if (!actualIp.equals(oldIp)) {
            URL_DNS += p.getProperty("sha-hash");
            URL url = new URL(URL_DNS);
            URLConnection urlConnection = url.openConnection();
            InputStream inStream = urlConnection.getInputStream();
            StringBuffer sb = streamToStringBuffer(inStream);
            StringTokenizer st = new StringTokenizer(sb.toString(), "|");
            Vector<String> v = new Vector<String>();
            while (st.hasMoreTokens()) {
                String token = st.nextToken();
                v.add(token);
            }
            String url4change = v.get(2);
            url = new URL(url4change);
            urlConnection = url.openConnection();
            inStream = urlConnection.getInputStream();
            System.out.println(streamToStringBuffer(inStream));
        }
        p.setProperty("ip", actualIp);
        p.store(new FileOutputStream(params.get("-c")), "");
    }

    public static String getActualIp() throws IOException {
        URL url = new URL(GET_IP);
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();
        byte[] b = new byte[512];
        int ch = 0;
        StringBuffer sb = new StringBuffer();
        while ((ch = in.read(b)) >= 0) {
            sb.append(new String(b, 0, ch));
        }
        return sb.toString();
    }

    private static StringBuffer streamToStringBuffer(InputStream inStream) throws IOException {
        byte[] buf = new byte[1024];
        int ch = 0;
        StringBuffer sb = new StringBuffer();
        while ((ch = inStream.read(buf)) >= 0) {
            sb.append(new String(buf, 0, ch));
        }
        inStream.close();
        return sb;
    }

    private static String hash(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            byte c = b[i];
            sb.append(toHex(c));
        }
        return sb.toString();
    }

    private static String toHex(byte c) {
        return "00".substring(Integer.toHexString(c & 0xff).length()) + Integer.toHexString(c & 0xff);
    }

    private static void usage() {
        System.out.println("Usage: ");
        System.out.println("If this is the first time you launch ");
        System.out.println("\t\t\t-c /path/to/configfile -u username -p password");
        System.out.println("otherwise");
        System.out.println("\t\t\t-c /path/to/configfile");
    }
}
