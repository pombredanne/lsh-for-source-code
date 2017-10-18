package jard.webshop.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

/**
 *
 * @author CJP
 */
public class Utils {

    public static String hash(String str) {
        MessageDigest summer;
        try {
            summer = MessageDigest.getInstance("md5");
            summer.update(str.getBytes());
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        BigInteger hash = new BigInteger(1, summer.digest());
        String hashword = hash.toString(16);
        return hashword;
    }

    public static void growl(String type, String message) {
        System.out.println("growling");
        FacesContext context = FacesContext.getCurrentInstance();
        if (!message.isEmpty()) {
            System.out.println("Had a message to display: " + message);
            context.addMessage(null, new FacesMessage(type, message));
        } else {
            System.out.println("Had no message to display.");
        }
    }
}
