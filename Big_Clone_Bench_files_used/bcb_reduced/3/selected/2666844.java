package assays.com;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.io.IOException;
import java.io.FileWriter;
import java.io.File;
import java.text.*;
import java.security.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;

public final class Util {

    public static String truncateTime(long inpData) {
        Date myDate = new Date(inpData);
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        sdf.applyPattern("yyyy-MMM-dd HH:mm");
        return sdf.format(myDate);
    }

    public static String truncateTime(Date inpDate) {
        if (inpDate != null) {
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            sdf.applyPattern("yyyy-MMM-dd HH:mm");
            return sdf.format(inpDate);
        }
        return "&nbsp;";
    }

    public static int getThisYear() {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(new Date());
        return myCal.get(Calendar.YEAR);
    }

    public static String getYear(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        return String.valueOf(myCal.get(Calendar.YEAR));
    }

    public static String getMonth(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        return String.valueOf(myCal.get(Calendar.MONTH) + 1);
    }

    public static String getDay(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        return String.valueOf(myCal.get(Calendar.DAY_OF_MONTH));
    }

    public static String getHour(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        return String.valueOf(myCal.get(Calendar.HOUR_OF_DAY));
    }

    public static String getMinute(Date date) {
        Calendar myCal = Calendar.getInstance();
        myCal.setTime(date);
        return String.valueOf(myCal.get(Calendar.MINUTE));
    }

    public static String truncateString(String inpStr) {
        if (inpStr != null) {
            int l = inpStr.length();
            if (l > 21) return inpStr.substring(0, 18) + "..."; else if (l > 0) return inpStr;
        }
        return "&nbsp;";
    }

    public static String truncateDate(Date inpDate) {
        if (inpDate != null) {
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
            sdf.applyPattern("yyyy-MMM-dd");
            return sdf.format(inpDate);
        }
        return "&nbsp;";
    }

    public static Integer getInteger(String number) {
        Integer myInteger;
        try {
            myInteger = new Integer(number);
        } catch (Exception ex) {
            myInteger = new Integer(0);
        }
        return myInteger;
    }

    public static Long getLong(String number) {
        Long myLong;
        try {
            myLong = new Long(number);
        } catch (Exception ex) {
            myLong = new Long(0);
        }
        return myLong;
    }

    public static int getInt(String number) {
        int myInteger;
        try {
            myInteger = Integer.parseInt(number);
        } catch (Exception ex) {
            myInteger = 0;
        }
        return myInteger;
    }

    public static int getInt(char digit) {
        int myInteger;
        if (digit < '1' || digit > '9') myInteger = 0; else myInteger = digit - '0';
        return myInteger;
    }

    public static Double getNumber(String number) {
        Double myDouble = null;
        if (!number.trim().equals("")) try {
            myDouble = new Double(Double.parseDouble(number));
        } catch (Exception ex) {
        }
        return myDouble;
    }

    public static Double getNumber(String v1, String v2) {
        Double myDouble = null;
        if (!v1.trim().equals("") || !v2.trim().equals("")) try {
            String s1 = (v1.equals("")) ? "0" : v1;
            String s2 = (v2.equals("")) ? "0" : v2;
            String s12 = s1 + "." + s2;
            myDouble = new Double(Double.parseDouble(s12));
        } catch (Exception ex) {
        }
        return myDouble;
    }

    public static String getIntegerPart(Double d) {
        if (d == null) return "";
        String s11 = d.toString();
        int i = s11.indexOf('.');
        return (i < 0) ? s11 : (i == 0) ? "0" : s11.substring(0, i);
    }

    public static String getFractionPart(Double d) {
        if (d == null) return "";
        String s11 = d.toString();
        int i = s11.indexOf('.');
        return (i < 0) ? "0" : s11.substring(i + 1);
    }

