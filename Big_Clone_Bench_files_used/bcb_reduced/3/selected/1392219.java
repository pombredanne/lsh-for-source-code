package cz.zcu.fav.hofhans.packer.bdo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import cz.zcu.fav.hofhans.packer.exception.PackerRuntimeException;

/**
 * Class for {@link PackerFile} compression parameters.
 * Each file for compression contains set of parameters for selecting best compression.
 * @author Tomáš Hofhans
 * @since 27.1.2010
 */
public class CompressionParams {

    /** LOG. */
    private static final Logger LOG = Logger.getLogger(CompressionParams.class.getName());

    /** Algorithm for counting file hash. */
    private static final String HASH_ALGORITHM = "MD5";

    /** Hash. */
    private String hash;

    /** Is image file. */
    private boolean image;

    /** Width. */
    private int width;

    /** Height. */
    private int height;

    /**
   * Constructor for parameters with counting all parameters.
   * Count parameters from given file.
   * @param file file for counting parameters.
   */
    public CompressionParams(PackerFile file) {
        super();
        calculateHash(file.getData());
        testImage(file);
    }

    /**
   * Crate new compression parameters without data.
   * This constructor is used for loading parameters from persistent storage.
   */
    public CompressionParams() {
        super();
    }

    /**
   * Get hash.
   * @return the hash
   */
    public String getHash() {
        return hash;
    }

    /**
   * Set stored hash. For loading parameters from persistent storage.
   * @param hash new hash
   */
    public void setHash(String hash) {
        this.hash = hash;
    }

    /**
   * Test if given file is supported image.
   * @return true if givem file is supported image file
   */
    public boolean isSupportedImage() {
        return image;
    }

    /**
   * Set if file is supported image. For loading parameters from persistent storage.
   * @param image true if is supported image.
   */
    public void setImage(boolean image) {
        this.image = image;
    }

    /**
   * Get image width. Not relavation for not image files.
   * @return the width
   */
    public int getWidth() {
        return width;
    }

    /**
   * Set image width.
   * @param width the width to set
   */
    public void setWidth(int width) {
        this.width = width;
    }

    /**
   * Get image height. Not relavation for not image files.
   * @return the height
   */
    public int getHeight() {
        return height;
    }

    /**
   * Set image height.
   * @param height the height to set
   */
    public void setHeight(int height) {
        this.height = height;
    }

    /**
   * Test if given file is supported image.
   */
    private void testImage(PackerFile file) {
        int dot = file.getName().lastIndexOf(".");
        String suffix = file.getName().substring(dot + 1);
        String[] types = ImageIO.getReaderFormatNames();
        boolean found = false;
        for (String type : types) {
            if (type.equals(suffix)) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (LOG.isLoggable(Level.CONFIG)) {
                LOG.config("Image reader for file name " + file.getName() + " not found.");
            }
            image = false;
            return;
        }
        types = ImageIO.getWriterFormatNames();
        found = false;
        for (String type : types) {
            if (type.equals(suffix)) {
                found = true;
                break;
            }
        }
        if (!found) {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.config("Image writer for file name " + file.getName() + " not found.");
            }
            image = false;
            return;
        }
        image = true;
    }

    /**
   * Calculate file hash.
   * @param data data for hash calculation
   */
    private void calculateHash(byte[] data) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance(HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new PackerRuntimeException("Invalid algorithm.", e);
        }
        md.update(data);
        byte messageDigest[] = md.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            String hex = Integer.toHexString(0xFF & messageDigest[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        hash = hexString.toString();
    }
}
