package net.sourceforge.dsnk.logic;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import net.sourceforge.dsnk.model.Gump;
import net.sourceforge.dsnk.model.GumpIdx;

/**
 * Reads the gumps from the gumpart.mul file.
 * 
 * @author Jochen Kraushaar
 */
public class GumpArtReader {

    private byte[] data = null;

    /**
	 * Constructor
	 * 
	 * @param gumpArtFile
	 *            gumpart.mul file
	 * @throws IOException
	 *             if an io error occurs
	 */
    public GumpArtReader(File gumpArtFile) throws IOException {
        FileInputStream fis = new FileInputStream(gumpArtFile);
        data = new byte[(int) gumpArtFile.length()];
        fis.read(data);
        fis.close();
    }

    /**
	 * Reads a gump according to the given gump index.
	 * 
	 * @param idx
	 *            gump index
	 * @return Gump
	 * @throws IOException
	 *             if an io error occurs
	 */
    public Gump read(GumpIdx idx) throws IOException {
        Gump gump = new Gump(idx, this);
        BufferedImage image = new BufferedImage(idx.getWidth(), idx.getHeight(), BufferedImage.TYPE_INT_ARGB);
        WritableRaster wr = image.getRaster();
        byte[] gumpData = new byte[idx.getSize()];
        System.arraycopy(data, idx.getOffset(), gumpData, 0, idx.getSize());
        String hash = generateHash(gumpData);
        gump.setHash(hash);
        BlockIterator blockIt = new BlockIterator(gumpData);
        int x = idx.getWidth();
        int y = idx.getHeight() - 1;
        byte[] block = null;
        while ((block = blockIt.getNextBlockFromEnd()) != null) {
            Color color = getColor(block);
            int length = getLength(block);
            x = x - length;
            drawBlock(wr, color, length, x, y);
            if (x == 0) {
                y = y - 1;
                x = idx.getWidth();
            }
            if (y == -1) {
                break;
            }
        }
        gump.setImage(image);
        return gump;
    }

    /**
	 * Draws an block of pixels on the image.
	 * 
	 * @param wr
	 *            WritableRaster for drawing pixels onto the image
	 * @param color
	 *            color of the pixels to draw
	 * @param length
	 *            no of pixels to draw in a row
	 * @param x
	 *            x start coordinate
	 * @param y
	 *            y start coordinate
	 */
    private void drawBlock(WritableRaster wr, Color color, int length, int x, int y) {
        int[] pixels = new int[length];
        for (int i = 0; i < pixels.length; i++) {
            pixels[i] = color.getRGB();
        }
        wr.setDataElements(x, y, length, 1, pixels);
    }

    /**
	 * Gets the color from the block.
	 * 
	 * @param block
	 *            block of data (2 byte)
	 * @return color
	 */
    private Color getColor(byte[] block) {
        int color16 = 0xFFFF & ((0xFF00 & (block[1] << 8)) | (0xFF & block[0]));
        double factor = (double) 0xFF / (double) 0x1F;
        int r = (int) (((color16 >> 10) & 0x1F) * factor);
        int g = (int) (((color16 >> 5) & 0x1F) * factor);
        int b = (int) (((color16) & 0x1F) * factor);
        int color32 = 0xFF000000 | (r << 16) | (g << 8) | b;
        return new Color(color32);
    }

    /**
	 * Gets the length from the block.
	 * 
	 * @param block
	 *            block of data (4 byte)
	 * @return length
	 */
    private int getLength(byte[] block) {
        int length = 0xFFFF & ((0xFF00 & (block[3] << 8)) | (0xFF & block[2]));
        return length;
    }

    /**
	 * Iterates over the blocks in the gump data. The iteration starts at the
	 * last byte and ends at the first byte.
	 * 
	 * @author Jochen Kraushaar
	 */
    private class BlockIterator {

        private byte[] gumpData = null;

        private int blockOffset = 0;

        /**
		 * Constructor
		 * 
		 * @param gumpData
		 *            data to be iterated
		 */
        public BlockIterator(byte[] gumpData) {
            this.gumpData = new byte[gumpData.length];
            System.arraycopy(gumpData, 0, this.gumpData, 0, gumpData.length);
            blockOffset = gumpData.length;
        }

        /**
		 * Gets the next block, starting at the end.
		 * 
		 * @return block containing 4 bytes of data
		 */
        public byte[] getNextBlockFromEnd() {
            byte[] block = new byte[4];
            if (blockOffset > 0) {
                blockOffset = blockOffset - 4;
                System.arraycopy(gumpData, blockOffset, block, 0, 4);
                return block;
            } else {
                return null;
            }
        }
    }

    /**
	 * Generates a hash for the given data.
	 * 
	 * @param data image data
	 * @return hash
	 */
    private String generateHash(byte[] data) {
        StringBuilder builder = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] hash = md.digest();
            for (byte hashByte : hash) {
                builder.append(Integer.toHexString(0xFF & hashByte));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