    public static int getNumberFromStringPostfix(String str) {
        int visitCount = 0;
        int fact = 1;
        for (int i = str.length(); i > 0; ) {
            char ch = str.charAt(--i);
            if (!Character.isUpperCase(ch)) break;
            visitCount += (ch - ('A' - 1)) * fact;
            fact *= 26;
        }
        return visitCount + 1;
    }

    public static String getStringPostfixFromNumber(int num) {
        String s = "";
        int darb = num;
        do {
            int darb1 = darb % 26;
            darb /= 26;
            if (darb1 == 0) {
                darb1 = 26;
                darb--;
            }
            s = ((char) (('A' - 1) + darb1)) + s;
        } while (darb > 0);
        return s;
    }

    public static String getFieldNameL(int inpDate) {
        DecimalFormat snf = (DecimalFormat) NumberFormat.getNumberInstance();
        snf.applyPattern("F000");
        return snf.format(inpDate);
    }

    public static String getFieldNameM(int inpDate) {
        DecimalFormat snf = (DecimalFormat) NumberFormat.getNumberInstance();
        snf.applyPattern("f000");
        return snf.format(inpDate);
    }

    public static int getFieldNameNumeric(String colName) {
        int result = 0;
        if (colName.charAt(0) == 'F') try {
            result = Integer.parseInt(colName.substring(1));
        } catch (Exception ex) {
        }
        return result;
    }

    public static boolean checkFieldNameNumeric(String colName) {
        int res = getFieldNameNumeric(colName);
        return (res > 0) && (res < 56);
    }

    private static File logFile = null;

    private static void writeString(String string) {
        boolean append = true;
        if (logFile != null) {
            try {
                FileWriter fos = new FileWriter(logFile, append);
                fos.write(string);
                fos.write("\r\n");
                fos.close();
            } catch (IOException ex) {
            }
        }
    }

    public static String getLogFilePath() {
        if (logFile == null) return "not exists"; else return logFile.getAbsolutePath();
    }

    public static void initializeLog(File logDirectory) {
        long MAX_SIZE = 100000;
        logFile = new File(logDirectory, "assays.log");
        long l = logFile.length();
        if (l > MAX_SIZE) {
            String[] files = logDirectory.list();
            int n = 0;
            for (int i = 0; i < files.length; i++) {
                int j = files[i].indexOf('_');
                if (j < 0) continue;
                int k = files[i].indexOf('.');
                if (k < 0) continue;
                int m = getInt(files[i].substring(j + 1, k));
                if (m > n) n = m;
            }
            n++;
            String newName = "assays_" + n + ".log";
            File permFile = new File(logDirectory, newName);
            logFile.renameTo(permFile);
        }
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(truncateTime(new Date())).append('\t').append("Start of application").append("(log size=").append(l).append(')');
        writeString(strBuf.toString());
    }

    public static synchronized void writeLog(String fullName, String logName, String sessId) {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(truncateTime(new Date())).append('\t').append(fullName).append('\t').append(logName).append('\t').append(sessId);
        writeString(strBuf.toString());
    }

    public static synchronized void writeEndOfApplication() {
        StringBuffer strBuf = new StringBuffer();
        strBuf.append(truncateTime(new Date())).append('\t').append("End of application");
        writeString(strBuf.toString());
    }

    public static boolean checkInteger(String val, int min, int max) {
        boolean result = false;
        if (!val.trim().equals("")) try {
            int yy = Integer.parseInt(val);
            if (yy >= min && yy <= max) result = true;
        } catch (Exception ex) {
        }
        return result;
    }

