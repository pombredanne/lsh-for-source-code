package org.jimcat.services.imagemanager;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jimcat.model.ExifMetadata;
import org.jimcat.model.Image;
import org.jimcat.model.ImageMetadata;
import org.jimcat.model.ImageRotation;
import org.jimcat.model.Thumbnail;
import org.jimcat.services.ServiceLocator;
import org.joda.time.DateTime;
import org.joda.time.IllegalFieldValueException;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifDirectory;
import com.drew.metadata.exif.ExifReader;

/**
 * A utility class with useful methods for image - handling.
 * 
 * $Id$
 * 
 * @author Christoph
 */
public final class ImageUtil {

    /**
	 * the size source files are stored within. A bigger size isn't supported by
	 * current graphical configuration.
	 */
    private static final Dimension SOURCE_BOUNDING_BOX;

    /**
	 * should tiles or subsampling be used to create images
	 */
    private static final boolean USE_TILES = false;

    /**
	 * fetch current maximum screen resolution
	 */
    static {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] screens = ge.getScreenDevices();
        Dimension res = new Dimension(0, 0);
        for (GraphicsDevice screen : screens) {
            Rectangle rect = screen.getDefaultConfiguration().getBounds();
            res.width = Math.max(rect.width, res.width);
            res.height = Math.max(rect.height, res.height);
        }
        SOURCE_BOUNDING_BOX = res;
    }

    /**
	 * a tile of an image loaded at one time.
	 * 
	 * The more memory consumed the less images will be stored within the cache
	 * 
	 * <ul>
	 * <li>~15 MB per tile at resolution 2000x2000 (original)</li>
	 * <li>~22 MB per tile at resolution 2816x2112 (~6MP)</li>
	 * <li>~35 MB per tile at resolution 3840x2400 (~9.2MP)</li>
	 * </ul>
	 * 
	 */
    private static final Dimension IMAGE_TILE_SIZE = new Dimension(3840, 2400);

    /**
	 * private constructor making this class uninstanceable
	 */
    private ImageUtil() {
    }

    /**
	 * This methode will scale the given Buffered image to the given dimension
	 * using the given ImageQuality
	 * 
	 * If image dimension already maches, it will return the images itself.
	 * 
	 * based on
	 * 
	 * http://today.java.net/pub/a/today/2007/04/03/perils-of-image-getscaledinstance.html
	 * 
	 * 
	 * @param img -
	 *            the image to scale
	 * @param dimension -
	 *            the destination dimension
	 * @param quality -
	 *            the quality
	 * @return a scaled instance of the buffered image
	 */
    public static BufferedImage getScaledInstance(BufferedImage img, Dimension dimension, ImageQuality quality) {
        if (img == null) {
            return null;
        }
        BufferedImage ret = img;
        int targetWidth = dimension.width;
        int targetHeight = dimension.height;
        if (img.getWidth() == targetWidth && img.getHeight() == targetHeight) {
            return img;
        }
        boolean higherQuality = quality.requiresIntermediateSteps();
        int type = getImageType(img);
        int w, h;
        if (higherQuality) {
            w = img.getWidth();
            h = img.getHeight();
        } else {
            w = targetWidth;
            h = targetHeight;
        }
        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
            }
            if (higherQuality && h > targetHeight) {
                h /= 2;
            }
            if (w < targetWidth) {
                w = targetWidth;
            }
            if (h < targetHeight) {
                h = targetHeight;
            }
            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, quality.getHint());
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
        } while (w != targetWidth || h != targetHeight);
        return ret;
    }

    /**
	 * this methode will create a rotated version of the given img. if the
	 * rotation is ROTATION_0 the original image will be returned
	 * 
	 * @param img -
	 *            the image to rotate
	 * @param rotation -
	 *            describing the requested rotation
	 * @return - the rotated image
	 */
    public static BufferedImage rotateImage(BufferedImage img, ImageRotation rotation) {
        if (rotation == ImageRotation.ROTATION_0) {
            return img;
        }
        Dimension resultDim = null;
        if (rotation == ImageRotation.ROTATION_180) {
            resultDim = new Dimension(img.getWidth(), img.getHeight());
        } else {
            resultDim = new Dimension(img.getHeight(), img.getWidth());
        }
        int type = getImageType(img);
        int width = resultDim.width;
        int height = resultDim.height;
        BufferedImage tmp = new BufferedImage(width, height, type);
        Graphics2D g2 = tmp.createGraphics();
        g2.rotate(rotation.getAngle());
        if (rotation == ImageRotation.ROTATION_90) {
            g2.translate(0, -width);
        } else if (rotation == ImageRotation.ROTATION_180) {
            g2.translate(-width, -height);
        } else if (rotation == ImageRotation.ROTATION_270) {
            g2.translate(-height, 0);
        }
        g2.drawImage(img, 0, 0, null);
        g2.dispose();
        return tmp;
    }

    /**
	 * calculate the dimension of an image to fit into a boundingBox without
	 * lossing aspect ration.
	 * 
	 * @param width -
	 *            the width to scale
	 * @param height -
	 *            the height to scale
	 * @param boundingBox -
	 *            the bounding box to fit
	 * @param allowUpScale -
	 *            if true, the resulting size may be bigger then the given size,
	 *            else it is smaller or equal
	 * @return the scaled dimension
	 */
    public static Dimension getScaledDimension(int width, int height, Dimension boundingBox, boolean allowUpScale) {
        float factorHeight = height / (float) boundingBox.height;
        float factorWidth = width / (float) boundingBox.width;
        float factor = Math.max(factorHeight, factorWidth);
        if (!allowUpScale) {
            factor = Math.max(factor, 1);
        }
        int resHeight = Math.max(1, (int) (height / factor));
        int resWidth = Math.max(1, (int) (width / factor));
        return new Dimension(resWidth, resHeight);
    }

    /**
	 * this method will resolve the given information to an Image object
	 * 
	 * @param file -
	 *            the file containing the image
	 * @param quality -
	 *            the quality used to render contained image
	 * @param importId -
	 *            the import id of the resulting image
	 * @param addedDate -
	 *            the added date of the resulting image
	 * @return - a full featured image object
	 * @throws IOException
	 */
    public static Image resolveImage(File file, ImageQuality quality, long importId, DateTime addedDate) throws IOException {
        byte[] content = loadFile(file);
        return resolveImage(content, quality, file, importId, addedDate);
    }

    /**
	 * this methode will resolve the given information to an Image object
	 * 
	 * @param content -
	 *            the content of the file as byte array
	 * @param quality -
	 *            the quality used to render contained image
	 * @param file -
	 *            the file containing the image
	 * @param importId -
	 *            the import id of the resulting image
	 * @param addedDate -
	 *            the added date of the resulting image
	 * @return - a fullfeatured image object
	 * @throws IOException
	 */
    public static Image resolveImage(byte content[], ImageQuality quality, File file, long importId, DateTime addedDate) throws IOException {
        Image img = new Image();
        BufferedImage image = loadImage(content, quality);
        if (image == null) throw new IOException("Image couldn't be read.");
        Thumbnail thumbnail = new Thumbnail(image);
        img.setThumbnail(thumbnail);
        String checksum = ImageUtil.getChecksum(content);
        Dimension dim = getImageDimension(content);
        ImageMetadata metadata = createMetadata(file, dim.width, dim.height, checksum, importId, addedDate);
        img.setMetadata(metadata);
        ExifMetadata exifMetadata = ImageUtil.readExifMetadata(content);
        img.setExifMetadata(exifMetadata);
        ServiceLocator.getImageManager().flushImage(img, image);
        return img;
    }

    /**
	 * use this methode to load an image from disc
	 * 
	 * @param path -
	 *            the file to load
	 * @param quality -
	 *            the quality to use if scaling is necessary
	 * @return - a BufferedImage containing the image, size may be limited by
	 *         SOURCE_BOUNDING_BOX constant
	 * @throws IOException -
	 *             if something goes wrong
	 */
    public static BufferedImage loadImage(File path, ImageQuality quality) throws IOException {
        return loadImage(loadFile(path), quality);
    }

    /**
	 * use this methode to load an image from a given byte array
	 * 
	 * @param data -
	 *            the byte array containing an encoded image
	 * @param quality
	 *            the rendering quality
	 * @return - a Buffered Image containing image. Its size is limited by
	 *         SOURCE_BOUNDING_BOX constant
	 * @throws IOException -
	 *             if something goes wrong
	 */
    public static BufferedImage loadImage(byte[] data, ImageQuality quality) throws IOException {
        ImageReader reader = getReaderForImage(data);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        Dimension size = getScaledDimension(width, height, SOURCE_BOUNDING_BOX, false);
        BufferedImage bi = null;
        try {
            if (USE_TILES) {
                if (width <= IMAGE_TILE_SIZE.width && height <= IMAGE_TILE_SIZE.height) {
                    bi = reader.read(0);
                } else {
                    ImageReadParam param = reader.getDefaultReadParam();
                    if (param.canSetSourceRenderSize()) {
                        param.setSourceRenderSize(size);
                        bi = reader.read(0, param);
                    } else {
                        bi = loadImageWithTiles(reader, size, quality);
                    }
                }
            } else {
                bi = loadImageWithSubSampling(reader, size, quality);
            }
        } finally {
            reader.dispose();
        }
        return bi;
    }

    /**
	 * this methode is used to read a file into an internal byte array.
	 * 
	 * @param file -
	 *            the file to read
	 * @return - a bytearray containing the content of the file
	 * @throws IOException -
	 *             if there are any problems reading the file
	 */
    public static byte[] loadFile(File file) throws IOException {
        BufferedInputStream in = null;
        ByteArrayOutputStream sink = null;
        try {
            in = new BufferedInputStream(new FileInputStream(file));
            sink = new ByteArrayOutputStream();
            IOUtils.copy(in, sink);
            return sink.toByteArray();
        } finally {
            IOUtils.closeQuietly(in);
            IOUtils.closeQuietly(sink);
        }
    }

    /**
	 * used to calculate the checksum of a chunk of bytes.
	 * 
	 * this implementation is creating an MD5 checksum.
	 * 
	 * @param file
	 * @return - the MD5 checksum as string or null on error
	 */
    public static String getChecksum(byte[] file) {
        String result = null;
        try {
            MessageDigest mdAlgorithm = MessageDigest.getInstance("MD5");
            mdAlgorithm.update(file);
            byte[] checksum = mdAlgorithm.digest();
            StringBuffer buffer = new StringBuffer(checksum.length * 2);
            for (byte b : checksum) {
                buffer.append(Integer.toHexString((b & 0xF0) >> 4));
                buffer.append(Integer.toHexString(b & 0x0F));
            }
            result = buffer.toString().toUpperCase();
        } catch (NoSuchAlgorithmException nsae) {
            nsae.printStackTrace();
        }
        return result;
    }

    /**
	 * create a ImageMetadata object from given information
	 * 
	 * @param source -
	 *            the image source file
	 * @param width -
	 *            the image width
	 * @param height -
	 *            the image height
	 * @param checksum -
	 *            the checksum of the given image
	 * @param importId -
	 *            the import id
	 * @param addedDate -
	 *            the date when this image was added
	 * @return the metadata of the image at source
	 */
    public static ImageMetadata createMetadata(File source, int width, int height, String checksum, long importId, DateTime addedDate) {
        File file = source.getAbsoluteFile();
        long size = file.length();
        DateTime creationDate = new DateTime(file.lastModified());
        return new ImageMetadata(file, width, height, size, checksum, importId, creationDate, addedDate);
    }

    /**
	 * Parses the exif metadata of a given file.
	 * 
	 * @param file
	 *            The input file, should be a jpeg file
	 * @return An instance of ExifMetadata or null in case of any error
	 */
    public static ExifMetadata readExifMetadata(byte[] file) {
        try {
            Metadata metadata = new Metadata();
            new ExifReader(new ByteArrayInputStream(file)).extract(metadata);
            Directory exif = metadata.getDirectory(ExifDirectory.class);
            if (!hasUsableExifData(exif)) {
                return null;
            }
            String manufacturer = exif.getString(ExifDirectory.TAG_MAKE);
            String model = exif.getString(ExifDirectory.TAG_MODEL);
            String dateTaken = exif.getString(ExifDirectory.TAG_DATETIME_ORIGINAL);
            String exposure = exif.getString(ExifDirectory.TAG_EXPOSURE_TIME);
            String aperture = exif.getString(ExifDirectory.TAG_FNUMBER);
            String focal = exif.getString(ExifDirectory.TAG_FOCAL_LENGTH);
            String flash = flashCodeToString(exif.getString(ExifDirectory.TAG_FLASH));
            String iso = exif.getString(ExifDirectory.TAG_ISO_EQUIVALENT);
            DateTime dateTime = null;
            if (dateTaken != null) {
                try {
                    DateTimeFormatter fmt = DateTimeFormat.forPattern("yyyy:MM:dd HH:mm:ss");
                    dateTime = fmt.parseDateTime(dateTaken);
                } catch (IllegalFieldValueException e) {
                }
            }
            return new ExifMetadata(manufacturer, model, dateTime, exposure, aperture, flash, focal, iso);
        } catch (JpegProcessingException e) {
        }
        return null;
    }

    /**
	 * this methode will load given image using tiles (saving memory)
	 * 
	 * this strategie is spliting the original image up into smaller parts
	 * called tiles. Those tiles are downscaled one by one using given quality.
	 * This results int probably best possible quality but may cost a lot of
	 * time.
	 * 
	 * @param reader -
	 *            the reader to load image from
	 * @param size -
	 *            the resulting image size
	 * @param quality -
	 *            the quality used for necessary rendering
	 * @return the image as buffered image
	 * @throws IOException
	 */
    @SuppressWarnings("unused")
    private static BufferedImage loadImageWithTiles(ImageReader reader, Dimension size, ImageQuality quality) throws IOException {
        ImageTypeSpecifier imageSpec = reader.getImageTypes(0).next();
        BufferedImage tile = imageSpec.createBufferedImage(IMAGE_TILE_SIZE.width, IMAGE_TILE_SIZE.height);
        BufferedImage result = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, quality.getHint());
        ImageReadParam param = reader.getDefaultReadParam();
        param.setDestination(tile);
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        int numX = (int) Math.ceil(width / (float) IMAGE_TILE_SIZE.width);
        int numY = (int) Math.ceil(height / (float) IMAGE_TILE_SIZE.height);
        float aspectX = (float) IMAGE_TILE_SIZE.width / width;
        float aspectY = (float) IMAGE_TILE_SIZE.height / height;
        int targetTileWidth = (int) (size.width * aspectX);
        int targetTileHeight = (int) (size.height * aspectY);
        Rectangle sourceRegion = new Rectangle();
        Rectangle targetRegion = new Rectangle();
        for (int i = 0; i < numX; i++) {
            sourceRegion.x = i * IMAGE_TILE_SIZE.width;
            sourceRegion.width = Math.min(IMAGE_TILE_SIZE.width, width - sourceRegion.x);
            targetRegion.x = i * targetTileWidth;
            targetRegion.width = Math.min(targetTileWidth, size.width - targetRegion.x);
            for (int j = 0; j < numY; j++) {
                sourceRegion.y = j * IMAGE_TILE_SIZE.height;
                sourceRegion.height = Math.min(IMAGE_TILE_SIZE.height, height - sourceRegion.y);
                targetRegion.y = j * targetTileHeight;
                targetRegion.height = Math.min(targetTileHeight, size.height - targetRegion.y);
                param.setSourceRegion(sourceRegion);
                reader.read(0, param);
                int dx1 = targetRegion.x;
                int dx2 = targetRegion.x + targetRegion.width;
                int dy1 = targetRegion.y;
                int dy2 = targetRegion.y + targetRegion.height;
                g.drawImage(tile, dx1, dy1, dx2, dy2, 0, 0, sourceRegion.width, sourceRegion.height, null);
            }
        }
        g.dispose();
        return result;
    }

    /**
	 * this methode will load given image using a subsample rate
	 * 
	 * this strategie is just reading a subset of images lines ans rowes to
	 * reduce memory usage and cpu time.
	 * 
	 * @param reader -
	 *            the reader to load image from
	 * @param size -
	 *            the resulting image size
	 * @param quality -
	 *            the quality used for necessary rendering
	 * @return the image as buffered image
	 * @throws IOException
	 */
    private static BufferedImage loadImageWithSubSampling(ImageReader reader, Dimension size, ImageQuality quality) throws IOException {
        ImageReadParam param = reader.getDefaultReadParam();
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        int rateX = width / size.width;
        int rateY = height / size.height;
        param.setSourceSubsampling(rateX, rateY, 0, 0);
        BufferedImage img = reader.read(0, param);
        BufferedImage result = getScaledInstance(img, size, quality);
        return result;
    }

    /**
	 * get the dimension of the image stored inside the given array
	 * 
	 * @param image
	 * @return the image dimension
	 * @throws IOException
	 */
    private static Dimension getImageDimension(byte image[]) throws IOException {
        ImageReader reader = getReaderForImage(image);
        return new Dimension(reader.getWidth(0), reader.getHeight(0));
    }

    /**
	 * get an image reader for the given file content
	 * 
	 * @param image
	 * @return an image reader for the given content
	 * @throws IOException
	 */
    private static ImageReader getReaderForImage(byte image[]) throws IOException {
        ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(image));
        Iterator iter = ImageIO.getImageReaders(stream);
        if (!iter.hasNext()) {
            return null;
        }
        ImageReader reader = (ImageReader) iter.next();
        reader.setInput(stream, true, true);
        return reader;
    }

    /**
	 * get a BufferedImageType of the given image used to replicate
	 * 
	 * @param img
	 * @return the type of the buffered image
	 */
    private static int getImageType(BufferedImage img) {
        if (img.getTransparency() == Transparency.OPAQUE) {
            return BufferedImage.TYPE_INT_RGB;
        }
        return BufferedImage.TYPE_INT_ARGB;
    }

    /**
	 * check if there is any usefull exifdata available
	 * 
	 * @param exif
	 * @return true if there is unsable exif data
	 */
    private static boolean hasUsableExifData(Directory exif) {
        if (exif.getTagCount() == 0) {
            return false;
        }
        int[] tags = new int[] { ExifDirectory.TAG_MAKE, ExifDirectory.TAG_DATETIME_ORIGINAL, ExifDirectory.TAG_EXPOSURE_TIME, ExifDirectory.TAG_FNUMBER };
        for (int tag : tags) {
            if (exif.containsTag(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
	 * maps a flash code to a string representation
	 * 
	 * @param code
	 * @return a string representation of the flash code
	 */
    private static String flashCodeToString(String code) {
        int flash;
        try {
            flash = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return null;
        }
        switch(flash) {
            case 0x0:
                return "No Flash";
            case 0x1:
                return "Fired";
            case 0x5:
                return "Fired, Return not detected";
            case 0x7:
                return "Fired, Return detected";
            case 0x9:
                return "On";
            case 0xd:
                return "On, Return not detected";
            case 0xf:
                return "On, Return detected";
            case 0x10:
                return "Off";
            case 0x18:
                return "Auto, Did not fire";
            case 0x19:
                return "Auto, Fired";
            case 0x1d:
                return "Auto, Fired, Return not detected";
            case 0x1f:
                return "Auto, Fired, Return detected";
            case 0x20:
                return "No flash function";
            case 0x41:
                return "Fired, Red-eye reduction";
            case 0x45:
                return "Fired, Red-eye reduction, Return not detected";
            case 0x47:
                return "Fired, Red-eye reduction, Return detected";
            case 0x49:
                return "On, Red-eye reduction";
            case 0x4d:
                return "On, Red-eye reduction, Return not detected";
            case 0x4f:
                return "On, Red-eye reduction, Return detected";
            case 0x59:
                return "Auto, Fired, Red-eye reduction";
            case 0x5d:
                return "Auto, Fired, Red-eye reduction, Return not detected";
            case 0x5f:
                return "Auto, Fired, Red-eye reduction, Return detected";
        }
        return null;
    }
}
