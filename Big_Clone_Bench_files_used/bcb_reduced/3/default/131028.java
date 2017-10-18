import java.awt.image.BufferedImage;
import java.awt.Point;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.DigestInputStream;
import java.security.NoSuchAlgorithmException;
import javax.imageio.ImageIO;

/**
 * contains decoded image data and meta information
 *
 * filename, rank, zoom
 *
 * @author Andreas Ziermann
 *
 */
class ImageData {

    private BufferedImage bImg;

    private byte[] key;

    private ByteBuffer bb;

    private Long firstPaint;

    private String fullName;

    private String name;

    private String path;

    private Long rank;

    private int zoomFactor;

    private Point offsetXy = new Point();

    String getFullname() {
        return fullName;
    }

    String getBasename() {
        return name;
    }

    String getPath() {
        return path;
    }

    int getZoomFactor() {
        return zoomFactor;
    }

    void zoomIncrease() {
        zoomFactor += 1;
    }

    void zoomDecrease() {
        zoomFactor -= 1;
    }

    Point getOffset() {
        return offsetXy;
    }

    BufferedImage getImage() {
        return bImg;
    }

    ImageData(String filename) {
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA");
            final File f = new File(filename);
            final DigestInputStream is = new DigestInputStream(new FileInputStream(f), md);
            bImg = ImageIO.read(is);
            key = is.getMessageDigest().digest();
            bb = ByteBuffer.wrap(key);
            fullName = filename;
            name = f.getName();
            path = f.getParent();
            zoomFactor = 0;
            is.close();
        } catch (IOException e) {
            System.out.println(e);
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e);
        }
    }
}