    public static boolean checkFloatValue(String value) {
        boolean result = true;
        try {
            if (!value.trim().equals("")) {
                Double.parseDouble(value);
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    public static boolean checkIntegerValue(String value) {
        boolean result = true;
        try {
            if (!value.trim().equals("")) {
                Integer.parseInt(value);
            }
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    public static boolean checkBooleanValue(String value) {
        return value.equals("0") || value.equals("1");
    }

    public static boolean checkFloatArray(String value) {
        int k = 0;
        int l;
        while ((l = value.indexOf(',', k)) > 0) {
            if (l > k && !checkFloatValue(value.substring(k, l))) return false;
            k = l + 1;
        }
        return true;
    }

    public static boolean checkIntegerArray(String value) {
        int k = 0;
        int l;
        while ((l = value.indexOf(',', k)) > 0) {
            if (l > k && !checkIntegerValue(value.substring(k, l))) return false;
            k = l + 1;
        }
        return true;
    }

    public static String checkDateTimeValue(String value, int attribute1) {
        StringBuffer xyz = new StringBuffer();
        SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
        if (attribute1 < 2) sdf.applyPattern("yyyy"); else if (attribute1 < 3) sdf.applyPattern("yyyy-MM-dd"); else sdf.applyPattern("yyyy-MM-dd HH:mm");
        try {
            Date myDate = sdf.parse(value);
            Calendar myCal = Calendar.getInstance();
            myCal.setTime(myDate);
            xyz.append(myCal.get(Calendar.YEAR));
            if (attribute1 > 1) xyz.append(',').append(myCal.get(Calendar.MONTH) + 1).append(',').append(myCal.get(Calendar.DAY_OF_MONTH));
            if (attribute1 > 2) xyz.append(',').append(myCal.get(Calendar.HOUR_OF_DAY)).append(',').append(myCal.get(Calendar.MINUTE));
            xyz.append(',');
            return xyz.toString();
        } catch (Exception ex) {
        }
        return null;
    }

    public static String checkTimeValue(String value, int attribute1) {
        StringBuffer xyz = new StringBuffer();
        int m = attribute1;
        int k = 0;
        int l;
        try {
            if (m >= 4) {
                l = value.indexOf(',', k);
                if (l > k) {
                    Integer.parseInt(value.substring(k, l));
                    xyz.append(value.substring(k, l));
                }
                if (l >= 0) k = l + 1;
                m -= 4;
            }
            xyz.append(',');
            if (m >= 2) {
                l = value.indexOf(',', k);
                if (l > k) {
                    Integer.parseInt(value.substring(k, l));
                    xyz.append(value.substring(k, l));
                }
                if (l >= 0) k = l + 1;
                m -= 2;
            }
            xyz.append(',');
            if (m >= 1) {
                l = value.indexOf(',', k);
                if (l > k) {
                    Integer.parseInt(value.substring(k, l));
                    xyz.append(value.substring(k, l));
                }
            }
            xyz.append(',');
            return xyz.toString();
        } catch (Exception ex) {
        }
        return null;
    }

    public static String getDateString(String value, int attribute1) {
        if (value != null && !value.equals("") && value.charAt(0) != ',') try {
            int k = 0;
            int l = value.indexOf(',', k);
            String val = value.substring(k, l);
            int yy = Integer.parseInt(val);
            int mm = 1;
            int dd = 1;
            if (attribute1 > 1) {
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                mm = Integer.parseInt(val);
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                dd = Integer.parseInt(val);
            }
            Calendar myCal = Calendar.getInstance();
            myCal.set(yy, mm - 1, dd);
            if (attribute1 > 2) {
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                int hh = Integer.parseInt(val);
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                int nn = Integer.parseInt(val);
                myCal.set(Calendar.HOUR_OF_DAY, hh);
                myCal.set(Calendar.MINUTE, nn);
            }
            Date myDate = myCal.getTime();
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            if (attribute1 < 2) sdf.applyPattern("yyyy"); else if (attribute1 < 3) sdf.applyPattern("yyyy-MMM-dd"); else sdf.applyPattern("yyyy-MMM-dd HH:mm");
            return sdf.format(myDate);
        } catch (Exception ex) {
        }
        return "";
    }

    public static String getDateStringForExport(String value, int attribute1) {
        if (value != null && !value.equals("") && value.charAt(0) != ',') try {
            int k = 0;
            int l = value.indexOf(',', k);
            String val = value.substring(k, l);
            int yy = Integer.parseInt(val);
            int mm = 1;
            int dd = 1;
            if (attribute1 > 1) {
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                mm = Integer.parseInt(val);
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                dd = Integer.parseInt(val);
            }
            Calendar myCal = Calendar.getInstance();
            myCal.set(yy, mm - 1, dd);
            if (attribute1 > 2) {
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                int hh = Integer.parseInt(val);
                k = l + 1;
                l = value.indexOf(',', k);
                val = value.substring(k, l);
                int nn = Integer.parseInt(val);
                myCal.set(Calendar.HOUR_OF_DAY, hh);
                myCal.set(Calendar.MINUTE, nn);
            }
            Date myDate = myCal.getTime();
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            if (attribute1 < 2) sdf.applyPattern("yyyy"); else if (attribute1 < 3) sdf.applyPattern("yyyy-MM-dd"); else sdf.applyPattern("yyyy-MM-dd HH:mm");
            return sdf.format(myDate);
        } catch (Exception ex) {
        }
        return "";
    }

    public static String getTimeString(String value, int attribute1) {
        StringBuffer strBuf = new StringBuffer();
        if (value != null && !value.equals("")) try {
            String val;
            int k = 0;
            int l = value.indexOf(',', k);
            if (l > k) {
                val = value.substring(k, l);
                if (!val.equals("0")) {
                    strBuf.append(val).append("h ");
                }
            }
            k = l + 1;
            l = value.indexOf(',', k);
            if (l > k) {
                val = value.substring(k, l);
                if (!val.equals("0")) {
                    strBuf.append(val).append("m ");
                }
            }
            k = l + 1;
            l = value.indexOf(',', k);
            if (l > k) {
                val = value.substring(k, l);
                if (!val.equals("0")) {
                    strBuf.append(val).append("s");
                }
            }
        } catch (Exception ex) {
        }
        return strBuf.toString();
    }

    public static String getBooleanString(String value) {
        String result;
        if (value != null && value.equals("1")) result = "yes"; else result = "no";
        return result;
    }

    public static String getHTMLString(String inpStr) {
        if (inpStr != null) {
            int l = inpStr.length();
            if (l > 0) {
                StringBuffer xyz = new StringBuffer();
                for (int i = 0; i < l; i++) {
                    char c = inpStr.charAt(i);
                    switch(c) {
                        case '<':
                            xyz.append("&lt;");
                            break;
                        case '>':
                            xyz.append("&gt;");
                            break;
                        case '"':
                            xyz.append("&quot;");
                            break;
                        case '&':
                            xyz.append("&amp;");
                            break;
                        default:
                            xyz.append(c);
                    }
                }
                return xyz.toString();
            }
        }
        return "";
    }

    public static String getStringWithoutSpaces(String inpStr) {
        if (inpStr != null) {
            int l = inpStr.length();
            if (l > 0) {
                StringBuffer xyz = new StringBuffer();
                for (int i = 0; i < l; i++) {
                    char c = inpStr.charAt(i);
                    switch(c) {
                        case '<':
                            xyz.append("&lt;");
                            break;
                        case '>':
                            xyz.append("&gt;");
                            break;
                        case '"':
                            xyz.append("&quot;");
                            break;
                        case '&':
                            xyz.append("&amp;");
                            break;
                        case ' ':
                            xyz.append('_');
                            break;
                        default:
                            xyz.append(c);
                    }
                }
                return xyz.toString();
            }
        }
        return "";
    }

    public static String truncateHTMLString(String inpStr) {
        if (inpStr != null) {
            int l = inpStr.length();
            if (l > 0) {
                int l1 = (l < 22) ? l : 18;
                StringBuffer xyz = new StringBuffer();
                for (int i = 0; i < l1; i++) {
                    char c = inpStr.charAt(i);
                    switch(c) {
                        case '<':
                            xyz.append("&lt;");
                            break;
                        case '>':
                            xyz.append("&gt;");
                            break;
                        case '"':
                            xyz.append("&quot;");
                            break;
                        case '&':
                            xyz.append("&amp;");
                            break;
                        default:
                            xyz.append(c);
                    }
                }
                if (l > 21) xyz.append("...");
                return xyz.toString();
            }
        }
        return "";
    }

    public static String criptString(String pswd) {
        StringBuffer xyz = new StringBuffer();
        try {
            byte inarray[] = pswd.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(inarray, 0, inarray.length);
            byte outarray[] = md5.digest();
            for (int i = 0; i < outarray.length; i++) xyz.append(outarray[i]);
        } catch (NoSuchAlgorithmException ex) {
        }
        return xyz.toString();
    }

    private static boolean checkFtpDirec(File ftpDir) {
        if (!ftpDir.exists()) ; else if (!ftpDir.isDirectory()) ; else if (!ftpDir.canRead()) Util.writeLog("---Prompt ftp files.", ftpDir.getAbsolutePath(), "cannot read"); else return true;
        return false;
    }

    private static void getDirectoryFilePrompter(String valuePrefix, String textPrefix, StringBuffer xyz, File ftpDir) {
        if (!ftpDir.canRead()) Util.writeLog("---Prompt ftp files.", ftpDir.getAbsolutePath(), "cannot read"); else {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                File zyx = new File(ftpDir, files[i]);
                if (zyx.isDirectory()) {
                    getDirectoryFilePrompter(valuePrefix + files[i] + File.separatorChar, textPrefix + files[i] + File.separatorChar, xyz, zyx);
                } else {
                    xyz.append("<option value=\"").append(valuePrefix + files[i]).append("\">").append(textPrefix + files[i]);
                }
            }
        }
    }

    public static String getFtpFilePrompter(String userDir, String technDir) {
        File ftpDir = UserHttpSession.getFtpDirectory();
        StringBuffer xyz = new StringBuffer();
        xyz.append("<option value=\"\">--- select file from ftp directory ---");
        if (checkFtpDirec(ftpDir) && userDir != null && technDir != null) {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                if (userDir.equals("*") || userDir.equals(files[i])) {
                    File ftpDir_user = new File(ftpDir, files[i]);
                    if (checkFtpDirec(ftpDir_user)) {
                        File ftpDir_user_techn = new File(ftpDir_user, technDir);
                        if (checkFtpDirec(ftpDir_user_techn)) {
                            String textPrefix = "($" + files[i] + ')' + File.separatorChar;
                            String valuePrefix = files[i] + File.separatorChar + technDir + File.separatorChar;
                            getDirectoryFilePrompter(valuePrefix, textPrefix, xyz, ftpDir_user_techn);
                        }
                    }
                }
            }
        }
        return xyz.toString();
    }

    private static void getDirectoryZipPrompter(String valuePrefix, String textPrefix, StringBuffer xyz, File ftpDir) {
        if (!ftpDir.canRead()) Util.writeLog("---Prompt ftp zipfiles.", ftpDir.getAbsolutePath(), "cannot read"); else {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                File zyx = new File(ftpDir, files[i]);
                if (zyx.isDirectory()) {
                    getDirectoryZipPrompter(valuePrefix + files[i] + File.separatorChar, textPrefix + files[i] + File.separatorChar, xyz, zyx);
                } else {
                    int j = files[i].lastIndexOf('.');
                    if (j > 0) {
                        String ext = files[i].substring(j + 1);
                        if (ext.equals("zip") || ext.equals("ZIP")) xyz.append("<option value=\"").append(valuePrefix + files[i]).append("\">").append(textPrefix + files[i]);
                    }
                }
            }
        }
    }

    public static String getFtpZipPrompter(String userDir, String technDir) {
        File ftpDir = UserHttpSession.getFtpDirectory();
        StringBuffer xyz = new StringBuffer();
        xyz.append("<option value=\"\">--- select zipfile from ftp directory ---");
        if (checkFtpDirec(ftpDir) && userDir != null && technDir != null) {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                if (userDir.equals("*") || userDir.equals(files[i])) {
                    File ftpDir_user = new File(ftpDir, files[i]);
                    if (checkFtpDirec(ftpDir_user)) {
                        File ftpDir_user_techn = new File(ftpDir_user, technDir);
                        if (checkFtpDirec(ftpDir_user_techn)) {
                            String textPrefix = "($" + files[i] + ')' + File.separatorChar;
                            String valuePrefix = files[i] + File.separatorChar + technDir + File.separatorChar;
                            getDirectoryZipPrompter(valuePrefix, textPrefix, xyz, ftpDir_user_techn);
                        }
                    }
                }
            }
        }
        return xyz.toString();
    }

    private static void getDirectoryPrompter(String valuePrefix, String textPrefix, StringBuffer xyz, File ftpDir) {
        if (!ftpDir.canRead()) Util.writeLog("---Prompt ftp directory.", ftpDir.getAbsolutePath(), "cannot read"); else {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                File zyx = new File(ftpDir, files[i]);
                if (zyx.isDirectory()) {
                    xyz.append("<option value=\"").append(valuePrefix + files[i]).append("\">").append(textPrefix + files[i]);
                    getDirectoryPrompter(valuePrefix + files[i] + File.separatorChar, textPrefix + files[i] + File.separatorChar, xyz, zyx);
                }
            }
        }
    }

