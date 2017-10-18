package org.paquitosoft.namtia.common;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.PropertyResourceBundle;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.apache.commons.codec.binary.Hex;

/**
 *
 * @author PaquitoSoft
 */
public class NamtiaUtilities {

    public static final String TRUE = "T";

    public static final String FALSE = "F";

    private static final String PREFERENCES_FILE_PATH = "resources/preferences.properties";

    public static Collection audioFormats;

    private static final String UNKNOWN_ARTIST = "others";

    private static final String IMAGE_FORMAT = "jpg";

    private static String COVERS_FOLDER = "namtia" + SystemValues.getFileSeparator() + "covers" + SystemValues.getFileSeparator();

    static {
        if (SystemValues.getOsName().toUpperCase().indexOf("WINDOWS") != -1) {
            COVERS_FOLDER = SystemValues.getUserHome() + SystemValues.getFileSeparator() + COVERS_FOLDER;
        } else {
            COVERS_FOLDER = SystemValues.getUserHome() + SystemValues.getFileSeparator() + "." + COVERS_FOLDER;
        }
        audioFormats = new ArrayList();
        audioFormats.add("mp3");
        audioFormats.add("ogg");
        audioFormats.add("wav");
    }

    /**
     * Creates a new instance of NamtiaUtilities
     */
    public NamtiaUtilities() {
    }

    /**
     *  In order to create an identifier we get the actual time in mili seconds
     *  and encrypt it. Then, we only keep 15 carachters of the result
     *  because that's the lenght of the identifiers in the database.
     *
     *  String -> identifier
     */
    public static String createIdentifier() {
        String miliSecs = String.valueOf(Calendar.getInstance().getTimeInMillis());
        MessageDigest cripter = null;
        try {
            cripter = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            e.getLocalizedMessage();
            return null;
        }
        byte[] a = cripter.digest(miliSecs.getBytes());
        byte[] b = new Hex().encode(a);
        return new String(b).substring(0, 15);
    }

    /**
     *  This method is used to collect any value from the preferences
     *  properties file.
     *  @parm String key
     *  @return String value
     */
    public static String getInternationalizedMessage(String key) {
        String value = null;
        try {
            File propertiesFile = new File(PREFERENCES_FILE_PATH);
            InputStream iStream = new FileInputStream(propertiesFile);
            PropertyResourceBundle props = new PropertyResourceBundle(iStream);
            value = (String) props.handleGetObject(key);
            iStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("WARNING: Problems when dealing with internationalization file!");
        }
        return value;
    }

    /**
     *  This method is used to save in the log file any message
     *  @param String -> message to write to log file
     */
    public static void log(String text) {
        text += "\n";
        System.out.println(text);
    }

    /**
     *  This method is used to get a String representing time
     *  (minutes and seconds) from an int
     *  @param int -> number of seconds
     *  @return String
     */
    public static String getDuration(int secs) {
        String result = "";
        int mins = (secs / 60);
        String minutes = "";
        if (mins < 10) {
            minutes += "0" + mins;
        } else {
            minutes += mins;
        }
        secs -= (mins * 60);
        String seconds = "";
        if (secs < 10) {
            seconds += "0" + secs;
        } else {
            seconds += secs;
        }
        result = "" + minutes + ":" + seconds;
        return result;
    }

