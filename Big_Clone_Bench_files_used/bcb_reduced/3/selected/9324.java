package icapture.com;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.text.DateFormat;
import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.security.NoSuchAlgorithmException;
import java.security.MessageDigest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;

public final class Util {

    public static String truncateTime(Date inpDate) {
        if (inpDate != null) {
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateTimeInstance();
            sdf.applyPattern("yyyy-MMM-dd HH:mm");
            return sdf.format(inpDate);
        }
        return "&nbsp;";
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
            if (l > 21) {
                return inpStr.substring(0, 18) + "...";
            } else if (l > 0) {
                return inpStr;
            }
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

    public static String truncateDateSQLFormat(Date inpDate) {
        if (inpDate != null) {
            SimpleDateFormat sdf = (SimpleDateFormat) DateFormat.getDateInstance();
            sdf.applyPattern("yyyy-MM-dd");
            return sdf.format(inpDate);
        }
        return "&nbsp;";
    }

    public static int getInt(String number) {
        int myInteger = 0;
        try {
            myInteger = Integer.parseInt(number);
        } catch (Exception ex) {
        }
        return myInteger;
    }

    public static Integer getInteger(String number) {
        Integer myInteger;
        try {
            myInteger = new Integer(Integer.parseInt(number));
        } catch (Exception ex) {
            myInteger = new Integer(0);
        }
        return myInteger;
    }

    public static Double getNumber(String number) {
        Double myDouble = null;
        if (!number.trim().equals("")) {
            try {
                myDouble = new Double(Double.parseDouble(number));
            } catch (Exception ex) {
            }
        }
        return myDouble;
    }

    public static Double getNumber(String v1, String v2) {
        Double myDouble = null;
        if (!v1.trim().equals("") || !v2.trim().equals("")) {
            try {
                String s1 = (v1.equals("")) ? "0" : v1;
                String s2 = (v2.equals("")) ? "0" : v2;
                String s12 = s1 + "." + s2;
                myDouble = new Double(Double.parseDouble(s12));
            } catch (Exception ex) {
            }
        }
        return myDouble;
    }

    public static String getIntegerPart(Double d) {
        if (d == null) {
            return "";
        }
        String s11 = d.toString();
        int i = s11.indexOf('.');
        return (i < 0) ? s11 : (i == 0) ? "0" : s11.substring(0, i);
    }

    public static String getFractionPart(Double d) {
        if (d == null) {
            return "";
        }
        String s11 = d.toString();
        int i = s11.indexOf('.');
        return (i < 0) ? "0" : s11.substring(i + 1);
    }

    public static int getNumberFromStringPostfix(String str) {
        int visitCount = 0;
        int fact = 1;
        for (int i = str.length(); i > 0; ) {
            char ch = str.charAt(--i);
            if (!Character.isUpperCase(ch)) {
                break;
            }
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

    public static String criptString(String pswd) {
        StringBuffer xyz = new StringBuffer();
        try {
            byte inarray[] = pswd.getBytes();
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(inarray, 0, inarray.length);
            byte outarray[] = md5.digest();
            for (int i = 0; i < outarray.length; i++) {
                xyz.append(outarray[i]);
            }
        } catch (NoSuchAlgorithmException ex) {
        }
        return xyz.toString();
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

    public static void initializeLog(File logDirectory) {
        long MAX_SIZE = 100000;
        logFile = new File(logDirectory, "samples.log");
        long l = logFile.length();
        if (l > MAX_SIZE) {
            String[] files = logDirectory.list();
            int n = 0;
            for (int i = 0; i < files.length; i++) {
                int j = files[i].indexOf('_');
                if (j < 0) {
                    continue;
                }
                int k = files[i].indexOf('.');
                if (k < 0) {
                    continue;
                }
                int m = getInt(files[i].substring(j + 1, k));
                if (m > n) {
                    n = m;
                }
            }
            n++;
            String newName = "samples_" + n + ".log";
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

    /**
     * takes a string number character and converts it to the appropriate
     * letter value in string form (ie "1" -> "A")
     * If can't convert to letter, just returns it 'as is'
     * @param num
     * @return letter
     */
    public static String numToLetter(String num) {
        num = num.equalsIgnoreCase("1") ? "A" : num.equalsIgnoreCase("2") ? "B" : num.equalsIgnoreCase("3") ? "C" : num.equalsIgnoreCase("4") ? "D" : num.equalsIgnoreCase("5") ? "E" : num.equalsIgnoreCase("6") ? "F" : num.equalsIgnoreCase("7") ? "G" : num.equalsIgnoreCase("8") ? "H" : num.equalsIgnoreCase("9") ? "I" : num.equalsIgnoreCase("10") ? "J" : num.equalsIgnoreCase("11") ? "K" : num.equalsIgnoreCase("12") ? "L" : num.equalsIgnoreCase("13") ? "M" : num.equalsIgnoreCase("14") ? "N" : num.equalsIgnoreCase("15") ? "O" : num.equalsIgnoreCase("16") ? "P" : num.equalsIgnoreCase("17") ? "Q" : num.equalsIgnoreCase("18") ? "R" : num.equalsIgnoreCase("19") ? "S" : num.equalsIgnoreCase("20") ? "T" : num.equalsIgnoreCase("21") ? "U" : num.equalsIgnoreCase("22") ? "V" : num.equalsIgnoreCase("23") ? "W" : num.equalsIgnoreCase("24") ? "X" : num.equalsIgnoreCase("25") ? "Y" : num.equalsIgnoreCase("26") ? "Z" : num;
        return num;
    }

    /**
     * takes a string number character and converts it to the appropriate
     * letter value in string form (ie "1" -> "A")
     * If can't convert to letter, just returns it 'as is'
     * @param num
     * @return letter
     */
    public static String letterToNum(String letter) {
        letter = letter.equalsIgnoreCase("A") ? "1" : letter.equalsIgnoreCase("B") ? "2" : letter.equalsIgnoreCase("C") ? "3" : letter.equalsIgnoreCase("D") ? "4" : letter.equalsIgnoreCase("E") ? "5" : letter.equalsIgnoreCase("F") ? "6" : letter.equalsIgnoreCase("G") ? "7" : letter.equalsIgnoreCase("H") ? "8" : letter.equalsIgnoreCase("I") ? "9" : letter.equalsIgnoreCase("J") ? "10" : letter.equalsIgnoreCase("K") ? "11" : letter.equalsIgnoreCase("L") ? "12" : letter.equalsIgnoreCase("M") ? "13" : letter.equalsIgnoreCase("N") ? "14" : letter.equalsIgnoreCase("O") ? "15" : letter.equalsIgnoreCase("P") ? "16" : letter.equalsIgnoreCase("Q") ? "17" : letter.equalsIgnoreCase("R") ? "18" : letter.equalsIgnoreCase("S") ? "19" : letter.equalsIgnoreCase("T") ? "20" : letter.equalsIgnoreCase("U") ? "21" : letter.equalsIgnoreCase("V") ? "22" : letter.equalsIgnoreCase("W") ? "23" : letter.equalsIgnoreCase("X") ? "24" : letter.equalsIgnoreCase("Y") ? "25" : letter.equalsIgnoreCase("Z") ? "26" : letter;
        return letter;
    }

    /**
     * "yyyy-MM-dd_HH"
     * @return
     */
    public static String getCurrentBatchID() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * "yyyy-MM-dd_HH-mm-ss"
     * @return
     */
    public static String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    /**
     * "yyyy-MM-dd"
     * @return
     */
    public static String getCurrentDate() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date date = new Date();
        return dateFormat.format(date);
    }

    public static double round(double d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Double.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.doubleValue();
    }

    /**
     * get a valid date from string params or null if invalid. "Low" because if
     * month is blank, it will be set to 1, if day is blank it will be set to 1
     * @param year
     * @param month
     * @param day
     * @return a date object or null
     */
    public static Date getLowDate(String year, String month, String day) {
        Date myDate = null;
        try {
            int yy = Integer.parseInt(year);
            if (yy > 1900 && yy < 2100) {
                int mm = (month.equals("")) ? 1 : Integer.parseInt(month);
                if (mm > 0 && mm < 13) {
                    int dd = (day.equals("")) ? 1 : Integer.parseInt(day);
                    if (dd > 0 & dd < 32) {
                        Calendar myCal = Calendar.getInstance();
                        myCal.set(yy, mm - 1, dd, 0, 0);
                        myDate = myCal.getTime();
                    }
                }
            }
        } catch (Exception ex) {
        }
        return myDate;
    }

    /**
     * get a valid date from string params or null if invalid. "High" because if
     * month is blank, it will be set to 12, if day is blank it will be set to 31
     * @param year
     * @param month
     * @param day
     * @return a date object or null
     */
    public static Date getHighDate(String year, String month, String day) {
        Date myDate = null;
        try {
            int yy = Integer.parseInt(year);
            if (yy > 1900 && yy < 2100) {
                int mm = (month.equals("")) ? 12 : Integer.parseInt(month);
                if (mm > 0 && mm < 13) {
                    int dd = (day.equals("")) ? 31 : Integer.parseInt(day);
                    if (dd > 0 & dd < 32) {
                        Calendar myCal = Calendar.getInstance();
                        myCal.set(yy, mm - 1, dd, 23, 59);
                        myDate = myCal.getTime();
                    }
                }
            }
        } catch (Exception ex) {
        }
        return myDate;
    }

    /**
     * get a valid date from string params or null if invalid. "Low" because if
     * month will be set to 1, day will be set to 1
     * @param year
     * @return a date object or null
     */
    private Date getLowDate(String year) {
        Date myDate = null;
        try {
            int yy = Integer.parseInt(year);
            if (yy > 1900 && yy < 2100) {
                Calendar myCal = Calendar.getInstance();
                myCal.set(yy, 0, 1, 0, 0);
                myDate = myCal.getTime();
            }
        } catch (Exception ex) {
        }
        return myDate;
    }

    private Date getHighDate(String year) {
        Date myDate = null;
        try {
            int yy = Integer.parseInt(year);
            if (yy > 1900 && yy < 2100) {
                Calendar myCal = Calendar.getInstance();
                myCal.set(yy, 11, 31, 23, 59);
                myDate = myCal.getTime();
            }
        } catch (Exception ex) {
        }
        return myDate;
    }

    /**
     * Generates the Look&Feel code for the modal popup dialogue box
     * @param errorType The type of error
     * @param errorMessage The error message
     * @return Returns a string of HTML necessary to display the error message
     */
    public static String writeModalErrorMessages(String errorType, String errorMessage) {
        String commonHeader = "" + "<div style='width: 100%; text-align:right'>" + "<a href='#' title='Close' class='simplemodal-close' style='font-size:23px;'><b>x</b></a>" + "</div>" + "<h3 style='margin-bottom: 0px'>";
        String commonFooter = "<br><input type='button' value='Ok' class='simplemodal-close'/>";
        String errorString = commonHeader + errorType + "</h3><br>" + errorMessage + commonFooter;
        return errorString;
    }

    /**
     * Increases the amount of parameters available for generating modal error messages for checkout feature
     * @param errorType The type of error
     * @param errorMessage the error message
     * @param errors an ArrayList of error messages that will be shown
     * @param additionalComments Any suggestions on remedying the error
     * @return Returns a string of HTML necessary to display the error message
     */
    public static String checkOutErrorHelper(String errorType, String errorMessage, ArrayList errors, String additionalComments) {
        Iterator iter = errors.iterator();
        while (iter.hasNext()) {
            String tempString = (String) iter.next();
            errorMessage += tempString + "<br>";
        }
        errorMessage += additionalComments;
        String errorString = Util.writeModalErrorMessages(errorType, errorMessage);
        return errorString;
    }
}
