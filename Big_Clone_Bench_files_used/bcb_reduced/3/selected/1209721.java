package simulab.configurator;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;
import simulab.exceptions.FatalException;

/**
 * This class is the central SimuLab instance for managing module configurations.
 * It is designed to be accessible independently by different modules, which request
 * or store their configuration properties.
 *
 * Configuration class is a singleton.
 * 
 * @author pguhl
 */
public class Configuration {

    /**
     * display info string of the application name and version
     */
    public static final String APPLICATION_NAME = "SimuLab v.1";

    /**
     * SL_* constants represent numeric identifiers for the different modules
     * available within SimuLab. The modules itself use this ids to identify
     * themself against the configuration and to store / recieve their
     * configuration information accordingly.
     */
    public static final int SL_MAIN = 0, SL_JSBSIM = 1, SL_NETWORK = 2, SL_SOUND = 3, SL_INPUTS = 4, SL_DATABASE = 5, SL_PRESENTER = 6, SL_ARTIFICIAL = 7, SL_AVIONICS = 8, SL_ACMI = 9, SL_CONFIGURATOR = 10, SL_LENGTH = 11;

    /**
     * bit array for tracking module states
     */
    private static int moduleState = 0;

    public static final short OS_WINDOWS = 1, OS_LINUX = 2, OS_MAC = 3;

    private static short OS = -1;

    private static boolean RELEASE_MODE = false;

    private static String applicationPath;

    public static final void init() throws FatalException {
        if (assertJREProperties()) {
            try {
                setApplicationPath(new File(".").getCanonicalPath().replaceAll("slconfigurator", ""));
                loadJNILibraries(SL_CONFIGURATOR);
            } catch (Exception common) {
                throw new FatalException("Path setup failed: " + common.getMessage());
            }
        } else {
            throw new FatalException("JRE assertion failed, unable to start!");
        }
    }

    public static int getValue(int moduleId, String valueName) {
        System.err.println("unimplemented access:" + valueName);
        if ("udp_port".equals(valueName)) {
            return 9090;
        }
        if ("udp_packet_size".equals(valueName)) {
            return 1024;
        }
        if ("/sim/model-hz".equals(valueName)) {
            return 120;
        }
        if ("/sim/speed-up".equals(valueName)) {
            return 1;
        }
        return 0;
    }

    public static String getStrValue(int moduleId, String valueName) {
        System.err.println("unimplemented access:" + valueName);
        if ("udp_port".equals(valueName)) {
            return "localhost";
        }
        return "";
    }

    /**
     * sets internal configurator state to active for any module identified by
     * @param moduleId numeric modulue identifier @see SL_NETWORK
     * @return false if module allready registered or module id was invalid
     */
    public static final boolean registerModule(int moduleId) {
        if (assertModuleIdentifier(moduleId)) {
            moduleState |= 1 << moduleId;
            loadJNILibraries(moduleId);
            return true;
        }
        return false;
    }

    /**
     *
     * @param eventUID
     * @param shortcut
     */
    public static void saveKey(String eventUID, int shortcut) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * 
     * @param eventUID
     * @param keyModificator
     */
    public static void saveKeyModificator(String eventUID, int keyModificator) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * sets internal configurator state to inactive for any module identified by
     * @param moduleId numeric modulue identifier @see SL_NETWORK
     * @return false if module allready registered or module id was invalid
     */
    public static final boolean unregisterModule(int moduleId) {
        if (assertModuleIdentifier(moduleId)) {
            moduleState ^= 1 << moduleId;
            return true;
        }
        return false;
    }

    public static final boolean isModuleRegistered(int moduleId) {
        return assertModuleIdentifier(moduleId) && ((moduleState & (1 << moduleId)) > 0);
    }

    private static final boolean assertModuleIdentifier(int moduleId) {
        return moduleId > -1 && moduleId < SL_LENGTH;
    }

    /**
     * exposes JVM properties to STDOUT:
     */
    public static final void printJREInfo() {
        Properties props = System.getProperties();
        props.list(System.out);
    }

    private static final boolean assertJREProperties() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            OS = OS_WINDOWS;
        } else if (osName.contains("Linux")) {
            OS = OS_LINUX;
        } else {
            System.err.println(osName + " is not supported!");
            System.exit(-1);
        }
        if (!"1.6".equals(System.getProperty("java.specification.version"))) {
            System.err.println(APPLICATION_NAME + " supports only 1.6 JVM!");
            return false;
        }
        if (!System.getProperty("java.vm.name").contains("HotSpot")) {
            System.err.println(APPLICATION_NAME + " requires 'Java HotSpot(TM) Server VM' JRE!");
            return false;
        }
        return true;
    }

    /**
     * loads custom native compiled libraries into JVM
     * @return
     */
    private static void loadJNILibraries(int moduleId) {
    }

    /**
     * generates md5 checksum for String provided
     * 
     * @param value
     * @return checksum
     */
    public static final String md5Checksum(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes;
            try {
                bytes = value.getBytes("UTF-8");
            } catch (UnsupportedEncodingException uee) {
                bytes = value.getBytes();
            }
            StringBuilder result = new StringBuilder();
            for (byte b : md.digest(bytes)) {
                result.append(Integer.toHexString((b & 0xf0) >>> 4));
                result.append(Integer.toHexString(b & 0x0f));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * name says at all
     * @return primary path the application runs in
     */
    public static final String getApplicationPath() {
        return applicationPath;
    }

    /**
     *
     * setter for the application path provides additional logic
     * and path normalization
     * @param value
     */
    public static final void setApplicationPath(String value) {
        RELEASE_MODE = value.contains("/release/");
        applicationPath = value;
    }

    public static final String getJdbcUrl() {
        return "jdbc:sqlite:" + applicationPath + "databases/%s.s3db";
    }

    public static final boolean runSilent() {
        return false;
    }

    /**
     * fetch event key mapping from database
     * @param eventUID globally unique event identifier
     * @param defaultKey if true, provides default key,
     *        otherwise currently assigned
     * @return
     */
    public static int getEventKey(String eventUID, boolean defaultKey) {
        if (!defaultKey) {
            return -1;
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static int getEventKeyModifier(String eventUID) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * supplies applicaton path redirected to the configured textures folder
     * Textures folder should contain any commonly shared textures
     * @return path to common textures
     */
    public static String getCommonTexturePath() {
        return applicationPath + ((RELEASE_MODE) ? ".." : "/../../release") + "/content/textures/";
    }

    /**
     * creates loading path for MM content by entity unique identifier
     * 
     * @param entityUID unuque entity identifier
     * @return complete path without filename
     */
    public static String getContentPathById(int entityUID) {
        return "";
    }
}
