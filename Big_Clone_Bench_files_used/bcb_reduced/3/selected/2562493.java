package org.zurell.java.SortImagesGUI;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;
import javax.swing.SwingUtilities;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectory;

public class ImageSorter extends Thread {

    private GUI parent;

    private HashMap files;

    /** A url to connect to the database. */
    private String dbURL = "jdbc:mysql://localhost:3306/jimages";

    private static String sep = System.getProperty("file.separator", "/");

    /** The Connection to the database. */
    private Connection conn;

    private Properties properties;

    private boolean UseDB = false;

    public ImageSorter(GUI gui, Properties props, HashMap files) {
        parent = gui;
        this.files = files;
        properties = props;
        if (props.getProperty("USEDB").equalsIgnoreCase("true")) {
            initDatabase();
            UseDB = true;
        }
    }

    public void run() {
        Set keys = files.keySet();
        Iterator iter = keys.iterator();
        int progress = 0;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                parent.setallProgress(0);
            }
        });
        while (iter.hasNext()) {
            progress++;
            final int percent = progress;
            final File tmp = (File) files.get(iter.next());
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    parent.setfileLabel("File: " + tmp.getName());
                }
            });
            doFile(tmp);
            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    parent.setallProgress(percent);
                }
            });
        }
        if (properties.getProperty("DUMPDB").equalsIgnoreCase("true")) {
            dumpDB();
            parent.message("Database dumped!");
        }
        parent.message("####### FINISHED #######");
    }

    private boolean dumpDB() {
        Statement stmt;
        String SQL = "SELECT * FROM " + properties.getProperty("DBTABLE");
        String output = new String();
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            SQLWarning warn = stmt.getWarnings();
            while (warn != null) {
                parent.message("SQLState: " + warn.getSQLState());
                parent.message("Message: " + warn.getMessage());
                parent.message("Vendor: " + warn.getErrorCode());
                parent.message("");
                warn = warn.getNextWarning();
            }
            ResultSetMetaData rsm = rs.getMetaData();
            rs.beforeFirst();
            output = output.concat("/*!40000 ALTER TABLE `md5sums` DISABLE KEYS */;\nLOCK TABLES `md5sums` WRITE;\n");
            while (rs.next()) {
                output = output.concat("INSERT INTO " + properties.getProperty("DBTABLE") + " VALUES (");
                for (int i = 1; i <= rsm.getColumnCount(); i++) {
                    if (i < rsm.getColumnCount()) {
                        output = output.concat("\'" + rs.getString(i) + "\',");
                    } else {
                        output = output.concat("\'" + rs.getString(i) + "\'");
                    }
                }
                output = output.concat(");\n");
            }
            output = output.concat("UNLOCK TABLES;\n/*!40000 ALTER TABLE `md5sums` ENABLE KEYS */;\n");
            File dumpFile = new File(properties.getProperty("PHOTODIR") + sep + "autodump.mysql");
            try {
                new FileOutputStream(dumpFile).write(output.getBytes());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            return true;
        } catch (SQLException e) {
            parent.message("[MySQLDBConnector] SQL-Error during insert: " + e.toString());
            return false;
        }
    }

    public static String calculateMD5SUM(File Filename) {
        String MD5SUM = new String();
        ;
        try {
            MessageDigest md = MessageDigest.getInstance("md5");
            FileInputStream in = new FileInputStream(Filename);
            int len;
            byte[] data = new byte[1024];
            while ((len = in.read(data)) > 0) {
                md.update(data, 0, len);
            }
            in.close();
            byte[] result = md.digest();
            for (int i = 0; i < result.length; ++i) {
                MD5SUM = MD5SUM + toHexString(result[i]);
            }
        } catch (Exception e) {
            System.err.println("[MD5Calculator] MD5-Fehler: " + e.toString());
        }
        return MD5SUM;
    }

    public static String toHexString(byte b) {
        int value = (b & 0x7F) + (b < 0 ? 128 : 0);
        String ret = (value < 16 ? "0" : "");
        ret += Integer.toHexString(value).toUpperCase();
        return ret;
    }

    private boolean copyFile(ImagePile file) {
        if (!UseDB) {
            if (file.getNewFilename().exists()) {
                String newmd5 = calculateMD5SUM(file.getNewFilename());
                System.err.println(file.getNewFilename().getName() + " " + newmd5 + " -> " + file.getFileName().getName() + " " + file.getMD5SUM());
                if (newmd5.equals(file.getMD5SUM())) {
                    parent.message("File already exist:" + file.getFileName().getName());
                    return false;
                }
            }
        }
        while (file.getNewFilename().exists()) {
            String Filename = "_" + file.getNewFilename().getName();
            file.setNewFilename(new File(file.getNewFilename().getParent() + sep + Filename));
            Filename = null;
        }
        File tmpPath = file.getNewFilename().getParentFile();
        LinkedList dirList = new LinkedList();
        while (!tmpPath.exists()) {
            dirList.add(tmpPath);
            tmpPath = tmpPath.getParentFile();
        }
        while (!dirList.isEmpty()) {
            File newDir = (File) dirList.removeLast();
            if (newDir.mkdir()) {
            } else {
                System.err.println("[FileMover] Fatal Error creating target directory.");
                System.exit(1);
            }
        }
        BufferedInputStream is;
        BufferedOutputStream os;
        try {
            is = new BufferedInputStream(new FileInputStream(file.getFileName()));
            os = new BufferedOutputStream(new FileOutputStream(file.getNewFilename()));
            int b;
            while ((b = is.read()) != -1) {
                os.write(b);
            }
            is.close();
            os.close();
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean initDatabase() {
        conn = null;
        String dbURL = "jdbc:mysql://" + properties.getProperty("DBHOST") + ":" + properties.getProperty("DBPORT") + "/" + properties.getProperty("DBNAME");
        try {
            Class c = Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(dbURL, properties.getProperty("DBUSER"), properties.getProperty("DBPASSWD"));
            SQLWarning warn = conn.getWarnings();
            while (warn != null) {
                warn = warn.getNextWarning();
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Database driver class not found.");
            System.err.println(e.toString());
            System.exit(1);
        } catch (SQLException e) {
            System.err.println("Database error: " + e.toString());
            System.exit(1);
        }
        return true;
    }

    public boolean insert(ImagePile image) {
        Statement stmt;
        String SQL = "INSERT INTO " + properties.getProperty("DBTABLE") + " (md5,cameramodel, cameramaker, capturedate, width, height, flash, aperture_time, exposure_time, focal_length, location, original_name) ";
        StringBuffer mySQLDate = new StringBuffer();
        SimpleDateFormat formatter = new SimpleDateFormat();
        formatter.applyPattern("yyyy-MM-dd HH:mm:ss");
        try {
            SQL = SQL + "VALUES ('" + image.getMD5SUM() + "','" + image.getCameraModel() + "','" + image.getCameraMaker() + "','" + formatter.format(image.getImageDate()) + "','" + image.getWidth() + "','" + image.getHeight() + "','" + image.getFlashUsed() + "','" + image.getApertureTime() + "','" + image.getExposureTime() + "','" + image.getFocalLength() + "','" + image.getNewFilename().getAbsolutePath() + "','" + image.getFileName().getName() + "');";
            stmt = conn.createStatement();
            SQL = adaptString(SQL);
            stmt.execute(SQL);
            SQLWarning warn = stmt.getWarnings();
            while (warn != null) {
                parent.message("SQLState: " + warn.getSQLState());
                parent.message("Message: " + warn.getMessage());
                parent.message("Vendor: " + warn.getErrorCode());
                parent.message("");
                warn = warn.getNextWarning();
            }
            return true;
        } catch (SQLException e) {
            parent.message("[MySQLDBConnector] SQL-Error during insert: " + e.toString());
            return false;
        }
    }

    private String adaptString(String input) {
        String output;
        if (System.getProperty("os.name").matches(".*indows.*")) {
            output = input.replaceAll("\\\\", "\\\\\\\\");
        } else {
            output = input;
        }
        return output;
    }

    /**
	 * Examines if an given image identified by its MD5 hash is already in the
	 * database.
	 * 
	 * @param md5sum
	 *                  A String with the requested MD5 hash
	 * @return True if the image was found in the database. False if it wasn't
	 *              found in the database.
	 */
    public boolean isInDB(String md5sum) {
        Statement stmt;
        String SQL = "SELECT * FROM " + properties.getProperty("DBTABLE") + " WHERE MD5 = '" + md5sum + "';";
        try {
            stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(SQL);
            rs.last();
            if (rs.getRow() == 1) {
                rs.close();
                return true;
            } else {
                rs.close();
                return false;
            }
        } catch (SQLException e) {
            parent.message("[MySQLDBConnector] " + e.toString());
        }
        return false;
    }

    private boolean doFile(File file) {
        boolean success = true;
        ImagePile imageProperties;
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                parent.setfileProgress(0);
            }
        });
        imageProperties = getImageData(file);
        if (imageProperties == null) {
            return false;
        }
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                parent.setfileProgress(1);
            }
        });
        imageProperties.setMD5SUM(calculateMD5SUM(file));
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                parent.setfileProgress(2);
            }
        });
        if (UseDB) {
            if (this.isInDB(imageProperties.getMD5SUM())) {
                parent.message("Image " + imageProperties.getFileName().getName() + " is already in Database... skipping.");
                success = false;
            } else {
                if (this.copyFile(imageProperties)) {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            parent.setfileProgress(3);
                        }
                    });
                    if (this.insert(imageProperties)) {
                        SwingUtilities.invokeLater(new Runnable() {

                            public void run() {
                                parent.setfileProgress(4);
                            }
                        });
                    } else {
                        parent.message("Couldn't insert file into database");
                        return false;
                    }
                } else {
                    parent.message("Couldn't copy file");
                    return false;
                }
            }
        } else {
            if (this.copyFile(imageProperties)) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        parent.setfileProgress(3);
                    }
                });
            } else {
                parent.message("Couldn't copy file");
                return false;
            }
        }
        if (success) {
            parent.message("Finished File: " + file.getName() + " -> " + imageProperties.getNewFilename().getName());
        } else {
            parent.message("Finished File: " + file.getName() + " without action.");
        }
        return true;
    }

    private ImagePile getImageData(File filename) {
        ImagePile exifMetaData;
        File myFile = filename;
        exifMetaData = new ImagePile(myFile);
        Calendar cal = Calendar.getInstance();
        Date myDate = new Date();
        DateFormat df = DateFormat.getDateInstance();
        Metadata metadata = new Metadata();
        try {
            metadata = JpegMetadataReader.readMetadata(myFile);
        } catch (JpegProcessingException e) {
            parent.message(myFile.getName() + ": error in reading JPEG Metadata.\n\tSkipping this image");
            return null;
        }
        Directory exifDirectory = metadata.getDirectory(ExifDirectory.class);
        if (exifDirectory.containsTag(ExifDirectory.TAG_MAKE)) {
            exifMetaData.setCameraMaker(exifDirectory.getString(ExifDirectory.TAG_MAKE));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_MODEL)) {
            exifMetaData.setCameraModel(exifDirectory.getString(ExifDirectory.TAG_MODEL));
        }
        if (!exifDirectory.containsTag(ExifDirectory.TAG_APERTURE)) {
            if (exifDirectory.containsTag(ExifDirectory.TAG_MAX_APERTURE)) {
                exifMetaData.setApertureTime(exifDirectory.getString(ExifDirectory.TAG_MAX_APERTURE));
            }
        } else {
            exifMetaData.setApertureTime(exifDirectory.getString(ExifDirectory.TAG_APERTURE));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_FLASH)) {
            exifMetaData.setFlashUsed(exifDirectory.getString(ExifDirectory.TAG_FLASH));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_FOCAL_LENGTH)) {
            exifMetaData.setFocalLength(exifDirectory.getString(ExifDirectory.TAG_FOCAL_LENGTH));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_WIDTH)) {
            exifMetaData.setWidth(exifDirectory.getString(ExifDirectory.TAG_EXIF_IMAGE_WIDTH));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT)) {
            exifMetaData.setHeight(exifDirectory.getString(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT));
        }
        if (!exifDirectory.containsTag(ExifDirectory.TAG_SHUTTER_SPEED)) {
            if (exifDirectory.containsTag(ExifDirectory.TAG_EXPOSURE_TIME)) {
                exifMetaData.setExposureTime(exifDirectory.getString(ExifDirectory.TAG_EXPOSURE_TIME));
            }
        } else {
            exifMetaData.setExposureTime(exifDirectory.getString(ExifDirectory.TAG_SHUTTER_SPEED));
        }
        if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_DIGITIZED)) {
            try {
                myDate = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_DIGITIZED);
            } catch (MetadataException e1) {
                parent.message(e1.toString());
            }
        } else if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL)) {
            try {
                myDate = exifDirectory.getDate(ExifDirectory.TAG_DATETIME_ORIGINAL);
            } catch (MetadataException e1) {
                parent.message(e1.toString());
            }
        } else {
            myDate = new Date();
        }
        exifMetaData.setImageDate(myDate);
        cal.setTime(myDate);
        String Year = beautifyNumbers(cal.get(Calendar.YEAR));
        String Month = beautifyNumbers(cal.get(Calendar.MONTH) + 1);
        String Day = beautifyNumbers(cal.get(Calendar.DAY_OF_MONTH));
        String Hour = beautifyNumbers(cal.get(Calendar.HOUR_OF_DAY));
        String Minute = beautifyNumbers(cal.get(Calendar.MINUTE));
        String Second = beautifyNumbers(cal.get(Calendar.SECOND));
        String NewFilename = Year + "_" + Month + "_" + Day + "-" + Hour + "_" + Minute + "_" + Second + ".jpg";
        String NewPath = properties.getProperty("PHOTODIR") + sep + Year + sep + Month + sep + Day + sep + NewFilename;
        exifMetaData.setNewFilename(new File(NewPath));
        return exifMetaData;
    }

    private String beautifyNumbers(int input) {
        String output;
        if (Integer.toString(input).length() == 1) {
            output = "0" + input;
        } else {
            output = Integer.toString(input);
        }
        return output;
    }
}
