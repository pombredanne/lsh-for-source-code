package de.cue4net.eventservice.web.images;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import org.springframework.beans.factory.InitializingBean;
import de.cue4net.eventservice.util.RandomTools;

/**
 * Generates a throw-away token image to enforce a non-automatic form
 * submissions. The token must be typed in by the user.
 *
 * @author Thorsten Vogel, <a
 *         href="http://thorsten-vogel.com">http://thorsten-vogel.com</a>
 */
public final class ImageGenerator implements InitializingBean {

    /**
     * the root dir
     */
    private String rootDirectory;

    /**
     * the path to the backgrounds.
     */
    private String tokenImageDir;

    /**
     * Directory with background templates.
     */
    private String backGroundTemplateDir;

    /**
     * Maximum cashed backgrounds.
     */
    private static final int MAX_CACHE_SIZE = 10;

    /**
     * Cached Background images.
     */
    private List<Image> backgroundcache = new ArrayList<Image>(0);

    /**
     * The rendering quality settings.
     */
    private Map<Key, Object> renderhints;

    /**
     * Static init.
     */
    @SuppressWarnings("unchecked")
    public void afterPropertiesSet() throws Exception {
        rootDirectory = System.getProperty(rootDirectory);
        File bgpicdir = new File(rootDirectory + backGroundTemplateDir);
        String[] bgFiles = null;
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File directory, String name) {
                return !name.startsWith(".") & name.endsWith("png");
            }
        };
        bgFiles = bgpicdir.list(filter);
        if (bgFiles != null) {
            backgroundcache = new ArrayList<Image>(MAX_CACHE_SIZE);
            for (int i = 0; i < bgFiles.length; i++) {
                final String filename = rootDirectory + backGroundTemplateDir + bgFiles[i];
                final Image image = new ImageIcon(filename).getImage();
                final int width = image.getWidth(null);
                if (width > 0) {
                    backgroundcache.add(image);
                } else {
                }
            }
        }
        renderhints = new HashMap(8);
        renderhints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        renderhints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        renderhints.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        renderhints.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DEFAULT);
        renderhints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        renderhints.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
        renderhints.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     */
    public String generateToken() {
        return generateToken(RandomTools.getRandomString(8));
    }

    /**
     * Generate a new transaction token, to be used for enforcing a single
     * request for a particular transaction.
     *
     * @param seed
     *        The random seed string.
     */
    public String generateToken(String seed) {
        try {
            byte id[] = seed.getBytes();
            byte now[] = new Long(System.currentTimeMillis()).toString().getBytes();
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(id);
            md.update(now);
            String pre = RandomTools.toHex(md.digest());
            StringBuffer post = new StringBuffer();
            for (int l = 0; l < pre.length() - 1; l += 4) {
                post.append(pre.substring(l, l + 1));
            }
            return post.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    /**
     * Renders the text on a random background.
     *
     * @param tokenString
     * @param filepath
     * @return String the full filename of the generated file.
     */
    public String createTokenImage(String tokenString, String fileprefix) {
        String writtenfilename = null;
        try {
            RenderedImage renderedImage = renderTokenImage(tokenString);
            StringBuffer outfilename;
            outfilename = new StringBuffer(fileprefix);
            outfilename.append(tokenString);
            outfilename.append(".png");
            writtenfilename = outfilename.toString();
            outfilename.insert(0, rootDirectory + tokenImageDir);
            File outfile = new File(outfilename.toString());
            ImageIO.write(renderedImage, "png", outfile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return writtenfilename;
    }

    public RenderedImage renderTokenImage(String text) {
        int width = 90;
        int height = 40;
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGfx = null;
        imageGfx = bufferedImage.createGraphics();
        imageGfx.setRenderingHints(renderhints);
        Image bg = (Image) backgroundcache.get(RandomTools.getRandomNum(0, backgroundcache.size()));
        imageGfx.drawImage(bg, 0, 0, null);
        imageGfx.drawBytes(text.getBytes(), 0, text.length(), 2, height / 2);
        imageGfx.dispose();
        return bufferedImage;
    }

    /**
     * Returns the backGroundTemplateDir.
     *
     * @return Returns the backGroundTemplateDir.
     */
    public String getBackGroundTemplateDir() {
        return backGroundTemplateDir;
    }

    /**
     * Set the backGroundTemplateDir.
     *
     * @param backGroundTemplateDir
     *        The backGroundTemplateDir to set.
     */
    public void setBackGroundTemplateDir(String backGroundTemplateDir) {
        this.backGroundTemplateDir = backGroundTemplateDir;
    }

    /**
     * Returns the tokenImageDir.
     *
     * @return Returns the tokenImageDir.
     */
    public String getTokenImageDir() {
        return tokenImageDir;
    }

    /**
     * Set the tokenImageDir.
     *
     * @param tokenImageDir
     *        The tokenImageDir to set.
     */
    public void setTokenImageDir(String tokenImageDir) {
        this.tokenImageDir = tokenImageDir;
    }

    /**
     * Returns the rootDirectory.
     *
     * @return Returns the rootDirectory.
     */
    public String getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Set the rootDirectory.
     *
     * @param rootDirectory The rootDirectory to set.
     */
    public void setRootDirectory(String rootDirectory) {
        this.rootDirectory = rootDirectory;
    }
}
