package mpower_export;

import java.math.*;
import java.security.*;
import mpower_hibernate.User;
import mpower_hibernate.UserFasade;

public class Hash {

    /**
     * Generates the MD5 hash of a given String
     * 
     * @param s Represents the to be hashed value
     * @return String with the hash value
     */
    public static String MD5(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update(s.getBytes(), 0, s.length());
            return new BigInteger(1, m.digest()).toString(16);
        } catch (NoSuchAlgorithmException ex) {
            return "";
        }
    }

    /**
     * Generates a hash for the given user by hashing "<userid>|<userpassword>"
     * Used for authentication in the CalendarSubscription module
     * So if the user password changes, the calendar subscriptions won't work any more
     * 
     * @param UserId Specifies the 
     * @return String with the hash value
     */
    public static String GetHashForUser(Long UserId) {
        UserFasade userfacade = new UserFasade();
        User user = userfacade.findById(UserId);
        if (user != null) {
            return Hash.MD5(UserId.toString() + "|" + user.getPassword().toString());
        } else {
            return "";
        }
    }
}
