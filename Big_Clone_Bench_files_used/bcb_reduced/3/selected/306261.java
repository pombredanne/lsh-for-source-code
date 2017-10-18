package malgnsoft.util;

import java.io.*;
import java.nio.channels.*;
import java.util.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.security.MessageDigest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.mail.*;
import javax.mail.internet.*;
import javax.activation.FileDataSource;
import javax.activation.DataHandler;
import javax.naming.*;
import javax.naming.directory.*;
import malgnsoft.db.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.swing.ImageIcon;

public class Malgn {

    public String secretId = "sdhflsdhflsdxxx";

    public String cookieDomain = null;

    public static String logDir = Config.getLogDir();

    public static String dataDir = Config.getDataDir();

    public static String webUrl = Config.getWebUrl();

    public static String dataUrl = Config.getDataUrl();

    public static String encoding = Config.getEncoding();

    public static String md5Encoding = Config.getMd5Encoding();

    public String mailFrom = Config.getMailFrom();

    public String mailHost = "127.0.0.1";

    private HttpServletRequest request;

    private HttpServletResponse response;

    private HttpSession session;

    private JspWriter out;

    public Malgn(HttpServletRequest request, HttpServletResponse response, JspWriter out) {
        this.request = request;
        this.response = response;
        this.out = out;
        this.session = request.getSession();
    }

    public String qstr(String str) {
        return replace(str, "'", "''");
    }

    public String request(String name) {
        return request(name, "");
    }

    public String request(String name, String str) {
        String value = request.getParameter(name);
        if (value == null) {
            return str;
        } else {
            return replace(replace(value.replace('\'', '`'), "<", "&lt;"), ">", "&gt;");
        }
    }

    public String reqSql(String name) {
        return replace(request(name, ""), "'", "''");
    }

    public String reqSql(String name, String str) {
        return replace(request(name, str), "'", "''");
    }

    public String[] reqArr(String name) {
        return request.getParameterValues(name);
    }

