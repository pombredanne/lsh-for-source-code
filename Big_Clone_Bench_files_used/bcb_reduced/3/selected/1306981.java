package com.lowagie.text;

import java.net.URL;
import java.security.MessageDigest;

/**
 * Support for JBIG2 images.
 * @since 2.1.5
 */
public class ImgJBIG2 extends Image {

    /** JBIG2 globals */
    private byte[] global;

    /** A unique hash */
    private byte[] globalHash;

    /**
	 * Copy contstructor.
	 * @param	image another Image
	 */
    ImgJBIG2(Image image) {
        super(image);
    }

    /**
	 * Empty constructor.
	 */
    public ImgJBIG2() {
        super((Image) null);
    }

    /**
	 * Actual constructor for ImgJBIG2 images.
	 * @param	width	the width of the image
	 * @param	height	the height of the image
	 * @param	data	the raw image data
	 * @param	globals	JBIG2 globals
	 */
    public ImgJBIG2(int width, int height, byte[] data, byte[] globals) {
        super((URL) null);
        type = JBIG2;
        originalType = ORIGINAL_JBIG2;
        scaledHeight = height;
        setTop(scaledHeight);
        scaledWidth = width;
        setRight(scaledWidth);
        bpc = 1;
        colorspace = 1;
        rawData = data;
        plainWidth = getWidth();
        plainHeight = getHeight();
        if (globals != null) {
            this.global = globals;
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                md.update(this.global);
                this.globalHash = md.digest();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * Getter for the JBIG2 global data.
	 * @return 	an array of bytes
	 */
    public byte[] getGlobalBytes() {
        return this.global;
    }

    /**
	 * Getter for the unique hash.
	 * @return	an array of bytes
	 */
    public byte[] getGlobalHash() {
        return this.globalHash;
    }
}
