package com.bkoenig.photo.toolkit.utils;

import java.io.File;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.jdom.Element;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.GpsDirectory;

public class Photo implements Comparable<Photo> {

    private long lastValidationDate = 0;

    private String imagePath = null;

    private String imageHash = null;

    private String imageWidth = null;

    private String imageHeight = null;

    private String imageTags = null;

    private String imageISO = null;

    private String imageFormat = null;

    private String imageComment = null;

    private String imageRating = null;

    private String imageWebGallery = null;

    private String imageBelichtung = null;

    private String imageBlende = null;

    private String imageLichtMessung = null;

    private String imageLichtquelle = null;

    private String imageBlitz = null;

    private String imageFocalLength = null;

    private String imageFocalLength35mm = null;

    private String imageCameraModel = null;

    private String imageCameraMake = null;

    private String imageDateTimeFile = null;

    private String imageDateTimeOriginal = null;

    private String imageGPSLatitude = null;

    private String imageGPSLongitude = null;

    private static String XML_IMAGE_PATH = "path";

    private static String XML_IMAGE_HASH = "hash";

    private static String XML_IMAGE_WIDTH = "width";

    private static String XML_IMAGE_HEIGHT = "height";

    private static String XML_IMAGE_TAGS = "tags";

    private static String XML_IMAGE_ISO = "iso";

    private static String XML_IMAGE_FORMAT = "format";

    private static String XML_IMAGE_COMMENT = "comment";

    private static String XML_IMAGE_RATING = "rating";

    private static String XML_IMAGE_WEBPAGE = "web";

    private static String XML_BELICHTUNG = "belichtung";

    private static String XML_BLENDE = "blende";

    private static String XML_LICHTMESSUNG = "lichtmessung";

    private static String XML_LICHTQUELLE = "lichtquelle";

    private static String XML_BLITZ = "blitz";

    private static String XML_FOCALLENGTH = "focal";

    private static String XML_FOCALLENGTH_35MM = "focal35mm";

    private static String XML_CAMERA_MODEL = "camera";

    private static String XML_CAMERA_MAKE = "marke";

    private static String XML_DATETIME_FILE = "filedate";

    private static String XML_DATETIME_ORIGINAL = "exifdate";

    private static String XML_GPS_LATITUDE = "lat";

    private static String XML_GPS_LONGITUDE = "lon";