    public static String getFtpDirectoryPrompter(String userDir, String technDir) {
        File ftpDir = UserHttpSession.getFtpDirectory();
        StringBuffer xyz = new StringBuffer();
        xyz.append("<option value=\"\">--- select ftp directory ---");
        if (checkFtpDirec(ftpDir) && userDir != null && technDir != null) {
            String[] files = ftpDir.list();
            for (int i = 0; i < files.length; i++) {
                if (userDir.equals("*") || userDir.equals(files[i])) {
                    File ftpDir_user = new File(ftpDir, files[i]);
                    if (checkFtpDirec(ftpDir_user)) {
                        File ftpDir_user_techn = new File(ftpDir_user, technDir);
                        if (checkFtpDirec(ftpDir_user_techn)) {
                            String textPrefix = "($" + files[i] + ')' + File.separatorChar;
                            String valuePrefix = files[i] + File.separatorChar + technDir + File.separatorChar;
                            xyz.append("<option value=\"").append(valuePrefix).append("\">").append(textPrefix);
                            getDirectoryPrompter(valuePrefix, textPrefix, xyz, ftpDir_user_techn);
                        }
                    }
                }
            }
        }
        return xyz.toString();
    }

    public static void copyFile(File in, File out) {
        int len;
        byte[] buffer = new byte[1024];
        try {
            FileInputStream fin = new FileInputStream(in);
            FileOutputStream fout = new FileOutputStream(out);
            while ((len = fin.read(buffer)) >= 0) fout.write(buffer, 0, len);
            fin.close();
            fout.close();
        } catch (IOException ex) {
        }
    }

    public static String[] makeArrayFromString(String in) {
        if (in == null) return null;
        int k = 0;
        int to = in.indexOf(',');
        while (to > 0) {
            k++;
            to++;
            to = in.indexOf(',', to);
        }
        if (k == 0) return null;
        String[] array = new String[k];
        int from = 0;
        to = in.indexOf(',');
        k = 0;
        while (to > 0) {
            array[k++] = in.substring(from, to);
            from = to + 1;
            to = in.indexOf(',', from);
        }
        return array;
    }
}
