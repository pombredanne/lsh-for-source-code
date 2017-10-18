package com.ehsunbehravesh.mypasswords;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class Utils {

    public static String getRestOfStrings(String small, String big) {
        big = big.toLowerCase();
        small = small.toLowerCase();
        if (big.indexOf(small) == 0 && small.length() < big.length()) {
            return big.substring(small.length());
        }
        return "";
    }

    public static void openURL(String url) {
        if (!java.awt.Desktop.isDesktopSupported()) {
            return;
        }
        java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
        if (!desktop.isSupported(java.awt.Desktop.Action.BROWSE)) {
            return;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            desktop.browse(uri);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public static String getFileExtension(File file) {
        String filename = file.getName().toLowerCase();
        return filename.substring(filename.lastIndexOf(".") + 1);
    }

    public static void setCenterOfScreen(JDialog dialog) {
        Dimension size = dialog.getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Point position = new Point(screenSize.width / 2 - (size.width / 2), screenSize.height / 2 - (size.height / 2));
        dialog.setLocation(position);
    }

    public static void setCenterOfParent(JFrame parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x + (parentSize.width / 2 - size.width / 2), parentPosition.y + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);
    }

    public static void setCenterOfParent(JDialog parent, JDialog dialog) {
        Point parentPosition = parent.getLocation();
        Dimension parentSize = parent.getSize();
        Dimension size = dialog.getSize();
        Point position = new Point(parentPosition.x + (parentSize.width / 2 - size.width / 2), parentPosition.y + (parentSize.height / 2 - size.height / 2));
        dialog.setLocation(position);
    }

    public static String encrypt(String key, String text) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] byteKey = key.getBytes("UTF8");
        byte[] byteText = text.getBytes("UTF8");
        Cipher c = Cipher.getInstance("AES");
        SecretKeySpec k = new SecretKeySpec(byteKey, "AES");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] byteEncrypted = c.doFinal(byteText);
        String encrypted = new String(Base64Coder.encode(byteEncrypted));
        return encrypted;
    }

    public static byte[] encrypt(String key, byte[] byteText) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] byteKey = key.getBytes("UTF8");
        Cipher c = Cipher.getInstance("AES");
        SecretKeySpec k = new SecretKeySpec(byteKey, "AES");
        c.init(Cipher.ENCRYPT_MODE, k);
        byte[] byteEncrypted = c.doFinal(byteText);
        return byteEncrypted;
    }

    public static String decrypt(String key, String encrypted) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        byte[] byteKey = key.getBytes("UTF8");
        byte[] byteEncrypted = Base64Coder.decode(encrypted);
        Cipher c = Cipher.getInstance("AES");
        SecretKeySpec k = new SecretKeySpec(byteKey, "AES");
        c.init(Cipher.DECRYPT_MODE, k);
        byte[] byteText = c.doFinal(byteEncrypted);
        String text = new String(byteText);
        return text;
    }

    public static byte[] decrypt(String key, byte[] byteEncrypted) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        byte[] byteKey = key.getBytes("UTF8");
        Cipher c = Cipher.getInstance("AES");
        SecretKeySpec k = new SecretKeySpec(byteKey, "AES");
        c.init(Cipher.DECRYPT_MODE, k);
        byte[] byteText = c.doFinal(byteEncrypted);
        return byteText;
    }

    public static String encryptGeneral1(String value) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        String ekey = getGeneralKey();
        value = encrypt(ekey, value);
        value = String.format("%s%s%s", value.substring(0, 5), ekey, value.substring(5));
        return value;
    }

    public static String getGeneralKey() {
        String result = "";
        String letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
        Random random = new Random(System.currentTimeMillis());
        for (int i = 0; i < 16; i++) {
            result += letters.charAt(random.nextInt(letters.length()));
        }
        return result;
    }

    public static void setClipboardContents(String aString, ClipboardOwner owner) {
        StringSelection stringSelection = new StringSelection(aString);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, owner);
    }

    public static String getClipboardContents() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException ex) {
                System.out.println(ex);
            } catch (IOException ex) {
                System.out.println(ex);
            }
        }
        return result;
    }

    public static String decryptGeneral1(String value) throws UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException {
        String ekey = value.substring(5, 21);
        value = String.format("%s%s", value.substring(0, 5), value.substring(21));
        value = decrypt(ekey, value);
        return value;
    }

    public static String createEncryptionKey(String text) {
        String key = null;
        if (text.length() >= 8) {
            key = text.substring(0, 8);
            key = key + key;
        }
        return key;
    }

    private static String MD5(String message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        final byte[] data = message.getBytes(Charset.forName("UTF8"));
        final byte[] digest = messageDigest.digest(data);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            result.append(Integer.toHexString(0xFF & b));
        }
        return result.toString();
    }

    public static String SHA256(String message) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        final byte[] data = message.getBytes(Charset.forName("UTF8"));
        final byte[] digest = messageDigest.digest(data);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < digest.length; i++) {
            byte b = digest[i];
            result.append(Integer.toHexString(0xFF & b));
        }
        return result.toString();
    }

    public static String getPathOfJar() {
        String jarFilePath = new File(Utils.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getAbsolutePath();
        jarFilePath = jarFilePath.substring(0, jarFilePath.lastIndexOf(getFileSeparator()) + 1);
        return jarFilePath;
    }

    public static String getFileSeparator() {
        Properties sysProperties = System.getProperties();
        String fileSeparator = sysProperties.getProperty("file.separator");
        return fileSeparator;
    }

    public static int randomInt(int minimum, int maximum) {
        Random rn = new Random();
        int n = maximum - minimum + 1;
        int i = rn.nextInt() % n;
        return minimum + i;
    }

    public static String createRecoveryContent(String password) {
        try {
            password = encryptGeneral1(password);
            String data = URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
            URL url = new URL("https://mypasswords-server.appspot.com/recovery_file");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder finalResult = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                finalResult.append(line);
            }
            wr.close();
            rd.close();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(new StringReader(finalResult.toString())));
            document.normalizeDocument();
            Element root = document.getDocumentElement();
            String textContent = root.getTextContent();
            return textContent;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static String recoverPassword(String token) {
        try {
            token = encryptGeneral1(token);
            String data = URLEncoder.encode("key", "UTF-8") + "=" + URLEncoder.encode(token, "UTF-8");
            URL url = new URL("https://mypasswords-server.appspot.com/recover_password");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder finalResult = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                finalResult.append(line);
            }
            wr.close();
            rd.close();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(new StringReader(finalResult.toString())));
            document.normalizeDocument();
            Element root = document.getDocumentElement();
            String password = root.getTextContent();
            password = decryptGeneral1(password);
            return password;
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
