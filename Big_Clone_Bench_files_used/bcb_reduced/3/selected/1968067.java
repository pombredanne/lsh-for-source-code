package org.exoplatform.mail.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Locale;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by The eXo Platform SAS
 * Author : Phung Nam
 *          phunghainam@gmail.com
 * Mar 15, 2008  
 */
public class MimeMessageParser {

    private static final Log logger = LogFactory.getLog(MimeMessageParser.class);

    public static Calendar getReceivedDate(javax.mail.Message msg) throws Exception {
        Calendar gc = GregorianCalendar.getInstance();
        if (msg.getReceivedDate() != null) {
            gc.setTime(msg.getReceivedDate());
        } else {
            gc.setTime(getDateHeader(msg));
        }
        return gc;
    }

    private static Date getDateHeader(javax.mail.Message msg) throws MessagingException {
        Date today = new Date();
        String[] received = msg.getHeader("received");
        if (received != null) for (int i = 0; i < received.length; i++) {
            String dateStr = null;
            try {
                dateStr = getDateString(received[i]);
                if (dateStr != null) {
                    Date msgDate = parseDate(dateStr);
                    if (!msgDate.after(today)) return msgDate;
                }
            } catch (ParseException ex) {
            }
        }
        String[] dateHeader = msg.getHeader("date");
        if (dateHeader != null) {
            String dateStr = dateHeader[0];
            try {
                Date msgDate = parseDate(dateStr);
                if (!msgDate.after(today)) return msgDate;
            } catch (ParseException ex) {
            }
        }
        return today;
    }

    private static String getDateString(String text) {
        String[] daysInDate = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        int startIndex = -1;
        for (int i = 0; i < daysInDate.length; i++) {
            startIndex = text.lastIndexOf(daysInDate[i]);
            if (startIndex != -1) break;
        }
        if (startIndex == -1) {
            return null;
        }
        return text.substring(startIndex);
    }

    private static Date parseDate(String dateStr) throws ParseException {
        dateStr = dateStr.replaceAll("\r\n", "");
        SimpleDateFormat dateFormat;
        try {
            dateFormat = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss Z", Locale.ENGLISH);
            return dateFormat.parse(dateStr);
        } catch (ParseException e) {
            try {
                dateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                return dateFormat.parse(dateStr);
            } catch (ParseException ex) {
                try {
                    dateFormat = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH);
                    return dateFormat.parse(dateStr);
                } catch (ParseException exx) {
                    try {
                        dateFormat = new SimpleDateFormat("EEE, d MMM yy HH:mm:ss", Locale.ENGLISH);
                        return dateFormat.parse(dateStr.substring(0, dateStr.lastIndexOf(":") + 2));
                    } catch (ParseException exxx) {
                        System.out.println(" [WARNING] Cannot parse date time from message: " + dateStr);
                        return null;
                    }
                }
            }
        }
    }

    public static long getPriority(javax.mail.Message message) throws MessagingException {
        MimeMessage msg = (MimeMessage) message;
        String xpriority = msg.getHeader("Importance", null);
        if (xpriority != null) {
            xpriority = xpriority.toLowerCase();
            if (xpriority.indexOf("high") == 0) {
                return Utils.PRIORITY_HIGH;
            } else if (xpriority.indexOf("low") == 0) {
                return Utils.PRIORITY_LOW;
            } else {
                return Utils.PRIORITY_NORMAL;
            }
        }
        xpriority = msg.getHeader("X-Priority", null);
        if (xpriority != null) {
            xpriority = xpriority.toLowerCase();
            if (xpriority.indexOf("1") == 0 || xpriority.indexOf("2") == 0) {
                return Utils.PRIORITY_HIGH;
            } else if (xpriority.indexOf("4") == 0 || xpriority.indexOf("5") == 0) {
                return Utils.PRIORITY_LOW;
            } else {
                return Utils.PRIORITY_NORMAL;
            }
        }
        return Utils.PRIORITY_NORMAL;
    }

    public static String getMessageId(javax.mail.Message message) throws Exception {
        String[] msgIdHeaders;
        try {
            msgIdHeaders = message.getHeader("Message-ID");
            if (msgIdHeaders != null && msgIdHeaders[0] != null) return msgIdHeaders[0];
        } catch (Exception e) {
        }
        return "";
    }

    /**
   * @return a MD5 string
   */
    public static String getMD5MsgId(javax.mail.Message msg) throws Exception {
        String key = "";
        long t1 = System.currentTimeMillis();
        Enumeration enu = msg.getAllHeaders();
        while (enu.hasMoreElements()) {
            Header header = (Header) enu.nextElement();
            key += header.getValue();
        }
        String md5 = getMD5(key);
        long t2 = System.currentTimeMillis();
        logger.error("getMD5MsgId spending time : " + (t2 - t1) + "ms");
        return md5;
    }

    /**
   * separated getMD5 method ... for a general use.
   * @param s
   * @return a MD5 string
   */
    public static String getMD5(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(), 0, s.length());
            return "" + new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            logger.error("MD5 is not supported !!!");
        }
        return s;
    }

    public static String getInReplyToHeader(javax.mail.Message message) throws Exception {
        String[] inReplyToHeaders = message.getHeader("In-Reply-To");
        if (inReplyToHeaders != null && inReplyToHeaders[0] != null) return inReplyToHeaders[0];
        return "";
    }

    public static String[] getReferencesHeader(javax.mail.Message message) throws Exception {
        String[] references = message.getHeader("References");
        return references;
    }

    public static String[] getInvitationHeader(javax.mail.Message message) throws Exception {
        String[] exoInvitationHeaders = message.getHeader("X-Exo-Invitation");
        if (exoInvitationHeaders != null) return exoInvitationHeaders;
        return null;
    }
}
