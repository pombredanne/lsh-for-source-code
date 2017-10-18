package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.media.jai.PlanarImage;
import org.odmg.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ColorCurve;

/**
   This class abstracts a single instance of a image that is stored in a file.
*/
public class ImageInstance {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ImageInstance.class.getName());

    /**
     ImageInstance constructor is private and they must be created by ImageInstance.create().
     Reason for this is that creation can fail if e.g. the given file is not a
     proper image file.
     */
    private ImageInstance() {
    }

    /**
     Creates a new ImageInstance for a certain file in a volume. The instance is not
     yet assigned to any PhotoInfo, this must be done later by PhotoInfo.addInstance().
     @param vol The volume
     @param imgFile The file
     @return The ImageInstance created or <code>null</code> if creation was unsuccesfull
     (e.g. if imgFile does not exist or is not of a recognized file type). In this case 
     the method also aborts ongoing ODMG transaction.
     */
    public static ImageInstance create(VolumeBase vol, File imgFile) {
        log.debug("Creating instance " + imgFile.getAbsolutePath());
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance i = new ImageInstance();
        i.uuid = UUID.randomUUID();
        i.volume = vol;
        i.volumeId = vol.getName();
        i.imageFile = imgFile;
        i.fileSize = imgFile.length();
        i.mtime = imgFile.lastModified();
        i.fname = vol.mapFileToVolumeRelativeName(imgFile);
        txw.lock(i, Transaction.WRITE);
        try {
            i.readImageFile();
        } catch (Exception e) {
            txw.abort();
            log.warn("Error opening image file: " + e.getMessage());
            return null;
        }
        i.calcHash();
        i.checkTime = new java.util.Date();
        txw.commit();
        return i;
    }

    /**
       Creates a new image file object. The object is persistent,
       i.e. it is stored in database
       @param volume Volume in which the instance is stored
       @param imageFile File object pointing to the image instance
       file
       @param photo PhotoInfo object that represents the content of
       the image file
       @param instanceType Type of the instance (original, copy, thumbnail, ...)
       @return A ImageInstance object

    */
    public static ImageInstance create(VolumeBase volume, File imageFile, PhotoInfo photo, int instanceType) {
        log.debug("Creating instance, volume = " + volume.getName() + " photo = " + photo.getUid() + " image file = " + imageFile.getName());
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance f = new ImageInstance();
        f.uuid = UUID.randomUUID();
        f.volume = volume;
        f.volumeId = volume.getName();
        f.imageFile = imageFile;
        f.fileSize = imageFile.length();
        f.mtime = imageFile.lastModified();
        f.fname = volume.mapFileToVolumeRelativeName(imageFile);
        f.instanceType = instanceType;
        txw.lock(f, Transaction.WRITE);
        log.debug("locked instance");
        f.photoUid = photo.getUid();
        try {
            f.readImageFile();
        } catch (Exception e) {
            txw.abort();
            log.warn("Error opening image file: " + e.getMessage());
            return null;
        }
        f.calcHash();
        f.checkTime = new java.util.Date();
        txw.commit();
        return f;
    }

    /**
     Creates a new ImageInstance with a given UUID. This method should only be
     used when importing data from other database, since the attributes of the
     image are not set to legal (even those that form primary key in database!)
     @param uuid UUID of the created instance
     @return New ImageInstance object
     */
    public static ImageInstance create(UUID uuid) {
        ImageInstance i = new ImageInstance();
        i.uuid = uuid;
        i.volume = null;
        i.volumeId = "##nonexistingfiles##";
        i.fname = uuid.toString();
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        db.makePersistent(i);
        txw.commit();
        return i;
    }

    /**
       Retrieves the info record for a image file based on its name and path.
       @param volume Volume in which the instance is stored
       @param fname Finle name for the image
       @return ImageFile object representing the image
       @throws PhotoNotFoundException exception if the object can not be retrieved.
    */
    public static ImageInstance retrieve(VolumeBase volume, String fname) throws PhotoNotFoundException {
        String oql = "select instance from " + ImageInstance.class.getName() + " where volumeId = \"" + volume.getName() + "\" and fname = \"" + fname + "\"";
        List instances = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            instances = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            txw.abort();
            return null;
        }
        ImageInstance instance = null;
        if (instances != null && instances.size() > 0) {
            instance = (ImageInstance) instances.get(0);
        }
        return instance;
    }

    public static ImageInstance retrieveByUuid(UUID uuid) {
        String oql = "select instance from " + ImageInstance.class.getName() + " where instance_uuid = \"" + uuid.toString() + "\"";
        List instances = null;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create(oql);
            instances = (List) query.execute();
            txw.commit();
        } catch (Exception e) {
            txw.abort();
            return null;
        }
        ImageInstance instance = null;
        if (instances != null && instances.size() > 0) {
            if (instances.size() > 1) {
                log.error("ERROR: " + instances.size() + " records for ImageInstance with uuid=" + uuid.toString());
            }
            instance = (ImageInstance) instances.get(0);
        }
        return instance;
    }

    UUID uuid = null;

    /**
     Get the globally unique ID for this photo;
     */
    public UUID getUUID() {
        if (uuid == null) {
            setUUID(UUID.randomUUID());
        }
        return uuid;
    }

    public void setUUID(UUID uuid) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.uuid = uuid;
        txw.commit();
    }

    /**
       Inits the complex attributes volume and imageFile. Since these
       are not mapped directly to database columns, this function will
       be called by OJB Rowreader to initialize these correctly after
       the object has been read from database.
    */
    protected void initFileAttrs() {
        volume = VolumeBase.getVolume(volumeId);
        try {
            imageFile = volume.mapFileName(fname);
        } catch (Exception e) {
            log.warn("Error while initializing imageFile: " + e.getMessage());
        }
    }

    /**
       Deletes the ImageInstance object from database.
     @deprecated Ise delete( boolean deleteFromExtVol ) instead.
    */
    public void delete() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        db.deletePersistent(this);
        if (!(volume instanceof ExternalVolume)) {
            if (imageFile != null && !imageFile.delete()) {
                log.error("File " + imageFile.getAbsolutePath() + " could not be deleted");
            }
        }
        txw.commit();
    }

    /**
     Attempts to delete the instance
     @param deleteFromExtVol If true, the instance is deleted even if it resides
     on external volume. 
     @return True if deletion was successful
     */
    public boolean delete(boolean deleteFromExtVol) {
        boolean success = false;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        if (!(volume instanceof ExternalVolume) || deleteFromExtVol) {
            if (imageFile == null || imageFile.delete()) {
                db.deletePersistent(this);
                success = true;
            }
        }
        txw.commit();
        return success;
    }

    /**
       Opens the image file specified by fname & dirname properties and reads the rest of fields from that
       @throws IOException if the image cannot be read.
    */
    protected void readImageFile() throws PhotovaultException, IOException {
        String fname = imageFile.getName();
        int lastDotPos = fname.lastIndexOf(".");
        if (lastDotPos <= 0 || lastDotPos >= fname.length() - 1) {
            throw new PhotovaultException("Cannot determine file type extension of " + imageFile.getAbsolutePath());
        }
        String suffix = fname.substring(lastDotPos + 1);
        Iterator readers = ImageIO.getImageReadersBySuffix(suffix);
        if (readers.hasNext()) {
            ImageReader reader = (ImageReader) readers.next();
            ImageInputStream iis = null;
            try {
                iis = ImageIO.createImageInputStream(imageFile);
                if (iis != null) {
                    reader.setInput(iis, true);
                    width = reader.getWidth(0);
                    height = reader.getHeight(0);
                    reader.dispose();
                }
            } catch (IOException ex) {
                log.debug("Exception in readImageFile: " + ex.getMessage());
                throw ex;
            } finally {
                if (iis != null) {
                    try {
                        iis.close();
                    } catch (IOException ex) {
                        log.warn("Cannot close image stream: " + ex.getMessage());
                    }
                }
            }
        } else {
            RawImage ri = new RawImage(imageFile);
            if (ri.isValidRawFile()) {
                width = ri.getWidth();
                height = ri.getHeight();
            } else {
                throw new PhotovaultException("Unknown image file extension " + suffix + "\nwhile reading " + imageFile.getAbsolutePath());
            }
        }
    }

    /**
        Calculates MD5 hash of the image file
     */
    protected void calcHash() {
        hash = calcHash(imageFile);
        if (instanceType == INSTANCE_TYPE_ORIGINAL && photoUid > 0) {
            try {
                PhotoInfo p = PhotoInfo.retrievePhotoInfo(photoUid);
                if (p != null) {
                    p.setOrigInstanceHash(hash);
                }
            } catch (PhotoNotFoundException ex) {
            }
        }
    }

    /**
     Utility function to calculate the hash of a specific file
     @param f The file
     @return Hash of f
     */
    public static byte[] calcHash(File f) {
        FileInputStream is = null;
        byte hash[] = null;
        try {
            is = new FileInputStream(f);
            byte readBuffer[] = new byte[4096];
            MessageDigest md = MessageDigest.getInstance("MD5");
            int bytesRead = -1;
            while ((bytesRead = is.read(readBuffer)) > 0) {
                md.update(readBuffer, 0, bytesRead);
            }
            hash = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            log.error("MD5 algorithm not found");
        } catch (FileNotFoundException ex) {
            log.error(f.getAbsolutePath() + "not found");
        } catch (IOException ex) {
            log.error("IOException while calculating hash: " + ex.getMessage());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error("Cannot close stream after calculating hash");
            }
        }
        return hash;
    }

    byte[] hash = null;

    /**
        Returns the MD5 hash
     */
    public byte[] getHash() {
        if (hash == null && imageFile != null) {
            calcHash();
        }
        return (hash != null) ? (byte[]) hash.clone() : null;
    }

    public void setHash(byte[] hash) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.hash = hash;
        txw.commit();
    }

    /**
       Id of the volume (for OJB)
    */
    String volumeId;

    /**
       reference to the volume where the image file is located
    */
    VolumeBase volume;

    /**
     * Get the value of volume.
     * @return value of volume.
     */
    public VolumeBase getVolume() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return volume;
    }

    /**
     * Set the value of volume.
     * @param v  Value to assign to volume.
     */
    public void setVolume(VolumeBase v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.volume = v;
        volumeId = volume.getName();
        txw.commit();
    }

    /**
       The image file
    */
    File imageFile;

    /**
       File name of the image file without directory (as returner by imageFile.getName()).
       Used as OJB reference.
    */
    String fname;

    /**
     * Get the value of imageFile.
     * @return value of imageFile.
     */
    public File getImageFile() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return imageFile;
    }

    /**
     * Set the value of imageFile.
     * @param v  Value to assign to imageFile.
     */
    public void setImageFile(File v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.imageFile = v;
        fname = volume.mapFileToVolumeRelativeName(v);
        txw.commit();
    }

    /**
       Returns the file path relative to the volume base directory
    */
    public String getRelativePath() {
        String baseDir = null;
        try {
            baseDir = volume.getBaseDir().getCanonicalPath();
        } catch (IOException e) {
        }
        String filePath = null;
        try {
            filePath = imageFile.getCanonicalPath();
        } catch (IOException e) {
        }
        if (!filePath.substring(0, baseDir.length()).equals(baseDir)) {
            log.warn("ERROR: " + filePath + " not under " + baseDir);
            return "";
        }
        String relPath = filePath.substring(baseDir.length());
        return relPath;
    }

    private long fileSize;

    /**
     Get the size of the image file <b>as stored in database</b>
     */
    public long getFileSize() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return fileSize;
    }

    /**
     Set the file size. NOTE!!! This method should only be used by XmlImporter.
     */
    public void setFileSize(long s) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.fileSize = s;
        txw.commit();
    }

    private long mtime;

    /**
     Get the last modification time of the actual image file <b>as stored in 
     database</b>. Measured as milliseconds since epoc(Jan 1, 1970 midnight)
     */
    public long getMtime() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return mtime;
    }

    private java.util.Date checkTime;

    /**
     Returns the time when consistency of the instance information was last checked 
     (i.e. that the image file really exists and is still unchanged after creating 
     the instance.
     */
    public java.util.Date getCheckTime() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return checkTime != null ? (java.util.Date) checkTime.clone() : null;
    }

    /**
     Check that the information in database and associated volume are consistent. 
     In praticular, this method checks that
     <ul>
     <li>The file exists</li>
     <li>That the actual file size matches information in database unless
     size in database is 0.</li>
     <li>That the last modification time matches that in the database. If the 
     modification times differ the hash is recalculated and that is used
     to determine consistency.</li>
     <li>If file hash matches the hash stored in database the information is 
     assumed to be consistent. If mtimes or file sizes differed thee are updated 
     into database.
     </ul>
     @return true if information was consistent, false otherwise
     */
    public boolean doConsistencyCheck() {
        boolean isConsistent = true;
        boolean needsHashCheck = false;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        File f = this.getImageFile();
        if (f.exists()) {
            long size = f.length();
            if (size != this.fileSize) {
                isConsistent = false;
                if (this.fileSize == 0) {
                    needsHashCheck = true;
                }
            }
            long mtime = f.lastModified();
            if (mtime != this.mtime) {
                needsHashCheck = true;
            }
            if (needsHashCheck) {
                byte[] dbHash = (byte[]) hash.clone();
                calcHash();
                byte[] realHash = (byte[]) hash.clone();
                isConsistent = Arrays.equals(dbHash, realHash);
                if (isConsistent) {
                    txw.lock(this, Transaction.WRITE);
                    this.mtime = mtime;
                    this.fileSize = size;
                }
            }
        }
        if (isConsistent) {
            txw.lock(this, Transaction.WRITE);
            this.checkTime = new java.util.Date();
        }
        txw.commit();
        return isConsistent;
    }

    private int width;

    /**
     * Get the value of width.
     * @return value of width.
     */
    public int getWidth() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return width;
    }

    /**
     * Set the value of width.
     * @param v  Value to assign to width.
     */
    public void setWidth(int v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.width = v;
        txw.commit();
    }

    int height;

    /**
     * Get the value of height.
     * @return value of height.
     */
    public int getHeight() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return height;
    }

    /**
     * Set the value of height.
     * @param v  Value to assign to height.
     */
    public void setHeight(int v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.height = v;
        txw.commit();
    }

    /**
       The file is a original
    */
    public static final int INSTANCE_TYPE_ORIGINAL = 1;

    /**
       The file has been created from original image by e.g. changing resolution, file format,
       by doing some image procesing...
    */
    public static final int INSTANCE_TYPE_MODIFIED = 2;

    /**
       The file is intended to be used only as a thumbnail, not by other appilcations
    */
    public static final int INSTANCE_TYPE_THUMBNAIL = 3;

    int instanceType = INSTANCE_TYPE_ORIGINAL;

    /**
     * Get the value of instanceType.
     * @return value of instanceType.
     */
    public int getInstanceType() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return instanceType;
    }

    /**
     * Set the value of instanceType.
     * @param v  Value to assign to instanceType.
     */
    public void setInstanceType(int v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.instanceType = v;
        txw.commit();
    }

    double rotated;

    /**
     * Get the amount this instance is rotated compared to the original image.
     * @return value of rotated.
     */
    public double getRotated() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return rotated;
    }

    /**
     * Set the amount this image is rotated when compared to the original image
     * @param v  Value to assign to rotated.
     */
    public void setRotated(double v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        this.rotated = v;
        txw.commit();
    }

    /**
     CropBounds describes the how this instance is cropped from original image. It is 
     defined as proportional coordinates that are applied after rotating the
     original image so that top left corner is (0.0, 0.0) and bottom right
     (1.0, 1.0)
     */
    double cropMinX;

    double cropMaxX;

    double cropMinY;

    double cropMaxY;

    /**
     Check that the e crop bounds are defined in consistent manner. This is needed
     since in old installations the max parameters can be larger than min ones.
     */
    private void checkCropBounds() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        if (cropMaxX - cropMinX <= 0) {
            txw.lock(this, Transaction.WRITE);
            cropMaxX = 1.0 - cropMinX;
        }
        if (cropMaxY - cropMinY <= 0) {
            txw.lock(this, Transaction.WRITE);
            cropMaxY = 1.0 - cropMinY;
        }
        txw.commit();
    }

    /**
     Get the preferred crop bounds of the original image
     */
    public Rectangle2D getCropBounds() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        checkCropBounds();
        txw.commit();
        return new Rectangle2D.Double(cropMinX, cropMinY, cropMaxX - cropMinX, cropMaxY - cropMinY);
    }

    /**
     Set the preferred cropping operation
     @param cropBounds New crop bounds
     */
    public void setCropBounds(Rectangle2D cropBounds) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        cropMinX = cropBounds.getMinX();
        cropMinY = cropBounds.getMinY();
        cropMaxX = cropBounds.getMaxX();
        cropMaxY = cropBounds.getMaxY();
        txw.commit();
    }

    /**
     Raw conversion settings that were used when creating this instance or 
     <code>null</code> if it was not created from raw image.
     */
    RawConversionSettings rawSettings = null;

    /**
     OJB database identified for the raw settings
     */
    int rawSettingsId;

    /**
     Set the raw conversion settings for this photo
     @param s The new raw conversion settings used to create this instance. 
     The method makes a clone of the object.     
     */
    public void setRawSettings(RawConversionSettings s) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        RawConversionSettings settings = null;
        if (s != null) {
            settings = s.clone();
            txw.lock(settings, Transaction.WRITE);
        }
        if (rawSettings != null) {
            Database db = ODMG.getODMGDatabase();
            db.deletePersistent(rawSettings);
        }
        rawSettings = settings;
        txw.commit();
    }

    /**
     Get the current raw conversion settings.
     @return Current settings or <code>null</code> if instance was not created
     from a raw image.     
     */
    public RawConversionSettings getRawSettings() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return rawSettings;
    }

    /**
     Mapping from original iamge color channels to those used in this instance.
     */
    ChannelMapOperation channelMap = null;

    /**
     Set the color channel mapping from original to this instance
     @param cm the new color channel mapping
     */
    public void setColorChannelMapping(ChannelMapOperation cm) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        channelMap = cm;
        txw.commit();
    }

    /**
     Get color channel mapping from original to this instance.
     @return The current color channel mapping
     */
    public ChannelMapOperation getColorChannelMapping() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.READ);
        txw.commit();
        return channelMap;
    }

    /**
     Sets the photo UID of this instance. THis should only be called by 
     PhotoInfo.addInstance()
     @param uid UID of the photo
     */
    protected void setPhotoUid(int uid) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock(this, Transaction.WRITE);
        photoUid = uid;
        txw.commit();
    }

    int photoUid;

    public int getPhotoUid() {
        return photoUid;
    }

    /**
     Get set of the operations that have been applied for this instance when 
     creating it from original image.
     */
    public EnumSet<ImageOperations> getAppliedOperations() {
        EnumSet<ImageOperations> applied = EnumSet.noneOf(ImageOperations.class);
        if (getInstanceType() != ImageInstance.INSTANCE_TYPE_ORIGINAL) {
            if (!getCropBounds().contains(0.0, 0.0, 1.0, 1.0) || this.getRotated() != 0.0) {
                applied.add(ImageOperations.CROP);
            }
            if (getRawSettings() != null) {
                applied.add(ImageOperations.RAW_CONVERSION);
            }
            ChannelMapOperation colorMap = getColorChannelMapping();
            if (colorMap != null) {
                applied.add(ImageOperations.COLOR_MAP);
            }
        }
        return applied;
    }
}
