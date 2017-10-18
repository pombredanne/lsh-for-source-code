package org.hyperimage.client;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import javax.swing.JOptionPane;
import org.hyperimage.client.gui.HIClientGUI;

/**
 * 
 * Class: HIRuntime
 * Package: org.hyperimage.client
 * @author Jens-Martin Loebel
 *
 */
public abstract class HIRuntime {

    private static final String clientVersion = "2.0";

    private static final String svnRev = "$Rev: 153 $";

    public static final int MAX_GROUP_ITEMS = 100;

    public static final long MINIMUM_FREE_MEMORY = (1024 * 1024 * 5);

    public static final Locale[] supportedLanguages = { new Locale("de"), new Locale("en") };

    private static Locale guiLanguage = supportedLanguages[0];

    private static HIWebServiceManager manager;

    private static HIClientGUI gui;

    private static String clipboard;

    /**
	 * Report all fatal errors to the user.
	 * Since we probably don´t have a GUI (anymore) at this stage
	 * display errors to user in a standard JOption pane. All errors reported
	 * are fatal. As the client cannot recover from those --> exit gracefully. 
	 * 
	 * @param message error message to display
	 */
    public static void displayFatalErrorAndExit(String message) {
        JOptionPane.showMessageDialog(null, "FATAL ERROR!\n" + message, "HyperImage Client: Fatal Error", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    public static HIClientGUI getGui() {
        return gui;
    }

    public static void setGui(HIClientGUI gui) {
        HIRuntime.gui = gui;
    }

    public static HIWebServiceManager getManager() {
        return manager;
    }

    public static void setManager(HIWebServiceManager manager) {
        HIRuntime.manager = manager;
    }

    public static void copyToClipboard(String contents) {
        clipboard = contents;
    }

    public static String pasteFromClipboard() {
        return clipboard;
    }

    public static void emptyClipboard() {
        clipboard = null;
    }

    public static boolean isClipboardEmpty() {
        if (clipboard == null) return true; else if (clipboard.length() == 0) return true;
        return false;
    }

    public static String getClientVersion() {
        if (svnRev.split(" ").length != 3) return clientVersion + ".unknown";
        return clientVersion + "." + svnRev.split(" ")[1];
    }

    public static byte[] getBytesFromFile(File file) throws IOException {
        InputStream is = new FileInputStream(file);
        long length = file.length();
        byte[] bytes = new byte[(int) length];
        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }
        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file);
        }
        is.close();
        return bytes;
    }

    public static String getMD5HashString(byte[] inputData) {
        String hashString = "";
        MessageDigest md5;
        byte[] digest;
        int curNum;
        final char[] hexTable = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(inputData);
            digest = md5.digest();
            for (int i = 0; i < digest.length; i++) {
                curNum = digest[i];
                if (curNum < 0) {
                    curNum = curNum + 256;
                }
                hashString = hashString + hexTable[curNum / 16] + hexTable[curNum % 16];
            }
        } catch (NoSuchAlgorithmException e) {
            System.out.println("MD5 not supported by current Java VM!");
            System.exit(1);
        }
        return hashString;
    }

    public static int getModifierKey() {
        if (System.getProperty("os.name").toLowerCase().indexOf("mac") != -1) return ActionEvent.META_MASK;
        return ActionEvent.CTRL_MASK;
    }

    public static Locale getGUILanguage() {
        return guiLanguage;
    }

    public static void setGUILanguage(Locale language) {
        guiLanguage = language;
        Messages.updateDefaultLanguage(language);
    }
}
