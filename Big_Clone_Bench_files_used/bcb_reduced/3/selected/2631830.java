package netblend;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * This class provides constants and utility methods for use throughout the
 * NetBlend system.
 * 
 * @author Ian Thompson
 * 
 */
public class NetBlendSystem {

    /**
	 * The system version
	 */
    public static final String VERSION = "0.4.1";

    /**
	 * Specifies the directory to which the controller application has access.
	 */
    public static final String SOURCE_PATH = "upload";

    /**
	 * Specifies the directory rendered frames are stored to and retrieved from.
	 * This is also used to identify the path on the controller side to download
	 * files to. Thus when running both the slave and controller from the same
	 * directory they share this directory and no downloads will be performed.
	 */
    public static final String OUTPUT_PATH = "render";

    /**
	 * Specifies the directory where configuration and utility files are found.
	 */
    public static final String SYSTEM_PATH = "system";

    /**
	 * The Python script executed pre-render on the scene to be rendered.
	 */
    public static final String RENDER_SCRIPT = "strip_render.py";

    /**
	 * Specifies the file name of the slave executable jar file. Used when
	 * updating the system.
	 */
    public static final String SLAVE_JAR_FILE = "slave.jar";

    /**
	 * Specifies the name of the slave main class file. Used when updating the
	 * system.
	 */
    public static final String SLAVE_JAR_MAIN_CLASS = "netblend.slave.Main";

    /**
	 * Specifies the temporary file name to be used when updating the system.
	 */
    public static final String TEMPORARY_UPDATE_FILE = "update.tmp";

    /**
	 * The file extension used when loading and saving slave lists.
	 */
    public static final String SLAVE_LIST_FILE_EXTENSION = "slaves.xml";

    /**
	 * The file extension used when loading and saving job lists.
	 */
    public static final String JOBS_FILE_EXTENSION = "jobs.xml";

    /**
	 * A list of supported output formats that Blender supports.
	 */
    public static final String[] SUPPORTED_OUTPUT_TYPES = { "TGA", "JPEG", "PNG", "BMP" };

    /**
	 * The suffixes corresponding to the output formats specified in
	 * <code>SUPPORTED_OUTPUT_TYPES</code>.
	 */
    public static final String[] TYPE_SUFFIXES = { "tga", "jpg", "png", "bmp" };

    /**
	 * Default output file format.
	 */
    public static final int DEFAULT_OUTPUT_TYPE = 2;

    /**
	 * Produces a hash from the specified file.
	 * 
	 * @param file
	 *            the input file to process.
	 * @return the hash from the file's data.
	 */
    public static String hashFile(File file) {
        try {
            InputStream in = new FileInputStream(file);
            MessageDigest md = MessageDigest.getInstance("sha1");
            DigestInputStream din = new DigestInputStream(in, md);
            byte[] buffer = new byte[1048576];
            while (din.read(buffer) != -1) ;
            din.close();
            byte[] digest = md.digest();
            return String.format("%0" + (digest.length * 2) + "x", new BigInteger(1, digest));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