    public Photo(String dateiname) {
        File image = new File(dateiname);
        try {
            imagePath = image.getCanonicalPath();
            imageHash = fileName2md5(imagePath);
            imageDateTimeFile = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(image.lastModified()));
            if (imagePath.toLowerCase().endsWith(".jpg") || imagePath.toLowerCase().endsWith(".nef") || imagePath.toLowerCase().endsWith(".cr2")) {
                Metadata metadata = ImageMetadataReader.readMetadata(image);
                Directory exifDirectory = metadata.getDirectory(ExifDirectory.class);
                Directory gpsDirectory = metadata.getDirectory(GpsDirectory.class);
                if (exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT)) imageHeight = exifDirectory.getString(ExifDirectory.TAG_EXIF_IMAGE_HEIGHT).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_EXIF_IMAGE_WIDTH)) imageWidth = exifDirectory.getString(ExifDirectory.TAG_EXIF_IMAGE_WIDTH).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_ISO_EQUIVALENT)) imageISO = exifDirectory.getString(ExifDirectory.TAG_ISO_EQUIVALENT).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_ORIENTATION)) imageFormat = exifDirectory.getString(ExifDirectory.TAG_ORIENTATION).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_USER_COMMENT)) {
                    String[] tmpComment = new String(exifDirectory.getByteArray(ExifDirectory.TAG_USER_COMMENT)).substring(8).trim().split("#");
                    if (tmpComment != null && tmpComment.length > 0 && !tmpComment[0].trim().equals("")) imageComment = tmpComment[0].trim();
                    if (tmpComment != null && tmpComment.length > 1 && !tmpComment[1].trim().equals("")) imageRating = tmpComment[1].trim();
                    if (tmpComment != null && tmpComment.length > 2 && !tmpComment[2].trim().equals("")) imageWebGallery = tmpComment[2].trim();
                    if (tmpComment != null && tmpComment.length > 3 && !tmpComment[3].trim().equals("")) imageTags = tmpComment[3].trim();
                }
                if (exifDirectory.containsTag(ExifDirectory.TAG_EXPOSURE_TIME)) imageBelichtung = exifDirectory.getString(ExifDirectory.TAG_EXPOSURE_TIME).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_FNUMBER)) imageBlende = exifDirectory.getString(ExifDirectory.TAG_FNUMBER).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_METERING_MODE)) imageLichtMessung = exifDirectory.getString(ExifDirectory.TAG_METERING_MODE).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_LIGHT_SOURCE)) imageLichtquelle = exifDirectory.getString(ExifDirectory.TAG_LIGHT_SOURCE).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_EXPOSURE_TIME)) imageBlitz = exifDirectory.getString(ExifDirectory.TAG_FLASH).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_FOCAL_LENGTH)) imageFocalLength = exifDirectory.getString(ExifDirectory.TAG_FOCAL_LENGTH).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH)) imageFocalLength35mm = exifDirectory.getString(ExifDirectory.TAG_35MM_FILM_EQUIV_FOCAL_LENGTH).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_MODEL)) imageCameraModel = exifDirectory.getString(ExifDirectory.TAG_MODEL).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_MAKE)) imageCameraMake = exifDirectory.getString(ExifDirectory.TAG_MAKE).trim();
                if (exifDirectory.containsTag(ExifDirectory.TAG_DATETIME_ORIGINAL)) imageDateTimeOriginal = exifDirectory.getString(ExifDirectory.TAG_DATETIME_ORIGINAL).trim();
                if (exifDirectory.containsTag(GpsDirectory.TAG_GPS_LATITUDE)) imageGPSLatitude = gpsDirectory.getString(GpsDirectory.TAG_GPS_LATITUDE).trim();
                if (exifDirectory.containsTag(GpsDirectory.TAG_GPS_LONGITUDE)) imageGPSLongitude = gpsDirectory.getString(GpsDirectory.TAG_GPS_LONGITUDE).trim();
                if (imagePath.toLowerCase().endsWith(".jpg")) {
                } else if (imagePath.toLowerCase().endsWith(".nef")) {
                } else if (imagePath.toLowerCase().endsWith(".cr2")) {
                }
            } else {
                Logger.error("unknown file format: " + imagePath);
            }
        } catch (Exception ex) {
            Logger.error("Fehler beim Einlesen eines Photos.");
            Logger.debug(ex.getClass() + " " + ex.getMessage());
            for (int e = 0; e < ex.getStackTrace().length; e++) Logger.debug("     " + ex.getStackTrace()[e].toString());
            ex.printStackTrace();
        }
    }

    public Photo(Element input) throws Exception {
        imagePath = input.getAttributeValue(XML_IMAGE_PATH);
        if (imagePath.trim().equals("")) imagePath = null;
        imageHash = input.getAttributeValue(XML_IMAGE_HASH);
        if (imageHash.trim().equals("")) imageHash = null;
        imageWidth = input.getAttributeValue(XML_IMAGE_WIDTH);
        if (imageWidth.trim().equals("")) imageWidth = null;
        imageHeight = input.getAttributeValue(XML_IMAGE_HEIGHT);
        if (imageHeight.trim().equals("")) imageHeight = null;
        imageTags = input.getAttributeValue(XML_IMAGE_TAGS);
        if (imageTags.trim().equals("")) imageTags = null;
        imageISO = input.getAttributeValue(XML_IMAGE_ISO);
        if (imageISO.trim().equals("")) imageISO = null;
        imageFormat = input.getAttributeValue(XML_IMAGE_FORMAT);
        if (imageFormat.trim().equals("")) imageFormat = null;
        imageComment = input.getAttributeValue(XML_IMAGE_COMMENT);
        if (imageComment.trim().equals("")) imageComment = null;
        imageRating = input.getAttributeValue(XML_IMAGE_RATING);
        if (imageRating.trim().equals("")) imageRating = null;
        imageWebGallery = input.getAttributeValue(XML_IMAGE_WEBPAGE);
        if (imageWebGallery.trim().equals("")) imageWebGallery = null;
        imageBelichtung = input.getAttributeValue(XML_BELICHTUNG);
        if (imageBelichtung.trim().equals("")) imageBelichtung = null;
        imageBlende = input.getAttributeValue(XML_BLENDE);
        if (imageBlende.trim().equals("")) imageBlende = null;
        imageLichtMessung = input.getAttributeValue(XML_LICHTMESSUNG);
        if (imageLichtMessung.trim().equals("")) imageLichtMessung = null;
        imageLichtquelle = input.getAttributeValue(XML_LICHTQUELLE);
        if (imageLichtquelle.trim().equals("")) imageLichtquelle = null;
        imageBlitz = input.getAttributeValue(XML_BLITZ);
        if (imageBlitz.trim().equals("")) imageBlitz = null;
        imageFocalLength = input.getAttributeValue(XML_FOCALLENGTH);
        if (imageFocalLength.trim().equals("")) imageFocalLength = null;
        imageFocalLength35mm = input.getAttributeValue(XML_FOCALLENGTH_35MM);
        if (imageFocalLength35mm.trim().equals("")) imageFocalLength35mm = null;
        imageCameraModel = input.getAttributeValue(XML_CAMERA_MODEL);
        if (imageCameraModel.trim().equals("")) imageCameraModel = null;
        imageCameraMake = input.getAttributeValue(XML_CAMERA_MAKE);
        if (imageCameraMake.trim().equals("")) imageCameraMake = null;
        imageDateTimeFile = input.getAttributeValue(XML_DATETIME_FILE);
        if (imageDateTimeFile.trim().equals("")) imageDateTimeFile = null;
        imageDateTimeOriginal = input.getAttributeValue(XML_DATETIME_ORIGINAL);
        if (imageDateTimeOriginal.trim().equals("")) imageDateTimeOriginal = null;
        imageGPSLatitude = input.getAttributeValue(XML_GPS_LATITUDE);
        if (imageGPSLatitude.trim().equals("")) imageGPSLatitude = null;
        imageGPSLongitude = input.getAttributeValue(XML_GPS_LONGITUDE);
        if (imageGPSLongitude.trim().equals("")) imageGPSLongitude = null;
    }

    public boolean validate() {
        if (lastValidationDate + 30000 > System.currentTimeMillis()) {
            return false;
        } else {
            boolean changed = false;
            Photo tmpPhoto = new Photo(imagePath);
            if ((tmpPhoto.getImageWidth() != null && imageWidth == null) || (tmpPhoto.getImageWidth() != null && imageWidth != null && !imageWidth.equals(tmpPhoto.getImageWidth()))) {
                imageWidth = tmpPhoto.getImageWidth();
                Logger.info("file has been changed (IMAGE_WIDTH) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageHeight() != null && imageHeight == null) || (tmpPhoto.getImageHeight() != null && imageHeight != null && !imageHeight.equals(tmpPhoto.getImageHeight()))) {
                imageHeight = tmpPhoto.getImageHeight();
                Logger.info("file has been changed (IMAGE_HEIGHT) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageTags() != null && imageTags == null) || (tmpPhoto.getImageTags() != null && imageTags != null && !imageTags.equals(tmpPhoto.getImageTags()))) {
                imageTags = tmpPhoto.getImageTags();
                Logger.info("file has been changed (IMAGE_TAGS) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageISO() != null && imageISO == null) || (tmpPhoto.getImageISO() != null && imageISO != null && !imageISO.equals(tmpPhoto.getImageISO()))) {
                imageISO = tmpPhoto.getImageISO();
                Logger.info("file has been changed (IMAGE_ISO) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageFormat() != null && imageFormat == null) || (tmpPhoto.getImageFormat() != null && imageFormat != null && !imageFormat.equals(tmpPhoto.getImageFormat()))) {
                imageFormat = tmpPhoto.getImageFormat();
                Logger.info("file has been changed (IMAGE_FORMAT) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageComment() != null && imageComment == null) || (tmpPhoto.getImageComment() != null && imageComment != null && !imageComment.equals(tmpPhoto.getImageComment()))) {
                System.err.println(imageComment);
                System.err.println(tmpPhoto.getImageComment());
                imageComment = tmpPhoto.getImageComment();
                Logger.info("file has been changed (IMAGE_COMMENT) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageRating() != null && imageRating == null) || (tmpPhoto.getImageRating() != null && imageRating != null && !imageRating.equals(tmpPhoto.getImageRating()))) {
                imageRating = tmpPhoto.getImageRating();
                Logger.info("file has been changed (IMAGE_RATING) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageWebGallery() != null && imageWebGallery == null) || (tmpPhoto.getImageWebGallery() != null && imageWebGallery != null && !imageWebGallery.equals(tmpPhoto.getImageWebGallery()))) {
                imageWebGallery = tmpPhoto.getImageWebGallery();
                Logger.info("file has been changed (IMAGE_WEBPAGE) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageFocalLength() != null && imageFocalLength == null) || (tmpPhoto.getImageFocalLength() != null && imageFocalLength != null && !imageFocalLength.equals(tmpPhoto.getImageFocalLength()))) {
                imageFocalLength = tmpPhoto.getImageFocalLength();
                Logger.info("file has been changed (IMAGE_FOCALLENGTH) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageFocalLength35mm() != null && imageFocalLength35mm == null) || (tmpPhoto.getImageFocalLength35mm() != null && imageFocalLength35mm != null && !imageFocalLength35mm.equals(tmpPhoto.getImageFocalLength35mm()))) {
                imageFocalLength35mm = tmpPhoto.getImageFocalLength35mm();
                Logger.info("file has been changed (IMAGE_FOCALLENGTH_35MM) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageCameraModel() != null && imageCameraModel == null) || (tmpPhoto.getImageCameraModel() != null && imageCameraModel != null && !imageCameraModel.equals(tmpPhoto.getImageCameraModel()))) {
                imageCameraModel = tmpPhoto.getImageCameraModel();
                Logger.info("file has been changed (CAMERA_MODEL) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageCameraMake() != null && imageCameraMake == null) || (tmpPhoto.getImageCameraMake() != null && imageCameraMake != null && !imageCameraMake.equals(tmpPhoto.getImageCameraMake()))) {
                imageCameraMake = tmpPhoto.getImageCameraMake();
                Logger.info("file has been changed (CAMERA_MAKE) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageDateTimeFile() != null && imageDateTimeFile == null) || (tmpPhoto.getImageDateTimeFile() != null && imageDateTimeFile != null && !imageDateTimeFile.equals(tmpPhoto.getImageDateTimeFile()))) {
                imageDateTimeFile = tmpPhoto.getImageDateTimeFile();
                Logger.info("file has been changed (DATETIME_FILE) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageDateTimeOriginal() != null && imageDateTimeOriginal == null) || (tmpPhoto.getImageDateTimeOriginal() != null && imageDateTimeOriginal != null && !imageDateTimeOriginal.equals(tmpPhoto.getImageDateTimeOriginal()))) {
                imageDateTimeOriginal = tmpPhoto.getImageDateTimeOriginal();
                Logger.info("file has been changed (DATETIME_ORIGINAL) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageGPSLatitude() != null && imageGPSLatitude == null) || (tmpPhoto.getImageGPSLatitude() != null && imageGPSLatitude != null && !imageGPSLatitude.equals(tmpPhoto.getImageGPSLatitude()))) {
                imageGPSLatitude = tmpPhoto.getImageGPSLatitude();
                Logger.info("file has been changed (GPS_LATITUDE) " + imagePath);
                changed = true;
            }
            if ((tmpPhoto.getImageGPSLongitude() != null && imageGPSLongitude == null) || (tmpPhoto.getImageGPSLongitude() != null && imageGPSLongitude != null && !imageGPSLongitude.equals(tmpPhoto.getImageGPSLongitude()))) {
                imageGPSLongitude = tmpPhoto.getImageGPSLongitude();
                Logger.info("file has been changed (GPS_LONGITUDE) " + imagePath);
                changed = true;
            }
            lastValidationDate = System.currentTimeMillis();
            return changed;
        }
    }

    public String toString() {
        String output;
        output = "Absoluter Pfad: " + imagePath + "\n";
        output = "Unique Hash:    " + imageHash + "\n";
        output += "Breite x H�he:  " + imageWidth + " x " + imageHeight + "\n";
        output += "ISO:            " + imageISO + "\n";
        output += "Ausrichtung:    " + imageFormat + "\n";
        output += "Kommentar:      " + imageComment + "\n";
        output += "Bewertung:      " + imageRating + "\n";
        output += "Webseite:       " + imageWebGallery + "\n";
        output += "Brennweite:     " + imageFocalLength + "mm (" + imageFocalLength35mm + "mm KB)\n";
        output += "Belichtungzeit: " + imageBelichtung + "\n";
        output += "Blende:         " + imageBlende + "\n";
        output += "Lichtmessung:   " + imageLichtMessung + "\n";
        output += "Lichtquelle:    " + imageLichtquelle + "\n";
        output += "Blitz:          " + imageBlitz + "\n";
        output += "Tagcloud:       " + imageTags + "\n";
        output += "Kamera Modell:  " + imageCameraModel + "\n";
        output += "Kamera Marke:   " + imageCameraMake + "\n";
        output += "Datum Aufnahme: " + imageDateTimeOriginal + "\n";
        output += "Datum Datei:    " + imageDateTimeFile + "\n";
        output += "GPS Breite:     " + imageGPSLatitude + "\n";
        output += "GPS L�nge:      " + imageGPSLongitude + "\n";
        return output;
    }

    public Element toXML() {
        Element output = new Element("photo");
        if (imagePath != null) output.setAttribute(XML_IMAGE_PATH, imagePath); else output.setAttribute(XML_IMAGE_PATH, "");
        if (imageHash != null) output.setAttribute(XML_IMAGE_HASH, imageHash); else output.setAttribute(XML_IMAGE_HASH, "");
        if (imageWidth != null) output.setAttribute(XML_IMAGE_WIDTH, imageWidth); else output.setAttribute(XML_IMAGE_WIDTH, "");
        if (imageHeight != null) output.setAttribute(XML_IMAGE_HEIGHT, imageHeight); else output.setAttribute(XML_IMAGE_HEIGHT, "");
        if (imageTags != null) output.setAttribute(XML_IMAGE_TAGS, imageTags); else output.setAttribute(XML_IMAGE_TAGS, "");
        if (imageISO != null) output.setAttribute(XML_IMAGE_ISO, imageISO); else output.setAttribute(XML_IMAGE_ISO, "");
        if (imageFormat != null) output.setAttribute(XML_IMAGE_FORMAT, imageFormat); else output.setAttribute(XML_IMAGE_FORMAT, "");
        if (imageComment != null) output.setAttribute(XML_IMAGE_COMMENT, imageComment); else output.setAttribute(XML_IMAGE_COMMENT, "");
        if (imageRating != null) output.setAttribute(XML_IMAGE_RATING, imageRating); else output.setAttribute(XML_IMAGE_RATING, "");
        if (imageWebGallery != null) output.setAttribute(XML_IMAGE_WEBPAGE, imageWebGallery); else output.setAttribute(XML_IMAGE_WEBPAGE, "");
        if (imageBelichtung != null) output.setAttribute(XML_BELICHTUNG, imageBelichtung); else output.setAttribute(XML_BELICHTUNG, "");
        if (imageBlende != null) output.setAttribute(XML_BLENDE, imageBlende); else output.setAttribute(XML_BLENDE, "");
        if (imageLichtMessung != null) output.setAttribute(XML_LICHTMESSUNG, imageLichtMessung); else output.setAttribute(XML_LICHTMESSUNG, "");
        if (imageLichtquelle != null) output.setAttribute(XML_LICHTQUELLE, imageLichtquelle); else output.setAttribute(XML_LICHTQUELLE, "");
        if (imageBlitz != null) output.setAttribute(XML_BLITZ, imageBlitz); else output.setAttribute(XML_BLITZ, "");
        if (imageFocalLength != null) output.setAttribute(XML_FOCALLENGTH, imageFocalLength); else output.setAttribute(XML_FOCALLENGTH, "");
        if (imageFocalLength35mm != null) output.setAttribute(XML_FOCALLENGTH_35MM, imageFocalLength35mm); else output.setAttribute(XML_FOCALLENGTH_35MM, "");
        if (imageCameraModel != null) output.setAttribute(XML_CAMERA_MODEL, imageCameraModel); else output.setAttribute(XML_CAMERA_MODEL, "");
        if (imageCameraMake != null) output.setAttribute(XML_CAMERA_MAKE, imageCameraMake); else output.setAttribute(XML_CAMERA_MAKE, "");
        if (imageDateTimeFile != null) output.setAttribute(XML_DATETIME_FILE, imageDateTimeFile); else output.setAttribute(XML_DATETIME_FILE, "");
        if (imageDateTimeOriginal != null) output.setAttribute(XML_DATETIME_ORIGINAL, imageDateTimeOriginal); else output.setAttribute(XML_DATETIME_ORIGINAL, "");
        if (imageGPSLatitude != null) output.setAttribute(XML_GPS_LATITUDE, imageGPSLatitude); else output.setAttribute(XML_GPS_LATITUDE, "");
        if (imageGPSLongitude != null) output.setAttribute(XML_GPS_LONGITUDE, imageGPSLongitude); else output.setAttribute(XML_GPS_LONGITUDE, "");
        return output;
    }

    public String getImageFolderPath() {
        String[] tmp = imagePath.split("\\\\");
        return tmp[tmp.length - 2];
    }

    public String getImageFolderName() {
        String[] tmp = imagePath.split("\\\\");
        return generateFolderName(tmp[tmp.length - 2])[0];
    }

    public String getImageFolderDate() {
        String[] tmp = imagePath.split("\\\\");
        return generateFolderName(tmp[tmp.length - 2])[1];
    }

    public String getImageName() {
        String[] tmp = imagePath.split("\\\\");
        return tmp[tmp.length - 1];
    }

    public static String[] generateFolderName(String input) {
        if (input != null && input.length() > 22 && input.substring(4, 5).equals("-") && input.substring(7, 8).equals("-") && input.substring(10, 11).equals(" ") && input.substring(15, 16).equals("-") && input.substring(18, 19).equals("-") && input.substring(21, 22).equals(" ")) {
            return new String[] { input.substring(22), input.substring(8, 10) + "." + input.substring(5, 7) + "." + input.substring(0, 4) + " bis " + input.substring(19, 21) + "." + input.substring(16, 18) + "." + input.substring(11, 15) };
        } else if (input != null && input.length() > 11 && input.substring(4, 5).equals("-") && input.substring(7, 8).equals("-") && input.substring(10, 11).equals(" ")) {
            return new String[] { input.substring(11), input.substring(8, 10) + "." + input.substring(5, 7) + "." + input.substring(0, 4) };
        } else {
            return new String[] { input, "" };
        }
    }

    public String getImageHash() {
        return imageHash;
    }

    public void setImageHash(String imageHash) {
        if (this.imageHash != imageHash) {
            this.imageHash = imageHash;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageCameraModel() {
        String tmpImageCameraModel = imageCameraModel;
        if (tmpImageCameraModel.toLowerCase().trim().startsWith("canon")) tmpImageCameraModel = tmpImageCameraModel.substring(6);
        if (tmpImageCameraModel.toLowerCase().trim().startsWith("nikon")) tmpImageCameraModel = tmpImageCameraModel.substring(6);
        if (tmpImageCameraModel.toLowerCase().trim().startsWith("kodak")) tmpImageCameraModel = tmpImageCameraModel.substring(6);
        if (tmpImageCameraModel.toLowerCase().trim().startsWith("digital")) tmpImageCameraModel = tmpImageCameraModel.substring(8);
        if (tmpImageCameraModel.toLowerCase().trim().endsWith("digital camera")) tmpImageCameraModel = tmpImageCameraModel.substring(0, tmpImageCameraModel.toLowerCase().trim().lastIndexOf("digital camera"));
        if (tmpImageCameraModel.toLowerCase().trim().endsWith("zoom")) tmpImageCameraModel = tmpImageCameraModel.substring(0, tmpImageCameraModel.toLowerCase().trim().lastIndexOf("zoom"));
        return tmpImageCameraModel;
    }

    public void setImageCameraModel(String imageCameraModel) {
        if (this.imageCameraModel != imageCameraModel) {
            this.imageCameraModel = imageCameraModel;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageCameraMake() {
        return imageCameraMake;
    }

    public void setImageCameraMake(String imageCameraMake) {
        if (this.imageCameraMake != imageCameraMake) {
            this.imageCameraMake = imageCameraMake;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageComment() {
        return imageComment;
    }

    public void setImageComment(String imageComment) {
        if (this.imageComment != imageComment) {
            this.imageComment = imageComment;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageDateTimeFile() {
        return imageDateTimeFile;
    }

    public void setImageDateTimeFile(String imageDateTimeFile) {
        if (this.imageDateTimeFile != imageDateTimeFile) {
            this.imageDateTimeFile = imageDateTimeFile;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageDateTimeOriginal() {
        return imageDateTimeOriginal;
    }

    public void setImageDateTimeOriginal(String imageDateTimeOriginal) {
        if (this.imageDateTimeOriginal != imageDateTimeOriginal) {
            this.imageDateTimeOriginal = imageDateTimeOriginal;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageFocalLength() {
        return imageFocalLength;
    }

    public void setImageFocalLength(String imageFocalLength) {
        if (this.imageFocalLength != imageFocalLength) {
            this.imageFocalLength = imageFocalLength;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageFocalLength35mm() {
        return imageFocalLength35mm;
    }

    public void setImageFocalLength35mm(String imageFocalLength35mm) {
        if (this.imageFocalLength35mm != imageFocalLength35mm) {
            this.imageFocalLength35mm = imageFocalLength35mm;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageFormat() {
        return imageFormat;
    }

    public void setImageFormat(String imageFormat) {
        if (this.imageFormat != imageFormat) {
            this.imageFormat = imageFormat;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageGPSLatitude() {
        return imageGPSLatitude;
    }

    public void setImageGPSLatitude(String imageGPSLatitude) {
        if (this.imageGPSLatitude != imageGPSLatitude) {
            this.imageGPSLatitude = imageGPSLatitude;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageGPSLongitude() {
        return imageGPSLongitude;
    }

    public void setImageGPSLongitude(String imageGPSLongitude) {
        if (this.imageGPSLongitude != imageGPSLongitude) {
            this.imageGPSLongitude = imageGPSLongitude;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(String imageHeight) {
        if (this.imageHeight != imageHeight) {
            this.imageHeight = imageHeight;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageISO() {
        return imageISO;
    }

    public void setImageISO(String imageISO) {
        if (this.imageISO != imageISO) {
            this.imageISO = imageISO;
            PhotoDB.newChanges = true;
        }
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        if (this.imagePath != imagePath) {
            this.imagePath = imagePath;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageRating() {
        return imageRating;
    }

    public void setImageRating(String imageRating) {
        if (this.imageRating != imageRating) {
            this.imageRating = imageRating;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageTags() {
        return imageTags;
    }

    public void setImageTags(String imageTags) {
        if (this.imageTags != imageTags) {
            this.imageTags = imageTags;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageWebGallery() {
        return imageWebGallery;
    }

    public void setImageWebGallery(String imageWebGallery) {
        if (this.imageWebGallery != imageWebGallery) {
            this.imageWebGallery = imageWebGallery;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(String imageWidth) {
        if (this.imageWidth != imageWidth) {
            this.imageWidth = imageWidth;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageBelichtung() {
        if (imageBelichtung != null && imageBelichtung.contains(".")) {
            int bruch = new Double(Math.pow(Double.valueOf(imageBelichtung).doubleValue(), -1d)).intValue();
            return "1/" + String.valueOf(bruch);
        } else return imageBelichtung;
    }

    public void setImageBelichtung(String imageBelichtung) {
        if (this.imageBelichtung != imageBelichtung) {
            this.imageBelichtung = imageBelichtung;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageBlende() {
        return imageBlende;
    }

    public void setImageBlende(String imageBlende) {
        if (this.imageBlende != imageBlende) {
            this.imageBlende = imageBlende;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageLichtMessung() {
        return imageLichtMessung;
    }

    public void setImageLichtMessung(String imageLichtMessung) {
        if (this.imageLichtMessung != imageLichtMessung) {
            this.imageLichtMessung = imageLichtMessung;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageLichtquelle() {
        return imageLichtquelle;
    }

    public void setImageLichtquelle(String imageLichtquelle) {
        if (this.imageLichtquelle != imageLichtquelle) {
            this.imageLichtquelle = imageLichtquelle;
            PhotoDB.newChanges = true;
        }
    }

    public String getImageBlitz() {
        return imageBlitz;
    }

    public void setImageBlitz(String imageBlitz) {
        if (this.imageBlitz != imageBlitz) {
            this.imageBlitz = imageBlitz;
            PhotoDB.newChanges = true;
        }
    }

    public int compareTo(Photo comparePhoto) {
        String[] tmp = new String[2];
        tmp[0] = comparePhoto.getImagePath();
        tmp[1] = imagePath;
        java.util.Arrays.sort(tmp);
        if (tmp[0].equals(comparePhoto.getImagePath())) return 1; else if (tmp[1].equals(comparePhoto.getImagePath())) return -1; else return 0;
    }

    protected static String fileName2md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(input.getBytes("iso-8859-1"));
            byte[] byteHash = md.digest();
            md.reset();
            StringBuffer resultString = new StringBuffer();
            for (int i = 0; i < byteHash.length; i++) {
                resultString.append(Integer.toHexString(0xFF & byteHash[i]));
            }
            return (resultString.toString());
        } catch (Exception ex) {
            Logger.error(ex.getClass() + " " + ex.getMessage());
            for (int i = 0; i < ex.getStackTrace().length; i++) Logger.error("     " + ex.getStackTrace()[i].toString());
            ex.printStackTrace();
        }
        return String.valueOf(Math.random() * Long.MAX_VALUE);
    }
}
