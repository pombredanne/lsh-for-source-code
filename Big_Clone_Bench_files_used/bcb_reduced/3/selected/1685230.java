package de.beeld.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.swing.ImageIcon;
import org.apache.log4j.Logger;

/**
 * This class contains some static methods which can be used in every other
 * class without this class.
 * 
 * @author Martin R&ouml;bert
 * @version $LastChangedRevision: 106 $
 * @since $HeadURL:
 *        https://beeld.svn.sourceforge.net/svnroot/beeld/src/de/beeld/
 *        util/Helper.java $
 */
public class Helper {

    private static Logger log = Logger.getLogger(Helper.class);

    private static String strBeeldDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".beeld" + System.getProperty("file.separator");

    private static File fileUserDir = new File(strBeeldDir);

    private static ImageIcon icon;

    private static final String strSlidePrefix = "slide_";

    private static final String strSlideSuffix = ".pdf";

    private static Helper helperInstance = null;

    private Helper() {
    }

    /**
	 * generate a message for tracing the program.<br/>
	 * template: Call method <i>meth</i> in class <i>clazz.getName</i>
	 * 
	 * @param meth
	 *            Method where you called this
	 * @param clazz
	 *            Class where you called this
	 * @return String generated template
	 */
    public static String constructTraceMessage(String meth, Class<?> clazz) {
        return "Rufe Methode " + meth + " in der Klasse " + clazz.getName() + " auf.";
    }

    /**
	 * Generate a hash of the given string
	 * 
	 * @param value
	 *            string to hashify
	 * @return String
	 */
    public static String generateHash(String value) {
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.reset();
            md5.update(value.getBytes());
        } catch (NoSuchAlgorithmException e) {
            log.error("Could not find the requested hash method: " + e.getMessage());
        }
        byte[] result = md5.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            hexString.append(Integer.toHexString(0xFF & result[i]));
        }
        return hexString.toString();
    }

    /**
	 * to identify a client you need a unique identification. here we generate
	 * the hash with several properties of the JVM .
	 * 
	 * @return String
	 */
    public static String generateUID() {
        return Helper.generateHash(System.getProperty("user.name") + System.getProperty("sun.cpu.isalist") + System.getProperty("sun.desktop") + System.getProperty("java.class.path"));
    }

    /**
	 * Get a direcotry where slides are stored.
	 * 
	 * If not existing - create it.
	 * 
	 * @return string to the working dir
	 */
    public static String getBeeldWorkingDir() {
        if (!fileUserDir.isDirectory()) {
            try {
                fileUserDir.mkdir();
            } catch (SecurityException e) {
                log.error("Security Manager Exception" + e.getMessage());
            }
        }
        log.trace(fileUserDir.getAbsolutePath());
        return fileUserDir.getAbsolutePath();
    }

    /**
	 * Get a ImageIcon from jar file or from gfx/- folder
	 * 
	 * @param name
	 *            name of image
	 * @return
	 */
    public static ImageIcon getImageRessource(String name) {
        System.out.println("getImageRessource");
        if (helperInstance == null) {
            helperInstance = new Helper();
        }
        icon = new ImageIcon(helperInstance.getImageURL(name));
        return icon;
    }

    /**
	 * get the single instance of helper
	 * 
	 * @return
	 */
    public static Helper getInstance() {
        if (helperInstance == null) {
            helperInstance = new Helper();
        }
        return helperInstance;
    }

    /**
	 * @see de.beeld.util.Helper#getImageRessource(String)
	 * @param name
	 * @return URL of requested ressource
	 */
    public URL getImageURL(String name) {
        URL res = null;
        System.out.println("getImageURL");
        if (this.getClass().getResource("/" + name) != null) {
            res = this.getClass().getResource("/" + name);
        } else {
            try {
                res = new URL("gfx/" + name);
            } catch (MalformedURLException e) {
                log.error("Could not load url" + e.getMessage());
            }
        }
        return res;
    }

    /**
	 * @see de.beeld.util.Helper#getImageRessource(String)
	 * @param name
	 * @return
	 */
    public ImageIcon getImageIcon(String name) {
        ImageIcon ico;
        if (this.getClass().getResource("/" + name) != null) {
            ico = new ImageIcon(getClass().getResource("/" + name));
        } else {
            ico = new ImageIcon("gfx/" + name);
        }
        return ico;
    }

    /**
	 * get beeld splash pdf from gfx/- folder
	 * 
	 * @return
	 */
    public static File getSplashPDF() {
        return new File("gfx/BeeldSplashPDF.pdf");
    }

    /**
	 * extract splas pdf from jar and save it to working-dir
	 * 
	 * @return
	 */
    public File extractSplash() {
        File efile = null;
        try {
            String home = getClass().getProtectionDomain().getCodeSource().getLocation().toString().substring(6);
            System.out.println(home);
            JarFile jar = new JarFile(home);
            ZipEntry entry = jar.getEntry("BeeldSplashPDF.pdf");
            efile = new File(Helper.getBeeldWorkingDir(), entry.getName());
            InputStream in = new BufferedInputStream(jar.getInputStream(entry));
            OutputStream out = new BufferedOutputStream(new FileOutputStream(efile));
            byte[] buffer = new byte[2048];
            for (; ; ) {
                int nBytes = in.read(buffer);
                if (nBytes <= 0) break;
                out.write(buffer, 0, nBytes);
            }
            out.flush();
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(efile.exists());
        return efile;
    }

    /**
	 * I wanted to get rid of calling System.getProperty("file.separator")
	 * 
	 * @return
	 */
    public static String getSeparator() {
        return System.getProperty("file.separator");
    }

    /**
	 * Rename string slide_i.pdf to slide_(i+1).pdf
	 * 
	 * @param name
	 * @return
	 */
    public static String putNextSlideName(String name) {
        name = name.replaceAll("slide_", "").replaceAll(".pdf", "");
        return "slide_" + (Integer.parseInt(name) + 1) + ".pdf";
    }

    /**
	 * Call with slide_i.pdf and get (i+1) returned
	 * 
	 * @param name
	 * @return
	 */
    public static int putNextSlideIntValue(String name) {
        name = name.replaceAll("slide_", "").replaceAll(".pdf", "");
        return Integer.parseInt(name) + 1;
    }

    /**
	 * Call with slide_i.pdf and get the i returned
	 * 
	 * @param name
	 * @return
	 */
    public static int putCurrentSlideIntValue(String name) {
        name = name.replaceAll("slide_", "").replaceAll(".pdf", "");
        return Integer.parseInt(name);
    }

    /**
	 * Call with int i and get slide_i.pdf returned
	 * 
	 * @param pageNum
	 * @return
	 */
    public static String putCurrentSlideName(int pageNum) {
        return strSlidePrefix + pageNum + strSlideSuffix;
    }

    public static boolean validateElement(String element, String hash) {
        if (Helper.generateHash(element).equals(hash)) {
            return true;
        } else {
            return false;
        }
    }
}
