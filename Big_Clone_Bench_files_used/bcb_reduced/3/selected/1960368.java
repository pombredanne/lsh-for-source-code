package jimagick.utils;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingWorker;
import jimagick.gui.GUI;
import jimagick.gui.list.JListModel;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * This class manages the thumbnails creation and the MD5 calculation for all
 * the images contained in a single folder (or in the image list after a
 * search). It extends the SwingWorker, a class which purpose is to preserve the
 * GUI reactivity during time-expensive calculations.
 */
public class ThumbCreatorWorkerThread extends SwingWorker<List<ImageListCell>, ImageListCell> {

    /** The list of image files. */
    private ArrayList<ImageListCell> listFiles;

    /** The model of the icon visualization. */
    private JListModel model;

    /** The thumb name. */
    private String thumbName;

    /**
	 * Instantiates a new thumb creator worker thread.
	 * 
	 * @param listFiles
	 *            the list files
	 * @param model
	 *            the list model
	 */
    public ThumbCreatorWorkerThread(ArrayList<ImageListCell> listFiles, JListModel model) {
        this.listFiles = listFiles;
        this.model = model;
    }

    /**
	 * This method starts a number of element of the list file of threads.<br>
	 * In the thread it calculate the md5 identifier of the file and then it
	 * produce the thumbnail (if doesn't just exist). The names of thumbs are
	 * his md5, and they will be saved in the project folder in the home.
	 * 
	 * @return the list of {@link ImageListCell} while ending threads.
	 * 
	 * @throws Exception
	 *             the exception
	 */
    @Override
    protected List<ImageListCell> doInBackground() throws Exception {
        ArrayList<ImageListCell> result = new ArrayList<ImageListCell>();
        MessageDigest digest = MessageDigest.getInstance("MD5");
        String md5String;
        if (listFiles.size() <= 1) GUI.instance(false).getProgressBar().setStringPainted(false);
        for (ImageListCell img : listFiles) {
            FileInputStream is = new FileInputStream(img.getSource());
            if (GUI.instance(false).getProgressBar().getValue() < listFiles.indexOf(img)) GUI.instance(false).getProgressBar().setValue(listFiles.indexOf(img));
            byte[] buffer = new byte[8192];
            int read = 0;
            try {
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
                byte[] md5sum = digest.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                md5String = bigInt.toString(16);
            } catch (IOException e) {
                throw new RuntimeException("Unable to process file for MD5", e);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
                }
            }
            thumbName = JIConfigurator.instance().getProperties().getProperty(JIConfigurator.JIMAGICK_WORKING_DIR, System.getProperty("user.home") + "/.jimagick/") + "thumbs/" + md5String + ".jpg";
            File thumb = new File(thumbName);
            thumb.getParentFile().mkdir();
            if (!thumb.exists()) createThumbnail(img.getSource().getAbsolutePath(), 100, 100, 100, thumbName);
            ImageListCell ilc = img.setParam(thumb, md5String);
            ilc.LoadXmlTag(DomXml.instance().getXmlDoc());
            publish(ilc);
            result.add(ilc);
        }
        return result;
    }

    @Override
    protected void process(List<ImageListCell> cellList) {
        for (ImageListCell ilc : cellList) {
            if (isCancelled()) {
                break;
            }
            model.addElement(ilc);
        }
    }

    @Override
    protected void done() {
        if (isCancelled()) {
            return;
        }
    }

    /**
	 * Creates the thumbnail.
	 * 
	 * @param filename
	 *            the filename of source file
	 * @param thumbWidth
	 *            the thumb width of the generated thumb that we want
	 * @param thumbHeight
	 *            the thumb height of the generated thumb that we want
	 * @param quality
	 *            the quality of the generated thumb that we want
	 * @param outFilename
	 *            the out filename of the generated thumb that we want
	 * 
	 * @throws InterruptedException
	 *             the interrupted exception
	 * @throws FileNotFoundException
	 *             Signals that the source file is not found
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
    private void createThumbnail(String filename, int thumbWidth, int thumbHeight, int quality, String outFilename) throws InterruptedException, FileNotFoundException, IOException {
        Image image = Toolkit.getDefaultToolkit().getImage(filename);
        MediaTracker mediaTracker = new MediaTracker(new Container());
        mediaTracker.addImage(image, 0);
        mediaTracker.waitForID(0);
        double thumbRatio = (double) thumbWidth / (double) thumbHeight;
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        double imageRatio = (double) imageWidth / (double) imageHeight;
        if (thumbRatio < imageRatio) {
            thumbHeight = (int) (thumbWidth / imageRatio);
        } else {
            thumbWidth = (int) (thumbHeight * imageRatio);
        }
        BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = thumbImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(outFilename));
        JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
        JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
        quality = Math.max(0, Math.min(quality, 100));
        param.setQuality((float) quality / 100.0f, false);
        encoder.setJPEGEncodeParam(param);
        encoder.encode(thumbImage);
        out.close();
    }
}