    /**
     *  This method is used to know if a given String represents an integer
     *  number.
     *  @param String
     *  @return boolean -> <b>true</b> if String represents a number
     */
    public static boolean isNumber(String text) {
        try {
            Integer aux = new Integer(text);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     *  This method is used to insert a boolean object into a sql statement.
     *  This is done this way because Derby DB does not has a boolean data type.
     *  @param PreparedStatement -> query
     *  @param int -> index
     *  @param boolean -> value
     */
    public static void insertBooleanIntoQuery(PreparedStatement query, int index, boolean value) throws SQLException {
        if (value) {
            query.setString(index, TRUE);
        } else {
            query.setString(index, FALSE);
        }
    }

    /**
     *  This method is used to insert a boolean object into a sql statement.
     *  This is done this way because Derby DB does not has a boolean data type.
     *  @param PreparedStatement -> query
     *  @param int -> index
     *  @param Boolean -> value
     */
    public static void insertBooleanIntoQuery(PreparedStatement query, int index, Boolean value) throws SQLException {
        if (value.booleanValue()) {
            query.setString(index, TRUE);
        } else {
            query.setString(index, FALSE);
        }
    }

    /**
     *  This method is used to convert a bbdd boolean into a java primitive boolean.
     *  Beacuse derby db does not support boolean data type, we use a String instead
     *  @param PreparedStatement -> query
     *  @param int -> index
     *  @return boolean -> value
     */
    public static boolean getPrimitiveBooleanFromDerbyResultSet(ResultSet rs, int index) throws SQLException {
        String result = rs.getString(index);
        if (result == null) {
            return false;
        } else if (result.toUpperCase().equals(TRUE)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     *  This method is used to convert a bbdd boolean into a java boolean object.
     *  Beacuse derby db does not support Boolean data type, we use a String instead
     *  @param PreparedStatement -> query
     *  @param int -> index
     *  @return Boolean -> value
     */
    public static Boolean getBooleanObjectFromDerbyResultSet(ResultSet rs, int index) throws SQLException {
        String result = rs.getString(index);
        if (result == null) {
            return null;
        } else if (result.toUpperCase().equals(TRUE)) {
            return new Boolean(true);
        } else {
            return new Boolean(false);
        }
    }

    /**
     *  This method is used to get a picture from internet.
     *  @param String -> picture's URL
     *  @return ImageIcon -> picture
     */
    public static ImageIcon createImage(String url) {
        ImageIcon result = null;
        try {
            URL address = new URL(url);
            URLConnection urlCon = address.openConnection();
            InputStream iStream = urlCon.getInputStream();
            int contentLength = urlCon.getContentLength();
            byte[] content = new byte[contentLength];
            int i = 0;
            while (i < contentLength) {
                int bytesReaded = iStream.read(content, i, contentLength - i);
                if (bytesReaded > 0) {
                    i += bytesReaded;
                } else {
                    return null;
                }
            }
            iStream.close();
            result = new ImageIcon(content);
        } catch (Exception e) {
            System.out.println("WARNING: NamtiaUtilities.createImage() -> " + e.getMessage());
        }
        return result;
    }

    /**
     *  This method is used to save an image into disc.
     *  A file absolute path is required
     *  @param Image image -> image to be saved
     *  @param String imagePath -> an absolute path to the file where we want
     *              to save the image
     *  @throws Exception
     */
    public static boolean saveImage(Image image, String imagePath) {
        boolean result = false;
        if (image != null && imagePath != null && imagePath.length() > 4) {
            BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
            bufferedImage.createGraphics().drawImage(image, 0, 0, null);
            File imageFile = new File(imagePath);
            imageFile.mkdirs();
            try {
                ImageIO.write(bufferedImage, "jpeg", imageFile);
                result = true;
            } catch (IOException e) {
            }
        }
        return result;
    }

    /**
     *  This method constructs an absolute path for a image
     *  to be saved, based in artist and album names
     *  @param String artistName
     *  @param String albumName
     *  @return String -> absolute path for the image to be saved
     */
    public static String getCoverPath(String artistName, String albumName) {
        if (artistName == null || artistName.trim().length() < 1) {
            artistName = UNKNOWN_ARTIST;
        }
        if (albumName == null || albumName.trim().length() < 1) {
            Calendar cal = Calendar.getInstance();
            albumName = "cover(" + cal.get(Calendar.DATE) + "-" + cal.get(Calendar.MONTH) + "-" + cal.get(Calendar.YEAR) + ": " + cal.get(Calendar.SECOND) + ")";
        }
        String imagePath = COVERS_FOLDER + artistName.trim() + SystemValues.getFileSeparator() + albumName.trim() + "." + IMAGE_FORMAT;
        return imagePath;
    }

    /**
     *  This method is used to copy a file.
     *  @param File sourceFile
     *  @param File destFile
     *  @throws IOException
     */
    public static void copyFile(File sourceFile, File destFile) throws IOException {
        copyFile(sourceFile.getAbsolutePath(), destFile.getAbsolutePath());
    }

    /**
     *  This method is used to copy a file.
     *  @param String sourceFilePath
     *  @param String destFilePath
     *  @throws IOException
     */
    public static void copyFile(String sourceFilePath, String destFilePath) throws IOException {
        FileChannel in = null;
        FileChannel out = null;
        try {
            in = new FileInputStream(sourceFilePath).getChannel();
            out = new FileOutputStream(destFilePath).getChannel();
            long inputSize = in.size();
            in.transferTo(0, inputSize, out);
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, inputSize);
            out.write(buf);
        } finally {
            if (in != null) in.close();
            if (out != null) out.close();
        }
    }
}
