package ru.newton.pokertrainer.utils;

import ru.newton.pokertrainer.web.constants.Constants;
import javax.servlet.http.HttpServletRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.Iterator;

/**
 * Global application utilities.
 *
 * @author echo
 */
public class Utils {

    public abstract static class RunnableWithParam<T> implements Runnable {

        private T object;

        public T getObject() {
            return object;
        }

        public void setObject(final T object) {
            this.object = object;
        }

        private int index;

        public int getIndex() {
            return index;
        }

        public void setIndex(final int index) {
            this.index = index;
        }
    }

    public static <T> void run(final Iterable<T> iterable, final RunnableWithParam<T> runnable) {
        int index = 0;
        for (Iterator<T> iterator = iterable.iterator(); iterator.hasNext(); index++) {
            T object = iterator.next();
            runnable.setObject(object);
            runnable.setIndex(index);
            runnable.run();
        }
    }

    public static <T> void run(final T[] objects, final RunnableWithParam<T> runnable) {
        for (int index = 0; index < objects.length; index++) {
            T object = objects[index];
            runnable.setObject(object);
            runnable.setIndex(index);
            runnable.run();
        }
    }

    private static final String MD5 = "MD5";

    private static final char DIGITS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    public static String encodePassword(final String password) {
        String encodedPassword = "";
        try {
            MessageDigest md5 = MessageDigest.getInstance(MD5);
            byte[] original = new byte[] {};
            if (password != null) {
                original = password.getBytes();
            }
            byte[] bytes = md5.digest(original);
            for (byte b : bytes) {
                encodedPassword = encodedPassword + DIGITS[(b >> 4) & 0x0F] + DIGITS[b & 0x0F];
            }
        } catch (NoSuchAlgorithmException ex) {
            encodedPassword = password;
        }
        return encodedPassword;
    }

    public static String getCurrentUserName(final HttpServletRequest request) {
        String userName = (String) request.getSession().getAttribute(Constants.USER_PRINCIPAL_NAME_ATTR_NAME);
        if (userName == null) {
            Principal userPrincipal = request.getUserPrincipal();
            if (userPrincipal != null) {
                userName = userPrincipal.getName();
            }
        }
        return userName;
    }
}
