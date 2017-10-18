package hr.veleri.util;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * User: iivanovic
 * Date: 05.10.2010.
 * Time: 13:07:32
 */
public class Utilities {

    /**
     * private constructor, this class cannot be instantiated
     */
    private Utilities() {
    }

    public static String getMD5(final String text) {
        if (null == text) return null;
        final MessageDigest algorithm;
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        algorithm.reset();
        algorithm.update(text.getBytes());
        final byte[] digest = algorithm.digest();
        final StringBuffer hexString = new StringBuffer();
        for (byte b : digest) {
            String str = Integer.toHexString(0xFF & b);
            str = str.length() == 1 ? '0' + str : str;
            hexString.append(str);
        }
        return hexString.toString();
    }

    public static final FocusListener SELECT_ON_FOCUS_LISTENER = new FocusAdapter() {

        public void focusGained(final FocusEvent e) {
            final JTextComponent textComponent = (JTextComponent) e.getSource();
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    textComponent.setSelectionStart(0);
                    textComponent.setSelectionEnd(textComponent.getText().length());
                }
            });
        }
    };
}