    public String reqEnum(String name, String[] arr) {
        if (arr == null) return null;
        String str = request(name);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(str)) return arr[i];
        }
        return arr[0];
    }

    public int reqInt(String name) {
        return reqInt(name, 0);
    }

    public int reqInt(String name, int i) {
        String str = request(name, i + "");
        if (str.matches("^-?[0-9]+$")) return Integer.parseInt(str); else return i;
    }

    public static int parseInt(String str) {
        if (str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str); else return 0;
    }

    public static long parseLong(String str) {
        if (str != null && str.matches("^-?[0-9]+$")) return Long.parseLong(str); else return 0;
    }

    public static double parseDouble(String str) {
        if (str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str) * 1.0; else if (str != null && str.matches("^-?[0-9]+\\.[0-9]+$")) return Double.parseDouble(str); else return 0.0;
    }

    public Hashtable reqMap(String name) {
        Hashtable<String, String> map = new Hashtable<String, String>();
        int len = name.length();
        try {
            Enumeration e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                if (key.matches("^(" + name + ")(.+)$")) {
                    map.put(key.substring(len), request.getParameter(key));
                }
            }
        } catch (Exception ex) {
            errorLog("{Malgn.reqMap} " + ex.getMessage());
        }
        return map;
    }

    public void redirect(String url) {
        try {
            response.sendRedirect(url);
        } catch (Exception e) {
            errorLog("{Malgn.redirect} " + e.getMessage());
            jsReplace(url);
        }
    }

    public boolean isPost() {
        if ("POST".equals(request.getMethod())) {
            return true;
        } else {
            return false;
        }
    }

    public void jsAlert(String msg) {
        try {
            out.print("<script>alert('" + msg + "');</script>");
        } catch (Exception e) {
            errorLog("{Malgn.jsAlert} " + e.getMessage());
        }
    }

    public void jsError(String msg) {
        try {
            out.print("<script>alert('" + msg + "');history.go(-1)</script>");
        } catch (Exception e) {
            errorLog("{Malgn.jsError} " + e.getMessage());
        }
    }

    public void jsError(String msg, String target) {
        try {
            out.print("<script>alert('" + msg + "');" + target + ".location.href = " + target + ".location.href;</script>");
        } catch (Exception e) {
            errorLog("{Malgn.jsError} " + e.getMessage());
        }
    }

    public void jsErrClose(String msg) {
        jsErrClose(msg, null);
    }

    public void jsErrClose(String msg, String tgt) {
        try {
            if (tgt == null) tgt = "window";
            out.print("<script>alert('" + msg + "');" + tgt + ".close()</script>");
        } catch (Exception e) {
            errorLog("{Malgn.jsErrClose} " + e.getMessage());
        }
    }

    public void jsReplace(String url) {
        jsReplace(url, "window");
    }

    public void jsReplace(String url, String target) {
        try {
            out.print("<script>" + target + ".location.replace('" + url + "');</script>");
        } catch (Exception e) {
            errorLog("{Malgn.jsReplace} " + e.getMessage());
        }
    }

    public String getCookie(String s) throws Exception {
        Cookie[] cookie = request.getCookies();
        if (cookie == null) return "";
        for (int i = 0; i < cookie.length; i++) {
            if (s.equals(cookie[i].getName())) {
                String value = URLDecoder.decode(cookie[i].getValue(), encoding);
                return value;
            }
        }
        return "";
    }

    public void setCookie(String name, String value) throws Exception {
        Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
        if (cookieDomain != null) cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        response.addCookie(cookie);
    }

    public void setCookie(String name, String value, int time) throws Exception {
        Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
        if (cookieDomain != null) cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        cookie.setMaxAge(time);
        response.addCookie(cookie);
    }

    public void delCookie(String name) {
        Cookie cookie = new Cookie(name, "");
        if (cookieDomain != null) cookie.setDomain(cookieDomain);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    public String getSession(String s) {
        Object obj = session.getAttribute(s);
        if (obj == null) return "";
        return (String) obj;
    }

    public void setSession(String name, String value) {
        session.setAttribute(name, value);
    }

    public void setSession(String name, int value) {
        session.setAttribute(name, "" + value);
    }

    public static String getTimeString() {
        return getTimeString("yyyyMMddHHmmss");
    }

    public static String getTimeString(String sformat) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
        return sdf.format((new GregorianCalendar()).getTime());
    }

    public static String getTimeString(String sformat, Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
        if (sdf == null || date == null) return "";
        return sdf.format(date);
    }

    public static String getTimeString(String sformat, String date) {
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
        if (sdf == null || d == null) return "";
        return sdf.format(d);
    }

    public static int diffDate(String type, String sdate, String edate) {
        Date d1 = strToDate(sdate.trim());
        Date d2 = strToDate(edate.trim());
        long diff = d2.getTime() - d1.getTime();
        int ret = 0;
        type = type.toUpperCase();
        if ("D".equals(type)) ret = (int) (diff / (long) (1000 * 3600 * 24)); else if ("H".equals(type)) ret = (int) (diff / (long) (1000 * 3600)); else if ("I".equals(type)) ret = (int) (diff / (long) (1000 * 60)); else if ("S".equals(type)) ret = (int) (diff / (long) 1000);
        return ret;
    }

    public static Date addDate(String type, int amount) {
        return addDate(type, amount, new Date());
    }

    public static Date addDate(String type, int amount, String d) {
        return addDate(type, amount, strToDate(d));
    }

    public static Date addDate(String type, int amount, Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        type = type.toUpperCase();
        if ("Y".equals(type)) cal.add(cal.YEAR, amount); else if ("M".equals(type)) cal.add(cal.MONTH, amount); else if ("W".equals(type)) cal.add(cal.WEEK_OF_YEAR, amount); else if ("D".equals(type)) cal.add(cal.DAY_OF_MONTH, amount); else if ("H".equals(type)) cal.add(cal.HOUR_OF_DAY, amount); else if ("I".equals(type)) cal.add(cal.MINUTE, amount); else if ("S".equals(type)) cal.add(cal.SECOND, amount);
        return cal.getTime();
    }

    public static String addDate(String type, int amount, String d, String format) {
        return addDate(type, amount, strToDate(d), format);
    }

    public static String addDate(String type, int amount, Date d, String format) {
        return getTimeString(format, addDate(type, amount, d));
    }

    public static Date strToDate(String format, String source, Locale loc) {
        if (source == null || "".equals(source)) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(format, loc);
        Date d = null;
        try {
            d = sdf.parse(source);
        } catch (Exception e) {
            errorLog("{Malgn.strToDate} " + e.getMessage());
        }
        return d;
    }

    public static Date strToDate(String format, String source) {
        if (source == null || "".equals(source)) return null;
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date d = null;
        try {
            d = sdf.parse(source);
        } catch (Exception e) {
            errorLog("{Malgn.strToDate} " + e.getMessage());
        }
        return d;
    }

    public static Date strToDate(String source) {
        if (source == null || "".equals(source)) return null;
        String format = "yyyyMMddHHmmss";
        if (source.matches("^[0-9]{8}$")) format = "yyyyMMdd"; else if (source.matches("^[0-9]{14}$")) format = "yyyyMMddHHmmss"; else if (source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) format = "yyyy-MM-dd"; else if (source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$ [0-9]{2}:[0-9]{2}:[0-9]{2}")) format = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        Date d = null;
        try {
            d = sdf.parse(source);
        } catch (Exception e) {
            errorLog("{Malgn.strToDate} " + e.getMessage());
        }
        return d;
    }

    public static double getPercent(int cnt, int total) {
        if (total <= 0) return 0.0;
        return java.lang.Math.round(((double) cnt / (double) total) * 100);
    }

    public static String md5(String str) {
        StringBuffer buf = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] data = new byte[32];
            md.update(str.getBytes(md5Encoding), 0, str.length());
            data = md.digest();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
        } catch (Exception e) {
            errorLog("{Malgn.md5} " + e.getMessage());
        }
        return buf.toString();
    }

    public static String sha1(String str) {
        StringBuffer buf = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] data = new byte[40];
            md.update(str.getBytes("iso-8859-1"), 0, str.length());
            data = md.digest();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
        } catch (Exception e) {
            errorLog("{Malgn.sha1} " + e.getMessage());
        }
        return buf.toString();
    }

    public static String sha256(String str) {
        StringBuffer buf = new StringBuffer();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = new byte[64];
            md.update(str.getBytes("iso-8859-1"), 0, str.length());
            data = md.digest();
            for (int i = 0; i < data.length; i++) {
                int halfbyte = (data[i] >>> 4) & 0x0F;
                int two_halfs = 0;
                do {
                    if ((0 <= halfbyte) && (halfbyte <= 9)) buf.append((char) ('0' + halfbyte)); else buf.append((char) ('a' + (halfbyte - 10)));
                    halfbyte = data[i] & 0x0F;
                } while (two_halfs++ < 1);
            }
        } catch (Exception e) {
            errorLog("{Malgn.sha256} " + e.getMessage());
        }
        return buf.toString();
    }

    public static String getFileExt(String filename) {
        int i = filename.lastIndexOf(".");
        if (i == -1) return ""; else return filename.substring(i + 1);
    }

    public static String getUploadUrl(String filename) {
        if ("".equals(filename)) return "noimg";
        String ext = getFileExt(filename);
        if ("jsp".equals(ext.toLowerCase())) ext = "xxx";
        String md5name = md5(filename + "sdhflsdhflsdxxx") + "." + ext;
        return dataUrl + "/file/" + md5name;
    }

    public static String getUploadPath(String filename) {
        String ext = getFileExt(filename);
        if ("jsp".equals(ext.toLowerCase())) ext = "xxx";
        String md5name = md5(filename + "sdhflsdhflsdxxx") + "." + ext;
        return dataDir + "/file/" + md5name;
    }

    public String getQueryString(String exception) {
        String query = "";
        if (null != request.getQueryString()) {
            String[] exceptions = exception.replaceAll(" +", "").split("\\,");
            String[] queries = request.getQueryString().split("\\&");
            for (int i = 0; i < queries.length; i++) {
                String[] attributes = queries[i].split("\\=");
                if (attributes.length > 0 && inArray(attributes[0], exceptions)) continue;
                query += "&" + queries[i];
            }
        }
        return query.length() > 0 ? query.substring(1) : "";
    }

    public String getQueryString() {
        return getQueryString("");
    }

    public String getThisURI() {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        String thisuri = "";
        if (query == null) thisuri = uri; else thisuri = uri + "?" + query;
        return thisuri;
    }

    public void log(String prefix, String msg) throws Exception {
        File log = new File(logDir);
        if (!log.exists()) log.mkdirs();
        FileWriter logger = new FileWriter(logDir + "/" + prefix + "_" + getTimeString("yyyyMMdd") + ".log", true);
        logger.write("[" + getTimeString("yyyy-MM-dd HH:mm:ss") + "] " + request.getRemoteAddr() + " : " + getThisURI() + "\n" + msg + "\n");
        logger.close();
    }

    public void log(String msg) throws Exception {
        log("debug", msg);
    }

    public static void errorLog(String msg) {
        errorLog(msg, null);
    }

    public static void errorLog(String msg, Exception ex) {
        try {
            if (logDir == null) logDir = "/tmp";
            File log = new File(logDir);
            if (!log.exists()) log.mkdirs();
            if (ex != null) {
                StackTraceElement[] arr = ex.getStackTrace();
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i].getClassName().indexOf("_jsp") != -1) msg = "at " + arr[i].getClassName() + "." + arr[i].getMethodName() + "(" + arr[i].getFileName() + ":" + arr[i].getLineNumber() + ")\n" + msg;
                }
            }
            FileWriter logger = new FileWriter(logDir + "/error_" + getTimeString("yyyyMMdd") + ".log", true);
            logger.write("[" + getTimeString("yyyy-MM-dd HH:mm:ss") + "] " + msg + "\n");
            logger.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMX(String domain) throws Exception {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = new InitialDirContext(env);
        Attributes attrs = ictx.getAttributes(domain, new String[] { "MX" });
        Attribute attr = attrs.get("MX");
        if ((attr == null) || (attr.size() == 0)) {
            attrs = ictx.getAttributes(domain, new String[] { "A" });
            attr = attrs.get("A");
            if (attr == null) throw new Exception("No match for name '" + domain + "'");
        }
        String x = (String) attr.get(0);
        String[] f = x.split(" ");
        if (f[1].endsWith(".")) f[1] = f[1].substring(0, (f[1].length() - 1));
        return f[1];
    }

    public void mail(String mailTo, String subject, String body) throws Exception {
        mail(mailTo, subject, body, null);
    }

    public void mail(String mailTo, String subject, String body, String filepath) throws Exception {
        try {
            if (mailHost == null) {
                String[] arr = mailTo.split("@");
                mailHost = getMX(replace(arr[1], ">", ""));
            }
            Properties props = new Properties();
            props.put("mail.smtp.host", mailHost);
            Session msgSession = Session.getDefaultInstance(props, null);
            MimeMessage msg = new MimeMessage(msgSession);
            InternetAddress from = new InternetAddress(new String(mailFrom.getBytes("KSC5601"), "8859_1"));
            InternetAddress to = new InternetAddress(new String(mailTo.getBytes("KSC5601"), "8859_1"));
            msg.setFrom(from);
            msg.setRecipient(Message.RecipientType.TO, to);
            msg.setSubject(subject, "KSC5601");
            msg.setSentDate(new Date());
            if (filepath == null) {
                msg.setContent(body, "text/html; charset=" + encoding);
            } else {
                MimeBodyPart mbp1 = new MimeBodyPart();
                mbp1.setContent(body, "text/html; charset=" + encoding);
                MimeBodyPart mbp2 = new MimeBodyPart();
                FileDataSource fds = new FileDataSource(filepath);
                mbp2.setDataHandler(new DataHandler(fds));
                mbp2.setFileName(fds.getName());
                Multipart mp = new MimeMultipart();
                mp.addBodyPart(mbp1);
                mp.addBodyPart(mbp2);
                msg.setContent(mp);
            }
            Transport.send(msg);
        } catch (Exception ex) {
            errorLog("{Malgn.mail} " + ex.getMessage());
        }
    }

    public static String getUniqId() {
        String chars = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random r = new Random();
        char[] buf = new char[10];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = chars.charAt(r.nextInt(chars.length()));
        }
        return new String(buf);
    }

    public static String repeatString(String src, int repeat) {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < repeat; i++) {
            buf.append(src);
        }
        return buf.toString();
    }

    public static String cutString(String str, int len) throws Exception {
        return cutString(str, len, "...");
    }

    public static String cutString(String str, int len, String tail) throws Exception {
        try {
            byte[] by = str.getBytes("KSC5601");
            if (by.length <= len) return str;
            int count = 0;
            for (int i = 0; i < len; i++) {
                if ((by[i] & 0x80) == 0x80) count++;
            }
            if ((by[len - 1] & 0x80) == 0x80 && (count % 2) == 1) len--;
            len = len - (int) (count / 2);
            return str.substring(0, len) + tail;
        } catch (Exception e) {
            errorLog("{Malgn.cutString} " + e.getMessage());
            return "";
        }
    }

    public static boolean inArray(String str, String[] array) {
        if (str != null && array != null) {
            for (int i = 0; i < array.length; i++) {
                if (str.equals(array[i])) return true;
            }
        }
        return false;
    }

    public static String join(String str, Object[] array) {
        if (str != null && array != null) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < array.length; i++) {
                sb.append(array[i].toString());
                if (i < (array.length - 1)) sb.append(str);
            }
            return sb.toString();
        }
        return "";
    }

    public static String join(String str, Hashtable map) {
        StringBuffer sb = new StringBuffer();
        Enumeration e = map.keys();
        int size = map.size(), i = 0;
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = map.get(key) != null ? map.get(key).toString() : "";
            sb.append(value);
            if (i < (size - 1)) sb.append(str);
            i++;
        }
        return sb.toString();
    }

    public static DataSet arr2loop(String[] arr) {
        return arr2loop(arr, false);
    }

    public static DataSet arr2loop(String[] arr, boolean empty) {
        DataSet result = new DataSet();
        if (null != arr) {
            for (int i = 0; i < arr.length; i++) {
                String[] tmp = arr[i].split("=>");
                String id = tmp[0].trim();
                String value = (tmp.length > 1 ? tmp[1] : (empty ? "" : tmp[0])).trim();
                result.addRow();
                result.put("id", id);
                result.put("value", value);
                result.put("name", value);
                result.put("__first", i == 0 ? "true" : "false");
                result.put("__last", i == arr.length - 1 ? "true" : "false");
                result.put("__idx", i + 1);
                result.put("__ord", arr.length - i);
            }
        }
        result.first();
        return result;
    }

    public static DataSet arr2loop(Hashtable map) {
        DataSet result = new DataSet();
        Enumeration e = map.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = map.get(key) != null ? map.get(key).toString() : "";
            result.addRow();
            result.put("id", key);
            result.put("value", value);
            result.put("name", value);
        }
        result.first();
        return result;
    }

    public static String getItem(int item, String[] arr) {
        return getItem(item + "", arr);
    }

    public static String getItem(String item, String[] arr) {
        if (null != arr) {
            for (int i = 0; i < arr.length; i++) {
                String[] tmp = arr[i].split("=>");
                String id = tmp[0].trim();
                String value = (tmp.length > 1 ? tmp[1] : tmp[0]).trim();
                if (id.equals(item)) return value;
            }
        }
        return "";
    }

    public static String getItem(int item, Hashtable map) {
        return getItem(item + "", map);
    }

    public static String getItem(String item, Hashtable map) {
        Enumeration e = map.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = map.get(key) != null ? map.get(key).toString() : "";
            if (key.equals(item)) return value;
        }
        return "";
    }

    public static String[] getItemKeys(String[] arr) {
        String[] data = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            String[] tmp = arr[i].split("=>");
            String id = tmp[0].trim();
            data[i] = id;
        }
        return data;
    }

    public void download(String path, String filename) throws Exception {
        File f = new File(path);
        if (f.exists()) {
            try {
                response.setContentType("application/octet-stream;");
                response.setContentLength((int) f.length());
                response.setHeader("Content-Disposition", "attachment; filename=\"" + new String(filename.getBytes("KSC5601"), "8859_1") + "\"");
                byte[] bbuf = new byte[2048];
                BufferedInputStream fin = new BufferedInputStream(new FileInputStream(f));
                BufferedOutputStream outs = new BufferedOutputStream(response.getOutputStream());
                int read = 0;
                while ((read = fin.read(bbuf)) != -1) {
                    outs.write(bbuf, 0, read);
                }
                outs.close();
                fin.close();
            } catch (Exception e) {
                errorLog("{Malgn.download} " + e.getMessage());
                response.setContentType("text/html");
                out.println("File Download Error : " + e.getMessage());
            }
        } else {
            response.setContentType("text/html");
            out.println("File Not Found : " + path);
        }
    }

    public static String iconv(String in, String out, String str) throws Exception {
        return new String(str.getBytes(in), out);
    }

    public static String readFile(String path) throws Exception {
        return readFile(path, encoding);
    }

    public static String readFile(String path, String encoding) throws Exception {
        File f = new File(path);
        if (f.exists()) {
            FileInputStream fin = new FileInputStream(f);
            Reader reader = new InputStreamReader(fin, encoding);
            BufferedReader br = new BufferedReader(reader);
            char[] chars = new char[(int) f.length()];
            br.read(chars);
            br.close();
            reader.close();
            fin.close();
            return new String(chars);
        } else {
            return "";
        }
    }

    public static void copyFile(String source, String target) throws Exception {
        copyFile(new File(source), new File(target));
    }

    public static void copyFile(File source, File target) throws Exception {
        if (source.isDirectory()) {
            if (!target.isDirectory()) {
                target.mkdirs();
            }
            String[] children = source.list();
            for (int i = 0; i < children.length; i++) {
                copyFile(new File(source, children[i]), new File(target, children[i]));
            }
        } else {
            FileChannel inChannel = new FileInputStream(source).getChannel();
            FileChannel outChannel = new FileOutputStream(target).getChannel();
            try {
                int maxCount = (64 * 1024 * 1024) - (32 * 1024);
                long size = inChannel.size();
                long position = 0;
                while (position < size) {
                    position += inChannel.transferTo(position, maxCount, outChannel);
                }
            } catch (IOException e) {
                errorLog("{Malgn.copyFile} " + e.getMessage());
                throw e;
            } finally {
                if (inChannel != null) inChannel.close();
                if (outChannel != null) outChannel.close();
            }
        }
    }

    public static void delFile(String path) throws Exception {
        File f = new File(path);
        if (f.exists()) {
            if (f.isDirectory()) {
                File[] files = f.listFiles();
                for (int i = 0; i < files.length; i++) delFile(path + "/" + files[i].getName());
            }
            f.delete();
        } else {
            System.out.print(path + " is not found");
        }
    }

    public int getRandInt(int start, int count) {
        Random r = new Random();
        return start + r.nextInt(count);
    }

    public static int getUnixTime() {
        Date d = new Date();
        return (int) (d.getTime() / 1000);
    }

    public static int getUnixTime(String date) {
        Date d = strToDate(date);
        if (d == null) return 0;
        return (int) (d.getTime() / 1000);
    }

    public static String urlencode(String url) throws Exception {
        return URLEncoder.encode(url, encoding);
    }

    public static String urldecode(String url) throws Exception {
        return URLDecoder.decode(url, encoding);
    }

    public static String encode(String str) throws Exception {
        return replace(replace(Base64.encode(str), "=", "EQUAL"), "+", "PLUS");
    }

    public static String decode(String str) throws Exception {
        return Base64.decode(replace(replace(str, "PLUS", "+"), "EQUAL", "="));
    }

    public Hashtable strToMap(String str) {
        return this.strToMap(str, "");
    }

    public Hashtable strToMap(String str, String prefix) {
        Hashtable<String, String> h = new Hashtable<String, String>();
        if (str == null) return h;
        StringTokenizer token = new StringTokenizer(str, ",");
        while (token.hasMoreTokens()) {
            String subtoken = token.nextToken();
            int idx = subtoken.indexOf(":");
            if (idx != -1) {
                h.put(prefix + subtoken.substring(0, idx), replace(replace(subtoken.substring(idx + 1), "%3A", ":"), "%2C", ","));
            }
        }
        return h;
    }

    public String mapToString(Hashtable values) {
        if (values == null) return "";
        StringBuffer sb = new StringBuffer();
        Enumeration e = values.keys();
        int i = 0;
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = values.get(key) != null ? replace(replace(values.get(key).toString(), ":", "%3A"), ",", "%2C") : "";
            sb.append("," + key + ":" + value);
            i++;
        }
        if (i > 0) return sb.toString().substring(1); else return "";
    }

    public static boolean serialize(String path, Object obj) {
        return serialize(new File(path), obj);
    }

    public static boolean serialize(File file, Object obj) {
        FileOutputStream f = null;
        ObjectOutput s = null;
        boolean flag = true;
        try {
            f = new FileOutputStream(file);
            s = new ObjectOutputStream(f);
            s.writeObject(obj);
            s.flush();
        } catch (Exception e) {
            errorLog("{Malgn.serialize} " + e.getMessage());
            e.printStackTrace();
            flag = false;
        } finally {
            if (s != null) try {
                s.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (f != null) try {
                f.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return flag;
    }

    public static Object unserialize(String path) {
        return unserialize(new File(path));
    }

    public static Object unserialize(File file) {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        Object obj = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            obj = ois.readObject();
        } catch (Exception e) {
            errorLog("{Malgn.unserialize} " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (ois != null) try {
                ois.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (fis != null) try {
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return obj;
    }

    public String encrypt(String prefix) {
        String key = "SDI913akfrvb";
        return md5(prefix + md5(key));
    }

    public String nl2br(String str) {
        return replace(replace(str, "\r\n", "<br>"), "\n", "<br>");
    }

    public String htmlToText(String str) {
        return nl2br(replace(replace(str, "<", "&lt;"), ">", "&gt;"));
    }

    public static String stripTags(String str) {
        int offset = 0;
        int i = 0;
        int j = 0;
        int size = str.length();
        StringBuffer buf = new StringBuffer();
        synchronized (buf) {
            while ((i = str.indexOf("<", offset)) != -1) {
                if ((j = str.indexOf(">", offset)) != -1) {
                    buf.append(str.substring(offset, i));
                    offset = j + 1;
                } else {
                    break;
                }
            }
            buf.append(str.substring(offset));
            return replace(replace(replace(buf.toString(), "\t", ""), "\r", ""), "\n", "").trim();
        }
    }

    public static String strpad(String input, int size, String pad) {
        int gap = size - input.getBytes().length;
        if (gap <= 0) return input;
        String output = input;
        for (int i = 0; i < gap; i++) {
            output += pad;
        }
        return output;
    }

    public static String strrpad(String input, int size, String pad) {
        int gap = size - input.getBytes().length;
        if (gap <= 0) return input;
        String output = "";
        for (int i = 0; i < gap; i++) {
            output += pad;
        }
        return output + input;
    }

    public static String getFileSize(long size) {
        if (size >= 1024 * 1024 * 1024) {
            return (size / (1024 * 1024 * 1024)) + "GB";
        } else if (size >= 1024 * 1024) {
            return (size / (1024 * 1024)) + "MB";
        } else if (size >= 1024) {
            return (size / 1024) + "KB";
        } else {
            return size + "B";
        }
    }

    public double round(double size, int i) {
        size = size * (10 ^ i);
        return java.lang.Math.round(size) / (10 ^ i);
    }

    public static String numberFormat(int n) {
        DecimalFormat df = new DecimalFormat("#,###");
        return df.format(n);
    }

    public static String numberFormat(double n, int i) {
        String format = "#,##0";
        if (i > 0) format += "." + strpad("", i, "0");
        DecimalFormat df = new DecimalFormat(format);
        return df.format(n);
    }

    public void p(Object obj) throws Exception {
        out.print("<script>try { parent.document.getElementById('sysfrm').width = '100%'; parent.document.getElementById('sysfrm').height = 700; } catch(e) {}</script><div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
        if (obj != null) {
            if (obj.getClass().toString().indexOf("RecordSet") != -1 || obj.getClass().toString().indexOf("DataSet") != -1) {
                out.println("<pre style='text-align:left;font-size:9pt;'>");
                out.println(replace(replace(replace(replace(obj.toString(), "{", "\r\n{\n\t"), ", ", ",\r\n\t["), "}", "\r\n}"), "=", "] => "));
                out.println("</pre>");
            } else {
                out.println(obj.toString());
            }
        } else {
            out.println("NULL");
        }
        out.print("</div>");
    }

    public void p(String[] obj) throws Exception {
        out.print("<script>try { parent.document.getElementById('sysfrm').width = '100%'; parent.document.getElementById('sysfrm').height = 700; } catch(e) {}</script><div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
        if (obj != null) {
            for (int i = 0; i < obj.length; i++) {
                if (i > 0) out.print(", ");
                out.print(obj[i]);
            }
        } else {
            out.println("NULL");
        }
        out.print("</div>");
    }

    public void p(int i) throws Exception {
        out.print("<script>try { parent.document.getElementById('sysfrm').width = '100%'; parent.document.getElementById('sysfrm').height = 700; } catch(e) {}</script><div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
        p("" + i);
        out.print("</div>");
    }

    public void p() throws Exception {
        out.print("<script>try { parent.document.getElementById('sysfrm').width = '100%'; parent.document.getElementById('sysfrm').height = 700; } catch(e) {}</script><div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
        out.print("<pre style='text-align:left;font-size:9pt;'>");
        Enumeration e = request.getParameterNames();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            for (int i = 0; i < request.getParameterValues(key).length; i++) {
                out.println("[" + key + "] => " + request.getParameterValues(key)[i] + "\r");
            }
        }
        out.print("</pre>");
        out.print("</div>");
    }

    public String getScriptDir() {
        return dirname(replace(request.getRealPath(request.getServletPath()), "\\", "/"));
    }

    public static String dirname(String path) {
        File f = new File(path);
        return f.getParent();
    }

    public static String[] split(String p, String str, int length) {
        String[] arr = str.split(p);
        String[] result = new String[length];
        for (int i = 0; i < length; i++) {
            if (i < arr.length) {
                result[i] = arr[i];
            } else {
                result[i] = "";
            }
        }
        return result;
    }

    public Hashtable getImageSize(String path, int bx, int by) {
        Hashtable<String, String> imgsize = new Hashtable<String, String>();
        int width = 0;
        int height = 0;
        imgsize.put("width", "" + bx);
        imgsize.put("height", "" + by);
        try {
            File file = new File(path);
            BufferedImage bi = ImageIO.read(file);
            width = bi.getWidth();
            height = bi.getHeight();
            if (width > bx) {
                imgsize.put("height", "" + ((height * bx) / width));
                imgsize.put("width", "" + bx);
            } else if (width < by) {
                imgsize.put("width", "" + by);
                imgsize.put("height", "" + ((height * by) / width));
            }
        } catch (Exception e) {
            errorLog("{Malgn.getImageSize} " + e.getMessage());
        }
        if (imgsize.containsKey("width") && Integer.parseInt(imgsize.get("width")) >= bx) imgsize.put("width", "" + bx);
        if (imgsize.containsKey("height") && Integer.parseInt(imgsize.get("height")) > by + 40) imgsize.put("height", "" + (by + 20));
        return imgsize;
    }

    public Hashtable getImageSize(String path, int bx) {
        Hashtable<String, String> imgsize = new Hashtable<String, String>();
        int width = 0;
        int height = 0;
        int by = 0;
        imgsize.put("width", "" + bx);
        imgsize.put("height", "" + by);
        imgsize.put("r_width", "" + bx);
        imgsize.put("r_height", "" + by);
        try {
            File file = new File(path);
            BufferedImage bi = ImageIO.read(file);
            width = bi.getWidth();
            height = bi.getHeight();
            imgsize.put("r_width", "" + width);
            imgsize.put("r_height", "" + height);
            imgsize.put("width", "" + (width > bx ? bx : width));
            imgsize.put("height", "" + (height * ((bx * 1.0) / width)));
        } catch (Exception e) {
            errorLog("{Malgn.getImageSize} " + e.getMessage());
        }
        return imgsize;
    }

    public static String addSlashes(String str) {
        return replace(replace(replace(replace(replace(str, "\"", "&quot;"), "\\", "\\\\"), "\"", "\\\""), "\'", "\\\'"), "\r\n", "\\r\\n");
    }

    public static String replace(String s, String sub, String with) {
        int c = 0;
        int i = s.indexOf(sub, c);
        if (i == -1) return s;
        StringBuffer buf = new StringBuffer(s.length() + with.length());
        synchronized (buf) {
            do {
                buf.append(s.substring(c, i));
                buf.append(with);
                c = i + sub.length();
            } while ((i = s.indexOf(sub, c)) != -1);
            if (c < s.length()) {
                buf.append(s.substring(c, s.length()));
            }
            return buf.toString();
        }
    }

    public static String replace(String s, String[] sub, String[] with) {
        if (sub.length != with.length) return s;
        for (int i = 0; i < sub.length; i++) {
            s = replace(s, sub[i], with[i]);
        }
        return s;
    }

    public static String replace(String s, String[] sub, String with) {
        for (int i = 0; i < sub.length; i++) {
            s = replace(s, sub[i], with);
        }
        return s;
    }
}
