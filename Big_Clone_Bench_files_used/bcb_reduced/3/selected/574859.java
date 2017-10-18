package glowaxes.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectStreamException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.apache.log4j.Logger;

/**
 * A register singleton that registers charts.
 * 
 * @author <a href="mailto:eddie@tinyelements.com">Eddie Moojen</a>
 * @version 1.0
 */
public class ChartRegistry implements java.io.Serializable {

    /** The algorithm. */
    private static MessageDigest algorithm;

    /** The id. */
    private static long id = Math.round(Math.random() * 1000000);

    /** The logger. */
    private static Logger logger = Logger.getLogger(ChartRegistry.class.getName());

    /** The Constant registeredCharts. */
    private static final AutoRefreshMap registeredCharts = new AutoRefreshMap("registeredCharts");

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -955823968012239964L;

    /** The Constant SINGLETON. */
    public static final ChartRegistry SINGLETON = new ChartRegistry();

    /**
     * Gets the m d5.
     * 
     * @param key
     *            the key
     * 
     * @return the m d5
     */
    private static String getMD5(String key) {
        algorithm.reset();
        algorithm.update(key.getBytes());
        byte messageDigest[] = algorithm.digest();
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
        }
        return hexString.toString();
    }

    /**
     * Singleton constructor for ChartRegistry().
     */
    private ChartRegistry() {
        try {
            algorithm = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            logger.fatal(e);
        }
    }

    /**
     * Adds the chart.
     * 
     * @param id
     *            the id
     * @param array
     *            the array
     */
    public void addChart(String id, byte[] array) {
        if (array == null) throw new NullPointerException("OutputStream is not defined (null)");
        registeredCharts.put(id, array);
    }

    /**
     * Adds the chart.
     * 
     * @param id
     *            the id
     * @param array
     *            the array
     * @param expiretime
     *            the expiretime
     */
    public void addChart(String id, byte[] array, long expiretime) {
        if (array == null) throw new NullPointerException("OutputStream is not defined (null)");
        registeredCharts.put(id, array, expiretime);
    }

    /**
     * Gets the chart.
     * 
     * @param id
     *            the id
     * 
     * @return the chart
     */
    public byte[] getChart(String id) {
        return (byte[]) registeredCharts.get(id);
    }

    /**
     * Gets the hex.
     * 
     * @return the hex
     */
    public String getHex() {
        return getMD5("" + id++);
    }

    /**
     * Gets the hex.
     * 
     * @return the hex
     */
    public String getHex(String hexme) {
        return getMD5("" + hexme);
    }

    /**
     * The readResolve method serves to prevent the release of multiple
     * instances upon de-serialization. Logic unknown... research needed.
     * 
     * @return the object
     * 
     * @throws ObjectStreamException
     *             the object stream exception
     */
    private Object readResolve() throws java.io.ObjectStreamException {
        return null;
    }

    /**
     * Save chart.
     * 
     * @param id
     *            the id
     * @param path
     *            the path
     * @param fileName
     *            the file name
     */
    public void saveChart(String id, String path, String fileName) {
        FileOutputStream ostream = null;
        try {
            ostream = new FileOutputStream(path + fileName);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        byte[] image = getChart(id);
        if (ostream == null || image == null) throw new RuntimeException("id: " + id + ", image value is null.");
        try {
            ostream.write(image);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ostream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            ostream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
