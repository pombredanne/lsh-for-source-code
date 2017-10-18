package com.google.gwt.user.rebind.ui;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.util.Util;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Accumulates state for the bundled image.
 */
class ImageBundleBuilder {

    /**
   * The rectangle at which the original image is placed into the composite
   * image.
   */
    public static class ImageRect {

        public final int height;

        public final BufferedImage image;

        public int left;

        public final int width;

        public ImageRect(BufferedImage image) {
            this.image = image;
            this.width = image.getWidth();
            this.height = image.getHeight();
        }
    }

    private final Map imageNameToImageRectMap = new HashMap();

    private final MessageDigest md5;

    private final List orderedImageRects = new ArrayList();

    public ImageBundleBuilder() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error initializing MD5", e);
        }
    }

    /**
   * Assimilates the image associated with a particular image method into the
   * master composite. If the method names an image that has already been
   * assimilated, the existing image rectangle is reused.
   * 
   * @param logger a hierarchical logger which logs to the hosted console
   * @param imageName the name of an image that can be found on the classpath
   * @throws UnableToCompleteException if the image with name
   *         <code>imageName</code> cannot be added to the master composite image
   */
    public void assimilate(TreeLogger logger, String imageName) throws UnableToCompleteException {
        ImageRect rect = getMapping(imageName);
        if (rect == null) {
            rect = addImage(logger, imageName);
            putMapping(imageName, rect);
        }
    }

    public ImageRect getMapping(String imageName) {
        return (ImageRect) imageNameToImageRectMap.get(imageName);
    }

    public String writeBundledImage(TreeLogger logger, GeneratorContext context) throws UnableToCompleteException {
        int nextLeft = 0;
        int maxHeight = 0;
        for (Iterator iter = orderedImageRects.iterator(); iter.hasNext(); ) {
            ImageRect imageRect = (ImageRect) iter.next();
            imageRect.left = nextLeft;
            nextLeft += imageRect.width;
            if (imageRect.height > maxHeight) {
                maxHeight = imageRect.height;
            }
        }
        BufferedImage bundledImage = new BufferedImage(nextLeft, maxHeight, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D g2d = bundledImage.createGraphics();
        for (Iterator iter = orderedImageRects.iterator(); iter.hasNext(); ) {
            ImageRect imageRect = (ImageRect) iter.next();
            g2d.drawImage(imageRect.image, imageRect.left, 0, null);
        }
        g2d.dispose();
        byte[] hash = md5.digest();
        char[] strongName = new char[2 * hash.length];
        int j = 0;
        for (int i = 0; i < hash.length; i++) {
            strongName[j++] = Util.HEX_CHARS[(hash[i] & 0xF0) >> 4];
            strongName[j++] = Util.HEX_CHARS[hash[i] & 0x0F];
        }
        String bundleFileType = "png";
        String bundleFileName = new String(strongName) + ".cache." + bundleFileType;
        OutputStream outStream = context.tryCreateResource(logger, bundleFileName);
        if (outStream != null) {
            try {
                if (!ImageIO.write(bundledImage, bundleFileType, outStream)) {
                    logger.log(TreeLogger.ERROR, "Unsupported output file type", null);
                    throw new UnableToCompleteException();
                }
                context.commitResource(logger, outStream);
            } catch (IOException e) {
                logger.log(TreeLogger.ERROR, "Failed while writing", e);
                throw new UnableToCompleteException();
            }
        } else {
            logger.log(TreeLogger.TRACE, "Generated image bundle file already exists; no need to rewrite it.", null);
        }
        return bundleFileName;
    }

    private ImageRect addImage(TreeLogger logger, String imageName) throws UnableToCompleteException {
        logger = logger.branch(TreeLogger.TRACE, "Adding image '" + imageName + "'", null);
        BufferedImage image = null;
        try {
            URL imageUrl = getClass().getClassLoader().getResource(imageName);
            if (imageUrl == null) {
                logger.log(TreeLogger.ERROR, "Resource not found on classpath (is the name specified as Class.getResource() would expect?)", null);
                throw new UnableToCompleteException();
            }
            InputStream is = imageUrl.openStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            byte imgByte;
            while ((imgByte = (byte) bis.read()) != -1) {
                md5.update(imgByte);
            }
            is.close();
            image = ImageIO.read(imageUrl);
            if (image == null) {
                logger.log(TreeLogger.ERROR, "Unrecognized image file format", null);
                throw new UnableToCompleteException();
            }
        } catch (IOException e) {
            logger.log(TreeLogger.ERROR, "Unable to read image resource", null);
            throw new UnableToCompleteException();
        }
        ImageRect imageRect = new ImageRect(image);
        orderedImageRects.add(imageRect);
        return imageRect;
    }

    private void putMapping(String imageName, ImageRect rect) {
        imageNameToImageRectMap.put(imageName, rect);
    }
}
