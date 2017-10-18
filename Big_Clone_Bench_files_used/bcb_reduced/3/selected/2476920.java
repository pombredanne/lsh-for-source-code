package net.zcarioca.zscrum.domain.util;

import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * A collection of static methods which simplify the development of domain level
 * tasks.
 * 
 * 
 * @author zcarioca
 */
public final class DomainUtils {

    private static final Logger logger = Logger.getLogger(DomainUtils.class);

    private static final char[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static final String emailPattern = "^[\\w\\-]([\\.\\w\\-\\+])+[\\w]+@([\\w\\-]+\\.)+[a-zA-Z]{2,4}$";

    /**
    * Determines whether the supplied string is a valid email address.
    * 
    * @param email The email to check.
    * @return Returns <code>true</code> if this is a valid email address;
    *         <code>false</code> if otherwise.
    */
    public static final boolean isEmailAddress(String email) {
        return email.matches(emailPattern);
    }

    /**
    * Converts a string into a SHA1 hash.
    * 
    * @param password The password to convert.
    * @return Returns the SHA1 hash.
    */
    public static final String toHash(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA1");
            byte[] binary = digest.digest(password.getBytes());
            return toHex(binary);
        } catch (Throwable t) {
            logger.warn(String.format("Could not convert password '%s' to a SHA1 hash: %s", password, t.getMessage()));
        }
        return null;
    }

    /**
    * Gets the next weekday.
    * 
    * @param date The date to start with.
    * @return Returns the next weekday.
    */
    public static final Date nextWeekday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DATE, 1);
        while (!isWeekday(cal)) {
            cal.add(Calendar.DATE, 1);
        }
        return cal.getTime();
    }

    /**
    * Gets the next weekday.
    * 
    * @param date The date to start with.
    * @param additionalDays The number of weekdays to add.
    * @return Returns the next weekday.
    */
    public static final Date addWeekdays(Date date, int additionalDays) {
        for (int i = 0; i < additionalDays; i++) {
            date = nextWeekday(date);
        }
        return date;
    }

    /**
    * Determines whether the supplied date is a weekday.
    * 
    * @param date The date to check.
    * @return Returns true if the date is a weekday.
    */
    public static boolean isWeekday(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return false;
        }
        return true;
    }

    private static boolean isWeekday(Calendar cal) {
        int day = cal.get(Calendar.DAY_OF_WEEK);
        if (day == Calendar.SATURDAY || day == Calendar.SUNDAY) {
            return false;
        }
        return true;
    }

    private static final String toHex(byte[] data) {
        StringBuffer sb = new StringBuffer(data.length * 2);
        for (byte b : data) {
            sb.append(digits[(b >> 4) & 0x0f]);
            sb.append(digits[b & 0x0f]);
        }
        return sb.toString();
    }
}
